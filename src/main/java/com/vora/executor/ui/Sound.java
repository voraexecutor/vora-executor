package com.vora.executor.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;


public final class Sound {

    private Sound() {
    }

    public static void tabSwitch() {
        sequence(new int[][] { { 1250, 26 } }, 0.5);
    }

    public static void execute() {
        sequence(new int[][] { { 523, 60 }, { 659, 55 }, { 784, 120 } }, 0.8);
    }









    private static void sequence(int[][] steps, double volume) {
        Thread thread = new Thread(() -> {
            try {
                float rate = 44100f;
                int total = 0;
                for (int[] step : steps) total += (int) (rate * step[1] / 1000f);
                byte[] data = new byte[Math.max(1, total)];

                int pos = 0;
                for (int[] step : steps) {
                    int freq = step[0];
                    int len = (int) (rate * step[1] / 1000f);
                    double base = 2.0 * Math.PI * freq / rate;
                    for (int i = 0; i < len && pos < data.length; i++, pos++) {
                        double t = i / (double) len;
                        double decay = Math.pow(1 - t, 2.2);
                        double wave = Math.sin(base * i) + 0.35 * Math.sin(base * 2 * i) * (1 - t);
                        double out = wave * decay * 62 * volume;
                        data[pos] = (byte) Math.max(-127, Math.min(127, out));
                    }
                }

                AudioFormat format = new AudioFormat(rate, 8, 1, true, false);
                final Clip clip = AudioSystem.getClip();
                clip.open(format, data, 0, data.length);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            } catch (Throwable ignored) {
            }
        }, "vora-sound");
        thread.setDaemon(true);
        thread.start();
    }
}