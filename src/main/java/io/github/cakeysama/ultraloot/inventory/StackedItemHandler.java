package io.github.cakeysama.ultraloot.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Read/insert/extract view that exposes several handlers as one, slot after slot. Unlike NeoForge's
 * {@code CombinedInvWrapper} it accepts plain {@link IItemHandler}s, so restricted automation views can be stacked
 * without granting {@code setStackInSlot} access.
 */
public final class StackedItemHandler implements IItemHandler {
    private final IItemHandler[] parts;

    public StackedItemHandler(IItemHandler... parts) {
        this.parts = parts.clone();
    }

    @Override
    public int getSlots() {
        int slots = 0;
        for (IItemHandler part : parts) slots += part.getSlots();
        return slots;
    }

    /** Index of the part holding {@code slot}; {@code slot} is rewritten into {@code local[0]}. -1 when out of range. */
    private int locate(int slot, int[] local) {
        if (slot < 0) return -1;
        int remaining = slot;
        for (int i = 0; i < parts.length; i++) {
            int n = parts[i].getSlots();
            if (remaining < n) {
                local[0] = remaining;
                return i;
            }
            remaining -= n;
        }
        return -1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int[] local = new int[1];
        int part = locate(slot, local);
        return part < 0 ? ItemStack.EMPTY : parts[part].getStackInSlot(local[0]);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int[] local = new int[1];
        int part = locate(slot, local);
        return part < 0 ? stack : parts[part].insertItem(local[0], stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int[] local = new int[1];
        int part = locate(slot, local);
        return part < 0 ? ItemStack.EMPTY : parts[part].extractItem(local[0], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int[] local = new int[1];
        int part = locate(slot, local);
        return part < 0 ? 0 : parts[part].getSlotLimit(local[0]);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int[] local = new int[1];
        int part = locate(slot, local);
        return part >= 0 && parts[part].isItemValid(local[0], stack);
    }
}
