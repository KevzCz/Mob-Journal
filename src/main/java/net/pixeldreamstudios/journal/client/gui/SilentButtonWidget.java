package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
@Environment(EnvType.CLIENT)
public class SilentButtonWidget extends ButtonWidget {
    public SilentButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Tooltip tooltip) {
        super(x, y, width, height, message, onPress, (NarrationSupplier) Tooltip.of((Text) tooltip));
    }

    @Override
    public void playDownSound(SoundManager soundManager) {

    }
}
