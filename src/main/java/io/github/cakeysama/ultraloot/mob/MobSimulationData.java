package io.github.cakeysama.ultraloot.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Per-mob simulation cost, read from the {@code ultraloot:mob_simulation} data map
 * ({@code data/<ns>/data_maps/entity_type/mob_simulation.json}). Mobs without an entry use
 * {@link Balance#defaultXpPerKill} and an energy multiplier of 1.
 *
 * @param xpCost           XP points consumed per simulated kill
 * @param energyMultiplier multiplier on E_base
 */
@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public record MobSimulationData(int xpCost, double energyMultiplier) {
    public static final Codec<MobSimulationData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100_000).fieldOf("xp_cost").forGetter(MobSimulationData::xpCost),
            Codec.doubleRange(0.1, 100.0).optionalFieldOf("energy_multiplier", 1.0).forGetter(MobSimulationData::energyMultiplier)
    ).apply(i, MobSimulationData::new));

    public static final DataMapType<EntityType<?>, MobSimulationData> TYPE = DataMapType
            .builder(UltraLoot.id("mob_simulation"), Registries.ENTITY_TYPE, CODEC)
            .synced(CODEC, false)
            .build();

    public static MobSimulationData of(EntityType<?> type) {
        MobSimulationData data = type.builtInRegistryHolder().getData(TYPE);
        return data != null ? data : new MobSimulationData(Balance.defaultXpPerKill, 1.0);
    }

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(TYPE);
    }
}
