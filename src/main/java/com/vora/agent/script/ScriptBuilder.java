package com.vora.agent.script;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;






public final class ScriptBuilder {

    private ScriptBuilder() {
    }

    public static String buildSource(String body) {
        return buildSource(body, ScriptClassConstants.MC_CLIENT, "1.21");
    }

    public static String buildSource(String body, String clientClass) {
        return buildSource(body, clientClass, "1.21");
    }








    public static String buildSource(String body, String clientClass, String screenApi) {
        StringBuilder imports = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String normalized = body.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) {
            if (line.trim().startsWith("import ")) {
                imports.append(line.trim()).append('\n');
            } else {
                code.append(line).append('\n');
            }
        }



        code = new StringBuilder(injectScreenImplementations(code.toString(), screenApi));

        String client = simpleName(clientClass);
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("package org.client.executor.script;");
        joiner.add("");
        joiner.add("import " + clientClass + ";");
        joiner.add("import java.util.*;");
        joiner.add(imports.toString());
        joiner.add("");
        joiner.add("public class Script implements Runnable {");
        joiner.add("    public static java.util.function.Consumer<String> LOG;");
        joiner.add("    public static java.util.function.Consumer<Throwable> ERR;");
        joiner.add("    public static final java.util.concurrent.CountDownLatch DONE =");
        joiner.add("            new java.util.concurrent.CountDownLatch(1);");
        joiner.add("");
        joiner.add("    public static void log(String message) {");
        joiner.add("        if (LOG != null) LOG.accept(String.valueOf(message));");
        joiner.add("    }");
        joiner.add("");
        joiner.add("    @Override");
        joiner.add("    public void run() {");
        joiner.add("        try {");
        joiner.add("            body();");
        joiner.add("        } catch (Throwable t) {");
        joiner.add("            if (ERR != null) ERR.accept(t);");
        joiner.add("        } finally {");
        joiner.add("            DONE.countDown();");
        joiner.add("        }");
        joiner.add("    }");
        joiner.add("");
        joiner.add("    private void body() throws Throwable {");
        joiner.add("        " + client + " mc = " + client + ".getInstance();");
        joiner.add(code.toString());
        joiner.add("    }");
        joiner.add("");
        joiner.add("    public static void dispatch() {");
        joiner.add("        " + client + " mc = " + client + ".getInstance();");
        joiner.add("        if (mc == null) {");
        joiner.add("            new Script().run();");
        joiner.add("        } else {");
        joiner.add("            mc.execute(new Script());");
        joiner.add("        }");
        joiner.add("    }");
        joiner.add("}");

