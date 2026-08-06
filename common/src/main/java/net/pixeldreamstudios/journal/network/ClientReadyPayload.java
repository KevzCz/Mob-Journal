package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record ClientReadyPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientReadyPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "client_ready"));

    public static final ClientReadyPayload INSTANCE = new ClientReadyPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientReadyPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {}, buf -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
