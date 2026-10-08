package com.trippycraft.substance.twocb;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public final class TwoCbSession {
    private double activeDoseMilligrams;
    private double targetDoseMilligrams;
    private double displayedDoseMilligrams;
    private int ageTicks;
    private int durationTicks;
    private int repeatedDoses;
    private double settingMultiplier = 1.0D;

    private TwoCbSession(double doseMilligrams, int durationTicks) {
        activeDoseMilligrams = doseMilligrams;
        targetDoseMilligrams = doseMilligrams;
        displayedDoseMilligrams = doseMilligrams;
        this.durationTicks = durationTicks;
    }

    public static TwoCbSession start(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        return new TwoCbSession(dose, TwoCbDoseCurve.baseDurationTicks(dose));
    }

    public void stackDose(double addedDoseMilligrams) {
        double added = Math.max(0.0D, addedDoseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (!isAlive()) {
            restart(added);
            return;
        }
        repeatedDoses++;
        targetDoseMilligrams += added;
        int extension = TwoCbDoseCurve.stackExtensionTicks(added, targetDoseMilligrams);
        durationTicks = Math.max(durationTicks + extension, ageTicks + extension);
    }

    public void restart(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        activeDoseMilligrams = dose;
        targetDoseMilligrams = dose;
        displayedDoseMilligrams = dose;
        ageTicks = 0;
        durationTicks = TwoCbDoseCurve.baseDurationTicks(dose);
        repeatedDoses = 0;
    }

    public void tick() {
        if (!isAlive()) {
            return;
        }
        ageTicks++;
        double delta = Math.abs(targetDoseMilligrams - activeDoseMilligrams);
        double ramp = 0.0065D + TwoCbDoseCurve.smoothstep(25.0D, 250.0D, delta) * 0.0055D;
        activeDoseMilligrams += (targetDoseMilligrams - activeDoseMilligrams) * ramp;
        displayedDoseMilligrams += (targetDoseMilligrams - displayedDoseMilligrams) * 0.014D;
        if (Math.abs(targetDoseMilligrams - activeDoseMilligrams) < 0.02D) {
            activeDoseMilligrams = targetDoseMilligrams;
        }
        if (Math.abs(targetDoseMilligrams - displayedDoseMilligrams) < 0.02D) {
            displayedDoseMilligrams = targetDoseMilligrams;
        }
    }

    public void setSettingMultiplier(double multiplier) {
        settingMultiplier = TwoCbDoseCurve.clamp(multiplier, 1.0D, 1.5D);
    }

    public TwoCbEffectState effectState() {
        if (!isAlive()) {
            return TwoCbEffectState.inactive();
        }
        double progress = TwoCbDoseCurve.progress(ageTicks, durationTicks);
        TwoCbPhase phase = TwoCbDoseCurve.phase(ageTicks, durationTicks);
        double envelope = TwoCbDoseCurve.phaseEnvelope(phase, progress);
        double intensity = TwoCbDoseCurve.peakIntensity(activeDoseMilligrams) * envelope * settingMultiplier;
        double highDose = TwoCbDoseCurve.smoothstep(25.0D, 500.0D, activeDoseMilligrams);
        double strongDose = TwoCbDoseCurve.smoothstep(100.0D, 500.0D, activeDoseMilligrams);
        double peak = TwoCbDoseCurve.smoothstep(0.12D, 0.28D, progress)
                * (1.0D - TwoCbDoseCurve.smoothstep(0.78D, 0.98D, progress));
        double crystallineWindow = TwoCbDoseCurve.smoothstep(0.38D, 0.62D, progress)
                * (1.0D - TwoCbDoseCurve.smoothstep(0.88D, 1.0D, progress));
        double repeatPressure = TwoCbDoseCurve.clamp(repeatedDoses * 0.16D, 0.0D, 0.72D);

        double saturation = intensity * (0.36D + peak * 0.58D + highDose * 0.28D);
        double edgeGlow = intensity * (0.26D + peak * 0.66D + highDose * 0.34D);
        double surfaceGeometry = intensity * (0.12D + peak * 0.38D + highDose * 0.62D);
        double tracerStrength = intensity * (0.08D + peak * 0.38D + strongDose * 0.58D);
        double bassSensitivity = intensity * (0.12D + peak * 0.68D + highDose * 0.26D);
        double fisheyePulse = intensity * (0.025D + peak * 0.070D + strongDose * 0.105D);
        double symmetry = intensity * strongDose * (0.10D + peak * 0.34D + crystallineWindow * 0.42D);
        double crystalline = intensity * strongDose * (0.06D + peak * 0.24D + crystallineWindow * 0.70D);
        double sensuality = intensity * (0.12D + peak * 0.42D + highDose * 0.16D);
        double stimulation = intensity * (0.10D + peak * 0.26D + highDose * 0.14D);
        double anxiety = intensity * (strongDose * 0.12D + repeatPressure * 0.32D);
        return new TwoCbEffectState(
                true, displayedDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks(), phase, progress, TwoCbDoseCurve.tier(activeDoseMilligrams),
                repeatedDoses, intensity, saturation, edgeGlow, surfaceGeometry, tracerStrength, bassSensitivity,
                fisheyePulse, symmetry, crystalline, sensuality, stimulation, anxiety, settingMultiplier
        );
    }

    public boolean isAlive() {
        return ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public String debugSummary() {
        TwoCbEffectState state = effectState();
        if (!state.active()) {
            return "2cb=inactive";
        }
        return String.format(Locale.ROOT, "2cb dose=%.0fmg tier=%d phase=%s intensity=%.2f remaining=%ds",
                state.displayDoseMilligrams(), state.tier(), state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(), state.remainingTicks() / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMilligrams", activeDoseMilligrams);
        tag.putDouble("targetDoseMilligrams", targetDoseMilligrams);
        tag.putDouble("displayedDoseMilligrams", displayedDoseMilligrams);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("repeatedDoses", repeatedDoses);
        return tag;
    }

    public static TwoCbSession load(CompoundTag tag) {
        double activeDose = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "activeDoseMilligrams", 0.0D));
        TwoCbSession session = new TwoCbSession(activeDose, Math.max(0, NbtCompat.getIntOr(tag, "durationTicks", 0)));
        session.targetDoseMilligrams = Math.max(activeDose, NbtCompat.getDoubleOr(tag, "targetDoseMilligrams", activeDose));
        session.displayedDoseMilligrams = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "displayedDoseMilligrams", activeDose));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.repeatedDoses = Math.max(0, NbtCompat.getIntOr(tag, "repeatedDoses", 0));
        return session;
    }
}
