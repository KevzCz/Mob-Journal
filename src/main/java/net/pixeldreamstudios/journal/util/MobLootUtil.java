package net.pixeldreamstudios.journal.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class MobLootUtil {

    public static Map<Identifier, ItemStack> getAllPossibleDrops(LivingEntity mob, ServerWorld world) {
        Map<Identifier, ItemStack> drops = new HashMap<>();

        var lootTableKey = mob.getLootTable();
        var allItems = AllLootTableItems.getAllItemsFromLootTable(lootTableKey, world);

        int index = 0;
        for (Item item : allItems) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && stack.getItem() != Items.AIR && stack.getCount() > 0) {
                drops.put(Identifier.of("journal", String.valueOf(index++)), stack);
            }

        }
        return drops;
    }


}
