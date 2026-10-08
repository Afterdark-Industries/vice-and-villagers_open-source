package com.trippycraft.substance.cannabis;

public final class CannabisDoseCurve {
    private CannabisDoseCurve() {
    }

    public static int baseDurationTicks(double thcMilligrams) {
        return secondsToTicks(durationSeconds(thcMilligrams));
    }

    public static int stackExtensionTicks(double addedThcMilligrams, double totalThcMilligrams) {
        if (addedThcMilligrams <= 0.0D) {
            return 0;
        }
        double seconds = Math.max(0.0D, addedThcMilligrams * 0.20D);
        return secondsToTicks(seconds);
    }

    public static CannabisPhase phase(int ageTicks, int durationTicks) {
        double progress = progress(ageTicks, durationTicks);
        if (progress < 0.14D) {
            return CannabisPhase.ONSET;
        }
        if (progress < 0.34D) {
            return CannabisPhase.LIFT;
        }
        if (progress < 0.70D) {
            return CannabisPhase.CRUISE;
        }
        if (progress < 0.90D) {
            return CannabisPhase.SINK;
        }
        return CannabisPhase.AFTERGLOW;
    }

    public static double progress(int ageTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return clamp(ageTicks / (double) durationTicks);
    }

    public static double phaseEnvelope(CannabisPhase phase, double progress) {
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> 0.18D + smoothstep(0.00D, 0.14D, progress) * 0.42D;
            case LIFT -> 0.58D + smoothstep(0.14D, 0.34D, progress) * 0.56D;
            case CRUISE -> 1.02D + Math.sin(progress * Math.PI * 4.0D) * 0.05D;
            case SINK -> 0.92D - smoothstep(0.70D, 0.90D, progress) * 0.30D;
            case AFTERGLOW -> 0.58D - smoothstep(0.90D, 1.00D, progress) * 0.34D;
        };
    }

    public static double doseScale(double thcMilligrams) {
        return Math.max(0.0D, thcMilligrams) / 50.0D;
    }

    public static double breathingRhythm(double doseScale) {
        return 0.78D + Math.min(0.85D, Math.log1p(doseScale) * 0.22D);
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double durationSeconds(double thcMilligrams) {
        return Math.max(180.0D, Math.max(0.0D, thcMilligrams) * 0.60D);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }
}