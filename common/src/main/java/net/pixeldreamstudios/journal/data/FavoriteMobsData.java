package net.pixeldreamstudios.journal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class FavoriteMobsData {
    private final Set<ResourceLocation> favorites = new HashSet<>();

    public boolean toggleFavorite(ResourceLocation mobId, boolean isFavorited) {
        if (isFavorited) {
            return favorites.add(mobId);
        } else {
            return favorites.remove(mobId);
        }
    }

    public Set<ResourceLocation> getFavorites() {
        return favorites;
    }

    public void copyFrom(FavoriteMobsData other) {
        this.favorites.clear();
        this.favorites.addAll(other.favorites);
    }

    public void load(CompoundTag root) {
        if (root.contains(JournalNbtKeys.MODERN_ROOT)) {
            readFrom(root.getCompound(JournalNbtKeys.MODERN_ROOT));
            return;
        }
        if (root.contains(JournalNbtKeys.LEGACY_ROOT)) {
            CompoundTag legacy = root.getCompound(JournalNbtKeys.LEGACY_ROOT);
            if (legacy.contains(JournalNbtKeys.LEGACY_FAVORITES)) {
                readFrom(legacy.getCompound(JournalNbtKeys.LEGACY_FAVORITES));
            }
        }
    }

    public void save(CompoundTag root) {
        CompoundTag tag = root.contains(JournalNbtKeys.MODERN_ROOT)
                ? root.getCompound(JournalNbtKeys.MODERN_ROOT)
                : new CompoundTag();
        writeTo(tag);
        root.put(JournalNbtKeys.MODERN_ROOT, tag);
    }

    public void readFrom(CompoundTag tag) {
        favorites.clear();
        if (tag.contains(JournalNbtKeys.FAVORITES, Tag.TAG_LIST)) {
            ListTag list = tag.getList(JournalNbtKeys.FAVORITES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) {
                    favorites.add(id);
                }
            }
        }
    }

    public void writeTo(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ResourceLocation id : favorites) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put(JournalNbtKeys.FAVORITES, list);
    }
}
