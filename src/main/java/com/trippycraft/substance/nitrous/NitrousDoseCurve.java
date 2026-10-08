package com.trippycraft.substance.nitrous;

import com.trippycraft.config.TrippyConfig;

/** Fictional gameplay hits and timing for nitrous canisters. */
public final class NitrousDoseCurve {
    private NitrousDoseCurve() {
    }

    public static double peakIntensity(double hits) {
        return Math.log1p(sanitizeHits(hits)) * 2.85D * intensityScale();
    }

    /** Unbounded. Each extra hit keeps adding charge. */
    public static double visualAmplitude(double hits) {
        return Math.log1p(sanitizeHits(hits)) * 1.85D * intensityScale();
    }

    public static double visualPresence(double intensity) {
        return 1.0D - Math.exp(-Math.max(0.0D, intensity) * 0.85D);
    }

    public static int tier(double hits) {
        double dose = sanitizeHits(hits);
        if (dose <= 0.0D) {
            return 0;
        }
        if (dose < 1.5D) {
            return 1;
        }
        if (dose < 2.5D) {
            return 2;
        }
        return 3;
    }

    public static int baseDurationTicks(double hits) {
        double dose = sanitizeHits(hits);
        if (dose <= 0.0D) {
            return 0;
        }
        double seconds = 16.0D;
        double scale = clamp(TrippyConfig.experienceTimeScale, 0.01D, 100.0D);
        return (int) Math.round(clamp(seconds * 20.0D * scale, 20.0D, maximumDurationTicks()));
    }

    public static int maximumDurationTicks() {
        return 8 * 60 * 20;
    }

    public static NitrousPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return NitrousPhase.SOBER;
        }
        double p = progress(ageTicks, durationTicks);
        if (p < 0.08D) {
            return NitrousPhase.INHALE;
        }
        if (p < 0.22D) {
            return NitrousPhase.RUSH;
        }
        if (p < 0.62D) {
            return NitrousPhase.PEAK;
        }
        if (p < 0.86D) {
            return NitrousPhase.FADE;
        }
        return NitrousPhase.AFTERGLOW;
    }

    public static double phaseEnvelope(NitrousPhase phase, double progress) {
        double p = clamp(progress, 0.0D, 1.0D);
        return switch (phase) {
            case SOBER -> 0.0D;
            case INHALE -> smoothstep(0.00D, 0.08D, p) * 0.55D;
            case RUSH -> 0.55D + smoothstep(0.08D, 0.22D, p) * 0.45D;
            case PEAK -> 1.0D;
            case FADE -> 1.0D - smoothstep(0.62D, 0.86D, p) * 0.72D;
            case AFTERGLOW -> 0.28D * (1.0D - smoothstep(0.86D, 1.0D, p));
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

    public static double sanitizeHits(double value) {
        if (Double.isNaN(value) || value <= 0.0D) {
            return 0.0D;
        }
        return Double.isInfinite(value) ? Double.MAX_VALUE : value;
    }

    public static double safeAdd(double a, double b) {
        a = sanitizeHits(a);
        b = sanitizeHits(b);
        return a > Double.MAX_VALUE - b ? Double.MAX_VALUE : a + b;
    }

    private static double intensityScale() {
        return clamp(TrippyConfig.globalIntensity, 0.0D, 100.0D);
    }
}
