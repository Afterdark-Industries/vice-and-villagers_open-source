package com.trippycraft.substance.mdma;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public final class MdmaSession {
    private double activeDoseMilligrams;
    private double targetDoseMilligrams;
    private double displayedDoseMilligrams;
    private int ageTicks;
    private int durationTicks;
    private int repeatedDoses;
    private int recoveryTicks;
    private int recoveryDurationTicks;
    private double recoveryDoseMilligrams;
    private int recoveryRepeatedDoses;
    private double settingMultiplier = 1.0D;

    private MdmaSession(double doseMilligrams, int durationTicks) {
        this.activeDoseMilligrams = doseMilligrams;
        this.targetDoseMilligrams = doseMilligrams;
        this.displayedDoseMilligrams = doseMilligrams;
        this.durationTicks = durationTicks;
    }

    public static MdmaSession start(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        return new MdmaSession(dose, MdmaDoseCurve.baseDurationTicks(dose));
    }

    public void stackDose(double addedDoseMilligrams) {
        double added = Math.max(0.0D, addedDoseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (!tripActive()) {
            if (recoveryTicks > 0) {
                beginTrip(added, true);
                return;
            }
            restart(added);
            return;
        }
        repeatedDoses++;
        targetDoseMilligrams += added;
        int extension = MdmaDoseCurve.stackExtensionTicks(added, targetDoseMilligrams);
        durationTicks = Math.max(durationTicks + extension, ageTicks + extension);
    }

    public void restart(double doseMilligrams) {
        recoveryTicks = 0;
        recoveryDurationTicks = 0;
        recoveryDoseMilligrams = 0.0D;
        recoveryRepeatedDoses = 0;
        beginTrip(doseMilligrams, false);
    }

    private void beginTrip(double doseMilligrams, boolean preserveRecovery) {
        activeDoseMilligrams = doseMilligrams;
        targetDoseMilligrams = doseMilligrams;
        displayedDoseMilligrams = doseMilligrams;
        ageTicks = 0;
        durationTicks = MdmaDoseCurve.baseDurationTicks(doseMilligrams);
        repeatedDoses = preserveRecovery ? Math.min(8, Math.max(repeatedDoses, recoveryRepeatedDoses) + 1) : 0;
    }

    public void tick() {
        if (!isAlive()) {
            return;
        }
        if (tripActive()) {
            ageTicks++;
            double delta = Math.abs(targetDoseMilligrams - activeDoseMilligrams);
            double ramp = 0.0060D + MdmaDoseCurve.smoothstep(25.0D, 250.0D, delta) * 0.0050D;
            activeDoseMilligrams += (targetDoseMilligrams - activeDoseMilligrams) * ramp;
            displayedDoseMilligrams += (targetDoseMilligrams - displayedDoseMilligrams) * 0.014D;
            if (Math.abs(targetDoseMilligrams - activeDoseMilligrams) < 0.02D) {
                activeDoseMilligrams = targetDoseMilligrams;
            }
            if (Math.abs(targetDoseMilligrams - displayedDoseMilligrams) < 0.02D) {
                displayedDoseMilligrams = targetDoseMilligrams;
            }
            if (!tripActive()) {
                queueRecovery();
            }
            return;
        }

        recoveryTicks = Math.max(0, recoveryTicks - 1);
        if (recoveryTicks <= 0) {
            clearRecovery();
        }
    }

    public void setSettingMultiplier(double multiplier) {
        settingMultiplier = multiplier >= 1.5D ? 1.5D : 1.0D;
    }

    public double settingMultiplier() {
        return settingMultiplier;
    }

    public MdmaEffectState effectState() {
        if (!isAlive()) {
            return MdmaEffectState.inactive();
        }
        if (!tripActive()) {
            return recoveryEffectState();
        }
        double progress = MdmaDoseCurve.progress(ageTicks, durationTicks);
        MdmaPhase phase = MdmaDoseCurve.phase(ageTicks, durationTicks);
        double phaseEnvelope = MdmaDoseCurve.phaseEnvelope(phase, progress);
        double intensity = MdmaDoseCurve.peakIntensity(activeDoseMilligrams) * phaseEnvelope;
        double repeatPenalty = MdmaDoseCurve.clamp(repeatedDoses * 0.18D, 0.0D, 0.72D);
        double euphoriaScale = 1.0D - repeatPenalty * 0.72D;

        double comeUp = MdmaDoseCurve.smoothstep(0.00D, 0.15D, progress)
                * (1.0D - MdmaDoseCurve.smoothstep(0.20D, 0.32D, progress));
        double peak = MdmaDoseCurve.smoothstep(0.10D, 0.18D, progress)
                * (1.0D - MdmaDoseCurve.smoothstep(0.55D, 0.67D, progress));
        double roll = MdmaDoseCurve.smoothstep(0.44D, 0.58D, progress)
                * (1.0D - MdmaDoseCurve.smoothstep(0.94D, 1.00D, progress));
        double highDose = MdmaDoseCurve.smoothstep(100.0D, 500.0D, activeDoseMilligrams);
        double healingDose = MdmaDoseCurve.smoothstep(75.0D, 250.0D, activeDoseMilligrams);

        double warmth = intensity * (0.18D + comeUp * 0.28D + peak * 0.48D + roll * 0.24D) * euphoriaScale;
        double saturation = intensity * (0.12D + comeUp * 0.18D + peak * 0.62D + roll * 0.36D + highDose * peak * 0.18D) * euphoriaScale;
        double bloom = intensity * (0.08D + peak * 0.58D + roll * 0.30D + highDose * peak * 0.18D) * euphoriaScale;
        double bassSensitivity = intensity * (0.12D + peak * 0.90D + roll * 0.44D + highDose * peak * 0.16D) * euphoriaScale;
        double fovBreathing = intensity * (0.05D + comeUp * 0.08D + peak * 0.28D + roll * 0.18D) * euphoriaScale;
        double movementLift = intensity * (0.08D + comeUp * 0.16D + peak * 0.24D + roll * 0.20D) * euphoriaScale;
        double hungerSuppression = intensity * (0.06D + peak * 0.18D + roll * 0.16D) * euphoriaScale;
        double regeneration = intensity * healingDose * peak * 0.24D * euphoriaScale;
        double socialAura = intensity * (0.08D + comeUp * 0.20D + peak * 0.52D + roll * 0.42D) * euphoriaScale;
        double touchAffinity = intensity * (0.05D + peak * 0.42D + roll * 0.38D) * euphoriaScale;
        double audioFullness = intensity * (0.12D + comeUp * 0.22D + peak * 0.72D + roll * 0.38D + highDose * peak * 0.16D) * euphoriaScale;
        double euphoria = intensity * (0.12D + comeUp * 0.24D + peak * 0.60D + roll * 0.44D) * euphoriaScale;
        return new MdmaEffectState(
                true, displayedDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks(), true, recoveryTicks, recoveryDurationTicks,
                phase, progress, MdmaDoseCurve.tier(activeDoseMilligrams),
                repeatedDoses, intensity, warmth, saturation, bloom, bassSensitivity, fovBreathing, movementLift,
                hungerSuppression, regeneration, socialAura, touchAffinity, audioFullness, euphoria,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, repeatPenalty
        );
    }

    private MdmaEffectState recoveryEffectState() {
        double progress = recoveryDurationTicks <= 0
                ? 1.0D
                : MdmaDoseCurve.clamp(1.0D - recoveryTicks / (double) recoveryDurationTicks, 0.0D, 1.0D);
        double doseFactor = MdmaDoseCurve.smoothstep(25.0D, 500.0D, recoveryDoseMilligrams);
        double repeatPenalty = MdmaDoseCurve.clamp(recoveryRepeatedDoses * 0.18D, 0.0D, 0.86D);
        double rampIn = MdmaDoseCurve.smoothstep(0.0D, 0.14D, progress);
        double rampOut = 1.0D - MdmaDoseCurve.smoothstep(0.45D, 1.0D, progress);
        double envelope = (0.16D + rampIn * 0.84D) * rampOut;
        double desaturation = envelope * (0.18D + doseFactor * 0.32D + repeatPenalty * 0.25D);
        double fatigue = envelope * (0.16D + doseFactor * 0.34D + repeatPenalty * 0.28D);
        double muffle = envelope * (0.12D + doseFactor * 0.32D + repeatPenalty * 0.22D);
        MdmaPhase phase = progress < 0.45D ? MdmaPhase.COMEDOWN : MdmaPhase.HANGOVER;

        return new MdmaEffectState(
                true, recoveryDoseMilligrams, recoveryDoseMilligrams, recoveryDoseMilligrams,
                durationTicks, durationTicks, 0, false, recoveryTicks, recoveryDurationTicks,
                phase, 1.0D, MdmaDoseCurve.tier(recoveryDoseMilligrams), recoveryRepeatedDoses,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, desaturation, fatigue, muffle, 0.0D, 0.0D, repeatPenalty
        );
    }

    public boolean isAlive() {
        return tripActive() || recoveryTicks > 0;
    }

    public boolean tripActive() {
        return ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public void forceEndTrip() {
        if (!tripActive()) {
            return;
        }
        ageTicks = durationTicks;
        activeDoseMilligrams = targetDoseMilligrams;
        displayedDoseMilligrams = targetDoseMilligrams;
        queueRecovery();
    }

    public void onSleep() {
        forceEndTrip();
        recoveryTicks = Math.max(0, recoveryTicks - MdmaDoseCurve.MINECRAFT_DAY_TICKS / 2);
        if (recoveryTicks <= 0) {
            clearRecovery();
        }
    }

    public String debugSummary() {
        MdmaEffectState state = effectState();
        if (!state.active()) {
            return "mdma=inactive";
        }
        int remaining = state.tripActive() ? state.remainingTicks() : state.recoveryTicks();
        return String.format(Locale.ROOT, "mdma dose=%.0fmg phase=%s intensity=%.2f repeats=%d remaining=%ds",
                state.displayDoseMilligrams(), state.phase().name().toLowerCase(Locale.ROOT), state.intensity(),
                state.repeatedDoses(), remaining / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams);
        tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("repeatedDoses", repeatedDoses);
        tag.putInt("recoveryTicks", recoveryTicks);
        tag.putInt("recoveryDurationTicks", recoveryDurationTicks);
        tag.putDouble("recoveryDoseMilligrams", recoveryDoseMilligrams);
        tag.putInt("recoveryRepeatedDoses", recoveryRepeatedDoses);
        return tag;
    }

    public static MdmaSession load(CompoundTag tag) {
        double activeDose = NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D);
        MdmaSession session = new MdmaSession(activeDose, NbtCompat.getIntOr(tag, "durationTicks", 0));
        session.targetDoseMilligrams = NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", activeDose);
        session.displayedDoseMilligrams = NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", activeDose);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        session.repeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "repeatedDoses", 0));
        session.recoveryTicks = Math.max(0, NbtCompat.getIntOr(tag, "recoveryTicks", 0));
        session.recoveryDurationTicks = Math.max(session.recoveryTicks, NbtCompat.getIntOr(tag, "recoveryDurationTicks", session.recoveryTicks));
        session.recoveryDoseMilligrams = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "recoveryDoseMilligrams", 0.0D));
        session.recoveryRepeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "recoveryRepeatedDoses", session.repeatedDoses));
        int minimumDuration = MdmaDoseCurve.baseDurationTicks(session.targetDoseMilligrams);
        session.durationTicks = Math.max(session.durationTicks, Math.min(minimumDuration, session.ageTicks + minimumDuration));
        return session;
    }

    private void queueRecovery() {
        int addedTicks = MdmaDoseCurve.recoveryDurationTicks(targetDoseMilligrams, repeatedDoses);
        recoveryTicks = Math.min(MdmaDoseCurve.MAX_RECOVERY_TICKS, recoveryTicks + addedTicks);
        recoveryDurationTicks = recoveryTicks;
        recoveryDoseMilligrams = Math.min(2_000.0D, recoveryDoseMilligrams + targetDoseMilligrams);
        recoveryRepeatedDoses = Math.max(recoveryRepeatedDoses, repeatedDoses);
    }

    private void clearRecovery() {
        recoveryTicks = 0;
        recoveryDurationTicks = 0;
        recoveryDoseMilligrams = 0.0D;
        recoveryRepeatedDoses = 0;
    }
}
