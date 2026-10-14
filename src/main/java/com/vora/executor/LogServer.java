package com.vora.executor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;


public final class LogServer implements Runnable {

    private final ServerSocket server;
    private final Consumer<String> sink;
    private final Thread thread;

    public LogServer(Consumer<String> sink) throws Exception {
        this.sink = sink;
        this.server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        this.thread = new Thread(this, "executor-log-server");
        this.thread.setDaemon(true);
    }

    public int getPort() {
        return server.getLocalPort();
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        try (Socket socket = server.accept();
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                sink.accept(line);
            }
        } catch (Exception e) {
            
        } finally {
            close();
        }
    }

    public void close() {
        try {
            server.close();
        } catch (Exception ignored) {
        }
    }
}
