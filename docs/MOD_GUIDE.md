# Mod guide

This is the contributor map for the whole of **Vice and Villagers**: acquisition and production, every substance system, village life, progression, presentation and saved state. Read [Development and validation](DEVELOPMENT.md) for setup, commands and acceptance checks, and [Contributing](../CONTRIBUTING.md) for submissions. Source and runtime resources are the authority when behavior and documentation differ.

All substance names, quantities and production chains describe fictional game mechanics. They are not real-world preparation, dosing or medical instructions.

## Find the right system

Java paths below are relative to [`src/main/java/com/trippycraft/`](../src/main/java/com/trippycraft/); resource paths are relative to [`src/main/resources/`](../src/main/resources/).

| Area | Entry points | Related boundaries |
|---|---|---|
| Startup and registration | `TrippyCraft.java`, `registry/` | Items, blocks, block entities, menus, features, recipe/ingredient serializers, sounds, villagers and loot modifiers |
| Consumption and simulation | `item/`, `substance/` | Inventory mutation, content, sessions, mixtures, vanilla effects, recovery and discovery |
| Cultivation and exploration | `block/`, `world/`, `loot/` | Biome placement, growth/harvest, structure chests and saved world state |
| Production | `block/entity/`, `menu/`, `crafting/` | Recipes, machine state, containers, dose selection and server/client synchronization |
| Villagers and economy | `economy/`, `world/VillageBarEvents.java` | Dealer, broker, customer, bartender, contracts, village placement and payouts |
| Book and progression | `guide/`, `item/CursedRecipeCodexItem.java` | Cursed Findings text, recipes, discovery, saved unlocks and client display |
| Networking and persistence | `network/`, `compat/NbtCompat.java` | Packet codecs, sidecar NBT, lifecycle hooks and legacy identifiers |
| Visuals, audio, menus and VR | `client/` | Shared effect graph, dedicated presentation, shaders, input and resource cleanup |
| Configuration and diagnostics | `config/`, `substance/ModCommands.java` | Common gameplay rules, client accessibility and development commands |

Runtime resources live under `assets/trippycraft/` and `data/trippycraft/`, with intentional Minecraft/NeoForge integration under their namespaces. `src/main/templates/` supplies expanded mod metadata. `scripts/` contains validators and source-writing authoring tools; `tests/fixtures/` contains required regression inputs. The display name and project attribution are Vice and Villagers / Afterdark Industries. The `trippycraft` namespace, Java package and saved identifiers remain compatibility contracts.

## From an item to an experience

The server owns consumption, inventory, discovery and persistent experience state. A typical path is:

1. An item or loaded vessel validates use and sends its supported content/route to `SubstanceEngine`.
2. `PsychoactiveExperience` updates the dedicated session and common exposure, tolerance, mixture and recovery state.
3. Server ticks advance phases, sample set/setting and blend channels/recovery. `CompositeEffectResolver` supplies the shared six-system psychedelic blend; `VanillaEffectBridge` applies vanilla status effects.
4. `ExperienceSnapshotPacket` synchronizes presentation state through `ModNetwork`.
5. `ClientExperienceStore` and `ClientEffectState` interpolate it; the effect graph feeds rendering, sound and client movement/interaction hooks.

Follow this complete path when changing a substance. A texture, channel or generic profile alone does not define its behavior. A client renderer must not introduce a second authoritative inventory or dose simulation.

### Shared state and boundaries

`SubstanceProfile` and `SubstanceProfiles` describe the catalog, family, dose response and channel weighting. The complete catalog is the ordered profiles plus dedicated session packages, rather than the smaller legacy `SubstanceType` enum. Cocaine powder and crack have separate profiles/routes within one cocaine session system.

`ExperienceChannel` describes drift, geometry, patterning, colour, time, ego dissolution, stimulation, sedation, motor noise, anxiety and risk. Family weights guide style; channels guide strength. Mixtures also use substance-specific signatures and interactions, rather than simply adopting the last item consumed.

Set/setting includes health, hunger, fatigue, light, biome, weather, time, dimension, movement, shelter, recent combat, nearby threats and social/ambient cues. Common state includes tolerance and cross-tolerance, strain, afterglow, comedown, craving and withdrawal-style pressure. Dedicated alcohol, heroin and MDMA recovery trackers can outlive the main active effects. Ketamine masking and cocaine/alcohol interactions have explicit paths that need their own review.

Keep four quantities distinct:

- **Physical content:** what remains in an item or what the HUD records as consumed game content.
- **Effect calibration:** the input used by the substance's native dose model.
- **Absorbed state:** the session's phase-, route- and tolerance-adjusted result.
- **Presentation gain:** the client artistic response to synchronized state and accessibility controls.

