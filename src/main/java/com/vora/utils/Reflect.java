package com.vora.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;




public final class Reflect {

    private Reflect() {
    }

    public static <T> T getField(Object obj, String name) {
        try {
            Field f = findField(obj.getClass(), name);
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get field " + name, e);
        }
    }

    public static void setField(Object obj, String name, Object value) {
        try {
            Field f = findField(obj.getClass(), name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + name, e);
        }
    }

    public static <T> T invokeMethod(Object obj, String name, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            Method m = findMethod(obj.getClass(), name, paramTypes);
            m.setAccessible(true);
            return (T) m.invoke(obj, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke method " + name, e);
        }
    }

    public static <T> T newInstance(Class<T> clazz, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            Constructor<T> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    public static Class<?> forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found: " + className, e);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new IllegalStateException("Field not found: " + name);
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new IllegalStateException("Method not found: " + name);
    }

    public static List<Method> findMethods(Class<?> clazz, Predicate<Method> filter) {
        List<Method> result = new ArrayList<>();
        while (clazz != null) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (filter.test(m)) result.add(m);
            }
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    public static boolean isPublicStatic(Method m) {
        int mod = m.getModifiers();
        return Modifier.isPublic(mod) && Modifier.isStatic(mod);
    }

    public static boolean isFinal(Field f) {
        return Modifier.isFinal(f.getModifiers());
    }
}