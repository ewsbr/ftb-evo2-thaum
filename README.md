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

## Design decisions

<details>
<summary>Why the installer works the way it does</summary>

### General approach

- **One Java program for every platform.** Every machine that runs Minecraft already has Java, so a single jar
  works on Linux, Windows and macOS without Python or other tools. It targets Java 17, so older system Java
  installs work too. The shell, PowerShell and batch scripts only locate a Java runtime and start the jar.
- **Merge, don't overwrite.** Each pack version ships different quest, recipe and config files. Copying 1.0.0 or
  1.8.1 files over an install would erase newer content. Every change is a merge into the file that is installed.
  This is also what makes the installer work across versions and re-runnable after pack updates.
- **Restore 1.0.0, keep later improvements.** The goal is what the pack would look like if it had kept
  Thaumaturge. The installer merges in the 1.0.0 Thaumaturge entries that were removed. Where a later version
  swapped a Thaumaturge item for a stand-in, that one value goes back. Where a later version redesigned a system,
  the redesign stays.
- **Download, don't redistribute.** Mods and pack files come from the official FTB and CurseForge CDNs at install
  time and are checked against pinned SHA-1 hashes. The repository and jar contain none of them. Requests use a
  generic browser User-Agent and send nothing that identifies the user.
- **Keep the files readable.** Edits are spliced into the original text. Comments, indentation, key order and
  one-line formatting in untouched parts of a file are kept, and new entries copy the style of their neighbours.

### Mods

- **Thaumaturge 1.1.0 instead of 1.0.2.** 1.0.0 shipped 1.0.2, but its JEI plugin calls a JEI method signature
  that current JEI no longer has, so Thaumaturge recipes disappear from JEI. 1.1.0 works with current JEI.
- **Patching 1.1.0 at install time.** 1.1.0 crashes on world load when a modded crafting recipe returns null from
  `assemble(CraftingInput.EMPTY)`, which Cyclops Core's custom-output recipes do
  ([#617](https://github.com/Leclowndu93150/Thaumaturge/issues/617)). The installer adds null checks to two
  methods with ASM and verifies the patched classes against known hashes. Patching locally avoids distributing a
  modified jar.
- **A newer Thaumaturge is kept.** If a jar newer than 1.1.0 is installed, the installer assumes an official
  release supersedes the patch and leaves it in place. Duplicate jars of either mod are disabled so only one
  version loads.
- **The pack's "absent" datapack is disabled.** Later versions ship `datapacks/thaumaturge-absent`, which hides
  every Thaumaturge item from recipe viewers. It has to be off while the mod is installed.

### Quests

- **The 1.0.0 chapter as shipped.** The Thaumaturge chapter and its text are copied unchanged from 1.0.0. It goes
  directly before Witchery in the Magic group, where it was in 1.0.0, and later chapters move down one.
- **Applied Thaumaturge quests are placed where they fit.** Later versions added quests to General Storage in the
  area where the Applied Thaumaturge branch was. The installer moves the branch down by the smallest offset at
  which nothing overlaps.
- **Rune Carving gets its Thaumaturge tasks back.** The quest asks for the 1.0.0 items again, with the 1.0.0 icon
  and description. Its position, prerequisite and the newer rewards stay. The 1.0.0 Arcane Workbench recipe for
  Rune Plates is added as a second recipe, so the newer Witchery recipe still works. `--no-pyramid` skips this.
- **Validation before writing.** The installer loads the whole resulting quest book first. It refuses to write if
  the result has new duplicate IDs, dependencies on missing quests, restored quests without text, or quest items
  the installed jars do not provide.

### Pack content

- **Cinnabar stays as redesigned.** Later versions replaced Thaumaturge's cinnabar with FTB Materials cinnabar
  gems in every machine, and quicksilver comes from a gem recipe that loads with Thaumaturge. That system is
  consistent, so the 1.0.0 cinnabar outputs and ore drops are not restored.
- **Content that already loads with the mod is not duplicated.** Later versions keep some Thaumaturge content in
  files that load only when the mod is present: amber ore generation, Apotheosis bosses and Productive Bees bees.
  Adding the 1.0.0 copies as well would break things. A second amber ore feature, for example, makes world
  generation fail with a feature order cycle.
- **Recipe ingredients are reverted in place.** The Blade of Vitality and Vigor Bow use Liquid Death again, and the
  Causal Anchor uses Thaumium. These ingredients were changed only because the mod left. Other changes to those
  recipes stay.
- **Restored files are added only where the pack has none.** This covers the Blue Heart infusion, the amber
  processing recipes and crop, and the brass nugget override. Existing files are never overwritten.
- **FTB Materials unification entries are restored by hand.** FTB Materials does not rebuild its unifier
  database at runtime. Without the 1.0.0 entries, Thaumaturge's brass, cinnabar ore and quartz ore would not
  merge with their FTB Materials equivalents.
- **The Thaumaturge skill path comes back.** Later versions turned it into a "Sage" path with Ars Magica and
  enchanting rewards. The installer restores the 1.0.0 icons, rewards and text for those skills. Their bonuses still
  work: vis discount comes from Thaumaturge, and focus power, warp ward and infusion stability from the pack's
  companion mod.
- **Loot crates get both sets of items.** Later versions replaced the Thaumaturge items in the arcane crate with
  Ars Magica and Auroral items. Both sets are kept. Restored entries whose 1.0.0 IDs were reused get new
  deterministic IDs.
- **New text where 1.0.0 text no longer fits.** Later versions still unlock the Arcanist with a Thaumometer, but
  the hint only names Vinteum Dust. The new hint names both. 1.0.0 had no Brazilian Portuguese text, so the
  restored skills get new pt_br translations.
- **The Creative Node Placer script is its own file.** In 1.0.0 it was all of `blocks.js`, a file later versions
  reuse for other blocks. Pack versions that still have the old handler do not get a second copy, which would
  consume the item twice.
- **Carry On entries are corrected.** 1.0.0 listed Thaumaturge's multiblock parts under the `thaumcraft:`
  namespace, so they never matched. They are restored as `thaumaturge:`.
- **Not restored:** the Hephaestus thaumium material, since later versions removed the whole Hephaestus material
  system, and Thaumaturge's aspect index cache, which the mod rebuilds itself.

### Settings

- **Key bindings change only on a collision.** Thaumaturge's default keys collide with other bindings, for example
  Change Focus and Swap Offhand both on F. For each colliding key, the installer applies the 1.0.0 pack binding.
  Keys the player rebound are left alone.
- **JEI, Jade and key defaults go into `configureddefaults`.** Those files only apply to new instances, which
  matches how the pack ships them. In existing instances, JEI and Jade add Thaumaturge to their own settings on
  first launch.

### Safety

- **Every change can be undone.** Replaced and disabled files are backed up with a journal. Uninstall restores
  only files that are still exactly what the installer wrote, and reports any that changed since. A failed install
  rolls itself back.
- **Unsupported installs are refused.** 1.0.0 and 1.0.0-rc.1 already ship Thaumaturge. A Minecraft version other
  than 26.1.2 needs `--force`, since the pinned jars are built for 26.1.2. Missing required mods (FTB Quests, AE2,
  Curios, Lithostitched) are reported.

</details>

## Credits

Thaumaturge is by Leclowndu93150 and contributors. FTB Evolution 2 is by Feed The Beast. This project is not
affiliated with either.

The jar bundles [ASM](https://asm.ow2.io/) 9.9.1, distributed under the BSD license in `LICENSE-ASM.txt`.

## License

MIT. See `LICENSE`.
