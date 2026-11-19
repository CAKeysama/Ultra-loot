package io.github.cakeysama.ultraloot.menu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Predicate;

/**
 * A machine slot that only shows (and can only be clicked) while its tab is open on the client. On the server
 * {@link MachineMenu#activeTab()} is -1, so every slot stays active and shift-clicks work from any tab.
 */
public class TabSlot extends SlotItemHandler {
    private final MachineMenu menu;
    private final int tab;
    private final Predicate<ItemStack> mayPlace;
    private final int maxStack;

    public TabSlot(MachineMenu menu, int tab, IItemHandler handler, int index, int x, int y, Predicate<ItemStack> mayPlace, int maxStack) {
        super(handler, index, x, y);
        this.menu = menu;
        this.tab = tab;
        this.mayPlace = mayPlace;
        this.maxStack = maxStack;
    }

    public int tab() {
        return tab;
    }

    @Override
    public boolean isActive() {
        return menu.activeTab() < 0 || menu.activeTab() == tab;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return mayPlace.test(stack);
    }

    @Override
    public int getMaxStackSize() {
        return Math.min(maxStack, super.getMaxStackSize());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(maxStack, super.getMaxStackSize(stack));
    }
}
