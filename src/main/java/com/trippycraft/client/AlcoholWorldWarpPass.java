package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import net.minecraft.client.Minecraft;

public final class AlcoholWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static float easedIntensity;
    private static float easedDiplopia;
    private static float easedBlur;
    private static float easedSway;
    private static float easedFocusLag;
    private static float easedNausea;
    private static float easedSedation;
    private static float easedMotor;
    private static float easedPeripheral;
    private static float easedRisk;
    private static float easedDose;

    private AlcoholWorldWarpPass() {
    }

    public static void render(float partialTick) {
        AlcoholEffectState state = ClientExperienceStore.alcohol();
        float time = worldTime();
        EasedWarp eased = updateEasedWarp(state, time);
        if (eased.intensity() <= 0.0001F) {
            return;
        }

        writeWarpUniform(state, eased, time, partialTick);
    }

    private static void writeWarpUniform(AlcoholEffectState state, EasedWarp eased, float time, float partialTick) {
        float progress = state.active() ? (float) state.progress() : 1.0F;
        float tier = state.active() ? state.tier() : 0.0F;
        float accessibility = TrippyConfig.alcoholAccessibilityMode || TrippyClientConfig.motionReduction ? 1.0F : 0.0F;
        float photosensitive = TrippyClientConfig.photosensitivityReduction ? 1.0F : 0.0F;
        WorldWarpPostProcessor.process(
                "alcohol_world_warp",
                true,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased.intensity(), eased.diplopia(), eased.blur(), time),
                WorldWarpPostProcessor.uniform("WarpB", eased.sway(), eased.focusLag(), eased.nausea(), eased.sedation()),
                WorldWarpPostProcessor.uniform("WarpC", eased.motor(), eased.peripheral(), progress, eased.dose()),
                WorldWarpPostProcessor.uniform("WarpD", eased.risk(), tier, accessibility, photosensitive)
        );
    }

    private static EasedWarp updateEasedWarp(AlcoholEffectState state, float time) {
        float safe = (float) Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
        float targetDiplopia = normalized(state.active() ? state.diplopia() : 0.0D, safe);
        float targetBlur = normalized(state.active() ? state.focusBlur() : 0.0D, safe);
        float targetSway = normalized(state.active() ? state.vestibularSway() : 0.0D, safe);
        float targetFocusLag = normalized(state.active() ? state.focusBlur() * 0.62D + state.sedation() * 0.38D : 0.0D, safe);
        float targetNausea = normalized(state.active() ? state.nausea() : 0.0D, safe);
        float targetSedation = normalized(state.active() ? state.sedation() : 0.0D, safe);
        float targetMotor = normalized(state.active() ? state.motorNoise() : 0.0D, safe);
        float targetPeripheral = normalized(state.active() ? state.peripheralNarrowing() : 0.0D, safe);
        float targetRisk = normalized(state.active() ? state.risk() : 0.0D, safe);
        float targetDose = (float) Math.min(10.0D, state.active() ? state.activeStandardDrinks() : 0.0D);
        float targetIntensity = shaderIntensity(state, targetDiplopia, targetBlur, targetSway, targetPeripheral);

        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            easedIntensity = targetIntensity;
            easedDiplopia = targetDiplopia;
            easedBlur = targetBlur;
            easedSway = targetSway;
            easedFocusLag = targetFocusLag;
            easedNausea = targetNausea;
            easedSedation = targetSedation;
            easedMotor = targetMotor;
            easedPeripheral = targetPeripheral;
            easedRisk = targetRisk;
            easedDose = targetDose;
        } else {
            easedIntensity = ease(easedIntensity, targetIntensity, delta, targetIntensity >= easedIntensity ? 14.0F : 30.0F);
            easedDiplopia = ease(easedDiplopia, targetDiplopia, delta, 18.0F);
            easedBlur = ease(easedBlur, targetBlur, delta, 24.0F);
            easedSway = ease(easedSway, targetSway, delta, 20.0F);
            easedFocusLag = ease(easedFocusLag, targetFocusLag, delta, 28.0F);
            easedNausea = ease(easedNausea, targetNausea, delta, 32.0F);
            easedSedation = ease(easedSedation, targetSedation, delta, 34.0F);
            easedMotor = ease(easedMotor, targetMotor, delta, 18.0F);
            easedPeripheral = ease(easedPeripheral, targetPeripheral, delta, 36.0F);
            easedRisk = ease(easedRisk, targetRisk, delta, 24.0F);
            easedDose = ease(easedDose, targetDose, delta, 36.0F);
        }
        lastEaseTime = time;
        return new EasedWarp(easedIntensity, easedDiplopia, easedBlur, easedSway, easedFocusLag, easedNausea, easedSedation, easedMotor, easedPeripheral, easedRisk, easedDose);
    }

    private static float shaderIntensity(AlcoholEffectState state, float diplopia, float blur, float sway, float peripheral) {
        if (!state.active() || !TrippyConfig.enableAlcoholEffects || !TrippyConfig.alcoholWorldWarp || !TrippyClientConfig.rendererEnabled) {
            return 0.0F;
        }
        float safe = (float) Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
        float load = normalized(state.intensity(), safe);
        float intensity = Math.min(1.35F, load * 0.34F + diplopia * 0.58F + blur * 0.34F + sway * 0.26F + peripheral * 0.18F);
        if (TrippyConfig.alcoholAccessibilityMode || TrippyClientConfig.motionReduction) {
            intensity *= 0.42F;
        }
        if (TrippyClientConfig.photosensitivityReduction) {
            intensity *= 0.72F;
        }
        return intensity;
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
            float diplopia,
            float blur,
            float sway,
            float focusLag,
            float nausea,
            float sedation,
            float motor,
            float peripheral,
            float risk,
            float dose
    ) {
    }
}
