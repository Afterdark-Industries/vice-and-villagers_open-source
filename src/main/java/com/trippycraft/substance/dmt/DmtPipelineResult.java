package com.trippycraft.substance.dmt;

public record DmtPipelineResult(
        DmtStage stage,
        DmtPhase phase,
        DmtLayerProfile layers,
        DmtEncounterState encounter
) {
}
