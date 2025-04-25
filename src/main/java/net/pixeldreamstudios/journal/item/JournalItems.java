
package net.pixeldreamstudios.journal.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.pixeldreamstudios.journal.Journal;

public class JournalItems {
    public static final String MOD_ID = Journal.MOD_ID;
    public static Item JOURNAL_ITEM;
    public static ItemGroup JOURNAL_TAB;

    public static void init() {
        // ─── Register the journal item ───
         JOURNAL_ITEM = Registry.register(
                Registries.ITEM,
                Identifier.of(MOD_ID, "journal"),
                new JournalItem(new Item.Settings().maxCount(1))
        );

        // ─── Create & register the item-group/tab ───
        JOURNAL_TAB = FabricItemGroup.builder()
                .icon(() -> new ItemStack(JOURNAL_ITEM))
                .displayName(Text.translatable("itemGroup.journal.tab"))
                .entries((context, entries) -> entries.add(JOURNAL_ITEM))
                .build();
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "tab"), JOURNAL_TAB);

        // ─── Also add to the built-in Tools tab ───
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(JOURNAL_ITEM));
    }
}
