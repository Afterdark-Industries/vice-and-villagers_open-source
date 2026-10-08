package com.trippycraft.substance.mdma;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class MdmaEffectProfile {
    private MdmaEffectProfile() {
    }

    public static void contribute(MdmaEffectState state,
                                  EnumMap<ExperienceChannel, Double> target,
                                  EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }
        double presence = compress(state.intensity() * 0.42D + state.socialAura() * 0.16D);
        familyTarget.merge(SubstanceFamily.EMPATHOGEN, presence, Double::sum);

        target.merge(ExperienceChannel.EUPHORIA, compress(state.euphoria() * 0.62D) * 0.92D, Double::sum);
        target.merge(ExperienceChannel.STIMULATION, compress(state.movementLift() * 0.55D) * 0.68D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, compress(state.saturation() * 0.48D + state.bloom() * 0.42D) * 0.94D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, compress(state.fovBreathing() * 0.40D + state.dreaminess() * 0.18D) * 0.62D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, compress(state.dreaminess() * 0.30D + state.coordinationNoise() * 0.22D) * 0.46D, Double::sum);
        target.merge(ExperienceChannel.MOTOR_NOISE, compress(state.coordinationNoise() * 0.62D + state.comedownFatigue() * 0.20D) * 0.74D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, compress(state.comedownFatigue() * 0.46D) * 0.66D, Double::sum);
        target.merge(ExperienceChannel.SEDATION, compress(state.comedownFatigue() * 0.42D) * 0.58D, Double::sum);
        target.merge(ExperienceChannel.ANXIETY, compress(state.repeatedUsePenalty() * state.comedownFatigue() * 0.34D) * 0.34D, Double::sum);
        target.merge(ExperienceChannel.CRAVING, compress(state.repeatedUsePenalty() * 0.28D) * 0.30D, Double::sum);
        target.merge(ExperienceChannel.RISK, compress(state.coordinationNoise() * 0.32D + state.repeatedUsePenalty() * 0.18D) * 0.36D, Double::sum);
    }

    public static double rawFamilyContribution(MdmaEffectState state) {
        return state.active() ? compress(state.intensity() * 0.38D + state.euphoria() * 0.12D) : 0.0D;
    }

    private static double compress(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - Math.exp(-value);
    }
}
