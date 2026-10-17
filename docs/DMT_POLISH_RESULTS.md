# DMT realm polish — implementation scorecard

Local branch: `codex/v2.2.0-dmt-rendering-review`. Baseline: `3451bc1`, shader `2A83A4A861C239954386F66C915CA273CC32AA93ECAE01352D8C90DEF442636E`. The [approved plan](DMT_POLISH_PLAN.md) contains the original live desktop observations and constraints. No push is performed.

## Instrumentation and preserved behavior

`-Dtrippycraft.profileDmtGpu=true` enables bounded asynchronous GPU timestamp measurements for the original DMT and generated-realm passes, reporting shader and copy time separately every five seconds. Reports include view, resolution, sample count, mean and percentiles. A four-invocation delay and availability checks precede result reads; a full 16-slot queue drops measurements instead of waiting. It capability-checks OpenGL 3.3 / `ARB_timer_query`, cleans up query objects with the effect targets, and allocates no query objects when disabled. Timestamp queries avoid conflicting with Minecraft's own elapsed-query scopes.

An isolated OpenGL probe executes the production timer, testing disabled/non-DMT paths, supported capability, delayed results, real finite shader/copy samples, separate view labels, periodic reporting, queue saturation, cancellation, cleanup and 32-bit wrap. The unavailable-result branch and unsupported hardware are source-reviewed; the tiny real GPU workload finished too quickly to hold an unavailable result during collection. Log: `build/dmt-polish-validation/production-gpu-timing-probe.log`.

The optional original-effects mode, saved player controls, native-resolution shading, uncapped artistic dose response and hidden-world influence remain. Integrated clocks preserve phase through redose and nonzero setting changes. Off/zero-intensity and world/resource changes intentionally reset the realm.

## Same-scene optimization

Distance-only tracing, normal and occlusion evaluations skip material-orbit tracking. The full material coordinate is evaluated once at the final hit. Candidate `8A59418A` preserves exact tested field/orbit/depth results over 576 stereo-view image/depth pairs and 864 field slices; distance-only/full-distance error is zero. Largest image-component difference is 0.001237, while worst image mean absolute error is 1.21e-7. No changed-depth/new-miss pixels were detected in this comparison.

Paired 1920×1080 GPU timing on an RX 6800 XT, three alternating warmed rounds, same generator/seed/time/dose:

| Fixture | Quality | Baseline | Distance-only candidate | Saving |
|---|---|---:|---:|---:|
| Standard, dose 6 | High | 13.699 ms | 13.607 ms | 0.7% |
| Standard, dose 200 | High | 16.071 ms | 15.932 ms | 0.9% |
| Nested-room, dose 3 | High | 41.483 ms | 40.218 ms | 3.1% |
| Nested-room, dose 48 | High | 42.496 ms | 41.328 ms | 2.7% |

These are medians for one native-resolution pass, excluding Minecraft's other rendering and a second VR eye. Ordinary Low/Balanced cases range from a 0.9% regression to a 0.4% improvement; the important benefit is limited to the heavier fixture. Full-orbit evaluations in that fixture fall from approximately 98 per ray to one. This is a modest optimization, not a claim of a major FPS improvement. Log: `build/dmt-polish-validation/stage1-generator-performance.log`.

## Tracing experiments

An early handoff from slow primary tracing to recovery was measured and discarded. Although it reduced total field calls in one heavy fixture, it increased farther-surface errors and slowed ordinary scenes by 2–4% and most heavy cases by about 1%. The accepted renderer retains the proven primary search and local nearest-surface brackets. Coverage alone was insufficient to accept this shortcut.

No redundant initial color copy was found in the postprocessor. The final scratch-to-main copy remains necessary for the current composition path; the legacy pass continues providing hidden-scene interpretation. Geometry-history acceleration remains deferred: the renderer has no realm depth/normal history, stereo shares a pass object, and validating fresh foreground geometry in a continuously morphing field needs a separate prototype. No blur/upscaling or hidden dose/quality throttle was introduced to meet a performance target.

## Visual polish

Architecture and surface detail use different fixed rates of the same integrated dose-sensitive clock. A slowly evolving seeded motif links geometric aperture, harmonic petals and spatial ornament/material variation. Pattern filtering now also fades unresolved broad tile colors, reducing crawling beneath fine lines. World-sampled color has greater influence than camera-dependent hidden-pixel tint, while hidden color/depth/normal cues remain active.

