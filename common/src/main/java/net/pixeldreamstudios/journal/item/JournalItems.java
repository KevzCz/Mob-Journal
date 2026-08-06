package net.pixeldreamstudios.journal.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.Journal;

public class JournalItems {
    public static final String MOD_ID = Journal.MOD_ID;

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MOD_ID, Registries.ITEM);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<Item> JOURNAL_ITEM =
            ITEMS.register("journal", () -> new JournalItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<CreativeModeTab> JOURNAL_TAB =
            TABS.register("tab", () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup.journal.tab"),
                    () -> new ItemStack(JOURNAL_ITEM.get())
            ));

    public static void init() {
        ITEMS.register();
        TABS.register();

        CreativeTabRegistry.append(JOURNAL_TAB, JOURNAL_ITEM);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, JOURNAL_ITEM);
    }
}
