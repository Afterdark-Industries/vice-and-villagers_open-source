package com.trippycraft.substance.dmt;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ActiveDoseState;
import com.trippycraft.substance.EffectStageResolver;

public final class DmtStageResolver implements EffectStageResolver<ActiveDoseState, DmtStage> {
    public static final DmtStageResolver INSTANCE = new DmtStageResolver();

    private DmtStageResolver() {
    }

    @Override
    public DmtStage resolve(ActiveDoseState state) {
        if (state == null || !state.active()) {
            return DmtStage.SOBER;
        }
        double progress = state.progress();
        if (progress >= TrippyConfig.dmtAfterglowStartProgress) {
            return DmtStage.AFTERGLOW;
        }
        if (progress >= TrippyConfig.dmtReturnStartProgress) {
            return DmtStage.RETURN;
        }

        double breakthroughGate = ActiveDoseState.smoothstep(
                TrippyConfig.dmtBreakthroughThresholdUnits * 0.92D,
                TrippyConfig.dmtBreakthroughThresholdUnits * 1.08D,
                state.activeDose()
        );
        if (breakthroughGate >= 0.35D
                && progress >= TrippyConfig.dmtTransitionStartProgress
                && progress < TrippyConfig.dmtTransitionEndProgress) {
            return DmtStage.BREAKTHROUGH_TRANSITION;
        }
        if (breakthroughGate >= 0.35D && progress >= TrippyConfig.dmtPostBreakthroughStartProgress) {
            return DmtStage.POST_BREAKTHROUGH;
        }
        if (state.activeDose() >= TrippyConfig.dmtPreBreakthroughThresholdUnits) {
            return DmtStage.PRE_BREAKTHROUGH;
        }
        return DmtStage.LOW_THRESHOLD;
    }
}