Glass, membrane and metal share a directional procedural lighting environment, while retaining separate Fresnel, transmission, thin-film and reflection responses. Existing normal/AO samples provide stronger cavity and form shading; distance absorption and restrained atmospheric chroma separate foreground and background. Lit facets and fine luminous accents were rebalanced after preview review found the first revision too dark. No extra field/shadow traces were added. This is an incremental art pass, not a claim that every seed/material mixture is equally readable or that subjective authenticity can be proven by numerical tests.

A per-cell carve-weight variation experiment was removed after review found it could introduce air/solid discontinuities at cell faces. The proven layout mixture remains. Existing seeded geometry and new shared/spatial motifs provide variation without accepting that seam risk.

## World sampling and return

The sampler now includes supporting terrain, eye-height walls and overhead canopy/ceiling evidence, with at most 168 block reads per cached update. It samples once per 20 world ticks unless the player moves eight blocks, and checks loaded chunks before access. An existing living-entity identity is retained unless another is at least 20% closer or the old entity dies/leaves range. Its world-space attractor position and influence continue easing in the render controller.

Return now eases distortion, motion rate, ornament and lighting activity along with opacity. Motion changes affect future integrated increments; they never rescale an elapsed phase. Controller tests cover the new settling state, redose, settings, pause-clock behavior and lifecycle resets. Actual production-sampler fixtures cover cache invalidation, unloaded boundaries, wall/ceiling/canopy evidence and entity hysteresis.

An additional continuity fix keeps the original trip anchor after a fully faded but still active afterglow. Walking during that invisible tail and redosing the same session now preserves the anchor and elapsed clock. The entrance still follows the player before the first crossing, and ordinary lifecycle/off/zero resets remain intentional.

## Final GPU validation and cost

Final desktop and native-VR shader SHA256: `6EF59970DF04E79F9942E26C9C7E10B5C70763226F1813C732FD0FC48044C302`.

The dedicated suite passes 6,561 finite GPU frames, all three qualities, four intensity presets plus zero, seven style preferences, accessibility, dose stress, depth-driven morphing, independent layers, settling and asymmetric stereo. An additional 180 quality/preset/dose cases inspect actual hit depths and images. The general suite passes all 31 production programs, 1,620 general frames and 484 additional 4-HO-MET temporal frames. Generated-resource checking passes all 15 variants.

| Final correctness/continuity measurement | Result in tested fixtures |
|---|---:|
| Exact disabled-source error / opaque alpha error / desktop-native parity | 0 / 0 / 0 |
| Minimum ray hit coverage Low / Balanced / High | 99.8291% / 99.9430% / 99.9512% |
| High recovered rays materially farther than same-revision 2048-step reference | ≤0.033% of whole image |
| Opposite-eye missing/farther correspondence across tested qualities | ≤0.0151% of projected probes |
| Largest seven-style 60 FPS mean image step | 0.000616 |
| Eased world-RGB maximum mean image / brightness step | 0.002646 / 0.000434 |
| Peak-to-quiet settling, 240 steps, maximum mean image step | 0.001036 |
| 40-second × 30 FPS depth-morph maximum mean image / brightness step | 0.007896 / 0.008256 |

Temporal checks exposed two real palette seams. Shortest-hue interpolation flipped when the target crossed the opposite hue; continuous circular tinting fixes this. Almost neutral stone/snow also injected an arbitrary hue: a green perturbation of just 0.00002 caused a 0.02634476 mean image jump. Chroma-aware weighting reduces that to 0.00000041. Hidden-source near-gray response is 0.00000011; the weak-chroma sector-circle test closes exactly with a maximum 0.000014 mean image step. Saturated world colors retain their influence, while neutral samples retain their structural/brightness response.

Final 1920×1080, 100% intensity, RX 6800 XT; each entry is a median after two warm-up draws and five GPU timer samples:

| Final fixture | Dose pair | Low ms | Balanced ms | High ms |
|---|---|---:|---:|---:|
| Standard, seed .317 / clock 12 | 6 / 200 | 9.596 / 10.504 | 13.169 / 13.283 | 15.377 / 17.631 |
| Nested chapter, seed .37 / clock 60 | 3 / 48 | 16.745 / 19.838 | 19.163 / 24.015 | 22.747 / 29.653 |
| Historical heavy input, seed .37 / clock 120 | 3 / 48 | 24.873 / 22.073 | 22.984 / 22.541 | 21.085 / 28.026 |
| Sampled demanding input, seed .37 / clock 20 | 3 / 48 | 29.957 / 26.731 | 33.869 / 31.210 | 40.691 / 40.208 |

