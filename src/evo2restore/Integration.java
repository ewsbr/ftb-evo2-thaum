package evo2restore;

import evo2restore.util.Downloader;
import evo2restore.util.Hashes;
import evo2restore.util.Json5;
import evo2restore.util.Json5Doc;
import evo2restore.util.ListMerge;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Puts back the Thaumaturge content that FTB Evolution 2 1.0.1 and later cut from files outside the quest book.
 *
 * Each step merges the 1.0.0 Thaumaturge entries into the installed file and leaves everything else as the newer
 * pack has it. Where the newer pack swapped a Thaumaturge item for a stand-in, the step reverts that one value.
 * Content the newer pack redesigned on purpose, such as cinnabar now yielding FTB Materials gems, stays.
 */
final class Integration {
    sealed interface Op permits AddItems, AddKeys, AddValues, Revert, Put {}

    /** Adds the old array's Thaumaturge elements; {@code idPath} is the dotted field that identifies an element. */
    record AddItems(String pointer, String idPath, boolean freshIds) implements Op {
        AddItems(String pointer, String idPath) {
            this(pointer, idPath, false);
        }
    }

    /** Adds the old object's Thaumaturge keys. */
    record AddKeys(String pointer) implements Op {}

    /** Adds strings to an array unless present. */
    record AddValues(String pointer, List<String> values) implements Op {}

    /** Sets the value to the old one. */
    record Revert(String pointer) implements Op {}

    /** Sets a string value written for this restore. */
    record Put(String pointer, String value) implements Op {}

    private interface Step {
        void plan(Integration in, Plan plan) throws IOException, InterruptedException;
    }

    private record JsonStep(String path, List<Op> ops) implements Step {
        @Override
        public void plan(Integration in, Plan plan) throws IOException, InterruptedException {
            in.json(plan, path, ops);
        }
    }

    private record LinesStep(String path) implements Step {
        @Override
        public void plan(Integration in, Plan plan) throws IOException, InterruptedException {
            in.lines(plan, path);
        }
    }

    private record TomlListStep(String path, String key, UnaryOperator<String> fix) implements Step {
        @Override
        public void plan(Integration in, Plan plan) throws IOException, InterruptedException {
            in.tomlList(plan, path, key, fix);
        }
    }

    /** Copies a 1.0.0 file that the newer pack deleted, unless {@code skipIf} already contains {@code marker}. */
    private record FileStep(String path, String source, String skipIf, String marker) implements Step {
        FileStep(String path, String source) {
            this(path, source, null, null);
        }

        @Override
        public void plan(Integration in, Plan plan) throws IOException, InterruptedException {
            if (skipIf != null && plan.readText(skipIf).map(t -> t.contains(marker)).orElse(false)) return;
            in.file(plan, path, source);
        }
    }

    private static final String FTB = "datapacks/ftb/data/";
    private static final String UNI = "datapacks/ftb-unification/data/";
    private static final String UNI_RECIPES = UNI + "ftb/recipe/unification/";
    private static final String SKILLS = FTB + "puffish_skills/puffish_skills/categories/magic/definitions.json";
    private static final String SKILL = "/skill.ftb.magic.";

    private static final List<String> REVERTED_SKILLS = List.of(
            "thaum_cap", "thaum_focus_power_+5%", "thaum_infusion_stability_+1", "thaum_warp_ward_+5%",
            "vis_discount_+2%");

    private static List<Op> skillOps() {
        List<Op> ops = new ArrayList<>();
        ops.add(new AddItems("/archmagus_cap/rewards", "data.attribute"));
        ops.add(new AddItems("/sorcery_hub/rewards", "data.attribute"));
        for (String s : REVERTED_SKILLS) {
            ops.add(new Revert("/" + s + "/icon"));
            ops.add(new Revert("/" + s + "/rewards"));
        }
        return ops;
    }

    private static List<Op> englishSkillText() {
        List<Op> ops = new ArrayList<>();
        for (String key : List.of("sorcery_hub.description", "archmagus_cap.description", "thaum_cap.title",
                "thaum_cap.description", "thaum_focus_power_+5%.description",
                "thaum_infusion_stability_+1.description", "thaum_warp_ward_+5%.description",
                "vis_discount_+2%.description")) {
            ops.add(new Revert(SKILL + key));
        }
        // 1.8.1 unlocks the Arcanist with the Arcane Compendium, Vinteum Dust or a Thaumometer (stage_triggers.js),
        // but its hint only names Vinteum Dust.
        ops.add(new Put("/ftb.shopkeeper.locked.arcanist",
                "Carry the Arcane Compendium, some Vinteum Dust or a Thaumometer and I'll share my stock."));
        return ops;
    }

