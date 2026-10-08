package com.trippycraft.substance.allad;

import com.trippycraft.config.TrippyConfig;

/** Fictional gameplay amounts and timing for AL-LAD blotters. */
public final class AlLadDoseCurve {
    private AlLadDoseCurve() {
    }

    private static final double VISUAL_STRENGTH = 1.5D;

    public static double peakIntensity(double doseMicrograms) {
        return Math.log1p(sanitizeDose(doseMicrograms) / 40.0D) * 1.55D
                * highDoseAmplification(doseMicrograms) * intensityScale() * VISUAL_STRENGTH;
    }

    public static double visualAmplitude(double doseMicrograms) {
        return Math.log1p(sanitizeDose(doseMicrograms) / 40.0D) / Math.log1p(150.0D / 40.0D)
                * highDoseAmplification(doseMicrograms) * intensityScale() * VISUAL_STRENGTH;
    }

    private static double highDoseAmplification(double doseMicrograms) {
        double dose = sanitizeDose(doseMicrograms);
        return 1.0D
                + 0.55D * smoothstep(140.0D, 170.0D, dose)
                + 0.85D * smoothstep(280.0D, 340.0D, dose)
                + 1.70D * smoothstep(420.0D, 560.0D, dose)
                + 2.40D * smoothstep(560.0D, 900.0D, dose);
    }

    /** Unbounded heroic lift past 450ug. 0 below ~420ug, then keeps growing. */
    public static double heroicLift(double doseMicrograms) {
        return Math.log1p(Math.max(0.0D, sanitizeDose(doseMicrograms) - 420.0D) / 70.0D);
    }

    public static double visualPresence(double intensity) {
        return 1.0D - Math.exp(-Math.max(0.0D, intensity));
    }

    public static int tier(double doseMicrograms) {
        double dose = sanitizeDose(doseMicrograms);
        if (dose <= 0.0D) {
            return 0;
        }
        if (dose < 90.0D) {
            return 1;
        }
        if (dose < 180.0D) {
            return 2;
        }
        if (dose < 360.0D) {
            return 3;
        }
        if (dose < 600.0D) {
            return 4;
        }
        return 5;
    }

    public static int baseDurationTicks(double doseMicrograms) {
        double dose = sanitizeDose(doseMicrograms);
        if (dose <= 0.0D) {
            return 0;
        }
        int seconds;
        if (dose <= 90.0D) {
            seconds = 180;
        } else if (dose <= 200.0D) {
            seconds = 240;
        } else if (dose <= 360.0D) {
            seconds = 300;
        } else {
            seconds = 360;
        }
        double scale = clamp(TrippyConfig.experienceTimeScale, 0.01D, 100.0D);
        return (int) Math.round(clamp(seconds * 20.0D * scale, 20.0D, maximumDurationTicks()));
    }

    public static int maximumDurationTicks() {
        return 45 * 60 * 20;
    }

    public static AlLadPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return AlLadPhase.SOBER;
        }
        double p = progress(ageTicks, durationTicks);
        if (p < 0.12D) {
            return AlLadPhase.ONSET;
        }
        if (p < 0.28D) {
            return AlLadPhase.COME_UP;
        }
        if (p < 0.52D) {
            return AlLadPhase.PEAK;
        }
        if (p < 0.78D) {
            return AlLadPhase.PLATEAU;
        }
        if (p < 0.92D) {
            return AlLadPhase.DESCENT;
        }
        return AlLadPhase.AFTERGLOW;
    }

    public static double phaseEnvelope(AlLadPhase phase, double progress) {
        double p = clamp(progress, 0.0D, 1.0D);
        return switch (phase) {
            case SOBER -> 0.0D;
            case ONSET -> smoothstep(0.01D, 0.12D, p) * 0.22D;
            case COME_UP -> 0.22D + smoothstep(0.12D, 0.28D, p) * 0.58D;
            case PEAK -> 0.80D + smoothstep(0.28D, 0.52D, p) * 0.20D;
            case PLATEAU -> 1.0D - smoothstep(0.52D, 0.78D, p) * 0.08D;
            case DESCENT -> 0.92D - smoothstep(0.78D, 0.92D, p) * 0.74D;
            case AFTERGLOW -> 0.18D * (1.0D - smoothstep(0.92D, 1.0D, p));
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
