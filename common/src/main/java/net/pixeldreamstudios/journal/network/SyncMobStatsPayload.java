package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.data.MobStat;

import java.util.HashMap;
import java.util.Map;

public record SyncMobStatsPayload(Map<ResourceLocation, MobStat> stats) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "sync_mob_stats");

    public static SyncMobStatsPayload read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, MobStat> stats = new HashMap<>();

        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int kills = buf.readVarInt();
            int deaths = buf.readVarInt();
            stats.put(id, new MobStat(kills, deaths));
        }

        return new SyncMobStatsPayload(stats);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(stats.size());
        for (var entry : stats.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            MobStat stat = entry.getValue();
            buf.writeVarInt(stat.kills());
            buf.writeVarInt(stat.deaths());
        }
    }
}
