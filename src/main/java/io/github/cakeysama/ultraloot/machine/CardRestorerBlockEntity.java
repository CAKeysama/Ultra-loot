package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.energy.MachineEnergyStorage;
import io.github.cakeysama.ultraloot.menu.CardRestorerMenu;
import io.github.cakeysama.ultraloot.mob.CardWear;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.mob.MobSimulationData;
import io.github.cakeysama.ultraloot.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Card Restorer: removes wear from the Mob Card in its input slot for FE, then moves the restored card to the output.
 * <p>
 * Each server tick repairs {@code min(wearPerTick, wear, storedFE / costPerWear)} points and pays exactly
 * {@code repaired * costPerWear} FE ({@link CardRestorerLogic}); the wear is mutated on the stack in place. A card at
 * 0 wear moves to the output only when the output is empty. Automation (every face) inserts only valid cards into the
 * input and extracts only from the output; energy is accepted on every face.
 */
public class CardRestorerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    /** Ticks the "active" look stays on after work stops; avoids block updates when FE arrives in bursts. */
    private static final int ACTIVE_LINGER_TICKS = 40;

    // ---- GUI sync (ContainerData values travel as shorts, so ints are split into 16-bit halves) ----
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_MAX = 2;
    public static final int DATA_WEAR = 4;
    public static final int DATA_MAX_WEAR = 6;
    public static final int DATA_COST = 8;
    public static final int DATA_RATE = 10;
    /** Single-slot values. */
    public static final int DATA_STATUS = 12;
    /** Card decay 0..1000 (per mille). */
    public static final int DATA_DECAY = 13;
    public static final int DATA_COUNT = 14;

    private final MachineEnergyStorage energy;
    private final IEnergyStorage energyInput;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidCard(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Automation view: insert only valid cards into the input, extract only from the output. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT && isValidCard(stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT && isValidCard(stack);
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_DECAY -> decayPerMille();
                default -> {
                    int value = wideValue(index & ~1);
                    yield (index & 1) == 0 ? value & 0xFFFF : value >>> 16;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side values are computed live; the client uses a SimpleContainerData.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private CardRestorerStatus status = CardRestorerStatus.IDLE;
    private int activeTimer;

    public CardRestorerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARD_RESTORER.get(), pos, state);
        this.energy = new MachineEnergyStorage(Balance.RESTORER_ENERGY_CAPACITY, Balance.RESTORER_ENERGY_INPUT, this::setChanged);
        this.energyInput = energy.inputView();
    }

    /** Bound Mob Cards only (same rule as the Mob Loot Generator's card slot). */
    public static boolean isValidCard(ItemStack stack) {
        return stack.getItem() instanceof MobCardItem && MobCardItem.getEntityType(stack).isPresent();
    }

    // ---- Ticking -------------------------------------------------------------------------------

    public void tickServer(ServerLevel level) {
        status = tickRepair();
        if (status.isWorking()) {
            activeTimer = ACTIVE_LINGER_TICKS;
        } else if (activeTimer > 0) {
            activeTimer--;
        }
        updateActiveState(activeTimer > 0);
    }

    private CardRestorerStatus tickRepair() {
        ItemStack card = inventory.getStackInSlot(INPUT);
        if (card.isEmpty()) return CardRestorerStatus.IDLE;
        int wear = CardWear.wear(card);
        boolean repairedNow = false;
        if (wear > 0) {
            long cost = costPerWear(card);
            // Dynamic buffer (as the generators): always able to hold at least one wear point, whatever the config.
            energy.ensureCapacity((int) Math.min(Balance.MAX_OPERATION_ENERGY, cost));
            CardRestorerLogic.Step step = CardRestorerLogic.repair(wear, energy.getEnergyStored(), cost,
                    Balance.restorerWearPerTick);
            // step.energy() <= stored FE (an int), so the cast is exact; consume is all-or-nothing.
            if (step.repaired() <= 0 || !energy.consume((int) step.energy())) return CardRestorerStatus.NO_ENERGY;
            wear -= step.repaired();
            // Mutated in place: the stack stays the same object, the slot's change hook is not needed.
            CardWear.setWear(card, wear);
            setChanged();
            if (wear > 0) return CardRestorerStatus.RUNNING;
            repairedNow = true;
        }
        if (!inventory.getStackInSlot(OUTPUT).isEmpty()) return CardRestorerStatus.OUTPUT_FULL;
        ItemStack restored = card.copy();
        inventory.setStackInSlot(INPUT, ItemStack.EMPTY);
        inventory.setStackInSlot(OUTPUT, restored);
        return repairedNow ? CardRestorerStatus.RUNNING : CardRestorerStatus.IDLE;
    }

    /** FE per wear point for {@code card}: ceil(energyPerWear x mob energy multiplier), at least 1. */
    public static long costPerWear(ItemStack card) {
        double multiplier = MobCardItem.getEntityType(card).map(type -> MobSimulationData.of(type).energyMultiplier()).orElse(1.0);
        return CardRestorerLogic.costPerWear(Balance.restorerEnergyPerWear, multiplier);
    }

    private int wideValue(int index) {
        ItemStack card = inventory.getStackInSlot(INPUT);
        return switch (index) {
            case DATA_ENERGY -> energy.getEnergyStored();
            case DATA_ENERGY_MAX -> energy.getMaxEnergyStored();
            case DATA_WEAR -> CardWear.wear(card);
            case DATA_MAX_WEAR -> CardWear.maxWear();
            case DATA_COST -> card.isEmpty() ? 0 : (int) Math.min(Integer.MAX_VALUE, costPerWear(card));
            case DATA_RATE -> Balance.restorerWearPerTick;
            default -> 0;
        };
    }

    private int decayPerMille() {
        ItemStack card = inventory.getStackInSlot(INPUT);
        return card.isEmpty() ? 0 : (int) Math.round(CardWear.decay(card) * 1000.0);
    }

    /** Same as {@link AbstractMachineBlockEntity#setChanged()}: no comparator output, so no neighbour updates. */
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

    // ---- Capabilities ----------------------------------------------------------------------------

    /** Every face (and the internal null side) accepts FE. */
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return energyInput;
    }

    /** Every face: insert cards into the input, extract restored cards from the output. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        return automation;
    }

    // ---- Accessors -------------------------------------------------------------------------------

    public MachineEnergyStorage energy() {
        return energy;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    public CardRestorerStatus status() {
        return status;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CardRestorerMenu(containerId, playerInventory, this);
    }

    // ---- Persistence -------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energy.serializeNBT(registries));
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energy.deserializeNBT(registries, tag.get("Energy"));
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    // ---- Removal ----------------------------------------------------------------------------------

    /** Drops both slots when the block is broken; the slots are cleared so nothing can be dropped twice. */
    public void dropContents(Level level) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}
