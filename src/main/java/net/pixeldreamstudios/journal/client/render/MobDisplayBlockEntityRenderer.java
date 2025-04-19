package net.pixeldreamstudios.journal.client.render;

import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.pixeldreamstudios.journal.block.entity.MobDisplayBlockEntity;

public class MobDisplayBlockEntityRenderer implements BlockEntityRenderer<MobDisplayBlockEntity> {
    private final EntityRenderDispatcher dispatcher;

    public MobDisplayBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.dispatcher = ctx.getEntityRenderDispatcher();
        System.out.println("[MobRenderer] Registered MobDisplayBlockEntityRenderer");
    }

    @Override
    public void render(MobDisplayBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        Entity mob = entity.getOrCreateMob();
        if (mob == null) {
            System.out.println("[MobRenderer] Mob is null.");
            return;
        }


        mob.setYaw((entity.getWorld().getTime() % 360));
        mob.setPitch(0f);
        mob.setBodyYaw(mob.getYaw());
        mob.setHeadYaw(mob.getYaw());
        mob.prevYaw = mob.getYaw();

        matrices.push();
        matrices.translate(0.5, 1.0, 0.5);
        matrices.scale(0.4f, 0.4f, 0.4f);

        System.out.println("[MobRenderer] Rendering mob: " + entity.getMobId());
        dispatcher.render(mob, 0, 0, 0, mob.getYaw(), tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(MobDisplayBlockEntity blockEntity) {
        return true;
    }
}
