package com.vora.executor.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;


public class GButton extends JButton {

    public enum Style { ACCENT, GHOST, ICON, SUBTLE, BLACK }

    private final Style style;
    private int radius = 8;
    private Color hoverText;
    private boolean danger;

    public GButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);

        switch (style) {
            case ACCENT -> {
                setFont(Theme.ui(Font.BOLD, 12));
                setForeground(Color.WHITE);
                radius = 10;
            }
            case GHOST -> {
                setFont(Theme.ui(Font.PLAIN, 12));
                setForeground(Theme.TEXT);
            }
            case ICON -> {
                setFont(Theme.ui(Font.BOLD, 14));
                setForeground(Theme.MUTED);
                hoverText = Theme.TEXT;
                radius = 7;
            }
            case SUBTLE -> {
                setFont(Theme.ui(Font.PLAIN, 12));
                setForeground(Theme.MUTED);
                hoverText = Theme.TEXT;
                radius = 7;
            }
            case BLACK -> {
                setFont(Theme.ui(Font.BOLD, 12));
                setForeground(Theme.BUTTON_TEXT);
                radius = 10;
            }
        }

        getModel().addChangeListener(e -> updateForeground());
    }

    public GButton radius(int r) {
        this.radius = r;
        return this;
    }

    
    public GButton danger() {
        this.danger = true;
        this.hoverText = java.awt.Color.WHITE;
        return this;
    }

    private void updateForeground() {
        if (danger) {
            setForeground(getModel().isRollover() || getModel().isPressed() ? java.awt.Color.WHITE : Theme.MUTED);
            return;
        }
        if (style == Style.ICON || style == Style.SUBTLE) {
            setForeground(getModel().isRollover() || getModel().isPressed() ? hoverText : Theme.MUTED);
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public void setHoverText(Color color) {
        this.hoverText = color;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);

        int w = getWidth();
        int h = getHeight();
        boolean hover = getModel().isRollover();
        boolean pressed = getModel().isPressed();

        switch (style) {
            case ACCENT -> {
                Color top = pressed ? Theme.ACCENT_2 : (hover ? Theme.ACCENT_HOVER : Theme.ACCENT);
                Color bottom = pressed ? Theme.ACCENT.darker() : (hover ? Theme.ACCENT : Theme.ACCENT_2);
                g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g.fill(Ui.round(0, 0, w, h, radius));

                
                g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, hover ? 60 : 46),
                        0, h * 0.65f, new Color(255, 255, 255, 0)));
                g.fill(Ui.round(0, 0, w, h, radius));

                
                g.setColor(new Color(255, 255, 255, 70));
                g.draw(Ui.round(0.5, 0.5, w - 1, h - 1, radius));
            }
            case GHOST -> {
                g.setColor(hover ? Theme.ELEVATED_HOVER : Theme.ELEVATED);
                g.fill(Ui.round(0, 0, w, h, radius));
                g.setColor(hover ? Theme.ACCENT.darker() : Theme.BORDER);
                g.draw(Ui.round(0.5, 0.5, w - 1, h - 1, radius));
            }
            case ICON, SUBTLE -> {
                if (hover || pressed) {
                    g.setColor(danger ? Theme.RED
                            : (style == Style.SUBTLE ? Theme.ELEVATED : Theme.ELEVATED_HOVER));
                    g.fill(Ui.round(0, 0, w, h, radius));
                }
            }
            case BLACK -> {
                Color top = pressed ? Theme.BUTTON.darker()
                        : (hover ? Theme.BUTTON_HOVER : Theme.BUTTON);
                Color bottom = pressed ? Theme.BUTTON_2.darker() : Theme.BUTTON_2;
                g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g.fill(Ui.round(0, 0, w, h, radius));

                g.setColor(withAlpha(Theme.BUTTON_TEXT, hover ? 45 : 22));
                g.draw(Ui.round(0.5, 0.5, w - 1, h - 1, radius));
            }
        }

        g.dispose();
        super.paintComponent(graphics);
    }
}
