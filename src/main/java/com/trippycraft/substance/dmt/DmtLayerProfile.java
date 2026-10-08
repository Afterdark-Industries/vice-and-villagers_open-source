package com.trippycraft.substance.dmt;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ActiveDoseState;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;

public record DmtLayerProfile(
        double alienShimmer,
        double spatialInstability,
        double edgeFlicker,
        double symbolPressure,
        double veilThinness,
        double breakthroughTransition,
        double impossibleArchitecture,
        double symbolLanguage,
        double entityLayer,
        double environmentalConsciousness,
        double realityReplacement,
        double dominance
) {
    public static DmtLayerProfile empty() {
        return new DmtLayerProfile(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static DmtLayerProfile resolve(
            ActiveDoseState dose,
            DmtStage stage,
            double transition,
            DmtTripSignature signature,
            PsilocybinEnvironmentSnapshot environment
    ) {
        if (!dose.active()) {
            return empty();
        }

        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double intensity = dose.intensityPresence(safe);
        double lowDose = dose.dosePresence(0.12D, Math.max(0.20D, TrippyConfig.dmtPreBreakthroughThresholdUnits * 0.46D));
        double preDose = dose.dosePresence(Math.max(0.20D, TrippyConfig.dmtPreBreakthroughThresholdUnits * 0.70D), TrippyConfig.dmtBreakthroughThresholdUnits);
        double breakthroughDose = dose.dosePresence(TrippyConfig.dmtBreakthroughThresholdUnits * 0.86D, TrippyConfig.dmtBreakthroughThresholdUnits * 1.18D);
        double tripRise = ActiveDoseState.smoothstep(0.0D, 0.12D, dose.progress());
        double tripFall = 1.0D - ActiveDoseState.smoothstep(0.82D, 1.0D, dose.progress());
        double tripEnvelope = tripRise * tripFall;
        double transitionStage = stage == DmtStage.BREAKTHROUGH_TRANSITION ? 1.0D : 0.0D;
        double post = stage == DmtStage.POST_BREAKTHROUGH ? 1.0D : 0.0D;
        double pre = stage == DmtStage.PRE_BREAKTHROUGH ? 1.0D : 0.0D;
        double low = stage == DmtStage.LOW_THRESHOLD ? 1.0D : 0.0D;
        double returning = stage == DmtStage.RETURN ? 1.0D : 0.0D;
        double replacementDose = dose.dosePresence(4.80D, Math.max(7.0D, TrippyConfig.dmtTier5Units));
        double extremeDose = dose.dosePresence(5.40D, Math.max(7.20D, TrippyConfig.dmtTier5Units));
        double thresholdPressure = ActiveDoseState.clamp(lowDose * (0.72D + low * 0.28D));
        double layerSeparation = ActiveDoseState.clamp(preDose * (0.55D + pre * 0.45D) + transition * 0.22D);
        double replacementAuthority = ActiveDoseState.clamp(
                breakthroughDose * (transition * 0.55D + post * (0.40D + replacementDose * 0.34D) + returning * 0.24D)
                        + extremeDose * (post * 0.70D + transitionStage * 0.24D)
        );
        double envAlive = environment.organicBlocks() * 0.28D
                + environment.forest() * 0.20D
                + environment.entities() * 0.18D
                + environment.light() * 0.08D
                + environment.end() * 0.14D;
        double constructed = environment.artificialBlocks() * 0.28D
                + environment.village() * 0.16D
                + environment.cave() * 0.10D
                + environment.end() * 0.24D;

        double alienShimmer = intensity * tripEnvelope * (0.18D + thresholdPressure * 0.70D + pre * 0.16D);
        double spatialInstability = intensity * tripEnvelope * (0.10D + thresholdPressure * 0.28D + layerSeparation * 0.58D + transition * 0.54D + replacementAuthority * 0.18D);
        double edgeFlicker = intensity * tripEnvelope * (0.08D + thresholdPressure * 0.36D + signature.colorStrobe() * 0.20D + layerSeparation * 0.22D + transition * 0.34D);
        double symbolPressure = intensity * tripEnvelope * (0.08D + signature.glyphDensity() * 0.46D + thresholdPressure * 0.20D + layerSeparation * 0.42D + post * 0.36D + extremeDose * 0.34D)
                * TrippyConfig.dmtSymbolDensity;
        double veilThinness = intensity * tripEnvelope * (pre * 0.40D + layerSeparation * 0.34D + transition * 0.86D + post * 0.20D + extremeDose * 0.18D);
        double breakthroughTransition = intensity * breakthroughDose * transition * (0.82D + signature.portalBias() * 0.42D + extremeDose * 0.24D);
        double impossibleArchitecture = intensity * breakthroughDose * (post * 0.98D + returning * 0.26D + transition * 0.28D + extremeDose * 0.52D)
                * (0.62D + signature.symmetry() * 0.42D + constructed * 0.26D)
                * TrippyConfig.dmtArchitectureFrequency;
        double symbolLanguage = intensity * (symbolPressure * 0.42D + breakthroughDose * (post * 0.84D + transition * 0.34D + extremeDose * 0.50D))
                * (0.62D + signature.glyphDensity() * 0.34D)
                * TrippyConfig.dmtSymbolDensity;
        double entityLayer = intensity * breakthroughDose * (post * 0.76D + transition * 0.14D + returning * 0.20D + extremeDose * 0.32D)
                * (0.48D + signature.entityHint() * 0.52D + environment.entities() * 0.14D)
                * TrippyConfig.dmtEntityDensity;
        double environmentalConsciousness = intensity * (low * 0.28D + pre * 0.42D + transitionStage * 0.34D + post * 0.70D + returning * 0.30D + extremeDose * 0.26D)
                * (0.52D + envAlive * 0.44D)
                * TrippyConfig.dmtEnvironmentalConsciousness;
        double realityReplacement = tripEnvelope
                * (replacementAuthority * 0.74D + transition * 0.30D + impossibleArchitecture * 0.20D + symbolLanguage * 0.14D + extremeDose * 0.42D)
                * TrippyConfig.dmtSpatialReplacementWeight;
        double dominance = ActiveDoseState.clamp((realityReplacement * 0.76D + breakthroughTransition * 0.32D + post * breakthroughDose * 0.36D + extremeDose * 0.28D)
                * TrippyConfig.dmtBreakthroughDominanceWeight);

        if (TrippyConfig.dmtAccessibilityMode) {
            alienShimmer *= 0.70D;
            spatialInstability *= 0.58D;
            edgeFlicker *= 0.48D;
            symbolPressure *= 0.55D;
            breakthroughTransition *= 0.48D;
            realityReplacement *= 0.52D;
            dominance *= 0.72D;
        }

        return new DmtLayerProfile(
                clampLayer(alienShimmer),
                clampLayer(spatialInstability),
                clampLayer(edgeFlicker),
                clampLayer(symbolPressure),
                clampLayer(veilThinness),
                clampLayer(breakthroughTransition),
                clampLayer(impossibleArchitecture),
                clampLayer(symbolLanguage),
                clampLayer(entityLayer),
                clampLayer(environmentalConsciousness),
                clampLayer(realityReplacement),
                ActiveDoseState.clamp(dominance)
        );
    }

    private static double clampLayer(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(2.0D, value));
    }
}
