package io.github.cakeysama.ultraloot.machine;

/** Operational state shown in the GUI. Only {@link #RUNNING} means progress is advancing. */
public enum MachineStatus {
    RUNNING(true),
    NO_ENERGY(false),
    NO_PROFILE(false),
    INVALID_PROFILE(false),
    PROFILE_LOCKED(false),
    NO_TARGETS(false),
    TARGETS_FULL(false),
    NO_MOB_CARD(false),
    INVALID_MOB(false),
    NO_XP(false),
    OUTPUT_FULL(false),
    /** One operation costs more FE than the tier's buffer can hold: remove cards or use a higher tier. */
    ENERGY_BUFFER_TOO_SMALL(false),
    /** One operation costs more XP than the tier's tank can hold: remove the Quantity Card or use a higher tier. */
    XP_TANK_TOO_SMALL(false),
    /** The mob has no native loot table and no fallback, or a configured supplemental/fallback table is missing. */
    NO_LOOT_SOURCE(false),
    /** Loot Generator in "Picked only" mode: there are inventories in range, but none of them is picked. */
    NO_PICKED_TARGETS(false);

    private final boolean working;

    MachineStatus(boolean working) {
        this.working = working;
    }

    public boolean isWorking() {
        return working;
    }

    public String translationKey() {
        return "ultraloot.status." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static MachineStatus byOrdinal(int ordinal) {
        MachineStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NO_ENERGY;
    }
}
