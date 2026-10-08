package com.trippycraft.substance.ketamine;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class KetamineEffectProfile {
    private KetamineEffectProfile() {
    }

    public static void contribute(
            KetamineEffectState state,
            EnumMap<ExperienceChannel, Double> channels,
            EnumMap<SubstanceFamily, Double> families
    ) {
        if (state == null || !state.active()) {
            return;
        }
        double safe = 6.4D;
        double load = saturate(state.intensity() / safe);
        families.merge(SubstanceFamily.DISSOCIATIVE, load * 1.12D, Double::sum);
        channels.merge(ExperienceChannel.DISSOCIATION, saturate((state.dissociation() + state.bodyEcho() * 0.20D) / safe) * 1.08D, Double::sum);
        channels.merge(ExperienceChannel.TIME_DILATION, saturate(state.timeSlip() / safe) * 0.82D, Double::sum);
        channels.merge(ExperienceChannel.DEPTH_BREATHING, saturate((state.depthMismatch() * 0.66D + state.bodyFloat() * 0.34D) / safe) * 0.94D, Double::sum);
        channels.merge(ExperienceChannel.VISUAL_DRIFT, saturate((state.gravityDrift() * 0.54D + state.bodyEcho() * 0.46D) / safe) * 0.62D, Double::sum);
        channels.merge(ExperienceChannel.MOTOR_NOISE, saturate((state.motorDisconnect() * 0.52D + state.inputLatency() * 0.48D) / safe) * 0.82D, Double::sum);
        channels.merge(ExperienceChannel.SEDATION, saturate((state.audioMuffle() * 0.40D + state.collapseDepth() * 0.60D) / safe) * 0.76D, Double::sum);
        channels.merge(ExperienceChannel.BODY_LOAD, saturate((state.gravityDrift() * 0.46D + state.uiFade() * 0.32D + state.collapseDepth() * 0.22D) / safe) * 0.62D, Double::sum);
        channels.merge(ExperienceChannel.GEOMETRY_MELT, saturate(state.depthMismatch() / safe) * 0.34D, Double::sum);
        channels.merge(ExperienceChannel.RISK, saturate((state.collapseRisk() * 0.60D + state.collapseDepth() * 0.40D) / safe) * 0.66D, Double::sum);
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}