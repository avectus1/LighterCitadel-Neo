package com.misanthropy.lightercitadel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain TOML-ish config read once during mixin bootstrap, before NeoForge's config system exists.
 * Mixins are switched on or off from these values, so a restart is needed after editing.
 */
public final class LighterCitadelConfig {
    public static final String FILE_NAME = "citadel-optimizer.toml";

    // server
    public static boolean fastEntityTickCheckServer = true;
    public static boolean cacheServerDataLookup = true;
    public static boolean fastMasterTick = true;
    public static boolean fastModifierQueries = true;
    public static int tickListIndexThreshold = 32;
    // client
    public static boolean fastEntityTickCheckClient = true;
    public static boolean cacheClientTracker = true;
    public static boolean fastSoundPitch = true;
    public static boolean optimizeHoverAnimations = true;
    public static boolean optimizePatreonFollowerRender = true;
    public static boolean disablePatreonFollowers = false;
    public static boolean skipUnlistenedRendererEvents = false;
    // safety
    public static boolean purgeUnloadedEntities = true;

    private static boolean loaded;

    private LighterCitadelConfig() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = resolvePath();
        try {
            if (Files.exists(path)) {
                Map<String, String> values = parse(path);
                apply(values);
                // Add options introduced by newer versions without touching the user's values.
                if (!values.keySet().containsAll(knownKeys())) {
                    Files.write(path, defaultFile(), StandardCharsets.UTF_8);
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.write(path, defaultFile(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("[LighterCitadel] Could not read or write " + path + ", using defaults: " + e);
        }
    }

    private static Path resolvePath() {
        try {
            return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        } catch (Throwable t) {
            return Paths.get(System.getProperty("user.dir", "."), "config", FILE_NAME);
        }
    }

    private static Map<String, String> parse(Path path) throws IOException {
        Map<String, String> map = new HashMap<>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw;
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '[') {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        return map;
    }

    private static List<String> knownKeys() {
        return List.of("fast_entity_tick_check_server", "cache_server_data_lookup", "fast_master_tick",
                "fast_modifier_queries", "tick_list_index_threshold", "fast_entity_tick_check_client",
                "cache_client_tracker", "fast_sound_pitch", "optimize_hover_animations",
                "optimize_patreon_follower_render", "disable_patreon_followers",
                "skip_unlistened_renderer_events", "purge_unloaded_entities");
    }

    private static void apply(Map<String, String> map) {
        fastEntityTickCheckServer = bool(map, "fast_entity_tick_check_server", fastEntityTickCheckServer);
        cacheServerDataLookup = bool(map, "cache_server_data_lookup", cacheServerDataLookup);
        fastMasterTick = bool(map, "fast_master_tick", fastMasterTick);
        fastModifierQueries = bool(map, "fast_modifier_queries", fastModifierQueries);
        tickListIndexThreshold = Math.max(1, integer(map, "tick_list_index_threshold", tickListIndexThreshold));
        fastEntityTickCheckClient = bool(map, "fast_entity_tick_check_client", fastEntityTickCheckClient);
        cacheClientTracker = bool(map, "cache_client_tracker", cacheClientTracker);
        fastSoundPitch = bool(map, "fast_sound_pitch", fastSoundPitch);
        optimizeHoverAnimations = bool(map, "optimize_hover_animations", optimizeHoverAnimations);
        optimizePatreonFollowerRender = bool(map, "optimize_patreon_follower_render", optimizePatreonFollowerRender);
        disablePatreonFollowers = bool(map, "disable_patreon_followers", disablePatreonFollowers);
        skipUnlistenedRendererEvents = bool(map, "skip_unlistened_renderer_events", skipUnlistenedRendererEvents);
        purgeUnloadedEntities = bool(map, "purge_unloaded_entities", purgeUnloadedEntities);
    }

    private static boolean bool(Map<String, String> map, String key, boolean def) {
        String value = map.get(key);
        if (value == null) {
            return def;
        }
        value = strip(value);
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return def;
    }

    private static int integer(Map<String, String> map, String key, int def) {
        String value = map.get(key);
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(strip(value));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String strip(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            if ((first == '"' || first == '\'') && value.charAt(value.length() - 1) == first) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static List<String> defaultFile() {
        List<String> l = new ArrayList<>();
        l.add("# LighterCitadel - Patches for Citadel!");
        l.add("#");
        l.add("# Every option below is behaviour-preserving: Citadel's tick-rate modifiers, celestial time");
        l.add("# scaling and cosmetic rendering all behave exactly as they do without this mod. The only");
        l.add("# exceptions are the two explicitly cosmetic toggles, which are opt-in and marked as such.");
        l.add("#");
        l.add("# Read once during mixin bootstrap; a game restart is required for changes to take effect.");
        l.add("");
        l.add("[server]");
        l.add("# Skip the whole tick-rate lookup for an entity when no modifier is active. This is the single");
        l.add("# hotpath in Citadel: it runs once per entity per tick on every level.");
        l.add("fast_entity_tick_check_server = " + fastEntityTickCheckServer);
        l.add("# Cache Citadel's saved-data lookup. Without this, every call allocates a factory with two");
        l.add("# capturing lambdas and walks the dimension data storage map.");
        l.add("cache_server_data_lookup = " + cacheServerDataLookup);
        l.add("# Allocation-free master tick with an early exit when nothing is scheduled.");
        l.add("fast_master_tick = " + fastMasterTick);
        l.add("# Early exit for entity/celestial modifier queries when the modifier list is empty.");
        l.add("fast_modifier_queries = " + fastModifierQueries);
        l.add("# Custom-tick-list size at which an identity index replaces the linear scan.");
        l.add("tick_list_index_threshold = " + tickListIndexThreshold);
        l.add("");
        l.add("[client]");
        l.add("fast_entity_tick_check_client = " + fastEntityTickCheckClient);
        l.add("cache_client_tracker = " + cacheClientTracker);
        l.add("# Early exit for sound pitch and client tick rate when no modifier is active.");
        l.add("fast_sound_pitch = " + fastSoundPitch);
        l.add("# Primitive tooltip hover animation state. Also fixes a map growth (memory leak) in Citadel");
        l.add("# where the 'previous frame' map is never cleared, and the 1.21.1 Citadel bug where the hover");
        l.add("# animation is interpolated with the frame time in nanoseconds instead of the partial tick.");
        l.add("optimize_hover_animations = " + optimizeHoverAnimations);
        l.add("# Avoid a username String allocation and a linear patreon-list scan per player per frame.");
        l.add("optimize_patreon_follower_render = " + optimizePatreonFollowerRender);
        l.add("# COSMETIC: set true to stop rendering patreon followers entirely.");
        l.add("disable_patreon_followers = " + disablePatreonFollowers);
        l.add("# EXPERIMENTAL: skip Citadel's four per-entity-per-frame renderer events when no mod listens to");
        l.add("# them. Large win with many visible mobs, but it hooks methods Citadel merges into");
        l.add("# LivingEntityRenderer, so that's why it's opt-in. It self-disables if those methods are not available or found.");
        l.add("skip_unlistened_renderer_events = " + skipUnlistenedRendererEvents);
        l.add("");
        l.add("[safety]");
        l.add("# Drop removed or unloaded entities from Citadel's manual tick list so they cannot keep a whole");
        l.add("# Level alive after a world unload or disconnect.");
        l.add("purge_unloaded_entities = " + purgeUnloadedEntities);
        l.add("");
        return l;
    }
}
