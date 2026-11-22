package io.github.cakeysama.ultraloot.registry;

import com.mojang.serialization.Codec;
import io.github.cakeysama.ultraloot.UltraLoot;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, UltraLoot.MOD_ID);

    /** Entity type id selected by a Mob Card. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MOB =
            REGISTER.registerComponentType("mob", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** XP points held by an Experience Capsule. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_XP =
            REGISTER.registerComponentType("stored_xp", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * Wear of a Mob Card: simulated kills read from it since it was imprinted or last restored (absent = 0, so cards
     * from older versions start intact). See {@link io.github.cakeysama.ultraloot.mob.CardWear}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CARD_WEAR =
            REGISTER.registerComponentType("card_wear", b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {}
}
