package com.vora.agent;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public final class Boot {

    private Boot() {
    }

    public static void premain(String args, Instrumentation instrumentation) {
        launch("premain", args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        launch("agentmain", args, instrumentation);
    }

    private static void launch(String entry, String args, Instrumentation instrumentation) {
        try {
            File jar = locateJar(args);
            if (jar == null) {
                throw new IllegalStateException("could not locate the agent jar to reload from");
            }

            System.setProperty("vora.agent.build", Long.toString(jar.lastModified()));
            System.setProperty("vora.agent.jar", jar.getAbsolutePath());

            
            
            
            
            
            
            URLClassLoader loader = new URLClassLoader(
                    new URL[] { jar.toURI().toURL() },
                    ClassLoader.getPlatformClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    
                    
                    
                    
                    if (name.equals("org.client.agent.ScriptRegistry")
                            || name.startsWith("org.client.agent.ScriptRegistry$")) {
                        try {
                            Class<?> shared = Class.forName(name, false, ClassLoader.getSystemClassLoader());
                            if (resolve) resolveClass(shared);
                            return shared;
                        } catch (ClassNotFoundException | LinkageError ignored) {
                            
                            
                        }
                    }
                    return super.loadClass(name, resolve);
                }
            };

            Class<?> agent = Class.forName("com.vora.agent.d", true, loader);
            agent.getMethod(entry, String.class, Instrumentation.class).invoke(null, args, instrumentation);
        } catch (Throwable t) {
            t.printStackTrace();
            try {




                StringBuilder sb = new StringBuilder();
                sb.append("== ").append(new java.util.Date()).append(" ==").append('\n');
                sb.append("entry: ").append(entry).append('\n');
                sb.append("java.version: ").append(System.getProperty("java.version")).append('\n');
                sb.append("java.vm.name: ").append(System.getProperty("java.vm.name")).append('\n');
                sb.append("agent.jar: ").append(System.getProperty("vora.agent.jar")).append('\n');
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                sb.append(sw);
                Path dir = Path.of(System.getProperty("user.home", "."), ".vora", "agent");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("agent-error.log"), sb.toString(),
                        StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Throwable ignored) {

            }
        }
    }

    private static File locateJar(String args) {
        String encoded = option(args, "bootjar");
        if (encoded != null) {
            try {
                File file = new File(new String(
                        java.util.Base64.getUrlDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8));
                if (file.isFile()) return file;
            } catch (Throwable ignored) {
            }
        }

        try {
            URL location = Boot.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null && "file".equals(location.getProtocol())) {
                File file = new File(location.toURI());
                if (file.isFile()) return file;
            }
        } catch (Throwable ignored) {
        }

        String property = System.getProperty("executor.agentJar");
        if (property != null) {
            File file = new File(property);
            if (file.isFile()) return file;
        }
        return null;
    }

    private static String option(String args, String key) {
        if (args == null) return null;
        for (String part : args.split(";")) {
            int equals = part.indexOf('=');
            if (equals > 0 && part.substring(0, equals).equals(key)) {
                return part.substring(equals + 1);
            }
        }
        return null;
    }
}
