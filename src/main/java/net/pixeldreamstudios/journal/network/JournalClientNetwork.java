// src/main/java/net/pixeldreamstudios/journal/client/JournalClientNetwork.java
package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.JournalClient;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import net.pixeldreamstudios.journal.client.gui.JournalScreen;
import net.pixeldreamstudios.journal.client.gui.MobDetailsScreen;
import net.pixeldreamstudios.journal.client.toast.MobDiscoveredToast;
import net.pixeldreamstudios.journal.item.JournalItems;

public class JournalClientNetwork {
    public static void init() {
        // Open-journal response
        ClientPlayNetworking.registerGlobalReceiver(SyncJournalPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.DISCOVERED.clear();
                JournalClientData.DISCOVERED.addAll(payload.mobIds());
                if (MinecraftClient.getInstance().currentScreen instanceof JournalScreen screen) {
                    screen.updateDiscoveredMobs();
                }
                // trigger screen open if requested
                if (JournalClientData.shouldOpenJournalScreen) {
                    MinecraftClient.getInstance().player.playSound(
                            net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN,
                            1f, 1f + (float)(Math.random()*0.2 - 0.1)
                    );
                    MinecraftClient.getInstance().setScreen(new JournalScreen());
                    JournalClientData.shouldOpenJournalScreen = false;
                }
            });
        });

        // Mob-drops response
        ClientPlayNetworking.registerGlobalReceiver(SyncMobDropsPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.LAST_DROPS = new java.util.ArrayList<>(payload.drops().values());
                if (MinecraftClient.getInstance().currentScreen instanceof MobDetailsScreen screen) {
                    screen.rebuildWithDrops();
                }
            });
        });

        // Toast on discovery
        ClientPlayNetworking.registerGlobalReceiver(DiscoveredMobToastPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Identifier mobId = payload.mobId();
                EntityType<?> type = Registries.ENTITY_TYPE.get(mobId);
                MobDiscoveredToast.show(
                        type,
                        Text.translatable("toast.journal.mob_discovered"),
                        Text.translatable(type.getTranslationKey())
                );
            });
        });

        // Stat sync
        ClientPlayNetworking.registerGlobalReceiver(SyncMobStatsPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.MOB_STATS.clear();
                JournalClientData.MOB_STATS.putAll(payload.stats());
            });
        });

        // Send “I’m ready” on join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sender.sendPacket(ClientReadyPayload.INSTANCE);
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (JournalClient.openJournalKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    boolean hasJournal = client.player.getInventory()
                            .contains(new ItemStack(JournalItems.JOURNAL_ITEM));  // ← use JournalItems.JOURNAL_ITEM
                    if (hasJournal) {
                        JournalClientData.shouldOpenJournalScreen = true;
                        ClientPlayNetworking.send(OpenJournalPayload.INSTANCE);
                    } else {
                        client.player.sendMessage(
                                Text.literal("§cYou need to carry your Journal to open it!"),
                                true
                        );
                    }
                }
            }
            MobUnlockTracker.tick();
        });
    }
}
