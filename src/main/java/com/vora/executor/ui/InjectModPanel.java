package com.vora.executor.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public class InjectModPanel extends RoundedPanel {

    public InjectModPanel() {
        super(Theme.CODE_BG, 12);
        border(Theme.BORDER_SOFT);
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT));

        JLabel title = new JLabel("INJECT MOD");
        title.setFont(Theme.ui(Font.BOLD, 11));
        title.setForeground(Theme.MUTED);
        title.setBorder(new EmptyBorder(9, 14, 9, 14));
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JComponent body() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(60, 40, 60, 40));


        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);

        JLabel soon = new JLabel("Soon");
        soon.setFont(Theme.ui(Font.BOLD, 28));
        soon.setForeground(Theme.MUTED);

        center.add(soon);
        content.add(center);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(content, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(wrap);
        ScrollBar.apply(scroll);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CODE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
}