package io.github.cakeysama.ultraloot.upgrade;

import java.util.Arrays;

/** Installed card level per {@link UpgradeType} (0 = no card, 1-3 = card level). Immutable. */
public final class UpgradeLevels {
    public static final UpgradeLevels NONE = new UpgradeLevels(new int[UpgradeType.values().length]);

    private final int[] levels;

    private UpgradeLevels(int[] levels) {
        this.levels = levels;
    }

    public static UpgradeLevels of(int speed, int quantity, int range, int efficiency, int loot) {
        int[] levels = {speed, quantity, range, efficiency, loot};
        for (int level : levels) {
            if (level < 0 || level > UpgradeType.MAX_LEVEL) throw new IllegalArgumentException("Card level out of range: " + level);
        }
        return new UpgradeLevels(levels);
    }

    public UpgradeLevels with(UpgradeType type, int level) {
        if (level < 0 || level > UpgradeType.MAX_LEVEL) throw new IllegalArgumentException("Card level out of range: " + level);
        int[] copy = levels.clone();
        copy[type.ordinal()] = level;
        return new UpgradeLevels(copy);
    }

    public int get(UpgradeType type) {
        return levels[type.ordinal()];
    }

    public boolean has(UpgradeType type) {
        return get(type) > 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UpgradeLevels other && Arrays.equals(levels, other.levels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(levels);
    }

    @Override
    public String toString() {
        return "UpgradeLevels" + Arrays.toString(levels);
    }
}
