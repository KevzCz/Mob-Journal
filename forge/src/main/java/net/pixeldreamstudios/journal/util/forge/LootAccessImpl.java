package net.pixeldreamstudios.journal.util.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;

// Forge exposes these loot internals through the mod's access transformer, so no mixin
// accessors are needed here.
public final class LootAccessImpl {

    public static LootPool[] getPools(LootTable table) {
        return table.pools.toArray(new LootPool[0]);
    }

    public static LootPoolEntryContainer[] getEntries(LootPool pool) {
        return pool.entries;
    }

    public static Item getItem(LootItem entry) {
        return entry.item;
    }

    public static LootPoolEntryContainer[] getChildren(CompositeEntryBase entry) {
        return entry.children;
    }

    public static ResourceLocation getReferencedTable(LootTableReference entry) {
        return entry.name;
    }

    private LootAccessImpl() {
    }
}
