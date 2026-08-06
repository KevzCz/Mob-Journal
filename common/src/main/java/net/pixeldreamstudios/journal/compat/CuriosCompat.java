package net.pixeldreamstudios.journal.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class CuriosCompat {

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerItem(Item item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasEquipped(Player player, Item item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setSlotEnabled(Player player, boolean enabled) {
        throw new AssertionError();
    }

    private CuriosCompat() {
    }
}
