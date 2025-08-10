package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.entity.LivingEntity;
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

            ServerPlayNetworking.send(player, new SyncJournalPayload(discoveries));
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

        ServerPlayNetworking.registerGlobalReceiver(UnlockMobPayload.ID, (payload, context) -> {
            context.player().server.execute(() -> {
                var player = context.player();
                var id = payload.mobId();
                var journal = JournalComponents.JOURNAL.get(player);
                if (JournalConfig.isBlacklisted(id)) return;
                if (journal.unlockMob(id)) {
                    ServerPlayNetworking.send(player, new DiscoveredMobToastPayload(id));
                    safeSendJournalSync(player);
                }
            });
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
            ServerPlayNetworking.send(player, new SyncFavoritesPayload(favorites.getFavorites()));
        });


        ServerPlayNetworking.registerGlobalReceiver(ClientReadyPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.player().server.execute(() -> {
                var journal = JournalComponents.JOURNAL.get(player);
                journal.removeBlacklistedMobs();

                safeSendJournalSync(player);
                var mobStats = JournalComponents.MOB_STATS.get(player);
                ServerPlayNetworking.send(player, new SyncMobStatsPayload(mobStats.getAllStats()));
            });
        });


        ServerPlayNetworking.registerGlobalReceiver(OpenJournalPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            var comp = JournalComponents.JOURNAL.get(player);

            ServerPlayNetworking.send(player,
                    new SyncJournalPayload(comp.getDiscovered()));
            var stats = JournalComponents.MOB_STATS.get(player);
            ServerPlayNetworking.send(player, new SyncMobStatsPayload(stats.getAllStats()));
        });


        ServerPlayNetworking.registerGlobalReceiver(RequestMobDropsPayload.ID, (payload, context) -> {
            var player = context.player();
            var world  = player.getServerWorld();
            var type   = Registries.ENTITY_TYPE.get(payload.mobId());
            if (type != null && type.isSummonable()) {
                var entity = type.create(world);
                if (entity instanceof LivingEntity mob) {
                    var drops = net.pixeldreamstudios.journal.util.MobLootUtil.getAllPossibleDrops(mob, world);
                    ServerPlayNetworking.send(player, new SyncMobDropsPayload(drops));
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ToggleFavoritePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            FavoriteMobsComponent comp = JournalComponents.FAVORITES.get(player);
            comp.toggleFavorite(payload.mobId(), payload.favorited());


            ServerPlayNetworking.send(player, new SyncFavoritesPayload(comp.getFavorites()));
        });
    }
}
