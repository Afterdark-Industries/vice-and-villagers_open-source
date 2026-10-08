package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadPhase;
import com.trippycraft.substance.allad.AlLadVisualDrive;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Material-locked AL-LAD phase contours, laminar depth and continuous refractive sheets in shared head coordinates. */
public final class AlLadWorldWarpPass {
    private static final float[] eased = new float[20];
    private static double lastTime = Double.NaN;
    private static double visualTicks;
    private static long lastSeed;
    private static int lastAge = -1;
    private static Vec3 lastPosition;
    private static Vec3 lastForward;
    private static Vec3 observedPoint;
    private static Direction observedFace;
    private static Vec3 movement = Vec3.ZERO;
    private static Vec3 turn = Vec3.ZERO;
    private static double observation;
    private static double musicBass;
    private static double musicMelody;
    private static double musicShimmer;

    private AlLadWorldWarpPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        AlLadEffectState state = ClientExperienceStore.alLad();
        if (!state.active() || state.phase() == AlLadPhase.SOBER || !TrippyClientConfig.rendererEnabled
                || minecraft.level == null || !Double.isFinite(TrippyClientConfig.visualIntensity)
                || TrippyClientConfig.visualIntensity <= 0.0D || !Double.isFinite(TrippyConfig.globalIntensity)
                || TrippyConfig.globalIntensity <= 0.0D) {
            reset();
            return;
        }
        if (lastAge >= 0 && (lastSeed != state.signature().seed() || state.ageTicks() < lastAge)) {
            reset();
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }
        double time = (double) minecraft.level.getGameTime() + clamp(partialTick, 0.0D, 1.0D);
        double delta = Double.isFinite(lastTime) ? time - lastTime : 0.0D;
        boolean discontinuity = !Double.isFinite(lastTime) || delta < 0.0D || delta > 80.0D;
        delta = discontinuity ? 0.0D : Math.min(delta, 5.0D);
        double motionRate = clamp(TrippyClientConfig.motionIntensity
                * (TrippyClientConfig.motionReduction ? 0.22D : 1.0D), 0.0D, 2.0D);
        visualTicks = discontinuity ? AlLadVisualDrive.wrapTicks(time)
                : AlLadVisualDrive.advanceVisualTicks(visualTicks, delta, motionRate);
        double gain = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),
                ClientExperienceStore.lsd(), ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), state,
                ClientExperienceStore.twoCe())
                .alLadShaderGain();
        AlLadVisualDrive drive = AlLadVisualDrive.resolve(state.targetContentMilligrams(), state.activeDoseMicrograms(),
                state.targetDoseMicrograms(), TrippyConfig.globalIntensity, TrippyClientConfig.visualIntensity, gain,
                new AlLadVisualDrive.Context(state.environment().light(), state.environment().village(),
                        state.signature().recursionBias()),
                new AlLadVisualDrive.Channels(state.intensity(), state.visualPresence(), state.stainedGlass(),
                        state.prismDepth(), state.paneMotion(), state.chromaticFringe(), state.cathedralRecursion(),
                        state.jewelColor(), state.musicSensitivity()));
        if (drive.visualPresence() <= 0.0D) {
            reset();
            return;
        }
        double[] target = {
                drive.visualPresence(), drive.stainedGlass(), drive.prismDepth(), drive.paneMotion(),
                drive.chromaticFringe(), drive.cathedralRecursion(), drive.jewelColor(), drive.musicSensitivity(),
                drive.gate75(), drive.gate150(), drive.gate300(), drive.heroicLift(),
                drive.phaseContours(), drive.laminarDepth(), drive.recursiveSheets(), drive.interferenceVolumes(),
                drive.phaseMultiplicity(), drive.laminarComplexity(), drive.recursiveInterference(), drive.immersiveContinuity()
        };
        for (int index = 0; index < eased.length; index++) {
            double tau = target[index] > eased[index] ? 24.0D : 36.0D;
            eased[index] = finite(discontinuity ? target[index]
                    : eased[index] + (target[index] - eased[index]) * (1.0D - Math.exp(-delta / tau)));
        }

        Vec3 position = VivecraftCompat.isVrWorldPass() && VivecraftCompat.headPosition() != null
                ? VivecraftCompat.headPosition() : camera.getPosition();
        Vec3 forward = camera.getNearPlane().getPointOnPlane(0.0F, 0.0F).normalize();
        if (VivecraftCompat.isVrWorldPass() && VivecraftCompat.headRotation() != null) {
            var trackedForward = VivecraftCompat.headRotation().transform(new org.joml.Vector3f(0, 0, -1));
            forward = new Vec3(trackedForward.x, trackedForward.y, trackedForward.z);
        }
        updateObservation(minecraft, position, forward, delta, discontinuity);
        var music = ClientTavernAmbience.musicStructure(partialTick);
        double musicEase = discontinuity ? 1.0D : 1.0D - Math.exp(-delta / 16.0D);
        musicBass += (music.bassExpansion() - musicBass) * musicEase;
        musicMelody += (music.melodicFlow() - musicMelody) * musicEase;
        musicShimmer += ((music.harmonicShift() + music.sustainedRibbon()) * 0.5D - musicShimmer) * musicEase;
        lastTime = time;
        lastSeed = state.signature().seed();
        lastAge = state.ageTicks();
        lastPosition = position;
        lastForward = forward;
        if (eased[0] <= 0.0001F || TrippyClientConfig.visualIntensity <= 0.0D) {
            return;
        }

        float seed = (state.signature().seed() & 65535L) / 65535.0F;
        Vec3 origin = new Vec3(origin(position.x), origin(position.y), origin(position.z));
        Vec3 focus = observedPoint == null ? position.add(forward.scale(8.0D)) : observedPoint;
        focus = focus.subtract(origin);
        float motionScale = (float) motionRate;
        float musicMotion = (float) Math.min(1.0D, motionRate);
        float colorScale = TrippyClientConfig.photosensitivityReduction ? 0.35F : 1.0F;
        WorldWarpPostProcessor.process("al_lad_world_warp", true, partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased[0], eased[1], eased[2], (float) visualTicks),
                WorldWarpPostProcessor.uniform("WarpB", eased[3], eased[4], eased[5], eased[6]),
                WorldWarpPostProcessor.uniform("WarpC", eased[7], seed, (float) observation,
                        (float) AlLadVisualDrive.colorCycle(visualTicks)),
                WorldWarpPostProcessor.uniform("Intensity", eased[8], eased[9], eased[10], eased[11]),
                WorldWarpPostProcessor.uniform("ExtraDose", eased[12], eased[13], eased[14], eased[15]),
                WorldWarpPostProcessor.uniform("HighDose", eased[16], eased[17], eased[18], eased[19]),
                WorldWarpPostProcessor.uniform("MusicA", (float) musicBass * musicMotion, (float) musicMelody * musicMotion,
                        (float) musicShimmer * musicMotion, (float) music.phrasePhase()),
                WorldWarpPostProcessor.uniform("MotionA", (float) movement.x, (float) movement.y, (float) movement.z, motionScale),
                WorldWarpPostProcessor.uniform("MotionB", (float) turn.x, (float) turn.y, (float) turn.z, colorScale),
                WorldWarpPostProcessor.uniform("Focus", (float) focus.x, (float) focus.y, (float) focus.z,
                        observedPoint == null ? 0.0F : (float) observation),
                WorldWarpPostProcessor.uniform("MixA",
                        load(ClientExperienceStore.lsd().prismSplit()),
                        load(ClientExperienceStore.psilocybin().organicBreathing()),
                        load(ClientExperienceStore.hoMet().spatialDepth()),
                        load(ClientExperienceStore.twoCb().crystalline())),
                WorldWarpPostProcessor.uniform("Control", colorScale, motionScale,
                        (float) clamp01(state.environment().light() + state.environment().sunriseSunset() * 0.5D),
                        (float) clamp01(state.environment().village() + state.environment().artificialBlocks() * 0.5D)));
    }

    private static void updateObservation(Minecraft minecraft, Vec3 position, Vec3 forward,
                                          double delta, boolean discontinuity) {
        if (discontinuity || lastPosition == null || lastForward == null || position.distanceToSqr(lastPosition) > 64.0D) {
            movement = Vec3.ZERO;
            turn = Vec3.ZERO;
            observation = 0.0D;
            observedPoint = null;
            observedFace = null;
        } else if (delta > 0.00001D) {
            double alpha = 1.0D - Math.exp(-delta / 4.0D);
            movement = movement.lerp(limit(position.subtract(lastPosition).scale(1.0D / delta), 0.8D), alpha);
            turn = turn.lerp(limit(forward.subtract(lastForward).scale(1.0D / delta), 0.18D), alpha);
        }
        HitResult hit = minecraft.level.clip(new net.minecraft.world.level.ClipContext(position,
                position.add(forward.scale(64.0D)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, minecraft.player));
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            Vec3 point = block.getLocation();
            double continuityRadius = 0.60D + Math.min(position.distanceTo(point), 32.0D) * 0.022D;
            boolean sameSurface = observedPoint != null && observedFace == block.getDirection()
                    && observedPoint.distanceToSqr(point) < continuityRadius * continuityRadius;
            if (!sameSurface) observation = 0.0D;
            double target = sameSurface && movement.lengthSqr() < 0.0064D && turn.lengthSqr() < 0.0004D ? 1.0D : 0.0D;
            observation += (target - observation) * (1.0D - Math.exp(-delta / (target > observation ? 90.0D : 9.0D)));
            observedPoint = sameSurface ? observedPoint.lerp(point, 1.0D - Math.exp(-delta / 20.0D)) : point;
            observedFace = block.getDirection();
        } else {
            observation *= Math.exp(-delta / 12.0D);
            observedPoint = null;
            observedFace = null;
        }
    }

    private static Vec3 limit(Vec3 value, double maximum) {
        double length = value.length();
        return length > maximum ? value.scale(maximum / length) : value;
    }

    private static float load(double value) {
        return (float) (1.0D - Math.exp(-Math.max(0.0D, value)));
    }

    private static float finite(double value) {
        return Double.isFinite(value) ? (float) Math.max(0.0D, Math.min(value, Float.MAX_VALUE)) : 0.0F;
    }

    private static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }

    private static double origin(double value) {
        return Math.floor(value / 4096.0D) * 4096.0D;
    }

    static void reset() {
        java.util.Arrays.fill(eased, 0.0F);
        lastTime = Double.NaN;
        visualTicks = 0.0D;
        lastSeed = 0L;
        lastAge = -1;
        lastPosition = null;
        lastForward = null;
        observedPoint = null;
        observedFace = null;
        movement = Vec3.ZERO;
        turn = Vec3.ZERO;
        observation = musicBass = musicMelody = musicShimmer = 0.0D;
    }
}
