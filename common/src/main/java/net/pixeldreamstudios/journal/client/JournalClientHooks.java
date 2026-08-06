package net.pixeldreamstudios.journal.client;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.OpenJournalPayload;

public final class JournalClientHooks {

    public static void requestOpenJournal() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            JournalClientData.shouldOpenJournalScreen = true;
            NetworkManager.sendToServer(OpenJournalPayload.INSTANCE);
        });
    }

    public static void onEntityTamed(LivingEntity target) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> MobUnlockTracker.onPlayerTamedEntity(target));
    }

    private JournalClientHooks() {
    }
}
