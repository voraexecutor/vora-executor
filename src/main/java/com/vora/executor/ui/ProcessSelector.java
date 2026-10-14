package com.vora.executor.ui;

import com.vora.agent.b;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class ProcessSelector extends GButton {

    public record Proc(String id, String name, boolean minecraft) {
        @Override
        public String toString() {
            return id + "   " + name;
        }
    }

    private static final long OWN_PID = ownPid();

    private final List<Proc> processes = new ArrayList<>();
    private Proc selected;
    private Consumer<Proc> onSelect;
    private Runnable onRefresh;
    private boolean autoAttach = true;

    public ProcessSelector() {
        super("Detecting processes...", Style.GHOST);
        setHorizontalAlignment(LEFT);
        radius(9);
        addActionListener(e -> showPopup());
        refresh();
    }

    public Proc getSelected() {
        return selected;
    }

    public void setOnSelect(Consumer<Proc> onSelect) {
        this.onSelect = onSelect;
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    public boolean isAutoAttach() {
        return autoAttach;
    }

    public void setAutoAttach(boolean value) {
        this.autoAttach = value;
        if (value) {
            Proc mc = minecraft();
            if (mc != null) {
                selected = mc;
                if (onSelect != null) onSelect.accept(selected);
            }
        }
        updateText();
    }

    private Proc minecraft() {
        return processes.stream().filter(Proc::minecraft).findFirst().orElse(null);
    }

    public void refresh() {
        processes.clear();
        for (String[] entry : b.listProcesses()) {
            String id = entry[0];
            String name = entry[1] == null ? "" : entry[1];
            if (isSelf(id, name)) continue;
            processes.add(new Proc(id, name, isMinecraft(name)));
        }
        processes.sort((a, c) -> {
            if (a.minecraft() != c.minecraft()) return a.minecraft() ? -1 : 1;
            return Long.compare(asLong(a.id()), asLong(c.id()));
        });

        Proc mc = minecraft();
        boolean stillExists = selected != null
                && processes.stream().anyMatch(p -> p.id().equals(selected.id()));
        if (autoAttach && mc != null) {
            selected = mc;
        } else if (!stillExists) {
            selected = mc != null ? mc : (processes.isEmpty() ? null : processes.get(0));
        }
        updateText();
        if (selected != null && onSelect != null) onSelect.accept(selected);
    }

    private static long ownPid() {
        try {
            return ProcessHandle.current().pid();
        } catch (Throwable t) {
            return -1L;
        }
    }

    private static long asLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean isSelf(String id, String name) {
        if (OWN_PID > 0 && String.valueOf(OWN_PID).equals(id)) return true;
        String lower = name == null ? "" : name.toLowerCase();
        return lower.contains("com.vora.executor") || lower.contains("vora.executor");
    }

    private static boolean isMinecraft(String name) {
        String lower = name == null ? "" : name.toLowerCase();
        return lower.contains("minecraft")
                || lower.contains("net.fabricmc")
                || lower.contains("launchwrapper")
                || lower.contains("cpw.mods")
                || lower.contains("gradlew");
    }

    private void updateText() {
        if (selected == null) {
            setText("No Java process found");
            setForeground(Theme.MUTED);
        } else {
            String name = shorten(selected.name(), 38);
            setText(name + "      " + selected.id());
            setForeground(selected.minecraft() ? Theme.TEXT : Theme.MUTED);
        }
        setToolTipText(selected == null ? "No process found" : selected.toString());
    }

    private void showPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        menu.setBackground(Theme.ELEVATED);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.ELEVATED);
        panel.setPreferredSize(new Dimension(460, 360));

        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        GButton refresh = new GButton("Refresh", GButton.Style.SUBTLE);
        refresh.setBorder(new EmptyBorder(5, 12, 5, 12));
        refresh.setFont(Theme.ui(Font.PLAIN, 11));
        refresh.addActionListener(e -> refresh());

        JLabel title = new JLabel("Select a process");
        title.setFont(Theme.ui(Font.BOLD, 13));
        title.setForeground(Theme.TEXT);

        JLabel count = new JLabel();
        count.setFont(Theme.ui(Font.PLAIN, 10));
        count.setForeground(Theme.FAINT);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        title.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        count.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(count);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(12, 16, 12, 12));
        header.add(titleBox, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);

        JPanel accent = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, getWidth(), 0, Theme.ACCENT_2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        accent.setPreferredSize(new Dimension(0, 2));

        JPanel divider = new JPanel();
        divider.setBackground(Theme.BORDER_SOFT);
        divider.setPreferredSize(new Dimension(0, 1));

        JPanel headerArea = new JPanel();
        headerArea.setOpaque(false);
        headerArea.setLayout(new BoxLayout(headerArea, BoxLayout.Y_AXIS));
        headerArea.add(accent);
        headerArea.add(header);
        headerArea.add(divider);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(headerArea, BorderLayout.NORTH);

        Runnable rebuild = () -> {
            listPanel.removeAll();
            if (processes.isEmpty()) {
                JLabel empty = new JLabel("No Java processes found.");
                empty.setFont(Theme.ui(Font.PLAIN, 12));
                empty.setForeground(Theme.FAINT);
                empty.setBorder(new EmptyBorder(18, 16, 18, 16));
                empty.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                listPanel.add(empty);
            } else {
                for (Proc proc : processes) {
                    listPanel.add(row(proc, () -> menu.setVisible(false)));
                    listPanel.add(Box.createVerticalStrut(3));
                }
            }
            listPanel.add(Box.createVerticalGlue());
            listPanel.revalidate();
            listPanel.repaint();
            count.setText(processes.size() + (processes.size() == 1
                    ? " running process" : " running processes"));
        };

        rebuild.run();

        JScrollPane scroll = new JScrollPane(listPanel);
        ScrollBar.apply(scroll);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.ELEVATED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        menu.add(panel);
        menu.show(this, 0, getHeight() + 4);
    }

    private JComponent row(Proc proc, Runnable onPick) {
        boolean selectedRow = proc.equals(selected);
        boolean[] hover = { false };

        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Ui.aa(g);
                if (selectedRow || hover[0]) {
                    g.setColor(selectedRow ? Theme.ACCENT_SOFT : Theme.ELEVATED_HOVER);
                    g.fill(Ui.round(4, 3, getWidth() - 8, getHeight() - 6, 10));
                }
                if (selectedRow) {
                    g.setColor(Theme.ACCENT);
                    g.fill(Ui.round(4, 3, 3, getHeight() - 6, 3));
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(7, 8, 7, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JComponent badge = proc.minecraft() ? mcBadge() : spacer(34, 18);

        JLabel name = new JLabel(shorten(proc.name(), 40));
        name.setFont(Theme.ui(proc.minecraft() || selectedRow ? Font.BOLD : Font.PLAIN, 12));
        name.setForeground(proc.minecraft() || selectedRow ? Theme.TEXT : Theme.MUTED);

        JLabel pid = new JLabel(proc.id());
        pid.setFont(Theme.mono(Font.PLAIN, 11));
        pid.setForeground(selectedRow ? Theme.ACCENT : Theme.FAINT);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(pid);

        row.add(badge, BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(east, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover[0] = true;
                row.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover[0] = false;
                row.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selected = proc;
                updateText();
                if (onSelect != null) onSelect.accept(proc);
                if (onPick != null) onPick.run();
            }
        });
        return row;
    }

    private static JComponent mcBadge() {
        JLabel badge = new JLabel("MC") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                g2.setColor(Theme.ACCENT_SOFT);
                g2.fill(Ui.round(0, 0, getWidth(), getHeight(), 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setFont(Theme.ui(Font.BOLD, 9));
        badge.setForeground(Theme.ACCENT);
        badge.setHorizontalAlignment(JLabel.CENTER);
        badge.setPreferredSize(new Dimension(34, 18));
        return badge;
    }

    private static JComponent spacer(int width, int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(width, height));
        return spacer;
    }

    private static String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}