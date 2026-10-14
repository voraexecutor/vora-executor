package com.vora.game;






















public final class VoraTickBridge {

    public static volatile Runnable listener;

    private VoraTickBridge() {
    }

    public static void tick() {
        Runnable current = listener;
        if (current != null) {
            try {
                current.run();
            } catch (Throwable ignored) {

            }
        }
    }
}
