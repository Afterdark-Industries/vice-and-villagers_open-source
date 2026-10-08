package com.trippycraft.substance.homet;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class HoMetSession {
    private static final double EFFECT_STRENGTH_MULTIPLIER = 2.0D;

    private double activeDoseMilligrams;
    private double effectiveTargetDoseMilligrams;
    private double targetDoseMilligrams;
    private double displayedDoseMilligrams;
    private double targetContentMilligrams;
    private double displayedContentMilligrams;
    private double visualEnvelope;
    private double acuteTolerance;
    private int ageTicks;
    private int durationTicks;
    private int repeatedDoses;
    private int environmentTicks;
    private final HoMetTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();

    private HoMetSession(double dose, int duration, HoMetTripSignature signature) {
        activeDoseMilligrams = dose; effectiveTargetDoseMilligrams = dose;
        targetDoseMilligrams = dose; displayedDoseMilligrams = dose;
        targetContentMilligrams = contentMilligramsForEffect(dose);
        displayedContentMilligrams = targetContentMilligrams;
        durationTicks = duration; this.signature = signature;
    }

    public static HoMetSession start(ServerPlayer player, double doseMilligrams) {
        return start(player, doseMilligrams, contentMilligramsForEffect(doseMilligrams));
    }

    public static HoMetSession start(ServerPlayer player, double doseMilligrams, double contentMilligrams) {
        double dose = HoMetDoseCurve.sanitizeDose(doseMilligrams);
        long seed = player == null ? Double.doubleToLongBits(dose) ^ 0x484F4D455434L
                : player.getUUID().getMostSignificantBits() ^ player.level().getGameTime() ^ Double.doubleToLongBits(dose);
        HoMetSession result = new HoMetSession(dose, HoMetDoseCurve.baseDurationTicks(dose), HoMetTripSignature.create(seed));
        result.targetContentMilligrams = sanitizeContent(contentMilligrams);
        result.displayedContentMilligrams = result.targetContentMilligrams;
        if (player != null) result.environment = PsilocybinEnvironmentSnapshot.capture(player);
        return result;
    }

    public void stackDose(double addedDoseMilligrams) {
        stackDose(addedDoseMilligrams, contentMilligramsForEffect(addedDoseMilligrams));
    }

    public void stackDose(double addedDoseMilligrams, double addedContentMilligrams) {
        double added = HoMetDoseCurve.sanitizeDose(addedDoseMilligrams);
        if (added <= 0.0D) return;
        repeatedDoses = Math.min(Integer.MAX_VALUE - 1, repeatedDoses) + 1;
        double efficiency = Math.max(0.24D, 1.0D / (1.0D + repeatedDoses * 0.16D));
        targetDoseMilligrams = HoMetDoseCurve.safeAdd(targetDoseMilligrams, added);
        targetContentMilligrams = HoMetDoseCurve.safeAdd(targetContentMilligrams, sanitizeContent(addedContentMilligrams));
        effectiveTargetDoseMilligrams = HoMetDoseCurve.safeAdd(effectiveTargetDoseMilligrams, added * efficiency);
        acuteTolerance = 1.0D - efficiency;
        int extension = (int) (HoMetDoseCurve.baseDurationTicks(added) * 0.50D);
        durationTicks = (int) Math.min(HoMetDoseCurve.maximumDurationTicks(),
                Math.max((long) durationTicks + extension, (long) ageTicks + extension));
        // Preserve visualEnvelope and activeDose here: redosing never changes a rendered channel immediately.
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) return;
        ageTicks++;
        activeDoseMilligrams = approach(activeDoseMilligrams, effectiveTargetDoseMilligrams, 0.0018D);
        displayedDoseMilligrams = approach(displayedDoseMilligrams, targetDoseMilligrams, 0.012D);
        displayedContentMilligrams = approach(displayedContentMilligrams, targetContentMilligrams, 0.012D);
        double desired = HoMetDoseCurve.phaseEnvelope(phase(), progress());
        visualEnvelope += (desired - visualEnvelope) * 0.018D;
        if (player != null && --environmentTicks <= 0) {
            environment = environment.blend(PsilocybinEnvironmentSnapshot.capture(player), 0.14D);
            environmentTicks = 30;
        }
    }

    public HoMetEffectState effectState() {
        if (!isAlive()) return HoMetEffectState.inactive();
        // The last two seconds smoothly close the small lag left by envelope interpolation.
        double envelope = visualEnvelope * HoMetDoseCurve.smoothstep(0.0D, 40.0D, remainingTicks());
        double intensity = HoMetDoseCurve.peakIntensity(activeDoseMilligrams) * envelope;
        // The bounded presence is only an onset/offset gate. Render amplitudes continue growing with dose.
        double presence = HoMetDoseCurve.visualPresence(HoMetDoseCurve.peakIntensity(activeDoseMilligrams)) * envelope;
        double amplitude = presence * HoMetDoseCurve.visualAmplitude(activeDoseMilligrams);
        double complexity = HoMetDoseCurve.complexity(activeDoseMilligrams) * envelope;
        double detail = HoMetDoseCurve.visualPresence(complexity * 0.45D);
        double slowBreath = 0.96D + Math.sin(ageTicks * 0.0045D * signature.breathingRhythm()
                + (signature.seed() & 1023L) * 0.007D) * 0.04D;
        double depth = amplitude * (0.42D + detail * 0.56D) * signature.depthBias();
        double deformation = amplitude * (0.18D + detail * 0.77D) * slowBreath;
        double trails = amplitude * (0.38D + detail * 0.61D) * signature.trailPersistence();
        double recursion = amplitude * detail * signature.recursionBias();
        double color = amplitude * (0.58D + detail * 0.42D) * signature.crystallineBias();
        double perspective = amplitude * (0.15D + detail * 0.70D) * signature.depthBias();
        double afterglow = HoMetDoseCurve.smoothstep(0.90D, 0.96D, progress()) * presence;
        // Amplify the finished effect once, preserving dose calibration, absorption, and trip timing.
        return new HoMetEffectState(true, displayedDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks(), phase(), progress(), HoMetDoseCurve.tier(activeDoseMilligrams),
                repeatedDoses, acuteTolerance, intensity, presence, depth, deformation, trails, recursion,
                color, perspective, complexity, presence * (0.25D + detail * 0.42D), afterglow, signature, environment,
                displayedContentMilligrams, targetContentMilligrams).scaled(EFFECT_STRENGTH_MULTIPLIER);
    }

    public boolean isAlive() { return durationTicks > 0 && ageTicks < durationTicks; }
    public int remainingTicks() { return Math.max(0, durationTicks - ageTicks); }
    public double targetDoseMilligrams() { return targetDoseMilligrams; }
    public double targetContentMilligrams() { return targetContentMilligrams; }
    public HoMetPhase phase() { return HoMetDoseCurve.phase(ageTicks, durationTicks); }
    public double progress() { return HoMetDoseCurve.progress(ageTicks, durationTicks); }

    public void setAgeTicks(int newAgeTicks) {
        ageTicks = Math.max(0, Math.min(durationTicks, newAgeTicks));
        visualEnvelope = HoMetDoseCurve.phaseEnvelope(phase(), progress());
    }

    public String debugSummary() {
        HoMetEffectState state = effectState();
        return state.active() ? String.format(Locale.ROOT,
                "4-ho-met dose=%.1fmg effective=%.1fmg phase=%s intensity=%.2f complexity=%.2f remaining=%ds",
                state.displayDoseMilligrams(), state.activeDoseMilligrams(), state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(), state.complexity(), state.remainingTicks() / 20) : "4-ho-met=inactive";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams); tag.putDouble("effectiveTargetDoseMilligrams", effectiveTargetDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams); tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putDouble("targetContentMilligrams", targetContentMilligrams); tag.putDouble("displayedContentMilligrams", displayedContentMilligrams);
        tag.putDouble("visualEnvelope", visualEnvelope); tag.putDouble("acuteTolerance", acuteTolerance);
        tag.putInt("ageTicks", ageTicks); tag.putInt("durationTicks", durationTicks); tag.putInt("repeatedDoses", repeatedDoses);
        tag.putInt("environmentTicks", environmentTicks); tag.put("signature", signature.save()); tag.put("environment", environment.save());
        return tag;
    }

    public static HoMetSession load(CompoundTag tag) {
        double active = HoMetDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D));
        HoMetSession session = new HoMetSession(active,
                Math.max(0, Math.min(HoMetDoseCurve.maximumDurationTicks(), NbtCompat.getIntOr(tag, "durationTicks", 0))),
                HoMetTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature")));
        session.effectiveTargetDoseMilligrams = Math.max(active, HoMetDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "effectiveTargetDoseMilligrams", active)));
        session.targetDoseMilligrams = Math.max(session.effectiveTargetDoseMilligrams, HoMetDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", active)));
        session.displayedDoseMilligrams = HoMetDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", active));
        // Older saves recorded effect quantities only. Inferring a stacked session's original contents is approximate.
        session.targetContentMilligrams = sanitizeContent(NbtCompat.getDoubleOr(tag, "targetContentMilligrams",
                contentMilligramsForEffect(session.targetDoseMilligrams)));
        session.displayedContentMilligrams = sanitizeContent(NbtCompat.getDoubleOr(tag, "displayedContentMilligrams",
                contentMilligramsForEffect(session.displayedDoseMilligrams)));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.visualEnvelope = HoMetDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "visualEnvelope",
                HoMetDoseCurve.phaseEnvelope(session.phase(), session.progress())), 0.0D, 1.0D);
        session.acuteTolerance = HoMetDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "acuteTolerance", 0.0D), 0.0D, 0.76D);
        session.repeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "repeatedDoses", 0));
        session.environmentTicks = Math.max(0, NbtCompat.getIntOr(tag, "environmentTicks", 0));
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        return session;
    }

    public static double contentMilligramsForEffect(double effectMilligrams) {
        double effect = HoMetDoseCurve.sanitizeDose(effectMilligrams);
        if (effect <= 5.0D) return effect * 5.0D;
        if (effect <= 10.0D) return 25.0D + (effect - 5.0D) * 15.0D;
        if (effect <= 20.0D) return 100.0D + (effect - 10.0D) * 15.0D;
        if (effect <= 40.0D) return 250.0D + (effect - 20.0D) * 12.5D;
        return effect > Double.MAX_VALUE / 12.5D ? Double.MAX_VALUE : effect * 12.5D;
    }

    private static double sanitizeContent(double contentMilligrams) {
        return Double.isFinite(contentMilligrams) && contentMilligrams > 0.0D ? contentMilligrams : 0.0D;
    }

    private static double approach(double current, double target, double rate) {
        // Convex interpolation avoids overflow at the largest representable game dose.
        return current * (1.0D - rate) + target * rate;
    }
}
