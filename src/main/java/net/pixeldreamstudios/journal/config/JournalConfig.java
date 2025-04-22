package net.pixeldreamstudios.journal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JournalConfig {
    private static final File CONFIG_FILE = new File("config/journal.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Set<String> blacklistedNamespaces = new HashSet<>();

    public static Set<Identifier> blacklistedMobs = new HashSet<>();
    public static ToastPosition toastPosition = ToastPosition.TOP_RIGHT;

    public enum ToastPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public static ToastPosition fromString(String value) {
            try {
                return ToastPosition.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return TOP_RIGHT;
            }
        }
    }


    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                saveDefault();
                return;
            }

            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);

                blacklistedMobs.clear();
                blacklistedNamespaces.clear();

                if (data.blacklisted_mobs != null) {
                    for (String idStr : data.blacklisted_mobs) {
                        if (idStr.endsWith(":*")) {
                            String namespace = idStr.substring(0, idStr.length() - 2);
                            blacklistedNamespaces.add(namespace);
                        } else {
                            Identifier id = Identifier.tryParse(idStr);
                            if (id != null) blacklistedMobs.add(id);
                        }
                    }
                }


                toastPosition = ToastPosition.fromString(data.toast_position);
            }

        } catch (Exception e) {
            System.err.println("[Journal] Failed to load config: " + e);
        }
    }
    public static boolean isBlacklisted(Identifier id) {
        return blacklistedMobs.contains(id) || blacklistedNamespaces.contains(id.getNamespace());
    }

    private static void saveDefault() {
        ConfigData defaultData = new ConfigData();
        defaultData.blacklisted_mobs = List.of("examplemod:badmob","examplenamespace:*");
        defaultData.toast_position = "top_right";

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(defaultData, writer);
        } catch (Exception e) {
            System.err.println("[Journal] Failed to save default config: " + e);
        }
    }

    private static class ConfigData {
        public List<String> blacklisted_mobs;
        public String toast_position;
    }
}
