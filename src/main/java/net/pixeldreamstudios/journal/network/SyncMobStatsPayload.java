package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.MobStat;

import java.util.HashMap;
import java.util.Map;

public record SyncMobStatsPayload(Map<Identifier, MobStat> stats) implements CustomPayload {

    public static final Id<SyncMobStatsPayload> ID =
            new Id<>(Identifier.of("journal", "sync_mob_stats"));

    public static final PacketCodec<RegistryByteBuf, SyncMobStatsPayload> CODEC =
            PacketCodec.of(SyncMobStatsPayload::write, SyncMobStatsPayload::read);

    public static SyncMobStatsPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        Map<Identifier, MobStat> stats = new HashMap<>();

        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            int kills = buf.readVarInt();
            int deaths = buf.readVarInt();
            stats.put(id, new MobStat(kills, deaths));
        }

        return new SyncMobStatsPayload(stats);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(stats.size());
        for (var entry : stats.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            MobStat stat = entry.getValue();
            buf.writeVarInt(stat.kills());
            buf.writeVarInt(stat.deaths());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
