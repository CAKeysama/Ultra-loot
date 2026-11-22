package io.github.cakeysama.ultraloot.mob;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hybrid loot = native loot table + explicit Ultra Loot extension + controlled fallback (docs/GDD.md §3.6).
 * <ol>
 *   <li><b>Native.</b> The mob's own loot table ({@code Mob#getLootTable}, any namespace) is the primary source
 *       whenever it exists in the loot table registry. A table that exists but yields nothing is a valid, empty
 *       result; it never triggers the fallback.</li>
 *   <li><b>Supplemental</b> (only if configured in the data map): {@code add} rolls the supplemental table in
 *       addition to the primary table on every kill; {@code replace} rolls only the supplemental table;
 *       {@code disabled} has no effect. Nothing is ever added automatically.</li>
 *   <li><b>Fallback</b> (only if configured): used as the primary table when the native table does not exist.</li>
 * </ol>
 * A configured table that does not exist is a configuration error, reported as such (never silently skipped).
 */
public final class HybridLootResolver {
    private HybridLootResolver() {}

    /** Read access to the loot table registry (the server's reloadable registries, or a test double). */
    public interface Tables {
        boolean exists(ResourceKey<LootTable> key);

        LootTable get(ResourceKey<LootTable> key);
    }

    public enum Source { NATIVE, NATIVE_AND_SUPPLEMENTAL, SUPPLEMENTAL_ONLY, FALLBACK, FALLBACK_AND_SUPPLEMENTAL }

    public enum Problem {
        /** No native table and no fallback configured. */
        NO_LOOT_TABLE,
        /** The configured fallback table does not exist. */
        FALLBACK_MISSING,
        /** The configured supplemental table does not exist. */
        SUPPLEMENTAL_MISSING
    }

    /** Either the tables to roll for each kill (in order) or the problem that prevents simulation. */
    public record Resolution(List<LootTable> tables, @Nullable Source source, @Nullable Problem problem) {
        public boolean ok() {
            return problem == null;
        }

        static Resolution of(Source source, LootTable... tables) {
            return new Resolution(List.of(tables), source, null);
        }

        static Resolution fail(Problem problem) {
            return new Resolution(List.of(), null, problem);
        }
    }

    public static Resolution resolve(ResourceKey<LootTable> nativeKey, MobSimulationData data, Tables tables) {
        var supplemental = data.activeSupplemental();
        LootTable extra = null;
        if (supplemental.isPresent()) {
            ResourceKey<LootTable> key = supplemental.get().lootTable();
            if (!tables.exists(key)) return Resolution.fail(Problem.SUPPLEMENTAL_MISSING);
            extra = tables.get(key);
            if (supplemental.get().mode() == MobSimulationData.SupplementalMode.REPLACE) {
                return Resolution.of(Source.SUPPLEMENTAL_ONLY, extra);
            }
        }

        boolean hasNative = !nativeKey.equals(BuiltInLootTables.EMPTY) && tables.exists(nativeKey);
        if (hasNative) {
            LootTable primary = tables.get(nativeKey);
            return extra == null ? Resolution.of(Source.NATIVE, primary)
                    : Resolution.of(Source.NATIVE_AND_SUPPLEMENTAL, primary, extra);
        }

        if (data.fallbackLootTable().isEmpty()) return Resolution.fail(Problem.NO_LOOT_TABLE);
        ResourceKey<LootTable> fallbackKey = data.fallbackLootTable().get();
        if (!tables.exists(fallbackKey)) return Resolution.fail(Problem.FALLBACK_MISSING);
        LootTable fallback = tables.get(fallbackKey);
        return extra == null ? Resolution.of(Source.FALLBACK, fallback)
                : Resolution.of(Source.FALLBACK_AND_SUPPLEMENTAL, fallback, extra);
    }
}
