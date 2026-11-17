# FTB Evolution 2 1.9.0 and 1.10.0: CPU and allocation findings

These findings come from profiling a player's game on FTB Evolution 2 1.9.0 (Minecraft 26.1.2, NeoForge
26.1.2.126). Each section lists the problem, the evidence, the fix that this installer applies, and the fix we
suggest for the pack or upstream. 1.10.0 ships the same versions of every mod below, so each fix applies unchanged.

The profile numbers come from two 15–20 s samples on the player's machine. The code findings come from decompiling
the mod jars that 1.9.0 ships. The FancyMenu findings come from its source on GitHub. Upstream status was checked on
2026-11-15. 1.9.0 ships the newest release of every mod below, and 1.10.0 ships the same jars, so no released fix
exists for any of them.

The first profile used about 320–370% of one core. After some fixes by hand, including GC flag changes, the second
used about 226%: render thread 74%, server thread 35%, concurrent GC 41%, chunk render workers 32% and entity culling
21%. GC CPU tracks allocation volume, so the allocation sites below matter more than their direct CPU time suggests.

What was tested:

- `./test.sh` covers the command rewrite, the `web_server.json` merge, re-runs and the standalone helper class.
- A 1.9.0 dedicated server booted cleanly with all fixes, with no new ERROR or WARN lines. The datapack sorted above
  `mod_data`, `in.prism` existed, and `incendium:clocks/main` ran. No player was online, so `player/main` did not run.
- A 1.10.0 dedicated server also booted cleanly with all fixes, with no new ERROR or WARN lines. `file/evo2-performance`
  loaded first among the file packs, and `in.prism` already existed when the server finished loading.
- The patched Pipez and Iron Jetpacks classes, and the helper class, pass `-Xverify:all` against the 26.1.2 client and
  NeoForge libraries. Neither patched method has run in a game yet.
- A second install plans nothing, and `--uninstall` restores the original jars byte for byte.

| # | Mod | Problem | Installer fix | Pack-side fix |
|---|---|---|---|---|
| 1 | KubeJS 8.0.8 | Local web server spins a core | `web_server.json`: `"enabled": false` | Ship that setting |
| 2 | Incendium 5.4.12 | ~220 command exceptions/s, missing objective | Datapack override | Report upstream, or ship the datapack |
| 3 | Iron Jetpacks 9.0.3 | HUD re-runs bidi reordering every frame | Bytecode patch | Report upstream |
| 4 | Pipez 1.2.31 | Fluid pipes build filter lists on every insert | Bytecode patch | Report upstream |
| 5 | FancyMenu 3.10.0 | Allocations on every `PoseStack.pushPose` | None | Report upstream |

## 1. KubeJS: the local web server spins a core

**Seen:** turning the web server off was a large CPU gain on the player's client.

**Cause:** KubeJS starts its local web server (bundled `tiny-java-server` 1.0.0 build 45) on every client while
`kubejs/config/web_server.json` has `"enabled": true`, which is the shipped value. A dedicated server starts it only
when `public_address` is set. `HTTPServer.start()` puts the `ServerSocketChannel` in non-blocking mode, and
`HTTPServer.run()` then polls it with no `Selector`, sleep or park:

```java
serverSocketChannel.configureBlocking(false);
// ...
while (isRunning()) {
    now = System.currentTimeMillis();
    acceptClient(ssc);                       // ssc.accept() returns null at once when nobody connects
    connections.keySet().removeIf(HTTPConnection::handleClosure);
}
```

That thread keeps one core at 100% for the whole session.

**Installer fix:** sets `"enabled": false` in `kubejs/config/web_server.json` and keeps the other keys.

**Pack fix:** ship `"enabled": false`. Upstream, `tiny-java-server` should block in `accept()` or wait on a
`Selector`.

**Also:** 1.8.1, 1.9.0 and 1.10.0 ship `web_server.json` with a fixed `auth` value, so every install has the same token.
The server binds to `127.0.0.1` when `public_address` is empty, which limits the exposure. Leaving `auth` out of the
shipped file makes KubeJS generate a random token per install (`WebServerProperties.load()`).

## 2. Incendium: per-tick command exceptions and a missing objective

