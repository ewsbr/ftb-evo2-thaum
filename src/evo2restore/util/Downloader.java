package evo2restore.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** Fetches files by URL and SHA-1, keeping verified copies in a local cache. */
public final class Downloader {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    private static final int ATTEMPTS = 3;

    private final Path cacheDir;
    private final boolean offline;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public Downloader(Path cacheDir, boolean offline) {
        this.cacheDir = cacheDir;
        this.offline = offline;
    }

    public byte[] fetch(String url, String sha1) throws IOException, InterruptedException {
        Path cached = cacheDir.resolve(sha1);
        if (Files.isRegularFile(cached)) {
            byte[] data = Files.readAllBytes(cached);
            if (Hashes.sha1(data).equals(sha1)) return data;
            Files.delete(cached);
        }
        if (offline) throw new IOException("not in cache and --offline is set: " + url);

        IOException last = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                byte[] data = get(url);
                String actual = Hashes.sha1(data);
                if (!actual.equals(sha1)) {
                    throw new IOException("SHA-1 mismatch for " + url + ": expected " + sha1 + ", got " + actual);
                }
                store(cached, data);
                return data;
            } catch (IOException e) {
                last = e;
            }
        }
        throw last;
    }

    private byte[] get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " for " + url);
        return response.body();
    }

    private void store(Path target, byte[] data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), "dl", ".tmp");
        Files.write(tmp, data);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Path defaultCacheDir() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return Path.of(local != null ? local : home, "evo2-thaumaturge-restore", "cache");
        }
        if (os.contains("mac")) return Path.of(home, "Library", "Caches", "evo2-thaumaturge-restore");
        String xdg = System.getenv("XDG_CACHE_HOME");
        return Path.of(xdg != null && !xdg.isBlank() ? xdg : home + "/.cache", "evo2-thaumaturge-restore");
    }
}
