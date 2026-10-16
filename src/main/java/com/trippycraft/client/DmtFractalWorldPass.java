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

    private DmtFractalWorldPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        // An off switch takes effect immediately, including during a fade out.
        // No new shader pass or render target is requested while disabled.
        if (!TrippyClientConfig.dmtFractalWorld || !TrippyClientConfig.rendererEnabled
                || !TrippyConfig.enableDmtEffects || !TrippyConfig.dmtWorldWarp
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
        double target = 0.0D;
        if (state.active()) {
            lastState = state;
            double entry = smoothstep(TrippyConfig.dmtTransitionStartProgress,
                    Math.max(TrippyConfig.dmtTransitionStartProgress + 0.01D,
                            TrippyConfig.dmtPostBreakthroughStartProgress), state.progress());
            double exit = 1.0D - smoothstep(TrippyConfig.dmtReturnStartProgress,
                    Math.max(TrippyConfig.dmtReturnStartProgress + 0.01D,
                            TrippyConfig.dmtAfterglowStartProgress), state.progress());
            double dose = smoothstep(TrippyConfig.dmtBreakthroughThresholdUnits * 0.85D,
                    TrippyConfig.dmtBreakthroughThresholdUnits * 1.10D, state.activeDoseUnits());
            double visual = smoothstep(0.01D, 0.40D, state.visualIntensity());
            double gain = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),
                    ClientExperienceStore.lsd(), state, ClientExperienceStore.hoMet(),
                    ClientExperienceStore.alLad(), ClientExperienceStore.twoCe()).dmtShaderGain();
            target = clamp(entry * exit * dose * visual * gain
                    * Math.min(1.0D, TrippyClientConfig.visualIntensity)
                    * Math.min(1.0D, TrippyConfig.globalIntensity));
        }
        // Keep the empty entrance chamber at the player until the crossing
        // begins; walking during onset must not start the realm inside a wall.
        if (target <= 0.0001D && opacity <= 0.0001D) {
            tripAnchor = head;
        }
        opacity += (target - opacity) * (1.0D - Math.exp(-delta / (target > opacity ? 12.0D : 20.0D)));
        if (Math.abs(opacity - target) < 0.001D) {
            opacity = target;
        }
        double motion = Math.max(0.0D, Math.min(2.0D, TrippyClientConfig.motionIntensity))
                * (TrippyClientConfig.motionReduction ? 0.15D : 1.0D)
                * (TrippyConfig.dmtAccessibilityMode ? 0.25D : 1.0D);
        // Integrate the rate: changing settings or redosing never rescales elapsed time.
        animationSeconds += delta * 0.05D * motion;
        if (opacity <= 0.0001D) {
            if (!state.active()) {
                reset();
            }
            return;
        }

        var signature = lastState.signature();
        var scaffold = lastState.fractalScaffold();
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
                        (float) scaffold.dimensionalDepth(), (float) scaffold.temporalFlow()),
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
    }

    private static double smoothstep(double low, double high, double value) {
        double t = clamp((value - low) / Math.max(0.0001D, high - low));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }
}
