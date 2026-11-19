package io.github.cakeysama.ultraloot.upgrade;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeInventoryTest {
    private final AtomicInteger changes = new AtomicInteger();
    private UpgradeInventory inventory;

    private static ItemStack card(UpgradeType type, int level, int count) {
        return new ItemStack(ModItems.card(type, level).get(), count);
    }

    @BeforeEach
    void setUp() {
        changes.set(0);
        inventory = new UpgradeInventory(changes::incrementAndGet);
    }

    @Test
    void hasOneSlotPerCategory() {
        assertEquals(Balance.UPGRADE_SLOTS, inventory.getSlots());
        assertEquals(UpgradeType.values().length, inventory.getSlots());
    }

    @Test
    void eachSlotOnlyAcceptsItsCategory() {
        for (UpgradeType type : UpgradeType.values()) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                for (int level = 1; level <= 3; level++) {
                    assertEquals(slot == type.slot(), inventory.isItemValid(slot, card(type, level, 1)), type + " L" + level + " in slot " + slot);
                }
            }
        }
    }

    @Test
    void rejectsNonCards() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            assertFalse(inventory.isItemValid(slot, new ItemStack(Items.STONE)));
            assertFalse(inventory.isItemValid(slot, new ItemStack(ModItems.BLANK_CARD.get())));
        }
        ItemStack stone = new ItemStack(Items.STONE, 5);
        assertEquals(5, inventory.insertItem(0, stone, false).getCount());
    }

    @Test
    void insertOfWrongCategoryIsRejected() {
        ItemStack rest = inventory.insertItem(UpgradeType.SPEED.slot(), card(UpgradeType.QUANTITY, 1, 1), false);
        assertEquals(1, rest.getCount());
        assertTrue(inventory.getStackInSlot(UpgradeType.SPEED.slot()).isEmpty());
    }

    @Test
    void slotLimitIsOneCard() {
        ItemStack rest = inventory.insertItem(UpgradeType.SPEED.slot(), card(UpgradeType.SPEED, 2, 16), false);
        assertEquals(15, rest.getCount());
        assertEquals(1, inventory.getStackInSlot(UpgradeType.SPEED.slot()).getCount());
        // A second card of the same category (any level) cannot be added.
        assertEquals(1, inventory.insertItem(UpgradeType.SPEED.slot(), card(UpgradeType.SPEED, 2, 1), false).getCount());
        assertEquals(1, inventory.insertItem(UpgradeType.SPEED.slot(), card(UpgradeType.SPEED, 3, 1), false).getCount());
        assertEquals(1, inventory.getSlotLimit(0));
    }

    @Test
    void levelsReflectInstalledCards() {
        assertEquals(UpgradeLevels.NONE, inventory.levels());
        inventory.insertItem(UpgradeType.SPEED.slot(), card(UpgradeType.SPEED, 3, 1), false);
        inventory.insertItem(UpgradeType.RANGE.slot(), card(UpgradeType.RANGE, 1, 1), false);
        inventory.insertItem(UpgradeType.LOOT.slot(), card(UpgradeType.LOOT, 2, 1), false);
        assertEquals(UpgradeLevels.of(3, 0, 1, 0, 2), inventory.levels());
        assertEquals(3, inventory.level(UpgradeType.SPEED));
        assertEquals(0, inventory.level(UpgradeType.QUANTITY));
        assertTrue(changes.get() >= 3, "every insertion notifies the machine");

        inventory.extractItem(UpgradeType.SPEED.slot(), 1, false);
        assertEquals(UpgradeLevels.of(0, 0, 1, 0, 2), inventory.levels());
    }

    @Test
    void levelsIgnoreCardForcedIntoTheWrongSlot() {
        inventory.setStackInSlot(UpgradeType.SPEED.slot(), card(UpgradeType.QUANTITY, 3, 1));
        assertEquals(UpgradeLevels.NONE, inventory.levels());
        assertEquals(0, inventory.level(UpgradeType.QUANTITY));
    }

    @Test
    void nbtRoundTrip() {
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        inventory.insertItem(UpgradeType.EFFICIENCY.slot(), card(UpgradeType.EFFICIENCY, 2, 1), false);
        inventory.insertItem(UpgradeType.QUANTITY.slot(), card(UpgradeType.QUANTITY, 3, 1), false);
        UpgradeInventory copy = new UpgradeInventory(() -> {});
        copy.deserializeNBT(registries, inventory.serializeNBT(registries));
        assertEquals(inventory.levels(), copy.levels());
    }
}
