package com.misanthropy.lightercitadel.client;

import com.github.alexthe666.citadel.Citadel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PatreonIndex {

    private static volatile Set<String> index = Set.of();
    private static volatile int indexedSize = -1;

    private PatreonIndex() {
    }

    public static boolean isPatreon(String username) {
        List<String> source = Citadel.PATREONS;
        int size = source.size();
        if (size == 0) {
            return false;
        }
        Set<String> local = index;
        if (indexedSize != size) {
            try {
                local = new HashSet<>(source);
                index = local;
                indexedSize = size;
            } catch (RuntimeException stillBeingFilled) {
                return source.contains(username);
            }
        }
        return local.contains(username);
    }

    public static boolean isEmpty() {
        return Citadel.PATREONS.isEmpty();
    }
}
