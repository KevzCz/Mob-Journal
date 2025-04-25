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
    public static Set<Identifier> blacklistedMobs       = new HashSet<>();
    public static ToastPosition toastPosition           = ToastPosition.TOP_RIGHT;

    // ← NEW FIELDS
    /** record server‐time (ticks) when a mob is discovered? */
    public static boolean recordDiscoveryTimestamp = true;
    /** show that timestamp in your GUI? */
    public static boolean showDiscoveryDate        = true;
    /** how often (in ticks) the client scans for nearby mobs */
    public static int     mobCheckInterval         = 40;
    /** how far (in blocks) the client scan radius is */
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
        def.blacklisted_mobs             = List.of("examplemod:badmob","examplenamespace:*");
        def.toast_position               = "top_right";
        def.record_discovery_timestamp   = recordDiscoveryTimestamp;
        def.show_discovery_date          = showDiscoveryDate;
        def.mob_check_interval           = mobCheckInterval;
        def.mob_check_radius             = mobCheckRadius;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(def, writer);
        } catch (Exception e) {
            System.err.println("[Journal] Failed to save default config: " + e);
        }
    }

    private static class ConfigData {
        public List<String> blacklisted_mobs;
        public String       toast_position;
        public boolean      record_discovery_timestamp;
        public boolean      show_discovery_date;
        public int          mob_check_interval;
        public double       mob_check_radius;
    }
}
