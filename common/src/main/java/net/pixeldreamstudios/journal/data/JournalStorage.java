package net.pixeldreamstudios.journal.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public class JournalStorage {

    @ExpectPlatform
    public static JournalData getJournal(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static MobStatsData getMobStats(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static FavoriteMobsData getFavorites(Player player) {
        throw new AssertionError();
    }
}
