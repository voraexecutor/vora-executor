package com.vora.agent.mapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
























public final class MappingFile {

    private final String[] namespaces;
    private final List<String[]> classes = new ArrayList<>();
    private final List<Member> methods = new ArrayList<>();
    private final List<Member> fields = new ArrayList<>();

    private final Map<Long, Map<String, String>> classMaps = new ConcurrentHashMap<>();
    private final Map<Long, Mapping> directions = new ConcurrentHashMap<>();

    private MappingFile(String[] namespaces) {
        this.namespaces = namespaces;
    }

    public String[] namespaces() {
        return namespaces;
    }

    public int classCount() {
        return classes.size();
    }

    public int indexOf(String namespace) {
        for (int i = 0; i < namespaces.length; i++) {
            if (namespaces[i].equals(namespace)) return i;
        }
        return -1;
    }

    public List<String> classNames(int namespace) {
        List<String> result = new ArrayList<>(classes.size());
        for (String[] c : classes) result.add(c[namespace]);
        return result;
    }

    public static MappingFile parse(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        MappingFile file = null;
        String[] currentClass = null;
        int format = 0;

        for (String raw : lines) {
            if (raw.isEmpty() || raw.charAt(0) == '#') continue;

            if (raw.startsWith("tiny\t")) {
                String[] parts = raw.split("\t");
                file = new MappingFile(slice(parts, 3));
                format = 2;
                continue;
            }
            if (raw.startsWith("v1\t")) {
                String[] parts = raw.split("\t");
                file = new MappingFile(slice(parts, 1));
                format = 1;
                continue;
            }
            if (file == null) continue;

            if (format == 2) {
                if (raw.charAt(0) == '\t') {
                    if (raw.startsWith("\tm\t") || raw.startsWith("\tf\t")) {
                        char kind = raw.charAt(1);
                        String[] parts = raw.substring(3).split("\t", -1);
                        Member member = new Member(currentClass, parts[0], slice(parts, 1));
                        (kind == 'm' ? file.methods : file.fields).add(member);
                    }

                } else if (raw.startsWith("c\t")) {
                    currentClass = raw.substring(2).split("\t", -1);
                    file.classes.add(currentClass);
                }
            } else {
                if (raw.startsWith("CLASS\t")) {
                    file.classes.add(raw.substring(6).split("\t", -1));
                } else if (raw.startsWith("METHOD\t") || raw.startsWith("FIELD\t")) {
                    boolean method = raw.charAt(0) == 'M';
                    String[] parts = raw.substring(method ? 7 : 6).split("\t", -1);
                    Member member = new Member(new String[] { parts[0] }, parts[1], slice(parts, 2));
                    (method ? file.methods : file.fields).add(member);
                }
            }
        }

        if (file == null) {
            throw new IOException("not a mapping file: " + path);
        }
        return file;
    }















    static MappingFile mergeMojang(Path intermediaryTiny, Path mojangTxt) throws IOException {
        MappingFile base = parse(intermediaryTiny);
        int official = base.indexOf("official");
        int intermediary = base.indexOf("intermediary");
        if (official < 0 || intermediary < 0) {
            throw new IOException("intermediary file lacks official/intermediary namespaces");
        }

        Map<String, String> classesByName = new HashMap<>();
        Map<String, String> methodsByKey = new HashMap<>();
        Map<String, String> fieldsByKey = new HashMap<>();

        String currentOwner = null;
        for (String raw : Files.readAllLines(mojangTxt, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.endsWith(":") && line.contains("->")) {
                String[] parts = line.substring(0, line.length() - 1).split("->", -1);
                if (parts.length < 2) continue;
                currentOwner = parts[0].trim();
                classesByName.put(currentOwner, parts[1].trim());
            } else if (currentOwner != null && line.contains("->")) {
                int arrow = line.indexOf("->");
                String left = line.substring(0, arrow).trim();
                String named = line.substring(arrow + 2).trim();
                int paren = left.indexOf('(');
                if (paren >= 0) {
                    String name = left.substring(0, paren).trim();
                    String desc = left.substring(paren).replaceAll("\\s+", "");
                    methodsByKey.put(key3(currentOwner, name, desc), named);
                } else {
                    fieldsByKey.put(key2(currentOwner, left), named);
                }
            }
        }

        MappingFile merged = new MappingFile(new String[] { "official", "intermediary", "named" });


        for (String[] c : base.classes) {
            String obf = c[official];
            String named = classesByName.get(obf);
            merged.classes.add(new String[] { obf, c[intermediary], named == null ? obf : named });
        }


        for (Member m : base.methods) {
            if (m.names.length <= official || m.names.length <= intermediary) continue;
            String obfOwner = m.owners[0];
            String obfName = m.names[official];
            String desc = m.desc == null ? null : m.desc.replaceAll("\\s+", "");
            String named = desc == null ? null : methodsByKey.get(key3(obfOwner, obfName, desc));
            if (named == null) continue;
            merged.methods.add(new Member(m.owners, desc,
                    new String[] { obfName, m.names[intermediary], named }));
        }
        for (Member m : base.fields) {
            if (m.names.length <= official || m.names.length <= intermediary) continue;
            String obfOwner = m.owners[0];
            String obfName = m.names[official];
            String named = fieldsByKey.get(key2(obfOwner, obfName));
            if (named == null) continue;
            String desc = m.desc == null ? null : m.desc.replaceAll("\\s+", "");
            merged.fields.add(new Member(m.owners, desc,
                    new String[] { obfName, m.names[intermediary], named }));
        }
        return merged;
    }

