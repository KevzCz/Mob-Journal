package net.pixeldreamstudios.journal.client.toast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.toast.Toast.Visibility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.client.render.item.ItemRenderer;
import net.pixeldreamstudios.journal.events.JournalSounds;

import java.util.Objects;

public class MobDiscoveredToast implements Toast {
    private final EntityType<?> entityType;
    private final Text title;
    private final Text description;

    private boolean playedSound = false;
    private LivingEntity cachedEntity;

    public MobDiscoveredToast(EntityType<?> entityType, Text title, Text description) {
        this.entityType = entityType;
        this.title = title;
        this.description = description;
    }

    public static void show(EntityType<?> entityType, Text title, Text description) {
        MinecraftClient.getInstance().getToastManager().add(new MobDiscoveredToast(entityType, title, description));
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!playedSound && startTime > 0L) {
            playedSound = true;
            client.getSoundManager().play(PositionedSoundInstance.master(JournalSounds.WRITING, 1.0F));
        }

        // Draw the book icon
        ItemRenderer itemRenderer = client.getItemRenderer();
        context.drawItem(Items.WRITABLE_BOOK.getDefaultStack(), 5, 5);

        // Draw the mob (if valid)
        if (entityType != null) {
            if (cachedEntity == null) {
                World world = Objects.requireNonNull(client.world);
                var entity = entityType.create(world);
                if (entity instanceof LivingEntity living) {
                    living.setPos(0, 0, 0);
                    living.tick();
                    cachedEntity = living;
                }
            }

            if (cachedEntity != null) {
                drawEntity(context, 30, 30, 16, cachedEntity);
            }
        }

        // Text: right side of mob
        int textX = 60;
        context.drawText(client.textRenderer, title, textX, 8, 0xFFFFFF, false);
        context.drawText(client.textRenderer, description, textX, 20, 0xAAAAAA, false);

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
        return 150;
    }

    @Override
    public int getHeight() {
        return 40;
    }
}
