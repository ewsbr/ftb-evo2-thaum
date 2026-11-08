package evo2restore;

import evo2restore.util.Hashes;
import evo2restore.util.Json5;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The file changes an install makes, staged in memory until {@link #apply}.
 *
 * Applying keeps a journal and a copy of every replaced or removed file under {@code thaumaturge-restore/} in the
 * game directory. {@link #uninstall} replays the journals backwards.
 */
final class Plan {
    static final String STATE_DIR = "thaumaturge-restore";
    private static final String JOURNAL = "journal.json";

    private final GameDir game;
    private final Map<String, byte[]> writes = new LinkedHashMap<>();
    private final List<String> removals = new ArrayList<>();
    private final Map<String, String> reasons = new LinkedHashMap<>();

    Plan(GameDir game) {
        this.game = game;
    }

    /** The file as it will be after this plan, or empty if it will not exist. */
    Optional<byte[]> read(String path) throws IOException {
        if (writes.containsKey(path)) return Optional.of(writes.get(path));
        if (removals.contains(path)) return Optional.empty();
        Path p = game.resolve(path);
        return Files.isRegularFile(p) ? Optional.of(Files.readAllBytes(p)) : Optional.empty();
    }

    /** The files directly inside {@code dir} after this plan, as paths relative to the game directory. */
    List<String> list(String dir) throws IOException {
        java.util.TreeSet<String> out = new java.util.TreeSet<>();
        Path p = game.resolve(dir);
        if (Files.isDirectory(p)) {
            try (Stream<Path> s = Files.list(p)) {
                s.filter(Files::isRegularFile).forEach(f -> out.add(dir + f.getFileName()));
            }
        }
        for (String w : writes.keySet()) {
            if (w.startsWith(dir) && w.indexOf('/', dir.length()) < 0) out.add(w);
        }
        out.removeAll(removals);
        return List.copyOf(out);
    }

    Optional<String> readText(String path) throws IOException {
        return read(path).map(b -> new String(b, StandardCharsets.UTF_8));
    }

    /** Stages a write unless the file already has these bytes. */
    void write(String path, byte[] data, String reason) throws IOException {
        Optional<byte[]> current = read(path);
        if (current.isPresent() && java.util.Arrays.equals(current.get(), data)) return;
        writes.put(path, data);
        reasons.put(path, reason);
    }

    void writeText(String path, String text, String reason) throws IOException {
        write(path, text.getBytes(StandardCharsets.UTF_8), reason);
    }

    /** Stages moving a file or folder into the backup. */
    void remove(String path, String reason) {
        writes.remove(path);
        if (!removals.contains(path)) removals.add(path);
        reasons.put(path, reason);
    }

    boolean isEmpty() {
        return writes.isEmpty() && removals.isEmpty();
    }

    void print(PrintStream out) {
        for (String path : writes.keySet()) {
            String verb = Files.exists(game.resolve(path)) ? "update" : "create";
            out.printf("  %-8s %s%n           %s%n", verb, path, reasons.get(path));
        }
        for (String path : removals) out.printf("  %-8s %s%n           %s%n", "disable", path, reasons.get(path));
    }

    void apply() throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path dir = game.resolve(STATE_DIR).resolve(stamp);
        Path backup = dir.resolve("backup");
        Files.createDirectories(backup);

        List<Object> entries = new ArrayList<>();
        java.util.Set<Path> created = new java.util.LinkedHashSet<>();
        for (String path : writes.keySet()) {
            List<Path> missing = new ArrayList<>();
            for (Path d = game.resolve(path).getParent(); d != null && !Files.exists(d); d = d.getParent()) missing.add(d);
            java.util.Collections.reverse(missing);
            created.addAll(missing);
        }
        for (Path d : created) entries.add(entry("mkdir", rel(d), null, null));
        for (Map.Entry<String, byte[]> w : writes.entrySet()) {
            Path target = game.resolve(w.getKey());
            String before = null;
            if (Files.isRegularFile(target)) {
                before = Hashes.sha1(Files.readAllBytes(target));
                copy(target, backup.resolve(w.getKey()));
            }
            entries.add(entry("write", w.getKey(), before, Hashes.sha1(w.getValue())));
        }
        for (String path : removals) entries.add(entry("remove", path, null, null));

        Map<String, Object> journal = new LinkedHashMap<>();
        journal.put("game_dir", Json5.Str.of(game.root().toString()));
        journal.put("entries", entries);
        Files.writeString(dir.resolve(JOURNAL), Json5.writeJson(journal), StandardCharsets.UTF_8);

        try {
            for (Map.Entry<String, byte[]> w : writes.entrySet()) atomicWrite(game.resolve(w.getKey()), w.getValue());
            for (String path : removals) move(game.resolve(path), backup.resolve(path));
        } catch (IOException e) {
            List<String> left = revert(game, dir, System.out);
            try (Stream<Path> s = Files.list(dir.getParent())) {
                if (s.findAny().isEmpty()) Files.delete(dir.getParent());
            }
            throw new IOException("install failed and was rolled back" + (left.isEmpty() ? "" : " except " + left)
                    + ". Is the game still running? " + e, e);
        }
    }

    /** Reverts every journal, newest first. Returns the paths that were left in place because they changed. */
    static List<String> uninstall(GameDir game, PrintStream out) throws IOException {
        Path state = game.resolve(STATE_DIR);
        List<String> skipped = new ArrayList<>();
        if (!Files.isDirectory(state)) {
            out.println("Nothing to uninstall: no " + STATE_DIR + " folder in " + game.root());
            return skipped;
        }
        List<Path> runs;
        try (Stream<Path> s = Files.list(state)) {
            runs = s.filter(p -> Files.isRegularFile(p.resolve(JOURNAL)))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
        }
        for (Path run : runs) {
            out.println("Reverting install " + run.getFileName());
            skipped.addAll(revert(game, run, out));
        }
        try (Stream<Path> s = Files.list(state)) {
            if (s.findAny().isEmpty()) Files.delete(state);
        }
        return skipped;
    }

    /** Reverts one journal and deletes its folder, unless some files changed since; those are returned. */
    private static List<String> revert(GameDir game, Path run, PrintStream out) throws IOException {
        Map<String, Object> journal = Json5.obj(Json5.parse(Files.readString(run.resolve(JOURNAL))));
        List<Object> entries = new ArrayList<>(Json5.arr(journal.get("entries")));
        java.util.Collections.reverse(entries);
        List<String> runSkipped = new ArrayList<>();
        for (Object o : entries) {
            Map<String, Object> e = Json5.obj(o);
            String path = Json5.str(e.get("path"));
            Path target = game.resolve(path);
            Path saved = run.resolve("backup").resolve(path);
            if ("mkdir".equals(Json5.str(e.get("op")))) {
                if (isEmptyDir(target)) Files.delete(target);
                continue;
            }
            if ("remove".equals(Json5.str(e.get("op")))) {
                if (!Files.exists(saved)) continue;
                if (Files.exists(target)) {
                    runSkipped.add(path + " (exists again, backup kept)");
                    continue;
                }
                move(saved, target);
                out.println("  restored " + path);
                continue;
            }
            String before = Json5.str(e.get("before"));
            String after = Json5.str(e.get("after"));
            String current = Files.isRegularFile(target) ? Hashes.sha1(Files.readAllBytes(target)) : null;
            if (current == null ? before == null : current.equals(before)) continue;
            if (current == null || !current.equals(after)) {
                runSkipped.add(path + " (changed since install)");
                continue;
            }
            if (before == null) {
                Files.delete(target);
                out.println("  deleted  " + path);
            } else {
                atomicWrite(target, Files.readAllBytes(saved));
                out.println("  restored " + path);
            }
        }
        if (runSkipped.isEmpty()) {
            deleteTree(run);
        } else {
            out.println("  kept " + run + " because some files changed since install");
        }
        return runSkipped;
    }

    static boolean hasJournal(GameDir game) {
        Path state = game.resolve(STATE_DIR);
        if (!Files.isDirectory(state)) return false;
        try (Stream<Path> s = Files.list(state)) {
            return s.anyMatch(p -> Files.isRegularFile(p.resolve(JOURNAL)));
        } catch (IOException e) {
            return false;
        }
    }

    private static Map<String, Object> entry(String op, String path, String before, String after) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("op", Json5.Str.of(op));
        e.put("path", Json5.Str.of(path));
        if (before != null) e.put("before", Json5.Str.of(before));
        if (after != null) e.put("after", Json5.Str.of(after));
        return e;
    }

    private static void atomicWrite(Path target, byte[] data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".restore", ".tmp");
        try {
            Files.write(tmp, data);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void copy(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void move(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.move(from, to);
    }

    private String rel(Path p) {
        return game.root().relativize(p).toString().replace('\\', '/');
    }

    private static boolean isEmptyDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return false;
        try (Stream<Path> s = Files.list(dir)) {
            return s.findAny().isEmpty();
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
