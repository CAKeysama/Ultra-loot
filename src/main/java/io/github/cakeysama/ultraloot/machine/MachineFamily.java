package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.config.Balance;

import java.util.List;

/** The two independent machine families and their family-specific rules. */
public enum MachineFamily {
    LOOT_GENERATOR("loot_generator",
            List.of(SideMode.NONE, SideMode.ENERGY_INPUT, SideMode.ITEM_TARGET),
            // FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
            new SideMode[]{SideMode.ITEM_TARGET, SideMode.ENERGY_INPUT, SideMode.ITEM_TARGET,
                    SideMode.ITEM_TARGET, SideMode.ITEM_TARGET, SideMode.ENERGY_INPUT}),
    MOB_LOOT_GENERATOR("mob_loot_generator",
            List.of(SideMode.NONE, SideMode.ENERGY_INPUT, SideMode.XP_INPUT, SideMode.ITEM_OUTPUT),
            new SideMode[]{SideMode.NONE, SideMode.ENERGY_INPUT, SideMode.XP_INPUT,
                    SideMode.NONE, SideMode.XP_INPUT, SideMode.ITEM_OUTPUT});

    private final String id;
    private final List<SideMode> allowedModes;
    private final SideMode[] defaultSides;

    MachineFamily(String id, List<SideMode> allowedModes, SideMode[] defaultSides) {
        this.id = id;
        this.allowedModes = allowedModes;
        this.defaultSides = defaultSides;
    }

    /** Registry path prefix, e.g. {@code loot_generator} for {@code loot_generator_t1}. */
    public String id() {
        return id;
    }

    public String blockId(MachineTier tier) {
        return id + "_" + tier.idSuffix();
    }

    public List<SideMode> allowedModes() {
        return allowedModes;
    }

    public boolean allows(SideMode mode) {
        return allowedModes.contains(mode);
    }

    public SideMode defaultMode(RelativeSide side) {
        return defaultSides[side.ordinal()];
    }

    public int baseTicks() {
        return this == LOOT_GENERATOR ? Balance.lootBaseTicks : Balance.mobBaseTicks;
    }

    /**
     * E_base: FE per operation before card and profile/mob multipliers = FE per attempt x base attempts. The config
     * does not bound FE per attempt, so the product saturates at {@link Integer#MAX_VALUE} instead of overflowing
     * (the machine then reports ENERGY_BUFFER_TOO_SMALL rather than running for a negative cost).
     */
    public int baseEnergyPerOperation(MachineTier tier) {
        return (int) Math.min(Integer.MAX_VALUE, (long) baseEnergyPerAttempt(tier) * baseAttempts(tier));
    }

    /** FE per loot roll (Loot Generator) or simulated kill (Mob Loot Generator) before multipliers. */
    public int baseEnergyPerAttempt(MachineTier tier) {
        int[] table = this == LOOT_GENERATOR ? Balance.lootEnergyPerAttempt : Balance.mobEnergyPerKill;
        return table[tier.index()];
    }

    /** Independent loot rolls (Loot Generator) or simulated kills (Mob Loot Generator) per operation before cards. */
    public int baseAttempts(MachineTier tier) {
        int[] table = this == LOOT_GENERATOR ? Balance.LOOT_BASE_ATTEMPTS : Balance.MOB_BASE_KILLS;
        return table[tier.index()];
    }
}
