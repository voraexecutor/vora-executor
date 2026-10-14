package com.vora.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;





public final class Json {

    private Json() {
    }

    public static String escape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 4);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String stringify(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return escape((String) obj);
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Iterable<?>) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) obj) {
                if (!first) sb.append(',');
                sb.append(stringify(item));
                first = false;
            }
            sb.append(']');
            return sb.toString();
        }
        if (obj instanceof Map<?, ?>) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(',');
                sb.append(escape(e.getKey().toString())).append(':').append(stringify(e.getValue()));
                first = false;
            }
            sb.append('}');
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                sb.append(stringify(java.lang.reflect.Array.get(obj, i)));
            }
            sb.append(']');
            return sb.toString();
        }

        return escape(obj.toString());
    }

    public static Object parse(String json) {
        json = json.trim();
        if (json.isEmpty()) return null;
        char first = json.charAt(0);
        if (first == '{') return parseObject(json);
        if (first == '[') return parseArray(json);
        if (first == '"') return unescape(json);
        if ("true".equalsIgnoreCase(json)) return true;
        if ("false".equalsIgnoreCase(json)) return false;
        if ("null".equalsIgnoreCase(json)) return null;
        try {
            if (json.contains(".")) return Double.parseDouble(json);
            return Long.parseLong(json);
        } catch (NumberFormatException e) {
            return json;
        }
    }

    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        int i = 1;
        while (i < json.length() - 1) {
            i = skipWhitespace(json, i);
            if (json.charAt(i) == '}') break;
            String key = parseString(json, i);
            i += key.length() + 2;
            i = skipWhitespace(json, i);
            if (json.charAt(i) != ':') throw new IllegalArgumentException("Expected : at " + i);
            i = skipWhitespace(json, i + 1);
            int valEnd = findValueEnd(json, i);
            String valJson = json.substring(i, valEnd);
            map.put(key, parse(valJson));
            i = skipWhitespace(json, valEnd);
            if (i < json.length() && json.charAt(i) == ',') i = skipWhitespace(json, i + 1);
        }
        return map;
    }

    private static List<Object> parseArray(String json) {
        List<Object> list = new ArrayList<>();
        int i = 1;
        while (i < json.length() - 1) {
            i = skipWhitespace(json, i);
            if (json.charAt(i) == ']') break;
            int valEnd = findValueEnd(json, i);
            String valJson = json.substring(i, valEnd);
            list.add(parse(valJson));
            i = skipWhitespace(json, valEnd);
            if (i < json.length() && json.charAt(i) == ',') i = skipWhitespace(json, i + 1);
        }
        return list;
    }

    private static String parseString(String json, int start) {
        if (json.charAt(start) != '"') throw new IllegalArgumentException("Expected string at " + start);
        StringBuilder sb = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                i++;
                c = json.charAt(i);
                switch (c) {
                    case '"', '\\', '/' -> sb.append(c);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static int findValueEnd(String json, int start) {
        if (start >= json.length()) return start;
        char c = json.charAt(start);
        if (c == '"') {
            for (int i = start + 1; i < json.length(); i++) {
                if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') return i + 1;
            }
        } else if (c == '{') {
            int depth = 1;
            for (int i = start + 1; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (ch == '{') depth++;
                else if (ch == '}') if (--depth == 0) return i + 1;
            }
        } else if (c == '[') {
            int depth = 1;
            for (int i = start + 1; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (ch == '[') depth++;
                else if (ch == ']') if (--depth == 0) return i + 1;
            }
        } else {
            for (int i = start; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (ch == ',' || ch == ']' || ch == '}') return i;
            }
        }
        return json.length();
    }

    private static String unescape(String s) {
        if (s.length() < 2 || s.charAt(0) != '"') return s;
        return parseString(s, 0);
    }
}