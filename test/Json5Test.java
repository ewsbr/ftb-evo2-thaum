import evo2restore.util.Json5;
import evo2restore.util.Json5Doc;
import evo2restore.util.ListMerge;
import java.util.List;
import java.util.Map;

/** Run with: java -cp dist/evo2-thaumaturge-restore.jar test/Json5Test.java */
public class Json5Test {
    public static void main(String[] args) {
        String ftbStyle = """
                {
                  id: "6EF52A92C93BE7E0",
                  order_index: 7,
                  x: -3.0,
                  quest_links: [
                  ],
                  tasks: [
                    {
                      "minecraft:enchantments": "\\& keeps \\f its \\u00e9scapes",
                      count: 1L,
                    },
                  ],
                }""";
        check(Json5.write(Json5.parse(ftbStyle)).equals(ftbStyle), "FTB Quests file round-trips byte for byte");

        Map<String, Object> o = Json5.obj(Json5.parse("// c\n{a: 'it\\'s', /* c */ b: [1, 2,], 'c-d': null}"));
        check(Json5.str(o.get("a")).equals("it's"), "single-quoted string is decoded");
        check(Json5.arr(o.get("b")).size() == 2, "trailing comma is accepted");
        check(o.containsKey("c-d") && o.get("c-d") == null, "quoted key and null literal");
        check(Json5.write(o).contains("\"c-d\": null"), "key that is not an identifier is quoted");

        String json = Json5.writeJson(Json5.parse("{type: 'a', pattern: ['TPT', ' S '], vis: 75}"));
        check(json.equals("{\n  \"type\": \"a\",\n  \"pattern\": [\n    \"TPT\",\n    \" S \"\n  ],\n  \"vis\": 75\n}\n"),
                "strict JSON quotes keys and drops trailing commas");

        check(Json5.Num.of(12.0).raw().equals("12.0") && Json5.Num.of(-1.5).raw().equals("-1.5"),
                "coordinates keep FTB's decimal form");
        check(Json5.write(Json5.Str.of("a\"b\nc")).equals("\"a\\\"b\\nc\""), "new strings are escaped");

        try {
            Json5.parse("{a: 1,,}");
            check(false, "double comma is rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("line 1"), "parse error names the line");
        }
        String commented = """
                {
                  // kept
                  list: [
                    "a",
                    "c",
                  ],
                  /* kept too */ other: { x: 1 },
                }""";
        Json5Doc doc = Json5Doc.parse(commented, false);
        List<Object> list = Json5.arr(Json5.obj(doc.root()).get("list"));
        list.add(1, Json5.Str.of("b"));
        doc.touched(list);
        String edited = doc.render();
        check(edited.contains("// kept") && edited.contains("/* kept too */ other: { x: 1 }"),
                "editing one array keeps the file's comments and untouched values");
        check(edited.contains("    \"a\",\n    \"b\",\n    \"c\",\n  ],"), "the edited array keeps indent and trailing comma");

        Json5Doc compact = Json5Doc.parse("{\n\t\"r\": [\n\t\t{ \"k\": 1 }\n\t]\n}", true);
        List<Object> rows = Json5.arr(Json5.obj(compact.root()).get("r"));
        rows.add(Json5.parse("{k: 2}"));
        compact.touched(rows);
        check(compact.render().equals("{\n\t\"r\": [\n\t\t{ \"k\": 1 },\n\t\t{ \"k\": 2 }\n\t]\n}"),
                "a new element copies its siblings' one-line style and tab indent");
        check(Json5Doc.parse(commented, false).render().equals(commented), "an unedited file renders unchanged");

        List<String> merged = ListMerge.restore(List.of("a", "T1", "T2", "b", "T3"), List.of("a", "b", "new"),
                s -> s.startsWith("T"), s -> s);
        check(merged.equals(List.of("a", "T1", "T2", "b", "T3", "new")), "restored items land after their old neighbours");
        check(ListMerge.restore(List.of("T1"), List.of("T1", "x"), s -> true, s -> s).equals(List.of("T1", "x")),
                "items already present are not added again");

        System.out.println("Json5Test: all passed");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
        System.out.println("ok  " + what);
    }
}
