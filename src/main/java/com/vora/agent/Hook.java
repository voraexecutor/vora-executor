package com.vora.agent;

import com.vora.agent.mapping.GameMappings;
import com.vora.agent.script.ScriptBuilder;
import com.vora.agent.script.ScriptClassConstants;
import com.vora.agent.script.ScriptClassLoader;
import com.vora.agent.script.ScriptCompiler;
import com.vora.agent.script.ScriptExecutor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;









































public final class Hook {

    private static final String BRIDGE_CLASS = "com.vora.game.VoraTickBridge";
    private static final String BRIDGE_INTERNAL = BRIDGE_CLASS.replace('.', '/');

    private static final String HOOK_BRIDGE = "com.vora.agent.HookBridge";
    private static final String HOOK_BRIDGE_INTERNAL = HOOK_BRIDGE.replace('.', '/');

    private static final String LEGACY_FIELD = "voraTickBridge";

    private static final String AGENT_PREFIX = "com/vora/agent";

    private static final List<JarFile> BOOTSTRAP_JARS = new ArrayList<>();

    private Hook() {
    }

    public static void execute(String id, String userCode, Instrumentation instrumentation,
                               Consumer<String> log, Consumer<Throwable> error) throws Exception {
        ClassLoader gameLoader = ScriptExecutor.findGameLoader(instrumentation);
        if (gameLoader == null) {
            throw new IllegalStateException(
                    "Could not find the Minecraft class loader. Is the game actually running?");
        }
        log.accept("[VORA] mixin mode - game class loader: " + gameLoader.getClass().getName());

        boolean named = ScriptExecutor.canLoad(gameLoader, ScriptClassConstants.MC_CLIENT);
        if (!named && !ScriptExecutor.canLoad(gameLoader, ScriptClassConstants.MC_CLIENT_INTERMEDIARY)) {
            throw new IllegalStateException(
                    "Found the game class loader but neither " + ScriptClassConstants.MC_CLIENT + " nor "
                            + ScriptClassConstants.MC_CLIENT_INTERMEDIARY + " is loadable.");
        }
        String owner = (named ? ScriptClassConstants.MC_CLIENT : ScriptClassConstants.MC_CLIENT_INTERMEDIARY)
                .replace('.', '/');






        healLegacy(instrumentation, gameLoader, owner, log);
        publishBridge(instrumentation, gameLoader, log);


        log.accept(named
                ? "[VORA] mixin mode - named (dev environment)"
                : "[VORA] mixin mode - intermediary (production) - preparing mappings");
        String classpath;
        GameMappings.Context context = null;
        if (named) {
            classpath = ScriptExecutor.buildRuntimeClasspath(gameLoader);
        } else {
            GameMappings.Context ctx = GameMappings.prepare(gameLoader);
            context = ctx;
            classpath = ScriptCompiler.addAgentJarToClasspath(ctx.namedClasspath);
        }

        String binaryName = ScriptClassConstants.SCRIPT_CLASS + "Hook" + Integer.toHexString(userCode.hashCode());
        String simpleName = binaryName.substring(binaryName.lastIndexOf('.') + 1);
        String source = context != null
                ? ScriptBuilder.buildHookSource(userCode, simpleName, context.namedClientClass, context.screenApi)
                : ScriptBuilder.buildHookSource(userCode, simpleName);
        Map<String, byte[]> classes = ScriptCompiler.compile(source, classpath);
        if (context != null) {
            classes = GameMappings.remapClasses(classes, context.namedToIntermediary, context.namedHierarchy);
        }
        log.accept("[VORA] mixin mode - compiled " + classes.size() + " class(es)");

        ClassLoader scriptLoader = new ScriptClassLoader(gameLoader, classes);
        Class<?> hookClass = scriptLoader.loadClass(binaryName);
        hookClass.getField("LOG").set(null, (Consumer<String>) log);
        hookClass.getField("ERR").set(null, (Consumer<Throwable>) error);





        Class<?> registry = Class.forName(HOOK_BRIDGE, true, ClassLoader.getSystemClassLoader());
        registry.getMethod("register", String.class, Class.class).invoke(null, binaryName, hookClass);


        Class<?> bridge = Class.forName(BRIDGE_CLASS, true, gameLoader);
        Field listener = bridge.getField("listener");
        listener.set(null, (Runnable) () -> {
            try {
                registry.getMethod("tick").invoke(null);
            } catch (Throwable ignored) {

            }
        });

        String tick;
        if (named) {
            tick = "tick";
        } else {


            String namedClientOwner = context.namedClientClass.replace('.', '/');
            String mapped = context.namedToIntermediary.mapMethod(namedClientOwner, "tick", "()V");
            tick = mapped != null ? mapped : "tick";
        }

        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(Module module, ClassLoader loader, String internalName,
                                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) {
                if (internalName == null || !internalName.equals(owner)) return null;
                if (loader != gameLoader) return null;
                try {
                    return injectTickCall(bytes, tick);
                } catch (Throwable t) {
                    log.accept("[VORA] mixin mode - transform failed: " + t);
                    return null;
                }
            }
        };

