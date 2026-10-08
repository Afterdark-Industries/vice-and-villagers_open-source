package com.trippycraft.substance.allad;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class AlLadSession {
    private double activeDoseMicrograms;
    private double effectiveTargetDoseMicrograms;
    private double targetDoseMicrograms;
    private double displayedDoseMicrograms;
    private double displayedContentMilligrams;
    private double targetContentMilligrams;
    private double visualEnvelope;
    private double acuteTolerance;
    private int ageTicks;
    private int durationTicks;
    private int repeatedDoses;
    private int environmentTicks;
    private final AlLadTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();

    private AlLadSession(double dose, double contentMilligrams, int duration, AlLadTripSignature signature) {
        activeDoseMicrograms = dose;
        effectiveTargetDoseMicrograms = dose;
        targetDoseMicrograms = dose;
        displayedDoseMicrograms = dose;
        displayedContentMilligrams = contentMilligrams;
        targetContentMilligrams = contentMilligrams;
        durationTicks = duration;
        this.signature = signature;
    }

    public static AlLadSession start(ServerPlayer player, double doseMicrograms) {
        return start(player, doseMicrograms, contentMilligramsForEffect(doseMicrograms));
    }

    public static AlLadSession start(ServerPlayer player, double doseMicrograms, double contentMilligrams) {
        double dose = AlLadDoseCurve.sanitizeDose(doseMicrograms);
        long seed = player == null ? Double.doubleToLongBits(dose) ^ 0xA11AD5A11AD5L
                : player.getUUID().getMostSignificantBits() ^ player.level().getGameTime() ^ Double.doubleToLongBits(dose);
        AlLadSession result = new AlLadSession(dose, sanitizeContentMilligrams(contentMilligrams),
                AlLadDoseCurve.baseDurationTicks(dose), AlLadTripSignature.create(seed));
        if (player != null) {
            result.environment = PsilocybinEnvironmentSnapshot.capture(player);
        }
        return result;
    }

    public void stackDose(double addedDoseMicrograms) {
        stackDose(addedDoseMicrograms, contentMilligramsForEffect(addedDoseMicrograms));
    }

    public void stackDose(double addedDoseMicrograms, double addedContentMilligrams) {
        double added = AlLadDoseCurve.sanitizeDose(addedDoseMicrograms);
        if (added <= 0.0D) {
            return;
        }
        repeatedDoses = Math.min(Integer.MAX_VALUE - 1, repeatedDoses) + 1;
        double efficiency = Math.max(0.28D, 1.0D / (1.0D + repeatedDoses * 0.14D));
        targetDoseMicrograms = AlLadDoseCurve.safeAdd(targetDoseMicrograms, added);
        targetContentMilligrams = safeAddContentMilligrams(targetContentMilligrams, addedContentMilligrams);
        effectiveTargetDoseMicrograms = AlLadDoseCurve.safeAdd(effectiveTargetDoseMicrograms, added * efficiency);
        acuteTolerance = 1.0D - efficiency;
        int extension = (int) (AlLadDoseCurve.baseDurationTicks(added) * 0.52D);
        durationTicks = (int) Math.min(AlLadDoseCurve.maximumDurationTicks(),
                Math.max((long) durationTicks + extension, (long) ageTicks + extension));
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) {
            return;
        }
        ageTicks++;
        activeDoseMicrograms = approach(activeDoseMicrograms, effectiveTargetDoseMicrograms, 0.0022D);
        displayedDoseMicrograms = approach(displayedDoseMicrograms, targetDoseMicrograms, 0.012D);
        displayedContentMilligrams = approach(displayedContentMilligrams, targetContentMilligrams, 0.012D);
        double desired = AlLadDoseCurve.phaseEnvelope(phase(), progress());
        visualEnvelope += (desired - visualEnvelope) * 0.016D;
        if (player != null && --environmentTicks <= 0) {
            environment = environment.blend(PsilocybinEnvironmentSnapshot.capture(player), 0.16D);
            environmentTicks = 28;
        }
    }

    public AlLadEffectState effectState() {
        if (!isAlive()) {
            return AlLadEffectState.inactive();
        }
        double envelope = visualEnvelope * AlLadDoseCurve.smoothstep(0.0D, 40.0D, remainingTicks());
        double intensity = AlLadDoseCurve.peakIntensity(activeDoseMicrograms) * envelope;
        double presence = AlLadDoseCurve.visualPresence(AlLadDoseCurve.peakIntensity(activeDoseMicrograms)) * envelope;
        double amplitude = presence * AlLadDoseCurve.visualAmplitude(activeDoseMicrograms);
        double tier2 = AlLadDoseCurve.smoothstep(70.0D, 150.0D, activeDoseMicrograms);
        double tier3 = AlLadDoseCurve.smoothstep(140.0D, 300.0D, activeDoseMicrograms);
        double tier4 = AlLadDoseCurve.smoothstep(280.0D, 450.0D, activeDoseMicrograms);
        double slowBreath = 0.97D + Math.sin(ageTicks * 0.0038D * signature.paneRhythm()
                + (signature.seed() & 1023L) * 0.006D) * 0.03D;
        double stainedGlass = amplitude * (0.52D + tier2 * 0.40D + tier3 * 0.22D) * signature.jewelBias() * slowBreath;
        double prismDepth = amplitude * (0.32D + tier2 * 0.34D + tier3 * 0.36D + tier4 * 0.28D) * signature.prismBias();
        double paneMotion = amplitude * (0.22D + tier2 * 0.36D + tier3 * 0.30D) * signature.paneRhythm();
        double chromatic = amplitude * (0.34D + tier2 * 0.40D + tier3 * 0.28D) * signature.prismBias();
        double heroic = AlLadDoseCurve.heroicLift(activeDoseMicrograms);
        double recursion = amplitude * (tier2 * 0.16D + tier3 * 0.48D + tier4 * 0.72D + heroic * 0.55D)
                * signature.recursionBias();
        double jewel = amplitude * (0.72D + tier2 * 0.30D + environment.light() * 0.16D) * signature.jewelBias();
        double music = presence * (0.28D + tier2 * 0.40D + environment.village() * 0.18D) * signature.musicBias();
        double afterglow = AlLadDoseCurve.smoothstep(0.90D, 0.96D, progress()) * presence;
        double stimulation = presence * (0.18D + tier2 * 0.16D + tier3 * 0.10D);
        double bodyLoad = presence * (0.06D + tier3 * 0.18D + tier4 * 0.22D);
        return new AlLadEffectState(true, displayedDoseMicrograms, activeDoseMicrograms, targetDoseMicrograms,
                ageTicks, durationTicks, remainingTicks(), phase(), progress(), AlLadDoseCurve.tier(activeDoseMicrograms),
                repeatedDoses, acuteTolerance, intensity, presence, stainedGlass, prismDepth, paneMotion, chromatic,
                recursion, jewel, music, afterglow, stimulation, bodyLoad, signature, environment,
                displayedContentMilligrams, targetContentMilligrams);
    }

    public boolean isAlive() {
        return durationTicks > 0 && ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public double targetDoseMicrograms() {
        return targetDoseMicrograms;
    }

    public double targetContentMilligrams() {
        return targetContentMilligrams;
    }

    /** Approximate physical content only when a legacy caller or save has no explicit content. */
    public static double contentMilligramsForEffect(double doseMicrograms) {
        double dose = AlLadDoseCurve.sanitizeDose(doseMicrograms);
        if (dose <= 75.0D) {
            return dose / 75.0D * 25.0D;
        }
        if (dose <= 150.0D) {
            return 25.0D + (dose - 75.0D) / 75.0D * 75.0D;
        }
        if (dose <= 300.0D) {
            return 100.0D + (dose - 150.0D) / 150.0D * 150.0D;
        }
        if (dose <= 450.0D) {
            return 250.0D + (dose - 300.0D) / 150.0D * 250.0D;
        }
        return dose > Double.MAX_VALUE / (500.0D / 450.0D)
                ? Double.MAX_VALUE : dose / 450.0D * 500.0D;
    }

    public AlLadPhase phase() {
        return AlLadDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        return AlLadDoseCurve.progress(ageTicks, durationTicks);
    }

    public void setAgeTicks(int newAgeTicks) {
        ageTicks = Math.max(0, Math.min(durationTicks, newAgeTicks));
        visualEnvelope = AlLadDoseCurve.phaseEnvelope(phase(), progress());
    }

    public String debugSummary() {
        AlLadEffectState state = effectState();
        return state.active() ? String.format(Locale.ROOT,
                "al-lad dose=%.0fug effective=%.0fug phase=%s intensity=%.2f glass=%.2f remaining=%ds",
                state.displayDoseMicrograms(), state.activeDoseMicrograms(), state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(), state.stainedGlass(), state.remainingTicks() / 20) : "al-lad=inactive";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMicrograms", activeDoseMicrograms);
        tag.putDouble("effectiveTargetDoseMicrograms", effectiveTargetDoseMicrograms);
        tag.putDouble("targetDoseMicrograms", targetDoseMicrograms);
        tag.putDouble("displayedDoseMicrograms", displayedDoseMicrograms);
        tag.putDouble("displayedContentMilligrams", displayedContentMilligrams);
        tag.putDouble("targetContentMilligrams", targetContentMilligrams);
        tag.putDouble("visualEnvelope", visualEnvelope);
        tag.putDouble("acuteTolerance", acuteTolerance);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("repeatedDoses", repeatedDoses);
        tag.putInt("environmentTicks", environmentTicks);
        tag.put("signature", signature.save());
        tag.put("environment", environment.save());
        return tag;
    }

    public static AlLadSession load(CompoundTag tag) {
        double active = AlLadDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "activeDoseMicrograms", 0.0D));
        AlLadSession session = new AlLadSession(active, contentMilligramsForEffect(active),
                Math.max(0, Math.min(AlLadDoseCurve.maximumDurationTicks(), NbtCompat.getIntOr(tag, "durationTicks", 0))),
                AlLadTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature")));
        session.effectiveTargetDoseMicrograms = Math.max(active, AlLadDoseCurve.sanitizeDose(
                NbtCompat.getDoubleOr(tag, "effectiveTargetDoseMicrograms", active)));
        session.targetDoseMicrograms = Math.max(session.effectiveTargetDoseMicrograms,
                AlLadDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "targetDoseMicrograms", active)));
        session.displayedDoseMicrograms = AlLadDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "displayedDoseMicrograms", active));
        session.targetContentMilligrams = sanitizeContentMilligrams(NbtCompat.getDoubleOr(tag, "targetContentMilligrams",
                contentMilligramsForEffect(session.targetDoseMicrograms)));
        session.displayedContentMilligrams = sanitizeContentMilligrams(NbtCompat.getDoubleOr(tag, "displayedContentMilligrams",
                contentMilligramsForEffect(session.displayedDoseMicrograms)));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.visualEnvelope = AlLadDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "visualEnvelope",
                AlLadDoseCurve.phaseEnvelope(session.phase(), session.progress())), 0.0D, 1.0D);
        session.acuteTolerance = AlLadDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "acuteTolerance", 0.0D), 0.0D, 0.76D);
        session.repeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "repeatedDoses", 0));
        session.environmentTicks = Math.max(0, NbtCompat.getIntOr(tag, "environmentTicks", 0));
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        return session;
    }

    private static double sanitizeContentMilligrams(double contentMilligrams) {
        return Double.isFinite(contentMilligrams) && contentMilligrams > 0.0D ? contentMilligrams : 0.0D;
    }

    private static double safeAddContentMilligrams(double current, double added) {
        current = sanitizeContentMilligrams(current);
        added = sanitizeContentMilligrams(added);
        return current > Double.MAX_VALUE - added ? Double.MAX_VALUE : current + added;
    }

    private static double approach(double current, double target, double rate) {
        return current * (1.0D - rate) + target * rate;
    }
}
