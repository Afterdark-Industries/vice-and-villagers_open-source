package com.trippycraft.substance.dmt;

import com.trippycraft.config.TrippyConfig;

public final class DmtDoseCurve {
    private DmtDoseCurve() {
    }

    public static double visualIntensity(double doseUnits) {
        return perceptualIntensity(doseUnits);
    }

    public static double peakIntensity(double doseUnits) {
        return perceptualIntensity(doseUnits);
    }

    public static int tier(double doseUnits) {
        if (doseUnits <= 0.0D) {
            return 0;
        }
        if (doseUnits < threshold2()) {
            return 1;
        }
        if (doseUnits < threshold3()) {
            return 2;
        }
        if (doseUnits < threshold4()) {
            return 3;
        }
        if (doseUnits < threshold5()) {
            return 4;
        }
        return 5;
    }

    public static int baseDurationTicks(double doseUnits) {
        double dose = Math.max(0.0D, doseUnits);
        int seconds;
        if (dose <= midpoint(threshold1(), threshold2())) {
            seconds = TrippyConfig.dmtLightDurationSeconds;
        } else if (dose <= midpoint(threshold2(), threshold4())) {
            seconds = TrippyConfig.dmtStandardDurationSeconds;
        } else if (dose <= midpoint(threshold4(), threshold5())) {
            seconds = TrippyConfig.dmtBreakthroughDurationSeconds;
        } else {
            seconds = TrippyConfig.dmtStackedDurationSeconds;
        }
        return Math.max(20, (int) Math.round(seconds * 20.0D * TrippyConfig.experienceTimeScale));
    }

    public static DmtPhase phase(int ageTicks, int durationTicks) {
        return phase(ageTicks, durationTicks, 0.0D);
    }

    public static DmtPhase phase(int ageTicks, int durationTicks, double doseUnits) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return DmtPhase.SOBER;
        }
        double progress = ageTicks / (double) durationTicks;
        if (progress < 0.08D) {
            return DmtPhase.RUSH;
        }
        if (progress < 0.22D) {
            return DmtPhase.TUNNEL;
        }
        if (doseUnits >= TrippyConfig.dmtBreakthroughThresholdUnits && progress < TrippyConfig.dmtReturnStartProgress) {
            return DmtPhase.BREAKTHROUGH;
        }
        if (progress < TrippyConfig.dmtAfterglowStartProgress) {
            return DmtPhase.RETURN;
        }
        return DmtPhase.AFTERGLOW;
    }

    public static double phaseEnvelope(DmtPhase phase, double progress) {
        if (phase == DmtPhase.SOBER) {
            return 0.0D;
        }
        double p = clamp(progress, 0.0D, 1.0D);
        double rush = smoothstep(0.0D, 0.06D, p) * (1.0D - smoothstep(0.15D, 0.28D, p));
        double tunnel = smoothstep(0.05D, 0.18D, p) * (1.0D - smoothstep(0.34D, 0.50D, p));
        double breakthrough = smoothstep(0.16D, 0.30D, p) * (1.0D - smoothstep(0.60D, 0.76D, p));
        double returns = smoothstep(0.55D, 0.70D, p) * (1.0D - smoothstep(0.86D, 0.96D, p));
        double afterglow = smoothstep(0.78D, 0.90D, p) * (1.0D - smoothstep(0.97D, 1.0D, p));
        double envelope = rush * 0.62D
                + tunnel * 0.92D
                + breakthrough * 1.16D
                + returns * 0.56D
                + afterglow * 0.16D;
        return clamp(envelope, 0.0D, 1.22D);
    }

    private static double perceptualIntensity(double doseUnits) {
        double dose = Math.max(0.0D, doseUnits);
        double t1 = threshold1();
        double t2 = threshold2();
        double t3 = threshold3();
        double t4 = threshold4();
        double t5 = threshold5();
        double replacementCeiling = Math.max(7.0D, t5);
        double value = 0.45D * smoothstep(0.0D, t1, dose)
                + 0.95D * smoothstep(t1, t2, dose)
                + 1.55D * smoothstep(t2, t3, dose)
                + 2.10D * smoothstep(t3, t4, dose)
                + 2.15D * smoothstep(t4, t5, dose)
                + 2.05D * smoothstep(t5, replacementCeiling, dose);
        value = Math.pow(Math.max(0.0D, value), Math.max(0.25D, TrippyConfig.dmtIntensityCurvePower));
        return clamp(value * TrippyConfig.globalIntensity, 0.0D, 10.0D);
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

    private static double threshold1() {
        return Math.max(0.05D, TrippyConfig.dmtTier1Units);
    }

    private static double threshold2() {
        return Math.max(threshold1() + 0.05D, TrippyConfig.dmtTier2Units);
    }

    private static double threshold3() {
        return Math.max(threshold2() + 0.05D, TrippyConfig.dmtTier3Units);
    }

    private static double threshold4() {
        return Math.max(threshold3() + 0.05D, TrippyConfig.dmtTier4Units);
    }

    private static double threshold5() {
        return Math.max(threshold4() + 0.05D, TrippyConfig.dmtTier5Units);
    }

    private static double midpoint(double a, double b) {
        return (a + b) * 0.5D;
    }
}
