package net.pixeldreamstudios.journal.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.events.JournalSounds;
import net.pixeldreamstudios.journal.mixin.LimbAnimatorAccessor;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.awt.*;
import java.util.*;
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

    public final List<MobSlot> mobSlots = new ArrayList<>();

    // 🧠 Lightweight page-based cache
    private final Map<Identifier, LivingEntity> currentPageMobMap = new HashMap<>();
    private final Map<Identifier, CachedPose> poseCache = new HashMap<>();
    private int renderFrameCounter = 0;
    private SortMode sortMode = SortMode.ALPHABETICAL;

    private enum SortMode {
        ALPHABETICAL,
        DATE_DISCOVERED,
        MOD_NAMESPACE
    }
    private static class CachedPose {
        float limbPos, yaw;
        int age;
        long lastUpdated;
    }

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
            LivingEntity living = currentPageMobMap.computeIfAbsent(id, key -> {
                var entityType = Registries.ENTITY_TYPE.get(key);
                return entityType != null ? (LivingEntity) entityType.create(MinecraftClient.getInstance().world) : null;
            });

            if (!(living instanceof LivingEntity entity)) continue;

            String entityName = entity.getDisplayName().getString().toLowerCase();
            if (nameMatch || entityName.contains(nameFilter) || nameFilter.isEmpty()) {
                filteredMobs.add(id);
            }
        }
        switch (sortMode) {
            case ALPHABETICAL -> {
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
            }
            case DATE_DISCOVERED -> {
                filteredMobs.sort(Comparator.comparingLong(
                        id -> -JournalClientData.DISCOVERED_TIME.getOrDefault(id, 0L)
                ));
            }
            case MOD_NAMESPACE -> {
                filteredMobs.sort((a, b) -> {
                    int nsCompare = a.getNamespace().compareToIgnoreCase(b.getNamespace());
                    if (nsCompare != 0) return nsCompare;

                    var typeA = Registries.ENTITY_TYPE.get(a);
                    var typeB = Registries.ENTITY_TYPE.get(b);

                    if (typeA == null || typeB == null) return 0;

                    var nameA = typeA.getName().getString();
                    var nameB = typeB.getName().getString();

                    return nameA.compareToIgnoreCase(nameB);
                });
            }
        }

        if (!JournalClientData.FAVORITE_MOBS.isEmpty()) {
            filteredMobs.sort((a, b) -> {
                boolean aFav = JournalClientData.FAVORITE_MOBS.contains(a);
                boolean bFav = JournalClientData.FAVORITE_MOBS.contains(b);
                if (aFav && !bFav) return -1;
                if (!aFav && bFav) return 1;
                return 0;
            });
        }

        totalPages = (int) Math.ceil(filteredMobs.size() / 12.0);
        if (currentPage >= totalPages) {
            currentPage = Math.max(totalPages - 1, 0);
        }

    }

    public void updateDiscoveredMobs() {
        updateFilteredList();
        updateButtons();
    }
    private int calculateDynamicScale(LivingEntity entity, int maxWidth, int maxHeight, int baseScale) {
        double width = entity.getWidth();
        double height = entity.getHeight();

        if (width == 0 || height == 0) return baseScale;

        // Add extra padding for visual margin (looks cleaner)
        double padding = 0.8;

        // Convert hitbox to pixel space and scale to fit
        double scaleX = (maxWidth / (width * 16.0)) * padding;
        double scaleY = (maxHeight / (height * 16.0)) * padding;

        double scale = Math.min(scaleX, scaleY);

        // Clamp to avoid mobs being way too small or exploding
        int clamped = (int) Math.max(8, Math.min(scale, baseScale));

        return clamped;
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
        int sortButtonX = this.width / 2 + 110;
        int sortButtonY = searchBox.getY();

        ButtonWidget sortButton = ButtonWidget.builder(
                        Text.literal("A→Z"),
                        btn -> {
                            sortMode = switch (sortMode) {
                                case ALPHABETICAL     -> SortMode.DATE_DISCOVERED;
                                case DATE_DISCOVERED  -> SortMode.MOD_NAMESPACE;
                                case MOD_NAMESPACE    -> SortMode.ALPHABETICAL;
                            };

                            String label = switch (sortMode) {
                                case ALPHABETICAL     -> "A→Z";
                                case DATE_DISCOVERED  -> "🕒";
                                case MOD_NAMESPACE    -> "@mod";
                            };

                            Text tooltipText = switch (sortMode) {
                                case ALPHABETICAL     -> Text.literal("Sort: A → Z");
                                case DATE_DISCOVERED  -> Text.literal("Sort: Recently Discovered");
                                case MOD_NAMESPACE    -> Text.literal("Sort: By Mod Namespace");
                            };

                            btn.setMessage(Text.literal(label));
                            btn.setTooltip(Tooltip.of(tooltipText));

                            updateFilteredList();
                        }
                ).dimensions(sortButtonX, sortButtonY, 30, 18)
                .tooltip(Tooltip.of(Text.literal("Sort: A → Z")))
                .build();



        this.addDrawableChild(sortButton);


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
    private static class Nameplate {
        String fullName;
        StringBuilder name;
        int centerX, topY;
        Rectangle bounds;
        int fullLength;
        boolean hovered = false;

        public Nameplate(String fullName, int centerX, int topY, TextRenderer renderer) {
            this.fullName = fullName;
            this.name = new StringBuilder(fullName);
            this.fullLength = fullName.length();
            this.centerX = centerX;
            this.topY = topY;
            this.bounds = calculateBounds(renderer);
        }

        public Rectangle calculateBounds(TextRenderer renderer) {
            String display = getDisplayName();
            int width = renderer.getWidth(display);
            return new Rectangle(centerX - width / 2, topY, width, 9);
        }

        public void trim() {
            if (name.length() > 3) {
                name.setLength(name.length() - 1);
            }
        }

        public String getDisplayName() {
            return hovered ? fullName : (name.length() < fullLength ? name + "..." : name.toString());
        }
    }




    private void renderMobGrid(DrawContext context, int leftStartX, int startY, int mouseX, int mouseY) {
        // ─── existing setup ───
        mobSlots.clear();
        renderFrameCounter = (renderFrameCounter + 1) % 3; // Only update pose every 3 frames
        boolean updatePose = renderFrameCounter == 0;

        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || filteredMobs.isEmpty()) return;

        int startIndex = currentPage * 12;
        int endIndex   = Math.min(startIndex + 12, filteredMobs.size());

        int columns    = 2;
        int spacingX   = 60;
        int spacingY   = 45;
        int baseScale  = 8;
        int hoverScale = 11;
        int boxWidth   = 30;
        int boxHeight  = 40;

        int leftPageStartX  = leftStartX;
        int rightPageStartX = leftPageStartX + 145;

        List<Nameplate> pendingNameplates = new ArrayList<>();

        // ─── NEW: disable shadows once ───
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);

        // ─── NEW: push once for entire batch ───
        MatrixStack matrices = context.getMatrices();
        matrices.push();

        // ─── draw each mob ───
        for (int i = startIndex; i < endIndex; i++) {
            Identifier id = filteredMobs.get(i);
            LivingEntity living = MobEntityCache.get(id, world);
            if (living == null) continue;

            boolean isRightPage = (i - startIndex) >= 6;
            int localIndex = (i - startIndex) % 6;
            int col = localIndex % columns;
            int row = localIndex / columns;

            int x = (isRightPage ? rightPageStartX : leftPageStartX) + col * spacingX;
            int y = startY + row * spacingY;

            mobSlots.add(new MobSlot(id, x - 15, y - 20, boxWidth, boxHeight));
            if (JournalClientData.FAVORITE_MOBS.contains(id)) {
                matrices.push();
                matrices.translate(x - boxWidth / 2 + 2, y - boxHeight / 2 + 2, 0);
                matrices.scale(0.75f, 0.75f, 1.0f);
                context.drawText(
                        MinecraftClient.getInstance().textRenderer,
                        Text.literal("★").styled(style -> style.withColor(0xFFFF55)), // bright yellow
                        0,
                        0,
                        0xFFFF55,
                        false
                );
                matrices.pop();
            }

            boolean isHovered = mouseX >= x - boxWidth/2 && mouseX <= x + boxWidth/2
                    && mouseY >= y - boxHeight/2 && mouseY <= y + boxHeight/2;
            if (isHovered) {
                double pulse = Math.sin(System.currentTimeMillis() / 150.0) * 0.15 + 0.25;
                int alpha = (int) (pulse * 255);
                int color = (alpha << 24) | 0x55FF55;
                context.fill(
                        x - boxWidth/2, y - boxHeight/2,
                        x + boxWidth/2, y + boxHeight/2,
                        color
                );
            }

            // Approximate on-screen check: inside client window bounds
            boolean isOnScreen = x + boxWidth / 2 >= 0 && x - boxWidth / 2 <= this.width &&
                    y + boxHeight / 2 >= 0 && y - boxHeight / 2 <= this.height;

            if (isOnScreen) {
                CachedPose pose = poseCache.computeIfAbsent(id, k -> new CachedPose());
                long now = System.currentTimeMillis();
                if (updatePose || now - pose.lastUpdated > 250) {
                    pose.limbPos     = (now % 10000L) / 1000.0f * 3f;
                    pose.yaw         = (now % 8000L)  / 8000.0f * 360F;
                    pose.age         = (int)(now / 50L);
                    pose.lastUpdated = now;
                }
                ((LimbAnimatorAccessor) living.limbAnimator).setPos(pose.limbPos);
                living.limbAnimator.updateLimbs(0.35f, 1f);
                living.bodyYaw = pose.yaw;
                living.setYaw(pose.yaw);
                living.setPitch(0.0f);
                living.headYaw = pose.yaw;
                living.age     = pose.age;
            }

            int scale = isHovered ? hoverScale : baseScale;
            int targetScale = calculateDynamicScale(living, boxWidth, boxHeight, isHovered ? hoverScale : baseScale);

            // drawMob still handles only per-mob push/pop
            drawMob(context, x, y, targetScale, mouseX, mouseY, living);


            Nameplate plate = new Nameplate(
                    living.getDisplayName().getString(),
                    x, y + scale + 10,
                    client.textRenderer
            );
            plate.hovered = isHovered;
            pendingNameplates.add(plate);
        }

        // ─── NEW: pop once after batch ───
        matrices.pop();
        // ─── NEW: restore shadows once ───
        dispatcher.setRenderShadows(true);

        // ─── existing nameplate draw ───
        drawNameplate(context, pendingNameplates, client.textRenderer);
    }


    private void drawNameplate(DrawContext context, List<Nameplate> plates, TextRenderer renderer) {
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < plates.size(); i++) {
                for (int j = i + 1; j < plates.size(); j++) {
                    Nameplate a = plates.get(i);
                    Nameplate b = plates.get(j);

                    if (a.bounds.intersects(b.bounds)) {
                        boolean trimmed = false;
                        int lenA = a.name.length();
                        int lenB = b.name.length();

                        // 1) Trim the longer text first
                        if (lenA > lenB && lenA > 3) {
                            a.trim();
                            a.bounds = a.calculateBounds(renderer);
                            trimmed = true;
                        } else if (lenB > lenA && lenB > 3) {
                            b.trim();
                            b.bounds = b.calculateBounds(renderer);
                            trimmed = true;
                        } else {
                            // 2) If same length (or both candidates), trim both as before
                            if (lenA > 3) {
                                a.trim();
                                a.bounds = a.calculateBounds(renderer);
                                trimmed = true;
                            }
                            if (lenB > 3) {
                                b.trim();
                                b.bounds = b.calculateBounds(renderer);
                                trimmed = true;
                            }
                        }

                        if (trimmed) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);

        // now actually draw all nameplates
        for (Nameplate plate : plates) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(plate.centerX, plate.topY, 0);
            matrices.scale(0.75f, 0.75f, 1f);

            String display = plate.getDisplayName();
            context.drawText(
                    renderer,
                    display,
                    -renderer.getWidth(display) / 2,
                    0,
                    0x000000,
                    false
            );
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

            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.getVertexConsumers(), 0xF000F0);
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
        // Don't manually call mouseClicked on widgets you've added via addDrawableChild!
        return super.mouseClicked(mouseX, mouseY, button);
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

        if (filteredMobs.isEmpty()) {
            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            String message = "No discovered mobs";

            int messageWidth = renderer.getWidth(message);
            int messageX = this.width / 2 - messageWidth / 2 - 75;
            int messageY = this.height / 2 - 70;

            context.drawText(renderer, message, messageX, messageY, 0x888888, false);
        } else {
            renderMobGrid(context, x + 176, y + 38, mouseX, mouseY);
        }
        context.draw();
        nextButton.render(context, mouseX, mouseY);
        backButton.render(context, mouseX, mouseY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
