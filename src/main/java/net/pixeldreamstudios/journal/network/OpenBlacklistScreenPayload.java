package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class OpenBlacklistScreenPayload {
    public static final Identifier ID = new Identifier("journal", "open_blacklist");
    public static final OpenBlacklistScreenPayload INSTANCE = new OpenBlacklistScreenPayload();

    private OpenBlacklistScreenPayload() {}

    public static OpenBlacklistScreenPayload read(PacketByteBuf buf) {
        return INSTANCE;
    }

    public void write(PacketByteBuf buf) {
    }

    public static void sendToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        INSTANCE.write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public interface S2CHandler {
        void handle(OpenBlacklistScreenPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            OpenBlacklistScreenPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }
}