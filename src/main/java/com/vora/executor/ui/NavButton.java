package com.vora.executor.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class NavButton extends JComponent {

    public enum Icon { CODE, CONSOLE, BOOK, LIST, GEAR, PLUGIN }

    private final Icon icon;
    private final Runnable onClick;
    private boolean active;
    private boolean hover;

    public NavButton(Icon icon, String tooltip, Runnable onClick) {
        this.icon = icon;
        this.onClick = onClick;
        setToolTipText(tooltip);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(40, 40);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);

        int w = getWidth();
        int h = getHeight();

        if (active) {
            g.setColor(Theme.ACCENT_SOFT);
            g.fill(Ui.round(0, 0, w, h, 10));
        } else if (hover) {
            g.setColor(Theme.ELEVATED);
            g.fill(Ui.round(0, 0, w, h, 10));
        }

        Color stroke = active ? Theme.ACCENT_HOVER : (hover ? Theme.TEXT : Theme.MUTED);
        g.setColor(stroke);
        g.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));

        int cx = w / 2;
        int cy = h / 2;
        switch (icon) {
            case CODE -> {
                g.drawLine(cx - 2, cy - 6, cx - 8, cy);
                g.drawLine(cx - 8, cy, cx - 2, cy + 6);
                g.drawLine(cx + 2, cy - 6, cx + 8, cy);
                g.drawLine(cx + 8, cy, cx + 2, cy + 6);
            }
            case CONSOLE -> {
                g.drawLine(cx - 8, cy - 5, cx - 2, cy);
                g.drawLine(cx - 2, cy, cx - 8, cy + 5);
                g.drawLine(cx + 1, cy + 5, cx + 8, cy + 5);
            }
            case BOOK -> {
                g.drawRoundRect(cx - 8, cy - 7, 16, 14, 3, 3);
                g.drawLine(cx - 3, cy - 7, cx - 3, cy + 7);
                g.drawLine(cx - 1, cy - 3, cx + 5, cy - 3);
                g.drawLine(cx - 1, cy + 1, cx + 5, cy + 1);
                g.drawLine(cx - 1, cy + 5, cx + 3, cy + 5);
            }
            case LIST -> {
                g.drawLine(cx - 2, cy - 6, cx + 8, cy - 6);
                g.drawLine(cx - 2, cy, cx + 8, cy);
                g.drawLine(cx - 2, cy + 6, cx + 8, cy + 6);
                g.fillOval(cx - 8, cy - 8, 3, 3);
                g.fillOval(cx - 8, cy - 2, 3, 3);
                g.fillOval(cx - 8, cy + 4, 3, 3);
            }
            case GEAR -> {
                int r = 5;
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
                g.drawOval(cx - 2, cy - 2, 4, 4);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * i / 4.0;
                    int x1 = cx + (int) Math.round(Math.cos(angle) * r);
                    int y1 = cy + (int) Math.round(Math.sin(angle) * r);
                    int x2 = cx + (int) Math.round(Math.cos(angle) * (r + 3));
                    int y2 = cy + (int) Math.round(Math.sin(angle) * (r + 3));
                    g.drawLine(x1, y1, x2, y2);
                }
            }
            case PLUGIN -> {

                g.drawRoundRect(cx - 8, cy - 9, 16, 16, 3, 3);
                g.drawLine(cx - 8, cy - 5, cx + 8, cy - 5);
                g.drawLine(cx - 4, cy - 1, cx + 4, cy - 1);
                g.drawLine(cx - 4, cy + 3, cx + 4, cy + 3);
            }
        }

        g.dispose();
    }
}
