package net.pixeldreamstudios.journal.item;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

            // 👇 Set flag to open screen after payload is received
            net.pixeldreamstudios.journal.client.JournalClientData.shouldOpenJournalScreen = true;

            // Send payload to server
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(OpenJournalPayload.INSTANCE);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }


}
