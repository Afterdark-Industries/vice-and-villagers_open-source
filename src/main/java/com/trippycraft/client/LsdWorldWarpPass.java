package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.lsd.LsdEffectState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

public final class LsdWorldWarpPass {
    private static Object presentationLevel;
    private static double lastEaseTime = Double.NaN;
    private static LsdEffectState lastPresentation = LsdEffectState.inactive();
    private static final float[] eased = new float[11];

    private LsdWorldWarpPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (presentationLevel != minecraft.level) {
            resetPresentation();
            presentationLevel = minecraft.level;
        }
        if (!TrippyClientConfig.rendererEnabled || !TrippyConfig.enableLsdEffects || !TrippyConfig.lsdWorldWarp) {
            resetPresentation();
            return;
        }
        LsdEffectState state = ClientExperienceStore.lsd();
        double ticks = LsdPresentationClock.timeTicks(partialTick);
        double elapsed = Double.isFinite(lastEaseTime) ? ticks - lastEaseTime : 0.0D;
        float delta = (float) Math.max(0.0D, Math.min(4.0D, elapsed));
        float shaderGain = (float) CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(), state,
                ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(),
                ClientExperienceStore.twoCe()).lsdShaderGain();
        float[] target = new float[]{shaderIntensity(state) * shaderGain,
                channel(state.edgeHum()), channel(state.geometryMelt()), channel(state.realityFold()),
                channel(state.blockAutonomy()), channel(state.liquidFlow()), channel(state.skyVortex()),
                channel(state.entityTrails()), channel(state.prismSplit()), channel(state.electricBloom()),
                state.active() ? (float) Math.min(Float.MAX_VALUE, Math.max(0.0D, state.activeDoseMicrograms())) : 0.0F};
        for (int i = 0; i < eased.length; i++) {
            // A time-based response also holds for low frame rates and both VR eyes.
            float tau = target[i] >= eased[i] ? 34.0F : 48.0F;
            eased[i] += (target[i] - eased[i]) * (float) -Math.expm1(-delta / tau);
        }
        lastEaseTime = ticks;
        if (state.active()) lastPresentation = state;
        if (eased[0] <= 0.0001F) return;
        writeWarpUniform(state.active() ? state : lastPresentation, (float) (ticks / 20.0D), partialTick);
    }

    static float shaderIntensity(LsdEffectState state) {
        if (!state.active() || !TrippyConfig.enableLsdEffects || !TrippyConfig.lsdWorldWarp) return 0.0F;
        // Session strengths contain the complete onset and uncapped dose curve.
        // A former maximum is neither a divisor nor an artistic intensity ceiling.
        return (float) (Math.log1p(Math.max(0.0D, state.visualIntensity()))
                * Math.max(0.0D, TrippyClientConfig.visualIntensity));
    }

    static float currentShaderIntensity() { return eased[0]; }
    static float currentShaderDose() { return eased[10]; }
    static float[] currentShaderChannels() { return eased.clone(); }

    private static float channel(double amplitude) {
        return (float) Math.log1p(Math.max(0.0D, amplitude) / 6.0D);
    }

    private static void resetPresentation() {
        java.util.Arrays.fill(eased, 0.0F);
        lastEaseTime = Double.NaN;
        lastPresentation = LsdEffectState.inactive();
    }

    private static void writeWarpUniform(LsdEffectState state, float time, float partialTick) {
        CameraData cameraData = cameraData();
        long seed = state.signature().seed();
        float seedA = ((seed >>> 6) & 4095L) / 4095.0F;
        float seedB = ((seed >>> 28) & 4095L) / 4095.0F;
        float envWater = (float) Math.min(1.0D, state.environment().water() + state.environment().rain() * 0.35D);
        float envHeat = (float) Math.min(1.0D, state.environment().nether() + state.environment().light() * 0.28D);
        float envSky = (float) Math.min(1.0D, state.environment().end() + state.environment().night() * 0.42D + state.environment().sunriseSunset() * 0.24D);
        float envBlocks = (float) Math.min(1.0D, state.environment().forest() * 0.32D + state.environment().cave() * 0.34D + state.environment().village() * 0.30D);
        float envDepth = (float) Math.min(1.0D, state.environment().depth() * 0.72D + state.environment().cave() * 0.28D);
        float tracerLayers = Math.max(1.0F, TrippyConfig.lsdPerformanceMode ? Math.min(3, TrippyConfig.lsdMaxTracerLayers) : TrippyConfig.lsdMaxTracerLayers);
        float dose = eased[10];
        WorldWarpPostProcessor.process(
                "lsd_world_warp", true, partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased[0], eased[1], eased[2], time),
                WorldWarpPostProcessor.uniform("WarpB", eased[3], eased[4], eased[5], eased[6]),
                WorldWarpPostProcessor.uniform("WarpC", TrippyConfig.lsdTracers ? eased[7] : 0.0F,
                        eased[8] * (float) TrippyConfig.lsdChromaticIntensity, eased[9], visualTier(dose)),
                WorldWarpPostProcessor.uniform("PaletteA", state.signature().primaryRed() / 255.0F, state.signature().primaryGreen() / 255.0F, state.signature().primaryBlue() / 255.0F, (float) state.signature().electricSharpness()),
                WorldWarpPostProcessor.uniform("PaletteB", state.signature().secondaryRed() / 255.0F, state.signature().secondaryGreen() / 255.0F, state.signature().secondaryBlue() / 255.0F, (float) state.signature().geometricPrecision()),
                WorldWarpPostProcessor.uniform("PaletteC", state.signature().accentRed() / 255.0F, state.signature().accentGreen() / 255.0F, state.signature().accentBlue() / 255.0F, (float) state.signature().prismSplit()),
                WorldWarpPostProcessor.uniform("Profile", seedA, seedB, state.signature().dominantDistortion(), state.signature().skyStyle()),
                WorldWarpPostProcessor.uniform("Env", envWater, envHeat, envSky, envBlocks),
                WorldWarpPostProcessor.uniform("EnvDetail", envDepth, tracerLayers / 8.0F, (float) TrippyConfig.lsdTracerLifetime, (float) state.progress()),
                WorldWarpPostProcessor.uniform("Dose", smoothstep(425.0F, 650.0F, dose), smoothstep(725.0F, 1000.0F, dose), smoothstep(1400.0F, 1600.0F, dose), dose / 1000.0F),
                WorldWarpPostProcessor.uniform("DoseDetail", dose / 1000.0F, dose / 1000.0F, 1.0F, 1.0F),
                // Surface deformation is visual; camera motion settings do not erase it.
                WorldWarpPostProcessor.uniform("Control", (TrippyConfig.lsdAccessibilityMode ? 0.35F : 1.0F)
                        * (TrippyClientConfig.motionReduction ? 0.24F : 1.0F),
                        (TrippyConfig.lsdAccessibilityMode ? 0.60F : 1.0F)
                                * (TrippyClientConfig.photosensitivityReduction ? 0.55F : 1.0F), 0.0F, 0.0F),
                WorldWarpPostProcessor.uniform("CameraA", cameraData.x(), cameraData.y(), cameraData.z(), cameraData.farPlane()),
                WorldWarpPostProcessor.uniform("CameraB", cameraData.forwardX(), cameraData.forwardY(), cameraData.forwardZ(), cameraData.nearPlane()),
                WorldWarpPostProcessor.uniform("CameraC", cameraData.rightX(), cameraData.rightY(), cameraData.rightZ(), 0.0F),
                WorldWarpPostProcessor.uniform("CameraD", cameraData.upX(), cameraData.upY(), cameraData.upZ(), 0.0F)
        );
    }

    private static CameraData cameraData() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) {
            return CameraData.identity();
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return CameraData.identity();
        }
        Vec3 position = camera.getPosition();
        Camera.NearPlane nearPlane = camera.getNearPlane();
        Vec3 forward = nearPlane.getPointOnPlane(0.0F, 0.0F);
        Vec3 right = nearPlane.getPointOnPlane(1.0F, 0.0F).subtract(forward);
        Vec3 up = nearPlane.getPointOnPlane(0.0F, 1.0F).subtract(forward);
        float farPlane = Math.max(16.0F, minecraft.gameRenderer.getDepthFar());
        return new CameraData(
                wrappedCoordinate(position.x),
                wrappedCoordinate(position.y),
                wrappedCoordinate(position.z),
                GameRenderer.PROJECTION_Z_NEAR,
                farPlane,
                (float) forward.x,
                (float) forward.y,
                (float) forward.z,
                (float) right.x,
                (float) right.y,
                (float) right.z,
                (float) up.x,
                (float) up.y,
                (float) up.z
        );
    }

    private static float wrappedCoordinate(double value) {
        double period = 4096.0D;
        double wrapped = value - Math.floor(value / period) * period;
        return (float) wrapped;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float visualTier(double micrograms) {
        float dose = (float) Math.max(0.0D, micrograms);
        return smoothstep(35.0F, 60.0F, dose)
                + smoothstep(80.0F, 120.0F, dose)
                + smoothstep(135.0F, 210.0F, dose)
                + smoothstep(210.0F, 285.0F, dose)
                + smoothstep(800.0F, 1000.0F, dose);
    }

    private record CameraData(
            float x,
            float y,
            float z,
            float nearPlane,
            float farPlane,
            float forwardX,
            float forwardY,
            float forwardZ,
            float rightX,
            float rightY,
            float rightZ,
            float upX,
            float upY,
            float upZ
    ) {
        private static CameraData identity() {
            return new CameraData(
                    0.0F,
                    0.0F,
                    0.0F,
                    GameRenderer.PROJECTION_Z_NEAR,
                    128.0F,
                    0.0F,
                    0.0F,
                    1.0F,
                    1.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    1.0F,
                    0.0F
            );
        }
    }
}
