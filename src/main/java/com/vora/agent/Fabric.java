package com.vora.agent;


public class Fabric {

    public static void remove() {
        Bridge.log("[VORA] executor mode - nothing to unload");
    }

    
    public static ClassLoader findClassLoader() {
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (canLoadGame(system)) return system;

        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            ClassLoader context = thread.getContextClassLoader();
            if (canLoadGame(context)) return context;
        }

        ClassLoader knot = findKnotClassLoader();
        if (knot != null && canLoadGame(knot)) return knot;

        return null;
    }

    static boolean canLoadGame(ClassLoader loader) {
        if (loader == null) return false;
        for (String name : new String[] { "net.minecraft.client.MinecraftClient", "net.minecraft.class_310" }) {
            try {
                Class.forName(name, false, loader);
                return true;
            } catch (Throwable ignored) {
                
            }
        }
        return false;
    }

    private static ClassLoader findKnotClassLoader() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) root = root.getParent();
        Thread[] buf = new Thread[root.activeCount() + 64];
        int n = root.enumerate(buf, true);
        for (int i = 0; i < n; i++) {
            ClassLoader cl = buf[i].getContextClassLoader();
            if (cl != null
                    && (cl.getClass().getName().contains("KnotClassLoader")
                        || cl.getClass().getName().contains("FabricClassLoader"))) {
                return cl;
            }
        }
        return null;
    }
}
