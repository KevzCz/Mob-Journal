package net.pixeldreamstudios.journal.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.events.JournalSounds;
import net.pixeldreamstudios.journal.mixin.LimbAnimatorAccessor;

import java.util.*;

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

    public final List<MobSlot> mobSlots = new ArrayList<>();

    // 🧠 Lightweight page-based cache
    private final Map<Identifier, LivingEntity> currentPageMobMap = new HashMap<>();

    public static class MobSlot {
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
        currentPageMobMap.clear(); // 🔄 Refresh cache for new page

        String namespaceFilter = null;
        String nameFilter = "";

        for (String token : searchQuery.split("\\s+")) {
            if (token.startsWith("@")) {
                namespaceFilter = token.substring(1);
            } else {
                nameFilter += token + " ";
            }
        }

        nameFilter = nameFilter.trim().toLowerCase();

        for (Identifier id : JournalClientData.DISCOVERED) {
            if (JournalConfig.isBlacklisted(id)) continue;

            if (namespaceFilter != null && !id.getNamespace().toLowerCase().contains(namespaceFilter)) {
                continue;
            }

            var type = Registries.ENTITY_TYPE.get(id);
            if (type == null) continue;

            var nameMatch = id.toString().contains(nameFilter);
            var living = type.create(MinecraftClient.getInstance().world);
            if (!(living instanceof LivingEntity entity)) continue;

            String entityName = entity.getDisplayName().getString().toLowerCase();
            if (nameMatch || entityName.contains(nameFilter) || nameFilter.isEmpty()) {
                filteredMobs.add(id);
            }
        }

        filteredMobs.sort((a, b) -> {
            var typeA = Registries.ENTITY_TYPE.get(a);
            var typeB = Registries.ENTITY_TYPE.get(b);
            if (typeA == null || typeB == null) return 0;

            var world = MinecraftClient.getInstance().world;
            if (world == null) return 0;

            var entA = typeA.create(world);
            var entB = typeB.create(world);

            if (!(entA instanceof LivingEntity la) || !(entB instanceof LivingEntity lb)) return 0;
            return la.getDisplayName().getString().compareToIgnoreCase(lb.getDisplayName().getString());
        });

        totalPages = (int) Math.ceil(filteredMobs.size() / 12.0);
        if (currentPage >= totalPages) {
            currentPage = Math.max(totalPages - 1, 0);
        }
    }

    public void updateDiscoveredMobs() {
        updateFilteredList();
        updateButtons();
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
        searchBox.setText(searchQuery);
        searchBox.setChangedListener(query -> {
            this.searchQuery = query.toLowerCase().trim();
            currentPage = 0;
            updateFilteredList();
            updateButtons();
        });

        this.addSelectableChild(searchBox);
        this.setInitialFocus(searchBox);

        int totalWidth = pageWidth * 2;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - pageHeight) / 2;
        int buttonY = y + pageHeight - 20;

        nextButton = new PageTurnButton(x + pageWidth * 2 - 135, buttonY, true, () -> {
            currentPage++;
            updateFilteredList();
            updateButtons();
        });

        backButton = new PageTurnButton(x + 101, buttonY, false, () -> {
            currentPage--;
            updateFilteredList();
            updateButtons();
        });

        updateFilteredList();
        updateButtons();
    }

    private void updateButtons() {
        nextButton.visible = currentPage < totalPages - 1;
        backButton.visible = currentPage > 0;
    }

    private void renderMobGrid(DrawContext context, int leftStartX, int startY, int mouseX, int mouseY) {
        mobSlots.clear();

        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || filteredMobs.isEmpty()) return;

        int startIndex = currentPage * 12;
        int endIndex = Math.min(startIndex + 12, filteredMobs.size());

        int columns = 2;
        int spacingX = 60;
        int spacingY = 45;
        int baseScale = 8;
        int hoverScale = 11;
        int boxWidth = 30;
        int boxHeight = 40;

        int leftPageStartX = leftStartX;
        int rightPageStartX = leftPageStartX + 140;

        for (int i = startIndex; i < endIndex; i++) {
            Identifier id = filteredMobs.get(i);
            LivingEntity living = currentPageMobMap.computeIfAbsent(id, key -> {
                var type = Registries.ENTITY_TYPE.get(key);
                return (type != null && type.isSummonable()) ? (LivingEntity) type.create(world) : null;
            });

            if (living == null) continue;

            boolean isRightPage = (i - startIndex) >= 6;
            int localIndex = (i - startIndex) % 6;
            int col = localIndex % columns;
            int row = localIndex / columns;

            int x = (isRightPage ? rightPageStartX : leftPageStartX) + col * spacingX;
            int y = startY + row * spacingY;

            mobSlots.add(new MobSlot(id, x - 15, y - 20, 30, 40));

            boolean isHovered = mouseX >= x - boxWidth / 2 && mouseX <= x + boxWidth / 2 &&
                    mouseY >= y - boxHeight / 2 && mouseY <= y + boxHeight / 2;

            if (isHovered) {
                double pulse = Math.sin(System.currentTimeMillis() / 150.0) * 0.15 + 0.25;
                int alpha = (int) (pulse * 255);
                int color = (alpha << 24) | 0x55FF55;
                context.fill(x - boxWidth / 2, y - boxHeight / 2, x + boxWidth / 2, y + boxHeight / 2, color);
            }

            long time = System.currentTimeMillis();
            float walkSpeed = 3f;
            float walkPos = (time % 10000L) / 1000.0f * walkSpeed;
            ((LimbAnimatorAccessor) living.limbAnimator).setPos(walkPos);
            living.limbAnimator.updateLimbs(0.35f, 1f);
            float spin = (time % 8000L) / 8000.0f * 360F;
            living.bodyYaw = spin;
            living.setYaw(spin);
            living.setPitch(0.0f);
            living.headYaw = spin;
            living.age = (int)(time / 50L);

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

        try {
            dispatcher.setRenderShadows(false);
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.getVertexConsumers(), 0xF000F0);
            context.draw();
        } catch (Throwable t) {
            TextRenderer renderer = client.textRenderer;
            int textWidth = renderer.getWidth("Can't render mob");
            context.drawTextWithShadow(
                    renderer,
                    Text.literal("Can't render mob"),
                    x - textWidth / 2,
                    y - 10,
                    0xFF5555
            );
        } finally {
            dispatcher.setRenderShadows(true);
            matrices.pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (MobSlot slot : mobSlots) {
            if (slot.isHovered((int) mouseX, (int) mouseY)) {
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
        return searchBox.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    private long nextTypingSoundTime = 0;

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (super.charTyped(chr, modifiers)) {
            long currentTime = System.currentTimeMillis();

            if (currentTime >= nextTypingSoundTime && MinecraftClient.getInstance().player != null) {
                float pitch = 0.95f + MinecraftClient.getInstance().world.random.nextFloat() * 0.1f;

                MinecraftClient.getInstance().player.playSound(
                        JournalSounds.WRITING,
                        1.0f,
                        pitch
                );

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

    @Override
    public boolean shouldPause() {
        return false;
    }
}
