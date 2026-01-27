package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.client.JournalClient;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import net.pixeldreamstudios.journal.client.gui.BlacklistScreen;
import net.pixeldreamstudios.journal.client.gui.JournalScreen;
import net.pixeldreamstudios.journal.client.gui.MobDetailsScreen;
import net.pixeldreamstudios.journal.client.toast.MobDiscoveredToast;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.item.JournalItems;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.util.Map;

public class JournalClientNetwork {
    public static void init() {
        SyncJournalPayload.registerS2C(payload -> {
            var mc = MinecraftClient.getInstance();

            JournalClientData.DISCOVERED.clear();
            JournalClientData.DISCOVERED_TIME.clear();
            for (Map.Entry<Identifier, Long> e : payload.discoveries().entrySet()) {
                JournalClientData.DISCOVERED.add(e.getKey());
                JournalClientData.DISCOVERED_TIME.put(e.getKey(), e.getValue());
            }

            MobEntityCache.preload(payload.discoveries().keySet(), mc.world);

            if (mc.currentScreen instanceof JournalScreen screen) {
                screen.updateDiscoveredMobs();
            }

            MobUnlockTracker.resetSentMobs();

            if (JournalClientData.shouldOpenJournalScreen) {
                if (mc.player != null) {
                    mc.player.playSound(
                            net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN,
                            1f, 1f + (float)(Math.random() * 0.2 - 0.1)
                    );
                }
                mc.setScreen(new JournalScreen());
                JournalClientData.shouldOpenJournalScreen = false;
            }
        });

        DiscoveredMobPayload.registerS2C(payload -> {
            var mc = MinecraftClient.getInstance();
            JournalClientData.DISCOVERED.add(payload.mobId());
            JournalClientData.DISCOVERED_TIME.put(payload.mobId(), payload.timestamp());
            if (mc.currentScreen instanceof JournalScreen screen) {
                screen.updateDiscoveredMobs();
            }
        });

        SyncMobDropsPayload.registerS2C(payload -> {
            var mc = MinecraftClient.getInstance();
            JournalClientData.LAST_DROPS = new java.util.ArrayList<>(payload.drops().values());
            if (mc.currentScreen instanceof MobDetailsScreen screen) {
                screen.rebuildWithDrops();
            }
        });

        DiscoveredMobToastPayload.registerS2C(payload -> {
            Identifier mobId = payload.mobId();
            EntityType<?> type = Registries.ENTITY_TYPE.get(mobId);
            MobDiscoveredToast.show(
                    type,
                    Text.translatable("toast.journal.mob_discovered"),
                    Text.translatable(type.getTranslationKey())
            );
        });

        SyncMobStatsPayload.registerS2C(payload -> {
            JournalClientData.MOB_STATS.clear();
            JournalClientData.MOB_STATS.putAll(payload.stats());
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientReadyPayload.sendToServer();
        });
        OpenBlacklistScreenPayload.registerS2C(payload -> {
            var mc = MinecraftClient.getInstance();
            mc.setScreen(new BlacklistScreen(mc.currentScreen));
        });
        SyncFavoritesPayload.registerS2C(payload -> {
            var mc = MinecraftClient.getInstance();
            JournalClientData.FAVORITE_MOBS.clear();
            JournalClientData.FAVORITE_MOBS.addAll(payload.favoriteMobs());
            if (mc.currentScreen instanceof JournalScreen screen) {
                screen.updateDiscoveredMobs();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (JournalClient.openJournalKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    boolean hasJournal = client.player.getInventory()
                            .contains(new ItemStack(JournalItems.JOURNAL_ITEM));
                    boolean needsBook = JournalConfig.requireJournalInInventory;

                    if (!needsBook || hasJournal) {
                        JournalClientData.shouldOpenJournalScreen = true;
                        OpenJournalPayload.sendToServer();
                    } else {
                        client.player.sendMessage(
                                Text.literal("§cYou need to carry your Journal to open it!"),
                                true
                        );
                    }
                }
            }
        });
    }
}