Printed milligram labels do not imply equal calibration across substances. Content-aware formats and capsules store content and effect amount separately. Preserve both through partial use, processing, stacking, packaging, snapshots and saves; do not rebuild either from a convenient display label. Redose age, signature reuse, absorption and expiry rules are specific to each session. Read its implementation before applying another substance's clock rules.

## All substance systems

The mod has sixteen dedicated substance systems. The following map gives older and newer systems the same contributor entry points. Acquisition summaries identify existing branches, rather than promising that every dose format is craftable or available in every chest. Check item registration, machine logic, recipes, economy catalogs and loot together for a particular format.

### Alcohol

Beer, wine and spirits connect the bartender, economy and exploration systems to `AlcoholItem`; ethanol also has a consumption path through `EthanolItem`. [`substance/alcohol/`](../src/main/java/com/trippycraft/substance/alcohol/) owns standard-drink calibration, phases, sedation, dependence, recovery and alcohol-specific damage behavior.

Presentation uses `AlcoholVisualRenderer`, `AlcoholWorldWarpPass`, `ClientAlcoholMovementEvents`, `AlcoholDependencyScreenOverlay` and `ClientAlcoholDeathPresentation`. Check blur/double vision, sway, movement, recovery and death presentation alongside vanilla status effects. Changes must consider drink/container behavior, mixtures and recovery after the active session ends.

### AL-LAD

The lysergic/aminoethyl production branch leads to concentrate and dose tabs through brewing, chemistry and advanced dose selection. `AlLadConcentrateItem` and `AlLadDoseItem` connect content reservoirs to [`substance/allad/`](../src/main/java/com/trippycraft/substance/allad/).

The session's calibrated effect amount and physical content remain separate. `AlLadVisualDrive` and `AlLadWorldWarpPass` produce material transport, split images, echoes and pigment/interference flow. Follow content, absorption, phase, shared visual controls and depth protection together; the retained GPU fixture is a focused regression input, not a complete gameplay specification.

### Cannabis

Wild plants and seeds lead through buds, drying and press processing to joints, dab pens, gummies, chocolate and loaded bongs. The registered dose formats use `CannabisDoseItem`; it and `BongItem` feed [`substance/cannabis/`](../src/main/java/com/trippycraft/substance/cannabis/). `CannabisPressBlockEntity` owns processing.

Warmth, green tint, breathing, temporal drag, focus and audio response use the shared graph and rendering paths. `ClientCannabisMovementEvents` supplies a separate movement hook; there is no dedicated cannabis world-warp class. Current formats feed the same session dose path. Verify per-use content, partial-use handling, mixtures, edible packaging, reusable vessels and movement/accessibility behavior.

### Cocaine, koka and crack

Koka cultivation supports leaf use and drying/grinding/paste processing into powder and crack formats. `KokaDoseItem` and `CocaineItem` enter [`substance/cocaine/`](../src/main/java/com/trippycraft/substance/cocaine/), where `CocaineRoute` distinguishes absorption, strength and clock behavior.

`CocaineWorldWarpPass` and `ClientCocaineMovementEvents` cover sharpness, jitter and momentum, while the session handles crash, risk and interactions such as alcohol masking. Preserve powder/crack profile distinctions, route-specific behavior and legacy koka item aliases. A shared session does not make the two product routes interchangeable.

### DMT

Acacia bark/leaves and brewing support extracts and crystal variants. `AcaciaHarvestEvents`, `BrewCauldronBlockEntity` and `DmtItem` connect acquisition to [`substance/dmt/`](../src/main/java/com/trippycraft/substance/dmt/).

The package includes stages, transitions, encounters, themes, signatures and a fractal scaffold, beyond a single generic effect curve. `DmtVisualRenderer`, `DmtWorldGeometryRenderer` and `DmtWorldWarpPass` compose symbols, encounters, architecture and breakthrough/world-replacement presentation. Review stage continuity, geometry lifecycle, ordinary-world visibility and stereo anchoring together.

The optional **DMT Fractal World** renders a 3D fractal realm during an active DMT experience. It starts disabled, preserving the existing DMT visuals by default. Enable `dmtFractalWorld` in your instance's `config/trippycraft-client.toml`, or assign **Toggle DMT Fractal World** under **Options → Controls → Key Binds → Vice and Villagers**. The binding starts unassigned; pressing your assigned key in game toggles the realm, displays its on/off state above the hotbar, and saves the setting for future launches. These settings affect only your client.

