package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DiscoveredMobPayload(Identifier mobId, long timestamp) implements CustomPayload {

    public static final CustomPayload.Id<DiscoveredMobPayload> ID =
            new CustomPayload.Id<>(Identifier.of("journal", "discovered_mob"));

    public static final PacketCodec<RegistryByteBuf, DiscoveredMobPayload> CODEC =
            PacketCodec.of(DiscoveredMobPayload::write, DiscoveredMobPayload::read);

    public static DiscoveredMobPayload read(RegistryByteBuf buf) {
        Identifier mobId = buf.readIdentifier();
        long timestamp = buf.readLong();
        return new DiscoveredMobPayload(mobId, timestamp);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeIdentifier(mobId);
        buf.writeLong(timestamp);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
