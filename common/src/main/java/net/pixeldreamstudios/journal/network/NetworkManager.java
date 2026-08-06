package net.pixeldreamstudios.journal.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

public class NetworkManager {

    @ExpectPlatform
    public static void registerCommonPayloads() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerClientReceivers() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToServer(JournalPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToClient(ServerPlayer player, JournalPayload payload) {
        throw new AssertionError();
    }
}
