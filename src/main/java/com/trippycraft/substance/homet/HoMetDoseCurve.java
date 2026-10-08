package com.trippycraft.substance.homet;

import com.trippycraft.config.TrippyConfig;

/** Fictional gameplay amounts and timing, independent of real-world pharmacology. */
public final class HoMetDoseCurve {
    private HoMetDoseCurve() { }

    public static double complexity(double doseMilligrams) {
        return Math.log1p(sanitizeDose(doseMilligrams) / 12.0D) / Math.log(2.0D)
                * 1.85D * highDoseAmplification(doseMilligrams) * intensityScale();
    }

    public static double peakIntensity(double doseMilligrams) {
        return Math.log1p(sanitizeDose(doseMilligrams) / 12.0D) * 1.65D
                * highDoseAmplification(doseMilligrams) * intensityScale();
    }

    /** Open-ended amplitude: 40 mg is 1, 50 mg is about 1.90, and every higher amount adds intensity. */
    public static double visualAmplitude(double doseMilligrams) {
        return Math.log1p(sanitizeDose(doseMilligrams) / 12.0D) / Math.log1p(40.0D / 12.0D)
                * highDoseAmplification(doseMilligrams) * intensityScale();
    }

    private static double highDoseAmplification(double doseMilligrams) {
        // A smooth opening into the stronger shader tier; the logarithmic dose response keeps growing past it.
        return 1.0D + 1.4D * smoothstep(40.0D, 60.0D, sanitizeDose(doseMilligrams));
    }

    /** One independent fictional gameplay roll per new dose, based on the session's consumed total. */
    public static double overdoseChanceForDose(double addedMilligrams, double consumedTotalMilligrams) {
        return sanitizeDose(addedMilligrams) > 0.0D && sanitizeDose(consumedTotalMilligrams) > 400.0D ? 0.30D : 0.0D;
    }

    public static boolean overdoseRollIsFatal(double addedMilligrams, double consumedTotalMilligrams, double randomRoll) {
        return Double.isFinite(randomRoll) && randomRoll >= 0.0D && randomRoll < 1.0D
                && randomRoll < overdoseChanceForDose(addedMilligrams, consumedTotalMilligrams);
    }

    public static double visualPresence(double intensity) {
        return 1.0D - Math.exp(-Math.max(0.0D, intensity));
    }

    public static int tier(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) return 0;
        if (dose < 10.0D) return 1;
        if (dose < 20.0D) return 2;
        if (dose < 40.0D) return 3;
        return 4 + (int) Math.floor(Math.log(dose / 40.0D) / Math.log(2.0D));
    }

    public static int baseDurationTicks(double doseMilligrams) {
        double dose = sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) return 0;
        double minutes = 3.0D + 4.0D * Math.log1p(dose / 5.0D) / Math.log(9.0D);
        double scale = clamp(TrippyConfig.experienceTimeScale, 0.01D, 100.0D);
        return (int) Math.round(clamp(minutes * 60.0D * 20.0D * scale, 20.0D, maximumDurationTicks()));
    }

    public static int maximumDurationTicks() { return 45 * 60 * 20; }

    public static HoMetPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) return HoMetPhase.SOBER;
        double p = progress(ageTicks, durationTicks);
        if (p < 0.18D) return HoMetPhase.COME_UP;
        if (p < 0.32D) return HoMetPhase.OPENING;
        if (p < 0.43D) return HoMetPhase.PEAK;
        if (p < 0.72D) return HoMetPhase.PLATEAU;
        if (p < 0.93D) return HoMetPhase.DESCENT;
        return HoMetPhase.AFTERGLOW;
    }

    /** All joins have matching values and zero slope, including the final sober join. */
    public static double phaseEnvelope(HoMetPhase phase, double progress) {
        double p = clamp(progress, 0.0D, 1.0D);
        return switch (phase) {
            case SOBER -> 0.0D;
            case COME_UP -> smoothstep(0.01D, 0.18D, p) * 0.16D;
            case OPENING -> 0.16D + smoothstep(0.18D, 0.32D, p) * 0.64D;
            case PEAK -> 0.80D + smoothstep(0.32D, 0.43D, p) * 0.20D;
            case PLATEAU -> 1.0D - smoothstep(0.43D, 0.72D, p) * 0.08D;
            case DESCENT -> 0.92D - smoothstep(0.72D, 0.93D, p) * 0.82D;
            case AFTERGLOW -> 0.10D * (1.0D - smoothstep(0.93D, 1.0D, p));
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

    public static double sanitizeDose(double value) {
        if (Double.isNaN(value) || value <= 0.0D) return 0.0D;
        return Double.isInfinite(value) ? Double.MAX_VALUE : value;
    }

    public static double safeAdd(double a, double b) {
        a = sanitizeDose(a); b = sanitizeDose(b);
        return a > Double.MAX_VALUE - b ? Double.MAX_VALUE : a + b;
    }

    private static double intensityScale() { return clamp(TrippyConfig.globalIntensity, 0.0D, 100.0D); }
}
