package evo2restore.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites {@code .mcfunction} commands that fail when their selector matches nothing into {@code execute if entity}
 * forms that do nothing instead.
 *
 * A failing command builds a {@code CommandSyntaxException} and its translated message, and a function that runs
 * every tick for every player turns that into hundreds of exceptions a second. The guarded form runs the same command
 * on the same entities.
 */
public final class FunctionGuard {
    // Matches:  tag @s[gamemode=survival] add in.survival_player   tag @s remove in.valid_player
    private static final Pattern TAG = Pattern.compile("^(\\s*)tag (@[aesp])(\\[.*])? (add|remove) (\\S+)\\s*$");
    // Matches:  scoreboard players remove @s[scores={in.cd_shield=1..}] in.cd_shield 1
    private static final Pattern SCORE = Pattern.compile("^(\\s*)scoreboard players (add|remove|set) (@[aesp])(\\[.*?]) (\\S+ -?\\d+)\\s*$");

    private FunctionGuard() {}

    public static String rewrite(String function) {
        return java.util.Arrays.stream(function.split("\n", -1))
                .map(FunctionGuard::rewriteLine)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    static String rewriteLine(String line) {
        Matcher tag = TAG.matcher(line);
        if (tag.matches()) {
            String tagName = tag.group(5);
            String selector = withFilter(tag.group(2), tag.group(3),
                    (tag.group(4).equals("add") ? "tag=!" : "tag=") + tagName);
            return tag.group(1) + "execute if entity " + selector + " run tag " + selector + " " + tag.group(4) + " " + tagName;
        }
        Matcher score = SCORE.matcher(line);
        if (score.matches() && balanced(score.group(4))) {
            String selector = score.group(3) + score.group(4);
            return score.group(1) + "execute if entity " + selector + " run scoreboard players " + score.group(2) + " "
                    + selector + " " + score.group(5);
        }
        return line;
    }

    private static String withFilter(String base, String brackets, String filter) {
        if (brackets == null) return base + "[" + filter + "]";
        return base + brackets.substring(0, brackets.length() - 1) + "," + filter + "]";
    }

    private static boolean balanced(String brackets) {
        int depth = 0;
        for (int i = 0; i < brackets.length(); i++) {
            char c = brackets.charAt(i);
            if (c == '[' || c == '{') depth++;
            if (c == ']' || c == '}') depth--;
            if (depth == 0 && i < brackets.length() - 1) return false;
        }
        return depth == 0;
    }
}
