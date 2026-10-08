package com.trippycraft.substance.cocaine;

import com.trippycraft.config.TrippyConfig;

public final class CocaineDoseCurve {
    private CocaineDoseCurve() {
    }

    public static int baseDurationTicks(double doseGrams, double durationScale) {
        double dose = Math.max(0.0D, doseGrams);
        double load = 1.0D - Math.exp(-dose / 1.85D);
        double heavyTail = smoothstep(3.0D, 5.5D, dose);
        double seconds = (98.0D + load * 132.0D + heavyTail * 44.0D) * clamp(durationScale, 0.38D, 1.10D);
        return secondsToTicks(seconds * TrippyConfig.experienceTimeScale);
    }

    public static int stackExtensionTicks(double addedDoseGrams, double totalDoseGrams, double extensionScale, double durationScale) {
        if (addedDoseGrams <= 0.0D) {
            return 0;
        }
        double added = Math.max(0.0D, addedDoseGrams);
        double escalation = smoothstep(2.0D, 6.0D, totalDoseGrams);
        double seconds = (6.0D + added * 16.0D + escalation * 10.0D)
                * clamp(extensionScale, 0.40D, 1.05D)
                * clamp(0.78D + durationScale * 0.28D, 0.48D, 1.10D);
        return secondsToTicks(seconds * TrippyConfig.experienceTimeScale);
    }

    public static CocainePhase phase(int ageTicks, int durationTicks) {
        double progress = progress(ageTicks, durationTicks);
        if (progress < 0.12D) {
            return CocainePhase.ONSET;
        }
        if (progress < 0.24D) {
            return CocainePhase.RUSH;
        }
        if (progress < 0.55D) {
            return CocainePhase.PEAK;
        }
        if (progress < 0.78D) {
            return CocainePhase.EDGE;
        }
        if (progress < 0.94D) {
            return CocainePhase.CRASH;
        }
        return CocainePhase.AFTERGLOW;
    }

    public static double progress(int ageTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public static double phaseEnvelope(CocainePhase phase, double progress) {
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> 0.18D + smoothstep(0.00D, 0.12D, progress) * 0.48D;
            case RUSH -> 0.70D + smoothstep(0.12D, 0.24D, progress) * 0.42D;
            case PEAK -> 1.04D + Math.sin(progress * Math.PI * 5.0D) * 0.04D;
            case EDGE -> 1.00D - smoothstep(0.55D, 0.78D, progress) * 0.26D;
            case CRASH -> 0.76D - smoothstep(0.78D, 0.94D, progress) * 0.42D;
            case AFTERGLOW -> 0.34D - smoothstep(0.94D, 1.00D, progress) * 0.24D;
        };
    }

    public static double peakIntensity(double doseGrams, double routeIntensityBoost) {
        double dose = Math.max(0.0D, doseGrams);
        double intensity = 0.42D * smoothstep(0.15D, 1.0D, dose)
                + 0.76D * smoothstep(1.0D, 3.0D, dose)
                + 1.18D * smoothstep(2.8D, 5.0D, dose)
                + 0.92D * smoothstep(5.0D, 8.0D, dose)
                + 0.60D * smoothstep(8.0D, 12.0D, dose);
        return clamp(intensity * Math.max(0.40D, routeIntensityBoost) * TrippyConfig.globalIntensity, 0.0D, 5.8D);
    }

    public static int tier(double doseGrams) {
        if (doseGrams <= 0.0D) {
            return 0;
        }
        if (doseGrams < 1.5D) {
            return 1;
        }
        if (doseGrams < 3.5D) {
            return 2;
        }
        if (doseGrams < 5.5D) {
            return 3;
        }
        if (doseGrams < 8.0D) {
            return 4;
        }
        return 5;
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