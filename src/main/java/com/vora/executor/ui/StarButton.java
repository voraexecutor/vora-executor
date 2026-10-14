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
import java.util.function.Consumer;


public class StarButton extends JComponent {

    private boolean on;
    private boolean hover;
    private final Consumer<Boolean> onToggle;

    public StarButton(boolean on, Consumer<Boolean> onToggle) {
        this.on = on;
        this.onToggle = onToggle;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(on ? "Remove from favorites" : "Add to favorites");
        setPreferredSize(new Dimension(30, 30));
        setMaximumSize(new Dimension(30, 30));
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
                StarButton.this.on = !StarButton.this.on;
                setToolTipText(StarButton.this.on ? "Remove from favorites" : "Add to favorites");
                repaint();
                if (StarButton.this.onToggle != null) {
                    StarButton.this.onToggle.accept(StarButton.this.on);
                }
            }
        });
    }

    public boolean isOn() {
        return on;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;
        int outer = 8;
        int inner = 4;

        java.awt.geom.Path2D star = new java.awt.geom.Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double radius = (i % 2 == 0) ? outer : inner;
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + Math.cos(angle) * radius;
            double y = cy + Math.sin(angle) * radius;
            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.closePath();

        if (on) {
            g.setColor(Theme.AMBER);
            g.fill(star);
        } else {
            if (hover) {
                g.setColor(Theme.ELEVATED_HOVER);
                g.fill(Ui.round(0, 0, w, h, 7));
            }
            g.setColor(hover ? Theme.TEXT : Theme.FAINT);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(star);
        }

        g.dispose();
    }
}
