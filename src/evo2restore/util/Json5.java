package evo2restore.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON5 reader and writer for FTB Quests files.
 *
 * Strings and numbers keep their source text, so an unmodified value is written back exactly as read. Objects are
 * {@code LinkedHashMap<String, Object>}, arrays are {@code ArrayList<Object>}, and literals are {@link Boolean} or
 * {@code null}.
 */
public final class Json5 {
    public record Str(String value, String raw) {
        public static Str of(String value) {
            return new Str(value, quote(value));
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public record Num(String raw) {
        public static Num of(double value) {
            if (value == Math.rint(value) && Math.abs(value) < 1e15) return new Num((long) value + ".0");
            return new Num(Double.toString(value));
        }

        public double doubleValue() {
            return Double.parseDouble(raw);
        }
    }

    private Json5() {}

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipSpace();
        Object v = p.value();
        p.skipSpace();
        if (p.pos != text.length()) throw p.error("trailing content");
        return v;
    }

    /** Writes FTB Quests' style: two-space indent, bare keys, trailing commas, no final newline. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 0);
        return sb.toString();
    }

    /** Writes strict JSON with quoted keys, as vanilla datapack loaders require. */
    public static String writeJson(Object value) {
        StringBuilder sb = new StringBuilder();
        writeJson(sb, value, 0);
        return sb.append('\n').toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object v) {
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("expected object, got " + describe(v));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object v) {
        if (v instanceof List<?> l) return (List<Object>) l;
        throw new IllegalArgumentException("expected array, got " + describe(v));
    }

    public static String str(Object v) {
        return v instanceof Str s ? s.value() : null;
    }

    public static List<Map<String, Object>> objects(Map<String, Object> parent, String key) {
        Object v = parent.get(key);
        if (v == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : arr(v)) out.add(obj(o));
        return out;
    }

    public static List<Object> arrayOrCreate(Map<String, Object> parent, String key) {
        return arr(parent.computeIfAbsent(key, k -> new ArrayList<>()));
    }

