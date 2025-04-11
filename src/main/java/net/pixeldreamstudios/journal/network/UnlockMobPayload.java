package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UnlockMobPayload(Identifier mobId) implements CustomPayload {

    public static final CustomPayload.Id<UnlockMobPayload> ID =
            new CustomPayload.Id<>(Identifier.of("journal", "unlock_mob"));

    public static final PacketCodec<RegistryByteBuf, UnlockMobPayload> CODEC =
            PacketCodec.of(UnlockMobPayload::write, UnlockMobPayload::read);

    public static UnlockMobPayload read(RegistryByteBuf buf) {
        return new UnlockMobPayload(buf.readIdentifier());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeIdentifier(mobId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
