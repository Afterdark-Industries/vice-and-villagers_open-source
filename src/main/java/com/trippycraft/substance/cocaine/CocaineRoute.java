package com.trippycraft.substance.cocaine;

public enum CocaineRoute {
    POWDER(1.0D, 1.0D, 1.0D, 1.0D),
    CRACK(1.65D, 0.46D, 1.42D, 0.60D);

    private final double speedMultiplier;
    private final double durationScale;
    private final double intensityBoost;
    private final double extensionScale;

    CocaineRoute(double speedMultiplier, double durationScale, double intensityBoost, double extensionScale) {
        this.speedMultiplier = speedMultiplier;
        this.durationScale = durationScale;
        this.intensityBoost = intensityBoost;
        this.extensionScale = extensionScale;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public double durationScale() {
        return durationScale;
    }

    public double intensityBoost() {
        return intensityBoost;
    }

    public double extensionScale() {
        return extensionScale;
    }
}