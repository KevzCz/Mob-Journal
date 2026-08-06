package net.pixeldreamstudios.journal.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.journal.client.JournalClientHooks;

public class JournalItem extends Item {
    public JournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            JournalClientHooks.requestOpenJournal();
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