**Seen:** 80% of all exceptions thrown, about 220/s. In 20 s the profile had 4,178 "No entity was found", 471
"Target does not have this tag" and 303 "Unknown scoreboard objective 'in.prism'".

**Cause:** `incendium:clocks/main` reschedules itself every tick and runs `incendium:player/main` as every player.
Many of those commands target a selector that usually matches nothing, and a command whose target matches nothing
throws a `CommandSyntaxException`:

```mcfunction
tag @s[gamemode=adventure] add in.survival_player                 # throws for survival players
scoreboard players remove @s[scores={in.cd_shield=1..}] in.cd_shield 1   # throws while the cooldown is 0
tag @s[tag=!in.offhand_scroll,tag=!in.mainhand_scroll] remove in.scroll  # throws when the tag is absent
scoreboard players set @s in.prism 0                              # in.prism is never created
```

`player/main` has about ten such lines, and `clocks/main` has one more, the `in.frozen` decrement over
`@e[type=#incendium:mobs]`. `in.prism` appears only in `player/main`. Incendium's load function never creates it.

Minecraft creates these exceptions without stack traces, because `SharedConstants` sets Brigadier's
`ENABLE_COMMAND_STACK_TRACES` from the `COMMAND_STACK_TRACES` debug flag, which is off. Each one still allocates
the exception and its translated message. The cost is allocation and GC work, not a large direct CPU cost. It is
the smallest fix in this list, but it is also the cheapest to apply.

**Installer fix:** a global datapack, `datapacks/evo2-performance/`:

- `#minecraft:load` runs `scoreboard objectives add in.prism dummy`, so every world gets the objective.
- `incendium:clocks/main` and `incendium:player/main` are regenerated from the installed Incendium jar, with each
  command that can fail on an empty selector wrapped so that it does nothing instead:

  ```mcfunction
  execute if entity @s[gamemode=adventure,tag=!in.survival_player] run tag @s[gamemode=adventure,tag=!in.survival_player] add in.survival_player
  execute if entity @s[scores={in.cd_shield=1..}] run scoreboard players remove @s[scores={in.cd_shield=1..}] in.cd_shield 1
  ```

  The wrapped command runs on the same entities as before. The override is regenerated on each install, so
  re-running the installer after an Incendium update picks up the new function.

