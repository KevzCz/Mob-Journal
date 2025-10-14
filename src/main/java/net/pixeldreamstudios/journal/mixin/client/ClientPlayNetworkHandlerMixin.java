package net.pixeldreamstudios.journal.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void journal$onEntityStatus(net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler self = (ClientPlayNetworkHandler)(Object)this;
        var world = self.getWorld();
        if (world == null) return;

        Entity entity = packet.getEntity(world);
        if (entity == null) return;

        byte status = packet.getStatus();

        if (status == EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES) {
            MobUnlockTracker.onEntityDied(entity);
        }
    }
}
