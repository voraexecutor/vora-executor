package com.vora.executor.ui;

import com.vora.executor.ScriptHistory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;

public class ScriptsPanel extends RoundedPanel {

    private final ScriptHistory history;
    private final Consumer<ScriptHistory.Entry> onOpen;
    private final JPanel list = new JPanel();

    public ScriptsPanel(ScriptHistory history, Consumer<ScriptHistory.Entry> onOpen) {
        super(Theme.CODE_BG, 12);
        border(Theme.BORDER_SOFT);
        this.history = history;
        this.onOpen = onOpen;
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        refresh();
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT));

        JLabel title = new JLabel("SCRIPTS");
        title.setFont(Theme.ui(Font.BOLD, 11));
        title.setForeground(Theme.MUTED);
        title.setBorder(new EmptyBorder(9, 14, 9, 14));
        header.add(title, BorderLayout.WEST);

        GButton clear = new GButton("Clear history", GButton.Style.SUBTLE);
        clear.setBorder(new EmptyBorder(7, 14, 7, 14));
        clear.addActionListener(e -> {
            history.clear();
            refresh();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        right.setOpaque(false);
        right.add(clear);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent body() {
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(10, 12, 12, 12));
        wrap.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        ScrollBar.apply(scroll);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CODE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public void refresh() {
        list.removeAll();
        List<ScriptHistory.Entry> favorites = history.favorites();
        List<ScriptHistory.Entry> others = history.others();

        if (favorites.isEmpty() && others.isEmpty()) {
            JLabel empty = new JLabel("run something.");
            empty.setFont(Theme.ui(Font.PLAIN, 13));
            empty.setForeground(Theme.FAINT);
            empty.setBorder(new EmptyBorder(8, 4, 8, 4));
            list.add(empty);
        } else {
            if (!favorites.isEmpty()) {
                list.add(sectionLabel("FAVORITES"));
                for (ScriptHistory.Entry entry : favorites) {
                    list.add(row(entry));
                    list.add(Box.createVerticalStrut(8));
                }
                if (!others.isEmpty()) list.add(Box.createVerticalStrut(6));
            }
            if (!others.isEmpty()) {
                list.add(sectionLabel(favorites.isEmpty() ? "ALL SCRIPTS" : "OTHER SCRIPTS"));
                for (ScriptHistory.Entry entry : others) {
                    list.add(row(entry));
                    list.add(Box.createVerticalStrut(8));
                }
            }
        }
        list.revalidate();
        list.repaint();
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.ui(Font.BOLD, 10));
        label.setForeground(Theme.FAINT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(2, 2, 8, 0));
        return label;
    }

    private static String modeLabel(ScriptHistory.Entry entry) {
        return "mixin".equals(entry.mode) ? "mixin" : "code";
    }

    private JComponent row(ScriptHistory.Entry entry) {
        RoundedPanel card = new RoundedPanel(Theme.ELEVATED, 10);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean favorite = history.isFavorite(entry);
        StarButton star = new StarButton(favorite, value -> {
            history.setFavorite(entry, value);
            refresh();
        });
        card.add(star, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(entry.name);
        name.setFont(Theme.ui(Font.BOLD, 13));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel meta = new JLabel(entry.timeText() + "   -   " + modeLabel(entry) + "   -   " + entry.id);
        meta.setFont(Theme.ui(Font.PLAIN, 11));
        meta.setForeground(Theme.FAINT);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(name);
        text.add(Box.createVerticalStrut(3));
        text.add(meta);
        card.add(text, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        GButton open = new GButton("Open", GButton.Style.GHOST);
        open.setBorder(new EmptyBorder(6, 12, 6, 12));
        open.addActionListener(e -> onOpen.accept(entry));

        GButton rename = new GButton("Rename", GButton.Style.GHOST);
        rename.setBorder(new EmptyBorder(6, 12, 6, 12));
        rename.addActionListener(e -> {
            String preview = entry.name.length() > 48
                    ? entry.name.substring(0, 45) + "..."
                    : entry.name;
            String next = PromptDialog.ask(
                    SwingUtilities.getWindowAncestor(card),
                    "Rename script",
                    "New name for \"" + preview + "\":",
                    entry.name);
            if (next != null && !next.equals(entry.name)) {
                history.rename(entry, next);
                refresh();
            }
        });

        GButton remove = new GButton("Remove", GButton.Style.SUBTLE);
        remove.danger();
        remove.setBorder(new EmptyBorder(6, 12, 6, 12));
        remove.addActionListener(e -> {
            if (history.isFavorite(entry)) {
                boolean confirmed = ConfirmDialog.ask(
                        SwingUtilities.getWindowAncestor(card),
                        "Remove favorite script",
                        "Are you sure you want to remove \"" + entry.name + "\"?\nIt is marked as a favorite.");
                if (!confirmed) return;
            }
            history.remove(entry);
            refresh();
        });

        actions.add(open);
        actions.add(rename);
        actions.add(remove);
        card.add(actions, BorderLayout.EAST);
        return card;
    }
}
