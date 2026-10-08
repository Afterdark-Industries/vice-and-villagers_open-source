package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.dmt.DmtEffectState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

public final class DmtWorldWarpPass {
    private static float smoothedDoseUnits;
    private static boolean smoothedDoseInitialized;
    private static DmtEffectState lingeringState = DmtEffectState.inactive();
    private static float lingeringIntensity;
    private static float lingeringDoseUnits;
    private static float lingeringDisplayDoseUnits;
    private static float lastLingeringTime = Float.NaN;
    private static long lastRenderClockNanos = -1L;
    private static double smoothRenderTicks;
    private static long lastTripAgeNanos = -1L;
    private static int lastTripAgeTicks = -1;
    private static int lastTripDurationTicks = -1;
    private static double smoothTripAgeTicks;
    private static boolean scaffoldUniformsInitialized;
    private static long lastScaffoldUniformNanos = -1L;
    private static float smoothFold3d;
    private static float smoothAnimatedFold;
    private static float smoothCoupling;
    private static float smoothAlternateWorld;
    private static float smoothDimensionalDepth;
    private static float smoothTemporalFlow;
    private static float smoothMorphRate;
    private static boolean scaffoldPhaseInitialized;
    private static double smoothScaffoldPhase;
    private static double lastScaffoldPhaseAgeTicks;

    private DmtWorldWarpPass() {
    }

    public static void render(float partialTick) {
        float time = worldTime();
        DmtEffectState state = ClientExperienceStore.dmt();
        float shaderDose = state.active() ? smoothedShaderDose(state) : smoothedDoseUnits;
        float visualTargetDose = (float) state.targetDoseUnits();
        float visualDisplayDose = (float) state.displayDoseUnits();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(), ClientExperienceStore.lsd(), state, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        float intensity = state.active() ? (float) (shaderIntensity(state, shaderDose) * blend.dmtShaderGain()) : 0.0F;
        if (intensity > 0.0001F) {
            lingeringState = state;
            lingeringIntensity = intensity;
            lingeringDoseUnits = shaderDose;
            lingeringDisplayDoseUnits = visualDisplayDose;
            lastLingeringTime = time;
        } else if (lingeringState.active() && lingeringIntensity > 0.0001F) {
            float delta = Float.isFinite(lastLingeringTime) ? time - lastLingeringTime : 1.0F;
            if (!Float.isFinite(delta) || delta < 0.0F || delta > 120.0F) {
                delta = 1.0F;
            }
            float intensityAlpha = 1.0F - (float) Math.exp(-delta / 92.0F);
            float doseAlpha = 1.0F - (float) Math.exp(-delta / 118.0F);
            lingeringIntensity += (0.0F - lingeringIntensity) * intensityAlpha;
            lingeringDoseUnits += (0.0F - lingeringDoseUnits) * doseAlpha;
            lingeringDisplayDoseUnits += (0.0F - lingeringDisplayDoseUnits) * doseAlpha;
            lastLingeringTime = time;

            state = lingeringState;
            intensity = lingeringIntensity;
            shaderDose = Math.max(0.0F, lingeringDoseUnits);
            visualTargetDose = shaderDose;
            visualDisplayDose = Math.max(0.0F, Math.min(lingeringDisplayDoseUnits, shaderDose));
        }
        if (intensity <= 0.0001F) {
            if (!state.active()) {
                lingeringState = DmtEffectState.inactive();
                lingeringIntensity = 0.0F;
                lingeringDoseUnits = 0.0F;
                lingeringDisplayDoseUnits = 0.0F;
                smoothedDoseUnits = 0.0F;
                smoothedDoseInitialized = false;
                lastTripAgeNanos = -1L;
                lastTripAgeTicks = -1;
                lastTripDurationTicks = -1;
                smoothTripAgeTicks = 0.0D;
                scaffoldUniformsInitialized = false;
                lastScaffoldUniformNanos = -1L;
                scaffoldPhaseInitialized = false;
                smoothScaffoldPhase = 0.0D;
                lastScaffoldPhaseAgeTicks = 0.0D;
            }
            return;
        }

        writeWarpUniform(state, intensity, time, shaderDose, visualTargetDose, visualDisplayDose, partialTick);
    }

