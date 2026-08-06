package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class SilentButtonWidget extends Button {
    public SilentButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress, Tooltip tooltip) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        if (tooltip != null) {
            this.setTooltip(tooltip);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }
}
