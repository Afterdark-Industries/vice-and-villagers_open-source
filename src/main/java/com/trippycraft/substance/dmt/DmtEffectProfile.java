package com.trippycraft.substance.dmt;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class DmtEffectProfile {
    private DmtEffectProfile() {
    }

    public static void contribute(DmtEffectState state, EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        if (state == null || !state.active()) {
            return;
        }
        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        DmtFractalScaffold scaffold = state.fractalScaffold();
        double visual = saturate((state.intensity() * 0.42D + state.visualIntensity() * 0.58D) / safe);
        double scaffoldLoad = saturate(scaffold.fractal3dPresence() * 0.26D
                + scaffold.animatedFractalPresence() * 0.24D
                + scaffold.staticAnimatedCoupling() * 0.18D
                + scaffold.alternateWorldReadiness() * 0.30D
                + scaffold.dimensionalDepth() * 0.16D);
        double afterglowCalm = state.phase() == DmtPhase.AFTERGLOW ? 0.42D : 1.0D;
        double load = saturate((visual * 0.62D + scaffoldLoad * 0.38D) * afterglowCalm);
        if (load <= 0.0D) {
            return;
        }

        target.merge(ExperienceChannel.PATTERNING, load * 0.030D, Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, load * 0.018D, Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, load * 0.025D, Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, load * 0.012D, Double::sum);
        target.merge(ExperienceChannel.EGO_DISSOLUTION, load * 0.016D, Double::sum);
        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, load * 0.74D, Double::sum);
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
