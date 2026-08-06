package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record UnlockMobPayload(ResourceLocation mobId) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "unlock_mob");

    public static UnlockMobPayload read(FriendlyByteBuf buf) {
        return new UnlockMobPayload(buf.readResourceLocation());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
    }
}
