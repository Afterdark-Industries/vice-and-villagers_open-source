package com.trippycraft.substance.alcohol;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public final class AlcoholSession {
    private double activeStandardDrinks;
    private double targetStandardDrinks;
    private double displayedStandardDrinks;
    private int ageTicks;
    private int durationTicks;

    private AlcoholSession(double standardDrinks, int durationTicks) {
        this.activeStandardDrinks = standardDrinks;
        this.targetStandardDrinks = standardDrinks;
        this.displayedStandardDrinks = standardDrinks;
        this.durationTicks = durationTicks;
    }

    public static AlcoholSession start(double standardDrinks) {
        return new AlcoholSession(standardDrinks, AlcoholDoseCurve.baseDurationTicks(standardDrinks));
    }

    public void stackDose(double addedStandardDrinks) {
        double added = Math.max(0.0D, addedStandardDrinks);
        targetStandardDrinks += added;
        int targetDuration = AlcoholDoseCurve.baseDurationTicks(targetStandardDrinks);
        int drinkExtension = stackExtensionTicks(added, targetStandardDrinks);
        int absorptionExtension = (int) Math.round(AlcoholDoseCurve.baseDurationTicks(added) * 0.14D
                + drinkExtension * (0.72D + AlcoholDoseCurve.clamp(targetStandardDrinks / 16.0D, 0.0D, 1.0D) * 0.28D));
        durationTicks = Math.max(durationTicks + drinkExtension, Math.max(targetDuration, ageTicks + absorptionExtension));
    }

    public void tick() {
        ageTicks++;
        double doseDelta = Math.abs(targetStandardDrinks - activeStandardDrinks);
        double ramp = (targetStandardDrinks > activeStandardDrinks ? 0.0065D : 0.0022D)
                + AlcoholDoseCurve.smoothstep(0.25D, 2.5D, doseDelta) * 0.0045D;
        activeStandardDrinks += (targetStandardDrinks - activeStandardDrinks) * ramp;
        displayedStandardDrinks += (targetStandardDrinks - displayedStandardDrinks) * 0.014D;
        if (Math.abs(targetStandardDrinks - activeStandardDrinks) < 0.002D) {
            activeStandardDrinks = targetStandardDrinks;
        }
        if (Math.abs(targetStandardDrinks - displayedStandardDrinks) < 0.002D) {
            displayedStandardDrinks = targetStandardDrinks;
        }
    }

    public AlcoholEffectState effectState() {
        AlcoholPhase phase = phase();
        double progress = progress();
        double doseIntensity = AlcoholDoseCurve.peakIntensity(activeStandardDrinks);
        double phaseEnvelope = AlcoholDoseCurve.phaseEnvelope(phase, progress);
        int tier = AlcoholDoseCurve.tier(activeStandardDrinks);
        double pulse = Math.sin(ageTicks * 0.018D + activeStandardDrinks * 1.7D) * 0.040D
                + Math.sin(ageTicks * 0.046D + targetStandardDrinks * 0.8D) * AlcoholDoseCurve.smoothstep(2.0D, 5.0D, activeStandardDrinks) * 0.030D;
        double living = AlcoholDoseCurve.clamp(1.0D + pulse, 0.88D, 1.12D);
        double intensity = AlcoholDoseCurve.clamp(doseIntensity * phaseEnvelope * living, 0.0D, 6.0D);

        double buzz = AlcoholDoseCurve.smoothstep(0.45D, 1.8D, activeStandardDrinks);
        double intox = AlcoholDoseCurve.smoothstep(1.8D, 4.6D, activeStandardDrinks);
        double heavy = AlcoholDoseCurve.smoothstep(5.2D, 9.5D, activeStandardDrinks);
        double extreme = AlcoholDoseCurve.smoothstep(11.5D, 16.0D, activeStandardDrinks);
        double euphoria = intensity * buzz * (0.50D + intox * 0.18D) * (1.0D - heavy * 0.24D - extreme * 0.18D);
        double sedation = intensity * (0.14D + intox * 0.34D + heavy * 0.54D + extreme * 0.62D);
        double motorNoise = intensity * (buzz * 0.16D + intox * 0.38D + heavy * 0.58D + extreme * 0.44D);
        double motorLoad = AlcoholDoseCurve.clamp(motorNoise / 6.0D, 0.0D, 1.0D);
        double sedationLoad = AlcoholDoseCurve.clamp(sedation / 6.0D, 0.0D, 1.0D);
        double diplopia = intensity * (intox * 0.18D + heavy * 0.50D + extreme * 0.62D) * (0.62D + motorLoad * 0.38D);
        double focusBlur = intensity * (intox * 0.16D + heavy * 0.42D + extreme * 0.48D + sedationLoad * 0.20D);
        double vestibularSway = intensity * (buzz * 0.09D + intox * 0.30D + heavy * 0.50D + extreme * 0.42D) * (0.72D + motorLoad * 0.28D);
        double peripheralNarrowing = intensity * (intox * 0.08D + heavy * 0.28D + extreme * 0.28D + sedationLoad * 0.18D);
        double nausea = intensity * (heavy * 0.30D + extreme * 0.52D) * (0.32D + AlcoholDoseCurve.smoothstep(8.0D, 15.0D, activeStandardDrinks) * 0.42D);
        double disinhibition = intensity * buzz * (0.30D + intox * 0.28D);
        double risk = intensity * (intox * 0.16D + heavy * 0.44D + extreme * 0.48D);

        return new AlcoholEffectState(
                true,
                displayedStandardDrinks,
                activeStandardDrinks,
                targetStandardDrinks,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                tier,
                intensity,
                sedation,
                motorNoise,
                diplopia,
                focusBlur,
                vestibularSway,
                peripheralNarrowing,
                euphoria,
                nausea,
                disinhibition,
                risk
        );
    }

    public boolean isAlive() {
        return ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public AlcoholPhase phase() {
        return AlcoholDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return AlcoholDoseCurve.clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public String debugSummary() {
        AlcoholEffectState state = effectState();
        return String.format(Locale.ROOT,
                "alcohol dose=%.2f standard_drinks tier=%d phase=%s intensity=%.2f remaining=%ds",
                state.displayStandardDrinks(),
                state.tier(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeStandardDrinks", activeStandardDrinks);
        tag.putDouble("targetStandardDrinks", targetStandardDrinks);
        tag.putDouble("displayedStandardDrinks", displayedStandardDrinks);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        return tag;
    }

    public static AlcoholSession load(CompoundTag tag) {
        AlcoholSession session = new AlcoholSession(
                NbtCompat.getDoubleOr(tag, "activeStandardDrinks", 0.0D),
                NbtCompat.getIntOr(tag, "durationTicks", 0)
        );
        session.targetStandardDrinks = NbtCompat.getDoubleOr(tag, "targetStandardDrinks", session.activeStandardDrinks);
        session.displayedStandardDrinks = NbtCompat.getDoubleOr(tag, "displayedStandardDrinks", session.activeStandardDrinks);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        int curveDuration = AlcoholDoseCurve.baseDurationTicks(session.targetStandardDrinks);
        int stackedDurationCap = curveDuration + secondsToTicks(360.0D + session.targetStandardDrinks * 18.0D);
        if (session.durationTicks < curveDuration) {
            session.durationTicks = curveDuration;
        } else if (session.durationTicks > stackedDurationCap) {
            session.durationTicks = Math.max(session.ageTicks + secondsToTicks(20.0D), stackedDurationCap);
        }
        return session;
    }

    private static int stackExtensionTicks(double addedStandardDrinks, double totalStandardDrinks) {
        if (addedStandardDrinks <= 0.0D) {
            return 0;
        }
        double added = AlcoholDoseCurve.clamp(addedStandardDrinks, 0.0D, 3.0D);
        double intoxicated = AlcoholDoseCurve.smoothstep(3.5D, 8.0D, totalStandardDrinks);
        double heavy = AlcoholDoseCurve.smoothstep(10.0D, 16.0D, totalStandardDrinks);
        double seconds = 16.0D + added * 14.0D + intoxicated * 7.0D + heavy * 7.0D;
        return secondsToTicks(seconds);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }
}
