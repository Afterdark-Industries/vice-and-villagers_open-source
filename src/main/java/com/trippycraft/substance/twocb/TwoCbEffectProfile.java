package com.trippycraft.substance.twocb;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class TwoCbEffectProfile {
    private TwoCbEffectProfile() {
    }

    public static void contribute(TwoCbEffectState state,
                                  EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }
        double presence = compress(state.intensity() * 0.48D + state.surfaceGeometry() * 0.12D);
        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, presence, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, compress(state.saturation() * 0.62D + state.edgeGlow() * 0.24D) * 0.96D, Double::sum);
        target.merge(ExperienceChannel.PATTERNING, compress(state.surfaceGeometry() * 0.54D + state.crystalline() * 0.48D) * 1.02D, Double::sum);
        // Keep the shared melt channel low: 2C-B should read as faceted and precise, never liquid.
        target.merge(ExperienceChannel.GEOMETRY_MELT, compress(state.crystalline() * 0.18D) * 0.22D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, compress(state.tracerStrength() * 0.24D) * 0.34D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, compress(state.fisheyePulse() * 1.8D) * 0.30D, Double::sum);
        target.merge(ExperienceChannel.STIMULATION, compress(state.stimulation() * 0.52D) * 0.56D, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, compress(state.sensuality() * 0.58D) * 0.62D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, compress(state.symmetry() * 0.22D) * 0.24D, Double::sum);
        target.merge(ExperienceChannel.ANXIETY, compress(state.anxiety() * 0.62D) * 0.40D, Double::sum);
        target.merge(ExperienceChannel.RISK, compress(state.anxiety() * 0.34D + state.stimulation() * 0.10D) * 0.28D, Double::sum);
    }

    public static double rawFamilyContribution(TwoCbEffectState state) {
        return state.active() ? compress(state.intensity() * 0.42D + state.surfaceGeometry() * 0.10D) : 0.0D;
    }

    private static double compress(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - Math.exp(-value);
    }
}
