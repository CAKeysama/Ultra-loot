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
    OUTPUT_FULL(false);

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
