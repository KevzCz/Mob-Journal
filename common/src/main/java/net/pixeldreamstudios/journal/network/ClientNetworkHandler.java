package net.pixeldreamstudios.journal.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import net.pixeldreamstudios.journal.client.gui.BlacklistScreen;
import net.pixeldreamstudios.journal.client.gui.JournalScreen;
import net.pixeldreamstudios.journal.client.gui.MobDetailsScreen;
import net.pixeldreamstudios.journal.client.toast.MobDiscoveredToast;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.util.ArrayList;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ClientNetworkHandler {

    public static void handleSyncJournal(SyncJournalPayload payload) {
        Minecraft client = Minecraft.getInstance();

        JournalClientData.DISCOVERED.clear();
        JournalClientData.DISCOVERED_TIME.clear();

        for (Map.Entry<ResourceLocation, Long> e : payload.discoveries().entrySet()) {
            JournalClientData.DISCOVERED.add(e.getKey());
            JournalClientData.DISCOVERED_TIME.put(e.getKey(), e.getValue());
        }

        MobEntityCache.preload(payload.discoveries().keySet(), client.level);

        if (client.screen instanceof JournalScreen screen) {
            screen.updateDiscoveredMobs();
        }

        MobUnlockTracker.resetSentMobs();

        if (JournalClientData.shouldOpenJournalScreen) {
            if (client.player != null) {
                client.player.playSound(
                        SoundEvents.BOOK_PAGE_TURN,
                        1f, 1f + (float) (Math.random() * 0.2 - 0.1)
                );
            }
            client.setScreen(new JournalScreen());
            JournalClientData.shouldOpenJournalScreen = false;
        }
    }

    public static void handleDiscoveredMob(DiscoveredMobPayload payload) {
        Minecraft client = Minecraft.getInstance();

        JournalClientData.DISCOVERED.add(payload.mobId());
        JournalClientData.DISCOVERED_TIME.put(payload.mobId(), payload.timestamp());

        if (client.screen instanceof JournalScreen screen) {
            screen.updateDiscoveredMobs();
        }
    }

    public static void handleSyncMobDrops(SyncMobDropsPayload payload) {
        Minecraft client = Minecraft.getInstance();

        JournalClientData.LAST_DROPS = new ArrayList<>(payload.drops().values());
        if (client.screen instanceof MobDetailsScreen screen) {
            screen.rebuildWithDrops();
        }
    }

    public static void handleDiscoveredMobToast(DiscoveredMobToastPayload payload) {
        ResourceLocation mobId = payload.mobId();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(mobId);
        MobDiscoveredToast.show(
                type,
                Component.translatable("toast.journal.mob_discovered"),
                Component.translatable(type.getDescriptionId())
        );
    }

    public static void handleSyncMobStats(SyncMobStatsPayload payload) {
        JournalClientData.MOB_STATS.clear();
        JournalClientData.MOB_STATS.putAll(payload.stats());
    }

    public static void handleOpenBlacklistScreen() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new BlacklistScreen(client.screen));
    }

    public static void handleSyncFavorites(SyncFavoritesPayload payload) {
        Minecraft client = Minecraft.getInstance();

        JournalClientData.FAVORITE_MOBS.clear();
        JournalClientData.FAVORITE_MOBS.addAll(payload.favoriteMobs());

        if (client.screen instanceof JournalScreen screen) {
            screen.updateDiscoveredMobs();
        }
    }
}