During the breakthrough crossing, visible Minecraft surfaces bend and acquire patterns as they morph into the realm. A dose-dependent cinematic ingress unfolds over approximately 26–32 seconds after crossing starts, with additional easing rather than an instant jump when the synchronized stage changes. Source geometry and materials continue transitioning through the final quarter. The return settles distortion, motion, ornament and lighting activity along with visibility. Looking and moving change the actual perspective; desktop and VR eyes share the same anchored geometry. Motion/photosensitivity reduction still apply, and turning the realm off immediately restores the existing presentation.

The realm is a continuous effect mixer, not a catalogue of scripted trips. Geometry/layout, materials, motion, pattern overlays and lighting/atmosphere evolve on independent seeded timelines, with real-world interpretation feeding the mixture. Recursive crystal folds, architectural structures and organic lace can become dominant rather than holding one hybrid style throughout a trip. Architecture evolves more slowly than surface activity, with a shared evolving motif linking openings, petals and ornament. Reflective materials use directional light, cavity shading and distance absorption to retain spatial depth. Fine lines and unresolved tile colors use footprint filtering to reduce shimmer while retaining native-resolution shading. Layouts, surface responses, motion behaviors, motifs and atmospheric treatment combine and change within a single trip as well as between seeds.

Visual complexity uses the actual active gameplay dose through stable logarithmic scaling, independently of the legacy `dmtMaxVisualIntensity` ceiling. Higher doses engage additional layers, recursion and interacting patterns, increase detail density and world-reactive distortion, and accelerate transformations. Speed is integrated continuously, so redosing never jumps an elapsed animation phase. Spacing, folding and nested detail continue changing beyond the former peak. Opaque coverage stays within 0–100%, while dose-driven intricacy continues increasing. Quality bounds ray-march/fold work for finite rendering cost; it is not a maximum gameplay dose or a dose-intensity plateau. This optional renderer does not change server-side substance balance, damage or dose handling.

The real world continues influencing the fully opaque realm. Organic/constructed surroundings, caves, water, light, weather/night, entity activity and movement shape the mixer. A cached sampler reads colors, relief and structural direction from supporting terrain, nearby walls and overhead canopy/ceilings once per second without requesting new chunks. Nearby living entities provide a spatial attractor anchored to their world position, shared between VR eyes; target identity remains stable through small distance changes and its influence fades when entities leave. These signals ease into persistent world influence; the hidden scene's color, depth and normals also leave material/relief cues at full immersion, with the persistent sampled world palette carrying more weight than view-dependent pixel tint. The generated realm remains a visual effect, with Minecraft's existing world and collision rules.

Assign **Cycle DMT Fractal Intensity** in the same Key Binds category to select **25%, 50%, 75% or 100%**, then wrap back to 25%. The binding starts unassigned. Each press shows the selected percentage above the hotbar and saves it as `dmtFractalIntensity` in `config/trippycraft-client.toml` (default `1.0`, range `0.0` to `1.0`). This is the player's multiplier: 100% exposes the full dose response, rather than capping dose complexity at a fixed peak. Lower values retain more of the existing view. Quality controls the rendering budget; intensity is not a proportional frame-rate control. Adjustments preserve the trip's seed, position and clocks, and do not switch the realm on or alter other substances' intensity. A value of `0.0` in the config disables the fractal pass.

The realm evolves while the player stands still. Its animation clock advances independently of synchronized gameplay snapshots; changing snapshot depth/flow values ease into the field. Setting `motionIntensity` to zero freezes autonomous animation, and motion reduction slows it. Dose changes, world influence and the cinematic crossing can still progress. Pausing a singleplayer game pauses its visual clock as well.

Set `dmtFractalQuality` in the same file to choose the rendering budget:

| Value | Quality | Primary fractal ray-march budget |
|---|---|---|
| `0` | Low: lowest GPU cost | 72 |
| `1` | Balanced: default | 112 |
| `2` | High: more detail, greater GPU cost | 192 |

Assign **Cycle DMT Fractal Quality** under **Options → Controls → Key Binds → Vice and Villagers** to cycle **Low → Balanced → High → Low**. The binding starts unassigned; the default is Balanced, so the first press selects High. Each press applies the quality immediately, shows its name above the hotbar, and saves `dmtFractalQuality` in the client config.

Unresolved rays can use up to 160/192/224 forward recovery samples followed by 8/10/12 local exact-field refinements, respectively. Proven empty regions permit cheaper advances. This recovers solid surfaces at lower quality without lowering image resolution; higher quality resolves more of the recursive geometry directly.

