package com.trippycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** A local double clock keeps sub-tick motion even after a world has run for months. */
final class PsilocybinPresentationClock {
    private static ClientLevel level;
    private static long origin;
    private static double previousFrame = Double.NaN;
    private static double stereoTime;

    private PsilocybinPresentationClock() {
    }

    static double timeTicks(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            level = null;
            previousFrame = Double.NaN;
            return 0.0D;
        }
        if (level != minecraft.level) {
            level = minecraft.level;
            origin = level.getGameTime();
            previousFrame = Double.NaN;
        }
        double frame = (level.getGameTime() - origin) + Math.max(0.0D, Math.min(1.0D, partialTick));
        if (VivecraftCompat.isVrActive()) {
            if (Double.isFinite(previousFrame)
                    && (!VivecraftCompat.isFirstWorldPass() || frame == previousFrame)) {
                return stereoTime;
            }
            previousFrame = frame;
            stereoTime = frame;
        }
        return frame;
    }
}
