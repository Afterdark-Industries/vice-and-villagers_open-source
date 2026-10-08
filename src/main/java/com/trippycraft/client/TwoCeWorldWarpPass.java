package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.twoce.TwoCeDoseCurve;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.twoce.TwoCePhase;
import com.trippycraft.substance.twoce.TwoCeVisualDoseResponse;
import com.trippycraft.substance.twoce.TwoCeVisualPresentation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Supplies a slowly evolving, surface-locked field and a shared gaze anchor for both VR eyes. */
public final class TwoCeWorldWarpPass {
    private static final float[] eased = new float[22];
    private static double lastTime = Double.NaN;
    private static double visualTicks;
    private static Vec3 lastPosition;
    private static Vec3 lastForward;
    private static Vec3 observedPoint;
    private static Direction observedFace;
    private static Vec3 movement = Vec3.ZERO;
    private static Vec3 turn = Vec3.ZERO;
    private static double observation;

    private TwoCeWorldWarpPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        TwoCeEffectState state = ClientExperienceStore.twoCe();
        if (!state.active() || state.phase() == TwoCePhase.SOBER || !TrippyClientConfig.rendererEnabled
                || minecraft.level == null || !Double.isFinite(TrippyClientConfig.visualIntensity)
                || TrippyClientConfig.visualIntensity <= 0.0D || !Double.isFinite(TrippyConfig.globalIntensity)
                || TrippyConfig.globalIntensity <= 0.0D) {
            reset();
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }
        double time = (double) minecraft.level.getGameTime() + partialTick;
        double delta = Double.isFinite(lastTime) ? time - lastTime : 0.0D;
        boolean discontinuity = !Double.isFinite(lastTime) || delta < 0.0D || delta > 80.0D;
        delta = discontinuity ? 0.0D : Math.min(delta, 5.0D);
        // All shader and channel harmonics complete an integer number of cycles
        // in this period, so wrapping remains continuous and a paused game freezes.
        double motionRate = Math.max(0.0D, Math.min(1.5D, TrippyClientConfig.motionIntensity
                * (TrippyClientConfig.motionReduction ? 0.22D : 0.86D)));
        visualTicks = discontinuity ? time % 96000.0D : (visualTicks + delta * motionRate) % 96000.0D;
        double gain = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),
                ClientExperienceStore.lsd(), ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(),
                ClientExperienceStore.alLad(), state).twoCeShaderGain();
        TwoCeVisualPresentation presentation = TwoCeVisualPresentation.resolve(state.targetContentMilligrams(),
                state.activeDoseMilligrams(), state.targetDoseMilligrams(), TrippyConfig.globalIntensity,
                TrippyClientConfig.visualIntensity, state.signature().symmetryBias(),
                new TwoCeVisualPresentation.Channels(state.intensity(), state.visualPresence(), state.cavities(),
                        state.nestedRooms(), state.tessellation(), state.edgeLock(), state.perspectiveFold(),
                        state.recursion(), state.corridorZoom(), state.symmetryLock(), state.geometricTrails()));
        TwoCeVisualDoseResponse highDose = presentation.highDose();
        TwoCeVisualPresentation.ExtraDose extraDose = presentation.extraDose();
        double gate10 = presentation.gate10();
        double gate20 = presentation.gate20();
        double gate40 = presentation.gate40();
        double seedPhase = (state.signature().seed() & 65535L) / 65535.0D * Math.PI * 2.0D;
        double seconds = visualTicks * 0.05D;
        double modulation = clamp01(TrippyClientConfig.motionIntensity)
                * (TrippyClientConfig.motionReduction ? 0.22D : 1.0D)
                * (TrippyClientConfig.photosensitivityReduction ? 0.25D : 1.0D);
        double[] target = {
                presentation.visualPresence() * gain,
                amplitude(presentation.cavities() * (0.46D + gate20 * 0.38D + gate40 * 0.16D))
                        * livingEnvelope(seconds, seedPhase, 0.0D, modulation),
                amplitude(presentation.nestedRooms() * (0.42D + gate20 * 0.40D + gate40 * 0.18D))
                        * livingEnvelope(seconds, seedPhase, 1.7D, modulation),
                amplitude(presentation.tessellation() * 1.10D)
                        * livingEnvelope(seconds, seedPhase, 3.2D, modulation),
                amplitude(presentation.edgeLock() * 0.90D),
                amplitude(presentation.perspectiveFold() * (0.45D + gate20 * 0.35D + gate40 * 0.20D))
                        * livingEnvelope(seconds, seedPhase, 4.9D, modulation),
                amplitude(presentation.recursion() * (0.60D + gate40 * 0.40D))
                        * livingEnvelope(seconds, seedPhase, 2.4D, modulation),
                amplitude(presentation.corridorZoom() * (0.65D + gate20 * 0.25D + gate40 * 0.10D))
                        * livingEnvelope(seconds, seedPhase, 5.8D, modulation),
                gate10,
                gate20,
                gate40,
                presentation.machineLift(),
                amplitude(presentation.symmetryLock()),
                amplitude(presentation.geometricTrails()),
                highDose.surfaceComplexity(),
                highDose.architecturalDepth(),
                highDose.perspectiveComplexity(),
                highDose.immersiveGeometry(),
                extraDose.recursiveVaults(),
                extraDose.impossiblePerspective(),
                extraDose.materialMorphogenesis(),
                extraDose.immersiveLattice()
        };
        for (int index = 0; index < eased.length; index++) {
            double tau = target[index] > eased[index] ? 20.0D : 28.0D;
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
        lastTime = time;
        lastPosition = position;
        lastForward = forward;
        if (eased[0] <= 0.0001F || TrippyClientConfig.visualIntensity <= 0.0D) {
            return;
        }

        float seed = (state.signature().seed() & 65535L) / 65535.0F;
        Vec3 origin = new Vec3(origin(position.x), origin(position.y), origin(position.z));
        Vec3 focus = observedPoint == null ? position.add(forward.scale(8.0D)) : observedPoint;
        focus = focus.subtract(origin);
        float motionScale = (float) (TrippyClientConfig.motionIntensity
                * (TrippyClientConfig.motionReduction ? 0.22D : 0.86D));
        float visualScale = (float) Math.min(1.35D, TrippyClientConfig.visualIntensity * 1.12D);
        float colorScale = TrippyClientConfig.photosensitivityReduction ? 0.38F : 0.92F;
        WorldWarpPostProcessor.process("two_ce_world_warp", true, partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased[0] * visualScale, eased[1], eased[2], (float) visualTicks),
                WorldWarpPostProcessor.uniform("WarpB", eased[3], eased[4], eased[5], eased[6]),
                WorldWarpPostProcessor.uniform("WarpC", eased[7], seed, (float) observation, eased[12]),
                WorldWarpPostProcessor.uniform("Intensity", eased[8], eased[9], eased[10], eased[11]),
                WorldWarpPostProcessor.uniform("HighDose", eased[14], eased[15], eased[16], eased[17]),
                WorldWarpPostProcessor.uniform("ExtraDose", eased[18], eased[19], eased[20], eased[21]),
                WorldWarpPostProcessor.uniform("MusicA", 0, 0, 0, 0),
                WorldWarpPostProcessor.uniform("MotionA", (float) movement.x, (float) movement.y, (float) movement.z, motionScale),
                WorldWarpPostProcessor.uniform("MotionB", (float) turn.x, (float) turn.y, (float) turn.z, colorScale),
                WorldWarpPostProcessor.uniform("Focus", (float) focus.x, (float) focus.y, (float) focus.z,
                        observedPoint == null ? 0.0F : (float) observation),
                WorldWarpPostProcessor.uniform("MixA",
                        load(ClientExperienceStore.lsd().prismSplit()),
                        load(ClientExperienceStore.alLad().stainedGlass()),
                        load(ClientExperienceStore.hoMet().spatialDepth()),
                        load(ClientExperienceStore.twoCb().crystalline())),
                WorldWarpPostProcessor.uniform("Control", colorScale, motionScale,
                        (float) Math.min(1.0D, state.environment().artificialBlocks() + state.environment().village() * 0.4D),
                        eased[13]));
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
            double alpha = 1.0D - Math.exp(-delta / 6.0D);
            movement = movement.lerp(limit(position.subtract(lastPosition).scale(1.0D / delta), 0.55D), alpha);
            turn = turn.lerp(limit(forward.subtract(lastForward).scale(1.0D / delta), 0.12D), alpha);
        }
        HitResult hit = minecraft.level.clip(new net.minecraft.world.level.ClipContext(position,
                position.add(forward.scale(64.0D)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, minecraft.player));
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            Vec3 point = block.getLocation();
            // Adjacent blocks on one wall remain one area of attention. Looking at another
            // wall starts a fresh reveal instead of transferring an almost-complete dwell.
            double continuityRadius = 0.60D + Math.min(position.distanceTo(point), 32.0D) * 0.022D;
            boolean sameSurface = observedPoint != null && observedFace == block.getDirection()
                    && observedPoint.distanceToSqr(point) < continuityRadius * continuityRadius;
            if (!sameSurface) {
                observation = 0.0D;
                observedPoint = point;
            } else {
                double stillness = (1.0D - TwoCeDoseCurve.smoothstep(0.025D, 0.16D, movement.length()))
                        * (1.0D - TwoCeDoseCurve.smoothstep(0.003D, 0.035D, turn.length()));
                observation += (stillness - observation)
                        * (1.0D - Math.exp(-delta / (stillness > observation ? 62.0D : 12.0D)));
                observedPoint = observedPoint.lerp(point, 1.0D - Math.exp(-delta / 12.0D));
            }
            observedFace = block.getDirection();
        } else {
            observation *= Math.exp(-delta / 14.0D);
            observedPoint = null;
            observedFace = null;
        }
    }

    private static double livingEnvelope(double seconds, double seedPhase, double offset, double modulation) {
        // Independent, overlapping breaths avoid a single whole-scene pulse. The steady
        // presence channel never oscillates, and dose changes do not alter these phases.
        return 0.94D + modulation * (Math.sin(seconds * Math.PI * 2.0D * 145.0D / 4800.0D
                + seedPhase + offset) * 0.045D
                + Math.sin(seconds * Math.PI * 2.0D * 63.0D / 4800.0D
                + seedPhase * 1.7D - offset * 0.6D) * 0.015D);
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

    private static double amplitude(double value) {
        // Slower compression keeps the authored medium/high levels distinguishable;
        // the previous exponential response flattened every channel to one too early.
        return Double.isFinite(value) ? 1.0D - 1.0D / (1.0D + Math.max(0.0D, value) * 1.45D) : 0.0D;
    }

    private static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }

    private static double origin(double value) {
        return Math.floor(value / 4096.0D) * 4096.0D;
    }

    static void reset() {
        java.util.Arrays.fill(eased, 0.0F);
        lastTime = Double.NaN;
        visualTicks = 0.0D;
        lastPosition = null;
        lastForward = null;
        observedPoint = null;
        observedFace = null;
        movement = Vec3.ZERO;
        turn = Vec3.ZERO;
        observation = 0.0D;
    }
}
