package evo2restore;

import evo2restore.util.Json5;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** The quest book as a {@link Plan} would leave it, and the problems found in it. */
final class QuestBook {
    private static final String CHAPTERS = Sources.QUESTS + "chapters/";

    final Map<String, Map<String, Object>> chapters = new LinkedHashMap<>();
    final Map<String, String> questChapter = new HashMap<>();
    final Map<String, Object> lang = new HashMap<>();
    final Set<String> problems = new LinkedHashSet<>();

    static QuestBook load(Plan plan, GameDir game) throws IOException {
        QuestBook book = new QuestBook();
        Map<String, Integer> idCount = new HashMap<>();
        for (String path : jsonFiles(plan, CHAPTERS)) {
            Map<String, Object> chapter = parse(plan, path);
            if (chapter == null) continue;
            String name = Json5.str(chapter.get("filename"));
            book.chapters.put(name == null ? path : name, chapter);
            for (Map<String, Object> q : Json5.objects(chapter, "quests")) {
                book.questChapter.put(Restore.id(q), name);
            }
            countIds(chapter, idCount);
        }
        for (String path : jsonFiles(plan, Sources.QUESTS + "reward_tables/")) {
            Map<String, Object> table = parse(plan, path);
            if (table != null) countIds(table, idCount);
        }
        for (String path : jsonFiles(plan, Sources.QUESTS + "lang/en_us/chapters/")) {
            Map<String, Object> l = parse(plan, path);
            if (l != null) book.lang.putAll(l);
        }
        Map<String, Object> chapterLang = parse(plan, Sources.QUESTS + "lang/en_us/chapter.json5");
        if (chapterLang != null) book.lang.putAll(chapterLang);

        idCount.forEach((id, n) -> {
            if (n > 1) book.problems.add("duplicate ID " + id);
        });
        book.checkReferences();
        return book;
    }

    static boolean questExists(Plan plan, GameDir game, String questId) throws IOException {
        for (String path : jsonFiles(plan, CHAPTERS)) {
            Map<String, Object> chapter = parse(plan, path);
            if (chapter == null) continue;
            for (Map<String, Object> q : Json5.objects(chapter, "quests")) {
                if (questId.equals(Restore.id(q))) return true;
            }
        }
        return false;
    }

    int questCount() {
        return questChapter.size();
    }

    /** Problems specific to the restored content. */
    List<String> restoredProblems(Plan plan, boolean pyramid) throws IOException {
        List<String> out = new ArrayList<>();
        Map<String, Object> thaum = chapters.get("thaumaturge");
        if (thaum == null) {
            out.add("thaumaturge chapter is missing");
        } else {
            for (Map<String, Object> q : Json5.objects(thaum, "quests")) {
                if (!lang.containsKey("quest." + Restore.id(q) + ".title")) {
                    out.add("thaumaturge quest " + Restore.id(q) + " has no title text");
                }
            }
        }
        for (String id : Restore.APPLIED_QUEST_IDS) {
            if (!"general_storage".equals(questChapter.get(id))) out.add("Applied quest " + id + " is missing");
            else if (!lang.containsKey("quest." + id + ".title")) out.add("Applied quest " + id + " has no title text");
        }
        if (!"Thaumaturge".equals(Json5.str(lang.get("chapter." + Restore.THAUMATURGE_CHAPTER_ID + ".title")))) {
            out.add("Thaumaturge chapter title is missing");
        }

        Set<String> itemIds = new TreeSet<>();
        if (thaum != null) collectItemIds(thaum.get("quests"), itemIds);
        Map<String, Object> storage = chapters.get("general_storage");
        if (storage != null) {
            for (Map<String, Object> q : Json5.objects(storage, "quests")) {
                if (Restore.APPLIED_QUEST_IDS.contains(Restore.id(q))) collectItemIds(q, itemIds);
            }
        }
        Map<String, Object> pyramidChapter = chapters.get("pyramid_challenge");
        if (pyramid && pyramidChapter != null) {
            Map<String, Object> rune = Restore.byId(Json5.objects(pyramidChapter, "quests")).get(Restore.RUNE_CARVING_ID);
            if (rune != null) collectItemIds(rune, itemIds);
        }
        Set<String> known = new TreeSet<>();
        known.addAll(registeredIds(plan, "mods/" + Sources.THAUMATURGE_PATCHED_NAME, "thaumaturge"));
        known.addAll(registeredIds(plan, "mods/" + Sources.APPLIED_NAME, "appliedthaumaturge"));
        if (!known.isEmpty()) {
            for (String id : itemIds) {
                String ns = id.substring(0, id.indexOf(':'));
                if ((ns.equals("thaumaturge") || ns.equals("appliedthaumaturge")) && !known.contains(id)) {
                    out.add("quest item " + id + " is not in the installed jars");
                }
            }
        }
        return out;
    }

