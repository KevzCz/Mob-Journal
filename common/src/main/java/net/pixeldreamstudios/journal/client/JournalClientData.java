package net.pixeldreamstudios.journal.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.data.MobStat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JournalClientData {
    public static final Set<ResourceLocation> DISCOVERED = new HashSet<>();
    public static final Map<ResourceLocation, MobStat> MOB_STATS = new HashMap<>();
    public static List<ItemStack> LAST_DROPS = new ArrayList<>();
    public static final Map<ResourceLocation, Long> DISCOVERED_TIME = new LinkedHashMap<>();
    public static boolean shouldOpenJournalScreen = false;
    public static final Set<ResourceLocation> FAVORITE_MOBS = new HashSet<>();
}
