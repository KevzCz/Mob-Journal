package net.pixeldreamstudios.journal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.UnlockMobPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobUnlockTracker {
    private static int tickCounter = 0;
    private static final Set<Identifier> alreadySent = new HashSet<>();

    public static void resetSentMobs() {
        alreadySent.clear();
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        var player = client.player;
        var nearby = client.world.getEntitiesByClass(
                LivingEntity.class,
                new Box(player.getPos(), player.getPos()).expand(JournalConfig.mobCheckRadius),
                e -> e != player && e.isAlive()
        );

        List<Identifier> toUnlock = new ArrayList<>();
        for (var mob : nearby) {
            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                toUnlock.add(id);
            }
        }

        if (toUnlock.isEmpty()) {
            tickCounter = 0;
            return;
        }

        tickCounter++;
        if (tickCounter < JournalConfig.mobCheckInterval) return;
        tickCounter = 0;

        for (Identifier id : toUnlock) {
            UnlockMobPayload.sendToServer(id);
            alreadySent.add(id);
        }
    }
}
