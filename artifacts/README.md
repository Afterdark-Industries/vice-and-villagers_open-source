# DMT fractal branch build

The ready-to-install [mod JAR](ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar) is in this repository's `artifacts/` folder. This is the build from the `codex/v2.2.0-dmt-rendering-review` branch for **Minecraft 1.21.1**, **NeoForge 21.1.249** and **Java 21**. The JAR is intentionally tracked here so it is available alongside this branch's source; other generated build output remains ignored.

Place the JAR in your instance's `mods` folder, replacing the previous Vice and Villagers JAR, then restart Minecraft. Keep one version of the mod installed.

The original DMT effects remain the default. To enable the optional 3D fractal realm, set these values in your instance's `config/trippycraft-client.toml`:

```toml
dmtFractalWorld = true
dmtFractalIntensity = 1.0
dmtFractalQuality = 1
```

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
