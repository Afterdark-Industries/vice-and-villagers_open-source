# DMT fractal branch build

The ready-to-install [mod JAR](ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar) is in this repository's `artifacts/` folder. This is the build from the `codex/v2.2.0-dmt-rendering-review` branch for **Minecraft 1.21.1**, **NeoForge 21.1.249** and **Java 21**. The JAR is intentionally tracked here so it is available alongside this branch's source; other generated build output remains ignored.

Place the JAR in your instance's `mods` folder, replacing the previous Vice and Villagers JAR, then restart Minecraft. Keep one version of the mod installed.

**DMT Powder and Ancient Pipe:** DMT crystals now require grinding in the Mortar and Pestle: one 1x crystal produces one powder, and one 5x crystal produces five. Each powder provides one gameplay unit, directly or through a loaded Bong/Ancient Pipe. Crystals cannot be consumed directly or loaded into new capsules. Previously saved DMT capsules remain usable.

Craft the Ancient Pipe at a crafting table with this layout (`I` = iron ingot, `W` = any wooden planks, `A` = Acacia Bark):

```text
· · ·
I · ·
W A W
```

Craft one empty Bong or Ancient Pipe with one **DMT Powder**, **Dried Cannabis Bud**, or **salvia extract** (1x/10x/25x/50x) in either crafting grid. Right-click the loaded device to consume one serving and retain that device empty. Loaded bowls cannot be combined or topped up. DMT retains its existing dose response and visuals. Cursed Findings has a new Smoking Devices chapter and updated substance pages.

The smoking update passed nine focused server GameTests and a fresh native Minecraft check: all seventeen related item models/names, all six paired device appearances, the updated journal pages and real inventory-based recipe discovery synchronized to the client. The first two supplied icons are included; the accidental third image is excluded.

**Market Broker coverage:** products from all sixteen substance families can be sold, including DMT Powder, MDMA capsules, bulk crystals/powders, AL-LAD concentrate, ethanol and all twelve loaded smoking devices. Sales and contracts require full, unused stock; partly used containers remain yours, and equivalent older item IDs count toward the current product. Selling a loaded Bong or Ancient Pipe transfers the whole vessel. Empty tools and custom blend capsules are excluded. Existing full-stock prices, demand/reputation rules and deferred payouts on market close are preserved.

The four broker contracts belong to each player and **never expire** with time or a new Minecraft day. They survive market close/reopen, logout and save/load. Completing one replaces it in place; the other three keep their IDs, requirements and rewards. Demand still changes daily, without repricing posted orders.

The broker update passed ten focused server GameTests, covering all 111 product rows, all Dealer products, fourteen legacy item variants, full/partial inventory handling, emerald payouts and saved market data. Contract checks include long elapsed time, save/load, older partial boards, replacement in each of the four slots, fixed posted rewards, replay protection and separate player boards.

Open **Mods → Vice and Villagers → Config** from Minecraft's title screen or in-game Mods menu. The settings screen offers the DMT realm toggle, intensity **0/25/50/75/100%**, and **Low/Balanced/High** quality. Changes apply immediately and save for future launches; existing custom intensity values are preserved when the menu opens. **Key Binds...** opens shortcut assignment, and **Open Trip Scrubber...** is available during supported active trips. The settings screen is organized into sections for future additions.

The config-menu build passed native Minecraft tests for Mods entry navigation, saved values, custom/zero intensity, a 320×240 GUI, parent-screen return and synchronization with the original key bindings. The JAR and checksum below include this menu.

The original DMT effects remain the default. **DMT Fractal World is experimental and off by default**; enabling it may affect performance. A fresh instance's `config/trippycraft-client.toml` uses these defaults:

```toml
dmtFractalWorld = false
dmtFractalIntensity = 0.75
dmtFractalQuality = 1
```

To try the realm, enable it in the Config menu or set `dmtFractalWorld = true`. **75% intensity** is the default and recommended for the best experience; try **50%** on mid-range PCs or **25%** on low-end PCs. Existing saved mode, intensity and quality settings are preserved.

