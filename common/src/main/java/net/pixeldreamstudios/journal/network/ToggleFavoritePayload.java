package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record ToggleFavoritePayload(ResourceLocation mobId, boolean favorited) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleFavoritePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "toggle_favorite"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFavoritePayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), ToggleFavoritePayload::read);

    public static ToggleFavoritePayload read(RegistryFriendlyByteBuf buf) {
        ResourceLocation mobId = buf.readResourceLocation();
        boolean favorited = buf.readBoolean();
        return new ToggleFavoritePayload(mobId, favorited);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
        buf.writeBoolean(favorited);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
