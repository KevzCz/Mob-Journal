package net.pixeldreamstudios.journal.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pixeldreamstudios.journal.block.entity.MobDisplayBlockEntity;

public class MobDisplayBlock extends BlockWithEntity {
    public static final MapCodec<MobDisplayBlock> CODEC = createCodec(MobDisplayBlock::new);

    public MobDisplayBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        System.out.println("[MobDisplayBlock] createBlockEntity called at " + pos);
        return new MobDisplayBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        System.out.println("[MobDisplayBlock] onUse triggered at " + pos);
        if (player.isSneaking()) return ActionResult.PASS;

        if (world.isClient) {
            player.sendMessage(Text.literal("Opening Mob Selector UI..."), true);
            net.minecraft.client.MinecraftClient.getInstance().setScreen(
                    new net.pixeldreamstudios.journal.client.gui.MobSelectorScreen()
            );
        }

        return ActionResult.SUCCESS;
    }
}
