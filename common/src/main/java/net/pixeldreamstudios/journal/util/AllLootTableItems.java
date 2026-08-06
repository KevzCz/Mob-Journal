package net.pixeldreamstudios.journal.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;

import java.util.HashSet;
import java.util.Set;

public class AllLootTableItems {

    public static Set<Item> getAllItemsFromLootTable(ResourceLocation key, ServerLevel level) {
        LootTable table = level.getServer().getLootData().getLootTable(key);
        Set<Item> items = new HashSet<>();
        Set<ResourceLocation> visited = new HashSet<>();
        collectItems(key, table, level, items, visited);
        return items;
    }

    private static void collectItems(ResourceLocation id, LootTable table, ServerLevel level, Set<Item> collected, Set<ResourceLocation> visited) {
        if (!visited.add(id)) return;

        LootPool[] pools = LootAccess.getPools(table);
        for (LootPool pool : pools) {
            LootPoolEntryContainer[] entries = LootAccess.getEntries(pool);
            for (LootPoolEntryContainer entry : entries) {
                if (entry instanceof LootTableReference reference) {
                    ResourceLocation childId = LootAccess.getReferencedTable(reference);
                    LootTable subTable = level.getServer().getLootData().getLootTable(childId);
                    collectItems(childId, subTable, level, collected, visited);
                }

                if (entry instanceof LootItem itemEntry) {
                    collected.add(LootAccess.getItem(itemEntry));
                }

                if (entry instanceof CompositeEntryBase groupEntry) {
                    for (LootPoolEntryContainer subEntry : LootAccess.getChildren(groupEntry)) {
                        simulateEntry(subEntry, collected);
                    }
                }
            }
        }
    }

    private static void simulateEntry(LootPoolEntryContainer entry, Set<Item> collected) {
        if (entry instanceof LootItem itemEntry) {
            collected.add(LootAccess.getItem(itemEntry));
        }

        if (entry instanceof CompositeEntryBase groupEntry) {
            for (LootPoolEntryContainer subEntry : LootAccess.getChildren(groupEntry)) {
                simulateEntry(subEntry, collected);
            }
        }
    }
}
