package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.energy.MachineEnergyStorage;
import io.github.cakeysama.ultraloot.loot.LootFilter;
import io.github.cakeysama.ultraloot.upgrade.UpgradeInventory;
import io.github.cakeysama.ultraloot.upgrade.UpgradeModifiers;
import io.github.cakeysama.ultraloot.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Shared machine core: FE buffer, upgrade cards, side configuration, statistics and the operation cycle.
 * <p>
 * Cycle (server only): progress advances only while the machine can work and holds enough FE for one operation.
 * When progress completes, {@link #performOperation(int)} must either finish everything (generate, insert, then
 * consume FE/XP) or change nothing. A blocked operation keeps its progress and retries after a short cooldown.
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity implements MenuProvider {
    protected final MachineFamily family;
    protected final MachineTier tier;
    protected final MachineEnergyStorage energy;
    protected final UpgradeInventory upgrades;
    protected final SideConfiguration sides;
    protected final MachineStatistics statistics = new MachineStatistics();

    private UpgradeModifiers modifiers;
    protected LootFilter filter = LootFilter.ALL;
    protected int progress;
    protected int cooldown;
    protected MachineStatus status = MachineStatus.NO_ENERGY;
    /** Ticks the "active" look stays on after work stops; avoids block updates when FE hovers around the cost. */
    private int activeTimer;

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineFamily family) {
        super(type, pos, state);
        this.family = family;
        this.tier = state.getBlock() instanceof MachineBlock block ? block.tier() : MachineTier.T1;
        this.energy = new MachineEnergyStorage(tier.energyCapacity(), tier.energyInput(), this::setChanged);
        this.upgrades = new UpgradeInventory(this::onUpgradesChanged);
        this.sides = new SideConfiguration(family);
        this.modifiers = UpgradeModifiers.compute(family, tier, upgrades.levels());
    }

    // ---- Ticking -------------------------------------------------------------------------------

    public final void tickServer(ServerLevel level) {
        tickOperation(level);
        if (status.isWorking()) {
            activeTimer = ACTIVE_LINGER_TICKS;
        } else if (activeTimer > 0) {
            activeTimer--;
        }
        updateActiveState(activeTimer > 0);
    }

    private static final int ACTIVE_LINGER_TICKS = 40;

    private void tickOperation(ServerLevel level) {
        preOperationTick(level);
        // Dynamic buffer: always large enough for one operation (cards, profile and card wear change the cost).
        boolean fits = energy.ensureCapacity(energyPerOperation());
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        MachineStatus blocker = checkCanOperate(level);
        if (blocker != null) {
            setStatus(blocker);
            return;
        }
        int cost = energyPerOperation();
        if (!fits || cost > energy.capacity()) {
            // FE is paid in one piece (design G2); only a cost above Balance.MAX_OPERATION_ENERGY can get here.
            setStatus(MachineStatus.ENERGY_BUFFER_TOO_SMALL);
            return;
        }
        if (!energy.has(cost)) {
            setStatus(MachineStatus.NO_ENERGY);
            return;
        }
        setStatus(MachineStatus.RUNNING);
        progress++;
        if (progress >= modifiers.ticksPerOperation()) {
            MachineStatus result = performOperation(cost);
            if (result == MachineStatus.RUNNING) {
                progress = 0;
            } else {
                progress = modifiers.ticksPerOperation();
                cooldown = Balance.BLOCKED_RETRY_TICKS;
                setStatus(result);
            }
        }
        setChanged();
    }

    /** Work done every tick before the operation check (scanning, exporting, XP intake...). */
    protected abstract void preOperationTick(ServerLevel level);

    /** @return null when an operation may run, otherwise the status explaining why not. */
    @Nullable
    protected abstract MachineStatus checkCanOperate(ServerLevel level);

    /**
     * Runs one complete operation. Implementations must consume {@code energyCost} through
     * {@link MachineEnergyStorage#consume(int)} only after the results are committed.
     *
     * @return {@link MachineStatus#RUNNING} on success; any other status means nothing changed.
     */
    protected abstract MachineStatus performOperation(int energyCost);

    /** Multiplier applied to E_base (loot profile or mob data). */
    protected abstract double energyBaseMultiplier();

    public int energyPerOperation() {
        return modifiers.energyPerOperation(family.baseEnergyPerOperation(tier) * energyBaseMultiplier());
    }

    protected void setStatus(MachineStatus newStatus) {
        status = newStatus;
    }

    /**
     * Marks the chunk as needing a save. Unlike the vanilla implementation this skips
     * {@code Level#updateNeighbourForOutputSignal}: machines have no comparator output, and the machine is marked
     * dirty every working tick (progress) and on every FE transfer, so the vanilla call would send useless
     * neighbour notifications to adjacent cables and chests several times per tick.
     */
    @Override
    public void setChanged() {
        if (level != null) level.blockEntityChanged(worldPosition);
    }

    private void updateActiveState(boolean active) {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.hasProperty(MachineBlock.ACTIVE) && state.getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Upgrades / configuration ---------------------------------------------------------------

    private void onUpgradesChanged() {
        modifiers = UpgradeModifiers.compute(family, tier, upgrades.levels());
        if (!upgrades.levels().has(UpgradeType.LOOT)) filter = LootFilter.ALL;
        progress = Math.min(progress, modifiers.ticksPerOperation());
        onModifiersChanged();
        setChanged();
    }

    /** Called after cards changed (e.g. the Loot Generator rescans when its range changes). */
    protected void onModifiersChanged() {}

    public UpgradeModifiers modifiers() {
        return modifiers;
    }

    /** Filters require a Loot Card. @return true when the filter changed. */
    public boolean setFilter(LootFilter newFilter) {
        if (newFilter != LootFilter.ALL && !upgrades.levels().has(UpgradeType.LOOT)) return false;
        if (filter == newFilter) return false;
        filter = newFilter;
        setChanged();
        return true;
    }

    public void cycleSide(RelativeSide side, boolean forward) {
        sides.cycle(side, forward);
        onSidesChanged();
    }

    public void setSide(RelativeSide side, SideMode mode) {
        if (sides.set(side, mode)) onSidesChanged();
    }

    protected void onSidesChanged() {
        if (level != null) level.invalidateCapabilities(worldPosition);
        setChanged();
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    public SideMode sideMode(@Nullable Direction worldSide) {
        return worldSide == null ? SideMode.NONE : sides.get(facing(), worldSide);
    }

    // ---- Capabilities ---------------------------------------------------------------------------

    /** null side = internal/full view (used by tools such as probes). */
    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (side == null) return energy.inputView();
        return sideMode(side) == SideMode.ENERGY_INPUT ? energy.inputView() : null;
    }

    @Nullable
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return null;
    }

    // ---- Accessors ------------------------------------------------------------------------------

    public MachineFamily family() {
        return family;
    }

    public MachineTier tier() {
        return tier;
    }

    public MachineEnergyStorage energy() {
        return energy;
    }

    public UpgradeInventory upgrades() {
        return upgrades;
    }

    public SideConfiguration sides() {
        return sides;
    }

    public MachineStatistics statistics() {
        return statistics;
    }

    public MachineStatus status() {
        return status;
    }

    public LootFilter filter() {
        return filter;
    }

    public int progress() {
        return progress;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- GUI sync --------------------------------------------------------------------------------

    /** Writes everything the GUI shows. Sent to players with the menu open only when it changes. */
    public void writeGuiData(CompoundTag tag) {
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("energyMax", energy.getMaxEnergyStored());
        tag.putInt("progress", progress);
        tag.putInt("ticksPerOp", modifiers.ticksPerOperation());
        tag.putInt("status", status.ordinal());
        tag.putInt("energyPerOp", energyPerOperation());
        tag.putIntArray("sides", sides.toArray());
        tag.putInt("filter", filter.ordinal());
        tag.putBoolean("filterUnlocked", upgrades.levels().has(UpgradeType.LOOT));
        tag.putDouble("speed", modifiers.speedMultiplier());
        tag.putInt("attempts", modifiers.attempts());
        tag.putInt("range", modifiers.range());
        tag.putInt("orbRadius", modifiers.orbCollectRadius());
        tag.putInt("lootBonus", modifiers.lootBonus());
        tag.putDouble("energyMult", modifiers.energyMultiplier());
        tag.put("stats", statistics.save());
    }

    // ---- Persistence -------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energy.serializeNBT(registries));
        tag.put("Upgrades", upgrades.serializeNBT(registries));
        tag.putIntArray("Sides", sides.toArray());
        tag.put("Stats", statistics.save());
        tag.putInt("Progress", progress);
        tag.putInt("Filter", filter.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energy.deserializeNBT(registries, tag.get("Energy"));
        if (tag.contains("Upgrades", Tag.TAG_COMPOUND)) upgrades.deserializeNBT(registries, tag.getCompound("Upgrades"));
        if (tag.contains("Sides")) sides.fromArray(tag.getIntArray("Sides"));
        statistics.load(tag.getCompound("Stats"));
        progress = Math.max(0, tag.getInt("Progress"));
        modifiers = UpgradeModifiers.compute(family, tier, upgrades.levels());
        filter = upgrades.levels().has(UpgradeType.LOOT) ? LootFilter.byOrdinal(tag.getInt("Filter")) : LootFilter.ALL;
    }

    // ---- Removal ----------------------------------------------------------------------------------

    /** Drops every item held by the machine when it is broken. Subclasses add their own inventories. */
    public void dropContents(Level level) {
        for (int i = 0; i < upgrades.getSlots(); i++) {
            dropStack(level, upgrades.getStackInSlot(i));
        }
    }

    protected void dropStack(Level level, ItemStack stack) {
        if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
    }
}
