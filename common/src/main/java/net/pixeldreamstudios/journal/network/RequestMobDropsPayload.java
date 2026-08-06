package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record RequestMobDropsPayload(ResourceLocation mobId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestMobDropsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "request_mob_drops"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMobDropsPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), RequestMobDropsPayload::read);

    public static RequestMobDropsPayload read(RegistryFriendlyByteBuf buf) {
        return new RequestMobDropsPayload(buf.readResourceLocation());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