        instrumentation.addTransformer(transformer, true);
        Class<?> mc = Class.forName(owner.replace('/', '.'), false, gameLoader);
        instrumentation.retransformClasses(mc);
        log.accept("[VORA] mixin mode - installed into " + owner + "." + tick);
    }









    public static void healNow(Instrumentation instrumentation, Consumer<String> log) {
        try {
            ClassLoader gameLoader = ScriptExecutor.findGameLoader(instrumentation);
            String owner = resolveOwner(gameLoader);
            if (owner == null) {
                log.accept("[VORA] mixin mode - early heal skipped (game class not loadable)");
                return;
            }
            healLegacy(instrumentation, gameLoader, owner, log);



            publishBridge(instrumentation, gameLoader, log);
        } catch (Throwable t) {
            try {
                log.accept("[VORA] mixin mode - early heal failed: " + t);
            } catch (Throwable ignored) {

            }
        }
    }

    private static String resolveOwner(ClassLoader gameLoader) {
        if (gameLoader == null) return null;
        if (ScriptExecutor.canLoad(gameLoader, ScriptClassConstants.MC_CLIENT)) {
            return ScriptClassConstants.MC_CLIENT.replace('.', '/');
        }
        if (ScriptExecutor.canLoad(gameLoader, ScriptClassConstants.MC_CLIENT_INTERMEDIARY)) {
            return ScriptClassConstants.MC_CLIENT_INTERMEDIARY.replace('.', '/');
        }
        return null;
    }








    private static void healLegacy(Instrumentation instrumentation, ClassLoader gameLoader,
                                   String owner, Consumer<String> log) {
        ClassFileTransformer healer = new ClassFileTransformer() {
            @Override
            public byte[] transform(Module module, ClassLoader loader, String internalName,
                                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) {
                if (internalName == null || !internalName.equals(owner)) return null;
                if (loader != gameLoader) return null;
                try {
                    byte[] healed = stripLegacy(bytes);
                    if (healed != null) {
                        log.accept("[VORA] mixin mode - healed a legacy injection in " + internalName);
                    }
                    return healed;
                } catch (Throwable t) {
                    return null;
                }
            }
        };
        instrumentation.addTransformer(healer, true);
        try {
            Class<?> target = Class.forName(owner.replace('/', '.'), false, gameLoader);
            instrumentation.retransformClasses(target);
        } catch (Throwable t) {
            log.accept("[VORA] mixin mode - legacy heal skipped: " + t);
        }
    }























    private static void publishBridge(Instrumentation instrumentation, ClassLoader gameLoader,
                                      Consumer<String> log) throws Exception {
        if (isDefinedBy(gameLoader)) {
            return;
        }

        Path jar = writeBridgeJar();

        if (isKnot(gameLoader)) {
            if (!addUrlToKnot(gameLoader, jar.toUri().toURL())) {
                throw new IllegalStateException(
                        "cannot expose " + BRIDGE_CLASS + " to the game: no addUrlFwd seam on "
                                + gameLoader.getClass().getName());
            }
            log.accept("[VORA] mixin mode - exposed " + BRIDGE_CLASS + " to the game class loader");
            return;
        }

        try {
            Class.forName(BRIDGE_CLASS, false, gameLoader);
            log.accept("[VORA] mixin mode - " + BRIDGE_CLASS + " loadable through the game loader");
            return;
        } catch (ClassNotFoundException ignored) {

        }

        JarFile boot = new JarFile(jar.toFile());
        BOOTSTRAP_JARS.add(boot);
        instrumentation.appendToBootstrapClassLoaderSearch(boot);
        log.accept("[VORA] mixin mode - published " + BRIDGE_CLASS + " on the bootstrap class path");
    }

    private static boolean isDefinedBy(ClassLoader gameLoader) {
        try {
            Class.forName(BRIDGE_CLASS, false, gameLoader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isKnot(ClassLoader loader) {
        return loader != null && "KnotClassLoader".equals(loader.getClass().getSimpleName());
    }









    private static boolean addUrlToKnot(ClassLoader gameLoader, URL url) {
        Class<?> type = gameLoader.getClass();
        try {
            Method addUrlFwd = type.getMethod("addUrlFwd", URL.class);
            addUrlFwd.setAccessible(true);
            addUrlFwd.invoke(gameLoader, url);
            return true;
        } catch (NoSuchMethodException ignored) {

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("addUrlFwd on the game loader failed", e);
        }

        try {
            Object knotDelegate = type.getMethod("getDelegate").invoke(gameLoader);
            Method addCodeSource = knotDelegate.getClass().getMethod("addCodeSource", Path.class);
            addCodeSource.setAccessible(true);
            addCodeSource.invoke(knotDelegate, Path.of(url.toURI()));
            return true;
        } catch (ReflectiveOperationException | java.net.URISyntaxException e) {
            return false;
        }
    }

    private static Path writeBridgeJar() throws Exception {
        byte[] bytes;
        try (InputStream in = Hook.class.getResourceAsStream("/" + BRIDGE_INTERNAL + ".class")) {
            if (in == null) {
                throw new IllegalStateException("bridge class " + BRIDGE_INTERNAL + " is missing from the agent jar");
            }
            bytes = in.readAllBytes();
        }

        Path dir = Path.of(System.getProperty("user.home", "."), ".vora", "agent");
        Files.createDirectories(dir);
        Path jar = dir.resolve("vora-bridge-" + bytes.length + "-"
                + Integer.toHexString(java.util.Arrays.hashCode(bytes)) + ".jar");
        if (!Files.isRegularFile(jar)) {
            Path tmp = dir.resolve(jar.getFileName() + ".tmp");
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(tmp,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                out.putNextEntry(new ZipEntry(BRIDGE_INTERNAL + ".class"));
                out.write(bytes);
                out.closeEntry();
            }
            try {
                Files.move(tmp, jar, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception moveFailed) {
                Files.deleteIfExists(tmp);
                if (!Files.isRegularFile(jar)) throw moveFailed;
            }
        }
        return jar;
    }










    private static byte[] injectTickCall(byte[] bytes, String tickMethod) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);

        boolean changed = stripLegacyIn(node);

        MethodNode tickNode = null;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            if (method.name.equals(tickMethod) && method.desc.equals("()V")) {
                tickNode = method;
                break;
            }
        }
        if (tickNode != null) {
            boolean hasCall = false;
            for (AbstractInsnNode i : tickNode.instructions) {
                if (i instanceof MethodInsnNode call
                        && call.getOpcode() == Opcodes.INVOKESTATIC
                        && call.owner.equals(BRIDGE_INTERNAL)
                        && call.name.equals("tick")
                        && call.desc.equals("()V")) {
                    hasCall = true;
                    break;
                }
            }
            if (!hasCall) {
                tickNode.instructions.insert(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, BRIDGE_INTERNAL, "tick", "()V", false));
                changed = true;
            }
        }

        if (!changed) return null;
        return write(node);
    }

    private static byte[] stripLegacy(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        if (!stripLegacyIn(node)) return null;
        return write(node);
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }













    private static boolean stripLegacyIn(ClassNode node) {
        boolean changed = false;





        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
                AbstractInsnNode next = insn.getNext();
                boolean remove = false;
                if (insn instanceof MethodInsnNode call) {
                    remove = isAgent(call.owner);
                } else if (insn instanceof FieldInsnNode field) {
                    remove = isAgent(field.owner);
                }
                if (remove) {
                    method.instructions.remove(insn);
                    changed = true;
                }
                insn = next;
            }
        }


        for (MethodNode method : node.methods) {
            if (stripLegacyGuard(method)) changed = true;
        }



        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof FieldInsnNode field && field.name.equals(LEGACY_FIELD)) {
                    method.instructions.remove(insn);
                    changed = true;
                }
                insn = next;
            }
        }






        return changed;
    }

    private static boolean isAgent(String internalName) {
        return internalName != null
                && (internalName.equals(HOOK_BRIDGE_INTERNAL) || internalName.startsWith(AGENT_PREFIX));
    }














    private static boolean stripLegacyGuard(MethodNode method) {
        if (method.instructions == null) return false;
        InsnList insns = method.instructions;
        boolean changed = false;
        boolean again = true;
        while (again) {
            again = false;
            for (AbstractInsnNode i = insns.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.GETSTATIC
                        && field.name.equals(LEGACY_FIELD)) {
                    AbstractInsnNode after = field.getNext();
                    if (after instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.IFNULL) {




                        AbstractInsnNode frameAfterLabel = jump.label.getNext();
                        for (AbstractInsnNode cur = field; cur != null; ) {
                            AbstractInsnNode next = cur.getNext();
                            boolean last = cur == jump.label;
                            insns.remove(cur);
                            if (last) break;
                            cur = next;
                        }
                        if (frameAfterLabel instanceof org.objectweb.asm.tree.FrameNode) {
                            insns.remove(frameAfterLabel);
                        }
                    } else {
                        insns.remove(field);
                    }
                    changed = true;
                    again = true;
                    break;
                }
            }
        }
        return changed;
    }
}
