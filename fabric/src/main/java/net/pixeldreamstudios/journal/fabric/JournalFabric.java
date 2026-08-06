package net.pixeldreamstudios.journal.fabric;

import net.fabricmc.api.ModInitializer;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.network.fabric.NetworkManagerImpl;

public final class JournalFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkManagerImpl.registerCommonPayloads();
        Journal.init();
    }
}