    /** 1.0.0 had no pt_br text; these match the restored English. */
    private static List<Op> portugueseSkillText() {
        return List.of(
                new Put(SKILL + "sorcery_hub.description", "+5% de dano mágico, +5% de regeneração de mana de Ars "
                        + "Magica e 2% de desconto de vis. Abre os caminhos Arquimago e Taumaturgo e o Estudo do Encantador."),
                new Put(SKILL + "archmagus_cap.description",
                        "+10% de dano mágico, feitiços de Ars Magica e Roots custam 5% menos e 3% de desconto de vis."),
                new Put(SKILL + "thaum_cap.title", "Taumaturgo"),
                new Put(SKILL + "thaum_cap.description", "4% de desconto de vis, focos lançados com 10% mais poder, mais "
                        + "10% de chance de resistir a cada ponto de distorção, e infusões começam com 1 de instabilidade a menos."),
                new Put(SKILL + "thaum_focus_power_+5%.description", "Focos de varinha lançados com 5% mais poder."),
                new Put(SKILL + "thaum_infusion_stability_+1.description",
                        "Infusões começam com 1 de instabilidade a menos."),
                new Put(SKILL + "thaum_warp_ward_+5%.description", "5% de chance de resistir a cada ponto de distorção."),
                new Put(SKILL + "vis_discount_+2%.description", "2% de desconto de vis."),
                new Put("/ftb.shopkeeper.locked.arcanist",
                        "Carregue o Compêndio Arcano, um pouco de Pó de Vinteum ou um Taumômetro e compartilharei meu estoque."));
    }

    private static final List<Step> STEPS = buildSteps();

