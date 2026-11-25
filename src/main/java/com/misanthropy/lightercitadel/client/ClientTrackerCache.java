package com.misanthropy.lightercitadel.client;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import net.minecraft.client.Minecraft;

/**
 * Citadel keeps its client tracker in a HashMap keyed by the Minecraft instance and looks it up
 * several times per entity per frame. There is only ever one Minecraft instance.
 */
public final class ClientTrackerCache {
    private record Entry(Minecraft client, ClientTickRateTracker tracker) {
    }

    private static volatile Entry cache;

    private ClientTrackerCache() {
    }

    public static ClientTickRateTracker lookup(Minecraft client) {
        Entry entry = cache;
        return entry != null && entry.client == client ? entry.tracker : null;
    }

    public static void store(Minecraft client, ClientTickRateTracker tracker) {
        Entry entry = cache;
        if (entry == null || entry.client != client || entry.tracker != tracker) {
            cache = new Entry(client, tracker);
        }
    }

    public static ClientTickRateTracker existing() {
        Entry entry = cache;
        return entry == null ? null : entry.tracker;
    }

    public static void invalidate() {
        cache = null;
    }
}
