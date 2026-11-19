package io.github.cakeysama.ultraloot.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A selectable loot source for the Loot Generator. Defined by data packs in
 * {@code data/<namespace>/ultraloot/loot_profiles/<path>.json}, or auto-discovered from {@code chests/*} tables.
 *
 * @param table            loot table to roll
 * @param context          how the table is executed
 * @param category         free-form grouping shown in the GUI (dungeon, nether, end, fishing, ...)
 * @param icon             item shown in the GUI
 * @param minTier          lowest machine tier (1-4) allowed to use the profile
 * @param energyMultiplier multiplier on E_base for this profile
 */
public record LootProfile(ResourceLocation id, ResourceKey<LootTable> table, LootContextType context, String category,
                          Item icon, int minTier, double energyMultiplier, boolean autoDiscovered) {

    /** JSON format (the id comes from the file path). */
    public record Definition(ResourceLocation table, LootContextType context, String category, Item icon, int minTier,
                             double energyMultiplier) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("loot_table").forGetter(Definition::table),
                LootContextType.CODEC.optionalFieldOf("context", LootContextType.CHEST).forGetter(Definition::context),
                Codec.STRING.optionalFieldOf("category", "custom").forGetter(Definition::category),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.CHEST).forGetter(Definition::icon),
                Codec.intRange(1, 4).optionalFieldOf("min_tier", 1).forGetter(Definition::minTier),
                Codec.doubleRange(0.1, 100.0).optionalFieldOf("energy_multiplier", 1.0).forGetter(Definition::energyMultiplier)
        ).apply(i, Definition::new));

        public LootProfile toProfile(ResourceLocation id) {
            return new LootProfile(id, ResourceKey.create(Registries.LOOT_TABLE, table), context, category, icon, minTier,
                    energyMultiplier, false);
        }
    }

    public static LootProfile autoChest(ResourceLocation table) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(table.getNamespace(), "auto/" + table.getPath());
        return new LootProfile(id, ResourceKey.create(Registries.LOOT_TABLE, table), LootContextType.CHEST, "auto",
                Items.CHEST, Balance.autoProfileMinTier, Balance.autoProfileEnergyMultiplier, true);
    }

    /** Translation key used by the GUI; falls back to a readable form of the path. */
    public String translationKey() {
        return "ultraloot.profile." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public String fallbackName() {
        String path = id.getPath();
        String last = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
        return last.isEmpty() ? path : Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }
}
