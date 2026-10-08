package com.trippycraft.substance.ketamine;

import com.trippycraft.config.TrippyConfig;

public final class KetamineDoseCurve {
    private KetamineDoseCurve() {
    }

    public static int baseDurationTicks(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        double load = 1.0D - Math.exp(-dose / 110.0D);
        double heavyTail = smoothstep(150.0D, 350.0D, dose);
        double seconds = 85.0D + load * 165.0D + heavyTail * 95.0D;
        return secondsToTicks(seconds * TrippyConfig.experienceTimeScale);
    }

    public static int stackExtensionTicks(double addedDoseMilligrams, double totalDoseMilligrams) {
        if (addedDoseMilligrams <= 0.0D) {
            return 0;
        }
        double added = Math.max(0.0D, addedDoseMilligrams);
        double servings = added / 50.0D;
        double escalation = smoothstep(150.0D, 300.0D, totalDoseMilligrams);
        double seconds = 4.0D + servings * 18.0D + escalation * 22.0D;
        return secondsToTicks(seconds * TrippyConfig.experienceTimeScale);
    }

    public static KetaminePhase phase(int ageTicks, int durationTicks, double doseMilligrams) {
        double progress = progress(ageTicks, durationTicks);
        if (progress < 0.12D) {
            return KetaminePhase.ONSET;
        }
        if (progress < 0.30D) {
            return KetaminePhase.FLOAT;
        }
        if (isKHoleDose(doseMilligrams) && progress < 0.68D) {
            return KetaminePhase.K_HOLE;
        }
        if (progress < 0.60D) {
            return KetaminePhase.DETACHMENT;
        }
        if (progress < 0.86D) {
            return KetaminePhase.REENTRY;
        }
        return KetaminePhase.AFTERMATH;
    }

    public static boolean isKHoleDose(double doseMilligrams) {
        return doseMilligrams >= 400.0D;
    }

    public static double progress(int ageTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public static double phaseEnvelope(KetaminePhase phase, double progress) {
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> 0.16D + smoothstep(0.00D, 0.12D, progress) * 0.34D;
            case FLOAT -> 0.54D + smoothstep(0.12D, 0.30D, progress) * 0.22D;
            case DETACHMENT -> 0.82D + Math.sin(progress * Math.PI * 3.0D) * 0.03D;
            case K_HOLE -> 0.98D + Math.sin(progress * Math.PI * 4.0D) * 0.04D;
            case REENTRY -> 0.70D - smoothstep(0.60D, 0.86D, progress) * 0.30D;
            case AFTERMATH -> 0.26D - smoothstep(0.86D, 1.00D, progress) * 0.18D;
        };
    }

    public static double peakIntensity(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        double intensity = 0.34D * smoothstep(20.0D, 50.0D, dose)
                + 0.58D * smoothstep(50.0D, 100.0D, dose)
                + 0.82D * smoothstep(100.0D, 150.0D, dose)
                + 1.04D * smoothstep(150.0D, 220.0D, dose)
                + 1.00D * smoothstep(220.0D, 400.0D, dose)
                + 1.10D * smoothstep(400.0D, 500.0D, dose);
        return clamp(intensity * TrippyConfig.globalIntensity, 0.0D, 6.4D);
    }

    public static int tier(double doseMilligrams) {
        if (doseMilligrams <= 0.0D) {
            return 0;
        }
        if (doseMilligrams < 75.0D) {
            return 1;
        }
        if (doseMilligrams < 125.0D) {
            return 2;
        }
        if (doseMilligrams < 175.0D) {
            return 3;
        }
        if (doseMilligrams < 250.0D) {
            return 4;
        }
        if (doseMilligrams < 400.0D) {
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

    private static int secondsToTicks(double seconds) {
        return Math.max(20, (int) Math.round(seconds * 20.0D));
    }
}