package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record DiscoveredMobPayload(ResourceLocation mobId, long timestamp) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "discovered_mob");

    public static DiscoveredMobPayload read(FriendlyByteBuf buf) {
        ResourceLocation mobId = buf.readResourceLocation();
        long timestamp = buf.readLong();
        return new DiscoveredMobPayload(mobId, timestamp);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
        buf.writeLong(timestamp);
    }
}