    private static float shaderIntensity(DmtEffectState state, float shaderDose) {
        if (!state.active() || !TrippyConfig.enableDmtEffects || !TrippyConfig.dmtWorldWarp) {
            return 0.0F;
        }
        var scaffold = state.fractalScaffold();
        double scaffoldPresence = Math.max(scaffold.fractal3dPresence(),
                Math.max(scaffold.animatedFractalPresence(), scaffold.alternateWorldReadiness()));
        if (scaffoldPresence <= 0.001D) {
            return 0.0F;
        }
        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double phase = Math.min(1.0D, state.visualIntensity() / safe);
        double tier = Math.min(1.0D, visualTier(shaderDose) / 5.0D);
        double shape = scaffold.fractal3dPresence() * 0.86D
                + scaffold.animatedFractalPresence() * 0.82D
                + scaffold.staticAnimatedCoupling() * 0.76D
                + scaffold.alternateWorldReadiness() * 0.94D
                + scaffold.dimensionalDepth() * 0.54D
                + scaffold.temporalFlow() * 0.18D;
        if (TrippyConfig.dmtAccessibilityMode) {
            shape *= 0.62D;
        }
        double doseOverdrive = smoothstep(5.40F, 7.20F, shaderDose);
        double doseExcess = Math.max(0.0D, shaderDose - 5.4D);
        shape *= 1.0D + doseOverdrive * 0.48D + Math.min(0.30D, doseExcess * 0.020D);
        double breakthrough = smoothstep(1.35F, 2.35F, shaderDose) * 0.46D
                + smoothstep(2.35F, 5.20F, shaderDose) * 0.52D
                + doseOverdrive * 0.92D
                + scaffold.alternateWorldReadiness() * 0.42D;
        return (float) Math.min(8.40D, scaffoldPresence * (0.22D + phase * 0.78D + tier * 0.28D + doseOverdrive * 0.52D) + shape + breakthrough * 0.26D);
    }

    private static void writeWarpUniform(DmtEffectState state, float intensity, float time, float shaderDose, float visualTargetDose, float visualDisplayDose, float partialTick) {
        CameraData cameraData = cameraData();
        long seed = state.signature().seed();
        float seedA = ((seed >>> 5) & 4095L) / 4095.0F;
        float seedB = ((seed >>> 31) & 4095L) / 4095.0F;
        var rawScaffold = state.fractalScaffold();
        updateSmoothTripAge(state);
        ScaffoldUniforms scaffold = smoothScaffoldUniforms(state);
        float tier = visualTier(shaderDose);
        float voidEnv = (float) Math.min(1.0D, state.environment().end() + state.environment().cave() * 0.38D + state.environment().night() * 0.20D);
        float lightEnv = (float) Math.min(1.0D, state.environment().light() * 0.60D + state.environment().sunriseSunset() * 0.32D + state.environment().nether() * 0.24D);
        float motionEnv = (float) Math.min(1.0D, state.environment().moving() + state.environment().entities() * 0.35D);
        float envDepth = (float) Math.min(1.0D, state.environment().depth() * 0.68D + state.environment().cave() * 0.28D + state.environment().end() * 0.25D);
        float breakthrough = Math.max(smoothstep(1.70F, 2.80F, shaderDose), scaffold.alternateWorld());
        float accessibility = TrippyConfig.dmtAccessibilityMode ? 1.0F : 0.0F;
        float shaderProgress = continuousProgress(state);
        float shaderRemainingSeconds = continuousRemainingSeconds(state, shaderProgress);
        float shaderScaffoldPhase = continuousScaffoldPhase(state, scaffold.morphRate());
        WorldWarpPostProcessor.process(
                "dmt_world_warp",
                true,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", intensity, scaffold.fold3d(), scaffold.animatedFold(), time),
                WorldWarpPostProcessor.uniform("WarpB", scaffold.coupling(), scaffold.alternateWorld(), scaffold.dimensionalDepth(), scaffold.temporalFlow()),
                WorldWarpPostProcessor.uniform("WarpC", scaffold.morphRate(), shaderScaffoldPhase, tier, shaderProgress),
                WorldWarpPostProcessor.uniform("PaletteA", state.signature().primaryRed() / 255.0F, state.signature().primaryGreen() / 255.0F, state.signature().primaryBlue() / 255.0F, (float) state.signature().symmetry()),
                WorldWarpPostProcessor.uniform("PaletteB", state.signature().secondaryRed() / 255.0F, state.signature().secondaryGreen() / 255.0F, state.signature().secondaryBlue() / 255.0F, (float) state.signature().tunnelSpeed()),
                WorldWarpPostProcessor.uniform("PaletteC", state.signature().accentRed() / 255.0F, state.signature().accentGreen() / 255.0F, state.signature().accentBlue() / 255.0F, (float) state.signature().glyphDensity()),
                WorldWarpPostProcessor.uniform("Profile", seedA, seedB, state.signature().hyperspaceStyle(), (float) state.signature().mandalaDensity()),
                WorldWarpPostProcessor.uniform("Env", envDepth, voidEnv, lightEnv, motionEnv),
                WorldWarpPostProcessor.uniform("Dose", shaderDose, visualTargetDose, breakthrough, accessibility),
                WorldWarpPostProcessor.uniform("LayersA", scaffold.fold3d(), scaffold.animatedFold(), scaffold.coupling(), scaffold.alternateWorld()),
                WorldWarpPostProcessor.uniform("LayersB", scaffold.dimensionalDepth(), scaffold.temporalFlow(), scaffold.morphRate(), shaderScaffoldPhase),
                WorldWarpPostProcessor.uniform("LayersC", (float) ((rawScaffold.structureSeed() >>> 7) & 4095L) / 4095.0F, (float) ((rawScaffold.animationSeed() >>> 11) & 4095L) / 4095.0F, visualDisplayDose, shaderRemainingSeconds),
                WorldWarpPostProcessor.uniform("CameraA", cameraData.x(), cameraData.y(), cameraData.z(), cameraData.farPlane()),
                WorldWarpPostProcessor.uniform("CameraB", cameraData.forwardX(), cameraData.forwardY(), cameraData.forwardZ(), cameraData.nearPlane()),
                WorldWarpPostProcessor.uniform("CameraC", cameraData.rightX(), cameraData.rightY(), cameraData.rightZ(), 0.0F),
                WorldWarpPostProcessor.uniform("CameraD", cameraData.upX(), cameraData.upY(), cameraData.upZ(), 0.0F)
        );
    }

