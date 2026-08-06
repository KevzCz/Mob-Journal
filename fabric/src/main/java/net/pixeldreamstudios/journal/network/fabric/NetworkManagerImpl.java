package net.pixeldreamstudios.journal.network.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.journal.network.ClientNetworkHandler;
import net.pixeldreamstudios.journal.network.ClientReadyPayload;
import net.pixeldreamstudios.journal.network.DiscoveredMobPayload;
import net.pixeldreamstudios.journal.network.DiscoveredMobToastPayload;
import net.pixeldreamstudios.journal.network.JournalPayload;
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
        registerServerReceivers();
    }

    private static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UnlockMobPayload.ID,
                (server, player, handler, buf, sender) -> {
                    UnlockMobPayload payload = UnlockMobPayload.read(buf);
                    server.execute(() -> ServerNetworkHandler.handleUnlockMob(payload, player));
                });

        ServerPlayNetworking.registerGlobalReceiver(ClientReadyPayload.ID,
                (server, player, handler, buf, sender) ->
                        server.execute(() -> ServerNetworkHandler.handleClientReady(player)));

        ServerPlayNetworking.registerGlobalReceiver(OpenJournalPayload.ID,
                (server, player, handler, buf, sender) ->
                        server.execute(() -> ServerNetworkHandler.handleOpenJournal(player)));

        ServerPlayNetworking.registerGlobalReceiver(RequestMobDropsPayload.ID,
                (server, player, handler, buf, sender) -> {
                    RequestMobDropsPayload payload = RequestMobDropsPayload.read(buf);
                    server.execute(() -> ServerNetworkHandler.handleRequestMobDrops(payload, player));
                });

        ServerPlayNetworking.registerGlobalReceiver(ToggleFavoritePayload.ID,
                (server, player, handler, buf, sender) -> {
                    ToggleFavoritePayload payload = ToggleFavoritePayload.read(buf);
                    server.execute(() -> ServerNetworkHandler.handleToggleFavorite(payload, player));
                });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SyncJournalPayload.ID,
                (client, handler, buf, sender) -> {
                    SyncJournalPayload payload = SyncJournalPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleSyncJournal(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(DiscoveredMobPayload.ID,
                (client, handler, buf, sender) -> {
                    DiscoveredMobPayload payload = DiscoveredMobPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleDiscoveredMob(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(SyncMobDropsPayload.ID,
                (client, handler, buf, sender) -> {
                    SyncMobDropsPayload payload = SyncMobDropsPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleSyncMobDrops(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(DiscoveredMobToastPayload.ID,
                (client, handler, buf, sender) -> {
                    DiscoveredMobToastPayload payload = DiscoveredMobToastPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleDiscoveredMobToast(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(SyncMobStatsPayload.ID,
                (client, handler, buf, sender) -> {
                    SyncMobStatsPayload payload = SyncMobStatsPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleSyncMobStats(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(OpenBlacklistScreenPayload.ID,
                (client, handler, buf, sender) ->
                        client.execute(ClientNetworkHandler::handleOpenBlacklistScreen));

        ClientPlayNetworking.registerGlobalReceiver(SyncFavoritesPayload.ID,
                (client, handler, buf, sender) -> {
                    SyncFavoritesPayload payload = SyncFavoritesPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleSyncFavorites(payload));
                });
    }

    public static void sendToServer(JournalPayload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ClientPlayNetworking.send(payload.id(), buf);
    }

    public static void sendToClient(ServerPlayer player, JournalPayload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, payload.id(), buf);
    }
}
