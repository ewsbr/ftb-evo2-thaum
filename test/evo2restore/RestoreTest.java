package evo2restore;

import evo2restore.util.Json5;
import evo2restore.util.Json5Doc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Run with ./test.sh. */
public class RestoreTest {
    public static void main(String[] args) throws IOException {
        chapterOrder();
        revertStandIn();
        integrationSources();
        uninstallAfterPackUpdate(Path.of(args.length > 0 ? args[0] : "build/test-work"));
        System.out.println("RestoreTest: all passed");
    }

    private static void chapterOrder() {
        Map<String, Double> pack181 = group("rootsclassic", 6, "witchery", 7);
        ChapterOrder.Placement p = ChapterOrder.before(7, pack181, "witchery");
        check(p.moved() == null && p.shifted().equals(Map.of("witchery", 8L)),
                "1.0.0's index is kept when it is already before Witchery; Witchery moves up");

        Map<String, Double> pack190 = group("forcecraft", 3, "rootsclassic", 7, "witchery", 8);
        p = ChapterOrder.before(7, pack190, "witchery");
        check(Long.valueOf(8).equals(p.moved()) && p.shifted().equals(Map.of("witchery", 9L)),
                "a chapter between Thaumaturge and Witchery moves Thaumaturge to Witchery's index");

        p = ChapterOrder.before(8, group("rootsclassic", 7, "witchery", 9), "witchery");
        check(p.moved() == null && p.shifted().isEmpty(), "a restored order is left as it is");

        p = ChapterOrder.before(12, group("anima", 0, "witchery", 5), "witchery");
        check(Long.valueOf(5).equals(p.moved()) && p.shifted().equals(Map.of("witchery", 6L)),
                "Thaumaturge after Witchery moves before it");

        p = ChapterOrder.before(2, group("anima", 2, "occultism", 4), "witchery");
        check(p.moved() == null && p.shifted().equals(Map.of("anima", 3L, "occultism", 5L)),
                "without Witchery only a collision is shifted");
    }

    private static void revertStandIn() {
        String old = "{ingredients: [{ingredient: 'a', count: 1}, {ingredient: 'thaumaturge:ingot_thaumium', count: 8},"
                + " {ingredient: 'nova', count: 1}]}";
        Integration.RevertStandIn op = new Integration.RevertStandIn("/ingredients", "ingredient");

        String cur = "{ingredients: [{ingredient: 'a', count: 1}, {ingredient: 'moonstone', count: 8},"
                + " {ingredient: {items: 'nova'}, count: 1}]}";
        check(revert(old, cur, op).equals("[\"a\",\"thaumaturge:ingot_thaumium\",{\"items\":\"nova\"}]"),
                "the stand-in at the old index is replaced, not the other new entry");

        cur = "{ingredients: [{ingredient: 'moonstone', count: 8}, {ingredient: 'a', count: 1},"
                + " {ingredient: {items: 'nova'}, count: 1}]}";
        check(revert(old, cur, op).equals("[\"thaumaturge:ingot_thaumium\",\"a\",{\"items\":\"nova\"}]"),
                "after a reorder the stand-in is found by its matching count");

        cur = "{ingredients: [{ingredient: 'x', count: 2}, {ingredient: 'a', count: 1}, {ingredient: 'y', count: 3}]}";
        check(revert(old, cur, op).equals("[\"x\",\"a\",\"y\"]"), "an ambiguous stand-in is left alone");

        String fluids = "{fluids: [{id: 'thaumaturge:liquid_death', amount: 8000}, {id: 'v', amount: 16000}]}";
        String now = "{fluids: [{id: 'evilcraft:blood', amount: 16000}, {id: 'v', amount: 16000}]}";
        Integration.RevertStandIn whole = new Integration.RevertStandIn("/fluids", null);
        Json5Doc doc = Json5Doc.parse(now, true);
        new Integration(null).revertStandIn(doc, Json5.parse(fluids), whole, "f.json");
        check(Json5.writeJson(Json5.obj(doc.root()).get("fluids")).equals(Json5.writeJson(Json5.obj(Json5.parse(fluids)).get("fluids"))),
                "a whole-element revert restores the 1.0.0 fluid with its amount");
    }

    private static String revert(String old, String cur, Integration.RevertStandIn op) {
        Json5Doc doc = Json5Doc.parse(cur, true);
        new Integration(null).revertStandIn(doc, Json5.parse(old), op, "r.json");
        StringBuilder sb = new StringBuilder("[");
        for (Object e : Json5.arr(Json5.obj(doc.root()).get("ingredients"))) {
            if (sb.length() > 1) sb.append(',');
            sb.append(Json5.writeJson(Json5.obj(e).get("ingredient")).replaceAll("\\s", ""));
        }
        return sb.append(']').toString();
    }

    private static void integrationSources() {
        List<String> missing = Integration.sourcePaths().stream()
                .filter(p -> !IntegrationSources.OLD.containsKey(p)).toList();
        check(missing.isEmpty(), "every integration step has a pinned 1.0.0 source " + missing);
    }

    /** Install, pack update, re-install, uninstall: the files end as the updated pack has them. */
    private static void uninstallAfterPackUpdate(Path work) throws IOException {
        deleteTree(work);
        Path root = work.resolve(".minecraft");
        Files.createDirectories(root.resolve("datapacks/absent"));
        Files.writeString(root.resolve("a.json"), "pack 1");
        GameDir game = new GameDir(root, work, "test", "test", null, null, null);
        PrintStream quiet = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        Plan first = new Plan(game);
        first.writeText("a.json", "restored 1", "test");
        first.remove("datapacks/absent", "test");
        first.apply(quiet);

        Files.writeString(root.resolve("a.json"), "pack 2");
        Files.createDirectories(root.resolve("datapacks/absent"));
        Plan second = new Plan(game);
        second.writeText("a.json", "restored 2", "test");
        second.remove("datapacks/absent", "test");
        second.apply(quiet);

        List<String> skipped = Plan.uninstall(game, quiet);
        check(skipped.isEmpty(), "uninstall after a pack update reports nothing as changed " + skipped);
        check(Files.readString(root.resolve("a.json")).equals("pack 2"), "a file ends as the updated pack has it");
        check(Files.isDirectory(root.resolve("datapacks/absent")), "a disabled folder is back");
        check(!Files.exists(root.resolve(Plan.STATE_DIR)), "no stale backup is left behind");
        deleteTree(work);
    }

    private static Map<String, Double> group(Object... nameOrder) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < nameOrder.length; i += 2) m.put((String) nameOrder[i], ((Number) nameOrder[i + 1]).doubleValue());
        return m;
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
        System.out.println("ok  " + what);
    }
}