    private static List<Step> buildSteps() {
        List<Step> s = new ArrayList<>();
        s.add(new JsonStep("config/ftbmaterials/unifier-db.json", List.of(new AddKeys("/items"))));
        s.add(new JsonStep("config/ftbmaterials-startup.json5",
                List.of(new AddItems("/blacklists/unification_blacklist_items", null))));
        s.add(new JsonStep(UNI + "c/tags/item/hidden_from_recipe_viewers.json", List.of(new AddItems("/values", null))));
        s.add(new JsonStep(UNI + "c/tags/item/ore_source/overworld/cinnabar.json", List.of(new AddItems("/values", null))));
        s.add(new JsonStep(UNI + "c/tags/item/ore_source/overworld/quartz.json", List.of(new AddItems("/values", null))));
        for (String layer : List.of("stone", "deepslate")) {
            s.add(new JsonStep(UNI + "apothic_enchanting/loot_table/boon_" + layer + "_drops.json",
                    List.of(new AddItems("/pools/*/entries", "name"))));
        }
        s.add(new JsonStep(FTB + "dimresources/recipe/crystalore.json", List.of(new AddItems("/outputs", "stack.id"))));
        s.add(new JsonStep(FTB + "ftb/echo_definitions/arcanist.json",
                List.of(new AddItems("/stages/*/shop_unlock", "name"))));
        // 1.0.0 used Thaumaturge's ore as the model's block; 1.8.1 uses FTB Materials' ore, so it becomes a variant.
        s.add(new JsonStep(FTB + "hostilenetworks/hostilenetworks/data_models/blocks/cinnabar_ore.json",
                List.of(new AddItems("/variants", null), new AddValues("/variants", List.of("thaumaturge:ore_cinnabar")))));
        s.add(new JsonStep(FTB + "hostilenetworks/hostilenetworks/data_models/blocks/nether_quartz_ore.json",
                List.of(new AddItems("/variants", null))));
        s.add(new JsonStep(FTB + "bhc/recipe/blade_of_vitality.json", List.of(new Revert("/fluids/0"))));
        s.add(new JsonStep(FTB + "bhc/recipe/vigor_bow.json", List.of(new Revert("/fluids/0"))));
        s.add(new JsonStep(FTB + "ftb/recipe/fabricating/causal_anchor.json",
                List.of(new Revert("/ingredients/4/ingredient"))));
        s.add(new JsonStep(SKILLS, skillOps()));
        s.add(new JsonStep("kubejs/assets/ftb/lang/en_us.json", englishSkillText()));
        s.add(new JsonStep("kubejs/assets/ftb/lang/pt_br.json", portugueseSkillText()));
        s.add(new JsonStep("config/ftbquests/quests/reward_tables/6e00621943707265.json5",
                List.of(new Revert("/icon"), new AddItems("/rewards", "item.id", true))));
        s.add(new JsonStep("config/ftbquests/quests/reward_tables/3162793db33d4e0a.json5",
                List.of(new AddItems("/rewards", "item.id", true))));
        s.add(new JsonStep("config/sdrp-client.json5", List.of(new AddItems("/dimensionsSupport", "matcher"))));
        s.add(new JsonStep("configureddefaults/config/jade/jade.json", List.of(new AddKeys("/plugin"))));
        for (int i = 1; i <= 3; i++) {
            s.add(new JsonStep("configureddefaults/config/jade/profiles/" + i + "/jade.json",
                    List.of(new AddKeys("/plugin"))));
        }
        s.add(new JsonStep("configureddefaults/config/jade/sort-order.json", List.of(new AddKeys(""))));

        s.add(new LinesStep("config/servercore/config.yml"));
        s.add(new LinesStep("config/mezz_config_gui/client/mod-navigation.ini"));
        s.add(new LinesStep("configureddefaults/options.txt"));
        for (String f : List.of("client/ingredient-list-mod-sort-order.ini", "client/ingredient-list-type-sort-order.ini",
                "client/recipe-category-sort-order.ini", "recipe-category-sort-order.ini")) {
            s.add(new LinesStep("configureddefaults/config/jei/" + f));
        }
        // 1.0.0 listed these blocks under the thaumcraft: namespace, so Carry On never matched them.
        s.add(new TomlListStep("config/carryon-common.toml", "forbiddenTiles",
                v -> v.replace("thaumcraft:", "thaumaturge:")));
        s.add(new TomlListStep("config/dankstorage-server.toml", "\"convertible tags\"", UnaryOperator.identity()));

        String amber = "overworld/gem_ore_to_gem/amber.json";
        s.add(new FileStep(UNI + "c/tags/item/ore_source/overworld/amber.json", null));
        for (String machine : List.of("ftbic/macerating/" + amber, "horsepowered/grinding/" + amber,
                "integrateddynamics/mechanical_squeezer/overworld/gem_ore_to_gem_standard/amber.json",
                "integrateddynamics/squeezer/overworld/gem_ore_to_gem_standard/amber.json", "ltxi/grinding/" + amber,
                "occultism/crushing/" + amber, "oritech/fragment_forge/" + amber, "oritech/pulverizer/" + amber,
                "energizedpower/pulverizer/overworld/ore_to_gem/amber.json",
                "enderio/sag_milling/ores/stone/gem_ore_to_gem/amber.json")) {
            s.add(new FileStep(UNI_RECIPES + machine, null));
        }
        s.add(new FileStep(UNI + "thaumaturge/recipe/nugget_brass.json", null));
        s.add(new FileStep(UNI + "thaumaturge/advancement/recipes/misc/nugget_brass.json", null));
        s.add(new FileStep(FTB + "bhc/recipe/blue_heart_infusion.json", null));
        s.add(new FileStep("config/mysticalcustomization/crops/amber.json", null));
        s.add(new FileStep(FTB + "ftb/recipe/mysticalagriculture/essence/amber.json", null));
        // In 1.0.0 this handler was all of blocks.js; 1.8.1 reuses that file for Eternal Water.
        s.add(new FileStep("kubejs/server_scripts/systems/thaumaturge_node_placer.js",
                "kubejs/server_scripts/systems/blocks.js", "kubejs/server_scripts/systems/blocks.js",
                "thaumaturge:creative_node_placer"));
        return s;
    }

    private final Downloader downloader;
    final List<String> notes = new ArrayList<>();

    Integration(Downloader downloader) {
        this.downloader = downloader;
    }

    void plan(Plan plan) throws IOException, InterruptedException {
        for (Step step : STEPS) step.plan(this, plan);
        keybinds(plan);
    }

    private String old(String path) throws IOException, InterruptedException {
        Sources.Remote r = Objects.requireNonNull(IntegrationSources.OLD.get(path), path);
        return new String(downloader.fetch(r.url(), r.sha1()), StandardCharsets.UTF_8);
    }

