package net.pixeldreamstudios.journal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.pixeldreamstudios.journal.client.toast.CustomToastManager;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.JournalClientNetwork;
import org.lwjgl.glfw.GLFW;

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
        JournalClientNetwork.init();
        JournalConfig.load();
        HudRenderCallback.EVENT.register((context, tickDelta) -> CustomToastManager.render(context));


    }
}
