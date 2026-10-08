package com.trippycraft.guide;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Supplier;

public record CursedFindingEntry(
        String id,
        String displayName,
        CursedFindingCategory category,
        Supplier<? extends ItemLike> icon,
        List<String> overview,
        List<String> effects,
        List<CursedFindingProductionStep> productionChain,
        List<CursedFindingRecipe> recipes,
        List<String> notes,
        String lockedMessage
) {
    public CursedFindingEntry {
        overview = List.copyOf(overview);
        effects = List.copyOf(effects);
        productionChain = List.copyOf(productionChain);
        recipes = List.copyOf(recipes);
        notes = List.copyOf(notes);
    }

    public ItemStack iconStack() {
        return new ItemStack(icon.get());
    }
}
