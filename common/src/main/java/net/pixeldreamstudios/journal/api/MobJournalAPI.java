package net.pixeldreamstudios.journal.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.journal.compat.JournalAccess;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.JournalStorage;
import net.pixeldreamstudios.journal.data.MobStat;
import net.pixeldreamstudios.journal.data.MobStatsData;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class MobJournalAPI {

    public static boolean hasDiscovered(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return false;
        return JournalStorage.getJournal(player).getDiscovered().containsKey(mobId);
    }

    public static Set<ResourceLocation> getDiscoveredMobs(Player player) {
        if (player == null) return Collections.emptySet();
        return Collections.unmodifiableSet(JournalStorage.getJournal(player).getDiscovered().keySet());
    }

    public static Map<ResourceLocation, Long> getDiscoveriesWithTime(Player player) {
        if (player == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(JournalStorage.getJournal(player).getDiscovered());
    }

    public static int getDiscoveredCount(Player player) {
        if (player == null) return 0;
        return JournalStorage.getJournal(player).getDiscovered().size();
    }

    public static long getDiscoveryTime(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return -1L;
        Long time = JournalStorage.getJournal(player).getDiscovered().get(mobId);
        return time == null ? -1L : time;
    }

    public static int getKills(Player player, ResourceLocation mobId) {
        return getStat(player, mobId).kills();
    }

    public static int getDeaths(Player player, ResourceLocation mobId) {
        return getStat(player, mobId).deaths();
    }

    public static MobStat getStat(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return new MobStat(0, 0);
        MobStatsData stats = JournalStorage.getMobStats(player);
        return stats.get(mobId);
    }

    public static Set<ResourceLocation> getFavorites(Player player) {
        if (player == null) return Collections.emptySet();
        return Collections.unmodifiableSet(JournalStorage.getFavorites(player).getFavorites());
    }

    public static boolean isFavorite(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return false;
        return JournalStorage.getFavorites(player).getFavorites().contains(mobId);
    }

    public static boolean hasJournal(Player player) {
        return JournalAccess.hasJournal(player);
    }

    public static boolean discoverMob(Player player, ResourceLocation mobId, long timestamp) {
        if (player == null || mobId == null) return false;
        JournalData journal = JournalStorage.getJournal(player);
        return journal.unlockMob(mobId, timestamp);
    }

    private MobJournalAPI() {
    }
}
