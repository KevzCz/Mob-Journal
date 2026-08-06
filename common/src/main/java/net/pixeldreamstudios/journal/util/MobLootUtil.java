package net.pixeldreamstudios.journal.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.pixeldreamstudios.journal.Journal;

import java.util.HashMap;
import java.util.Map;

public class MobLootUtil {

    public static Map<ResourceLocation, ItemStack> getAllPossibleDrops(LivingEntity mob, ServerLevel level) {
        Map<ResourceLocation, ItemStack> drops = new HashMap<>();

        var lootTableKey = mob.getLootTable();
        var allItems = AllLootTableItems.getAllItemsFromLootTable(lootTableKey, level);

        int index = 0;
        for (Item item : allItems) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && stack.getItem() != Items.AIR && stack.getCount() > 0) {
                drops.put(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, String.valueOf(index++)), stack);
            }
        }
        return drops;
    }
}
