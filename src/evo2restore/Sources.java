package evo2restore;

import java.util.List;

/** Every remote file the restore uses, pinned by SHA-1. Quest content comes from FTB Evolution 2 1.0.0 (100549). */
final class Sources {
    record Remote(String url, String sha1) {}

    /** A file copied verbatim from 1.0.0 into the game directory. */
    record Copied(String path, Remote remote) {}

    private static final String BLOB = "https://files.feed-the-beast.com/blob/";

    static Remote blob(String hash, String ext, String sha1) {
        return new Remote(BLOB + hash.substring(0, 2) + "/" + hash + "." + ext, sha1);
    }

    static final Remote THAUMATURGE_JAR = new Remote(
            "https://edge.forgecdn.net/files/9428/45/thaumaturge-26.1.2-NeoForge-1.1.0.jar",
            "a95554503a7cf9d8e6ca6468ce2f8e1032ee7417");
    static final String THAUMATURGE_PATCHED_NAME = "thaumaturge-26.1.2-NeoForge-1.1.0-patched.jar";

    static final Remote APPLIED_JAR = new Remote(
            "https://edge.forgecdn.net/files/9090/736/appliedthaumaturge-1.0.0.jar",
            "e6f9cb1d198e7fbdf6d065a5f64c937119c72683");
    static final String APPLIED_NAME = "appliedthaumaturge-1.0.0.jar";

    static final Remote OLD_GENERAL_STORAGE = blob(
            "a30f4ab9b4de38ed6aed2d538505794b80d050d4f432a14ef801b7f7ced633ea", "json5",
            "287f4471b71144f3b6aeafa85796745af4fe30ac");
    static final Remote OLD_GENERAL_STORAGE_LANG = blob(
            "d48af0f8cb5d36d08b7c695218107c849c3b0073d2020ed7cf1b5031b2419ee9", "json5",
            "549c018b6c522964a5f58309695466edeac7121a");
    static final Remote OLD_PYRAMID = blob(
            "63d2670e9d34131fdcbf361ac212d6ae964bb8dd640b99a76cd8ea6ba099417b", "json5",
            "3c2a3bc11176d5ca37a1b9bba4185c7660d54eb4");
    static final Remote OLD_PYRAMID_LANG = blob(
            "49efa75531dbce96da49f8f99410d445439c21563870c7bc07be99b1e59857eb", "json5",
            "be2ed6d795ad75620330fd8e434e69ed792a2682");

    static final String RECIPES = "datapacks/ftb/data/ftb/recipe/feed_the_beast/";
    static final String TEXTURES = "kubejs/assets/ftb/textures/quests/";

    static final List<Copied> COPIED = List.of(
            new Copied(Quests.chapter(Quests.THAUMATURGE), blob(
                    "b6cccda7db667d21c3468f9a8755104fbfdf1ca3ebe0aadc43bd6c7b310c85b0", "json5",
                    "12fd98bebbcf2c4ef43b48caae1965bc41def622")),
            new Copied(Quests.chapterLang(Quests.THAUMATURGE), blob(
                    "ee5d14bf75a2701d19b385b7890be7cbd068d19eb7f58edc3cc73b400a85a4c6", "json5",
                    "4d200a428f0c316394707dd2fd3e37f05aca59d5")),
            // 1.0.0 shipped this as rune_plate.json. Newer packs use that name for the Witchery recipe.
            new Copied(RECIPES + "rune_plate_thaumaturge.json", blob(
                    "48afc3210683522c6498bc1fec9f78e4b7623980f907fceca80515dcee81e58b", "json",
                    "0529d985585dc928d337862c342ae47cc1e4bcf3")),
            new Copied(TEXTURES + "titles_and_logos/thaumaturge.png", blob(
                    "34154b996f4b7d026304c12be9cb2e8fcd3035e0d51c13e0fd1d1dfc361af557", "png",
                    "84c2479cff147bb5a2c33b2d0a7572898e59de90")),
            new Copied(TEXTURES + "titles_and_logos/thaumaturge_title.png", blob(
                    "fce1b8e60f557fbcfb177c3345146ad0b090e8f22d8ab53933e58930b0eaba78", "png",
                    "c3a48a25c304992af1eac54069985fda3a30a6df")),
            new Copied(TEXTURES + "renders/thaum_alchemy_rig.png", blob(
                    "adc08bc0a0e4f8569fba26ee4bcd0dfec0af2943c4e89bb4bf1baea3cc706e1d", "png",
                    "30b9b8b1e7a9f5b5396596c7b8535dd71588a5c9")),
            new Copied(TEXTURES + "renders/thaum_brass_golem.png", blob(
                    "1cb7edf8fe89a0369db476a0f8032a174669b6d47f705a8ab1fce94d11eeb09e", "png",
                    "f9c374d435b77a5823f5074b0b56351bda3405e5")),
            new Copied(TEXTURES + "renders/thaum_crimson_knight.png", blob(
                    "108a99d09eab44878d1b9ebd12790fd6cbb5af6fe3d17111189f5af091e9637a", "png",
                    "3c2f56947965b41060032043a47f38abc0f74899")),
            new Copied(TEXTURES + "renders/thaum_infusion_altar.png", blob(
                    "bb09a565966b2deca81d7ef0a0f32b94270d91ab96a4cae631cf4511158e77bd", "png",
                    "6b879f1a49928ece4d75656329f71653163a9866")),
            new Copied(TEXTURES + "renders/thaum_vis_crystals.png", blob(
                    "15383b705e31607822ffff242f8c651ff9118fef20a570adbbfd4f8521e6e8e9", "png",
                    "531f6bd4dab41eea9e2a197b8274aeaa082e604d")),
            new Copied(TEXTURES + "renders/at_infusion_provider.png", blob(
                    "44f692b1f4c484cb781cfeafb1ecc76eb158353bc2f36e434de5c2abd87d2a4b", "png",
                    "8fcd26791d0a9a3fb70c2d04611ff8dd214551a3")));

    private Sources() {}
}
