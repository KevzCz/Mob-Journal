// src/main/java/net/pixeldreamstudios/journal/JournalNetwork.java
package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.entity.LivingEntity;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.events.MobStatEventHandler;
import net.pixeldreamstudios.journal.item.JournalItems;

public class JournalNetwork {
    public static void init() {
        // ─── Mob stat handler ───
        MobStatEventHandler.register();

        // ─── Unlock-mob packet ───
        ServerPlayNetworking.registerGlobalReceiver(UnlockMobPayload.ID, (payload, context) -> {
            context.player().server.execute(() -> {
                var player = context.player();
                var id = payload.mobId();
                var journal = JournalComponents.JOURNAL.get(player);
                if (JournalConfig.isBlacklisted(id)) return;
                if (journal.unlockMob(id)) {
                    ServerPlayNetworking.send(player, new DiscoveredMobToastPayload(id));
                    // now send the full id→timestamp map instead of just a list
                    ServerPlayNetworking.send(player,
                            new SyncJournalPayload(journal.getDiscovered()));
                }
            });
        });

        // ─── On player join ───
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            var journal = JournalComponents.JOURNAL.get(player);
            if (!journal.hasReceivedJournal()) {
                player.giveItemStack(new ItemStack(JournalItems.JOURNAL_ITEM));
                journal.setReceivedJournal(true);
            }
            journal.removeBlacklistedMobs();
        });

        // ─── Client-ready sync ───
        ServerPlayNetworking.registerGlobalReceiver(ClientReadyPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.player().server.execute(() -> {
                var journal = JournalComponents.JOURNAL.get(player);
                journal.removeBlacklistedMobs();
                // likewise here
                ServerPlayNetworking.send(player,
                        new SyncJournalPayload(journal.getDiscovered()));
                var mobStats = JournalComponents.MOB_STATS.get(player);
                ServerPlayNetworking.send(player, new SyncMobStatsPayload(mobStats.getAllStats()));
            });
        });

        // ─── Open-journal request ───
        ServerPlayNetworking.registerGlobalReceiver(OpenJournalPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            var comp = JournalComponents.JOURNAL.get(player);
            // …and here
            ServerPlayNetworking.send(player,
                    new SyncJournalPayload(comp.getDiscovered()));
            var stats = JournalComponents.MOB_STATS.get(player);
            ServerPlayNetworking.send(player, new SyncMobStatsPayload(stats.getAllStats()));
        });

        // ─── Mob-drops preview ───
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
    }
}