    private static float continuousProgress(DmtEffectState state) {
        if (state.durationTicks() <= 0) {
            return (float) state.progress();
        }
        float age = (float) Math.min((double) state.durationTicks(), Math.max(0.0D, smoothTripAgeTicks));
        return Math.max(0.0F, Math.min(1.0F, age / (float) state.durationTicks()));
    }

    private static float continuousRemainingSeconds(DmtEffectState state, float shaderProgress) {
        if (state.durationTicks() <= 0) {
            return Math.max(0.0F, (float) state.remainingTicks() / 20.0F);
        }
        float age = shaderProgress * (float) state.durationTicks();
        return Math.max(0.0F, ((float) state.durationTicks() - age) / 20.0F);
    }

    private static float continuousScaffoldPhase(DmtEffectState state, float morphRate) {
        if (state.durationTicks() <= 0 || morphRate <= 0.0F) {
            return (float) state.fractalScaffold().phase();
        }
        double seedPhase = ((state.signature().seed() >>> 16) & 1023L) / 1023.0D;
        boolean reset = !scaffoldPhaseInitialized || state.ageTicks() <= 2 || smoothTripAgeTicks < lastScaffoldPhaseAgeTicks - 0.5D;
        if (reset) {
            smoothScaffoldPhase = seedPhase + smoothTripAgeTicks * morphRate;
            lastScaffoldPhaseAgeTicks = smoothTripAgeTicks;
            scaffoldPhaseInitialized = true;
        } else {
            double deltaAge = Math.max(0.0D, Math.min(3.0D, smoothTripAgeTicks - lastScaffoldPhaseAgeTicks));
            smoothScaffoldPhase += deltaAge * morphRate;
            lastScaffoldPhaseAgeTicks = smoothTripAgeTicks;
        }
        double phase = smoothScaffoldPhase % 1.0D;
        if (phase < 0.0D) {
            phase += 1.0D;
        }
        return (float) phase;
    }

    private static ScaffoldUniforms smoothScaffoldUniforms(DmtEffectState state) {
        var scaffold = state.fractalScaffold();
        float targetFold3d = (float) scaffold.fractal3dPresence();
        float targetAnimatedFold = (float) scaffold.animatedFractalPresence();
        float targetCoupling = (float) scaffold.staticAnimatedCoupling();
        float targetAlternateWorld = (float) scaffold.alternateWorldReadiness();
        float targetDimensionalDepth = (float) scaffold.dimensionalDepth();
        float targetTemporalFlow = (float) scaffold.temporalFlow();
        float targetMorphRate = (float) scaffold.morphRate();
        long now = System.nanoTime();
        boolean reset = !scaffoldUniformsInitialized || state.ageTicks() <= 2 || lastScaffoldUniformNanos < 0L;
        if (reset) {
            smoothFold3d = targetFold3d;
            smoothAnimatedFold = targetAnimatedFold;
            smoothCoupling = targetCoupling;
            smoothAlternateWorld = targetAlternateWorld;
            smoothDimensionalDepth = targetDimensionalDepth;
            smoothTemporalFlow = targetTemporalFlow;
            smoothMorphRate = targetMorphRate;
            scaffoldUniformsInitialized = true;
            lastScaffoldUniformNanos = now;
        } else {
            double deltaSeconds = (now - lastScaffoldUniformNanos) / 1_000_000_000.0D;
            lastScaffoldUniformNanos = now;
            float alpha = 1.0F;
            if (Double.isFinite(deltaSeconds) && deltaSeconds >= 0.0D && deltaSeconds <= 0.25D) {
                alpha = 1.0F - (float) Math.exp(-deltaSeconds * 16.0D);
            }
            smoothFold3d += (targetFold3d - smoothFold3d) * alpha;
            smoothAnimatedFold += (targetAnimatedFold - smoothAnimatedFold) * alpha;
            smoothCoupling += (targetCoupling - smoothCoupling) * alpha;
            smoothAlternateWorld += (targetAlternateWorld - smoothAlternateWorld) * alpha;
            smoothDimensionalDepth += (targetDimensionalDepth - smoothDimensionalDepth) * alpha;
            smoothTemporalFlow += (targetTemporalFlow - smoothTemporalFlow) * alpha;
            smoothMorphRate += (targetMorphRate - smoothMorphRate) * alpha;
        }
        return new ScaffoldUniforms(
                smoothFold3d,
                smoothAnimatedFold,
                smoothCoupling,
                smoothAlternateWorld,
                smoothDimensionalDepth,
                smoothTemporalFlow,
                smoothMorphRate
        );
    }

