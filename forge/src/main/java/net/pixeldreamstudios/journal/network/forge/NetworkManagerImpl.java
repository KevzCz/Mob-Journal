package net.pixeldreamstudios.journal.network.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.pixeldreamstudios.journal.Journal;
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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkManagerImpl {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Journal.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void registerCommonPayloads() {
        registerC2S(UnlockMobPayload.class, UnlockMobPayload::read, ServerNetworkHandler::handleUnlockMob);
        registerC2S(ClientReadyPayload.class, ClientReadyPayload::read,
                (payload, player) -> ServerNetworkHandler.handleClientReady(player));
        registerC2S(OpenJournalPayload.class, OpenJournalPayload::read,
                (payload, player) -> ServerNetworkHandler.handleOpenJournal(player));
        registerC2S(RequestMobDropsPayload.class, RequestMobDropsPayload::read,
                ServerNetworkHandler::handleRequestMobDrops);
        registerC2S(ToggleFavoritePayload.class, ToggleFavoritePayload::read,
                ServerNetworkHandler::handleToggleFavorite);

        registerS2C(SyncJournalPayload.class, SyncJournalPayload::read, ClientNetworkHandler::handleSyncJournal);
        registerS2C(SyncMobStatsPayload.class, SyncMobStatsPayload::read, ClientNetworkHandler::handleSyncMobStats);
        registerS2C(DiscoveredMobToastPayload.class, DiscoveredMobToastPayload::read,
                ClientNetworkHandler::handleDiscoveredMobToast);
        registerS2C(SyncMobDropsPayload.class, SyncMobDropsPayload::read, ClientNetworkHandler::handleSyncMobDrops);
        registerS2C(DiscoveredMobPayload.class, DiscoveredMobPayload::read, ClientNetworkHandler::handleDiscoveredMob);
        registerS2C(SyncFavoritesPayload.class, SyncFavoritesPayload::read, ClientNetworkHandler::handleSyncFavorites);
        registerS2C(OpenBlacklistScreenPayload.class, OpenBlacklistScreenPayload::read,
                payload -> ClientNetworkHandler.handleOpenBlacklistScreen());
    }

    public static void registerClientReceivers() {
    }

    private static <T extends JournalPayload> void registerC2S(Class<T> type,
                                                               Function<FriendlyByteBuf, T> decoder,
                                                               BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buf) -> payload.write(buf))
                .decoder(decoder::apply)
                .consumerMainThread((payload, context) -> {
                    NetworkEvent.Context ctx = context.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        handler.accept(payload, player);
                    }
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    private static <T extends JournalPayload> void registerS2C(Class<T> type,
                                                               Function<FriendlyByteBuf, T> decoder,
                                                               Consumer<T> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buf) -> payload.write(buf))
                .decoder(decoder::apply)
                .consumerMainThread((payload, context) -> {
                    NetworkEvent.Context ctx = context.get();
                    handler.accept(payload);
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    public static void sendToServer(JournalPayload payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToClient(ServerPlayer player, JournalPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with((Supplier<ServerPlayer>) () -> player), payload);
    }
}
