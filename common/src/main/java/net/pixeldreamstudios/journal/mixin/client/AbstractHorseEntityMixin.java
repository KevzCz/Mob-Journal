package net.pixeldreamstudios.journal.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.pixeldreamstudios.journal.client.JournalClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public class AbstractHorseEntityMixin {

    @Inject(method = "setTamed", at = @At("TAIL"))
    private void journal$onSetTame(boolean tame, CallbackInfo ci) {
        if (!tame) return;
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (!self.level().isClientSide()) return;
        JournalClientHooks.onEntityTamed((LivingEntity) (Object) this);
    }
}
