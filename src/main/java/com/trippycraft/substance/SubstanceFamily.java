package com.trippycraft.substance;

public enum SubstanceFamily {
    PSYCHEDELIC("substance_family.trippycraft.psychedelic"),
    CANNABINOID("substance_family.trippycraft.cannabinoid"),
    STIMULANT("substance_family.trippycraft.stimulant"),
    OPIOID("substance_family.trippycraft.opioid"),
    EMPATHOGEN("substance_family.trippycraft.empathogen"),
    DISSOCIATIVE("substance_family.trippycraft.dissociative"),
    DEPRESSANT("substance_family.trippycraft.depressant"),
    DELIRIANT("substance_family.trippycraft.deliriant");

    private final String translationKey;

    SubstanceFamily(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
