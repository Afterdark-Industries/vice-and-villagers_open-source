package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.mescaline.MescalineEffectState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

/** Slow, depth-aware living geometry with bounded scene distortion and unbounded complexity input. */
public final class MescalineWorldWarpPass {
    private static float lastEaseTime = Float.NaN;
    private static final float[] eased = new float[12];

    private MescalineWorldWarpPass() {
    }

    public static void render(float partialTick) {
        MescalineEffectState state = ClientExperienceStore.mescaline();
        float time = worldTime();
        float[] warp = update(state, time);
        if (warp[0] <= 0.0001F || !TrippyClientConfig.rendererEnabled) return;

        var music = ClientTavernAmbience.musicStructure(partialTick);
        var mdma = ClientExperienceStore.mdma();
        var twoCb = ClientExperienceStore.twoCb();
        var lsd = ClientExperienceStore.lsd();
        var psilocybin = ClientExperienceStore.psilocybin();
        var cannabis = ClientExperienceStore.cannabis();
        var alcohol = ClientExperienceStore.alcohol();
        var ketamine = ClientExperienceStore.ketamine();

        float mdmaLoad = compress(mdma.active() ? mdma.warmth() * 0.36D + mdma.bloom() * 0.34D + mdma.audioFullness() * 0.30D : 0.0D);
        float twoCbLoad = compress(twoCb.active() ? twoCb.surfaceGeometry() * 0.46D + twoCb.crystalline() * 0.36D + twoCb.edgeGlow() * 0.18D : 0.0D);
        float lsdLoad = compress(lsd.active() ? lsd.realityFold() * 0.42D + lsd.edgeHum() * 0.34D + lsd.prismSplit() * 0.24D : 0.0D);
        float psilocybinLoad = compress(psilocybin.active() ? psilocybin.organicBreathing() * 0.54D + psilocybin.surfaceMelting() * 0.28D + psilocybin.colorWarmth() * 0.18D : 0.0D);
        float cannabisLoad = compress(cannabis.active() ? cannabis.temporalDrag() * 0.48D + cannabis.immersion() * 0.32D + cannabis.breathingPulse() * 0.20D : 0.0D);
        float stimulantLoad = compress(ClientExperienceStore.state().sample(ExperienceChannel.STIMULATION, partialTick) * 1.4D);
        float alcoholLoad = compress(alcohol.active() ? alcohol.focusBlur() * 0.44D + alcohol.vestibularSway() * 0.36D + alcohol.nausea() * 0.20D : 0.0D);
        float ketamineLoad = compress(ketamine.active() ? ketamine.depthMismatch() * 0.54D + ketamine.dissociation() * 0.46D : 0.0D);
        float photosensitive = TrippyClientConfig.photosensitivityReduction ? 1.0F : 0.0F;
        float motionReduction = TrippyClientConfig.motionReduction ? 1.0F : 0.0F;
        float seed = (state.signature().seed() & 0xFFFFL) / 65535.0F;
        float rawDose = finite(state.activeDoseMilligrams());
        float normalizedDose = rawDose / 500.0F;
        float strongDose = smoothstep(100.0F, 500.0F, rawDose);
        float beyondDose = finite(Math.log1p(Math.max(0.0D, state.activeDoseMilligrams() - 500.0D) / 500.0D));
        CameraData cameraData = cameraData();

        WorldWarpPostProcessor.process(
                "mescaline_world_warp", true, partialTick,
                WorldWarpPostProcessor.uniform("WarpA", warp[0], warp[1], warp[2], time),
                WorldWarpPostProcessor.uniform("WarpB", warp[3], warp[4], warp[5], warp[6]),
                WorldWarpPostProcessor.uniform("WarpC", warp[7], warp[8], warp[9], warp[10]),
                WorldWarpPostProcessor.uniform("MusicA", (float) music.bassExpansion(), (float) music.harmonicShift(),
                        (float) music.melodicFlow(), (float) music.sustainedRibbon()),
                WorldWarpPostProcessor.uniform("MusicB", (float) music.saxRibbon(), music.tavern() ? 1.0F : 0.0F,
                        (float) music.phrasePhase(), seed),
                WorldWarpPostProcessor.uniform("WarpD", warp[11], seed,
                        finite(state.environment().forest() * 0.72D + state.environment().organicBlocks() * 0.46D),
                        finite(state.environment().sunriseSunset() * 0.74D + state.environment().village() * 0.42D + state.environment().light() * 0.18D)),
                WorldWarpPostProcessor.uniform("MixA", mdmaLoad, twoCbLoad, lsdLoad, psilocybinLoad),
                WorldWarpPostProcessor.uniform("MixB", cannabisLoad, stimulantLoad, alcoholLoad, ketamineLoad),
                WorldWarpPostProcessor.uniform("DoseA", normalizedDose, strongDose, beyondDose, (float) state.progress()),
                WorldWarpPostProcessor.uniform("Control", photosensitive, motionReduction,
                        (float) TrippyClientConfig.visualIntensity, (float) TrippyClientConfig.motionIntensity),
                WorldWarpPostProcessor.uniform("CameraA", cameraData.x(), cameraData.y(), cameraData.z(), cameraData.farPlane()),
                WorldWarpPostProcessor.uniform("CameraB", cameraData.forwardX(), cameraData.forwardY(), cameraData.forwardZ(), cameraData.nearPlane()),
                WorldWarpPostProcessor.uniform("CameraC", cameraData.rightX(), cameraData.rightY(), cameraData.rightZ(), 0.0F),
                WorldWarpPostProcessor.uniform("CameraD", cameraData.upX(), cameraData.upY(), cameraData.upZ(), 0.0F)
        );
    }

