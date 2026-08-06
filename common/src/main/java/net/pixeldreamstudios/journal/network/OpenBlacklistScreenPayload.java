package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record OpenBlacklistScreenPayload() implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "open_blacklist");

    public static final OpenBlacklistScreenPayload INSTANCE = new OpenBlacklistScreenPayload();

    public static OpenBlacklistScreenPayload read(FriendlyByteBuf buf) {
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
