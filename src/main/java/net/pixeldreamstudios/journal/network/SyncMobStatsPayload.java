package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.MobStat;

import java.util.HashMap;
import java.util.Map;

public final class SyncMobStatsPayload {
    public static final Identifier ID = new Identifier("journal", "sync_mob_stats");

    private final Map<Identifier, MobStat> stats;

    public SyncMobStatsPayload(Map<Identifier, MobStat> stats) {
        this.stats = stats;
    }

    public Map<Identifier, MobStat> stats() {
        return stats;
    }

    public static SyncMobStatsPayload read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Map<Identifier, MobStat> stats = new HashMap<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            int kills = buf.readVarInt();
            int deaths = buf.readVarInt();
            stats.put(id, new MobStat(kills, deaths));
        }
        return new SyncMobStatsPayload(stats);
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(stats.size());
        for (Map.Entry<Identifier, MobStat> e : stats.entrySet()) {
            buf.writeIdentifier(e.getKey());
            MobStat s = e.getValue();
            buf.writeVarInt(s.kills());
            buf.writeVarInt(s.deaths());
        }
    }

    public static void sendToClient(ServerPlayerEntity player, Map<Identifier, MobStat> stats) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncMobStatsPayload(stats).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void sendToServer(Map<Identifier, MobStat> stats) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncMobStatsPayload(stats).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public interface S2CHandler {
        void handle(SyncMobStatsPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            SyncMobStatsPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, SyncMobStatsPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            SyncMobStatsPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }
}
