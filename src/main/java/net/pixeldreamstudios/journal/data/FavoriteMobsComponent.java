package net.pixeldreamstudios.journal.data;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class FavoriteMobsComponent implements ComponentV3, AutoSyncedComponent, CopyableComponent<FavoriteMobsComponent> {
    private final Set<Identifier> favorites = new HashSet<>();
    private ServerPlayerEntity owner;

    public FavoriteMobsComponent(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp) {
            this.owner = sp;
        }
    }

    public boolean toggleFavorite(Identifier mobId, boolean isFavorited) {
        return isFavorited ? favorites.add(mobId) : favorites.remove(mobId);
    }

    public Set<Identifier> getFavorites() {
        return favorites;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        favorites.clear();
        if (tag.contains("favorites", NbtElement.LIST_TYPE)) {
            NbtList list = tag.getList("favorites", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                Identifier id = Identifier.tryParse(list.getString(i));
                if (id != null) favorites.add(id);
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (Identifier id : favorites) {
            list.add(NbtString.of(id.toString()));
        }
        tag.put("favorites", list);
    }

    @Override
    public void copyFrom(FavoriteMobsComponent other) {
        this.favorites.clear();
        this.favorites.addAll(other.favorites);
        this.owner = other.owner;
    }
}
