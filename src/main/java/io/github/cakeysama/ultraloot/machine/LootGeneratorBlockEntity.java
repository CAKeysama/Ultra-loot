package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.inventory.InventoryScanner;
import io.github.cakeysama.ultraloot.inventory.TransactionalInserter;
import io.github.cakeysama.ultraloot.loot.LootEngine;
import io.github.cakeysama.ultraloot.loot.LootProfile;
import io.github.cakeysama.ultraloot.loot.LootProfileManager;
import io.github.cakeysama.ultraloot.menu.LootGeneratorMenu;
import io.github.cakeysama.ultraloot.mob.SimulatedEntityCache;
import io.github.cakeysama.ultraloot.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Loot Generator: rolls the selected loot profile and inserts the results directly into nearby inventories.
 * It has no output inventory and never drops items. A small overflow buffer only exists to keep items a target
 * rejected after a successful simulation (rare, misbehaving inventories); the machine pauses until it is flushed.
 */
public class LootGeneratorBlockEntity extends AbstractMachineBlockEntity {
    @Nullable
    private ResourceLocation profileId;
    private InventoryScanner.TargetMode targetMode = InventoryScanner.TargetMode.STORAGE_ONLY;
    /** "Picked only": deliver only to the inventories the player picked in the Inventory Scan tab. */
    private boolean pickedOnly;
    /** Picked inventories as offsets from this machine, so the choice follows the machine if it is moved. */
    private final Set<BlockPos> pickedOffsets = new LinkedHashSet<>();
    private InventoryScanner.Result scan = InventoryScanner.Result.EMPTY;
    /** Capability caches of the targets, reused across scans (pruned by every scan, cleared on config change/removal). */
    private final InventoryScanner.TargetCaches targetCaches = new InventoryScanner.TargetCaches();
    private int scanTimer;
    private int roundRobin;
    private long lastManualRescan = Long.MIN_VALUE / 2;
    private final List<ItemStack> overflow = new ArrayList<>();
    private final SimulatedEntityCache contextEntities = new SimulatedEntityCache();

