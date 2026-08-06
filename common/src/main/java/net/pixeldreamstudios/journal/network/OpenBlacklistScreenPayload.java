package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record OpenBlacklistScreenPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBlacklistScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "open_blacklist"));

    public static final OpenBlacklistScreenPayload INSTANCE = new OpenBlacklistScreenPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBlacklistScreenPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {}, buf -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
