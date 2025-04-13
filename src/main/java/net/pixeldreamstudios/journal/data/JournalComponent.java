package net.pixeldreamstudios.journal.data;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import java.util.HashSet;
import java.util.Set;

public class JournalComponent implements ComponentV3, AutoSyncedComponent, RespawnableComponent<JournalComponent> {
    private final Set<Identifier> discovered = new HashSet<>();
    private ServerPlayerEntity owner;

    public JournalComponent(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            this.owner = serverPlayer;
        }
    }

    public boolean isServerSide() {
        return owner != null;
    }

    public boolean unlockMob(Identifier id) {
        boolean added = discovered.add(id);
        if (added && isServerSide()) {
            ServerPlayNetworking.send(owner, new SyncJournalPayload(discovered.stream().toList()));
        }
        return added;
    }
    private boolean hasReceivedJournal = false;

    public boolean hasReceivedJournal() {
        return hasReceivedJournal;
    }

    public void setReceivedJournal(boolean received) {
        this.hasReceivedJournal = received;
    }

    public Set<Identifier> getDiscovered() {
        return discovered;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        discovered.clear();
        if (tag.contains("mobs")) {
            String[] ids = tag.getString("mobs").split(",");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    discovered.add(Identifier.tryParse(id));
                }
            }
        }
        this.hasReceivedJournal = tag.getBoolean("journal:given");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StringBuilder joined = new StringBuilder();
        for (Identifier id : discovered) {
            if (!joined.isEmpty()) joined.append(",");
            joined.append(id.toString());
        }
        tag.putString("mobs", joined.toString());
        tag.putBoolean("journal:given", this.hasReceivedJournal);
    }

    @Override
    public void copyForRespawn(JournalComponent original, RegistryWrapper.WrapperLookup registryLookup, boolean lossless, boolean keepInventory, boolean sameCharacter) {
        this.owner = original.owner.server.getPlayerManager().getPlayer(original.owner.getUuid());

        this.discovered.clear();
        this.discovered.addAll(original.discovered);

        if (isServerSide()) {
            ServerPlayNetworking.send(owner, new SyncJournalPayload(discovered.stream().toList()));
        }
    }
}
