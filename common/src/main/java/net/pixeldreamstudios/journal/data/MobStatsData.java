package net.pixeldreamstudios.journal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class MobStatsData {
    private final Map<ResourceLocation, MobStat> stats = new HashMap<>();

    public MobStat get(ResourceLocation id) {
        return stats.getOrDefault(id, new MobStat(0, 0));
    }

    public void incrementKills(ResourceLocation id) {
        stats.put(id, get(id).incrementKills());
    }

    public void incrementDeaths(ResourceLocation id) {
        stats.put(id, get(id).incrementDeaths());
    }

    public Map<ResourceLocation, MobStat> getAllStats() {
        return stats;
    }

    public void copyFrom(MobStatsData other) {
        this.stats.clear();
        this.stats.putAll(other.stats);
    }

    public void load(CompoundTag root) {
        if (root.contains(JournalNbtKeys.MODERN_ROOT)) {
            readFrom(root.getCompound(JournalNbtKeys.MODERN_ROOT));
            return;
        }
        if (root.contains(JournalNbtKeys.LEGACY_ROOT)) {
            CompoundTag legacy = root.getCompound(JournalNbtKeys.LEGACY_ROOT);
            if (legacy.contains(JournalNbtKeys.LEGACY_MOB_STATS)) {
                readFrom(legacy.getCompound(JournalNbtKeys.LEGACY_MOB_STATS));
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
        stats.clear();
        if (!tag.contains(JournalNbtKeys.MOB_STATS)) return;

        CompoundTag data = tag.getCompound(JournalNbtKeys.MOB_STATS);
        for (String key : data.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                CompoundTag value = data.getCompound(key);
                stats.put(id, new MobStat(
                        value.getInt(JournalNbtKeys.KILLS),
                        value.getInt(JournalNbtKeys.DEATHS)
                ));
            }
        }
    }

    public void writeTo(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        for (Map.Entry<ResourceLocation, MobStat> entry : stats.entrySet()) {
            CompoundTag value = new CompoundTag();
            value.putInt(JournalNbtKeys.KILLS, entry.getValue().kills());
            value.putInt(JournalNbtKeys.DEATHS, entry.getValue().deaths());
            data.put(entry.getKey().toString(), value);
        }
        tag.put(JournalNbtKeys.MOB_STATS, data);
    }
}
