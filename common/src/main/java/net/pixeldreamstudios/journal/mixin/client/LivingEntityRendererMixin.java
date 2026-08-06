package net.pixeldreamstudios.journal.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.journal.client.gui.AnimationOverride;
import net.pixeldreamstudios.journal.mixin.LimbAnimatorAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow protected M model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V")
    )
    private void beforeAnimateModel(T entity, float f, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        AnimationOverride.PoseData override = AnimationOverride.getPoseData(entity);
        if (override != null) {
            var limbAccessor = (LimbAnimatorAccessor) entity.walkAnimation;
            limbAccessor.setPos(override.currentPos);
            limbAccessor.setPrevSpeed(override.speed);
            limbAccessor.setSpeed(override.speed);
        }
    }
}
