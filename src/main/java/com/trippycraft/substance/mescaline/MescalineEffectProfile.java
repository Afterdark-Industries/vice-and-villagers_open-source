package com.trippycraft.substance.mescaline;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class MescalineEffectProfile {
    private MescalineEffectProfile() {
    }

    public static void contribute(MescalineEffectState state, EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) return;
        // The persistent session authors its own gentle afterglow. Raw contribution still builds
        // psychedelic tolerance, without adding the generic comedown/craving tail.
        target.merge(ExperienceChannel.COLOR_BLOOM, compress(state.colorRichness() * 0.68D + state.warmth() * 0.22D), Double::sum);
        target.merge(ExperienceChannel.PATTERNING, compress(state.tessellation() * 0.42D + state.recursiveComplexity() * 0.14D), Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, compress(state.organicFlow() * 0.34D + state.surfaceBreathing() * 0.22D) * 0.62D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, compress(state.softTrails() * 0.48D + state.organicFlow() * 0.18D) * 0.72D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, compress(state.spatialDepth() * 0.44D + state.surfaceBreathing() * 0.28D) * 0.78D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, compress(state.organicFlow() * 0.30D) * 0.52D, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, compress(state.bodyWarmth() * 0.34D + state.afterglow() * 0.30D) * 0.48D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, compress(state.bodyWarmth() * 0.16D) * 0.20D, Double::sum);
        target.merge(ExperienceChannel.NAUSEA, compress(state.nauseaWave() * 0.22D) * 0.18D, Double::sum);
    }

    public static double rawFamilyContribution(MescalineEffectState state) {
        return state.active() ? compress(state.intensity() * 0.36D + state.recursiveComplexity() * 0.045D) : 0.0D;
    }

    private static double compress(double value) {
        return !Double.isFinite(value) || value <= 0.0D ? 0.0D : 1.0D - Math.exp(-value);
    }
}
