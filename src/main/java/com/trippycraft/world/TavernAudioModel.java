package com.trippycraft.world;

/** Shared, testable volume curve measured from the tavern's exterior bounds. */
public final class TavernAudioModel {
    public static final double FULL_VOLUME_OUTSIDE_DISTANCE = 1.0D;
    public static final double SILENT_OUTSIDE_DISTANCE = 10.0D;

    private TavernAudioModel() {
    }

    public static float fadeForOutsideDistance(double distance) {
        if (!Double.isFinite(distance) || distance >= SILENT_OUTSIDE_DISTANCE) {
            return 0.0F;
        }
        if (distance <= FULL_VOLUME_OUTSIDE_DISTANCE) {
            return 1.0F;
        }
        double progress = (distance - FULL_VOLUME_OUTSIDE_DISTANCE)
                / (SILENT_OUTSIDE_DISTANCE - FULL_VOLUME_OUTSIDE_DISTANCE);
        double smooth = progress * progress * (3.0D - 2.0D * progress);
        return (float) (1.0D - smooth);
    }
}
