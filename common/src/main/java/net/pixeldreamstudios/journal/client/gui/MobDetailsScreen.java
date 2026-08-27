package net.pixeldreamstudios.journal.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
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
import net.pixeldreamstudios.journal.util.TagPreviewCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class MobDetailsScreen extends Screen {
    private static final ResourceLocation LEFT_PAGE =
            new ResourceLocation(Journal.MOD_ID, "textures/book.png");
    private static final ResourceLocation RIGHT_PAGE =
            new ResourceLocation(Journal.MOD_ID, "textures/book_flipped.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int ICON_SIZE = 16;
    private static final int TOOLTIP_WRAP_WIDTH = 180;
    private static final int GRID_COLUMNS = 4;
    private static final int GRID_ROWS = 4;
    private static final int GRID_PAGE_SIZE = GRID_COLUMNS * GRID_ROWS;
    private static final int CYCLE_TICKS = 20;
    private static final int GRID_PAGE_TICKS = 40;
    private static final int PREVIEW_PADDING = 4;
    private static final int PREVIEW_CURSOR_OFFSET = 12;
    private static final int PREVIEW_BACKGROUND = 0xF0100010;
    private static final int PREVIEW_BORDER = 0x505000FF;
    private static final int PREVIEW_LABEL_COLOR = 0xFFAAAAAA;
    private static final int PART_GAP = 2;
    private static final Pattern WORD_CHUNK = Pattern.compile("\\S+\\s*|\\s+");
    private static final float DESC_SCALE = 0.85f;

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

        int maxHeight = (int) (descSlotH / DESC_SCALE) - 10;
        int wrapWidth = descriptionWrapWidth();

        Font renderer = Minecraft.getInstance().font;
        List<List<ParsedLine>> currentPage = new ArrayList<>();
        int currentHeight = 0;
        boolean insideLootSection = false;

        for (int rowIndex = 0; rowIndex < lines.size(); rowIndex++) {
            List<ParsedLine> inputRow = lines.get(rowIndex);

            boolean rowIsLoot = inputRow.stream().anyMatch(part ->
                    part.isItem() && JournalClientData.LAST_DROPS.stream()
                            .anyMatch(stack -> ItemStack.isSameItemSameTags(stack, part.item))
            );

            if (inputRow.stream().anyMatch(p -> p.isText() && p.text.getString().replace("§", "").equalsIgnoreCase("Drops"))) {
                insideLootSection = true;
            }

            List<ParsedLine> currentLine = new ArrayList<>();
            int currentLineWidth = 0;
            int currentLineHeight = 0;

            for (ParsedLine part : splitIntoWords(inputRow)) {
                int width = part.isText() ? renderer.width(part.text) : part.drawnWidth(ICON_SIZE);
                int height = part.isText() ? renderer.lineHeight : part.drawnHeight(ICON_SIZE);

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
                currentLineWidth += width + (part.isText() ? 0 : PART_GAP);
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

    private int descriptionWrapWidth() {
        return (int) (descSlotW / DESC_SCALE) - 10;
    }

    private int alignmentOffset(List<ParsedLine> row, Font renderer) {
        if (row.isEmpty()) {
            return 0;
        }

        MarkdownParser.Alignment alignment = row.get(0).alignment;
        if (alignment == MarkdownParser.Alignment.LEFT) {
            return 0;
        }

        int rowWidth = 0;
        for (ParsedLine part : row) {
            if (part.isText()) {
                rowWidth += renderer.width(part.text);
            } else {
                rowWidth += part.drawnWidth(ICON_SIZE) + PART_GAP;
            }
        }

        int free = descriptionWrapWidth() - rowWidth;
        if (free <= 0) {
            return 0;
        }

        return alignment == MarkdownParser.Alignment.CENTER ? free / 2 : free;
    }

    private List<ParsedLine> splitIntoWords(List<ParsedLine> row) {
        List<ParsedLine> result = new ArrayList<>();
        MarkdownParser.Alignment rowAlignment = row.isEmpty()
                ? MarkdownParser.Alignment.LEFT
                : row.get(0).alignment;

        for (ParsedLine part : row) {
            part.alignment = rowAlignment;
            if (!part.isText() || part.hasHoverPreview()) {
                result.add(part);
                continue;
            }

            String raw = part.text.getString();
            if (raw.isEmpty()) {
                result.add(part);
                continue;
            }

            Matcher chunk = WORD_CHUNK.matcher(raw);
            boolean added = false;
            while (chunk.find()) {
                ParsedLine word = new ParsedLine(
                        Component.literal(chunk.group()).setStyle(part.text.getStyle()));
                word.scale = part.scale;
                part.copyHoverInto(word);
                result.add(word);
                added = true;
            }

            if (!added) {
                result.add(part);
            }
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
                int xOffset = alignmentOffset(row, renderer);
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

                        if (mouseX >= drawX && mouseX <= drawX + width &&
                                mouseY >= drawY && mouseY <= drawY + height) {
                            if (part.hasHoverPreview()) {
                                renderHoverPreview(context, renderer, part, mouseX, mouseY);
                            } else if (part.hasTooltip()) {
                                List<FormattedCharSequence> tooltip = renderer.split(part.tooltip, TOOLTIP_WRAP_WIDTH);
                                context.renderTooltip(renderer, tooltip, mouseX, mouseY);
                            }
                        }

                        xOffset += width;
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

                        xOffset += iconSize + PART_GAP;
                        lineHeight = Math.max(lineHeight, iconSize);

                    } else if (part.isTexture()) {
                        int texWidth = part.drawnWidth(ICON_SIZE);
                        int texHeight = part.drawnHeight(ICON_SIZE);

                        context.blit(part.texture, drawX, drawY, 0, 0, texWidth, texHeight,
                                part.sourceWidth(texWidth), part.sourceHeight(texHeight));

                        if (mouseX >= drawX && mouseX <= drawX + texWidth &&
                                mouseY >= drawY && mouseY <= drawY + texHeight &&
                                part.hasTooltip()) {
                            List<FormattedCharSequence> tooltip = renderer.split(part.tooltip, TOOLTIP_WRAP_WIDTH);
                            context.renderTooltip(renderer, tooltip, mouseX, mouseY);
                        }

                        xOffset += texWidth + PART_GAP;
                        lineHeight = Math.max(lineHeight, texHeight);
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

    private void renderHoverPreview(GuiGraphics context, Font renderer, ParsedLine part, int mouseX, int mouseY) {
        if (part.hoverTexture != null) {
            int drawW = part.drawnWidth(ICON_SIZE);
            int drawH = part.drawnHeight(ICON_SIZE);
            renderPreviewPanel(context, renderer, List.of(), part.hoverTexture,
                    drawW, drawH, part.sourceWidth(drawW), part.sourceHeight(drawH),
                    part.tooltip, mouseX, mouseY);
            return;
        }

        List<ItemStack> items = part.hoverMode == MarkdownParser.HoverMode.SINGLE
                ? part.hoverItems
                : TagPreviewCache.get(part.hoverTag);

        if (items.isEmpty()) {
            if (part.hasTooltip()) {
                context.renderTooltip(renderer, renderer.split(part.tooltip, TOOLTIP_WRAP_WIDTH), mouseX, mouseY);
            }
            return;
        }

        long time = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();

        if (part.hoverMode == MarkdownParser.HoverMode.GRID && items.size() > 1) {
            int pageCount = (items.size() + GRID_PAGE_SIZE - 1) / GRID_PAGE_SIZE;
            int page = (int) ((time / GRID_PAGE_TICKS) % pageCount);
            int from = page * GRID_PAGE_SIZE;
            int to = Math.min(from + GRID_PAGE_SIZE, items.size());

            Component label = pageCount > 1
                    ? Component.literal((from + 1) + "-" + to + "/" + items.size())
                    : part.tooltip;

            renderPreviewPanel(context, renderer, items.subList(from, to), null, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, label, mouseX, mouseY);
            return;
        }

        if (part.hoverMode == MarkdownParser.HoverMode.CYCLE && items.size() > 1) {
            int index = (int) ((time / CYCLE_TICKS) % items.size());
            ItemStack current = items.get(index);

            Component label = current.getHoverName()
                    .copy()
                    .append(Component.literal(" (" + (index + 1) + "/" + items.size() + ")")
                            .withStyle(ChatFormatting.DARK_GRAY));

            renderPreviewPanel(context, renderer, List.of(current), null, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, label, mouseX, mouseY);
            return;
        }

        ItemStack only = items.get(0);
        Component label = part.hasTooltip() ? part.tooltip : only.getHoverName();
        renderPreviewPanel(context, renderer, List.of(only), null, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, label, mouseX, mouseY);
    }

    private void renderPreviewPanel(GuiGraphics context, Font renderer, List<ItemStack> items,
                                    ResourceLocation texture, int textureWidth, int textureHeight,
                                    int sourceWidth, int sourceHeight,
                                    Component label, int mouseX, int mouseY) {
        int columns = Math.min(Math.max(items.size(), 1), GRID_COLUMNS);
        int rows = items.isEmpty() ? 1 : (items.size() + columns - 1) / columns;

        int iconsWidth = texture != null
                ? textureWidth
                : columns * ICON_SIZE + (columns - 1) * PREVIEW_PADDING;
        int iconsHeight = texture != null
                ? textureHeight
                : rows * ICON_SIZE + (rows - 1) * PREVIEW_PADDING;

        boolean hasLabel = label != null && !label.getString().isBlank();
        int labelWidth = hasLabel ? renderer.width(label) : 0;
        boolean inlineLabel = hasLabel && (texture != null || items.size() <= 1);

        int contentWidth;
        int contentHeight;
        if (inlineLabel) {
            contentWidth = iconsWidth + PREVIEW_PADDING + labelWidth;
            contentHeight = Math.max(iconsHeight, renderer.lineHeight);
        } else {
            contentWidth = Math.max(iconsWidth, labelWidth);
            contentHeight = iconsHeight + (hasLabel ? renderer.lineHeight + PREVIEW_PADDING : 0);
        }

        int panelWidth = contentWidth + PREVIEW_PADDING * 2;
        int panelHeight = contentHeight + PREVIEW_PADDING * 2;

        int panelX = mouseX + PREVIEW_CURSOR_OFFSET;
        int panelY = mouseY - PREVIEW_CURSOR_OFFSET;

        if (panelX + panelWidth > this.width) {
            panelX = this.width - panelWidth - PREVIEW_PADDING;
        }
        if (panelY + panelHeight > this.height) {
            panelY = this.height - panelHeight - PREVIEW_PADDING;
        }
        if (panelY < 0) {
            panelY = 0;
        }

        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate(0, 0, 400);

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PREVIEW_BACKGROUND);
        context.renderOutline(panelX, panelY, panelWidth, panelHeight, PREVIEW_BORDER);

        int originX = panelX + PREVIEW_PADDING;
        int originY = panelY + PREVIEW_PADDING;

        if (texture != null) {
            context.blit(texture, originX, originY, 0, 0, textureWidth, textureHeight, sourceWidth, sourceHeight);
        }

        for (int i = 0; i < items.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int iconX = originX + col * (ICON_SIZE + PREVIEW_PADDING);
            int iconY = originY + row * (ICON_SIZE + PREVIEW_PADDING);

            context.renderItem(items.get(i), iconX, iconY);
            context.renderItemDecorations(renderer, items.get(i), iconX, iconY);
        }

        if (hasLabel) {
            int labelX = inlineLabel ? originX + iconsWidth + PREVIEW_PADDING : originX;
            int labelY = inlineLabel
                    ? originY + Math.max(0, (iconsHeight - renderer.lineHeight) / 2)
                    : originY + iconsHeight + PREVIEW_PADDING;
            context.drawString(renderer, label, labelX, labelY, PREVIEW_LABEL_COLOR, false);
        }

        matrices.popPose();
    }

    private List<Component> getEffectiveTooltip(ParsedLine part, ItemStack item) {
        if (part.isItem()) {
            if (part.hasExplicitTooltip && part.tooltip != null && !part.tooltip.getString().isBlank()) {
                return List.of(part.tooltip);
            }

            var mc = Minecraft.getInstance();
            return item.getTooltipLines(
                    mc.player,
                    mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
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
