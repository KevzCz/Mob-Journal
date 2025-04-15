package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientReadyPayload() implements CustomPayload {
    public static final CustomPayload.Id<ClientReadyPayload> ID =
            new CustomPayload.Id<>(Identifier.of("journal", "client_ready"));

    public static final ClientReadyPayload INSTANCE = new ClientReadyPayload();

    public static final PacketCodec<RegistryByteBuf, ClientReadyPayload> CODEC =
            PacketCodec.of((buf, payload) -> {}, buf -> INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
