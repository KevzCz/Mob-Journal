package net.pixeldreamstudios.journal.data;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import java.util.HashMap;
import java.util.Map;

public class JournalComponent implements ComponentV3, AutoSyncedComponent, RespawnableComponent<JournalComponent> {
    private final Map<Identifier, Long> discovered = new HashMap<>();
    private ServerPlayerEntity owner;

    public JournalComponent(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            this.owner = serverPlayer;
        }
    }

    public boolean isServerSide() {
        return owner != null;
    }

    /**
     * Unlocks the mob and, if enabled, records the current server time.
     */
    public boolean unlockMob(Identifier id) {
        if (discovered.containsKey(id)) return false;
        long timestamp = JournalConfig.recordDiscoveryTimestamp
                ? owner.server.getOverworld().getTime()
                : -1L;
        discovered.put(id, timestamp);
        if (isServerSide()) {
            ServerPlayNetworking.send(owner, new SyncJournalPayload(discovered));
        }
        return true;
    }

    private boolean hasReceivedJournal = false;

    public void clearDiscovered() {
        this.discovered.clear();
    }

    public void removeBlacklistedMobs() {
        discovered.keySet().removeIf(JournalConfig::isBlacklisted);
    }

    public boolean removeMob(Identifier id) {
        return discovered.remove(id) != null;
    }

    public boolean hasReceivedJournal() {
        return hasReceivedJournal;
    }

    public void setReceivedJournal(boolean received) {
        this.hasReceivedJournal = received;
    }

    /** Expose both mob IDs and their discovery timestamps */
    public Map<Identifier, Long> getDiscovered() {
        return discovered;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        discovered.clear();
        if (tag.contains("discoveries")) {
            NbtList list = tag.getList("discoveries", NbtCompound.COMPOUND_TYPE);
            for (var elem : list) {
                var c = (NbtCompound) elem;
                Identifier id = Identifier.tryParse(c.getString("id"));
                long t = c.getLong("time");
                if (id != null) discovered.put(id, t);
            }
        }
        this.hasReceivedJournal = tag.getBoolean("journal:given");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (var e : discovered.entrySet()) {
            var c = new NbtCompound();
            c.putString("id", e.getKey().toString());
            c.putLong("time", e.getValue());
            list.add(c);
        }
        tag.put("discoveries", list);
        tag.putBoolean("journal:given", this.hasReceivedJournal);
    }

    @Override
    public void copyForRespawn(JournalComponent original,
                               RegistryWrapper.WrapperLookup registryLookup,
                               boolean lossless, boolean keepInventory, boolean sameCharacter) {
        this.owner = original.owner.server
                .getPlayerManager()
                .getPlayer(original.owner.getUuid());
        this.discovered.clear();
        this.discovered.putAll(original.discovered);
        if (isServerSide()) {
            ServerPlayNetworking.send(owner, new SyncJournalPayload(discovered));
        }
    }
}
