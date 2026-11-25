package com.misanthropy.lightercitadel.runtime;

import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.github.alexthe666.citadel.server.world.CitadelServerData;
import net.minecraft.server.MinecraftServer;

/**
 * Remembers the CitadelServerData of the running server. Citadel looks it up through the overworld's
 * data storage on every call, which allocates a factory with two capturing lambdas each time.
 */
public final class CitadelDataCache {
    private record Entry(MinecraftServer server, CitadelServerData data) {
    }

    private static volatile Entry cache;

    private CitadelDataCache() {
    }

    public static CitadelServerData lookup(MinecraftServer server) {
        Entry entry = cache;
        return entry != null && entry.server == server ? entry.data : null;
    }

    public static void store(MinecraftServer server, CitadelServerData data) {
        Entry entry = cache;
        if (entry == null || entry.server != server || entry.data != data) {
            cache = new Entry(server, data);
        }
    }

    /**
     * The tracker for this server, creating it the same way Citadel would if needed.
     */
    public static ServerTickRateTracker trackerFor(MinecraftServer server) {
        CitadelServerData data = lookup(server);
        return data != null ? data.getOrCreateTickRateTracker() : ServerTickRateTracker.getForServer(server);
    }

    /**
     * The tracker for this server if Citadel has already set one up, without creating anything.
     */
    public static ServerTickRateTracker existingTracker(MinecraftServer server) {
        CitadelServerData data = lookup(server);
        if (data != null) {
            return data.getOrCreateTickRateTracker();
        }
        if (!com.misanthropy.lightercitadel.LighterCitadelConfig.cacheServerDataLookup && server != null) {
            // Nothing is cached when the data cache is turned off, so ask Citadel directly.
            try {
                return ServerTickRateTracker.getForServer(server);
            } catch (Throwable ignored) {
                // Overworld already gone (server shutting down).
            }
        }
        return null;
    }

    public static void invalidate() {
        cache = null;
    }
}
