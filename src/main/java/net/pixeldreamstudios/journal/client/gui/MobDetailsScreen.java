package net.pixeldreamstudios.journal.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
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
import net.pixeldreamstudios.journal.network.ToggleFavoritePayload;
import net.pixeldreamstudios.journal.util.MarkdownParser;
import net.pixeldreamstudios.journal.util.MarkdownParser.ParsedLine;
import net.pixeldreamstudios.journal.util.MobEntityCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class MobDetailsScreen extends Screen {
    private static final Identifier LEFT_PAGE = Identifier.of("journal", "textures/book.png");
    private static final Identifier RIGHT_PAGE = Identifier.of("journal", "textures/book_flipped.png");
    private final int returnPage;
    private boolean isFavorite;
    private ButtonWidget favButton;
    private final Identifier mobId;
    private LivingEntity mob;
    private boolean expandSymbolDrawn = false;
    private PageTurnButton backButton;
    private DetailPageTurnButton nextButton;
    private DetailPageTurnButton backDescButton;

    private int descPage = 0;
    private List<List<List<ParsedLine>>> paginatedLines = new ArrayList<>();
    private final String returnQuery;
    private final int mobSlotW = 120, mobSlotH = 140;
    private final int descSlotW = 110, descSlotH = 130;
    private final Map<Identifier, CachedPose> poseCache = new java.util.HashMap<>();
    private boolean showAllDrops = false;
    private int expandButtonX = -1;
    private int expandButtonY = -1;
    private int expandButtonWidth = -1;
    private int expandButtonHeight = -1;
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
    public MobDetailsScreen(Identifier mobId, int returnPage, String returnQuery) {
        super(Text.literal("Mob Info"));
        this.mobId = mobId;
        this.returnPage = returnPage;
        this.returnQuery = returnQuery;
    }


    public void rebuildWithDrops() {
        List<List<ParsedLine>> allLines = mob != null
                ? net.pixeldreamstudios.journal.data.MobDescriptionLoader.getDescription(mobId, mob)
                : List.of(List.of(new ParsedLine(Text.literal("§cUnknown mob"))));

        List<ParsedLine> dropIcons = new ArrayList<>();

        int maxDrops = showAllDrops ? JournalClientData.LAST_DROPS.size() : Math.min(6, JournalClientData.LAST_DROPS.size());
        for (int i = 0; i < maxDrops; i++) {
            ItemStack stack = JournalClientData.LAST_DROPS.get(i);
            ParsedLine icon = new ParsedLine(stack);
            icon.scale = 1.0f;
            dropIcons.add(icon);
        }

        if (dropIcons.isEmpty()) {
            dropIcons.add(new ParsedLine(Text.literal("§7(No known drops)")));
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
            if (! part.isEmpty()) {
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
        this.isFavorite = JournalClientData.FAVORITE_MOBS.contains(mobId);

        if (world != null) {
            LivingEntity cached = MobEntityCache.get(mobId, world);
            if (cached != null) {
                this.mob = cached;
            }
        }



        JournalClientData.LAST_DROPS.clear();
        ClientPlayNetworking.send(new RequestMobDropsPayload(mobId));

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

        paginateDescription(allLines);

        int pageWidth = 276;
        int pageHeight = 180;
        int totalWidth = pageWidth * 2;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - pageHeight) / 2;
        int mobSlotX = x + 145;
        int mobSlotY = y + 5;

        this.favButton = ButtonWidget.builder(
                Text.literal(isFavorite ? "★" : "☆").styled(style -> style.withColor(0xFFFF55)),
                btn -> {
                    isFavorite = !isFavorite;
                    ClientPlayNetworking.send(new ToggleFavoritePayload(mobId, isFavorite));
                    btn.setMessage(Text.literal(isFavorite ?  "★" : "☆").styled(style -> style.withColor(0xFFFF55)));
                }
        ).dimensions(0, 0, 18, 18).tooltip(
                Tooltip.of(Text.literal("Toggle Favorite"))
        ).build();
        this.addDrawableChild(favButton);
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
        boolean insideLootSection = false;

        for (int rowIndex = 0; rowIndex < lines.size(); rowIndex++) {
            List<ParsedLine> inputRow = lines.get(rowIndex);

            boolean rowIsLoot = inputRow.stream().anyMatch(part ->
                    part.isItem() && JournalClientData.LAST_DROPS.stream()
                            .anyMatch(stack -> ItemStack.areItemsAndComponentsEqual(stack, part.item))
            );
            if (inputRow.stream().anyMatch(p -> p.isText() && p.text.getString().replace("§", "").equalsIgnoreCase("Drops"))) {
                insideLootSection = true;
            }

            List<ParsedLine> currentLine = new ArrayList<>();
            int currentLineWidth = 0;
            int currentLineHeight = 0;

            for (ParsedLine part : inputRow) {
                float scale = part.scale <= 0 ? 1.0f : part.scale;
                int width = part.isText()
                        ? renderer.getWidth(part.text)
                        : (int) (16 * scale);
                int height = part.isText()
                        ? renderer.fontHeight
                        : (int) (16 * scale);

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

            if (! currentLine.isEmpty()) {
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
                    ParsedLine expandButton = new ParsedLine(Text.literal("{EXPAND_COLLAPSE}"));
                    List<ParsedLine> expandRow = List.of(expandButton);

                    if (currentHeight + renderer.fontHeight > maxHeight && !currentPage.isEmpty()) {
                        paginatedLines.add(currentPage);
                        currentPage = new ArrayList<>();
                        currentHeight = 0;
                    }
                    currentPage.add(expandRow);
                    currentHeight += renderer.fontHeight + 4;
                }
                insideLootSection = false;
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

        expandSymbolDrawn = false;

        if (mob != null) {
            drawMob(context, mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH / 2 + 5, 30, mouseX, mouseY, mob, delta);

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

            favButton.setX(mobSlotX + mobSlotW / 2 - 9);
            favButton.setY(mobSlotY + mobSlotH - 3);

            Long ticks = JournalClientData.DISCOVERED_TIME.get(mobId);
            if (ticks != null && ticks >= 0) {
                int day = (int) (ticks / 24000L);
                String dayText = "Day " + day;
                matrices.push();
                matrices.translate(mobSlotX + mobSlotW / 2, mobSlotY + mobSlotH + 10, 0);
                matrices.scale(0.7f, 0.7f, 1f);
                int dayWidth = renderer.getWidth(dayText);
                context.drawText(renderer, dayText, -dayWidth / 2, 0, 0x777777, false);
                matrices.pop();
            }

            int yOffset = 0;
            boolean insideLootSection = false;

            for (List<ParsedLine> row : rows) {
                int xOffset = 0;
                int lineHeight = 0;

                for (ParsedLine part : row) {
                    float scale = part.scale <= 0 ? 1.0f : part.scale;
                    int drawX = descSlotX + 5 + xOffset;
                    int drawY = descSlotY + yOffset;

                    if (part.isText()) {
                        String raw = part.text.getString();

                        if (raw.equalsIgnoreCase("Drops")) {
                            insideLootSection = true;
                        }

                        if (raw.equals("{EXPAND_COLLAPSE}")) {
                            String expandSymbol = showAllDrops ? "<< Collapse" : ">> Expand";
                            int symbolWidth = renderer.getWidth(expandSymbol);

                            matrices.push();
                            matrices.translate(drawX, drawY, 0);
                            context.drawText(renderer, expandSymbol, 0, 0, 0x777777, false);
                            matrices.pop();

                            expandButtonX = drawX;
                            expandButtonY = drawY;
                            expandButtonWidth = symbolWidth;
                            expandButtonHeight = renderer.fontHeight;

                            lineHeight = renderer.fontHeight;
                            continue;
                        }

                        int width = renderer.getWidth(part.text);
                        int height = renderer.fontHeight;

                        matrices.push();
                        matrices.translate(drawX, drawY, 0);
                        context.drawText(renderer, part.text, 0, 0, 0x535c55, false);
                        matrices.pop();

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

                        matrices.push();
                        matrices.translate(drawX, drawY, 0);
                        matrices.scale(scale, scale, 1.0f);
                        context.drawItem(part.item, 0, 0);
                        context.drawItemInSlot(renderer, part.item, 0, 0);
                        matrices.pop();

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








    private final Map<LivingEntity, Boolean> animatedEntities = new WeakHashMap<>();
    private List<Text> getEffectiveTooltip(ParsedLine part, ItemStack item, int mouseX, int mouseY) {
        if (part.isItem()) {
            if (part.hasExplicitTooltip && part.tooltip != null && ! part.tooltip.getString().isBlank()) {
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

    private void drawMob(DrawContext context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity, float delta) {
		MinecraftClient client = MinecraftClient.getInstance();
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		MatrixStack matrices = context.getMatrices();
		dispatcher.setRenderShadows(false);
		matrices.push();
		matrices.translate(x, y, 100.0);
		matrices.scale(scale, -scale, scale);
		matrices.translate(0.0, -1.5, 0.0);

		try {
			CachedPose pose = poseCache.computeIfAbsent(mobId, k -> new CachedPose());
			long now = System.currentTimeMillis();

			if (!  pose.initialized) {
				pose.limbSwingAmount = 0.6f;
				pose.initialized = true;
			}

			float prevLimbSwing = pose.limbSwing;

			long elapsed = now - pose.lastUpdated;
			if (elapsed > 0) {
				pose.limbSwing += (elapsed / 1000.0f) * 12.0f;
				pose.lastUpdated = now;
			}

			pose.prevYaw = pose.yaw;
			pose.yaw = (now % 8000L) / 8000.0f * 360F;
			pose.age = (int)(now / 50L);

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
				dragon.wingPosition += 0.05f;
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

			net.pixeldreamstudios.journal.client.gui.AnimationOverride.set(entity, pose.limbSwing, prevLimbSwing);

			dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, delta, matrices, context.getVertexConsumers(), 0xF000F0);

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