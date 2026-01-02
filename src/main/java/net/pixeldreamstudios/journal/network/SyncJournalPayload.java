package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class SyncJournalPayload {
    public static final Identifier ID = new Identifier("journal", "sync_journal");

    private final Map<Identifier, Long> discoveries;

    public SyncJournalPayload(Map<Identifier, Long> discoveries) {
        this.discoveries = discoveries;
    }

    public Map<Identifier, Long> discoveries() {
        return discoveries;
    }

    public static SyncJournalPayload read(PacketByteBuf buf) {
        int size = buf.readInt();
        Map<Identifier, Long> map = new HashMap<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            long t = buf.readLong();
            map.put(id, t);
        }
        return new SyncJournalPayload(map);
    }

    public void write(PacketByteBuf buf) {
        Map<Identifier, Long> validEntries = new HashMap<>();
        for (Map.Entry<Identifier, Long> e : discoveries.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                validEntries.put(e.getKey(), e.getValue());
            }
        }

        buf.writeInt(validEntries.size());
        for (Map.Entry<Identifier, Long> e : validEntries.entrySet()) {
            buf.writeIdentifier(e.getKey());
            buf.writeLong(e.getValue());
        }
    }

    public static void sendToClient(ServerPlayerEntity player, Map<Identifier, Long> discoveries) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncJournalPayload(discoveries).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void sendToServer(Map<Identifier, Long> discoveries) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncJournalPayload(discoveries).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public interface S2CHandler {
        void handle(SyncJournalPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            SyncJournalPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, SyncJournalPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            SyncJournalPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }
}