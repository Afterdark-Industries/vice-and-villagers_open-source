package com.trippycraft.substance.mescaline;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class MescalineSession {
    private static final int ENVIRONMENT_UPDATE_INTERVAL = 30;

    private double activeDoseMilligrams;
    private double effectiveTargetDoseMilligrams;
    private double targetDoseMilligrams;
    private double displayedDoseMilligrams;
    private int ageTicks;
    private int durationTicks;
    private int repeatedDoses;
    private int environmentTicks;
    private double acuteTolerance;
    private MescalineTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();

    private MescalineSession(double dose, int duration, MescalineTripSignature signature) {
        activeDoseMilligrams = dose;
        effectiveTargetDoseMilligrams = dose;
        targetDoseMilligrams = dose;
        displayedDoseMilligrams = dose;
        durationTicks = duration;
        this.signature = signature;
    }

    public static MescalineSession start(ServerPlayer player, double doseMilligrams) {
        double dose = MescalineDoseCurve.sanitizeDose(doseMilligrams);
        long seed = player.getUUID().getMostSignificantBits() ^ player.level().getGameTime()
                ^ Double.doubleToLongBits(dose) ^ 0x6D657363616C696EL;
        MescalineSession result = new MescalineSession(dose, MescalineDoseCurve.baseDurationTicks(dose), MescalineTripSignature.create(seed));
        result.environment = PsilocybinEnvironmentSnapshot.capture(player);
        return result;
    }

    public void stackDose(double addedDoseMilligrams) {
        double added = MescalineDoseCurve.sanitizeDose(addedDoseMilligrams);
        if (added <= 0.0D) return;
        repeatedDoses++;
        double efficiency = Math.max(0.16D, 1.0D / (1.0D + repeatedDoses * 0.23D + Math.sqrt(targetDoseMilligrams / 100.0D) * 0.045D));
        targetDoseMilligrams = MescalineDoseCurve.safeAdd(targetDoseMilligrams, added);
        effectiveTargetDoseMilligrams = MescalineDoseCurve.safeAdd(effectiveTargetDoseMilligrams, added * efficiency);
        acuteTolerance = 1.0D - efficiency;
        int extension = MescalineDoseCurve.stackExtensionTicks(added, targetDoseMilligrams);
        long extendedDuration = Math.max((long) durationTicks + extension, (long) ageTicks + extension);
        durationTicks = safeDuration(Math.min(extendedDuration, MescalineDoseCurve.maximumDurationTicks()));
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) return;
        ageTicks++;
        double delta = Math.abs(effectiveTargetDoseMilligrams - activeDoseMilligrams);
        double ramp = 0.0018D + MescalineDoseCurve.smoothstep(25.0D, 500.0D, delta) * 0.0022D;
        activeDoseMilligrams += (effectiveTargetDoseMilligrams - activeDoseMilligrams) * ramp;
        displayedDoseMilligrams += (targetDoseMilligrams - displayedDoseMilligrams) * 0.009D;
        if (Math.abs(effectiveTargetDoseMilligrams - activeDoseMilligrams) < 0.01D) activeDoseMilligrams = effectiveTargetDoseMilligrams;
        if (Math.abs(targetDoseMilligrams - displayedDoseMilligrams) < 0.01D) displayedDoseMilligrams = targetDoseMilligrams;
        if (--environmentTicks <= 0) {
            environment = environment.blend(PsilocybinEnvironmentSnapshot.capture(player), 0.12D);
            environmentTicks = ENVIRONMENT_UPDATE_INTERVAL;
        }
    }

    public MescalineEffectState effectState() {
        if (!isAlive()) return MescalineEffectState.inactive();
        double p = progress();
        MescalinePhase phase = phase();
        double envelope = MescalineDoseCurve.phaseEnvelope(phase, p);
        double intensity = MescalineDoseCurve.peakIntensity(activeDoseMilligrams) * envelope;
        double presence = MescalineDoseCurve.visualPresence(intensity);
        double complexity = MescalineDoseCurve.complexity(activeDoseMilligrams) * envelope;
        double opening = MescalineDoseCurve.smoothstep(0.08D, 0.42D, p);
        double depthWindow = MescalineDoseCurve.smoothstep(0.30D, 0.56D, p)
                * (1.0D - MescalineDoseCurve.smoothstep(0.84D, 0.98D, p));
        double high = MescalineDoseCurve.smoothstep(120.0D, 500.0D, activeDoseMilligrams);
        double extreme = Math.max(0.0D, complexity - 3.0D);
        double slowBreath = 0.92D + Math.sin(ageTicks * 0.0062D * signature.breathingRhythm() + (signature.seed() & 1023L) * 0.007D) * 0.08D;
        double afterglow = phase == MescalinePhase.AFTERGLOW ? envelope * 0.82D : 0.0D;
        double warmth = presence * (0.30D + opening * 0.52D) + afterglow * 0.22D;
        double richness = presence * (0.20D + opening * 0.64D) + afterglow * 0.16D;
        double breathing = presence * (0.075D + opening * 0.38D + high * 0.22D) * slowBreath;
        double organic = presence * (0.08D + opening * 0.48D + high * 0.32D) * signature.organicBias();
        double tessellation = presence * opening * (0.18D + high * 0.82D) * signature.tessellationBias();
        double recursion = complexity * depthWindow * (0.24D + high * 1.06D) * (1.0D + Math.log1p(extreme) * 0.55D);
        double symmetry = presence * depthWindow * (0.14D + high * 0.88D) * signature.mandalaBias();
        double trails = presence * opening * (0.14D + high * 0.50D) * signature.trailPersistence();
        double spatialDepth = presence * opening * (0.16D + high * 0.62D);
        double comeUp = phase == MescalinePhase.COME_UP ? MescalineDoseCurve.smoothstep(0.025D, 0.18D, p) : 0.0D;
        double bodyWarmth = Math.min(0.72D, presence * (0.14D + comeUp * 0.42D));
        double nausea = Math.min(0.20D, comeUp * presence * (0.35D + 0.65D * Math.pow(Math.sin(ageTicks * 0.018D), 2.0D)));
        double music = presence * opening * (0.22D + high * 0.54D) * signature.musicSensitivity();
        return new MescalineEffectState(true, displayedDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks(), phase, p, MescalineDoseCurve.tier(activeDoseMilligrams), repeatedDoses,
                acuteTolerance, intensity, presence, warmth, richness, breathing, organic, tessellation, recursion, symmetry,
                trails, spatialDepth, bodyWarmth, nausea, music, afterglow, signature, environment);
    }

    public boolean isAlive() { return ageTicks < durationTicks; }
    public int remainingTicks() { return Math.max(0, durationTicks - ageTicks); }
    public MescalinePhase phase() { return MescalineDoseCurve.phase(ageTicks, durationTicks); }
    public double progress() { return MescalineDoseCurve.progress(ageTicks, durationTicks); }

    public String debugSummary() {
        MescalineEffectState state = effectState();
        return state.active() ? String.format(Locale.ROOT,
                "mescaline dose=%.0fmg effective=%.0fmg tier=%d phase=%s intensity=%.2f complexity=%.2f acuteTolerance=%.0f%% remaining=%ds",
                state.displayDoseMilligrams(), state.activeDoseMilligrams(), state.tier(), state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(), state.recursiveComplexity(), state.acuteTolerance() * 100.0D, state.remainingTicks() / 20)
                : "mescaline=inactive";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams);
        tag.putDouble("effectiveTargetDoseMilligrams", effectiveTargetDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams);
        tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putInt("ageTicks", ageTicks); tag.putInt("durationTicks", durationTicks); tag.putInt("repeatedDoses", repeatedDoses);
        tag.putInt("environmentTicks", environmentTicks); tag.putDouble("acuteTolerance", acuteTolerance);
        tag.put("signature", signature.save()); tag.put("environment", environment.save());
        return tag;
    }

    public static MescalineSession load(CompoundTag tag) {
        double active = MescalineDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D));
        int savedDuration = Math.max(0, NbtCompat.getIntOr(tag, "durationTicks", 0));
        MescalineSession session = new MescalineSession(active,
                Math.min(savedDuration, MescalineDoseCurve.maximumDurationTicks()),
                MescalineTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature")));
        session.effectiveTargetDoseMilligrams = Math.max(active, MescalineDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "effectiveTargetDoseMilligrams", active)));
        session.targetDoseMilligrams = Math.max(session.effectiveTargetDoseMilligrams, MescalineDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", active)));
        session.displayedDoseMilligrams = MescalineDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", active));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.repeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "repeatedDoses", 0));
        session.environmentTicks = Math.max(0, NbtCompat.getIntOr(tag, "environmentTicks", 0));
        session.acuteTolerance = MescalineDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "acuteTolerance", 0.0D), 0.0D, 0.84D);
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        return session;
    }

    private static int safeDuration(long value) { return (int) Math.min(Integer.MAX_VALUE, Math.max(20L, value)); }
}
