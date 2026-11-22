package io.github.cakeysama.ultraloot.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.Locale;
import java.util.Optional;

/**
 * Per-mob simulation settings, read from the {@code ultraloot:mob_simulation} data map
 * ({@code data/<ns>/data_maps/entity_type/mob_simulation.json}). Mobs without an entry use
 * {@link Balance#defaultXpPerKill}, an energy multiplier of 1 and mob tier II, the floor of the Tier II cost band.
 * <p>
 * Hybrid loot (docs/GDD.md §3.6): the mob's native loot table is always the primary source. {@code supplemental}
 * optionally ADDs a second table to every kill or REPLACEs the native table; it is never applied unless configured.
 * {@code fallback_loot_table} is used only when the native table does not exist (a native table that exists but rolls
 * nothing is a valid, empty result).
 *
 * @param xpCost            XP points consumed per simulated kill
 * @param energyMultiplier  multiplier on E_base
 * @param tier              mob tier I-III (reward class; costs rise with tier, see docs/BALANCE.md)
 * @param costException     reason why this mob's cost is outside its tier band (empty = must be inside the band)
 * @param supplemental      optional Ultra Loot extension table
 * @param fallbackLootTable table used when the mob has no native loot table
 */
@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public record MobSimulationData(int xpCost, double energyMultiplier, int tier, Optional<String> costException,
                                Optional<Supplemental> supplemental, Optional<ResourceKey<LootTable>> fallbackLootTable) {
    public static final int DEFAULT_TIER = 2;

    public enum SupplementalMode implements StringRepresentable {
        /** The supplemental table is rolled in addition to the native table on every kill. */
        ADD,
        /** Only the supplemental table is rolled; the native table is ignored. */
        REPLACE,
        /** The entry is kept for documentation/overrides but has no effect. */
        DISABLED;

        public static final Codec<SupplementalMode> CODEC = StringRepresentable.fromEnum(SupplementalMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Supplemental(SupplementalMode mode, ResourceKey<LootTable> lootTable) {
        public static final Codec<Supplemental> CODEC = RecordCodecBuilder.create(i -> i.group(
                SupplementalMode.CODEC.fieldOf("mode").forGetter(Supplemental::mode),
                ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(Supplemental::lootTable)
        ).apply(i, Supplemental::new));
    }

    public static final Codec<MobSimulationData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100_000).fieldOf("xp_cost").forGetter(MobSimulationData::xpCost),
            Codec.doubleRange(0.1, 100.0).optionalFieldOf("energy_multiplier", 1.0).forGetter(MobSimulationData::energyMultiplier),
            Codec.intRange(1, 3).optionalFieldOf("tier", DEFAULT_TIER).forGetter(MobSimulationData::tier),
            Codec.STRING.optionalFieldOf("cost_exception").forGetter(MobSimulationData::costException),
            Supplemental.CODEC.optionalFieldOf("supplemental").forGetter(MobSimulationData::supplemental),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("fallback_loot_table").forGetter(MobSimulationData::fallbackLootTable)
    ).apply(i, MobSimulationData::new));

    public static final DataMapType<EntityType<?>, MobSimulationData> TYPE = DataMapType
            .builder(UltraLoot.id("mob_simulation"), Registries.ENTITY_TYPE, CODEC)
            .synced(CODEC, false)
            .build();

    /** Cost-only constructor (tests, defaults). */
    public MobSimulationData(int xpCost, double energyMultiplier) {
        this(xpCost, energyMultiplier, DEFAULT_TIER, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static MobSimulationData of(EntityType<?> type) {
        MobSimulationData data = type.builtInRegistryHolder().getData(TYPE);
        return data != null ? data : new MobSimulationData(Balance.defaultXpPerKill, 1.0);
    }

    /** The supplemental entry when it is active (mode ADD or REPLACE). */
    public Optional<Supplemental> activeSupplemental() {
        return supplemental.filter(s -> s.mode() != SupplementalMode.DISABLED);
    }

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(TYPE);
    }
}
