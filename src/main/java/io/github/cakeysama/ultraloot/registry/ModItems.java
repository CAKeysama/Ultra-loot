package io.github.cakeysama.ultraloot.registry;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.experience.ExperienceCapsuleItem;
import io.github.cakeysama.ultraloot.machine.MachineBlock;
import io.github.cakeysama.ultraloot.machine.MachineBlockItem;
import io.github.cakeysama.ultraloot.mob.BlankMobCardItem;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.upgrade.UpgradeCardItem;
import io.github.cakeysama.ultraloot.upgrade.UpgradeType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(UltraLoot.MOD_ID);

    /** Registration order, used by the creative tab. */
    private static final List<DeferredItem<? extends Item>> ORDERED = new ArrayList<>();

    static {
        for (DeferredBlock<MachineBlock> block : ModBlocks.allMachines()) {
            ORDERED.add(REGISTER.register(block.getId().getPath(), () -> new MachineBlockItem(block.get(), new Item.Properties())));
        }
    }

    // Crafting components
    public static final DeferredItem<Item> MACHINE_FRAME = simple("machine_frame", Rarity.COMMON);
    public static final DeferredItem<Item> LOOT_CORE = simple("loot_core", Rarity.UNCOMMON);
    public static final DeferredItem<Item> SOUL_CORE = simple("soul_core", Rarity.UNCOMMON);
    public static final DeferredItem<Item> LOGIC_CIRCUIT = simple("logic_circuit", Rarity.COMMON);
    public static final DeferredItem<Item> ENHANCED_CIRCUIT = simple("enhanced_circuit", Rarity.UNCOMMON);
    public static final DeferredItem<Item> REFINED_CIRCUIT = simple("refined_circuit", Rarity.RARE);
    public static final DeferredItem<Item> STELLAR_CIRCUIT = simple("stellar_circuit", Rarity.EPIC);
    public static final DeferredItem<Item> BLANK_CARD = simple("blank_card", Rarity.COMMON);

    private static final Map<UpgradeType, List<DeferredItem<UpgradeCardItem>>> CARDS = new EnumMap<>(UpgradeType.class);

    static {
        for (UpgradeType type : UpgradeType.values()) {
            List<DeferredItem<UpgradeCardItem>> levels = new ArrayList<>();
            for (int level = 1; level <= UpgradeType.MAX_LEVEL; level++) {
                int lvl = level;
                Rarity rarity = level == 1 ? Rarity.COMMON : level == 2 ? Rarity.UNCOMMON : Rarity.RARE;
                DeferredItem<UpgradeCardItem> card = REGISTER.register(type.cardId(level),
                        () -> new UpgradeCardItem(type, lvl, new Item.Properties().stacksTo(16).rarity(rarity)));
                levels.add(card);
                ORDERED.add(card);
            }
            CARDS.put(type, List.copyOf(levels));
        }
    }

    public static final DeferredItem<BlankMobCardItem> BLANK_MOB_CARD = register("blank_mob_card",
            () -> new BlankMobCardItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<MobCardItem> MOB_CARD = register("mob_card",
            () -> new MobCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ExperienceCapsuleItem> EXPERIENCE_CAPSULE = register("experience_capsule",
            () -> new ExperienceCapsuleItem(new Item.Properties()));

    private static DeferredItem<Item> simple(String id, Rarity rarity) {
        return register(id, () -> new Item(new Item.Properties().rarity(rarity)));
    }

    private static <T extends Item> DeferredItem<T> register(String id, java.util.function.Supplier<T> factory) {
        DeferredItem<T> item = REGISTER.register(id, factory);
        ORDERED.add(item);
        return item;
    }

    public static DeferredItem<UpgradeCardItem> card(UpgradeType type, int level) {
        return CARDS.get(type).get(level - 1);
    }

    public static List<DeferredItem<? extends Item>> ordered() {
        return List.copyOf(ORDERED);
    }

    private ModItems() {}
}
