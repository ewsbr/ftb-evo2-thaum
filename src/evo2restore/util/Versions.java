package evo2restore.util;

public final class Versions {
    private Versions() {}

    /** Compares dotted numeric versions; qualifiers after '-' or '+' are ignored. */
    public static int compare(String a, String b) {
        String[] x = a.split("[-+]", 2)[0].split("\\.");
        String[] y = b.split("[-+]", 2)[0].split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int c = Long.compare(part(x, i), part(y, i));
            if (c != 0) return c;
        }
        return 0;
    }

    private static long part(String[] parts, int i) {
        if (i >= parts.length) return 0;
        String digits = parts[i].replaceAll("\\D.*", "");
        return digits.isEmpty() ? 0 : Long.parseLong(digits);
    }
}
