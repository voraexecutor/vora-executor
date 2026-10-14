package com.vora.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;


public class a {

    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "list" -> {
                for (String[] process : b.listProcesses()) {
                    System.out.println(process[0] + "  " + process[1]);
                }
            }
            case "exec", "attach", "load" -> {
                String pid = args.length > 1 ? args[1] : "auto";
                String source = args.length > 2 ? readSource(args[2]) : "";
                String options = "cmd=exec" + encodeSource(source);
                run(pid, options);
            }
            case "remove", "detach" -> {
                String pid = args.length > 1 ? args[1] : "auto";
                run(pid, "cmd=remove");
            }
            default -> usage();
        }
    }

    private static void run(String pid, String options) {
        boolean ok = pid.equalsIgnoreCase("auto")
                ? b.attachToMinecraft(options)
                : b.attach(pid, options);
        System.out.println(ok ? "ok" : "failed");
    }

    private static String encodeSource(String source) {
        if (source == null || source.isEmpty()) return "";
        return ";code=" + Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8));
    }

    private static String readSource(String path) {
        try {
            if (path.equals("-")) {
                return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            }
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("could not read " + path + ": " + e.getMessage());
            return "";
        }
    }

    private static void usage() {
        System.out.println("""
                Minecraft Executor CLI
                  list                          list attachable JVMs
                  exec auto [file.java]         attach to Minecraft and run a file (or nothing)
                  exec <pid> [file.java]        attach to a specific pid and run a file
                  remove auto|<pid>             detach the agent
                No arguments -> launches the GUI.""");
    }
}
