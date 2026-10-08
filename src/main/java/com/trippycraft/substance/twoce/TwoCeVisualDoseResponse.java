package com.trippycraft.substance.twoce;

/**
 * Additional visual detail above the established fictional 250mg content tier.
 *
 * The content ledger and calibrated effect amount stay separate. Only the
 * absorbed, tolerance-adjusted fraction of displayed content opens new layers,
 * and the synchronized session envelope controls their arrival and departure.
 * No item, network, persistence or timing contract changes are needed.
 */
public record TwoCeVisualDoseResponse(double surfaceComplexity, double architecturalDepth,
        double perspectiveComplexity, double immersiveGeometry) {

    private static final TwoCeVisualDoseResponse NONE = new TwoCeVisualDoseResponse(0, 0, 0, 0);

    public static TwoCeVisualDoseResponse resolve(double displayContentMilligrams,
            double activeEffectMilligrams, double targetEffectMilligrams, double phaseEnvelope) {
        double envelope = unit(phaseEnvelope);
        double content = effectiveContentMilligrams(displayContentMilligrams,
                activeEffectMilligrams, targetEffectMilligrams);
        if (content <= 250.0D || envelope <= 0.0D) {
            return NONE;
        }
        return new TwoCeVisualDoseResponse(
                detail(content, 250.0D, 250.0D) * envelope,
                detail(content, 650.0D, 600.0D) * envelope,
                detail(content, 1500.0D, 1000.0D) * envelope,
                detail(content, 2800.0D, 850.0D) * envelope);
    }

    public static double effectiveContentMilligrams(double displayContentMilligrams,
            double activeEffectMilligrams, double targetEffectMilligrams) {
        double content = positiveFinite(displayContentMilligrams);
        double active = positiveFinite(activeEffectMilligrams);
        double target = positiveFinite(targetEffectMilligrams);
        if (content <= 0.0D || active <= 0.0D || target <= 0.0D) {
            return 0.0D;
        }
        // Divide a bounded numerator, so even extreme valid inputs cannot
        // overflow. Stacked content follows actual absorption and tolerance.
        return content * (Math.min(active, target) / target);
    }

    private static double detail(double content, double threshold, double span) {
        if (content <= threshold) {
            return 0.0D;
        }
        double expansion = (content - threshold) / span;
        // A squared logarithmic ramp has zero slope at its threshold. The
        // rational tail retains increasing complexity through 5000mg and past
        // it without unbounded shader amplitudes or overflowing the square.
        double lift = expansion <= Math.sqrt(Double.MAX_VALUE)
                ? Math.log1p(expansion * expansion) : 2.0D * Math.log(expansion);
        return lift / (1.0D + lift);
    }

    private static double unit(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }

    private static double positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
