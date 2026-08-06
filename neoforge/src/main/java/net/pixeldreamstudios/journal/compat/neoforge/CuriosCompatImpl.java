package net.pixeldreamstudios.journal.compat.neoforge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.pixeldreamstudios.journal.compat.AccessorySlots;

public class CuriosCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded(AccessorySlots.CURIOS_MOD_ID);
    }

    public static void registerItem(Item item) {
        if (!isLoaded()) return;
        CuriosInternal.registerItem(item);
    }

    public static boolean hasEquipped(Player player, Item item) {
        if (!isLoaded()) return false;
        return CuriosInternal.hasEquipped(player, item);
    }

    public static void setSlotEnabled(Player player, boolean enabled) {
        if (!isLoaded()) return;
        CuriosInternal.setSlotEnabled(player, enabled);
    }
}
