package io.github.cakeysama.ultraloot.config;

import io.github.cakeysama.ultraloot.UltraLoot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

/** Server config: overrides the non-final values of {@link Balance}. */
@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public final class UltraLootConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue LOOT_BASE_TICKS = BUILDER
            .comment("Ticks per Loot Generator operation at 1x speed")
            .defineInRange("lootGenerator.baseTicks", Balance.lootBaseTicks, 20, 72_000);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> LOOT_ENERGY = BUILDER
            .comment("Loot Generator FE per operation (E_base) for tiers I-IV")
            .defineList("lootGenerator.energyPerOperation", toList(Balance.lootEnergyPerOperation), () -> 1000, UltraLootConfig::isPositiveInt);
    private static final ModConfigSpec.IntValue SCAN_INTERVAL = BUILDER
            .comment("Ticks between nearby inventory scans")
            .defineInRange("lootGenerator.scanIntervalTicks", Balance.scanIntervalTicks, 20, 6_000);
    private static final ModConfigSpec.BooleanValue AUTO_DISCOVER = BUILDER
            .comment("Expose every chests/* loot table (any mod) as a selectable loot profile")
            .define("lootGenerator.autoDiscoverChestTables", Balance.autoDiscoverChestTables);
    private static final ModConfigSpec.IntValue AUTO_MIN_TIER = BUILDER
            .comment("Minimum machine tier for auto-discovered chest profiles")
            .defineInRange("lootGenerator.autoProfileMinTier", Balance.autoProfileMinTier, 1, 4);
    private static final ModConfigSpec.DoubleValue AUTO_ENERGY = BUILDER
            .comment("Energy multiplier for auto-discovered chest profiles")
            .defineInRange("lootGenerator.autoProfileEnergyMultiplier", Balance.autoProfileEnergyMultiplier, 0.1, 100.0);

    private static final ModConfigSpec.IntValue MOB_BASE_TICKS = BUILDER
            .comment("Ticks per Mob Loot Generator operation at 1x speed")
            .defineInRange("mobLootGenerator.baseTicks", Balance.mobBaseTicks, 20, 72_000);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> MOB_ENERGY = BUILDER
            .comment("Mob Loot Generator FE per operation (E_base) for tiers I-IV")
            .defineList("mobLootGenerator.energyPerOperation", toList(Balance.mobEnergyPerOperation), () -> 1000, UltraLootConfig::isPositiveInt);
    private static final ModConfigSpec.IntValue DEFAULT_XP = BUILDER
            .comment("XP points per simulated kill for mobs without a data map entry")
            .defineInRange("mobLootGenerator.defaultXpPerKill", Balance.defaultXpPerKill, 0, 10_000);

    private static final ModConfigSpec.IntValue XP_FLUID_RATIO = BUILDER
            .comment("mB of experience fluid (tag c:experience) per XP point")
            .defineInRange("experience.fluidRatio", Balance.xpFluidRatio, 1, 1_000);
    private static final ModConfigSpec.DoubleValue MIN_ENERGY_FRACTION = BUILDER
            .comment("Minimum fraction of E_base an operation costs regardless of upgrades")
            .defineInRange("energy.minFraction", Balance.minEnergyFraction, 0.05, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private UltraLootConfig() {}

    private static List<Integer> toList(int[] values) {
        return Arrays.stream(values).boxed().toList();
    }

    private static boolean isPositiveInt(Object o) {
        return o instanceof Integer i && i > 0;
    }

    private static int[] toTierArray(List<? extends Integer> values, int[] fallback) {
        if (values.size() != 4) {
            UltraLoot.LOGGER.warn("Ultra Loot config lists need 4 entries (one per tier); keeping defaults");
            return fallback;
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        apply(event.getConfig());
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        apply(event.getConfig());
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() != SPEC) return;
        Balance.lootBaseTicks = LOOT_BASE_TICKS.get();
        Balance.lootEnergyPerOperation = toTierArray(LOOT_ENERGY.get(), Balance.lootEnergyPerOperation);
        Balance.scanIntervalTicks = SCAN_INTERVAL.get();
        Balance.autoDiscoverChestTables = AUTO_DISCOVER.get();
        Balance.autoProfileMinTier = AUTO_MIN_TIER.get();
        Balance.autoProfileEnergyMultiplier = AUTO_ENERGY.get();
        Balance.mobBaseTicks = MOB_BASE_TICKS.get();
        Balance.mobEnergyPerOperation = toTierArray(MOB_ENERGY.get(), Balance.mobEnergyPerOperation);
        Balance.defaultXpPerKill = DEFAULT_XP.get();
        Balance.xpFluidRatio = XP_FLUID_RATIO.get();
        Balance.minEnergyFraction = MIN_ENERGY_FRACTION.get();
    }
}
