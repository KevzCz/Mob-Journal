package net.pixeldreamstudios.journal.neoforge.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.journal.data.JournalNbtKeys;
import net.pixeldreamstudios.journal.neoforge.JournalAttachments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerLegacyDataMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void journal$readLegacyCardinalData(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(JournalNbtKeys.LEGACY_ROOT)) return;

        Player self = (Player) (Object) this;
        CompoundTag legacy = tag.getCompound(JournalNbtKeys.LEGACY_ROOT);

        if (legacy.contains(JournalNbtKeys.LEGACY_JOURNAL)
                && !tag.contains(JournalNbtKeys.MODERN_ROOT)) {
            self.getData(JournalAttachments.JOURNAL.get())
                    .readFrom(legacy.getCompound(JournalNbtKeys.LEGACY_JOURNAL));
        }

        if (legacy.contains(JournalNbtKeys.LEGACY_MOB_STATS)) {
            self.getData(JournalAttachments.MOB_STATS.get())
                    .readFrom(legacy.getCompound(JournalNbtKeys.LEGACY_MOB_STATS));
        }

        if (legacy.contains(JournalNbtKeys.LEGACY_FAVORITES)) {
            self.getData(JournalAttachments.FAVORITES.get())
                    .readFrom(legacy.getCompound(JournalNbtKeys.LEGACY_FAVORITES));
        }
    }
}
