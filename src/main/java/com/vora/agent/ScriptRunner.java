package com.vora.agent;

import com.vora.agent.script.ScriptExecutor;

import java.lang.instrument.Instrumentation;





public final class ScriptRunner {

    private ScriptRunner() {
    }

    public static void execute(String id, String userCode, Instrumentation instrumentation) throws Exception {
        ScriptExecutor executor = new ScriptExecutor(Bridge::log, Bridge::error);
        executor.execute(id, userCode, instrumentation);
    }
}