        return joiner.toString();
    }






    public static String buildHookSource(String body, String className) {
        return buildHookSource(body, className, ScriptClassConstants.MC_CLIENT, "1.21");
    }

    public static String buildHookSource(String body, String className, String clientClass) {
        return buildHookSource(body, className, clientClass, "1.21");
    }

    public static String buildHookSource(String body, String className, String clientClass, String screenApi) {
        StringBuilder imports = new StringBuilder();
        StringBuilder code = new StringBuilder();

        String normalized = body.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) {
            if (line.trim().startsWith("import ")) {
                imports.append(line.trim()).append('\n');
            } else {
                code.append(line).append('\n');
            }
        }

        code = new StringBuilder(injectScreenImplementations(code.toString(), screenApi));

        String client = simpleName(clientClass);
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("package org.client.executor.script;");
        joiner.add("");
        joiner.add("import " + clientClass + ";");
        joiner.add("import java.util.*;");
        joiner.add(imports.toString());
        joiner.add("");
        joiner.add("public class " + className + " {");
        joiner.add("    public static java.util.function.Consumer<String> LOG;");
        joiner.add("    public static java.util.function.Consumer<Throwable> ERR;");
        joiner.add("    public static void log(String message) {");
        joiner.add("        if (LOG != null) LOG.accept(String.valueOf(message));");
        joiner.add("    }");
        joiner.add("    public static void run() {");
        joiner.add("        try {");
        joiner.add("            body();");
        joiner.add("        } catch (Throwable t) {");
        joiner.add("            if (ERR != null) ERR.accept(t);");
        joiner.add("        }");
        joiner.add("    }");
        joiner.add("    private static void body() throws Throwable {");
        joiner.add("        " + client + " mc = " + client + ".getInstance();");
        joiner.add(code.toString());
        joiner.add("    }");
        joiner.add("}");
        return joiner.toString();
    }

    private static String simpleName(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot >= 0 ? binaryName.substring(dot + 1) : binaryName;
    }

    private static String injectScreenImplementations(String code, String screenApi) {
        StringBuilder result = new StringBuilder(code);


        int index = 0;
        while (index < result.length()) {
            int classIndex = result.indexOf("class ", index);
            if (classIndex == -1) break;

            int extendsIndex = extendsKeyword(result, classIndex);
            if (extendsIndex == -1 || !isScreenSupertype(result, extendsIndex)) {
                index = classIndex + 6;
                continue;
            }

            int braceIndex = result.indexOf("{", extendsIndex);
            if (braceIndex == -1) break;

            int endIndex = findMatchingBrace(result, braceIndex);
            if (endIndex != -1) {
                String body = result.substring(braceIndex + 1, endIndex - 1);
                String implementations = getScreenImplementations(body, screenApi);
                if (!implementations.isEmpty()) {
                    result.insert(endIndex - 1, "\n" + implementations + "\n");
                    index = endIndex + implementations.length() + 1;
                } else {
                    index = endIndex + 1;
                }
            } else {
                index = extendsIndex + 8;
            }
        }


        index = 0;
        while (index < result.length()) {
            int newIndex = nextScreenInstantiation(result, index);
            if (newIndex == -1) break;

            int parenCount = 0;
            int braceIndex = -1;
            for (int i = newIndex + 4; i < result.length(); i++) {
                char c = result.charAt(i);
                if (c == '(') parenCount++;
                else if (c == ')') parenCount--;
                else if (c == '{' && parenCount == 0) {
                    braceIndex = i;
                    break;
                }
            }

            if (braceIndex == -1) {
                index = newIndex + 4;
                continue;
            }

            int endIndex = findMatchingBrace(result, braceIndex);
            if (endIndex != -1) {
                String body = result.substring(braceIndex + 1, endIndex - 1);
                String implementations = getScreenImplementations(body, screenApi);
                if (!implementations.isEmpty()) {
                    result.insert(endIndex - 1, "\n" + implementations + "\n");
                    index = endIndex + implementations.length() + 1;
                } else {
                    index = endIndex + 1;
                }
            } else {
                index = newIndex + 4;
            }
        }

        return result.toString();
    }

    private static int extendsKeyword(StringBuilder result, int classIndex) {
        int limit = Math.min(result.length(), classIndex + 80);
        int found = result.indexOf("extends", classIndex + 6);
        return (found != -1 && found <= limit) ? found : -1;
    }

    private static boolean isScreenSupertype(StringBuilder result, int extendsIndex) {
        int i = extendsIndex + "extends".length();
        while (i < result.length() && Character.isWhitespace(result.charAt(i))) {
            i++;
        }
        if (i >= result.length()) return false;
        String rest = result.substring(i);
        for (String type : new String[]{
                "net.minecraft.client.gui.screen.Screen",
                "net.minecraft.class_437",
                "Screen"}) {
            if (rest.startsWith(type)) {
                int after = type.length();
                return after >= rest.length() || !isIdentifierChar(rest.charAt(after));
            }
        }
        return false;
    }

    private static int nextScreenInstantiation(StringBuilder result, int from) {
        int best = -1;
        for (String candidate : new String[]{
                "new net.minecraft.client.gui.screen.Screen",
                "new net.minecraft.class_437",
                "new Screen"}) {
            int found = result.indexOf(candidate, from);
            if (found != -1 && (best == -1 || found < best)) best = found;
        }
        return best;
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static int findMatchingBrace(StringBuilder sb, int openBraceIndex) {
        int braceCount = 1;
        int currentIndex = openBraceIndex + 1;
        while (currentIndex < sb.length() && braceCount > 0) {
            char c = sb.charAt(currentIndex);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
            }
            currentIndex++;
        }
        return braceCount == 0 ? currentIndex : -1;
    }

    private static String getScreenImplementations(String body, String screenApi) {





        StringBuilder sb = new StringBuilder();
        if ("legacy".equals(screenApi)) {
            sb.append("// ===== Required abstract method implementations for Screen =====\n");
            if (!isDefined(body, "render(")) {
                sb.append("@Override public void render(net.minecraft.client.util.math.MatrixStack matrices, int mouseX, int mouseY, float delta) {\n");
                sb.append("    try { this.renderBackground(matrices, mouseX, mouseY, delta); } catch (Throwable ignored) {}\n");
                sb.append("}\n");
            }
            return sb.toString();
        }
        sb.append("// ===== Required abstract method implementations for Screen =====\n");
        if (!isDefined(body, "isFocused(")) {
            sb.append("@Override public boolean isFocused() { return this.focused; }\n");
        }
        if (!containsMethod(body, "setFocused(boolean")) {
            sb.append("@Override public void setFocused(boolean focused) { this.focused = focused; }\n");
        }


        if ("1.21".equals(screenApi)) {
            if (!containsMethod(body, "setFocused(net.minecraft") && !containsMethod(body, "setFocused(Element")) {
                sb.append("@Override public void setFocused(net.minecraft.client.gui.Element element) { this.focusedElement = element; }\n");
            }
            if (!isDefined(body, "getFocused(")) {
                sb.append("@Override public net.minecraft.client.gui.Element getFocused() { return this.focusedElement; }\n");
            }
        }
        if (!isDefined(body, "setDragging(")) {
            sb.append("@Override public void setDragging(boolean dragging) { this.dragging = dragging; }\n");
        }
        if (!isDefined(body, "isDragging(")) {
            sb.append("@Override public boolean isDragging() { return this.dragging; }\n");
        }
        if (!isDefined(body, "children(")) {
            sb.append("@Override public java.util.List<? extends net.minecraft.client.gui.Element> children() { return this.childrenList; }\n");
        }
        if (!isDefined(body, "addDrawableChild(")) {
            sb.append("@Override protected <T extends net.minecraft.client.gui.Element & net.minecraft.client.gui.Drawable & net.minecraft.client.gui.Selectable> T addDrawableChild(T drawableElement) {\n");
            sb.append("    this.childrenList.add(drawableElement);\n");
            sb.append("    return super.addDrawableChild(drawableElement);\n");
            sb.append("}\n");
        }
        if (!isDefined(body, "render(")) {
            sb.append("@Override public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {\n");
            sb.append("    try { this.renderBackground(context, mouseX, mouseY, delta); } catch (Throwable ignored) {}\n");
            sb.append("    for (net.minecraft.client.gui.Element child : this.childrenList) {\n");
            sb.append("        if (child instanceof net.minecraft.client.gui.Drawable) {\n");
            sb.append("            ((net.minecraft.client.gui.Drawable) child).render(context, mouseX, mouseY, delta);\n");
            sb.append("        }\n");
            sb.append("    }\n");
            sb.append("}\n");
        }
        sb.append("\n");
        sb.append("// State fields (do NOT declare x/y/width/height - they shadow Screen's own fields)\n");
        return sb.toString() + buildStateFields(body);
    }







    private static boolean isDefined(String body, String fragment) {
        int search = 0;
        while (true) {
            int index = body.indexOf(fragment, search);
            if (index == -1) return false;
            search = index + 1;

            int depth = 0;
            int i = index + fragment.length() - 1;
            for (; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            if (i >= body.length()) return false;
            int j = i + 1;
            while (j < body.length() && Character.isWhitespace(body.charAt(j))) {
                j++;
            }
            if (j < body.length() && body.charAt(j) == '{') return true;
        }
    }

    private static boolean containsMethod(String body, String signature) {
        int search = 0;
        while (true) {
            int index = body.indexOf(signature, search);
            if (index == -1) return false;
            int before = index - 1;
            char prev = before >= 0 ? body.charAt(before) : ' ';
            if (!Character.isLetterOrDigit(prev) && prev != '_' && prev != '$') return true;
            search = index + 1;
        }
    }

    private static String buildStateFields(String body) {
        StringBuilder sb = new StringBuilder();
        if (!containsState(body, "focused") && !containsState(body, "boolean focused")) {
            sb.append("private boolean focused = false;\n");
        }
        if (!containsState(body, "focusedElement")) {
            sb.append("private net.minecraft.client.gui.Element focusedElement = null;\n");
        }
        if (!containsState(body, "dragging")) {
            sb.append("private boolean dragging = false;\n");
        }
        if (!containsState(body, "childrenList")) {
            sb.append("private final java.util.List<net.minecraft.client.gui.Element> childrenList = new java.util.ArrayList<>();\n");
        }
        return sb.toString();
    }

    private static boolean containsState(String body, String name) {


        int search = 0;
        while (true) {
            int index = body.indexOf(name, search);
            if (index == -1) return false;
            int before = index - 1;
            char prev = before >= 0 ? body.charAt(before) : ' ';
            int afterIdx = index + name.length();
            char next = afterIdx < body.length() ? body.charAt(afterIdx) : ' ';
            if (!Character.isLetterOrDigit(prev) && prev != '_' && prev != '$'
                    && !Character.isLetterOrDigit(next) && next != '_' && next != '$') {
                return true;
            }
            search = index + 1;
        }
    }
}