    private static float[] update(MescalineEffectState state, float time) {
        float[] target = new float[12];
        if (state.active()) {
            double visual = Math.max(0.0D, TrippyClientConfig.visualIntensity);
            target[0] = finite(state.visualPresence() * visual);
            target[1] = finite(state.warmth() * visual);
            target[2] = finite(state.colorRichness() * visual);
            target[3] = finite(state.surfaceBreathing() * visual);
            target[4] = finite(state.organicFlow() * visual);
            target[5] = finite(state.tessellation() * visual);
            // Log encoding remains open-ended while keeping shader arithmetic stable at absurd accumulated doses.
            target[6] = finite(Math.log1p(Math.max(0.0D, state.recursiveComplexity())) * visual);
            target[7] = finite(state.symmetry() * visual);
            target[8] = finite(state.softTrails() * visual);
            target[9] = finite(state.spatialDepth() * visual);
            target[10] = finite(state.afterglow() * visual);
            target[11] = finite(state.musicSensitivity() * visual);
        }
        float delta = Float.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 80.0F || !state.active()) {
            System.arraycopy(target, 0, eased, 0, eased.length);
        } else {
            for (int i = 0; i < eased.length; i++) {
                float tau = target[i] > eased[i] ? (i == 6 ? 28.0F : 18.0F) : 34.0F;
                float alpha = 1.0F - (float) Math.exp(-delta / tau);
                eased[i] += (target[i] - eased[i]) * alpha;
            }
        }
        lastEaseTime = time;
        return eased.clone();
    }

    private static float compress(double value) {
        return !Double.isFinite(value) || value <= 0.0D ? 0.0F : (float) (1.0D - Math.exp(-value));
    }

    private static float finite(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) return 0.0F;
        return (float) Math.min(value, Float.MAX_VALUE);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }

    private static CameraData cameraData() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return CameraData.identity();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) return CameraData.identity();
        Vec3 position = camera.getPosition();
        Camera.NearPlane nearPlane = camera.getNearPlane();
        Vec3 forward = nearPlane.getPointOnPlane(0.0F, 0.0F);
        Vec3 right = nearPlane.getPointOnPlane(1.0F, 0.0F).subtract(forward);
        Vec3 up = nearPlane.getPointOnPlane(0.0F, 1.0F).subtract(forward);
        return new CameraData(
                wrappedCoordinate(position.x), wrappedCoordinate(position.y), wrappedCoordinate(position.z),
                GameRenderer.PROJECTION_Z_NEAR, Math.max(16.0F, minecraft.gameRenderer.getDepthFar()),
                (float) forward.x, (float) forward.y, (float) forward.z,
                (float) right.x, (float) right.y, (float) right.z,
                (float) up.x, (float) up.y, (float) up.z
        );
    }

    private static float wrappedCoordinate(double value) {
        double period = 4096.0D;
        return (float) (value - Math.floor(value / period) * period);
    }

    private static float worldTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0.0F
                : minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }

    private record CameraData(
            float x, float y, float z, float nearPlane, float farPlane,
            float forwardX, float forwardY, float forwardZ,
            float rightX, float rightY, float rightZ,
            float upX, float upY, float upZ
    ) {
        private static CameraData identity() {
            return new CameraData(0.0F, 0.0F, 0.0F, GameRenderer.PROJECTION_Z_NEAR, 128.0F,
                    0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        }
    }
}
