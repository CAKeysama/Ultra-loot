package io.github.cakeysama.ultraloot.machine;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativeSideTest {
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    void mappingIsABijectionAndRoundTrips(Direction facing) {
        Set<Direction> seen = EnumSet.noneOf(Direction.class);
        for (RelativeSide side : RelativeSide.values()) {
            Direction world = side.toDirection(facing);
            seen.add(world);
            assertEquals(side, RelativeSide.fromDirection(facing, world), facing + " " + side);
        }
        assertEquals(6, seen.size(), "every relative side maps to a distinct world direction");
        for (Direction world : Direction.values()) {
            assertEquals(world, RelativeSide.fromDirection(facing, world).toDirection(facing));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    void fixedSides(Direction facing) {
        assertEquals(facing, RelativeSide.FRONT.toDirection(facing));
        assertEquals(facing.getOpposite(), RelativeSide.BACK.toDirection(facing));
        assertEquals(Direction.UP, RelativeSide.TOP.toDirection(facing));
        assertEquals(Direction.DOWN, RelativeSide.BOTTOM.toDirection(facing));
        assertEquals(RelativeSide.LEFT.toDirection(facing).getOpposite(), RelativeSide.RIGHT.toDirection(facing));
    }

    /** Left/right are as seen by a player looking at the machine's front. */
    @Test
    void leftAndRightForEveryFacing() {
        assertEquals(Direction.EAST, RelativeSide.LEFT.toDirection(Direction.NORTH));
        assertEquals(Direction.WEST, RelativeSide.RIGHT.toDirection(Direction.NORTH));
        assertEquals(Direction.WEST, RelativeSide.LEFT.toDirection(Direction.SOUTH));
        assertEquals(Direction.EAST, RelativeSide.RIGHT.toDirection(Direction.SOUTH));
        assertEquals(Direction.SOUTH, RelativeSide.LEFT.toDirection(Direction.EAST));
        assertEquals(Direction.NORTH, RelativeSide.RIGHT.toDirection(Direction.EAST));
        assertEquals(Direction.NORTH, RelativeSide.LEFT.toDirection(Direction.WEST));
        assertEquals(Direction.SOUTH, RelativeSide.RIGHT.toDirection(Direction.WEST));
    }

    @Test
    void rotatingTheMachineRotatesItsSides() {
        for (RelativeSide side : new RelativeSide[]{RelativeSide.FRONT, RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT}) {
            Direction facing = Direction.NORTH;
            for (int i = 0; i < 4; i++) {
                assertEquals(side.toDirection(facing).getClockWise(), side.toDirection(facing.getClockWise()), side + " " + facing);
                facing = facing.getClockWise();
            }
        }
    }

    @Test
    void byOrdinalFallsBackToFront() {
        assertEquals(RelativeSide.BOTTOM, RelativeSide.byOrdinal(5));
        assertEquals(RelativeSide.FRONT, RelativeSide.byOrdinal(6));
        assertEquals(RelativeSide.FRONT, RelativeSide.byOrdinal(-1));
    }
}