    private void json(Plan plan, String path, List<Op> ops) throws IOException, InterruptedException {
        Optional<String> text = plan.readText(path);
        if (text.isEmpty()) {
            notes.add("skipped " + path + ": not in this pack version");
            return;
        }
        Json5Doc doc = Json5Doc.parse(text.get(), !path.endsWith(".json5"));
        Object cur = doc.root();
        boolean needsOld = ops.stream().anyMatch(o -> o instanceof AddItems || o instanceof AddKeys || o instanceof Revert);
        Object old = needsOld ? Json5.parse(old(path)) : null;
        String before = Json5.writeJson(cur);
        for (Op op : ops) {
            if (op instanceof AddItems a) addItems(doc, old, a, path);
            else if (op instanceof AddKeys a) addKeys(doc, old, a.pointer(), path);
            else if (op instanceof AddValues a) addValues(doc, a, path);
            else if (op instanceof Revert r) set(doc, r.pointer(), resolve(old, r.pointer()), path);
            else if (op instanceof Put p) set(doc, p.pointer(), Json5.Str.of(p.value()), path);
        }
        if (Json5.writeJson(cur).equals(before)) return;
        plan.writeText(path, doc.render(), "1.0.0 Thaumaturge integration");
    }

    private void addItems(Json5Doc doc, Object old, AddItems op, String path) {
        List<Object[]> pairs = new ArrayList<>();
        pairs(old, doc.root(), segments(op.pointer()), 0, pairs);
        if (pairs.isEmpty()) notes.add("skipped " + path + op.pointer() + ": structure changed");
        for (Object[] pair : pairs) {
            if (!(pair[0] instanceof List<?> && pair[1] instanceof List<?>)) continue;
            List<Object> oldList = Json5.arr(pair[0]);
            List<Object> curList = Json5.arr(pair[1]);
            java.util.Set<String> takenIds = new java.util.HashSet<>();
            for (Object o : curList) {
                if (o instanceof Map<?, ?> m && Json5.str(m.get("id")) != null) takenIds.add(Json5.str(m.get("id")));
            }
            java.util.Set<Object> existing = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            existing.addAll(curList);
            List<Object> merged = ListMerge.restore(oldList, curList, Integration::isThaum, v -> identity(v, op.idPath()));
            for (int i = 0; i < merged.size(); i++) {
                Object v = merged.get(i);
                if (existing.contains(v)) continue;
                v = Json5.deepCopy(v);
                if (op.freshIds() && v instanceof Map<?, ?> m) {
                    Map<String, Object> entry = Json5.obj(m);
                    String id = Json5.str(entry.get("id"));
                    if (id != null && takenIds.contains(id)) entry.put("id", Json5.Str.of(freshId(path, id, takenIds)));
                    takenIds.add(Json5.str(entry.get("id")));
                }
                merged.set(i, v);
            }
            if (merged.size() == curList.size()) continue;
            curList.clear();
            curList.addAll(merged);
            doc.touched(curList);
        }
    }

    private void addValues(Json5Doc doc, AddValues op, String path) {
        if (!(resolve(doc.root(), op.pointer()) instanceof List<?> l)) {
            notes.add("skipped " + path + op.pointer() + ": structure changed");
            return;
        }
        List<Object> list = Json5.arr(l);
        for (String v : op.values()) {
            if (list.stream().anyMatch(x -> v.equals(Json5.str(x)))) continue;
            list.add(Json5.Str.of(v));
            doc.touched(list);
        }
    }

    private void addKeys(Json5Doc doc, Object old, String pointer, String path) {
        Object o = resolve(old, pointer);
        Object c = resolve(doc.root(), pointer);
        if (!(o instanceof Map<?, ?>) || !(c instanceof Map<?, ?>)) {
            notes.add("skipped " + path + pointer + ": structure changed");
            return;
        }
        Map<String, Object> oldMap = Json5.obj(o);
        Map<String, Object> curMap = Json5.obj(c);
        List<String> keys = ListMerge.restore(new ArrayList<>(oldMap.keySet()), new ArrayList<>(curMap.keySet()),
                k -> isThaum(k) || isThaum(oldMap.get(k)), k -> k);
        if (keys.size() == curMap.size()) return;
        Map<String, Object> rebuilt = new LinkedHashMap<>();
        for (String k : keys) rebuilt.put(k, curMap.containsKey(k) ? curMap.get(k) : Json5.deepCopy(oldMap.get(k)));
        curMap.clear();
        curMap.putAll(rebuilt);
        doc.touched(curMap);
    }

