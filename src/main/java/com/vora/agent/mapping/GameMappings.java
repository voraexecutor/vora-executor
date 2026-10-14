package com.vora.agent.mapping;

import com.vora.agent.Bridge;
import com.vora.common.VoraTemp;
import com.vora.utils.Json;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;













public final class GameMappings {

    private static final String MC_CLIENT_INTERMEDIARY = "net/minecraft/class_310";

    private static final String INTERMEDIARY_URL =
            "https://maven.fabricmc.net/net/fabricmc/intermediary/%s/intermediary-%s.jar";
    private static final String YARN_META_URL =
            "https://meta.fabricmc.net/v2/versions/yarn/%s";
    private static final String YARN_JAR_URL =
            "https://maven.fabricmc.net/net/fabricmc/yarn/%s/yarn-%s-v2.jar";
    private static final String MOJANG_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private static volatile Context cached;

    private GameMappings() {
    }

    public static final class Context {
        public final String namedClasspath;
        public final Mapping namedToIntermediary;
        public final ClassHierarchy namedHierarchy;
        public final String clientRuntimeName;



        public final String namedClientClass;
        public final String screenApi;
        public final String version;

        Context(String namedClasspath, Mapping namedToIntermediary, ClassHierarchy namedHierarchy,
                String clientRuntimeName, String namedClientClass, String screenApi, String version) {
            this.namedClasspath = namedClasspath;
            this.namedToIntermediary = namedToIntermediary;
            this.namedHierarchy = namedHierarchy;
            this.clientRuntimeName = clientRuntimeName;
            this.namedClientClass = namedClientClass;
            this.screenApi = screenApi;
            this.version = version;
        }
    }

    public static Context prepare(ClassLoader gameLoader) throws Exception {
        Context local = cached;
        if (local != null) return local;

        List<String> candidates = versionCandidates(gameLoader);
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Could not determine the Minecraft version. Cannot download matching mappings.");
        }

        Path cache = cacheDir();
        Path intermediaryTiny = null;
        NamedMappings namedMappings = null;
        String version = null;
        IOException lastFailure = null;
        for (String candidate : candidates) {
            try {
                intermediaryTiny = fetchIntermediary(candidate, cache);
                namedMappings = fetchNamed(candidate, cache);
                version = candidate;
                break;
            } catch (IOException e) {
                lastFailure = e;
                Bridge.log("[agent] no mappings for candidate version " + candidate + " (" + e.getMessage() + ")");
            }
        }
        if (version == null) {
            throw new IllegalStateException("Could not fetch mappings for any of " + candidates
                    + " - the game needs internet access (maven.fabricmc.net). Last error: "
                    + (lastFailure == null ? "?" : lastFailure.getMessage()), lastFailure);
        }
        Bridge.log("[agent] minecraft version: " + version + " (named mappings: " + namedMappings.basis + ")");

        MappingFile intermediaryFile = MappingFile.parse(intermediaryTiny);
        MappingFile namedFile;
        if ("yarn".equals(namedMappings.basis)) {
            namedFile = MappingFile.parse(namedMappings.file);
        } else {
            Bridge.log("[agent] yarn had no build for " + version
                    + " - using Mojang official names (scripts must use Mojang names)");
            namedFile = MappingFile.mergeMojang(intermediaryTiny, namedMappings.file);
        }

        int official = intermediaryFile.indexOf("official");
        int intermediary = intermediaryFile.indexOf("intermediary");
        if (official < 0 || intermediary < 0) {
            throw new IllegalStateException("Unexpected intermediary mapping namespaces: "
                    + String.join(",", intermediaryFile.namespaces()));
        }
        int namedIntermediary = namedFile.indexOf("intermediary");
        int named = namedFile.indexOf("named");
        if (namedIntermediary < 0 || named < 0) {
            throw new IllegalStateException("Unexpected named mapping namespaces: "
                    + String.join(",", namedFile.namespaces()));
        }

        Mapping officialToIntermediary = intermediaryFile.directional(official, intermediary);
        Mapping intermediaryToNamed = namedFile.directional(namedIntermediary, named);
        Mapping namedToIntermediary = intermediaryToNamed.reverse();

