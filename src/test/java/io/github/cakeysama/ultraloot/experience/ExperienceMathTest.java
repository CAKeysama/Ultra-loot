package io.github.cakeysama.ultraloot.experience;

import io.github.cakeysama.ultraloot.config.Balance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExperienceMathTest {
    /** Vanilla {@code Player#getXpNeededForNextLevel}, used as an independent oracle. */
    private static int vanillaNeededForNext(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    @Test
    void knownVanillaTotals() {
        assertEquals(0, ExperienceMath.totalForLevel(0));
        assertEquals(7, ExperienceMath.totalForLevel(1));
        assertEquals(315, ExperienceMath.totalForLevel(15));
        assertEquals(352, ExperienceMath.totalForLevel(16));
        assertEquals(394, ExperienceMath.totalForLevel(17));
        assertEquals(1395, ExperienceMath.totalForLevel(30));
        assertEquals(1507, ExperienceMath.totalForLevel(31));
        assertEquals(1628, ExperienceMath.totalForLevel(32));
    }

    @Test
    void matchesSumOfVanillaLevelCosts() {
        int total = 0;
        for (int level = 0; level <= 200; level++) {
            assertEquals(total, ExperienceMath.totalForLevel(level), "total XP for level " + level);
            total += vanillaNeededForNext(level);
        }
    }

    @Test
    void negativeLevelIsZero() {
        assertEquals(0, ExperienceMath.totalForLevel(-5));
    }

    @Test
    void capsuleHoldsExactlyThirtyLevels() {
        assertEquals(ExperienceMath.totalForLevel(30), Balance.XP_CAPSULE_CAPACITY);
    }
}
