package net.pixeldreamstudios.journal.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.pixeldreamstudios.journal.mixin.CombinedEntryAccessor;
import net.pixeldreamstudios.journal.mixin.ItemEntryAccessor;
import net.pixeldreamstudios.journal.mixin.LootPoolAccessor;
import net.pixeldreamstudios.journal.mixin.LootTableAccessor;
import net.pixeldreamstudios.journal.mixin.LootTableEntryAccessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllLootTableItems {

    public static Set<Item> getAllItemsFromLootTable(ResourceKey<LootTable> key, ServerLevel level) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        Set<Item> items = new HashSet<>();
        Set<ResourceLocation> visited = new HashSet<>();
        collectItems(key.location(), table, level, items, visited);
        return items;
    }

    private static void collectItems(ResourceLocation id, LootTable table, ServerLevel level, Set<Item> collected, Set<ResourceLocation> visited) {
        if (!visited.add(id)) return;

        List<LootPool> pools = ((LootTableAccessor) table).getPools();
        for (LootPool pool : pools) {
            List<LootPoolEntryContainer> entries = ((LootPoolAccessor) pool).getEntries();
            for (LootPoolEntryContainer entry : entries) {
                if (entry instanceof NestedLootTable nested) {
                    var accessor = (LootTableEntryAccessor) nested;
                    accessor.getValue().left().ifPresent(lootKey -> {
                        LootTable subTable = level.getServer().reloadableRegistries().getLootTable(lootKey);
                        collectItems(lootKey.location(), subTable, level, collected, visited);
                    });
                }

                if (entry instanceof LootItem itemEntry) {
                    Holder<Item> regItem = ((ItemEntryAccessor) itemEntry).getItem();
                    collected.add(regItem.value());
                }

                if (entry instanceof CompositeEntryBase groupEntry) {
                    List<LootPoolEntryContainer> children = ((CombinedEntryAccessor) groupEntry).getChildren();
                    for (LootPoolEntryContainer subEntry : children) {
                        simulateEntry(subEntry, collected);
                    }
                }
            }
        }
    }

    private static void simulateEntry(LootPoolEntryContainer entry, Set<Item> collected) {
        if (entry instanceof LootItem itemEntry) {
            Holder<Item> regItem = ((ItemEntryAccessor) itemEntry).getItem();
            collected.add(regItem.value());
        }

        if (entry instanceof CompositeEntryBase groupEntry) {
            List<LootPoolEntryContainer> children = ((CombinedEntryAccessor) groupEntry).getChildren();
            for (LootPoolEntryContainer subEntry : children) {
                simulateEntry(subEntry, collected);
            }
        }
    }
}
