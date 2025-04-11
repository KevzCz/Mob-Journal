package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DiscoveredMobToastPayload(Identifier mobId) implements CustomPayload {

    public static final Id<DiscoveredMobToastPayload> ID =
            new Id<>(Identifier.of("journal", "toast_discovered_mob"));

    public static final PacketCodec<RegistryByteBuf, DiscoveredMobToastPayload> CODEC =
            PacketCodec.of(DiscoveredMobToastPayload::write, DiscoveredMobToastPayload::read);

    public static DiscoveredMobToastPayload read(RegistryByteBuf buf) {
        return new DiscoveredMobToastPayload(buf.readIdentifier());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeIdentifier(mobId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