    private static String key3(String owner, String name, String desc) {
        return owner + '/' + name + '/' + desc;
    }

    private static String key2(String owner, String name) {
        return owner + '/' + name;
    }





    public Mapping directional(int src, int dst) {
        long id = ((long) src << 32) | (dst & 0xffffffffL);
        return directions.computeIfAbsent(id, key -> build(src, dst));
    }

    private Mapping build(int src, int dst) {
        Map<String, String> toDst = classMap(src, dst);
        Map<String, String> toSrc = classMap(dst, src);
        Map<String, String> fromZeroToSrc = classMap(0, src);

        Map<String, Integer> indexByZero = new HashMap<>();
        for (int i = 0; i < classes.size(); i++) {
            indexByZero.put(classes.get(i)[0], i);
        }

        Map<String, String> methods = new HashMap<>();
        Map<String, String> methodsRev = new HashMap<>();
        Map<String, String> fields = new HashMap<>();
        Map<String, String> fieldsRev = new HashMap<>();

        for (Member member : this.methods) {
            addMember(member, src, dst, indexByZero, fromZeroToSrc, toDst, methods, methodsRev);
        }
        for (Member member : this.fields) {
            addMember(member, src, dst, indexByZero, fromZeroToSrc, toDst, fields, fieldsRev);
        }

        return new Mapping(toDst, toSrc, methods, methodsRev, fields, fieldsRev);
    }

    private void addMember(Member member, int src, int dst, Map<String, Integer> indexByZero,
                           Map<String, String> fromZeroToSrc, Map<String, String> toDst,
                           Map<String, String> forward, Map<String, String> reverse) {
        if (member.names.length <= Math.max(src, dst)) return;

        String ownerSrc = owner(member, src, indexByZero);
        String ownerDst = owner(member, dst, indexByZero);
        if (ownerSrc == null || ownerDst == null) return;

        String descSrc = mapDescriptor(member.desc, fromZeroToSrc);
        String descDst = mapDescriptor(descSrc, toDst);
        String nameSrc = member.names[src];
        String nameDst = member.names[dst];

        forward.put(Mapping.key(ownerSrc, nameSrc, descSrc), nameDst);
        reverse.put(Mapping.key(ownerDst, nameDst, descDst), nameSrc);
    }

    private String owner(Member member, int namespace, Map<String, Integer> indexByZero) {
        if (member.owners.length == namespaces.length) {
            return member.owners[namespace];
        }
        Integer index = indexByZero.get(member.owners[0]);
        return index == null ? null : classes.get(index)[namespace];
    }

    private Map<String, String> classMap(int from, int to) {
        long id = ((long) from << 32) | (to & 0xffffffffL);
        return classMaps.computeIfAbsent(id, key -> {
            Map<String, String> map = new HashMap<>(classes.size() * 2);
            for (String[] names : classes) {
                map.put(names[from], names[to]);
            }
            return map;
        });
    }

    public static String mapDescriptor(String descriptor, Map<String, String> classes) {
        if (descriptor == null || descriptor.indexOf('L') < 0) return descriptor;
        StringBuilder out = new StringBuilder(descriptor.length() + 16);
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int end = descriptor.indexOf(';', i);
                if (end < 0) {
                    out.append(descriptor, i, descriptor.length());
                    break;
                }
                String name = descriptor.substring(i + 1, end);
                String mapped = classes.get(name);
                out.append('L').append(mapped == null ? name : mapped).append(';');
                i = end + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String[] slice(String[] source, int from) {
        String[] result = new String[source.length - from];
        System.arraycopy(source, from, result, 0, result.length);
        return result;
    }

    private static final class Member {
        final String[] owners;
        final String desc;
        final String[] names;

        Member(String[] owners, String desc, String[] names) {
            this.owners = owners;
            this.desc = desc;
            this.names = names;
        }
    }
}
