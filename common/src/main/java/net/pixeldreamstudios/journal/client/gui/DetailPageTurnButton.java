package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.pixeldreamstudios.journal.Journal;

@Environment(EnvType.CLIENT)
public class DetailPageTurnButton {
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/arrow-icon.png");
    private static final ResourceLocation ARROW_HOVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/arrow-icon-hover.png");

    private static final ResourceLocation ARROW_FLIPPED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/flipped-arrow-icon.png");
    private static final ResourceLocation ARROW_FLIPPED_HOVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/flipped-arrow-icon-hover.png");

    private final boolean isNext;
    private final int x, y;
    private final int width = 35, height = 22;
    private final Runnable onClick;

    private boolean hovered = false;
    public boolean visible = true;
    public boolean active = true;

    private float fillProgress = 0f;
    private final float fillSpeed = 0.05f;

    public DetailPageTurnButton(int x, int y, boolean isNext, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.isNext = isNext;
        this.onClick = onClick;
    }

    public void render(GuiGraphics context, int mouseX, int mouseY) {
        if (!visible) return;

        hovered = isMouseOver(mouseX, mouseY);

        if (hovered) {
            fillProgress = Math.min(1.0f, fillProgress + fillSpeed);
        } else {
            fillProgress = Math.max(0.0f, fillProgress - fillSpeed);
        }

        ResourceLocation base = isNext ? ARROW_FLIPPED_TEXTURE : ARROW_TEXTURE;
        ResourceLocation fill = isNext ? ARROW_FLIPPED_HOVER_TEXTURE : ARROW_HOVER_TEXTURE;

        context.blit(base, x, y, 0, 0, width, height, width, height);

        if (fillProgress > 0) {
            int fillWidth = (int) (width * fillProgress);

            if (isNext) {
                context.blit(
                        fill,
                        x, y,
                        0, 0,
                        fillWidth, height,
                        width, height
                );
            } else {
                int offsetX = width - fillWidth;
                context.blit(
                        fill,
                        x + offsetX, y,
                        offsetX, 0,
                        fillWidth, height,
                        width, height
                );
            }
        }

        if (!active) {
            context.fill(x, y, x + width, y + height, 0x66000000);
        }

        if (hovered) {
            context.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.literal(isNext ? "Next Info Page" : "Previous Info Page"),
                    mouseX, mouseY
            );
        }
    }

    public void mouseClicked(double mouseX, double mouseY) {
        if (!visible || !active) return;

        if (isMouseOver((int) mouseX, (int) mouseY)) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F)
            );
            onClick.run();
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return visible && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
