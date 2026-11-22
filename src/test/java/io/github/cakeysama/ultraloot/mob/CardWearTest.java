package io.github.cakeysama.ultraloot.mob;

import io.github.cakeysama.ultraloot.config.Balance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mob Card wear math (owner decision: cards deteriorate, then yield Rotten Flesh at higher FE, repairable). */
class CardWearTest {
    private static final int INTEGRITY = Balance.mobCardIntegrity;
    private static final int SPAN = Balance.mobCardDecaySpan;

    @Test
    void intactCardHasNoPenalty() {
        assertEquals(0.0, CardWear.decay(0));
        assertEquals(0.0, CardWear.decay(INTEGRITY));
        assertEquals(1.0, CardWear.energyFactor(INTEGRITY));
        assertEquals(1.0, CardWear.quality(0));
    }

    @Test
    void decayIsLinearAfterIntegrity() {
        assertEquals(0.5, CardWear.decay(INTEGRITY + SPAN / 2), 1e-9);
        assertEquals(1.5, CardWear.energyFactor(INTEGRITY + SPAN / 2), 1e-9);
        assertEquals(1.0 / SPAN, CardWear.decay(INTEGRITY + 1), 1e-12);
    }

    @Test
    void corruptedCardSaturates() {
        int max = CardWear.maxWear();
        assertEquals(INTEGRITY + SPAN, max);
        assertEquals(1.0, CardWear.decay(max));
        assertEquals(1.0, CardWear.decay(max * 10));
        assertEquals(1.0 + Balance.MOB_CARD_DECAY_ENERGY, CardWear.energyFactor(max));
        assertEquals(0.0, CardWear.quality(max));
    }

    @Test
    void addWearSaturatesWithoutOverflow() {
        assertEquals(5, CardWear.addWear(0, 5));
        assertEquals(CardWear.maxWear(), CardWear.addWear(CardWear.maxWear() - 1, 50));
        assertEquals(CardWear.maxWear(), CardWear.addWear(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(0, CardWear.addWear(-10, -3), "negative inputs are treated as zero");
    }

    @Test
    void penaltiesNeverDecreaseWithWear() {
        double lastDecay = -1;
        double lastFactor = 0;
        for (int wear = 0; wear <= CardWear.maxWear() + 10; wear += 7) {
            double decay = CardWear.decay(wear);
            double factor = CardWear.energyFactor(wear);
            assertTrue(decay >= lastDecay && factor >= lastFactor, "wear " + wear);
            lastDecay = decay;
            lastFactor = factor;
        }
    }
}
