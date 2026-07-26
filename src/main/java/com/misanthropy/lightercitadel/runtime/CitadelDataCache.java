package com.misanthropy.lightercitadel.runtime;

import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.github.alexthe666.citadel.server.world.CitadelServerData;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class CitadelDataCache {

    private record Entry(MinecraftServer server, CitadelServerData data) {
    }

    private static volatile Entry cache;

    private CitadelDataCache() {
    }

    @Nullable
    public static CitadelServerData lookup(MinecraftServer server) {
        Entry e = cache;
        return e != null && e.server == server ? e.data : null;
    }

    public static void store(MinecraftServer server, CitadelServerData data) {
        Entry e = cache;
        if (e == null || e.server != server || e.data != data) {
            cache = new Entry(server, data);
        }
    }

    public static ServerTickRateTracker trackerFor(MinecraftServer server) {
        CitadelServerData data = lookup(server);
        if (data != null) {
            return data.getOrCreateTickRateTracker();
        }
        return ServerTickRateTracker.getForServer(server);
    }

    @Nullable
    public static ServerTickRateTracker existingTracker(MinecraftServer server) {
        CitadelServerData data = lookup(server);
        return data == null ? null : data.getOrCreateTickRateTracker();
    }

    public static void invalidate() {
        cache = null;
    }
}
