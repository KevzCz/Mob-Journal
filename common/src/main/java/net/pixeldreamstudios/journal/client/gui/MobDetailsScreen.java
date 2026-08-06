package net.pixeldreamstudios.journal.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.data.MobDescriptionLoader;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.RequestMobDropsPayload;
import net.pixeldreamstudios.journal.network.ToggleFavoritePayload;
import net.pixeldreamstudios.journal.util.MarkdownParser;
import net.pixeldreamstudios.journal.util.MarkdownParser.ParsedLine;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class MobDetailsScreen extends Screen {
    private static final ResourceLocation LEFT_PAGE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/book.png");
    private static final ResourceLocation RIGHT_PAGE =
            ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID, "textures/book_flipped.png");
    private static final int TEXTURE_SIZE = 256;

    private final int returnPage;
    private boolean isFavorite;
    private Button favButton;
    private final ResourceLocation mobId;
    private LivingEntity mob;
    private PageTurnButton backButton;
    private DetailPageTurnButton nextButton;
    private DetailPageTurnButton backDescButton;

    private int descPage = 0;
    private List<List<List<ParsedLine>>> paginatedLines = new ArrayList<>();
    private final String returnQuery;
    private final int mobSlotW = 120, mobSlotH = 140;
    private final int descSlotW = 110, descSlotH = 130;
    private final Map<ResourceLocation, CachedPose> poseCache = new HashMap<>();
    private boolean showAllDrops = false;
    private int expandButtonX = -1;
    private int expandButtonY = -1;
    private int expandButtonWidth = -1;
    private int expandButtonHeight = -1;
    private int mobRenderX = -1;
    private int mobRenderY = -1;
    private int mobRenderWidth = -1;
    private int mobRenderHeight = -1;

    private boolean isDraggingMob = false;
    private double lastDragX = 0;
    private float manualRotation = 0f;

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

    public MobDetailsScreen(ResourceLocation mobId, int returnPage, String returnQuery) {
        super(Component.literal("Mob Info"));
        this.mobId = mobId;
        this.returnPage = returnPage;
        this.returnQuery = returnQuery;
    }

    public void rebuildWithDrops() {
        List<List<ParsedLine>> allLines = mob != null
                ? MobDescriptionLoader.getDescription(mobId, mob)
                : List.of(List.of(new ParsedLine(Component.literal("§cUnknown mob"))));

        List<ParsedLine> dropIcons = new ArrayList<>();
        int maxDrops = showAllDrops ? JournalClientData.LAST_DROPS.size() : Math.min(6, JournalClientData.LAST_DROPS.size());
        for (int i = 0; i < maxDrops; i++) {
            ItemStack stack = JournalClientData.LAST_DROPS.get(i);
            ParsedLine icon = new ParsedLine(stack);
            icon.scale = 1.0f;
            dropIcons.add(icon);
        }
        if (dropIcons.isEmpty()) {
            dropIcons.add(new ParsedLine(Component.literal("§7(No known drops)")));
        }

        for (List<ParsedLine> line : allLines) {
            for (int i = 0; i < line.size(); i++) {
                ParsedLine part = line.get(i);
                if (part.isText() && part.text.getString().contains("{INJECT_LOOT_DROPS}")) {
                    line.remove(i);
                    line.addAll(i, dropIcons);
                    break;
                }
            }
        }

        this.descPage = 0;
        paginateDescription(allLines);
        updatePageButtons();
    }

    private String formatModName(String namespace) {
        String[] parts = namespace.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)));
                builder.append(part.substring(1));
                builder.append(" ");
            }
        }
        return builder.toString().trim();
    }

    @Override
    protected void init() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        this.isFavorite = JournalClientData.FAVORITE_MOBS.contains(mobId);

        if (level != null) {
            LivingEntity cached = MobEntityCache.get(mobId, level);
            if (cached != null) this.mob = cached;
        }

        JournalClientData.LAST_DROPS.clear();
        NetworkManager.sendToServer(new RequestMobDropsPayload(mobId));

        List<List<ParsedLine>> allLines = mob != null
                ? MobDescriptionLoader.getDescription(mobId, mob)
                : List.of(List.of(new ParsedLine(Component.literal("§cUnknown mob"))));

        if (MarkdownParser.containsPlaceholder(allLines, "{getLootDrops}")) {
            List<ParsedLine> dropIcons = new ArrayList<>();
            for (ItemStack stack : JournalClientData.LAST_DROPS) {
                ParsedLine icon = new ParsedLine(stack);
                icon.scale = 1.0f;
                dropIcons.add(icon);
            }
            if (dropIcons.isEmpty()) {
                dropIcons.add(new ParsedLine(Component.literal("§7(No known drops)")));
            }
            MarkdownParser.replacePlaceholder(allLines, "{getLootDrops}", dropIcons);
        }

        paginateDescription(allLines);

        int pageWidth = 276;
        int pageHeight = 180;
        int totalWidth = pageWidth * 2;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - pageHeight) / 2;

        this.favButton = Button.builder(
                Component.literal(isFavorite ? "★" : "☆").withStyle(style -> style.withColor(0xFFFF55)),
                btn -> {
                    isFavorite = !isFavorite;
                    NetworkManager.sendToServer(new ToggleFavoritePayload(mobId, isFavorite));
                    btn.setMessage(Component.literal(isFavorite ? "★" : "☆").withStyle(style -> style.withColor(0xFFFF55)));
                }
        ).bounds(0, 0, 18, 18).tooltip(Tooltip.create(Component.literal("Toggle Favorite"))).build();
        this.addRenderableWidget(favButton);

        int buttonY = y + pageHeight - 20;
        backButton = new PageTurnButton(x + 101, buttonY, false, () -> {
            Minecraft.getInstance().setScreen(new JournalScreen(returnPage, returnQuery));
        });

        backDescButton = new DetailPageTurnButton(x + 385, buttonY, false, () -> {
            descPage--;
            updatePageButtons();
        });

        nextButton = new DetailPageTurnButton(x + pageWidth * 2 - 135, buttonY, true, () -> {
            descPage++;
            updatePageButtons();
        });

        updatePageButtons();
    }

    private void paginateDescription(List<List<ParsedLine>> lines) {
        paginatedLines.clear();

        float baseScale = 0.85f;
        int maxHeight = (int) (descSlotH / baseScale) - 10;
        int wrapWidth = (int) (descSlotW / baseScale) - 10;

        Font renderer = Minecraft.getInstance().font;
        List<List<ParsedLine>> currentPage = new ArrayList<>();
        int currentHeight = 0;
        boolean insideLootSection = false;

        for (int rowIndex = 0; rowIndex < lines.size(); rowIndex++) {
            List<ParsedLine> inputRow = lines.get(rowIndex);

            boolean rowIsLoot = inputRow.stream().anyMatch(part ->
                    part.isItem() && JournalClientData.LAST_DROPS.stream()
                            .anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, part.item))
            );

            if (inputRow.stream().anyMatch(p -> p.isText() && p.text.getString().replace("§", "").equalsIgnoreCase("Drops"))) {
                insideLootSection = true;
            }

            List<ParsedLine> currentLine = new ArrayList<>();
            int currentLineWidth = 0;
            int currentLineHeight = 0;

            for (ParsedLine part : inputRow) {
                if (part.isText()) {
                    String text = part.text.getString();
                    int textWidth = renderer.width(text);

                    if (textWidth > wrapWidth) {
                        List<ParsedLine> wrappedParts = wrapTextPart(part, wrapWidth, renderer);
                        for (ParsedLine wrappedPart : wrappedParts) {
                            int width = renderer.width(wrappedPart.text);
                            int height = renderer.lineHeight;

                            if (currentLineWidth + width > wrapWidth && !currentLine.isEmpty()) {
                                if (currentHeight + currentLineHeight > maxHeight) {
                                    paginatedLines.add(currentPage);
                                    currentPage = new ArrayList<>();
                                    currentHeight = 0;
                                }
                                currentPage.add(currentLine);
                                currentHeight += currentLineHeight + 4;
                                currentLine = new ArrayList<>();
                                currentLineWidth = 0;
                                currentLineHeight = 0;
                            }

                            currentLine.add(wrappedPart);
                            currentLineWidth += width + 2;
                            currentLineHeight = Math.max(currentLineHeight, height);
                        }
                        continue;
                    }
                }

                float scale = part.scale <= 0 ? 1.0f : part.scale;
                int width = part.isText() ? renderer.width(part.text) : (int) (16 * scale);
                int height = part.isText() ? renderer.lineHeight : (int) (16 * scale);

                if (currentLineWidth + width > wrapWidth && !currentLine.isEmpty()) {
                    if (currentHeight + currentLineHeight > maxHeight) {
                        paginatedLines.add(currentPage);
                        currentPage = new ArrayList<>();
                        currentHeight = 0;
                    }
                    currentPage.add(currentLine);
                    currentHeight += currentLineHeight + 4;
                    currentLine = new ArrayList<>();
                    currentLineWidth = 0;
                    currentLineHeight = 0;
                }

                currentLine.add(part);
                currentLineWidth += width + 2;
                currentLineHeight = Math.max(currentLineHeight, height);
            }

            if (!currentLine.isEmpty()) {
                if (currentHeight + currentLineHeight > maxHeight && !currentPage.isEmpty()) {
                    paginatedLines.add(currentPage);
                    currentPage = new ArrayList<>();
                    currentHeight = 0;
                }
                currentPage.add(currentLine);
                currentHeight += currentLineHeight + 4;
            }

            boolean nextRowIsNotLoot = (rowIndex + 1 >= lines.size()) ||
                    (lines.get(rowIndex + 1).stream().noneMatch(p -> p.isItem()));

            if (insideLootSection && rowIsLoot && nextRowIsNotLoot) {
                if (JournalClientData.LAST_DROPS.size() > 5) {
                    ParsedLine expandButton = new ParsedLine(Component.literal("{EXPAND_COLLAPSE}"));
                    List<ParsedLine> expandRow = List.of(expandButton);

                    if (currentHeight + renderer.lineHeight > maxHeight && !currentPage.isEmpty()) {
                        paginatedLines.add(currentPage);
                        currentPage = new ArrayList<>();
                        currentHeight = 0;
                    }
                    currentPage.add(expandRow);
                    currentHeight += renderer.lineHeight + 4;
                }
                insideLootSection = false;
            }
        }

        if (!currentPage.isEmpty()) {
            paginatedLines.add(currentPage);
        }
        if (paginatedLines.isEmpty()) {
            paginatedLines.add(List.of(List.of(new ParsedLine(Component.literal("§cNo content")))));
        }
    }

    private List<ParsedLine> wrapTextPart(ParsedLine original, int maxWidth, Font renderer) {
        List<ParsedLine> result = new ArrayList<>();

        List<FormattedCharSequence> wrappedLines = renderer.split(original.text, maxWidth);

        for (FormattedCharSequence orderedText : wrappedLines) {
            StringBuilder sb = new StringBuilder();
            orderedText.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });

            Component wrappedText = Component.literal(sb.toString()).setStyle(original.text.getStyle());
            ParsedLine wrapped = new ParsedLine(wrappedText);
            wrapped.scale = original.scale;
            if (original.hasTooltip()) {
                wrapped.tooltip = original.tooltip;
            }
            result.add(wrapped);
        }

        if (result.isEmpty()) {
            result.add(original);
        }

        return result;
    }

    private void updatePageButtons() {
        backDescButton.visible = descPage > 0;
        nextButton.visible = descPage < paginatedLines.size() - 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mobRenderX >= 0 && mobRenderY >= 0) {
            if (mouseX >= mobRenderX && mouseX <= mobRenderX + mobRenderWidth &&
                    mouseY >= mobRenderY && mouseY <= mobRenderY + mobRenderHeight) {

                boolean altPressed = Screen.hasAltDown();

                if (altPressed && button == 0) {
                    Minecraft.getInstance().setScreen(
                            new MobRenderConfigScreen(this, mobId, false)
                    );
                    return true;
                }

                if (button == 0 && !altPressed) {
                    isDraggingMob = true;
                    lastDragX = mouseX;
                    return true;
                }
            }
        }

        backButton.mouseClicked(mouseX, mouseY);
        backDescButton.mouseClicked(mouseX, mouseY);
        nextButton.mouseClicked(mouseX, mouseY);
        if (expandButtonX >= 0 && expandButtonY >= 0) {
            if (mouseX >= expandButtonX && mouseX <= expandButtonX + expandButtonWidth &&
                    mouseY >= expandButtonY && mouseY <= expandButtonY + expandButtonHeight) {
                showAllDrops = !showAllDrops;
                rebuildWithDrops();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingMob = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingMob && button == 0) {
            double dragDelta = mouseX - lastDragX;
            manualRotation += (float) dragDelta * 2.0f;
            lastDragX = mouseX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int pageWidth = 276;
        int pageHeight = 180;
        int totalWidth = pageWidth * 2;
        int baseX = (this.width - totalWidth) / 2;
        int baseY = (this.height - pageHeight) / 2;

        context.blit(RIGHT_PAGE, baseX + 41, baseY, 0, 0, pageWidth, pageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        context.blit(LEFT_PAGE, baseX + pageWidth / 2 + 118, baseY, 0, 0, pageWidth, pageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        int mobSlotX = baseX + 145;
        int mobSlotY = baseY + 5;
        int descSlotX = baseX + pageWidth / 2 + 150;
        int descSlotY = baseY + 20;

        if (mob != null) {
            mobRenderX = mobSlotX;
            mobRenderY = mobSlotY;
            mobRenderWidth = mobSlotW;
            mobRenderHeight = mobSlotH;

            drawMob(context, mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH / 2 + 5, 30, mouseX, mouseY, mob, delta);

            List<List<ParsedLine>> rows = paginatedLines.get(descPage);
            Font renderer = Minecraft.getInstance().font;
            String modName = formatModName(mobId.getNamespace());
            PoseStack matrices = context.pose();

            matrices.pushPose();
            matrices.translate(mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH - 5, 0);
            matrices.scale(0.7f, 0.7f, 1.0f);
            int modWidth = renderer.width(modName);
            context.drawString(renderer, modName, -(modWidth / 2), 0, 0x777777, false);
            matrices.popPose();

            favButton.setX(mobSlotX + mobSlotW / 2 - 9);
            favButton.setY(mobSlotY + mobSlotH - 3);

            Long ticks = JournalClientData.DISCOVERED_TIME.get(mobId);
            if (ticks != null && ticks >= 0) {
                int day = (int) (ticks / 24000L);
                String dayText = "Day " + day;
                matrices.pushPose();
                matrices.translate(mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH + 10, 0);
                matrices.scale(0.7f, 0.7f, 1f);
                int dayWidth = renderer.width(dayText);
                context.drawString(renderer, dayText, -dayWidth / 2, 0, 0x777777, false);
                matrices.popPose();
            }

            int yOffset = 0;

            for (List<ParsedLine> row : rows) {
                int xOffset = 0;
                int lineHeight = 0;

                for (ParsedLine part : row) {
                    float scale = part.scale <= 0 ? 1.0f : part.scale;
                    int drawX = descSlotX + 5 + xOffset;
                    int drawY = descSlotY + yOffset;

                    if (part.isText()) {
                        String raw = part.text.getString();

                        if (raw.equals("{EXPAND_COLLAPSE}")) {
                            String expandSymbol = showAllDrops ? "<< Collapse" : ">> Expand";
                            int symbolWidth = renderer.width(expandSymbol);

                            matrices.pushPose();
                            matrices.translate(drawX, drawY, 0);
                            context.drawString(renderer, expandSymbol, 0, 0, 0x777777, false);
                            matrices.popPose();

                            expandButtonX = drawX;
                            expandButtonY = drawY;
                            expandButtonWidth = symbolWidth;
                            expandButtonHeight = renderer.lineHeight;

                            lineHeight = renderer.lineHeight;
                            continue;
                        }

                        int width = renderer.width(part.text);
                        int height = renderer.lineHeight;

                        matrices.pushPose();
                        matrices.translate(drawX, drawY, 0);
                        context.drawString(renderer, part.text, 0, 0, 0x535c55, false);
                        matrices.popPose();

                        if (part.hasTooltip() &&
                                mouseX >= drawX && mouseX <= drawX + width &&
                                mouseY >= drawY && mouseY <= drawY + height) {
                            List<FormattedCharSequence> tooltip = renderer.split(part.tooltip, 180);
                            context.renderTooltip(renderer, tooltip, mouseX, mouseY);
                        }

                        xOffset += width + 2;
                        lineHeight = Math.max(lineHeight, height);

                    } else if (part.isItem()) {
                        int iconSize = (int) (16 * scale);

                        matrices.pushPose();
                        matrices.translate(drawX, drawY, 0);
                        matrices.scale(scale, scale, 1.0f);
                        context.renderItem(part.item, 0, 0);
                        context.renderItemDecorations(renderer, part.item, 0, 0);
                        matrices.popPose();

                        if (mouseX >= drawX && mouseX <= drawX + iconSize &&
                                mouseY >= drawY && mouseY <= drawY + iconSize) {
                            List<Component> tooltip = getEffectiveTooltip(part, part.item);
                            context.renderComponentTooltip(renderer, tooltip, mouseX, mouseY);
                        }

                        xOffset += iconSize + 2;
                        lineHeight = Math.max(lineHeight, iconSize);

                    } else if (part.isTexture()) {
                        int texSize = (int) (16 * scale);

                        context.blit(part.texture, drawX, drawY, 0, 0, texSize, texSize, 16, 16);

                        if (mouseX >= drawX && mouseX <= drawX + texSize &&
                                mouseY >= drawY && mouseY <= drawY + texSize &&
                                part.hasTooltip()) {
                            List<FormattedCharSequence> tooltip = renderer.split(part.tooltip, 180);
                            context.renderTooltip(renderer, tooltip, mouseX, mouseY);
                        }

                        xOffset += texSize + 2;
                        lineHeight = Math.max(lineHeight, texSize);
                    }
                }
                yOffset += lineHeight + 4;
            }
        }

        backButton.render(context, mouseX, mouseY);
        backDescButton.render(context, mouseX, mouseY);
        nextButton.render(context, mouseX, mouseY);
        context.flush();
    }

    private List<Component> getEffectiveTooltip(ParsedLine part, ItemStack item) {
        if (part.isItem()) {
            if (part.hasExplicitTooltip && part.tooltip != null && !part.tooltip.getString().isBlank()) {
                return List.of(part.tooltip);
            }

            var mc = Minecraft.getInstance();
            return item.getTooltipLines(
                    Item.TooltipContext.of(mc.level),
                    mc.player,
                    mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
            );
        }

        if (part.tooltip != null && !part.tooltip.getString().isBlank()) {
            return List.of(part.tooltip);
        }
        return List.of();
    }

    private void drawMob(GuiGraphics context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity, float delta) {
        JournalConfig.MobRenderConfig config = JournalConfig.getMobRenderConfig(mobId, false);

        int adjustedX = x + (int) config.xOffset;
        int adjustedY = y + (int) config.yOffset;
        int adjustedScale = (int) (scale * config.scale);

        Minecraft client = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PoseStack matrices = context.pose();
        dispatcher.setRenderShadow(false);
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

            float finalYaw;
            if (isDraggingMob) {
                finalYaw = manualRotation;
            } else {
                if (config.animationMode != JournalConfig.AnimationMode.STATIC) {
                    pose.prevYaw = pose.yaw;
                    pose.yaw = (now % 8000L) / 8000.0f * 360F;
                    finalYaw = pose.yaw;
                } else {
                    finalYaw = 0f;
                }
            }

            pose.age = (int) (now / 50L);
            entity.tickCount = pose.age;
            entity.yBodyRotO = finalYaw;
            entity.yBodyRot = finalYaw;
            entity.yRotO = finalYaw;
            entity.setYRot(finalYaw);
            entity.setXRot(0.0f);
            entity.yHeadRotO = finalYaw;
            entity.yHeadRot = finalYaw;

            if (entity instanceof EnderDragon dragon) {
                dragon.oFlapTime = dragon.flapTime;
                dragon.flapTime += 0.05f;
                if (dragon.flapTime > 1.0f) {
                    dragon.flapTime = 0.0f;
                }

                dragon.yRotO = finalYaw;
                dragon.setYRot(finalYaw);
                dragon.yBodyRot = finalYaw;
                dragon.yBodyRotO = finalYaw;
                dragon.yHeadRot = finalYaw;
                dragon.yHeadRotO = finalYaw;
            }

            AnimationOverride.set(entity, limbSwing, prevLimbSwing, limbSwingAmount);

            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, delta, matrices, context.bufferSource(), 0xF000F0);
        } catch (Throwable t) {
            matrices.popPose();
            Font renderer = client.font;
            String errorText = "Can't render mob";
            int textWidth = renderer.width(errorText);
            context.drawString(renderer, Component.literal(errorText), adjustedX - textWidth / 2, adjustedY - 10, 0xFF5555, true);
            return;
        }
        dispatcher.setRenderShadow(true);
        matrices.popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
