package io.github.cakeysama.ultraloot.upgrade;

import java.util.Locale;

/** Upgrade card categories. The ordinal is also the machine upgrade slot index that accepts the category. */
public enum UpgradeType {
    SPEED, QUANTITY, RANGE, EFFICIENCY, LOOT;

    public static final int MAX_LEVEL = 3;

    public int slot() {
        return ordinal();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Registry path of the card of the given level (1-3), e.g. {@code speed_card_2}. */
    public String cardId(int level) {
        return id() + "_card_" + level;
    }

    public String translationKey() {
        return "ultraloot.upgrade." + id();
    }

    public static UpgradeType bySlot(int slot) {
        return values()[slot];
    }
}