The fractal realm is a visual effect only. It does not generate blocks, change world generation, or add collision; movement and interaction still use the Minecraft world.

### Heroin and opioid intermediates

Poppy tapping and processing connect latex, opium, morphine, chemistry base and advanced doses. Consumable intermediates also route into the heroin engine; inspect their item registrations instead of treating them as inert materials. [`substance/heroin/`](../src/main/java/com/trippycraft/substance/heroin/) owns sedation, pain/hunger effects, motor/mining behavior, tolerance, dependence, recovery and overdose state.

`HeroinVisualRenderer`, `ClientHeroinMovementEvents`, `ClientHeroinInteractionEvents` and `OpioidDependencyScreenOverlay` handle warmth, lids, nodding/microsleep and interaction effects. There is no dedicated heroin world-warp class. Changes need review across active effects, recovery, block interaction and death/respawn; desktop interaction targeting and VR controller picking have distinct constraints.

### 4-HO-MET

The aminoethyl/MET branch leads to freebase, crystals, powder and bottled doses. `HoMetCrystalItem`, `HoMetPowderItem` and `HoMetDoseItem` connect brewing, chemistry, mortar processing and advanced packaging to [`substance/homet/`](../src/main/java/com/trippycraft/substance/homet/).

Physical content, calibrated effect units, duration and risk are separate contracts. `HoMetWorldWarpPass` and `HoMetTrailRenderer` provide spatial deformation, recursion, crystal colour and trails. Check absorption/redosing, content-aware reservoirs, signatures, dose response, risk thresholds and history cleanup rather than deriving gameplay from artistic intensity.

### Ketamine

Medicinal/precursor processing leads through vials and advanced doses. `KetamineBottleItem` and `KetamineDoseItem` route into [`substance/ketamine/`](../src/main/java/com/trippycraft/substance/ketamine/); partially used bottles also interact with packaging and exploration loot.

The system covers dissociation, time/depth changes, camera lag, input latency, body gravity, collapse and masking of other effects through `KetamineMasking`. `KetamineWorldWarpPass` and `ClientKetamineMovementEvents` must be considered separately. Its post-process visual pull cannot reconstruct hidden geometry or provide a complete collision-aware third-person avatar scene.

### LSD

Infected wheat supports the lysergic extract/solution/blotter branch. `LsdSolutionItem`, `LsdBlotterItem` and machine logic feed [`substance/lsd/`](../src/main/java/com/trippycraft/substance/lsd/).

`LsdWorldWarpPass`, `LsdVisualRenderer` and `LsdPresentationClock` control autonomous material/colour flow, geometry, sky and entity presentation. Its surface imagery retains a steady viewpoint; `LsdMovementFov` and the attributed vanilla nausea exclusions are part of that contract. Preserve independent nausea from other substances or external debuffs while checking absorption, signatures, mixtures and expiry.

### MDMA

The aromatic/empathogen production branch leads to crystals, tablets and capsules through the press/advanced processing paths. `MdmaCrystalItem` and `MdmaDoseItem` connect those formats to [`substance/mdma/`](../src/main/java/com/trippycraft/substance/mdma/).

`MdmaVisualRenderer` and `MdmaWorldWarpPass` supply warm bloom and music/bass response. `MdmaSocialEffects` influences social/economy behavior; `MdmaRecoveryTracker` and `MdmaDepletionScreenOverlay` preserve depletion/recovery beyond the active experience. Follow tavern choreography, social discounts, content handling and longer-term state when changing any part of this system.

### Mescaline

Dryland cactus growth/harvest leads to the registered dose recipes and `MescalineDoseItem`. `MescalineCactusBlock` and `MescalineCactusPatchFeature` connect cultivation to [`substance/mescaline/`](../src/main/java/com/trippycraft/substance/mescaline/).

`MescalineWorldWarpPass` provides breathing, rich colour, tessellation, symmetry and trails, with body warmth, nausea, environment/signature and music response supplied through the session/shared model. Check habitat/support rules, harvest and every registered dose recipe as well as phases, mixture response and presentation controls.

### Nitrous oxide

The registered canister, trade catalogs and exploration loot provide acquisition; `NitrousDoseItem` feeds [`substance/nitrous/`](../src/main/java/com/trippycraft/substance/nitrous/). Do not infer an unimplemented manufacturing chain from a finished dose item.

The short experience supports stacked hits, dissociation and motor effects. `NitrousWorldWarpPass`, audio shaping and tavern hooks provide tunnel/fisheye, haze, chroma, echo/stretch and music response. Check repeated hits, the short phase arc, saved/synchronized state and clean sober reset across visual and audio paths.

