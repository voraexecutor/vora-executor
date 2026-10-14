package com.vora.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class d {

    public static void premain(String args, Instrumentation instrumentation) {
        handle(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        handle(args, instrumentation);
    }

    private static void handle(String args, Instrumentation instrumentation) {
        Map<String, String> options = parse(args);

        Bridge.connect("127.0.0.1", parseInt(options.get("port"), 0));

        try {
            Bridge.log("[VORA] build " + System.getProperty("vora.agent.build", "unknown"));

            String mode = options.getOrDefault("mode", "code");
            if ("mixin".equalsIgnoreCase(mode)) {
                Hook.healNow(instrumentation, Bridge::log);
            }

            String code = resolveCode(options);
            if (code == null) {
                Bridge.log("[VORA] no code supplied");
                return;
            }
            Bridge.log("[VORA] executing script (" + code.length() + " chars) as " + mode);
            if ("mixin".equalsIgnoreCase(mode)) {
                c.executeMixin(options.get("id"), code, instrumentation);
            } else {
                c.execute(options.get("id"), code, instrumentation);
            }
        } catch (Throwable t) {
            Bridge.error(t);
        } finally {
            Bridge.log("[VORA] finished");
            Bridge.close();
        }
    }

    private static void addUrlToGameLoader(ClassLoader loader, java.net.URL url) throws Exception {
        Class<?> type = loader.getClass();
        try {

            java.lang.reflect.Method addUrlFwd = type.getMethod("addUrlFwd", java.net.URL.class);
            addUrlFwd.setAccessible(true);
            addUrlFwd.invoke(loader, url);
            return;
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("addUrlFwd failed", e);
        }
        try {

            Object delegate = type.getMethod("getDelegate").invoke(loader);
            java.lang.reflect.Method addCodeSource = delegate.getClass().getMethod("addCodeSource", java.nio.file.Path.class);
            addCodeSource.setAccessible(true);
            addCodeSource.invoke(delegate, java.nio.file.Path.of(url.toURI()));
            return;
        } catch (ReflectiveOperationException | java.net.URISyntaxException ignored) {
        }

        if (loader instanceof java.net.URLClassLoader) {
            java.lang.reflect.Method addURL = java.net.URLClassLoader.class.getDeclaredMethod("addURL", java.net.URL.class);
            addURL.setAccessible(true);
            addURL.invoke(loader, url);
        }
    }

    private static String resolveCode(Map<String, String> options) throws Exception {
        String codeFile = options.get("codefile");
        if (codeFile != null && !codeFile.isEmpty()) {
            String path = new String(Base64.getUrlDecoder().decode(codeFile), StandardCharsets.UTF_8);
            return java.nio.file.Files.readString(java.nio.file.Path.of(path), StandardCharsets.UTF_8);
        }
        String encoded = options.get("code");
        if (encoded != null && !encoded.isEmpty()) {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        }
        return null;
    }

    static Map<String, String> parse(String args) {
        Map<String, String> options = new HashMap<>();
        if (args == null) return options;
        for (String part : args.split(";")) {
            if (part.isEmpty()) continue;
            int equals = part.indexOf('=');
            if (equals < 0) {
                options.put(part, "");
            } else {
                options.put(part.substring(0, equals), part.substring(equals + 1));
            }
        }
        return options;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}