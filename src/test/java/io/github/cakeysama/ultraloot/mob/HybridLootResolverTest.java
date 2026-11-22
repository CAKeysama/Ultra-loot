package io.github.cakeysama.ultraloot.mob;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hybrid loot semantics: native first, explicit supplemental ADD/REPLACE/DISABLED, fallback only when the native
 * table does not exist (a valid empty table is not a fallback trigger), missing configured tables are errors.
 */
class HybridLootResolverTest {
    private static final ResourceKey<LootTable> VANILLA = key("minecraft", "entities/zombie");
    private static final ResourceKey<LootTable> MODDED = key("examplemod", "entities/frost_wraith");
    private static final ResourceKey<LootTable> EMPTY_NATIVE = key("minecraft", "entities/goat");
    private static final ResourceKey<LootTable> MISSING = key("examplemod", "entities/no_table");
    private static final ResourceKey<LootTable> EXTRA = key("ultraloot", "mob_supplemental/zombie");
    private static final ResourceKey<LootTable> FALLBACK = key("ultraloot", "mob_fallback/generic");

    private final Map<ResourceKey<LootTable>, LootTable> registry = new HashMap<>();
    private final HybridLootResolver.Tables tables = new HybridLootResolver.Tables() {
        @Override
        public boolean exists(ResourceKey<LootTable> key) {
            return registry.containsKey(key);
        }

        @Override
        public LootTable get(ResourceKey<LootTable> key) {
            return registry.getOrDefault(key, LootTable.EMPTY);
        }
    };

    HybridLootResolverTest() {
        for (var k : List.of(VANILLA, MODDED, EXTRA, FALLBACK)) registry.put(k, LootTable.lootTable().build());
        registry.put(EMPTY_NATIVE, LootTable.lootTable().build()); // exists, zero pools: a valid empty result
    }

    private static ResourceKey<LootTable> key(String ns, String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    private static MobSimulationData data(MobSimulationData.Supplemental supplemental, ResourceKey<LootTable> fallback) {
        return new MobSimulationData(8, 1.0, 2, Optional.empty(), Optional.ofNullable(supplemental), Optional.ofNullable(fallback));
    }

    private static MobSimulationData.Supplemental sup(MobSimulationData.SupplementalMode mode, ResourceKey<LootTable> table) {
        return new MobSimulationData.Supplemental(mode, table);
    }

    @Test
    void vanillaNativeTableIsPrimary() {
        var r = HybridLootResolver.resolve(VANILLA, data(null, FALLBACK), tables);
        assertTrue(r.ok());
        assertEquals(HybridLootResolver.Source.NATIVE, r.source());
        assertEquals(List.of(registry.get(VANILLA)), r.tables());
    }

    @Test
    void moddedNativeTableIsPrimary() {
        var r = HybridLootResolver.resolve(MODDED, data(null, null), tables);
        assertEquals(HybridLootResolver.Source.NATIVE, r.source());
        assertSame(registry.get(MODDED), r.tables().getFirst());
    }

    @Test
    void validEmptyNativeTableDoesNotTriggerFallback() {
        var r = HybridLootResolver.resolve(EMPTY_NATIVE, data(null, FALLBACK), tables);
        assertEquals(HybridLootResolver.Source.NATIVE, r.source());
        assertSame(registry.get(EMPTY_NATIVE), r.tables().getFirst());
    }

    @Test
    void fallbackOnlyWhenNativeTableIsMissing() {
        var r = HybridLootResolver.resolve(MISSING, data(null, FALLBACK), tables);
        assertEquals(HybridLootResolver.Source.FALLBACK, r.source());
        assertSame(registry.get(FALLBACK), r.tables().getFirst());
    }

    @Test
    void entityWithoutLootTableKeyUsesFallback() {
        var r = HybridLootResolver.resolve(BuiltInLootTables.EMPTY, data(null, FALLBACK), tables);
        assertEquals(HybridLootResolver.Source.FALLBACK, r.source());
    }

    @Test
    void missingNativeWithoutFallbackIsReported() {
        var r = HybridLootResolver.resolve(MISSING, data(null, null), tables);
        assertFalse(r.ok());
        assertEquals(HybridLootResolver.Problem.NO_LOOT_TABLE, r.problem());
        assertTrue(r.tables().isEmpty());
    }

    @Test
    void missingFallbackIsAnError() {
        var r = HybridLootResolver.resolve(MISSING, data(null, key("ultraloot", "nope")), tables);
        assertEquals(HybridLootResolver.Problem.FALLBACK_MISSING, r.problem());
    }

    @Test
    void supplementalAddRollsBothTables() {
        var r = HybridLootResolver.resolve(VANILLA, data(sup(MobSimulationData.SupplementalMode.ADD, EXTRA), null), tables);
        assertEquals(HybridLootResolver.Source.NATIVE_AND_SUPPLEMENTAL, r.source());
        assertEquals(List.of(registry.get(VANILLA), registry.get(EXTRA)), r.tables());
    }

    @Test
    void supplementalAddAlsoAppliesToFallback() {
        var r = HybridLootResolver.resolve(MISSING, data(sup(MobSimulationData.SupplementalMode.ADD, EXTRA), FALLBACK), tables);
        assertEquals(HybridLootResolver.Source.FALLBACK_AND_SUPPLEMENTAL, r.source());
        assertEquals(2, r.tables().size());
    }

    @Test
    void supplementalReplaceIgnoresNative() {
        var r = HybridLootResolver.resolve(VANILLA, data(sup(MobSimulationData.SupplementalMode.REPLACE, EXTRA), null), tables);
        assertEquals(HybridLootResolver.Source.SUPPLEMENTAL_ONLY, r.source());
        assertEquals(List.of(registry.get(EXTRA)), r.tables());
    }

    @Test
    void disabledSupplementalHasNoEffect() {
        var r = HybridLootResolver.resolve(VANILLA, data(sup(MobSimulationData.SupplementalMode.DISABLED, key("x", "missing")), null), tables);
        assertEquals(HybridLootResolver.Source.NATIVE, r.source());
        assertNull(r.problem());
    }

    @Test
    void missingSupplementalIsAnError() {
        var r = HybridLootResolver.resolve(VANILLA, data(sup(MobSimulationData.SupplementalMode.ADD, key("x", "missing")), null), tables);
        assertEquals(HybridLootResolver.Problem.SUPPLEMENTAL_MISSING, r.problem());
    }

    @Test
    void noSupplementalIsEverAddedAutomatically() {
        var r = HybridLootResolver.resolve(VANILLA, new MobSimulationData(8, 1.0), tables);
        assertEquals(List.of(registry.get(VANILLA)), r.tables());
    }
}
