package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleFavoritePayload(Identifier mobId, boolean favorited) implements CustomPayload {
    public static final Id<ToggleFavoritePayload> ID =
            new Id<>(Identifier.of("journal", "toggle_favorite"));

    public static final PacketCodec<RegistryByteBuf, ToggleFavoritePayload> CODEC =
            PacketCodec.of(ToggleFavoritePayload::write, ToggleFavoritePayload::read);

    public static ToggleFavoritePayload read(RegistryByteBuf buf) {
        Identifier mobId = buf.readIdentifier();
        boolean favorited = buf.readBoolean();
        return new ToggleFavoritePayload(mobId, favorited);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeIdentifier(mobId);
        buf.writeBoolean(favorited);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
