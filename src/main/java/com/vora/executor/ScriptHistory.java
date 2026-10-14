package com.vora.executor;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public final class ScriptHistory {

    
    public static final class Entry {
        public final String id;
        public final long time;
        public final String name;
        public final String code;
        public final String mode;

        Entry(String id, long time, String name, String code) {
            this(id, time, name, code, "code");
        }

        Entry(String id, long time, String name, String code, String mode) {
            this.id = id;
            this.time = time;
            this.name = name;
            this.code = code;
            this.mode = "mixin".equals(mode) ? "mixin" : "code";
        }

        public String timeText() {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(time));
        }
    }

    private final Path dir;
    private final Path favoritesFile;
    private final List<Entry> entries = new ArrayList<>();
    private final Set<String> favorites = new LinkedHashSet<>();

    public ScriptHistory() {
        this(Path.of(System.getProperty("user.home"), ".vora", "scripts"));
        load();
    }

    ScriptHistory(Path dir) {
        this.dir = dir;
        this.favoritesFile = dir.resolveSibling("favorites.txt");
    }

    private void load() {
        try {
            Files.createDirectories(dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.vora")) {
                for (Path file : stream) {
                    try {
                        String id = file.getFileName().toString().replace(".vora", "");
                        long time = Long.parseLong(id.split("-")[0]);
                        String code = Files.readString(file, StandardCharsets.UTF_8);
                        String mode = modeFor(dir, id);
                        entries.add(new Entry(id, time, nameOrDefault(dir, id, code), code, mode));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        loadFavorites();
        sort();
    }

    private void loadFavorites() {
        try {
            if (!Files.exists(favoritesFile)) return;
            for (String line : Files.readAllLines(favoritesFile, StandardCharsets.UTF_8)) {
                String id = line.trim();
                if (!id.isEmpty()) favorites.add(id);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveFavorites() {
        try {
            if (favoritesFile.getParent() != null) Files.createDirectories(favoritesFile.getParent());
            Files.write(favoritesFile, favorites, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static String modeFor(Path dir, String id) {
        try {
            Path file = dir.resolve(id + ".mode");
            if (Files.isRegularFile(file)) {
                String text = Files.readString(file, StandardCharsets.UTF_8).trim();
                if ("mixin".equalsIgnoreCase(text)) return "mixin";
            }
        } catch (Exception ignored) {
        }
        return "code";
    }

    private static String nameOrDefault(Path dir, String id, String code) {
        try {
            Path file = dir.resolve(id + ".name");
            if (Files.isRegularFile(file)) {
                String text = Files.readString(file, StandardCharsets.UTF_8).trim();
                if (!text.isEmpty()) return text;
            }
        } catch (Exception ignored) {
        }
        return nameFor(code);
    }






    public synchronized Entry rename(Entry entry, String newName) {
        if (entry == null) return entry;
        String cleaned = newName == null ? "" : newName.trim();
        if (cleaned.isEmpty() || cleaned.equals(entry.name)) return entry;
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(entry.id + ".name"), cleaned, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        Entry updated = new Entry(entry.id, entry.time, cleaned, entry.code, entry.mode);
        entries.removeIf(e -> e.id.equals(entry.id));
        entries.add(updated);
        sort();
        return updated;
    }

    public synchronized Entry add(String id, String code) {
        return add(id, code, "code");
    }

    public synchronized Entry add(String id, String code, String mode) {
        long time;
        try {
            time = Long.parseLong(id.split("-")[0]);
        } catch (Exception e) {
            time = System.currentTimeMillis();
        }
        Entry entry = new Entry(id, time, nameOrDefault(dir, id, code), code, mode);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".vora"), code, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(id + ".mode"), entry.mode, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        entries.removeIf(e -> e.id.equals(id));
        entries.add(entry);
        sort();
        return entry;
    }

    public synchronized void remove(Entry entry) {
        if (entry == null) return;
        entries.removeIf(e -> e.id.equals(entry.id));
        if (favorites.remove(entry.id)) saveFavorites();
        try {
            Files.deleteIfExists(dir.resolve(entry.id + ".vora"));
            Files.deleteIfExists(dir.resolve(entry.id + ".mode"));
            Files.deleteIfExists(dir.resolve(entry.id + ".name"));
        } catch (Exception ignored) {
        }
    }

    public synchronized void clear() {

        List<Entry> kept = new ArrayList<>();
        for (Entry entry : entries) {
            if (favorites.contains(entry.id)) kept.add(entry);
        }
        entries.clear();
        entries.addAll(kept);
        sort();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.vora")) {
            for (Path file : stream) {
                String id = file.getFileName().toString().replace(".vora", "");
                if (favorites.contains(id)) continue;
                try {
                    Files.deleteIfExists(file);
                    Files.deleteIfExists(dir.resolve(id + ".mode"));
                    Files.deleteIfExists(dir.resolve(id + ".name"));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized boolean isFavorite(Entry entry) {
        return entry != null && favorites.contains(entry.id);
    }

    public synchronized void setFavorite(Entry entry, boolean favorite) {
        if (entry == null) return;
        if (favorite) {
            favorites.add(entry.id);
        } else {
            favorites.remove(entry.id);
        }
        saveFavorites();
    }

    public synchronized boolean toggleFavorite(Entry entry) {
        if (entry == null) return false;
        boolean next = !favorites.contains(entry.id);
        setFavorite(entry, next);
        return next;
    }

    public synchronized List<Entry> favorites() {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (favorites.contains(entry.id)) result.add(entry);
        }
        return result;
    }

    public synchronized List<Entry> others() {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (!favorites.contains(entry.id)) result.add(entry);
        }
        return result;
    }

    public synchronized List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    private void sort() {
        entries.sort(Comparator.comparingLong((Entry e) -> e.time).reversed());
    }

    
    static String nameFor(String code) {
        for (String line : code.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;
            if (trimmed.length() > 52) trimmed = trimmed.substring(0, 49) + "...";
            return trimmed;
        }
        return "Untitled script";
    }
}
