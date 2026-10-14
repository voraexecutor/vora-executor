package com.vora.agent.mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;






public final class JarRemapper {

    private JarRemapper() {
    }

    public static int remap(Path input, Path output, Remapper remapper) throws IOException {
        int count = 0;
        try (JarFile jar = new JarFile(input.toFile());
             OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(output));
             JarOutputStream out = new JarOutputStream(fileOut)) {

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class")) continue;

                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, remapper), 0);
                bytes = writer.toByteArray();

                String mappedName = remapper.map(name.substring(0, name.length() - 6)) + ".class";
                out.putNextEntry(new JarEntry(mappedName));
                out.write(bytes);
                out.closeEntry();
                count++;
            }
        }
        return count;
    }
}
