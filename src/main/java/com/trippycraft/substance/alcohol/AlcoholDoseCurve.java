package com.trippycraft.substance.alcohol;

import com.trippycraft.config.TrippyConfig;

public final class AlcoholDoseCurve {
    public static final double STANDARD_DRINK_ETHANOL_OUNCES = 0.60D;

    private AlcoholDoseCurve() {
    }

    public static double standardDrinks(double servingOunces, double alcoholByVolume) {
        return Math.max(0.0D, servingOunces) * Math.max(0.0D, alcoholByVolume) / STANDARD_DRINK_ETHANOL_OUNCES;
    }

    public static int baseDurationTicks(double standardDrinks) {
        double dose = Math.max(0.0D, standardDrinks);
        double cappedDose = Math.min(10.0D, dose);
        double load = 1.0D - Math.exp(-cappedDose / 3.20D);
        double heavyTail = smoothstep(5.50D, 10.0D, cappedDose);
        double seconds = 110.0D + load * 430.0D + heavyTail * 80.0D;
        return Math.max(20, (int) Math.round(seconds * 20.0D * TrippyConfig.experienceTimeScale));
    }

    public static double peakIntensity(double standardDrinks) {
        double dose = Math.max(0.0D, standardDrinks);
        double value = 0.45D * smoothstep(0.25D, 1.2D, dose)
                + 0.85D * smoothstep(1.4D, 3.0D, dose)
                + 1.80D * smoothstep(3.2D, 7.2D, dose)
                + 1.20D * smoothstep(7.5D, 11.5D, dose)
                + 1.75D * smoothstep(11.5D, 16.0D, dose);
        return clamp(value * TrippyConfig.globalIntensity, 0.0D, 6.0D);
    }

    public static int tier(double standardDrinks) {
        if (standardDrinks <= 0.0D) {
            return 0;
        }
        if (standardDrinks < 0.65D) {
            return 1;
        }
        if (standardDrinks < 1.60D) {
            return 2;
        }
        if (standardDrinks < 3.25D) {
            return 3;
        }
        if (standardDrinks < 5.50D) {
            return 4;
        }
        return 5;
    }

    public static AlcoholPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return AlcoholPhase.SOBER;
        }
        double progress = ageTicks / (double) durationTicks;
        if (progress < 0.12D) {
            return AlcoholPhase.ONSET;
        }
        if (progress < 0.32D) {
            return AlcoholPhase.BUZZ;
        }
        if (progress < 0.66D) {
            return AlcoholPhase.INTOXICATED;
        }
        if (progress < 0.82D) {
            return AlcoholPhase.HEAVY;
        }
        if (progress < 0.94D) {
            return AlcoholPhase.COMEDOWN;
        }
        return AlcoholPhase.AFTERGLOW;
    }

    public static double phaseEnvelope(AlcoholPhase phase, double progress) {
        if (phase == AlcoholPhase.SOBER) {
            return 0.0D;
        }
        double p = clamp(progress, 0.0D, 1.0D);
        double onset = smoothstep(0.0D, 0.16D, p);
        double plateau = 1.0D - smoothstep(0.68D, 0.90D, p);
        double tail = 1.0D - smoothstep(0.92D, 1.0D, p) * 0.78D;
        return clamp(onset * Math.max(plateau, tail * 0.34D), 0.0D, 1.0D);
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