**Upstream:** the missing objective is reported as
[Incendium#158](https://github.com/Stardust-Labs-MC/Incendium/issues/158), which is open with no reply. The
selector exceptions are not reported. The public repository was last pushed in 2024 and has no 26.1 source. The
maintainers say a performance rewrite is in progress
([#144](https://github.com/Stardust-Labs-MC/Incendium/issues/144),
[#146](https://github.com/Stardust-Labs-MC/Incendium/issues/146)). A related open report,
[#159](https://github.com/Stardust-Labs-MC/Incendium/issues/159), measures about 8% of a server tick in
`entity/sentry/life`, whose `sort=nearest` selectors have no `distance` limit. This installer does not fix that one.

**Pack fix:** until the rewrite ships, the pack can ship the same datapack in `datapacks/`.

## 3. Iron Jetpacks: the HUD re-runs bidi reordering every frame

**Seen:** in the first profile, at 120 FPS, the HUD was the largest allocation site, at about 43% of sampled
allocation bytes. The samples showed bidi reordering and `ArrayList` growth under the HUD's text rendering.

**Cause:** `HudHandler.onRegisterGuiOverlays` builds four new components every frame (`getFuelComponent`,
`getThrottleComponent`, `getEngineComponent`, `getHoverComponent`) and draws each with `GuiGraphicsExtractor.text`.
That method calls `Component.getVisualOrderText()`. `MutableComponent` caches the reordered text on the instance
(`visualOrderText`, keyed by `decomposedWith`), but a new instance per frame never hits that cache.

**Installer fix:** a bytecode patch to `HudHandler`. Each of the four component results goes through a small LRU
cache of 32 entries (`evo2restore.patch.HudTextCache`, added to the jar). The cache returns the earlier, equal
instance, so Minecraft's own per-instance cache hits. `MutableComponent.equals` compares contents, style and
siblings, so a changed value still renders. The patch applies only if `HudHandler.class` has the exact SHA-1 of
9.0.3.

The patched class passes the JVM verifier, but it has not run on a client yet. If it misbehaves, `config/ironjetpacks-client.toml` → `[HUD] enable = false`,
or the in-game HUD toggle key, removes the cost entirely.

**Upstream:** not reported. The `26.1` branch still builds the components per frame. Its last `HudHandler` change
(2026-05-14) only fixed the HUD not rendering. [IronJetpacks#65](https://github.com/BlakeBr0/IronJetpacks/issues/65)
was a different 1.19 bug, where the HUD rendered several times per frame.

**Pack fix:** report to Iron Jetpacks (BlakeBr0). Building the components only when energy, throttle, engine or
hover state changes fixes it upstream.

## 4. Pipez: fluid pipes build filter lists on every insert

**Seen:** 29% of server-thread allocation bytes in the second profile.

**Cause:** `FluidPipeType.canInsert` runs for every fluid resource and connection on every transfer. Even when the
pipe has no filters, it builds two streams and two lists:

```java
for (Filter<?, Fluid> filter : filters.stream().map(...).filter(Filter::isInvert)
        .filter(f -> matchesConnection(connection, f)).collect(Collectors.toList())) { ... }
List<Filter<?, Fluid>> collect = filters.stream().map(...).filter(f -> !f.isInvert())
        .filter(f -> matchesConnection(connection, f)).collect(Collectors.toList());
if (collect.isEmpty()) return true;
```

The pack's `pipezlagfix` 1.1.0 patches only `ItemPipeType` and `PipeLogicTileEntity`.

**Installer fix:** a bytecode patch that adds `if (filters.isEmpty()) return true;` at the start of `canInsert`.
With an empty list, both loops do nothing and the method already returns `true`, so behavior does not change. The
patch applies only if `FluidPipeType.class` has the exact SHA-1 of 1.2.31.

**Upstream:** open pull request [pipez#311](https://github.com/henkelmax/pipez/pull/311) (2026-11-13, against
`26.1`) adds the same `filters.isEmpty()` check. It also tracks the count of non-full destinations in
`insertEqually`, instead of rescanning the array on every round-robin step. It is not merged, and the `26.1`, `26.2`
and `master` branches still have the old code.

**Pack fix:** ask for pipez#311 to be merged and released, or ask the `pipezlagfix` author to cover fluid pipes.

## 5. FancyMenu: allocations on every `PoseStack.pushPose`

**Seen:** in the second profile, FancyMenu was the largest allocation source, at 56% of sampled allocation bytes.

**Cause:** `MixinPoseStack` injects at the tail of `pushPose` and, on every call, allocates a new
`RenderTranslationUtil.TranslationState` and `RenderRotationUtil.RotationState`, then boxes a `Float` for the scale
stack. Every `PoseStack` gets this, including those for GeckoLib entity models and GUI item rendering, and no
config option controls it:

```java
@Inject(method = "pushPose", at = @At("TAIL"))
private void after_pushPose_FancyMenu(CallbackInfo info) {
    this.renderScaleStack_FancyMenu.addLast(this.renderScaleStack_FancyMenu.getLast());
    RenderTranslationUtil.TranslationState nextTranslation = new RenderTranslationUtil.TranslationState();
    // ...
    this.renderRotationStack_FancyMenu.addLast(new RenderRotationUtil.RotationState(lastRotation));
```

**Upstream:** not reported. The `26.1.2` and `26.2.0` branches have this code unchanged since the 26.1 port
(2026-04-27). `26.3.0` changed only the rotation hooks.

**Installer fix:** none. Removing the mixin could break FancyMenu's menu layouts, and we have not tested that.

**Pack fix:** report to FancyMenu (Keksuccino). Pooling the state objects, or recording them only while FancyMenu
is rendering its own elements, would fix it.

## Not pack bugs

These came up in the same profiles and are player settings:

- **Uncapped FPS:** 120 FPS with VSync off was the largest direct CPU cost. A 60 FPS cap roughly halves render-thread
  time and every per-frame allocation above.
- **Open AE2 terminals:** an open terminal runs `MEStorageMenu.broadcastChanges` every tick. Close terminals when AFK.
- **Mob farms:** loot and equipment contexts allocate for every kill.
- **GC "pauses":** the ~2 s G1 events in the log are whole concurrent mark cycles. The stop-the-world parts were
  7–14 ms for young collections and ~36 ms for remark and cleanup.
