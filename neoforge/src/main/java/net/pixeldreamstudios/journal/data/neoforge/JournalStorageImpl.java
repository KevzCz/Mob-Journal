package net.pixeldreamstudios.journal.data.neoforge;

import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.journal.data.FavoriteMobsData;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.MobStatsData;
import net.pixeldreamstudios.journal.neoforge.JournalAttachments;

public class JournalStorageImpl {

    public static JournalData getJournal(Player player) {
        return player.getData(JournalAttachments.JOURNAL.get());
    }

    public static MobStatsData getMobStats(Player player) {
        return player.getData(JournalAttachments.MOB_STATS.get());
    }

    public static FavoriteMobsData getFavorites(Player player) {
        return player.getData(JournalAttachments.FAVORITES.get());
    }
}
