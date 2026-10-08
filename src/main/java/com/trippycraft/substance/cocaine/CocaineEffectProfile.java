package com.trippycraft.substance.cocaine;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class CocaineEffectProfile {
    private CocaineEffectProfile() {
    }

    public static void contribute(
            CocaineEffectState state,
            EnumMap<ExperienceChannel, Double> channels,
            EnumMap<SubstanceFamily, Double> families
    ) {
        if (state == null || !state.active()) {
            return;
        }
        double safe = 5.8D;
        double load = saturate(state.intensity() / safe);
        families.merge(SubstanceFamily.STIMULANT, load * 1.10D, Double::sum);
        channels.merge(ExperienceChannel.STIMULATION, saturate(state.stimulation() / safe) * 0.94D, Double::sum);
        channels.merge(ExperienceChannel.EUPHORIA, saturate(state.euphoria() / safe) * 0.48D, Double::sum);
        channels.merge(ExperienceChannel.TIME_DILATION, saturate(state.tunnelFocus() / safe) * 0.18D, Double::sum);
        channels.merge(ExperienceChannel.MOTOR_NOISE, saturate((state.jitter() * 0.58D + state.overcorrection() * 0.42D) / safe) * 0.64D, Double::sum);
        channels.merge(ExperienceChannel.ANXIETY, saturate(state.anxiety() / safe) * 0.70D, Double::sum);
        channels.merge(ExperienceChannel.CRAVING, saturate(state.craving() / safe) * 0.82D, Double::sum);
        channels.merge(ExperienceChannel.RISK, saturate(state.risk() / safe) * 0.76D, Double::sum);
        channels.merge(ExperienceChannel.BODY_LOAD, saturate((state.crashDrag() * 0.64D + state.lightHarshness() * 0.36D) / safe) * 0.22D, Double::sum);
        channels.merge(ExperienceChannel.VISUAL_DRIFT, saturate((state.edgeShimmer() * 0.52D + state.microFlicker() * 0.48D) / safe) * 0.10D, Double::sum);
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}