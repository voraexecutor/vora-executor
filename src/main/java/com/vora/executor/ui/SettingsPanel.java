package com.vora.executor.ui;

import com.vora.executor.Settings;

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
import java.util.function.Consumer;


public class SettingsPanel extends RoundedPanel {

    private final Settings settings;
    private final Consumer<String> onChange;

    public SettingsPanel(Settings settings, Consumer<String> onChange) {
        super(Theme.CODE_BG, 12);
        border(Theme.BORDER_SOFT);
        this.settings = settings;
        this.onChange = onChange;
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT));

        JLabel title = new JLabel("SETTINGS");
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

        content.add(sectionLabel("APPEARANCE"));
        content.add(themeCard());
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("CONNECTION"));
        content.add(toggleCard("Auto attach",
                "Automatically find and select the running Minecraft process.",
                settings.autoAttach(), settings::setAutoAttach, "autoAttach"));
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("NOTIFICATIONS & SOUNDS"));
        content.add(toggleCard("Notifications",
                "Show a toast when a script finishes and when Minecraft is attached.",
                settings.notifications(), settings::setNotifications, "notifications"));
        content.add(Box.createVerticalStrut(8));
        content.add(toggleCard("Sound effects",
                "Play short sounds when switching panels and executing code.",
                settings.soundEffects(), settings::setSoundEffects, "soundEffects"));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 16, 16, 16));
        wrap.add(content, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        ScrollBar.apply(scroll);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CODE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.ui(Font.BOLD, 10));
        label.setForeground(Theme.FAINT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 2, 8, 0));
        return label;
    }

    private JComponent themeCard() {
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        control.setOpaque(false);

        GButton dark = segmentButton("Dark", settings.theme() == Theme.Mode.DARK);
        GButton light = segmentButton("Light", settings.theme() == Theme.Mode.LIGHT);

        dark.addActionListener(e -> {
            settings.setTheme(Theme.Mode.DARK);
            notify("theme");
        });
        light.addActionListener(e -> {
            settings.setTheme(Theme.Mode.LIGHT);
            notify("theme");
        });

        control.add(dark);
        control.add(light);
        return card("Theme", "Switch between the dark and light appearance.", control);
    }

    private GButton segmentButton(String text, boolean active) {
        GButton button = new GButton(text, active ? GButton.Style.BLACK : GButton.Style.GHOST);
        Dimension size = new Dimension(78, 30);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.radius(8);
        return button;
    }

    private JComponent toggleCard(String title, String description, boolean value,
                                  Consumer<Boolean> setter, String key) {
        ToggleSwitch toggle = new ToggleSwitch(value, v -> {
            setter.accept(v);
            notify(key);
        });

        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        control.setOpaque(false);
        control.add(toggle);
        return card(title, description, control);
    }

    private void notify(String key) {
        if (onChange != null) onChange.accept(key);
    }

    private JComponent card(String title, String description, JComponent control) {
        RoundedPanel card = new RoundedPanel(Theme.ELEVATED, 10);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(title);
        name.setFont(Theme.ui(Font.BOLD, 13));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel(description);
        desc.setFont(Theme.ui(Font.PLAIN, 11));
        desc.setForeground(Theme.MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(name);
        text.add(Box.createVerticalStrut(3));
        text.add(desc);

        card.add(text, BorderLayout.CENTER);
        card.add(control, BorderLayout.EAST);
        return card;
    }
}
