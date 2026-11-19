package io.github.cakeysama.ultraloot.machine;

import net.minecraft.core.Direction;

/**
 * A machine face relative to its facing. Side configuration is stored per relative side so that it keeps its
 * meaning when the machine is placed facing another direction.
 */
public enum RelativeSide {
    FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

    public String translationKey() {
        return "ultraloot.side." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** World direction of this side for a machine whose front points to {@code facing} (horizontal). */
    public Direction toDirection(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            // Looking at the machine's front, its left side is the facing rotated clockwise.
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    public static RelativeSide fromDirection(Direction facing, Direction world) {
        for (RelativeSide side : values()) {
            if (side.toDirection(facing) == world) return side;
        }
        throw new IllegalArgumentException("Unreachable direction " + world);
    }

    public static RelativeSide byOrdinal(int ordinal) {
        RelativeSide[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FRONT;
    }
}
