<p align="center">
  <img src="assets/wizard_dance_fframes.gif" width="576" alt="Three pixel-art wizards in purple, green and red robes dancing in a moonlit forest, each holding a wand">
</p>

# FTB Evolution 2 - Thaumaturge Restore

An installer that adds Thaumaturge back to FTB Evolution 2. The nostalgia must not stop.

FTB Evolution 2 shipped [Thaumaturge](https://github.com/Leclowndu93150/Thaumaturge) and Applied Thaumaturge in
1.0.0 and removed them in 1.0.1 due to [copyright issues with Team CoFH](https://x.com/FTB_Team/status/2108144492410757300).

This installer adds back both mods, integrating the Thaumaturge quest chapter and rest of the removed content
into the later versions, without losing the newer improvements and fixes.

This is a hack and required multiple manual patches. I really hope FTB manages to reintegrate Thaumaturge into the pack.

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

The installer adds only what is missing. After a pack update it re-applies only
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

## Pack decisions

<details>
<summary>What was restored, what was kept from newer versions, and why</summary>

The aim is the pack as it would be if it had kept Thaumaturge: 1.0.0's Thaumaturge content, plus every
improvement made since. Where a later version replaced a Thaumaturge item with a stand-in, the Thaumaturge item
comes back. Where a later version redesigned a system, the redesign stays.

### Mods

- **Thaumaturge 1.1.0 instead of 1.0.0's 1.0.2.** 1.0.2's JEI integration fails with the JEI version in current
  packs, so Thaumaturge recipes would be missing from JEI. 1.1.0 works with it.
- **1.1.0 with a crash fix.** Unpatched 1.1.0 crashes on world load with this pack's Cyclops Core recipes
  ([#617](https://github.com/Leclowndu93150/Thaumaturge/issues/617)). The fix only adds two null checks. An official
  release newer than 1.1.0 replaces it.
- **Applied Thaumaturge 1.0.0**, the version 1.0.0 shipped.
- **Thaumaturge items are visible again.** Later versions hide every Thaumaturge item from recipe viewers. That is
  switched off.

### Quests

- **The 1.0.0 Thaumaturge chapter, unchanged**, in its 1.0.0 place before Witchery in the Magic group.
- **The Applied Thaumaturge branch in General Storage**, with its links to the Thaumaturge and AE2 quests. Later
  versions put new quests where the branch was, so it sits slightly lower.
- **Rune Carving asks for Thaumaturge again.** The Pyramid quest gets its 1.0.0 tasks (Arcane Workbench, Thaumium,
  Void Metal), icon and description back. It keeps its newer position, prerequisite and rewards. The 1.0.0 Arcane
  Workbench recipe for Rune Plates comes back next to the newer Witchery recipe, so either route works.

### Materials and recipes

- **Cinnabar stays as redesigned.** Later versions made every machine produce FTB Materials cinnabar gems, with
  quicksilver crafted from the gem when Thaumaturge is installed. That works well with Thaumaturge, so cinnabar
  ore still drops gems rather than quicksilver.
- **Unification is back.** Thaumaturge's brass, cinnabar ore and quartz ore merge into their FTB Materials versions
  again, and the duplicates are hidden, as in 1.0.0.
- **Amber is back in full.** Amber ore processing in every grinder and crusher, the Mystical Agriculture amber
  crop, and amber from Apotheosis boons and the Dimensional Resources crystal ore. Amber ore generation already
  returns with the mod in later versions and is unchanged.
- **Recipes use Thaumaturge ingredients again.** The Blade of Vitality and Vigor Bow take Liquid Death instead of
  EvilCraft blood, and the Causal Anchor takes Thaumium instead of Moonstone. Other changes to those recipes stay.
- **The Blue Heart infusion returns** as a third way to make Blue Hearts.

### Skills, loot and shops

- **The Thaumaturge skill path returns.** Later versions turned it into a "Sage" path with Ars Magica and
  enchanting bonuses. The Thaumaturge capstone and its skills get back their 1.0.0 icons and bonuses: vis
  discount, focus power, warp resistance and infusion stability. Sorcery and Archmagus get their vis discounts
  back.
- **Loot crates contain both.** The arcane crate keeps the newer Ars Magica and Auroral items and gets its 1.0.0
  Thaumaturge items back, so it is now roughly a third Thaumaturge. The sapling crate gets the Greatwood and
  Silverwood saplings back.
- **The Arcanist sells Thaumaturge items again**, including the Creative Node Placer. As in 1.0.0, the Node Placer
  is used up when placed outside creative mode.
- **The Arcanist's hint names every unlock item.** It unlocks with the Arcane Compendium, Vinteum Dust or a
  Thaumometer, and the hint now lists all three.
- **Brazilian Portuguese text.** 1.0.0 had no pt_br text, so the restored skills get new translations.

### Settings

- **Thaumaturge's default keys are moved off conflicts.** Its defaults clash with other bindings, for example Change
  Focus and Swap Offhand both on F. Those keys get 1.0.0's bindings: Change Focus on Ctrl+F, and the Thaumonomicon
  and misc toggle unbound. Keys a player has rebound are left alone.
- **Carry On can't pick up Thaumaturge multiblock parts.** 1.0.0 meant to block them, but its entries used the wrong
  mod name and never matched. They now work.
- **Thaumaturge settings return** for ServerCore (golems, turrets and bosses), Dank Storage (thaumium), the Discord
  status (Outer Lands), and the default JEI and Jade layout for new instances.

### Not restored

- **Thaumium tool parts.** Later versions removed the Hephaestus material system for every metal, not only
  Thaumium.
- **1.0.0's cinnabar drops and outputs**, replaced by the redesign above.

</details>

## Credits

Thaumaturge is by Leclowndu93150 and contributors. FTB Evolution 2 is by the Feed The Beast team. This project is not
affiliated with either.

The jar bundles [ASM](https://asm.ow2.io/) 9.9.1, distributed under the BSD license in `LICENSE-ASM.txt`.
