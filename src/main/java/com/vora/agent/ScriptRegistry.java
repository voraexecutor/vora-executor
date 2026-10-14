package com.vora.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;





public final class ScriptRegistry {

    private static final Map<String, ClassLoader> LOADED = new ConcurrentHashMap<>();

    private ScriptRegistry() {
    }

    public static void register(String id, ClassLoader loader, Class<?> scriptClass) {
        if (id == null || id.isEmpty() || loader == null || scriptClass == null) return;

        LOADED.put(id, loader);
    }

    public static int size() {
        return LOADED.size();
    }
}