package com.trippycraft.substance.mdma;

import com.trippycraft.config.TrippyConfig;

public final class MdmaDoseCurve {
    public static final int MINECRAFT_DAY_TICKS = 24_000;
    public static final int MAX_RECOVERY_TICKS = MINECRAFT_DAY_TICKS * 2;
    private static final double ACTIVE_DURATION_MULTIPLIER = 1.5D;

    private MdmaDoseCurve() {
    }

    public static int baseDurationTicks(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        double load = 1.0D - Math.exp(-dose / 120.0D);
        double heavyTail = smoothstep(250.0D, 500.0D, dose);
        double seconds = 125.0D + load * 105.0D + heavyTail * 35.0D;
        return secondsToTicks(seconds * ACTIVE_DURATION_MULTIPLIER * TrippyConfig.experienceTimeScale);
    }

    public static int stackExtensionTicks(double addedDoseMilligrams, double totalDoseMilligrams) {
        if (addedDoseMilligrams <= 0.0D) {
            return 0;
        }
        double servings = addedDoseMilligrams / 100.0D;
        double escalation = smoothstep(250.0D, 750.0D, totalDoseMilligrams);
        double seconds = 8.0D + servings * 13.0D + escalation * 16.0D;
        return secondsToTicks(seconds * ACTIVE_DURATION_MULTIPLIER * TrippyConfig.experienceTimeScale);
    }

    public static MdmaPhase phase(int ageTicks, int durationTicks) {
        double progress = progress(ageTicks, durationTicks);
        if (progress < 0.15D) {
            return MdmaPhase.COME_UP;
        }
        if (progress < 0.55D) {
            return MdmaPhase.PEAK;
        }
        return MdmaPhase.ROLL;
    }

    public static double phaseEnvelope(MdmaPhase phase, double progress) {
        return switch (phase) {
            case SOBER -> 0.0D;
            case COME_UP -> 0.14D + smoothstep(0.00D, 0.15D, progress) * 0.68D;
            case PEAK -> 0.96D + Math.sin(progress * Math.PI * 5.0D) * 0.035D;
            case ROLL -> 0.90D - smoothstep(0.55D, 1.00D, progress) * 0.72D;
            case COMEDOWN, HANGOVER -> 0.0D;
        };
    }

    /**
     * Recovery is deliberately outside the visible trip timeline. A 500mg session queues half a
     * Minecraft day, while lower doses have shorter tails and stacked totals can extend toward two days.
     */
    public static int recoveryDurationTicks(double doseMilligrams, int repeatedDoses) {
        double dose = Math.max(0.0D, doseMilligrams);
        double doseTicks = MINECRAFT_DAY_TICKS * (0.08D + dose * 0.00084D);
        double repeatTicks = Math.max(0, repeatedDoses) * MINECRAFT_DAY_TICKS * 0.06D;
        return (int) Math.round(clamp(doseTicks + repeatTicks,
                MINECRAFT_DAY_TICKS / 12.0D,
                MAX_RECOVERY_TICKS));
    }

    public static double peakIntensity(double doseMilligrams) {
        double dose = Math.max(0.0D, doseMilligrams);
        double intensity = 0.38D * smoothstep(0.0D, 25.0D, dose)
                + 0.40D * smoothstep(25.0D, 100.0D, dose)
                + 0.37D * smoothstep(100.0D, 250.0D, dose)
                + 0.40D * smoothstep(250.0D, 500.0D, dose);
        return clamp(intensity * TrippyConfig.globalIntensity, 0.0D, 2.2D);
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
        return 4;
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

    private static int secondsToTicks(double seconds) {
        return Math.max(20, (int) Math.round(seconds * 20.0D));
    }
}
