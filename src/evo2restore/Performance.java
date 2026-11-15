package evo2restore;

import evo2restore.util.Asm;
import evo2restore.util.FunctionGuard;
import evo2restore.util.Hashes;
import evo2restore.util.Json5;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * CPU fixes for FTB Evolution 2 that do not depend on Thaumaturge. PERFORMANCE.md describes each problem.
 *
 * Every fix is generated from the installed mods, so a mod update that changes the patched code skips its fix
 * instead of breaking the mod.
 */
final class Performance {
    static final String DATAPACK = "datapacks/evo2-performance/";
    static final String WEB_SERVER_CONFIG = "kubejs/config/web_server.json";
    private static final String INCENDIUM_FUNCTIONS = "data/incendium/function/";
    private static final List<String> INCENDIUM_TICK_FUNCTIONS = List.of("clocks/main", "player/main");
    private static final String HUD_TEXT_CACHE = "evo2restore/patch/HudTextCache.class";

    private record ClassPatch(String modId, String entry, String originalSha1, String patchedSha1,
            UnaryOperator<byte[]> transform, boolean addsHudTextCache, String reason) {}

    private static final List<ClassPatch> CLASS_PATCHES = List.of(
            new ClassPatch("ironjetpacks", "com/blakebr0/ironjetpacks/client/handler/HudHandler.class",
                    "3003cc9ea317bcfbe93e923683d96a332e8cdd91", "bd6e2ef4976c9922ade2176e0cb72b1cda53d1aa",
                    Performance::internHudText, true,
                    "Iron Jetpacks HUD: reuse equal text components so their bidi reordering stays cached"),
            new ClassPatch("pipez", "de/maxhenkel/pipez/blocks/tileentity/types/FluidPipeType.class",
                    "23b0cd09abb68e6a27f49841cfd1c0321950c759", "7ff82556ac0c3a21da649c201135a713f210c920",
                    Performance::skipEmptyFluidFilters, false,
                    "Pipez fluid pipes: skip building filter lists when a pipe has no filters"));

    final List<String> notes = new ArrayList<>();

    void plan(Plan plan, GameDir game) throws IOException {
        List<ModJar> jars = ModJar.scan(game.resolve("mods"));
        if (installed(jars, "kubejs")) kubejsWebServer(plan);
        Optional<ModJar> incendium = jars.stream().filter(j -> j.declares("incendium")).findFirst();
        if (incendium.isPresent()) incendium(plan, game.relative(incendium.get().path()));
        for (ClassPatch p : CLASS_PATCHES) {
            for (ModJar j : jars) {
                if (j.declares(p.modId())) patchClass(plan, game.relative(j.path()), p);
            }
        }
    }

    private static boolean installed(List<ModJar> jars, String modId) {
        return jars.stream().anyMatch(j -> j.declares(modId));
    }

    private void kubejsWebServer(Plan plan) throws IOException {
        Map<String, Object> config = new LinkedHashMap<>();
        Optional<String> text = plan.readText(WEB_SERVER_CONFIG);
        if (text.isPresent()) {
            try {
                config = Json5.obj(Json5.parse(text.get()));
            } catch (IllegalArgumentException e) {
                notes.add(WEB_SERVER_CONFIG + " is not valid JSON; left the KubeJS web server as it is");
                return;
            }
        }
        if (Boolean.FALSE.equals(config.get("enabled"))) return;
        config.put("enabled", Boolean.FALSE);
        plan.writeText(WEB_SERVER_CONFIG, Json5.writeJson(config) + "\n",
                "KubeJS web server off: its accept loop spins a core while the game runs");
    }

