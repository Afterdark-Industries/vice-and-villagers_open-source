package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PsilocybinWorldWarpPass {
    private static BlockPos staredBlock;
    private static int staredFace = -1;
    private static int stareTicks;
    private static long lastStareGameTime = Long.MIN_VALUE;
    private static long lastStareSeenGameTime = Long.MIN_VALUE;
    private static final int STARE_FADE_TICKS = 100;
    private static double lastEaseTime = Double.NaN;
    private static Object presentationLevel;
    private static PsilocybinEffectState lastPresentationState = PsilocybinEffectState.inactive();
    private static float easedIntensity;
    private static float easedBreath;
    private static float easedMelt;
    private static float easedDose;
    private static float easedMild;
    private static float easedObjectPattern;
    private static float easedHeroic;
    private static float easedOverload;

    private PsilocybinWorldWarpPass() {
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (presentationLevel != minecraft.level) {
            resetPresentation();
            presentationLevel = minecraft.level;
        }
        if (!TrippyClientConfig.rendererEnabled || !TrippyConfig.enablePsilocybinEffects
                || !TrippyConfig.psilocybinWorldWarp) {
            resetPresentation();
            return;
        }
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        double ticks = PsilocybinPresentationClock.timeTicks(partialTick);
        float time = (float) (ticks / 20.0D);
        float targetIntensity = (float) (shaderIntensity(state) * CompositeEffectResolver.resolve(state, ClientExperienceStore.lsd(), ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe()).psilocybinShaderGain());
        EasedWarp eased = updateEasedWarp(state, targetIntensity, ticks);
        if (state.active()) {
            lastPresentationState = state;
        }
        if (eased.intensity() <= 0.0001F) {
            return;
        }
        writeWarpUniform(state.active() ? state : lastPresentationState, eased, time, partialTick);
    }

    static float shaderIntensity(PsilocybinEffectState state) {
        if (!state.active() || !TrippyConfig.enablePsilocybinEffects || !TrippyConfig.psilocybinWorldWarp) {
            return 0.0F;
        }
        // The session already carries dose response and the onset envelope.
        // Re-dividing by a former visual ceiling and applying low-dose gates
        // again erased ordinary doses. Compress once without another threshold.
        double strength = Math.log1p(Math.max(0.0D, state.visualIntensity()));
        return (float) (strength * Math.max(0.0D, TrippyClientConfig.visualIntensity));
    }

    private static void writeWarpUniform(PsilocybinEffectState state, EasedWarp eased, float time, float partialTick) {
        long seed = state.signature().seed();
        float seedA = ((seed >>> 8) & 4095L) / 4095.0F;
        float seedB = ((seed >>> 24) & 4095L) / 4095.0F;
        FocusData focus = focusData(state, time, eased.objectPattern(), eased.heroic(), eased.overload());
        WorldWarpPostProcessor.process(
                "psilocybin_world_warp",
                true,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", eased.intensity(), eased.breath(), eased.melt(), time),
                WorldWarpPostProcessor.uniform("WarpB", seedA, seedB, eased.dose(), eased.heroic()),
                WorldWarpPostProcessor.uniform("PaletteB", state.signature().secondaryRed() / 255.0F, state.signature().secondaryGreen() / 255.0F, state.signature().secondaryBlue() / 255.0F, focus.strength()),
                WorldWarpPostProcessor.uniform("Focus", focus.x(), focus.y(), focus.radius(), focus.seed()),
                WorldWarpPostProcessor.uniform("Dose", eased.mild(), eased.objectPattern(), eased.heroic(), eased.overload()),
                // Texture deformation is a visual effect; disabling camera
                // motion must not silently disable the user's surface visuals.
                WorldWarpPostProcessor.uniform("Control", (TrippyConfig.psilocybinAccessibilityMode ? 0.35F : 1.0F)
                        * (TrippyClientConfig.motionReduction ? 0.24F : 1.0F),
                        (TrippyClientConfig.photosensitivityReduction ? 0.55F : 1.0F)
                                * (TrippyConfig.psilocybinAccessibilityMode ? 0.60F : 1.0F), 0.0F, 0.0F)
        );
    }

    private static EasedWarp updateEasedWarp(PsilocybinEffectState state, float targetIntensity, double time) {
        double dose = state.active() ? state.activeDoseGrams() : 0.0D;
        // GLSL transport is float-sized; a finite double dose must not become
        // infinity here. Its intensity still scales via the full double dose.
        float targetDose = (float) Math.min(Float.MAX_VALUE, Math.max(0.0D, dose));
        float targetBreath = state.active()
                ? (float) Math.log1p(Math.max(0.0D, state.organicBreathing()) / 6.0D)
                : 0.0F;
        float targetMelt = state.active() && TrippyConfig.psilocybinSurfaceMelting
                ? (float) Math.log1p(Math.max(0.0D, state.surfaceMelting()) / 6.0D)
                : 0.0F;
        float targetObjectPattern = (float) smoothstep(1.35D, 3.0D, dose);
        float targetHeroic = (float) smoothstep(4.0D, 5.0D, dose);
        float targetOverload = (float) smoothstep(8.0D, 10.0D, dose);
        float targetMild = (float) smoothstep(0.85D, 1.25D, dose);

        double elapsed = Double.isFinite(lastEaseTime) ? time - lastEaseTime : 0.0D;
        float delta = (float) Math.max(0.0D, Math.min(4.0D, elapsed));
        // Start at zero, also after reconnecting or loading a long-running
        // world. A stalled frame must never snap the envelope to its target.
        easedIntensity = ease(easedIntensity, targetIntensity, delta, targetIntensity >= easedIntensity ? 36.0F : 48.0F);
        easedBreath = ease(easedBreath, targetBreath, delta, 40.0F);
        easedMelt = ease(easedMelt, targetMelt, delta, 44.0F);
        easedDose = ease(easedDose, targetDose, delta, 40.0F);
        easedMild = ease(easedMild, targetMild, delta, 40.0F);
        easedObjectPattern = ease(easedObjectPattern, targetObjectPattern, delta, 44.0F);
        easedHeroic = ease(easedHeroic, targetHeroic, delta, 48.0F);
        easedOverload = ease(easedOverload, targetOverload, delta, 52.0F);
        lastEaseTime = time;
        return new EasedWarp(easedIntensity, easedBreath, easedMelt, easedDose, easedMild, easedObjectPattern, easedHeroic, easedOverload);
    }

    static float currentShaderIntensity() {
        return easedIntensity;
    }

    static float currentShaderDose() {
        return easedDose;
    }

    private static void resetPresentation() {
        lastEaseTime = Double.NaN;
        easedIntensity = easedBreath = easedMelt = easedDose = 0.0F;
        easedMild = easedObjectPattern = easedHeroic = easedOverload = 0.0F;
        lastPresentationState = PsilocybinEffectState.inactive();
        resetStareTracker();
    }

    private static float ease(float current, float target, float delta, float timeConstant) {
        if (delta <= 0.0F) {
            return current;
        }
        float alpha = 1.0F - (float) Math.exp(-delta / Math.max(1.0F, timeConstant));
        return current + (target - current) * Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static FocusData focusData(PsilocybinEffectState state, float time, float objectPattern, float heroic, float overload) {
        Minecraft minecraft = Minecraft.getInstance();
        float strength = 0.0F;
        float radius = 0.045F;
        float seed = (state.signature().seed() & 4095L) / 4095.0F;
        BlockPos focusBlock = null;
        int focusFace = -1;
        double focusDistance = 6.5D;
        double fadePresence = 1.0D;
        HitResult gazeHit = VivecraftCompat.isVrActive() ? VivecraftCompat.headBlockHit(minecraft) : minecraft.hitResult;
        if (gazeHit instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            focusFace = hit.getDirection().ordinal();
            updateStareTracker(minecraft, pos, focusFace);
            focusBlock = pos;
            focusDistance = focusDistance(minecraft, hit);
        } else {
            fadePresence = updateStareFade(minecraft);
            if (fadePresence > 0.0D && staredBlock != null && staredFace >= 0) {
                focusBlock = staredBlock;
                focusFace = staredFace;
                focusDistance = focusDistance(minecraft, focusBlock, focusFace);
            }
        }
        if (focusBlock != null) {
            int blockHash = hash(focusBlock.getX(), focusBlock.getY(), focusBlock.getZ(), focusFace);
            seed = (blockHash & 4095) / 4095.0F;
            double phase = Math.log1p(Math.max(0.0D, state.intensity()) / 6.0D);
            double dose = state.activeDoseGrams();
            double tripWake = psilocybinTripWake(state);
            double startGate = smoothstep(1.0D, 1.50D, dose) * tripWake;
            double doseStep = perGramDoseStep(dose);
            double doseWake = smoothstep(1.0D, 3.0D, dose);
            int stareStart = psilocybinActivationStartTicks(dose, focusBlock, focusFace);
            int stareRamp = psilocybinActivationRampTicks(dose);
            double stareGate = smoothstep(stareStart, stareStart + stareRamp, stareTicks);
            double deepStart = stareStart + stareRamp * mix(0.82D, 0.55D, doseWake);
            double deepStare = smoothstep(deepStart, deepStart + mix(100.0D, 54.0D, doseWake), stareTicks);
            double lockedStart = stareStart + stareRamp;
            double lockedStare = smoothstep(lockedStart, lockedStart + mix(82.0D, 44.0D, doseWake), stareTicks);
            double closeGate = 1.0D - smoothstep(1.25D, 6.5D, focusDistance);
            double closeBoost = 0.58D + closeGate * 0.32D;
            double stareBoost = 0.10D + stareGate * 0.40D + deepStare * 0.20D + lockedStare * 0.13D;
            double doseBoost = 0.12D + doseStep * 0.58D + heroic * 0.10D + overload * 0.12D;
            strength = (float) (phase * startGate * doseBoost * stareBoost * closeBoost * fadePresence);
            radius = (float) (0.045D + Math.log1p(doseStep) * 0.028D + heroic * 0.010D + overload * 0.012D + deepStare * 0.012D + closeGate * 0.014D);
        }
        return new FocusData(0.5F, 0.5F, radius, seed, strength);
    }

    public static int currentStareTicks(BlockPos pos, Direction face) {
        if (pos == null || face == null || staredBlock == null) {
            return 0;
        }
        return pos.equals(staredBlock) && face.ordinal() == staredFace ? stareTicks : 0;
    }

    private static double focusDistance(Minecraft minecraft, BlockHitResult hit) {
        if (minecraft.player == null) {
            return 6.5D;
        }
        Vec3 head = VivecraftCompat.headPosition();
        Vec3 eye = head != null ? head : minecraft.player.getEyePosition();
        return Math.min(8.0D, Math.max(0.0D, eye.distanceTo(hit.getLocation())));
    }

    private static double focusDistance(Minecraft minecraft, BlockPos pos, Direction face) {
        if (minecraft.player == null || pos == null || face == null) {
            return 6.5D;
        }
        Vec3 head = VivecraftCompat.headPosition();
        Vec3 eye = head != null ? head : minecraft.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        double x = center.x;
        double y = center.y;
        double z = center.z;
        switch (face) {
            case UP -> y = pos.getY() + 1.0D;
            case DOWN -> y = pos.getY();
            case NORTH -> z = pos.getZ();
            case SOUTH -> z = pos.getZ() + 1.0D;
            case WEST -> x = pos.getX();
            case EAST -> x = pos.getX() + 1.0D;
        }
        return Math.min(8.0D, Math.max(0.0D, eye.distanceTo(new Vec3(x, y, z))));
    }

    private static double focusDistance(Minecraft minecraft, BlockPos pos, int face) {
        if (face < 0 || face >= Direction.values().length) {
            return 6.5D;
        }
        return focusDistance(minecraft, pos, Direction.values()[face]);
    }

    private static void updateStareTracker(Minecraft minecraft, BlockPos pos, int face) {
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (!pos.equals(staredBlock) || face != staredFace) {
            staredBlock = pos;
            staredFace = face;
            stareTicks = 0;
            lastStareGameTime = gameTime;
            lastStareSeenGameTime = gameTime;
            return;
        }
        if (gameTime == lastStareGameTime) {
            return;
        }
        int delta = (int) Math.max(1L, Math.min(8L, gameTime - lastStareGameTime));
        stareTicks = Math.min(1200, stareTicks + delta);
        lastStareGameTime = gameTime;
        lastStareSeenGameTime = gameTime;
    }

    private static void resetStareTracker() {
        staredBlock = null;
        staredFace = -1;
        stareTicks = 0;
        lastStareGameTime = Long.MIN_VALUE;
        lastStareSeenGameTime = Long.MIN_VALUE;
    }

    private static double updateStareFade(Minecraft minecraft) {
        if (staredBlock == null || staredFace < 0) {
            return 0.0D;
        }
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (gameTime == lastStareGameTime) {
            return 1.0D;
        }
        lastStareGameTime = gameTime;
        long age = Math.max(0L, gameTime - lastStareSeenGameTime);
        double fade = 1.0D - smoothstep(0.0D, STARE_FADE_TICKS, age);
        if (fade <= 0.0D) {
            resetStareTracker();
            return 0.0D;
        }
        return fade;
    }

    private static double perGramDoseStep(double dose) {
        if (dose < 1.0D) {
            return 0.0D;
        }
        return Math.log1p(Math.max(0.0D, dose - 1.0D) / 3.0D);
    }

    private static double psilocybinTripWake(PsilocybinEffectState state) {
        if (!state.active()) {
            return 0.0D;
        }
        double progressWake = smoothstep(0.025D, 0.22D, state.progress());
        double visualWake = smoothstep(0.06D, 0.85D, state.visualIntensity());
        return Math.max(0.0D, Math.min(1.0D, progressWake * visualWake));
    }

    private static int psilocybinActivationStartTicks(double dose, BlockPos pos, int face) {
        double seconds = mix(10.0D, 8.0D, smoothstep(1.0D, 1.50D, dose));
        seconds = mix(seconds, 6.0D, smoothstep(1.50D, 3.0D, dose));
        seconds = mix(seconds, 4.2D, smoothstep(3.0D, 5.0D, dose));
        seconds = mix(seconds, 2.6D, smoothstep(5.0D, 10.0D, dose));
        double jitterWindow = mix(2.0D, 0.55D, smoothstep(1.50D, 5.0D, dose));
        seconds += surfaceRandom(pos, face, 0x6A09E667) * jitterWindow;
        return secondsToTicks(seconds);
    }

    private static int psilocybinActivationRampTicks(double dose) {
        double seconds = mix(3.0D, 2.0D, smoothstep(1.0D, 3.0D, dose));
        seconds = mix(seconds, 1.3D, smoothstep(3.0D, 5.0D, dose));
        seconds = mix(seconds, 0.9D, smoothstep(5.0D, 10.0D, dose));
        return secondsToTicks(seconds);
    }

    private static double surfaceRandom(BlockPos pos, int face, int salt) {
        if (pos == null || face < 0) {
            return 0.5D;
        }
        int value = hash(pos.getX(), pos.getY(), pos.getZ(), face) ^ salt;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return (value & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }

    private static double mix(double a, double b, double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        return a + (b - a) * clamped;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static int hash(int x, int y, int z, int face) {
        int value = x * 0x1F1F1F1F ^ y * 0x45D9F3B ^ z * 0x119DE1F3 ^ face * 0x3449B13;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return value;
    }

    private record EasedWarp(
            float intensity,
            float breath,
            float melt,
            float dose,
            float mild,
            float objectPattern,
            float heroic,
            float overload
    ) {
    }

    private record FocusData(float x, float y, float radius, float seed, float strength) {
    }
}