### Psilocybin

Strange mushrooms connect drying, mortar powder and capsule production to `PsilocybinCapsuleItem`. [`substance/psilocybin/`](../src/main/java/com/trippycraft/substance/psilocybin/) owns dose response, phases, signature and environmental behavior.

`PsilocybinWorldWarpPass`, `PsilocybinVisualRenderer` and `PsilocybinPresentationClock` provide organic, earthy, nature-reactive surfaces, spores and trails. Surface imagery retains a steady viewpoint, independently of camera-motion settings. Preserve its attributed vanilla nausea exclusions while retaining external/other-substance nausea; test all supported amounts, stacking, environment, mixtures and history reset.

### Salvia

Jungle cultivation and seeds lead to leaves, drying and extract strengths. `SalviaDivinorumPlantBlock`, `SalviaPatchFeature`, `SalviaLeafItem` and `BongItem` connect those formats to [`substance/salvia/`](../src/main/java/com/trippycraft/substance/salvia/).

The active route is the one-hit loaded bong. Loose leaves/extracts and legacy capsule entries do not provide a working direct salvia effect route. `SalviaVisualRenderer` and `SalviaWorldWarpPass` handle scene replacement, loop seams, observer cutouts, flattening/objecthood and re-entry. Preserve vessel returns, strength selection, route exclusions and phase transitions together.

### 2C-B

Phenethylamine/dimethoxy processing leads through freebase, crystals and mortar powder to advanced tablets. `TwoCbCrystalItem` and `TwoCbDoseItem` connect this chain to [`substance/twocb/`](../src/main/java/com/trippycraft/substance/twocb/).

`TwoCbWorldWarpPass` supplies saturated, crystalline, symmetrical imagery, trails and bass/fisheye response alongside shared graph and vanilla bridge effects. Preserve physical content, native dose amounts, partial crystals, recipe branch selection and packaging. Review it independently of 2C-E even where the production chain shares materials.

### 2C-E

The shared phenethylamine branch continues through its own precursor, freebase, crystal/powder and tablets. `TwoCeCrystalItem` and `TwoCeDoseItem` feed [`substance/twoce/`](../src/main/java/com/trippycraft/substance/twoce/).

Physical content and effect units remain separate; stacked sessions retain their original-age ceiling. `TwoCeWorldWarpPass` creates nested rooms/cavities, folding, perspective and trails. Changes to artistic gain must preserve server calibration, content and clock behavior. Check redosing, legacy clocks, reservoirs, foreground protection and shared-material continuity.

## Cultivation, world generation and exploration

[`block/`](../src/main/java/com/trippycraft/block/) contains cannabis, koka, opium poppy, strange mushroom, salvia, mescaline cactus, salt crystal and ergot wheat behavior. Growth, support, harvest and drops are distinct contracts. Poppy tapping persists the last harvest day in its block entity. `ErgotInfectionEvents`/`ErgotInfectionData` track mature vanilla wheat shaded continuously for one in-game day before conversion. `AcaciaHarvestEvents` supplies bark from Survival axe-stripping logs and leaves from Survival leaf breaking, without duplicating Creative drops.

Biome placement is defined by `data/trippycraft/worldgen/`, biome modifiers and tags. Follow configured feature → placed feature → biome modifier/tag, plus custom Java features when present. Mescaline belongs in desert/badlands clusters with its authored support/harvest behavior, rather than vanilla cactus height growth. Salvia uses jungle/sparse-jungle habitat. Models, blockstates, placement and loot tables must all agree with registered block states.

Exploration loot uses [`loot/`](../src/main/java/com/trippycraft/loot/) and additive injection tables under `data/trippycraft/loot_table/injections/`. Registration runs through `data/neoforge/loot_modifiers/global_loot_modifiers.json` and `data/trippycraft/loot_modifiers/`. Existing vanilla loot is retained; themed bundles and supported partial-content items add variation. Check the actual tables for product, weight and location rather than assuming universal availability.

`VillageHouseChestProcessor` integrates safe house chests with village templates/processor lists while retaining existing containers. Loot is generated when an unopened loot-table container resolves; already opened chests are not replenished. Validate fresh structures and old unopened containers separately when changing exploration data.

## Processing, dose formats and reusable equipment

Normal crafting/heating lives in `data/trippycraft/recipe/`, with custom serializers in `registry/` and `crafting/`. Machine processing is also encoded in [`block/entity/`](../src/main/java/com/trippycraft/block/entity/); recipe JSON alone is not the complete production catalog.

