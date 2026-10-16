# Development and validation

This guide covers how to build, change and verify the whole mod. Start with the [mod guide](MOD_GUIDE.md) for the system map and [contribution guide](../CONTRIBUTING.md) for submitting work. Substance names and quantities describe fictional gameplay; source code and runtime resources are the authority for current behavior.

## Set up and build

Install a Java 21 JDK. Set `JAVA_HOME` to that installation or put its `bin` directory on `PATH`; check `java -version` before building. Use the checked-in Gradle wrapper. The Minecraft, NeoForge, mappings and mod versions are declared in `gradle.properties`, with the Java toolchain and run configurations in `build.gradle`.

From the repository root on Windows:

```powershell
.\scripts\build.ps1 build
```

On other platforms, use the Gradle wrapper directly:

```sh
./gradlew build
```

The release JAR is written to `build/libs/`. Its current name is `ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar`. The display name is **Vice and Villagers** and the author is **Afterdark Industries**. The `trippycraft` namespace and Java package are retained compatibility identifiers; renaming them can break resources, worlds and network integration.

The wrapper pins Gradle and its distribution checksum. Strict dependency verification checks resolved artifacts against `gradle/verification-metadata.xml`. Investigate a verification failure before changing that file; do not disable verification to make a download succeed. Dependency changes need a deliberate version, provenance and compatibility review.

No bundled JDK, local game installation or checked-in dependency cache is required. The first build downloads the declared dependencies. The launcher handles Windows checkout paths containing spaces and ampersands.

## Run a development instance

```powershell
.\scripts\build.ps1 runClient
.\scripts\build.ps1 runServer
```

These tasks use the ignored `run/` directory by default. To reuse an external development instance, supply `-PdevRunDir=<directory>` to either task. Use a private copy of a world for testing changes to saves or structures. Keep exactly one mod JAR in an installed test instance and restart Minecraft after replacing it.

Common controls are defined in `config/TrippyConfig.java`; client presentation controls are in `config/TrippyClientConfig.java`. Their generated config filenames retain the `trippycraft` prefix. Defaults are part of the current behavior, so config changes belong in the review just like code changes.

Client controls include visual/audio enablement and intensity, motion intensity, motion reduction, photosensitivity reduction and debug channels. Rendering disabled does not turn off server-side substance gameplay, and audio is controlled independently. `motionReduction` disables the dedicated cannabis, cocaine, ketamine, heroin and alcohol client movement hooks; setting only `motionIntensity` to zero does not disable those velocity adjustments.

## Plan a change across its boundaries

Follow an existing item's path from acquisition to consumption, persistence, networking and presentation before implementing a change. The [complete substance map](MOD_GUIDE.md#all-substance-systems) covers older substances as well as newer additions. A family-wide effect or shared-channel change can affect more than the substance being edited.

| Change area | Follow through | Verify |
|---|---|---|
| Substance behavior or dose | Item/content format, profile, dedicated session, engine, effect resolver, vanilla bridge, snapshot and client consumers | Direct use, redose, mixtures, phase timing, dose display, old saves and accessibility controls |
| Recipes and processing | Recipe JSON or serializer, machine/menu state, output item and reusable containers | Exact counts, fuel/progress, output blocking, shift-click, selected branch/dose and save/reload |
| Plants and exploration | Registries, blocks, biome placement/modifiers, growth/harvest and loot resources | Suitable biomes/support, growth stages, Survival/Creative drops, fresh and older unopened chests |
| Villages and taverns | Placement/saved data, regional structures, role assignment, bartender stock, ambience packets | Terrain and entrance alignment, intact/broken stations, player edits, revisits and server restart |
| Economy and multiplayer | Catalogs, broker/customer services, packets, proximity and inventory validation, saved state | Two clients, stale screens, reconnect, payouts, inventory consumption and permissions |
| Menus and book | Screen/store, server menu or discovery state, fitted coordinates, content entries and translations | Complete panels, scroll clipping, tooltips, keyboard/pointer alignment, inventory dragging and final book entries |
| Visual effects | Shared graph, dedicated renderer/pass, clocks, depth/mesh paths, shader resources and VR adapter | Sober identity, phase/mixtures, foreground/sky/hand protection, accessibility, reload and both eye views |
| Audio and music response | Sound event routing, stream processing, playlist clock, live analysis and tavern visual consumers | Categories, attenuation, toggles, all tracks, phase/mixtures, reconnect and music-reactive presentation |
| Persistence or packets | All schemas, clone/respawn/dimension handling, legacy aliases and client cleanup | Existing-world copy, load/save/reconnect, old identifiers, mixed state and dedicated server |
| Documentation only | Existing source/resource behavior and links between the general guides | Accurate terminology, complete scope, working links and unchanged non-documentation files |