    public LootGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOT_GENERATOR.get(), pos, state, MachineFamily.LOOT_GENERATOR);
    }

    // ---- Operation -------------------------------------------------------------------------------

    @Override
    protected void preOperationTick(ServerLevel level) {
        if (--scanTimer <= 0) rescan(level);
    }

    private void rescan(ServerLevel level) {
        scanTimer = Balance.scanIntervalTicks;
        Set<BlockPos> picked = pickedPositions();
        scan = InventoryScanner.scan(level, worldPosition, facing(), sides, modifiers().range(), targetMode, targetCaches,
                pickedOnly ? picked : null, picked);
    }

    /** Absolute positions of the picked inventories. */
    public Set<BlockPos> pickedPositions() {
        Set<BlockPos> result = new HashSet<>(pickedOffsets.size() * 2);
        for (BlockPos offset : pickedOffsets) result.add(worldPosition.offset(offset));
        return result;
    }

    /** Drops every cached target capability and schedules a fresh scan (range, faces or target mode changed). */
    private void resetTargets() {
        targetCaches.clear();
        requestRescan();
    }

    /** Capability caches currently owned by this machine (exposed for tests). */
    public InventoryScanner.TargetCaches targetCaches() {
        return targetCaches;
    }

    /** Schedules a scan on the next tick; manual requests are throttled so clicking can't force a scan every tick. */
    public void requestRescan() {
        scanTimer = Math.min(scanTimer, 1);
    }

    public void requestManualRescan() {
        long now = level == null ? 0 : level.getGameTime();
        if (now - lastManualRescan < Balance.MANUAL_RESCAN_COOLDOWN) return;
        lastManualRescan = now;
        requestRescan();
    }

    @Nullable
    public LootProfile profile() {
        if (profileId == null || level == null || level.getServer() == null) return null;
        return LootProfileManager.get().get(level.getServer(), profileId);
    }

    @Nullable
    @Override
    protected MachineStatus checkCanOperate(ServerLevel level) {
        if (profileId == null) return MachineStatus.NO_PROFILE;
        LootProfile profile = profile();
        if (profile == null) return MachineStatus.INVALID_PROFILE;
        if (profile.minTier() > tier.number()) return MachineStatus.PROFILE_LOCKED;
        List<IItemHandler> handlers = handlers();
        if (handlers.isEmpty()) return noTargetsStatus();
        if (!overflow.isEmpty() && !flushOverflow(handlers)) return MachineStatus.TARGETS_FULL;
        return null;
    }

    /** No usable target: in "Picked only" mode with inventories in range but none picked, say so. */
    private MachineStatus noTargetsStatus() {
        if (pickedOnly && scan.entries().stream().anyMatch(e -> e.state() == InventoryScanner.EntryState.NOT_PICKED)) {
            return MachineStatus.NO_PICKED_TARGETS;
        }
        return MachineStatus.NO_TARGETS;
    }

    @Override
    protected MachineStatus performOperation(int energyCost) {
        ServerLevel serverLevel = (ServerLevel) Objects.requireNonNull(level);
        LootProfile profile = profile();
        if (profile == null) return MachineStatus.INVALID_PROFILE;
        LootTable table = LootEngine.resolveTable(serverLevel, profile);
        LootParams params = table == null ? null
                : LootEngine.buildParams(serverLevel, worldPosition, profile, modifiers().lootBonus(), contextEntities);
        if (params == null) return MachineStatus.INVALID_PROFILE;

        List<IItemHandler> handlers = handlers();
        if (handlers.isEmpty()) return noTargetsStatus();

        LootEngine.Result result;
        try {
            result = LootEngine.roll(table, params, modifiers().attempts(), filter);
        } catch (RuntimeException e) {
            UltraLoot.LOGGER.error("Loot profile {} failed to roll at {}", profile.id(), worldPosition, e);
            return MachineStatus.INVALID_PROFILE;
        }
        var plan = TransactionalInserter.plan(result.items(), handlers, roundRobin);
        if (plan.isEmpty()) return MachineStatus.TARGETS_FULL; // nothing generated, nothing consumed

        overflow.addAll(TransactionalInserter.commit(plan.get(), handlers));
        roundRobin = plan.get().nextStart();
        energy.consume(energyCost);
        statistics.recordOperation(energyCost, 0, result.itemCount(), result.filteredCount());
        return MachineStatus.RUNNING;
    }

    private List<IItemHandler> handlers() {
        List<IItemHandler> handlers = new ArrayList<>(scan.targets().size());
        for (InventoryScanner.Target target : scan.targets()) {
            IItemHandler handler = target.handler();
            if (handler != null) handlers.add(handler);
        }
        return handlers;
    }

    private boolean flushOverflow(List<IItemHandler> handlers) {
        var plan = TransactionalInserter.plan(List.copyOf(overflow), handlers, roundRobin);
        if (plan.isEmpty()) return false;
        overflow.clear();
        overflow.addAll(TransactionalInserter.commit(plan.get(), handlers));
        setChanged();
        return overflow.isEmpty();
    }

    @Override
    protected double energyBaseMultiplier() {
        LootProfile profile = profile();
        return profile == null ? 1.0 : profile.energyMultiplier();
    }

    @Override
    protected void onModifiersChanged() {
        resetTargets(); // the range may have changed
    }

    @Override
    protected void onSidesChanged() {
        super.onSidesChanged();
        resetTargets();
    }

    // ---- Configuration (validated server-side by the menu) ---------------------------------------------

    /** @return true when accepted (profile exists and the tier is high enough). */
    public boolean selectProfile(@Nullable ResourceLocation id) {
        if (id == null) {
            profileId = null;
        } else {
            if (level == null || level.getServer() == null) return false;
            LootProfile profile = LootProfileManager.get().get(level.getServer(), id);
            if (profile == null || profile.minTier() > tier.number()) return false;
            profileId = id;
        }
        progress = 0;
        setChanged();
        return true;
    }

    public void setTargetMode(InventoryScanner.TargetMode mode) {
        if (targetMode == mode) return;
        targetMode = mode;
        resetTargets();
        setChanged();
    }

    public InventoryScanner.TargetMode targetMode() {
        return targetMode;
    }

    public void setPickedOnly(boolean value) {
        if (pickedOnly == value) return;
        pickedOnly = value;
        requestRescan();
        setChanged();
    }

    public boolean pickedOnly() {
        return pickedOnly;
    }

    /**
     * Picks or unpicks the inventory at {@code pos}. Unpicking always works; picking only accepts an inventory the
     * last scan listed (so a client can't store arbitrary positions) and at most {@link Balance#MAX_PICKED_TARGETS}.
     *
     * @return true when the picked set changed
     */
    public boolean togglePicked(BlockPos pos) {
        BlockPos offset = pos.subtract(worldPosition);
        boolean changed;
        if (pickedOffsets.remove(offset)) {
            changed = true;
        } else {
            InventoryScanner.Entry entry = scan.entries().stream().filter(e -> e.pos().equals(pos)).findFirst().orElse(null);
            if (entry == null) return false;
            if (entry.picked()) {
                // The other half of this double chest is the stored one: unpick that one instead.
                changed = pickedOffsets.removeIf(o -> isOtherHalf(pos, worldPosition.offset(o)));
            } else {
                if (pickedOffsets.size() >= Balance.MAX_PICKED_TARGETS) return false;
                changed = pickedOffsets.add(offset.immutable());
            }
        }
        if (changed) {
            requestRescan();
            setChanged();
        }
        return changed;
    }

    private boolean isOtherHalf(BlockPos pos, BlockPos candidate) {
        if (level == null) return false;
        return InventoryScanner.isPicked(Set.of(candidate), pos, level.getBlockState(pos));
    }

    public int pickedCount() {
        return pickedOffsets.size();
    }

    public InventoryScanner.Result scanResult() {
        return scan;
    }

    // ---- GUI ------------------------------------------------------------------------------------------

    @Override
    public void writeGuiData(CompoundTag tag) {
        super.writeGuiData(tag);
        if (profileId != null) tag.putString("profile", profileId.toString());
        tag.putInt("targetMode", targetMode.ordinal());
        tag.putBoolean("pickedOnly", pickedOnly);
        tag.putInt("pickedCount", pickedOffsets.size());
        tag.putInt("overflow", overflow.stream().mapToInt(ItemStack::getCount).sum());

        ListTag entries = new ListTag();
        for (InventoryScanner.Entry entry : scan.entries()) {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", entry.pos().asLong());
            e.putInt("dist", entry.distance());
            e.putInt("free", entry.freeSlots());
            e.putInt("total", entry.totalSlots());
            e.putInt("state", entry.state().ordinal());
            e.putBoolean("picked", entry.picked());
            e.putString("block", entry.block().toString());
            entries.add(e);
        }
        tag.put("scan", entries);
        tag.putInt("targets", scan.targets().size());

        ListTag profiles = new ListTag();
        if (level != null && level.getServer() != null) {
            for (LootProfile profile : LootProfileManager.get().all(level.getServer())) {
                CompoundTag p = new CompoundTag();
                p.putString("id", profile.id().toString());
                p.putString("key", profile.translationKey());
                p.putString("name", profile.fallbackName());
                p.putString("icon", BuiltInRegistries.ITEM.getKey(profile.icon()).toString());
                p.putString("category", profile.category());
                p.putInt("minTier", profile.minTier());
                p.putDouble("energy", profile.energyMultiplier());
                profiles.add(p);
            }
        }
        tag.put("profiles", profiles);
    }

    // ---- Persistence ------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (profileId != null) tag.putString("Profile", profileId.toString());
        tag.putInt("TargetMode", targetMode.ordinal());
        tag.putBoolean("PickedOnly", pickedOnly);
        tag.putLongArray("Picked", pickedOffsets.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("RoundRobin", roundRobin);
        ListTag list = new ListTag();
        for (ItemStack stack : overflow) {
            if (!stack.isEmpty()) list.add(stack.save(registries));
        }
        tag.put("Overflow", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        profileId = tag.contains("Profile") ? ResourceLocation.tryParse(tag.getString("Profile")) : null;
        targetMode = InventoryScanner.TargetMode.byOrdinal(tag.getInt("TargetMode"));
        pickedOnly = tag.getBoolean("PickedOnly");
        pickedOffsets.clear();
        for (long packed : tag.getLongArray("Picked")) {
            if (pickedOffsets.size() >= Balance.MAX_PICKED_TARGETS) break;
            pickedOffsets.add(BlockPos.of(packed));
        }
        roundRobin = Math.max(0, tag.getInt("RoundRobin"));
        overflow.clear();
        ListTag list = tag.getList("Overflow", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(overflow::add);
        }
        scanTimer = 0;
    }

    @Override
    public void dropContents(Level level) {
        super.dropContents(level);
        overflow.forEach(stack -> dropStack(level, stack));
        overflow.clear();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        contextEntities.clear();
        // Also runs on chunk unload (LevelChunk#clearAllBlockEntities); drop the caches and the targets holding them.
        targetCaches.clear();
        scan = InventoryScanner.Result.EMPTY;
    }

    @Override
    public void setLevel(Level level) {
        if (this.level != level) {
            targetCaches.clear();
            scan = InventoryScanner.Result.EMPTY;
        }
        super.setLevel(level);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LootGeneratorMenu(containerId, inventory, this);
    }
}