        String officialClient = officialToIntermediary.mapClassReverse(MC_CLIENT_INTERMEDIARY);


        String namedClientClass = namedToIntermediary.mapClassReverse(MC_CLIENT_INTERMEDIARY).replace('/', '.');
        Bridge.log("[agent] named client class: " + namedClientClass);

        Set<String> officialNames = new HashSet<>(intermediaryFile.classNames(official));
        Path gameJar = findGameJar(classpathEntries(gameLoader), officialNames);
        Bridge.log("[agent] game jar: " + gameJar);
        if (gameJar == null) {
            throw new IllegalStateException(
                    "Could not locate the Minecraft jar on the classpath. Unsupported launcher layout?");
        }

        Path namedJar = remapGameJar(gameJar, version, namedMappings.basis,
                officialToIntermediary, intermediaryToNamed, cache);

        String classpath = buildNamedClasspath(classpathEntries(gameLoader), gameJar, namedJar);
        local = new Context(classpath, namedToIntermediary, new ClassHierarchy(namedJar),
                MC_CLIENT_INTERMEDIARY, namedClientClass, detectScreenApi(namedJar), version);
        cached = local;
        return local;
    }

    public static Map<String, byte[]> remapClasses(Map<String, byte[]> classes, Mapping namedToIntermediary) {
        return remapClasses(classes, namedToIntermediary, null);
    }

    public static Map<String, byte[]> remapClasses(Map<String, byte[]> classes, Mapping namedToIntermediary,
                                                   ClassHierarchy namedHierarchy) {
        Remapper remapper = new MappingRemapper(namedToIntermediary, namedHierarchy);
        Map<String, byte[]> result = new HashMap<>(classes.size() * 2);
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            ClassReader reader = new ClassReader(entry.getValue());
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassRemapper(writer, remapper), 0);
            result.put(entry.getKey(), writer.toByteArray());
        }
        return result;
    }



    private static Path cacheDir() throws IOException {
        return VoraTemp.mappingsDir();
    }

    private static Path fetchIntermediary(String version, Path cache) throws IOException {
        Path out = cache.resolve("intermediary-" + safe(version) + ".tiny");
        if (Files.exists(out) && Files.size(out) > 0) return out;
        Bridge.log("[agent] downloading intermediary mappings for " + version + " ...");
        byte[] jar = httpGet(String.format(INTERMEDIARY_URL, version, version));
        extract(jar, "mappings/mappings.tiny", out);
        return out;
    }







    private static NamedMappings fetchNamed(String version, Path cache) throws IOException {
        Path yarnOut = cache.resolve("yarn-" + safe(version) + ".tiny");
        if (Files.exists(yarnOut) && Files.size(yarnOut) > 0) {
            return new NamedMappings(yarnOut, "yarn");
        }
        try {
            Bridge.log("[agent] downloading yarn mappings for " + version + " ...");
            String meta = new String(httpGet(String.format(YARN_META_URL, version)), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(meta);
            if (!matcher.find()) {
                throw new IOException("No yarn build published for Minecraft " + version);
            }
            String yarnVersion = matcher.group(1);
            Bridge.log("[agent] using yarn " + yarnVersion);
            byte[] jar = httpGet(String.format(YARN_JAR_URL, yarnVersion, yarnVersion));
            extract(jar, "mappings/mappings.tiny", yarnOut);
            return new NamedMappings(yarnOut, "yarn");
        } catch (IOException yarnError) {
            Bridge.log("[agent] yarn unavailable for " + version + " (" + yarnError.getMessage()
                    + ") - falling back to Mojang official mappings");
        }
        Path mojangOut = cache.resolve("mojang-" + safe(version) + ".txt");
        if (!Files.exists(mojangOut) || Files.size(mojangOut) == 0) {
            Bridge.log("[agent] downloading official (Mojang) mappings for " + version + " ...");
            Files.write(mojangOut, httpGet(clientMappingsUrl(version)));
        }
        return new NamedMappings(mojangOut, "mojang");
    }

    private static String clientMappingsUrl(String version) throws IOException {
        String manifest = new String(httpGet(MOJANG_MANIFEST_URL), StandardCharsets.UTF_8);
        Object parsed;
        try {
            parsed = Json.parse(manifest);
        } catch (RuntimeException e) {
            throw new IOException("could not parse Mojang version manifest: " + e.getMessage(), e);
        }
        @SuppressWarnings("unchecked")
        List<Object> versions = (List<Object>) ((Map<String, Object>) parsed).get("versions");
        String versionUrl = null;
        if (versions != null) {
            for (Object entry : versions) {
                if (!(entry instanceof Map<?, ?>)) continue;
                if (version.equals(String.valueOf(((Map<?, ?>) entry).get("id")))) {
                    versionUrl = String.valueOf(((Map<?, ?>) entry).get("url"));
                    break;
                }
            }
        }
        if (versionUrl == null) {
            throw new IOException("no Mojang manifest entry for Minecraft " + version);
        }
        Object versionJson;
        try {
            versionJson = Json.parse(new String(httpGet(versionUrl), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IOException("could not parse Mojang version JSON for " + version + ": " + e.getMessage(), e);
        }
        if (!(versionJson instanceof Map<?, ?> root)) throw new IOException("unexpected version JSON for " + version);
        Object downloads = root.get("downloads");
        Object mappings = downloads instanceof Map<?, ?> ? ((Map<?, ?>) downloads).get("client_mappings") : null;
        Object url = mappings instanceof Map<?, ?> ? ((Map<?, ?>) mappings).get("url") : null;
        if (!(url instanceof String string) || string.isEmpty()) {
            throw new IOException("version JSON for " + version + " has no client_mappings");
        }
        return string;
    }

    private static final class NamedMappings {
        final Path file;
        final String basis;

        NamedMappings(Path file, String basis) {
            this.file = file;
            this.basis = basis;
        }
    }

    private static Path remapGameJar(Path gameJar, String version, String basis, Mapping officialToIntermediary,
                                     Mapping intermediaryToNamed, Path cache) throws IOException {



        String cacheName = "vora-" + safe(version) + "-" + basis + ".jar";
        Path out = cache.resolve(cacheName);
        if (Files.exists(out) && Files.size(out) > 1024) return out;

        Bridge.log("[agent] remapping game jar ...");
        Remapper remapper = new ChainedRemapper(
                new MappingRemapper(officialToIntermediary),
                new MappingRemapper(intermediaryToNamed));

        Path tmp = cache.resolve(out.getFileName() + ".tmp");
        int count = JarRemapper.remap(gameJar, tmp, remapper);
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        Bridge.log("[agent] remapped " + count + " classes -> " + out.getFileName());
        return out;
    }










    private static String detectScreenApi(Path namedJar) {
        try (JarFile jar = new JarFile(namedJar.toFile())) {
            JarEntry entry = jar.getJarEntry("net/minecraft/client/gui/screen/Screen.class");
            if (entry == null) return "legacy";
            byte[] bytes;
            try (InputStream in = jar.getInputStream(entry)) {
                bytes = in.readAllBytes();
            }
            boolean[] elementFocus = new boolean[1];
            boolean[] drawContextRender = new boolean[1];
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if ("setFocused".equals(name) && "(Lnet/minecraft/client/gui/Element;)V".equals(descriptor)) {
                        elementFocus[0] = true;
                    } else if ("render".equals(name)
                            && "(Lnet/minecraft/client/gui/DrawContext;IIIF)V".equals(descriptor)) {
                        drawContextRender[0] = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (elementFocus[0]) return "1.21";
            if (drawContextRender[0]) return "1.20";
            return "legacy";
        } catch (IOException e) {

            return "1.21";
        }
    }

    private static String buildNamedClasspath(List<String> runtime, Path gameJar, Path namedJar) {
        String game = canonical(gameJar.toFile());
        Set<String> entries = new LinkedHashSet<>();
        for (String entry : runtime) {
            if (entry == null || entry.isEmpty()) continue;
            if (canonical(new File(entry)).equals(game)) continue;
            entries.add(entry);
        }
        entries.add(namedJar.toAbsolutePath().toString());
        return String.join(File.pathSeparator, entries);
    }








    static List<String> versionCandidates(ClassLoader gameLoader) {
        List<String> runtime = classpathEntries(gameLoader);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();


        String fromJar = versionFromGameJar(runtime);
        if (fromJar != null) candidates.add(fromJar);


        try {
            Class<?> implClass = Class.forName("net.fabricmc.loader.impl.FabricLoaderImpl", false, gameLoader);
            Object loader = implClass.getField("INSTANCE").get(null);
            Object provider = implClass.getMethod("getGameProvider").invoke(loader);
            if (provider != null) {
                for (String method : new String[] { "getRawGameVersion", "getNormalizedGameVersion" }) {
                    try {
                        Object value = provider.getClass().getMethod(method).invoke(provider);
                        if (value instanceof String version && looksLikeVersion(version)) candidates.add(version);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }


        for (String entry : runtime) {
            String name = new File(entry).getName();
            if (!name.toLowerCase().endsWith(".jar")) continue;
            String version = name.substring(0, name.length() - 4);
            if (looksLikeVersion(version)) candidates.add(version);
        }

        return new ArrayList<>(candidates);
    }

    static String versionFromGameJar(List<String> runtime) {
        for (String entry : runtime) {
            if (!entry.toLowerCase().endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(entry)) {
                if (jar.getJarEntry("net/minecraft/client/main/Main.class") == null) continue;
                JarEntry versionJson = jar.getJarEntry("version.json");
                if (versionJson == null) continue;
                String json;
                try (InputStream in = jar.getInputStream(versionJson)) {
                    json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                if (matcher.find() && looksLikeVersion(matcher.group(1))) {
                    return matcher.group(1);
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static boolean looksLikeVersion(String value) {
        return value != null && value.matches("(?:\\d+\\.\\d+(?:\\.\\d+)?(?:[-+].*)?|\\d{2}w\\d{2}[a-z])");
    }

    static Path findGameJar(List<String> runtime, Set<String> officialNames) {
        Path best = null;
        int bestCount = 0;
        for (String entry : runtime) {
            File file = new File(entry);
            if (!file.isFile() || !entry.toLowerCase().endsWith(".jar")) continue;
            int count = 0;
            try (JarFile jar = new JarFile(file)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.endsWith(".class")
                            && officialNames.contains(name.substring(0, name.length() - 6))) {
                        count++;
                    }
                }
            } catch (IOException ignored) {
                continue;
            }
            if (count > bestCount) {
                bestCount = count;
                best = file.toPath();
            }
        }
        return bestCount > 50 ? best : null;
    }

    public static List<String> classpathEntries(ClassLoader gameLoader) {
        Set<String> entries = new LinkedHashSet<>();
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String part : classpath.split(File.pathSeparator)) {
                if (!part.isEmpty()) entries.add(part);
            }
        }
        for (ClassLoader loader = gameLoader; loader != null; loader = loader.getParent()) {
            try {
                Object urls = loader.getClass().getMethod("getURLs").invoke(loader);
                if (urls instanceof URL[] array) {
                    for (URL url : array) addUrl(entries, url);
                }
            } catch (Throwable ignored) {
                if (loader instanceof URLClassLoader urlLoader) {
                    for (URL url : urlLoader.getURLs()) addUrl(entries, url);
                }
            }
        }
        return new ArrayList<>(entries);
    }

    private static void addUrl(Set<String> entries, URL url) {
        if (url == null) return;
        try {
            if ("file".equals(url.getProtocol())) {
                entries.add(new File(url.toURI()).getAbsolutePath());
            } else {
                entries.add(url.toString());
            }
        } catch (Throwable ignored) {
        }
    }



    private static byte[] httpGet(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(120000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Vora-Executor");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " for " + url);
        }
        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static void extract(byte[] jar, String entryName, Path target) throws IOException {
        try (JarInputStream in = new JarInputStream(new ByteArrayInputStream(jar))) {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                if (!entryName.equals(entry.getName())) continue;
                Files.write(target, in.readAllBytes());
                return;
            }
        }
        throw new IOException("entry " + entryName + " missing from mapping jar");
    }

    private static String canonical(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