| Utility | Main responsibility | Contributor checks |
|---|---|---|
| Mortar and Pestle | Dried mushroom/koka processing and supported 4-HO-MET/2C-B/2C-E crystal-to-powder conversion | Remaining content, output counts, allowed inputs and tool/container behavior |
| Cannabis Press | Bud/extract processing and the retained MDMA tablet route | Fuel/progress, branch inputs, packaging and full output slots |
| Brew Cauldron | Plant, lysergic, medicinal, empathogen and synthetic precursor steps | Ingredient matching, returned vessels, counts, progress and saved state |
| Chemistry Table | Refinement, solvent/heroin/ketamine steps and selectable aromatic/phenethylamine branches | Branch selection, progress reset, server agreement and shared intermediates |
| Advanced Chemistry Table | Supported substance strength selection and final packaging | Preview versus actual output, physical/effect amounts, secondary inputs and legacy formats |

Each utility spans a block, block entity, menu and client screen. The server owns inputs, progress and outputs; the screen displays that state and requests selections. Inspect quick-move behavior and persistence alongside the main processing tick.

`StackableDoseItemSupport`, `ContainerReturnItemSupport` and substance-specific reservoir items preserve partially used content. Stack count, available content, label and engine dose are different values. Partial-stack identities prevent incompatible reservoirs from merging. Full inventories, Creative/Survival and both hands affect return paths and need direct checks.

Loading recipes accept one dried cannabis bud or a supported salvia strength. `BongItem` consumes the loaded hit and returns the reusable empty vessel. Fresh buds, extra ingredients, mixed bowls and already loaded vessels are not interchangeable inputs. Preserve the loading recipe and item-use rules together.

`CapsuleBlendRecipe` requires one empty capsule and supported ingredient slots. `CapsuleBlend.entryFromDoseItem` is the allowed-format boundary; not every item or substance belongs in a blend. Entries preserve route/content/effect amounts. Salvia loose items and bongs are excluded, and a readable legacy salvia-kind entry does not establish a functional capsule route.

## Villagers, trading and progression

The [`economy/`](../src/main/java/com/trippycraft/economy/) services own catalogs, role behavior, player progression and transactions. Their client screens display synchronized snapshots. Follow service validation and persistence whenever changing an offer or screen action.

| Role/system | Entry points | Behavior to preserve or deliberately review |
|---|---|---|
| Dealer | `DealerVillageEvents`, `DealerService`, `DealerRegistries`, `DealerProduct` | Village assignment, catalog categories, products/legacy aliases and purchase validation |
| Market Broker | `MarketService`, `MarketRegistries`, `MarketMath`, `MarketPlayerData`, `MarketContract` | Buying/selling, demand, reputation, levels, daily contracts and saved progression |
| Customer | `CustomerService`, `CustomerSampleEvents`, `CustomerAddictionProfile`, `CustomerOffer` | Daily cached requests, sampling, habits and offer completion |
| Currency/payouts | `EmeraldWallet`, `EmeraldPayoutLedger` | Inventory accounting, change, deferred payouts and transaction completion |
| Bartender | `BartenderService`, `ModVillagers` | Native merchant offers, drink stock, job-site identity and tavern anchoring |

Loaded-village role assignment excludes bartenders and temporary nightlife guests. Trading validates server-side context, proximity, inventory, quantities and catalog state. Some payouts are retained until a suitable completion/close path. MDMA social state can affect discounts. Changing these systems can change balance even when a GUI looks identical.

Known gameplay exploits are not silently corrected by documentation or asset maintenance; see [Security](../SECURITY.md). Tests must distinguish existing behavior from an intentionally proposed gameplay fix.

### Taverns and village life

`VillageBarEvents`, `VillageBarData`, `TavernGeometry` and `TavernNightlifeService` coordinate saved placement, terrain fit, roles and night guests. Structures under `data/trippycraft/structure/village/bars/` provide plains, desert, savanna, snowy and taiga palettes. `scripts/custom_tavern_blueprint.json` and its generator are the authoring sources.

Non-zombie villages receive the bounded one-or-two tavern target through safe loaded-world placement. Bartenders are tracked relative to a saved station; night patrons/dealers have separate lifecycle limits. Missing stations suppress migration/replacement spawning, while existing bartender anchoring and stock ticks have their own rules. Layout migrations differ: older intact layouts can be rebuilt, while some fixture updates fill only empty positions. Review the applicable migration and player-edit behavior before changing structures. Check foundations, entrance/path alignment, shoreline/slopes, revisits and server restarts rather than testing only one flat village.

### Cursed Findings

