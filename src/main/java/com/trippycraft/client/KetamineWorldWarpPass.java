package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetamineMasking;
import net.minecraft.client.Minecraft;

public final class KetamineWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static float easedIntensity;
    private static float easedLag;
    private static float easedDepth;
    private static float easedDrift;
    private static float easedTunnel;
    private static float easedEcho;
    private static float easedDose;
    private static float easedProgress;
    private static float easedCollapse;

    private KetamineWorldWarpPass() {
    }

    public static void render(float partialTick) {
        KetamineEffectState state = ClientExperienceStore.ketamine();
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
                "ketamine_world_warp",
                false,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased.intensity(), eased.lag(), eased.depth(), time),
                WorldWarpPostProcessor.uniform("WarpB", eased.drift(), eased.tunnel(), eased.echo(), eased.dose()),
                WorldWarpPostProcessor.uniform("WarpC", eased.progress(), eased.collapse(), photosensitive, 0.0F)
        );
    }

    private static EasedWarp updateEasedWarp(KetamineEffectState state, float time) {
        float targetIntensity = shaderIntensity(state);
        float targetLag = normalized(state.active() ? state.cameraLag() : 0.0D, 2.4F);
        float targetDepth = normalized(state.active() ? state.depthMismatch() + state.collapseDepth() * 0.30D : 0.0D, 2.4F);
        float targetDrift = normalized(state.active() ? state.gravityDrift() + state.bodyFloat() * 0.44D + state.timeSlip() * 0.16D : 0.0D, 2.6F);
        float targetTunnel = normalized(state.active() ? state.uiFade() + state.audioMuffle() * 0.45D + state.depthMismatch() * 0.20D : 0.0D, 2.2F);
        float targetEcho = normalized(state.active() ? state.bodyEcho() + state.bodyFloat() * 0.34D : 0.0D, 2.4F);
        float targetDose = state.active() ? (float) Math.max(0.0D, Math.min(1.0D, state.activeDoseMilligrams() / 500.0D)) : 0.0F;
        float targetProgress = state.active() ? (float) Math.max(0.0D, Math.min(1.0D, state.progress())) : 1.0F;
        float targetCollapse = normalized(state.active() ? state.collapseRisk() + state.collapseDepth() * 0.65D : 0.0D, 2.0F);

        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            easedIntensity = targetIntensity;
            easedLag = targetLag;
            easedDepth = targetDepth;
            easedDrift = targetDrift;
            easedTunnel = targetTunnel;
            easedEcho = targetEcho;
            easedDose = targetDose;
            easedProgress = targetProgress;
            easedCollapse = targetCollapse;
        } else {
            easedIntensity = ease(easedIntensity, targetIntensity, delta, targetIntensity >= easedIntensity ? 18.0F : 30.0F);
            easedLag = ease(easedLag, targetLag, delta, 26.0F);
            easedDepth = ease(easedDepth, targetDepth, delta, 24.0F);
            easedDrift = ease(easedDrift, targetDrift, delta, 30.0F);
            easedTunnel = ease(easedTunnel, targetTunnel, delta, 24.0F);
            easedEcho = ease(easedEcho, targetEcho, delta, 26.0F);
            easedDose = ease(easedDose, targetDose, delta, 36.0F);
            easedProgress = ease(easedProgress, targetProgress, delta, 28.0F);
            easedCollapse = ease(easedCollapse, targetCollapse, delta, 22.0F);
        }
        lastEaseTime = time;
        return new EasedWarp(easedIntensity, easedLag, easedDepth, easedDrift, easedTunnel, easedEcho, easedDose, easedProgress, easedCollapse);
    }

    private static float shaderIntensity(KetamineEffectState state) {
        if (!state.active() || !TrippyClientConfig.rendererEnabled) {
            return 0.0F;
        }
        double load = ketamineVisualLoad(state);
        double presence = KetamineMasking.visualPresence(state);
        double realityBend = KetamineMasking.realityBend(state);
        double lag = Math.min(1.0D, state.cameraLag() / 2.4D);
        double depth = Math.min(1.0D, state.depthMismatch() / 2.6D);
        double tunnel = Math.min(1.0D, (state.uiFade() + state.audioMuffle() * 0.35D) / 2.4D);
        double echo = Math.min(1.0D, (state.bodyEcho() + state.bodyFloat() * 0.30D) / 2.4D);
        double collapse = Math.min(1.0D, (state.collapseRisk() + state.collapseDepth() * 0.65D) / 2.0D);
        double intensity = load * (0.18D + presence * 0.18D + realityBend * 0.08D)
            + lag * (0.16D + presence * 0.04D)
            + depth * (0.36D + presence * 0.10D + realityBend * 0.16D)
            + tunnel * (0.22D + presence * 0.10D)
            + echo * (0.16D + presence * 0.08D)
                + collapse * 0.12D;
        if (TrippyClientConfig.photosensitivityReduction) {
            intensity *= 0.62D;
        }
        return (float) Math.min(1.05D, intensity);
    }

    private static double ketamineVisualLoad(KetamineEffectState state) {
        double presence = KetamineMasking.visualPresence(state);
        double realityBend = KetamineMasking.realityBend(state);
        double load = Math.log1p(Math.max(0.0D, state.intensity() * (1.08D + presence * 1.04D + realityBend * 0.40D))) / Math.log1p(6.4D);
        load += presence * 0.40D + realityBend * 0.32D;
        load += Math.max(0.0D, Math.min(1.0D, (state.depthMismatch() + state.bodyEcho() * 0.60D) / 8.0D)) * (0.10D + presence * 0.18D + realityBend * 0.06D);
        return Math.max(0.0D, Math.min(1.0D, load));
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
            float lag,
            float depth,
            float drift,
            float tunnel,
            float echo,
            float dose,
            float progress,
            float collapse
    ) {
    }
}
