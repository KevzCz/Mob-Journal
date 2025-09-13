package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ToggleFavoritePayload {
    public static final Identifier ID = new Identifier("journal", "toggle_favorite");

    private final Identifier mobId;
    private final boolean favorited;

    public ToggleFavoritePayload(Identifier mobId, boolean favorited) {
        this.mobId = mobId;
        this.favorited = favorited;
    }

    public Identifier mobId() {
        return mobId;
    }

    public boolean favorited() {
        return favorited;
    }

    public static ToggleFavoritePayload read(PacketByteBuf buf) {
        Identifier mobId = buf.readIdentifier();
        boolean favorited = buf.readBoolean();
        return new ToggleFavoritePayload(mobId, favorited);
    }

    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(mobId);
        buf.writeBoolean(favorited);
    }

    public static void sendToServer(Identifier mobId, boolean favorited) {
        PacketByteBuf buf = PacketByteBufs.create();
        new ToggleFavoritePayload(mobId, favorited).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void sendToClient(ServerPlayerEntity player, Identifier mobId, boolean favorited) {
        PacketByteBuf buf = PacketByteBufs.create();
        new ToggleFavoritePayload(mobId, favorited).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, ToggleFavoritePayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            ToggleFavoritePayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }

    public interface S2CHandler {
        void handle(ToggleFavoritePayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            ToggleFavoritePayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }
}
