package net.pixeldreamstudios.journal.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.registry.Registries;
import io.netty.buffer.Unpooled;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.UnlockMobPayload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobUnlockTracker {

    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 40; // 2 seconds
    private static final double RADIUS = 8.0;
    private static final Set<Identifier> alreadySent = new HashSet<>();
    public static final Identifier UNLOCK_MOB_PACKET_ID = Identifier.of("journal", "unlock_mob");
    public static void resetSentMobs() {
        alreadySent.clear();
    }
    public static void tick() {
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return;

        // Get nearby mobs
        List<LivingEntity> nearbyMobs = client.world.getEntitiesByClass(
                LivingEntity.class,
                new Box(player.getPos(), player.getPos()).expand(RADIUS),
                e -> e != player && e.isAlive()
        );

        for (LivingEntity mob : nearbyMobs) {
            Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());

            // Send to server for real tracking
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeIdentifier(id);
            if (!alreadySent.contains(id)) {
                if (JournalConfig.blacklistedMobs.contains(id)) continue;
                ClientPlayNetworking.send(new UnlockMobPayload(id));
                alreadySent.add(id);
                }
        }
    }
}
