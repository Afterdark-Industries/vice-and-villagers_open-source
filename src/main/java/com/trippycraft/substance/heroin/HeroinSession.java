package com.trippycraft.substance.heroin;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public final class HeroinSession {
    private double activeDoseMilligrams;
    private double targetDoseMilligrams;
    private double displayedDoseMilligrams;
    private int ageTicks;
    private int durationTicks;
    private double toleranceProgress;
    private double dependenceProgress;

    private HeroinSession(double doseMilligrams, double toleranceProgress, double dependenceProgress, int durationTicks) {
        this.activeDoseMilligrams = doseMilligrams;
        this.targetDoseMilligrams = doseMilligrams;
        this.displayedDoseMilligrams = doseMilligrams;
        this.toleranceProgress = HeroinDoseCurve.saturate(toleranceProgress);
        this.dependenceProgress = HeroinDoseCurve.saturate(dependenceProgress);
        this.durationTicks = durationTicks;
    }

    public static HeroinSession start(double doseMilligrams, double toleranceProgress, double dependenceProgress) {
        return new HeroinSession(
                doseMilligrams,
                toleranceProgress,
                dependenceProgress,
                HeroinDoseCurve.baseDurationTicks(doseMilligrams, toleranceProgress)
        );
    }

    public void stackDose(double addedDoseMilligrams, double currentToleranceProgress, double currentDependenceProgress) {
        double added = Math.max(0.0D, addedDoseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (!hasActiveEffects()) {
            restart(added, currentToleranceProgress, currentDependenceProgress);
            return;
        }
        toleranceProgress = Math.max(toleranceProgress, HeroinDoseCurve.saturate(currentToleranceProgress));
        dependenceProgress = Math.max(dependenceProgress, HeroinDoseCurve.saturate(currentDependenceProgress));
        double previousTarget = targetDoseMilligrams;
        targetDoseMilligrams += added;
        int targetDuration = HeroinDoseCurve.baseDurationTicks(targetDoseMilligrams, toleranceProgress);
        int extension = HeroinDoseCurve.stackExtensionTicks(added, Math.max(previousTarget, targetDoseMilligrams), toleranceProgress);
        durationTicks = Math.max(durationTicks + extension, Math.max(targetDuration, ageTicks + extension));
    }

    public void restart(double doseMilligrams, double currentToleranceProgress, double currentDependenceProgress) {
        activeDoseMilligrams = doseMilligrams;
        targetDoseMilligrams = doseMilligrams;
        displayedDoseMilligrams = doseMilligrams;
        ageTicks = 0;
        toleranceProgress = HeroinDoseCurve.saturate(currentToleranceProgress);
        dependenceProgress = HeroinDoseCurve.saturate(currentDependenceProgress);
        durationTicks = HeroinDoseCurve.baseDurationTicks(doseMilligrams, toleranceProgress);
    }

    public void tick() {
        if (!hasActiveEffects()) {
            return;
        }
        ageTicks++;
        double doseDelta = Math.abs(targetDoseMilligrams - activeDoseMilligrams);
        double ramp = (targetDoseMilligrams > activeDoseMilligrams ? 0.024D : 0.008D)
            + HeroinDoseCurve.smoothstep(10.0D, 120.0D, doseDelta) * 0.018D;
        activeDoseMilligrams += (targetDoseMilligrams - activeDoseMilligrams) * ramp;
        displayedDoseMilligrams += (targetDoseMilligrams - displayedDoseMilligrams) * 0.014D;
        if (Math.abs(targetDoseMilligrams - activeDoseMilligrams) < 0.05D) {
            activeDoseMilligrams = targetDoseMilligrams;
        }
        if (Math.abs(targetDoseMilligrams - displayedDoseMilligrams) < 0.05D) {
            displayedDoseMilligrams = targetDoseMilligrams;
        }
    }

    public HeroinEffectState effectState() {
        if (!hasActiveEffects()) {
            return HeroinEffectState.inactive();
        }

        HeroinPhase phase = phase();
        double progress = progress();
        int tier = HeroinDoseCurve.tier(activeDoseMilligrams);
        double phaseEnvelope = HeroinDoseCurve.phaseEnvelope(phase, progress, activeDoseMilligrams);
        double doseIntensity = HeroinDoseCurve.peakIntensity(activeDoseMilligrams, toleranceProgress, dependenceProgress);
        double intensity = HeroinDoseCurve.clamp(doseIntensity * phaseEnvelope, 0.0D, 8.2D);

        double low = HeroinDoseCurve.smoothstep(15.0D, 25.0D, activeDoseMilligrams);
        double moderate = HeroinDoseCurve.smoothstep(75.0D, 100.0D, activeDoseMilligrams);
        double strong = HeroinDoseCurve.smoothstep(150.0D, 250.0D, activeDoseMilligrams);
        double nodDose = HeroinDoseCurve.smoothstep(230.0D, 300.0D, activeDoseMilligrams);
        double shutdown = HeroinDoseCurve.smoothstep(300.0D, 500.0D, activeDoseMilligrams);
        double onset = HeroinDoseCurve.smoothstep(0.00D, 0.03D, progress);
        double warmthWindow = HeroinDoseCurve.smoothstep(0.02D, 0.24D, progress) * (1.0D - HeroinDoseCurve.smoothstep(0.74D, 0.92D, progress));
        double blissWindow = HeroinDoseCurve.smoothstep(0.22D, 0.48D, progress) * (1.0D - HeroinDoseCurve.smoothstep(0.64D, 0.84D, progress));
        double comedown = HeroinDoseCurve.smoothstep(0.62D, 0.95D, progress);
        double goodEffectScale = Math.max(0.30D, 1.06D - toleranceProgress * 0.54D - dependenceProgress * 0.08D);
        double maintenanceRelief = 0.22D + dependenceProgress * 0.22D;
        double warmthPhasePresence = phase == HeroinPhase.ONSET || phase == HeroinPhase.SOBER ? 0.0D : 1.0D;

        double nodPulse = Math.pow(Math.max(0.0D, Math.sin(ageTicks * (0.020D + nodDose * 0.012D) + activeDoseMilligrams * 0.018D)), 8.0D);
        double microSleepPulse = Math.pow(Math.max(0.0D, Math.sin(ageTicks * (0.014D + shutdown * 0.010D) + 1.7D)), 12.0D);
        double nodWindow = HeroinDoseCurve.smoothstep(0.34D, 0.54D, progress) * (1.0D - HeroinDoseCurve.smoothstep(0.72D, 0.92D, progress));

        double warmth = intensity * (0.28D + low * 0.32D + moderate * 0.24D + warmthWindow * 0.50D) * goodEffectScale;
        double softFocus = intensity * (0.16D + moderate * 0.22D + strong * 0.28D + warmthWindow * 0.30D + comedown * 0.10D);
        double euphoria = intensity * (0.14D + moderate * 0.30D + strong * 0.38D + blissWindow * 0.50D) * goodEffectScale;
        double sedation = intensity * (0.10D + low * 0.12D + moderate * 0.26D + strong * 0.36D + nodDose * 0.38D + shutdown * 0.32D);
        double painSuppression = intensity * (0.08D + low * 0.14D + moderate * 0.30D + strong * 0.38D + shutdown * 0.24D) * goodEffectScale;
        double hungerSuppression = intensity * (0.05D + moderate * 0.18D + strong * 0.26D + shutdown * 0.12D) * (0.90D + maintenanceRelief);
        double regeneration = intensity * (low * 0.10D + moderate * 0.16D + strong * 0.24D + warmthPhasePresence * 0.22D + warmthWindow * 0.14D + blissWindow * 0.18D) * goodEffectScale;
        double resistance = intensity * (low * 0.08D + moderate * 0.13D + strong * 0.22D + warmthPhasePresence * 0.18D + warmthWindow * 0.10D + painSuppression / 8.2D * 0.16D) * goodEffectScale;
        double absorption = intensity * (low * 0.06D + moderate * 0.10D + strong * 0.18D + warmthPhasePresence * 0.16D + warmthWindow * 0.12D + shutdown * 0.10D) * goodEffectScale;
        double motorDrag = intensity * (low * 0.08D + moderate * 0.20D + strong * 0.34D + nodDose * 0.42D + shutdown * 0.36D);
        double miningDrag = intensity * (moderate * 0.10D + strong * 0.26D + nodDose * 0.34D + shutdown * 0.28D);
        double nodding = intensity * nodWindow * (strong * 0.30D + nodDose * 0.62D + shutdown * 0.38D) * (0.40D + nodPulse * 0.60D);
        double microSleep = intensity * nodWindow * (nodDose * 0.24D + shutdown * 0.66D) * (0.32D + microSleepPulse * 0.68D);
        double respiratorySuppression = intensity * (nodDose * 0.16D + shutdown * 0.46D) * (0.30D + nodWindow * 0.48D + comedown * 0.12D);
        double nauseaRelief = intensity * (low * 0.12D + moderate * 0.24D + strong * 0.22D) * goodEffectScale;
        double appetiteSuppression = hungerSuppression * (0.82D + comedown * 0.20D);

        if (phase == HeroinPhase.ONSET) {
            warmth *= 0.86D + onset * 0.14D;
            euphoria *= 0.54D + onset * 0.34D;
            sedation *= 0.82D;
        }
        if (phase == HeroinPhase.AFTERGLOW) {
            euphoria *= 0.44D;
            regeneration *= 0.36D;
            resistance *= 0.38D;
            absorption *= 0.32D;
        }

        return new HeroinEffectState(
                true,
                displayedDoseMilligrams,
                activeDoseMilligrams,
                targetDoseMilligrams,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                tier,
                intensity,
                warmth,
                softFocus,
                euphoria,
                sedation,
                painSuppression,
                hungerSuppression,
                regeneration,
                resistance,
                absorption,
                motorDrag,
                miningDrag,
                nodding,
                microSleep,
                respiratorySuppression,
                nauseaRelief,
                appetiteSuppression
        );
    }

    public boolean isAlive() {
        return hasActiveEffects();
    }

    public boolean hasActiveEffects() {
        return ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public HeroinPhase phase() {
        return HeroinDoseCurve.phase(ageTicks, durationTicks, activeDoseMilligrams);
    }

    public double progress() {
        return HeroinDoseCurve.progress(ageTicks, durationTicks);
    }

    public double targetDoseMilligrams() {
        return targetDoseMilligrams;
    }

    public double activeDoseMilligrams() {
        return activeDoseMilligrams;
    }

    public String debugSummary() {
        HeroinEffectState state = effectState();
        if (!state.active()) {
            return "heroin=inactive";
        }
        return String.format(Locale.ROOT,
                "heroin dose=%.0fmg tier=%d phase=%s intensity=%.2f nod=%.2f remaining=%ds",
                state.displayDoseMilligrams(),
                state.tier(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.nodding(),
                state.remainingTicks() / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams);
        tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putDouble("toleranceProgress", toleranceProgress);
        tag.putDouble("dependenceProgress", dependenceProgress);
        return tag;
    }

    public static HeroinSession load(CompoundTag tag) {
        double activeDose = NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D);
        double tolerance = NbtCompat.getDoubleOr(tag, "toleranceProgress", 0.0D);
        double dependence = NbtCompat.getDoubleOr(tag, "dependenceProgress", 0.0D);
        HeroinSession session = new HeroinSession(activeDose, tolerance, dependence, NbtCompat.getIntOr(tag, "durationTicks", 0));
        session.targetDoseMilligrams = NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", activeDose);
        session.displayedDoseMilligrams = NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", activeDose);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        int curveDuration = HeroinDoseCurve.baseDurationTicks(session.targetDoseMilligrams, session.toleranceProgress);
        int stackedDurationCap = curveDuration + HeroinDoseCurve.stackExtensionTicks(session.targetDoseMilligrams * 0.60D, session.targetDoseMilligrams, session.toleranceProgress);
        if (session.durationTicks < curveDuration) {
            session.durationTicks = curveDuration;
        } else if (session.durationTicks > stackedDurationCap) {
            session.durationTicks = Math.max(session.ageTicks + 40, stackedDurationCap);
        }
        return session;
    }
}
