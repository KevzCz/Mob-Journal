package net.pixeldreamstudios.journal.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.pixeldreamstudios.journal.compat.AccessorySlots;

public class TrinketsCompatImpl {

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(AccessorySlots.TRINKETS_MOD_ID);
    }

    public static void registerItem(Item item) {
        if (!isLoaded()) return;
        TrinketsInternal.registerItem(item);
    }

    public static boolean hasEquipped(Player player, Item item) {
        if (!isLoaded()) return false;
        return TrinketsInternal.hasEquipped(player, item);
    }

    public static void setSlotEnabled(Player player, boolean enabled) {
        if (!isLoaded()) return;
        TrinketsInternal.setSlotEnabled(player, enabled);
    }
}
