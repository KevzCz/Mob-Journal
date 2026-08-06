package net.pixeldreamstudios.journal.compat.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class TrinketsCompatImpl {

    public static boolean isLoaded() {
        return false;
    }

    public static void registerItem(Item item) {
    }

    public static boolean hasEquipped(Player player, Item item) {
        return false;
    }

    public static void setSlotEnabled(Player player, boolean enabled) {
    }
}
