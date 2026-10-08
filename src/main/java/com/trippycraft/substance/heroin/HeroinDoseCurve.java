package com.trippycraft.substance.heroin;

import com.trippycraft.config.TrippyConfig;

public final class HeroinDoseCurve {
    private HeroinDoseCurve() {
    }

    public static int baseDurationTicks(double doseMilligrams, double toleranceProgress) {
        double dose = Math.max(0.0D, doseMilligrams);
        double tolerance = saturate(toleranceProgress);
        double load = 1.0D - Math.exp(-dose / 135.0D);
        double heavyTail = smoothstep(250.0D, 500.0D, dose);
        double seconds = 150.0D + load * 390.0D + heavyTail * 220.0D;
        seconds *= 1.0D - tolerance * 0.42D;
        return secondsToTicks(seconds * TrippyConfig.experienceTimeScale);
    }

    public static int stackExtensionTicks(double addedDoseMilligrams, double totalDoseMilligrams, double toleranceProgress) {
        if (addedDoseMilligrams <= 0.0D) {
            return 0;
        }
        double servings = addedDoseMilligrams / 25.0D;
        double escalation = smoothstep(100.0D, 300.0D, totalDoseMilligrams);
        double tolerance = saturate(toleranceProgress);
        double seconds = 9.0D + servings * 18.0D + escalation * 44.0D;
        seconds *= 1.0D - tolerance * 0.32D;
        return secondsToTicks(seconds * TrippyConfig.experienceTimeScale);
    }

    public static HeroinPhase phase(int ageTicks, int durationTicks, double doseMilligrams) {
        double progress = progress(ageTicks, durationTicks);
        if (progress < 0.02D) {
            return HeroinPhase.ONSET;
        }
        if (progress < 0.32D) {
            return HeroinPhase.WARMTH;
        }
        if (isNodDose(doseMilligrams) && progress < nodEndProgress(doseMilligrams)) {
            return HeroinPhase.NOD;
        }
        if (progress < 0.64D) {
            return HeroinPhase.BLISS;
        }
        if (progress < 0.86D) {
            return HeroinPhase.COMEDOWN;
        }
        return HeroinPhase.AFTERGLOW;
    }

    public static boolean isNodDose(double doseMilligrams) {
        return doseMilligrams >= 250.0D;
    }

    private static double nodEndProgress(double doseMilligrams) {
        return doseMilligrams >= 300.0D ? 0.74D : 0.56D;
    }

    public static double progress(int ageTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public static double phaseEnvelope(HeroinPhase phase, double progress, double doseMilligrams) {
        double heavy = smoothstep(250.0D, 350.0D, doseMilligrams);
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> 0.26D + smoothstep(0.00D, 0.02D, progress) * 0.46D;
            case WARMTH -> 0.70D + smoothstep(0.02D, 0.24D, progress) * 0.20D;
            case BLISS -> 0.88D + Math.sin(progress * Math.PI * 2.0D) * 0.035D;
            case NOD -> 0.92D + heavy * 0.10D + Math.sin(progress * Math.PI * 6.0D) * 0.025D;
            case COMEDOWN -> 0.62D - smoothstep(0.64D, 0.86D, progress) * 0.28D;
            case AFTERGLOW -> 0.30D - smoothstep(0.86D, 1.00D, progress) * 0.18D;
        };
    }

    public static double peakIntensity(double doseMilligrams, double toleranceProgress, double dependenceProgress) {
        double dose = Math.max(0.0D, doseMilligrams);
        double intensity = 0.50D * smoothstep(5.0D, 25.0D, dose)
            + 1.10D * smoothstep(25.0D, 100.0D, dose)
            + 2.10D * smoothstep(100.0D, 250.0D, dose)
            + 1.60D * smoothstep(250.0D, 300.0D, dose)
            + 1.75D * smoothstep(300.0D, 500.0D, dose)
            + 0.70D * smoothstep(500.0D, 700.0D, dose);
        double toleranceRelief = 1.0D - saturate(toleranceProgress) * 0.50D;
        double dependenceDrag = 1.0D - saturate(dependenceProgress) * 0.12D;
        return clamp(intensity * toleranceRelief * dependenceDrag * TrippyConfig.globalIntensity, 0.0D, 8.2D);
    }

    public static int tier(double doseMilligrams) {
        if (doseMilligrams <= 0.0D) {
            return 0;
        }
        if (doseMilligrams < 50.0D) {
            return 1;
        }
        if (doseMilligrams < 150.0D) {
            return 2;
        }
        if (doseMilligrams < 250.0D) {
            return 3;
        }
        if (doseMilligrams < 300.0D) {
            return 4;
        }
        if (doseMilligrams < 500.0D) {
            return 5;
        }
        return 6;
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

    public static double saturate(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(20, (int) Math.round(seconds * 20.0D));
    }
}
