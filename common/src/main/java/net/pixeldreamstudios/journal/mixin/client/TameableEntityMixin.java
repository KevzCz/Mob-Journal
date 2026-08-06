package net.pixeldreamstudios.journal.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.TamableAnimal;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(TamableAnimal.class)
public class TameableEntityMixin {

    @Inject(method = "setTame(ZZ)V", at = @At("TAIL"))
    private void journal$onSetTamed(boolean tamed, boolean updateAttributes, CallbackInfo ci) {
        if (!tamed) return;
        TamableAnimal self = (TamableAnimal) (Object) this;
        MobUnlockTracker.onPlayerTamedEntity(self);
    }
}
