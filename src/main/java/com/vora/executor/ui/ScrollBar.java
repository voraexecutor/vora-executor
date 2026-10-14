package com.vora.executor.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;


public class ScrollBar extends BasicScrollBarUI {

    private static final int THICKNESS = 12;

    private static Color track() {
        return Theme.mode() == Theme.Mode.LIGHT ? new Color(0xEDEDF2) : new Color(0x0B0B0E);
    }

    private static Color thumb() {
        return Theme.mode() == Theme.Mode.LIGHT ? new Color(0xC4C4CF) : new Color(0x3A3A46);
    }

    public static void apply(JScrollPane scrollPane) {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();

        
        
        vertical.setUI(new ScrollBar());
        horizontal.setUI(new ScrollBar());

        vertical.setUnitIncrement(18);
        horizontal.setUnitIncrement(18);
        vertical.setOpaque(false);
        horizontal.setOpaque(false);

        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        Scrollhelper.enableWheel(scrollPane);
    }

    @Override
    protected void configureScrollBarColors() {
        trackColor = track();
        thumbColor = thumb();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private JButton zeroButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        Ui.aa(g2);
        g2.setColor(track());
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = trackBounds.x + 3;
            int w = Math.max(4, trackBounds.width - 6);
            g2.fill(Ui.round(x, trackBounds.y, w, trackBounds.height, w));
        } else {
            int y = trackBounds.y + 3;
            int h = Math.max(4, trackBounds.height - 6);
            g2.fill(Ui.round(trackBounds.x, y, trackBounds.width, h, h));
        }
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        Ui.aa(g2);
        g2.setColor(isThumbRollover() || isDragging ? Theme.ACCENT : thumb());

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = thumbBounds.x + 3;
            int w = Math.max(4, thumbBounds.width - 6);
            g2.fill(Ui.round(x, thumbBounds.y + 1, w, Math.max(8, thumbBounds.height - 2), w));
        } else {
            int y = thumbBounds.y + 3;
            int h = Math.max(4, thumbBounds.height - 6);
            g2.fill(Ui.round(thumbBounds.x + 1, y, Math.max(8, thumbBounds.width - 2), h, h));
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(THICKNESS, THICKNESS);
    }
}
