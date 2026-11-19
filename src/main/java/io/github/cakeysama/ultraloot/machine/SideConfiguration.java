package io.github.cakeysama.ultraloot.machine;

import net.minecraft.core.Direction;

import java.util.Arrays;

/**
 * Role of each of the six faces of a machine, stored per {@link RelativeSide}. Only modes allowed by the family
 * can be set; invalid persisted values fall back to the family default.
 */
public final class SideConfiguration {
    private final MachineFamily family;
    private final SideMode[] modes = new SideMode[RelativeSide.values().length];

    public SideConfiguration(MachineFamily family) {
        this.family = family;
        reset();
    }

    public void reset() {
        for (RelativeSide side : RelativeSide.values()) {
            modes[side.ordinal()] = family.defaultMode(side);
        }
    }

    public SideMode get(RelativeSide side) {
        return modes[side.ordinal()];
    }

    public SideMode get(Direction facing, Direction world) {
        return get(RelativeSide.fromDirection(facing, world));
    }

    /** @return true when the mode changed. Disallowed modes are rejected. */
    public boolean set(RelativeSide side, SideMode mode) {
        if (!family.allows(mode) || modes[side.ordinal()] == mode) return false;
        modes[side.ordinal()] = mode;
        return true;
    }

    /** Moves to the next (or previous) allowed mode for this side. */
    public SideMode cycle(RelativeSide side, boolean forward) {
        var allowed = family.allowedModes();
        int current = Math.max(0, allowed.indexOf(get(side)));
        int next = Math.floorMod(current + (forward ? 1 : -1), allowed.size());
        modes[side.ordinal()] = allowed.get(next);
        return modes[side.ordinal()];
    }

    public boolean any(SideMode mode) {
        for (SideMode m : modes) {
            if (m == mode) return true;
        }
        return false;
    }

    public int[] toArray() {
        return Arrays.stream(modes).mapToInt(Enum::ordinal).toArray();
    }

    public void fromArray(int[] values) {
        reset();
        if (values.length != modes.length) return;
        SideMode[] all = SideMode.values();
        for (int i = 0; i < values.length; i++) {
            int ordinal = values[i];
            if (ordinal >= 0 && ordinal < all.length && family.allows(all[ordinal])) modes[i] = all[ordinal];
        }
    }
}
