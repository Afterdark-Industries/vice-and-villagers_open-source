package com.trippycraft.substance.cannabis;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class CannabisSession {
    private double activeThcMilligrams;
    private double targetThcMilligrams;
    private double displayedThcMilligrams;
    private int ageTicks;
    private int durationTicks;

    private CannabisSession(double thcMilligrams, int durationTicks) {
        this.activeThcMilligrams = thcMilligrams;
        this.targetThcMilligrams = thcMilligrams;
        this.displayedThcMilligrams = thcMilligrams;
        this.durationTicks = durationTicks;
    }

    public static CannabisSession start(ServerPlayer player, double thcMilligrams) {
        return new CannabisSession(thcMilligrams, CannabisDoseCurve.baseDurationTicks(thcMilligrams));
    }

    public void stackDose(double addedThcMilligrams) {
        double added = Math.max(0.0D, addedThcMilligrams);
        targetThcMilligrams += added;
        int targetDuration = CannabisDoseCurve.baseDurationTicks(targetThcMilligrams);
        int extension = CannabisDoseCurve.stackExtensionTicks(added, targetThcMilligrams);
        durationTicks = Math.max(durationTicks + extension, Math.max(targetDuration, ageTicks + extension));
    }

    public void tick() {
        ageTicks++;
        double doseDelta = Math.abs(targetThcMilligrams - activeThcMilligrams);
        double ramp = (targetThcMilligrams > activeThcMilligrams ? 0.0050D : 0.0018D)
                + CannabisDoseCurve.smoothstep(25.0D, 400.0D, doseDelta) * 0.0048D;
        activeThcMilligrams += (targetThcMilligrams - activeThcMilligrams) * ramp;
        displayedThcMilligrams += (targetThcMilligrams - displayedThcMilligrams) * 0.012D;
        if (Math.abs(targetThcMilligrams - activeThcMilligrams) < 0.002D) {
            activeThcMilligrams = targetThcMilligrams;
        }
        if (Math.abs(targetThcMilligrams - displayedThcMilligrams) < 0.002D) {
            displayedThcMilligrams = targetThcMilligrams;
        }
    }

    public CannabisEffectState effectState() {
        CannabisPhase phase = phase();
        double progress = progress();
        double doseScale = CannabisDoseCurve.doseScale(activeThcMilligrams);
        double phaseEnvelope = CannabisDoseCurve.phaseEnvelope(phase, progress);
        double stonedLift = CannabisDoseCurve.smoothstep(1.0D, 20.0D, doseScale);
        double couchLift = CannabisDoseCurve.smoothstep(4.0D, 20.0D, doseScale);
        double smallDoseLift = 1.0D + CannabisDoseCurve.smoothstep(0.0D, 4.0D, doseScale) * 0.88D;
        double heavyDoseLift = 1.0D + CannabisDoseCurve.smoothstep(6.0D, 20.0D, doseScale) * 0.68D;
        double drift = Math.sin(ageTicks * 0.011D * CannabisDoseCurve.breathingRhythm(doseScale) + doseScale * 0.9D) * (0.05D + Math.min(0.30D, doseScale * 0.01D));
        double intensity = Math.max(0.0D, doseScale * phaseEnvelope * smallDoseLift * heavyDoseLift * (1.12D + stonedLift * 0.72D) * (1.0D + drift));

        double onset = CannabisDoseCurve.smoothstep(0.0D, 0.16D, progress);
        double cruise = CannabisDoseCurve.smoothstep(0.22D, 0.58D, progress);
        double sink = CannabisDoseCurve.smoothstep(0.68D, 0.92D, progress);
        double after = CannabisDoseCurve.smoothstep(0.88D, 1.0D, progress);

        double immersion = intensity * (0.56D + cruise * 0.34D + sink * 0.22D + stonedLift * 0.16D);
        double temporalDrag = intensity * (0.34D + onset * 0.18D + cruise * 0.44D + sink * 0.24D + stonedLift * 0.18D);
        double breathingPulse = intensity * (0.14D + cruise * 0.18D + after * 0.10D + stonedLift * 0.04D);
        double focusTunnel = intensity * (0.18D + cruise * 0.18D + sink * 0.14D + stonedLift * 0.12D);
        double audioGlue = intensity * (0.10D + cruise * 0.14D + sink * 0.08D + stonedLift * 0.08D);
        double thoughtStickiness = intensity * (0.18D + onset * 0.10D + cruise * 0.24D + sink * 0.20D + couchLift * 0.16D);
        double warmthShift = intensity * (0.14D + cruise * 0.16D + after * 0.08D);
        double saturationBoost = intensity * (0.12D + cruise * 0.12D + stonedLift * 0.05D);
        double greenRichness = intensity * (0.12D + cruise * 0.14D + stonedLift * 0.04D);

        return new CannabisEffectState(
                true,
                displayedThcMilligrams,
                activeThcMilligrams,
                targetThcMilligrams,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                doseScale,
                intensity,
                immersion,
                temporalDrag,
                breathingPulse,
                focusTunnel,
                audioGlue,
                thoughtStickiness,
                warmthShift,
                saturationBoost,
                greenRichness
        );
    }

    public boolean isAlive() {
        return ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public CannabisPhase phase() {
        return CannabisDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        return CannabisDoseCurve.progress(ageTicks, durationTicks);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeThcMilligrams", activeThcMilligrams);
        tag.putDouble("targetThcMilligrams", targetThcMilligrams);
        tag.putDouble("displayedThcMilligrams", displayedThcMilligrams);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        return tag;
    }

    public static CannabisSession load(CompoundTag tag) {
        CannabisSession session = new CannabisSession(
                NbtCompat.getDoubleOr(tag, "activeThcMilligrams", 0.0D),
                NbtCompat.getIntOr(tag, "durationTicks", 0)
        );
        session.targetThcMilligrams = NbtCompat.getDoubleOr(tag, "targetThcMilligrams", session.activeThcMilligrams);
        session.displayedThcMilligrams = NbtCompat.getDoubleOr(tag, "displayedThcMilligrams", session.activeThcMilligrams);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        int curveDuration = CannabisDoseCurve.baseDurationTicks(session.targetThcMilligrams);
        int stackedDurationCap = curveDuration + Math.max(200, CannabisDoseCurve.stackExtensionTicks(session.targetThcMilligrams * 0.35D, session.targetThcMilligrams));
        if (session.durationTicks < curveDuration) {
            session.durationTicks = curveDuration;
        } else if (session.durationTicks > stackedDurationCap) {
            session.durationTicks = Math.max(session.ageTicks + 200, stackedDurationCap);
        }
        return session;
    }

    public String debugSummary() {
        CannabisEffectState state = effectState();
        return String.format(Locale.ROOT,
                "cannabis thc=%.0fmg phase=%s intensity=%.2f remaining=%ds",
                state.displayThcMilligrams(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20);
    }
}