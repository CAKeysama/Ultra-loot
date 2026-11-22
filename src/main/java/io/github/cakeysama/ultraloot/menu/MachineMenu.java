package io.github.cakeysama.ultraloot.menu;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.loot.LootFilter;
import io.github.cakeysama.ultraloot.machine.AbstractMachineBlockEntity;
import io.github.cakeysama.ultraloot.machine.MachineBlock;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineStatus;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import io.github.cakeysama.ultraloot.machine.RelativeSide;
import io.github.cakeysama.ultraloot.machine.SideMode;
import io.github.cakeysama.ultraloot.network.MachineSyncPayload;
import io.github.cakeysama.ultraloot.upgrade.UpgradeCardItem;
import io.github.cakeysama.ultraloot.upgrade.UpgradeInventory;
import io.github.cakeysama.ultraloot.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Shared menu of both machine families.
 * <p>
 * Server side it holds the block entity, validates every {@link MachineAction} and pushes a GUI snapshot
 * ({@link AbstractMachineBlockEntity#writeGuiData}) to the viewing player only when it changed. Client side it
 * only stores the last snapshot; screens read it through the typed getters below.
 * <p>
 * Layout (GUI coordinates, 176 px wide): machine content area {@code y 18..112}, player inventory at
 * ({@link #PLAYER_INV_X}, {@link #PLAYER_INV_Y}), hotbar 58 px below.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 202;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 120;
    /** Upgrade slot row (Upgrades tab). */
    public static final int UPGRADE_X = 35;
    public static final int UPGRADE_Y = 26;
    public static final int UPGRADE_SPACING = 22;

    @Nullable
    protected final AbstractMachineBlockEntity machine;
    protected final BlockPos pos;
    protected final MachineFamily family;
    protected final MachineTier tier;
    protected final Player player;

    private int upgradeSlotStart = -1;
    private int machineSlotEnd;
    private int activeTab = -1;
    private CompoundTag syncData = new CompoundTag();
    @Nullable
    private CompoundTag lastSent;
    private int syncTimer;

    protected MachineMenu(MenuType<?> type, int containerId, Inventory inventory, BlockPos pos, @Nullable AbstractMachineBlockEntity machine) {
        super(type, containerId);
        this.machine = machine;
        this.pos = pos;
        this.player = inventory.player;
        BlockState state = inventory.player.level().getBlockState(pos);
        if (state.getBlock() instanceof MachineBlock block) {
            this.family = block.family();
            this.tier = block.tier();
        } else {
            this.family = machine != null ? machine.family() : MachineFamily.LOOT_GENERATOR;
            this.tier = machine != null ? machine.tier() : MachineTier.T1;
        }
    }

    // ---- Slot setup ----------------------------------------------------------------------------------

    protected void addUpgradeSlots(int tab, IItemHandler upgrades) {
        upgradeSlotStart = slots.size();
        for (int i = 0; i < Balance.UPGRADE_SLOTS; i++) {
            int slot = i;
            addSlot(new TabSlot(this, tab, upgrades, i, UPGRADE_X + i * UPGRADE_SPACING, UPGRADE_Y,
                    stack -> UpgradeInventory.isValidFor(slot, stack), 1));
        }
    }

    /** Must be called last, after every machine slot. */
    protected void addPlayerInventory(Inventory inventory) {
        machineSlotEnd = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    // ---- Tabs (client) -----------------------------------------------------------------------------------

    public int activeTab() {
        return activeTab;
    }

    /** Called by the screen when the player switches tabs; hides the other tabs' slots. */
    public void setActiveTab(int tab) {
        this.activeTab = tab;
    }

    // ---- Shift-click -------------------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < machineSlotEnd) {
            if (!moveItemStackTo(stack, machineSlotEnd, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof UpgradeCardItem card && upgradeSlotStart >= 0) {
            return moveCard(player, slot, stack, card);
        } else if (!moveToMachine(stack)) {
            int hotbarStart = machineSlotEnd + 27;
            boolean moved = index < hotbarStart
                    ? moveItemStackTo(stack, hotbarStart, slots.size(), false)
                    : moveItemStackTo(stack, machineSlotEnd, hotbarStart, false);
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /**
     * Shift-clicked card: fills its category slot, or replaces a lower-level card of the same category (the old card
     * goes back to the player). Equal or lower levels are not swapped.
     */
    private ItemStack moveCard(Player player, Slot source, ItemStack stack, UpgradeCardItem card) {
        Slot target = slots.get(upgradeSlotStart + card.type().slot());
        ItemStack existing = target.getItem();
        if (existing.isEmpty()) {
            moveItemStackTo(stack, target.index, target.index + 1, false);
        } else if (existing.getItem() instanceof UpgradeCardItem old && card.level() > old.level()) {
            ItemStack returned = existing.copy();
            target.set(stack.split(1));
            moveItemStackTo(returned, machineSlotEnd, slots.size(), false);
            if (!returned.isEmpty()) player.getInventory().placeItemBackInInventory(returned);
        } else {
            return ItemStack.EMPTY;
        }
        source.setChanged();
        return ItemStack.EMPTY;
    }

    /** Family-specific shift-click targets (Mob Card slot, XP slot). @return true if anything moved. */
    protected boolean moveToMachine(ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine != null) {
            return !machine.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }
        return true;
    }

    // ---- Server: actions and sync ------------------------------------------------------------------------

    /** Applies a client request after validating it. Unknown or invalid requests are ignored. */
    public void handleAction(ServerPlayer sender, MachineAction action, int a, int b, String text) {
        if (machine == null || sender != player || !stillValid(sender)) return;
        switch (action) {
            case CYCLE_SIDE -> {
                if (a >= 0 && a < RelativeSide.values().length) machine.cycleSide(RelativeSide.byOrdinal(a), b >= 0);
            }
            case SET_SIDE -> {
                if (a >= 0 && a < RelativeSide.values().length && b >= 0 && b < SideMode.values().length) {
                    machine.setSide(RelativeSide.byOrdinal(a), SideMode.byOrdinal(b));
                }
            }
            case SET_FILTER -> {
                if (a >= 0 && a < LootFilter.values().length) machine.setFilter(LootFilter.byOrdinal(a));
            }
            default -> handleFamilyAction(action, a, b, text);
        }
        syncTimer = Balance.GUI_SYNC_INTERVAL; // reflect the change immediately
    }

    protected void handleFamilyAction(MachineAction action, int a, int b, String text) {}

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (machine == null || !(player instanceof ServerPlayer serverPlayer)) return;
        if (lastSent != null && ++syncTimer < Balance.GUI_SYNC_INTERVAL) return;
        syncTimer = 0;
        CompoundTag tag = new CompoundTag();
        machine.writeGuiData(tag);
        if (!tag.equals(lastSent)) {
            CompoundTag payload = GuiSyncDelta.strip(tag, lastSent);
            lastSent = tag;
            PacketDistributor.sendToPlayer(serverPlayer, new MachineSyncPayload(containerId, payload));
        }
    }

    /** Client: receives a snapshot from {@link MachineSyncPayload}; unchanged sticky parts are kept from the last one. */
    public void receiveSync(CompoundTag tag) {
        this.syncData = GuiSyncDelta.merge(tag, syncData);
    }

    // ---- Client getters ------------------------------------------------------------------------------------

    public CompoundTag data() {
        return syncData;
    }

    public BlockPos pos() {
        return pos;
    }

    public MachineFamily family() {
        return family;
    }

    public MachineTier tier() {
        return tier;
    }

    public int energy() {
        return syncData.getInt("energy");
    }

    public int energyMax() {
        return Math.max(1, syncData.contains("energyMax") ? syncData.getInt("energyMax") : tier.energyCapacity());
    }

    public int progress() {
        return syncData.getInt("progress");
    }

    public int ticksPerOperation() {
        return Math.max(1, syncData.getInt("ticksPerOp"));
    }

    /** 0..1 */
    public float progressFraction() {
        return Math.min(1f, (float) progress() / ticksPerOperation());
    }

    public MachineStatus status() {
        return MachineStatus.byOrdinal(syncData.getInt("status"));
    }

    public int energyPerOperation() {
        return syncData.getInt("energyPerOp");
    }

    public SideMode sideMode(RelativeSide side) {
        int[] sides = syncData.getIntArray("sides");
        return side.ordinal() < sides.length ? SideMode.byOrdinal(sides[side.ordinal()]) : SideMode.NONE;
    }

    public LootFilter filter() {
        return LootFilter.byOrdinal(syncData.getInt("filter"));
    }

    public boolean filterUnlocked() {
        return syncData.getBoolean("filterUnlocked");
    }

    public double speedMultiplier() {
        return syncData.getDouble("speed");
    }

    public int attempts() {
        return syncData.getInt("attempts");
    }

    public int range() {
        return syncData.getInt("range");
    }

    public int orbRadius() {
        return syncData.getInt("orbRadius");
    }

    public int lootBonus() {
        return syncData.getInt("lootBonus");
    }

    public double energyMultiplier() {
        return syncData.getDouble("energyMult");
    }

    public long statItems() {
        return syncData.getCompound("stats").getLong("items");
    }

    public long statOperations() {
        return syncData.getCompound("stats").getLong("operations");
    }

    public long statEnergy() {
        return syncData.getCompound("stats").getLong("energy");
    }

    public long statExperience() {
        return syncData.getCompound("stats").getLong("xp");
    }

    public long statFiltered() {
        return syncData.getCompound("stats").getLong("filtered");
    }

    /** Index of the first upgrade slot in {@link #slots}, for screens that decorate them. */
    public int upgradeSlotIndex(UpgradeType type) {
        return upgradeSlotStart + type.slot();
    }
}
