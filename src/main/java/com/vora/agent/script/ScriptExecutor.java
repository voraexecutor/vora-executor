package com.vora.agent.script;

import com.vora.agent.Bridge;
import com.vora.agent.Fabric;
import com.vora.agent.mapping.GameMappings;

import java.lang.instrument.Instrumentation;
import java.util.function.Consumer;




public final class ScriptExecutor {

    private final Consumer<String> log;
    private final Consumer<Throwable> error;

    public ScriptExecutor(Consumer<String> log, Consumer<Throwable> error) {
        this.log = log;
        this.error = error;
    }

    public void execute(String id, String userCode, Instrumentation instrumentation) throws Exception {
        ClassLoader gameLoader = findGameLoader(instrumentation);
        if (gameLoader == null) {
            throw new IllegalStateException(
                    "Could not find the Minecraft class loader. Is the game actually running with Fabric/Forge?");
        }
        log.accept("[VORA] game class loader: " + gameLoader.getClass().getName());

        String classpath;
        GameMappings.Context context = null;
        if (canLoad(gameLoader, ScriptClassConstants.MC_CLIENT)) {
            log.accept("[VORA] game namespace: named (dev environment)");
            classpath = buildRuntimeClasspath(gameLoader);
        } else if (canLoad(gameLoader, ScriptClassConstants.MC_CLIENT_INTERMEDIARY)) {
            log.accept("[VORA] game namespace: intermediary (production) - preparing mappings");
            GameMappings.Context ctx = GameMappings.prepare(gameLoader);
            context = ctx;
            classpath = ScriptCompiler.addAgentJarToClasspath(ctx.namedClasspath);
        } else {
            throw new IllegalStateException(
                    "Found the game class loader but neither " + ScriptClassConstants.MC_CLIENT + " nor " + ScriptClassConstants.MC_CLIENT_INTERMEDIARY
                            + " is loadable. This client uses an unsupported namespace (vanilla/obfuscated?).");
        }

        String source = context != null
                ? ScriptBuilder.buildSource(userCode, context.namedClientClass, context.screenApi)
                : ScriptBuilder.buildSource(userCode);
        var classes = ScriptCompiler.compile(source, classpath);
        if (context != null) {
            classes = GameMappings.remapClasses(classes, context.namedToIntermediary, context.namedHierarchy);
        }
        log.accept("[VORA] compiled " + classes.size() + " class(es)");

        ScriptRunnerImpl.run(id, classes, gameLoader, this.log, this.error);
    }

    public static boolean canLoad(ClassLoader loader, String className) {
        if (loader == null) return false;
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ClassLoader findGameLoader(Instrumentation instrumentation) {
        if (instrumentation != null) {
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                String name = loaded.getName();
                if (ScriptClassConstants.MC_CLIENT.equals(name) || ScriptClassConstants.MC_CLIENT_INTERMEDIARY.equals(name)) {
                    return loaded.getClassLoader();
                }
            }
        }
        return Fabric.findClassLoader();
    }

    public static String buildRuntimeClasspath(ClassLoader gameLoader) {
        var entries = new java.util.LinkedHashSet<>(GameMappings.classpathEntries(gameLoader));
        addCodeSource(entries, ScriptExecutor.class);
        addCodeSource(entries, Bridge.class);
        ScriptCompiler.addAgentJar(entries);
        return String.join(java.io.File.pathSeparator, entries);
    }

    private static void addCodeSource(java.util.Set<String> entries, Class<?> type) {
        try {
            java.net.URL url = type.getProtectionDomain().getCodeSource().getLocation();
            if (url != null && "file".equals(url.getProtocol())) {
                entries.add(new java.io.File(url.toURI()).getAbsolutePath());
            }
        } catch (Throwable ignored) {
        }
    }
}