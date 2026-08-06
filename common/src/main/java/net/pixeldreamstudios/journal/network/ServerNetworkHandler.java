package net.pixeldreamstudios.journal.network;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.util.SafeEntityFactory;
import net.pixeldreamstudios.journal.compat.JournalAccess;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.data.FavoriteMobsData;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.JournalStorage;
import net.pixeldreamstudios.journal.data.MobStatsData;
import net.pixeldreamstudios.journal.events.MobStatEventHandler;
import net.pixeldreamstudios.journal.item.JournalItems;
import net.pixeldreamstudios.journal.util.MobLootUtil;

public class ServerNetworkHandler {

    public static void init() {
        MobStatEventHandler.register();
        registerPlayerLifecycle();
    }

    private static void registerPlayerLifecycle() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            JournalAccess.refreshSlotState(player);

            JournalData journal = JournalStorage.getJournal(player);
            if (JournalConfig.giveJournalOnJoin && !journal.hasReceivedJournal()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(JournalItems.JOURNAL_ITEM.get()));
                journal.setReceivedJournal(true);
            }
            journal.removeBlacklistedMobs();

            FavoriteMobsData favorites = JournalStorage.getFavorites(player);
            NetworkManager.sendToClient(player, new SyncFavoritesPayload(favorites.getFavorites()));
        });

        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wasDeath) -> {
            JournalStorage.getJournal(newPlayer).copyFrom(JournalStorage.getJournal(oldPlayer));
            JournalStorage.getMobStats(newPlayer).copyFrom(JournalStorage.getMobStats(oldPlayer));
            JournalStorage.getFavorites(newPlayer).copyFrom(JournalStorage.getFavorites(oldPlayer));
        });

        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, removalReason) -> {
            JournalAccess.refreshSlotState(player);

            sendJournalSync(player);
            NetworkManager.sendToClient(player,
                    new SyncMobStatsPayload(JournalStorage.getMobStats(player).getAllStats()));
            NetworkManager.sendToClient(player,
                    new SyncFavoritesPayload(JournalStorage.getFavorites(player).getFavorites()));
        });
    }

    public static void sendJournalSync(ServerPlayer player) {
        JournalData journal = JournalStorage.getJournal(player);
        var discoveries = journal.getDiscovered();
        discoveries.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        NetworkManager.sendToClient(player, new SyncJournalPayload(discoveries));
    }

    public static void handleUnlockMob(UnlockMobPayload payload, ServerPlayer player) {
        ResourceLocation id = payload.mobId();
        if (JournalConfig.isBlacklisted(id)) return;

        JournalData journal = JournalStorage.getJournal(player);
        long timestamp = player.getServer().overworld().getGameTime();
        if (journal.unlockMob(id, timestamp)) {
            NetworkManager.sendToClient(player, new DiscoveredMobToastPayload(id));
            NetworkManager.sendToClient(player, new DiscoveredMobPayload(id, timestamp));
            sendJournalSync(player);
        }
    }

    public static void handleClientReady(ServerPlayer player) {
        JournalData journal = JournalStorage.getJournal(player);
        journal.removeBlacklistedMobs();

        sendJournalSync(player);
        MobStatsData mobStats = JournalStorage.getMobStats(player);
        NetworkManager.sendToClient(player, new SyncMobStatsPayload(mobStats.getAllStats()));
    }

    public static void handleOpenJournal(ServerPlayer player) {
        JournalData journal = JournalStorage.getJournal(player);
        NetworkManager.sendToClient(player, new SyncJournalPayload(journal.getDiscovered()));

        MobStatsData stats = JournalStorage.getMobStats(player);
        NetworkManager.sendToClient(player, new SyncMobStatsPayload(stats.getAllStats()));
    }

    public static void handleRequestMobDrops(RequestMobDropsPayload payload, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var type = BuiltInRegistries.ENTITY_TYPE.get(payload.mobId());
        if (type != null && type.canSummon()) {
            LivingEntity mob = SafeEntityFactory.createLiving(type, level);
            if (mob != null) {
                var drops = MobLootUtil.getAllPossibleDrops(mob, level);
                NetworkManager.sendToClient(player, new SyncMobDropsPayload(drops));
            }
        }
    }

    public static void handleToggleFavorite(ToggleFavoritePayload payload, ServerPlayer player) {
        FavoriteMobsData favorites = JournalStorage.getFavorites(player);
        favorites.toggleFavorite(payload.mobId(), payload.favorited());

        NetworkManager.sendToClient(player, new SyncFavoritesPayload(favorites.getFavorites()));
    }
}
