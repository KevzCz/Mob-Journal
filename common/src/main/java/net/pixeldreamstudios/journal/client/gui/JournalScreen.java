package net.pixeldreamstudios.journal.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.events.JournalSounds;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class JournalScreen extends Screen {
    private static final ResourceLocation LEFT_PAGE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/book.png");
    private static final ResourceLocation RIGHT_PAGE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/book_flipped.png");
    private static final int TEXTURE_SIZE = 256;

    private EditBox searchBox;
    private String searchQuery = "";
    private final List<ResourceLocation> filteredMobs = new ArrayList<>();
    private final int pageWidth = 276;
    private final int pageHeight = 180;
    private int currentPage = 0;
    private int totalPages = 0;

    private PageTurnButton nextButton;
    private PageTurnButton backButton;

    public final List<MobSlot> mobSlots = new ArrayList<>();

    private final Map<ResourceLocation, LivingEntity> currentPageMobMap = new HashMap<>();
    private final Map<ResourceLocation, CachedPose> poseCache = new HashMap<>();
    private SortMode sortMode = SortMode.ALPHABETICAL;

    private enum SortMode {
        ALPHABETICAL,
        DATE_DISCOVERED,
        MOD_NAMESPACE
    }

    private static class CachedPose {
        float yaw;
        float prevYaw;
        int age;
        long lastUpdated;
        float limbSwing;
        float limbSwingAmount;
        boolean initialized;

        CachedPose() {
            this.yaw = 0f;
            this.prevYaw = 0f;
            this.limbSwing = 0f;
            this.limbSwingAmount = 1.0f;
            this.initialized = false;
            this.lastUpdated = System.currentTimeMillis();
        }
    }

    public static class MobSlot {
        ResourceLocation id;
        int x, y, width, height;

        MobSlot(ResourceLocation id, int x, int y, int width, int height) {
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
        super(Component.literal("Mob Journal"));
        this.currentPage = initialPage;
        this.searchQuery = searchQuery == null ? "" : searchQuery;
    }

    private void updateFilteredList() {
        filteredMobs.clear();
        currentPageMobMap.clear();

        String namespaceFilter = null;
        String tagFilter = null;
        String nameFilter = "";

        for (String token : searchQuery.split("\\s+")) {
            if (token.startsWith("@")) {
                namespaceFilter = token.substring(1);
            } else if (token.startsWith("#")) {
                tagFilter = token.substring(1);
            } else {
                nameFilter += token + " ";
            }
        }

        nameFilter = nameFilter.trim().toLowerCase();

        for (ResourceLocation id : JournalClientData.DISCOVERED) {
            if (JournalConfig.isBlacklisted(id)) continue;

            if (namespaceFilter != null && !id.getNamespace().toLowerCase().contains(namespaceFilter)) {
                continue;
            }

            var type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type == null) continue;

            if (tagFilter != null) {
                boolean matchesTag = false;
                for (var tagKey : type.builtInRegistryHolder().tags().toList()) {
                    if (tagKey.location().toString().toLowerCase().contains(tagFilter)) {
                        matchesTag = true;
                        break;
                    }
                }
                if (!matchesTag) continue;
            }

            var nameMatch = id.toString().contains(nameFilter);
            LivingEntity living = currentPageMobMap.computeIfAbsent(id, key -> {
                var entityType = BuiltInRegistries.ENTITY_TYPE.get(key);
                if (entityType == null) return null;
                var created = entityType.create(Minecraft.getInstance().level);
                return created instanceof LivingEntity le ? le : null;
            });

            if (living == null) continue;

            String entityName = living.getDisplayName().getString().toLowerCase();
            if (nameMatch || entityName.contains(nameFilter) || nameFilter.isEmpty()) {
                filteredMobs.add(id);
            }
        }
        switch (sortMode) {
            case ALPHABETICAL -> {
                filteredMobs.sort((a, b) -> {
                    var typeA = BuiltInRegistries.ENTITY_TYPE.get(a);
                    var typeB = BuiltInRegistries.ENTITY_TYPE.get(b);
                    if (typeA == null || typeB == null) return 0;

                    var level = Minecraft.getInstance().level;
                    if (level == null) return 0;

                    var entA = typeA.create(level);
                    var entB = typeB.create(level);

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

                    var typeA = BuiltInRegistries.ENTITY_TYPE.get(a);
                    var typeB = BuiltInRegistries.ENTITY_TYPE.get(b);

                    if (typeA == null || typeB == null) return 0;

                    var nameA = typeA.getDescription().getString();
                    var nameB = typeB.getDescription().getString();

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
        double width = entity.getBbWidth();
        double height = entity.getBbHeight();

        if (width == 0 || height == 0) return baseScale;

        double padding = 0.8;

        double scaleX = (maxWidth / (width * 16.0)) * padding;
        double scaleY = (maxHeight / (height * 16.0)) * padding;

        double scale = Math.min(scaleX, scaleY);

        return (int) Math.max(8, Math.min(scale, baseScale));
    }

    @Override
    protected void init() {
        super.init();
        Font font = Minecraft.getInstance().font;

        searchBox = new EditBox(
                font,
                this.width / 2 - 96,
                this.height / 2 - pageHeight / 2 - 25,
                200,
                18,
                Component.literal("Search...")
        );
        searchBox.setHint(Component.literal("Search mobs... (@mod #tag)"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            this.searchQuery = query.toLowerCase().trim();
            currentPage = 0;
            updateFilteredList();
            updateButtons();
        });
        int sortButtonX = this.width / 2 + 110;
        int sortButtonY = searchBox.getY();

        Button sortButton = Button.builder(
                        Component.literal("A→Z"),
                        btn -> {
                            sortMode = switch (sortMode) {
                                case ALPHABETICAL -> SortMode.DATE_DISCOVERED;
                                case DATE_DISCOVERED -> SortMode.MOD_NAMESPACE;
                                case MOD_NAMESPACE -> SortMode.ALPHABETICAL;
                            };

                            String label = switch (sortMode) {
                                case ALPHABETICAL -> "A→Z";
                                case DATE_DISCOVERED -> "🕒";
                                case MOD_NAMESPACE -> "@mod";
                            };

                            Component tooltipText = switch (sortMode) {
                                case ALPHABETICAL -> Component.literal("Sort:    A → Z");
                                case DATE_DISCOVERED -> Component.literal("Sort:  Recently Discovered");
                                case MOD_NAMESPACE -> Component.literal("Sort:  By Mod Namespace");
                            };

                            btn.setMessage(Component.literal(label));
                            btn.setTooltip(Tooltip.create(tooltipText));

                            updateFilteredList();
                        }
                ).bounds(sortButtonX, sortButtonY, 30, 18)
                .tooltip(Tooltip.create(Component.literal("Sort:    A → Z")))
                .build();

        this.addRenderableWidget(sortButton);
        this.addWidget(searchBox);
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

        public Nameplate(String fullName, int centerX, int topY, Font renderer) {
            this.fullName = fullName;
            this.name = new StringBuilder(fullName);
            this.fullLength = fullName.length();
            this.centerX = centerX;
            this.topY = topY;
            this.bounds = calculateBounds(renderer);
        }

        public Rectangle calculateBounds(Font renderer) {
            String display = getDisplayName();
            int width = renderer.width(display);
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

    private void renderMobGrid(GuiGraphics context, int leftStartX, int startY, int mouseX, int mouseY, float delta) {
        mobSlots.clear();

        Minecraft client = Minecraft.getInstance();
        Level level = client.level;
        if (level == null || filteredMobs.isEmpty()) return;

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
        int rightPageStartX = leftPageStartX + 145;

        List<Nameplate> pendingNameplates = new ArrayList<>();

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        PoseStack matrices = context.pose();
        matrices.pushPose();

        for (int i = startIndex; i < endIndex; i++) {
            ResourceLocation id = filteredMobs.get(i);
            LivingEntity living = MobEntityCache.get(id, level);
            if (living == null) continue;

            boolean isRightPage = (i - startIndex) >= 6;
            int localIndex = (i - startIndex) % 6;
            int col = localIndex % columns;
            int row = localIndex / columns;

            int x = (isRightPage ? rightPageStartX : leftPageStartX) + col * spacingX;
            int y = startY + row * spacingY;

            mobSlots.add(new MobSlot(id, x - 15, y - 20, boxWidth, boxHeight));
            if (JournalClientData.FAVORITE_MOBS.contains(id)) {
                matrices.pushPose();
                matrices.translate(x - boxWidth / 2 + 2, y - boxHeight / 2 + 2, 0);
                matrices.scale(0.75f, 0.75f, 1.0f);
                context.drawString(
                        Minecraft.getInstance().font,
                        Component.literal("★").withStyle(style -> style.withColor(0xFFFF55)),
                        0,
                        0,
                        0xFFFF55,
                        false
                );
                matrices.popPose();
            }

            boolean isHovered = mouseX >= x - boxWidth / 2 && mouseX <= x + boxWidth / 2
                    && mouseY >= y - boxHeight / 2 && mouseY <= y + boxHeight / 2;
            if (isHovered) {
                double pulse = Math.sin(System.currentTimeMillis() / 150.0) * 0.15 + 0.25;
                int alpha = (int) (pulse * 255);
                int color = (alpha << 24) | 0x55FF55;
                context.fill(
                        x - boxWidth / 2, y - boxHeight / 2,
                        x + boxWidth / 2, y + boxHeight / 2,
                        color
                );
            }

            boolean isOnScreen = x + boxWidth / 2 >= 0 && x - boxWidth / 2 <= this.width &&
                    y + boxHeight / 2 >= 0 && y - boxHeight / 2 <= this.height;

            if (isOnScreen) {
                CachedPose pose = poseCache.computeIfAbsent(id, k -> new CachedPose());
                long now = System.currentTimeMillis();
                JournalConfig.MobRenderConfig config = JournalConfig.getMobRenderConfig(id, true);

                if (!pose.initialized) {
                    pose.limbSwingAmount = config.speed;
                    pose.initialized = true;
                }

                float prevLimbSwing = pose.limbSwing;

                long elapsed = now - pose.lastUpdated;
                if (elapsed > 0) {
                    pose.limbSwing += (elapsed / 1000.0f) * config.smoothing;
                    pose.lastUpdated = now;
                }

                pose.prevYaw = pose.yaw;
                pose.yaw = (now % 8000L) / 8000.0f * 360F;
                pose.age = (int) (now / 50L);

                living.tickCount = pose.age;
                living.yBodyRotO = pose.prevYaw;
                living.yBodyRot = pose.yaw;
                living.yRotO = pose.prevYaw;
                living.setYRot(pose.yaw);
                living.setXRot(0.0f);
                living.yHeadRotO = pose.prevYaw;
                living.yHeadRot = pose.yaw;

                if (living instanceof EnderDragon dragon) {
                    dragon.oFlapTime = dragon.flapTime;
                    dragon.flapTime += 0.1f;
                    if (dragon.flapTime > 1.0f) {
                        dragon.flapTime = 0.0f;
                    }

                    dragon.yRotO = pose.prevYaw;
                    dragon.setYRot(pose.yaw);
                    dragon.yBodyRot = pose.yaw;
                    dragon.yBodyRotO = pose.prevYaw;
                    dragon.yHeadRot = pose.yaw;
                    dragon.yHeadRotO = pose.prevYaw;
                }

                AnimationOverride.set(living, pose.limbSwing, prevLimbSwing, config.speed);
            }

            int scale = isHovered ? hoverScale : baseScale;
            int targetScale = calculateDynamicScale(living, boxWidth, boxHeight, isHovered ? hoverScale : baseScale);

            drawMob(context, x, y, targetScale, mouseX, mouseY, living, delta);

            Nameplate plate = new Nameplate(
                    living.getDisplayName().getString(),
                    x, y + scale + 10,
                    client.font
            );
            plate.hovered = isHovered;
            pendingNameplates.add(plate);
        }
        matrices.popPose();
        dispatcher.setRenderShadow(true);
        drawNameplate(context, pendingNameplates, client.font);
    }

    private void drawNameplate(GuiGraphics context, List<Nameplate> plates, Font renderer) {
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
                        if (lenA > lenB && lenA > 3) {
                            a.trim();
                            a.bounds = a.calculateBounds(renderer);
                            trimmed = true;
                        } else if (lenB > lenA && lenB > 3) {
                            b.trim();
                            b.bounds = b.calculateBounds(renderer);
                            trimmed = true;
                        } else {
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
        for (Nameplate plate : plates) {
            PoseStack matrices = context.pose();
            matrices.pushPose();
            matrices.translate(plate.centerX, plate.topY, 0);
            matrices.scale(0.75f, 0.75f, 1f);

            String display = plate.getDisplayName();
            context.drawString(
                    renderer,
                    display,
                    -renderer.width(display) / 2,
                    0,
                    0x000000,
                    false
            );
            matrices.popPose();
        }
    }

    private void drawMob(GuiGraphics context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity, float delta) {
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        JournalConfig.MobRenderConfig config = JournalConfig.getMobRenderConfig(mobId, true);

        int adjustedX = x + (int) config.xOffset;
        int adjustedY = y + (int) config.yOffset;
        int adjustedScale = (int) (scale * config.scale);

        Minecraft client = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PoseStack matrices = context.pose();

        matrices.pushPose();
        matrices.translate(adjustedX, adjustedY, 100.0);
        matrices.scale(adjustedScale, -adjustedScale, adjustedScale);
        matrices.translate(0.0, -1.5, 0.0);

        try {
            CachedPose pose = poseCache.computeIfAbsent(mobId, k -> new CachedPose());
            long now = System.currentTimeMillis();

            if (!pose.initialized) {
                pose.limbSwingAmount = config.speed;
                pose.initialized = true;
            }

            float limbSwing = 0f;
            float prevLimbSwing = 0f;
            float limbSwingAmount = 0f;

            switch (config.animationMode) {
                case IDLE -> {
                    long elapsed = now - pose.lastUpdated;
                    if (elapsed > 0) {
                        pose.limbSwing += (elapsed / 1000.0f) * (config.smoothing * 0.3f);
                        pose.lastUpdated = now;
                    }
                    limbSwing = (float) Math.sin(pose.limbSwing * 0.3f) * 0.5f;
                    prevLimbSwing = (float) Math.sin((pose.limbSwing - 0.1f) * 0.3f) * 0.5f;
                    limbSwingAmount = 0.1f;
                }
                case WALKING -> {
                    prevLimbSwing = pose.limbSwing;
                    long elapsed = now - pose.lastUpdated;
                    if (elapsed > 0) {
                        pose.limbSwing += (elapsed / 1000.0f) * config.smoothing;
                        pose.lastUpdated = now;
                    }
                    limbSwing = pose.limbSwing;
                    limbSwingAmount = config.speed;
                }
                case STATIC -> {
                    limbSwing = 0f;
                    prevLimbSwing = 0f;
                    limbSwingAmount = 0f;
                }
            }

            if (config.animationMode != JournalConfig.AnimationMode.STATIC) {
                pose.prevYaw = pose.yaw;
                pose.yaw = (now % 8000L) / 8000.0f * 360F;
                pose.age = (int) (now / 50L);

                entity.tickCount = pose.age;
                entity.yBodyRotO = pose.prevYaw;
                entity.yBodyRot = pose.yaw;
                entity.yRotO = pose.prevYaw;
                entity.setYRot(pose.yaw);
                entity.setXRot(0.0f);
                entity.yHeadRotO = pose.prevYaw;
                entity.yHeadRot = pose.yaw;

                if (entity instanceof EnderDragon dragon) {
                    dragon.oFlapTime = dragon.flapTime;
                    dragon.flapTime += 0.1f;
                    if (dragon.flapTime > 1.0f) {
                        dragon.flapTime = 0.0f;
                    }

                    dragon.yRotO = pose.prevYaw;
                    dragon.setYRot(pose.yaw);
                    dragon.yBodyRot = pose.yaw;
                    dragon.yBodyRotO = pose.prevYaw;
                    dragon.yHeadRot = pose.yaw;
                    dragon.yHeadRotO = pose.prevYaw;
                }
            } else {
                entity.setYRot(0f);
                entity.setXRot(0f);
                entity.yBodyRot = 0f;
                entity.yBodyRotO = 0f;
                entity.yHeadRot = 0f;
                entity.yHeadRotO = 0f;
            }

            AnimationOverride.set(entity, limbSwing, prevLimbSwing, limbSwingAmount);

            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, delta, matrices, context.bufferSource(), 0xF000F0);
        } catch (Throwable t) {
            matrices.popPose();
            Font renderer = client.font;
            String errorText = "Can't render mob";
            int textWidth = renderer.width(errorText);
            context.drawString(
                    renderer,
                    Component.literal(errorText),
                    adjustedX - textWidth / 2,
                    adjustedY - 10,
                    0xFF5555,
                    true
            );
            return;
        }

        matrices.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (MobSlot slot : mobSlots) {
            if (slot.isHovered((int) mouseX, (int) mouseY)) {
                boolean altPressed = Screen.hasAltDown();
                boolean shiftPressed = Screen.hasShiftDown();

                if (altPressed && shiftPressed && button == 0) {
                    JournalConfig.addToBlacklist(slot.id);
                    JournalClientData.DISCOVERED.remove(slot.id);
                    updateFilteredList();
                    updateButtons();

                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§eBlacklisted:  §f" + slot.id.toString()),
                                true
                        );
                    }
                    return true;
                }

                if (altPressed && !shiftPressed && button == 0) {
                    Minecraft.getInstance().setScreen(
                            new MobRenderConfigScreen(this, slot.id, true)
                    );
                    return true;
                }

                if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
                    float pitch = 0.95f + Minecraft.getInstance().level.random.nextFloat() * 0.1f;
                    Minecraft.getInstance().player.playSound(
                            SoundEvents.BOOK_PAGE_TURN,
                            1.0f,
                            pitch
                    );
                }

                Minecraft.getInstance().setScreen(new MobDetailsScreen(slot.id, currentPage, searchQuery));
                return true;
            }
        }
        nextButton.mouseClicked(mouseX, mouseY);
        backButton.mouseClicked(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private long nextTypingSoundTime = 0;

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (super.charTyped(chr, modifiers)) {
            long currentTime = System.currentTimeMillis();

            if (currentTime >= nextTypingSoundTime
                    && Minecraft.getInstance().player != null
                    && Minecraft.getInstance().level != null) {
                float pitch = 0.95f + Minecraft.getInstance().level.random.nextFloat() * 0.1f;

                Minecraft.getInstance().player.playSound(
                        JournalSounds.WRITING.get(),
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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int totalWidth = pageWidth * 2;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - pageHeight) / 2;

        context.blit(RIGHT_PAGE, x + 41, y, 0, 0, pageWidth, pageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        context.blit(LEFT_PAGE, x + pageWidth / 2 + 118, y, 0, 0, pageWidth, pageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        searchBox.render(context, mouseX, mouseY, delta);

        if (filteredMobs.isEmpty()) {
            Font renderer = Minecraft.getInstance().font;
            String message = "No discovered mobs";

            int messageWidth = renderer.width(message);
            int messageX = this.width / 2 - messageWidth / 2 - 75;
            int messageY = this.height / 2 - 70;

            context.drawString(renderer, message, messageX, messageY, 0x888888, false);
        } else {
            renderMobGrid(context, x + 176, y + 38, mouseX, mouseY, delta);
        }
        context.flush();
        nextButton.render(context, mouseX, mouseY);
        backButton.render(context, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
