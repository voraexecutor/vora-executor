package com.vora.agent;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;













public final class HookBridge {

    private static final List<Class<?>> HOOKS = new CopyOnWriteArrayList<>();

    private HookBridge() {
    }

    public static void register(String key, Class<?> hook) {
        if (key == null || hook == null) return;
        for (int i = 0; i < HOOKS.size(); i++) {
            if (HOOKS.get(i).getName().equals(key)) {
                HOOKS.set(i, hook);
                return;
            }
        }
        HOOKS.add(hook);
    }

    public static void tick() {
        for (Class<?> hook : HOOKS) {
            try {
                Method run = hook.getMethod("run");
                run.invoke(null);
            } catch (Throwable ignored) {


            }
        }
    }
}