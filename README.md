# FTB Evolution 2 Thaumaturge Restore

An installer that adds Thaumaturge back to FTB Evolution 2.

FTB Evolution 2 shipped [Thaumaturge](https://github.com/Leclowndu93150/Thaumaturge) and Applied Thaumaturge in
1.0.0 and removed them in 1.0.1. This installer puts back both mods, the Thaumaturge quest chapter and the rest of
the pack's Thaumaturge content, into any later version of the pack. Content added in newer pack versions is left
unchanged.

It runs on Linux, Windows and macOS, for client instances and dedicated servers.

## Requirements

- FTB Evolution 2 **1.0.1 or later** on Minecraft 26.1.2
- Java 17 or newer. Minecraft launchers bundle a Java runtime, and the install scripts find it.
- An internet connection on first run. Mods and pack files are downloaded from the FTB and CurseForge CDNs.

## Quick start

Close the game, then run the script for your system from the folder that contains the jar:

```bash
./install.sh            # Linux and macOS
```

```bat
install.cmd             :: Windows. Double-clicking it also works.
```

The installer finds your FTB Evolution 2 instances, shows every change it will make and asks before writing
anything. Start the game once afterwards. On a multiplayer server, install on the server and on every client.

To preview without changing anything:

```bash
./install.sh --dry-run
```

To undo:

```bash
./install.sh --uninstall
```

## Usage

```
install.sh [options] [INSTANCE]
java -jar evo2-thaumaturge-restore.jar [options] [INSTANCE]
```

`INSTANCE` is a launcher instance folder, its `.minecraft` or `minecraft` folder, or a server folder. Without it,
the installer searches the default instance folders of Prism Launcher (including Flatpak), MultiMC, PolyMC, the
FTB App, CurseForge, the Modrinth App, ATLauncher and GDLauncher.

| Option | Effect |
|---|---|
| `--dry-run` | Show the changes and write nothing |
| `--uninstall` | Revert every change the installer made |
| `--yes`, `-y` | Do not ask for confirmation |
| `--quests-only` | Restore only the mods and quests, not the recipe, loot, skill and config content |
| `--no-pyramid` | Leave the Pyramid's Rune Carving quest as your pack version has it |
| `--force` | Continue on an unrecognized Minecraft version |
| `--offline` | Use only files already in the download cache |
| `--cache DIR` | Use another download cache folder |
| `--list` | List the FTB Evolution 2 instances found |

## What it changes

**Mods**
- Thaumaturge 1.1.0, with a fix for a world-load crash
  ([#617](https://github.com/Leclowndu93150/Thaumaturge/issues/617)) applied during install. If you already have a
  newer Thaumaturge, it is kept.
- Applied Thaumaturge 1.0.0.

**Quests**
- The Thaumaturge chapter, placed before Witchery in the Magic group.
- The 13 Applied Thaumaturge quests in General Storage, linked to the Thaumaturge and AE2 quests they depend on.
- The Pyramid's Rune Carving quest asks for the Thaumaturge items again. The Witchery Rune Plate recipe still works.

**Pack content**
- FTB Materials unification of Thaumaturge's brass, cinnabar and quartz.
- Amber: ore processing in every grinder and crusher, the Mystical Agriculture amber crop, and amber drops from
  Apotheosis boons and Dimensional Resources.
- Thaumaturge ingredients in the Blade of Vitality, Vigor Bow and Causal Anchor recipes, and the Blue Heart
  infusion recipe.
- The Thaumaturge path in the Magic skill tree, in English and Brazilian Portuguese.
- Thaumaturge items in the arcane and sapling loot crates and in the Arcanist's shop.
- Carry On, ServerCore, Dank Storage, Discord status, JEI and Jade settings for Thaumaturge.
- Key bindings: Thaumaturge keys whose defaults collide with another binding get the pack's 1.0.0 bindings, for
  example Change Focus on Ctrl+F instead of F. Bindings you changed yourself are left alone.

The installer adds only what is missing. Re-running it changes nothing, and after a pack update it re-applies only
what the update removed. Before writing, it checks the resulting quest book for duplicate IDs, missing quests and
item IDs that the installed mods do not provide. If a check fails, it writes nothing.

## Safety and uninstalling

Every file the installer replaces or disables is backed up to `thaumaturge-restore/` in the game folder, along with
a journal of the changes. `--uninstall` uses that journal to restore the original files. It leaves alone any file
that changed after the install, such as one updated by the pack, and reports it. If an install fails partway, it
rolls back automatically.

All downloads are checked against pinned SHA-1 hashes. No mod or pack files are included in this repository or in
the built jar.

## Compatibility

Tested on every FTB Evolution 2 release from 1.0.1-rc.1 to 1.8.1, and with a dedicated server boot on 1.8.1.
Windows and macOS support is implemented but has not been tested yet. Reports are welcome.

## Building

Requires a JDK 17+ and `curl`.

```bash
./build.sh                                              # writes dist/evo2-thaumaturge-restore.jar
java -cp dist/evo2-thaumaturge-restore.jar test/Json5Test.java
```

To package a release, put `dist/evo2-thaumaturge-restore.jar`, `install.sh`, `install.cmd` and `install.ps1` in one
folder.

## Credits

Thaumaturge is by Leclowndu93150 and contributors. FTB Evolution 2 is by Feed The Beast. This project is not
affiliated with either.

The jar bundles [ASM](https://asm.ow2.io/) 9.9.1, distributed under the BSD license in `LICENSE-ASM.txt`.
