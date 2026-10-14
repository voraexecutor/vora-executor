package com.vora.executor;

import com.vora.agent.b;
import com.vora.common.VoraTemp;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Injector {

    private Injector() {
    }

    public static String agentJar() {
        return b.getAgentPath();
    }

    public static void execute(String pid, String id, String code, String mode, Consumer<String> sink) throws Exception {
        String agentJar = b.getAgentPath();
        if (agentJar == null) {
            throw new IllegalStateException(
                    "Agent jar not found. Build it with: gradlew jar  (then run build/libs/executor-1.0.jar)");
        }

        LogServer server = new LogServer(sink);
        server.start();

        Path codeFile = VoraTemp.mappingsDir().resolve("vora-code-" + System.currentTimeMillis() + ".java");
        Files.writeString(codeFile, code, StandardCharsets.UTF_8);

        String options = "cmd=exec"
                + ";id=" + id
                + ";mode=" + (mode == null || mode.isBlank() ? "code" : mode)
                + ";port=" + server.getPort()
                + ";codefile=" + encodePath(codeFile.toAbsolutePath().toString());

        sink.accept("[client] attaching to pid " + pid + " ...");

        boolean ok;
        try {
            ok = b.attach(pid, options, sink);
        } finally {
            try {
                Files.deleteIfExists(codeFile);
            } catch (Exception ignored) {
            }
        }
        if (!ok) {
            server.close();
            throw new IllegalStateException("attach failed.");
        }
    }





    public static String eval(String pid, String code, int timeoutSeconds) throws Exception {
        String agentJar = b.getAgentPath();
        if (agentJar == null) {
            throw new IllegalStateException("Agent jar not found.");
        }

        final String[] result = { null };
        final CountDownLatch latch = new CountDownLatch(1);
        final String id = "eval_" + System.currentTimeMillis();

        Consumer<String> captureSink = line -> {

            if (line != null && !line.trim().isEmpty() 
                    && !line.startsWith("[client]") 
                    && !line.startsWith("[VORA]")) {
                result[0] = line.trim();
                latch.countDown();
            }
        };

        LogServer server = new LogServer(captureSink);
        server.start();

        Path codeFile = VoraTemp.mappingsDir().resolve("vora-eval-" + System.currentTimeMillis() + ".java");
        Files.writeString(codeFile, code, StandardCharsets.UTF_8);

        String options = "cmd=exec"
                + ";id=" + id
                + ";mode=code"
                + ";port=" + server.getPort()
                + ";codefile=" + encodePath(codeFile.toAbsolutePath().toString());

        boolean ok;
        try {
            ok = b.attach(pid, options, captureSink);
        } finally {
            try {
                Files.deleteIfExists(codeFile);
            } catch (Exception ignored) {
            }
        }
        if (!ok) {
            server.close();
            throw new IllegalStateException("eval attach failed.");
        }


        boolean gotResult = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        server.close();

        return result[0];
    }

    private static String encodePath(String path) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(path.getBytes(StandardCharsets.UTF_8));
    }
}