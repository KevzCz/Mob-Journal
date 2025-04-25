package net.pixeldreamstudios.journal.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MobEntityCache {
    private static final Map<Identifier, LivingEntity> CACHE = new HashMap<>();

    /**  Call this once when you first get the full list from the server. */
    public static void preload(Collection<Identifier> allIds, World world) {
        for (Identifier id : allIds) {
            CACHE.computeIfAbsent(id, key -> {
                var type = Registries.ENTITY_TYPE.get(key);
                return (type != null && type.isSummonable())
                        ? (LivingEntity)type.create(world)
                        : null;
            });
        }
    }

    /**  Later, when the GUI needs one: */
    public static LivingEntity get(Identifier id, World world) {
        return CACHE.computeIfAbsent(id, key -> {
            var type = Registries.ENTITY_TYPE.get(key);
            return (type != null && type.isSummonable())
                    ? (LivingEntity)type.create(world)
                    : null;
        });
    }
}
