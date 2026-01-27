package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class JournalPayload {
    public static void init() {

        PayloadTypeRegistry.playS2C().register(SyncJournalPayload.ID,      SyncJournalPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMobStatsPayload.ID,    SyncMobStatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DiscoveredMobToastPayload.ID, DiscoveredMobToastPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMobDropsPayload.ID,    SyncMobDropsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DiscoveredMobPayload.ID, DiscoveredMobPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFavoritesPayload.ID, SyncFavoritesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenBlacklistScreenPayload.ID, OpenBlacklistScreenPayload.CODEC);


        PayloadTypeRegistry.playC2S().register(OpenJournalPayload.ID,      OpenJournalPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UnlockMobPayload.ID,       UnlockMobPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestMobDropsPayload.ID, RequestMobDropsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SyncJournalPayload.ID,     SyncJournalPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientReadyPayload.ID,     ClientReadyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleFavoritePayload.ID, ToggleFavoritePayload.CODEC);

    }
}
