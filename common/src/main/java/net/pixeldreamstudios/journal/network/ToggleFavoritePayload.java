package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record ToggleFavoritePayload(ResourceLocation mobId, boolean favorited) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "toggle_favorite");

    public static ToggleFavoritePayload read(FriendlyByteBuf buf) {
        ResourceLocation mobId = buf.readResourceLocation();
        boolean favorited = buf.readBoolean();
        return new ToggleFavoritePayload(mobId, favorited);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
        buf.writeBoolean(favorited);
    }
}
