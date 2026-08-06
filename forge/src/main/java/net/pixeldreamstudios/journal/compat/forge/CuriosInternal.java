package net.pixeldreamstudios.journal.compat.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.compat.AccessorySlots;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

final class CuriosInternal {

    static boolean hasEquipped(Player player, Item item) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player).resolve();
        if (optional.isEmpty()) return false;

        for (ICurioStacksHandler handler : optional.get().getCurios().values()) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStacks().getStackInSlot(i);
                if (!stack.isEmpty() && stack.is(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    static void setSlotEnabled(Player player, boolean enabled) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player).resolve();
        if (optional.isEmpty()) return;

        ICuriosItemHandler handler = optional.get();

        if (enabled) {
            if (handler.getLockedSlots().contains(AccessorySlots.JOURNAL_SLOT)) {
                handler.unlockSlotType(AccessorySlots.JOURNAL_SLOT, 1, true, true);
            }
        } else {
            handler.lockSlotType(AccessorySlots.JOURNAL_SLOT);
        }
    }

    private CuriosInternal() {
    }
}
