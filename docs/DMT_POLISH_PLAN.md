# DMT realm polish plan — 17 October 2026

**Status: local polish implementation complete; test JAR refreshed.** The original playtest and proposed acceptance gates below are retained as the baseline. [DMT_POLISH_RESULTS.md](DMT_POLISH_RESULTS.md) records accepted changes, rejected experiments, the passing full-trip integration test, measured performance limits and further live/headset QA. Geometry history remains a deferred conditional prototype. Nothing is pushed.

The aim is a convincing, continuously generated DMT-inspired world: clear spatial depth, distinctive materials, evolving motifs and transformations that feel connected. Preserve the existing dose response, player controls, world influence and optional original-effects mode.

## What I tried and measured

I used the already-running Minecraft 1.21.1 / NeoForge 21.1.249 singleplayer session. Its installed JAR matches the current local branch build: SHA256 `0e6ed6d38c75c15f379100cb47a5b1f5c922db62479b71ef512def7c3719135d`, commit `3451bc1` on `codex/v2.2.0-dmt-rendering-review`.

The session runs at native 1920×1080 on an RX 6800 XT and Ryzen 9 5900X, with Fancy graphics, render distance 10, simulation distance 8, fractal intensity 100% and High quality. I tried the ordinary 1× DMT item and the 5× item, watched onset, breakthrough and return, changed the view, and cycled the quality binding. High quality and the normal HUD were restored; the game was left paused. This was a short, largely stationary desktop test with coarse automated mouse control. It does not establish smooth walking/headset comfort or performance across every environment, dose and seed. Quality was cycled as the trip ended, so that was a control check, not a matched quality-performance comparison.

Two native F3+L captures provide the following measurements:

| Measurement | Sober capture | 5× breakthrough, High / 100% |
|---|---:|---:|
| Capture duration | 10.000 s | 10.028 s |
| Recorded rendering-loop throughput | 351.7/s | 30.12/s |
| Median loop duration | 2.490 ms | 36.196 ms |
| 95th percentile | 5.483 ms | 46.223 ms |
| 99th percentile | 6.636 ms | 50.386 ms |
| Integrated-server throughput | 20.03 TPS | 20.00 TPS |

These are instrumented render-loop measurements, not guaranteed displayed FPS. The captures used different camera directions and different numbers of visible chunks, so they are not a controlled before/after speedup ratio. The world and trip also evolved during capture. The breakthrough sample's successive 2.5-second windows averaged approximately 22, 43, 36 and 40 ms per loop, illustrating why average FPS alone is insufficient.

The trip profile spent about 86% in display update/waiting, with a high Minecraft GPU-duration/CPU-frame-duration ratio and low whole-system CPU usage. This supports a GPU/presentation bottleneck rather than server tick pressure; it does not isolate the fractal shader's GPU milliseconds. The large heap changes warrant checking garbage collection during longer tests, without assuming a leak from this sample.

Evidence archives remain in the instance's `debug/profiling` folder: `2026-10-17_12_26_33-late nights vibe-1_21_1.zip` and `2026-10-17_12_31_41-late nights vibe-1_21_1.zip`. Convenient local copies are saved as the [sober capture](<D:/TrippyCraft versions/V&V-2.2.0-open-source-1.21.1-neoforge/build/dmt-polish-playtest/sober-1080p-village.zip>) and [5× High breakthrough capture](<D:/TrippyCraft versions/V&V-2.2.0-open-source-1.21.1-neoforge/build/dmt-polish-playtest/dmt-5x-high-1080p.zip>).

Visually, the realm clearly evolves and contains convincing recursive depth in some states. Other material/lighting mixtures become broad, flat color washes that obscure fine structure. Entry and return deserve more attention to shape, depth and material continuity. This playtest does not establish that every seed has those problems.

## Constraints that stay in place

- Actual gameplay dose continues driving increasing detail, recursion, concurrent patterns, transformation speed and world distortion beyond the old visual ceiling. Finite rendering budgets remain necessary; optimization must not quietly clamp the artistic dose response.
- Keep the integrated animation clock: redosing and nonzero intensity/quality adjustments must not jump elapsed phase, reset the trip seed or move its anchor. Preserve the existing mode-off, intensity-zero and world/resource lifecycle resets.
- Preserve the mode toggle, 25/50/75/100% intensity binding, Low/Balanced/High quality binding, saved preferences, pause and accessibility controls.
- Keep the hidden overworld influential at full immersion, including terrain, weather, light and entities.
- Retain native-resolution shading and crisp ornament as the reference experience. Any later reconstruction mode must be an explicit, separately evaluated choice.
- Work remains local. Test JARs are refreshed after approved implementation milestones, not during this planning stage.

