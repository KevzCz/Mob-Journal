package net.pixeldreamstudios.journal.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.Journal;

public class ModBlocks {
    public static final Block MOB_DISPLAY_BLOCK = new MobDisplayBlock(
            FabricBlockSettings.copyOf(Blocks.STONE)
    );

    public static void register() {
        System.out.println("[ModBlocks] Registering Mob Display Block...");

        Registry.register(Registries.BLOCK,
                Identifier.of(Journal.MOD_ID, "mob_display_block"),
                MOB_DISPLAY_BLOCK
        );

        Registry.register(Registries.ITEM,
                Identifier.of(Journal.MOD_ID, "mob_display_block"),
                new BlockItem(MOB_DISPLAY_BLOCK, new Item.Settings())
        );

        System.out.println("[ModBlocks] Mob Display Block registered!");
    }
}
