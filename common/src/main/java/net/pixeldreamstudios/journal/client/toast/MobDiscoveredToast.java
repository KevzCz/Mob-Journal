package net.pixeldreamstudios.journal.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.journal.util.SafeEntityFactory;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.events.JournalSounds;
import net.pixeldreamstudios.journal.item.JournalItems;

import com.mojang.blaze3d.vertex.PoseStack;

@Environment(EnvType.CLIENT)
public class MobDiscoveredToast implements Toast {
    private final EntityType<?> entityType;
    private final Component description;

    private boolean playedSound = false;
    private LivingEntity cachedEntity;
    private int dynamicWidth = 140;

    public MobDiscoveredToast(EntityType<?> entityType, Component description) {
        this.entityType = entityType;
        this.description = description;
    }

    public static void show(EntityType<?> entityType, Component title, Component description) {
        ResourceLocation id = EntityType.getKey(entityType);
        if (JournalConfig.isBlacklisted(id)) return;
        CustomToastManager.add(new MobDiscoveredToast(entityType, description));
    }

    @Override
    public Visibility render(GuiGraphics context, ToastComponent manager, long startTime) {
        Minecraft client = Minecraft.getInstance();

        if (entityType != null && cachedEntity == null) {
            Level level = client.level;
            if (level != null) {
                LivingEntity living = SafeEntityFactory.createLiving(entityType, level);
                if (living != null) {
                    living.setPos(0, 0, 0);
                    living.tick();
                    cachedEntity = living;
                }
            }
        }

        if (!playedSound && startTime > 0L) {
            playedSound = true;
            client.getSoundManager().play(SimpleSoundInstance.forUI(JournalSounds.WRITING.get(), 2.0F));
        }

        boolean right = JournalConfig.toastPosition == JournalConfig.ToastPosition.TOP_RIGHT ||
                JournalConfig.toastPosition == JournalConfig.ToastPosition.BOTTOM_RIGHT;

        int spacing = 10;
        int iconWidth = 40;
        int bookIconWidth = 20;
        int textWidth = client.font.width(description);
        dynamicWidth = iconWidth + spacing + textWidth + spacing + bookIconWidth;

        int textX;
        int mobX;
        int bookItemX;

        if (right) {
            mobX = 15;
            textX = spacing + 45;
            bookItemX = textX + textWidth + spacing;
        } else {
            textX = 25;
            mobX = width() - spacing - 50;
            bookItemX = textX - 25;
        }

        if (cachedEntity != null) {
            drawEntity(context, mobX + 15, 5, 5, cachedEntity);
        } else {
            context.renderItem(JournalItems.JOURNAL_ITEM.get().getDefaultInstance(), mobX + 15, 5);
        }

        context.drawString(client.font, description, textX, 5, 0xFFFFFF, false);
        context.renderItem(JournalItems.JOURNAL_ITEM.get().getDefaultInstance(), bookItemX, 0);

        return startTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    private void drawEntity(GuiGraphics context, int x, int y, int scale, LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PoseStack matrices = context.pose();

        matrices.pushPose();
        matrices.translate(x, y, 200.0);
        matrices.scale(scale, -scale, scale);
        matrices.translate(0.0, -1.5, 0.0);

        float angle = (System.currentTimeMillis() % 8000L) / 8000.0F * 360F;
        entity.yBodyRot = angle;
        entity.setYRot(angle);
        entity.setXRot(0.0f);
        entity.yHeadRot = angle;

        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.bufferSource(), 0xF000F0);
        context.flush();
        dispatcher.setRenderShadow(true);
        matrices.popPose();
    }

    @Override
    public int width() {
        return dynamicWidth;
    }

    @Override
    public int height() {
        return 18;
    }
}
