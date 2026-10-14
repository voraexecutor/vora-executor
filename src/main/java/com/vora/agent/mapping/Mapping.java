package com.vora.agent.mapping;

import java.util.Map;






public final class Mapping {

    private final Map<String, String> classes;
    private final Map<String, String> classesRev;
    private final Map<String, String> methods;
    private final Map<String, String> methodsRev;
    private final Map<String, String> fields;
    private final Map<String, String> fieldsRev;

    Mapping(Map<String, String> classes, Map<String, String> classesRev,
            Map<String, String> methods, Map<String, String> methodsRev,
            Map<String, String> fields, Map<String, String> fieldsRev) {
        this.classes = classes;
        this.classesRev = classesRev;
        this.methods = methods;
        this.methodsRev = methodsRev;
        this.fields = fields;
        this.fieldsRev = fieldsRev;
    }

    public String mapClass(String internalName) {
        String mapped = classes.get(internalName);
        return mapped == null ? internalName : mapped;
    }

    public String mapClassReverse(String internalName) {
        String mapped = classesRev.get(internalName);
        return mapped == null ? internalName : mapped;
    }

    public String mapMethod(String owner, String name, String descriptor) {
        return methods.get(key(owner, name, descriptor));
    }

    public String mapField(String owner, String name, String descriptor) {
        return fields.get(key(owner, name, descriptor));
    }

    public int classCount() {
        return classes.size();
    }

    public int methodCount() {
        return methods.size();
    }

    public int fieldCount() {
        return fields.size();
    }

    public Mapping reverse() {
        return new Mapping(classesRev, classes, methodsRev, methods, fieldsRev, fields);
    }

    static String key(String owner, String name, String descriptor) {
        return owner + '#' + name + '#' + descriptor;
    }
}
