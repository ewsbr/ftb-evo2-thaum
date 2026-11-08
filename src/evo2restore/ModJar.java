package evo2restore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/** The mods a jar in {@code mods/} declares in its NeoForge metadata. */
record ModJar(Path path, List<Mod> mods) {
    record Mod(String id, String version) {}

    // Matches the table header and the first two keys of each mod:  [[mods]] modId = "ae2" ... version = "1.0"
    private static final Pattern MOD_BLOCK = Pattern.compile("\\[\\[mods]](.*?)(?=\\[\\[|\\z)", Pattern.DOTALL);
    private static final Pattern MOD_ID = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"");

    boolean declares(String modId) {
        return mods.stream().anyMatch(m -> m.id().equals(modId));
    }

    String version(String modId) {
        return mods.stream().filter(m -> m.id().equals(modId)).map(Mod::version).findFirst().orElse(null);
    }

    static List<ModJar> scan(Path modsDir) throws IOException {
        List<ModJar> jars = new ArrayList<>();
        if (!Files.isDirectory(modsDir)) return jars;
        try (Stream<Path> s = Files.list(modsDir)) {
            for (Path p : s.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                jars.add(read(p));
            }
        }
        return jars;
    }

    static ModJar read(Path jar) {
        List<Mod> mods = new ArrayList<>();
        try (JarFile jf = new JarFile(jar.toFile())) {
            ZipEntry toml = jf.getEntry("META-INF/neoforge.mods.toml");
            if (toml == null) toml = jf.getEntry("META-INF/mods.toml");
            if (toml != null) {
                String text;
                try (InputStream in = jf.getInputStream(toml)) {
                    text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                Manifest mf = jf.getManifest();
                String jarVersion = mf == null ? null : mf.getMainAttributes().getValue("Implementation-Version");
                Matcher block = MOD_BLOCK.matcher(text);
                while (block.find()) {
                    Matcher id = MOD_ID.matcher(block.group(1));
                    if (!id.find()) continue;
                    Matcher v = VERSION.matcher(block.group(1));
                    String version = v.find() ? v.group(1) : null;
                    if (version != null && version.contains("${")) version = jarVersion;
                    mods.add(new Mod(id.group(1), version));
                }
            }
        } catch (IOException e) {
            return new ModJar(jar, List.of());
        }
        return new ModJar(jar, mods);
    }
}
