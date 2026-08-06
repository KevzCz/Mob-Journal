package net.pixeldreamstudios.journal.forge.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.journal.data.FavoriteMobsData;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.JournalDataHolder;
import net.pixeldreamstudios.journal.data.MobStatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDataMixin implements JournalDataHolder {

    @Unique
    private final JournalData journal$journalData = new JournalData();

    @Unique
    private final MobStatsData journal$mobStatsData = new MobStatsData();

    @Unique
    private final FavoriteMobsData journal$favoriteMobsData = new FavoriteMobsData();

    @Override
    public JournalData journal$getJournalData() {
        return journal$journalData;
    }

    @Override
    public MobStatsData journal$getMobStatsData() {
        return journal$mobStatsData;
    }

    @Override
    public FavoriteMobsData journal$getFavoriteMobsData() {
        return journal$favoriteMobsData;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void journal$save(CompoundTag tag, CallbackInfo ci) {
        journal$journalData.save(tag);
        journal$mobStatsData.save(tag);
        journal$favoriteMobsData.save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void journal$load(CompoundTag tag, CallbackInfo ci) {
        journal$journalData.load(tag);
        journal$mobStatsData.load(tag);
        journal$favoriteMobsData.load(tag);
    }
}
