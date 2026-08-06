package net.pixeldreamstudios.journal.util.fabric;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.pixeldreamstudios.journal.mixin.CombinedEntryAccessor;
import net.pixeldreamstudios.journal.mixin.ItemEntryAccessor;
import net.pixeldreamstudios.journal.mixin.LootPoolAccessor;
import net.pixeldreamstudios.journal.mixin.LootTableAccessor;
import net.pixeldreamstudios.journal.mixin.LootTableEntryAccessor;

public final class LootAccessImpl {

    public static LootPool[] getPools(LootTable table) {
        return ((LootTableAccessor) table).getPools();
    }

    public static LootPoolEntryContainer[] getEntries(LootPool pool) {
        return ((LootPoolAccessor) pool).getEntries();
    }

    public static Item getItem(LootItem entry) {
        return ((ItemEntryAccessor) entry).getItem();
    }

    public static LootPoolEntryContainer[] getChildren(CompositeEntryBase entry) {
        return ((CombinedEntryAccessor) entry).getChildren();
    }

    public static ResourceLocation getReferencedTable(LootTableReference entry) {
        return ((LootTableEntryAccessor) entry).getId();
    }

    private LootAccessImpl() {
    }
}
