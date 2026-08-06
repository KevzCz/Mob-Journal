package net.pixeldreamstudios.journal.network.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.journal.network.ClientNetworkHandler;
import net.pixeldreamstudios.journal.network.ClientReadyPayload;
import net.pixeldreamstudios.journal.network.DiscoveredMobPayload;
import net.pixeldreamstudios.journal.network.DiscoveredMobToastPayload;
import net.pixeldreamstudios.journal.network.OpenBlacklistScreenPayload;
import net.pixeldreamstudios.journal.network.OpenJournalPayload;
import net.pixeldreamstudios.journal.network.RequestMobDropsPayload;
import net.pixeldreamstudios.journal.network.ServerNetworkHandler;
import net.pixeldreamstudios.journal.network.SyncFavoritesPayload;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;
import net.pixeldreamstudios.journal.network.SyncMobDropsPayload;
import net.pixeldreamstudios.journal.network.SyncMobStatsPayload;
import net.pixeldreamstudios.journal.network.ToggleFavoritePayload;
import net.pixeldreamstudios.journal.network.UnlockMobPayload;

public class NetworkManagerImpl {

    public static void registerCommonPayloads() {
        PayloadTypeRegistry.playS2C().register(SyncJournalPayload.TYPE, SyncJournalPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMobStatsPayload.TYPE, SyncMobStatsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DiscoveredMobToastPayload.TYPE, DiscoveredMobToastPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncMobDropsPayload.TYPE, SyncMobDropsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DiscoveredMobPayload.TYPE, DiscoveredMobPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncFavoritesPayload.TYPE, SyncFavoritesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenBlacklistScreenPayload.TYPE, OpenBlacklistScreenPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(OpenJournalPayload.TYPE, OpenJournalPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UnlockMobPayload.TYPE, UnlockMobPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestMobDropsPayload.TYPE, RequestMobDropsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClientReadyPayload.TYPE, ClientReadyPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleFavoritePayload.TYPE, ToggleFavoritePayload.STREAM_CODEC);

        registerServerReceivers();
    }

    private static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UnlockMobPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        ServerNetworkHandler.handleUnlockMob(payload, context.player())));

        ServerPlayNetworking.registerGlobalReceiver(ClientReadyPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        ServerNetworkHandler.handleClientReady(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(OpenJournalPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        ServerNetworkHandler.handleOpenJournal(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(RequestMobDropsPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        ServerNetworkHandler.handleRequestMobDrops(payload, context.player())));

        ServerPlayNetworking.registerGlobalReceiver(ToggleFavoritePayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        ServerNetworkHandler.handleToggleFavorite(payload, context.player())));
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SyncJournalPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientNetworkHandler.handleSyncJournal(payload)));

        ClientPlayNetworking.registerGlobalReceiver(DiscoveredMobPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientNetworkHandler.handleDiscoveredMob(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SyncMobDropsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientNetworkHandler.handleSyncMobDrops(payload)));

        ClientPlayNetworking.registerGlobalReceiver(DiscoveredMobToastPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientNetworkHandler.handleDiscoveredMobToast(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SyncMobStatsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientNetworkHandler.handleSyncMobStats(payload)));

        ClientPlayNetworking.registerGlobalReceiver(OpenBlacklistScreenPayload.TYPE, (payload, context) ->
                context.client().execute(ClientNetworkHandler::handleOpenBlacklistScreen));

        ClientPlayNetworking.registerGlobalReceiver(SyncFavoritesPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientNetworkHandler.handleSyncFavorites(payload)));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
