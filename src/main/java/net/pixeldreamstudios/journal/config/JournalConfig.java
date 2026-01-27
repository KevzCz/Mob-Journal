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
    private static final File BLACKLIST_FILE = new File("config/journal_blacklist.json");
    private static final File RENDER_CONFIG_FILE = new File("config/journal_render.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String configVersion = getCurrentModVersion();

    public enum DiscoveryMode {
        NEAR,
        HIT,
        KILL,
        INTERACT
    }

    public static Set<Identifier> blacklistedMobs = new HashSet<>();
    public static ToastPosition toastPosition = ToastPosition.TOP_RIGHT;
    public static boolean recordDiscoveryTimestamp = true;
    public static boolean showDiscoveryDate = true;

    public static int mobCheckInterval = 40;
    public static double mobCheckRadius = 8.0;

    public static boolean requireJournalInInventory = true;
    public static boolean giveJournalOnJoin = true;

    public static DiscoveryMode discoveryMode = DiscoveryMode.NEAR;
    public static boolean enableTamedTrigger = false;

    public enum ToastPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;
        public static ToastPosition fromString(String value) {
            try { return value == null ? TOP_RIGHT : valueOf(value.toUpperCase()); }
            catch (IllegalArgumentException e) { return TOP_RIGHT; }
        }
    }

    public static void load() {
        try {
            if (! CONFIG_FILE.exists()) {
                saveDefault();
            } else {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    migrateOldConfig(data);

                    toastPosition = ToastPosition.fromString(data.toast_position);
                    recordDiscoveryTimestamp = getOrDefault(data.record_discovery_timestamp, true);
                    showDiscoveryDate = getOrDefault(data.show_discovery_date, true);
                    mobCheckInterval = data.mob_check_interval == 0 ? 40 :  data.mob_check_interval;
                    mobCheckRadius = data.mob_check_radius == 0.0 ? 8.0 : data.mob_check_radius;
                    requireJournalInInventory = getOrDefault(data.require_journal_in_inventory, true);
                    giveJournalOnJoin = getOrDefault(data.give_journal_on_join, true);

                    discoveryMode = parseDiscoveryMode(data.discovery_mode);
                    enableTamedTrigger = getOrDefault(data.enable_tamed_trigger, false);
                }
            }

            loadBlacklist();
            loadRenderConfigs();
        } catch (Exception e) {
            System.err.println("[Journal] Failed to load config: " + e);
        }
    }

    public static void loadBlacklist() {
        blacklistedMobs.clear();
        if (! BLACKLIST_FILE.exists()) {
            saveBlacklist();
            return;
        }

        try (FileReader reader = new FileReader(BLACKLIST_FILE)) {
            BlacklistData data = GSON.fromJson(reader, BlacklistData.class);
            if (data != null && data.blacklisted_mobs != null) {
                for (String idStr : data.blacklisted_mobs) {
                    Identifier id = Identifier.tryParse(idStr);
                    if (id != null) {
                        blacklistedMobs.add(id);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Journal] Failed to load blacklist: " + e);
        }
    }

    public static void saveBlacklist() {
        try {
            BlacklistData data = new BlacklistData();
            data.blacklisted_mobs = blacklistedMobs.stream()
                    .map(Identifier::toString)
                    .sorted()
                    .toList();

            try (FileWriter writer = new FileWriter(BLACKLIST_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            System.err.println("[Journal] Failed to save blacklist:  " + e);
        }
    }

    public static void addToBlacklist(Identifier mobId) {
        if (blacklistedMobs.add(mobId)) {
            saveBlacklist();
        }
    }

    public static void removeFromBlacklist(Identifier mobId) {
        if (blacklistedMobs.remove(mobId)) {
            saveBlacklist();
        }
    }

    public static boolean isBlacklisted(Identifier id) {
        return blacklistedMobs.contains(id);
    }

    private static void saveDefault() {
        ConfigData def = new ConfigData();
        def.toast_position = "top_right";
        def.record_discovery_timestamp = recordDiscoveryTimestamp;
        def.show_discovery_date = showDiscoveryDate;
        def.mob_check_interval = mobCheckInterval;
        def.mob_check_radius = mobCheckRadius;
        def.current_version = configVersion;
        def.require_journal_in_inventory = requireJournalInInventory;
        def.give_journal_on_join = giveJournalOnJoin;
        def.discovery_mode = "NEAR";
        def.enable_tamed_trigger = false;
        def.discovery_mode_options = List.of("NEAR", "HIT", "INTERACT", "KILL");

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(def, writer);
        } catch (Exception e) {
            System.err.println("[Journal] Failed to save default config: " + e);
        }
    }

    private static void migrateOldConfig(ConfigData data) {
        boolean changed = false;

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
        if (data.require_journal_in_inventory == null) {
            data.require_journal_in_inventory = true;
            changed = true;
        }
        if (data.give_journal_on_join == null) {
            data.give_journal_on_join = true;
            changed = true;
        }
        if (data.discovery_mode == null) {
            data.discovery_mode = "NEAR";
            changed = true;
        }
        if (data.enable_tamed_trigger == null) {
            data.enable_tamed_trigger = false;
            changed = true;
        }
        if (data.discovery_mode_options == null || data.discovery_mode_options.isEmpty()) {
            data.discovery_mode_options = List.of("NEAR", "HIT", "INTERACT", "KILL");
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

    private static DiscoveryMode parseDiscoveryMode(String value) {
        if (value == null) return DiscoveryMode.NEAR;
        try {
            return DiscoveryMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DiscoveryMode.NEAR;
        }
    }

    private static String getCurrentModVersion() {
        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("journal");
        return mod.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    private static boolean getOrDefault(Boolean b, boolean d) {
        return b == null ? d : b;
    }

    public static class MobRenderConfig {
        public float scale = 1.0f;
        public float xOffset = 0.0f;
        public float yOffset = 0.0f;
        public float speed = 0.6f;
        public float smoothing = 12.0f;
        public AnimationMode animationMode = AnimationMode.WALKING;

        public MobRenderConfig() {}

        public MobRenderConfig(float scale, float xOffset, float yOffset, float speed, float smoothing, AnimationMode animationMode) {
            this.scale = scale;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.speed = speed;
            this.smoothing = smoothing;
            this.animationMode = animationMode;
        }
    }

    public enum AnimationMode {
        IDLE,
        WALKING,
        STATIC
    }

    public static void setMobRenderConfig(Identifier mobId, boolean isGridMode, float scale, float xOffset, float yOffset, float speed, float smoothing, AnimationMode animationMode) {
        String key = mobId.toString();
        Map<String, MobRenderConfig> configs = isGridMode ? gridRenderConfigs : detailRenderConfigs;
        configs.put(key, new MobRenderConfig(scale, xOffset, yOffset, speed, smoothing, animationMode));
    }

    public static void deleteMobRenderConfig(Identifier mobId, boolean isGridMode) {
        String key = mobId.toString();
        Map<String, MobRenderConfig> configs = isGridMode ? gridRenderConfigs :  detailRenderConfigs;
        configs.remove(key);
    }

    public static Map<String, MobRenderConfig> gridRenderConfigs = new HashMap<>();
    public static Map<String, MobRenderConfig> detailRenderConfigs = new HashMap<>();
    public static MobRenderConfig getMobRenderConfig(Identifier mobId, boolean isGridMode) {
        String key = mobId.toString();
        Map<String, MobRenderConfig> configs = isGridMode ? gridRenderConfigs : detailRenderConfigs;
        return configs.getOrDefault(key, new MobRenderConfig());
    }

    public static void loadRenderConfigs() {
        try {
            if (!RENDER_CONFIG_FILE.exists()) {
                return;
            }
            try (FileReader reader = new FileReader(RENDER_CONFIG_FILE)) {
                RenderConfigData data = GSON.fromJson(reader, RenderConfigData.class);
                if (data != null) {
                    if (data.grid_configs != null) {
                        gridRenderConfigs = data.grid_configs;
                    }
                    if (data.detail_configs != null) {
                        detailRenderConfigs = data.detail_configs;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Journal] Failed to load render configs: " + e);
        }
    }

    public static void saveRenderConfigs() {
        try {
            RenderConfigData data = new RenderConfigData();
            data.grid_configs = gridRenderConfigs;
            data.detail_configs = detailRenderConfigs;

            try (FileWriter writer = new FileWriter(RENDER_CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            System.err.println("[Journal] Failed to save render configs: " + e);
        }
    }

    private static class BlacklistData {
        public List<String> blacklisted_mobs;
    }

    private static class RenderConfigData {
        public Map<String, MobRenderConfig> grid_configs;
        public Map<String, MobRenderConfig> detail_configs;
    }

    private static class ConfigData {
        public String toast_position;
        public Boolean record_discovery_timestamp;
        public Boolean show_discovery_date;
        public int mob_check_interval;
        public double mob_check_radius;
        public String current_version;
        public Boolean require_journal_in_inventory;
        public Boolean give_journal_on_join;
        public String discovery_mode;
        public Boolean enable_tamed_trigger;
        public List<String> discovery_mode_options;
    }
}