    private void checkReferences() {
        for (Map.Entry<String, Map<String, Object>> c : chapters.entrySet()) {
            for (Map<String, Object> q : Json5.objects(c.getValue(), "quests")) {
                for (Object dep : Json5.arr(q.getOrDefault("dependencies", List.of()))) {
                    if (!questChapter.containsKey(Json5.str(dep))) {
                        problems.add(c.getKey() + ": quest " + Restore.id(q) + " depends on missing " + Json5.str(dep));
                    }
                }
            }
            for (Map<String, Object> link : Json5.objects(c.getValue(), "quest_links")) {
                String target = Json5.str(link.get("linked_quest"));
                if (!questChapter.containsKey(target)) {
                    problems.add(c.getKey() + ": link " + Restore.id(link) + " points at missing " + target);
                }
            }
        }
    }

    private static void countIds(Object v, Map<String, Integer> counts) {
        if (v instanceof Map<?, ?> m) {
            Object id = m.get("id");
            if (id instanceof Json5.Str s && s.value().matches("[0-9A-F]{16}")) counts.merge(s.value(), 1, Integer::sum);
            m.values().forEach(x -> countIds(x, counts));
        } else if (v instanceof List<?> l) {
            l.forEach(x -> countIds(x, counts));
        }
    }

    private static void collectItemIds(Object v, Set<String> out) {
        if (v instanceof Map<?, ?> m) {
            String id = Json5.str(m.get("id"));
            if (id != null && id.contains(":")) out.add(id);
            m.values().forEach(x -> collectItemIds(x, out));
        } else if (v instanceof List<?> l) {
            l.forEach(x -> collectItemIds(x, out));
        }
    }

    /** Item and block IDs a mod jar ships models, blockstates or English names for. */
    private static Set<String> registeredIds(Plan plan, String jarPath, String ns) throws IOException {
        Set<String> ids = new TreeSet<>();
        byte[] jar = plan.read(jarPath).orElse(null);
        if (jar == null) return ids;
        String models = "assets/" + ns + "/models/item/";
        String states = "assets/" + ns + "/blockstates/";
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                String n = e.getName();
                for (String prefix : List.of(models, states)) {
                    if (n.startsWith(prefix) && n.endsWith(".json") && n.indexOf('/', prefix.length()) < 0) {
                        ids.add(ns + ":" + n.substring(prefix.length(), n.length() - 5));
                    }
                }
                if (n.equals("assets/" + ns + "/lang/en_us.json")) {
                    Map<String, Object> l = Json5.obj(Json5.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                    for (String key : l.keySet()) {
                        for (String kind : List.of("item.", "block.")) {
                            String p = kind + ns + ".";
                            if (key.startsWith(p)) ids.add(ns + ":" + key.substring(p.length()).split("\\.")[0]);
                        }
                    }
                }
            }
        }
        return ids;
    }

    private static Map<String, Object> parse(Plan plan, String path) throws IOException {
        String text = plan.readText(path).orElse(null);
        if (text == null) return null;
        try {
            return Json5.obj(Json5.parse(text));
        } catch (IllegalArgumentException e) {
            throw new IOException(path + ": " + e.getMessage(), e);
        }
    }

    private static List<String> jsonFiles(Plan plan, String dir) throws IOException {
        return plan.list(dir).stream().filter(f -> f.endsWith(".json5")).toList();
    }
}
