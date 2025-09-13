package net.pixeldreamstudios.journal.data;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.network.DiscoveredMobPayload;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;

import java.util.HashMap;
import java.util.Map;

public class JournalComponent implements ComponentV3, AutoSyncedComponent, CopyableComponent<JournalComponent> {
    private final Map<Identifier, Long> discovered = new HashMap<>();
    private ServerPlayerEntity owner;

    public JournalComponent(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp) {
            this.owner = sp;
        }
    }

    public boolean isServerSide() {
        return owner != null;
    }

    public boolean unlockMob(Identifier id) {
        if (id == null || discovered.containsKey(id)) return false;

        long timestamp = JournalConfig.recordDiscoveryTimestamp
                ? owner.server.getOverworld().getTime()
                : -1L;

        discovered.put(id, timestamp);

        if (isServerSide()) {
            DiscoveredMobPayload.sendToClient(owner, id, timestamp);
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

    public Map<Identifier, Long> getDiscovered() {
        return discovered;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        discovered.clear();
        if (tag.contains("discoveries")) {
            NbtList list = tag.getList("discoveries", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                Identifier id = Identifier.tryParse(c.getString("id"));
                long t = c.getLong("time");
                if (id != null) discovered.put(id, t);
            }
        }
        this.hasReceivedJournal = tag.getBoolean("journal:given");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (var e : discovered.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putString("id", e.getKey().toString());
            c.putLong("time", e.getValue());
            list.add(c);
        }
        tag.put("discoveries", list);
        tag.putBoolean("journal:given", this.hasReceivedJournal);
    }

    @Override
    public void copyFrom(JournalComponent other) {
        this.discovered.clear();
        this.discovered.putAll(other.discovered);
        this.hasReceivedJournal = other.hasReceivedJournal;

        if (isServerSide()) {
            SyncJournalPayload.sendToClient(owner, discovered);
        }
    }
}
