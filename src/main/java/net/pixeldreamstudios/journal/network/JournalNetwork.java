package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.data.FavoriteMobsComponent;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.events.MobStatEventHandler;
import net.pixeldreamstudios.journal.item.JournalItems;

public class JournalNetwork {
    private static void safeSendJournalSync(ServerPlayerEntity player) {
        try {
            var journal = JournalComponents.JOURNAL.get(player);
            var discoveries = journal.getDiscovered();
            discoveries.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
            SyncJournalPayload.sendToClient(player, discoveries);
        } catch (Exception e) {
            player.server.execute(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                safeSendJournalSync(player);
            });
        }
    }

    public static void init() {
        MobStatEventHandler.register();

        UnlockMobPayload.registerC2S((player, payload) -> {
            var id = payload.mobId();
            var journal = JournalComponents.JOURNAL.get(player);
            if (JournalConfig.isBlacklisted(id)) return;
            if (journal.unlockMob(id)) {
                DiscoveredMobToastPayload.sendToClient(player, id);
                safeSendJournalSync(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            var journal = JournalComponents.JOURNAL.get(player);
            if (!journal.hasReceivedJournal()) {
                player.giveItemStack(new ItemStack(JournalItems.JOURNAL_ITEM));
                journal.setReceivedJournal(true);
            }
            journal.removeBlacklistedMobs();
            var favorites = JournalComponents.FAVORITES.get(player);
            SyncFavoritesPayload.sendToClient(player, favorites.getFavorites());
        });

        ClientReadyPayload.registerC2S((player, payload) -> {
            var journal = JournalComponents.JOURNAL.get(player);
            journal.removeBlacklistedMobs();
            safeSendJournalSync(player);
            var mobStats = JournalComponents.MOB_STATS.get(player);
            SyncMobStatsPayload.sendToClient(player, mobStats.getAllStats());
        });

        OpenJournalPayload.registerC2S((player, payload) -> {
            var comp = JournalComponents.JOURNAL.get(player);
            SyncJournalPayload.sendToClient(player, comp.getDiscovered());
            var stats = JournalComponents.MOB_STATS.get(player);
            SyncMobStatsPayload.sendToClient(player, stats.getAllStats());
        });

        RequestMobDropsPayload.registerC2S((player, payload) -> {
            var world = player.getServerWorld();
            var type = Registries.ENTITY_TYPE.get(payload.mobId());
            if (type != null && type.isSummonable()) {
                var entity = type.create(world);
                if (entity instanceof LivingEntity mob) {
                    var drops = net.pixeldreamstudios.journal.util.MobLootUtil.getAllPossibleDrops(mob, world);
                    SyncMobDropsPayload.sendToClient(player, drops);
                }
            }
        });

        ToggleFavoritePayload.registerC2S((player, payload) -> {
            FavoriteMobsComponent comp = JournalComponents.FAVORITES.get(player);
            comp.toggleFavorite(payload.mobId(), payload.favorited());
            SyncFavoritesPayload.sendToClient(player, comp.getFavorites());
        });
    }
}
