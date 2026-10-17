package com.trippycraft.guide;

public enum CursedFindingCategory {
    PSYCHEDELICS("psychedelics", "Psychedelics"),
    STIMULANTS("stimulants", "Stimulants"),
    EMPATHOGENS("empathogens", "Empathogens"),
    OPIOIDS("opioids", "Opioids"),
    DISSOCIATIVES("dissociatives", "Dissociatives"),
    ALCOHOL("alcohol", "Alcohol"),
    DEPENDENCE_RECOVERY("dependence_recovery", "Dependence & Recovery"),
    ECONOMY_DEALING("economy_dealing", "Economy / Dealing"),
    UTILITIES("utilities", "Utilities");

    private final String id;
    private final String displayName;

    CursedFindingCategory(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
