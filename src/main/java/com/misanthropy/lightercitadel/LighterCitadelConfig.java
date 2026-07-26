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

public final class LighterCitadelConfig {

    public static final String FILE_NAME = "citadel-optimizer.toml";


    public static boolean fastEntityTickCheckServer = true;
    public static boolean cacheServerDataLookup = true;
    public static boolean fastMasterTick = true;
    public static boolean fastModifierQueries = true;
    public static int tickListIndexThreshold = 32;


    public static boolean fastEntityTickCheckClient = true;
    public static boolean cacheClientTracker = true;
    public static boolean fastSoundPitch = true;
    public static boolean optimizeHoverAnimations = true;
    public static boolean optimizePatreonFollowerRender = true;
    public static boolean disablePatreonFollowers = false;
    public static boolean skipUnlistenedRendererEvents = false;


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
                apply(parse(path));
            } else {
                Files.createDirectories(path.getParent());
                Files.write(path, defaultFile(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("[LighterCitadel] Could not read " + FILE_NAME + ", using defaults: " + e);
        }
    }

    private static Path resolvePath() {
        String gameDir = System.getProperty("user.dir", ".");
        return Paths.get(gameDir, "config", FILE_NAME);
    }

    private static Map<String, String> parse(Path path) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw;
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '[') {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        return values;
    }

    private static void apply(Map<String, String> v) {
        fastEntityTickCheckServer = bool(v, "fast_entity_tick_check_server", fastEntityTickCheckServer);
        cacheServerDataLookup = bool(v, "cache_server_data_lookup", cacheServerDataLookup);
        fastMasterTick = bool(v, "fast_master_tick", fastMasterTick);
        fastModifierQueries = bool(v, "fast_modifier_queries", fastModifierQueries);
        tickListIndexThreshold = Math.max(2, integer(v, tickListIndexThreshold));

        fastEntityTickCheckClient = bool(v, "fast_entity_tick_check_client", fastEntityTickCheckClient);
        cacheClientTracker = bool(v, "cache_client_tracker", cacheClientTracker);
        fastSoundPitch = bool(v, "fast_sound_pitch", fastSoundPitch);
        optimizeHoverAnimations = bool(v, "optimize_hover_animations", optimizeHoverAnimations);
        optimizePatreonFollowerRender = bool(v, "optimize_patreon_follower_render", optimizePatreonFollowerRender);
        disablePatreonFollowers = bool(v, "disable_patreon_followers", disablePatreonFollowers);
        skipUnlistenedRendererEvents = bool(v, "skip_unlistened_renderer_events", skipUnlistenedRendererEvents);

        purgeUnloadedEntities = bool(v, "purge_unloaded_entities", purgeUnloadedEntities);
    }

    private static boolean bool(Map<String, String> v, String key, boolean fallback) {
        String s = v.get(key);
        if (s == null) {
            return fallback;
        }
        s = strip(s);
        if ("true".equalsIgnoreCase(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s)) {
            return false;
        }
        return fallback;
    }

    private static int integer(Map<String, String> v, int fallback) {
        String s = v.get("tick_list_index_threshold");
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(strip(s));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String strip(String s) {
        if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
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
        l.add("fast_entity_tick_check_server = true");
        l.add("# Cache Citadel's saved-data lookup. Without this, every call allocates two capturing lambdas");
        l.add("# and walks the dimension data storage map.");
        l.add("cache_server_data_lookup = true");
        l.add("# Allocation-free master tick with an early exit when nothing is scheduled.");
        l.add("fast_master_tick = true");
        l.add("# Early exit for entity/celestial modifier queries when the modifier list is empty.");
        l.add("fast_modifier_queries = true");
        l.add("# Custom-tick-list size at which an identity index replaces the linear scan.");
        l.add("tick_list_index_threshold = 32");
        l.add("");
        l.add("[client]");
        l.add("fast_entity_tick_check_client = true");
        l.add("cache_client_tracker = true");
        l.add("# Early exit for sound pitch and client tick rate when nun is active.");
        l.add("fast_sound_pitch = true");
        l.add("# Primitive tooltip hover animation state. Also fixes an map growth (mem leak)");
        l.add("# in Citadel where the 'previous frame' map is never cleared.");
        l.add("optimize_hover_animations = true");
        l.add("# Avoid a username String allocation and a linear patreon-list scan per player per frame.");
        l.add("optimize_patreon_follower_render = true");
        l.add("# COSMETIC: set true to stop rendering patreon followers entirely.");
        l.add("disable_patreon_followers = false");
        l.add("# EXPERIMENTAL: skip Citadel's four per-entity-per-frame renderer events when no mod listens to");
        l.add("# them. Large win with many visible mobs, but it hooks methods Citadel merges into");
        l.add("# LivingEntityRenderer, so that's why its opt-in. It self-disables if those methods are not available or found.");
        l.add("skip_unlistened_renderer_events = false");
        l.add("");
        l.add("[safety]");
        l.add("# Drop removed or unloaded entities from Citadel manual tick list so they cannot keep a whole");
        l.add("# Level alive after a world unload or disconnect.");
        l.add("purge_unloaded_entities = true");
        l.add("");
        return l;
    }
}
