package net.pixeldreamstudios.journal.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.pixeldreamstudios.journal.config.JournalConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CustomToastManager {
    private static final List<CustomToastEntry> toasts = new ArrayList<>();

    public static void add(Toast toast) {
        toasts.add(new CustomToastEntry(toast));
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        long slideDuration = 600;
        long totalDuration = 5000;

        boolean top = JournalConfig.toastPosition == JournalConfig.ToastPosition.TOP_LEFT ||
                JournalConfig.toastPosition == JournalConfig.ToastPosition.TOP_RIGHT;

        boolean right = JournalConfig.toastPosition == JournalConfig.ToastPosition.TOP_RIGHT ||
                JournalConfig.toastPosition == JournalConfig.ToastPosition.BOTTOM_RIGHT;

        int yStart = top ? 5 : screenHeight - 5;

        Iterator<CustomToastEntry> iterator = toasts.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            CustomToastEntry entry = iterator.next();
            long elapsed = entry.getElapsed();
            long remaining = totalDuration - elapsed;

            int toastWidth = entry.toast.width();
            int toastHeight = entry.toast.height();

            float progress;
            int x;

            if (elapsed < slideDuration) {
                progress = easeOutQuad(elapsed / (float) slideDuration);
                x = interpolateX(right, toastWidth, progress);
            } else if (remaining <= slideDuration) {
                progress = easeInQuad((slideDuration - remaining) / (float) slideDuration);
                x = interpolateX(right, toastWidth, 1 - progress);
            } else {
                x = right ? screenWidth - toastWidth - 5 : 5;
            }

            int y = top
                    ? yStart + index * (toastHeight + 4)
                    : yStart - ((index + 1) * (toastHeight + 4));

            context.pose().pushPose();
            context.pose().translate(x, y, 0);

            var result = entry.toast.render(context, client.getToasts(), elapsed);
            context.pose().popPose();

            if (elapsed >= totalDuration || result == Toast.Visibility.HIDE) {
                iterator.remove();
            } else {
                index++;
            }
        }
    }

    private static int interpolateX(boolean rightSide, int toastWidth, float progress) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int startX = rightSide ? screenWidth : -toastWidth;
        int endX = rightSide ? screenWidth - toastWidth - 5 : 5;
        return (int) (startX + (endX - startX) * progress);
    }

    private static float easeOutQuad(float t) {
        return -t * (t - 2);
    }

    private static float easeInQuad(float t) {
        return t * t;
    }

    private static class CustomToastEntry {
        final Toast toast;
        final long startTime;

        CustomToastEntry(Toast toast) {
            this.toast = toast;
            this.startTime = System.currentTimeMillis();
        }

        long getElapsed() {
            return System.currentTimeMillis() - startTime;
        }
    }
}