Quality `0` is low, `1` is balanced, and `2` is high. Assign **Toggle DMT Fractal World** under **Options → Controls → Key Binds → Vice and Villagers** to switch modes in game; the binding starts unassigned and saves your preference. The generated realm is visual only, with Minecraft's existing blocks and collisions.

Assign **Cycle DMT Fractal Quality** under **Options → Controls → Key Binds → Vice and Villagers** to cycle **Low → Balanced → High → Low**. It starts unassigned; Balanced is the default, so the first press selects High. Each press applies the quality live, shows its name above the hotbar, and saves `dmtFractalQuality` for future launches.

For the largest detail budget, select 100% intensity and set `dmtFractalQuality = 2`. In this polished revision's isolated 1080p RX 6800 XT tests, High measured 15–18 ms in the standard scene and about 40–41 ms in a sampled demanding scene, excluding Minecraft's other rendering and another VR eye. The accepted distance-only optimization saved 2.3–3.1% in matched demanding scenes of the earlier generator; the art revision's timing changes are not optimization ratios. Complexity changes throughout a trip, and lowering quality does not guarantee a particular frame rate. See the [polish scorecard](../docs/DMT_POLISH_RESULTS.md) for the full timing matrix and limits.

Assign **Cycle DMT Fractal Intensity** in the same Key Binds category to cycle **25% → 50% → 75% → 100% → 25%**. The selected percentage appears above the hotbar and is saved for future launches. It multiplies the dose response while preserving the trip's seed, position and clocks. At 100%, dose-driven intricacy continues beyond the old visual ceiling. Opaque coverage remains at most 100%; nested detail and transformations continue scaling. Quality controls rendering cost, and the realm's on/off toggle stays separate.

This build independently mixes geometry/layout, materials, motion, pattern overlays and lighting/atmosphere, with world interpretation shaping the combinations. Higher doses engage more layers, recursion and interacting patterns, denser detail, stronger scene response and faster transformations. Nearby terrain direction, blocks, lighting, weather, living entity positions and the hidden world scene influence the fully immersive realm. It evolves even while standing still. Set `motionIntensity` above zero for autonomous animation; motion reduction slows it, and pausing a singleplayer game pauses its clock.

The breakthrough transition unfolds over roughly 26–32 seconds after crossing starts, with additional easing. It bends the visible Minecraft scene using depth, grows ornament across its surfaces and gradually morphs toward the realm; the final quarter remains a visible transition. The return settles motion, distortion, ornament and lighting as it fades. Slower architecture and faster surface activity share evolving motifs, with clearer material depth and filtered fine patterns. World-color transitions stay continuous through opposite hues and neutral stone/snow; nearby walls, canopy/ceilings and stable entity tracking strengthen hidden-world interpretation.

Java 21 build, isolated GPU shader checks and controller/sampler tests pass. A fresh Minecraft development-world test also passed a complete six-unit trip through expiry, movement across realm cells, a moving entity and live setting changes. High remains demanding and some broad membrane states remain; steady 60 FPS is not guaranteed. Physical-headset and broader modpack/environment testing remain outstanding. See the [polish results](../docs/DMT_POLISH_RESULTS.md), [mod guide](../docs/MOD_GUIDE.md#dmt) and [development guide](../docs/DEVELOPMENT.md#shader-and-audio-checks) for evidence and commands.

The adjacent [SHA256SUMS](SHA256SUMS) records the JAR's checksum. To rebuild and refresh this branch artifact from the repository root in PowerShell with Java 21 selected:

```powershell
.\scripts\build.ps1 build
Copy-Item -LiteralPath 'build/libs/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar' -Destination 'artifacts/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar' -Force
$artifactHash = (Get-FileHash -LiteralPath 'artifacts/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar' -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath 'artifacts/SHA256SUMS' -Value "$artifactHash  ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar" -Encoding ascii
```