The in-game book covers every substance plus dependence/recovery and economy/dealing. Entries offer Overview, Effects, Chain, Recipes and Notes tabs. [`guide/CursedFindingsRegistry.java`](../src/main/java/com/trippycraft/guide/CursedFindingsRegistry.java) holds prose, item references and production/recipe descriptions; [`guide/CursedRecipeCodex.java`](../src/main/java/com/trippycraft/guide/CursedRecipeCodex.java) manages discovery, persistence and legacy chapter keys.

Consumption unlocks relevant discoveries through the engine; general system chapters are available independently. `CursedRecipeCodexItem`, `CursedFindingsSyncPacket`, `ClientCursedFindingsStore` and `CursedFindingsScreen` form the item/server/client path. The book is a structured guide, rather than a vanilla written-book page dump.

Human-written replacements for its AI-generated content are welcome across **all** chapters. Improve clarity, voice and descriptions while matching implemented fictional mechanics. An ingredient, dose, output, unlock or registry key is executable/reference data, not merely a wording choice. See [Contributing](../CONTRIBUTING.md) before submitting text or accompanying art.

## Client presentation, menus and VR

[`client/`](../src/main/java/com/trippycraft/client/) is registered through `ClientBootstrap`. Client-only registration keeps rendering and sound dependencies out of dedicated-server startup. `ClientEffectGraph`, `ClientFamilyBlend`, `ClientRenderSignature` and synchronized state combine the shared channels with each session's style. Accessibility/presentation settings must not erase authoritative server behavior.

### Rendering and lifecycle

`ClientRenderEvents` handles camera, fog and FOV integration. `WorldWarpPostProcessor` and dedicated `*WorldWarpPass` classes execute shader programs under `assets/trippycraft/shaders/program/`. Depth, view/projection, foreground, sky, hand and silhouette guards belong to the shared composition contract. Reprojection can move visible surfaces; it cannot reveal unseen block faces or change collisions.

`ClientBlockIllusions`, `ClientEntityHallucinations`, `ClientWorldHallucinations`, `HoMetTrailRenderer` and DMT geometry add separate world-object/mesh/history paths. They are not all implemented by fullscreen shaders. Particle simulation and phase clocks must not advance twice for stereo views.

`PsychedelicOverlayRenderer`, `TripStatusBarOverlay`, `TripScrubScreen`, dependency/depletion overlays and death presentation cover art, status and recovery. Keep sober readability, recovery state and UI clipping/input behavior intact. The default `J` timeline is available for psilocybin, 4-HO-MET, AL-LAD, 2C-E and nitrous; it is not a timeline for every substance.

`ClientWorldSessionEvents` and reload listeners clear world/session/resource-dependent histories, renderer state and audio. Review expiry, fresh use, logout, world switches, resource reloads and disabled presentation settings when changing any retained buffer or static cache.

### Menus and utility screens

Dealer, Market Broker, Customer requests and Cursed Findings use custom screens; all five utilities use container screens. Bartender trading uses Minecraft's native merchant screen. `FittedGui` and `FittedContainerScreen` fit panels into the actual logical GUI canvas and map drawing, scissors, hover/click coordinates and inventory dragging together.

Keep tooltip placement, scroll bounds, hidden-row rejection, release/drag coordinates and server selections aligned with the visible controls. Substance effects on menus are supported. A layout change must work at small VR canvases as well as desktop sizes; it must not override Vivecraft's native GUI pose or implement independent eye projections.

### VR status and boundaries

**VR support is available through optional Vivecraft integration, especially for visual effects. Movement, camera-motion and other input-related effects are currently buggy in VR. These paths need further contributor work and physical-headset testing. Automated stereo and GUI checks do not establish complete VR compatibility or comfort.**

`VivecraftCompat` discovers the optional integration without making Vivecraft required. Native tracked eye positions, asymmetric projection and controller picking remain authoritative. Shared head/world anchors, artistic clocks, smoothed state, overlays and tavern choreography are computed consistently for the stereo frame. Artistic overlays use premultiplied alpha and restore render state; genuine menu/status/recovery UI stays on Vivecraft's GUI surface.

HMD gaze can target visual surfaces while controllers select real objects. Camera art must preserve tracked poses. Dedicated movement hooks for cannabis, cocaine, ketamine, heroin and alcohol still affect player velocity and are not a complete VR input adaptation. `motionReduction` disables those hooks; `motionIntensity = 0` alone does not. This distinction does not guarantee a full workaround for every input/camera bug.

