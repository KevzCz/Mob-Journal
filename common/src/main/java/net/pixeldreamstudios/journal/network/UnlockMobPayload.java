package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

public record UnlockMobPayload(ResourceLocation mobId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlockMobPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "unlock_mob"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockMobPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), UnlockMobPayload::read);

    public static UnlockMobPayload read(RegistryFriendlyByteBuf buf) {
        return new UnlockMobPayload(buf.readResourceLocation());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(mobId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
