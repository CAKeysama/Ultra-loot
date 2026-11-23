package io.github.cakeysama.ultraloot.menu;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MachineActionTest {
    @Test
    void blockPositionsSurviveTheTwoIntPacking() {
        BlockPos[] positions = {BlockPos.ZERO, new BlockPos(-30_000_000, -64, 29_999_999), new BlockPos(123, 319, -456),
                new BlockPos(-1, -1, -1)};
        for (BlockPos pos : positions) {
            int[] parts = MachineAction.packPos(pos.asLong());
            assertEquals(pos, BlockPos.of(MachineAction.unpackPos(parts[0], parts[1])), pos.toShortString());
        }
    }

    @Test
    void newActionsKeepTheExistingOrdinals() {
        // Ordinals travel in MachineActionPayload: appending is fine, reordering would break older clients.
        assertEquals(MachineAction.RESCAN, MachineAction.byOrdinal(5));
        assertEquals(MachineAction.SET_PICKED_ONLY, MachineAction.byOrdinal(6));
        assertEquals(MachineAction.TOGGLE_PICKED, MachineAction.byOrdinal(7));
        assertNull(MachineAction.byOrdinal(8));
    }
}
