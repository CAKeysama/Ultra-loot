package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.config.Balance;

/** The four machine tiers. All numbers come from {@link Balance}. */
public enum MachineTier {
    T1, T2, T3, T4;

    public int index() {
        return ordinal();
    }

    /** 1-based tier number shown to players. */
    public int number() {
        return ordinal() + 1;
    }

    public String roman() {
        return switch (this) {
            case T1 -> "I";
            case T2 -> "II";
            case T3 -> "III";
            case T4 -> "IV";
        };
    }

    public String idSuffix() {
        return "t" + number();
    }

    public double speed() {
        return Balance.TIER_SPEED[index()];
    }

    public int energyCapacity() {
        return Balance.TIER_ENERGY_CAPACITY[index()];
    }

    public int energyInput() {
        return Balance.TIER_ENERGY_INPUT[index()];
    }

    public static MachineTier byNumber(int number) {
        return values()[Math.clamp(number - 1, 0, values().length - 1)];
    }
}
