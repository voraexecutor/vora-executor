package com.vora.agent.script;

import com.vora.agent.Bridge;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScriptCompiler {

    private ScriptCompiler() {
    }

    public static Map<String, byte[]> compile(String source, String classpath) {
        JavaCompiler system = ToolProvider.getSystemJavaCompiler();
        if (system != null) {
            return compileWith(system, false, source, classpath);
        }

        Map<String, byte[]> viaEcj = compileWithEcj(source, classpath);
        if (viaEcj != null) return viaEcj;

        JavaCompiler external = externalCompiler();
        if (external != null) {
            return compileWith(external, true, source, classpath);
        }
        throw new IllegalStateException(
                "No Java compiler is available. The bundled compiler could not be loaded either.");
    }

    static String pseudoName(String source) {
        String simple = topLevelClassName(source);
        return ScriptClassConstants.SCRIPT_CLASS.substring(0, ScriptClassConstants.SCRIPT_CLASS.lastIndexOf('.')) + "." + simple;
    }

    static String topLevelClassName(String source) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "(?m)^\\s*public\\s+(?:final\\s+)?class\\s+([A-Za-z_$][A-Za-z0-9_$]*)").matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return ScriptClassConstants.SCRIPT_CLASS.substring(ScriptClassConstants.SCRIPT_CLASS.lastIndexOf('.') + 1);
    }

    static Map<String, byte[]> compileWithEcj(String source, String classpath) {
        final Class<?> batch;
        final Class<?> progress;
        try {
            batch = Class.forName("org.eclipse.jdt.core.compiler.batch.BatchCompiler");
            progress = Class.forName("org.eclipse.jdt.core.compiler.CompilationProgress");
        } catch (Throwable missing) {
            return null;
        }

        Path work = null;
        try {
            work = Files.createTempDirectory("vora-ecj");
            Path srcDir = work.resolve("src");
            Files.createDirectories(srcDir);

            Path scriptFile = srcDir.resolve(pseudoName(source).replace('.', '/') + ".java");
            Files.createDirectories(scriptFile.getParent());
            Files.writeString(scriptFile, source, StandardCharsets.UTF_8);

            Path outputDir = work.resolve("out");
            Files.createDirectories(outputDir);

            int feature = Runtime.version().feature();
            List<String> args = new ArrayList<>(Arrays.asList(
                    "-proc:none", "-nowarn",
                    "-source", String.valueOf(feature),
                    "-target", String.valueOf(feature),
                    "-d", outputDir.toString()));
            if (classpath != null && !classpath.isEmpty()) {
                args.add("-classpath");
                args.add(classpath);
            }
            args.add(scriptFile.toString());

            StringWriter out = new StringWriter();
            StringWriter err = new StringWriter();
            Method compile = batch.getMethod("compile", String[].class,
                    PrintWriter.class, PrintWriter.class, progress);
            Object ok = compile.invoke(null, args.toArray(new String[0]),
                    new PrintWriter(out), new PrintWriter(err), null);
            if (!Boolean.TRUE.equals(ok)) {
                throw new IllegalStateException("compilation failed (bundled ECJ compiler):\n" + out + err);
            }

            Map<String, byte[]> result = new HashMap<>();
            try (var walk = Files.walk(outputDir)) {
                for (Path path : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                    String relative = outputDir.relativize(path).toString().replace(File.separatorChar, '/');
                    if (!relative.endsWith(".class")) continue;
                    String binaryName = relative.substring(0, relative.length() - 6).replace('/', '.');
                    result.put(binaryName, Files.readAllBytes(path));
                }
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("compilation failed: " + t, t);
        } finally {
            deleteRecursively(work);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null) return;
        try (var walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    static Map<String, byte[]> compileWith(JavaCompiler compiler, boolean external,
                                           String source, String classpath) {
        Bridge.log("[VORA] compiler: " + compiler.getClass().getName());

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        MemFileManager manager = new MemFileManager(standard);

        List<String> options = new ArrayList<>(Arrays.asList("-proc:none", "-nowarn"));
        int feature = Runtime.version().feature();
        options.add("-source");
        options.add(String.valueOf(feature));
        options.add("-target");
        options.add(String.valueOf(feature));
        if (classpath != null && !classpath.isEmpty()) {
            options.add("-classpath");
            options.add(classpath);
        }

        JavaFileObject scriptUnit = new SourceFile(pseudoName(source), source);
        Boolean success = compiler
                .getTask(null, manager, diagnostics, options, null, List.of(scriptUnit))
                .call();

        if (success == null || !success) {
            StringBuilder message = new StringBuilder(external
                    ? "compilation failed (bundled ECJ compiler):\n"
                    : "compilation failed:\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                message.append(diagnostic.toString()).append('\n');
            }
            throw new IllegalStateException(message.toString());
        }

        Map<String, byte[]> result = new HashMap<>();
        for (Map.Entry<String, ByteFile> entry : manager.outputs.entrySet()) {
            result.put(entry.getKey(), entry.getValue().bytes());
        }
        return result;
    }

    private static JavaCompiler externalCompiler() {
        try {
            for (JavaCompiler candidate : java.util.ServiceLoader.load(
                    JavaCompiler.class, ScriptCompiler.class.getClassLoader())) {
                if (candidate != null) return candidate;
            }
        } catch (Throwable ignored) {
        }
        for (String name : new String[] {
                "org.eclipse.jdt.internal.compiler.tool.EclipseCompiler" }) {
            try {
                return (JavaCompiler) Class.forName(name, true, ScriptCompiler.class.getClassLoader())
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static String addAgentJarToClasspath(String classpath) {
        String agentJar = System.getProperty("vora.agent.jar");
        if (agentJar != null) {
            File f = new File(agentJar);
            if (f.isFile()) {
                return classpath + File.pathSeparator + f.getAbsolutePath();
            }
        }
        return classpath;
    }

    public static void addAgentJar(java.util.Set<String> entries) {
        String agentJar = System.getProperty("vora.agent.jar");
        if (agentJar != null) {
            File f = new File(agentJar);
            if (f.isFile()) entries.add(f.getAbsolutePath());
        }
    }

    private static void addCodeSource(Set<String> entries, Class<?> type) {
        try {
            URL url = type.getProtectionDomain().getCodeSource().getLocation();
            if (url != null && "file".equals(url.getProtocol())) {
                entries.add(new File(url.toURI()).getAbsolutePath());
            }
        } catch (Throwable ignored) {
        }
    }





    static final class SourceFile extends SimpleJavaFileObject {
        private final String code;

        SourceFile(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    static final class ByteFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        ByteFile(String className, Kind kind) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return buffer;
        }

        byte[] bytes() {
            return buffer.toByteArray();
        }
    }

    static final class MemFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, ByteFile> outputs = new HashMap<>();

        MemFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            ByteFile file = new ByteFile(className, kind);
            outputs.put(className, file);
            return file;
        }

        @Override
        public void close() {

        }
    }
}