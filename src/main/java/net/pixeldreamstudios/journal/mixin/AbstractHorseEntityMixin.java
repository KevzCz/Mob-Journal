package net.pixeldreamstudios.journal.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorseEntity.class)
public class AbstractHorseEntityMixin {

    @Inject(method = "setTame", at = @At("TAIL"))
    private void journal$onSetTame(boolean tame, CallbackInfo ci) {
        if (!tame) return;
        MobUnlockTracker.onPlayerTamedEntity((LivingEntity)(Object)this);
    }
}
