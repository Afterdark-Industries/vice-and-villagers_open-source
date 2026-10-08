package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.cocaine.CocainePhase;
import net.minecraft.client.Minecraft;

public final class CocaineWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static float easedIntensity;
    private static float easedHarshness;
    private static float easedFocus;
    private static float easedFlicker;
    private static float easedCrash;
    private static float easedClarity;
    private static float easedDose;
    private static float easedProgress;

    private CocaineWorldWarpPass() {
    }

    public static void render(float partialTick) {
        CocaineEffectState state = ClientExperienceStore.cocaine();
        float time = worldTime();
        EasedWarp eased = updateEasedWarp(state, time);
        if (eased.intensity() <= 0.0001F) {
            return;
        }

        writeWarpUniform(eased, time, partialTick);
    }

    private static void writeWarpUniform(EasedWarp eased, float time, float partialTick) {
        float photosensitive = TrippyClientConfig.photosensitivityReduction ? 1.0F : 0.0F;
        WorldWarpPostProcessor.process(
                "cocaine_world_warp",
                false,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased.intensity(), eased.harshness(), eased.focus(), time),
                WorldWarpPostProcessor.uniform("WarpB", eased.flicker(), eased.crash(), eased.clarity(), eased.dose()),
                WorldWarpPostProcessor.uniform("WarpC", eased.progress(), photosensitive, 0.0F, 0.0F)
        );
    }

    private static EasedWarp updateEasedWarp(CocaineEffectState state, float time) {
        float targetIntensity = shaderIntensity(state);
        float targetHarshness = normalized(state.active() ? state.lightHarshness() : 0.0D, 3.2F);
        float targetFocus = normalized(state.active() ? state.tunnelFocus() + state.edgeShimmer() * 0.55D : 0.0D, 2.6F);
        float targetFlicker = normalized(state.active() ? state.microFlicker() + state.jitter() * 0.35D : 0.0D, 2.0F);
        float targetCrash = normalized(state.active() ? state.crashDesaturation() + state.crashDrag() * 0.70D : 0.0D, 1.4F);
        float targetClarity = normalized(state.active() ? state.clarityLift() : 0.0D, 2.0F);
        float targetDose = state.active() ? (float) Math.max(0.0D, Math.min(1.0D, state.activeDoseGrams() / 8.0D)) : 0.0F;
        float targetProgress = state.active() ? (float) Math.max(0.0D, Math.min(1.0D, state.progress())) : 1.0F;

        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            easedIntensity = targetIntensity;
            easedHarshness = targetHarshness;
            easedFocus = targetFocus;
            easedFlicker = targetFlicker;
            easedCrash = targetCrash;
            easedClarity = targetClarity;
            easedDose = targetDose;
            easedProgress = targetProgress;
        } else {
            easedIntensity = ease(easedIntensity, targetIntensity, delta, targetIntensity >= easedIntensity ? 12.0F : 24.0F);
            easedHarshness = ease(easedHarshness, targetHarshness, delta, 16.0F);
            easedFocus = ease(easedFocus, targetFocus, delta, 18.0F);
            easedFlicker = ease(easedFlicker, targetFlicker, delta, 12.0F);
            easedCrash = ease(easedCrash, targetCrash, delta, 22.0F);
            easedClarity = ease(easedClarity, targetClarity, delta, 18.0F);
            easedDose = ease(easedDose, targetDose, delta, 28.0F);
            easedProgress = ease(easedProgress, targetProgress, delta, 22.0F);
        }
        lastEaseTime = time;
        return new EasedWarp(easedIntensity, easedHarshness, easedFocus, easedFlicker, easedCrash, easedClarity, easedDose, easedProgress);
    }

    private static float shaderIntensity(CocaineEffectState state) {
        if (!state.active() || !TrippyClientConfig.rendererEnabled) {
            return 0.0F;
        }
        double load = Math.min(1.0D, Math.log1p(Math.max(0.0D, state.intensity())) / Math.log1p(6.0D));
        double harshness = Math.min(1.0D, state.lightHarshness() / 2.8D);
        double clarity = Math.min(1.0D, state.clarityLift() / 1.8D);
        double flicker = Math.min(1.0D, (state.microFlicker() + state.jitter() * 0.30D) / 1.8D);
        double focus = Math.min(1.0D, (state.tunnelFocus() + state.edgeShimmer() * 0.55D) / 2.3D);
        double crash = Math.min(1.0D, (state.crashDesaturation() + state.crashDrag() * 0.55D) / 1.2D);
        double phaseBias = state.phase() == CocainePhase.CRASH ? 0.42D : state.phase() == CocainePhase.AFTERGLOW ? 0.20D : 1.0D;
        double intensity = (load * 0.16D + harshness * 0.40D + clarity * 0.22D + focus * 0.18D + flicker * 0.10D) * phaseBias;
        if (TrippyClientConfig.photosensitivityReduction) {
            intensity *= 0.55D;
        }
        return (float) Math.min(1.10D, intensity);
    }

    private static float normalized(double value, float safe) {
        if (!Double.isFinite(value) || safe <= 0.0F) {
            return 0.0F;
        }
        return (float) Math.max(0.0D, Math.min(1.0D, value / safe));
    }

    private static float ease(float current, float target, float delta, float timeConstant) {
        if (delta <= 0.0F) {
            return current;
        }
        float alpha = 1.0F - (float) Math.exp(-delta / Math.max(1.0F, timeConstant));
        return current + (target - current) * Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static float worldTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }

    private record EasedWarp(
            float intensity,
            float harshness,
            float focus,
            float flicker,
            float crash,
            float clarity,
            float dose,
            float progress
    ) {
    }
}
