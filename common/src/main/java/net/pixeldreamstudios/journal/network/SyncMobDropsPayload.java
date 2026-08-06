package net.pixeldreamstudios.journal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.pixeldreamstudios.journal.Journal;

import java.util.HashMap;
import java.util.Map;

public record SyncMobDropsPayload(Map<ResourceLocation, ItemStack> drops) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMobDropsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "sync_mob_drops"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMobDropsPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), SyncMobDropsPayload::read);

    public static SyncMobDropsPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, ItemStack> drops = new HashMap<>();

        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
            drops.put(id, stack);
        }

        return new SyncMobDropsPayload(drops);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        var validDrops = drops.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty() && entry.getValue().getItem() != Items.AIR)
                .toList();

        buf.writeVarInt(validDrops.size());
        for (var entry : validDrops) {
            buf.writeResourceLocation(entry.getKey());
            ItemStack.STREAM_CODEC.encode(buf, entry.getValue());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
