package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;

public record SyncJournalPayload(Map<Identifier, Long> discoveries) implements CustomPayload {

    public static final CustomPayload.Id<SyncJournalPayload> ID =
            new CustomPayload.Id<>(Identifier.of("journal", "sync_journal"));

    public static final PacketCodec<RegistryByteBuf, SyncJournalPayload> CODEC =
            PacketCodec.of(SyncJournalPayload::write, SyncJournalPayload::read);

    public static SyncJournalPayload read(RegistryByteBuf buf) {
        int size = buf.readInt();
        var map = new java.util.HashMap<Identifier, Long>(size);
        for (int i = 0; i < size; i++) {
            var id = buf.readIdentifier();
            long t  = buf.readLong();
            map.put(id, t);
        }
        return new SyncJournalPayload(map);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeInt(discoveries.size());
        for (var e : discoveries.entrySet()) {
            if (e.getKey() == null) continue;
            buf.writeIdentifier(e.getKey());
            buf.writeLong(e.getValue() == null ? -1L : e.getValue());
        }
    }


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
