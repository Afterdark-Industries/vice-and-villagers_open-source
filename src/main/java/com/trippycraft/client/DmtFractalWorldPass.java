package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.dmt.DmtEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Optional geometric realm, composited after the existing DMT presentation. */
public final class DmtFractalWorldPass {
    private static final double WORLD_SCALE = 0.35D;
    private static Object lastLevel;
    private static long tripSeed;
    private static Vec3 tripAnchor;
    private static DmtEffectState lastState;
    private static double lastTicks = Double.NaN;
    private static double animationSeconds;
    private static double opacity;
    private static boolean fieldInitialized;
    private static double smoothDimensionalDepth;
    private static double smoothTemporalFlow;
    private static double elapsedTripSeconds;
    private static double crossingSeconds;
    private static boolean crossingStarted;
    private static boolean doseInitialized;
    private static double smoothDoseEnergy;
    private static double smoothAbsoluteDoseLog;
    private static double cinematicIngress;
    private static final double[] worldInfluence = new double[16];
    private static final double[] worldAttractor = new double[4];
    private static boolean attractorInitialized;
    private static boolean influenceInitialized;

    private DmtFractalWorldPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        // An off switch takes effect immediately, including during a fade out.
        // No new shader pass or render target is requested while disabled.
        if (!TrippyClientConfig.dmtFractalWorld || !TrippyClientConfig.rendererEnabled
                || !TrippyConfig.enableDmtEffects || !TrippyConfig.dmtWorldWarp
                || TrippyClientConfig.dmtFractalIntensity <= 0.0D
                || TrippyClientConfig.visualIntensity <= 0.0D || TrippyConfig.globalIntensity <= 0.0D
                || minecraft.level == null || minecraft.player == null) {
            reset();
            return;
        }

        DmtEffectState state = ClientExperienceStore.dmt();
        if (lastLevel != minecraft.level) {
            reset();
            lastLevel = minecraft.level;
        }
        Vec3 head = VivecraftCompat.isVrWorldPass() && VivecraftCompat.headPosition() != null
                ? VivecraftCompat.headPosition() : minecraft.gameRenderer.getMainCamera().getPosition();
        if (state.active() && (tripAnchor == null || state.signature().seed() != tripSeed)) {
            reset();
            lastLevel = minecraft.level;
            tripAnchor = head;
            tripSeed = state.signature().seed();
        }
        if (tripAnchor == null) {
            return;
        }