Keep changes focused, but review every affected boundary. Client visuals must not become authoritative for consumption, inventory transactions or saved progression. Preserve compatibility IDs, hidden legacy items, dealer aliases and migration fallbacks unless a reviewed migration replaces them.

For Cursed Findings contributions, distinguish prose from executable recipe/discovery information. Human-written replacement text can improve the book without silently changing an ingredient, output, machine, dose or unlock condition. Read the contribution guide before replacing artwork, music or book content; record its authorship and license in [asset provenance](ASSET_PROVENANCE.md).

## Choose the checks that match the change

A successful build proves compilation and packaging, not a running world or complete gameplay compatibility. Use the relevant automated checks and the manual matrix below. Keep unavailable checks explicit in the contribution description.

### Build and server contracts

```powershell
.\scripts\build.ps1 build
.\scripts\build.ps1 runHoMetGameTestServer
```

Despite its historical name, `runHoMetGameTestServer` runs the retained whole-mod GameTest suite, including substance/session, crafting/content, plants, exploration loot, commands and village/bartender contracts. Its test world and logs are under ignored `build/` directories. Check the actual failed-test summary and process exit; assembly alone does not execute GameTests.

Coverage is uneven. There are dedicated LSD and psilocybin contracts, cannabis-bong and bartender tests, and extensive tests for several later substance systems. Cannabis, cocaine, heroin, alcohol, MDMA and ketamine lack dedicated full-session GameTest classes, and DMT's complete session/stage behavior also needs direct manual checks. Focused item/crafting tests do not replace that coverage. Passing the suite does not prove every substance, saved world or modpack.

New tests should exercise meaningful behavior or compatibility, rather than mirror a helper's implementation. Use the existing GameTests near the affected system as examples. A documentation-only change needs link and scope review, rather than a new runtime test.

### Shader and audio checks

Shader generation requires Python 3. GPU scripts require Windows OpenGL and the project's resolved LWJGL/JOML dependencies. The PowerShell launchers resolve Java and dependencies through Gradle instead of relying on machine-specific cache paths.

```powershell
python scripts/generate-vr-shaders.py --check
.\scripts\validate-vr-shaders.ps1
.\scripts\validate-dmt-fractal-shaders.ps1
.\scripts\validate-dmt-fractal-controls.ps1
.\scripts\validate-psilocybin-shaders.ps1
.\scripts\validate-lsd-shaders.ps1
.\scripts\validate-substance-audio.ps1
```

The generator's `--check` mode verifies checked-in desktop/stereo parity and reproducibility without rewriting resources. When deliberately editing a desktop shader or the shared VR helper, regenerate without `--check`, inspect the runtime resource diff, then repeat the GPU and relevant native/manual checks.

The DMT fractal GPU harness checks all quality levels, exact disabled output, full-replacement independence from the Minecraft image, intermediate blends, movement/rotation, seeded geometry, animation, accessibility and shared coordinates between asymmetric stereo eyes. It writes a rendered contact sheet and isolated GPU timings under `build/`. Retest the player toggle, breakthrough/return transitions, resource reload and performance in Minecraft; offscreen checks do not establish live headset behavior.

The DMT fractal controls harness compiles and executes the production pass and blend resolver against isolated time, session and render-target substitutes. It checks player intensity presets, mode/renderer gates, dose and phase transitions, mixed effects, animation controls, session/world resets and VR head anchoring. These tests exercise Java control flow without launching Minecraft or proving config-file persistence.

The VR validator compiles program shaders and checks the fifteen world-warp effects, asymmetric eye optics, depth/sky behavior, composition and finite mathematical output. Cannabis and heroin have additional shared/overlay paths; this does not provide complete scene coverage for them. These isolated GPU checks do not launch a headset or establish comfort, live compatibility or whole-game performance.

The audio validator exercises all sixteen dedicated substance states across world, ambient, music, record and tavern routing. It checks toggles, sober reset, sound identity and synthetic PCM processing. It does not play the real tracks or establish subjective audio quality.

Two focused visual regression harnesses retain fixed source fixtures:

```powershell
.\scripts\allad-double-strength-review.ps1 -Width 1280
.\scripts\twoce-expanded-visual-review.ps1 -Width 1280
```

Their references in `tests/fixtures/` are required maintenance inputs, not disposable build output. The harnesses cover production presentation math and selected GPU layers; they complement broader checks rather than define the mod's whole visual test plan. Generated classes, logs and images belong under `build/`.

### Native client smoke checks

Use a fresh smoke directory for each run. These harnesses protect existing worlds rather than overwrite them. A task must exit successfully and produce its expected `*-smoke-result.txt`; screenshots alone are not proof of success.

