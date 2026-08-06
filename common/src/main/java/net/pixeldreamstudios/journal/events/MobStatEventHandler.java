package net.pixeldreamstudios.journal.events;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.journal.data.JournalStorage;
import net.pixeldreamstudios.journal.data.MobStatsData;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.SyncMobStatsPayload;

public class MobStatEventHandler {

    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide()) return EventResult.pass();

            if (entity instanceof ServerPlayer deadPlayer) {
                if (source.getEntity() instanceof LivingEntity killer) {
                    ResourceLocation id = EntityType.getKey(killer.getType());
                    MobStatsData stats = JournalStorage.getMobStats(deadPlayer);
                    stats.incrementDeaths(id);
                    syncStats(deadPlayer, stats);
                }
                return EventResult.pass();
            }

            if (source.getEntity() instanceof ServerPlayer player && entity.getType().canSummon()) {
                ResourceLocation id = EntityType.getKey(entity.getType());
                MobStatsData stats = JournalStorage.getMobStats(player);
                stats.incrementKills(id);
                syncStats(player, stats);
            }

            return EventResult.pass();
        });
    }

    private static void syncStats(ServerPlayer player, MobStatsData stats) {
        NetworkManager.sendToClient(player, new SyncMobStatsPayload(stats.getAllStats()));
    }
}
