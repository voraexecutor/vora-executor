package com.vora.agent;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public class b {

    private static final String ATTACH_CLASS = "com.sun.tools.attach.VirtualMachine";
    private static final String AGENT_MAIN = "com.vora.agent.Boot";

    public static boolean attachToProcess(String pid) {
        return attach(pid, "cmd=exec");
    }

    public static boolean attachToMinecraft() {
        return attachToMinecraft("cmd=exec");
    }

    public static boolean attachToMinecraft(String options) {
        Optional<String> mcPid = findMinecraftProcess();
        if (mcPid.isEmpty()) return false;
        System.out.println("minecraft pid: " + mcPid.get());
        return attach(mcPid.get(), options);
    }

    public static boolean attach(String pid, String options) {
        return attach(pid, options, null);
    }

    public static boolean attach(String pid, String options, java.util.function.Consumer<String> log) {
        try {
            Class<?> vmClass = Class.forName(ATTACH_CLASS);
            Method attachMethod = vmClass.getMethod("attach", String.class);
            Object vm = attachMethod.invoke(null, pid);

            String agentPath = getAgentPath();
            if (agentPath == null) {
                System.err.println("could not locate the agent jar (run: gradlew jar)");
                return false;
            }

            File agentJar = new File(agentPath);
            String enriched = options + (options == null || options.isEmpty() ? "" : ";")
                    + "bootjar=" + Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(agentJar.getAbsolutePath().getBytes(StandardCharsets.UTF_8));

            Method loadAgentMethod = vmClass.getMethod("loadAgent", String.class, String.class);
            loadAgentMethod.invoke(vm, agentJar.getAbsolutePath(), enriched);

            Method detachMethod = vmClass.getMethod("detach");
            detachMethod.invoke(vm);

            System.out.println("attached to pid: " + pid);
            return true;

        } catch (Throwable e) {
            if (log != null) {
                log.accept("[client] attach error: " + e.getClass().getName()
                        + (e.getMessage() == null ? "" : ": " + e.getMessage()));
                Throwable cause = e.getCause();
                if (cause != null) {
                    log.accept("[client]   cause: " + cause.getClass().getName()
                            + (cause.getMessage() == null ? "" : ": " + cause.getMessage()));
                }
            }
            e.printStackTrace();
            return false;
        }
    }

    
    public static List<String[]> listProcesses() {
        List<String[]> result = new ArrayList<>();
        try {
            Class<?> vmClass = Class.forName(ATTACH_CLASS);
            Method listMethod = vmClass.getMethod("list");
            @SuppressWarnings("unchecked")
            List<Object> vms = (List<Object>) listMethod.invoke(null);

            for (Object vmDesc : vms) {
                Method displayNameMethod = vmDesc.getClass().getMethod("displayName");
                Method idMethod = vmDesc.getClass().getMethod("id");
                String displayName = (String) displayNameMethod.invoke(vmDesc);
                String id = (String) idMethod.invoke(vmDesc);
                result.add(new String[] { id, displayName == null ? "" : displayName });
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Optional<String> findMinecraftProcess() {
        for (String[] p : listProcesses()) {
            if (isMinecraftProcess(p[1])) return Optional.of(p[0]);
        }
        return Optional.empty();
    }

    private static boolean isMinecraftProcess(String displayName) {
        if (displayName == null) return false;
        String lower = displayName.toLowerCase();
        return lower.contains("minecraft")
            || lower.contains("net.minecraft")
            || lower.contains("launchwrapper")
            || lower.contains("net.fabricmc")
            || lower.contains("cpw.mods")
            || lower.contains("gradlew");
    }



















    public static String getAgentPath() {
        File source = resolveSourceJar();
        if (source == null) return null;
        return syncAgentCopy(source);
    }

    private static File resolveSourceJar() {
        String property = System.getProperty("executor.agentJar");
        if (property != null) {
            File file = new File(property);
            if (file.isFile()) return file.getAbsoluteFile();
        }

        try {
            URL location = b.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                File file = new File(location.toURI());
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    return file.getAbsoluteFile();
                }
            }
        } catch (Exception ignored) {
        }

        File libs = new File("build/libs");
        File[] jars = libs.listFiles((dir, name) ->
                name.endsWith(".jar")
                        && !name.endsWith("-sources.jar")
                        && !name.endsWith("-javadoc.jar"));
        if (jars != null && jars.length > 0) {
            Arrays.sort(jars, Comparator.comparingLong(File::lastModified).reversed());
            for (File jar : jars) {
                if (hasAgentManifest(jar) || containsEcj(jar)) return jar.getAbsoluteFile();
            }
        }

        return null;
    }

    private static String syncAgentCopy(File source) {
        try {
            Path dir = Path.of(System.getProperty("user.home", "."), ".vora", "agent");
            Files.createDirectories(dir);




            Path dest = dir.resolve("vora-agent-" + source.length() + "-" + source.lastModified() + ".jar");
            File destFile = dest.toFile();
            if (destFile.isFile() && destFile.length() == source.length() && isReadableAgentJar(dest)) {
                return destFile.getAbsolutePath();
            }

            Path tmp = dir.resolve(dest.getFileName() + ".tmp");
            Files.copy(source.toPath(), tmp, StandardCopyOption.REPLACE_EXISTING);
            if (!isReadableAgentJar(tmp)) {


                Files.deleteIfExists(tmp);
                return source.getAbsolutePath();
            }
            try {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception moveFailed) {
                Files.deleteIfExists(tmp);
                if (destFile.isFile()) return destFile.getAbsolutePath();
                return source.getAbsolutePath();
            }
            return destFile.isFile() ? destFile.getAbsolutePath() : source.getAbsolutePath();
        } catch (Exception e) {

            return source.getAbsolutePath();
        }
    }

    private static boolean hasAgentManifest(File jar) {
        try (java.util.jar.JarFile file = new java.util.jar.JarFile(jar)) {
            java.util.jar.Manifest manifest = file.getManifest();
            return manifest != null
                    && AGENT_MAIN.equals(
                            manifest.getMainAttributes().getValue("Agent-Class"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean containsEcj(File jar) {
        try (java.util.jar.JarFile file = new java.util.jar.JarFile(jar)) {
            return file.getEntry(
                    "org/eclipse/jdt/internal/compiler/batch/messages.properties") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isReadableAgentJar(Path jar) {
        try (java.util.jar.JarFile file = new java.util.jar.JarFile(jar.toFile())) {
            var entry = file.getEntry(
                    "org/eclipse/jdt/internal/compiler/batch/messages.properties");
            if (entry == null) return false;
            try (var in = file.getInputStream(entry)) {
                byte[] buf = new byte[4096];
                int total = 0;
                int n;
                while ((n = in.read(buf)) > 0) total += n;
                return total > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }
}