    private void incendium(Plan plan, String jarPath) throws IOException {
        byte[] jar = plan.read(jarPath).orElseThrow();
        plan.writeText(DATAPACK + "pack.mcmeta", """
                {
                  "pack": {
                    "description": "FTB Evolution 2 performance fixes (Thaumaturge restore installer)",
                    "min_format": [101, 0],
                    "max_format": [101, 2147483647]
                  }
                }
                """, "datapack for the Incendium fixes below");
        plan.writeText(DATAPACK + "data/minecraft/tags/function/load.json", """
                {
                  "values": ["evo2_performance:load"]
                }
                """, "runs the load function below on every world load");
        plan.writeText(DATAPACK + "data/evo2_performance/function/load.mcfunction",
                "scoreboard objectives add in.prism dummy\n",
                "Incendium: adds the in.prism objective its player tick uses but never creates");
        for (String name : INCENDIUM_TICK_FUNCTIONS) {
            String path = INCENDIUM_FUNCTIONS + name + ".mcfunction";
            byte[] original = Asm.entry(jar, path);
            if (original == null) {
                notes.add("Incendium has no " + path + "; skipped its guard");
                continue;
            }
            String text = new String(original, StandardCharsets.UTF_8);
            String guarded = FunctionGuard.rewrite(text);
            if (guarded.equals(text)) continue;
            plan.writeText(DATAPACK + path, guarded,
                    "Incendium " + name + ": guards selectors that fail every tick; regenerated from " + jarPath);
        }
    }

    private void patchClass(Plan plan, String jarPath, ClassPatch p) throws IOException {
        byte[] jar = plan.read(jarPath).orElseThrow();
        byte[] original = Asm.entry(jar, p.entry());
        String sha1 = original == null ? null : Hashes.sha1(original);
        Map<String, byte[]> added = p.addsHudTextCache() ? Map.of(HUD_TEXT_CACHE, ownClass(HUD_TEXT_CACHE)) : Map.of();
        if (p.patchedSha1().equals(sha1)) {
            for (Map.Entry<String, byte[]> a : added.entrySet()) {
                if (!java.util.Arrays.equals(Asm.entry(jar, a.getKey()), a.getValue())) {
                    plan.write(jarPath, Asm.rewriteJar(jar, Map.of(), added), p.reason() + " (updated helper class)");
                    return;
                }
            }
            return;
        }
        if (!p.originalSha1().equals(sha1)) {
            notes.add(jarPath + " is a version this installer does not know; skipped: " + p.reason());
            return;
        }
        byte[] patched = Asm.rewriteJar(jar, Map.of(p.entry(), p.transform()), added);
        String patchedSha1 = Hashes.sha1(Asm.entry(patched, p.entry()));
        if (!patchedSha1.equals(p.patchedSha1())) {
            throw new IOException("patched " + p.entry() + " has SHA-1 " + patchedSha1 + ", expected " + p.patchedSha1());
        }
        plan.write(jarPath, patched, p.reason());
    }

    private static byte[] ownClass(String name) throws IOException {
        try (InputStream in = Performance.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IOException("installer jar lacks " + name);
            return in.readAllBytes();
        }
    }

    /** Each {@code getFuelComponent(stack)} and its siblings becomes {@code (Component) HudTextCache.intern(...)}. */
    static byte[] internHudText(byte[] bytes) {
        String handler = "com/blakebr0/ironjetpacks/client/handler/HudHandler";
        String component = "net/minecraft/network/chat/Component";
        String getter = "(Lnet/minecraft/world/item/ItemStack;)L" + component + ";";
        return Asm.transform(bytes, "onRegisterGuiOverlays", mv -> new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                super.visitMethodInsn(op, owner, name, desc, itf);
                if (op == Opcodes.INVOKESTATIC && owner.equals(handler) && desc.equals(getter)
                        && name.matches("get(Fuel|Throttle|Engine|Hover)Component")) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "evo2restore/patch/HudTextCache", "intern",
                            "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                    super.visitTypeInsn(Opcodes.CHECKCAST, component);
                }
            }
        });
    }

    /** {@code canInsert} starts with {@code if (filters.isEmpty()) return true;}, which it returned anyway. */
    static byte[] skipEmptyFluidFilters(byte[] bytes) {
        return Asm.transform(bytes, "canInsert", mv -> new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitCode() {
                super.visitCode();
                Label filtered = new Label();
                super.visitVarInsn(Opcodes.ALOAD, 4);
                super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);
                super.visitJumpInsn(Opcodes.IFEQ, filtered);
                super.visitInsn(Opcodes.ICONST_1);
                super.visitInsn(Opcodes.IRETURN);
                super.visitLabel(filtered);
            }
        });
    }
}
