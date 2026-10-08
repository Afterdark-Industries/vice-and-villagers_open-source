package com.trippycraft.substance.allad;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class AlLadEffectProfile {
    private AlLadEffectProfile() {
    }

    public static void contribute(AlLadEffectState state, EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }
        target.merge(ExperienceChannel.PATTERNING, state.stainedGlass() * 0.78D + state.jewelColor() * 0.22D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, state.jewelColor() * 0.62D + state.chromaticFringe() * 0.28D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, state.prismDepth() * 0.72D + state.cathedralRecursion() * 0.24D, Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, state.paneMotion() * 0.36D + state.prismDepth() * 0.22D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, state.paneMotion() * 0.28D + state.chromaticFringe() * 0.18D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, state.visualPresence() * 0.22D + state.prismDepth() * 0.10D, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, state.visualPresence() * 0.28D + state.afterglow() * 0.16D, Double::sum);
        target.merge(ExperienceChannel.STIMULATION, state.stimulation() * 0.70D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, state.bodyLoad() * 0.55D, Double::sum);
        target.merge(ExperienceChannel.ANXIETY, state.bodyLoad() * 0.08D, Double::sum);
        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, state.visualPresence() * 0.78D, Double::sum);
    }

    public static double rawFamilyContribution(AlLadEffectState state) {
        return state.active() ? state.visualPresence() * 0.78D : 0.0D;
    }
}
