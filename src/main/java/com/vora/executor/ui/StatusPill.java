package com.vora.executor.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;


public class StatusPill extends JComponent {

    public enum State { IDLE, ATTACHED, ERROR, WORKING }

    private State state = State.IDLE;

    public StatusPill() {
        setFont(Theme.ui(Font.BOLD, 11));
    }

    public void setState(State state) {
        this.state = state;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (hidden()) return new Dimension(0, 26);
        FontMetrics fm = getFontMetrics(getFont());
        String text = label();
        return new Dimension(fm.stringWidth(text) + 34, 26);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private String label() {
        return switch (state) {
            case IDLE, ATTACHED, WORKING -> "";
            case ERROR -> "ERROR";
        };
    }

    
    private boolean hidden() {
        return state == State.IDLE || state == State.ATTACHED || state == State.WORKING;
    }

    private Color color() {
        return switch (state) {
            case IDLE -> Theme.MUTED;
            case ATTACHED -> Theme.GREEN;
            case ERROR -> Theme.RED;
            case WORKING -> Theme.AMBER;
        };
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);

        if (hidden()) {
            g.dispose();
            return;
        }

        int w = getWidth();
        int h = getHeight();
        Color color = color();

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 34));
        g.fill(Ui.round(0, 0, w, h, h));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
        g.draw(Ui.round(0.5, 0.5, w - 1, h - 1, h));

        int dot = 7;
        g.setColor(color);
        g.fillOval(11, (h - dot) / 2, dot, dot);

        String text = label();
        if (!text.isEmpty()) {
            g.setColor(color);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int textX = 11 + dot + 7;
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, textX, baseline);
        }

        g.dispose();
    }
}