| Task | Coverage |
|---|---|
| `runHoMetSmoke` | 4-HO-MET renderer scenes; optional AL-LAD/2C-B/2C-E crafting flags consume formats and resolve models |
| `runSalviaSmoke` | Salvia plant-stage and item-model assets; does not consume a dose or test active effects |
| `runMescalineSmoke` | Cactus/item-model and Findings UI assets; does not consume a dose or test active effects |
| `runPsilocybinSmoke` | Psilocybin surface-flow, phase and camera/FOV regression scenes |
| `runLsdSmoke` | LSD presentation, palette and camera/FOV regression scenes |
| `runTwoCeSmoke` | 2C-E presentation and HUD regression scenes |
| `runAlLadSmoke` | AL-LAD presentation regression scenes |
| `runGuiSmoke` | All mod screens plus bartender vanilla trading, fitting/scissors/tooltips, displayed controls and inventory dragging |

```powershell
.\scripts\build.ps1 runHoMetSmoke -PhometSmokeDir=build/qa-homet-fresh -PalLadCraftingSmoke=true -PtwoCbCraftingSmoke=true -PtwoCeCraftingSmoke=true
.\scripts\build.ps1 runSalviaSmoke -PsalviaSmokeDir=build/qa-salvia-fresh
.\scripts\build.ps1 runMescalineSmoke -PmescalineSmokeDir=build/qa-mescaline-fresh
.\scripts\build.ps1 runPsilocybinSmoke -PpsilocybinSmokeDir=build/qa-psilocybin-fresh
.\scripts\build.ps1 runLsdSmoke -PlsdSmokeDir=build/qa-lsd-fresh
.\scripts\build.ps1 runTwoCeSmoke -PtwoCeSmokeDir=build/qa-twoce-fresh
.\scripts\build.ps1 runAlLadSmoke -PalLadSmokeDir=build/qa-allad-fresh
.\scripts\build.ps1 runGuiSmoke -PguiSmokeDir=build/qa-gui-fresh
```

`-PlsdSmokePaletteOnly=true` selects the focused LSD palette run. Combined HoMet crafting flags do not turn its renderer scenes into complete visual tests for AL-LAD, 2C-B or 2C-E. GUI smoke tests drawing/input correspondence on a native client; physical Vivecraft controller aiming and binocular comfort remain manual.

## Manual acceptance for the whole mod

Use a private test-world copy and record the mod/loader versions, changed configs, relevant mods, scene, phase, expected behavior, observation and untested cases. Keep personal paths, account names and player data out of public attachments. Test sober first, then the relevant substance alone and representative mixtures.

### Every substance and shared state

