package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public record SyncJournalPayload(Map<ResourceLocation, Long> discoveries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncJournalPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "sync_journal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncJournalPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), SyncJournalPayload::read);

    public static SyncJournalPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<ResourceLocation, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            long t = buf.readLong();
            map.put(id, t);
        }
        return new SyncJournalPayload(map);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        var entries = new ArrayList<>(discoveries.entrySet());

        var validEntries = entries.stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .toList();

        buf.writeInt(validEntries.size());
        for (var e : validEntries) {
            buf.writeResourceLocation(e.getKey());
            buf.writeLong(e.getValue());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
