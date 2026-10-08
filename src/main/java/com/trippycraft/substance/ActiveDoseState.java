package com.trippycraft.substance;

public record ActiveDoseState(
        SubstanceType type,
        boolean active,
        double activeDose,
        double targetDose,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        double progress,
        int tier,
        double intensity,
        double visualIntensity
) {
    public static ActiveDoseState inactive(SubstanceType type) {
        return new ActiveDoseState(type, false, 0.0D, 0.0D, 0, 0, 0, 1.0D, 0, 0.0D, 0.0D);
    }

    public double remainingProgress() {
        if (durationTicks <= 0) {
            return 0.0D;
        }
        return clamp(remainingTicks / (double) durationTicks);
    }

    public double dosePresence(double low, double high) {
        return smoothstep(low, high, activeDose);
    }

    public double intensityPresence(double visualCap) {
        double cap = Math.max(1.0D, visualCap);
        return clamp((intensity * 0.55D + visualIntensity * 0.45D) / cap);
    }

    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    public static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
