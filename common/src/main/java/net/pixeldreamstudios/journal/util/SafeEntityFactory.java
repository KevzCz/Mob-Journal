package net.pixeldreamstudios.journal.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// Some entity constructors touch state that is not ready on a bare instance -- vanilla's
// Sniffer reads its scale attribute before the attribute map exists -- and modded entities
// are worse. Every sample instance the journal builds goes through here so one bad entity
// type cannot abort a whole registry sweep.
public final class SafeEntityFactory {

    public static Entity create(EntityType<?> type, Level level) {
        if (type == null || level == null) return null;
        try {
            return type.create(level);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static LivingEntity createLiving(EntityType<?> type, Level level) {
        return create(type, level) instanceof LivingEntity living ? living : null;
    }

    private SafeEntityFactory() {
    }
}