These are final-revision scene costs, **not speedup ratios against the earlier art**. The changed clocks/materials change the scenes. The demanding case came from a 640×360 scan over one seed, clocks 0–300 every ten seconds and doses 3/6/48, then fresh native-resolution timing; it is not a global worst case. Only the earlier same-generator stage-1 comparison demonstrates an optimization ratio. High still exceeds a 60 FPS budget in demanding scenes before Minecraft or another VR eye is included. Lower quality does not guarantee a particular frame rate.

Logs and immutable sources: `build/dmt-polish-validation/` contains `final-dmt-gpu.log`, `final-vr-gpu.log`, `final-neutral-world.log`, `final-world-hue-temporal.log`, `final-6ef599.fsh` and the complete `SCORECARD.md`. PNG/GIF previews are regenerated under `build/`.

## Live development-world check and artifact

`runDmtSmoke` passed in a fresh disposable Minecraft 1.21.1 / NeoForge 21.1.249 client at 1920×1080, render/simulation distance 5, VSync off, 120 FPS cap, on the same RX 6800 XT. The user's existing instance/save was not replaced. This automated development-world run observes a natural six-unit session through expiry, slow scripted turns, 51.641 blocks of supported forward movement, a moving villager, nearby wall/canopy evidence, all three quality settings and a 50%→100% intensity adjustment. It completed 9,314 successful visible-fractal draws and nine screenshots; all nine were visually inspected. Entry, opaque realm and return are visible, with the original world restored after expiry. Some close views still contain broad patterned membranes; this art pass does not remove every flat-looking state.

Selected render-stage interval results from that run:

| Phase/settings | Samples | Mean / p50 / p95 / p99 ms |
|---|---:|---|
| Breakthrough transition, High / 100% | 1,344 | 13.757 / 13.805 / 19.611 / 20.338 |
| Post-breakthrough, High / 100% | 1,998 | 16.784 / 15.935 / 26.323 / 36.086 |
| Post-breakthrough, Balanced / 100% | 857 | 17.456 / 17.862 / 21.332 / 22.379 |
| Post-breakthrough, Low / 100% | 1,112 | 13.488 / 13.767 / 19.381 / 21.506 |
| Return, High / 100% | 1,520 | 14.119 / 14.322 / 17.327 / 22.624 |

These are intervals between `AFTER_LEVEL` events, not guaranteed presented frames or isolated GPU durations. Quality changes occur at different trip times/views, so these rows are **not matched quality comparisons**. They also cannot be compared as a speedup against the original village playtest: world, camera, render distance, seed and generator differ. The High post-breakthrough p95/p99 target is still unmet in this run. Opt-in GPU instrumentation separately reports roughly 0.68–0.69 ms for the original DMT pass, with realm cost varying during the session; copy cost remains separately visible in the log. No query-queue drops or DMT renderer-disable errors occurred.

During 175 game seconds, the observed GC delta was 31 collections / 131 ms. Sampled used heap ranged from about 378 to 628 MiB and returned to about 437 MiB near expiry. This short instrumented run is not a long-session allocation/leak study. Physical-headset comfort/per-eye application performance, shader packs/modpacks, manual reload/toggle/pause combinations and extended forest/village/cave/ocean walking sessions remain further live QA; isolated tests cover their relevant mathematical/controller/sampler behavior, not those complete environments.

Evidence: `build/dmt-client-smoke-20261017-polish/dmt-smoke-result.txt`, its `screenshots/` folder, and `build/dmt-polish-live-smoke.log`. The development client exited normally after the test.

The final Java 21 build passes. The tracked `artifacts/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar` is refreshed and byte-identical to `build/libs/`. ZIP CRC and duplicate-entry checks pass; all 565 compiled classes and all four changed shader resources match the build/source. Both packaged fractal shaders match the exact tested hash above, both JSON settling defaults are verified, the legacy shaders are unchanged after newline normalization, and English translations match source.

JAR size: **76,451,648 bytes**. SHA256: `3c9880607232f78077a330f7fe44865ef614ca315fc0d6ceb3cf70c6a93bd638` (also recorded in `artifacts/SHA256SUMS`). Work remains local; no push is performed.
