package com.trippycraft.substance.twocb;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.mdma.MdmaDoseCurve;

/** Fictional gameplay dose curve; milligram labels are not real-world guidance. */
public final class TwoCbDoseCurve {
    private TwoCbDoseCurve() {
    }

    /** 2C-B deliberately shares MDMA's authored active-trip clock. */
    public static int baseDurationTicks(double doseMilligrams) {
        return MdmaDoseCurve.baseDurationTicks(doseMilligrams);
    }

    public static int stackExtensionTicks(double addedDoseMilligrams, double totalDoseMilligrams) {
        return MdmaDoseCurve.stackExtensionTicks(addedDoseMilligrams, totalDoseMilligrams);
    }

    public static TwoCbPhase phase(int ageTicks, int durationTicks) {
        double progress = progress(ageTicks, durationTicks);
        if (progress < 0.12D) {
            return TwoCbPhase.ONSET;
        }
        if (progress < 0.28D) {
            return TwoCbPhase.LIFT;
        }
        if (progress < 0.60D) {
            return TwoCbPhase.PEAK;
        }
        if (progress < 0.82D) {
            return TwoCbPhase.CRYSTALLINE;
        }
        return TwoCbPhase.DESCENT;
    }

    public static double phaseEnvelope(TwoCbPhase phase, double progress) {
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> 0.10D + smoothstep(0.0D, 0.12D, progress) * 0.52D;
            case LIFT -> 0.62D + smoothstep(0.12D, 0.28D, progress) * 0.36D;
            case PEAK -> 0.98D + Math.sin(progress * Math.PI * 8.0D) * 0.025D;
            case CRYSTALLINE -> 0.94D - smoothstep(0.60D, 0.82D, progress) * 0.18D;
            case DESCENT -> 0.76D - smoothstep(0.82D, 1.0D, progress) * 0.70D;
        };
    }

    public static double peakIntensity(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        double authoredIntensity = 0.34D * smoothstep(0.0D, 25.0D, dose)
                + 0.48D * smoothstep(25.0D, 100.0D, dose)
                + 0.50D * smoothstep(100.0D, 250.0D, dose)
                + 0.54D * smoothstep(250.0D, 500.0D, dose);
        double extremeEscalation = 2.0D * smoothstep(500.0D, 1_000.0D, dose)
                + 4.0D * smoothstep(1_000.0D, 2_000.0D, dose)
                + 5.0D * smoothstep(2_000.0D, 3_000.0D, dose);
        return clamp((authoredIntensity * 2.0D + extremeEscalation) * TrippyConfig.globalIntensity,
                0.0D, 16.0D);
    }

    public static int tier(double doseMilligrams) {
        if (doseMilligrams < 50.0D) {
            return 1;
        }
        if (doseMilligrams < 175.0D) {
            return 2;
        }
        if (doseMilligrams < 375.0D) {
            return 3;
        }
        if (doseMilligrams < 750.0D) {
            return 4;
        }
        if (doseMilligrams < 1_000.0D) {
            return 5;
        }
        if (doseMilligrams < 2_000.0D) {
            return 6;
        }
        if (doseMilligrams < 2_500.0D) {
            return 7;
        }
        if (doseMilligrams < 3_000.0D) {
            return 8;
        }
        return 9;
    }

    public static double progress(int ageTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
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
}
