package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class RequestMobDropsPayload {
    public static final Identifier ID = new Identifier("journal", "request_mob_drops");

    private final Identifier mobId;

    public RequestMobDropsPayload(Identifier mobId) {
        this.mobId = mobId;
    }

    public Identifier mobId() {
        return mobId;
    }

    public static RequestMobDropsPayload read(PacketByteBuf buf) {
        return new RequestMobDropsPayload(buf.readIdentifier());
    }

    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(mobId);
    }

    public static void sendToServer(Identifier mobId) {
        PacketByteBuf buf = PacketByteBufs.create();
        new RequestMobDropsPayload(mobId).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void sendToClient(ServerPlayerEntity player, Identifier mobId) {
        PacketByteBuf buf = PacketByteBufs.create();
        new RequestMobDropsPayload(mobId).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, RequestMobDropsPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            RequestMobDropsPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }

    public interface S2CHandler {
        void handle(RequestMobDropsPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            RequestMobDropsPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }
}
