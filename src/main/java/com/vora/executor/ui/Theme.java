package com.vora.executor.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;


public final class Theme {

    public enum Mode { DARK, LIGHT }

    public static Color WINDOW;
    public static Color PANEL;
    public static Color ELEVATED;
    public static Color ELEVATED_HOVER;
    public static Color BORDER;
    public static Color BORDER_SOFT;

    public static Color ACCENT;
    public static Color ACCENT_2;
    public static Color ACCENT_HOVER;
    public static Color ACCENT_SOFT;

    public static Color BUTTON;
    public static Color BUTTON_2;
    public static Color BUTTON_HOVER;
    public static Color BUTTON_TEXT;

    public static Color TEXT;
    public static Color MUTED;
    public static Color FAINT;

    public static Color GREEN;
    public static Color RED;
    public static Color AMBER;
    public static Color BLUE;

    public static Color CODE_BG;
    public static Color GUTTER_BG;
    public static Color GUTTER_TEXT;
    public static Color GUTTER_ACTIVE;
    public static Color SELECTION;
    public static Color CARET;

    private static Mode mode = Mode.DARK;

    static {
        apply(Mode.DARK);
    }

    private Theme() {
    }

    public static Mode mode() {
        return mode;
    }

    public static void apply(Mode next) {
        mode = next == null ? Mode.DARK : next;
        if (mode == Mode.LIGHT) {
            light();
        } else {
            dark();
        }
    }

    private static void dark() {
        WINDOW = new Color(0x000000);
        PANEL = new Color(0x050506);
        ELEVATED = new Color(0x0C0C0E);
        ELEVATED_HOVER = new Color(0x17171B);
        BORDER = new Color(0x1E1E22);
        BORDER_SOFT = new Color(0x141416);

        ACCENT = new Color(0x8B5CF6);
        ACCENT_2 = new Color(0x6366F1);
        ACCENT_HOVER = new Color(0xA78BFA);
        ACCENT_SOFT = new Color(0x241E38);

        BUTTON = new Color(0xFFFFFF);
        BUTTON_2 = new Color(0xE2E2E8);
        BUTTON_HOVER = new Color(0xF1F1F5);
        BUTTON_TEXT = new Color(0x0A0A0C);

        TEXT = new Color(0xEDEDF0);
        MUTED = new Color(0x7C7C87);
        FAINT = new Color(0x4C4C55);

        GREEN = new Color(0x22C55E);
        RED = new Color(0xEF4444);
        AMBER = new Color(0xEAB308);
        BLUE = new Color(0x60A5FA);

        CODE_BG = new Color(0x000000);
        GUTTER_BG = new Color(0x000000);
        GUTTER_TEXT = new Color(0x3A3A42);
        GUTTER_ACTIVE = new Color(0xA78BFA);
        SELECTION = new Color(0x241E38);
        CARET = new Color(0xC4B5FD);
    }

    private static void light() {
        WINDOW = new Color(0xF4F4F7);
        PANEL = new Color(0xFFFFFF);
        ELEVATED = new Color(0xFFFFFF);
        ELEVATED_HOVER = new Color(0xEFEFF4);
        BORDER = new Color(0xD9D9E0);
        BORDER_SOFT = new Color(0xE8E8EE);

        ACCENT = new Color(0x7C3AED);
        ACCENT_2 = new Color(0x6366F1);
        ACCENT_HOVER = new Color(0x8B5CF6);
        ACCENT_SOFT = new Color(0xEDE7FE);

        BUTTON = new Color(0x000000);
        BUTTON_2 = new Color(0x17171B);
        BUTTON_HOVER = new Color(0x2A2A31);
        BUTTON_TEXT = new Color(0xFFFFFF);

        TEXT = new Color(0x1A1A20);
        MUTED = new Color(0x5C5C68);
        FAINT = new Color(0x9A9AA6);

        GREEN = new Color(0x16A34A);
        RED = new Color(0xDC2626);
        AMBER = new Color(0xD97706);
        BLUE = new Color(0x2563EB);

        CODE_BG = new Color(0xFFFFFF);
        GUTTER_BG = new Color(0xFAFAFC);
        GUTTER_TEXT = new Color(0xB2B2BE);
        GUTTER_ACTIVE = new Color(0x7C3AED);
        SELECTION = new Color(0xDDD6FE);
        CARET = new Color(0x7C3AED);
    }

    public static Font ui(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static Font mono(int style, int size) {
        List<String> candidates = Arrays.asList(
                "JetBrains Mono", "Cascadia Mono", "Cascadia Code", "Consolas", "Monospaced");
        List<String> available = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : candidates) {
            if (available.contains(name)) return new Font(name, style, size);
        }
        return new Font(Font.MONOSPACED, style, size);
    }
}
