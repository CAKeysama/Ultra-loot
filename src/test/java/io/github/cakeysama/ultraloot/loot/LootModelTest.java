package io.github.cakeysama.ultraloot.loot;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.cakeysama.ultraloot.machine.MachineStatistics;
import io.github.cakeysama.ultraloot.machine.MachineStatus;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import io.github.cakeysama.ultraloot.machine.SideMode;
import io.github.cakeysama.ultraloot.mob.MobSimulationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loot profile model, filters and the small value types persisted or synced by machines. */
class LootModelTest {
    private static LootProfile.Definition parse(String json) {
        return LootProfile.Definition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static boolean parses(String json) {
        return LootProfile.Definition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().isPresent();
    }

    @Test
    void autoChestProfileIdFormat() {
        LootProfile p = LootProfile.autoChest(ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
        assertEquals(ResourceLocation.withDefaultNamespace("auto/chests/simple_dungeon"), p.id());
        assertEquals(ResourceLocation.withDefaultNamespace("chests/simple_dungeon"), p.table().location());
        assertEquals(LootContextType.CHEST, p.context());
        // Auto-discovered profiles are gated: higher minimum tier and an energy surcharge (Balance, server config).
        assertEquals(io.github.cakeysama.ultraloot.config.Balance.autoProfileMinTier, p.minTier());
        assertEquals(io.github.cakeysama.ultraloot.config.Balance.autoProfileEnergyMultiplier, p.energyMultiplier());
        assertTrue(p.autoDiscovered());
        assertEquals("Simple dungeon", p.fallbackName());
        assertEquals("ultraloot.profile.minecraft.auto.chests.simple_dungeon", p.translationKey());
    }

    @Test
    void definitionDefaults() {
        LootProfile.Definition d = parse("{\"loot_table\":\"minecraft:chests/abandoned_mineshaft\"}");
        assertEquals(LootContextType.CHEST, d.context());
        assertEquals("custom", d.category());
        assertEquals(Items.CHEST, d.icon());
        assertEquals(1, d.minTier());
        assertEquals(1.0, d.energyMultiplier());
        LootProfile p = d.toProfile(ResourceLocation.fromNamespaceAndPath("ultraloot", "mineshaft"));
        assertFalse(p.autoDiscovered());
        assertEquals("Mineshaft", p.fallbackName());
    }

    @Test
    void definitionFullForm() {
        LootProfile.Definition d = parse("{\"loot_table\":\"minecraft:gameplay/fishing\",\"context\":\"fishing\",\"category\":\"fishing\","
                + "\"icon\":\"minecraft:fishing_rod\",\"min_tier\":2,\"energy_multiplier\":1.5}");
        assertEquals(LootContextType.FISHING, d.context());
        assertEquals(Items.FISHING_ROD, d.icon());
        assertEquals(2, d.minTier());
        assertEquals(1.5, d.energyMultiplier());
    }

    @Test
    void definitionRejectsInvalidValues() {
        assertFalse(parses("{}"));
        assertFalse(parses("{\"loot_table\":\"minecraft:chests/x\",\"min_tier\":5}"));
        assertFalse(parses("{\"loot_table\":\"minecraft:chests/x\",\"min_tier\":0}"));
        assertFalse(parses("{\"loot_table\":\"minecraft:chests/x\",\"energy_multiplier\":0}"));
        assertFalse(parses("{\"loot_table\":\"minecraft:chests/x\",\"context\":\"nonsense\"}"));
    }

    @Test
    void filterAllKeepsEverything() {
        assertTrue(LootFilter.ALL.test(new ItemStack(Items.STONE)));
        assertTrue(LootFilter.ALL.test(new ItemStack(Items.DIAMOND_SWORD)));
    }

    @Test
    void foodFilterUsesFoodComponent() {
        assertTrue(LootFilter.FOOD.test(new ItemStack(Items.BREAD)));
        assertTrue(LootFilter.FOOD.test(new ItemStack(Items.ROTTEN_FLESH)));
        assertFalse(LootFilter.FOOD.test(new ItemStack(Items.STONE)));
        assertFalse(LootFilter.FOOD.test(new ItemStack(Items.IRON_SWORD)));
    }

    @Test
    void equipmentFilterUsesDurability() {
        assertTrue(LootFilter.EQUIPMENT.test(new ItemStack(Items.IRON_SWORD)));
        assertTrue(LootFilter.EQUIPMENT.test(new ItemStack(Items.GOLDEN_HELMET)));
        assertFalse(LootFilter.EQUIPMENT.test(new ItemStack(Items.BREAD)));
    }

    @Test
    void ordinalFallbacks() {
        assertEquals(LootFilter.ALL, LootFilter.byOrdinal(42));
        assertEquals(LootFilter.EQUIPMENT, LootFilter.byOrdinal(3));
        assertEquals(SideMode.NONE, SideMode.byOrdinal(-3));
        assertEquals(MachineStatus.NO_ENERGY, MachineStatus.byOrdinal(1_000));
        assertEquals(MachineTier.T1, MachineTier.byNumber(0));
        assertEquals(MachineTier.T4, MachineTier.byNumber(9));
        assertEquals(MachineTier.T2, MachineTier.byNumber(2));
    }

    @Test
    void onlyRunningIsWorking() {
        for (MachineStatus status : MachineStatus.values()) {
            assertEquals(status == MachineStatus.RUNNING, status.isWorking(), status.name());
        }
    }

    @Test
    void lootResultCountsItems() {
        LootEngine.Result r = new LootEngine.Result(java.util.List.of(new ItemStack(Items.BONE, 3), new ItemStack(Items.STRING, 2)), 4);
        assertEquals(5, r.itemCount());
        assertEquals(4, r.filteredCount());
    }

    @Test
    void statisticsAccumulateAndRoundTrip() {
        MachineStatistics stats = new MachineStatistics();
        stats.recordOperation(800, 8, 5, 1);
        stats.recordOperation(800, 8, 3, 0);
        assertEquals(2, stats.operations());
        assertEquals(1600, stats.energyConsumed());
        assertEquals(16, stats.experienceUsed());
        assertEquals(8, stats.itemsGenerated());
        assertEquals(1, stats.itemsFiltered());

        CompoundTag tag = stats.save();
        MachineStatistics copy = new MachineStatistics();
        copy.load(tag);
        assertEquals(tag, copy.save());
        copy.load(new CompoundTag());
        assertEquals(0, copy.operations());
    }

    @Test
    void mobSimulationDataCodec() {
        MobSimulationData d = MobSimulationData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"xp_cost\":12}")).getOrThrow();
        assertEquals(12, d.xpCost());
        assertEquals(1.0, d.energyMultiplier());
        assertTrue(MobSimulationData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"xp_cost\":-1}")).result().isEmpty());
        assertTrue(MobSimulationData.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"xp_cost\":1,\"energy_multiplier\":0.01}")).result().isEmpty());
    }
}
