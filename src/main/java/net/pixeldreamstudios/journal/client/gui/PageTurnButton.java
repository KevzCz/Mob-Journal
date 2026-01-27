package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
@Environment(EnvType.CLIENT)
public class PageTurnButton {
    private static final Identifier ARROW_TEXTURE = Identifier.of("journal", "textures/arrow-icon.png");
    private static final Identifier ARROW_HOVER_TEXTURE = Identifier.of("journal", "textures/arrow-icon-hover.png");

    private static final Identifier ARROW_FLIPPED_TEXTURE = Identifier.of("journal", "textures/flipped-arrow-icon.png");
    private static final Identifier ARROW_FLIPPED_HOVER_TEXTURE = Identifier.of("journal", "textures/flipped-arrow-icon-hover.png");

    private final boolean isNext;
    private final int x, y;
    private final int width = 35, height = 22;
    private final Runnable onClick;

    private boolean hovered = false;
    public boolean visible = true;
    public boolean active = true;

    private float fillProgress = 0f;
    private final float fillSpeed = 0.05f;
    public PageTurnButton(int x, int y, boolean isNext, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.isNext = isNext;
        this.onClick = onClick;
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!visible) return;

        hovered = isMouseOver(mouseX, mouseY);

        if (hovered) {
            fillProgress = Math.min(1.0f, fillProgress + fillSpeed);
        } else {
            fillProgress = Math.max(0.0f, fillProgress - fillSpeed);
        }

        Identifier base = isNext ? ARROW_FLIPPED_TEXTURE : ARROW_TEXTURE;
        Identifier fill = isNext ? ARROW_FLIPPED_HOVER_TEXTURE : ARROW_HOVER_TEXTURE;

        context.drawTexture(base, x, y, 0, 0, width, height, width, height);

        if (fillProgress > 0) {
            int fillWidth = (int) (width * fillProgress);

            if (isNext) {
                context.drawTexture(
                        fill,
                        x, y,
                        0, 0,
                        fillWidth, height,
                        width, height
                );
            } else {
                int offsetX = width - fillWidth;
                context.drawTexture(
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
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal(isNext ? "Next Page" : "Previous Page"),
                    mouseX, mouseY
            );
        }
    }

    public void mouseClicked(double mouseX, double mouseY) {
        if (!visible || !active) return;

        if (isMouseOver((int) mouseX, (int) mouseY)) {
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F)
            );
            onClick.run();
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return visible && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
