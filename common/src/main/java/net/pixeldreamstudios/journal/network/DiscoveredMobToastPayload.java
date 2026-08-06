package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record DiscoveredMobToastPayload(ResourceLocation mobId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DiscoveredMobToastPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "toast_discovered_mob"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscoveredMobToastPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), DiscoveredMobToastPayload::read);

    public static DiscoveredMobToastPayload read(RegistryFriendlyByteBuf buf) {
        return new DiscoveredMobToastPayload(buf.readResourceLocation());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
