package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.mdma.MdmaEffectState;
import net.minecraft.client.Minecraft;

/** Saturation, soft highlight bloom, and pink-cloud grading without spatial distortion. */
public final class MdmaWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static float easedIntensity;
    private static float easedSaturation;
    private static float easedBloom;
    private static float easedWarmth;
    private static float easedBass;
    private static float easedDose;
    private static float easedPeak;
    private static float easedComedown;
    private static float easedDreaminess;

    private MdmaWorldWarpPass() {
    }

    public static void render(float partialTick) {
        MdmaEffectState state = ClientExperienceStore.mdma();
        float time = worldTime();
        EasedWarp eased = update(state, time, partialTick);
        if (eased.intensity() <= 0.0001F && eased.comedown() <= 0.0001F) {
            return;
        }
        float photosensitive = TrippyClientConfig.photosensitivityReduction ? 1.0F : 0.0F;
        float club = ClientTavernAmbience.mdmaSettingMultiplier() > 1.0D ? 1.0F : 0.0F;
        WorldWarpPostProcessor.process(
                "mdma_world_warp",
                false,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased.intensity(), eased.saturation(), eased.bloom(), time),
                WorldWarpPostProcessor.uniform("WarpB", eased.warmth(), eased.bass(), eased.dose(), eased.peak()),
                WorldWarpPostProcessor.uniform("WarpC", eased.comedown(), photosensitive, eased.dreaminess(), club)
        );
    }

    private static EasedWarp update(MdmaEffectState state, float time, float partialTick) {
        float visualScale = (float) clamp(TrippyClientConfig.visualIntensity / 1.10D, 0.0D, 1.55D);
        float targetIntensity = state.active()
                ? (float) clamp((Math.log1p(Math.max(0.0D, state.intensity())) / Math.log(3.15D)) * visualScale, 0.0D, 1.35D)
                : 0.0F;
        float targetSaturation = state.active() ? (float) clamp(state.saturation() / 1.10D * visualScale, 0.0D, 1.55D) : 0.0F;
        float targetBloom = state.active() ? (float) clamp(state.bloom() / 1.05D * visualScale, 0.0D, 1.55D) : 0.0F;
        float targetWarmth = state.active() ? (float) clamp(state.warmth() / 0.95D * visualScale, 0.0D, 1.45D) : 0.0F;
        float targetBass = state.active()
                ? (float) clamp(ClientTavernAmbience.bassPulse(partialTick) * state.bassSensitivity() / 1.25D * visualScale, 0.0D, 1.45D)
                : 0.0F;
        float targetDose = state.active() ? (float) clamp(state.activeDoseMilligrams() / 500.0D, 0.0D, 1.5D) : 0.0F;
        float targetPeak = state.active() && state.peakWindow() ? 1.0F : 0.0F;
        float targetComedown = state.active() ? (float) clamp(state.comedownDesaturation() / 0.72D, 0.0D, 1.25D) : 0.0F;
        float targetDreaminess = state.active() ? (float) clamp(state.dreaminess(), 0.0D, 1.2D) : 0.0F;

        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            easedIntensity = targetIntensity;
            easedSaturation = targetSaturation;
            easedBloom = targetBloom;
            easedWarmth = targetWarmth;
            easedBass = targetBass;
            easedDose = targetDose;
            easedPeak = targetPeak;
            easedComedown = targetComedown;
            easedDreaminess = targetDreaminess;
        } else {
            easedIntensity = ease(easedIntensity, targetIntensity, delta, targetIntensity >= easedIntensity ? 8.0F : 20.0F);
            easedSaturation = ease(easedSaturation, targetSaturation, delta, 9.0F);
            easedBloom = ease(easedBloom, targetBloom, delta, 10.0F);
            easedWarmth = ease(easedWarmth, targetWarmth, delta, 14.0F);
            easedBass = ease(easedBass, targetBass, delta, targetBass >= easedBass ? 1.2F : 3.8F);
            easedDose = ease(easedDose, targetDose, delta, 20.0F);
            easedPeak = ease(easedPeak, targetPeak, delta, 12.0F);
            easedComedown = ease(easedComedown, targetComedown, delta, 16.0F);
            easedDreaminess = ease(easedDreaminess, targetDreaminess, delta, 16.0F);
        }
        lastEaseTime = time;
        return new EasedWarp(easedIntensity, easedSaturation, easedBloom, easedWarmth, easedBass,
                easedDose, easedPeak, easedComedown, easedDreaminess);
    }

    private static float ease(float current, float target, float delta, float timeConstant) {
        if (delta <= 0.0F) {
            return current;
        }
        float alpha = 1.0F - (float) Math.exp(-delta / Math.max(0.2F, timeConstant));
        return current + (target - current) * Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float worldTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }

    private record EasedWarp(float intensity, float saturation, float bloom, float warmth, float bass,
                             float dose, float peak, float comedown, float dreaminess) {
    }
}
