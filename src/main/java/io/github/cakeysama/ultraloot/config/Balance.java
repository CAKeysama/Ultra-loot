package io.github.cakeysama.ultraloot.config;

/**
 * Every tunable gameplay number of Ultra Loot. Arrays indexed by tier are 0-based (index 0 = Tier I);
 * arrays indexed by card level are 0-based (index 0 = level I).
 * <p>
 * Fields that are not {@code final} can be overridden by the server config ({@link UltraLootConfig});
 * the defaults below are the design values and are what unit tests run against.
 */
public final class Balance {
    private Balance() {}

    // ---- Shared tier parameters -------------------------------------------------------------
    public static final double[] TIER_SPEED = {1.0, 1.5, 2.0, 3.0};
    public static final int[] TIER_ENERGY_CAPACITY = {10_000, 25_000, 50_000, 100_000};
    /** Maximum FE accepted per tick through Energy Input faces. */
    public static final int[] TIER_ENERGY_INPUT = {250, 625, 1_250, 2_500};
    public static final int UPGRADE_SLOTS = 5;

    // ---- Loot Generator ---------------------------------------------------------------------
    public static final int[] LOOT_BASE_ATTEMPTS = {1, 2, 3, 4};
    public static final int[] LOOT_BASE_RANGE = {5, 8, 12, 16};
    /** Ticks per operation at 1x speed. */
    public static int lootBaseTicks = 200;
    /**
     * FE per loot roll before modifiers, by tier. E_base (FE per operation) = this x {@link #LOOT_BASE_ATTEMPTS}.
     * Design rule (docs/BALANCE.md P2-04): never increases with tier; each tier saves 10% of the Tier I cost.
     */
    public static int[] lootEnergyPerAttempt = {800, 720, 640, 560};
    /** Ticks between inventory scans. */
    public static int scanIntervalTicks = 100;
    /** Maximum scan entries kept for the GUI list. */
    public static final int MAX_SCAN_ENTRIES = 64;
    /** Minimum ticks between scans requested from the GUI. */
    public static final int MANUAL_RESCAN_COOLDOWN = 20;

    // ---- Mob Loot Generator -----------------------------------------------------------------
    /** Independent simulated kills per operation before the Quantity Card. */
    public static final int[] MOB_BASE_KILLS = {1, 1, 2, 2};
    public static final int[] MOB_XP_CAPACITY = {1_000, 4_000, 12_000, 40_000};
    /** Maximum XP points accepted per tick through XP Input faces (capability / fluid). */
    public static final int[] MOB_XP_INPUT = {50, 150, 400, 1_000};
    public static int mobBaseTicks = 300;
    /**
     * FE per simulated kill before modifiers, by tier. E_base = this x {@link #MOB_BASE_KILLS}.
     * Same rule as {@link #lootEnergyPerAttempt}.
     */
    public static int[] mobEnergyPerKill = {1_200, 1_080, 960, 840};
    /** XP points per simulated kill when a mob has no {@code ultraloot:mob_simulation} data map entry. */
    public static int defaultXpPerKill = 8;
    public static final int MOB_OUTPUT_SLOTS = 27;
    public static final int EXPORT_INTERVAL_TICKS = 10;
    public static final int EXPORT_ITEMS_PER_FACE = 64;
    public static final int XP_ITEM_INTERVAL_TICKS = 5;
    public static final int ORB_COLLECT_INTERVAL_TICKS = 20;

    // ---- Mob Card wear (ultraloot:card_wear = simulated kills read from the card) ---------------
    /** Kills a Mob Card delivers at full quality. */
    public static int mobCardIntegrity = 2_048;
    /** Kills over which a worn card decays from full quality to fully corrupted (rotten flesh only). */
    public static int mobCardDecaySpan = 1_024;
    /** Extra FE per operation at full corruption, as a fraction of the normal cost (1.0 = twice the cost). */
    public static final double MOB_CARD_DECAY_ENERGY = 1.0;

    // ---- Card Restorer ------------------------------------------------------------------------
    public static final int RESTORER_ENERGY_CAPACITY = 50_000;
    public static final int RESTORER_ENERGY_INPUT = 1_000;
    /** Wear points removed per tick while powered. */
    public static int restorerWearPerTick = 8;
    /** FE per wear point removed, multiplied by the mob's energy multiplier. */
    public static int restorerEnergyPerWear = 40;

    // ---- Guide book ----
    /** Give every player one Ultra Loot Guide the first time they join a world (server config). */
    public static boolean giveGuideOnFirstJoin = true;

    // ---- Experience ---------------------------------------------------------------------------
    /** mB of an experience fluid (tag c:experience) per XP point. */
    public static int xpFluidRatio = 20;
    public static final int XP_BOTTLE_POINTS = 7;
    /** Total XP needed to go from level 0 to level 30. */
    public static final int XP_CAPSULE_CAPACITY = 1_395;

    // ---- Upgrade cards (index = level - 1) ----------------------------------------------------
    public static final double[] SPEED_MULTIPLIER = {1.25, 1.5, 2.0};
    public static final double[] SPEED_ENERGY = {1.15, 1.35, 1.75};
    public static final int[] QUANTITY_MULTIPLIER = {2, 3, 4};
    public static final double[] QUANTITY_ENERGY = {1.6, 2.2, 2.8};
    public static final int[] RANGE_BONUS = {8, 16, 24};
    public static final double[] RANGE_ENERGY = {1.1, 1.2, 1.3};
    /** Mob Loot Generator: XP orb collection radius granted by the Range Card. */
    public static final int[] ORB_COLLECT_RADIUS = {2, 4, 6};
    public static final double[] EFFICIENCY_ENERGY = {0.9, 0.8, 0.65};
    /** Loot Card: luck (Loot Generator) / Looting level (Mob Loot Generator) bonus. */
    public static final int[] LOOT_BONUS = {1, 2, 3};
    public static final double[] LOOT_ENERGY = {1.1, 1.25, 1.5};

    // ---- Safety floors ------------------------------------------------------------------------
    /** An operation never costs less than this fraction of E_base. */
    public static double minEnergyFraction = 0.25;
    public static final int MIN_OPERATION_TICKS = 5;
    /** Wait before retrying an operation that was blocked by full inventories. */
    public static final int BLOCKED_RETRY_TICKS = 20;
    /** Ticks between GUI sync snapshots sent to players with the menu open. */
    public static final int GUI_SYNC_INTERVAL = 5;
    /** Whether chests/* loot tables of every namespace become selectable profiles. */
    public static boolean autoDiscoverChestTables = true;
    /** Auto-discovered (usually modded) chest tables are unknown value: gate them behind Tier II and extra FE. */
    public static int autoProfileMinTier = 2;
    /** 1.67 keeps the Tier II price of 0.1.x (1.5 x 800 FE per roll) under the 0.2.0 tier costs (720 FE per roll). */
    public static double autoProfileEnergyMultiplier = 1.67;

    // ---- Dynamic FE buffer --------------------------------------------------------------------
    /**
     * Upper bound of one operation's FE cost and therefore of the dynamic buffer: the buffer grows to hold one
     * operation (pay once, on success) but never beyond this, whatever a datapack or config says.
     */
    public static final int MAX_OPERATION_ENERGY = 100_000_000;
}
