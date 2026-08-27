package net.pixeldreamstudios.journal.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.journal.client.toast.CustomToastManager;
import net.pixeldreamstudios.journal.compat.JournalAccess;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.ClientReadyPayload;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.OpenJournalPayload;
import net.pixeldreamstudios.journal.util.TagPreviewCache;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class JournalClient {
    public static KeyMapping openJournalKey;

    public static void init() {
        openJournalKey = new KeyMapping(
                "key.journal.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.journal"
        );
        KeyMappingRegistry.register(openJournalKey);

        JournalConfig.load();

        ClientTickEvent.CLIENT_POST.register(client -> {
            MobUnlockTracker.tick();
            handleKeybind(client);
        });

        ClientGuiEvent.RENDER_HUD.register((context, tickDelta) ->
                CustomToastManager.render(context));

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            TagPreviewCache.clear();
            NetworkManager.sendToServer(ClientReadyPayload.INSTANCE);
        });
    }

    private static void handleKeybind(Minecraft client) {
        while (openJournalKey.consumeClick()) {
            if (client.player != null && client.level != null) {
                boolean hasJournal = JournalAccess.hasJournal(client.player);

                boolean needsBook = JournalConfig.requireJournalInInventory;

                if (!needsBook || hasJournal) {
                    JournalClientData.shouldOpenJournalScreen = true;
                    NetworkManager.sendToServer(OpenJournalPayload.INSTANCE);
                } else {
                    client.player.displayClientMessage(
                            Component.literal("§cYou need to carry your Journal to open it!"),
                            true
                    );
                }
            }
        }
    }
}
