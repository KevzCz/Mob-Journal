package net.pixeldreamstudios.journal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.gui.JournalScreen;
import net.pixeldreamstudios.journal.client.gui.MobDetailsScreen;
import net.pixeldreamstudios.journal.client.gui.MobSelectorScreen;
import net.pixeldreamstudios.journal.client.toast.CustomToastManager;
import net.pixeldreamstudios.journal.client.toast.MobDiscoveredToast;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.*;
import net.pixeldreamstudios.journal.registry.ModBlockEntities;
import net.pixeldreamstudios.journal.client.render.MobDisplayBlockEntityRenderer;
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
                    boolean hasJournal = client.player.getInventory().contains(new ItemStack(Journal.JOURNAL_ITEM));
                    if (hasJournal) {
                        JournalClientData.shouldOpenJournalScreen = true;
                        ClientPlayNetworking.send(OpenJournalPayload.INSTANCE);
                    } else {
                        client.player.sendMessage(Text.literal("§cYou need to carry your Journal to open it!"), true);
                    }
                }
            }

        });
        ClientPlayNetworking.registerGlobalReceiver(SyncMobDropsPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.LAST_DROPS = new ArrayList<>(payload.drops().values());
                if (MinecraftClient.getInstance().currentScreen instanceof MobDetailsScreen screen) {
                    screen.rebuildWithDrops(); // new method you'll add below
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncJournalPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.DISCOVERED.clear();
                JournalClientData.DISCOVERED.addAll(payload.mobIds());
                if (MinecraftClient.getInstance().currentScreen instanceof JournalScreen journalScreen) {
                    journalScreen.updateDiscoveredMobs(); // You'll define this method
                }
                MobUnlockTracker.resetSentMobs();

                if (JournalClientData.shouldOpenJournalScreen) {
                    MinecraftClient.getInstance().player.playSound(
                            net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN,
                            1.0f, // volume
                            1.0f + (float)(Math.random() * 0.2 - 0.1) // slight pitch variation
                    );
                    MinecraftClient.getInstance().setScreen(new JournalScreen());
                    JournalClientData.shouldOpenJournalScreen = false;
                }
            });
        });

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

        ClientPlayNetworking.registerGlobalReceiver(SyncMobStatsPayload.ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() -> {
                JournalClientData.MOB_STATS.clear();
                JournalClientData.MOB_STATS.putAll(payload.stats());
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MobUnlockTracker.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sender.sendPacket(ClientReadyPayload.INSTANCE);
        });
        BlockEntityRendererFactories.register(
                ModBlockEntities.MOB_DISPLAY_BLOCK_ENTITY,
                MobDisplayBlockEntityRenderer::new
        );
        JournalConfig.load();
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            CustomToastManager.render(context);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null && client.crosshairTarget instanceof BlockHitResult hit) {
                if (client.options.useKey.isPressed()) {
                    // You could check block type if needed
                    client.setScreen(new MobSelectorScreen());
                }
            }
        });

    }
}
