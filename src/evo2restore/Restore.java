package evo2restore;

import evo2restore.util.Downloader;
import evo2restore.util.Hashes;
import evo2restore.util.Json5;
import evo2restore.util.Versions;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds the {@link Plan} that restores Thaumaturge into a newer FTB Evolution 2 install. */
final class Restore {
    static final String THAUMATURGE_CHAPTER_ID = "6EF52A92C93BE7E0";
    static final String RUNE_CARVING_ID = "6AA6C01368D6E51F";
    static final List<String> APPLIED_QUEST_IDS = List.of(
            "2EF2653C8BEC84FD", "5200D69AD476A22C", "23B3F8FD74731E52", "4D07CC3D8FF2CA4B", "6B2708211C8FB2B8",
            "4983F2E7A759A626", "4065FA972819C652", "7D5920CB9FEDB16F", "57C5F3F3A1543B06", "42289C15EC33E341",
            "5F11CA49505BC13E", "45F0E93597386E6F", "7DD4014676EDBDFB");
    private static final String APPLIED_IMAGE_ID = "7953A6F252E6B438";
    private static final List<String> APPLIED_LINK_IDS = List.of(
            "488530B6F1A92B79", "2FAC07A361E5E508", "2AFA2CD4B58CB9F8", "2CF474D13EF33B28");
    static final List<String> REQUIRED_MODS = List.of("ftbquests", "ae2", "curios", "lithostitched");

    private static final String CHAPTERS = Sources.QUESTS + "chapters/";
    private static final String LANG = Sources.QUESTS + "lang/en_us/";
    private static final String ABSENT_DATAPACK = "datapacks/thaumaturge-absent";

    record Options(boolean pyramid, boolean integration) {}

    private final GameDir game;
    private final Downloader downloader;
    private final Options options;
    private final PrintStream out;
    final List<String> warnings = new ArrayList<>();
    final List<String> notes = new ArrayList<>();

    Restore(GameDir game, Downloader downloader, Options options, PrintStream out) {
        this.game = game;
        this.downloader = downloader;
        this.options = options;
        this.out = out;
    }

    Plan plan() throws IOException, InterruptedException {
        Plan plan = new Plan(game);
        mods(plan);
        copied(plan);
        chapterTitle(plan);
        chapterOrder(plan);
        appliedBranch(plan);
        if (options.pyramid()) runeCarving(plan);
        if (options.integration()) {
            Integration integration = new Integration(downloader);
            integration.plan(plan);
            notes.addAll(integration.notes);
        }
        if (Files.isDirectory(game.resolve(ABSENT_DATAPACK))) {
            plan.remove(ABSENT_DATAPACK, "hides Thaumaturge items from recipe viewers; it must be off while the mod is loaded");
        }
        return plan;
    }

    private byte[] fetch(Sources.Remote remote) throws IOException, InterruptedException {
        return downloader.fetch(remote.url(), remote.sha1());
    }

    private Map<String, Object> fetchJson(Sources.Remote remote) throws IOException, InterruptedException {
        return Json5.obj(Json5.parse(new String(fetch(remote), StandardCharsets.UTF_8)));
    }

    private Map<String, Object> readJson(Plan plan, String path) throws IOException {
        String text = plan.readText(path).orElseThrow(() -> new IOException("missing " + path));
        try {
            return Json5.obj(Json5.parse(text));
        } catch (IllegalArgumentException e) {
            throw new IOException(path + ": " + e.getMessage(), e);
        }
    }

    private static String rel(GameDir game, Path p) {
        return game.root().relativize(p).toString().replace('\\', '/');
    }

    private void mods(Plan plan) throws IOException, InterruptedException {
        List<ModJar> jars = ModJar.scan(game.resolve("mods"));
        Set<String> present = new HashSet<>();
        for (ModJar j : jars) j.mods().forEach(m -> present.add(m.id()));
        for (String id : REQUIRED_MODS) {
            if (!present.contains(id)) warnings.add("required mod '" + id + "' was not found in mods/");
        }

        installMod(plan, jars, "thaumaturge", "1.1.0", Sources.THAUMATURGE_PATCHED_NAME, () -> {
            byte[] original = fetch(Sources.THAUMATURGE_JAR);
            out.println("Patching Thaumaturge 1.1.0 (issue #617 null guards)...");
            return ThaumPatch.patchJar(original);
        });
        installMod(plan, jars, "appliedthaumaturge", "1.0.0", Sources.APPLIED_NAME,
                () -> fetch(Sources.APPLIED_JAR));
    }

