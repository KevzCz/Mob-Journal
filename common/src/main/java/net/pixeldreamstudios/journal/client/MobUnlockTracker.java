package net.pixeldreamstudios.journal.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.pixeldreamstudios.journal.compat.JournalAccess;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.UnlockMobPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class MobUnlockTracker {
    private static int tickCounter = 0;
    private static final Set<ResourceLocation> alreadySent = new HashSet<>();

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
        var client = Minecraft.getInstance();
        var level = client.level;
        var player = client.player;
        if (player == null || level == null) return;

        long now = System.currentTimeMillis();
        recentHits.entrySet().removeIf(e -> now - e.getValue() > LAST_HIT_EXPIRY_MS);
        recentInteracts.entrySet().removeIf(e -> now - e.getValue() > LAST_INTERACT_EXPIRY_MS);

        if (JournalConfig.requireJournalInInventory && !hasJournalInInventory(player)) {
            tickCounter = 0;
            return;
        }

        switch (JournalConfig.discoveryMode) {
            case NEAR -> processNearMode(player);
            case HIT -> {}
            case KILL -> {}
            case INTERACT -> {}
        }
    }

    private static boolean hasJournalInInventory(LocalPlayer player) {
        return JournalAccess.hasJournal(player);
    }

    private static void processNearMode(LocalPlayer player) {
        var level = player.clientLevel;
        var nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.position(), player.position()).inflate(JournalConfig.mobCheckRadius),
                e -> e != player && e.isAlive()
        );
        List<ResourceLocation> toUnlock = new ArrayList<>();
        for (var mob : nearby) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
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
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                sendUnlock(id);
            }
        }
    }

    public static void onPlayerInteractEntity(Entity target) {
        if (!(target instanceof LivingEntity living)) return;
        recentInteracts.put(target.getId(), System.currentTimeMillis());

        if (JournalConfig.discoveryMode == JournalConfig.DiscoveryMode.INTERACT) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                sendUnlock(id);
            }
        }
    }

    public static void onEntityDied(Entity target) {
        if (!(target instanceof LivingEntity living)) return;
        Long last = recentHits.get(target.getId());
        if (JournalConfig.discoveryMode == JournalConfig.DiscoveryMode.KILL && last != null) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
                sendUnlock(id);
            }
        }
    }

    public static void onPlayerTamedEntity(LivingEntity target) {
        if (!JournalConfig.enableTamedTrigger) return;

        var client = Minecraft.getInstance();
        var local = client.player;
        if (local == null) return;

        UUID ownerUuid = null;
        if (target instanceof TamableAnimal t) {
            ownerUuid = t.getOwnerUUID();
        } else if (target instanceof AbstractHorse h) {
            ownerUuid = h.getOwnerUUID();
        }

        if (ownerUuid == null || !ownerUuid.equals(local.getUUID())) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (!alreadySent.contains(id) && !JournalConfig.isBlacklisted(id)) {
            sendUnlock(id);
        }
    }

    private static void sendUnlock(ResourceLocation id) {
        NetworkManager.sendToServer(new UnlockMobPayload(id));
        alreadySent.add(id);
    }
}
