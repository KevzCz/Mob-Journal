package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

import java.util.HashSet;
import java.util.Set;

public record SyncFavoritesPayload(Set<ResourceLocation> favoriteMobs) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "sync_favorites");

    public static SyncFavoritesPayload read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Set<ResourceLocation> favorites = new HashSet<>();
        for (int i = 0; i < size; i++) {
            favorites.add(buf.readResourceLocation());
        }
        return new SyncFavoritesPayload(favorites);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(favoriteMobs.size());
        for (ResourceLocation id : favoriteMobs) {
            buf.writeResourceLocation(id);
        }
    }
}
