package net.pixeldreamstudios.journal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class JournalConfig {
    private static final File CONFIG_FILE = new File("config/journal.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String configVersion = getCurrentModVersion();

    public static Set<String> blacklistedNamespaces = new HashSet<>();
    public static Set<Identifier> blacklistedMobs    = new HashSet<>();
    public static ToastPosition toastPosition        = ToastPosition.TOP_RIGHT;

    // New fields
    public static boolean recordDiscoveryTimestamp = true;
    public static boolean showDiscoveryDate        = true;
    public static int     mobCheckInterval         = 40;
    public static double  mobCheckRadius           = 8.0;

    public enum ToastPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;
        public static ToastPosition fromString(String value) {
            try { return value == null ? TOP_RIGHT : valueOf(value.toUpperCase()); }
            catch (IllegalArgumentException e) { return TOP_RIGHT; }
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

                // Migrate if missing fields
                migrateOldConfig(data);

                blacklistedMobs.clear();
                blacklistedNamespaces.clear();
                if (data.blacklisted_mobs != null) {
                    for (String idStr : data.blacklisted_mobs) {
                        if (idStr.endsWith(":*")) {
                            blacklistedNamespaces.add(idStr.substring(0, idStr.length() - 2));
                        } else {
                            Identifier id = Identifier.tryParse(idStr);
                            if (id != null) blacklistedMobs.add(id);
                        }
                    }
                }

                toastPosition            = ToastPosition.fromString(data.toast_position);
                recordDiscoveryTimestamp = data.record_discovery_timestamp;
                showDiscoveryDate        = data.show_discovery_date;
                mobCheckInterval         = data.mob_check_interval;
                mobCheckRadius           = data.mob_check_radius;
            }
        } catch (Exception e) {
            System.err.println("[Journal] Failed to load config: " + e);
        }
    }

    public static boolean isBlacklisted(Identifier id) {
        return blacklistedMobs.contains(id)
                || blacklistedNamespaces.contains(id.getNamespace());
    }

    private static void saveDefault() {
        ConfigData def = new ConfigData();
        def.blacklisted_mobs = List.of(
                "minecraft:armor_stand",
                "examplemod:badmob",
                "examplenamespace:*"
        );
        def.toast_position = "top_right";
        def.record_discovery_timestamp = recordDiscoveryTimestamp;
        def.show_discovery_date = showDiscoveryDate;
        def.mob_check_interval = mobCheckInterval;
        def.mob_check_radius = mobCheckRadius;
        def.current_version = configVersion;

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(def, writer);
        } catch (Exception e) {
            System.err.println("[Journal] Failed to save default config: " + e);
        }
    }

    private static void migrateOldConfig(ConfigData data) {
        boolean changed = false;

        if (data.blacklisted_mobs == null) {
            data.blacklisted_mobs = List.of(
                    "minecraft:armor_stand",
                    "examplemod:badmob",
                    "examplenamespace:*"
            );
            changed = true;
        }
        if (data.toast_position == null) {
            data.toast_position = "top_right";
            changed = true;
        }
        if (data.mob_check_interval == 0) {
            data.mob_check_interval = 40;
            changed = true;
        }
        if (data.mob_check_radius == 0.0) {
            data.mob_check_radius = 8.0;
            changed = true;
        }
        // 🧠 Here's the smart part:
        if (data.record_discovery_timestamp == null) {
            data.record_discovery_timestamp = true;
            changed = true;
        }
        if (data.show_discovery_date == null) {
            data.show_discovery_date = true;
            changed = true;
        }
        if (data.current_version == null || !data.current_version.equals(configVersion)) {
            data.current_version = configVersion;
            changed = true;
        }

        if (changed) {
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
                System.out.println("[Journal] Config updated to version " + configVersion);
            } catch (Exception e) {
                System.err.println("[Journal] Failed to migrate config: " + e);
            }
        }
    }


    private static String getCurrentModVersion() {
        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("journal");
        return mod.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    private static class ConfigData {
        public List<String> blacklisted_mobs;
        public String       toast_position;
        public Boolean      record_discovery_timestamp;
        public Boolean      show_discovery_date;
        public int          mob_check_interval;
        public double       mob_check_radius;
        public String       current_version;
    }
}
