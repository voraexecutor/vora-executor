package com.vora.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;





public final class VoraTemp {

    private static final Path VORA_ROOT;

    static {
        try {
            VORA_ROOT = initRoot();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Path initRoot() throws IOException {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "vora");
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("mappings"));
        return root;
    }

    public static Path mappingsDir() {
        return VORA_ROOT.resolve("mappings");
    }


    public static Path root() {
        return VORA_ROOT;
    }

    public static void cleanup() {
        try {
            if (Files.exists(VORA_ROOT)) {
                Files.walk(VORA_ROOT)
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }

    private VoraTemp() {
    }
}