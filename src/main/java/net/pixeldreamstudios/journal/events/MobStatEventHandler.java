package net.pixeldreamstudios.journal.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.data.MobStatTrackerComponent;

public class MobStatEventHandler {

    public static void register() {
        // 🗡️ Kill tracking
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (entity instanceof ServerPlayerEntity player && killed.getType().isSummonable()) {
                Identifier id = EntityType.getId(killed.getType());
                JournalComponents.MOB_STATS.get(player).incrementKills(id);
            }
        });

        // 💀 Death tracking (player dies to mob)
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, victim) -> {
            if (victim instanceof ServerPlayerEntity player && killer instanceof LivingEntity mob) {
                Identifier id = EntityType.getId(mob.getType());
                JournalComponents.MOB_STATS.get(player).incrementDeaths(id);
            }
        });

    }
}
