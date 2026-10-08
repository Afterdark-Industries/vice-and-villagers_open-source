package com.trippycraft.substance.mescaline;

import com.trippycraft.config.TrippyConfig;

/** Fictional gameplay timing and intensity curve; labels are not real-world guidance. */
public final class MescalineDoseCurve {
    private static final int MAXIMUM_DURATION_TICKS = 12 * 60 * 20;

    private MescalineDoseCurve() {
    }

    /**
     * Deliberately has no upper ceiling. Growth above authored doses is logarithmic so the
     * renderer can spend it on additional pattern octaves instead of unsafe brightness/FOV.
     */
    public static double complexity(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        return Math.log1p(dose / 70.0D) / Math.log(2.0D) * Math.max(0.0D, TrippyConfig.globalIntensity);
    }

    public static double peakIntensity(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        double authored = 0.30D * smoothstep(0.0D, 25.0D, dose)
                + 0.90D * smoothstep(25.0D, 100.0D, dose)
                + 1.35D * smoothstep(100.0D, 250.0D, dose)
                + 2.05D * smoothstep(250.0D, 500.0D, dose);
        double beyond = dose > 500.0D ? Math.log1p((dose - 500.0D) / 360.0D) * 1.10D : 0.0D;
        return (authored + beyond) * Math.max(0.0D, TrippyConfig.globalIntensity);
    }

    /** Bounded perceptual presence keeps the scene readable while complexity remains unbounded. */
    public static double visualPresence(double intensity) {
        return 1.0D - Math.exp(-Math.max(0.0D, intensity) * 0.90D);
    }

    public static int tier(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) return 0;
        if (dose < 100.0D) return 1;
        if (dose < 250.0D) return 2;
        if (dose < 500.0D) return 3;
        double octaves = Math.log(Math.max(1.0D, dose / 500.0D)) / Math.log(2.0D);
        return 4 + (int) Math.min(1_000_000.0D, Math.floor(octaves));
    }

    public static int baseDurationTicks(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        double seconds;
        if (dose <= 25.0D) {
            seconds = 120.0D * smoothstep(0.0D, 25.0D, dose);
        } else if (dose <= 100.0D) {
            seconds = 120.0D + 90.0D * smoothstep(25.0D, 100.0D, dose);
        } else if (dose <= 250.0D) {
            seconds = 210.0D + 90.0D * smoothstep(100.0D, 250.0D, dose);
        } else {
            seconds = 300.0D + 60.0D * smoothstep(250.0D, 500.0D, dose);
        }

        // Very large initial mixed doses remain longer without recreating the former
        // 24-minute baseline. Each doubling beyond 500 mg adds only 45 seconds.
        double extremeExtension = dose > 500.0D ? Math.log(dose / 500.0D) / Math.log(2.0D) * 45.0D : 0.0D;
        return Math.min(MAXIMUM_DURATION_TICKS,
                safeTicks((seconds + extremeExtension) * 20.0D * TrippyConfig.experienceTimeScale));
    }

    public static int stackExtensionTicks(double addedDoseMilligrams, double totalDoseMilligrams) {
        // Stacking extends by the duration represented by the newly added amount:
        // another 500 mg adds about six minutes at the default time scale.
        return baseDurationTicks(addedDoseMilligrams);
    }

    /** Absolute ceiling for one continuous trip, regardless of dose stacking. */
    public static int maximumDurationTicks() {
        return MAXIMUM_DURATION_TICKS;
    }

    public static MescalinePhase phase(int ageTicks, int durationTicks) {
        double p = progress(ageTicks, durationTicks);
        if (durationTicks <= 0 || ageTicks >= durationTicks) return MescalinePhase.SOBER;
        if (p < 0.20D) return MescalinePhase.COME_UP;
        if (p < 0.39D) return MescalinePhase.OPENING;
        if (p < 0.62D) return MescalinePhase.PLATEAU;
        if (p < 0.74D) return MescalinePhase.DEEP_PEAK;
        if (p < 0.92D) return MescalinePhase.DESCENT;
        return MescalinePhase.AFTERGLOW;
    }

    public static double phaseEnvelope(MescalinePhase phase, double progress) {
        double p = clamp(progress, 0.0D, 1.0D);
        return switch (phase) {
            case SOBER -> 0.0D;
            // A new trip starts almost imperceptibly. Even a large dose must travel
            // through onset instead of exposing its peak-strength shader inputs at once.
            case COME_UP -> smoothstep(0.015D, 0.20D, p) * 0.12D;
            case OPENING -> 0.12D + smoothstep(0.20D, 0.39D, p) * 0.56D;
            case PLATEAU -> 0.68D + smoothstep(0.39D, 0.62D, p) * 0.26D;
            case DEEP_PEAK -> 0.94D + Math.sin((p - 0.62D) / 0.12D * Math.PI) * 0.06D;
            case DESCENT -> 0.94D - smoothstep(0.74D, 0.92D, p) * 0.72D;
            case AFTERGLOW -> 0.22D - smoothstep(0.92D, 1.0D, p) * 0.215D;
        };
    }

    public static double progress(int ageTicks, int durationTicks) {
        return durationTicks <= 0 ? 1.0D : clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) return value >= edge1 ? 1.0D : 0.0D;
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static double safeAdd(double a, double b) {
        a = sanitizeDose(a);
        b = sanitizeDose(b);
        return a > Double.MAX_VALUE - b ? Double.MAX_VALUE : a + b;
    }

    public static double sanitizeDose(double value) {
        if (Double.isNaN(value) || value <= 0.0D) return 0.0D;
        return Double.isInfinite(value) ? Double.MAX_VALUE : value;
    }

    private static int safeTicks(double ticks) {
        if (!Double.isFinite(ticks) || ticks >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(20, (int) Math.round(ticks));
    }
}
