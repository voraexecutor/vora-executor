package com.vora.executor.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class Button extends JComponent {

    public enum Glyph { MINIMIZE, CLOSE }

    private final Glyph glyph;
    private final boolean danger;
    private final Runnable action;
    private boolean hover;
    private boolean pressed;

    public Button(Glyph glyph, String tooltip, boolean danger, Runnable action) {
        this.glyph = glyph;
        this.danger = danger;
        this.action = action;
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
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
                if (hover) action.run();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(30, 26);
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

        if (hover || pressed) {
            g.setColor(danger ? Theme.RED : Theme.ELEVATED_HOVER);
            g.fill(Ui.round(0, 0, w, h, 7));
        }

        Color stroke = (danger && hover) ? Color.WHITE : (hover ? Theme.TEXT : Theme.MUTED);
        g.setColor(stroke);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = w / 2;
        int cy = h / 2;
        int r = 5;

        switch (glyph) {
            case MINIMIZE -> g.drawLine(cx - r, cy, cx + r, cy);
            case CLOSE -> {
                g.drawLine(cx - r, cy - r, cx + r, cy + r);
                g.drawLine(cx + r, cy - r, cx - r, cy + r);
            }
        }

        g.dispose();
    }
}
