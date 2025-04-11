package net.pixeldreamstudios.journal.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SyncMobDropsPayload(Map<Identifier, ItemStack> drops) implements CustomPayload {

    public static final Id<SyncMobDropsPayload> ID =
            new Id<>(Identifier.of("journal", "sync_mob_drops"));

    public static final PacketCodec<RegistryByteBuf, SyncMobDropsPayload> CODEC =
            PacketCodec.of(SyncMobDropsPayload::write, SyncMobDropsPayload::read);

    public static SyncMobDropsPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        Map<Identifier, ItemStack> drops = new HashMap<>();

        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            ItemStack stack = ItemStack.PACKET_CODEC.decode(buf);
            drops.put(id, stack);
        }

        return new SyncMobDropsPayload(drops);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(drops.size());
        for (var entry : drops.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            ItemStack.PACKET_CODEC.encode(buf, entry.getValue());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
