package com.trippycraft.guide;

public enum CursedFindingTab {
    OVERVIEW("Overview"),
    EFFECTS("Effects"),
    PRODUCTION("Chain"),
    RECIPES("Recipes"),
    NOTES("Notes");

    private final String displayName;

    CursedFindingTab(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
