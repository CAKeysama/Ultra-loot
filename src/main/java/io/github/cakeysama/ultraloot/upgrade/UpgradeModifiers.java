package io.github.cakeysama.ultraloot.upgrade;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineTier;

/**
 * Central modifier service: turns tier + family + installed cards into the operational parameters of a machine.
 * <p>
 * Energy per operation:
 * {@code E_final = max(E_base * M_speed * M_quantity * M_range * M_loot * M_efficiency, E_base * minEnergyFraction)},
 * where E_base already includes the loot profile / mob energy multiplier. The floor makes free operations impossible.
 *
 * @param ticksPerOperation ticks between operations
 * @param attempts          independent loot rolls / simulated kills per operation
 * @param range             Loot Generator scan range in blocks (0 for the Mob Loot Generator)
 * @param orbCollectRadius  Mob Loot Generator XP orb collection radius (0 = disabled)
 * @param lootBonus         luck (Loot Generator) or Looting level (Mob Loot Generator)
 * @param energyMultiplier  product of all card energy multipliers (before the floor)
 */
public record UpgradeModifiers(int ticksPerOperation, int attempts, int range, int orbCollectRadius, int lootBonus,
                               double speedMultiplier, double energyMultiplier) {

    public static UpgradeModifiers compute(MachineFamily family, MachineTier tier, UpgradeLevels cards) {
        double speed = tier.speed() * pick(Balance.SPEED_MULTIPLIER, cards.get(UpgradeType.SPEED), 1.0);
        int ticks = Math.max(Balance.MIN_OPERATION_TICKS, (int) Math.round(family.baseTicks() / speed));

        int attempts = family.baseAttempts(tier) * pick(Balance.QUANTITY_MULTIPLIER, cards.get(UpgradeType.QUANTITY), 1);

        int rangeLevel = cards.get(UpgradeType.RANGE);
        int range = 0;
        int orbRadius = 0;
        if (family == MachineFamily.LOOT_GENERATOR) {
            range = Balance.LOOT_BASE_RANGE[tier.index()] + pick(Balance.RANGE_BONUS, rangeLevel, 0);
        } else {
            orbRadius = pick(Balance.ORB_COLLECT_RADIUS, rangeLevel, 0);
        }

        int lootBonus = pick(Balance.LOOT_BONUS, cards.get(UpgradeType.LOOT), 0);

        double energy = pick(Balance.SPEED_ENERGY, cards.get(UpgradeType.SPEED), 1.0)
                * pick(Balance.QUANTITY_ENERGY, cards.get(UpgradeType.QUANTITY), 1.0)
                * pick(Balance.RANGE_ENERGY, rangeLevel, 1.0)
                * pick(Balance.LOOT_ENERGY, cards.get(UpgradeType.LOOT), 1.0)
                * pick(Balance.EFFICIENCY_ENERGY, cards.get(UpgradeType.EFFICIENCY), 1.0);

        return new UpgradeModifiers(ticks, attempts, range, orbRadius, lootBonus, speed, energy);
    }

    /** FE per operation for a given E_base (already scaled by profile/mob multipliers). Always at least 1 FE. */
    public int energyPerOperation(double baseEnergy) {
        double floor = baseEnergy * Balance.minEnergyFraction;
        double cost = Math.max(baseEnergy * energyMultiplier, floor);
        // The epsilon keeps exact products such as 800 * 1.1 (= 880.0000000000001) from rounding up to 881.
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.ceil(cost - 1e-6)));
    }

    private static double pick(double[] table, int level, double none) {
        return level <= 0 ? none : table[level - 1];
    }

    private static int pick(int[] table, int level, int none) {
        return level <= 0 ? none : table[level - 1];
    }
}
