package io.github.cakeysama.ultraloot.registry;

import io.github.cakeysama.ultraloot.UltraLoot;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {}

    public static final class Blocks {
        /** Blocks the Loot Generator fills in "Storage only" target mode (chests, barrels, shulker boxes...). */
        public static final TagKey<Block> STORAGE_TARGETS = tag("storage_targets");
        /** Blocks the scanner never uses, even in "Any inventory" mode (furnaces, hoppers, ...). */
        public static final TagKey<Block> SCAN_BLACKLIST = tag("scan_blacklist");

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, UltraLoot.id(path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> FILTER_RESOURCES = tag("loot_filter/resources");
        public static final TagKey<Item> FILTER_FOOD = tag("loot_filter/food");
        public static final TagKey<Item> FILTER_EQUIPMENT = tag("loot_filter/equipment");
        public static final TagKey<Item> UPGRADE_CARDS = tag("upgrade_cards");

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM, UltraLoot.id(path));
        }
    }

    public static final class Entities {
        /** Mobs that cannot be imprinted on a Mob Card (bosses, non-living entities are rejected anyway). */
        public static final TagKey<EntityType<?>> MOB_CARD_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, UltraLoot.id("mob_card_blacklist"));
    }
}
