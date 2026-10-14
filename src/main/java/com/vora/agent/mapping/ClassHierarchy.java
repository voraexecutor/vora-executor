package com.vora.agent.mapping;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;











public final class ClassHierarchy implements AutoCloseable {

    private static final String[] UNKNOWN = new String[0];

    private final JarFile jar;
    private final Map<String, String[]> cache = new ConcurrentHashMap<>();

    public ClassHierarchy(Path jarPath) throws IOException {
        this.jar = new JarFile(jarPath.toFile());
    }

    String[] directSupertypes(String internalName) {
        String[] cached = cache.get(internalName);
        if (cached != null) return cached;
        String[] result = read(internalName);
        cache.put(internalName, result);
        return result;
    }

    public List<String> ancestors(String internalName) {
        List<String> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(internalName);
        ArrayDeque<String> queue = new ArrayDeque<>();

        enqueue(queue, directSupertypes(internalName), seen);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            ordered.add(current);
            enqueue(queue, directSupertypes(current), seen);
        }
        return ordered;
    }

    private static void enqueue(ArrayDeque<String> queue, String[] types, Set<String> seen) {
        for (String type : types) {
            if (seen.add(type)) queue.add(type);
        }
    }

    private String[] read(String internalName) {
        JarEntry entry = jar.getJarEntry(internalName + ".class");
        if (entry == null) return UNKNOWN;
        try (InputStream in = jar.getInputStream(entry)) {
            ClassReader reader = new ClassReader(in.readAllBytes());
            String superName = reader.getSuperName();
            String[] interfaces = reader.getInterfaces();
            List<String> types = new ArrayList<>(1 + (interfaces == null ? 0 : interfaces.length));
            if (superName != null) types.add(superName);
            if (interfaces != null) {
                for (String type : interfaces) types.add(type);
            }
            return types.toArray(new String[0]);
        } catch (IOException e) {
            return UNKNOWN;
        }
    }

    @Override
    public void close() throws IOException {
        jar.close();
    }
}
