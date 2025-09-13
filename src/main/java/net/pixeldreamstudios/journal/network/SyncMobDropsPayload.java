package net.pixeldreamstudios.journal.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class SyncMobDropsPayload {
    public static final Identifier ID = new Identifier("journal", "sync_mob_drops");

    private final Map<Identifier, ItemStack> drops;

    public SyncMobDropsPayload(Map<Identifier, ItemStack> drops) {
        this.drops = drops;
    }

    public Map<Identifier, ItemStack> drops() {
        return drops;
    }

    public static SyncMobDropsPayload read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Map<Identifier, ItemStack> drops = new HashMap<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            ItemStack stack = buf.readItemStack();
            drops.put(id, stack);
        }
        return new SyncMobDropsPayload(drops);
    }

    public void write(PacketByteBuf buf) {
        Map<Identifier, ItemStack> valid = new HashMap<>();
        for (Map.Entry<Identifier, ItemStack> e : drops.entrySet()) {
            ItemStack s = e.getValue();
            if (s != null && !s.isEmpty() && s.getItem() != Items.AIR) {
                valid.put(e.getKey(), s);
            }
        }
        buf.writeVarInt(valid.size());
        for (Map.Entry<Identifier, ItemStack> e : valid.entrySet()) {
            buf.writeIdentifier(e.getKey());
            buf.writeItemStack(e.getValue());
        }
    }

    public static void sendToClient(ServerPlayerEntity player, Map<Identifier, ItemStack> drops) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncMobDropsPayload(drops).write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void sendToServer(Map<Identifier, ItemStack> drops) {
        PacketByteBuf buf = PacketByteBufs.create();
        new SyncMobDropsPayload(drops).write(buf);
        ClientPlayNetworking.send(ID, buf);
    }

    public interface S2CHandler {
        void handle(SyncMobDropsPayload payload);
    }

    public static void registerS2C(S2CHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, netHandler, buf, responseSender) -> {
            SyncMobDropsPayload payload = read(buf);
            client.execute(() -> handler.handle(payload));
        });
    }

    public interface C2SHandler {
        void handle(ServerPlayerEntity player, SyncMobDropsPayload payload);
    }

    public static void registerC2S(C2SHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, netHandler, buf, responseSender) -> {
            SyncMobDropsPayload payload = read(buf);
            server.execute(() -> handler.handle(player, payload));
        });
    }
}
