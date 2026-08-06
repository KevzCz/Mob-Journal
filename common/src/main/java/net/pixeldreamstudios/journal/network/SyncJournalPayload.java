package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.Journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public record SyncJournalPayload(Map<ResourceLocation, Long> discoveries) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "sync_journal");

    public static SyncJournalPayload read(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<ResourceLocation, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            long t = buf.readLong();
            map.put(id, t);
        }
        return new SyncJournalPayload(map);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
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
}
