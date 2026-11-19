package io.github.cakeysama.ultraloot.network;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.menu.MachineAction;
import io.github.cakeysama.ultraloot.menu.MachineMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: a configuration request for the open machine menu. Validated by {@link MachineMenu#handleAction}. */
public record MachineActionPayload(int containerId, int action, int a, int b, String text) implements CustomPacketPayload {
    public static final Type<MachineActionPayload> TYPE = new Type<>(UltraLoot.id("machine_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineActionPayload::containerId,
            ByteBufCodecs.VAR_INT, MachineActionPayload::action,
            ByteBufCodecs.VAR_INT, MachineActionPayload::a,
            ByteBufCodecs.VAR_INT, MachineActionPayload::b,
            ByteBufCodecs.stringUtf8(256), MachineActionPayload::text,
            MachineActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Client helper used by screens. */
    public static void send(MachineMenu menu, MachineAction action, int a, int b, String text) {
        PacketDistributor.sendToServer(new MachineActionPayload(menu.containerId, action.ordinal(), a, b, text));
    }

    public static void send(MachineMenu menu, MachineAction action, int a, int b) {
        send(menu, action, a, b, "");
    }

    static void handle(MachineActionPayload payload, IPayloadContext context) {
        MachineAction action = MachineAction.byOrdinal(payload.action());
        if (action == null || !(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof MachineMenu menu && menu.containerId == payload.containerId()) {
            menu.handleAction(player, action, payload.a(), payload.b(), payload.text());
        }
    }
}
