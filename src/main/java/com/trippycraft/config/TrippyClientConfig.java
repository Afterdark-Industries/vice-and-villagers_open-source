package com.trippycraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class TrippyClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue RENDERER_ENABLED = BUILDER
            .comment("Enables Vice and Villagers client visuals.")
            .define("rendererEnabled", true);

    private static final ModConfigSpec.BooleanValue DMT_FRACTAL_WORLD = BUILDER
            .comment("Experimental 3D DMT fractal realm. Off by default; enabling may affect performance. Off uses the original DMT visuals.")
            .translation("trippycraft.configuration.dmtFractalWorld")
            .define("dmtFractalWorld", false);

    private static final ModConfigSpec.IntValue DMT_FRACTAL_QUALITY = BUILDER
            .comment("3D DMT fractal quality: 0 = low (72 primary steps), 1 = balanced (112 primary steps), 2 = high (192 primary steps), with bounded surface recovery for unresolved rays. Dose-driven complexity continues beyond the legacy intensity ceiling; this setting controls the rendering budget.")
            .translation("trippycraft.configuration.dmtFractalQuality")
            .defineInRange("dmtFractalQuality", 1, 0, 2);

    private static final ModConfigSpec.DoubleValue DMT_FRACTAL_INTENSITY = BUILDER
            .comment("Player multiplier for the optional DMT realm. Default 0.75 (75%): recommended for best experience. Try 0.50 (50%) for mid-range PCs, or 0.25 (25%) for low-end PCs. 0.0 = hidden, 1.0 = full dose response. Dose complexity has no visual dose ceiling. Use quality to control rendering cost.")
            .translation("trippycraft.configuration.dmtFractalIntensity")
            .defineInRange("dmtFractalIntensity", 0.75D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue VISUAL_INTENSITY = BUILDER
            .comment("Global multiplier for screen-space and color visuals.")
            .defineInRange("visualIntensity", 1.0D, 0.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue MOTION_INTENSITY = BUILDER
            .comment("Global multiplier for camera, FOV, fog, and motion-like visuals.")
            .defineInRange("motionIntensity", 1.0D, 0.0D, 2.0D);

    private static final ModConfigSpec.BooleanValue AUDIO_ENABLED = BUILDER
            .comment("Enables subtle Vice and Villagers sound pitch and volume shaping while an experience is active.")
            .define("audioEnabled", true);

    private static final ModConfigSpec.DoubleValue AUDIO_INTENSITY = BUILDER
            .comment("Global multiplier for sound pitch and volume shaping.")
            .defineInRange("audioIntensity", 0.65D, 0.0D, 2.0D);

    private static final ModConfigSpec.BooleanValue MOTION_REDUCTION = BUILDER
            .comment("Caps camera roll, FOV breathing, drift, and trail strength for motion-sensitive players.")
            .define("motionReduction", false);

    private static final ModConfigSpec.BooleanValue PHOTOSENSITIVITY_REDUCTION = BUILDER
            .comment("Caps high-contrast patterning, rapid color pulses, and bright overlays.")
            .define("photosensitivityReduction", false);

    private static final ModConfigSpec.BooleanValue DEBUG_CHANNELS = BUILDER
            .comment("Adds Vice and Villagers client channel lines to the F3 debug overlay while an experience is active.")
            .define("debugChannels", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean rendererEnabled = true;
    public static boolean dmtFractalWorld = false;
    public static int dmtFractalQuality = 1;
    public static double dmtFractalIntensity = 0.75D;
    public static double visualIntensity = 1.0D;
    public static double motionIntensity = 1.0D;
    public static boolean audioEnabled = true;
    public static double audioIntensity = 0.65D;
    public static boolean motionReduction = false;
    public static boolean photosensitivityReduction = false;
    public static boolean debugChannels = true;

    private TrippyClientConfig() {
    }

    public static void setDmtFractalWorld(boolean enabled) {
        DMT_FRACTAL_WORLD.set(enabled);
        dmtFractalWorld = enabled;
        SPEC.save();
    }

    public static void setDmtFractalIntensity(double intensity) {
        double normalized = normalizedFractalIntensity(intensity);
        DMT_FRACTAL_INTENSITY.set(normalized);
        dmtFractalIntensity = normalized;
        SPEC.save();
    }

    public static double cycleDmtFractalIntensity() {
        double current = normalizedFractalIntensity(dmtFractalIntensity);
        double next = (Math.floor(current * 4.0D) + 1.0D) * 0.25D;
        setDmtFractalIntensity(next > 1.0D ? 0.25D : next);
        return dmtFractalIntensity;
    }

    public static void setDmtFractalQuality(int quality) {
        int normalized = Math.max(0, Math.min(2, quality));
        DMT_FRACTAL_QUALITY.set(normalized);
        dmtFractalQuality = normalized;
        SPEC.save();
    }

    public static int cycleDmtFractalQuality() {
        int current = Math.max(0, Math.min(2, dmtFractalQuality));
        setDmtFractalQuality((current + 1) % 3);
        return dmtFractalQuality;
    }

    private static double normalizedFractalIntensity(double intensity) {
        return Double.isFinite(intensity) ? Math.max(0.0D, Math.min(1.0D, intensity)) : 0.75D;
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        rendererEnabled = RENDERER_ENABLED.get();
        dmtFractalWorld = DMT_FRACTAL_WORLD.get();
        dmtFractalQuality = DMT_FRACTAL_QUALITY.get();
        dmtFractalIntensity = normalizedFractalIntensity(DMT_FRACTAL_INTENSITY.get());
        visualIntensity = VISUAL_INTENSITY.get();
        motionIntensity = MOTION_INTENSITY.get();
        audioEnabled = AUDIO_ENABLED.get();
        audioIntensity = AUDIO_INTENSITY.get();
        motionReduction = MOTION_REDUCTION.get();
        photosensitivityReduction = PHOTOSENSITIVITY_REDUCTION.get();
        debugChannels = DEBUG_CHANNELS.get();
    }
}
