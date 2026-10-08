package com.trippycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class TrippyConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_VANILLA_BRIDGE = BUILDER
            .comment("Applies subtle vanilla status effects as server-side physiological feedback.")
            .define("enableVanillaBridge", true);

    private static final ModConfigSpec.DoubleValue EXPERIENCE_TIME_SCALE = BUILDER
            .comment("Scales experience durations. 1.0 is the authored gameplay duration.")
            .defineInRange("experienceTimeScale", 1.0D, 0.1D, 10.0D);

    private static final ModConfigSpec.DoubleValue GLOBAL_INTENSITY = BUILDER
            .comment("Global multiplier for abstract experience intensity. Does not represent real-world potency.")
            .defineInRange("globalIntensity", 1.0D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue PSILOCYBIN_BLEND_WEIGHT = BUILDER
            .comment("Composable psychedelic blend weight for psilocybin. Defaults preserve existing single-substance behavior.")
            .defineInRange("psilocybinBlendWeight", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue LSD_BLEND_WEIGHT = BUILDER
            .comment("Composable psychedelic blend weight for LSD. Defaults preserve existing single-substance behavior.")
            .defineInRange("lsdBlendWeight", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue TWO_CB_BLEND_WEIGHT = BUILDER
            .comment("Composable psychedelic blend weight for the dedicated 2C-B effect system.")
            .defineInRange("twoCbBlendWeight", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_BLEND_WEIGHT = BUILDER
            .comment("Composable psychedelic blend weight for DMT before breakthrough dominance is applied.")
            .defineInRange("dmtBlendWeight", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.BooleanValue ENABLE_RISK_MECHANICS = BUILDER
            .comment("Enables negative risk channels such as heavy sedation, panic bias, and stimulant strain.")
            .define("enableRiskMechanics", true);

    private static final ModConfigSpec.DoubleValue RISK_SEVERITY = BUILDER
            .comment("Scales fictional gameplay risk pressure from mixed sessions, comedown, craving, and strain. Does not represent real-world medical risk.")
            .defineInRange("riskSeverity", 1.0D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue RECOVERY_RATE = BUILDER
            .comment("Scales recovery from tolerance, comedown, craving, and strain when the player rests, eats, and stays in safer settings.")
            .defineInRange("recoveryRate", 1.0D, 0.1D, 5.0D);

    private static final ModConfigSpec.BooleanValue ENABLE_PSILOCYBIN_EFFECTS = BUILDER
            .comment("Enables the dedicated psilocybin capsule dose-response system.")
            .define("enablePsilocybinEffects", true);

    private static final ModConfigSpec.DoubleValue PSILOCYBIN_MAX_VISUAL_INTENSITY = BUILDER
            .comment("Legacy compatibility key. Positive values no longer cap or reduce psilocybin visuals; 0 still disables them. Use psilocybinVisualGain to adjust strength.")
            .defineInRange("psilocybinMaxVisualIntensity", 6.0D, 0.0D, 12.0D);

    private static final ModConfigSpec.DoubleValue PSILOCYBIN_VISUAL_GAIN = BUILDER
            .comment("Scales psilocybin surface visuals at every dose: 1 is normal, 0 disables them. Dose intensity has no ceiling.")
            .defineInRange("psilocybinVisualGain", 1.0D, 0.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue PSILOCYBIN_MAX_CAMERA_DISTORTION = BUILDER
            .comment("Legacy compatibility setting. Psilocybin keeps the camera steady and animates surface textures instead.")
            .defineInRange("psilocybinMaxCameraDistortion", 4.50D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue PSILOCYBIN_PARTICLE_MULTIPLIER = BUILDER
            .comment("Multiplier for psilocybin spores, motes, and hyphae-style particles.")
            .defineInRange("psilocybinParticleMultiplier", 1.75D, 0.0D, 6.0D);

    private static final ModConfigSpec.BooleanValue PSILOCYBIN_CAMERA_MOVEMENT = BUILDER
            .comment("Legacy compatibility setting. Camera breathing is no longer used by psilocybin.")
            .define("psilocybinCameraMovement", false);

    private static final ModConfigSpec.BooleanValue PSILOCYBIN_TRACERS = BUILDER
            .comment("Enables psilocybin-specific organic tracers and afterimages.")
            .define("psilocybinTracers", true);

    private static final ModConfigSpec.BooleanValue PSILOCYBIN_SURFACE_MELTING = BUILDER
            .comment("Enables psilocybin-specific surface melting illusions. Block states and collisions never change.")
            .define("psilocybinSurfaceMelting", true);

    private static final ModConfigSpec.BooleanValue PSILOCYBIN_FUNGAL_OVERLAYS = BUILDER
            .comment("Enables psilocybin-specific fungal/mycelium overlay patterns.")
            .define("psilocybinFungalOverlays", true);

    private static final ModConfigSpec.BooleanValue PSILOCYBIN_WORLD_WARP = BUILDER
            .comment("Enables psilocybin surface shaders. Accessibility mode softens their motion and contrast.")
            .define("psilocybinWorldWarp", true);

    private static final ModConfigSpec.BooleanValue PSILOCYBIN_ACCESSIBILITY_MODE = BUILDER
            .comment("Reduces psilocybin surface distortion, tracer presence and particle density.")
            .define("psilocybinAccessibilityMode", false);

    private static final ModConfigSpec.BooleanValue ENABLE_LSD_EFFECTS = BUILDER
            .comment("Enables the dedicated LSD blotter dose-response system.")
            .define("enableLsdEffects", true);

    private static final ModConfigSpec.DoubleValue LSD_MAX_VISUAL_INTENSITY = BUILDER
            .comment("Legacy compatibility key. Positive values no longer cap or reduce LSD visuals; 0 still disables them. Use lsdVisualGain to adjust strength.")
            .defineInRange("lsdMaxVisualIntensity", 7.0D, 0.0D, 14.0D);

    private static final ModConfigSpec.DoubleValue LSD_VISUAL_GAIN = BUILDER
            .comment("Scales every LSD visual dose: 1 is normal, 0 disables visuals. Higher doses continue increasing without an intensity ceiling.")
            .defineInRange("lsdVisualGain", 1.0D, 0.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue LSD_TIER_1_MICROGRAMS = BUILDER
            .comment("Total active LSD gameplay dose for tier 1 onset visuals.")
            .defineInRange("lsdTier1Micrograms", 50.0D, 1.0D, 1000.0D);

    private static final ModConfigSpec.DoubleValue LSD_TIER_2_MICROGRAMS = BUILDER
            .comment("Total active LSD gameplay dose for tier 2 clear visuals.")
            .defineInRange("lsdTier2Micrograms", 100.0D, 1.0D, 1200.0D);

    private static final ModConfigSpec.DoubleValue LSD_TIER_3_MICROGRAMS = BUILDER
            .comment("Total active LSD gameplay dose for tier 3 strong visuals.")
            .defineInRange("lsdTier3Micrograms", 150.0D, 1.0D, 1500.0D);

    private static final ModConfigSpec.DoubleValue LSD_TIER_4_MICROGRAMS = BUILDER
            .comment("Total active LSD gameplay dose for tier 4 very strong visuals.")
            .defineInRange("lsdTier4Micrograms", 250.0D, 1.0D, 1800.0D);

    private static final ModConfigSpec.DoubleValue LSD_TIER_5_MICROGRAMS = BUILDER
            .comment("Total active LSD gameplay dose for tier 5 reality collapse visuals.")
            .defineInRange("lsdTier5Micrograms", 800.0D, 1.0D, 2200.0D);

    private static final ModConfigSpec.IntValue LSD_50_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for a 50ug LSD blotter.")
            .defineInRange("lsd50ugDurationSeconds", 210, 20, 3600);

    private static final ModConfigSpec.IntValue LSD_100_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for a 100ug LSD blotter.")
            .defineInRange("lsd100ugDurationSeconds", 360, 20, 5400);

    private static final ModConfigSpec.IntValue LSD_250_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for a 250ug LSD blotter.")
            .defineInRange("lsd250ugDurationSeconds", 520, 20, 7200);

    private static final ModConfigSpec.IntValue LSD_STACKED_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for stacked high LSD doses.")
            .defineInRange("lsdStackedDurationSeconds", 660, 20, 9000);

    private static final ModConfigSpec.BooleanValue LSD_WORLD_WARP = BUILDER
            .comment("Enables LSD world-surface distortion, flowing color, geometry echoes, and sky vortexing.")
            .define("lsdWorldWarp", true);

    private static final ModConfigSpec.BooleanValue LSD_BLOCK_EFFECTS = BUILDER
            .comment("Enables LSD client-side block-face melting, autonomy, duplicated outlines, and liquid-gloss illusions.")
            .define("lsdBlockEffects", true);

    private static final ModConfigSpec.BooleanValue LSD_ENTITY_EFFECTS = BUILDER
            .comment("Enables LSD entity wobble, chromatic echo silhouettes, and geometric duplication illusions.")
            .define("lsdEntityEffects", true);

    private static final ModConfigSpec.BooleanValue LSD_TRACERS = BUILDER
            .comment("Enables LSD temporal tracer and afterimage effects. These are client-side visual echoes only.")
            .define("lsdTracers", true);

    private static final ModConfigSpec.IntValue LSD_MAX_TRACER_LAYERS = BUILDER
            .comment("Maximum LSD tracer/afterimage layers used by the shader.")
            .defineInRange("lsdMaxTracerLayers", 6, 1, 8);

    private static final ModConfigSpec.DoubleValue LSD_TRACER_LIFETIME = BUILDER
            .comment("Scales LSD tracer persistence. Higher values make temporal echoes last longer.")
            .defineInRange("lsdTracerLifetime", 1.0D, 0.2D, 3.0D);

    private static final ModConfigSpec.DoubleValue LSD_CHROMATIC_INTENSITY = BUILDER
            .comment("Scales localized LSD chromatic splitting on edges, tracers, and geometry echoes.")
            .defineInRange("lsdChromaticIntensity", 1.0D, 0.0D, 3.0D);

    private static final ModConfigSpec.BooleanValue LSD_PERFORMANCE_MODE = BUILDER
            .comment("Reduces LSD particles, entity echoes, block illusion density, and tracer layer count.")
            .define("lsdPerformanceMode", false);

    private static final ModConfigSpec.BooleanValue LSD_ACCESSIBILITY_MODE = BUILDER
            .comment("Softens LSD geometry motion, tracer presence, and color contrast while keeping slow world distortions.")
            .define("lsdAccessibilityMode", false);

    private static final ModConfigSpec.DoubleValue LSD_PARTICLE_MULTIPLIER = BUILDER
            .comment("Multiplier for LSD prismatic motes, liquid current sparks, and floating block-fragment particles.")
            .defineInRange("lsdParticleMultiplier", 1.25D, 0.0D, 6.0D);

    private static final ModConfigSpec.BooleanValue ENABLE_TWO_CB_EFFECTS = BUILDER
            .comment("Enables the dedicated 2C-B tablet dose-response system.")
            .define("enableTwoCbEffects", true);

    private static final ModConfigSpec.DoubleValue TWO_CB_MAX_VISUAL_INTENSITY = BUILDER
            .comment("Soft normalization ceiling for 2C-B saturation, neon edges, beat tracers, and crystalline geometry.")
            .defineInRange("twoCbMaxVisualIntensity", 5.5D, 0.0D, 12.0D);

    private static final ModConfigSpec.BooleanValue TWO_CB_WORLD_WARP = BUILDER
            .comment("Enables the 2C-B crisp geometric post-world shader.")
            .define("twoCbWorldWarp", true);

    private static final ModConfigSpec.BooleanValue TWO_CB_ACCESSIBILITY_MODE = BUILDER
            .comment("Reduces 2C-B high-contrast outlines, rapid beat pulses, tracers, and kaleidoscope moments.")
            .define("twoCbAccessibilityMode", false);

    private static final ModConfigSpec.BooleanValue ENABLE_DMT_EFFECTS = BUILDER
            .comment("Enables the dedicated DMT dose-response system.")
            .define("enableDmtEffects", true);

    private static final ModConfigSpec.DoubleValue DMT_MAX_VISUAL_INTENSITY = BUILDER
            .comment("Maximum visual intensity for DMT-specific hyperspace geometry, kaleidoscopes, and world dissolve.")
            .defineInRange("dmtMaxVisualIntensity", 8.5D, 0.0D, 16.0D);

    private static final ModConfigSpec.DoubleValue DMT_TIER_1_UNITS = BUILDER
            .comment("Total active DMT gameplay dose for tier 1 launch visuals.")
            .defineInRange("dmtTier1Units", 0.45D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_TIER_2_UNITS = BUILDER
            .comment("Total active DMT gameplay dose for tier 2 tunnel visuals.")
            .defineInRange("dmtTier2Units", 0.90D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_TIER_3_UNITS = BUILDER
            .comment("Total active DMT gameplay dose for tier 3 chrysanthemum visuals.")
            .defineInRange("dmtTier3Units", 1.55D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_TIER_4_UNITS = BUILDER
            .comment("Total active DMT gameplay dose for tier 4 breakthrough visuals.")
            .defineInRange("dmtTier4Units", 2.40D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_TIER_5_UNITS = BUILDER
            .comment("Total active DMT gameplay dose for tier 5 late hyperspace visuals. Full world replacement is shader-ramped closer to 7x.")
            .defineInRange("dmtTier5Units", 5.20D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_ONSET_SPEED = BUILDER
            .comment("Scales how quickly stacked DMT doses ramp into active effect state.")
            .defineInRange("dmtOnsetSpeed", 1.0D, 0.1D, 5.0D);

    private static final ModConfigSpec.DoubleValue DMT_PRE_BREAKTHROUGH_THRESHOLD_UNITS = BUILDER
            .comment("DMT gameplay dose where threshold shimmer resolves into pre-breakthrough pressure.")
            .defineInRange("dmtPreBreakthroughThresholdUnits", 1.55D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_BREAKTHROUGH_THRESHOLD_UNITS = BUILDER
            .comment("DMT gameplay dose where transition and post-breakthrough encounter-space can become dominant.")
            .defineInRange("dmtBreakthroughThresholdUnits", 2.40D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue DMT_TRANSITION_START_PROGRESS = BUILDER
            .comment("Trip progress where the breakthrough crossing transition may begin.")
            .defineInRange("dmtTransitionStartProgress", 0.18D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue DMT_TRANSITION_END_PROGRESS = BUILDER
            .comment("Trip progress where the breakthrough crossing transition settles into encounter-space.")
            .defineInRange("dmtTransitionEndProgress", 0.30D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue DMT_POST_BREAKTHROUGH_START_PROGRESS = BUILDER
            .comment("Trip progress where post-breakthrough themed encounter-space takes over after the crossing.")
            .defineInRange("dmtPostBreakthroughStartProgress", 0.30D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue DMT_RETURN_START_PROGRESS = BUILDER
            .comment("Trip progress where DMT encounter-space begins returning toward the ordinary world.")
            .defineInRange("dmtReturnStartProgress", 0.72D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue DMT_AFTERGLOW_START_PROGRESS = BUILDER
            .comment("Trip progress where DMT-specific effects resolve into afterglow.")
            .defineInRange("dmtAfterglowStartProgress", 0.88D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue DMT_INTENSITY_CURVE_POWER = BUILDER
            .comment("Power curve applied to DMT perceptual intensity. 1.0 preserves the authored response.")
            .defineInRange("dmtIntensityCurvePower", 1.0D, 0.25D, 3.0D);

    private static final ModConfigSpec.IntValue DMT_LIGHT_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for a light DMT trip.")
            .defineInRange("dmtLightDurationSeconds", 55, 10, 1200);

    private static final ModConfigSpec.IntValue DMT_STANDARD_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for a standard DMT trip.")
            .defineInRange("dmtStandardDurationSeconds", 85, 10, 1600);

    private static final ModConfigSpec.IntValue DMT_BREAKTHROUGH_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for a breakthrough DMT trip.")
            .defineInRange("dmtBreakthroughDurationSeconds", 120, 10, 2200);

    private static final ModConfigSpec.IntValue DMT_STACKED_DURATION_SECONDS = BUILDER
            .comment("Base authored gameplay duration for stacked high DMT doses.")
            .defineInRange("dmtStackedDurationSeconds", 155, 10, 2600);

    private static final ModConfigSpec.BooleanValue DMT_WORLD_WARP = BUILDER
            .comment("Enables DMT post-world shader distortion, kaleidoscopic tunnels, and volumetric hyperspace mapping.")
            .define("dmtWorldWarp", true);

    private static final ModConfigSpec.BooleanValue DMT_PARTICLES = BUILDER
            .comment("Enables DMT hyperspace motes and tunnel particle hints.")
            .define("dmtParticles", true);

    private static final ModConfigSpec.DoubleValue DMT_PARTICLE_MULTIPLIER = BUILDER
            .comment("Multiplier for DMT hyperspace motes.")
            .defineInRange("dmtParticleMultiplier", 1.45D, 0.0D, 6.0D);

    private static final ModConfigSpec.DoubleValue DMT_ENCOUNTER_FREQUENCY = BUILDER
            .comment("Frequency of rare DMT post-breakthrough encounter events. Higher values create more event windows, not constant spam.")
            .defineInRange("dmtEncounterFrequency", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_ARCHITECTURE_FREQUENCY = BUILDER
            .comment("Scales impossible architecture layer density during DMT post-breakthrough.")
            .defineInRange("dmtArchitectureFrequency", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_SYMBOL_DENSITY = BUILDER
            .comment("Scales DMT symbolic language density in shaders, overlays, and particles.")
            .defineInRange("dmtSymbolDensity", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_ENTITY_DENSITY = BUILDER
            .comment("Scales rare DMT entity-presence layers. Higher values increase presence intensity without hostile spawns.")
            .defineInRange("dmtEntityDensity", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_ENVIRONMENTAL_CONSCIOUSNESS = BUILDER
            .comment("Scales DMT environmental aliveness and world-consciousness layers.")
            .defineInRange("dmtEnvironmentalConsciousness", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_SPATIAL_REPLACEMENT_WEIGHT = BUILDER
            .comment("Scales how strongly breakthrough DMT replaces ordinary spatial logic with encounter-space.")
            .defineInRange("dmtSpatialReplacementWeight", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue DMT_BREAKTHROUGH_DOMINANCE_WEIGHT = BUILDER
            .comment("Scales DMT dominance over spatial and encounter layers after breakthrough while LSD/psilocybin still color the result.")
            .defineInRange("dmtBreakthroughDominanceWeight", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.BooleanValue DMT_ACCESSIBILITY_MODE = BUILDER
            .comment("Reduces DMT camera pull, high-frequency kaleidoscope contrast, and bright chromatic pulses.")
            .define("dmtAccessibilityMode", false);

    private static final ModConfigSpec.BooleanValue ENABLE_SALVIA_EFFECTS = BUILDER
            .comment("Enables the dedicated salvia dissociation visuals, camera pull, and audio shaping.")
            .define("enableSalviaEffects", true);

    private static final ModConfigSpec.DoubleValue SALVIA_MAX_VISUAL_INTENSITY = BUILDER
            .comment("Soft normalization ceiling for salvia-specific seams, wheel/folding distortions, stacked realities, and re-entry confusion.")
            .defineInRange("salviaMaxVisualIntensity", 20.0D, 0.0D, 48.0D);

    private static final ModConfigSpec.BooleanValue SALVIA_ACCESSIBILITY_MODE = BUILDER
            .comment("Reduces salvia camera displacement, observer silhouettes, and seam density while keeping slower uncanny layering.")
            .define("salviaAccessibilityMode", false);

    private static final ModConfigSpec.BooleanValue ENABLE_ALCOHOL_EFFECTS = BUILDER
            .comment("Enables the dedicated alcohol dose-response system, including double vision, balance loss, and depressant visuals.")
            .define("enableAlcoholEffects", true);

    private static final ModConfigSpec.DoubleValue ALCOHOL_MAX_VISUAL_INTENSITY = BUILDER
            .comment("Maximum visual intensity for alcohol-specific diplopia, focus blur, sway, and peripheral narrowing.")
            .defineInRange("alcoholMaxVisualIntensity", 6.0D, 0.0D, 12.0D);

    private static final ModConfigSpec.BooleanValue ALCOHOL_WORLD_WARP = BUILDER
            .comment("Enables alcohol post-world shader distortion, double vision, swimmy focus, and peripheral darkening.")
            .define("alcoholWorldWarp", true);

    private static final ModConfigSpec.BooleanValue ALCOHOL_MOVEMENT_DIFFICULTY = BUILDER
            .comment("Enables alcohol-specific client-side balance drift in addition to vanilla slowness feedback.")
            .define("alcoholMovementDifficulty", true);

    private static final ModConfigSpec.BooleanValue ALCOHOL_ACCESSIBILITY_MODE = BUILDER
            .comment("Reduces alcohol camera sway, balance drift, double-vision separation, and blur while keeping slower depressant cues.")
            .define("alcoholAccessibilityMode", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableVanillaBridge = true;
    public static double experienceTimeScale = 1.0D;
    public static double globalIntensity = 1.0D;
    public static double psilocybinBlendWeight = 1.0D;
    public static double lsdBlendWeight = 1.0D;
    public static double twoCbBlendWeight = 1.0D;
    public static double dmtBlendWeight = 1.0D;
    public static boolean enableRiskMechanics = true;
    public static double riskSeverity = 1.0D;
    public static double recoveryRate = 1.0D;
    public static boolean enablePsilocybinEffects = true;
    public static double psilocybinMaxVisualIntensity = 6.0D;
    public static double psilocybinVisualGain = 1.0D;
    public static double psilocybinMaxCameraDistortion = 4.50D;
    public static double psilocybinParticleMultiplier = 1.75D;
    public static boolean psilocybinCameraMovement = false;
    public static boolean psilocybinTracers = true;
    public static boolean psilocybinSurfaceMelting = true;
    public static boolean psilocybinFungalOverlays = true;
    public static boolean psilocybinWorldWarp = true;
    public static boolean psilocybinAccessibilityMode = false;
    public static boolean enableLsdEffects = true;
    public static double lsdMaxVisualIntensity = 7.0D;
    public static double lsdVisualGain = 1.0D;
    public static double lsdTier1Micrograms = 50.0D;
    public static double lsdTier2Micrograms = 100.0D;
    public static double lsdTier3Micrograms = 150.0D;
    public static double lsdTier4Micrograms = 250.0D;
    public static double lsdTier5Micrograms = 800.0D;
    public static int lsd50ugDurationSeconds = 210;
    public static int lsd100ugDurationSeconds = 360;
    public static int lsd250ugDurationSeconds = 520;
    public static int lsdStackedDurationSeconds = 660;
    public static boolean lsdWorldWarp = true;
    public static boolean lsdBlockEffects = true;
    public static boolean lsdEntityEffects = true;
    public static boolean lsdTracers = true;
    public static int lsdMaxTracerLayers = 6;
    public static double lsdTracerLifetime = 1.0D;
    public static double lsdChromaticIntensity = 1.0D;
    public static boolean lsdPerformanceMode = false;
    public static boolean lsdAccessibilityMode = false;
    public static double lsdParticleMultiplier = 1.25D;
    public static boolean enableTwoCbEffects = true;
    public static double twoCbMaxVisualIntensity = 5.5D;
    public static boolean twoCbWorldWarp = true;
    public static boolean twoCbAccessibilityMode = false;
    public static boolean enableDmtEffects = true;
    public static double dmtMaxVisualIntensity = 8.5D;
    public static double dmtTier1Units = 0.45D;
    public static double dmtTier2Units = 0.90D;
    public static double dmtTier3Units = 1.55D;
    public static double dmtTier4Units = 2.40D;
    public static double dmtTier5Units = 5.20D;
    public static double dmtOnsetSpeed = 1.0D;
    public static double dmtPreBreakthroughThresholdUnits = 1.55D;
    public static double dmtBreakthroughThresholdUnits = 2.40D;
    public static double dmtTransitionStartProgress = 0.18D;
    public static double dmtTransitionEndProgress = 0.30D;
    public static double dmtPostBreakthroughStartProgress = 0.30D;
    public static double dmtReturnStartProgress = 0.72D;
    public static double dmtAfterglowStartProgress = 0.88D;
    public static double dmtIntensityCurvePower = 1.0D;
    public static int dmtLightDurationSeconds = 55;
    public static int dmtStandardDurationSeconds = 85;
    public static int dmtBreakthroughDurationSeconds = 120;
    public static int dmtStackedDurationSeconds = 155;
    public static boolean dmtWorldWarp = true;
    public static boolean dmtParticles = true;
    public static double dmtParticleMultiplier = 1.45D;
    public static double dmtEncounterFrequency = 1.0D;
    public static double dmtArchitectureFrequency = 1.0D;
    public static double dmtSymbolDensity = 1.0D;
    public static double dmtEntityDensity = 1.0D;
    public static double dmtEnvironmentalConsciousness = 1.0D;
    public static double dmtSpatialReplacementWeight = 1.0D;
    public static double dmtBreakthroughDominanceWeight = 1.0D;
    public static boolean dmtAccessibilityMode = false;
        public static boolean enableSalviaEffects = true;
        public static double salviaMaxVisualIntensity = 10.8D;
        public static boolean salviaAccessibilityMode = false;
    public static boolean enableAlcoholEffects = true;
    public static double alcoholMaxVisualIntensity = 6.0D;
    public static boolean alcoholWorldWarp = true;
    public static boolean alcoholMovementDifficulty = true;
    public static boolean alcoholAccessibilityMode = false;

    private TrippyConfig() {
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enableVanillaBridge = ENABLE_VANILLA_BRIDGE.get();
        experienceTimeScale = EXPERIENCE_TIME_SCALE.get();
        globalIntensity = GLOBAL_INTENSITY.get();
        psilocybinBlendWeight = PSILOCYBIN_BLEND_WEIGHT.get();
        lsdBlendWeight = LSD_BLEND_WEIGHT.get();
        twoCbBlendWeight = TWO_CB_BLEND_WEIGHT.get();
        dmtBlendWeight = DMT_BLEND_WEIGHT.get();
        enableRiskMechanics = ENABLE_RISK_MECHANICS.get();
        riskSeverity = RISK_SEVERITY.get();
        recoveryRate = RECOVERY_RATE.get();
        enablePsilocybinEffects = ENABLE_PSILOCYBIN_EFFECTS.get();
        psilocybinMaxVisualIntensity = PSILOCYBIN_MAX_VISUAL_INTENSITY.get();
        psilocybinVisualGain = PSILOCYBIN_VISUAL_GAIN.get();
        psilocybinMaxCameraDistortion = PSILOCYBIN_MAX_CAMERA_DISTORTION.get();
        psilocybinParticleMultiplier = PSILOCYBIN_PARTICLE_MULTIPLIER.get();
        psilocybinCameraMovement = PSILOCYBIN_CAMERA_MOVEMENT.get();
        psilocybinTracers = PSILOCYBIN_TRACERS.get();
        psilocybinSurfaceMelting = PSILOCYBIN_SURFACE_MELTING.get();
        psilocybinFungalOverlays = PSILOCYBIN_FUNGAL_OVERLAYS.get();
        psilocybinWorldWarp = PSILOCYBIN_WORLD_WARP.get();
        psilocybinAccessibilityMode = PSILOCYBIN_ACCESSIBILITY_MODE.get();
        enableLsdEffects = ENABLE_LSD_EFFECTS.get();
        lsdMaxVisualIntensity = LSD_MAX_VISUAL_INTENSITY.get();
        lsdVisualGain = LSD_VISUAL_GAIN.get();
        lsdTier1Micrograms = LSD_TIER_1_MICROGRAMS.get();
        lsdTier2Micrograms = LSD_TIER_2_MICROGRAMS.get();
        lsdTier3Micrograms = LSD_TIER_3_MICROGRAMS.get();
        lsdTier4Micrograms = LSD_TIER_4_MICROGRAMS.get();
        lsdTier5Micrograms = LSD_TIER_5_MICROGRAMS.get();
        lsd50ugDurationSeconds = LSD_50_DURATION_SECONDS.get();
        lsd100ugDurationSeconds = LSD_100_DURATION_SECONDS.get();
        lsd250ugDurationSeconds = LSD_250_DURATION_SECONDS.get();
        lsdStackedDurationSeconds = LSD_STACKED_DURATION_SECONDS.get();
        lsdWorldWarp = LSD_WORLD_WARP.get();
        lsdBlockEffects = LSD_BLOCK_EFFECTS.get();
        lsdEntityEffects = LSD_ENTITY_EFFECTS.get();
        lsdTracers = LSD_TRACERS.get();
        lsdMaxTracerLayers = LSD_MAX_TRACER_LAYERS.get();
        lsdTracerLifetime = LSD_TRACER_LIFETIME.get();
        lsdChromaticIntensity = LSD_CHROMATIC_INTENSITY.get();
        lsdPerformanceMode = LSD_PERFORMANCE_MODE.get();
        lsdAccessibilityMode = LSD_ACCESSIBILITY_MODE.get();
        lsdParticleMultiplier = LSD_PARTICLE_MULTIPLIER.get();
        enableTwoCbEffects = ENABLE_TWO_CB_EFFECTS.get();
        twoCbMaxVisualIntensity = TWO_CB_MAX_VISUAL_INTENSITY.get();
        twoCbWorldWarp = TWO_CB_WORLD_WARP.get();
        twoCbAccessibilityMode = TWO_CB_ACCESSIBILITY_MODE.get();
        enableDmtEffects = ENABLE_DMT_EFFECTS.get();
        dmtMaxVisualIntensity = DMT_MAX_VISUAL_INTENSITY.get();
        dmtTier1Units = DMT_TIER_1_UNITS.get();
        dmtTier2Units = DMT_TIER_2_UNITS.get();
        dmtTier3Units = DMT_TIER_3_UNITS.get();
        dmtTier4Units = DMT_TIER_4_UNITS.get();
        dmtTier5Units = DMT_TIER_5_UNITS.get();
        dmtOnsetSpeed = DMT_ONSET_SPEED.get();
        dmtPreBreakthroughThresholdUnits = DMT_PRE_BREAKTHROUGH_THRESHOLD_UNITS.get();
        dmtBreakthroughThresholdUnits = DMT_BREAKTHROUGH_THRESHOLD_UNITS.get();
        dmtTransitionStartProgress = DMT_TRANSITION_START_PROGRESS.get();
        dmtTransitionEndProgress = DMT_TRANSITION_END_PROGRESS.get();
        dmtPostBreakthroughStartProgress = DMT_POST_BREAKTHROUGH_START_PROGRESS.get();
        dmtReturnStartProgress = DMT_RETURN_START_PROGRESS.get();
        dmtAfterglowStartProgress = DMT_AFTERGLOW_START_PROGRESS.get();
        dmtIntensityCurvePower = DMT_INTENSITY_CURVE_POWER.get();
        dmtLightDurationSeconds = DMT_LIGHT_DURATION_SECONDS.get();
        dmtStandardDurationSeconds = DMT_STANDARD_DURATION_SECONDS.get();
        dmtBreakthroughDurationSeconds = DMT_BREAKTHROUGH_DURATION_SECONDS.get();
        dmtStackedDurationSeconds = DMT_STACKED_DURATION_SECONDS.get();
        dmtWorldWarp = DMT_WORLD_WARP.get();
        dmtParticles = DMT_PARTICLES.get();
        dmtParticleMultiplier = DMT_PARTICLE_MULTIPLIER.get();
        dmtEncounterFrequency = DMT_ENCOUNTER_FREQUENCY.get();
        dmtArchitectureFrequency = DMT_ARCHITECTURE_FREQUENCY.get();
        dmtSymbolDensity = DMT_SYMBOL_DENSITY.get();
        dmtEntityDensity = DMT_ENTITY_DENSITY.get();
        dmtEnvironmentalConsciousness = DMT_ENVIRONMENTAL_CONSCIOUSNESS.get();
        dmtSpatialReplacementWeight = DMT_SPATIAL_REPLACEMENT_WEIGHT.get();
        dmtBreakthroughDominanceWeight = DMT_BREAKTHROUGH_DOMINANCE_WEIGHT.get();
        dmtAccessibilityMode = DMT_ACCESSIBILITY_MODE.get();
        enableSalviaEffects = ENABLE_SALVIA_EFFECTS.get();
        salviaMaxVisualIntensity = SALVIA_MAX_VISUAL_INTENSITY.get();
        salviaAccessibilityMode = SALVIA_ACCESSIBILITY_MODE.get();
        enableAlcoholEffects = ENABLE_ALCOHOL_EFFECTS.get();
        alcoholMaxVisualIntensity = ALCOHOL_MAX_VISUAL_INTENSITY.get();
        alcoholWorldWarp = ALCOHOL_WORLD_WARP.get();
        alcoholMovementDifficulty = ALCOHOL_MOVEMENT_DIFFICULTY.get();
        alcoholAccessibilityMode = ALCOHOL_ACCESSIBILITY_MODE.get();
    }
}
