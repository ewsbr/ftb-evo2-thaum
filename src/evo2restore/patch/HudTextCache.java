package evo2restore.patch;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copied into Iron Jetpacks, where the HUD passes each text component it builds through {@link #intern}.
 *
 * The HUD builds four new components every frame. Minecraft caches a component's bidi-reordered text on the instance,
 * so a new instance per frame reorders the text again each frame. Returning the earlier equal instance lets that
 * cache hit. Only this one class file is copied, so it uses java.base and no nested or anonymous classes.
 */
public final class HudTextCache {
    private static final int SIZE = 32;
    private static final Map<Object, Object> SEEN = new LinkedHashMap<>(SIZE * 2, 0.75f, true);

    private HudTextCache() {}

    public static Object intern(Object component) {
        Object seen = SEEN.get(component);
        if (seen != null) return seen;
        SEEN.put(component, component);
        if (SEEN.size() > SIZE) {
            Iterator<Object> eldest = SEEN.keySet().iterator();
            eldest.next();
            eldest.remove();
        }
        return component;
    }
}
