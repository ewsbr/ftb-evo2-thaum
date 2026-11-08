package evo2restore;

import evo2restore.util.Json5;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds a vertical offset at which restored quest-map entries overlap nothing already in the chapter. */
final class Layout {
    private static final double STEP = 0.5;
    private static final double MAX_OFFSET = 64;
    private static final double MARGIN = 0.25;

    private record Box(double x, double y, double w, double h) {
        boolean overlaps(Box o, double dy) {
            return Math.abs(x - o.x) * 2 < w + o.w + MARGIN * 2 && Math.abs(y + dy - o.y) * 2 < h + o.h + MARGIN * 2;
        }
    }

    private Layout() {}

    static double y(Map<String, Object> o) {
        return num(o.get("y"), 0);
    }

    /** The smallest offset, trying 0, +0.5, -0.5, +1, ... where no placed entry overlaps an existing one. */
    static double offset(List<Map<String, Object>> placed, Map<String, Object> chapter, Set<String> presentIds) {
        List<Box> moving = new ArrayList<>();
        for (Map<String, Object> o : placed) {
            if (!presentIds.contains(Restore.id(o))) moving.add(box(o, chapter));
        }
        List<Box> fixed = new ArrayList<>();
        for (String list : List.of("quests", "images", "quest_links")) {
            for (Map<String, Object> o : Json5.objects(chapter, list)) fixed.add(box(o, chapter));
        }
        for (double d = 0; d <= MAX_OFFSET; d += STEP) {
            if (fits(moving, fixed, d)) return d;
            if (d > 0 && fits(moving, fixed, -d)) return -d;
        }
        return 0;
    }

    private static boolean fits(List<Box> moving, List<Box> fixed, double dy) {
        for (Box m : moving) for (Box f : fixed) if (m.overlaps(f, dy)) return false;
        return true;
    }

    private static Box box(Map<String, Object> o, Map<String, Object> chapter) {
        double x = num(o.get("x"), 0);
        double y = y(o);
        if (o.containsKey("width") || o.containsKey("height")) {
            return new Box(x, y, num(o.get("width"), 1), num(o.get("height"), 1));
        }
        double size = num(o.get("size"), num(chapter.get("default_quest_size"), 1));
        return new Box(x, y, size, size);
    }

    private static double num(Object v, double fallback) {
        return v instanceof Json5.Num n ? n.doubleValue() : fallback;
    }
}
