package io.github.cakeysama.ultraloot.menu;

/** Configuration actions a client may request through {@code MachineActionPayload}. Every one is validated server-side. */
public enum MachineAction {
    /** a = relative side ordinal, b = +1 (next) / -1 (previous). */
    CYCLE_SIDE,
    /** a = relative side ordinal, b = side mode ordinal. */
    SET_SIDE,
    /** a = loot filter ordinal (requires a Loot Card unless ALL). */
    SET_FILTER,
    /** text = loot profile id, empty to clear (Loot Generator). */
    SET_PROFILE,
    /** a = target mode ordinal (Loot Generator). */
    SET_TARGET_MODE,
    /** Rescan nearby inventories now (Loot Generator). */
    RESCAN;

    public static MachineAction byOrdinal(int ordinal) {
        MachineAction[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
