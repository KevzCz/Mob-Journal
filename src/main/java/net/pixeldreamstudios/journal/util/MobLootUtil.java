package net.pixeldreamstudios.journal.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobLootUtil {

    public static Map<Identifier, ItemStack> getAllPossibleDrops(LivingEntity mob, ServerWorld world) {
        Map<Identifier, ItemStack> drops = new HashMap<>();

        var lootTableKey = mob.getLootTable();
        var allItems = AllLootTableItems.getAllItemsFromLootTable(lootTableKey, world);

        int index = 0;
        for (Item item : allItems) {
            drops.put(Identifier.of("journal", String.valueOf(index++)), new ItemStack(item));
        }

        return drops;
    }


}
