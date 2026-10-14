package com.vora.agent.script;

import java.lang.instrument.Instrumentation;
import java.util.Map;





public final class ScriptClassLoader extends ClassLoader {

    private final Map<String, byte[]> classes;

    public ScriptClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
        super(parent);
        this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name);
        if (bytes == null) throw new ClassNotFoundException(name);
        return defineClass(name, bytes, 0, bytes.length);
    }
}