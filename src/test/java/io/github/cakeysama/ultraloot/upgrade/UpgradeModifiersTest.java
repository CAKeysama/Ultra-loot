package io.github.cakeysama.ultraloot.upgrade;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeModifiersTest {
    private static final int DEFAULT_LOOT_TICKS = Balance.lootBaseTicks;
    private static final int DEFAULT_MOB_TICKS = Balance.mobBaseTicks;
    private static final double DEFAULT_MIN_FRACTION = Balance.minEnergyFraction;

    @AfterEach
    void restoreBalance() {
        Balance.lootBaseTicks = DEFAULT_LOOT_TICKS;
        Balance.mobBaseTicks = DEFAULT_MOB_TICKS;
        Balance.minEnergyFraction = DEFAULT_MIN_FRACTION;
    }

    private static UpgradeModifiers loot(MachineTier tier, UpgradeLevels cards) {
        return UpgradeModifiers.compute(MachineFamily.LOOT_GENERATOR, tier, cards);
    }

    private static UpgradeModifiers mob(MachineTier tier, UpgradeLevels cards) {
        return UpgradeModifiers.compute(MachineFamily.MOB_LOOT_GENERATOR, tier, cards);
    }

    @Test
    void lootGeneratorWithoutCards() {
        UpgradeModifiers m = loot(MachineTier.T1, UpgradeLevels.NONE);
        assertEquals(200, m.ticksPerOperation());
        assertEquals(1, m.attempts());
        assertEquals(5, m.range());
        assertEquals(0, m.orbCollectRadius());
        assertEquals(0, m.lootBonus());
        assertEquals(1.0, m.speedMultiplier(), 1e-9);
        assertEquals(1.0, m.energyMultiplier(), 1e-9);
        assertEquals(800, m.energyPerOperation(800));
    }

    @ParameterizedTest
    @EnumSource(MachineTier.class)
    void tierBaseValues(MachineTier tier) {
        UpgradeModifiers l = loot(tier, UpgradeLevels.NONE);
        assertEquals((int) Math.round(Balance.lootBaseTicks / Balance.TIER_SPEED[tier.index()]), l.ticksPerOperation());
        assertEquals(Balance.LOOT_BASE_ATTEMPTS[tier.index()], l.attempts());
        assertEquals(Balance.LOOT_BASE_RANGE[tier.index()], l.range());

        UpgradeModifiers m = mob(tier, UpgradeLevels.NONE);
        assertEquals((int) Math.round(Balance.mobBaseTicks / Balance.TIER_SPEED[tier.index()]), m.ticksPerOperation());
        assertEquals(Balance.MOB_BASE_KILLS[tier.index()], m.attempts());
        assertEquals(0, m.range());
        assertEquals(0, m.orbCollectRadius());
    }

    @Test
    void speedCardLevels() {
        int[] lootTicks = {160, 133, 100};
        int[] mobTicks = {240, 200, 150};
        for (int level = 1; level <= 3; level++) {
            UpgradeLevels cards = UpgradeLevels.NONE.with(UpgradeType.SPEED, level);
            UpgradeModifiers l = loot(MachineTier.T1, cards);
            assertEquals(lootTicks[level - 1], l.ticksPerOperation(), "loot speed " + level);
            assertEquals(Balance.SPEED_MULTIPLIER[level - 1], l.speedMultiplier(), 1e-9);
            assertEquals(Balance.SPEED_ENERGY[level - 1], l.energyMultiplier(), 1e-9);
            assertEquals(mobTicks[level - 1], mob(MachineTier.T1, cards).ticksPerOperation(), "mob speed " + level);
        }
        // Tier speed and card speed multiply: T4 (3x) * Speed III (2x) = 6x.
        assertEquals(33, loot(MachineTier.T4, UpgradeLevels.NONE.with(UpgradeType.SPEED, 3)).ticksPerOperation());
    }

    @Test
    void quantityCardLevels() {
        for (int level = 1; level <= 3; level++) {
            UpgradeLevels cards = UpgradeLevels.NONE.with(UpgradeType.QUANTITY, level);
            assertEquals(Balance.QUANTITY_MULTIPLIER[level - 1], loot(MachineTier.T1, cards).attempts());
            assertEquals(Balance.QUANTITY_MULTIPLIER[level - 1] * 2, loot(MachineTier.T2, cards).attempts());
            assertEquals(Balance.QUANTITY_MULTIPLIER[level - 1] * 2, mob(MachineTier.T3, cards).attempts());
            assertEquals(Balance.QUANTITY_ENERGY[level - 1], loot(MachineTier.T1, cards).energyMultiplier(), 1e-9);
        }
    }

    @Test
    void rangeCardExtendsLootRangeButOnlyOrbRadiusOnMobGenerator() {
        for (int level = 1; level <= 3; level++) {
            UpgradeLevels cards = UpgradeLevels.NONE.with(UpgradeType.RANGE, level);
            UpgradeModifiers l = loot(MachineTier.T2, cards);
            assertEquals(Balance.LOOT_BASE_RANGE[1] + Balance.RANGE_BONUS[level - 1], l.range());
            assertEquals(0, l.orbCollectRadius());

            UpgradeModifiers m = mob(MachineTier.T2, cards);
            assertEquals(0, m.range());
            assertEquals(Balance.ORB_COLLECT_RADIUS[level - 1], m.orbCollectRadius());
            assertEquals(Balance.RANGE_ENERGY[level - 1], m.energyMultiplier(), 1e-9);
        }
    }

    @Test
    void efficiencyCardLevels() {
        for (int level = 1; level <= 3; level++) {
            UpgradeModifiers m = loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.EFFICIENCY, level));
            assertEquals(Balance.EFFICIENCY_ENERGY[level - 1], m.energyMultiplier(), 1e-9);
            assertEquals(200, m.ticksPerOperation());
            assertEquals(1, m.attempts());
        }
        assertEquals(720, loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.EFFICIENCY, 1)).energyPerOperation(800));
        assertEquals(640, loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.EFFICIENCY, 2)).energyPerOperation(800));
        assertEquals(520, loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.EFFICIENCY, 3)).energyPerOperation(800));
    }

    @Test
    void lootCardLevels() {
        for (int level = 1; level <= 3; level++) {
            UpgradeLevels cards = UpgradeLevels.NONE.with(UpgradeType.LOOT, level);
            assertEquals(Balance.LOOT_BONUS[level - 1], loot(MachineTier.T1, cards).lootBonus());
            assertEquals(Balance.LOOT_BONUS[level - 1], mob(MachineTier.T1, cards).lootBonus());
            assertEquals(Balance.LOOT_ENERGY[level - 1], loot(MachineTier.T1, cards).energyMultiplier(), 1e-9);
        }
    }

    @Test
    void allCardsAtLevelThreeCombine() {
        UpgradeLevels all = UpgradeLevels.of(3, 3, 3, 3, 3);
        double expectedEnergy = 1.75 * 2.8 * 1.3 * 1.5 * 0.65;

        UpgradeModifiers l = loot(MachineTier.T1, all);
        assertEquals(100, l.ticksPerOperation());
        assertEquals(4, l.attempts());
        assertEquals(29, l.range());
        assertEquals(0, l.orbCollectRadius());
        assertEquals(3, l.lootBonus());
        assertEquals(expectedEnergy, l.energyMultiplier(), 1e-9);
        assertEquals(4969, l.energyPerOperation(800));

        UpgradeModifiers m = mob(MachineTier.T1, all);
        assertEquals(150, m.ticksPerOperation());
        assertEquals(4, m.attempts());
        assertEquals(0, m.range());
        assertEquals(6, m.orbCollectRadius());
        assertEquals(expectedEnergy, m.energyMultiplier(), 1e-9);
    }

    @Test
    @DisplayName("Exact multiples are not rounded up by floating point noise: 880 FE for Range I at 800 FE (regression BUG-02)")
    void exactEnergyCostsAreNotInflatedByFloatingPointNoise() {
        // 800 * 1.1 = 880.0000000000001 in double arithmetic; Math.ceil then yields 881.
        assertEquals(880, loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.RANGE, 1)).energyPerOperation(800));
        assertEquals(1760, loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.QUANTITY, 2)).energyPerOperation(800));
        assertEquals(4050, loot(MachineTier.T3, UpgradeLevels.NONE.with(UpgradeType.SPEED, 2)).energyPerOperation(3000));
    }

    @Test
    void energyFloorPreventsNearZeroCost() {
        Balance.minEnergyFraction = 0.9;
        UpgradeModifiers m = loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.EFFICIENCY, 3));
        assertEquals(0.65, m.energyMultiplier(), 1e-9, "the multiplier itself is not floored");
        assertEquals(720, m.energyPerOperation(800), "cost is floored at E_base * minEnergyFraction");
    }

    @Test
    void energyCostIsAlwaysAtLeastOneFe() {
        UpgradeModifiers m = loot(MachineTier.T1, UpgradeLevels.NONE.with(UpgradeType.EFFICIENCY, 3));
        assertEquals(1, m.energyPerOperation(1.0));
        assertEquals(1, m.energyPerOperation(0.0));
    }

    @Test
    void ticksNeverDropBelowMinimum() {
        Balance.lootBaseTicks = 6;
        Balance.mobBaseTicks = 1;
        UpgradeLevels speed3 = UpgradeLevels.NONE.with(UpgradeType.SPEED, 3);
        assertEquals(Balance.MIN_OPERATION_TICKS, loot(MachineTier.T4, speed3).ticksPerOperation());
        assertEquals(Balance.MIN_OPERATION_TICKS, mob(MachineTier.T1, UpgradeLevels.NONE).ticksPerOperation());
    }

    /** Exhaustive invariants over every card combination, both families and all tiers. */
    @Test
    void invariantsHoldForEveryCombination() {
        for (MachineFamily family : MachineFamily.values()) {
            for (MachineTier tier : MachineTier.values()) {
                int base = family.baseEnergyPerOperation(tier);
                for (int s = 0; s <= 3; s++) for (int q = 0; q <= 3; q++) for (int r = 0; r <= 3; r++)
                    for (int e = 0; e <= 3; e++) for (int lo = 0; lo <= 3; lo++) {
                        UpgradeLevels cards = UpgradeLevels.of(s, q, r, e, lo);
                        UpgradeModifiers m = UpgradeModifiers.compute(family, tier, cards);
                        String ctx = family + " " + tier + " " + cards;
                        assertTrue(m.ticksPerOperation() >= Balance.MIN_OPERATION_TICKS, ctx);
                        assertTrue(m.attempts() >= family.baseAttempts(tier), ctx);
                        assertTrue(m.energyPerOperation(base) >= Math.ceil(base * Balance.minEnergyFraction), ctx);
                        assertTrue(m.energyMultiplier() > 0, ctx);
                        if (family == MachineFamily.LOOT_GENERATOR) {
                            assertTrue(m.range() >= Balance.LOOT_BASE_RANGE[tier.index()], ctx);
                            assertEquals(0, m.orbCollectRadius(), ctx);
                        } else {
                            assertEquals(0, m.range(), ctx);
                        }
                    }
            }
        }
    }

    @Test
    void upgradeLevelsValidation() {
        assertThrows(IllegalArgumentException.class, () -> UpgradeLevels.of(4, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> UpgradeLevels.of(0, -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> UpgradeLevels.NONE.with(UpgradeType.LOOT, 4));
        UpgradeLevels a = UpgradeLevels.NONE.with(UpgradeType.RANGE, 2);
        assertEquals(0, UpgradeLevels.NONE.get(UpgradeType.RANGE), "with() must not mutate the original");
        assertEquals(2, a.get(UpgradeType.RANGE));
        assertEquals(UpgradeLevels.of(0, 0, 2, 0, 0), a);
        assertEquals(UpgradeLevels.of(0, 0, 2, 0, 0).hashCode(), a.hashCode());
    }
}
