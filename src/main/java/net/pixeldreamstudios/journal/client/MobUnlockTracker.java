package net.pixeldreamstudios.journal.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.UnlockMobPayload;

import java.util.*;

public class MobUnlockTracker {
    private static int tickCounter = 0;
    private static final Set<Identifier> alreadySent = new HashSet<>();


    private static final Long LAST_HIT_EXPIRY_MS = 5000L;
    private static final Map<Integer, Long> recentHits = new HashMap<>();


    private static final Long LAST_INTERACT_EXPIRY_MS = 5000L;
    private static final Map<Integer, Long> recentInteracts = new HashMap<>();

    public static void resetSentMobs() {
        alreadySent.clear();
        recentHits.clear();
        recentInteracts.clear();
    }

    public static void tick() {
        var client = MinecraftClient.getInstance();
        var world = client.world;
        var player = client.player;
        if (player == null || world == null) return;


        long now = System.currentTimeMillis();
        recentHits.entrySet().removeIf(e -> now - e.getValue() > LAST_HIT_EXPIRY_MS);
        recentInteracts.entrySet().removeIf(e -> now - e.getValue() > LAST_INTERACT_EXPIRY_MS);


        if (JournalConfig.requireJournalInInventory && !hasJournalInInventory(player)) {
            tickCounter = 0;
            return;
        }

        switch (JournalConfig.discoveryMode) {
            case NEAR -> processNearMode(player);
            case HIT -> { /* handled by onPlayerHitEntity(...) */ }
            case KILL -> { /* kill recorded + onEntityDied(...) finishes */ }
            case INTERACT -> { /* handled by onPlayerInteractEntity(...) */ }
        }
    }

    private static boolean hasJournalInInventory(ClientPlayerEntity player) {
        return true;
    }

    private static void processNearMode(ClientPlayerEntity player) {
        var world = player.clientWorld;
        var nearby = world.getEntitiesByClass(
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
        for (var id : toUnlock) {
            sendUnlock(id);
        }
    }

    public static void onPlayerHitEntity(Entity target) {
        if (!(target instanceof LivingEntity living)) return;

        recentHits.put(target.getId(), System.currentTimeMillis());

        if (JournalConfig.discoveryMode == JournalConfig.DiscoveryMode.HIT) {
            Identifier id = Registries.ENTITY_TYPE.getId(living.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                sendUnlock(id);
            }
        }
    }

    public static void onPlayerInteractEntity(Entity target) {
        if (!(target instanceof LivingEntity living)) return;
        recentInteracts.put(target.getId(), System.currentTimeMillis());

        if (JournalConfig.discoveryMode == JournalConfig.DiscoveryMode.INTERACT) {
            Identifier id = Registries.ENTITY_TYPE.getId(living.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                sendUnlock(id);
            }
        }
    }

    public static void onEntityDied(Entity target) {
        if (!(target instanceof LivingEntity living)) return;
        Long last = recentHits.get(target.getId());
        if (JournalConfig.discoveryMode == JournalConfig.DiscoveryMode.KILL && last != null) {
            Identifier id = Registries.ENTITY_TYPE.getId(living.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                sendUnlock(id);
            }
        }
    }

    public static void onPlayerTamedEntity(LivingEntity target) {
        if (!JournalConfig.enableTamedTrigger) return;

        var client = MinecraftClient.getInstance();
        var local = client.player;
        if (local == null) return;

        java.util.UUID ownerUuid = null;
        if (target instanceof net.minecraft.entity.passive.TameableEntity t) {
            ownerUuid = t.getOwnerUuid();
        } else if (target instanceof net.minecraft.entity.passive.AbstractHorseEntity h) {
            ownerUuid = h.getOwnerUuid();
        } else if (target instanceof net.minecraft.entity.Tameable tameableIface) {

        }

        if (ownerUuid == null || !ownerUuid.equals(local.getUuid())) return;

        Identifier id = Registries.ENTITY_TYPE.getId(target.getType());
        if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
            sendUnlock(id);
        }
    }


    private static void sendUnlock(Identifier id) {
        ClientPlayNetworking.send(new UnlockMobPayload(id));
        alreadySent.add(id);
    }
}