    public static Object deepCopy(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, val) -> out.put((String) k, deepCopy(val)));
            return out;
        }
        if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>(l.size());
            for (Object o : l) out.add(deepCopy(o));
            return out;
        }
        return v;
    }

    private static String describe(Object v) {
        return v == null ? "null" : v.getClass().getSimpleName();
    }

    private static void write(StringBuilder sb, Object v, int depth) {
        if (v instanceof Map<?, ?> m) {
            if (m.isEmpty()) {
                sb.append("{\n");
                indent(sb, depth);
                sb.append('}');
                return;
            }
            sb.append("{\n");
            for (Map.Entry<?, ?> e : m.entrySet()) {
                indent(sb, depth + 1);
                String key = (String) e.getKey();
                sb.append(isIdentifier(key) ? key : quote(key)).append(": ");
                write(sb, e.getValue(), depth + 1);
                sb.append(",\n");
            }
            indent(sb, depth);
            sb.append('}');
        } else if (v instanceof List<?> l) {
            if (l.isEmpty()) {
                sb.append("[\n");
                indent(sb, depth);
                sb.append(']');
                return;
            }
            sb.append("[\n");
            for (Object o : l) {
                indent(sb, depth + 1);
                write(sb, o, depth + 1);
                sb.append(",\n");
            }
            indent(sb, depth);
            sb.append(']');
        } else {
            sb.append(scalar(v));
        }
    }

    private static void writeJson(StringBuilder sb, Object v, int depth) {
        if (v instanceof Map<?, ?> m) {
            sb.append('{');
            String sep = "\n";
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sb.append(sep);
                indent(sb, depth + 1);
                sb.append(quote((String) e.getKey())).append(": ");
                writeJson(sb, e.getValue(), depth + 1);
                sep = ",\n";
            }
            if (!m.isEmpty()) {
                sb.append('\n');
                indent(sb, depth);
            }
            sb.append('}');
        } else if (v instanceof List<?> l) {
            sb.append('[');
            String sep = "\n";
            for (Object o : l) {
                sb.append(sep);
                indent(sb, depth + 1);
                writeJson(sb, o, depth + 1);
                sep = ",\n";
            }
            if (!l.isEmpty()) {
                sb.append('\n');
                indent(sb, depth);
            }
            sb.append(']');
        } else if (v instanceof Str s) {
            sb.append(quote(s.value()));
        } else {
            sb.append(scalar(v));
        }
    }

    private static String scalar(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof Str s) return s.raw();
        if (v instanceof Num n) return n.raw();
        throw new IllegalArgumentException("cannot write " + describe(v));
    }

    private static void indent(StringBuilder sb, int depth) {
        sb.append("  ".repeat(depth));
    }

    private static boolean isIdentifier(String key) {
        if (key.isEmpty() || !Character.isJavaIdentifierStart(key.charAt(0))) return false;
        for (int i = 1; i < key.length(); i++) {
            if (!Character.isJavaIdentifierPart(key.charAt(i))) return false;
        }
        return true;
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    /** Parses {@code text} and records each container's, string's and number's source range in {@code spans}. */
    static Object parse(String text, java.util.IdentityHashMap<Object, int[]> spans) {
        Parser p = new Parser(text);
        p.spans = spans;
        p.skipSpace();
        Object v = p.value();
        p.skipSpace();
        if (p.pos != p.s.length()) throw p.error("trailing content");
        return v;
    }

    static boolean isBareKey(String key) {
        return isIdentifier(key);
    }

    static String scalarText(Object v, boolean strict) {
        return strict && v instanceof Str s ? quote(s.value()) : scalar(v);
    }

    private static final class Parser {
        private final String s;
        private int pos;
        private java.util.IdentityHashMap<Object, int[]> spans;

        Parser(String s) {
            this.s = s.startsWith("\uFEFF") ? " " + s.substring(1) : s;
        }

        IllegalArgumentException error(String msg) {
            int line = 1;
            for (int i = 0; i < pos && i < s.length(); i++) if (s.charAt(i) == '\n') line++;
            return new IllegalArgumentException("JSON5 " + msg + " at line " + line);
        }

        void skipSpace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (Character.isWhitespace(c) || c == '\uFEFF') {
                    pos++;
                } else if (s.startsWith("//", pos)) {
                    int nl = s.indexOf('\n', pos);
                    pos = nl < 0 ? s.length() : nl + 1;
                } else if (s.startsWith("/*", pos)) {
                    int end = s.indexOf("*/", pos + 2);
                    if (end < 0) throw error("unterminated comment");
                    pos = end + 2;
                } else {
                    return;
                }
            }
        }

        Object value() {
            if (pos >= s.length()) throw error("unexpected end");
            int start = pos;
            char c = s.charAt(pos);
            Object v = switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"', '\'' -> string();
                default -> literal();
            };
            if (spans != null && v != null && !(v instanceof Boolean)) spans.put(v, new int[] {start, pos});
            return v;
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            pos++;
            while (true) {
                skipSpace();
                if (peek() == '}') {
                    pos++;
                    return m;
                }
                String key = peek() == '"' || peek() == '\'' ? string().value() : identifier();
                skipSpace();
                expect(':');
                skipSpace();
                m.put(key, value());
                skipSpace();
                if (peek() == ',') pos++;
                else if (peek() != '}') throw error("expected ',' or '}'");
            }
        }

        List<Object> array() {
            List<Object> l = new ArrayList<>();
            pos++;
            while (true) {
                skipSpace();
                if (peek() == ']') {
                    pos++;
                    return l;
                }
                l.add(value());
                skipSpace();
                if (peek() == ',') pos++;
                else if (peek() != ']') throw error("expected ',' or ']'");
            }
        }

        Str string() {
            int start = pos;
            char q = s.charAt(pos++);
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) throw error("unterminated string");
                char c = s.charAt(pos++);
                if (c == q) break;
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'v' -> sb.append('\u000B');
                    case '0' -> sb.append('\0');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    case 'x' -> {
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 2), 16));
                        pos += 2;
                    }
                    case '\r' -> {
                        if (peek() == '\n') pos++;
                    }
                    case '\n', ' ', ' ' -> {}
                    default -> sb.append(e);
                }
            }
            return new Str(sb.toString(), s.substring(start, pos));
        }

        String identifier() {
            int start = pos;
            while (pos < s.length() && (Character.isJavaIdentifierPart(s.charAt(pos)))) pos++;
            if (start == pos) throw error("expected key");
            return s.substring(start, pos);
        }

        Object literal() {
            int start = pos;
            while (pos < s.length() && ",:]}/".indexOf(s.charAt(pos)) < 0 && !Character.isWhitespace(s.charAt(pos))) pos++;
            String word = s.substring(start, pos);
            switch (word) {
                case "true":
                    return Boolean.TRUE;
                case "false":
                    return Boolean.FALSE;
                case "null":
                    return null;
                default:
                    if (word.isEmpty()) throw error("unexpected character '" + s.charAt(pos) + "'");
                    return new Num(word);
            }
        }

        char peek() {
            if (pos >= s.length()) throw error("unexpected end");
            return s.charAt(pos);
        }

        void expect(char c) {
            if (peek() != c) throw error("expected '" + c + "'");
            pos++;
        }
    }
}
