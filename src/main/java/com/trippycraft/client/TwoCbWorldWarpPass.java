package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import net.minecraft.client.Minecraft;

/** Crisp, high-contrast 2C-B visuals: neon edges, beat tracers, and high-dose reality discontinuities. */
public final class TwoCbWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static final float[] eased = new float[10];

    private TwoCbWorldWarpPass() {
    }

    public static void render(float partialTick) {
        TwoCbEffectState state = ClientExperienceStore.twoCb();
        float time = worldTime();
        float[] warp = update(state, time, partialTick);
        if (warp[0] <= 0.0001F || !TrippyConfig.twoCbWorldWarp) {
            return;
        }
        float photosensitive = TrippyClientConfig.photosensitivityReduction || TrippyConfig.twoCbAccessibilityMode ? 1.0F : 0.0F;
        var cocaine = ClientExperienceStore.cocaine();
        double cocaineLoad = cocaine.active()
                ? cocaine.stimulation() * 0.35D + cocaine.inputSharpness() * 0.35D + cocaine.edgeShimmer() * 0.30D
                : 0.0D;
        float cocaineSynergy = (float) clamp(Math.log1p(Math.max(0.0D, cocaineLoad)) / Math.log(4.5D), 0.0D, 1.35D);
        float club = ClientTavernAmbience.twoCbSettingMultiplier() > 1.0D ? 1.0F : 0.0F;
        WorldWarpPostProcessor.process(
                "two_cb_world_warp",
                false,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", warp[0], warp[1], warp[2], time),
                WorldWarpPostProcessor.uniform("WarpB", warp[3], warp[4], warp[5], warp[6]),
                WorldWarpPostProcessor.uniform("WarpC", warp[7], warp[8], photosensitive, warp[9]),
                WorldWarpPostProcessor.uniform("WarpD", cocaineSynergy, club, (float) state.progress(), state.tier() / 9.0F)
        );
    }

    private static float[] update(TwoCbEffectState state, float time, float partialTick) {
        float visualScale = (float) clamp(TrippyClientConfig.visualIntensity / 1.10D, 0.0D, 1.55D);
        float accessScale = TrippyConfig.twoCbAccessibilityMode ? 0.58F : 1.0F;
        float safeMax = (float) Math.max(1.0D, TrippyConfig.twoCbMaxVisualIntensity);
        boolean club = ClientTavernAmbience.twoCbSettingMultiplier() > 1.0D;
        double fallbackBeat = Math.pow(Math.max(0.0D, Math.sin(time * 0.155D)), 10.0D) * 0.52D;
        double beat = club ? ClientTavernAmbience.bassPulse(partialTick) : fallbackBeat;
        float[] target = new float[10];
        if (state.active()) {
            target[0] = (float) clamp(Math.log1p(state.intensity()) / Math.log1p(safeMax) * visualScale, 0.0D, 3.2D);
            target[1] = (float) clamp(state.saturation() / 2.2D * visualScale, 0.0D, 4.0D);
            target[2] = (float) clamp(state.edgeGlow() / 2.2D * visualScale * accessScale, 0.0D, 4.0D);
            target[3] = (float) clamp(state.surfaceGeometry() / 2.5D * visualScale * accessScale, 0.0D, 4.0D);
            target[4] = (float) clamp(state.tracerStrength() / 2.2D * visualScale * accessScale, 0.0D, 4.0D);
            target[5] = (float) clamp(beat * state.bassSensitivity() / 1.8D * visualScale * accessScale, 0.0D, 3.0D);
            target[6] = (float) clamp(state.fisheyePulse() * 2.8D * visualScale * accessScale, 0.0D, 1.2D);
            target[7] = (float) clamp(state.symmetry() / 2.0D * visualScale * accessScale, 0.0D, 4.0D);
            target[8] = (float) clamp(state.crystalline() / 2.2D * visualScale, 0.0D, 4.0D);
            target[9] = (float) clamp((state.activeDoseMilligrams() - 500.0D) / 833.333D, 0.0D, 3.0D);
        }

        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            System.arraycopy(target, 0, eased, 0, eased.length);
        } else {
            for (int i = 0; i < eased.length; i++) {
                float speed = i == 5 ? (target[i] >= eased[i] ? 0.8F : 2.6F) : (i == 7 ? 5.0F : 10.0F);
                eased[i] = ease(eased[i], target[i], delta, speed);
            }
        }
        lastEaseTime = time;
        return new float[]{eased[0], eased[1], eased[2], eased[3], eased[4], eased[5], eased[6], eased[7], eased[8], eased[9]};
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
}
