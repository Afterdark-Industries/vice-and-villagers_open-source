package com.trippycraft.guide;

public enum CursedFindingRecipeType {
    CRAFTING_TABLE("Crafting Table"),
    HEATING("Heating"),
    MORTAR_AND_PESTLE("Mortar and Pestle"),
    CANNABIS_PRESS("Cannabis Press"),
    BREW_CAULDRON("Brew Cauldron"),
    CHEMISTRY_TABLE("Chemistry Table"),
    ADVANCED_CHEMISTRY_TABLE("Advanced Chemistry Table"),
    DOSAGE_SELECTOR("Dosage Selector"),
    WORLD_INTERACTION("World Interaction");

    private final String displayName;

    CursedFindingRecipeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
