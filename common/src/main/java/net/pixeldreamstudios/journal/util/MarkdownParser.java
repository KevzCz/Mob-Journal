package net.pixeldreamstudios.journal.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {

    public static final String HOVER_PREFIX = "hover:";
    public static final String TARGET_ITEM = "item:";
    public static final String TARGET_TAG = "tag:";
    public static final String TARGET_TAG_GRID = "tag_grid:";
    public static final String TARGET_TEXTURE = "texture:";

    private static final String UNKNOWN_MARKER = "❓";
    private static final float DEFAULT_SCALE = 1.0f;
    public static final int SIZE_UNSET = -1;

    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern TITLE = Pattern.compile("## (.+)");

    private static final Pattern INLINE_TOKEN = Pattern.compile(
            "(!)?\\[(.*?)\\]\\((hover:.*?|(?:item|texture):[^\\s\")]+)"
                    + "(?:\\s+scale=([0-9.]+))?"
                    + "(?:\\s+width=([0-9]+))?"
                    + "(?:\\s+height=([0-9]+))?"
                    + "(?:\\s+srcwidth=([0-9]+))?"
                    + "(?:\\s+srcheight=([0-9]+))?"
                    + "(?:\\s+\"(.*?)\")?\\)"
    );

    private static final int GROUP_LABEL = 2;
    private static final int GROUP_TARGET = 3;
    private static final int GROUP_SCALE = 4;
    private static final int GROUP_WIDTH = 5;
    private static final int GROUP_HEIGHT = 6;
    private static final int GROUP_SRC_WIDTH = 7;
    private static final int GROUP_SRC_HEIGHT = 8;
    private static final int GROUP_TOOLTIP = 9;

    public static boolean containsPlaceholder(List<List<ParsedLine>> lines, String placeholder) {
        for (List<ParsedLine> line : lines) {
            for (ParsedLine part : line) {
                if (part.isText() && part.text.getString().contains(placeholder)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void replacePlaceholder(List<List<ParsedLine>> lines, String placeholder, List<ParsedLine> replacement) {
        for (int i = 0; i < lines.size(); i++) {
            List<ParsedLine> line = lines.get(i);
            for (int j = 0; j < line.size(); j++) {
                ParsedLine part = line.get(j);
                if (part.isText() && part.text.getString().contains(placeholder)) {
                    line.remove(j);
                    line.addAll(j, replacement);
                    return;
                }
            }
        }
    }

    public static List<ParsedLine> parse(String rawLine) {
        List<ParsedLine> parts = new ArrayList<>();

        Alignment alignment = null;
        String line = rawLine;
        for (Alignment candidate : Alignment.values()) {
            if (line.startsWith(candidate.directive)) {
                alignment = candidate;
                line = line.substring(candidate.directive.length());
                break;
            }
        }

        Matcher title = TITLE.matcher(line);
        if (title.matches()) {
            ParsedLine heading = new ParsedLine(
                    Component.literal(title.group(1)).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD));
            heading.alignment = alignment == null ? Alignment.CENTER : alignment;
            parts.add(heading);
            return parts;
        }

        String processed = replaceMarkdown(line);

        Matcher matcher = INLINE_TOKEN.matcher(processed);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                appendText(parts, processed.substring(lastEnd, matcher.start()));
            }

            String label = matcher.group(GROUP_LABEL);
            String target = matcher.group(GROUP_TARGET);
            String scaleStr = matcher.group(GROUP_SCALE);
            String tooltipStr = matcher.group(GROUP_TOOLTIP);

            float scale = parseScale(scaleStr);
            int width = parseSize(matcher.group(GROUP_WIDTH));
            int height = parseSize(matcher.group(GROUP_HEIGHT));

            ParsedLine built;
            if (target.startsWith(HOVER_PREFIX)) {
                built = buildHoverPart(label, target.substring(HOVER_PREFIX.length()), scale, tooltipStr);
            } else {
                built = buildInlinePart(target, scale, tooltipStr);
            }

            if (built != null) {
                built.textureWidth = width;
                built.textureHeight = height;
                built.sourceWidth = parseSize(matcher.group(GROUP_SRC_WIDTH));
                built.sourceHeight = parseSize(matcher.group(GROUP_SRC_HEIGHT));
                parts.add(built);
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < processed.length()) {
            appendText(parts, processed.substring(lastEnd));
        }

        if (!parts.isEmpty()) {
            parts.get(0).alignment = alignment == null ? Alignment.LEFT : alignment;
        }

        return parts;
    }

    private static void appendText(List<ParsedLine> parts, String raw) {
        if (!raw.isBlank()) {
            parts.add(new ParsedLine(Component.literal(raw)));
        }
    }

    private static float parseScale(String scaleStr) {
        if (scaleStr == null) {
            return DEFAULT_SCALE;
        }
        try {
            return Float.parseFloat(scaleStr);
        } catch (NumberFormatException ignored) {
            return DEFAULT_SCALE;
        }
    }

    private static int parseSize(String value) {
        if (value == null) {
            return SIZE_UNSET;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : SIZE_UNSET;
        } catch (NumberFormatException ignored) {
            return SIZE_UNSET;
        }
    }

    private static ParsedLine buildHoverPart(String label, String payload, float scale, String tooltipStr) {
        ParsedLine part = new ParsedLine(Component.literal(label).withStyle(ChatFormatting.UNDERLINE));
        part.scale = scale;

        if (payload.startsWith(TARGET_ITEM)) {
            ItemStack stack = resolveItem(payload.substring(TARGET_ITEM.length()));
            if (!stack.isEmpty()) {
                part.hoverItems = List.of(stack);
                part.hoverMode = HoverMode.SINGLE;
            }
        } else if (payload.startsWith(TARGET_TAG_GRID)) {
            part.hoverTag = ResourceLocation.tryParse(payload.substring(TARGET_TAG_GRID.length()));
            part.hoverMode = HoverMode.GRID;
        } else if (payload.startsWith(TARGET_TAG)) {
            part.hoverTag = ResourceLocation.tryParse(payload.substring(TARGET_TAG.length()));
            part.hoverMode = HoverMode.CYCLE;
        } else if (payload.startsWith(TARGET_TEXTURE)) {
            part.hoverTexture = ResourceLocation.tryParse(payload.substring(TARGET_TEXTURE.length()));
        } else if (!payload.isBlank()) {
            part.tooltip = Component.literal(payload);
            part.hasExplicitTooltip = true;
            return part;
        }

        if (tooltipStr != null && !tooltipStr.strip().isEmpty()) {
            part.tooltip = Component.literal(replaceMarkdown(tooltipStr));
            part.hasExplicitTooltip = true;
        }

        return part;
    }

    private static ParsedLine buildInlinePart(String target, float scale, String tooltipStr) {
        if (target.startsWith(TARGET_ITEM)) {
            String value = target.substring(TARGET_ITEM.length());
            ItemStack stack = resolveItem(value);
            if (stack.isEmpty()) {
                return new ParsedLine(Component.literal(UNKNOWN_MARKER + value).withStyle(ChatFormatting.GRAY));
            }

            ParsedLine part = new ParsedLine(stack);
            part.scale = scale;
            if (tooltipStr != null && !tooltipStr.strip().isEmpty()) {
                part.tooltip = Component.literal(replaceMarkdown(tooltipStr));
                part.hasExplicitTooltip = true;
            }
            return part;
        }

        if (target.startsWith(TARGET_TEXTURE)) {
            ResourceLocation id = ResourceLocation.tryParse(target.substring(TARGET_TEXTURE.length()));
            if (id == null) {
                return null;
            }

            ParsedLine part = new ParsedLine(id);
            part.scale = scale;
            if (tooltipStr != null && !tooltipStr.strip().isEmpty()) {
                part.tooltip = Component.literal(replaceMarkdown(tooltipStr));
            }
            return part;
        }

        return null;
    }

    private static ItemStack resolveItem(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    private static final Map<String, String> COLOR_CODES = Map.ofEntries(
            Map.entry("black", "§0"),
            Map.entry("dark_blue", "§1"),
            Map.entry("dark_green", "§2"),
            Map.entry("dark_aqua", "§3"),
            Map.entry("dark_red", "§4"),
            Map.entry("dark_purple", "§5"),
            Map.entry("gold", "§6"),
            Map.entry("gray", "§7"),
            Map.entry("dark_gray", "§8"),
            Map.entry("blue", "§9"),
            Map.entry("green", "§a"),
            Map.entry("aqua", "§b"),
            Map.entry("red", "§c"),
            Map.entry("light_purple", "§d"),
            Map.entry("yellow", "§e"),
            Map.entry("white", "§f"),
            Map.entry("obfuscated", "§k"),
            Map.entry("bold", "§l"),
            Map.entry("strikethrough", "§m"),
            Map.entry("underline", "§n"),
            Map.entry("italic", "§o"),
            Map.entry("reset", "§r"),
            Map.entry("ofus", "§k")
    );

    private static String replaceMarkdown(String input) {
        String result = BOLD.matcher(input).replaceAll("§l$1§r");
        result = ITALIC.matcher(result).replaceAll("§o$1§r");

        for (Map.Entry<String, String> entry : COLOR_CODES.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }

    public enum Alignment {
        CENTER("{center}"),
        RIGHT("{right}"),
        LEFT("{left}");

        public final String directive;

        Alignment(String directive) {
            this.directive = directive;
        }
    }

    public enum HoverMode {
        NONE,
        SINGLE,
        CYCLE,
        GRID
    }

    public static class ParsedLine {
        public final Component text;
        public final ItemStack item;
        public final ResourceLocation texture;
        public Component tooltip;
        public ResourceLocation hoverTexture;
        public ResourceLocation hoverTag;
        public List<ItemStack> hoverItems = List.of();
        public HoverMode hoverMode = HoverMode.NONE;
        public float scale = DEFAULT_SCALE;
        public int textureWidth = SIZE_UNSET;
        public int textureHeight = SIZE_UNSET;
        public int sourceWidth = SIZE_UNSET;
        public int sourceHeight = SIZE_UNSET;
        public Alignment alignment = Alignment.LEFT;

        public ParsedLine(Component text) {
            this.text = text;
            this.item = ItemStack.EMPTY;
            this.texture = null;
        }

        public ParsedLine(ItemStack item) {
            this.item = item;
            this.text = null;
            this.texture = null;
        }

        public ParsedLine(ResourceLocation texture) {
            this.texture = texture;
            this.item = ItemStack.EMPTY;
            this.text = null;
        }

        public boolean hasExplicitTooltip = false;
        public boolean isText() { return text != null; }
        public boolean isItem() { return !item.isEmpty(); }
        public boolean isTexture() { return texture != null && text == null; }
        public boolean hasTooltip() {
            return tooltip != null && !tooltip.getString().isBlank();
        }
        public boolean hasHoverPreview() {
            return hoverMode != HoverMode.NONE || hoverTexture != null;
        }
        public void copyHoverInto(ParsedLine target) {
            target.tooltip = tooltip;
            target.hasExplicitTooltip = hasExplicitTooltip;
            target.hoverTexture = hoverTexture;
            target.hoverTag = hoverTag;
            target.hoverItems = hoverItems;
            target.hoverMode = hoverMode;
            target.textureWidth = textureWidth;
            target.textureHeight = textureHeight;
            target.sourceWidth = sourceWidth;
            target.sourceHeight = sourceHeight;
            target.alignment = alignment;
        }
        public int sourceWidth(int fallback) {
            return sourceWidth != SIZE_UNSET ? sourceWidth : fallback;
        }
        public int sourceHeight(int fallback) {
            return sourceHeight != SIZE_UNSET ? sourceHeight : fallback;
        }
        public int drawnWidth(int baseSize) {
            if (textureWidth != SIZE_UNSET) {
                return textureWidth;
            }
            return (int) (baseSize * (scale <= 0 ? DEFAULT_SCALE : scale));
        }
        public int drawnHeight(int baseSize) {
            if (textureHeight != SIZE_UNSET) {
                return textureHeight;
            }
            if (textureWidth != SIZE_UNSET) {
                return textureWidth;
            }
            return (int) (baseSize * (scale <= 0 ? DEFAULT_SCALE : scale));
        }
    }
}
