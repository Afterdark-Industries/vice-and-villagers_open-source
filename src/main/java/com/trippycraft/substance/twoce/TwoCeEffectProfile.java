package com.trippycraft.substance.twoce;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class TwoCeEffectProfile {
    private TwoCeEffectProfile() {
    }

    public static void contribute(TwoCeEffectState state, EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }
        // The native world pass owns the room reveals. Keep the shared presentation
        // weighted toward living detail and color, with less aggregate lens movement.
        target.merge(ExperienceChannel.PATTERNING, state.tessellation() * 0.56D + state.edgeLock() * 0.23D, Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, state.perspectiveFold() * 0.24D + state.cavities() * 0.11D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, state.nestedRooms() * 0.42D + state.corridorZoom() * 0.22D
                + state.tessellation() * state.visualPresence() * 0.18D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, state.geometricTrails() * 0.22D + state.perspectiveFold() * 0.13D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, state.visualPresence() * 0.28D + state.corridorZoom() * 0.16D, Double::sum);
        target.merge(ExperienceChannel.EGO_DISSOLUTION, state.recursion() * 0.22D + state.symmetryLock() * 0.18D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, state.bodyLoad() * 0.70D, Double::sum);
        target.merge(ExperienceChannel.ANXIETY, state.bodyLoad() * 0.16D + state.recursion() * 0.08D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, state.tessellation() * 0.32D + state.edgeLock() * 0.18D
                + state.afterglow() * 0.10D, Double::sum);
        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, state.visualPresence() * 0.80D, Double::sum);
    }

    public static double rawFamilyContribution(TwoCeEffectState state) {
        return state.active() ? state.visualPresence() * 0.80D : 0.0D;
    }
}
