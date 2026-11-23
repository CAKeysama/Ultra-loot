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
    RESCAN,
    /** a = 1 for "Picked only", 0 for every inventory in range (Loot Generator). */
    SET_PICKED_ONLY,
    /** Pick / unpick the scanned inventory at {@code BlockPos.of(((long) a << 32) | (b & 0xFFFFFFFFL))} (Loot Generator). */
    TOGGLE_PICKED;

    /** Splits a packed {@code BlockPos} into the two int arguments of an action. */
    public static int[] packPos(long pos) {
        return new int[] {(int) (pos >>> 32), (int) pos};
    }

    public static long unpackPos(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    public static MachineAction byOrdinal(int ordinal) {
        MachineAction[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
