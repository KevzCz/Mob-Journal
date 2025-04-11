package net.pixeldreamstudios.journal.util;

import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.mixin.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllLootTableItems {

    public static Set<Item> getAllItemsFromLootTable(RegistryKey<LootTable> key, ServerWorld world) {
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        Set<Item> items = new HashSet<>();
        Set<Identifier> visited = new HashSet<>();
        collectItems(key.getValue(), table, world, items, visited);
        return items;
    }

    private static void collectItems(Identifier id, LootTable table, ServerWorld world, Set<Item> collected, Set<Identifier> visited) {
        if (!visited.add(id)) return;

        List<LootPool> pools = ((LootTableAccessor) table).getPools();
        for (LootPool pool : pools) {
            List<LootPoolEntry> entries = ((LootPoolAccessor) pool).getEntries();
            for (LootPoolEntry entry : entries) {
                if (entry instanceof LootTableEntry lootTableEntry) {
                    var accessor = (LootTableEntryAccessor) lootTableEntry;
                    accessor.getValue().left().ifPresent(lootKey -> {
                        LootTable subTable = world.getServer().getReloadableRegistries().getLootTable(lootKey);
                        collectItems(lootKey.getValue(), subTable, world, collected, visited);
                    });
                }


                if (entry instanceof ItemEntry itemEntry) {
                    RegistryEntry<Item> regItem = ((ItemEntryAccessor) itemEntry).getItem();
                    collected.add(regItem.value());
                }

                if (entry instanceof GroupEntry groupEntry) {
                    List<LootPoolEntry> children = ((CombinedEntryAccessor) groupEntry).getChildren();
                    for (LootPoolEntry subEntry : children) {
                        simulateEntry(subEntry, collected);
                    }
                }

            }
        }
    }

    private static void simulateEntry(LootPoolEntry entry, Set<Item> collected) {
        if (entry instanceof ItemEntry itemEntry) {
            RegistryEntry<Item> regItem = ((ItemEntryAccessor) itemEntry).getItem();
            collected.add(regItem.value());
        }

        if (entry instanceof GroupEntry groupEntry) {
            List<LootPoolEntry> children = ((CombinedEntryAccessor) groupEntry).getChildren();
            for (LootPoolEntry subEntry : children) {
                simulateEntry(subEntry, collected);
            }
        }

    }
}
