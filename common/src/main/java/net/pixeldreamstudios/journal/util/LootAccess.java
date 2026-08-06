package net.pixeldreamstudios.journal.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;

public final class LootAccess {

    @ExpectPlatform
    public static LootPool[] getPools(LootTable table) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static LootPoolEntryContainer[] getEntries(LootPool pool) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Item getItem(LootItem entry) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static LootPoolEntryContainer[] getChildren(CompositeEntryBase entry) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ResourceLocation getReferencedTable(LootTableReference entry) {
        throw new AssertionError();
    }

    private LootAccess() {
    }
}
