package net.pixeldreamstudios.journal.compat.fabric;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.compat.AccessorySlots;

import java.util.Map;
import java.util.Optional;

final class TrinketsInternal {

    private static final String SLOT_GROUP = "chest";

    private static final ResourceLocation SLOT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "journal_slot_disabled");

    // Without this the item resolves to the default trinket, which rejects every slot —
    // the datapack tag alone is not enough to make an item equippable.
    static void registerItem(Item item) {
        TrinketsApi.registerTrinket(item, new Trinket() {
        });
    }

    static boolean hasEquipped(Player player, Item item) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) return false;

        for (var pair : component.get().getEquipped(stack -> stack.is(item))) {
            ItemStack stack = pair.getB();
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static void setSlotEnabled(Player player, boolean enabled) {
        TrinketInventory inventory = findJournalInventory(player);
        if (inventory == null) return;

        if (enabled) {
            inventory.removeModifier(SLOT_MODIFIER_ID);
        } else {
            inventory.addPersistentModifier(new AttributeModifier(
                    SLOT_MODIFIER_ID,
                    -1024.0D,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        inventory.markUpdate();
    }

    private static TrinketInventory findJournalInventory(Player player) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) return null;

        Map<String, SlotGroup> groups = component.get().getGroups();
        SlotGroup group = groups.get(SLOT_GROUP);
        if (group == null) return null;

        Map<String, TrinketInventory> inventories = component.get().getInventory().get(SLOT_GROUP);
        if (inventories == null) return null;

        return inventories.get(AccessorySlots.JOURNAL_SLOT);
    }

    private TrinketsInternal() {
    }
}
