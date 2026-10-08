package com.trippycraft.substance.dmt;

import com.trippycraft.substance.ActiveDoseState;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;

public final class DmtEffectPipeline {
    private DmtEffectPipeline() {
    }

    public static DmtPipelineResult resolve(
            ActiveDoseState dose,
            DmtTripSignature signature,
            PsilocybinEnvironmentSnapshot environment,
            DmtEncounterDirector director
    ) {
        DmtStage stage = DmtStageResolver.INSTANCE.resolve(dose);
        DmtPhase phase = DmtDoseCurve.phase(dose.ageTicks(), dose.durationTicks(), dose.activeDose());
        double transition = DmtTransitionController.breakthroughTransition(dose, stage);
        DmtLayerProfile layers = DmtLayerProfile.resolve(dose, stage, transition, signature, environment);
        DmtEncounterState encounter = director == null
                ? DmtEncounterState.inactive(DmtEncounterTheme.HYPERDIMENSIONAL_CHAMBERS)
                : director.resolve(dose, stage, layers, signature, environment);
        return new DmtPipelineResult(stage, phase, layers, encounter);
    }
}
