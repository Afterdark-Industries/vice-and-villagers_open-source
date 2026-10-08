package com.trippycraft.substance.dmt;

public enum DmtEncounterTheme {
    IMPOSSIBLE_ARCHITECTURE(1.45D, 0.42D, 0.72D, 0.44D),
    HYPERDIMENSIONAL_CHAMBERS(1.12D, 0.58D, 0.82D, 0.52D),
    LIVING_SYMBOL_SPACE(0.64D, 0.70D, 1.48D, 0.62D),
    MACHINE_CATHEDRAL(1.22D, 0.86D, 1.12D, 0.42D),
    ENTITY_OBSERVATORY(0.62D, 1.52D, 0.82D, 0.72D),
    WORLD_BEHIND_WORLD(0.92D, 0.72D, 0.86D, 1.36D),
    ORGANIC_ENTITY_REALM(0.58D, 1.14D, 0.78D, 1.55D);

    private final double architectureBias;
    private final double entityBias;
    private final double symbolBias;
    private final double consciousnessBias;

    DmtEncounterTheme(double architectureBias, double entityBias, double symbolBias, double consciousnessBias) {
        this.architectureBias = architectureBias;
        this.entityBias = entityBias;
        this.symbolBias = symbolBias;
        this.consciousnessBias = consciousnessBias;
    }

    public double architectureBias() {
        return architectureBias;
    }

    public double entityBias() {
        return entityBias;
    }

    public double symbolBias() {
        return symbolBias;
    }

    public double consciousnessBias() {
        return consciousnessBias;
    }
}
