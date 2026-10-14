package com.vora.agent.script;

import com.vora.agent.ScriptRegistry;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;





public final class ScriptRunnerImpl {

    private ScriptRunnerImpl() {
    }

    public static void execute(String id, String userCode, Instrumentation instrumentation, Consumer<String> log, Consumer<Throwable> error) throws Exception {
        ScriptExecutor executor = new ScriptExecutor(log, error);
        executor.execute(id, userCode, instrumentation);
    }

    static void run(String id, Map<String, byte[]> classes, ClassLoader gameLoader, Consumer<String> log, Consumer<Throwable> error) throws Exception {
        ClassLoader scriptLoader = new ScriptClassLoader(gameLoader, classes);
        Class<?> scriptClass = scriptLoader.loadClass(ScriptClassConstants.SCRIPT_CLASS);
        scriptClass.getField("LOG").set(null, (Consumer<String>) log);
        scriptClass.getField("ERR").set(null, (Consumer<Throwable>) error);

        ScriptRegistry.register(id, scriptLoader, scriptClass);

        scriptClass.getMethod("dispatch").invoke(null);

        CountDownLatch done = (CountDownLatch) scriptClass.getField("DONE").get(null);
        if (!done.await(120, TimeUnit.SECONDS)) {

        }
    }
}