## 1. Establish repeatable frame-time and image baselines

Add development-only measurement for the legacy DMT pass, fractal pass and relevant compositing/copy work. Record frame-time distributions and the fraction of rays entering recovery, rather than only an FPS counter. GPU query results should be read several frames later when available; capability-check timer-query support and avoid introducing a synchronous wait into gameplay. [Khronos documents both asynchronous query retrieval and the stall caused by requesting an unfinished result.](https://wikis.khronos.org/opengl/Query_Object)

Use repeatable seeds, dose timelines, camera paths and loaded test worlds for comparisons. Keep ordinary player dose handling unchanged. Record the same entry, peak, redose and return timeline before and after each change. Compare three warmed captures per important case and separate shader compilation/chunk loading from steady-state rendering.

**Deliverable:** a baseline scorecard identifying where GPU time and visible instability occur. This is the first implementation milestone, not an assumption that every optimization below is necessary.

## 2. Remove repeated computation while preserving the generated scene

The current full-resolution ray marcher can use 72/112/192 primary samples, then 160/192/224 recovery samples and local refinements. Previous isolated tests measured approximately 14–16 ms for the High pass in one ordinary scene and 41–42 ms in a difficult nested-room scene, excluding the rest of Minecraft. The live capture separately establishes a substantial frame-time limitation in the tested trip; per-pass profiling is needed to attribute that cost within the running game.

Prioritize these experiments:

| Change | Intended preservation | Acceptance gate |
|---|---|---|
| Separate distance-only field evaluation from material-orbit evaluation | Trace the same geometry; calculate orbit/material coordinates at the final hit instead of at every tracing, normal and AO sample | Field, hit-depth, image and stereo comparisons; measured GPU benefit |
| Move genuinely frame-constant field parameters out of repeated evaluation | Same formulas and dose parameters | Verify compiler output through measurements; discard changes with no meaningful benefit |
| Inspect redundant target copies and pass setup | Preserve all source color/depth needed for the hidden-world interpretation and the original-effects path | Per-pass GPU timings and composition parity |

The previous exact-zero layout experiment did not establish a speed benefit. Tiny nonzero blend weights often keep every layout active, and compiler changes can affect a few fractal-edge pixels. Do not assume a source-code cleanup is a rendering optimization.

**Deliverable:** a first test JAR with measured savings and the same scene generation. No numerical speedup is promised before profiling.

## 3. Reduce difficult-ray recovery work

Build on the existing proven carved-empty-space bound. Investigate conservative cell/tile bounds and detecting stalled convergence earlier, so rays do not pay the entire primary budget before using another search method. Keep the current local forward bracket and nearest-surface checks.

Every bound must account for current geometry, relief, morphing, dose and entity structures. A coarse approximation is useful only when it safely certifies empty space. Distance/derivative bounds are central to avoiding surface penetration in sphere tracing. [Hart's original sphere-tracing paper describes this requirement.](https://experts.illinois.edu/en/publications/sphere-tracing-a-geometric-method-for-the-antialiased-ray-tracing/)

Check both the difficult nested-room fixture and ordinary scenes. Faster tracing must retain thin foreground structures, sharp silhouettes and stable depth when looking around. The earlier broad-interval recovery approach found real but sometimes incorrect back walls; it is not an acceptable shortcut.

**Deliverable:** a second test JAR and matched performance/depth comparisons. Keep only improvements that reduce expensive recovery without visible geometry loss.

## 4. Make the evolving world feel connected and materially richer

Treat this as a separate art/presentation milestone after the renderer is measurable.

- **Persistent motifs:** give each trip a small evolving memory shared by geometry, ornament and material. A motif can unfold into architecture, become an eye/glyph lattice, then reorganize into another structure while retaining recognizable relationships. Independent seeded variation remains active; this is not a list of scripted trips.
- **Several scales of motion:** let large architecture develop more slowly than surface activity and fine recursive openings. Give each layer its own continuously integrated, dose-sensitive rate. Higher doses still increase activity and interactions; avoid rescaling an elapsed clock.
- **Spatial variety:** investigate the visibly repeated cell system. Source inspection shows a 13.6-unit repeated cell, repeated nested rooms and gallery columns, and a cleared core in every cell. Vary large-scale layout and transitions while retaining a safe entrance and valid distance bounds. Confirm repetition through walking tests before replacing a structural component.
- **Material identity and readability:** tune membrane, glass, tissue, metal and glyph states so their lighting responses remain distinct. Preserve local contrast and silhouettes when palettes mix; make close surfaces reveal structure instead of becoming a uniform reflective color field. Tune line/pattern filtering against shimmer during slow movement, with fresh full-resolution shading.
- **Continuous entry and return:** maintain world-surface correspondence as structures unfold. On return, settle distortion, ornament activity and lighting alongside visibility, rather than merely exposing the overworld beneath an otherwise intense realm. Preserve the existing cinematic duration and dose response unless a later playtest justifies tuning them.

**Deliverable:** a visual-polish JAR reviewed with slow camera motion and complete trips, not only still contact sheets.

## 5. Strengthen scene interpretation and long-term variety

Expand the existing bounded, cached world sampling to capture some wall, canopy and ceiling structure as well as supporting surfaces. Keep all reads within loaded chunks. Let environmental directions affect emergent branching, density and spatial flow, with palette and material retaining the same world context.

Maintain entity identity with a little hysteresis so similarly distant entities do not repeatedly exchange the attractor. Keep their positions anchored in world space. Prefer persistent world-anchored evidence for opaque-realm materials; inspect current per-pixel hidden-image cues for camera-dependent changes.

Use multiscale seeded variation and motif memory to reduce short-term recurrence. Measure within-trip changes, between-seed diversity and reproducibility under the same world/timeline. The goal is extensive emergent combinations with continuity, not arbitrary noise or a claim that repetitions are mathematically impossible.

**Deliverable:** longer stationary and moving playtests in forest, village, cave and water settings, including nearby moving entities.

## 6. Consider geometry history only if the earlier stages need it

The higher-risk acceleration option is to use previous-frame geometry to propose where the current ray should search. Continue shading materials and ornament freshly at native resolution. Validate the proposal against the current field and certify the foreground interval; reject stale or newly occluded geometry and fall back to the reliable current-frame path.

This requires actual **realm hit depth/normal history**. The existing depth texture is Minecraft world depth, and the scratch target contains color. A history system also needs separate eye buffers: the current stereo pass object is shared between left and right. Reset history on incompatible camera/world/resource/quality changes, while revalidating it during continuous dose, environment and geometry changes.

Temporal reconstruction documentation illustrates why depth, motion and changing/disoccluded content require special treatment. That supports caution, not a claim that installing an upscaler will preserve this animated fractal automatically. [AMD's temporal reconstruction documentation details those inputs and history-reactivity mechanisms.](https://gpuopen.com/manuals/fidelityfx_sdk/techniques/super-resolution-temporal/)

**Decision gate:** implement this as an optional prototype only if earlier measurements justify the added buffers, complexity and ghosting/occlusion risks. Fast high-dose morphing may invalidate much of the history and reduce its benefit.

## Validation and completion criteria

1. Test 3/6/12/48/200-unit timelines, with 10000 separately as numerical/complexity stress; include complete entry, redose and return. Confirm that dose above former peaks still increases measurable complexity and transformation response.
2. Compare all three qualities and four intensity presets across ordinary and recovery-heavy layouts, with matched seeds/cameras/times. Large-scale identity should remain coherent as quality changes.
3. Preserve existing nearest-hit, coverage, temporal and asymmetric-eye tests. Use the high-budget reference for the same generator revision, so intentional art changes are distinguished from tracing errors.
4. Run slow turns, walking across cells, entity movement, pause, toggles, reload and world changes. Require stable depth, crisp ornament and no new ghosting or missing foreground geometry.
5. Repeat live frame-time captures. A working desktop target is steady 60 FPS in representative 1080p cases, p95 around 16.7 ms and p99 below 25 ms where achievable without altering the selected experience. This is a target, not a guarantee for arbitrary high doses or every generated layout. Report demanding cases openly rather than silently lowering dose complexity.
6. Check longer-session allocations and GC, then actual VR per-eye/application timings and comfort separately. The desktop playtest and offscreen stereo tests do not establish headset behavior.

The approved order is **measurement → same-scene computation savings → reliable tracing acceleration → visual/motif/world polish**. Geometry-history experiments remain conditional. The [implementation scorecard](DMT_POLISH_RESULTS.md) distinguishes accepted work, discarded experiments and remaining live/headset coverage.
