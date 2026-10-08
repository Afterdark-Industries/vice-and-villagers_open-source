package com.trippycraft.substance.twoce;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class TwoCeSession {
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
    private final TwoCeTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();

    private TwoCeSession(double dose, int duration, TwoCeTripSignature signature) {
        activeDoseMilligrams = dose;
        effectiveTargetDoseMilligrams = dose;
        targetDoseMilligrams = dose;
        displayedDoseMilligrams = dose;
        targetContentMilligrams = contentMilligramsForEffect(dose);
        displayedContentMilligrams = targetContentMilligrams;
        durationTicks = duration;
        this.signature = signature;
    }

    public static TwoCeSession start(ServerPlayer player, double doseMilligrams) {
        return start(player, doseMilligrams, contentMilligramsForEffect(doseMilligrams));
    }

    public static TwoCeSession start(ServerPlayer player, double doseMilligrams, double contentMilligrams) {
        double dose = TwoCeDoseCurve.sanitizeDose(doseMilligrams);
        long seed = player == null ? Double.doubleToLongBits(dose) ^ 0x2CE2CE2CE2CEL
                : player.getUUID().getMostSignificantBits() ^ player.level().getGameTime() ^ Double.doubleToLongBits(dose);
        TwoCeSession result = new TwoCeSession(dose, TwoCeDoseCurve.baseDurationTicks(dose), TwoCeTripSignature.create(seed));
        result.targetContentMilligrams = sanitizeContent(contentMilligrams);
        result.displayedContentMilligrams = result.targetContentMilligrams;
        if (player != null) {
            result.environment = PsilocybinEnvironmentSnapshot.capture(player);
        }
        return result;
    }

    public void stackDose(double addedDoseMilligrams) {
        stackDose(addedDoseMilligrams, contentMilligramsForEffect(addedDoseMilligrams));
    }

    public void stackDose(double addedDoseMilligrams, double addedContentMilligrams) {
        double added = TwoCeDoseCurve.sanitizeDose(addedDoseMilligrams);
        if (added <= 0.0D || !isAlive()) {
            return;
        }
        repeatedDoses = Math.min(Integer.MAX_VALUE - 1, repeatedDoses) + 1;
        double efficiency = Math.max(0.32D, 1.0D / (1.0D + repeatedDoses * 0.12D));
        targetDoseMilligrams = TwoCeDoseCurve.safeAdd(targetDoseMilligrams, added);
        targetContentMilligrams = TwoCeDoseCurve.safeAdd(targetContentMilligrams, sanitizeContent(addedContentMilligrams));
        effectiveTargetDoseMilligrams = TwoCeDoseCurve.safeAdd(effectiveTargetDoseMilligrams, added * efficiency);
        acuteTolerance = 1.0D - efficiency;
        int extension = (int) (TwoCeDoseCurve.baseDurationTicks(added) * 0.58D);
        durationTicks = (int) Math.min(TwoCeDoseCurve.maximumDurationTicks(),
                Math.max((long) durationTicks + extension, (long) ageTicks + extension));
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) {
            return;
        }
        ageTicks++;
        activeDoseMilligrams = approach(activeDoseMilligrams, effectiveTargetDoseMilligrams, 0.0016D);
        displayedDoseMilligrams = approach(displayedDoseMilligrams, targetDoseMilligrams, 0.010D);
        displayedContentMilligrams = approach(displayedContentMilligrams, targetContentMilligrams, 0.010D);
        double desired = TwoCeDoseCurve.phaseEnvelope(phase(), progress());
        visualEnvelope += (desired - visualEnvelope) * 0.012D;
        if (player != null && --environmentTicks <= 0) {
            environment = environment.blend(PsilocybinEnvironmentSnapshot.capture(player), 0.14D);
            environmentTicks = 32;
        }
    }

    public TwoCeEffectState effectState() {
        if (!isAlive()) {
            return TwoCeEffectState.inactive();
        }
        double envelope = visualEnvelope * TwoCeDoseCurve.smoothstep(0.0D, 40.0D, remainingTicks());
        double intensity = TwoCeDoseCurve.peakIntensity(activeDoseMilligrams) * envelope;
        double presence = TwoCeDoseCurve.visualPresence(TwoCeDoseCurve.peakIntensity(activeDoseMilligrams)) * envelope;
        double amplitude = presence * TwoCeDoseCurve.visualAmplitude(activeDoseMilligrams);
        double t2 = TwoCeDoseCurve.smoothstep(8.0D, 20.0D, activeDoseMilligrams);
        double t3 = TwoCeDoseCurve.smoothstep(18.0D, 40.0D, activeDoseMilligrams);
        double t4 = TwoCeDoseCurve.smoothstep(35.0D, 80.0D, activeDoseMilligrams);
        double machine = TwoCeDoseCurve.machineLift(activeDoseMilligrams);
        double slow = 0.98D + Math.sin(ageTicks * 0.0024D + (signature.seed() & 511L) * 0.01D) * 0.02D;
        double cavities = amplitude * (0.72D + t2 * 0.40D + t3 * 0.28D) * signature.cavityBias() * slow;
        double nested = amplitude * (0.32D + t2 * 0.50D + t3 * 0.45D + t4 * 0.28D) * signature.recursionBias();
        double tessellation = amplitude * (0.58D + t2 * 0.40D + t3 * 0.28D) * signature.lockBias();
        double edgeLock = amplitude * (0.74D + t2 * 0.35D + t3 * 0.22D) * signature.lockBias();
        double fold = amplitude * (0.22D + t2 * 0.40D + t3 * 0.50D + t4 * 0.40D) * signature.corridorBias();
        double recursion = amplitude * (t2 * 0.28D + t3 * 0.55D + t4 * 0.80D + machine * 0.60D) * signature.recursionBias();
        double corridor = amplitude * (0.22D + t2 * 0.42D + t3 * 0.55D + t4 * 0.50D + machine * 0.45D) * signature.corridorBias();
        double symmetry = presence * (t2 * 0.16D + t3 * 0.42D + t4 * 0.55D + machine * 0.35D) * signature.symmetryBias();
        double trails = amplitude * (0.38D + t2 * 0.40D + t3 * 0.30D) * signature.trailBias();
        double afterglow = TwoCeDoseCurve.smoothstep(0.90D, 0.96D, progress()) * presence;
        double bodyLoad = presence * (0.12D + t3 * 0.22D + t4 * 0.28D);
        return new TwoCeEffectState(true, displayedDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks(), phase(), progress(), TwoCeDoseCurve.tier(activeDoseMilligrams),
                repeatedDoses, acuteTolerance, intensity, presence, cavities, nested, tessellation, edgeLock, fold,
                recursion, corridor, symmetry, trails, afterglow, bodyLoad, signature, environment,
                displayedContentMilligrams, targetContentMilligrams);
    }

    public boolean isAlive() {
        return durationTicks > 0 && ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public double targetDoseMilligrams() {
        return targetDoseMilligrams;
    }

    public TwoCePhase phase() {
        return TwoCeDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        return TwoCeDoseCurve.progress(ageTicks, durationTicks);
    }

    public void setAgeTicks(int newAgeTicks) {
        ageTicks = Math.max(0, Math.min(durationTicks, newAgeTicks));
        visualEnvelope = TwoCeDoseCurve.phaseEnvelope(phase(), progress());
    }

    public String debugSummary() {
        TwoCeEffectState state = effectState();
        return state.active() ? String.format(Locale.ROOT,
                "2c-e dose=%.0fmg effective=%.0fmg phase=%s intensity=%.2f remaining=%ds",
                state.displayDoseMilligrams(), state.activeDoseMilligrams(), state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(), state.remainingTicks() / 20) : "2c-e=inactive";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams);
        tag.putDouble("effectiveTargetDoseMilligrams", effectiveTargetDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams);
        tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putDouble("targetContentMilligrams", targetContentMilligrams);
        tag.putDouble("displayedContentMilligrams", displayedContentMilligrams);
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

    public static TwoCeSession load(CompoundTag tag) {
        double active = TwoCeDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D));
        TwoCeSession session = new TwoCeSession(active,
                Math.max(0, Math.min(TwoCeDoseCurve.maximumDurationTicks(), NbtCompat.getIntOr(tag, "durationTicks", 0))),
                TwoCeTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature")));
        session.effectiveTargetDoseMilligrams = Math.max(active, TwoCeDoseCurve.sanitizeDose(
                NbtCompat.getDoubleOr(tag, "effectiveTargetDoseMilligrams", active)));
        session.targetDoseMilligrams = Math.max(session.effectiveTargetDoseMilligrams,
                TwoCeDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", active)));
        session.displayedDoseMilligrams = TwoCeDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", active));
        // Old saves have only calibrated effects, so an accumulated dose's original content is approximate.
        session.targetContentMilligrams = sanitizeContent(NbtCompat.getDoubleOr(tag, "targetContentMilligrams",
                contentMilligramsForEffect(session.targetDoseMilligrams)));
        session.displayedContentMilligrams = sanitizeContent(NbtCompat.getDoubleOr(tag, "displayedContentMilligrams",
                contentMilligramsForEffect(session.displayedDoseMilligrams)));
        // An old save may carry the former 45-minute clock. Cap both values
        // while preserving elapsed age, so loading cannot restart an expired
        // session or grant a new fifteen-minute allowance.
        session.ageTicks = Math.max(0, Math.min(session.durationTicks, NbtCompat.getIntOr(tag, "ageTicks", 0)));
        session.visualEnvelope = TwoCeDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "visualEnvelope",
                TwoCeDoseCurve.phaseEnvelope(session.phase(), session.progress())), 0.0D, 1.0D);
        session.acuteTolerance = TwoCeDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "acuteTolerance", 0.0D), 0.0D, 0.76D);
        session.repeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "repeatedDoses", 0));
        session.environmentTicks = Math.max(0, NbtCompat.getIntOr(tag, "environmentTicks", 0));
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        return session;
    }

    public static double contentMilligramsForEffect(double effectMilligrams) {
        double effect = TwoCeDoseCurve.sanitizeDose(effectMilligrams);
        if (effect <= 10.0D) return effect * 2.5D;
        if (effect <= 20.0D) return 25.0D + (effect - 10.0D) * 7.5D;
        if (effect <= 40.0D) return 100.0D + (effect - 20.0D) * 7.5D;
        if (effect <= 80.0D) return 250.0D + (effect - 40.0D) * 6.25D;
        return effect > Double.MAX_VALUE / 6.25D ? Double.MAX_VALUE : effect * 6.25D;
    }

    private static double sanitizeContent(double contentMilligrams) {
        return Double.isFinite(contentMilligrams) && contentMilligrams > 0.0D ? contentMilligrams : 0.0D;
    }

    private static double approach(double current, double target, double rate) {
        return current * (1.0D - rate) + target * rate;
    }
}
