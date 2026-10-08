package com.trippycraft.substance;

import java.util.Map;

public record SubstanceProfile(
        String id,
        String translationKey,
        SubstanceFamily family,
        DoseCurve curve,
        double defaultAbstractDoseUnits,
        double settingSensitivity,
        double riskPressure,
        Map<ExperienceChannel, Double> channelWeights
) {
    public double channelWeight(ExperienceChannel channel) {
        return channelWeights.getOrDefault(channel, 0.0D);
    }

    public boolean hasGameplayEffects() {
        if (settingSensitivity > 0.0D || riskPressure > 0.0D) {
            return true;
        }
        for (double weight : channelWeights.values()) {
            if (weight > 0.0D) {
                return true;
            }
        }
        return false;
    }
}
