package net.pixeldreamstudios.journal.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TagPreviewCache {

    private static final Map<ResourceLocation, List<ItemStack>> CACHE = new HashMap<>();

    private TagPreviewCache() {
    }

    public static List<ItemStack> get(ResourceLocation tagId) {
        if (tagId == null) {
            return List.of();
        }
        return CACHE.computeIfAbsent(tagId, TagPreviewCache::resolve);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static List<ItemStack> resolve(ResourceLocation tagId) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        List<ItemStack> stacks = new ArrayList<>();

        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
            ItemStack stack = new ItemStack(holder.value());
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        return List.copyOf(stacks);
    }
}
