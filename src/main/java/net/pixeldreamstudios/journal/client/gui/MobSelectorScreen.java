    package net.pixeldreamstudios.journal.client.gui;

    import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
    import net.minecraft.client.MinecraftClient;
    import net.minecraft.client.gui.screen.Screen;
    import net.minecraft.entity.player.PlayerEntity;
    import net.minecraft.text.Text;
    import net.minecraft.util.hit.BlockHitResult;
    import net.minecraft.util.hit.HitResult;
    import net.minecraft.util.math.BlockPos;
    import net.pixeldreamstudios.journal.block.entity.MobDisplayBlockEntity;
    import net.pixeldreamstudios.journal.network.SetMobIdPayload;

    public class MobSelectorScreen extends JournalScreen {

        public MobSelectorScreen() {
            super();
            System.out.println("[MobSelectorScreen] Initialized");
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            System.out.println("[MobSelectorScreen] mouseClicked at (" + mouseX + ", " + mouseY + ")");

            for (MobSlot slot : this.mobSlots) {
                if (slot.isHovered((int) mouseX, (int) mouseY)) {
                    var mobId = slot.id;
                    PlayerEntity player = MinecraftClient.getInstance().player;
                    BlockPos pos = MinecraftClient.getInstance().crosshairTarget != null &&
                            MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK
                            ? ((BlockHitResult) MinecraftClient.getInstance().crosshairTarget).getBlockPos()
                            : null;
                    System.out.println("[MobSelectorScreen] Clicked mob slot: " + mobId.toString());

                    if (MinecraftClient.getInstance().player != null && pos != null) {
                        // Send the payload to the server
                        ClientPlayNetworking.send(new SetMobIdPayload(pos, mobId));

                        // Optional debug
                        MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("§7[Client] Sent SetMobIdPayload for: " + mobId + " at " + pos), false
                        );
                    }


                    return true;
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
