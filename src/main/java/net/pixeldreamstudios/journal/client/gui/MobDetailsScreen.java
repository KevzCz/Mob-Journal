package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.network.RequestMobDropsPayload;
import net.pixeldreamstudios.journal.util.MarkdownParser;
import net.pixeldreamstudios.journal.util.MarkdownParser.ParsedLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class MobDetailsScreen extends Screen {
    private static final Identifier LEFT_PAGE = Identifier.of("journal", "textures/book.png");
    private static final Identifier RIGHT_PAGE = Identifier.of("journal", "textures/book_flipped.png");
    private final int returnPage;
    private final Identifier mobId;
    private LivingEntity mob;

    private PageTurnButton backButton;
    private DetailPageTurnButton nextButton;
    private DetailPageTurnButton backDescButton;

    private int descPage = 0;
    private List<List<List<ParsedLine>>> paginatedLines = new ArrayList<>();
    private final String returnQuery;
    private final int mobSlotW = 120, mobSlotH = 140;
    private final int descSlotW = 110, descSlotH = 120;
    private final Map<Identifier, CachedPose> poseCache = new java.util.HashMap<>();
    private int renderFrameCounter = 0;

    private static class CachedPose {
        float limbPos, yaw;
        int age;
        long lastUpdated;
    }
    public MobDetailsScreen(Identifier mobId, int returnPage, String returnQuery) {
        super(Text.literal("Mob Info"));
        this.mobId = mobId;
        this.returnPage = returnPage;
        this.returnQuery = returnQuery;
    }


    private List<Text> splitText(Text input, int maxWidth, TextRenderer renderer) {
        List<Text> result = new ArrayList<>();
        String[] words = input.getString().split(" ");
        StringBuilder line = new StringBuilder();
        Style currentStyle = input.getStyle();

        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            int width = renderer.getWidth(test);
            if (width > maxWidth && line.length() > 0) {
                result.add(Text.literal(line.toString()).setStyle(currentStyle));
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }

        if (line.length() > 0) {
            result.add(Text.literal(line.toString()).setStyle(currentStyle));
        }

        return result;
    }
    public void rebuildWithDrops() {
        List<List<ParsedLine>> allLines = mob != null
                ? net.pixeldreamstudios.journal.data.MobDescriptionLoader.getDescription(mobId, mob)
                : List.of(List.of(new ParsedLine(Text.literal("§cUnknown mob"))));

        if (MarkdownParser.containsPlaceholder(allLines, "{getLootDrops}")) {
            List<ParsedLine> dropIcons = new ArrayList<>();
            for (ItemStack stack : JournalClientData.LAST_DROPS) {
                ParsedLine icon = new ParsedLine(stack);
                icon.scale = 1.0f;
                dropIcons.add(icon);
            }

            if (dropIcons.isEmpty()) {
                dropIcons.add(new ParsedLine(Text.literal("§7(No known drops)")));
            }

            MarkdownParser.replacePlaceholder(allLines, "{getLootDrops}", dropIcons);
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
        World world = MinecraftClient.getInstance().world;

        if (world != null) {
            var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(mobId);
            if (type != null && type.isSummonable()) {
                var entity = type.create(world);
                if (entity instanceof LivingEntity living) {
                    this.mob = living;
                }
            }
        }

        JournalClientData.LAST_DROPS.clear();
        ClientPlayNetworking.send(new RequestMobDropsPayload(mobId));

        List<List<ParsedLine>> allLines = mob != null
                ? net.pixeldreamstudios.journal.data.MobDescriptionLoader.getDescription(mobId, mob)
                : List.of(List.of(new ParsedLine(Text.literal("§cUnknown mob"))));

        // Inject dynamic loot only if placeholder is present
        if (MarkdownParser.containsPlaceholder(allLines, "{getLootDrops}")) {
            List<ParsedLine> dropIcons = new ArrayList<>();
            for (ItemStack stack : JournalClientData.LAST_DROPS) {
                ParsedLine icon = new ParsedLine(stack);
                icon.scale = 1.0f;
                dropIcons.add(icon);
            }

            if (dropIcons.isEmpty()) {
                dropIcons.add(new ParsedLine(Text.literal("§7(No known drops)")));
            }

            MarkdownParser.replacePlaceholder(allLines, "{getLootDrops}", dropIcons);
        }

        paginateDescription(allLines);

        int pageWidth = 276;
        int pageHeight = 180;
        int totalWidth = pageWidth * 2;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - pageHeight) / 2;
        int buttonY = y + pageHeight - 20;

        backButton = new PageTurnButton(x + 101, buttonY, false, () -> {
            MinecraftClient.getInstance().setScreen(new JournalScreen(returnPage, returnQuery));
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

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

        List<List<ParsedLine>> currentPage = new ArrayList<>();
        int currentHeight = 0;

        for (List<ParsedLine> inputRow : lines) {
            List<ParsedLine> currentLine = new ArrayList<>();
            int currentLineWidth = 0;
            int currentLineHeight = 0;

            for (ParsedLine part : inputRow) {
                float scale = part.scale <= 0 ? 1.0f : part.scale;

                if (part.isText()) {
                    List<Text> wrappedLines = splitText(part.text, wrapWidth, renderer);

                    for (Text wrappedText : wrappedLines) {
                        int width = renderer.getWidth(wrappedText);
                        int height = renderer.fontHeight;

                        if (currentLineWidth + width > wrapWidth && !currentLine.isEmpty()) {
                            currentPage.add(currentLine);
                            currentHeight += currentLineHeight + 4;
                            currentLine = new ArrayList<>();
                            currentLineWidth = 0;
                            currentLineHeight = 0;
                        }

                        ParsedLine wrappedPart = new ParsedLine(wrappedText);
                        wrappedPart.tooltip = part.tooltip;
                        wrappedPart.scale = part.scale;

                        currentLine.add(wrappedPart);
                        currentLineWidth += width + 2;
                        currentLineHeight = Math.max(currentLineHeight, height);

                        if (currentHeight + currentLineHeight >= maxHeight) {
                            currentPage.add(currentLine);
                            paginatedLines.add(currentPage);
                            currentPage = new ArrayList<>();
                            currentLine = new ArrayList<>();
                            currentHeight = 0;
                            currentLineWidth = 0;
                            currentLineHeight = 0;
                        }
                    }

                    continue;
                }

                int width = (int) (16 * scale);
                int height = (int) (16 * scale);

                if (currentLineWidth + width > wrapWidth && !currentLine.isEmpty()) {
                    currentPage.add(currentLine);
                    currentHeight += currentLineHeight + 4;
                    currentLine = new ArrayList<>();
                    currentLineWidth = 0;
                    currentLineHeight = 0;
                }

                currentLine.add(part);
                currentLineWidth += width + 2;
                currentLineHeight = Math.max(currentLineHeight, height);

                if (currentHeight + currentLineHeight >= maxHeight) {
                    currentPage.add(currentLine);
                    paginatedLines.add(currentPage);
                    currentPage = new ArrayList<>();
                    currentLine = new ArrayList<>();
                    currentHeight = 0;
                    currentLineWidth = 0;
                    currentLineHeight = 0;
                }
            }

            if (!currentLine.isEmpty()) {
                currentPage.add(currentLine);
                currentHeight += currentLineHeight + 4;
            }

            if (currentHeight >= maxHeight) {
                paginatedLines.add(currentPage);
                currentPage = new ArrayList<>();
                currentHeight = 0;
            }
        }

        if (!currentPage.isEmpty()) {
            paginatedLines.add(currentPage);
        }

        if (paginatedLines.isEmpty()) {
            paginatedLines.add(List.of(List.of(new ParsedLine(Text.literal("§cNo content")))));
        }
    }

    private void updatePageButtons() {
        backDescButton.visible = descPage > 0;
        nextButton.visible = descPage < paginatedLines.size() - 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        backButton.mouseClicked(mouseX, mouseY);
        backDescButton.mouseClicked(mouseX, mouseY);
        nextButton.mouseClicked(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int pageWidth = 276;
        int pageHeight = 180;
        int totalWidth = pageWidth * 2;
        int baseX = (this.width - totalWidth) / 2;
        int baseY = (this.height - pageHeight) / 2;

        context.drawTexture(RIGHT_PAGE, baseX + 41, baseY, 0, 0, pageWidth, pageHeight);
        context.drawTexture(LEFT_PAGE, baseX + pageWidth / 2 + 118, baseY, 0, 0, pageWidth, pageHeight);

        int mobSlotX = baseX + 145;
        int mobSlotY = baseY + 5;
        int descSlotX = baseX + pageWidth / 2 + 150;
        int descSlotY = baseY + 20;

        if (mob != null) {
            drawMob(context, mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH / 2 + 5, 30, mouseX, mouseY, mob);

            List<List<ParsedLine>> rows = paginatedLines.get(descPage);
            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            String modName = formatModName(mobId.getNamespace());
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH - 5, 0);
            matrices.scale(0.7f, 0.7f, 1.0f);
            int modWidth = renderer.getWidth(modName);
            context.drawText(renderer, modName, -(modWidth / 2), 0, 0x777777, false);
            matrices.pop();
            int yOffset = 0;

            for (List<ParsedLine> row : rows) {
                int xOffset = 0;
                int lineHeight = 0;

                for (ParsedLine part : row) {
                    float scale = part.scale <= 0 ? 1.0f : part.scale;
                    int drawX = descSlotX + 5 + xOffset;
                    int drawY = descSlotY + yOffset;

                    if (part.isText()) {
                        int width = renderer.getWidth(part.text);
                        int height = renderer.fontHeight;

                        context.getMatrices().push();
                        context.getMatrices().translate(drawX, drawY, 0);
                        context.drawText(renderer, part.text, 0, 0, 0x535c55, false);
                        context.getMatrices().pop();

                        if (part.hasTooltip() &&
                                mouseX >= drawX && mouseX <= drawX + width &&
                                mouseY >= drawY && mouseY <= drawY + height) {
                            List<OrderedText> tooltip = renderer.wrapLines(part.tooltip, 180);
                            context.drawOrderedTooltip(renderer, tooltip, mouseX, mouseY);
                        }

                        xOffset += width + 2;
                        lineHeight = Math.max(lineHeight, height);

                    } else if (part.isItem()) {
                        int iconSize = (int) (16 * scale);

                        context.getMatrices().push();
                        context.getMatrices().translate(drawX, drawY, 0);
                        context.getMatrices().scale(scale, scale, 1.0f);
                        context.drawItem(part.item, 0, 0);
                        context.drawItemInSlot(renderer, part.item, 0, 0);
                        context.getMatrices().pop();

                        if (mouseX >= drawX && mouseX <= drawX + iconSize &&
                                mouseY >= drawY && mouseY <= drawY + iconSize) {
                            List<Text> tooltip = getEffectiveTooltip(part, part.item, mouseX, mouseY);
                            context.drawTooltip(renderer, tooltip, mouseX, mouseY);

                        }

                        xOffset += iconSize + 2;
                        lineHeight = Math.max(lineHeight, iconSize);

                    } else if (part.isTexture()) {
                        int texSize = (int) (16 * scale);
                        context.drawTexture(part.texture, drawX, drawY, 0, 0, texSize, texSize, 16, 16);

                        if (mouseX >= drawX && mouseX <= drawX + texSize &&
                                mouseY >= drawY && mouseY <= drawY + texSize &&
                                part.hasTooltip()) {
                            List<OrderedText> tooltip = renderer.wrapLines(part.tooltip, 180);
                            context.drawOrderedTooltip(renderer, tooltip, mouseX, mouseY);
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
        context.draw();
    }

    // At the top of the class
    private final Map<LivingEntity, Boolean> animatedEntities = new WeakHashMap<>();
    private List<Text> getEffectiveTooltip(ParsedLine part, ItemStack item, int mouseX, int mouseY) {
        if (part.isItem()) {
            // Always use vanilla tooltip for items unless explicitly overridden
            if (part.hasExplicitTooltip && part.tooltip != null && !part.tooltip.getString().isBlank()) {
                return List.of(part.tooltip);
            }

            return item.getTooltip(
                    new Item.TooltipContext() {
                        @Override public RegistryWrapper.WrapperLookup getRegistryLookup() { return null; }
                        @Override public float getUpdateTickRate() { return 0; }
                        @Override public MapState getMapState(MapIdComponent component) { return null; }

                        public boolean isAdvanced() {
                            return MinecraftClient.getInstance().options.advancedItemTooltips;
                        }

                        public boolean isCreative() {
                            return MinecraftClient.getInstance().player != null &&
                                    MinecraftClient.getInstance().player.isCreative();
                        }
                    },
                    MinecraftClient.getInstance().player,
                    MinecraftClient.getInstance().options.advancedItemTooltips
                            ? TooltipType.ADVANCED
                            : TooltipType.BASIC
            );
        }


        if (part.tooltip != null && !part.tooltip.getString().isBlank()) {
            return List.of(part.tooltip);
        }

        return List.of();

    }

    private void drawMob(DrawContext context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        MatrixStack matrices = context.getMatrices();
        dispatcher.setRenderShadows(false);
        matrices.push();
        matrices.translate(x, y, 100.0);
        matrices.scale(scale, -scale, scale);
        matrices.translate(0.0, -1.5, 0.0);

        try {
            renderFrameCounter = (renderFrameCounter + 1) % 3;
            boolean updatePose = renderFrameCounter == 0;

            CachedPose pose = poseCache.computeIfAbsent(mobId, k -> new CachedPose());
            long now = System.currentTimeMillis();

            if (updatePose || now - pose.lastUpdated > 250) {
                pose.limbPos = (now % 10000L) / 1000.0f * 3f;
                pose.yaw = (now % 8000L) / 8000.0f * 360F;
                pose.age = (int)(now / 50L);
                pose.lastUpdated = now;
            }

            ((net.pixeldreamstudios.journal.mixin.LimbAnimatorAccessor) entity.limbAnimator).setPos(pose.limbPos);
            entity.limbAnimator.updateLimbs(0.35f, 1f);
            entity.bodyYaw = pose.yaw;
            entity.setYaw(pose.yaw);
            entity.setPitch(0.0f);
            entity.headYaw = pose.yaw;
            entity.age = pose.age;


            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.getVertexConsumers(), 0xF000F0);

        } catch (Throwable t) {

            matrices.pop();

            TextRenderer renderer = client.textRenderer;
            String errorText = "Can't render mob";
            int textWidth = renderer.getWidth(errorText);

            context.drawTextWithShadow(renderer, Text.literal(errorText), x - textWidth / 2, y - 10, 0xFF5555);
            return;
        }
        dispatcher.setRenderShadows(true);
        matrices.pop();
    }




    @Override
    public boolean shouldPause() {
        return false;
    }
}
