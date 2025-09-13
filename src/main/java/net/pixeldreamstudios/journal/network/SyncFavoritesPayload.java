package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public final class SyncFavoritesPayload {
    public static final Identifier ID = new Identifier("journal", "sync_favorites");

    private final Set<Identifier> favoriteMobs;

    public SyncFavoritesPayload(Set<Identifier> favoriteMobs) {
        this.favoriteMobs = favoriteMobs;
    }

    public Set<Identifier> favoriteMobs() {
        return favoriteMobs;
    }

    public static SyncFavoritesPayload read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Set<Identifier> favorites = new HashSet<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            favorites.add(buf.readIdentifier());
        }
        return new SyncFavoritesPayload(favorites);
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(favoriteMobs.size());
        for (Identifier id : favoriteMobs) {
            buf.writeIdentifier(id);
        }
    }

    public static void sendToClient(ServerPlayerEntity player, Set<Identifier> favorites) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncFavoritesPayload(favorites).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void sendToServer(Set<Identifier> favorites) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncFavoritesPayload(favorites).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public interface S2CHandler {
        void handle(SyncFavoritesPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            SyncFavoritesPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, SyncFavoritesPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            SyncFavoritesPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }
}
