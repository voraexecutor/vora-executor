package com.vora.agent;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public final class Bridge {

    private static Socket socket;
    private static PrintWriter out;

    private Bridge() {
    }

    public static synchronized void connect(String host, int port) {
        if (port <= 0) return;
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (Exception e) {
            System.err.println("[executor] log bridge unavailable: " + e);
            socket = null;
            out = null;
        }
    }

    public static void log(String message) {
        if (out != null) {
            out.println(message);
        } else {
            System.out.println("[executor] " + message);
        }
    }

    public static void error(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        if (out != null) {
            for (String line : sw.toString().split("\n")) out.println(line);
        } else {
            System.err.println(sw);
        }
    }

    public static synchronized void close() {
        try {
            if (out != null) out.flush();
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        out = null;
        socket = null;
    }
}
