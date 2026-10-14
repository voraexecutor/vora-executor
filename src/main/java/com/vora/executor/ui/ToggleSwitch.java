package com.vora.executor.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;


public class ToggleSwitch extends JComponent {

    private boolean on;
    private final Consumer<Boolean> onChange;

    public ToggleSwitch(boolean on, Consumer<Boolean> onChange) {
        this.on = on;
        this.onChange = onChange;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(46, 26));
        setMaximumSize(new Dimension(46, 26));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ToggleSwitch.this.on = !ToggleSwitch.this.on;
                repaint();
                if (ToggleSwitch.this.onChange != null) {
                    ToggleSwitch.this.onChange.accept(ToggleSwitch.this.on);
                }
            }
        });
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean value) {
        this.on = value;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);

        int w = getWidth();
        int h = getHeight();

        g.setColor(on ? Theme.BUTTON : Theme.ELEVATED_HOVER);
        g.fill(Ui.round(0, 0, w, h, h));
        if (!on) {
            g.setColor(Theme.BORDER);
            g.draw(Ui.round(0.5, 0.5, w - 1, h - 1, h));
        }

        int d = h - 6;
        int x = on ? w - d - 3 : 3;
        g.setColor(on ? Theme.BUTTON_TEXT : Color.WHITE);
        g.fillOval(x, 3, d, d);
        g.setColor(new Color(0, 0, 0, 40));
        g.drawOval(x, 3, d, d);

        g.dispose();
    }
}
