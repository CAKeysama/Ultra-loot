package io.github.cakeysama.ultraloot.upgrade;

import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The five upgrade slots of a machine. Slot {@code i} only accepts one card of {@link UpgradeType#bySlot(int)}, so two
 * cards of the same category can never coexist. Never exposed to automation.
 */
public class UpgradeInventory extends ItemStackHandler {
    private final Runnable onChanged;

    public UpgradeInventory(Runnable onChanged) {
        super(Balance.UPGRADE_SLOTS);
        this.onChanged = onChanged;
    }

    public static boolean isValidFor(int slot, ItemStack stack) {
        return stack.getItem() instanceof UpgradeCardItem card && card.type().slot() == slot;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValidFor(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }

    public UpgradeLevels levels() {
        UpgradeLevels levels = UpgradeLevels.NONE;
        for (UpgradeType type : UpgradeType.values()) {
            if (getStackInSlot(type.slot()).getItem() instanceof UpgradeCardItem card && card.type() == type) {
                levels = levels.with(type, card.level());
            }
        }
        return levels;
    }

    public int level(UpgradeType type) {
        return getStackInSlot(type.slot()).getItem() instanceof UpgradeCardItem card && card.type() == type ? card.level() : 0;
    }
}
