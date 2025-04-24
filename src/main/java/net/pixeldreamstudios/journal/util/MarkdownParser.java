package net.pixeldreamstudios.journal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {

    // Markdown styling
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern TITLE = Pattern.compile("## (.+)");
    private static final Pattern HOVER_TEXT = Pattern.compile("\\[(.+?)\\]\\(hover:(.*?)\\)");

    // Extended inline tag support with optional scale and tooltip
    private static final Pattern INLINE_TAG = Pattern.compile(
            "(!)?\\[(.*?)\\]\\((item|texture):([^\\s\")]+)(?:\\s+scale=([0-9.]+))?(?:\\s+\"(.*?)\")?\\)"
    );
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

    public static List<ParsedLine> parse(String line) {
        List<ParsedLine> parts = new ArrayList<>();

        // Handle headers (titles)
        Matcher title = TITLE.matcher(line);
        if (title.matches()) {
            parts.add(new ParsedLine(Text.literal(title.group(1)).formatted(Formatting.UNDERLINE, Formatting.BOLD)));
            return parts;
        }

        String processed = replaceMarkdown(line);

        // Parse hover text links
        Matcher hoverText = HOVER_TEXT.matcher(processed);
        while (hoverText.find()) {
            String label = hoverText.group(1);
            String tooltip = hoverText.group(2);

            ParsedLine linePart = new ParsedLine(Text.literal(label).styled(s -> s.withColor(Formatting.AQUA)));
            linePart.tooltip = Text.literal(tooltip);
            parts.add(linePart);

            processed = processed.replace(hoverText.group(0), "");
        }

        // Parse inline tags (item/texture)
        Matcher matcher = INLINE_TAG.matcher(processed);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add preceding text as literal
            if (matcher.start() > lastEnd) {
                String before = processed.substring(lastEnd, matcher.start());
                if (!before.isBlank()) {
                    parts.add(new ParsedLine(Text.literal(before)));
                }
            }

            boolean isBang = matcher.group(1) != null; // Starts with !
            String label = matcher.group(2); // unused
            String type = matcher.group(3); // item or texture
            String value = matcher.group(4); // item ID or texture path
            String scaleStr = matcher.group(5); // optional
            String tooltipStr = matcher.group(6); // optional

            float scale = 1.0f;
            if (scaleStr != null) {
                try {
                    scale = Float.parseFloat(scaleStr);
                } catch (NumberFormatException ignored) {}
            }

            if ("item".equals(type)) {
                Identifier id = Identifier.tryParse(value);
                if (id != null && Registries.ITEM.containsId(id)) {
                    ItemStack stack = new ItemStack(Registries.ITEM.get(id));
                    ParsedLine linePart = new ParsedLine(stack);
                    linePart.scale = scale;

                    if (tooltipStr != null && !tooltipStr.strip().isEmpty()) {
                        linePart.tooltip = Text.literal(replaceMarkdown(tooltipStr));
                        linePart.hasExplicitTooltip = true;
                    }

                    parts.add(linePart);
                } else {
                    parts.add(new ParsedLine(Text.literal("❓" + value).formatted(Formatting.GRAY)));
                }
            } else if ("texture".equals(type)) {
                Identifier id = Identifier.tryParse(value);
                if (id != null) {
                    ParsedLine linePart = new ParsedLine(id);
                    linePart.scale = scale;

                    if (tooltipStr != null && !tooltipStr.strip().isEmpty()) {
                        linePart.tooltip = Text.literal(replaceMarkdown(tooltipStr));
                    }

                    parts.add(linePart);
                }
            }

            lastEnd = matcher.end();
        }

        // Add any remaining text
        if (lastEnd < processed.length()) {
            String trailing = processed.substring(lastEnd);
            if (!trailing.isBlank()) {
                parts.add(new ParsedLine(Text.literal(trailing)));
            }
        }

        return parts;
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
        // Replace **bold** and *italic* markdown
        String result = BOLD.matcher(input).replaceAll("§l$1§r");
        result = ITALIC.matcher(result).replaceAll("§o$1§r");

        // Replace {color} placeholders
        for (Map.Entry<String, String> entry : COLOR_CODES.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }

    public static class ParsedLine {
        public final Text text;
        public final ItemStack item;
        public final Identifier texture;
        public Text tooltip;
        public Identifier hoverTexture;
        public float scale = 1.0f;

        public ParsedLine(Text text) {
            this.text = text;
            this.item = ItemStack.EMPTY;
            this.texture = null;
        }

        public ParsedLine(ItemStack item) {
            this.item = item;
            this.text = null;
            this.texture = null;
        }

        public ParsedLine(Identifier texture) {
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

    }

}
