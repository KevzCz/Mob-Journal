package net.pixeldreamstudios.journal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.client.gui.JournalScreen;
import net.pixeldreamstudios.journal.client.gui.MobDetailsScreen;
import net.pixeldreamstudios.journal.client.toast.MobDiscoveredToast;
import net.pixeldreamstudios.journal.network.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class JournalClient implements ClientModInitializer {
    public static KeyBinding openJournalKey;


    @Override
    public void onInitializeClient() {
        openJournalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.journal.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.journal"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openJournalKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    JournalClientData.shouldOpenJournalScreen = true;
                    ClientPlayNetworking.send(OpenJournalPayload.INSTANCE);
                }
            }
        });
        PayloadTypeRegistry.playS2C().register(SyncMobDropsPayload.ID, SyncMobDropsPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SyncMobDropsPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.LAST_DROPS = new ArrayList<>(payload.drops().values());
                if (MinecraftClient.getInstance().currentScreen instanceof MobDetailsScreen screen) {
                    screen.rebuildWithDrops(); // new method you'll add below
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(SyncJournalPayload.ID, SyncJournalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SyncJournalPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.DISCOVERED.clear();
                JournalClientData.DISCOVERED.addAll(payload.mobIds());
                MobUnlockTracker.resetSentMobs();
                if (JournalClientData.shouldOpenJournalScreen) {
                    MinecraftClient.getInstance().setScreen(new JournalScreen());
                    JournalClientData.shouldOpenJournalScreen = false;
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(DiscoveredMobToastPayload.ID, DiscoveredMobToastPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DiscoveredMobToastPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Identifier mobId = payload.mobId();
                EntityType<?> entityType = Registries.ENTITY_TYPE.get(mobId);
                MobDiscoveredToast.show(
                        entityType,
                        Text.translatable("toast.journal.mob_discovered"),
                        Text.translatable(entityType.getTranslationKey())
                );
            });
        });

        PayloadTypeRegistry.playS2C().register(SyncMobStatsPayload.ID, SyncMobStatsPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SyncMobStatsPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.MOB_STATS.clear();
                JournalClientData.MOB_STATS.putAll(payload.stats());
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MobUnlockTracker.tick();
        });
    }
}
