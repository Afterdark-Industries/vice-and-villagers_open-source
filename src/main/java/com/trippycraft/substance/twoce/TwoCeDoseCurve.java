package com.trippycraft.substance.twoce;

import com.trippycraft.config.TrippyConfig;

/** Fictional gameplay amounts and timing for 2C-E tablets. */
public final class TwoCeDoseCurve {
    private TwoCeDoseCurve() {
    }

    private static final double VISUAL_STRENGTH = 1.85D;
    private static final double[] DURATION_EFFECT_ANCHORS = {10, 20, 40, 80, 160, 320, 480, 800};
    private static final double[] DURATION_SECONDS = {240, 300, 345, 390, 540, 750, 870, 900};

    public static double peakIntensity(double doseMilligrams) {
        return Math.log1p(sanitizeDose(doseMilligrams) / 8.0D) * 1.55D
                * highDoseAmplification(doseMilligrams) * intensityScale() * VISUAL_STRENGTH;
    }

    public static double visualAmplitude(double doseMilligrams) {
        return Math.log1p(sanitizeDose(doseMilligrams) / 8.0D) / Math.log1p(40.0D / 8.0D)
                * highDoseAmplification(doseMilligrams) * intensityScale() * VISUAL_STRENGTH;
    }

    private static double highDoseAmplification(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        return 1.0D
                + 0.50D * smoothstep(8.0D, 16.0D, dose)
                + 0.85D * smoothstep(16.0D, 28.0D, dose)
                + 1.15D * smoothstep(32.0D, 50.0D, dose)
                + 1.80D * smoothstep(65.0D, 100.0D, dose);
    }

    /** Unbounded machine-overwhelm lift past 80mg. */
    public static double machineLift(double doseMilligrams) {
        return Math.log1p(Math.max(0.0D, sanitizeDose(doseMilligrams) - 70.0D) / 18.0D);
    }

    public static double visualPresence(double intensity) {
        return 1.0D - Math.exp(-Math.max(0.0D, intensity));
    }

    public static int tier(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) {
            return 0;
        }
        if (dose < 15.0D) {
            return 1;
        }
        if (dose < 28.0D) {
            return 2;
        }
        if (dose < 55.0D) {
            return 3;
        }
        if (dose < 90.0D) {
            return 4;
        }
        return 5;
    }

    public static int baseDurationTicks(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) {
            return 0;
        }
        // Effect amounts are calibrated gameplay units: 80 corresponds to
        // 500mg of recorded item content, and 800 corresponds to 5000mg.
        // Smooth intervals retain progression between content tiers instead
        // of jumping clocks at a strength boundary.
        double seconds = DURATION_SECONDS[DURATION_SECONDS.length - 1];
        double lowerDose = 0.0D;
        double lowerSeconds = 0.0D;
        for (int index = 0; index < DURATION_EFFECT_ANCHORS.length; index++) {
            double upperDose = DURATION_EFFECT_ANCHORS[index];
            if (dose <= upperDose) {
                seconds = lowerSeconds + (DURATION_SECONDS[index] - lowerSeconds)
                        * smoothstep(lowerDose, upperDose, dose);
                break;
            }
            lowerDose = upperDose;
            lowerSeconds = DURATION_SECONDS[index];
        }
        double scale = clamp(TrippyConfig.experienceTimeScale, 0.01D, 100.0D);
        return (int) Math.round(clamp(seconds * 20.0D * scale, 20.0D, maximumDurationTicks()));
    }

    public static int maximumDurationTicks() {
        // This bounds the age of the original session, including late redoses
        // and configured time scaling; it is not a fresh per-dose allowance.
        return 15 * 60 * 20;
    }

    public static TwoCePhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return TwoCePhase.SOBER;
        }
        double p = progress(ageTicks, durationTicks);
        if (p < 0.10D) {
            return TwoCePhase.ONSET;
        }
        if (p < 0.24D) {
            return TwoCePhase.LOCK_IN;
        }
        if (p < 0.46D) {
            return TwoCePhase.PEAK;
        }
        if (p < 0.76D) {
            return TwoCePhase.MACHINE;
        }
        if (p < 0.92D) {
            return TwoCePhase.DESCENT;
        }
        return TwoCePhase.AFTERGLOW;
    }

    public static double phaseEnvelope(TwoCePhase phase, double progress) {
        double p = clamp(progress, 0.0D, 1.0D);
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> smoothstep(0.01D, 0.10D, p) * 0.48D;
            case LOCK_IN -> 0.48D + smoothstep(0.10D, 0.24D, p) * 0.42D;
            case PEAK -> 0.90D + smoothstep(0.24D, 0.46D, p) * 0.10D;
            case MACHINE -> 1.0D;
            case DESCENT -> 0.94D - smoothstep(0.76D, 0.92D, p) * 0.78D;
            case AFTERGLOW -> 0.16D * (1.0D - smoothstep(0.92D, 1.0D, p));
        };
    }

    public static double progress(int ageTicks, int durationTicks) {
        return durationTicks <= 0 ? 1.0D : clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static double sanitizeDose(double value) {
        if (Double.isNaN(value) || value <= 0.0D) {
            return 0.0D;
        }
        return Double.isInfinite(value) ? Double.MAX_VALUE : value;
    }

    public static double safeAdd(double a, double b) {
        a = sanitizeDose(a);
        b = sanitizeDose(b);
        return a > Double.MAX_VALUE - b ? Double.MAX_VALUE : a + b;
    }

    private static double intensityScale() {
        return clamp(TrippyConfig.globalIntensity, 0.0D, 100.0D);
    }
}
