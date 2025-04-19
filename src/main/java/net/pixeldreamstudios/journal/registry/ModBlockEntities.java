// ModBlockEntities.java
package net.pixeldreamstudios.journal.registry;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.block.MobDisplayBlock;
import net.pixeldreamstudios.journal.block.ModBlocks;
import net.pixeldreamstudios.journal.block.entity.MobDisplayBlockEntity;

public class ModBlockEntities {
    public static final BlockEntityType<MobDisplayBlockEntity> MOB_DISPLAY_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Journal.MOD_ID, "mob_display_block_entity"),
                    BlockEntityType.Builder.create(MobDisplayBlockEntity::new, ModBlocks.MOB_DISPLAY_BLOCK).build());

    public static void register() {}
}
