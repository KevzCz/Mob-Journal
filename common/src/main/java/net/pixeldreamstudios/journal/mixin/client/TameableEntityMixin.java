package net.pixeldreamstudios.journal.mixin.client;

import net.minecraft.world.entity.TamableAnimal;
import net.pixeldreamstudios.journal.client.JournalClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public class TameableEntityMixin {

    @Inject(method = "setTame(Z)V", at = @At("TAIL"))
    private void journal$onSetTamed(boolean tamed, CallbackInfo ci) {
        if (!tamed) return;
        TamableAnimal self = (TamableAnimal) (Object) this;
        if (!self.level().isClientSide()) return;
        JournalClientHooks.onEntityTamed(self);
    }
}
