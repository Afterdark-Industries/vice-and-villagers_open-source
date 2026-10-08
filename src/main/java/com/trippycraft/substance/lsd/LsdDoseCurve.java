package com.trippycraft.substance.lsd;

import com.trippycraft.config.TrippyConfig;

public final class LsdDoseCurve {
    private LsdDoseCurve() {
    }

    public static double visualIntensity(double microtabs) {
        return perceptualIntensity(microtabs);
    }

    public static double peakIntensity(double microtabs) {
        return perceptualIntensity(microtabs);
    }

    public static int tier(double microtabs) {
        double dose = sanitizeDose(microtabs);
        if (dose <= 0.0D) {
            return 0;
        }
        if (dose < threshold2()) {
            return 1;
        }
        if (dose < threshold3()) {
            return 2;
        }
        if (dose < threshold4()) {
            return 3;
        }
        if (dose < threshold5()) {
            return 4;
        }
        return 5;
    }

    public static int baseDurationTicks(double microtabs) {
        double dose = sanitizeDose(microtabs);
        if (dose <= 0.0D) return 0;
        int seconds;
        if (dose <= midpoint(threshold1(), threshold2())) {
            seconds = TrippyConfig.lsd50ugDurationSeconds;
        } else if (dose <= midpoint(threshold2(), threshold4())) {
            seconds = TrippyConfig.lsd100ugDurationSeconds;
        } else if (dose <= midpoint(threshold4(), threshold5())) {
            seconds = TrippyConfig.lsd250ugDurationSeconds;
        } else {
            seconds = TrippyConfig.lsdStackedDurationSeconds;
        }
        long ticks = Math.round(seconds * 20.0D * TrippyConfig.experienceTimeScale);
        return (int) Math.max(20L, Math.min(Integer.MAX_VALUE, ticks));
    }

    public static LsdPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return LsdPhase.SOBER;
        }
        double progress = ageTicks / (double) durationTicks;
        if (progress < 0.11D) {
            return LsdPhase.ONSET;
        }
        if (progress < 0.30D) {
            return LsdPhase.COME_UP;
        }
        if (progress < 0.62D) {
            return LsdPhase.PEAK;
        }
        if (progress < 0.78D) {
            return LsdPhase.WAVE;
        }
        if (progress < 0.92D) {
            return LsdPhase.COMEDOWN;
        }
        return LsdPhase.AFTERGLOW;
    }

    public static double phaseEnvelope(LsdPhase phase, double progress) {
        if (phase == LsdPhase.SOBER) {
            return 0.0D;
        }
        double p = clamp(progress, 0.0D, 1.0D);
        // Each join has matching value, velocity, and acceleration. Phase names are
        // descriptive labels, so changing one cannot switch a rendering filter.
        if (p < 0.11D) return smootherstep(0.012D, 0.11D, p) * 0.08D;
        if (p < 0.30D) return 0.08D + smootherstep(0.11D, 0.30D, p) * 0.92D;
        if (p < 0.62D) return 1.0D - smootherstep(0.30D, 0.62D, p) * 0.04D;
        if (p < 0.78D) return 0.96D - smootherstep(0.62D, 0.78D, p) * 0.34D;
        if (p < 0.92D) return 0.62D - smootherstep(0.78D, 0.92D, p) * 0.48D;
        return 0.14D * (1.0D - smootherstep(0.92D, 1.0D, p));
    }

    private static double perceptualIntensity(double microtabs) {
        double dose = sanitizeDose(microtabs);
        double t1 = threshold1();
        double t2 = threshold2();
        double t3 = threshold3();
        double t4 = threshold4();
        double t5 = threshold5();
        double tailStart = t5 + Math.max(160.0D, t5 * 0.62D);
        double value = 0.34D * smoothstep(0.0D, t1, dose)
                + 0.72D * smoothstep(t1, t2, dose)
                + 1.45D * smoothstep(t2, midpoint(t3, t4), dose)
                + 2.10D * smoothstep(midpoint(t3, t4), t4, dose)
                + 2.45D * smoothstep(t4, t5, dose)
                + 2.20D * smoothstep(t5, tailStart, dose);
        // Preserve the authored ordinary-dose curve, then keep growing past its last
        // tier. hypot is overflow-safe and joins the previous segment at zero slope.
        double beyond = Math.max(0.0D, (dose - tailStart) / tailStart);
        double openEnded = value + 18.0D * Math.log(Math.hypot(1.0D, beyond));
        return openEnded * clamp(TrippyConfig.globalIntensity, 0.0D, 10.0D);
    }

    public static double smootherstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    static double sanitizeDose(double dose) {
        return Double.isFinite(dose) && dose > 0.0D ? dose : 0.0D;
    }

    static double safeAdd(double first, double second) {
        first = sanitizeDose(first);
        second = sanitizeDose(second);
        return first > Double.MAX_VALUE - second ? Double.MAX_VALUE : first + second;
    }

    private static double threshold1() {
        return Math.max(1.0D, TrippyConfig.lsdTier1Micrograms);
    }

    private static double threshold2() {
        return Math.max(threshold1() + 1.0D, TrippyConfig.lsdTier2Micrograms);
    }

    private static double threshold3() {
        return Math.max(threshold2() + 1.0D, TrippyConfig.lsdTier3Micrograms);
    }

    private static double threshold4() {
        return Math.max(threshold3() + 1.0D, TrippyConfig.lsdTier4Micrograms);
    }

    private static double threshold5() {
        return Math.max(Math.max(threshold4() + 1.0D, TrippyConfig.lsdTier5Micrograms), 800.0D);
    }

    private static double midpoint(double a, double b) {
        return (a + b) * 0.5D;
    }
}
