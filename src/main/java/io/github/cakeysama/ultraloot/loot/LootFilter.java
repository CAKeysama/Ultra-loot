package io.github.cakeysama.ultraloot.loot;

import io.github.cakeysama.ultraloot.registry.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Output filter unlocked by an installed Loot Card. Filters only remove items from what the loot table rolled;
 * they never add or guarantee anything. Removed items are destroyed before they exist (never dropped).
 */
public enum LootFilter {
    ALL,
    /** Ingots, gems, nuggets, dusts, raw materials, ores... ({@code #ultraloot:loot_filter/resources}). */
    RESOURCES,
    /** Anything edible or in {@code #ultraloot:loot_filter/food}. */
    FOOD,
    /** Damageable items (tools, weapons, armour) or {@code #ultraloot:loot_filter/equipment}. */
    EQUIPMENT;

    public boolean test(ItemStack stack) {
        return switch (this) {
            case ALL -> true;
            case RESOURCES -> stack.is(ModTags.Items.FILTER_RESOURCES);
            case FOOD -> stack.has(DataComponents.FOOD) || stack.is(ModTags.Items.FILTER_FOOD);
            case EQUIPMENT -> stack.isDamageableItem() || stack.is(ModTags.Items.FILTER_EQUIPMENT);
        };
    }

    public String translationKey() {
        return "ultraloot.filter." + name().toLowerCase(Locale.ROOT);
    }

    public static LootFilter byOrdinal(int ordinal) {
        LootFilter[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ALL;
    }
}
