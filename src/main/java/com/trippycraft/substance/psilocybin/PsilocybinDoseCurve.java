package com.trippycraft.substance.psilocybin;

public final class PsilocybinDoseCurve {
    private PsilocybinDoseCurve() {
    }

    public static double normalizedDose(double grams) {
        return sanitizeDose(grams) / 5.0D;
    }

    public static double visualIntensity(double grams) {
        return perceptualIntensity(grams);
    }

    public static double peakIntensity(double grams) {
        return perceptualIntensity(grams);
    }

    private static double perceptualIntensity(double grams) {
        double g = sanitizeDose(grams);
        double value = 0.10D * smoothstep(0.0D, 0.5D, g)
                + 0.45D * smoothstep(0.75D, 1.0D, g)
                + 0.70D * smoothstep(1.0D, 1.5D, g)
                + 0.80D * smoothstep(1.5D, 2.0D, g)
                + 0.80D * smoothstep(2.0D, 2.5D, g)
                + 0.70D * smoothstep(2.5D, 3.0D, g)
                + 0.65D * smoothstep(3.0D, 3.5D, g)
                + 0.60D * smoothstep(3.5D, 4.0D, g)
                + 0.75D * smoothstep(4.0D, 5.0D, g)
                + 1.40D * smoothstep(5.0D, 7.0D, g)
                + 1.75D * smoothstep(7.0D, 10.0D, g)
                + 1.30D * smoothstep(10.0D, 14.0D, g);
        // Preserve the authored capsule strengths, then keep increasing beyond the last tier.
        // hypot avoids overflow and makes the new tail meet 14 g with zero slope.
        double beyond = Math.max(0.0D, (g - 14.0D) / 14.0D);
        // Keep every existing visual amplitude through 8 g. Above that point,
        // a gradual extra gain makes stacked doses visibly deepen the same
        // breathing/melting fields instead of approaching a perceptual plateau.
        // log1p is almost linear at normal gameplay amounts and remains finite
        // for every finite double dose; it never imposes an intensity ceiling.
        double escalation = 1_024.0D * Math.log1p(Math.max(0.0D, (g - 8.0D) / 1_024.0D))
                * smootherstep(8.0D, 12.0D, g);
        return value + 8.0D * Math.log(Math.hypot(1.0D, beyond)) + escalation;
    }

    public static int baseDurationTicks(double grams) {
        if (sanitizeDose(grams) <= 0.0D) {
            return 0;
        }
        if (grams <= 0.55D) {
            return 90 * 20;
        }
        if (grams <= 1.2D) {
            return 180 * 20;
        }
        if (grams <= 3.4D) {
            return 300 * 20;
        }
        return 420 * 20;
    }

    public static PsilocybinPhase phase(int ageTicks, int durationTicks) {
        if (durationTicks <= 0 || ageTicks >= durationTicks) {
            return PsilocybinPhase.SOBER;
        }
        double progress = ageTicks / (double) durationTicks;
        if (progress < 0.10D) {
            return PsilocybinPhase.ONSET;
        }
        if (progress < 0.30D) {
            return PsilocybinPhase.COME_UP;
        }
        if (progress < 0.65D) {
            return PsilocybinPhase.PEAK;
        }
        if (progress < 0.85D) {
            return PsilocybinPhase.WAVE;
        }
        return PsilocybinPhase.AFTERGLOW;
    }

    public static double phaseEnvelope(PsilocybinPhase phase, double progress) {
        if (phase == PsilocybinPhase.SOBER) {
            return 0.0D;
        }
        double p = clamp(progress, 0.0D, 1.0D);
        // This continuous curve is independent of the phase label. Values, slopes and
        // accelerations agree at every join, so phase changes cannot kick texture/color motion.
        if (p < 0.10D) {
            return smootherstep(0.012D, 0.10D, p) * 0.08D;
        }
        if (p < 0.30D) {
            return 0.08D + smootherstep(0.10D, 0.30D, p) * 0.92D;
        }
        if (p < 0.65D) {
            return 1.0D - smootherstep(0.30D, 0.65D, p) * 0.04D;
        }
        if (p < 0.85D) {
            return 0.96D - smootherstep(0.65D, 0.85D, p) * 0.70D;
        }
        return 0.26D * (1.0D - smootherstep(0.85D, 1.0D, p));
    }

    static double smootherstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    static double sanitizeDose(double grams) {
        return Double.isFinite(grams) && grams > 0.0D ? grams : 0.0D;
    }

    static double safeAdd(double first, double second) {
        first = sanitizeDose(first);
        second = sanitizeDose(second);
        return first > Double.MAX_VALUE - second ? Double.MAX_VALUE : first + second;
    }
}
