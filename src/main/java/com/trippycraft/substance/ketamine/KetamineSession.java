package com.trippycraft.substance.ketamine;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public final class KetamineSession {
    private double activeDoseMilligrams;
    private double targetDoseMilligrams;
    private double displayedDoseMilligrams;
    private int ageTicks;
    private int durationTicks;

    private KetamineSession(double doseMilligrams, int durationTicks) {
        this.activeDoseMilligrams = doseMilligrams;
        this.targetDoseMilligrams = doseMilligrams;
        this.displayedDoseMilligrams = doseMilligrams;
        this.durationTicks = durationTicks;
    }

    public static KetamineSession start(double doseMilligrams) {
        return new KetamineSession(doseMilligrams, KetamineDoseCurve.baseDurationTicks(doseMilligrams));
    }

    public void stackDose(double addedDoseMilligrams) {
        double added = Math.max(0.0D, addedDoseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (!hasActiveEffects()) {
            restart(added);
            return;
        }
        double previousTarget = targetDoseMilligrams;
        targetDoseMilligrams += added;
        int targetDuration = KetamineDoseCurve.baseDurationTicks(targetDoseMilligrams);
        int extension = KetamineDoseCurve.stackExtensionTicks(added, Math.max(previousTarget, targetDoseMilligrams));
        durationTicks = Math.max(durationTicks + extension, Math.max(targetDuration, ageTicks + extension));
    }

    public void restart(double doseMilligrams) {
        activeDoseMilligrams = doseMilligrams;
        targetDoseMilligrams = doseMilligrams;
        displayedDoseMilligrams = doseMilligrams;
        ageTicks = 0;
        durationTicks = KetamineDoseCurve.baseDurationTicks(doseMilligrams);
    }

    public void tick() {
        if (!hasActiveEffects()) {
            return;
        }
        ageTicks++;
        double doseDelta = Math.abs(targetDoseMilligrams - activeDoseMilligrams);
        double ramp = (targetDoseMilligrams > activeDoseMilligrams ? 0.010D : 0.0040D)
                + KetamineDoseCurve.smoothstep(15.0D, 120.0D, doseDelta) * 0.0085D;
        activeDoseMilligrams += (targetDoseMilligrams - activeDoseMilligrams) * ramp;
        displayedDoseMilligrams += (targetDoseMilligrams - displayedDoseMilligrams) * 0.017D;
        if (Math.abs(targetDoseMilligrams - activeDoseMilligrams) < 0.05D) {
            activeDoseMilligrams = targetDoseMilligrams;
        }
        if (Math.abs(targetDoseMilligrams - displayedDoseMilligrams) < 0.05D) {
            displayedDoseMilligrams = targetDoseMilligrams;
        }
    }

    public KetamineEffectState effectState() {
        if (!hasActiveEffects()) {
            return KetamineEffectState.inactive();
        }

        KetaminePhase phase = phase();
        double progress = progress();
        int tier = KetamineDoseCurve.tier(activeDoseMilligrams);
        double phaseEnvelope = KetamineDoseCurve.phaseEnvelope(phase, progress);
        double doseIntensity = KetamineDoseCurve.peakIntensity(activeDoseMilligrams);
        double intensity = KetamineDoseCurve.clamp(doseIntensity * phaseEnvelope, 0.0D, 6.4D);

        double clear = KetamineDoseCurve.smoothstep(50.0D, 100.0D, activeDoseMilligrams);
        double disconnect = KetamineDoseCurve.smoothstep(100.0D, 150.0D, activeDoseMilligrams);
        double kHoleDose = KetamineDoseCurve.smoothstep(400.0D, 500.0D, activeDoseMilligrams);
        double danger = KetamineDoseCurve.smoothstep(300.0D, 425.0D, activeDoseMilligrams);
        double brutal = KetamineDoseCurve.smoothstep(400.0D, 500.0D, activeDoseMilligrams);

        double onset = KetamineDoseCurve.smoothstep(0.00D, 0.12D, progress);
        double floatWindow = KetamineDoseCurve.smoothstep(0.10D, 0.30D, progress);
        double floatPeak = KetamineDoseCurve.smoothstep(0.08D, 0.18D, progress)
            * (1.0D - KetamineDoseCurve.smoothstep(0.32D, 0.46D, progress));
        double detachWindow = KetamineDoseCurve.smoothstep(0.24D, 0.60D, progress);
        double detachPeak = KetamineDoseCurve.smoothstep(0.26D, 0.38D, progress)
            * (1.0D - KetamineDoseCurve.smoothstep(0.60D, 0.78D, progress));
        double kHoleWindow = KetamineDoseCurve.isKHoleDose(activeDoseMilligrams)
            ? KetamineDoseCurve.smoothstep(0.22D, 0.36D, progress) * (1.0D - KetamineDoseCurve.smoothstep(0.68D, 0.84D, progress)) * (0.20D + kHoleDose * 0.80D)
                : 0.0D;
        double reentry = KetamineDoseCurve.smoothstep(0.56D, 0.86D, progress);
        double aftermath = KetamineDoseCurve.smoothstep(0.82D, 1.00D, progress);
        double deepSpace = KetamineDoseCurve.smoothstep(300.0D, 450.0D, activeDoseMilligrams)
            * (0.28D + detachWindow * 0.20D + kHoleWindow * 0.32D);
        double visualPresence = KetamineMasking.visualPresence(activeDoseMilligrams);
        double realityBend = KetamineMasking.realityBend(activeDoseMilligrams);
        double visualCreep = KetamineDoseCurve.smoothstep(0.02D, 0.42D, progress);
        double visualBoost = 1.0D
            + KetamineDoseCurve.smoothstep(100.0D, 200.0D, activeDoseMilligrams) * 0.32D
            + KetamineDoseCurve.smoothstep(200.0D, 400.0D, activeDoseMilligrams) * 0.54D
            + realityBend * 0.24D;

        double dissociation = intensity * (0.20D + floatWindow * 0.14D + floatPeak * 0.08D + detachWindow * 0.28D + kHoleWindow * 0.44D + deepSpace * 0.10D);
        double timeSlip = intensity * (0.08D + floatWindow * 0.08D + floatPeak * 0.12D + detachWindow * 0.18D + kHoleWindow * 0.30D + deepSpace * 0.08D + aftermath * 0.08D);
        double motorDisconnect = intensity * (clear * 0.12D + disconnect * 0.24D + kHoleWindow * 0.52D + danger * 0.22D)
                * (0.10D + detachWindow * 0.18D + reentry * 0.20D + kHoleWindow * 0.42D);
        double bodyFloat = intensity * (0.12D + onset * 0.06D + floatWindow * 0.10D + floatPeak * 0.30D + detachPeak * 0.20D + kHoleWindow * 0.24D + deepSpace * 0.10D);
        double cameraLag = intensity * (0.06D + clear * 0.12D + disconnect * 0.18D + floatPeak * 0.10D + detachWindow * 0.16D + kHoleWindow * 0.36D + deepSpace * 0.08D);
        double depthMismatch = intensity * (0.10D + floatPeak * 0.18D + detachWindow * 0.26D + kHoleWindow * 0.34D + deepSpace * 0.22D);
        double audioMuffle = intensity * (0.06D + detachWindow * 0.14D + kHoleWindow * 0.30D + danger * 0.18D + deepSpace * 0.14D + aftermath * 0.10D);
        double gravityDrift = intensity * (0.06D + floatWindow * 0.08D + floatPeak * 0.26D + detachPeak * 0.20D + kHoleWindow * 0.28D + deepSpace * 0.12D);
        double inputLatency = intensity * (0.05D + disconnect * 0.14D + detachWindow * 0.20D + kHoleWindow * 0.44D + danger * 0.16D);
        double painSuppression = intensity * (0.04D + detachWindow * 0.10D + kHoleWindow * 0.30D + danger * 0.28D + brutal * 0.18D);
        double uiFade = intensity * (0.02D + detachWindow * 0.10D + kHoleWindow * 0.38D + danger * 0.24D + deepSpace * 0.16D);
        double bodyEcho = intensity * (0.05D + detachWindow * 0.18D + kHoleWindow * 0.30D + danger * 0.14D + deepSpace * 0.18D);
        double collapseRisk = intensity * KetamineDoseCurve.clamp(
                (danger * 0.40D + brutal * 0.52D) * (0.10D + detachWindow * 0.14D + kHoleWindow * 0.44D + reentry * 0.20D),
                0.0D,
                1.0D
        );
        double collapsePulse = Math.pow(Math.max(0.0D, Math.sin(ageTicks * (0.033D + danger * 0.020D) + activeDoseMilligrams * 0.010D)), 10.0D);
        double collapseDepth = intensity * collapsePulse * (danger * 0.36D + brutal * 0.44D + kHoleWindow * 0.22D);

        dissociation *= 0.94D + visualPresence * 0.34D;
        timeSlip *= visualBoost * visualCreep;
        bodyFloat *= (0.96D + visualPresence * 0.42D) * visualCreep;
        cameraLag *= visualBoost * visualCreep;
        depthMismatch *= (0.92D + visualPresence * 1.02D + realityBend * 0.26D) * visualCreep;
        gravityDrift *= (0.96D + visualPresence * 0.70D) * visualCreep;
        uiFade *= (0.92D + visualPresence * 0.92D + realityBend * 0.24D) * visualCreep;
        bodyEcho *= (0.94D + visualPresence * 1.04D + realityBend * 0.30D) * visualCreep;

        return new KetamineEffectState(
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
                dissociation,
                timeSlip,
                motorDisconnect,
                bodyFloat,
                cameraLag,
                depthMismatch,
                audioMuffle,
                gravityDrift,
                inputLatency,
                painSuppression,
                uiFade,
                bodyEcho,
                collapseRisk,
                collapseDepth
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

    public KetaminePhase phase() {
        return KetamineDoseCurve.phase(ageTicks, durationTicks, activeDoseMilligrams);
    }

    public double progress() {
        return KetamineDoseCurve.progress(ageTicks, durationTicks);
    }

    public double targetDoseMilligrams() {
        return targetDoseMilligrams;
    }

    public String debugSummary() {
        KetamineEffectState state = effectState();
        if (!state.active()) {
            return "ketamine=inactive";
        }
        return String.format(Locale.ROOT,
                "ketamine dose=%.0fmg tier=%d phase=%s intensity=%.2f remaining=%ds",
                state.displayDoseMilligrams(),
                state.tier(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams);
        tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        return tag;
    }

    public static KetamineSession load(CompoundTag tag) {
        double activeDose = NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D);
        KetamineSession session = new KetamineSession(activeDose, NbtCompat.getIntOr(tag, "durationTicks", 0));
        session.targetDoseMilligrams = NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", activeDose);
        session.displayedDoseMilligrams = NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", activeDose);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        int curveDuration = KetamineDoseCurve.baseDurationTicks(session.targetDoseMilligrams);
        int stackedDurationCap = curveDuration + KetamineDoseCurve.stackExtensionTicks(session.targetDoseMilligrams * 0.40D, session.targetDoseMilligrams);
        if (session.durationTicks < curveDuration) {
            session.durationTicks = curveDuration;
        } else if (session.durationTicks > stackedDurationCap) {
            session.durationTicks = Math.max(session.ageTicks + 40, stackedDurationCap);
        }
        return session;
    }
}