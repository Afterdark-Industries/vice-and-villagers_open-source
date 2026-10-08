package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.nitrous.NitrousEffectState;
import net.minecraft.client.Minecraft;

/** Short rush: tunnel, fisheye, haze, bloom, chroma, echo. */
public final class NitrousWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static final float[] eased = new float[9];

    private NitrousWorldWarpPass() {
    }

    public static void render(float partialTick) {
        NitrousEffectState state = ClientExperienceStore.nitrous();
        float time = worldTime();
        float[] warp = update(state, time);
        if (warp[0] <= 0.0001F || !TrippyClientConfig.rendererEnabled) {
            return;
        }
        float photosensitive = TrippyClientConfig.photosensitivityReduction ? 1.0F : 0.0F;
        float music = (float) Math.min(1.0D, ClientTavernAmbience.twoCbSettingMultiplier() > 1.0D
                ? 0.55D + ClientTavernAmbience.bassPulse(partialTick) * 0.45D
                : 0.22D);
        WorldWarpPostProcessor.process(
                "nitrous_world_warp",
                false,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", warp[0], warp[1], warp[2], time),
                WorldWarpPostProcessor.uniform("WarpB", warp[3], warp[4], warp[5], warp[6]),
                WorldWarpPostProcessor.uniform("WarpC", warp[7], warp[8], music, photosensitive)
        );
    }

    private static float[] update(NitrousEffectState state, float time) {
        float visual = (float) Math.max(0.0D, TrippyClientConfig.visualIntensity);
        float[] target = new float[9];
        if (state.active()) {
            target[0] = (float) (state.visualPresence() * visual);
            target[1] = (float) (state.tunnel() * visual);
            target[2] = (float) (state.fisheye() * visual);
            target[3] = (float) (state.haze() * visual);
            target[4] = (float) (state.bloom() * visual);
            target[5] = (float) (state.chroma() * visual);
            target[6] = (float) (state.echo() * visual);
            target[7] = (float) (state.stretch() * visual);
            target[8] = (float) (state.dissociation() * visual);
        }
        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            System.arraycopy(target, 0, eased, 0, eased.length);
        } else {
            for (int i = 0; i < eased.length; i++) {
                float tau = target[i] >= eased[i] ? 6.0F : 11.0F;
                eased[i] += (target[i] - eased[i]) * (1.0F - (float) Math.exp(-delta / tau));
            }
        }
        lastEaseTime = time;
        return eased;
    }

    private static float worldTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }

    static void reset() {
        java.util.Arrays.fill(eased, 0.0F);
        lastEaseTime = Float.NaN;
    }
}
