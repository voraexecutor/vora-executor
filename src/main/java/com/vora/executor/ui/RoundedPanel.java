package com.vora.executor.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;


public class RoundedPanel extends JPanel {

    private final Color fill;
    private int radius;
    private Color border;
    private boolean showBorder = true;

    public RoundedPanel(Color fill, int radius) {
        this.fill = fill;
        this.radius = radius;
        setOpaque(false);
    }

    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    public RoundedPanel border(Color color) {
        this.border = color;
        return this;
    }

    public RoundedPanel noBorder() {
        this.showBorder = false;
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);
        if (fill != null) {
            g.setColor(fill);
            g.fill(Ui.round(0, 0, getWidth(), getHeight(), radius));
        }
        if (showBorder && border != null) {
            g.setColor(border);
            g.draw(Ui.round(0.5, 0.5, getWidth() - 1, getHeight() - 1, radius));
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
