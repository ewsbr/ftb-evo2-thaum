package evo2restore;

import java.util.LinkedHashMap;
import java.util.Map;

/** Computes the order_index changes that put one chapter directly before another in its group. */
final class ChapterOrder {
    /** {@code moved} is the new index of the placed chapter, or null if it stays; {@code shifted} maps the others. */
    record Placement(Long moved, Map<String, Long> shifted) {}

    private ChapterOrder() {}

    /**
     * Places a chapter at index {@code current} before {@code anchor}. {@code group} maps every other chapter in the
     * group, including the anchor, to its order_index. Without the anchor, the chapter keeps its index and only
     * collisions with it are shifted.
     */
    static Placement before(double current, Map<String, Double> group, String anchor) {
        Double target = group.get(anchor);
        double slot = current;
        Long moved = null;
        if (target != null && (current >= target
                || group.values().stream().anyMatch(o -> o >= current && o < target))) {
            slot = target;
            if (slot != current) moved = (long) slot;
        }
        Map<String, Long> shifted = new LinkedHashMap<>();
        final double s = slot;
        if (group.values().stream().anyMatch(o -> o == s)) {
            group.forEach((name, o) -> {
                if (o >= s) shifted.put(name, (long) (o + 1));
            });
        }
        return new Placement(moved, shifted);
    }
}
