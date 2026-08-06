package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record OpenJournalPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenJournalPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "open_request"));

    public static final OpenJournalPayload INSTANCE = new OpenJournalPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenJournalPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {}, buf -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
