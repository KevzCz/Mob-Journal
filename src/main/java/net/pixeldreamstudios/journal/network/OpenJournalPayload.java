package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class OpenJournalPayload {
    public static final Identifier ID = new Identifier("journal", "open_request");
    public static final OpenJournalPayload INSTANCE = new OpenJournalPayload();

    private OpenJournalPayload() {}

    public static OpenJournalPayload read(PacketByteBuf buf) {
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
        void handle(ServerPlayerEntity player, OpenJournalPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            OpenJournalPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }

    public interface S2CHandler {
        void handle(OpenJournalPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            OpenJournalPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }
}
