package evo2restore;

import evo2restore.util.Json5;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/** A Minecraft game directory holding an FTB Evolution 2 install, plus what its launcher reports about it. */
record GameDir(Path root, Path instance, String launcher, String name, String packVersion, Long packVersionId,
        String minecraft) {

    static final long FTB_PACK_ID = 135;
    static final String MINECRAFT_VERSION = "26.1.2";

    Path resolve(String relative) {
        return root.resolve(relative);
    }

    /** The path relative to the game directory, with forward slashes on every OS. */
    String relative(Path p) {
        return root.relativize(p).toString().replace('\\', '/');
    }

    String describe() {
        return String.format("%s [%s] pack %s, Minecraft %s%n    %s", name, launcher,
                packVersion == null ? "unknown" : packVersion + (packVersionId == null ? "" : " (" + packVersionId + ")"),
                minecraft == null ? "unknown" : minecraft, root);
    }

    /** Accepts an instance folder or the game directory inside it. */
    static GameDir open(Path path) throws IOException {
        Path dir = path.toAbsolutePath().normalize();
        for (Path candidate : List.of(dir.resolve(".minecraft"), dir.resolve("minecraft"), dir)) {
            if (isEvolution2(candidate)) {
                Path instance = candidate.equals(dir) && isNamedGameDir(dir) ? dir.getParent() : dir;
                return describe(candidate, instance);
            }
        }
        throw new IOException("no FTB Evolution 2 quest book found in " + dir);
    }

    static List<GameDir> discover() {
        List<GameDir> found = new ArrayList<>();
        for (Path root : launcherRoots()) {
            if (!Files.isDirectory(root)) continue;
            for (Path child : list(root)) probe(child).ifPresent(found::add);
        }
        return found;
    }

    private static Optional<GameDir> probe(Path dir) {
        try {
            return Optional.of(open(dir));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static List<Path> list(Path dir) {
        try (Stream<Path> children = Files.list(dir)) {
            return children.sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static boolean isNamedGameDir(Path dir) {
        String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
        return name.equals(".minecraft") || name.equals("minecraft");
    }

    private static boolean isEvolution2(Path gameDir) {
        return Files.isRegularFile(gameDir.resolve(Quests.chapter(Quests.GENERAL_STORAGE)))
                && Files.isRegularFile(gameDir.resolve(Quests.chapter(Quests.PYRAMID)))
                && Files.isDirectory(gameDir.resolve("mods"));
    }

    private static GameDir describe(Path gameDir, Path instance) {
        String launcher = "unknown launcher or server";
        String name = instance.getFileName() == null ? instance.toString() : instance.getFileName().toString();
        String packVersion = null;
        Long packVersionId = null;
        String minecraft = null;

        Path cfg = instance.resolve("instance.cfg");
        Path mmcPack = instance.resolve("mmc-pack.json");
        Path ftbApp = instance.resolve("instance.json");
        Path curse = instance.resolve("minecraftinstance.json");
        Path modrinth = instance.resolve("profile.json");
        try {
            if (Files.isRegularFile(cfg)) {
                launcher = "Prism/MultiMC";
                Properties p = iniSection(cfg);
                name = p.getProperty("name", name);
                if (String.valueOf(FTB_PACK_ID).equals(p.getProperty("ManagedPackID"))) {
                    packVersion = p.getProperty("ManagedPackVersionName");
                    packVersionId = parseLong(p.getProperty("ManagedPackVersionID"));
                }
                if (Files.isRegularFile(mmcPack)) {
                    for (Map<String, Object> c : Json5.objects(readJson(mmcPack), "components")) {
                        if ("net.minecraft".equals(Json5.str(c.get("uid")))) {
                            minecraft = Json5.str(c.getOrDefault("version", c.get("cachedVersion")));
                        }
                    }
                }
            } else if (Files.isRegularFile(ftbApp)) {
                Map<String, Object> j = readJson(ftbApp);
                launcher = j.containsKey("launcher") ? "ATLauncher" : "FTB App";
                name = firstString(j, name, "name");
                minecraft = firstString(j, null, "mcVersion", "minecraft");
                Object id = j.get("id");
                if (id instanceof Json5.Num n && n.doubleValue() == FTB_PACK_ID) {
                    packVersion = firstString(j, null, "version");
                    Object vid = j.get("versionId");
                    if (vid instanceof Json5.Num v) packVersionId = (long) v.doubleValue();
                }
                if (j.get("launcher") instanceof Map<?, ?> at) {
                    packVersion = Json5.str(Json5.obj(at).get("version"));
                    minecraft = firstString(j, minecraft, "id");
                }
            } else if (Files.isRegularFile(curse)) {
                Map<String, Object> j = readJson(curse);
                launcher = "CurseForge";
                name = firstString(j, name, "name");
                minecraft = firstString(j, null, "gameVersion");
            } else if (Files.isRegularFile(modrinth)) {
                Map<String, Object> j = readJson(modrinth);
                launcher = "Modrinth App";
                if (j.get("metadata") instanceof Map<?, ?> m) {
                    name = firstString(Json5.obj(m), name, "name");
                    minecraft = firstString(Json5.obj(m), null, "game_version");
                }
            } else if (Files.isRegularFile(gameDir.resolve("server.properties"))) {
                launcher = "dedicated server";
            }
        } catch (IOException | RuntimeException e) {
            launcher += " (unreadable metadata: " + e.getMessage() + ")";
        }
        return new GameDir(gameDir, instance, launcher, name, packVersion, packVersionId, minecraft);
    }

    private static String firstString(Map<String, Object> j, String fallback, String... keys) {
        for (String key : keys) {
            String v = Json5.str(j.get(key));
            if (v != null && !v.isBlank()) return v;
        }
        return fallback;
    }

    private static Long parseLong(String s) {
        try {
            return s == null ? null : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, Object> readJson(Path p) throws IOException {
        return Json5.obj(Json5.parse(Files.readString(p, StandardCharsets.UTF_8)));
    }

    /** Reads the [General] section of a Qt-style INI file. */
    private static Properties iniSection(Path p) throws IOException {
        Properties props = new Properties();
        boolean general = true;
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.startsWith("[")) {
                general = t.equals("[General]");
                continue;
            }
            int eq = t.indexOf('=');
            if (general && eq > 0) props.setProperty(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
        }
        return props;
    }

    private static Set<Path> launcherRoots() {
        Path home = Path.of(System.getProperty("user.home"));
        String os = System.getProperty("os.name", "").toLowerCase();
        Set<Path> roots = new LinkedHashSet<>();
        if (os.contains("win")) {
            Path appData = env("APPDATA", home.resolve("AppData/Roaming"));
            Path local = env("LOCALAPPDATA", home.resolve("AppData/Local"));
            roots.add(appData.resolve("PrismLauncher/instances"));
            roots.add(appData.resolve("PolyMC/instances"));
            roots.add(local.resolve(".ftba/instances"));
            roots.add(home.resolve(".ftba/instances"));
            roots.add(home.resolve("curseforge/minecraft/Instances"));
            roots.add(appData.resolve("ModrinthApp/profiles"));
            roots.add(appData.resolve("com.modrinth.theseus/profiles"));
            roots.add(appData.resolve("ATLauncher/instances"));
            roots.add(appData.resolve("gdlauncher_carbon/data/instances"));
        } else if (os.contains("mac")) {
            Path support = home.resolve("Library/Application Support");
            roots.add(support.resolve("PrismLauncher/instances"));
            roots.add(support.resolve(".ftba/instances"));
            roots.add(home.resolve("Documents/curseforge/minecraft/Instances"));
            roots.add(support.resolve("ModrinthApp/profiles"));
            roots.add(support.resolve("ATLauncher/instances"));
            roots.add(support.resolve("gdlauncher_carbon/data/instances"));
        } else {
            Path data = env("XDG_DATA_HOME", home.resolve(".local/share"));
            Path config = env("XDG_CONFIG_HOME", home.resolve(".config"));
            Path flatpak = home.resolve(".var/app");
            roots.add(data.resolve("PrismLauncher/instances"));
            roots.add(flatpak.resolve("org.prismlauncher.PrismLauncher/data/PrismLauncher/instances"));
            roots.add(data.resolve("PolyMC/instances"));
            roots.add(data.resolve("multimc/instances"));
            roots.add(home.resolve(".ftba/instances"));
            roots.add(data.resolve(".ftba/instances"));
            roots.add(data.resolve("ModrinthApp/profiles"));
            roots.add(data.resolve("com.modrinth.theseus/profiles"));
            roots.add(data.resolve("ATLauncher/instances"));
            roots.add(flatpak.resolve("com.atlauncher.ATLauncher/data/instances"));
            roots.add(config.resolve("gdlauncher_carbon/data/instances"));
            roots.add(home.resolve("curseforge/minecraft/Instances"));
        }
        return roots;
    }

    private static Path env(String name, Path fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : Path.of(v);
    }
}
