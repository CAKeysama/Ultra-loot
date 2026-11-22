package io.github.cakeysama.ultraloot.menu;

import io.github.cakeysama.ultraloot.machine.CardRestorerBlockEntity;
import io.github.cakeysama.ultraloot.machine.CardRestorerStatus;
import io.github.cakeysama.ultraloot.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu of the Card Restorer: input slot (bound Mob Cards only), take-only output slot and the player inventory.
 * GUI values travel through {@link ContainerData} ({@link CardRestorerBlockEntity#data()}); ints are split into two
 * 16-bit halves because vanilla sends container data as shorts.
 * <p>
 * Layout uses the shared machine panel size ({@link MachineMenu#GUI_WIDTH} x {@link MachineMenu#GUI_HEIGHT}).
 */
public class CardRestorerMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 52;
    public static final int INPUT_Y = 41;
    public static final int OUTPUT_X = 124;
    public static final int OUTPUT_Y = 41;
    private static final int MACHINE_SLOTS = 2;

    @Nullable
    private final CardRestorerBlockEntity restorer;
    private final BlockPos pos;
    private final ContainerData data;

    public CardRestorerMenu(int containerId, Inventory inventory, CardRestorerBlockEntity restorer) {
        this(containerId, inventory, restorer.getBlockPos(), restorer, restorer.inventory(), restorer.data());
    }

    public CardRestorerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null, new ItemStackHandler(MACHINE_SLOTS),
                new SimpleContainerData(CardRestorerBlockEntity.DATA_COUNT));
    }

    private CardRestorerMenu(int containerId, Inventory inventory, BlockPos pos, @Nullable CardRestorerBlockEntity restorer,
                             IItemHandler slots, ContainerData data) {
        super(ModMenus.CARD_RESTORER.get(), containerId);
        this.restorer = restorer;
        this.pos = pos;
        this.data = data;
        addSlot(new SlotItemHandler(slots, CardRestorerBlockEntity.INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CardRestorerBlockEntity.isValidCard(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
        addSlot(new SlotItemHandler(slots, CardRestorerBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // take-only
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, MachineMenu.PLAYER_INV_X + col * 18, MachineMenu.PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, MachineMenu.PLAYER_INV_X + col * 18, MachineMenu.PLAYER_INV_Y + 58));
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = MACHINE_SLOTS;
        int hotbarStart = playerStart + 27;
        int end = slots.size();
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, end, true)) return ItemStack.EMPTY;
        } else if (CardRestorerBlockEntity.isValidCard(stack)) {
            if (!moveItemStackTo(stack, CardRestorerBlockEntity.INPUT, CardRestorerBlockEntity.INPUT + 1, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = index < hotbarStart
                    ? moveItemStackTo(stack, hotbarStart, end, false)
                    : moveItemStackTo(stack, playerStart, hotbarStart, false);
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

    @Override
    public boolean stillValid(Player player) {
        if (restorer != null) {
            return !restorer.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }
        return true;
    }

    // ---- Client getters ------------------------------------------------------------------------------

    private int wide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) << 16);
    }

    public BlockPos pos() {
        return pos;
    }

    public int energy() {
        return Math.max(0, wide(CardRestorerBlockEntity.DATA_ENERGY));
    }

    public int energyMax() {
        return Math.max(1, wide(CardRestorerBlockEntity.DATA_ENERGY_MAX));
    }

    public int wear() {
        return Math.max(0, wide(CardRestorerBlockEntity.DATA_WEAR));
    }

    public int maxWear() {
        return Math.max(1, wide(CardRestorerBlockEntity.DATA_MAX_WEAR));
    }

    /** FE per wear point of the inserted card (0 without a card). */
    public int costPerWear() {
        return Math.max(0, wide(CardRestorerBlockEntity.DATA_COST));
    }

    public int wearPerTick() {
        return Math.max(1, wide(CardRestorerBlockEntity.DATA_RATE));
    }

    public CardRestorerStatus status() {
        return CardRestorerStatus.byOrdinal(data.get(CardRestorerBlockEntity.DATA_STATUS));
    }

    /** Card decay 0..1 (0 intact, 1 corrupted). */
    public float decay() {
        return Math.clamp(data.get(CardRestorerBlockEntity.DATA_DECAY) / 1000f, 0f, 1f);
    }

    public ItemStack inputCard() {
        return slots.get(CardRestorerBlockEntity.INPUT).getItem();
    }

    /** Remaining integrity 0..1 of the input card. */
    public float integrity() {
        return 1f - Math.min(1f, (float) wear() / maxWear());
    }
}
