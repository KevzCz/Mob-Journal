package net.pixeldreamstudios.journal.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.events.JournalSounds;
import net.pixeldreamstudios.journal.item.JournalItems;
@Environment(EnvType.CLIENT)
public class MobDiscoveredToast implements Toast {
    private final EntityType<?> entityType;
    private final Text description;

    private boolean playedSound = false;
    private LivingEntity cachedEntity;
    private int dynamicWidth = 140;
    public MobDiscoveredToast(EntityType<?> entityType, Text description) {
        this.entityType = entityType;
        this.description = description;
    }

    public static void show(EntityType<?> entityType, Text title, Text description) {
        Identifier id = EntityType.getId(entityType);
        if (JournalConfig.isBlacklisted(id)) return;
        CustomToastManager.add(new MobDiscoveredToast(entityType, description));
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (entityType != null && cachedEntity == null) {
            World world = client.world;
            if (world != null) {
                var entity = entityType.create(world);
                if (entity instanceof LivingEntity living) {
                    living.setPos(0, 0, 0);
                    living.tick();
                    cachedEntity = living;
                }
            }
        }

        if (!playedSound && startTime > 0L) {
            playedSound = true;
            client.getSoundManager().play(PositionedSoundInstance.master(JournalSounds.WRITING, 2.0F));
        }

        boolean right = JournalConfig.toastPosition == JournalConfig.ToastPosition.TOP_RIGHT ||
                JournalConfig.toastPosition == JournalConfig.ToastPosition.BOTTOM_RIGHT;

        int spacing = 10;
        int iconWidth = 40;
        int bookIconWidth = 20;
        int textWidth = client.textRenderer.getWidth(description);
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
            mobX = getWidth() - spacing - 50;
            bookItemX = textX - 25;
        }

        if (cachedEntity != null) {
            drawEntity(context, mobX + 15, 5, 5, cachedEntity);
        } else {
            context.drawItem(JournalItems.JOURNAL_ITEM.getDefaultStack(), mobX + 15, 5);
        }

        context.drawText(client.textRenderer, description, textX, 5, 0xFFFFFF, false);
        context.drawItem(JournalItems.JOURNAL_ITEM.getDefaultStack(), bookItemX, 0);

        return startTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    private void drawEntity(DrawContext context, int x, int y, int scale, LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(x, y, 200.0);
        matrices.scale(scale, -scale, scale);
        matrices.translate(0.0, -1.5, 0.0);

        float angle = (System.currentTimeMillis() % 8000L) / 8000.0F * 360F;
        entity.bodyYaw = angle;
        entity.setYaw(angle);
        entity.setPitch(0.0f);
        entity.headYaw = angle;

        dispatcher.setRenderShadows(false);
        dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.getVertexConsumers(), 0xF000F0);
        context.draw();
        dispatcher.setRenderShadows(true);
        matrices.pop();
    }

    @Override
    public int getWidth() {
        return dynamicWidth;
    }

    @Override
    public int getHeight() {
        return 18;
    }
}
