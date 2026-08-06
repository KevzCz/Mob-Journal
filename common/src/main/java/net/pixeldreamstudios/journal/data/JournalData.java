package net.pixeldreamstudios.journal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.journal.config.JournalConfig;

import java.util.HashMap;
import java.util.Map;

public class JournalData {
    private final Map<ResourceLocation, Long> discovered = new HashMap<>();
    private boolean hasReceivedJournal = false;

    public boolean unlockMob(ResourceLocation id, long timestamp) {
        if (id == null || discovered.containsKey(id)) return false;
        discovered.put(id, JournalConfig.recordDiscoveryTimestamp ? timestamp : -1L);
        return true;
    }

    public void clearDiscovered() {
        this.discovered.clear();
    }

    public void removeBlacklistedMobs() {
        discovered.keySet().removeIf(JournalConfig::isBlacklisted);
    }

    public boolean removeMob(ResourceLocation id) {
        return discovered.remove(id) != null;
    }

    public boolean hasReceivedJournal() {
        return hasReceivedJournal;
    }

    public void setReceivedJournal(boolean received) {
        this.hasReceivedJournal = received;
    }

    public Map<ResourceLocation, Long> getDiscovered() {
        return discovered;
    }

    public void copyFrom(JournalData other) {
        this.discovered.clear();
        this.discovered.putAll(other.discovered);
        this.hasReceivedJournal = other.hasReceivedJournal;
    }

    public void load(CompoundTag root) {
        if (root.contains(JournalNbtKeys.MODERN_ROOT)) {
            readFrom(root.getCompound(JournalNbtKeys.MODERN_ROOT));
            return;
        }
        if (root.contains(JournalNbtKeys.LEGACY_ROOT)) {
            CompoundTag legacy = root.getCompound(JournalNbtKeys.LEGACY_ROOT);
            if (legacy.contains(JournalNbtKeys.LEGACY_JOURNAL)) {
                readFrom(legacy.getCompound(JournalNbtKeys.LEGACY_JOURNAL));
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
        discovered.clear();
        if (tag.contains(JournalNbtKeys.DISCOVERIES)) {
            ListTag list = tag.getList(JournalNbtKeys.DISCOVERIES, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entry.getString(JournalNbtKeys.DISCOVERY_ID));
                if (id != null) {
                    discovered.put(id, entry.getLong(JournalNbtKeys.DISCOVERY_TIME));
                }
            }
        }
        this.hasReceivedJournal = tag.getBoolean(JournalNbtKeys.JOURNAL_GIVEN);
    }

    public void writeTo(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Long> e : discovered.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(JournalNbtKeys.DISCOVERY_ID, e.getKey().toString());
            entry.putLong(JournalNbtKeys.DISCOVERY_TIME, e.getValue());
            list.add(entry);
        }
        tag.put(JournalNbtKeys.DISCOVERIES, list);
        tag.putBoolean(JournalNbtKeys.JOURNAL_GIVEN, this.hasReceivedJournal);
    }
}
