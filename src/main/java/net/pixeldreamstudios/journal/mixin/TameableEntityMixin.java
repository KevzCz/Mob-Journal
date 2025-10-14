package net.pixeldreamstudios.journal.mixin;

import net.minecraft.entity.passive.TameableEntity;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TameableEntity.class)
public class TameableEntityMixin {

    @Inject(method = "setTamed", at = @At("TAIL"))
    private void journal$onSetTamed(boolean tamed, boolean updateAttributes, CallbackInfo ci) {
        if (!tamed) return;
        TameableEntity self = (TameableEntity)(Object)this;
        MobUnlockTracker.onPlayerTamedEntity(self);
    }
}
