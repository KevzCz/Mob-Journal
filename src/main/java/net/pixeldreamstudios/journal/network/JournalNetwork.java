// src/main/java/net/pixeldreamstudios/journal/JournalNetwork.java
package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class JournalNetwork {
    public static void init() {
        // ─── Clientbound (S2C) ───
        PayloadTypeRegistry.playS2C().register(SyncJournalPayload.ID,      SyncJournalPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMobStatsPayload.ID,    SyncMobStatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DiscoveredMobToastPayload.ID, DiscoveredMobToastPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMobDropsPayload.ID,    SyncMobDropsPayload.CODEC);

        // ─── Serverbound (C2S) ───
        PayloadTypeRegistry.playC2S().register(OpenJournalPayload.ID,      OpenJournalPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UnlockMobPayload.ID,       UnlockMobPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestMobDropsPayload.ID, RequestMobDropsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SyncJournalPayload.ID,     SyncJournalPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientReadyPayload.ID,     ClientReadyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetMobIdPayload.ID,        SetMobIdPayload.CODEC);
    }
}
