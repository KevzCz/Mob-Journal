package net.pixeldreamstudios.journal.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.pixeldreamstudios.journal.client.JournalClient;
import net.pixeldreamstudios.journal.network.fabric.NetworkManagerImpl;

public final class JournalFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkManagerImpl.registerClientReceivers();
        JournalClient.init();
    }
}