    private static void updateSmoothTripAge(DmtEffectState state) {
        long now = System.nanoTime();
        boolean reset = lastTripAgeNanos < 0L
                || state.ageTicks() <= 2
                || state.ageTicks() < lastTripAgeTicks
                || state.durationTicks() != lastTripDurationTicks;
        if (reset) {
            smoothTripAgeTicks = Math.max(0.0D, state.ageTicks());
            lastTripAgeNanos = now;
            lastTripAgeTicks = state.ageTicks();
            lastTripDurationTicks = state.durationTicks();
            return;
        }

        double deltaSeconds = (now - lastTripAgeNanos) / 1_000_000_000.0D;
        lastTripAgeNanos = now;
        if (Double.isFinite(deltaSeconds) && deltaSeconds >= 0.0D && deltaSeconds <= 0.25D) {
            smoothTripAgeTicks += deltaSeconds * 20.0D;
        }

        double serverAge = Math.max(0.0D, state.ageTicks());
        if (smoothTripAgeTicks < serverAge) {
            smoothTripAgeTicks = serverAge;
        } else if (smoothTripAgeTicks > serverAge + 1.25D) {
            smoothTripAgeTicks = serverAge + 1.0D;
        }
        smoothTripAgeTicks = Math.min(smoothTripAgeTicks, Math.max(0.0D, state.durationTicks()));
        lastTripAgeTicks = state.ageTicks();
        lastTripDurationTicks = state.durationTicks();
    }

    private static float smoothedShaderDose(DmtEffectState state) {
        float target = (float) Math.max(0.0D, state.activeDoseUnits());
        if (!smoothedDoseInitialized || state.ageTicks() <= 2 || target < 0.01F) {
            smoothedDoseUnits = target;
            smoothedDoseInitialized = true;
            return smoothedDoseUnits;
        }
        float delta = target - smoothedDoseUnits;
        float blend = Math.min(0.18F, 0.055F + Math.abs(delta) / 16.0F);
        smoothedDoseUnits += delta * blend;
        if (Math.abs(delta) < 0.002F) {
            smoothedDoseUnits = target;
        }
        return smoothedDoseUnits;
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

    private static float visualTier(double doseUnits) {
        float dose = (float) Math.max(0.0D, doseUnits);
        return smoothstep(0.20F, 0.55F, dose)
                + smoothstep(0.70F, 1.05F, dose)
                + smoothstep(1.20F, 1.85F, dose)
                + smoothstep(2.00F, 2.85F, dose)
                + smoothstep(5.20F, 7.00F, dose);
    }

    private static float worldTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            lastRenderClockNanos = -1L;
            smoothRenderTicks = 0.0D;
            return 0.0F;
        }
        long now = System.nanoTime();
        if (lastRenderClockNanos < 0L) {
            lastRenderClockNanos = now;
            smoothRenderTicks = minecraft.level.getGameTime();
            return (float) smoothRenderTicks;
        }
        double deltaSeconds = (now - lastRenderClockNanos) / 1_000_000_000.0D;
        lastRenderClockNanos = now;
        if (Double.isFinite(deltaSeconds) && deltaSeconds >= 0.0D && deltaSeconds <= 0.25D) {
            smoothRenderTicks += deltaSeconds * 20.0D;
        }
        double worldTicks = minecraft.level.getGameTime();
        if (smoothRenderTicks < worldTicks - 4.0D || smoothRenderTicks > worldTicks + 80.0D) {
            smoothRenderTicks = worldTicks;
        }
        return (float) smoothRenderTicks;
    }

    private record ScaffoldUniforms(
            float fold3d,
            float animatedFold,
            float coupling,
            float alternateWorld,
            float dimensionalDepth,
            float temporalFlow,
            float morphRate
    ) {
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
