package net.pixeldreamstudios.journal.data;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.network.SyncMobStatsPayload;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import java.util.HashMap;
import java.util.Map;

public class MobStatTrackerComponent implements ComponentV3, AutoSyncedComponent, RespawnableComponent<MobStatTrackerComponent> {
    private final Map<Identifier, MobStat> stats = new HashMap<>();
    private ServerPlayerEntity owner;

    public MobStatTrackerComponent(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            this.owner = serverPlayer;
        }
    }

    public boolean isServerSide() {
        return owner != null;
    }

    public MobStat get(Identifier id) {
        return stats.getOrDefault(id, new MobStat(0, 0));
    }

    public void incrementKills(Identifier id) {
        stats.put(id, get(id).incrementKills());
        if (isServerSide()) JournalComponents.MOB_STATS.sync(owner);
    }

    public void incrementDeaths(Identifier id) {
        stats.put(id, get(id).incrementDeaths());
        if (isServerSide()) JournalComponents.MOB_STATS.sync(owner);
    }

    public Map<Identifier, MobStat> getAllStats() {
        return stats;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        stats.clear();
        if (!tag.contains("mobStats")) return;

        NbtCompound data = tag.getCompound("mobStats");
        for (String key : data.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                NbtCompound value = data.getCompound(key);
                int kills = value.getInt("kills");
                int deaths = value.getInt("deaths");
                stats.put(id, new MobStat(kills, deaths));
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound data = new NbtCompound();
        for (Map.Entry<Identifier, MobStat> entry : stats.entrySet()) {
            NbtCompound value = new NbtCompound();
            value.putInt("kills", entry.getValue().kills());
            value.putInt("deaths", entry.getValue().deaths());
            data.put(entry.getKey().toString(), value);
        }
        tag.put("mobStats", data);
    }

    @Override
    public void copyForRespawn(MobStatTrackerComponent original, RegistryWrapper.WrapperLookup registryLookup, boolean lossless, boolean keepInventory, boolean sameCharacter) {
        this.owner = original.owner.server.getPlayerManager().getPlayer(original.owner.getUuid());

        this.stats.clear();
        this.stats.putAll(original.stats);

        if (isServerSide()) {
            ServerPlayNetworking.send(owner, new SyncMobStatsPayload(getAllStats()));
        }
    }
}
