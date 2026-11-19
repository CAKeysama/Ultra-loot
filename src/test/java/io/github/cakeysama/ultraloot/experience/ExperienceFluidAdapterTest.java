package io.github.cakeysama.ultraloot.experience;

import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Neither vanilla nor NeoForge tags any fluid as {@code c:experience} (NeoForge ships the tag empty), so there is no
 * real experience fluid to fill with. Rejection is tested with water and lava; the mB/point conversion is tested with
 * {@link AcceptAll}, which only replaces the tag check and keeps the adapter's own {@code fill} logic.
 */
class ExperienceFluidAdapterTest {
    private ExperienceTank tank;
    private ExperienceFluidAdapter adapter;
    private AcceptAll acceptAll;

    /** Same adapter, but treats any fluid as an experience fluid. */
    private static final class AcceptAll extends ExperienceFluidAdapter {
        AcceptAll(IExperienceStorage storage) {
            super(storage);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        tank = new ExperienceTank(1_000, 1_000, () -> {});
        adapter = new ExperienceFluidAdapter(tank);
        acceptAll = new AcceptAll(tank);
    }

    @Test
    void noVanillaFluidIsTaggedAsExperience() {
        assertFalse(new FluidStack(Fluids.WATER, 1000).is(Tags.Fluids.EXPERIENCE));
        assertFalse(new FluidStack(Fluids.LAVA, 1000).is(Tags.Fluids.EXPERIENCE));
    }

    @Test
    void rejectsWaterAndLava() {
        assertFalse(adapter.isFluidValid(0, new FluidStack(Fluids.WATER, 1000)));
        assertEquals(0, adapter.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
        assertEquals(0, adapter.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
        assertEquals(0, tank.getExperience());
    }

    @Test
    void rejectsEmptyStack() {
        assertEquals(0, acceptAll.fill(FluidStack.EMPTY, FluidAction.EXECUTE));
    }

    @Test
    void neverExposesOrDrainsFluid() {
        tank.setExperience(500);
        assertEquals(1, adapter.getTanks());
        assertTrue(adapter.getFluidInTank(0).isEmpty());
        assertTrue(adapter.drain(1000, FluidAction.EXECUTE).isEmpty());
        assertTrue(adapter.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).isEmpty());
        assertEquals(500, tank.getExperience());
    }

    @Test
    void tankCapacityReportsFreeSpaceInMillibuckets() {
        tank.setExperience(900);
        assertEquals(100 * Balance.xpFluidRatio, adapter.getTankCapacity(0));
    }

    @Test
    void convertsWholePointsOnly() {
        int ratio = Balance.xpFluidRatio;
        // 50 points + a remainder smaller than one point: the remainder is not taken.
        int filled = acceptAll.fill(new FluidStack(Fluids.WATER, 50 * ratio + ratio - 1), FluidAction.EXECUTE);
        assertEquals(50 * ratio, filled);
        assertEquals(50, tank.getExperience());
    }

    @Test
    void lessThanOnePointIsRejected() {
        assertEquals(0, acceptAll.fill(new FluidStack(Fluids.WATER, Balance.xpFluidRatio - 1), FluidAction.EXECUTE));
        assertEquals(0, tank.getExperience());
    }

    @Test
    void simulateDoesNotChangeTank() {
        assertEquals(10 * Balance.xpFluidRatio, acceptAll.fill(new FluidStack(Fluids.WATER, 10 * Balance.xpFluidRatio), FluidAction.SIMULATE));
        assertEquals(0, tank.getExperience());
    }

    @Test
    void fillStopsAtTankSpace() {
        tank.setExperience(997);
        assertEquals(3 * Balance.xpFluidRatio, acceptAll.fill(new FluidStack(Fluids.WATER, 100_000), FluidAction.EXECUTE));
        assertEquals(1_000, tank.getExperience());
    }

    @Test
    void fillRespectsTankRateLimit() {
        ExperienceTank limited = new ExperienceTank(1_000, 50, () -> {});
        AcceptAll limitedAdapter = new AcceptAll(limited);
        assertEquals(50 * Balance.xpFluidRatio, limitedAdapter.fill(new FluidStack(Fluids.WATER, 100_000), FluidAction.EXECUTE));
        assertEquals(50, limited.getExperience());
    }
}