    private interface JarSource {
        byte[] get() throws IOException, InterruptedException;
    }

    /**
     * Installs {@code fileName} and disables other jars of the same mod. A jar of a newer version than
     * {@code version} is kept instead, since an official release supersedes the pinned one.
     */
    private void installMod(Plan plan, List<ModJar> jars, String modId, String version, String fileName,
            JarSource source) throws IOException, InterruptedException {
        List<ModJar> existing = jars.stream().filter(j -> j.declares(modId)).toList();
        for (ModJar j : existing) {
            String v = j.version(modId);
            if (v != null && Versions.compare(v, version) > 0) {
                out.println("Keeping " + j.path().getFileName() + " (" + modId + " " + v + " is newer than " + version + ")");
                for (ModJar other : existing) {
                    if (other != j) plan.remove(rel(game, other.path()), "duplicate " + modId + " jar");
                }
                return;
            }
        }
        String target = "mods/" + fileName;
        Path targetPath = game.resolve(target);
        boolean current = Files.isRegularFile(targetPath)
                && (modId.equals("thaumaturge")
                        ? ThaumPatch.isPatched(Files.readAllBytes(targetPath))
                        : Hashes.sha1(Files.readAllBytes(targetPath)).equals(Sources.APPLIED_JAR.sha1()));
        if (!current) plan.write(target, source.get(), modId + " " + version);
        for (ModJar j : existing) {
            if (!j.path().getFileName().toString().equals(fileName)) {
                plan.remove(rel(game, j.path()), "other " + modId + " jar; only one version can load");
            }
        }
    }

    private void copied(Plan plan) throws IOException, InterruptedException {
        for (Sources.Copied c : Sources.COPIED) {
            Path p = game.resolve(c.path());
            if (Files.isRegularFile(p) && Hashes.sha1(Files.readAllBytes(p)).equals(c.remote().sha1())) continue;
            if (c.path().startsWith(CHAPTERS) && Files.isRegularFile(p)) {
                if (sameChapterIgnoringOrder(Files.readString(p), fetch(c.remote()))) continue;
                warnings.add(c.path() + " exists with other content; replacing it with the 1.0.0 chapter");
            }
            plan.write(c.path(), fetch(c.remote()), "from FTB Evolution 2 1.0.0");
        }
    }

    private void chapterTitle(Plan plan) throws IOException {
        String path = LANG + "chapter.json5";
        Map<String, Object> lang = readJson(plan, path);
        String key = "chapter." + THAUMATURGE_CHAPTER_ID + ".title";
        if ("Thaumaturge".equals(Json5.str(lang.get(key)))) return;
        lang.put(key, Json5.Str.of("Thaumaturge"));
        plan.writeText(path, Json5.write(lang), "Thaumaturge chapter title");
    }

