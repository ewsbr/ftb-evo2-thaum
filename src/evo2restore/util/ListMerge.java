package evo2restore.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ListMerge {
    private ListMerge() {}

    /**
     * Returns {@code current} plus every {@code wanted} element of {@code old} whose identity it lacks. Each restored
     * element goes right after the nearest earlier {@code old} element that the result contains, so it lands where it
     * was in {@code old}. Without such an element it goes before the nearest later one, or at the end.
     */
    public static <T> List<T> restore(List<T> old, List<T> current, Predicate<T> wanted, Function<T, ?> identity) {
        List<T> out = new ArrayList<>(current);
        Set<Object> present = new HashSet<>();
        for (T t : current) present.add(identity.apply(t));
        for (int i = 0; i < old.size(); i++) {
            T item = old.get(i);
            if (!wanted.test(item) || present.contains(identity.apply(item))) continue;
            out.add(position(old, i, out, identity), item);
            present.add(identity.apply(item));
        }
        return out;
    }

    private static <T> int position(List<T> old, int index, List<T> out, Function<T, ?> identity) {
        for (int j = index - 1; j >= 0; j--) {
            int at = indexOf(out, identity.apply(old.get(j)), identity);
            if (at >= 0) return at + 1;
        }
        for (int j = index + 1; j < old.size(); j++) {
            int at = indexOf(out, identity.apply(old.get(j)), identity);
            if (at >= 0) return at;
        }
        return out.size();
    }

    private static <T> int indexOf(List<T> list, Object id, Function<T, ?> identity) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(identity.apply(list.get(i)), id)) return i;
        }
        return -1;
    }
}
