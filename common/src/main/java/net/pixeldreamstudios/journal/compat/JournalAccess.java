package net.pixeldreamstudios.journal.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.item.JournalItems;

public final class JournalAccess {

    // Deferred: on NeoForge the item does not exist until the registry event fires, so
    // resolving the supplier during init throws "Registry Object not present".
    public static void registerAccessoryItem() {
        JournalItems.JOURNAL_ITEM.listen(item -> {
            if (TrinketsCompat.isLoaded()) {
                TrinketsCompat.registerItem(item);
            }
            if (CuriosCompat.isLoaded()) {
                CuriosCompat.registerItem(item);
            }
        });
    }

    public static boolean hasJournal(Player player) {
        if (player == null) return false;

        if (player.getInventory().contains(new ItemStack(JournalItems.JOURNAL_ITEM.get()))) {
            return true;
        }

        return isEquippedInAccessorySlot(player);
    }

    public static boolean isEquippedInAccessorySlot(Player player) {
        if (player == null) return false;

        if (JournalConfig.enableTrinketsCompat
                && TrinketsCompat.isLoaded()
                && TrinketsCompat.hasEquipped(player, JournalItems.JOURNAL_ITEM.get())) {
            return true;
        }

        return JournalConfig.enableCuriosCompat
                && CuriosCompat.isLoaded()
                && CuriosCompat.hasEquipped(player, JournalItems.JOURNAL_ITEM.get());
    }

    public static void refreshSlotState(Player player) {
        if (player == null) return;

        if (TrinketsCompat.isLoaded()) {
            TrinketsCompat.setSlotEnabled(player, JournalConfig.enableTrinketsCompat);
        }

        if (CuriosCompat.isLoaded()) {
            CuriosCompat.setSlotEnabled(player, JournalConfig.enableCuriosCompat);
        }
    }

    private JournalAccess() {
    }
}
