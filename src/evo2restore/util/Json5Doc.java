package evo2restore.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A JSON or JSON5 file edited in place.
 *
 * Callers change the parsed tree, then report each container whose children they added, removed or reordered with
 * {@link #touched}, and each value they put where another value was with {@link #replaced}. {@link #render} keeps
 * the source text of everything else, comments and formatting included, and writes only those parts afresh.
 */
public final class Json5Doc {
    private final String text;
    private final Object root;
    private final IdentityHashMap<Object, int[]> spans = new IdentityHashMap<>();
    private final Set<Object> touched = Collections.newSetFromMap(new IdentityHashMap<>());
    private final IdentityHashMap<Object, int[]> replacedSpans = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Object> replacedValues = new IdentityHashMap<>();
    private final boolean strict;

    private Json5Doc(String text, boolean strict) {
        this.text = text;
        this.strict = strict;
        this.root = Json5.parse(text, spans);
    }

    /** {@code strict} writes plain JSON with quoted keys and no trailing commas. */
    public static Json5Doc parse(String text, boolean strict) {
        return new Json5Doc(text, strict);
    }

    public Object root() {
        return root;
    }

    public void touched(Object container) {
        touched.add(container);
    }

    public void replaced(Object oldValue, Object newValue) {
        int[] span = spans.get(oldValue);
        if (span == null) return;
        replacedSpans.put(newValue, span);
        replacedValues.put(newValue, oldValue);
    }

    public String render() {
        if (!dirty(root, new IdentityHashMap<>())) return text;
        int[] span = spans.get(root);
        return text.substring(0, span[0]) + render(root, lineIndent(span[0]), false) + text.substring(span[1]);
    }

    private int[] span(Object v) {
        int[] s = spans.get(v);
        return s != null ? s : replacedSpans.get(v);
    }

    private boolean dirty(Object v, IdentityHashMap<Object, Boolean> memo) {
        Boolean known = memo.get(v);
        if (known != null) return known;
        boolean d = touched.contains(v) || !spans.containsKey(v);
        if (!d && v instanceof Map<?, ?> m) for (Object c : m.values()) d |= c != null && !(c instanceof Boolean) && dirty(c, memo);
        if (!d && v instanceof List<?> l) for (Object c : l) d |= c != null && !(c instanceof Boolean) && dirty(c, memo);
        memo.put(v, d);
        return d;
    }

    private String render(Object v, String indent, boolean compact) {
        if (v == null || v instanceof Boolean) return String.valueOf(v);
        int[] own = spans.get(v);
        if (own != null && !dirty(v, new IdentityHashMap<>())) return text.substring(own[0], own[1]);
        if (!(v instanceof Map<?, ?>) && !(v instanceof List<?>)) return Json5.scalarText(v, strict);
        int[] at = span(v);
        if (own != null && !touched.contains(v) && childrenHaveSpans(v)) return splice(v, own);
        boolean oneLine = compact || (at != null && text.substring(at[0], at[1]).indexOf('\n') < 0);
        return fresh(v, indent, oneLine, at);
    }

    private boolean childrenHaveSpans(Object v) {
        for (Object c : children(v)) if (c != null && !(c instanceof Boolean) && span(c) == null) return false;
        return true;
    }

    /** The container's own text with each changed child's range rewritten. */
    private String splice(Object v, int[] own) {
        List<Object> kids = new ArrayList<>(children(v));
        kids.removeIf(c -> c == null || c instanceof Boolean || !dirty(c, new IdentityHashMap<>()));
        kids.sort((a, b) -> Integer.compare(span(a)[0], span(b)[0]));
        StringBuilder sb = new StringBuilder();
        int pos = own[0];
        for (Object c : kids) {
            int[] s = span(c);
            sb.append(text, pos, s[0]);
            sb.append(render(c, lineIndent(s[0]), text.substring(s[0], s[1]).indexOf('\n') < 0));
            pos = s[1];
        }
        return sb.append(text, pos, own[1]).toString();
    }

    private String fresh(Object v, String indent, boolean oneLine, int[] at) {
        boolean isMap = v instanceof Map<?, ?>;
        List<Object> kids = children(v);
        String open = isMap ? "{" : "[";
        String close = isMap ? "}" : "]";
        if (kids.isEmpty()) return open + close;
        String unit = unit(kids, indent);
        boolean kidsCompact = compactChildren(kids) || compactChildren(children(replacedValues.get(v)));
        boolean trailing = !strict && (at == null || trailingComma(at));
        List<String> parts = new ArrayList<>();
        List<String> keys = isMap ? new ArrayList<>(((Map<?, ?>) v).keySet().stream().map(String::valueOf).toList()) : null;
        for (int i = 0; i < kids.size(); i++) {
            Object c = kids.get(i);
            String body = render(c, indent + unit, oneLine || kidsCompact);
            if (isMap) body = key(keys.get(i)) + ": " + body;
            parts.add(body);
        }
        if (oneLine) {
            String inner = String.join(", ", parts);
            return isMap ? "{ " + inner + " }" : "[" + inner + "]";
        }
        StringBuilder sb = new StringBuilder(open).append('\n');
        for (int i = 0; i < parts.size(); i++) {
            sb.append(indent).append(unit).append(parts.get(i));
            if (i < parts.size() - 1 || trailing) sb.append(',');
            sb.append('\n');
        }
        return sb.append(indent).append(close).toString();
    }

    /** Whether the children that have source text are all containers written on one line. */
    private boolean compactChildren(List<Object> kids) {
        boolean any = false;
        for (Object c : kids) {
            if (!(c instanceof Map<?, ?>) && !(c instanceof List<?>)) continue;
            int[] s = spans.get(c);
            if (s == null) continue;
            if (text.substring(s[0], s[1]).indexOf('\n') >= 0) return false;
            any = true;
        }
        return any;
    }

    private String key(String k) {
        return strict || !Json5.isBareKey(k) ? Json5.quote(k) : k;
    }

    /** The indent step the file uses inside this container, from its first child that has source text. */
    private String unit(List<Object> kids, String indent) {
        for (Object c : kids) {
            int[] s = c == null || c instanceof Boolean ? null : spans.get(c);
            if (s == null) continue;
            String own = lineIndent(s[0]);
            if (own.length() > indent.length() && own.startsWith(indent)) return own.substring(indent.length());
        }
        return indent.contains("\t") ? "\t" : "  ";
    }

    private boolean trailingComma(int[] at) {
        int i = at[1] - 2;
        while (i > at[0] && Character.isWhitespace(text.charAt(i))) i--;
        return text.charAt(i) == ',';
    }

    private String lineIndent(int offset) {
        int start = text.lastIndexOf('\n', offset - 1) + 1;
        int end = start;
        while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) end++;
        return text.substring(start, Math.min(end, offset));
    }

    private static List<Object> children(Object v) {
        if (v instanceof Map<?, ?> m) return new ArrayList<>(m.values());
        if (v instanceof List<?> l) return new ArrayList<>(l);
        return List.of();
    }
}
