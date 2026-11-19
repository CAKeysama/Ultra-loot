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
import java.util.List;
import java.util.Objects;

/**
 * Loot Generator: rolls the selected loot profile and inserts the results directly into nearby inventories.
 * It has no output inventory and never drops items. A small overflow buffer only exists to keep items a target
 * rejected after a successful simulation (rare, misbehaving inventories); the machine pauses until it is flushed.
 */
public class LootGeneratorBlockEntity extends AbstractMachineBlockEntity {
    @Nullable
    private ResourceLocation profileId;
    private InventoryScanner.TargetMode targetMode = InventoryScanner.TargetMode.STORAGE_ONLY;
    private InventoryScanner.Result scan = InventoryScanner.Result.EMPTY;
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
        scan = InventoryScanner.scan(level, worldPosition, facing(), sides, modifiers().range(), targetMode);
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
        if (handlers.isEmpty()) return MachineStatus.NO_TARGETS;
        if (!overflow.isEmpty() && !flushOverflow(handlers)) return MachineStatus.TARGETS_FULL;
        return null;
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
        if (handlers.isEmpty()) return MachineStatus.NO_TARGETS;

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
        requestRescan();
    }

    @Override
    protected void onSidesChanged() {
        super.onSidesChanged();
        requestRescan();
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
        requestRescan();
        setChanged();
    }

    public InventoryScanner.TargetMode targetMode() {
        return targetMode;
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
        tag.putInt("overflow", overflow.stream().mapToInt(ItemStack::getCount).sum());

        ListTag entries = new ListTag();
        for (InventoryScanner.Entry entry : scan.entries()) {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", entry.pos().asLong());
            e.putInt("dist", entry.distance());
            e.putInt("free", entry.freeSlots());
            e.putInt("total", entry.totalSlots());
            e.putInt("state", entry.state().ordinal());
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
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LootGeneratorMenu(containerId, inventory, this);
    }
}
