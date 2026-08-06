package net.pixeldreamstudios.journal.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MobEntityCache {
    private static final Map<ResourceLocation, LivingEntity> CACHE = new HashMap<>();

    public static void preload(Collection<ResourceLocation> allIds, Level level) {
        for (ResourceLocation id : allIds) {
            CACHE.computeIfAbsent(id, key -> create(key, level));
        }
    }

    public static LivingEntity get(ResourceLocation id, Level level) {
        return CACHE.computeIfAbsent(id, key -> create(key, level));
    }

    private static LivingEntity create(ResourceLocation id, Level level) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null || !type.canSummon()) return null;
        return SafeEntityFactory.createLiving(type, level);
    }
}
