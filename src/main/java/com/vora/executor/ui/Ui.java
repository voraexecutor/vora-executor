package com.vora.executor.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;


public final class Ui {

    private Ui() {
    }

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Shape round(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r, r);
    }

    public static void fillRound(Graphics2D g, double x, double y, double w, double h, double r,
                                 java.awt.Color color) {
        g.setColor(color);
        g.fill(round(x, y, w, h, r));
    }
}