Work through [all substance systems](MOD_GUIDE.md#all-substance-systems): psilocybin, LSD, DMT, cannabis, MDMA, 2C-B, mescaline, 4-HO-MET, AL-LAD, 2C-E, nitrous oxide, ketamine, cocaine, heroin, salvia and alcohol. The checklist applies to older systems as well as recent ones.

- Consume the supported forms and quantities; confirm displayed content, container returns, cooldowns and discovery.
- Observe onset, peak, descent/afterglow and expiry; redose during different phases and check existing stacking rules.
- Exercise mixtures and Blend Capsules where supported, including retained exclusions and separate physical-content/effect-calibration behavior.
- Save/reload, reconnect, die/respawn, change dimensions and sleep/wake; preserve durations, content, recovery and existing rules.
- Check tolerance, depletion, dependency, withdrawal, risk and recovery where implemented; reset between unrelated scenarios.
- Use daylight/night, caves/interiors, weather, villages, passive/hostile mobs, combat, nearby players and different dimensions to exercise set/setting.
- Inspect hotbar, chat, inventory, crosshair, hand, menus and recovery screens for readability and stale effects.
- Repeat renderer/audio toggles, reduced/zero intensities, motion reduction and photosensitivity controls; server behavior should remain as designed.

Use `/viceandvillagers status` and `sync` for inspection, and the permission-level-2 `reset` in a test world when needed. The legacy `/trippycraft` alias remains supported. The default `J` timeline key applies to psilocybin, 4-HO-MET, AL-LAD, 2C-E and nitrous; it is not a universal timeline for every substance.

### Acquisition, production, books and economy

- Visit each plant's supported habitat, check support/growth stages and compare Survival versus Creative harvests.
- Check exploration pools in fresh structures and older unopened chests; already opened chests should not refill.
- Exercise Mortar and Pestle, Cannabis Press, Chemistry Table, Advanced Chemistry Table and Brew Cauldron: input/output counts, fuel, progress, blocking, shift-click and saved branch/dose choices.
- Test supported partial-content processing, legacy formats and reusable vessels in both hands/modes; reject unsupported ingredients and loaded/mixed vessel combinations.
- Open Dealer, Market Broker and Customer menus; check categories/search/scroll, stock, offers, transaction counts and saved market state with two clients.
- Visit all regional village bars on flat, steep and shoreline terrain; check entrance/foundation/path, schedules, bartender anchoring, revisits/restarts and player-edited/broken stations.
- Check bartender stock and vanilla trading UI. Role assignment must keep bartenders distinct from dealer, broker and customer systems.
- Open Cursed Findings, verify discovery gates, tabs, recipe references and text, then scroll through the final entry. Hidden/clipped rows must not react to clicks.
- Repeat relevant recipes, transactions and reconnects on a dedicated server; client appearance is not proof of a valid server transaction.

Existing economy behavior includes known gameplay exploits. Consult [security guidance](../SECURITY.md) and review any proposed gameplay fix separately; documentation or asset maintenance must not silently rebalance it.

### Visuals, audio and VR

Check still and moving viewpoints, near objects, silhouettes, sky, water, foliage and oblique surfaces. Trails should retain intended source appearance and reset cleanly. Surface effects, camera effects and world-object hallucinations have separate paths; test each affected path rather than only a flat wall screenshot.

At modest volume, check footsteps, placement, damage, weather/ambient, music, records and every tavern track. Preserve category volumes, spatial fade and attenuation. Compare music-reactive visuals and stream processing before/after audio work; a track replacement can affect live analysis even when playlist timing is unchanged. Menus, recovery and world changes must not leave stale sound shaping.

**VR visual effects work through optional Vivecraft integration, but movement, camera-motion and other input effects are currently buggy.** Treat those paths as ongoing contributor work. The Vivecraft 1.21.1-1.3.5 GUI checks provide a useful target, not a guarantee for every headset or version.

- Test Vivecraft absent/inactive, then active with both eyes, mirror, camera and telescope passes.
- Turn/roll/translate the head: world patterns and geometry should remain coherently anchored. Check controller picking separately from HMD-gaze visual targeting.
- Open every menu and utility at several GUI scales/resolutions, including the smaller logical VR canvas. Check full panels, list-edge tooltips, pointer highlight/action correspondence, inventory drag/release and the timeline slider.
- Repeat menus sober and with substance effects; those effects remain enabled on menus.
- Exercise movement/input effects explicitly and record incorrect behavior instead of treating a shader pass as validation of them.
- Check ketamine's high-dose pull with its reconstruction limit in mind: post-processing cannot reveal occluded faces or create a complete third-person avatar/collision scene.

Physical controller aiming, headset appearance/comfort, shader packs and modpack combinations require live QA. For performance work, compare matched scenes/phases after warm-up and record application CPU/GPU frame time, eye resolution, render scale, refresh rate and relevant settings. Separate application frames from compositor/reprojected frames; isolated shader timing is not Minecraft FPS. Check memory stability across longer sessions and resource/resolution changes.

## Asset and structure authoring tools

The retained Python generators are deliberate source-editing tools:

| Tool | Writes |
|---|---|
| `scripts/build_salvia_plant_assets.py` | Salvia block models/textures; preview under `build/asset-previews/` |
| `scripts/build_mescaline_cactus_assets.py` | Mescaline cactus model/textures; preview under `build/asset-previews/` |
| `scripts/generate_bar_structures.py` | Regional village-bar NBT structures from `scripts/custom_tavern_blueprint.json` |
| `scripts/generate-vr-shaders.py` | Generated VR program resources unless `--check` is supplied |
| Gradle `runData` | Datagen output in `src/generated/resources/` |

Image generators require Pillow; the cactus preview also uses NumPy and Windows Arial fonts. Inspect every generated runtime diff. `runData` and asset generators are not read-only validation commands. Preserve model/MTL/texture dependencies and separate resource identifiers even when files look alike.

## Keep contributions and release sources clean

Keep generated output under ignored `build/`, `.gradle/` and development game directories. Do not commit worlds, player data, caches, logs, screenshots, recovery archives, bundled toolchains or machine-specific path lists. Public attachments should contain only relevant sanitized evidence. Use [asset provenance](ASSET_PROVENANCE.md) for media attribution and [security guidance](../SECURITY.md) for private reporting.

Before a release, build from a fresh maintained-source copy without local caches/game directories and run the checks appropriate to the change. Test an existing-world copy as well as a fresh world. Review the source and JAR for private identifiers, credentials, paths and embedded metadata; keep denylist/recovery evidence outside the public tree. Record checks actually performed and remaining limits, without presenting a historical result as a new run.

For a documentation-only pass, compare the non-documentation file inventory and hashes before/after, verify local links and command names, and leave the existing JAR untouched. For code or asset changes, give reviewers a concise problem/behavior description, affected systems, reproducible validation and relevant compatibility limits. Return to the [README](../README.md) for the contributor entry points.
