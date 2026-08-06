package net.pixeldreamstudios.journal.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.pixeldreamstudios.journal.client.MobUnlockTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void journal$onEntityStatus(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        ClientPacketListener self = (ClientPacketListener) (Object) this;
        var level = self.getLevel();
        if (level == null) return;

        Entity entity = packet.getEntity(level);
        if (entity == null) return;

        byte status = packet.getEventId();

        if (status == EntityEvent.DEATH) {
            MobUnlockTracker.onEntityDied(entity);
        }
    }
}
