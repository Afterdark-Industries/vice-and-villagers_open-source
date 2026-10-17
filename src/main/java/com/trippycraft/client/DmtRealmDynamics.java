package com.trippycraft.client;

/** Dose energy and cinematic timing, independent of the legacy visual ceiling. */
public final class DmtRealmDynamics {
    private static final double LOG_TWO = Math.log(2.0D);

    private DmtRealmDynamics() {
    }

    public static double doseLog(double dose) {
        return Math.log1p(positiveDose(dose)) / LOG_TWO;
    }

    public static double doseLog(double dose, double threshold) {
        double amount = positiveDose(dose);
        double reference = Double.isFinite(threshold) ? Math.max(0.0001D, threshold) : 1.0D;
        // Avoid overflowing amount/reference for very large finite doses.
        return amount <= reference ? Math.log1p(amount / reference) / LOG_TWO
                : (Math.log(amount) - Math.log(reference) + Math.log1p(reference / amount)) / LOG_TWO;
    }

    public static double cinematicEntry(double crossingSeconds, double doseEnergy) {
        double energy = Math.max(0.0D, Double.isFinite(doseEnergy) ? doseEnergy : 0.0D);
        // Stronger trips cross somewhat faster, but retain a substantial unfolding.
        double duration = 32.0D - 6.0D * (1.0D - 1.0D / (1.0D + energy));
        return smootherstep(0.0D, duration, crossingSeconds);
    }

    /** Integrate this rate; never multiply an already elapsed animation phase. */
    public static double transformationRate(double doseEnergy) {
        return 1.0D + Math.log1p(positiveDose(doseEnergy)) * 0.45D;
    }

    /** Preserve a quiet residual while the remaining realm fades back to the world. */
    public static double settlingGain(double settledAmount, double residual) {
        double amount = Double.isFinite(settledAmount) ? Math.max(0.0D, Math.min(1.0D, settledAmount)) : 0.0D;
        double floor = Double.isFinite(residual) ? Math.max(0.0D, Math.min(1.0D, residual)) : 0.0D;
        return 1.0D - amount * (1.0D - floor);
    }

    public static double smootherstep(double low, double high, double value) {
        double t = Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D,
                (value - low) / Math.max(0.0001D, high - low))) : 0.0D;
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double positiveDose(double dose) {
        return Double.isNaN(dose) || dose <= 0.0D ? 0.0D : Math.min(Double.MAX_VALUE, dose);
    }
}
