package net.pixeldreamstudios.journal.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import java.util.HashSet;
import java.util.Set;

public class FavoriteMobsComponent implements ComponentV3, AutoSyncedComponent, RespawnableComponent<FavoriteMobsComponent> {
    private final Set<Identifier> favorites = new HashSet<>();
    private ServerPlayerEntity owner;

    public FavoriteMobsComponent(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            this.owner = serverPlayer;
        }
    }

    public boolean toggleFavorite(Identifier mobId, boolean isFavorited) {
        if (isFavorited) {
            return favorites.add(mobId);
        } else {
            return favorites.remove(mobId);
        }
    }

    public Set<Identifier> getFavorites() {
        return favorites;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        favorites.clear();
        if (tag.contains("favorites", NbtElement.LIST_TYPE)) {
            NbtList list = tag.getList("favorites", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                favorites.add(Identifier.tryParse(list.getString(i)));
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Identifier id : favorites) {
            list.add(NbtString.of(id.toString()));
        }
        tag.put("favorites", list);
    }

    @Override
    public void copyForRespawn(FavoriteMobsComponent original,
                               RegistryWrapper.WrapperLookup registryLookup,
                               boolean lossless, boolean keepInventory, boolean sameCharacter) {
        this.owner = original.owner;
        this.favorites.clear();
        this.favorites.addAll(original.favorites);
    }
}
