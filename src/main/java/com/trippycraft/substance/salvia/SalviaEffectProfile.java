package com.trippycraft.substance.salvia;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class SalviaEffectProfile {
    private SalviaEffectProfile() {
    }

    public static void contribute(SalviaEffectState state, EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        if (state == null || !state.active()) {
            return;
        }
        double intensity = state.intensity();
        double safe = Math.max(1.0D, SalviaDoseCurve.safeVisualIntensity());
        double load = SalviaDoseCurve.clamp((intensity * 0.56D + state.visualIntensity() * 0.44D) / safe, 0.0D, 1.0D);
        familyTarget.merge(SubstanceFamily.DELIRIANT, intensity * (1.08D + state.sceneReplacement() * 0.14D), Double::sum);
        target.merge(ExperienceChannel.DISSOCIATION, state.anchorSlip() * 0.44D + state.sceneReplacement() * 0.30D + state.observerPressure() * 0.16D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, state.loopSeam() * 0.50D + state.anchorSlip() * 0.18D + state.reentryConfusion() * 0.22D, Double::sum);
        target.merge(ExperienceChannel.MOTOR_NOISE, state.bodyPull() * 0.52D + state.reentryConfusion() * 0.22D, Double::sum);
        target.merge(ExperienceChannel.EGO_DISSOLUTION, state.sceneReplacement() * 0.20D + state.observerPressure() * 0.22D + intensity * SalviaDoseCurve.smoothstep(1.35D, 2.40D, state.activeDoseUnits()) * 0.20D, Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, state.sceneReplacement() * 0.22D + state.loopSeam() * 0.14D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, state.bodyPull() * 0.24D + state.loopSeam() * 0.18D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, state.sceneReplacement() * 0.14D + state.observerPressure() * 0.10D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, state.bodyPull() * 0.34D + state.reentryConfusion() * 0.16D, Double::sum);
        target.merge(ExperienceChannel.ANXIETY, state.observerPressure() * 0.28D + intensity * (state.environment().hostileEntities() * 0.08D + state.environment().lowHealth() * 0.06D), Double::sum);
        target.merge(ExperienceChannel.RISK, load * SalviaDoseCurve.smoothstep(1.45D, 2.90D, state.activeDoseUnits()) * 0.22D + state.observerPressure() * 0.10D, Double::sum);
    }
}