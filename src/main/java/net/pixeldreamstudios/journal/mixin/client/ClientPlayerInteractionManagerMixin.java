package net.pixeldreamstudios.journal.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void journal$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        MobUnlockTracker.onPlayerHitEntity(target);
    }

    @Inject(method = "interactEntity", at = @At("TAIL"))
    private void journal$onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MobUnlockTracker.onPlayerInteractEntity(entity);
    }

    @Inject(method = "interactEntityAtLocation", at = @At("TAIL"))
    private void journal$onInteractEntityAt(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MobUnlockTracker.onPlayerInteractEntity(entity);
    }
}
