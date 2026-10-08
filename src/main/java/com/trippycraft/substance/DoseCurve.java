package com.trippycraft.substance;

public record DoseCurve(
        double thresholdUnits,
        double commonUnits,
        double heavyUnits,
        int onsetTicks,
        int peakTicks,
        int durationTicks
) {
    public double normalizedDose(double abstractUnits) {
        if (abstractUnits <= thresholdUnits) {
            return 0.0D;
        }
        double span = Math.max(heavyUnits - thresholdUnits, 0.0001D);
        return saturate((abstractUnits - thresholdUnits) / span);
    }

    public double phaseIntensity(int ageTicks, double timeScale) {
        double scale = Math.max(timeScale, 0.1D);
        double scaledAge = ageTicks / scale;
        double rise = smoothstep(scaledAge / Math.max(onsetTicks, 1));
        double peakEase = smoothstep((scaledAge - onsetTicks) / Math.max(peakTicks - onsetTicks, 1));
        double tail = 1.0D - smoothstep((scaledAge - peakTicks) / Math.max(durationTicks - peakTicks, 1));
        double plateau = Math.max(rise, peakEase);
        return saturate(plateau * tail);
    }

    public boolean isExpired(int ageTicks, double timeScale) {
        return ageTicks / Math.max(timeScale, 0.1D) > durationTicks;
    }

    static double smoothstep(double value) {
        double x = saturate(value);
        return x * x * (3.0D - 2.0D * x);
    }

    static double saturate(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
