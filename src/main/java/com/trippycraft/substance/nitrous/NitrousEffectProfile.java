package com.trippycraft.substance.nitrous;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class NitrousEffectProfile {
    private NitrousEffectProfile() {
    }

    public static void contribute(NitrousEffectState state, EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }
        target.merge(ExperienceChannel.DISSOCIATION, state.dissociation() * 0.70D, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, state.euphoria() * 0.86D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, state.echo() * 0.48D + state.stretch() * 0.22D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, state.stretch() * 0.62D + state.tunnel() * 0.28D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, state.echo() * 0.44D + state.fisheye() * 0.18D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, state.bloom() * 0.70D + state.haze() * 0.24D, Double::sum);
        target.merge(ExperienceChannel.MOTOR_NOISE, state.motorNoise() * 0.80D, Double::sum);
        target.merge(ExperienceChannel.SEDATION, state.audioMuffle() * 0.28D, Double::sum);
        familyTarget.merge(SubstanceFamily.DISSOCIATIVE, state.visualPresence() * 0.72D, Double::sum);
    }

    public static double rawFamilyContribution(NitrousEffectState state) {
        return state.active() ? state.visualPresence() * 0.72D : 0.0D;
    }
}
