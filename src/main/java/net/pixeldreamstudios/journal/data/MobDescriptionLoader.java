package net.pixeldreamstudios.journal.data;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.client.JournalClientData;
import net.pixeldreamstudios.journal.util.MarkdownParser;
import net.pixeldreamstudios.journal.util.MarkdownParser.ParsedLine;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MobDescriptionLoader {

    public static List<List<ParsedLine>> getDescription(Identifier mobId, LivingEntity mob) {
        Identifier jsonPath = Identifier.of("journal", "mobs_desc/" + mobId.getNamespace() + "/" + mobId.getPath() + ".json");

        Optional<Resource> resourceOpt = MinecraftClient.getInstance().getResourceManager().getResource(jsonPath);
        if (resourceOpt.isEmpty()) {

            // 1. Try namespace-level default (e.g. mobs_desc/minecraft/default.json)
            List<List<ParsedLine>> nsFallback = getNamespaceDefaultDescription(mobId, mob);
            if (!nsFallback.isEmpty()) {
                return nsFallback;
            }

            // 2. Try global default (e.g. mobs_desc/journal/default.json)
            List<List<ParsedLine>> fallback = getDefaultDescription(mobId, mob);
            if (!fallback.isEmpty()) {
                return fallback;
            }

            // 3. Fall back to hardcoded values
            return getFallback(mobId, mob);
        }



        try (InputStreamReader reader = new InputStreamReader(resourceOpt.get().getInputStream())) {
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
    private static List<List<ParsedLine>> getNamespaceDefaultDescription(Identifier mobId, LivingEntity mob) {
        Identifier fallbackJson = Identifier.of("journal", "mobs_desc/" + mobId.getNamespace() + "/default.json");

        Optional<Resource> nsRes = MinecraftClient.getInstance().getResourceManager().getResource(fallbackJson);
        if (nsRes.isEmpty()) {
            return List.of();
        }

        try (InputStreamReader reader = new InputStreamReader(nsRes.get().getInputStream())) {
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


    private static List<List<ParsedLine>> getFallback(Identifier mobId, LivingEntity mob) {
        List<List<ParsedLine>> rows = new ArrayList<>();

        rows.add(List.of(new ParsedLine(Text.literal("§6" + mob.getDisplayName().getString()))));
        rows.add(List.of(new ParsedLine(Text.literal(""))));

        rows.add(List.of(new ParsedLine(Text.literal("§cHealth:§r " + mob.getMaxHealth()))));
        rows.add(List.of(new ParsedLine(Text.literal("§7Armor:§r " + mob.getArmor()))));

        rows.add(List.of(new ParsedLine(Text.literal("§dDrops"))));

        List<ParsedLine> drops = getLootDropLines();
        if (drops.isEmpty()) {
            rows.add(List.of(new ParsedLine(Text.literal("§7(No known drops)"))));
        } else {
            rows.add(drops);
        }
        var stat = JournalClientData.MOB_STATS.getOrDefault(mobId, new net.pixeldreamstudios.journal.data.MobStat(0, 0));
        rows.add(List.of(new ParsedLine(Text.literal("§aYou've killed this mob §l" + stat.kills() + "§r times."))));
        rows.add(List.of(new ParsedLine(Text.literal("§cThis mob has killed you §l" + stat.deaths() + "§r times."))));

        rows.add(List.of(new ParsedLine(Text.literal(""))));

        rows.add(List.of(new ParsedLine(Text.literal("§7Description data not available."))));

        return rows;
    }
    private static List<List<ParsedLine>> getDefaultDescription(Identifier mobId, LivingEntity mob) {
        Identifier fallbackJson = Identifier.of("journal", "mobs_desc/journal/default.json");

        Optional<Resource> defaultRes = MinecraftClient.getInstance().getResourceManager().getResource(fallbackJson);
        if (defaultRes.isEmpty()) {
            return List.of();
        }

        try (InputStreamReader reader = new InputStreamReader(defaultRes.get().getInputStream())) {
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

    private static List<List<ParsedLine>> getEmptyJsonFallback(Identifier mobId, LivingEntity mob) {
        List<List<ParsedLine>> rows = new ArrayList<>();
        rows.addAll(getFallback(mobId, mob));
        return rows;
    }

    private static String applyVariables(String input, Identifier mobId, LivingEntity mob) {
        String result = input
                .replace("{mobName}", mob.getDisplayName().getString())
                .replace("{getHealth}", String.valueOf(mob.getMaxHealth()))
                .replace("{getArmor}", String.valueOf(mob.getArmor()))
                .replace("{entityType}", Registries.ENTITY_TYPE.getId(mob.getType()).toString())
                .replace("{namespace}", mobId.getNamespace())
                .replace("{path}", mobId.getPath());

        // Get client-side stats if present
        var stat = net.pixeldreamstudios.journal.client.JournalClientData.MOB_STATS.getOrDefault(mobId, new net.pixeldreamstudios.journal.data.MobStat(0, 0));

        result = result
                .replace("{getTimesKilled}", String.valueOf(stat.kills()))
                .replace("{getTimesDiedTo}", String.valueOf(stat.deaths()));

        return result;
    }


    private static List<List<ParsedLine>> injectLootDrops(List<List<ParsedLine>> lines) {
        List<ParsedLine> dropIcons = getLootDropLines();

        if (dropIcons.isEmpty()) {
            dropIcons.add(new ParsedLine(Text.literal("§7(No known drops)")));
        }

        MarkdownParser.replacePlaceholder(lines, "{getLootDrops}", dropIcons);
        return lines;
    }

    private static List<ParsedLine> getLootDropLines() {
        List<ParsedLine> dropIcons = new ArrayList<>();
        for (ItemStack stack : JournalClientData.LAST_DROPS) {
            if (!stack.isEmpty()) {
                ParsedLine icon = new ParsedLine(stack);
                icon.scale = 1.0f;
                icon.tooltip = stack.getName();
                dropIcons.add(icon);
            }
        }
        return dropIcons;
    }
}