    /** True when the chapter differs from 1.0.0's only in the order_index that {@link #chapterOrder} sets. */
    private static boolean sameChapterIgnoringOrder(String installed, byte[] original) {
        try {
            Map<String, Object> a = Json5.obj(Json5.parse(installed));
            Map<String, Object> b = Json5.obj(Json5.parse(new String(original, StandardCharsets.UTF_8)));
            a.remove("order_index");
            b.remove("order_index");
            return a.equals(b);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Places Thaumaturge right before Witchery in its chapter group, as in 1.0.0. */
    private void chapterOrder(Plan plan) throws IOException {
        String thaumPath = CHAPTERS + "thaumaturge.json5";
        Map<String, Object> thaum = readJson(plan, thaumPath);
        String group = Json5.str(thaum.get("group"));

        record Chapter(String path, Map<String, Object> json) {
            double order() {
                return json.get("order_index") instanceof Json5.Num n ? n.doubleValue() : 0;
            }
        }
        List<Chapter> others = new ArrayList<>();
        for (String path : plan.list(CHAPTERS)) {
            if (!path.endsWith(".json5") || path.equals(thaumPath)) continue;
            Map<String, Object> json = readJson(plan, path);
            if (Objects.equals(Json5.str(json.get("group")), group)) others.add(new Chapter(path, json));
        }
        Chapter thaumChapter = new Chapter(thaumPath, thaum);
        double order = thaumChapter.order();
        Chapter witchery = others.stream()
                .filter(c -> "witchery".equals(Json5.str(c.json().get("filename"))))
                .findFirst().orElse(null);
        boolean thaumChanged = false;
        final double current = order;
        if (witchery != null && (current >= witchery.order() || others.stream()
                .anyMatch(c -> c.order() >= current && c.order() < witchery.order()))) {
            order = witchery.order();
            thaum.put("order_index", new Json5.Num(Long.toString((long) order)));
            thaumChanged = true;
        }
        final double slot = order;
        if (others.stream().anyMatch(c -> c.order() == slot)) {
            for (Chapter c : others) {
                if (c.order() < slot) continue;
                c.json().put("order_index", new Json5.Num(Long.toString((long) c.order() + 1)));
                plan.writeText(c.path(), Json5.write(c.json()), "chapter order: makes room for Thaumaturge");
            }
        }
        if (thaumChanged) plan.writeText(thaumPath, Json5.write(thaum), "chapter order: before Witchery");
    }

    private void appliedBranch(Plan plan) throws IOException, InterruptedException {
        String chapterPath = CHAPTERS + "general_storage.json5";
        String langPath = LANG + "chapters/general_storage.json5";
        Map<String, Object> old = fetchJson(Sources.OLD_GENERAL_STORAGE);
        Map<String, Object> oldLang = fetchJson(Sources.OLD_GENERAL_STORAGE_LANG);
        Map<String, Object> cur = readJson(plan, chapterPath);
        Map<String, Object> curLang = readJson(plan, langPath);

        Map<String, Map<String, Object>> oldQuests = byId(Json5.objects(old, "quests"));
        Map<String, Map<String, Object>> curQuests = byId(Json5.objects(cur, "quests"));
        List<Map<String, Object>> placed = new ArrayList<>();
        for (String id : APPLIED_QUEST_IDS) placed.add(require(oldQuests, id, "quest"));
        placed.add(require(byId(Json5.objects(old, "images")), APPLIED_IMAGE_ID, "image"));
        Map<String, Map<String, Object>> oldLinks = byId(Json5.objects(old, "quest_links"));
        for (String id : APPLIED_LINK_IDS) placed.add(require(oldLinks, id, "quest link"));

        Set<String> curIds = new HashSet<>(curQuests.keySet());
        curIds.addAll(byId(Json5.objects(cur, "images")).keySet());
        curIds.addAll(byId(Json5.objects(cur, "quest_links")).keySet());
        List<Map<String, Object>> missing = placed.stream().filter(e -> !curIds.contains(id(e))).toList();

        boolean changed = false;
        if (!missing.isEmpty()) {
            double dy = Layout.offset(placed, cur, curIds);
            out.printf("Adding %d Applied Thaumaturge quests, images and links to General Storage, %+.1f on Y%n",
                    missing.size(), dy);
            for (Map<String, Object> e : missing) {
                @SuppressWarnings("unchecked")
                Map<String, Object> copy = (Map<String, Object>) Json5.deepCopy(e);
                copy.put("y", Json5.Num.of(Layout.y(copy) + dy));
                String list = oldQuests.containsKey(id(e)) ? "quests"
                        : id(e).equals(APPLIED_IMAGE_ID) ? "images" : "quest_links";
                Json5.arrayOrCreate(cur, list).add(copy);
            }
            changed = true;
        }
        if (changed) plan.writeText(chapterPath, Json5.write(cur), "13 Applied Thaumaturge quests from 1.0.0");

        Set<String> nested = new HashSet<>();
        for (String id : APPLIED_QUEST_IDS) collectIds(oldQuests.get(id), nested);
        boolean langChanged = false;
        for (Map.Entry<String, Object> e : oldLang.entrySet()) {
            if (nested.stream().noneMatch(e.getKey()::contains)) continue;
            if (sameValue(curLang.get(e.getKey()), e.getValue())) continue;
            curLang.put(e.getKey(), e.getValue());
            langChanged = true;
        }
        if (langChanged) plan.writeText(langPath, Json5.write(curLang), "Applied Thaumaturge quest text");

        for (String id : APPLIED_QUEST_IDS) {
            for (Object dep : Json5.arr(oldQuests.get(id).getOrDefault("dependencies", List.of()))) {
                String target = Json5.str(dep);
                if (!QuestBook.questExists(plan, game, target)) {
                    warnings.add("Applied quest " + id + " depends on " + target + ", which this pack version lacks");
                }
            }
        }
    }

    /** Option A: Rune Carving asks for the 1.0.0 Thaumaturge items again; position, dependency and rewards stay. */
    private void runeCarving(Plan plan) throws IOException, InterruptedException {
        String chapterPath = CHAPTERS + "pyramid_challenge.json5";
        String langPath = LANG + "chapters/pyramid_challenge.json5";
        Map<String, Object> cur = readJson(plan, chapterPath);
        Map<String, Object> quest = byId(Json5.objects(cur, "quests")).get(RUNE_CARVING_ID);
        if (quest == null) {
            warnings.add("Pyramid Rune Carving quest " + RUNE_CARVING_ID + " not found; skipped Option A");
            return;
        }
        Map<String, Object> oldQuest = require(
                byId(Json5.objects(fetchJson(Sources.OLD_PYRAMID), "quests")), RUNE_CARVING_ID, "quest");
        boolean changed = false;
        for (String key : List.of("icon", "tasks")) {
            if (sameValue(quest.get(key), oldQuest.get(key))) continue;
            quest.put(key, Json5.deepCopy(oldQuest.get(key)));
            changed = true;
        }
        if (changed) plan.writeText(chapterPath, Json5.write(cur), "Rune Carving: 1.0.0 Thaumaturge tasks");

        Set<String> keys = new HashSet<>();
        collectIds(Json5.arr(oldQuest.get("tasks")), keys);
        Map<String, Object> oldLang = fetchJson(Sources.OLD_PYRAMID_LANG);
        Map<String, Object> curLang = readJson(plan, langPath);
        boolean langChanged = false;
        for (Map.Entry<String, Object> e : oldLang.entrySet()) {
            boolean wanted = e.getKey().equals("quest." + RUNE_CARVING_ID + ".quest_desc")
                    || keys.stream().anyMatch(e.getKey()::contains);
            if (!wanted || sameValue(curLang.get(e.getKey()), e.getValue())) continue;
            curLang.put(e.getKey(), e.getValue());
            langChanged = true;
        }
        if (langChanged) plan.writeText(langPath, Json5.write(curLang), "Rune Carving: 1.0.0 description");
    }

    static String id(Map<String, Object> o) {
        return Json5.str(o.get("id"));
    }

    static Map<String, Map<String, Object>> byId(List<Map<String, Object>> list) {
        Map<String, Map<String, Object>> m = new java.util.LinkedHashMap<>();
        for (Map<String, Object> o : list) if (id(o) != null) m.put(id(o), o);
        return m;
    }

    private static Map<String, Object> require(Map<String, Map<String, Object>> m, String id, String what)
            throws IOException {
        Map<String, Object> o = m.get(id);
        if (o == null) throw new IOException("1.0.0 source is missing " + what + " " + id);
        return o;
    }

    private static void collectIds(Object v, Set<String> out) {
        if (v instanceof Map<?, ?> m) {
            String id = Json5.str(m.get("id"));
            if (id != null) out.add(id);
            m.values().forEach(x -> collectIds(x, out));
        } else if (v instanceof List<?> l) {
            l.forEach(x -> collectIds(x, out));
        }
    }

    private static boolean sameValue(Object a, Object b) {
        return Json5.writeJson(a).equals(Json5.writeJson(b));
    }
}
