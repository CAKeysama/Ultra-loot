package io.github.cakeysama.ultraloot.machine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cakeysama.ultraloot.config.Balance;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Balance invariants (docs/BALANCE.md):
 * <ul>
 *   <li>machine tiers: FE per loot roll / simulated kill never increases with tier. Within one profile or mob the
 *       expected items and value per roll do not depend on the tier, so FE per roll is the normalized efficiency
 *       (FE / expected loot and FE / expected value) and must be non-increasing;</li>
 *   <li>mob tiers: I &lt; II &lt; III in FE and XP, every mob inside its band unless it declares a cost_exception.</li>
 * </ul>
 */
class TierBalanceTest {
    /** (multiplier lo, hi, xp lo, hi) per mob tier; must match MOB_BANDS in tools/generate_data.py. */
    private static final double[][] BANDS = {{0.5, 0.9, 2, 6}, {1.0, 1.6, 8, 20}, {1.8, 4.0, 25, 120}};

    @Test
    void machineFePerAttemptNeverIncreasesWithTier() {
        for (MachineFamily family : MachineFamily.values()) {
            int last = Integer.MAX_VALUE;
            for (MachineTier tier : MachineTier.values()) {
                int perAttempt = family.baseEnergyPerAttempt(tier);
                assertTrue(perAttempt <= last, family + " " + tier + " costs more per attempt than the tier below");
                assertEquals(perAttempt * family.baseAttempts(tier), family.baseEnergyPerOperation(tier));
                last = perAttempt;
            }
        }
    }

    @Test
    void hugeConfiguredCostSaturatesInsteadOfOverflowing() {
        int[] saved = Balance.lootEnergyPerAttempt.clone();
        try {
            Balance.lootEnergyPerAttempt[MachineTier.values()[3].index()] = Integer.MAX_VALUE / 2;
            int cost = MachineFamily.LOOT_GENERATOR.baseEnergyPerOperation(MachineTier.values()[3]);
            assertEquals(Integer.MAX_VALUE, cost, "E_base must saturate, never wrap to a negative cost");
        } finally {
            System.arraycopy(saved, 0, Balance.lootEnergyPerAttempt, 0, saved.length);
        }
    }

    @Test
    void autoProfilesAreNeverCheaperThanExplicitOnes() {
        assertTrue(Balance.autoProfileEnergyMultiplier >= 1.0);
    }

    @Test
    void mobTierBandsAreOrderedAndDisjoint() {
        for (int t = 0; t + 1 < BANDS.length; t++) {
            assertTrue(BANDS[t][1] < BANDS[t + 1][0], "FE bands overlap at tier " + (t + 1));
            assertTrue(BANDS[t][3] < BANDS[t + 1][2], "XP bands overlap at tier " + (t + 1));
        }
    }

    @Test
    void everyMobIsInsideItsTierBandOrDeclaresAnException() throws Exception {
        JsonObject values = readDataMap().getAsJsonObject("values");
        assertTrue(values.size() > 50, "data map looks truncated");
        for (Map.Entry<String, com.google.gson.JsonElement> e : values.entrySet()) {
            JsonObject entry = e.getValue().getAsJsonObject();
            int tier = entry.has("tier") ? entry.get("tier").getAsInt() : 2;
            double mult = entry.has("energy_multiplier") ? entry.get("energy_multiplier").getAsDouble() : 1.0;
            int xp = entry.get("xp_cost").getAsInt();
            assertTrue(tier >= 1 && tier <= 3, e.getKey() + " tier " + tier);
            double[] band = BANDS[tier - 1];
            boolean inside = mult >= band[0] && mult <= band[1] && xp >= band[2] && xp <= band[3];
            if (entry.has("cost_exception")) {
                assertTrue(!inside, e.getKey() + " declares a cost_exception but is inside its band");
                assertTrue(entry.get("cost_exception").getAsString().length() > 20, e.getKey() + " exception needs a reason");
            } else {
                assertTrue(inside, e.getKey() + " (tier " + tier + ", x" + mult + ", " + xp + " XP) is outside its band");
            }
        }
    }

    @Test
    void unlistedMobsUseTheTierTwoFloor() {
        assertEquals(BANDS[1][2], Balance.defaultXpPerKill);
    }

    private static JsonObject readDataMap() throws Exception {
        var in = TierBalanceTest.class.getClassLoader()
                .getResourceAsStream("data/ultraloot/data_maps/entity_type/mob_simulation.json");
        assertNotNull(in, "mob_simulation.json not on the test classpath");
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
