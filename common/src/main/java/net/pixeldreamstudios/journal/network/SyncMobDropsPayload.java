package net.pixeldreamstudios.journal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.pixeldreamstudios.journal.Journal;

import java.util.HashMap;
import java.util.Map;

public record SyncMobDropsPayload(Map<ResourceLocation, ItemStack> drops) implements JournalPayload {
    public static final ResourceLocation ID = new ResourceLocation(Journal.MOD_ID, "sync_mob_drops");

    public static SyncMobDropsPayload read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, ItemStack> drops = new HashMap<>();

        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            ItemStack stack = buf.readItem();
            drops.put(id, stack);
        }

        return new SyncMobDropsPayload(drops);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        var validDrops = drops.entrySet().stream()
                .filter(entry -> entry.getValue() != null
                        && !entry.getValue().isEmpty()
                        && entry.getValue().getItem() != Items.AIR)
                .toList();

        buf.writeVarInt(validDrops.size());
        for (var entry : validDrops) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeItem(entry.getValue());
        }
    }
}
