package com.trippycraft.substance.alcohol;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class AlcoholEffectProfile {
    private AlcoholEffectProfile() {
    }

    public static void contribute(
            AlcoholEffectState state,
            EnumMap<ExperienceChannel, Double> channels,
            EnumMap<SubstanceFamily, Double> families
    ) {
        if (state == null || !state.active()) {
            return;
        }
        double safe = 6.0D;
        double load = saturate(state.intensity() / safe);
        families.merge(SubstanceFamily.DEPRESSANT, load * 1.08D, Double::sum);
        channels.merge(ExperienceChannel.VISUAL_DRIFT, saturate((state.diplopia() * 0.82D + state.focusBlur() * 0.18D) / safe) * 0.16D, Double::sum);
        channels.merge(ExperienceChannel.TIME_DILATION, saturate((state.focusBlur() * 0.45D + state.sedation() * 0.55D) / safe) * 0.12D, Double::sum);
        channels.merge(ExperienceChannel.SEDATION, saturate(state.sedation() / safe) * 0.86D, Double::sum);
        channels.merge(ExperienceChannel.MOTOR_NOISE, saturate(state.motorNoise() / safe) * 0.78D, Double::sum);
        channels.merge(ExperienceChannel.EUPHORIA, saturate(state.euphoria() / safe) * 0.42D, Double::sum);
        channels.merge(ExperienceChannel.NAUSEA, saturate(state.nausea() / safe) * 0.52D, Double::sum);
        channels.merge(ExperienceChannel.BODY_LOAD, saturate((state.peripheralNarrowing() * 0.25D + state.sedation() * 0.75D) / safe) * 0.28D, Double::sum);
        channels.merge(ExperienceChannel.DISSOCIATION, saturate((state.focusBlur() * 0.62D + state.peripheralNarrowing() * 0.38D) / safe) * 0.055D, Double::sum);
        channels.merge(ExperienceChannel.RISK, saturate(state.risk() / safe) * 0.58D, Double::sum);
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
