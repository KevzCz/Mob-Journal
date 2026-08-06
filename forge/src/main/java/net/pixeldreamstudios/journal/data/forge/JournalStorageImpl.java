package net.pixeldreamstudios.journal.data.forge;

import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.journal.data.FavoriteMobsData;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.JournalDataHolder;
import net.pixeldreamstudios.journal.data.MobStatsData;

public class JournalStorageImpl {

    public static JournalData getJournal(Player player) {
        return ((JournalDataHolder) player).journal$getJournalData();
    }

    public static MobStatsData getMobStats(Player player) {
        return ((JournalDataHolder) player).journal$getMobStatsData();
    }

    public static FavoriteMobsData getFavorites(Player player) {
        return ((JournalDataHolder) player).journal$getFavoriteMobsData();
    }
}
