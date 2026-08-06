package net.pixeldreamstudios.journal.network.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.pixeldreamstudios.journal.Journal;
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

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Journal.MOD_ID);

        registrar.playToServer(UnlockMobPayload.TYPE, UnlockMobPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworkHandler.handleUnlockMob(payload, (ServerPlayer) context.player())));

        registrar.playToServer(ClientReadyPayload.TYPE, ClientReadyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworkHandler.handleClientReady((ServerPlayer) context.player())));

        registrar.playToServer(OpenJournalPayload.TYPE, OpenJournalPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworkHandler.handleOpenJournal((ServerPlayer) context.player())));

        registrar.playToServer(RequestMobDropsPayload.TYPE, RequestMobDropsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworkHandler.handleRequestMobDrops(payload, (ServerPlayer) context.player())));

        registrar.playToServer(ToggleFavoritePayload.TYPE, ToggleFavoritePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ServerNetworkHandler.handleToggleFavorite(payload, (ServerPlayer) context.player())));

        registrar.playToClient(SyncJournalPayload.TYPE, SyncJournalPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientNetworkHandler.handleSyncJournal(payload)));

        registrar.playToClient(SyncMobStatsPayload.TYPE, SyncMobStatsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientNetworkHandler.handleSyncMobStats(payload)));

        registrar.playToClient(DiscoveredMobToastPayload.TYPE, DiscoveredMobToastPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientNetworkHandler.handleDiscoveredMobToast(payload)));

        registrar.playToClient(SyncMobDropsPayload.TYPE, SyncMobDropsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientNetworkHandler.handleSyncMobDrops(payload)));

        registrar.playToClient(DiscoveredMobPayload.TYPE, DiscoveredMobPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientNetworkHandler.handleDiscoveredMob(payload)));

        registrar.playToClient(SyncFavoritesPayload.TYPE, SyncFavoritesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientNetworkHandler.handleSyncFavorites(payload)));

        registrar.playToClient(OpenBlacklistScreenPayload.TYPE, OpenBlacklistScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        ClientNetworkHandler::handleOpenBlacklistScreen));
    }

    public static void registerCommonPayloads() {
    }

    public static void registerClientReceivers() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
