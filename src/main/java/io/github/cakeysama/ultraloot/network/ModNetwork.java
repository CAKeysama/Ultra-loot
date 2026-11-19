package io.github.cakeysama.ultraloot.network;

import io.github.cakeysama.ultraloot.UltraLoot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public final class ModNetwork {
    private static final String VERSION = "1";

    private ModNetwork() {}

    /** Handlers run on the main thread (NeoForge default for 1.21.1). */
    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(MachineSyncPayload.TYPE, MachineSyncPayload.STREAM_CODEC, MachineSyncPayload::handle);
        registrar.playToServer(MachineActionPayload.TYPE, MachineActionPayload.STREAM_CODEC, MachineActionPayload::handle);
    }
}
