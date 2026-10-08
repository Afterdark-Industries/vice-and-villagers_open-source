package com.trippycraft.substance.lsd;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class LsdEffectProfile {
    private LsdEffectProfile() {
    }

    public static void contribute(LsdEffectState state, EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active()) {
            return;
        }
        double intensity = state.intensity();
        double depth = state.realityFold() * 0.34D + state.skyVortex() * 0.28D + state.liquidFlow() * 0.16D;
        double dissociation = state.realityFold() * 0.26D + state.entityTrails() * 0.24D + state.skyVortex() * 0.18D;
        double egoSlip = state.skyVortex() * 0.20D + state.prismSplit() * 0.26D + intensity * 0.08D;
        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, intensity * (1.15D + state.prismSplit() * 0.22D), Double::sum);
        target.merge(ExperienceChannel.PATTERNING, state.edgeHum() * 0.52D + state.prismSplit() * 0.46D + state.skyVortex() * 0.24D, Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, state.geometryMelt() * 0.54D + state.realityFold() * 0.62D, Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, state.blockAutonomy() * 0.42D + state.liquidFlow() * 0.28D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, state.electricBloom() * 0.58D + state.prismSplit() * 0.36D, Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, depth, Double::sum);
        double dosePhenotype = LsdDoseCurve.smoothstep(35.0D, 1000.0D, state.activeDoseMicrograms());
        target.merge(ExperienceChannel.TIME_DILATION, intensity * (0.19D + dosePhenotype * 0.12D), Double::sum);
        target.merge(ExperienceChannel.EGO_DISSOLUTION, egoSlip, Double::sum);
        target.merge(ExperienceChannel.STIMULATION, state.edgeHum() * 0.34D + state.visualIntensity() * 0.18D, Double::sum);
        target.merge(ExperienceChannel.MOTOR_NOISE, state.edgeHum() * 0.08D
                + intensity * LsdDoseCurve.smoothstep(225.0D, 1000.0D, state.activeDoseMicrograms()) * 0.012D, Double::sum);
        target.merge(ExperienceChannel.DISSOCIATION, dissociation, Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, intensity * 0.08D, Double::sum);
    }
}
