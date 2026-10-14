package com.vora.executor.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class FrameResizer implements AWTEventListener {

    private static final int EDGE = 6;

    private final JFrame frame;
    private int mode;
    private Point start;
    private Rectangle startBounds;

    public FrameResizer(JFrame frame) {
        this.frame = frame;
        Toolkit.getDefaultToolkit().addAWTEventListener(this,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (!(event instanceof MouseEvent me)) return;
        if (!frame.isShowing() || !frame.isActive()) return;
        if (SwingUtilities.getWindowAncestor(me.getComponent()) != frame) return;

        switch (me.getID()) {
            case MouseEvent.MOUSE_MOVED -> updateCursor(me);
            case MouseEvent.MOUSE_PRESSED -> {
                mode = edgesAt(me);
                if (mode != 0) {
                    start = me.getLocationOnScreen();
                    startBounds = frame.getBounds();
                }
            }
            case MouseEvent.MOUSE_DRAGGED -> {
                if (mode != 0) resize(me);
            }
            case MouseEvent.MOUSE_RELEASED -> mode = 0;
            default -> {
            }
        }
    }

    private int edgesAt(MouseEvent me) {
        if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) return 0;
        Point p = me.getLocationOnScreen();
        Rectangle b = frame.getBounds();
        int mode = 0;
        if (p.x - b.x <= EDGE) mode |= 4;
        if (b.x + b.width - p.x <= EDGE) mode |= 8;
        if (p.y - b.y <= EDGE) mode |= 1;
        if (b.y + b.height - p.y <= EDGE) mode |= 2;
        return mode;
    }

    private void updateCursor(MouseEvent me) {
        int mode = edgesAt(me);
        if (mode == 0) {
            frame.setCursor(Cursor.getDefaultCursor());
            return;
        }
        int type = switch (mode) {
            case 1, 2 -> Cursor.N_RESIZE_CURSOR;
            case 4, 8 -> Cursor.W_RESIZE_CURSOR;
            case 1 | 4 -> Cursor.NW_RESIZE_CURSOR;
            case 1 | 8 -> Cursor.NE_RESIZE_CURSOR;
            case 2 | 4 -> Cursor.SW_RESIZE_CURSOR;
            default -> Cursor.SE_RESIZE_CURSOR;
        };
        frame.setCursor(Cursor.getPredefinedCursor(type));
    }

    private void resize(MouseEvent me) {
        Point p = me.getLocationOnScreen();
        int dx = p.x - start.x;
        int dy = p.y - start.y;

        Rectangle r = new Rectangle(startBounds);
        int minW = Math.max(frame.getMinimumSize().width, 640);
        int minH = Math.max(frame.getMinimumSize().height, 460);

        if ((mode & 4) != 0) {
            int newX = startBounds.x + dx;
            int newW = startBounds.width - dx;
            if (newW >= minW) {
                r.x = newX;
                r.width = newW;
            }
        }
        if ((mode & 8) != 0) {
            r.width = Math.max(minW, startBounds.width + dx);
        }
        if ((mode & 1) != 0) {
            int newY = startBounds.y + dy;
            int newH = startBounds.height - dy;
            if (newH >= minH) {
                r.y = newY;
                r.height = newH;
            }
        }
        if ((mode & 2) != 0) {
            r.height = Math.max(minH, startBounds.height + dy);
        }

        frame.setBounds(r);
    }

    
    public static void enableDrag(JFrame frame, java.awt.Component handle) {
        MouseAdapter adapter = new MouseAdapter() {
            private Point origin;

            @Override
            public void mousePressed(MouseEvent e) {
                origin = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                origin = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin == null || frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) return;
                Point location = e.getLocationOnScreen();
                frame.setLocation(location.x - origin.x, location.y - origin.y);
            }
        };
        handle.addMouseListener(adapter);
        handle.addMouseMotionListener(adapter);
    }
}
