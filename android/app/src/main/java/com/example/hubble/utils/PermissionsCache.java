package com.example.hubble.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-level cache for the current user's effective permissions per server.
 * Written by MainViewModel when permissions are fetched; read by any screen
 * (including those running in separate Activities) that needs instant permission state.
 */
public final class PermissionsCache {

    private static final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    private PermissionsCache() {}

    public static void put(String serverId, Set<String> permissions) {
        if (serverId != null && permissions != null) {
            cache.put(serverId, permissions);
        }
    }

    /** Returns the cached permission set for the server, or null if not yet loaded. */
    public static Set<String> get(String serverId) {
        if (serverId == null) return null;
        return cache.get(serverId);
    }

    public static void invalidate(String serverId) {
        if (serverId != null) cache.remove(serverId);
    }

    public static void clear() {
        cache.clear();
    }
}
