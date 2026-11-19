package io.github.cakeysama.ultraloot.network;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.menu.MachineMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: GUI snapshot of the machine whose menu the player has open. */
public record MachineSyncPayload(int containerId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<MachineSyncPayload> TYPE = new Type<>(UltraLoot.id("machine_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineSyncPayload::containerId,
            ByteBufCodecs.COMPOUND_TAG, MachineSyncPayload::data,
            MachineSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(MachineSyncPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof MachineMenu menu && menu.containerId == payload.containerId()) {
            menu.receiveSync(payload.data());
        }
    }
}
