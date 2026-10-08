package com.trippycraft.substance.dmt;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ActiveDoseState;

public final class DmtTransitionController {
    private DmtTransitionController() {
    }

    public static double breakthroughTransition(ActiveDoseState dose, DmtStage stage) {
        if (dose == null || !dose.active()) {
            return 0.0D;
        }
        double doseGate = ActiveDoseState.smoothstep(
                TrippyConfig.dmtBreakthroughThresholdUnits * 0.90D,
                TrippyConfig.dmtBreakthroughThresholdUnits * 1.12D,
                dose.activeDose()
        );
        double windowStart = TrippyConfig.dmtTransitionStartProgress;
        double windowEnd = TrippyConfig.dmtTransitionEndProgress;
        double progress = dose.progress();
        double inWindow = ActiveDoseState.smoothstep(windowStart, mix(windowStart, windowEnd, 0.42D), progress)
                * (1.0D - ActiveDoseState.smoothstep(mix(windowStart, windowEnd, 0.68D), windowEnd, progress));
        double stageBoost = stage == DmtStage.BREAKTHROUGH_TRANSITION ? 1.0D : stage == DmtStage.POST_BREAKTHROUGH ? 0.22D : 0.0D;
        return ActiveDoseState.clamp((inWindow + stageBoost * 0.18D) * doseGate * (0.60D + dose.intensityPresence(TrippyConfig.dmtMaxVisualIntensity) * 0.40D));
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0D, Math.min(1.0D, t));
    }
}
