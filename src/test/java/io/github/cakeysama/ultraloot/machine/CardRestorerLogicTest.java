package io.github.cakeysama.ultraloot.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Card Restorer repair math: never pays without repairing, never repairs without paying, no overflow. */
class CardRestorerLogicTest {
    @Test
    void fullRateWhenEnergyIsPlenty() {
        CardRestorerLogic.Step step = CardRestorerLogic.repair(1_000, 50_000, 40, 8);
        assertEquals(8, step.repaired());
        assertEquals(320, step.energy());
    }

    @Test
    void zeroEnergyRepairsNothing() {
        assertEquals(CardRestorerLogic.Step.NONE, CardRestorerLogic.repair(500, 0, 40, 8));
        assertEquals(CardRestorerLogic.Step.NONE, CardRestorerLogic.repair(500, 39, 40, 8));
    }

    @Test
    void partialEnergyRepairsOnlyWhatIsPaid() {
        CardRestorerLogic.Step step = CardRestorerLogic.repair(500, 130, 40, 8);
        assertEquals(3, step.repaired());
        assertEquals(120, step.energy());
        assertTrue(step.energy() <= 130);
    }

    @Test
    void exactFillSpendsEverything() {
        CardRestorerLogic.Step step = CardRestorerLogic.repair(500, 320, 40, 8);
        assertEquals(8, step.repaired());
        assertEquals(320, step.energy());
    }

    @Test
    void wearBelowRateFinishesTheCard() {
        CardRestorerLogic.Step step = CardRestorerLogic.repair(3, 50_000, 40, 8);
        assertEquals(3, step.repaired());
        assertEquals(120, step.energy());
    }

    @Test
    void noWearOrNoRateDoesNothing() {
        assertEquals(CardRestorerLogic.Step.NONE, CardRestorerLogic.repair(0, 50_000, 40, 8));
        assertEquals(CardRestorerLogic.Step.NONE, CardRestorerLogic.repair(-5, 50_000, 40, 8));
        assertEquals(CardRestorerLogic.Step.NONE, CardRestorerLogic.repair(100, 50_000, 40, 0));
    }

    @Test
    void largeValuesDoNotOverflow() {
        CardRestorerLogic.Step step = CardRestorerLogic.repair(Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE / 2, Integer.MAX_VALUE);
        assertEquals(2, step.repaired());
        assertEquals((Long.MAX_VALUE / 2) * 2, step.energy());
        assertTrue(step.energy() > 0);

        CardRestorerLogic.Step huge = CardRestorerLogic.repair(Integer.MAX_VALUE, Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, huge.repaired());
        assertEquals(Integer.MAX_VALUE, huge.energy());

        assertEquals(CardRestorerLogic.Step.NONE, CardRestorerLogic.repair(10, Integer.MAX_VALUE, Long.MAX_VALUE, 8));
    }

    @Test
    void costBelowOneCountsAsOne() {
        CardRestorerLogic.Step step = CardRestorerLogic.repair(100, 5, 0, 8);
        assertEquals(5, step.repaired());
        assertEquals(5, step.energy());
    }

    @Test
    void costPerWearRoundsUpWithMinimumOne() {
        assertEquals(40, CardRestorerLogic.costPerWear(40, 1.0));
        assertEquals(61, CardRestorerLogic.costPerWear(40, 1.51));
        assertEquals(10, CardRestorerLogic.costPerWear(40, 0.25));
        assertEquals(1, CardRestorerLogic.costPerWear(0, 2.0));
        assertEquals(1, CardRestorerLogic.costPerWear(-10, 2.0));
        assertEquals(1, CardRestorerLogic.costPerWear(Double.NaN, 1.0));
        assertEquals(Long.MAX_VALUE, CardRestorerLogic.costPerWear(1e300, 1e10));
    }

    @Test
    void finishEstimates() {
        assertEquals(0, CardRestorerLogic.ticksToFinish(0, 8));
        assertEquals(1, CardRestorerLogic.ticksToFinish(8, 8));
        assertEquals(2, CardRestorerLogic.ticksToFinish(9, 8));
        assertEquals(268_435_456, CardRestorerLogic.ticksToFinish(Integer.MAX_VALUE, 8));
        assertEquals(400, CardRestorerLogic.energyToFinish(10, 40));
        assertEquals(Long.MAX_VALUE, CardRestorerLogic.energyToFinish(10, Long.MAX_VALUE / 2));
    }
}
