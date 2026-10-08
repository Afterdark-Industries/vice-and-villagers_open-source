package com.trippycraft.substance.allad;

/**
 * Pure client inputs for AL-LAD's synthetic flow, phase contours and laminar world reconstruction.
 * Ten times the absorbed physical content drives the established material appearance.
 * The received channels retain the authored trip signature, phase and mixture;
 * four additional families continue above 500 mg without changing the physical trip.
 */
public record AlLadVisualDrive(double visualPresence, double stainedGlass, double prismDepth,
        double paneMotion, double chromaticFringe, double cathedralRecursion, double jewelColor,
        double musicSensitivity, double gate75, double gate150, double gate300, double heroicLift,
        double phaseContours, double laminarDepth, double recursiveSheets, double interferenceVolumes,
        double phaseMultiplicity, double laminarComplexity, double recursiveInterference, double immersiveContinuity) {
    public static final double CLOCK_PERIOD_TICKS = 96000.0D;
    private static final AlLadVisualDrive NONE = new AlLadVisualDrive(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private static final Context DEFAULT_CONTEXT = new Context(0.0D, 0.0D, 1.0D);

    /** Raw synchronized channels before the final shader-uniform compression. */
    public record Channels(double intensity, double visualPresence, double stainedGlass, double prismDepth,
            double paneMotion, double chromaticFringe, double cathedralRecursion, double jewelColor,
            double musicSensitivity) {
    }

    /** Numeric environment and signature inputs needed by dose-dependent profile ratios. */
    public record Context(double light, double village, double recursionBias) {
    }

    public static AlLadVisualDrive resolve(double targetContentMilligrams, double activeEffectMicrograms,
            double targetEffectMicrograms, double globalIntensity, double visualIntensity,
            double blendGain, Channels channels) {
        return resolve(targetContentMilligrams, activeEffectMicrograms, targetEffectMicrograms,
                globalIntensity, visualIntensity, blendGain, DEFAULT_CONTEXT, channels);
    }

    public static AlLadVisualDrive resolve(double targetContentMilligrams, double activeEffectMicrograms,
            double targetEffectMicrograms, double globalIntensity, double visualIntensity,
            double blendGain, Context context, Channels channels) {
        double dose = positive(activeEffectMicrograms);
        double peak = peakIntensity(dose, globalIntensity);
        double visualGain = clamp(visualIntensity, 0.0D, 2.0D);
        double blend = clamp(blendGain, 0.0D, 8.0D);
        double content = effectiveContentMilligrams(targetContentMilligrams, dose, targetEffectMicrograms);
        if (channels == null || content <= 0.0D || dose <= 0.0D || peak <= 0.0D || visualGain <= 0.0D || blend <= 0.0D
                || positive(channels.intensity()) <= 0.0D) {
            return NONE;
        }
        Context setting = context == null ? DEFAULT_CONTEXT : context;
        double virtualContent = promotedContentMilligrams(content);
        double virtualDose = effectMicrogramsForContent(virtualContent);
        double virtualPeak = peakIntensity(virtualDose, globalIntensity);
        double envelope = Math.min(positive(channels.intensity()), peak) / peak;
        double peakPresence = -Math.expm1(-peak);
        double virtualPresence = -Math.expm1(-virtualPeak);
        double nativeAmplitude = peakPresence * visualAmplitude(dose, globalIntensity);
        double virtualAmplitude = virtualPresence * visualAmplitude(virtualDose, globalIntensity);
        Profile nativeProfile = profile(dose, setting);
        Profile virtualProfile = profile(virtualDose, setting);
        // A saturated bounded state may still round to one after blend scaling.
        // The synchronized intensity ratio prevents that from retaining peak coverage.
        double presence = Math.min(unit(positive(channels.visualPresence()) * (virtualPresence / peakPresence)),
                virtualPresence * envelope) * visualGain * blend;
        double detailGain = envelope * Math.min(visualGain, 1.35D) * Math.min(blend, 1.0D);
        // Below the native recursion threshold the channel carries no signature.
        // Its supplied numeric bias restores that family using the received phase/blend.
        double recursion = nativeProfile.recursion() > 0.0D
                ? remap(channels.cathedralRecursion(), nativeAmplitude, nativeProfile.recursion(),
                        virtualAmplitude, virtualProfile.recursion())
                : virtualAmplitude * virtualProfile.recursion() * clamp(setting.recursionBias(), 0.0D, 2.0D)
                        * clamp(positive(channels.intensity()) / peak, 0.0D, 8.0D);
        return new AlLadVisualDrive(presence,
                compress(remap(channels.stainedGlass(), nativeAmplitude, nativeProfile.glass(), virtualAmplitude, virtualProfile.glass())),
                compress(remap(channels.prismDepth(), nativeAmplitude, nativeProfile.prism(), virtualAmplitude, virtualProfile.prism())),
                compress(remap(channels.paneMotion(), nativeAmplitude, nativeProfile.motion(), virtualAmplitude, virtualProfile.motion())),
                compress(remap(channels.chromaticFringe(), nativeAmplitude, nativeProfile.chroma(), virtualAmplitude, virtualProfile.chroma())),
                compress(recursion),
                compress(remap(channels.jewelColor(), nativeAmplitude, nativeProfile.jewel(), virtualAmplitude, virtualProfile.jewel())),
                compress(remap(channels.musicSensitivity(), peakPresence, nativeProfile.music(), virtualPresence, virtualProfile.music())),
                unit((virtualDose - 70.0D) / 80.0D), unit((virtualDose - 140.0D) / 160.0D),
                unit((virtualDose - 280.0D) / 170.0D), heroicLift(virtualDose),
                detail(virtualContent, 100.0D, 200.0D) * detailGain,
                detail(virtualContent, 250.0D, 350.0D) * detailGain,
                detail(virtualContent, 500.0D, 650.0D) * detailGain,
                detail(virtualContent, 1500.0D, 1200.0D) * detailGain,
                detail(content, 500.0D, 500.0D) * detailGain,
                detail(content, 900.0D, 800.0D) * detailGain,
                detail(content, 1800.0D, 1200.0D) * detailGain,
                detail(content, 3000.0D, 1400.0D) * detailGain);
    }

    /** Finite client-only promotion; the synchronized dose/content ledger is never changed. */
    public static double promotedContentMilligrams(double effectiveContentMilligrams) {
        double content = positive(effectiveContentMilligrams);
        return content > Double.MAX_VALUE / 10.0D ? Double.MAX_VALUE : content * 10.0D;
    }

    /** Inverse of the authored physical-content calibration, used only by presentation. */
    public static double effectMicrogramsForContent(double contentMilligrams) {
        double content = positive(contentMilligrams);
        if (content <= 25.0D) return content * 3.0D;
        if (content <= 100.0D) return 75.0D + (content - 25.0D);
        if (content <= 250.0D) return 150.0D + (content - 100.0D);
        if (content <= 500.0D) return 300.0D + (content - 250.0D) * 0.6D;
        return content * 0.9D;
    }

    /** Uses the ledger's target with actual absorption/tolerance, without HUD lag. */
    public static double effectiveContentMilligrams(double targetContentMilligrams,
            double activeEffectMicrograms, double targetEffectMicrograms) {
        double content = positive(targetContentMilligrams);
        double active = positive(activeEffectMicrograms);
        double target = positive(targetEffectMicrograms);
        if (content <= 0.0D || active <= 0.0D || target <= 0.0D) return 0.0D;
        return content * (Math.min(active, target) / target);
    }

    /** Exact approved native curve; exposed so numeric GPU QA uses production input mapping. */
    public static double peakIntensity(double effectMicrograms, double globalIntensity) {
        double dose = positive(effectMicrograms);
        return Math.log1p(dose / 40.0D) * 1.55D * amplification(dose) * clamp(globalIntensity, 0.0D, 100.0D) * 1.5D;
    }

    private static double amplification(double dose) {
        return 1.0D + 0.55D * smoothstep(140.0D, 170.0D, dose)
                + 0.85D * smoothstep(280.0D, 340.0D, dose)
                + 1.70D * smoothstep(420.0D, 560.0D, dose)
                + 2.40D * smoothstep(560.0D, 900.0D, dose);
    }

    private static double visualAmplitude(double dose, double globalIntensity) {
        return Math.log1p(dose / 40.0D) / Math.log1p(150.0D / 40.0D)
                * amplification(dose) * clamp(globalIntensity, 0.0D, 100.0D) * 1.5D;
    }

    private record Profile(double glass, double prism, double motion, double chroma,
                           double recursion, double jewel, double music) {
    }

    private static Profile profile(double dose, Context context) {
        double tier2 = smoothstep(70.0D, 150.0D, dose);
        double tier3 = smoothstep(140.0D, 300.0D, dose);
        double tier4 = smoothstep(280.0D, 450.0D, dose);
        return new Profile(0.52D + tier2 * 0.40D + tier3 * 0.22D,
                0.32D + tier2 * 0.34D + tier3 * 0.36D + tier4 * 0.28D,
                0.22D + tier2 * 0.36D + tier3 * 0.30D,
                0.34D + tier2 * 0.40D + tier3 * 0.28D,
                tier2 * 0.16D + tier3 * 0.48D + tier4 * 0.72D + heroicLift(dose) * 0.55D,
                0.72D + tier2 * 0.30D + unit(context.light()) * 0.16D,
                0.28D + tier2 * 0.40D + unit(context.village()) * 0.18D);
    }

    private static double remap(double received, double nativeAmplitude, double nativeCoefficient,
                                double virtualAmplitude, double virtualCoefficient) {
        double denominator = nativeAmplitude * nativeCoefficient;
        if (denominator <= 0.0D || !Double.isFinite(denominator)) return 0.0D;
        double modulation = clamp(positive(received) / denominator, 0.0D, 1.0E6D);
        return virtualAmplitude * virtualCoefficient * modulation;
    }

    private static double heroicLift(double dose) {
        return Math.log1p(Math.max(0.0D, dose - 420.0D) / 70.0D);
    }

    /** Monotonic bounded channel compression keeps shader material amplitudes finite. */
    public static double compress(double value) {
        double amount = positive(value);
        if (amount > Double.MAX_VALUE / 1.10D) return 1.0D;
        return 1.0D - 1.0D / (1.0D + amount * 1.10D);
    }

    /** Shared dose-independent clock. Both color travel and material motion use this cycle. */
    public static double advanceVisualTicks(double currentTicks, double deltaTicks, double motionScale) {
        return wrapTicks(wrapTicks(currentTicks) + clamp(deltaTicks, 0.0D, 5.0D) * clamp(motionScale, 0.0D, 2.0D));
    }

    public static double wrapTicks(double ticks) {
        if (!Double.isFinite(ticks)) return 0.0D;
        double wrapped = ticks % CLOCK_PERIOD_TICKS;
        return wrapped < 0.0D ? wrapped + CLOCK_PERIOD_TICKS : wrapped;
    }

    public static double colorCycle(double ticks) {
        return wrapTicks(ticks) / CLOCK_PERIOD_TICKS;
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
