package com.vora.executor.ui;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;


public final class Scrollhelper {

    private Scrollhelper() {
    }

    
    public static void enableWheel(JScrollPane pane) {
        pane.setWheelScrollingEnabled(true);

        JViewport viewport = pane.getViewport();
        viewport.addMouseWheelListener(e -> {
            JScrollBar bar = e.isShiftDown() && pane.getHorizontalScrollBar().isVisible()
                    ? pane.getHorizontalScrollBar()
                    : pane.getVerticalScrollBar();
            if (!bar.isVisible()) return;

            int units = e.getUnitsToScroll();
            if (units == 0) return;

            int step = Math.max(1, bar.getUnitIncrement(units > 0 ? 1 : -1)) * Math.abs(units);
            setClamped(bar, bar.getValue() + (units > 0 ? step : -step));
            e.consume();
        });
    }

    
    public static void enableMiddleDrag(JFrame frame) {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            private JScrollPane target;
            private Point anchor;
            private int startVertical;
            private int startHorizontal;

            @Override
            public void eventDispatched(AWTEvent event) {
                if (!(event instanceof MouseEvent me)) return;
                if (!frame.isShowing() || !frame.isActive()) return;
                if (SwingUtilities.getWindowAncestor(me.getComponent()) != frame) return;

                switch (me.getID()) {
                    case MouseEvent.MOUSE_PRESSED -> {
                        if (!SwingUtilities.isMiddleMouseButton(me)) return;
                        JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(
                                JScrollPane.class, me.getComponent());
                        if (pane == null) return;
                        target = pane;
                        anchor = me.getLocationOnScreen();
                        startVertical = pane.getVerticalScrollBar().getValue();
                        startHorizontal = pane.getHorizontalScrollBar().getValue();
                        me.consume();
                    }
                    case MouseEvent.MOUSE_DRAGGED -> {
                        if (target == null || anchor == null) return;
                        Point point = me.getLocationOnScreen();
                        setClamped(target.getVerticalScrollBar(), startVertical + (anchor.y - point.y));
                        setClamped(target.getHorizontalScrollBar(), startHorizontal + (anchor.x - point.x));
                        me.consume();
                    }
                    case MouseEvent.MOUSE_RELEASED -> {
                        if (target == null || !SwingUtilities.isMiddleMouseButton(me)) return;
                        target = null;
                        anchor = null;
                        me.consume();
                    }
                    default -> {
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private static void setClamped(JScrollBar bar, int value) {
        int max = bar.getMaximum() - bar.getVisibleAmount();
        bar.setValue(Math.max(bar.getMinimum(), Math.min(max, value)));
    }
}
