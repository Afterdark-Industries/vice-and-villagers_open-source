package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.homet.HoMetEffectState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Supplies a shared spatial field; no camera angles, FOV, frame ghosts or HUD overlay. */
public final class HoMetWorldWarpPass {
    private static final float[] eased = new float[12];
    private static double lastTime = Double.NaN;
    private static Vec3 lastPosition;
    private static Vec3 lastForward;
    private static Vec3 observedPoint;
    private static Vec3 movement = Vec3.ZERO;
    private static Vec3 turn = Vec3.ZERO;
    private static double observation;
    private static double musicBass;
    private static double musicMelody;
    private static double musicShimmer;
    private static double colorFlow;

    private HoMetWorldWarpPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        HoMetEffectState state = ClientExperienceStore.hoMet();
        if (!state.active() || !TrippyClientConfig.rendererEnabled || minecraft.level == null) {
            reset();
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) return;
        double time = (double) minecraft.level.getGameTime() + partialTick;
        double delta = Double.isFinite(lastTime) ? time - lastTime : 0.0D;
        boolean discontinuity = !Double.isFinite(lastTime) || delta < 0.0D || delta > 80.0D;
        delta = discontinuity ? 0.0D : Math.min(delta, 5.0D);
        double gain = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),
                ClientExperienceStore.lsd(), ClientExperienceStore.dmt(), state, ClientExperienceStore.alLad(),
                ClientExperienceStore.twoCe()).hoMetShaderGain();
        double dose = Math.max(0.0D, state.activeDoseMilligrams());
        double highThreshold = Math.max(0.0D, Math.min(1.0D, (dose - 40.0D) / 10.0D));
        double[] target = {
                state.visualPresence() * gain, amplitude(state.spatialDepth()), amplitude(state.blockDeformation()),
                amplitude(state.surfaceRecursion()), amplitude(state.crystallineColor()), amplitude(state.perspectiveInstability()),
                Math.log1p(Math.max(0.0D, state.complexity())), amplitude(state.musicSensitivity()), amplitude(state.trails()),
                highThreshold * highThreshold * (3.0D - 2.0D * highThreshold),
                Math.log1p(Math.max(0.0D, dose - 40.0D) / 10.0D), Math.log1p(Math.max(0.0D, state.intensity()))
        };
        for (int index = 0; index < eased.length; index++) {
            double tau = target[index] > eased[index] ? 22.0D : 34.0D;
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
        updateObservation(minecraft, camera, position, forward, delta, discontinuity);
        var music = ClientTavernAmbience.musicStructure(partialTick);
        double musicEase = discontinuity ? 1.0D : 1.0D - Math.exp(-delta / 16.0D);
        musicBass += (music.bassExpansion() - musicBass) * musicEase;
        musicMelody += (music.melodicFlow() - musicMelody) * musicEase;
        musicShimmer += ((music.harmonicShift() + music.sustainedRibbon()) * 0.5D - musicShimmer) * musicEase;
        // Integrate velocity, never multiply total world time by a changing dose:
        // redosing changes color travel smoothly rather than jumping its phase.
        colorFlow = (colorFlow + delta * 0.05D * (0.035D + Math.log1p(eased[10]) * 0.012D)) % 1024.0D;
        lastTime = time;
        lastPosition = position;
        lastForward = forward;
        if (eased[0] <= 0.0001F || TrippyClientConfig.visualIntensity <= 0.0D) return;

        float seed = (state.signature().seed() & 65535L) / 65535.0F;
        Vec3 origin = new Vec3(origin(position.x), origin(position.y), origin(position.z));
        Vec3 focus = observedPoint == null ? position.add(forward.scale(8.0D)) : observedPoint;
        focus = focus.subtract(origin);
        float motionScale = (float) (TrippyClientConfig.motionIntensity
                * (TrippyClientConfig.motionReduction ? 0.22D : 1.0D));
        float visualScale = (float) TrippyClientConfig.visualIntensity;
        float colorScale = TrippyClientConfig.photosensitivityReduction ? 0.35F : 1.0F;
        WorldWarpPostProcessor.process("homet_world_warp", true, partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased[0] * visualScale, eased[1], eased[2], (float) (time % 96000.0D)),
                WorldWarpPostProcessor.uniform("WarpB", eased[3], eased[4], eased[5], eased[6]),
                WorldWarpPostProcessor.uniform("WarpC", eased[7], seed, (float) observation, eased[8]),
                WorldWarpPostProcessor.uniform("Intensity", eased[9], eased[10], eased[11], (float) colorFlow),
                WorldWarpPostProcessor.uniform("MusicA", (float) musicBass, (float) musicMelody,
                        (float) musicShimmer, (float) music.phrasePhase()),
                WorldWarpPostProcessor.uniform("MotionA", (float) movement.x, (float) movement.y, (float) movement.z, motionScale),
                WorldWarpPostProcessor.uniform("MotionB", (float) turn.x, (float) turn.y, (float) turn.z, colorScale),
                WorldWarpPostProcessor.uniform("Focus", (float) focus.x, (float) focus.y, (float) focus.z,
                        observedPoint == null ? 0.0F : (float) observation),
                WorldWarpPostProcessor.uniform("MixA",
                        load(ClientExperienceStore.lsd().realityFold()),
                        load(ClientExperienceStore.psilocybin().organicBreathing()),
                        load(ClientExperienceStore.mescaline().surfaceBreathing()),
                        load(ClientExperienceStore.twoCb().crystalline())),
                WorldWarpPostProcessor.uniform("Control", colorScale, motionScale,
                        (float) Math.min(1.0D, state.environment().forest() + state.environment().organicBlocks() * 0.5D),
                        (float) Math.min(1.0D, state.environment().cave() + state.environment().village() * 0.5D)));
    }

    private static void updateObservation(Minecraft minecraft, Camera camera, Vec3 position, Vec3 forward,
                                          double delta, boolean discontinuity) {
        if (discontinuity || lastPosition == null || lastForward == null || position.distanceToSqr(lastPosition) > 64.0D) {
            movement = Vec3.ZERO;
            turn = Vec3.ZERO;
            observation = 0.0D;
            observedPoint = null;
        } else if (delta > 0.00001D) {
            double alpha = 1.0D - Math.exp(-delta / 4.0D);
            movement = movement.lerp(limit(position.subtract(lastPosition).scale(1.0D / delta), 0.8D), alpha);
            turn = turn.lerp(limit(forward.subtract(lastForward).scale(1.0D / delta), 0.18D), alpha);
        }
        // Attention is visual, not limited to the player's block-interaction reach.
        // A receding wall or building can reveal detail while observed from across a room.
        HitResult hit = minecraft.level.clip(new net.minecraft.world.level.ClipContext(position,
                position.add(forward.scale(64.0D)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, minecraft.player));
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            Vec3 point = block.getLocation();
            boolean sameSurface = observedPoint != null && observedPoint.distanceToSqr(point) < 3.5D;
            double target = sameSurface && movement.lengthSqr() < 0.0064D && turn.lengthSqr() < 0.0004D ? 1.0D : 0.0D;
            observation += (target - observation) * (1.0D - Math.exp(-delta / (target > observation ? 95.0D : 9.0D)));
            observedPoint = sameSurface ? observedPoint.lerp(point, 1.0D - Math.exp(-delta / 20.0D)) : point;
        } else {
            observation *= Math.exp(-delta / 12.0D);
            observedPoint = null;
        }
    }

    private static Vec3 limit(Vec3 value, double maximum) {
        double length = value.length();
        return length > maximum ? value.scale(maximum / length) : value;
    }

    public static double observationDwell() {
        return observation;
    }

    private static float load(double value) {
        return (float) (1.0D - Math.exp(-Math.max(0.0D, value)));
    }

    private static float finite(double value) {
        return Double.isFinite(value) ? (float) Math.max(0.0D, Math.min(value, Float.MAX_VALUE)) : 0.0F;
    }

    private static double amplitude(double value) {
        // Open-ended log encoding keeps even the largest finite gameplay inputs
        // representable by the GPU, with no fixed dose or effect-amplitude cap.
        return Double.isFinite(value) ? Math.log1p(Math.max(0.0D, value)) / Math.log(2.0D) : 0.0D;
    }

    private static double origin(double value) {
        return Math.floor(value / 4096.0D) * 4096.0D;
    }

    static void reset() {
        java.util.Arrays.fill(eased, 0.0F);
        lastTime = Double.NaN;
        lastPosition = null;
        lastForward = null;
        observedPoint = null;
        movement = Vec3.ZERO;
        turn = Vec3.ZERO;
        observation = musicBass = musicMelody = musicShimmer = 0.0D;
        colorFlow = 0.0D;
    }
}
