package net.pixeldreamstudios.journal.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.pixeldreamstudios.journal.network.OpenJournalPayload;

public class JournalItem extends Item {
    public JournalItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient) {
            net.pixeldreamstudios.journal.client.JournalClientData.shouldOpenJournalScreen = true;
            OpenJournalPayload.sendToServer();
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
