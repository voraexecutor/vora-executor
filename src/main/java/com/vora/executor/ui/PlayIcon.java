package com.vora.executor.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;


public class PlayIcon implements Icon {

    private final int height;
    private final Color color;

    public PlayIcon(int height, Color color) {
        this.height = height;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return (int) Math.round(height * 0.82);
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);
        g.setColor(color);

        double w = getIconWidth();
        double h = height;
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(x, y);
        triangle.lineTo(x + w, y + h / 2.0);
        triangle.lineTo(x, y + h);
        triangle.closePath();

        g.fill(triangle);
        g.dispose();
    }
}