        double ticks = minecraft.level.getGameTime() + (double) partialTick;
        double delta = Double.isFinite(lastTicks) ? ticks - lastTicks : 0.0D;
        // Pauses, lag and clock discontinuities must not jump the realm animation.
        delta = Double.isFinite(delta) && delta >= 0.0D ? Math.min(5.0D, delta) : 0.0D;
        lastTicks = ticks;
        elapsedTripSeconds += delta * 0.05D;
        double selectedIntensity = clamp(TrippyClientConfig.dmtFractalIntensity);
        // Lowering the player-selected limit takes effect immediately. Raising
        // it eases into the new amount without resetting the realm or its clock.
        opacity = Math.min(opacity, selectedIntensity);
        double target = 0.0D;
        if (state.active()) {
            lastState = state;
            double doseEase = doseInitialized ? 1.0D - Math.exp(-delta / 45.0D) : 1.0D;
            smoothDoseEnergy += (DmtRealmDynamics.doseLog(state.activeDoseUnits(),
                    TrippyConfig.dmtBreakthroughThresholdUnits) - smoothDoseEnergy) * doseEase;
            smoothAbsoluteDoseLog += (DmtRealmDynamics.doseLog(state.activeDoseUnits())
                    - smoothAbsoluteDoseLog) * doseEase;
            doseInitialized = true;
            double exit = 1.0D - DmtRealmDynamics.smootherstep(TrippyConfig.dmtReturnStartProgress,
                    Math.max(TrippyConfig.dmtReturnStartProgress + 0.01D,
                            TrippyConfig.dmtAfterglowStartProgress), state.progress());
            double dose = smoothstep(TrippyConfig.dmtBreakthroughThresholdUnits * 0.85D,
                    TrippyConfig.dmtBreakthroughThresholdUnits * 1.10D, state.activeDoseUnits());
            double visual = smoothstep(0.01D, 0.40D, state.visualIntensity());
            if (!crossingStarted && state.progress() >= TrippyConfig.dmtTransitionStartProgress
                    && dose > 0.0D && visual > 0.0D) crossingStarted = true;
            if (crossingStarted) crossingSeconds += delta * 0.05D;
            cinematicIngress = DmtRealmDynamics.cinematicEntry(crossingSeconds, smoothDoseEnergy);
            double gain = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),
                    ClientExperienceStore.lsd(), state, ClientExperienceStore.hoMet(),
                    ClientExperienceStore.alLad(), ClientExperienceStore.twoCe()).dmtShaderGain();
            target = clamp(cinematicIngress * exit * dose * visual * gain
                    * selectedIntensity
                    * Math.min(1.0D, TrippyClientConfig.visualIntensity)
                    * Math.min(1.0D, TrippyConfig.globalIntensity));
        }
        // Keep the empty entrance chamber at the player until the crossing
        // begins; walking during onset must not start the realm inside a wall.
        if (target <= 0.0001D && opacity <= 0.0001D) {
            tripAnchor = head;
        }
        opacity += (target - opacity) * (1.0D - Math.exp(-delta / (target > opacity ? 32.0D : 100.0D)));
        if (Math.abs(opacity - target) < 0.001D) {
            opacity = target;
        }
        double motion = Math.max(0.0D, Math.min(2.0D, TrippyClientConfig.motionIntensity))
                * (TrippyClientConfig.motionReduction ? 0.15D : 1.0D)
                * (TrippyConfig.dmtAccessibilityMode ? 0.25D : 1.0D);
        // Integrate the rate: changing settings or redosing never rescales elapsed time.
        animationSeconds += delta * 0.05D * motion
                * DmtRealmDynamics.transformationRate(smoothDoseEnergy * selectedIntensity);
        if (opacity <= 0.0001D) {
            if (!state.active()) {
                reset();
            }
            return;
        }

        var signature = lastState.signature();
        var scaffold = lastState.fractalScaffold();
        // Gameplay snapshots arrive on ticks. Ease their structural parameters
        // while the independent render clock keeps the realm moving every frame.
        double fieldEase = fieldInitialized ? 1.0D - Math.exp(-delta / 18.0D) : 1.0D;
        smoothDimensionalDepth += (clamp(scaffold.dimensionalDepth()) - smoothDimensionalDepth) * fieldEase;
        smoothTemporalFlow += (clamp(scaffold.temporalFlow()) - smoothTemporalFlow) * fieldEase;
        fieldInitialized = true;
        var environment = lastState.environment();
        var worldSample = DmtWorldInfluenceSampler.sample(minecraft);
        double influenceEase = influenceInitialized ? 1.0D - Math.exp(-delta / 36.0D) : 1.0D;
        smoothWorld(0, environment.organicBlocks() * 0.75D + environment.forest() * 0.25D,
                Math.max(environment.artificialBlocks(), environment.village() * 0.65D),
                Math.max(environment.cave(), environment.depth() * 0.60D), environment.water(), influenceEase);
        smoothWorld(4, environment.light(), environment.night() * 0.70D + environment.rain() * 0.30D,
                environment.entities() * 0.70D + environment.hostileEntities() * 0.30D,
                environment.moving(), influenceEase);
        smoothWorld(8, worldSample.red(), worldSample.green(), worldSample.blue(), worldSample.relief(), influenceEase);
        smoothSignedWorld(12, worldSample.directionX(), influenceEase);
        smoothSignedWorld(13, worldSample.directionY(), influenceEase);
        smoothSignedWorld(14, worldSample.directionZ(), influenceEase);
        smoothSignedWorld(15, clamp(worldSample.directionStrength()), influenceEase);
        if (worldSample.attractorStrength() > 0.0D) {
            Vec3 attractor = new Vec3(worldSample.attractorX(), worldSample.attractorY(), worldSample.attractorZ())
                    .subtract(tripAnchor).scale(WORLD_SCALE);
            double attractorEase = attractorInitialized ? influenceEase : 1.0D;
            worldAttractor[0] += (attractor.x - worldAttractor[0]) * attractorEase;
            worldAttractor[1] += (attractor.y - worldAttractor[1]) * attractorEase;
            worldAttractor[2] += (attractor.z - worldAttractor[2]) * attractorEase;
            attractorInitialized = true;
        }
        worldAttractor[3] += (clamp(worldSample.attractorStrength()) - worldAttractor[3]) * influenceEase;
        influenceInitialized = true;
        Vec3 realmPosition = head.subtract(tripAnchor).scale(WORLD_SCALE);
        boolean softerColors = TrippyClientConfig.photosensitivityReduction || TrippyConfig.dmtAccessibilityMode;
        // WorldInverseViewProjection and the actual per-eye/head positions are
        // supplied by WorldWarpPostProcessor. The same command is replayed for
        // both eyes, so neither the clock nor the trip origin advances twice.
        WorldWarpPostProcessor.process("dmt_fractal_world_warp", true, partialTick,
                WorldWarpPostProcessor.uniform("FractalA", (float) opacity, (float) animationSeconds,
                        Math.max(0, Math.min(2, TrippyClientConfig.dmtFractalQuality)), softerColors ? 1.0F : 0.0F),
                WorldWarpPostProcessor.uniform("FractalB", ((tripSeed >>> 5) & 4095L) / 4095.0F,
                        ((tripSeed >>> 31) & 4095L) / 4095.0F,
                        (float) smoothDimensionalDepth, (float) smoothTemporalFlow),
                WorldWarpPostProcessor.uniform("FractalC", (float) selectedIntensity,
                        ((tripSeed >>> 17) & 4095L) / 4095.0F,
                        ((tripSeed >>> 43) & 4095L) / 4095.0F, signature.hyperspaceStyle()),
                WorldWarpPostProcessor.uniform("RealmDynamics", (float) (smoothDoseEnergy * selectedIntensity),
                        (float) cinematicIngress, (float) elapsedTripSeconds, (float) smoothAbsoluteDoseLog),
                WorldWarpPostProcessor.uniform("WorldInfluenceA", (float) worldInfluence[0],
                        (float) worldInfluence[1], (float) worldInfluence[2], (float) worldInfluence[3]),
                WorldWarpPostProcessor.uniform("WorldInfluenceB", (float) worldInfluence[4],
                        (float) worldInfluence[5], (float) worldInfluence[6], (float) worldInfluence[7]),
                WorldWarpPostProcessor.uniform("WorldInfluenceC", (float) worldInfluence[8],
                        (float) worldInfluence[9], (float) worldInfluence[10], (float) worldInfluence[11]),
                WorldWarpPostProcessor.uniform("WorldInfluenceD", (float) worldInfluence[12],
                        (float) worldInfluence[13], (float) worldInfluence[14], (float) worldInfluence[15]),
                WorldWarpPostProcessor.uniform("WorldAttractor", (float) worldAttractor[0],
                        (float) worldAttractor[1], (float) worldAttractor[2], (float) worldAttractor[3]),
                WorldWarpPostProcessor.uniform("RealmOrigin", (float) realmPosition.x, (float) realmPosition.y,
                        (float) realmPosition.z, (float) WORLD_SCALE),
                WorldWarpPostProcessor.uniform("PaletteA", signature.primaryRed() / 255.0F,
                        signature.primaryGreen() / 255.0F, signature.primaryBlue() / 255.0F, 0.0F),
                WorldWarpPostProcessor.uniform("PaletteB", signature.secondaryRed() / 255.0F,
                        signature.secondaryGreen() / 255.0F, signature.secondaryBlue() / 255.0F, 0.0F),
                WorldWarpPostProcessor.uniform("PaletteC", signature.accentRed() / 255.0F,
                        signature.accentGreen() / 255.0F, signature.accentBlue() / 255.0F, 0.0F));
    }

    public static void reset() {
        lastLevel = null;
        tripSeed = 0L;
        tripAnchor = null;
        lastState = null;
        lastTicks = Double.NaN;
        animationSeconds = 0.0D;
        opacity = 0.0D;
        fieldInitialized = false;
        smoothDimensionalDepth = 0.0D;
        smoothTemporalFlow = 0.0D;
        elapsedTripSeconds = 0.0D;
        crossingSeconds = 0.0D;
        crossingStarted = false;
        doseInitialized = false;
        smoothDoseEnergy = 0.0D;
        smoothAbsoluteDoseLog = 0.0D;
        cinematicIngress = 0.0D;
        java.util.Arrays.fill(worldInfluence, 0.0D);
        java.util.Arrays.fill(worldAttractor, 0.0D);
        attractorInitialized = false;
        influenceInitialized = false;
        DmtWorldInfluenceSampler.reset();
    }

    private static void smoothWorld(int offset, double a, double b, double c, double d, double ease) {
        worldInfluence[offset] += (clamp(a) - worldInfluence[offset]) * ease;
        worldInfluence[offset + 1] += (clamp(b) - worldInfluence[offset + 1]) * ease;
        worldInfluence[offset + 2] += (clamp(c) - worldInfluence[offset + 2]) * ease;
        worldInfluence[offset + 3] += (clamp(d) - worldInfluence[offset + 3]) * ease;
    }

    private static void smoothSignedWorld(int index, double value, double ease) {
        double target = Double.isFinite(value) ? Math.max(-1.0D, Math.min(1.0D, value)) : 0.0D;
        worldInfluence[index] += (target - worldInfluence[index]) * ease;
    }

    private static double smoothstep(double low, double high, double value) {
        double t = clamp((value - low) / Math.max(0.0001D, high - low));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }
}
