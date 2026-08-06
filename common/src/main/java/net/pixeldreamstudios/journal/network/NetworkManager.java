package net.pixeldreamstudios.journal.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
    public static void sendToServer(CustomPacketPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        throw new AssertionError();
    }
}
