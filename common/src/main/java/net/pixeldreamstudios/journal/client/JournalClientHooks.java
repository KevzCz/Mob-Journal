package net.pixeldreamstudios.journal.client;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.OpenJournalPayload;

public final class JournalClientHooks {

    public static void requestOpenJournal() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            JournalClientData.shouldOpenJournalScreen = true;
            NetworkManager.sendToServer(OpenJournalPayload.INSTANCE);
        });
    }

    private JournalClientHooks() {
    }
}
