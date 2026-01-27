package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenBlacklistScreenPayload() implements CustomPayload {

    public static final Id<OpenBlacklistScreenPayload> ID =
            new Id<>(Identifier.of("journal", "open_blacklist"));

    public static final OpenBlacklistScreenPayload INSTANCE = new OpenBlacklistScreenPayload();

    public static final PacketCodec<RegistryByteBuf, OpenBlacklistScreenPayload> CODEC =
            PacketCodec.of(
                    (buf, payload) -> {
                    },
                    buf -> INSTANCE
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}