package io.github.cakeysama.ultraloot.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Predicate;

/**
 * Automation view of an internal inventory: insertion and extraction are allowed only for stacks accepted by the
 * given predicates. Used so faces never grant more access than their configured role.
 */
public class RestrictedItemHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final Predicate<ItemStack> canInsert;
    private final Predicate<ItemStack> canExtract;

    public RestrictedItemHandler(IItemHandler delegate, Predicate<ItemStack> canInsert, Predicate<ItemStack> canExtract) {
        this.delegate = delegate;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    public static RestrictedItemHandler extractOnly(IItemHandler delegate) {
        return new RestrictedItemHandler(delegate, s -> false, s -> true);
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return canInsert.test(stack) ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return canExtract.test(delegate.getStackInSlot(slot)) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInsert.test(stack) && delegate.isItemValid(slot, stack);
    }
}
