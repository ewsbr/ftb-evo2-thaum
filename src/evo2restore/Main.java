package evo2restore;

import evo2restore.util.Downloader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Main {
    private static final String USAGE = """
            FTB Evolution 2 Thaumaturge restore

            Usage: install.sh [options] [INSTANCE]
                   install.cmd [options] [INSTANCE]
                   java -jar evo2-thaumaturge-restore.jar [options] [INSTANCE]

            INSTANCE is a launcher instance folder, its .minecraft/minecraft folder, or a server folder.
            Without it, the installer searches the usual Prism, MultiMC, FTB App, CurseForge, Modrinth,
            ATLauncher and GDLauncher folders.

            Options:
              --dry-run       show what would change and write nothing
              --uninstall     revert every change this installer made
              --yes, -y       do not ask for confirmation
              --no-pyramid    keep the Witchery version of the Pyramid "Rune Carving" quest
              --quests-only   restore only the quest book and mods, not the 1.0.0 Thaumaturge
                              integration in recipes, loot, worldgen, skills and configs
              --force         continue on an unrecognized pack or Minecraft version
              --offline       use only files already in the download cache
              --cache DIR     download cache (default: %s)
              --list          list detected FTB Evolution 2 instances and exit
              --help          show this text
            """;

    /** 1.0.0-rc.1 and 1.0.0 ship Thaumaturge themselves. */
    private static final Set<Long> NATIVE_THAUMATURGE_VERSIONS = Set.of(100548L, 100549L);

    private Main() {}

    public static void main(String[] args) {
        PrintStream out = System.out;
        try {
            System.exit(run(args, out));
        } catch (Failure e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        } catch (IOException | InterruptedException | RuntimeException e) {
            System.err.println("error: " + e);
            System.exit(1);
        }
    }

    private static final class Failure extends Exception {
        private static final long serialVersionUID = 1L;

        Failure(String message) {
            super(message);
        }
    }

    private static int run(String[] args, PrintStream out) throws Failure, IOException, InterruptedException {
        boolean dryRun = false, uninstall = false, yes = false, pyramid = true, force = false, offline = false;
        boolean integration = true;
        boolean list = false;
        Path cache = Downloader.defaultCacheDir();
        Path instance = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dry-run" -> dryRun = true;
                case "--uninstall" -> uninstall = true;
                case "--yes", "-y" -> yes = true;
                case "--no-pyramid" -> pyramid = false;
                case "--quests-only" -> integration = false;
                case "--force" -> force = true;
                case "--offline" -> offline = true;
                case "--list" -> list = true;
                case "--cache" -> {
                    if (++i >= args.length) throw new Failure("--cache needs a directory");
                    cache = Path.of(args[i]);
                }
                case "--help", "-h" -> {
                    out.printf(USAGE, Downloader.defaultCacheDir());
                    return 0;
                }
                default -> {
                    if (args[i].startsWith("-")) throw new Failure("unknown option " + args[i]);
                    if (instance != null) throw new Failure("only one INSTANCE can be given");
                    instance = Path.of(args[i]);
                }
            }
        }

        if (list) {
            List<GameDir> found = GameDir.discover();
            if (found.isEmpty()) out.println("No FTB Evolution 2 instances found.");
            for (GameDir g : found) out.println(g.describe());
            return 0;
        }

        GameDir game = instance != null ? GameDir.open(instance) : choose(out, yes);
        out.println("Instance: " + game.describe());

        if (uninstall) {
            if (!yes && !confirm(out, "Revert the Thaumaturge restore in this instance?")) return 1;
            List<String> skipped = Plan.uninstall(game, out);
            for (String s : skipped) out.println("warning: left in place: " + s);
            out.println(skipped.isEmpty() ? "Uninstalled." : "Uninstalled with warnings.");
            return skipped.isEmpty() ? 0 : 2;
        }

        checkVersion(game, force, out);
        if (!force && shipsThaumaturge(game)) {
            throw new Failure("this install already ships Thaumaturge and the Applied quests, like pack 1.0.0;"
                    + " this restore is for 1.0.1 and later (--force to continue)");
        }

        Restore restore = new Restore(game, new Downloader(cache, offline), new Restore.Options(pyramid, integration), out);
        Plan plan = restore.plan();

        QuestBook before = QuestBook.load(new Plan(game));
        QuestBook after = QuestBook.load(plan);
        List<String> introduced = new ArrayList<>(after.problems);
        introduced.removeAll(before.problems);
        introduced.addAll(after.restoredProblems(plan, pyramid, restore.modJars));

        for (String n : restore.notes) out.println("note: " + n);
        for (String w : restore.warnings) out.println("warning: " + w);
        if (!introduced.isEmpty()) {
            for (String p : introduced) out.println("problem: " + p);
            if (!force) throw new Failure("the restored quest book would have problems; nothing was written (--force to write anyway)");
        }

        out.printf("Quest book after restore: %d chapters, %d quests%n", after.chapters.size(), after.questCount());
        if (plan.isEmpty()) {
            out.println("Already restored. Nothing to do.");
            return 0;
        }
        out.println("Changes:");
        plan.print(out);
        if (dryRun) {
            out.println("Dry run: nothing was written.");
            return 0;
        }
        if (!yes && !confirm(out, "Close the game first. Apply these changes?")) return 1;
        plan.apply(out);
        out.println("Done. Backups and the uninstall journal are in " + game.resolve(Plan.STATE_DIR));
        out.println("Start the game once. FTB Quests should report " + after.chapters.size() + " chapters and "
                + after.questCount() + " quests in logs/latest.log.");
        return 0;
    }

    private static void checkVersion(GameDir game, boolean force, PrintStream out) throws Failure {
        List<String> issues = new ArrayList<>();
        if (game.packVersionId() != null && NATIVE_THAUMATURGE_VERSIONS.contains(game.packVersionId())) {
            throw new Failure("pack " + game.packVersion() + " already ships Thaumaturge; this restore is for 1.0.1 and later");
        }
        if (game.minecraft() != null && !game.minecraft().equals(GameDir.MINECRAFT_VERSION)) {
            issues.add("Minecraft " + game.minecraft() + " is not " + GameDir.MINECRAFT_VERSION
                    + "; the pinned Thaumaturge jars are built for " + GameDir.MINECRAFT_VERSION);
        }
        if (issues.isEmpty()) return;
        if (!force) throw new Failure(String.join("; ", issues) + " (--force to continue)");
        issues.forEach(i -> out.println("warning: " + i));
    }

    /** A pack that still has Thaumaturge: the Applied quests and a Thaumaturge jar this installer did not write. */
    private static boolean shipsThaumaturge(GameDir game) throws IOException {
        if (Plan.hasJournal(game)) return false;
        boolean foreignJar = ModJar.scan(game.resolve("mods")).stream()
                .anyMatch(j -> j.declares("thaumaturge")
                        && !j.path().getFileName().toString().equals(Sources.THAUMATURGE_PATCHED_NAME));
        return foreignJar && QuestBook.questExists(new Plan(game), Quests.APPLIED_QUEST_IDS.get(0));
    }

    private static GameDir choose(PrintStream out, boolean yes) throws IOException, Failure {
        List<GameDir> found = GameDir.discover();
        if (found.isEmpty()) {
            throw new Failure("no FTB Evolution 2 instance found; pass the instance folder as an argument");
        }
        if (found.size() == 1) return found.get(0);
        if (yes) throw new Failure("several instances found; pass one as an argument (see --list)");
        for (int i = 0; i < found.size(); i++) out.printf("%d) %s%n", i + 1, found.get(i).describe());
        String answer = prompt(out, "Instance number: ");
        try {
            return found.get(Integer.parseInt(answer.trim()) - 1);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new Failure("no instance selected");
        }
    }

    private static boolean confirm(PrintStream out, String question) throws IOException {
        String answer = prompt(out, question + " [y/N] ");
        return answer.trim().equalsIgnoreCase("y") || answer.trim().equalsIgnoreCase("yes");
    }

    private static String prompt(PrintStream out, String text) throws IOException {
        out.print(text);
        out.flush();
        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        return line == null ? "" : line;
    }
}
