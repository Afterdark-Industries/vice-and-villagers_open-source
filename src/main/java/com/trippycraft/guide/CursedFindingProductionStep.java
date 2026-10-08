package com.trippycraft.guide;

public record CursedFindingProductionStep(
        CursedFindingItemRef input,
        CursedFindingItemRef station,
        CursedFindingItemRef output,
        String note
) {
}
