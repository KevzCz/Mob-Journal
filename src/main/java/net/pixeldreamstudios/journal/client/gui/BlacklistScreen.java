package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class BlacklistScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchBox;
    private String searchQuery = "";
    private final List<MobEntry> blacklistedEntries = new ArrayList<>();
    private final List<MobEntry> searchResults = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static final int MOB_SLOT_SIZE = 80;
    private static final int ROWS = 2;
    private static final int COLS = 5;
    private static final int SLOTS_PER_PAGE = ROWS * COLS;
    private static final int SPACING = 10;

    private ButtonWidget backButton;
    private final Map<Identifier, CachedPose> poseCache = new HashMap<>();
    private final Map<Identifier, LivingEntity> entityCache = new HashMap<>();

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

    private static class MobEntry {
        final Identifier id;
        final String name;
        final LivingEntity entity;
        int x, y, width, height;

        MobEntry(Identifier id, String name, LivingEntity entity) {
            this.id = id;
            this.name = name;
            this.entity = entity;
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    public BlacklistScreen(Screen parent) {
        super(Text.literal("Blacklist Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int centerX = this.width / 2;
        int topY = 20;

        searchBox = new TextFieldWidget(
                textRenderer,
                centerX - 100,
                topY,
                200,
                18,
                Text.literal("Search...")
        );
        searchBox.setPlaceholder(Text.literal("Search mobs to blacklist..."));
        searchBox.setText(searchQuery);
        searchBox.setChangedListener(query -> {
            this.searchQuery = query.toLowerCase().trim();
            scrollOffset = 0;
            updateSearchResults();
            updateMaxScroll();
        });
        this.addSelectableChild(searchBox);
        this.setInitialFocus(searchBox);

        backButton = ButtonWidget.builder(
                Text.literal("Back"),
                btn -> MinecraftClient.getInstance().setScreen(parent)
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build();
        this.addDrawableChild(backButton);

        preloadEntities();
        updateBlacklistEntries();
        updateSearchResults();
        updateMaxScroll();
    }

    private void preloadEntities() {
        var world = MinecraftClient.getInstance().world;
        if (world == null) return;

        for (Identifier id : JournalConfig.blacklistedMobs) {
            if (! entityCache.containsKey(id)) {
                LivingEntity entity = MobEntityCache.get(id, world);
                if (entity != null) {
                    entityCache.put(id, entity);
                }
            }
        }
    }

    private void updateBlacklistEntries() {
        blacklistedEntries.clear();
        var world = MinecraftClient.getInstance().world;
        if (world == null) return;

        for (Identifier id : JournalConfig.blacklistedMobs) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            String name = type != null ? type.getName().getString() : id.toString();

            LivingEntity entity = entityCache.computeIfAbsent(id, key -> MobEntityCache.get(key, world));

            if (entity != null) {
                blacklistedEntries.add(new MobEntry(id, name, entity));
            }
        }
        blacklistedEntries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    private void updateSearchResults() {
        searchResults.clear();
        if (searchQuery.isEmpty()) {
            return;
        }

        var world = MinecraftClient.getInstance().world;
        if (world == null) return;

        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            if (!  type.isSummonable()) continue;

            var entity = type.create(world);
            if (!(entity instanceof LivingEntity living)) continue;

            Identifier id = Registries.ENTITY_TYPE.getId(type);
            if (JournalConfig.isBlacklisted(id)) continue;

            String name = type.getName().getString().toLowerCase();
            String idStr = id.toString().toLowerCase();

            if (name.contains(searchQuery) || idStr.contains(searchQuery)) {
                searchResults.add(new MobEntry(id, type.getName().getString(), living));
            }
        }

        searchResults.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    private void updateMaxScroll() {
        List<MobEntry> activeList = searchQuery.isEmpty() ? blacklistedEntries : searchResults;
        int totalPages = (int) Math.ceil(activeList.size() / (double) SLOTS_PER_PAGE);
        maxScroll = Math.max(0, totalPages - 1);

        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<MobEntry> activeList = searchQuery.isEmpty() ? blacklistedEntries : searchResults;

        if (! activeList.isEmpty() && maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return searchBox.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int centerX = this.width / 2;

        context.drawCenteredTextWithShadow(renderer, "Blacklist Manager", centerX, 5, 0xFFFFFF);
        searchBox.render(context, mouseX, mouseY, delta);

        if (searchQuery.isEmpty()) {
            String pageInfo = (maxScroll > 0) ? " (Page " + (scrollOffset + 1) + "/" + (maxScroll + 1) + ")" : "";
            context.drawTextWithShadow(renderer, "Blacklisted Mobs (" + blacklistedEntries.size() + ")" + pageInfo, 20, 45, 0xFFFFFF);
            renderMobGrid(context, blacklistedEntries, 60, mouseX, mouseY, delta, true);
        } else {
            String pageInfo = (maxScroll > 0) ? " (Page " + (scrollOffset + 1) + "/" + (maxScroll + 1) + ")" : "";
            context.drawTextWithShadow(renderer, "Search Results (" + searchResults.size() + ")" + pageInfo, 20, 45, 0xFFFFFF);
            renderMobGrid(context, searchResults, 60, mouseX, mouseY, delta, false);
        }
    }

    private void renderMobGrid(DrawContext context, List<MobEntry> entries, int startY, int mouseX, int mouseY, float delta, boolean showRemove) {
        if (entries.isEmpty()) {
            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            String message = showRemove ? "No blacklisted mobs" : "No results found";
            context.drawCenteredTextWithShadow(renderer, message, this.width / 2, this.height / 2, 0x888888);
            return;
        }

        int gridWidth = COLS * (MOB_SLOT_SIZE + SPACING) - SPACING;
        int gridStartX = (this.width - gridWidth) / 2;

        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);

        int startIndex = scrollOffset * SLOTS_PER_PAGE;
        int endIndex = Math.min(entries.size(), startIndex + SLOTS_PER_PAGE);

        for (int i = startIndex; i < endIndex; i++) {
            MobEntry entry = entries.get(i);
            int localIndex = i - startIndex;
            int col = localIndex % COLS;
            int row = localIndex / COLS;

            int x = gridStartX + col * (MOB_SLOT_SIZE + SPACING);
            int y = startY + row * (MOB_SLOT_SIZE + SPACING);

            entry.x = x;
            entry.y = y;
            entry.width = MOB_SLOT_SIZE;
            entry.height = MOB_SLOT_SIZE;

            boolean hovered = entry.isHovered(mouseX, mouseY);

            context.fill(x, y, x + MOB_SLOT_SIZE, y + MOB_SLOT_SIZE, hovered ? 0x80555555 : 0x80222222);
            context.drawBorder(x, y, MOB_SLOT_SIZE, MOB_SLOT_SIZE, hovered ? 0xFFFFFFFF : 0xFF888888);

            if (entry.entity != null) {
                drawMob(context, entry.id, entry.entity, x + MOB_SLOT_SIZE / 2, y + MOB_SLOT_SIZE / 2 - 10, 20, delta);
            }

            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            String displayName = entry.name;
            int nameWidth = renderer.getWidth(displayName);

            if (nameWidth > MOB_SLOT_SIZE - 4) {
                displayName = renderer.trimToWidth(displayName, MOB_SLOT_SIZE - 8) + "..";
            }

            context.drawCenteredTextWithShadow(
                    renderer,
                    displayName,
                    x + MOB_SLOT_SIZE / 2,
                    y + MOB_SLOT_SIZE - 12,
                    0xFFFFFF
            );

            if (hovered) {
                int buttonY = y + MOB_SLOT_SIZE - 25;
                int buttonWidth = MOB_SLOT_SIZE - 10;
                int buttonHeight = 12;
                int buttonX = x + 5;

                if (showRemove) {
                    context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFFAA0000);
                    context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, 0xFFFFFFFF);
                    context.drawCenteredTextWithShadow(renderer, "Remove", buttonX + buttonWidth / 2, buttonY + 2, 0xFFFFFF);
                } else {
                    context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFF00AA00);
                    context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, 0xFFFFFFFF);
                    context.drawCenteredTextWithShadow(renderer, "Add", buttonX + buttonWidth / 2, buttonY + 2, 0xFFFFFF);
                }
            }
        }

        dispatcher.setRenderShadows(true);
    }

    private void drawMob(DrawContext context, Identifier mobId, LivingEntity entity, int x, int y, int baseScale, float delta) {
        JournalConfig.MobRenderConfig config = JournalConfig.getMobRenderConfig(mobId, true);

        int adjustedX = x + (int) config.xOffset;
        int adjustedY = y + (int) config.yOffset;
        int adjustedScale = (int) (baseScale * config.scale);

        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(adjustedX, adjustedY, 100.0);
        matrices.scale(adjustedScale, -adjustedScale, adjustedScale);
        matrices.translate(0.0, -1.5, 0.0);

        try {
            CachedPose pose = poseCache.computeIfAbsent(mobId, k -> new CachedPose());
            long now = System.currentTimeMillis();

            if (! pose.initialized) {
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

                entity.age = pose.age;
                entity.prevBodyYaw = pose.prevYaw;
                entity.bodyYaw = pose.yaw;
                entity.prevYaw = pose.prevYaw;
                entity.setYaw(pose.yaw);
                entity.setPitch(0.0f);
                entity.prevHeadYaw = pose.prevYaw;
                entity.headYaw = pose.yaw;

                if (entity instanceof EnderDragonEntity dragon) {
                    dragon.prevWingPosition = dragon.wingPosition;
                    dragon.wingPosition += 0.1f;
                    if (dragon.wingPosition > 1.0f) {
                        dragon.wingPosition = 0.0f;
                    }

                    dragon.prevYaw = pose.prevYaw;
                    dragon.setYaw(pose.yaw);
                    dragon.bodyYaw = pose.yaw;
                    dragon.prevBodyYaw = pose.prevYaw;
                    dragon.headYaw = pose.yaw;
                    dragon.prevHeadYaw = pose.prevYaw;
                }
            } else {
                entity.setYaw(0f);
                entity.setPitch(0f);
                entity.bodyYaw = 0f;
                entity.prevBodyYaw = 0f;
                entity.headYaw = 0f;
                entity.prevHeadYaw = 0f;
            }

            net.pixeldreamstudios.journal.client.gui.AnimationOverride.set(entity, limbSwing, prevLimbSwing, limbSwingAmount);

            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, delta, matrices, context.getVertexConsumers(), 0xF000F0);
        } catch (Throwable t) {
            matrices.pop();
            return;
        }

        matrices.pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<MobEntry> activeList = searchQuery.isEmpty() ? blacklistedEntries : searchResults;
        boolean showRemove = searchQuery.isEmpty();

        for (MobEntry entry : activeList) {
            if (entry.isHovered((int) mouseX, (int) mouseY)) {
                int buttonY = entry.y + MOB_SLOT_SIZE - 25;
                int buttonHeight = 12;

                if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    if (showRemove) {
                        JournalConfig.removeFromBlacklist(entry.id);
                        entityCache.remove(entry.id);
                        updateBlacklistEntries();
                        updateMaxScroll();
                    } else {
                        JournalConfig.addToBlacklist(entry.id);
                        updateBlacklistEntries();
                        updateSearchResults();
                        updateMaxScroll();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        entityCache.clear();
        poseCache.clear();
    }
}