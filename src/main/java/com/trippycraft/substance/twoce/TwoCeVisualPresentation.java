package com.trippycraft.substance.twoce;

/**
 * Numeric client presentation contract. Ten times the absorbed physical content
 * drives the established 2C-E appearance, while new layers continue to develop
 * above 500 mg. Physical dose, body load, session timing and persistence stay on
 * their original server contracts. Shader validation compiles this class directly.
 */
public record TwoCeVisualPresentation(double virtualContentMilligrams, double virtualEffectMilligrams,
        double phaseEnvelope, double visualPresence, double cavities, double nestedRooms,
        double tessellation, double edgeLock, double perspectiveFold, double recursion,
        double corridorZoom, double symmetryLock, double geometricTrails,
        double gate10, double gate20, double gate40, double machineLift,
        TwoCeVisualDoseResponse highDose, ExtraDose extraDose) {
    private static final TwoCeVisualDoseResponse NO_HIGH_DOSE = new TwoCeVisualDoseResponse(0, 0, 0, 0);
    private static final ExtraDose NO_EXTRA_DOSE = new ExtraDose(0, 0, 0, 0);
    private static final TwoCeVisualPresentation NONE = new TwoCeVisualPresentation(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, NO_HIGH_DOSE, NO_EXTRA_DOSE);

    /** Synchronized visual channels already include phase and mixture response. */
    public record Channels(double intensity, double visualPresence, double cavities, double nestedRooms,
                           double tessellation, double edgeLock, double perspectiveFold, double recursion,
                           double corridorZoom, double symmetryLock, double geometricTrails) {
    }

    /** Smooth premium surface/spatial layers above the promoted baseline. */
    public record ExtraDose(double recursiveVaults, double impossiblePerspective,
                            double materialMorphogenesis, double immersiveLattice) {
    }

    public static TwoCeVisualPresentation resolve(double targetContentMilligrams,
            double activeEffectMilligrams, double targetEffectMilligrams, double globalIntensity,
            double visualIntensity, double symmetryBias, Channels channels) {
        // The target content ledger times the absorbed fraction stays stable at
        // a redose. The separately lagged HUD content would briefly halve the
        // existing appearance when its target rises before the display catches up.
        double physicalContent = TwoCeVisualDoseResponse.effectiveContentMilligrams(
                targetContentMilligrams, activeEffectMilligrams, targetEffectMilligrams);
        double actualEffect = positive(activeEffectMilligrams);
        double nativePeak = peakIntensity(actualEffect, globalIntensity);
        if (channels == null || physicalContent <= 0.0D || nativePeak <= 0.0D
                || !Double.isFinite(visualIntensity) || visualIntensity <= 0.0D
                || positive(channels.intensity()) <= 0.0D) {
            return NONE;
        }
        double virtualContent = physicalContent > Double.MAX_VALUE / 10.0D
                ? Double.MAX_VALUE : physicalContent * 10.0D;
        double virtualEffect = effectMilligramsForContent(virtualContent);
        double virtualPeak = peakIntensity(virtualEffect, globalIntensity);
        double nativePresence = presence(nativePeak);
        double virtualPresence = presence(virtualPeak);
        double nativeAmplitude = nativePresence * visualAmplitude(actualEffect, globalIntensity);
        double virtualAmplitude = virtualPresence * visualAmplitude(virtualEffect, globalIntensity);
        double envelope = Math.min(positive(channels.intensity()), nativePeak) / nativePeak;
        double receivedPresence = positive(channels.visualPresence());
        // Bounded state scaling can round a saturated presence back to one even
        // when intensity was attenuated. Preserve that received attenuation while
        // using the synchronized intensity ratio to prevent full-strength coverage.
        double promotedPresence = Math.min(clamp(receivedPresence * (virtualPresence / nativePresence), 0.0D, 1.0D),
                virtualPresence * envelope);
        Profile nativeProfile = profile(actualEffect);
        Profile virtualProfile = profile(virtualEffect);
        double recursive = nativeProfile.recursion() > 0.0D
                ? remap(channels.recursion(), nativeAmplitude, nativeProfile.recursion(),
                        virtualAmplitude, virtualProfile.recursion())
                : remap(channels.nestedRooms(), nativeAmplitude, nativeProfile.nested(),
                        virtualAmplitude, virtualProfile.recursion());
        double symmetry = nativeProfile.symmetry() > 0.0D
                ? remap(channels.symmetryLock(), nativePresence, nativeProfile.symmetry(),
                        virtualPresence, virtualProfile.symmetry())
                : virtualPresence * envelope * virtualProfile.symmetry() * clamp(symmetryBias, 0.0D, 2.0D);
        TwoCeVisualDoseResponse highDose = TwoCeVisualDoseResponse.resolve(virtualContent, 1.0D, 1.0D, envelope);
        double extraGain = envelope * clamp(visualIntensity, 0.0D, 1.35D);
        return new TwoCeVisualPresentation(virtualContent, virtualEffect, envelope, promotedPresence,
                remap(channels.cavities(), nativeAmplitude, nativeProfile.cavities(), virtualAmplitude, virtualProfile.cavities()),
                remap(channels.nestedRooms(), nativeAmplitude, nativeProfile.nested(), virtualAmplitude, virtualProfile.nested()),
                remap(channels.tessellation(), nativeAmplitude, nativeProfile.tessellation(), virtualAmplitude, virtualProfile.tessellation()),
                remap(channels.edgeLock(), nativeAmplitude, nativeProfile.edgeLock(), virtualAmplitude, virtualProfile.edgeLock()),
                remap(channels.perspectiveFold(), nativeAmplitude, nativeProfile.fold(), virtualAmplitude, virtualProfile.fold()),
                recursive,
                remap(channels.corridorZoom(), nativeAmplitude, nativeProfile.corridor(), virtualAmplitude, virtualProfile.corridor()),
                symmetry,
                remap(channels.geometricTrails(), nativeAmplitude, nativeProfile.trails(), virtualAmplitude, virtualProfile.trails()),
                unit((virtualEffect - 4.0D) / 8.0D), unit((virtualEffect - 12.0D) / 12.0D),
                unit((virtualEffect - 24.0D) / 20.0D), machineLift(virtualEffect), highDose,
                new ExtraDose(detail(physicalContent, 500.0D, 500.0D) * extraGain,
                        detail(physicalContent, 900.0D, 800.0D) * extraGain,
                        detail(physicalContent, 1800.0D, 1000.0D) * extraGain,
                        detail(physicalContent, 3000.0D, 1000.0D) * extraGain));
    }

    /** Inverse of the existing authored content/effect calibration, used only for display. */
    public static double effectMilligramsForContent(double contentMilligrams) {
        double content = positive(contentMilligrams);
        if (content <= 25.0D) return content / 2.5D;
        if (content <= 100.0D) return 10.0D + (content - 25.0D) / 7.5D;
        if (content <= 250.0D) return 20.0D + (content - 100.0D) / 7.5D;
        if (content <= 500.0D) return 40.0D + (content - 250.0D) / 6.25D;
        return content * 0.16D;
    }

    /** Mirrors the approved legacy curve so the plain-Java GPU harness shares exact input mapping. */
    public static double peakIntensity(double effectMilligrams, double globalIntensity) {
        return Math.log1p(positive(effectMilligrams) / 8.0D) * 1.55D
                * amplification(effectMilligrams) * clamp(globalIntensity, 0.0D, 100.0D) * 1.85D;
    }

    private static double visualAmplitude(double effect, double globalIntensity) {
        return Math.log1p(positive(effect) / 8.0D) / Math.log1p(40.0D / 8.0D)
                * amplification(effect) * clamp(globalIntensity, 0.0D, 100.0D) * 1.85D;
    }

    private static double amplification(double effect) {
        return 1.0D + 0.50D * smoothstep(8.0D, 16.0D, effect)
                + 0.85D * smoothstep(16.0D, 28.0D, effect)
                + 1.15D * smoothstep(32.0D, 50.0D, effect)
                + 1.80D * smoothstep(65.0D, 100.0D, effect);
    }

    private record Profile(double cavities, double nested, double tessellation, double edgeLock,
                           double fold, double recursion, double corridor, double symmetry, double trails) {
    }

    private static Profile profile(double effect) {
        double t2 = smoothstep(8.0D, 20.0D, effect);
        double t3 = smoothstep(18.0D, 40.0D, effect);
        double t4 = smoothstep(35.0D, 80.0D, effect);
        double machine = machineLift(effect);
        return new Profile(0.72D + t2 * 0.40D + t3 * 0.28D,
                0.32D + t2 * 0.50D + t3 * 0.45D + t4 * 0.28D,
                0.58D + t2 * 0.40D + t3 * 0.28D,
                0.74D + t2 * 0.35D + t3 * 0.22D,
                0.22D + t2 * 0.40D + t3 * 0.50D + t4 * 0.40D,
                t2 * 0.28D + t3 * 0.55D + t4 * 0.80D + machine * 0.60D,
                0.22D + t2 * 0.42D + t3 * 0.55D + t4 * 0.50D + machine * 0.45D,
                t2 * 0.16D + t3 * 0.42D + t4 * 0.55D + machine * 0.35D,
                0.38D + t2 * 0.40D + t3 * 0.30D);
    }

    private static double remap(double received, double nativeAmplitude, double nativeCoefficient,
                                double virtualAmplitude, double virtualCoefficient) {
        double denominator = nativeAmplitude * nativeCoefficient;
        if (denominator <= 0.0D || !Double.isFinite(denominator)) {
            return 0.0D;
        }
        double modulation = clamp(positive(received) / denominator, 0.0D, 1.0E6D);
        return virtualAmplitude * virtualCoefficient * modulation;
    }

    private static double machineLift(double effect) {
        return Math.log1p(Math.max(0.0D, positive(effect) - 70.0D) / 18.0D);
    }

    private static double presence(double peak) {
        return -Math.expm1(-Math.max(0.0D, peak));
    }

    private static double detail(double content, double threshold, double span) {
        if (content <= threshold) return 0.0D;
        double expansion = (content - threshold) / span;
        double logarithm = expansion <= Math.sqrt(Double.MAX_VALUE)
                ? Math.log1p(expansion * expansion) : 2.0D * Math.log(expansion);
        return logarithm / (1.0D + logarithm);
    }

    private static double smoothstep(double start, double end, double value) {
        double fraction = unit((value - start) / (end - start));
        return fraction * fraction * (3.0D - 2.0D * fraction);
    }

    private static double positive(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static double unit(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }
}
