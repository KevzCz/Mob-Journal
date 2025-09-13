package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ClientReadyPayload {
    public static final Identifier ID = new Identifier("journal", "client_ready");
    public static final ClientReadyPayload INSTANCE = new ClientReadyPayload();

    private ClientReadyPayload() {}

    public static ClientReadyPayload read(PacketByteBuf buf) {
        return INSTANCE;
    }

    public void write(PacketByteBuf buf) {
    }

    public static void sendToServer() {
        PacketByteBuf buf = PacketByteBufs.create();
        INSTANCE.write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void sendToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        INSTANCE.write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, ClientReadyPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            ClientReadyPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }

    public interface S2CHandler {
        void handle(ClientReadyPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            ClientReadyPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }
}