    private void set(Json5Doc doc, String pointer, Object value, String path) {
        List<String> seg = segments(pointer);
        Object parent = resolve(doc.root(), seg.subList(0, seg.size() - 1));
        String last = seg.get(seg.size() - 1);
        if (value == null) {
            notes.add("skipped " + path + pointer + ": not in the 1.0.0 file");
            return;
        }
        Object copy = Json5.deepCopy(value);
        if (parent instanceof Map<?, ?> m) {
            Map<String, Object> map = Json5.obj(m);
            if (map.containsKey(last) && Json5.writeJson(map.get(last)).equals(Json5.writeJson(copy))) return;
            if (map.containsKey(last)) doc.replaced(map.get(last), copy);
            else doc.touched(map);
            map.put(last, copy);
        } else if (parent instanceof List<?> l && isIndex(last) && Integer.parseInt(last) < l.size()) {
            List<Object> list = Json5.arr(l);
            int i = Integer.parseInt(last);
            if (Json5.writeJson(list.get(i)).equals(Json5.writeJson(copy))) return;
            doc.replaced(list.get(i), copy);
            list.set(i, copy);
        } else {
            notes.add("skipped " + path + pointer + ": structure changed");
        }
    }

    private void lines(Plan plan, String path) throws IOException, InterruptedException {
        Optional<String> text = plan.readText(path);
        if (text.isEmpty()) {
            notes.add("skipped " + path + ": not in this pack version");
            return;
        }
        String eol = text.get().contains("\r\n") ? "\r\n" : "\n";
        boolean finalEol = text.get().endsWith("\n");
        List<String> cur = List.of(text.get().split("\r?\n", -1));
        if (finalEol) cur = cur.subList(0, cur.size() - 1);
        List<String> old = List.of(old(path).split("\r?\n"));
        List<String> merged = ListMerge.restore(old, cur, Integration::isThaum, String::strip);
        if (merged.size() == cur.size()) return;
        plan.writeText(path, String.join(eol, merged) + (finalEol ? eol : ""), "1.0.0 Thaumaturge entries");
    }

    // Matches one TOML line that assigns a single-line array:  forbiddenTiles = ["a", "b"]
    private static final String TOML_LIST = "(?m)^(\\s*%s\\s*=\\s*\\[)(.*)(\\]\\s*)$";
    private static final Pattern QUOTED = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

    private void tomlList(Plan plan, String path, String key, UnaryOperator<String> fix)
            throws IOException, InterruptedException {
        Optional<String> text = plan.readText(path);
        if (text.isEmpty()) {
            notes.add("skipped " + path + ": not in this pack version");
            return;
        }
        Pattern line = Pattern.compile(String.format(TOML_LIST, Pattern.quote(key)));
        Matcher cur = line.matcher(text.get());
        Matcher old = line.matcher(old(path));
        if (!cur.find() || !old.find()) {
            notes.add("skipped " + path + " " + key + ": not a single-line array");
            return;
        }
        List<String> curItems = quoted(cur.group(2));
        List<String> oldItems = quoted(old.group(2)).stream().map(fix).toList();
        List<String> merged = ListMerge.restore(oldItems, curItems, Integration::isThaum, v -> v);
        if (merged.size() == curItems.size()) return;
        String replaced = text.get().substring(0, cur.start(2)) + String.join(", ", merged) + text.get().substring(cur.end(2));
        plan.writeText(path, replaced, "1.0.0 Thaumaturge entries");
    }

    private static List<String> quoted(String s) {
        List<String> out = new ArrayList<>();
        Matcher m = QUOTED.matcher(s);
        while (m.find()) out.add(m.group());
        return out;
    }

    private void file(Plan plan, String path, String source) throws IOException, InterruptedException {
        if (plan.read(path).isPresent()) return;
        Sources.Remote r = IntegrationSources.OLD.get(source == null ? path : source);
        plan.write(path, downloader.fetch(r.url(), r.sha1()), "from FTB Evolution 2 1.0.0");
    }

    // Matches an options.txt key binding:  key_key.thaumaturge.change_focus:key.keyboard.f:CONTROL
    private static final Pattern KEY_LINE = Pattern.compile("^(key_[^:]+):(.+)$");

