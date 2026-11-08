package evo2restore;

import java.util.Map;

/** FTB Evolution 2 1.0.0 (100549) files that the integration step merges from, pinned by SHA-1. Generated from the manifest. */
final class IntegrationSources {
    private static final String BLOB = "https://files.feed-the-beast.com/blob/";

    private static Sources.Remote blob(String hash, String ext, String sha1) {
        return new Sources.Remote(BLOB + hash.substring(0, 2) + "/" + hash + "." + ext, sha1);
    }

    static final Map<String, Sources.Remote> OLD = Map.ofEntries(
            Map.entry("config/ftbmaterials/unifier-db.json", blob("5eab70092a18135c7c36b0c4bf6be7d9e36ec92c4a8035c15848f2ed862f32fc", "json", "299baeeffe6f3b8e8ebd3542ece1bf64d1f57bc3")),
            Map.entry("config/ftbmaterials-startup.json5", blob("3f6c81100625345238a8657e5811a4b73dee51a900c002f3adc7f91f3a3684f7", "json5", "31bc8613d031b3edfe2b99fd973e371ccc16ae11")),
            Map.entry("config/sdrp-client.json5", blob("cfce5e0d42eb239abc54f73da953792257bb7cd04b64fab2096484701ec64fa2", "json5", "2f650a0c7daa35912ee8d1588ba268b5e592ccee")),
            Map.entry("config/servercore/config.yml", blob("8f4c79e76f8187faa1e4dc58504b5725843f8b7803256152c337b0c1d47eb7ec", "yml", "cdc612b019110a3fb15eead473afd9e8fd55648d")),
            Map.entry("config/carryon-common.toml", blob("dce09452dc8d8ee51cda696786cc92b55747f4afb9050dfc84c5f16cddc7f4e9", "toml", "6dbb55f6e68bec630685c94c640cb19305398bd0")),
            Map.entry("config/dankstorage-server.toml", blob("ca4464002ffc52e0a43cd485536d12288f52634939d961af22ec2064b0dbddbd", "toml", "e452608b8bda0cfd0213870d6c45106b6a30b12e")),
            Map.entry("config/mezz_config_gui/client/mod-navigation.ini", blob("7973e94c14a9733e40e0d153685955a075e9ae308c847c3b510b8f6173826b8b", "ini", "42fba4d07bc5560c7c5ac41c6f69dc7e95a1b649")),
            Map.entry("config/ftbquests/quests/reward_tables/6e00621943707265.json5", blob("a790d093013b60849c3a76d0eb08b35d4889eab607192a9d2126f22e48553c40", "json5", "f9f2b346016c6a3da1b705b73d171d65b2231b13")),
            Map.entry("config/ftbquests/quests/reward_tables/3162793db33d4e0a.json5", blob("a640fa2f70ad144f99ad8dc9bb596573d9f47e8227c4cd7745430b2dd1775025", "json5", "f6976441d9c0e97b2ac05ec859182e0646395fc6")),
            Map.entry("config/mysticalcustomization/crops/amber.json", blob("31731dd93a0e67a14c43fcfc1710b47554fb8947feed5cfc092dc2ab6b623a76", "json", "19e15ecd7d9eea7f99bdcd20ac0e589e380ce6ce")),
            Map.entry("configureddefaults/options.txt", blob("85a48bb73355c83122ab2037b6999459880a5597f6ff3183e837d541eb245117", "txt", "e65e02ca4e78061a978f5084821dfd541c1f4a6d")),
            Map.entry("configureddefaults/config/jade/jade.json", blob("197d6b276e97a8830070e039fba6a02a9a9cc2dd2d826344e2b4d98afc2c7001", "json", "372a577cc1ccd127b380b4ce8d0e3a4be2aa025b")),
            Map.entry("configureddefaults/config/jade/profiles/1/jade.json", blob("a8b17bd93dbdab82324e80fa8d5138f081ca93d144d99a3fea880a4d881cfede", "json", "5042559a4605bff9c9a05908cbc71316d6f0ea95")),
            Map.entry("configureddefaults/config/jade/profiles/2/jade.json", blob("90a4209afaaf699de7435d23692ea2a912ec02efb883db7d791bf47659a70d37", "json", "e922a8b22145db3e6698814c4f5b931a19686b56")),
            Map.entry("configureddefaults/config/jade/profiles/3/jade.json", blob("dd4d9eca0e1628df188bbc8a2172e01b47cb4d04cc4cb7aeda5c6c613af4c18f", "json", "481599478ffa9fe0972833e3999659aa913e3b2a")),
            Map.entry("configureddefaults/config/jade/sort-order.json", blob("b80aa05e8f2fcca4565ef9d427daea1b7b373592266800ce50d59d125c6d6b72", "json", "9a292b814536a006e85a64bd97754bac06fcff3b")),
            Map.entry("configureddefaults/config/jei/client/ingredient-list-mod-sort-order.ini", blob("4ae109f16566a95fd390efb44ec250a706246648f36e79625205e4c5f74ec81b", "ini", "b6b3672f06da41388b39960f1d440244d85e4599")),
            Map.entry("configureddefaults/config/jei/client/ingredient-list-type-sort-order.ini", blob("2d59c5f2c06a93afc3672f5378218cc2cbd474d4d1df9533fe831d133d15de54", "ini", "98b10d8b7ba96d86a51895a4efb6977a451b1693")),
            Map.entry("configureddefaults/config/jei/client/recipe-category-sort-order.ini", blob("5bafce759f839bba4c490e8d397c31e96265f81194b0867a112b8e65585ea377", "ini", "7a002b36da289885773da66eb50069ecd2c8c4e3")),
            Map.entry("configureddefaults/config/jei/recipe-category-sort-order.ini", blob("34a0261d87c550b4e2a4aac5975bf404726f132115d2e504ee88d617a117e27a", "ini", "6523becfb1257a27c457d1af061d1a1020fe0437")),
            Map.entry("datapacks/ftb-unification/data/apothic_enchanting/loot_table/boon_deepslate_drops.json", blob("82acbe926f9d8301f4b30702867da007e35ad5e38a3c59f86fa7d558f116ef23", "json", "8340ff7091ba01b174e5f93f03294ddfd87c17e2")),
            Map.entry("datapacks/ftb-unification/data/apothic_enchanting/loot_table/boon_stone_drops.json", blob("e1e1b49158661e4d353908c92112d481da434b97b1494d304de01e7494a4feba", "json", "065fc37c0107be01c7e430c00ebeee093e680180")),
            Map.entry("datapacks/ftb-unification/data/c/tags/item/hidden_from_recipe_viewers.json", blob("c06f408304bfc542285480e79d2fc5a6a1b9990201f02d09abd5d5fd1b7b5eda", "json", "6d4926fa57b058d310b5efbff151cceff4957dad")),
            Map.entry("datapacks/ftb-unification/data/c/tags/item/ore_source/overworld/cinnabar.json", blob("62882f8c12cc9fcc77d173dee2ec723f8b2f3fbc8e008b33ff31c51300b58d54", "json", "812dfbd9625634d068ac564f823f586dd0a23577")),
            Map.entry("datapacks/ftb-unification/data/c/tags/item/ore_source/overworld/quartz.json", blob("77ec13fff05f10c5153d1e074235323a83952f2da47f599a099f4fc6c8c317d1", "json", "f31090da80524ca0d410e7d79fd219c7f8ca76d8")),
            Map.entry("datapacks/ftb-unification/data/c/tags/item/ore_source/overworld/amber.json", blob("5cb61a960d3ef7b2a153003e2536e813a2524f8abb0fcd2d0ff3aefda040cc4a", "json", "226d16178b7ba5ef4b820cffe7356a313353aebe")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/enderio/sag_milling/ores/stone/gem_ore_to_gem/amber.json", blob("d8f9041fe3d9906cc279608ef4936e4e77c627da7a4cc9306684199233b8e134", "json", "09955772bcf349c0f51f0cbe8e5a96edeac02238")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/energizedpower/pulverizer/overworld/ore_to_gem/amber.json", blob("b6d37f05e795e76cba9f95cb9ae16b958fb99b7ac12c94fd23f984613af3c4f0", "json", "1a7d94195849f891dcc644f03573bbda97deb2fa")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/ftbic/macerating/overworld/gem_ore_to_gem/amber.json", blob("f378a45dbe974679663fbe5cb19fe0a39eafa81ac433a745b17da2a991ff22d4", "json", "6cd2f7aeed6eee8af1fc36537e8e6af88d67caf1")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/horsepowered/grinding/overworld/gem_ore_to_gem/amber.json", blob("895914323161d1e1a511b6ebaab7a7726d1f96b12b90348cbacb4c07e1369c3a", "json", "3a71bef9050809869e15f8285a8c1ce884f81df0")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/integrateddynamics/mechanical_squeezer/overworld/gem_ore_to_gem_standard/amber.json", blob("5304c5ec0edd7afc1cf077dc75ce476a9aa611a65ef73c92c954eb7ae3e35d5e", "json", "d7df3b67a7252ce579a5959d6e355487e55f9ae4")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/integrateddynamics/squeezer/overworld/gem_ore_to_gem_standard/amber.json", blob("ad4dd3a0570a351cbcdf7d5b6d463a6d3186c4720bcc434e6563302f5ee233ed", "json", "3ed6aee2605f33f782afd5b598979d48aa150d96")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/ltxi/grinding/overworld/gem_ore_to_gem/amber.json", blob("7b92dd1f43b0ed3957076f382c5aa4b5e2f910541cfa0247331c27b0f1b646fc", "json", "586ffdb98443db1a2be92e0c84c1892ae1d1ac95")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/occultism/crushing/overworld/gem_ore_to_gem/amber.json", blob("bc4b37391fa52334d2cdc2267de9c55067abeb5db482211d9a89d018e505bf1e", "json", "e561d227a1222bb229114c1cf76d386f431ce4b8")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/oritech/fragment_forge/overworld/gem_ore_to_gem/amber.json", blob("b0ea0a7ef0c6cd225c14a0080a6035a78b0dcb6e06b56371561b90ef66532b83", "json", "fe01350ea8bad8a234ebaf0d68c8fbc04135c553")),
            Map.entry("datapacks/ftb-unification/data/ftb/recipe/unification/oritech/pulverizer/overworld/gem_ore_to_gem/amber.json", blob("03703f8143036bfd7493d5dff0b228f8dc1931b292134c0e722ac9fd9a99f051", "json", "cc7dff8e047c3aa970e9bde751c019a633c61664")),
            Map.entry("datapacks/ftb-unification/data/thaumaturge/recipe/nugget_brass.json", blob("84ab2191e6a79d29b8c737ebec074fe0ba188bc743175414ced07fc1343b5977", "json", "70307fd5fadfe68791da4712372ae99bd2df8917")),
            Map.entry("datapacks/ftb-unification/data/thaumaturge/advancement/recipes/misc/nugget_brass.json", blob("2c40a78e462c238229f8fba7bfb279ee6bea6a1e657ef128c88e41b0a8cef4de", "json", "5a09f5804dbec1b4ca4e6ebec8b0bba47d11f97f")),
            Map.entry("datapacks/ftb/data/bhc/recipe/blade_of_vitality.json", blob("b5aeca0a49efc2acb86a6ff944236ccbf7ed18405491118e40b2a305e805d72a", "json", "0d46c5f68c9ed8b1b706b537d2c28266844058b9")),
            Map.entry("datapacks/ftb/data/bhc/recipe/vigor_bow.json", blob("d6e92e4db9e80a48d7b99f85d7790137246fea63ec13b057debbb91a6b0f81d0", "json", "278aaeee9f3c1f1b7f6e3705a67c44b427d55f70")),
            Map.entry("datapacks/ftb/data/bhc/recipe/blue_heart_infusion.json", blob("9dcbc20f7eec747520daab539d0a35ad206b54b04f48f861d920ce7769f8bcd7", "json", "1178c655457ecd073edb8e0b105fe77947e6ff11")),
            Map.entry("datapacks/ftb/data/dimresources/recipe/crystalore.json", blob("c225e47dddf5bb35af57cd31723e59522e50f86334a3722fbd894470429a2ccf", "json", "def84b675fbc62ede8d1ea888f01496740e03f04")),
            Map.entry("datapacks/ftb/data/ftb/echo_definitions/arcanist.json", blob("7cdac4a3106013c42d702585f8eb1d11a2026fb9be5df7d1f3673bed972ba7c0", "json", "4577eaf6d4fccd3ff3b5146005420c4c0091d6dd")),
            Map.entry("datapacks/ftb/data/ftb/recipe/fabricating/causal_anchor.json", blob("91fceb5e2d54042e7d082f77ce7f622dd674357f735124b6a29e2ae6fcc05459", "json", "d70c9dd0c20404a40339b0b87725279f9814073b")),
            Map.entry("datapacks/ftb/data/ftb/recipe/mysticalagriculture/essence/amber.json", blob("412c3ac077c1b0ed4db11c6f00d4d6fe470d4d5e464d3a33249934d49e01f367", "json", "f51e16a2de9a5341b2917687a4b31f4036da60fa")),
            Map.entry("datapacks/ftb/data/hostilenetworks/hostilenetworks/data_models/blocks/cinnabar_ore.json", blob("9ce434c643719bde7c4af11a24d37ec0620dec99765c80c56f16a10624163770", "json", "79681881b8ff582d9ebb00262f45f791015bd534")),
            Map.entry("datapacks/ftb/data/hostilenetworks/hostilenetworks/data_models/blocks/nether_quartz_ore.json", blob("da98a0472fe78d64fff6eb8fb9d9fbaca5fc037ca626e35b2f0b829a2fdd0c42", "json", "01694e28d3e2840d6cdc597f3cc4c3cfbbb6a4d6")),
            Map.entry("datapacks/ftb/data/puffish_skills/puffish_skills/categories/magic/definitions.json", blob("c757c34b80cd6e797aed1f11b3875aa17cb21fbe66e753cedc93672b3f7778cf", "json", "f6f98dc5d7e176ce794e16e36b060b08ebb1dc78")),
            Map.entry("kubejs/assets/ftb/lang/en_us.json", blob("e1fa55128415a450217f286fa6ed89071fb85bd03352c31e9f6afcc4218160ca", "json", "4b1bf84bcd15be5e617f7b0f067cba8918a491fc")),
            Map.entry("kubejs/server_scripts/systems/blocks.js", blob("a3fa8f052539767fd25c76b250c16a27685efd498fb61d899613718b8ce044ab", "js", "596c9500759d64932c5f9c498c5713f558c87592")));

    private IntegrationSources() {}
}
