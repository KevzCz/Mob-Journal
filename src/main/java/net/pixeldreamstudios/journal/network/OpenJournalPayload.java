package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenJournalPayload() implements CustomPayload {

    public static final Id<OpenJournalPayload> ID =
            new Id<>(Identifier.of("journal", "open_request"));

    public static final OpenJournalPayload INSTANCE = new OpenJournalPayload();

    // 🧩 Register codec for Fabric 1.21.1+ (no data sent)
    public static final PacketCodec<RegistryByteBuf, OpenJournalPayload> CODEC =
            PacketCodec.of(
                    (buf, payload) -> {
                        // No data to write
                    },
                    buf -> INSTANCE
            );


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
