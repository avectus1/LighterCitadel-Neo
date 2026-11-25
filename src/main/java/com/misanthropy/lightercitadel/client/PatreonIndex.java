package com.misanthropy.lightercitadel.client;

import com.github.alexthe666.citadel.Citadel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hash index over Citadel.PATREONS (a list Citadel fills in from a background download), rebuilt
 * whenever the list's size changes.
 */
public final class PatreonIndex {
    private static volatile Set<String> index = Set.of();
    private static volatile int indexedSize = 0;

    private PatreonIndex() {
    }

    public static boolean isPatreon(String name) {
        List<String> patreons = Citadel.PATREONS;
        if (patreons == null) {
            return false;
        }
        int size = patreons.size();
        if (size == 0) {
            return false;
        }
        if (size != indexedSize) {
            try {
                index = new HashSet<>(patreons);
                indexedSize = size;
            } catch (RuntimeException e) {
                // List is being filled right now; fall back to Citadel's own lookup this frame.
                return patreons.contains(name);
            }
        }
        return index.contains(name);
    }

    public static boolean isEmpty() {
        List<String> patreons = Citadel.PATREONS;
        return patreons == null || patreons.isEmpty();
    }
}