Desktop shaders and generated VR adapters share authored expressions through `scripts/generate-vr-shaders.py` and `scripts/shaders/vr-common.glsl`. Check generation parity, both eyes, state restoration, mirrors/cameras/telescopes, real controller targeting and headset comfort for relevant changes. Vivecraft 1.21.1-1.3.5 is a useful current test target; broader versions, shader packs and modpacks require their own checks.

## Audio, music and reactive effects

`ClientAudioEvents` shapes sound through event routing while retaining categories and attenuation. `TavernMusicSound` handles tavern PCM stream processing. World, ambient, music, records and tavern audio have distinct paths. Audio enablement/intensity is independent of visual controls; check sober reset, mixtures and world changes as well as a peak effect.

The nineteen-track tavern playlist connects `TavernPlaylist`, `TavernPlaybackData`, `TavernAudioModel`, `TavernAmbienceService` and `TavernJukeboxEvents` to `TavernAmbiencePacket`. Client playback uses `ClientTavernAmbience` and `TavernMusicSound`; `TavernRaveRenderer` and substance presentation consume music/bass signals.

Authored track lengths, world-time playlist position, saved offsets, distance fade and live audio analysis affect synchronized playback and choreography. Replacing or re-encoding a track may alter reactive presentation even if the file still plays. Review seeking, reconnects, all tracks, spatial volume and music-driven effects together. Asset authorship and submission requirements are in [Asset provenance](ASSET_PROVENANCE.md).

## Networking, configuration and saves

[`network/ModNetwork.java`](../src/main/java/com/trippycraft/network/ModNetwork.java) registers snapshot/action packets and protocol compatibility. Packet records/codecs and `ClientPacketHandlers` define synchronization boundaries. Server services validate requested transactions and selections; snapshots describe state for clients. A changed packet shape needs a deliberate protocol/compatibility review and matching client/server checks.

[`config/TrippyConfig.java`](../src/main/java/com/trippycraft/config/TrippyConfig.java) defines common gameplay controls such as timing, risk and recovery. [`config/TrippyClientConfig.java`](../src/main/java/com/trippycraft/config/TrippyClientConfig.java) defines visual/audio intensity, motion, photosensitivity and debug controls. Defaults are behavior, not cosmetic build settings. `/viceandvillagers status` and `sync` inspect development state; permission-level-2 `reset` clears it. `/trippycraft` remains the compatibility alias.

| Saved state | Source owners | Compatibility concerns |
|---|---|---|
| Player experience | `SubstanceEvents`, `SubstancePersistence`, `PsychoactiveExperience` | Dedicated sessions, physical/effect amounts, signatures, clocks, tolerance and recovery |
| Player economy | `MarketEvents`, `MarketDataStore`, `MarketPersistence` | Reputation and trade/contract counters, demand/contracts and refresh times; level is derived |
| Book discovery | `CursedRecipeCodex` | Chapter IDs, legacy keys and synchronization |
| World systems | `VillageBarData`, `ErgotInfectionData`, `TavernPlaybackData` | Placement/revisions, infection timers and playlist offsets |
| Blocks/items/villagers | Their NBT/data-component and block-entity paths | Reservoir content, selection/progress, tapped crops, role/station links and customer habits/daily offers |

Player sidecars retain the `trippycraft` and `trippycraft_market` names alongside vanilla player data. `NbtCompat` and saved aliases support existing worlds. For example, old koka/cocaine identifiers, the legacy MDMA tablet, dealer products and book chapter keys cannot be removed just because they are hidden from current menus.

Pending payouts in `EmeraldPayoutLedger` are transient server state, rather than persisted market data. Review their open/close/logout flushing and server-stop cleanup separately from a sidecar save. Customer state belongs to villager persistent NBT, not the player's market sidecar.

Login, clone, death/respawn, dimension change, logout and server stop have different hooks. Trace them rather than assuming a save/load round trip covers every lifecycle. Use a private copy of an existing world for changes to persistence, aliases, roles or structures.

## Working through a contribution

Choose the relevant system above, then trace its dependencies through acquisition, server behavior, saved state, packets and presentation. For a new substance or format, include items, the ordered profile/native session, registries, language/model/texture resources, recipes/loot/catalogs, book discovery, persistence/snapshots and appropriate tests. Minecraft 1.21.1 loads item models from `models/item/`; newer-format `items/` definitions are not a substitute.

For existing systems, preserve contracts outside the intended change: hidden IDs, migration fallbacks, content calibration, session clocks, render depth/foreground protection, audio categories and fitted input coordinates. Read [Development and validation](DEVELOPMENT.md) for automated coverage, the whole-mod manual matrix and source-writing tools. Submit concise evidence of the actual checks and unresolved limits rather than adding another historical implementation report to the general documentation.
