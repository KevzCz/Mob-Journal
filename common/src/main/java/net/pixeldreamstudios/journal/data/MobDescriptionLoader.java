package net.pixeldreamstudios.journal.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.util.MarkdownParser;
import net.pixeldreamstudios.journal.util.MarkdownParser.ParsedLine;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MobDescriptionLoader {

    public static List<List<ParsedLine>> getDescription(ResourceLocation mobId, LivingEntity mob) {
        ResourceLocation jsonPath = ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID,
                "mobs_desc/" + mobId.getNamespace() + "/" + mobId.getPath() + ".json");

        Optional<Resource> resourceOpt = Minecraft.getInstance().getResourceManager().getResource(jsonPath);
        if (resourceOpt.isEmpty()) {

            List<List<ParsedLine>> nsFallback = getNamespaceDefaultDescription(mobId, mob);
            if (!nsFallback.isEmpty()) {
                return nsFallback;
            }

            List<List<ParsedLine>> fallback = getDefaultDescription(mobId, mob);
            if (!fallback.isEmpty()) {
                return fallback;
            }

            return getFallback(mobId, mob);
        }

        try (InputStreamReader reader = new InputStreamReader(resourceOpt.get().open())) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root == null || !root.isJsonObject()) {

                List<List<ParsedLine>> nsDefault = getNamespaceDefaultDescription(mobId, mob);
                if (!nsDefault.isEmpty()) {
                    return nsDefault;
                }

                List<List<ParsedLine>> globalDefault = getDefaultDescription(mobId, mob);
                if (!globalDefault.isEmpty()) {
                    return globalDefault;
                }

                return getFallback(mobId, mob);
            }

            JsonObject obj = root.getAsJsonObject();

            if (!obj.has("description") || !obj.get("description").isJsonArray()) {

                List<List<ParsedLine>> nsDefault = getNamespaceDefaultDescription(mobId, mob);
                if (!nsDefault.isEmpty()) {
                    return nsDefault;
                }

                List<List<ParsedLine>> globalDefault = getDefaultDescription(mobId, mob);
                if (!globalDefault.isEmpty()) {
                    return globalDefault;
                }

                return getFallback(mobId, mob);
            }

            JsonArray desc = obj.getAsJsonArray("description");
            List<List<ParsedLine>> rows = new ArrayList<>();

            for (JsonElement el : desc) {
                String raw = el.getAsString();
                List<ParsedLine> parsed = MarkdownParser.parse(applyVariables(raw, mobId, mob));
                rows.add(parsed);
            }

            if (MarkdownParser.containsPlaceholder(rows, "{getLootDrops}")) {
                rows = injectLootDrops(rows);
            }

            return rows;

        } catch (Exception e) {
            e.printStackTrace();
            return getFallback(mobId, mob);
        }
    }

    private static List<List<ParsedLine>> getNamespaceDefaultDescription(ResourceLocation mobId, LivingEntity mob) {
        ResourceLocation fallbackJson = ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID,
                "mobs_desc/" + mobId.getNamespace() + "/default.json");

        Optional<Resource> nsRes = Minecraft.getInstance().getResourceManager().getResource(fallbackJson);
        if (nsRes.isEmpty()) {
            return List.of();
        }

        try (InputStreamReader reader = new InputStreamReader(nsRes.get().open())) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root == null || !root.isJsonObject()) {
                return List.of();
            }

            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("description") || !obj.get("description").isJsonArray()) {
                return List.of();
            }

            JsonArray desc = obj.getAsJsonArray("description");
            List<List<ParsedLine>> lines = new ArrayList<>();

            for (JsonElement el : desc) {
                String raw = el.getAsString();
                List<ParsedLine> parsed = MarkdownParser.parse(applyVariables(raw, mobId, mob));
                lines.add(parsed);
            }

            if (MarkdownParser.containsPlaceholder(lines, "{getLootDrops}")) {
                lines = injectLootDrops(lines);
            }

            return lines;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static List<List<ParsedLine>> getFallback(ResourceLocation mobId, LivingEntity mob) {
        List<List<ParsedLine>> rows = new ArrayList<>();

        rows.add(List.of(new ParsedLine(Component.literal("§6" + mob.getDisplayName().getString()))));
        rows.add(List.of(new ParsedLine(Component.literal(""))));

        rows.add(List.of(new ParsedLine(Component.literal("§cHealth:§r " + mob.getMaxHealth()))));
        rows.add(List.of(new ParsedLine(Component.literal("§7Armor:§r " + mob.getArmorValue()))));

        rows.add(List.of(new ParsedLine(Component.literal("§dDrops"))));

        List<ParsedLine> drops = getLootDropLines();
        if (drops.isEmpty()) {
            rows.add(List.of(new ParsedLine(Component.literal("§7(No known drops)"))));
        } else {
            rows.add(drops);
        }
        var stat = JournalClientData.MOB_STATS.getOrDefault(mobId, new MobStat(0, 0));
        rows.add(List.of(new ParsedLine(Component.literal("§aYou've killed this mob §l" + stat.kills() + "§r times."))));
        rows.add(List.of(new ParsedLine(Component.literal("§cThis mob has killed you §l" + stat.deaths() + "§r times."))));

        rows.add(List.of(new ParsedLine(Component.literal(""))));

        rows.add(List.of(new ParsedLine(Component.literal("§7Description data not available."))));

        return rows;
    }

    private static List<List<ParsedLine>> getDefaultDescription(ResourceLocation mobId, LivingEntity mob) {
        ResourceLocation fallbackJson = ResourceLocation.fromNamespaceAndPath(Journal.MOD_ID,
                "mobs_desc/journal/default.json");

        Optional<Resource> defaultRes = Minecraft.getInstance().getResourceManager().getResource(fallbackJson);
        if (defaultRes.isEmpty()) {
            return List.of();
        }

        try (InputStreamReader reader = new InputStreamReader(defaultRes.get().open())) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root == null || !root.isJsonObject()) {
                return List.of();
            }

            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("description") || !obj.get("description").isJsonArray()) {
                return List.of();
            }

            JsonArray desc = obj.getAsJsonArray("description");
            List<List<ParsedLine>> lines = new ArrayList<>();

            for (JsonElement el : desc) {
                String raw = el.getAsString();
                List<ParsedLine> parsed = MarkdownParser.parse(applyVariables(raw, mobId, mob));
                lines.add(parsed);
            }

            if (MarkdownParser.containsPlaceholder(lines, "{getLootDrops}")) {
                lines = injectLootDrops(lines);
            }

            return lines;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static String applyVariables(String input, ResourceLocation mobId, LivingEntity mob) {
        String result = input
                .replace("{mobName}", mob.getDisplayName().getString())
                .replace("{getHealth}", String.valueOf(mob.getMaxHealth()))
                .replace("{getArmor}", String.valueOf(mob.getArmorValue()))
                .replace("{entityType}", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())
                .replace("{namespace}", mobId.getNamespace())
                .replace("{path}", mobId.getPath());

        var stat = JournalClientData.MOB_STATS.getOrDefault(mobId, new MobStat(0, 0));
        boolean isTameable = mob instanceof TamableAnimal;
        result = result.replace("{getTameable}", isTameable ? "Yes" : "No");

        String category = mob.getType().getCategory().getName();
        result = result.replace("{getCategory}", capitalizeFirst(category));

        result = result
                .replace("{getTimesKilled}", String.valueOf(stat.kills()))
                .replace("{getTimesDiedTo}", String.valueOf(stat.deaths()));

        var tags = mob.getType().builtInRegistryHolder().tags().toList();

        if (tags.isEmpty()) {
            result = result.replace("{getTags}", "None");
        } else {
            StringBuilder tagsBuilder = new StringBuilder();
            for (int i = 0; i < tags.size(); i++) {
                String tagPath = tags.get(i).location().getPath();
                tagsBuilder.append(tagPath);
                if (i < tags.size() - 1) {
                    tagsBuilder.append(", ");
                }
            }
            result = result.replace("{getTags}", tagsBuilder.toString());
        }

        result = processAttributePlaceholders(result, mob);

        return result;
    }

    private static String processAttributePlaceholders(String input, LivingEntity mob) {
        Pattern attributePattern = Pattern.compile("\\{attribute\\.([^}]+)\\}");
        Matcher matcher = attributePattern.matcher(input);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String attributeId = matcher.group(1);
            String value = getAttributeValue(mob, attributeId);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String getAttributeValue(LivingEntity mob, String attributeId) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(attributeId);
            if (id == null) {
                return "Invalid ID";
            }

            var attributeEntry = BuiltInRegistries.ATTRIBUTE.getHolder(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.ATTRIBUTE, id));
            if (attributeEntry.isEmpty()) {
                return "Unknown";
            }

            var instance = mob.getAttribute(attributeEntry.get());
            if (instance == null) {
                return "N/A";
            }

            double value = instance.getValue();

            if (value == (long) value) {
                return String.valueOf((long) value);
            } else {
                return String.format("%.2f", value);
            }

        } catch (Exception e) {
            return "Error";
        }
    }

    private static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static List<List<ParsedLine>> injectLootDrops(List<List<ParsedLine>> lines) {
        List<ParsedLine> placeholder = List.of(new ParsedLine(Component.literal("§d{INJECT_LOOT_DROPS}")));
        MarkdownParser.replacePlaceholder(lines, "{getLootDrops}", placeholder);
        return lines;
    }

    private static List<ParsedLine> getLootDropLines() {
        List<ParsedLine> dropIcons = new ArrayList<>();

        for (ItemStack stack : JournalClientData.LAST_DROPS) {
            if (!stack.isEmpty()) {
                ParsedLine icon = new ParsedLine(stack);
                icon.scale = 1.0f;
                dropIcons.add(icon);
            }
        }

        return dropIcons;
    }
}
