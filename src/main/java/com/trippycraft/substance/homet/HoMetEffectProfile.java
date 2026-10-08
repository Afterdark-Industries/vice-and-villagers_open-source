package com.trippycraft.substance.homet;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;
import java.util.EnumMap;

public final class HoMetEffectProfile {
    private HoMetEffectProfile() { }

    public static void contribute(HoMetEffectState state, EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) return;
        target.merge(ExperienceChannel.DEPTH_BREATHING, state.spatialDepth() * 0.80D, Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, state.blockDeformation() * 0.64D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, state.trails() * 0.48D + state.perspectiveInstability() * 0.16D, Double::sum);
        target.merge(ExperienceChannel.PATTERNING, state.surfaceRecursion() * 0.70D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, state.crystallineColor() * 0.55D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, state.trails() * 0.10D, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, state.visualPresence() * 0.18D + state.afterglow() * 0.10D, Double::sum);
        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, state.visualPresence() * 0.74D, Double::sum);
    }

    public static double rawFamilyContribution(HoMetEffectState state) {
        return state.active() ? state.visualPresence() * 0.74D : 0.0D;
    }
}
