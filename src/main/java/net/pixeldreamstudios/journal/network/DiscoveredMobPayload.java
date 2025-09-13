package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class DiscoveredMobPayload {
    public static final Identifier ID = new Identifier("journal", "discovered_mob");

    private final Identifier mobId;
    private final long timestamp;

    public DiscoveredMobPayload(Identifier mobId, long timestamp) {
        this.mobId = mobId;
        this.timestamp = timestamp;
    }

    public Identifier mobId() {
        return mobId;
    }

    public long timestamp() {
        return timestamp;
    }

    public static DiscoveredMobPayload read(PacketByteBuf buf) {
        Identifier mobId = buf.readIdentifier();
        long timestamp = buf.readLong();
        return new DiscoveredMobPayload(mobId, timestamp);
    }

    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(mobId);
        buf.writeLong(timestamp);
    }

    public static void sendToServer(Identifier mobId, long timestamp) {
        PacketByteBuf buf = PacketByteBufs.create();
        new DiscoveredMobPayload(mobId, timestamp).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void sendToClient(ServerPlayerEntity player, Identifier mobId, long timestamp) {
        PacketByteBuf buf = PacketByteBufs.create();
        new DiscoveredMobPayload(mobId, timestamp).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, DiscoveredMobPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            DiscoveredMobPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }

    public interface S2CHandler {
        void handle(DiscoveredMobPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            DiscoveredMobPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }
}
