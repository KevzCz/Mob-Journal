package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public record SyncFavoritesPayload(Set<Identifier> favoriteMobs) implements CustomPayload {

    public static final Id<SyncFavoritesPayload> ID =
            new Id<>(Identifier.of("journal", "sync_favorites"));

    public static final PacketCodec<RegistryByteBuf, SyncFavoritesPayload> CODEC =
            PacketCodec.of(SyncFavoritesPayload::write, SyncFavoritesPayload::read);

    public static SyncFavoritesPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        Set<Identifier> favorites = new HashSet<>();
        for (int i = 0; i < size; i++) {
            favorites.add(buf.readIdentifier());
        }
        return new SyncFavoritesPayload(favorites);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(favoriteMobs.size());
        for (Identifier id : favoriteMobs) {
            buf.writeIdentifier(id);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
