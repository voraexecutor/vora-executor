package com.vora.executor;

import com.vora.executor.ui.Theme;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;


public final class Settings {

    private final Path file;

    private Theme.Mode theme = Theme.Mode.DARK;
    private boolean autoAttach = true;
    private boolean notifications = true;
    private boolean soundEffects = true;

    public Settings() {
        this(Path.of(System.getProperty("user.home"), ".vora", "settings.properties"));
    }

    Settings(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        try {
            if (!Files.exists(file)) return;
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }
            theme = "LIGHT".equalsIgnoreCase(props.getProperty("theme", "DARK"))
                    ? Theme.Mode.LIGHT : Theme.Mode.DARK;
            autoAttach = Boolean.parseBoolean(props.getProperty("autoAttach", "true"));
            soundEffects = Boolean.parseBoolean(props.getProperty("soundEffects", "true"));
            notifications = Boolean.parseBoolean(props.getProperty("notifications", "true"));
        } catch (Exception ignored) {
        }
    }

    public void save() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Properties props = new Properties();
            props.setProperty("theme", theme.name());
            props.setProperty("autoAttach", Boolean.toString(autoAttach));
            props.setProperty("soundEffects", Boolean.toString(soundEffects));
            props.setProperty("notifications", Boolean.toString(notifications));
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Vora settings");
            }
        } catch (Exception ignored) {
        }
    }

    public Theme.Mode theme() {
        return theme;
    }

    public void setTheme(Theme.Mode theme) {
        this.theme = theme == null ? Theme.Mode.DARK : theme;
        save();
    }

    public boolean autoAttach() {
        return autoAttach;
    }

    public void setAutoAttach(boolean autoAttach) {
        this.autoAttach = autoAttach;
        save();
    }

    public boolean notifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
        save();
    }

    public boolean soundEffects() {
        return soundEffects;
    }

    public void setSoundEffects(boolean soundEffects) {
        this.soundEffects = soundEffects;
        save();
    }
}