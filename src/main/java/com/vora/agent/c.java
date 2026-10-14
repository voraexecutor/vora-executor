package com.vora.agent;

import java.lang.instrument.Instrumentation;

public class c {

    public static void execute(String id, String code, Instrumentation instrumentation) {
        try {
            ScriptRunner.execute(id, code, instrumentation);
        } catch (Throwable t) {
            Bridge.error(t);
        }
    }

    public static void executeMixin(String id, String code, Instrumentation instrumentation) {
        try {
            Hook.execute(id, code, instrumentation, Bridge::log, Bridge::error);
        } catch (Throwable t) {
            Bridge.error(t);
        }
    }
}