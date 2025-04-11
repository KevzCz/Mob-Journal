package net.pixeldreamstudios.journal.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PageTurnButton {
    private static final Identifier ARROW_TEXTURE = Identifier.of("journal", "textures/arrow.png");
    private static final Identifier ARROW_FLIPPED_TEXTURE = Identifier.of("journal", "textures/arrow_flipped.png");

    private final boolean isNext;
    private final int x, y;
    private final int width = 25, height = 23;
    private final Runnable onClick;

    private boolean hovered = false;
    public boolean visible = true;
    public boolean active = true;

    public PageTurnButton(int x, int y, boolean isNext, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.isNext = isNext;
        this.onClick = onClick;
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!visible) return;

        hovered = isMouseOver(mouseX, mouseY);

        Identifier texture = isNext ? ARROW_TEXTURE : ARROW_FLIPPED_TEXTURE;

        context.drawTexture(
                texture,
                x, y,
                0, 0,
                width, height,
                width, height
        );

        if (hovered && active) {
            context.fill(x, y, x + width, y + height, 0x33FFFFFF); // hover overlay
        }

        if (!active) {
            context.fill(x, y, x + width, y + height, 0x66000000); // disabled darken
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
