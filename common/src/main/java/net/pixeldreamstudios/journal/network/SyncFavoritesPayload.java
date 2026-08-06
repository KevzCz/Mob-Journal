package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

import java.util.HashSet;
import java.util.Set;

public record SyncFavoritesPayload(Set<ResourceLocation> favoriteMobs) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncFavoritesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "sync_favorites"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFavoritesPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), SyncFavoritesPayload::read);

    public static SyncFavoritesPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<ResourceLocation> favorites = new HashSet<>();
        for (int i = 0; i < size; i++) {
            favorites.add(buf.readResourceLocation());
        }
        return new SyncFavoritesPayload(favorites);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(favoriteMobs.size());
        for (ResourceLocation id : favoriteMobs) {
            buf.writeResourceLocation(id);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
