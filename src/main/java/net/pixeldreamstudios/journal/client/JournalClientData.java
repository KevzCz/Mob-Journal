package net.pixeldreamstudios.journal.client;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.MobStat;

import java.util.*;

public class JournalClientData {
    public static final Set<Identifier> DISCOVERED = new HashSet<>();
    public static final Map<Identifier, MobStat> MOB_STATS = new HashMap<>();
    public static List<ItemStack> LAST_DROPS = new ArrayList<>();
    public static final Map<Identifier, Long> DISCOVERED_TIME = new LinkedHashMap<>();

    public static boolean shouldOpenJournalScreen = false;
}
