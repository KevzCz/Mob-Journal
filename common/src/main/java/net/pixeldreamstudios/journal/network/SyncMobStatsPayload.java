package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.data.MobStat;

import java.util.HashMap;
import java.util.Map;

public record SyncMobStatsPayload(Map<ResourceLocation, MobStat> stats) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMobStatsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "sync_mob_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMobStatsPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), SyncMobStatsPayload::read);

    public static SyncMobStatsPayload read(RegistryFriendlyByteBuf buf) {
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

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(stats.size());
        for (var entry : stats.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            MobStat stat = entry.getValue();
            buf.writeVarInt(stat.kills());
            buf.writeVarInt(stat.deaths());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
