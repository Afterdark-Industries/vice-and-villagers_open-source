package com.trippycraft.substance.salvia;

import com.trippycraft.config.TrippyConfig;

import java.util.Locale;

public final class SalviaDoseCurve {
    public static final double SAFE_VISUAL_INTENSITY = 20.0D;

    private SalviaDoseCurve() {
    }

    public static double safeVisualIntensity() {
        return SAFE_VISUAL_INTENSITY;
    }

    public static double visualIntensity(double doseUnits) {
        return perceptualIntensity(doseUnits);
    }

    public static double peakIntensity(double doseUnits) {
        return perceptualIntensity(doseUnits);
    }

    public static double megaPresence(double doseUnits) {
        double dose = Math.max(0.0D, doseUnits);
        double value = 0.42D * smoothstep(5.6D, 7.0D, dose)
            + 0.38D * smoothstep(7.0D, 10.0D, dose)
            + 0.20D * smoothstep(10.0D, 12.0D, dose)
            + 0.16D * smoothstep(12.0D, 15.0D, dose)
            + 0.20D * smoothstep(15.0D, 20.0D, dose)
            + 0.22D * smoothstep(20.0D, 28.0D, dose);
        return clamp(value, 0.0D, 1.58D);
    }

    public static int tier(double doseUnits) {
        if (doseUnits <= 0.0D) {
            return 0;
        }
        if (doseUnits < 0.45D) {
            return 1;
        }
        if (doseUnits < 0.90D) {
            return 2;
        }
        if (doseUnits < 1.45D) {
            return 3;
        }
        if (doseUnits < 2.20D) {
            return 4;
        }
        return 5;
    }

    public static int baseDurationTicks(double doseUnits) {
        double dose = Math.max(0.0D, doseUnits);
        double seconds = 160.0D
                + 20.0D * smoothstep(0.20D, 1.00D, dose)
                + 120.0D * smoothstep(1.00D, 10.00D, dose)
                + 300.0D * smoothstep(10.00D, 15.00D, dose)
                + 180.0D * smoothstep(15.00D, 24.00D, dose)
                + 220.0D * smoothstep(24.00D, 36.00D, dose);
        return Math.max(20, (int) Math.round(seconds * 20.0D * TrippyConfig.experienceTimeScale));
    }

    public static SalviaPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return SalviaPhase.SOBER;
        }
        double progress = ageTicks / (double) durationTicks;
        if (progress < 0.16D) {
            return SalviaPhase.LOAD_IN;
        }
        if (progress < 0.36D) {
            return SalviaPhase.PULL;
        }
        if (progress < 0.70D) {
            return SalviaPhase.DISLOCATION;
        }
        if (progress < 0.88D) {
            return SalviaPhase.RECURSION;
        }
        return SalviaPhase.SHAKEOUT;
    }

    public static double phaseEnvelope(SalviaPhase phase, double progress) {
        if (phase == SalviaPhase.SOBER) {
            return 0.0D;
        }
        double p = clamp(progress, 0.0D, 1.0D);
        double loadIn = smoothstep(0.0D, 0.12D, p) * (1.0D - smoothstep(0.20D, 0.30D, p));
        double pull = smoothstep(0.10D, 0.22D, p) * (1.0D - smoothstep(0.34D, 0.52D, p));
        double dislocation = smoothstep(0.20D, 0.34D, p) * (1.0D - smoothstep(0.72D, 0.86D, p));
        double recursion = smoothstep(0.48D, 0.62D, p) * (1.0D - smoothstep(0.90D, 0.98D, p));
        double shakeout = smoothstep(0.84D, 0.94D, p) * (1.0D - smoothstep(0.995D, 1.0D, p));
        double envelope = loadIn * 0.22D
                + pull * 0.56D
                + dislocation * 1.08D
                + recursion * 0.98D
                + shakeout * 0.28D;
        return clamp(envelope, 0.0D, 1.16D);
    }

    private static double perceptualIntensity(double doseUnits) {
        double dose = Math.max(0.0D, doseUnits);
        double value = 0.20D * smoothstep(0.10D, 0.35D, dose)
                + 0.55D * smoothstep(0.35D, 0.75D, dose)
                + 0.95D * smoothstep(0.75D, 1.20D, dose)
                + 1.15D * smoothstep(1.20D, 1.80D, dose)
                + 1.35D * smoothstep(1.80D, 2.60D, dose)
                + 1.55D * smoothstep(2.60D, 3.60D, dose)
                + 0.75D * smoothstep(3.60D, 6.00D, dose)
                + 1.45D * smoothstep(6.00D, 10.00D, dose)
                + 1.95D * smoothstep(10.00D, 15.00D, dose)
                + 2.10D * smoothstep(15.00D, 20.00D, dose)
                + 2.40D * smoothstep(20.00D, 28.00D, dose)
                + 2.60D * smoothstep(28.00D, 40.00D, dose);
        return clamp(value * TrippyConfig.globalIntensity, 0.0D, 32.0D);
    }

    public static double displayPotencyX(double doseUnits) {
        double dose = Math.max(0.0D, doseUnits);
        if (dose <= 1.0D) {
            return dose;
        }
        if (dose <= 5.0D) {
            return interpolate(1.0D, 5.0D, 1.0D, 10.0D, dose);
        }
        if (dose <= 10.0D) {
            return interpolate(5.0D, 10.0D, 10.0D, 25.0D, dose);
        }
        if (dose <= 15.0D) {
            return interpolate(10.0D, 15.0D, 25.0D, 50.0D, dose);
        }
        return 50.0D + (dose - 15.0D) * 5.0D;
    }

    public static String formatPotencyLabel(double doseUnits) {
        double potency = displayPotencyX(doseUnits);
        double rounded = Math.rint(potency);
        if (Math.abs(potency - rounded) < 0.05D) {
            return String.format(Locale.ROOT, "%.0fx", rounded);
        }
        return String.format(Locale.ROOT, "%.1fx", potency);
    }

    private static double interpolate(double inMin, double inMax, double outMin, double outMax, double value) {
        if (inMax <= inMin) {
            return outMax;
        }
        double t = clamp((value - inMin) / (inMax - inMin), 0.0D, 1.0D);
        return outMin + (outMax - outMin) * t;
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