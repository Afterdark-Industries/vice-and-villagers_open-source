package com.trippycraft.substance.cannabis;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class CannabisEffectProfile {
    private CannabisEffectProfile() {
    }

    public static void contribute(CannabisEffectState state, EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }

        double familyPresence = compress(state.intensity() * 0.34D + state.immersion() * 0.08D);
        familyTarget.merge(SubstanceFamily.CANNABINOID, familyPresence, Double::sum);

        target.merge(ExperienceChannel.TIME_DILATION, compress(state.temporalDrag() * 0.28D) * 0.78D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, compress(state.breathingPulse() * 0.32D) * 0.55D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, compress((state.warmthShift() + state.saturationBoost() + state.greenRichness()) * 0.20D) * 0.64D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, compress(state.immersion() * 0.24D + state.focusTunnel() * 0.10D) * 0.62D, Double::sum);
        target.merge(ExperienceChannel.SEDATION, compress(state.temporalDrag() * 0.10D + state.thoughtStickiness() * 0.18D) * 0.56D, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, compress(state.warmthShift() * 0.12D + state.saturationBoost() * 0.08D) * 0.32D, Double::sum);
        target.merge(ExperienceChannel.CRAVING, compress(state.doseScale() * 0.04D) * 0.18D, Double::sum);
    }

    public static double rawFamilyContribution(CannabisEffectState state) {
        if (!state.active()) {
            return 0.0D;
        }
        return compress(state.intensity() * 0.30D + state.immersion() * 0.10D);
    }

    private static double compress(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - Math.exp(-value);
    }
}