    /**
     * Applies 1.0.0's Thaumaturge key bindings to the player's options.txt where Thaumaturge's own default collides
     * with another binding, such as Change Focus on F with Swap Offhand. Bindings the player changed are left alone.
     */
    private void keybinds(Plan plan) throws IOException, InterruptedException {
        Optional<String> text = plan.readText("options.txt");
        if (text.isEmpty()) return;
        Map<String, String> pack = new LinkedHashMap<>();
        for (String l : old("configureddefaults/options.txt").split("\r?\n")) {
            Matcher m = KEY_LINE.matcher(l);
            if (m.matches() && isThaum(m.group(1))) pack.put(m.group(1), m.group(2));
        }
        List<String> lines = new ArrayList<>(List.of(text.get().split("\n", -1)));
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = KEY_LINE.matcher(lines.get(i).strip());
            if (m.matches()) index.put(m.group(1), i);
        }
        boolean changed = false;
        for (Map.Entry<String, String> e : pack.entrySet()) {
            Integer at = index.get(e.getKey());
            String current = at == null ? null : lines.get(at).strip().substring(e.getKey().length() + 1);
            if (e.getValue().equals(current)) continue;
            if (current != null && (current.equals("key.keyboard.unknown") || !collides(lines, e.getKey(), current))) {
                continue;
            }
            String line = e.getKey() + ":" + e.getValue();
            if (at != null) {
                lines.set(at, lines.get(at).endsWith("\r") ? line + "\r" : line);
            } else {
                boolean trailingEmpty = !lines.isEmpty() && lines.get(lines.size() - 1).isEmpty();
                lines.add(trailingEmpty ? lines.size() - 1 : lines.size(), line);
            }
            notes.add("key binding " + e.getKey() + " set to " + e.getValue() + (current == null ? "" : " (was " + current + ", which collides)"));
            changed = true;
        }
        if (changed) plan.writeText("options.txt", String.join("\n", lines), "1.0.0 Thaumaturge key bindings");
    }

    private static boolean collides(List<String> lines, String key, String binding) {
        for (String l : lines) {
            Matcher m = KEY_LINE.matcher(l.strip());
            if (m.matches() && !m.group(1).equals(key) && m.group(2).equals(binding)) return true;
        }
        return false;
    }

    private static boolean isThaum(Object v) {
        String s = v instanceof String str ? str : Json5.writeJson(v);
        return s.toLowerCase(Locale.ROOT).contains("thaum");
    }

    private static Object identity(Object v, String idPath) {
        if (idPath == null) return Json5.writeJson(v);
        Object cur = v;
        for (String part : idPath.split("\\.")) {
            if (!(cur instanceof Map<?, ?> m)) return Json5.writeJson(v);
            cur = m.get(part);
        }
        return cur == null ? Json5.writeJson(v) : Json5.writeJson(cur);
    }

    /** A stable FTB Quests ID for a restored entry whose 1.0.0 ID the newer pack reused. */
    private static String freshId(String path, String oldId, java.util.Set<String> taken) {
        for (int salt = 0; ; salt++) {
            String id = Hashes.sha1((path + "/" + oldId + "/" + salt).getBytes(StandardCharsets.UTF_8))
                    .substring(0, 16).toUpperCase(Locale.ROOT);
            if (!taken.contains(id)) return id;
        }
    }

    private static void pairs(Object old, Object cur, List<String> seg, int i, List<Object[]> out) {
        if (old == null || cur == null) return;
        if (i == seg.size()) {
            out.add(new Object[] {old, cur});
            return;
        }
        String s = seg.get(i);
        if (s.equals("*") && old instanceof List<?> lo && cur instanceof List<?> lc) {
            for (int k = 0; k < Math.min(lo.size(), lc.size()); k++) pairs(lo.get(k), lc.get(k), seg, i + 1, out);
        } else if (s.equals("*") && old instanceof Map<?, ?> mo && cur instanceof Map<?, ?> mc) {
            for (Object k : mo.keySet()) pairs(mo.get(k), mc.get(k), seg, i + 1, out);
        } else {
            pairs(child(old, s), child(cur, s), seg, i + 1, out);
        }
    }

    private static Object resolve(Object root, String pointer) {
        return root == null ? null : resolve(root, segments(pointer));
    }

    private static Object resolve(Object root, List<String> seg) {
        Object v = root;
        for (String s : seg) v = child(v, s);
        return v;
    }

    private static Object child(Object v, String s) {
        if (v instanceof Map<?, ?> m) return m.get(s);
        if (v instanceof List<?> l && isIndex(s) && Integer.parseInt(s) < l.size()) return l.get(Integer.parseInt(s));
        return null;
    }

    private static boolean isIndex(String s) {
        return !s.isEmpty() && s.chars().allMatch(Character::isDigit);
    }

    private static List<String> segments(String pointer) {
        return pointer.isEmpty() ? List.of() : List.of(pointer.substring(1).split("/", -1));
    }
}
