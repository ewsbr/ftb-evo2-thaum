package evo2restore;

import evo2restore.util.Json5;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quest book paths, chapter names and IDs that the restore and its checks share. */
final class Quests {
    static final String DIR = "config/ftbquests/quests/";
    static final String CHAPTERS = DIR + "chapters/";
    static final String LANG = DIR + "lang/en_us/";

    static final String THAUMATURGE = "thaumaturge";
    static final String WITCHERY = "witchery";
    static final String GENERAL_STORAGE = "general_storage";
    static final String PYRAMID = "pyramid_challenge";

    static final String THAUMATURGE_CHAPTER_ID = "6EF52A92C93BE7E0";
    static final String RUNE_CARVING_ID = "6AA6C01368D6E51F";
    static final List<String> APPLIED_QUEST_IDS = List.of(
            "2EF2653C8BEC84FD", "5200D69AD476A22C", "23B3F8FD74731E52", "4D07CC3D8FF2CA4B", "6B2708211C8FB2B8",
            "4983F2E7A759A626", "4065FA972819C652", "7D5920CB9FEDB16F", "57C5F3F3A1543B06", "42289C15EC33E341",
            "5F11CA49505BC13E", "45F0E93597386E6F", "7DD4014676EDBDFB");

    private Quests() {}

    static String chapter(String name) {
        return CHAPTERS + name + ".json5";
    }

    static String chapterLang(String name) {
        return LANG + "chapters/" + name + ".json5";
    }

    static String id(Map<String, Object> o) {
        return Json5.str(o.get("id"));
    }

    static Map<String, Map<String, Object>> byId(List<Map<String, Object>> list) {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        for (Map<String, Object> o : list) if (id(o) != null) m.put(id(o), o);
        return m;
    }
}
