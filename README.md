<p align="center">
  <a href="https://www.youtube.com/watch?v=qdb2hMzFCCA"><img src="assets/wizard_dance_fframes.gif" width="576" alt="Three pixel-art wizards in purple, green and red robes dancing in a moonlit forest, each holding a wand"></a>
</p>

# FTB Evolution 2 - Thaumaturge Restore

An installer that brings Thaumaturge back to FTB Evolution 2. The nostalgia must not stop.

FTB Evolution 2 shipped [Thaumaturge](https://github.com/Leclowndu93150/Thaumaturge) and Applied Thaumaturge in
1.0.0, and removed them in 1.0.1 over [copyright issues with Team CoFH](https://x.com/FTB_Team/status/2108144492410757300).

The installer adds both mods back. It merges the Thaumaturge quest chapter and the rest of the removed content
into later pack versions, without losing the later improvements and fixes.

This is a hack that took several manual patches. I hope FTB manages to reintegrate Thaumaturge into the pack.

## Requirements

- FTB Evolution 2 **1.0.1 or later** on Minecraft 26.1.2.
- Java 17 or newer. Minecraft launchers ship a Java runtime, and the install scripts locate it.
- An internet connection on first run. Mods and pack files are downloaded from the FTB and CurseForge CDNs.

## Quick start

Close the game, then run the script for your system from the folder that contains the jar:

```bash
./install.sh            # Linux and macOS
```

```bat
install.cmd             :: Windows. Double-clicking it also works.
```

The installer finds your FTB Evolution 2 instances, shows every change it will make, and asks before writing
anything. Start the game once after installing. On a multiplayer server, install on the server and on every client.

To see what would change without writing anything:

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
the installer looks in the default instance folders of Prism Launcher (including Flatpak), MultiMC, PolyMC, the
FTB App, CurseForge, the Modrinth App, ATLauncher and GDLauncher.

| Option | Effect |
|---|---|
| `--dry-run` | Show the planned changes without writing anything |
| `--uninstall` | Revert every change the installer made |
| `--yes`, `-y` | Do not ask for confirmation |
| `--quests-only` | Restore only the mods and quests, not the recipe, loot, skill and config content |
| `--no-pyramid` | Leave the Pyramid's Rune Carving quest as your pack version has it |
| `--force` | Continue on an unrecognized Minecraft version |
| `--offline` | Use only files already in the download cache |
| `--cache DIR` | Use a different download cache folder |
| `--list` | List the FTB Evolution 2 instances it finds |

## What it changes

**Mods**
- Thaumaturge 1.1.0, with a fix for a world-load crash
  ([#617](https://github.com/Leclowndu93150/Thaumaturge/issues/617)) applied during install. If a newer version is
  already installed, it is kept.
- Applied Thaumaturge 1.0.0.

**Quests**
- The Thaumaturge chapter, placed before Witchery in the Magic group.
- The 13 Applied Thaumaturge quests in General Storage, linked to the Thaumaturge and AE2 quests they depend on.
- The Pyramid's Rune Carving quest asks for the Thaumaturge items again. The Witchery Rune Plate recipe still
  works too.

**Pack content**
- FTB Materials unification for Thaumaturge's brass, cinnabar and quartz.
- Amber ore processing in every grinder and crusher, the Mystical Agriculture amber crop, and amber drops from
  Apotheosis boons and Dimensional Resources.
- Thaumaturge ingredients in the Blade of Vitality, Vigor Bow and Causal Anchor recipes, and the Blue Heart
  infusion recipe.
- The Thaumaturge path in the Magic skill tree, in both English and Brazilian Portuguese.
- Thaumaturge items in the arcane and sapling loot crates and the Arcanist's shop.
- Thaumaturge settings for Carry On, ServerCore, Dank Storage, Discord status, JEI and Jade.
- Key bindings: where a Thaumaturge default collides with another binding, the key gets the pack's 1.0.0 binding.
  Change Focus moves from F to Ctrl+F, for example. Bindings you changed yourself are left alone.

The installer adds only what is missing, so after a pack update it re-applies only what the update removed.
Before writing, it checks the resulting quest book for duplicate IDs, missing quests and item IDs that the
installed mods do not provide. If a check fails, it writes nothing.

## Safety and uninstalling

Every file the installer replaces or disables is backed up to `thaumaturge-restore/` in the game folder, with a
journal of the changes. `--uninstall` uses the journal to restore the original files. Any file that changed after
the install, such as one updated by the pack, is left untouched and reported. A failed install rolls back
automatically.

All downloads are checked against pinned SHA-1 hashes. No mod or pack files are included in this repository or in
the built jar.

## Compatibility

Tested on every FTB Evolution 2 release from 1.0.1-rc.1 to 1.8.1, including a dedicated server boot on 1.8.1.
Windows and macOS support is implemented but has not been tested yet. Reports are welcome.

## Building

Requires JDK 17+ and `curl`.

```bash
./build.sh                                              # writes dist/evo2-thaumaturge-restore.jar
java -cp dist/evo2-thaumaturge-restore.jar test/Json5Test.java
```

To package a release, put `dist/evo2-thaumaturge-restore.jar`, `install.sh`, `install.cmd` and `install.ps1` in a
single folder.

## Pack decisions

<details>
<summary>What was restored, what was kept from newer versions, and why</summary>

The goal is the pack as if Thaumaturge had never been removed: all of Thaumaturge's content from pack 1.0.0, plus
every later improvement. A Thaumaturge item that a later version swapped for a stand-in returns. A system that a
later version redesigned keeps that redesign.

### Mods

- **Thaumaturge 1.1.0 instead of the 1.0.2 that pack 1.0.0 shipped.** 1.0.2's JEI integration fails with the JEI
  version in current packs, so its recipes would be missing from JEI. 1.1.0 works with it.
- **1.1.0 with a crash fix.** Unpatched 1.1.0 crashes on world load with this pack's Cyclops Core recipes
  ([#617](https://github.com/Leclowndu93150/Thaumaturge/issues/617)). The fix adds only two null checks. A newer
  official release replaces it.
- **Applied Thaumaturge 1.0.0**, the version that pack 1.0.0 shipped.
- **Thaumaturge items are visible again.** Later versions hide every Thaumaturge item from recipe viewers. The
  installer switches that off.

### Quests

- **The 1.0.0 Thaumaturge chapter, unchanged**, before Witchery in the Magic group, where it sat in 1.0.0.
- **The Applied Thaumaturge branch in General Storage**, with its links to the Thaumaturge and AE2 quests. Later
  versions added quests where the branch used to be, so the installer moves it down just enough to avoid overlap.
- **Rune Carving asks for Thaumaturge again.** The Pyramid quest regains its 1.0.0 tasks (Arcane Workbench,
  Thaumium, Void Metal), icon and description. It keeps its newer position, prerequisite and rewards. The 1.0.0
  Arcane Workbench recipe for Rune Plates returns next to the newer Witchery recipe, so either route works.

### Materials and recipes

- **Cinnabar keeps the redesign.** Later versions made every machine produce FTB Materials cinnabar gems, and
  quicksilver is crafted from the gem when Thaumaturge is installed. That works well with the mod, so cinnabar
  ore still drops gems rather than quicksilver.
- **Unification is back.** Thaumaturge's brass, cinnabar ore and quartz ore merge into their FTB Materials versions
  again, and the duplicates are hidden, as in 1.0.0.
- **Amber is back in full.** Amber ore processing returns to every grinder and crusher, along with the Mystical
  Agriculture amber crop and amber drops from Apotheosis boons and the Dimensional Resources crystal ore. Amber
  ore generation already returns with the mod in later versions, so it is unchanged.
- **Recipes use Thaumaturge ingredients again.** The Blade of Vitality and Vigor Bow take Liquid Death instead of
  EvilCraft blood, and the Causal Anchor takes Thaumium instead of Moonstone. Other changes to those recipes stay.
- **The Blue Heart infusion returns** as a third way to make Blue Hearts.

### Skills, loot and shops

- **The Thaumaturge skill path returns.** Later versions turned it into a "Sage" path with Ars Magica and
  enchanting bonuses. The Thaumaturge capstone and its skills regain their 1.0.0 icons and bonuses: vis discount,
  focus power, warp resistance and infusion stability. Sorcery and Archmagus regain their vis discounts.
- **Loot crates contain both.** The arcane crate keeps the newer Ars Magica and Auroral items and regains its
  1.0.0 Thaumaturge items, so it is now roughly a third Thaumaturge. The sapling crate regains the Greatwood and
  Silverwood saplings.
- **The Arcanist sells Thaumaturge items again**, including the Creative Node Placer. As in 1.0.0, the Node Placer
  is used up when placed outside creative mode.
- **The Arcanist's hint names every unlock item.** It unlocks with the Arcane Compendium, Vinteum Dust or a
  Thaumometer, and the hint now lists all three.
- **Brazilian Portuguese text.** 1.0.0 shipped no pt_br text, so the restored skills get new translations.

### Settings

- **Colliding Thaumaturge keys get 1.0.0's bindings.** Its defaults clash with other bindings, for example Change
  Focus and Swap Offhand both on F. Change Focus moves to Ctrl+F, while the Thaumonomicon and misc toggle become
  unbound. Keys a player has rebound are left alone.
- **Carry On no longer picks up Thaumaturge multiblock parts.** 1.0.0 tried to block them, but its entries used
  the wrong mod name and never matched. The entries now use the right name.
- **Thaumaturge settings return** for ServerCore (golems, turrets and bosses), Dank Storage (thaumium) and the
  Discord status (Outer Lands), along with the default JEI and Jade layouts for new instances.

### Not restored

- **Thaumium tool parts.** Later versions removed the Hephaestus material system for every metal, not only
  Thaumium.
- **1.0.0's cinnabar drops and outputs**, replaced by the redesign above.

</details>

## Credits

Thaumaturge is by Leclowndu93150 and contributors. FTB Evolution 2 is by the Feed The Beast team. This project is not
affiliated with either.

The jar bundles [ASM](https://asm.ow2.io/) 9.9.1, distributed under the BSD license in `LICENSE-ASM.txt`.
