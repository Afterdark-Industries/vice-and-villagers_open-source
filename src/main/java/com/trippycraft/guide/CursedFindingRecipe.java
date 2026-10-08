package com.trippycraft.guide;

import java.util.List;

public record CursedFindingRecipe(
        String title,
        CursedFindingRecipeType type,
        List<CursedFindingItemRef> inputs,
        CursedFindingItemRef output,
        List<String> notes
) {
    public CursedFindingRecipe {
        inputs = List.copyOf(inputs);
        notes = List.copyOf(notes);
    }
}
