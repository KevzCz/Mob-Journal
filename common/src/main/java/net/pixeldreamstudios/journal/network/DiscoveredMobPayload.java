package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record DiscoveredMobPayload(ResourceLocation mobId, long timestamp) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DiscoveredMobPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "discovered_mob"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscoveredMobPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), DiscoveredMobPayload::read);

    public static DiscoveredMobPayload read(RegistryFriendlyByteBuf buf) {
        ResourceLocation mobId = buf.readResourceLocation();
        long timestamp = buf.readLong();
        return new DiscoveredMobPayload(mobId, timestamp);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
        buf.writeLong(timestamp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
