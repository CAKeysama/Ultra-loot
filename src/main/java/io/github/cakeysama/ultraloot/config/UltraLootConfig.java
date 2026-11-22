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
            .comment("Loot Generator FE per loot roll for tiers I-IV (E_base = this x rolls per operation). Keep it non-increasing so higher tiers are never less efficient")
            .defineList("lootGenerator.energyPerAttempt", toList(Balance.lootEnergyPerAttempt), () -> 800, UltraLootConfig::isPositiveInt);
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
            .comment("Mob Loot Generator FE per simulated kill for tiers I-IV (E_base = this x kills per operation). Keep it non-increasing")
            .defineList("mobLootGenerator.energyPerKill", toList(Balance.mobEnergyPerKill), () -> 1200, UltraLootConfig::isPositiveInt);
    private static final ModConfigSpec.IntValue DEFAULT_XP = BUILDER
            .comment("XP points per simulated kill for mobs without a data map entry")
            .defineInRange("mobLootGenerator.defaultXpPerKill", Balance.defaultXpPerKill, 0, 10_000);

    private static final ModConfigSpec.IntValue CARD_INTEGRITY = BUILDER
            .comment("Simulated kills a Mob Card delivers at full quality before it starts to decay")
            .defineInRange("mobCard.integrity", Balance.mobCardIntegrity, 1, 10_000_000);
    private static final ModConfigSpec.IntValue CARD_DECAY_SPAN = BUILDER
            .comment("Simulated kills over which a worn Mob Card decays to fully corrupted (rotten flesh only, twice the FE)")
            .defineInRange("mobCard.decaySpan", Balance.mobCardDecaySpan, 1, 10_000_000);
    private static final ModConfigSpec.IntValue RESTORER_RATE = BUILDER
            .comment("Card Restorer: wear points repaired per tick")
            .defineInRange("cardRestorer.wearPerTick", Balance.restorerWearPerTick, 1, 10_000);
    private static final ModConfigSpec.IntValue RESTORER_COST = BUILDER
            .comment("Card Restorer: FE per wear point (multiplied by the mob's energy multiplier)")
            .defineInRange("cardRestorer.energyPerWear", Balance.restorerEnergyPerWear, 0, 100_000);

    private static final ModConfigSpec.BooleanValue GUIDE_ON_JOIN = BUILDER
            .comment("Give every player one Ultra Loot Guide the first time they join the world")
            .define("guide.giveOnFirstJoin", Balance.giveGuideOnFirstJoin);

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
        Balance.lootEnergyPerAttempt = toTierArray(LOOT_ENERGY.get(), Balance.lootEnergyPerAttempt);
        Balance.scanIntervalTicks = SCAN_INTERVAL.get();
        Balance.autoDiscoverChestTables = AUTO_DISCOVER.get();
        Balance.autoProfileMinTier = AUTO_MIN_TIER.get();
        Balance.autoProfileEnergyMultiplier = AUTO_ENERGY.get();
        Balance.mobBaseTicks = MOB_BASE_TICKS.get();
        Balance.mobEnergyPerKill = toTierArray(MOB_ENERGY.get(), Balance.mobEnergyPerKill);
        Balance.mobCardIntegrity = CARD_INTEGRITY.get();
        Balance.mobCardDecaySpan = CARD_DECAY_SPAN.get();
        Balance.restorerWearPerTick = RESTORER_RATE.get();
        Balance.restorerEnergyPerWear = RESTORER_COST.get();
        Balance.giveGuideOnFirstJoin = GUIDE_ON_JOIN.get();
        Balance.defaultXpPerKill = DEFAULT_XP.get();
        Balance.xpFluidRatio = XP_FLUID_RATIO.get();
        Balance.minEnergyFraction = MIN_ENERGY_FRACTION.get();
    }
}
