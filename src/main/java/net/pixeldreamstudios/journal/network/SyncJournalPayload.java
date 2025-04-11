package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;

import java.util.List;

public record SyncJournalPayload(List<Identifier> mobIds) implements CustomPayload {

    public static final CustomPayload.Id<SyncJournalPayload> ID =
            new CustomPayload.Id<>(Identifier.of("journal", "sync_journal"));

    public static final PacketCodec<RegistryByteBuf, SyncJournalPayload> CODEC =
            PacketCodec.of(SyncJournalPayload::write, SyncJournalPayload::read);

    public static SyncJournalPayload read(RegistryByteBuf buf) {
        List<Identifier> mobIds = buf.readCollection(ArrayList::new, b -> b.readIdentifier());
        return new SyncJournalPayload(mobIds);
    }



    public void write(RegistryByteBuf buf) {
        buf.writeCollection(mobIds, (buf2, id) -> ((RegistryByteBuf) buf2).writeIdentifier(id));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
