package io.github.cakeysama.ultraloot.machine;

import java.util.Locale;

/** Operational state of the Card Restorer shown in its GUI. Only {@link #RUNNING} means wear is being removed. */
public enum CardRestorerStatus {
    /** No card in the input slot. */
    IDLE,
    /** A worn card is waiting for FE. */
    NO_ENERGY,
    /** Removing wear this tick. */
    RUNNING,
    /** The card is fully restored but the output slot is occupied. */
    OUTPUT_FULL;

    public boolean isWorking() {
        return this == RUNNING;
    }

    public String translationKey() {
        return "ultraloot.restorer.status." + name().toLowerCase(Locale.ROOT);
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }

    public static CardRestorerStatus byOrdinal(int ordinal) {
        CardRestorerStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }
}
