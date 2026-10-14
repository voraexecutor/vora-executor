package com.vora.executor.ui;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;


public class LineNumber extends JComponent {

    private final JTextArea area;
    private final Font font;

    public LineNumber(JTextArea area) {
        this.area = area;
        this.font = Theme.mono(Font.PLAIN, 12);
        setFont(font);
        setOpaque(true);
        setBackground(Theme.GUTTER_BG);

        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });

        area.addCaretListener(e -> repaint());
    }

    private void refresh() {
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private int currentLine() {
        try {
            return area.getLineOfOffset(area.getCaretPosition()) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int lines = Math.max(area.getLineCount(), 1);
        int digits = Math.max(2, String.valueOf(lines).length());
        FontMetrics fm = getFontMetrics(font);
        int width = fm.charWidth('0') * digits + 22;
        return new Dimension(width, area.getHeight());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Ui.aa(g);

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.BORDER_SOFT);
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int active = currentLine();

        String text = area.getText();
        int length = text.length();
        int start = 0;
        int line = 1;

        while (start <= length) {
            int end = text.indexOf('\n', start);
            if (end < 0) end = length;

            try {
                Rectangle2D rect = area.modelToView2D(start);
                if (rect != null) {
                    int baseline = (int) rect.getY() + fm.getAscent();
                    String number = String.valueOf(line);
                    int x = getWidth() - 11 - fm.stringWidth(number);
                    g.setColor(line == active ? Theme.GUTTER_ACTIVE : Theme.GUTTER_TEXT);
                    g.drawString(number, x, baseline);
                }
            } catch (Exception ignored) {
                
            }

            if (end == length) break;
            start = end + 1;
            line++;
        }

        g.dispose();
    }
}
