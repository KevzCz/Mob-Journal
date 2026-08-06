package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record ClientReadyPayload() implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "client_ready");

    public static final ClientReadyPayload INSTANCE = new ClientReadyPayload();

    public static ClientReadyPayload read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }
}
