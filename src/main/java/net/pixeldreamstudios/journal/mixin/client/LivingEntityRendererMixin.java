package net.pixeldreamstudios.journal.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.pixeldreamstudios.journal.client.gui.AnimationOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow protected M model;

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;animateModel(Lnet/minecraft/entity/Entity;FFF)V")
    )
    private void beforeAnimateModel(T entity, float f, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        AnimationOverride.PoseData override = AnimationOverride.getPoseData(entity);
        if (override != null) {
            var limbAccessor = (net.pixeldreamstudios.journal.mixin.LimbAnimatorAccessor) entity.limbAnimator;
            limbAccessor.setPos(override.currentPos);
            limbAccessor.setPrevSpeed(override.speed);
            limbAccessor.setSpeed(override.speed);
        }
    }
}