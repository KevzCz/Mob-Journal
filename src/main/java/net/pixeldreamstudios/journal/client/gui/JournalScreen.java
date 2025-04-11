    package net.pixeldreamstudios.journal.client.gui;

    import net.minecraft.client.MinecraftClient;
    import net.minecraft.client.gui.DrawContext;
    import net.minecraft.client.gui.screen.Screen;
    import net.minecraft.client.gui.widget.TextFieldWidget;
    import net.minecraft.client.render.entity.EntityRenderDispatcher;
    import net.minecraft.client.util.math.MatrixStack;
    import net.minecraft.entity.LivingEntity;
    import net.minecraft.registry.Registries;
    import net.minecraft.sound.SoundEvents;
    import net.minecraft.text.Text;
    import net.minecraft.util.Identifier;
    import net.minecraft.world.World;
    import net.pixeldreamstudios.journal.client.JournalClientData;
    import net.minecraft.client.gui.widget.TextFieldWidget;
    import net.minecraft.client.font.TextRenderer;
    import net.pixeldreamstudios.journal.events.JournalSounds;

    import java.util.ArrayList;
    import java.util.List;

    public class JournalScreen extends Screen {
        private static final Identifier LEFT_PAGE = Identifier.of("journal", "textures/book.png");
        private static final Identifier RIGHT_PAGE = Identifier.of("journal", "textures/book_flipped.png");
        private TextFieldWidget searchBox;
        private String searchQuery = "";
        private final List<Identifier> filteredMobs = new ArrayList<>();
        private final int pageWidth = 276;
        private final int pageHeight = 180;
        private int currentPage = 0;
        private int totalPages = 0;
        private PageTurnButton nextButton;
        private PageTurnButton backButton;
        private final List<MobSlot> mobSlots = new ArrayList<>();
        private static class MobSlot {
            Identifier id;
            int x, y, width, height;

            MobSlot(Identifier id, int x, int y, int width, int height) {
                this.id = id;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            boolean isHovered(int mouseX, int mouseY) {
                return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            }
        }

        public JournalScreen() {
            this(0, "");
        }

        public JournalScreen(int initialPage, String searchQuery) {
            super(Text.literal("Mob Journal"));
            this.currentPage = initialPage;
            this.searchQuery = searchQuery == null ? "" : searchQuery;
        }

        private void updateFilteredList() {
            filteredMobs.clear();

            String namespaceFilter = null;
            String nameFilter = "";

            String[] tokens = searchQuery.split("\\s+");
            for (String token : tokens) {
                if (token.startsWith("@")) {
                    namespaceFilter = token.substring(1); // Remove '@'
                } else {
                    nameFilter += token + " ";
                }
            }

            nameFilter = nameFilter.trim().toLowerCase();

            List<Identifier> matched = new ArrayList<>();

            for (Identifier id : JournalClientData.DISCOVERED) {
                if (namespaceFilter != null && !id.getNamespace().toLowerCase().contains(namespaceFilter.toLowerCase())) {
                    continue;
                }

                boolean matches = id.toString().contains(nameFilter);

                var type = Registries.ENTITY_TYPE.get(id);
                if (type != null && type.create(MinecraftClient.getInstance().world) instanceof LivingEntity living) {
                    String name = living.getDisplayName().getString().toLowerCase();
                    matches |= name.contains(nameFilter);

                    if (matches || nameFilter.isEmpty()) {
                        matched.add(id);
                    }
                }
            }

            matched.sort((a, b) -> {
                var typeA = Registries.ENTITY_TYPE.get(a);
                var typeB = Registries.ENTITY_TYPE.get(b);
                if (typeA == null || typeB == null) return 0;

                var world = MinecraftClient.getInstance().world;
                if (world == null) return 0;

                var entA = typeA.create(world);
                var entB = typeB.create(world);

                if (!(entA instanceof LivingEntity livingA) || !(entB instanceof LivingEntity livingB)) return 0;

                return livingA.getDisplayName().getString().compareToIgnoreCase(livingB.getDisplayName().getString());
            });

            filteredMobs.addAll(matched);

            totalPages = (int) Math.ceil(filteredMobs.size() / 12.0);


            if (currentPage >= totalPages) {
                currentPage = Math.max(totalPages - 1, 0);
            }
        }




        @Override
        protected void init() {
            super.init();
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            searchBox = new TextFieldWidget(
                    textRenderer,
                    this.width / 2 - 96,
                    this.height / 2 - pageHeight / 2 - 25,
                    200,
                    18,
                    Text.literal("Search...")
            );
            searchBox.setPlaceholder(Text.literal("Search mobs..."));
            searchBox.setText(searchQuery); // 👈 restores previous search

            searchBox.setChangedListener(query -> {
                this.searchQuery = query.toLowerCase().trim();
                currentPage = 0;
                updateFilteredList();  // ✅ safe to call now
                updateButtons();       // ✅ safe to call now
            });

            this.addSelectableChild(searchBox);
            this.setInitialFocus(searchBox);

            int totalWidth = pageWidth * 2;
            int x = (this.width - totalWidth) / 2;
            int y = (this.height - pageHeight) / 2;
            int buttonY = y + pageHeight - 20;

            // ✅ Create buttons before updateFilteredList
            nextButton = new PageTurnButton(x + pageWidth * 2 - 130, buttonY, true, () -> {
                currentPage++;
                updateButtons();
            });

            backButton = new PageTurnButton(x + 106, buttonY, false, () -> {
                currentPage--;
                updateButtons();
            });

            // ✅ NOW it's safe to update filtered mobs and buttons
            updateFilteredList();
            updateButtons();
        }


        private void updateButtons() {
            nextButton.visible = currentPage < totalPages - 1;
            backButton.visible = currentPage > 0;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (MobSlot slot : mobSlots) {
                if (slot.isHovered((int) mouseX, (int) mouseY)) {
                    // ✅ Play click sound before switching screens
                    if (MinecraftClient.getInstance().player != null) {
                        float pitch = 0.95f + MinecraftClient.getInstance().world.random.nextFloat() * 0.1f;
                        MinecraftClient.getInstance().player.playSound(
                                SoundEvents.ITEM_BOOK_PAGE_TURN,
                                1.0f,
                                pitch
                        );
                    }

                    MinecraftClient.getInstance().setScreen(new MobDetailsScreen(slot.id, currentPage, searchQuery));


                    return true;
                }
            }

            nextButton.mouseClicked(mouseX, mouseY);
            backButton.mouseClicked(mouseX, mouseY);
            if (searchBox.mouseClicked(mouseX, mouseY, button)) return true;

            return super.mouseClicked(mouseX, mouseY, button);
        }

        private long nextTypingSoundTime = 0;

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (super.charTyped(chr, modifiers)) {
                long currentTime = System.currentTimeMillis();

                if (currentTime >= nextTypingSoundTime) {
                    float pitch = 0.95f + MinecraftClient.getInstance().world.random.nextFloat() * 0.1f;

                    MinecraftClient.getInstance().player.playSound(
                            JournalSounds.WRITING,
                            1.0f,
                            pitch
                    );

                    // Prevent another sound from playing until after 400ms (adjust to your .ogg duration)
                    nextTypingSoundTime = currentTime + 1000;
                }

                return true;
            }
            return false;
        }


        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            int totalWidth = pageWidth * 2;
            int x = (this.width - totalWidth) / 2;
            int y = (this.height - pageHeight) / 2;

            context.drawTexture(RIGHT_PAGE, x + 41, y, 0, 0, pageWidth, pageHeight);
            context.drawTexture(LEFT_PAGE, x + pageWidth / 2 + 118, y, 0, 0, pageWidth, pageHeight);
            searchBox.render(context, mouseX, mouseY, delta);

            renderMobGrid(context, x + 176, y + 38, mouseX, mouseY);
            nextButton.render(context, mouseX, mouseY);
            backButton.render(context, mouseX, mouseY);
        }

        private float walkCycle = 0.0f;

        private void renderMobGrid(DrawContext context, int leftStartX, int startY, int mouseX, int mouseY) {
            mobSlots.clear();

            MinecraftClient client = MinecraftClient.getInstance();
            World world = client.world;
            if (world == null) return;

            walkCycle += 0.09f;

            int columns = 2;
            int spacingX = 60;
            int spacingY = 45;
            int baseScale = 8;
            int hoverScale = 11;
            int boxWidth = 30;
            int boxHeight = 40;

            int leftPageStartX = leftStartX;
            int rightPageStartX = leftStartX + 140;
            List<Identifier> discoveredList = filteredMobs;

            if (discoveredList.isEmpty()) {
                context.drawTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        Text.literal("No mobs found."),
                        (this.width - 100) / 2 - 75,
                        startY - 20,
                        0xAAAAAA
                );

                return; // 🚫 Don't try to draw anything
            }
            int index = 0;
            int startIndex = currentPage * 12;
            int endIndex = Math.min(startIndex + 12, filteredMobs.size()); // <- ensures you don't go OOB
            for (int i = startIndex; i < endIndex; i++) {
                Identifier id = filteredMobs.get(i);

                boolean isRightPage = index >= 6;
                int localIndex = isRightPage ? index - 6 : index;

                int col = localIndex % columns;
                int row = localIndex / columns;

                int x = (isRightPage ? rightPageStartX : leftPageStartX) + col * spacingX;
                int y = startY + row * spacingY;
                mobSlots.add(new MobSlot(id, x - 15, y - 20, 30, 40));

                var type = Registries.ENTITY_TYPE.get(id);
                if (type == null || !type.isSummonable()) continue;

                var entity = type.create(world);
                if (!(entity instanceof LivingEntity living)) continue;

                double walkOffset = Math.sin(walkCycle + index) * 0.1;
                living.prevX = 0;
                living.lastRenderX = 0;
                living.setPos(walkOffset, 0, 0);
                living.prevYaw = living.getYaw();
                float yaw = (float)(Math.sin(walkCycle + index) * 10);
                living.bodyYaw = yaw;
                living.setYaw(yaw);
                living.headYaw = yaw;
                living.tick();

                boolean isHovered = mouseX >= x - boxWidth / 2 && mouseX <= x + boxWidth / 2 &&
                        mouseY >= y - boxHeight / 2 && mouseY <= y + boxHeight / 2;


                if (isHovered) {
                    double pulse = Math.sin(System.currentTimeMillis() / 150.0) * 0.15 + 0.25;
                    int alpha = (int) (pulse * 255);
                    int color = (alpha << 24) | 0x55FF55; // Greenish glow
                    context.fill(x - boxWidth / 2, y - boxHeight / 2, x + boxWidth / 2, y + boxHeight / 2, color);
                }

                int scale = isHovered ? hoverScale : baseScale;
                drawMob(context, x, y, scale, mouseX, mouseY, living);


                String name = living.getDisplayName().getString();
                float textScale = 0.75f;

                MatrixStack matrices = context.getMatrices();
                matrices.push();
                matrices.translate(x - 3, y + scale + 10, 0);
                matrices.scale(textScale, textScale, 1.0f);

                int adjustedTextWidth = (int) (client.textRenderer.getWidth(name) * textScale);
                context.drawText(client.textRenderer, name, -(adjustedTextWidth / 2), 0, 0x000000, false);
                matrices.pop();

                index++;
            }
        }

        private void drawMob(DrawContext context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity) {
            MinecraftClient client = MinecraftClient.getInstance();
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
            MatrixStack matrices = context.getMatrices();

            matrices.push();
            matrices.translate(x, y, 100.0);
            matrices.scale(scale, -scale, scale);
            matrices.translate(0.0, -1.5, 0.0);

            float angle = (System.currentTimeMillis() % 8000L) / 8000.0F * 360F;

            if (entity.getType().toString().contains("ender_dragon")) {
                entity.setPitch(0.0f);
                entity.prevYaw = 0.0f;
                entity.prevBodyYaw = 0.0f;
                entity.setYaw(0.0f);
                entity.setBodyYaw(0.0f);
                entity.setHeadYaw(0.0f);
            } else {
                entity.prevYaw = angle;
                entity.prevBodyYaw = angle;
                entity.setYaw(angle);
                entity.setBodyYaw(angle);
                entity.setPitch(0.0f);
                entity.setHeadYaw(angle);
            }
            entity.tick(); // after applying yaw values


            dispatcher.setRenderShadows(false);
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.getVertexConsumers(), 0xF000F0);
            context.draw();
            dispatcher.setRenderShadows(true);
            matrices.pop();
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
