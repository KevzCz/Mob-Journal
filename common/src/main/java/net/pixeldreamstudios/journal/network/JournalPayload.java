package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface JournalPayload {
    ResourceLocation id();

    void write(FriendlyByteBuf buf);
}
