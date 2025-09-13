package net.pixeldreamstudios.journal.util;

import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.CombinedEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.mixin.CombinedEntryAccessor;
import net.pixeldreamstudios.journal.mixin.ItemEntryAccessor;
import net.pixeldreamstudios.journal.mixin.LootPoolAccessor;
import net.pixeldreamstudios.journal.mixin.LootTableAccessor;
import net.pixeldreamstudios.journal.mixin.LootTableEntryAccessor;

import java.util.HashSet;
import java.util.Set;

public final class AllLootTableItems {

    private AllLootTableItems() {}

    public static Set<Item> getAllItemsFromLootTable(Identifier key, ServerWorld world) {
        LootTable table = world.getServer().getLootManager().getLootTable(key);
        Set<Item> items = new HashSet<>();
        Set<Identifier> visited = new HashSet<>();
        collectItems(key, table, world, items, visited);
        return items;
    }

    private static void collectItems(Identifier id, LootTable table, ServerWorld world,
                                     Set<Item> collected, Set<Identifier> visited) {
        if (!visited.add(id)) return;

        LootPool[] pools = ((LootTableAccessor) table).getPools();
        for (LootPool pool : pools) {
            LootPoolEntry[] entries = ((LootPoolAccessor) pool).getEntries();
            for (LootPoolEntry entry : entries) {
                if (entry instanceof LootTableEntry lootTableEntry) {
                    Identifier childId = ((LootTableEntryAccessor) lootTableEntry).getId();
                    LootTable subTable = world.getServer().getLootManager().getLootTable(childId);
                    collectItems(childId, subTable, world, collected, visited);
                } else if (entry instanceof ItemEntry itemEntry) {
                    Item item = ((ItemEntryAccessor) itemEntry).getItem();
                    collected.add(item);
                } else if (entry instanceof CombinedEntry combined) {
                    LootPoolEntry[] children = ((CombinedEntryAccessor) combined).getChildren();
                    for (LootPoolEntry child : children) {
                        simulateEntry(child, collected, world, visited);
                    }
                }
            }
        }
    }

    private static void simulateEntry(LootPoolEntry entry, Set<Item> collected,
                                      ServerWorld world, Set<Identifier> visited) {
        if (entry instanceof ItemEntry itemEntry) {
            Item item = ((ItemEntryAccessor) itemEntry).getItem();
            collected.add(item);
        } else if (entry instanceof LootTableEntry lootTableEntry) {
            Identifier childId = ((LootTableEntryAccessor) lootTableEntry).getId();
            LootTable subTable = world.getServer().getLootManager().getLootTable(childId);
            collectItems(childId, subTable, world, collected, visited);
        } else if (entry instanceof CombinedEntry combined) {
            LootPoolEntry[] children = ((CombinedEntryAccessor) combined).getChildren();
            for (LootPoolEntry child : children) {
                simulateEntry(child, collected, world, visited);
            }
        }
    }
}
