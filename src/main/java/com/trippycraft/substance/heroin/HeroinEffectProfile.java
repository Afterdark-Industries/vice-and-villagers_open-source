package com.trippycraft.substance.heroin;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class HeroinEffectProfile {
    private HeroinEffectProfile() {
    }

    public static void contribute(
            HeroinEffectState state,
            EnumMap<ExperienceChannel, Double> channels,
            EnumMap<SubstanceFamily, Double> families
    ) {
        if (state == null || !state.active()) {
            return;
        }
        double safe = 8.2D;
        double load = saturate(state.intensity() / safe);
        families.merge(SubstanceFamily.OPIOID, load * 1.24D, Double::sum);
        channels.merge(ExperienceChannel.SEDATION, saturate((state.sedation() * 0.72D + state.nodding() * 0.50D + state.microSleep() * 0.42D) / safe) * 1.12D, Double::sum);
        channels.merge(ExperienceChannel.EUPHORIA, saturate((state.euphoria() + state.warmth() * 0.34D) / safe) * 0.82D, Double::sum);
        channels.merge(ExperienceChannel.COLOR_BLOOM, saturate((state.warmth() * 0.88D + state.euphoria() * 0.26D) / safe) * 0.58D, Double::sum);
        channels.merge(ExperienceChannel.DEPTH_BREATHING, saturate((state.softFocus() * 0.50D + state.painSuppression() * 0.38D + state.sedation() * 0.22D) / safe) * 0.50D, Double::sum);
        channels.merge(ExperienceChannel.TIME_DILATION, saturate((state.softFocus() * 0.52D + state.nodding() * 0.34D) / safe) * 0.56D, Double::sum);
        channels.merge(ExperienceChannel.BODY_LOAD, saturate((state.sedation() * 0.38D + state.motorDrag() * 0.44D + state.respiratorySuppression() * 0.30D) / safe) * 0.84D, Double::sum);
        channels.merge(ExperienceChannel.MOTOR_NOISE, saturate((state.motorDrag() * 0.56D + state.miningDrag() * 0.32D + state.microSleep() * 0.38D) / safe) * 0.88D, Double::sum);
        channels.merge(ExperienceChannel.CRAVING, load * 0.25D + saturate(state.euphoria() / safe) * 0.18D, Double::sum);
        channels.merge(ExperienceChannel.RISK, saturate((state.respiratorySuppression() * 0.72D + state.microSleep() * 0.40D + state.nodding() * 0.22D) / safe) * 0.84D, Double::sum);
    }

    public static double rawFamilyContribution(HeroinEffectState state) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        return saturate(state.intensity() / 8.2D) * 1.16D;
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
