package io.github.cakeysama.ultraloot.machine;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SideConfigurationTest {
    @Test
    void lootGeneratorDefaults() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.FRONT));
        assertEquals(SideMode.ENERGY_INPUT, c.get(RelativeSide.BACK));
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.LEFT));
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.RIGHT));
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.TOP));
        assertEquals(SideMode.ENERGY_INPUT, c.get(RelativeSide.BOTTOM));
    }

    @Test
    void mobLootGeneratorDefaults() {
        SideConfiguration c = new SideConfiguration(MachineFamily.MOB_LOOT_GENERATOR);
        assertEquals(SideMode.NONE, c.get(RelativeSide.FRONT));
        assertEquals(SideMode.ENERGY_INPUT, c.get(RelativeSide.BACK));
        assertEquals(SideMode.XP_INPUT, c.get(RelativeSide.LEFT));
        assertEquals(SideMode.NONE, c.get(RelativeSide.RIGHT));
        assertEquals(SideMode.XP_INPUT, c.get(RelativeSide.TOP));
        assertEquals(SideMode.ITEM_OUTPUT, c.get(RelativeSide.BOTTOM));
    }

    @ParameterizedTest
    @EnumSource(MachineFamily.class)
    void defaultsOnlyUseAllowedModes(MachineFamily family) {
        SideConfiguration c = new SideConfiguration(family);
        for (RelativeSide side : RelativeSide.values()) assertTrue(family.allows(c.get(side)), family + " " + side);
    }

    @Test
    void setRejectsModesOfTheOtherFamily() {
        SideConfiguration loot = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        assertFalse(loot.set(RelativeSide.FRONT, SideMode.ITEM_OUTPUT));
        assertFalse(loot.set(RelativeSide.FRONT, SideMode.XP_INPUT));
        assertEquals(SideMode.ITEM_TARGET, loot.get(RelativeSide.FRONT));

        SideConfiguration mob = new SideConfiguration(MachineFamily.MOB_LOOT_GENERATOR);
        assertFalse(mob.set(RelativeSide.FRONT, SideMode.ITEM_TARGET));
        assertEquals(SideMode.NONE, mob.get(RelativeSide.FRONT));
    }

    @Test
    void setReportsChanges() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        assertTrue(c.set(RelativeSide.FRONT, SideMode.ENERGY_INPUT));
        assertEquals(SideMode.ENERGY_INPUT, c.get(RelativeSide.FRONT));
        assertFalse(c.set(RelativeSide.FRONT, SideMode.ENERGY_INPUT), "setting the same mode is not a change");
    }

    @ParameterizedTest
    @EnumSource(MachineFamily.class)
    void cycleVisitsExactlyTheAllowedModesAndWraps(MachineFamily family) {
        for (boolean forward : new boolean[]{true, false}) {
            SideConfiguration c = new SideConfiguration(family);
            SideMode start = c.get(RelativeSide.TOP);
            Set<SideMode> seen = EnumSet.noneOf(SideMode.class);
            int n = family.allowedModes().size();
            for (int i = 0; i < n; i++) {
                SideMode mode = c.cycle(RelativeSide.TOP, forward);
                assertTrue(family.allows(mode));
                seen.add(mode);
            }
            assertEquals(Set.copyOf(family.allowedModes()), seen);
            assertEquals(start, c.get(RelativeSide.TOP), "cycling " + n + " times returns to the start");
        }
    }

    @Test
    void cycleOrderAndWrapForLootGenerator() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        // ITEM_TARGET is last in the allowed list, so forward wraps to NONE.
        assertEquals(SideMode.NONE, c.cycle(RelativeSide.FRONT, true));
        assertEquals(SideMode.ENERGY_INPUT, c.cycle(RelativeSide.FRONT, true));
        assertEquals(SideMode.NONE, c.cycle(RelativeSide.FRONT, false));
        assertEquals(SideMode.ITEM_TARGET, c.cycle(RelativeSide.FRONT, false));
    }

    @Test
    void cycleOnlyTouchesOneSide() {
        SideConfiguration c = new SideConfiguration(MachineFamily.MOB_LOOT_GENERATOR);
        int[] before = c.toArray();
        c.cycle(RelativeSide.LEFT, true);
        int[] after = c.toArray();
        for (RelativeSide side : RelativeSide.values()) {
            if (side != RelativeSide.LEFT) assertEquals(before[side.ordinal()], after[side.ordinal()], side.name());
        }
    }

    @Test
    void anyFindsConfiguredModes() {
        SideConfiguration c = new SideConfiguration(MachineFamily.MOB_LOOT_GENERATOR);
        assertTrue(c.any(SideMode.ITEM_OUTPUT));
        c.set(RelativeSide.BOTTOM, SideMode.NONE);
        assertFalse(c.any(SideMode.ITEM_OUTPUT));
    }

    @Test
    void getByWorldDirectionUsesFacing() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        // Facing north: back = south is Energy Input, front = north is Item Target.
        assertEquals(SideMode.ENERGY_INPUT, c.get(Direction.NORTH, Direction.SOUTH));
        assertEquals(SideMode.ITEM_TARGET, c.get(Direction.NORTH, Direction.NORTH));
        assertEquals(SideMode.ENERGY_INPUT, c.get(Direction.EAST, Direction.WEST));
        assertEquals(SideMode.ENERGY_INPUT, c.get(Direction.EAST, Direction.DOWN));
    }

    @ParameterizedTest
    @EnumSource(MachineFamily.class)
    void arrayRoundTrip(MachineFamily family) {
        SideConfiguration c = new SideConfiguration(family);
        c.cycle(RelativeSide.FRONT, true);
        c.cycle(RelativeSide.BACK, false);
        c.cycle(RelativeSide.TOP, true);
        c.cycle(RelativeSide.TOP, true);
        SideConfiguration copy = new SideConfiguration(family);
        copy.fromArray(c.toArray());
        assertArrayEquals(c.toArray(), copy.toArray());
    }

    @Test
    void fromArrayWithWrongLengthResetsToDefaults() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        int[] defaults = c.toArray();
        c.set(RelativeSide.FRONT, SideMode.NONE);
        c.fromArray(new int[]{0, 0, 0});
        assertArrayEquals(defaults, c.toArray());
        c.set(RelativeSide.FRONT, SideMode.NONE);
        c.fromArray(new int[0]);
        assertArrayEquals(defaults, c.toArray());
    }

    @Test
    void fromArrayRejectsModesNotAllowedForTheFamily() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        int[] values = c.toArray();
        values[RelativeSide.FRONT.ordinal()] = SideMode.ITEM_OUTPUT.ordinal();
        values[RelativeSide.TOP.ordinal()] = SideMode.XP_INPUT.ordinal();
        values[RelativeSide.LEFT.ordinal()] = SideMode.NONE.ordinal();
        c.fromArray(values);
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.FRONT));
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.TOP));
        assertEquals(SideMode.NONE, c.get(RelativeSide.LEFT));
    }

    @Test
    @DisplayName("Out-of-range persisted ordinals fall back to the family default, not to NONE (regression BUG-01)")
    void fromArrayOutOfRangeOrdinalFallsBackToFamilyDefault() {
        SideConfiguration c = new SideConfiguration(MachineFamily.LOOT_GENERATOR);
        int[] values = c.toArray();
        values[RelativeSide.FRONT.ordinal()] = 99;
        values[RelativeSide.BACK.ordinal()] = -1;
        c.fromArray(values);
        assertEquals(SideMode.ITEM_TARGET, c.get(RelativeSide.FRONT));
        assertEquals(SideMode.ENERGY_INPUT, c.get(RelativeSide.BACK));
    }

    @Test
    void resetRestoresDefaults() {
        SideConfiguration c = new SideConfiguration(MachineFamily.MOB_LOOT_GENERATOR);
        int[] defaults = c.toArray();
        for (RelativeSide side : RelativeSide.values()) c.cycle(side, true);
        c.reset();
        assertArrayEquals(defaults, c.toArray());
    }
}
