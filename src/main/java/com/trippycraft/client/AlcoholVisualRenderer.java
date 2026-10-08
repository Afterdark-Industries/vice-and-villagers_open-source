package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import net.minecraft.client.gui.GuiGraphics;

public final class AlcoholVisualRenderer {
    private AlcoholVisualRenderer() {
    }

    public static void render(GuiGraphics graphics, AlcoholEffectState state, RecoveryState recovery, float time) {
        double hangover = hangoverLoad(recovery, state);
        double withdrawal = cap(recovery.withdrawalSeverity());
        if (!TrippyClientConfig.rendererEnabled || !TrippyConfig.enableAlcoholEffects || (!state.active() && hangover <= 0.01D && withdrawal <= 0.01D)) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double safe = Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
        double diplopia = cap(state.diplopia() / safe);
        double blur = cap(state.focusBlur() / safe);
        double tunnel = cap(state.peripheralNarrowing() / safe);
        double discomfort = Math.max(hangover, withdrawal);
        diplopia = Math.max(diplopia, hangover * 0.10D + withdrawal * 0.18D);
        blur = Math.max(blur, hangover * 0.26D + withdrawal * 0.20D);
        tunnel = Math.max(tunnel, discomfort * 0.30D + withdrawal * 0.10D);
        if (TrippyConfig.alcoholAccessibilityMode || TrippyClientConfig.motionReduction) {
            diplopia *= 0.42D;
            blur *= 0.48D;
        }

        renderLightSensitivity(graphics, width, height, time, hangover, withdrawal);
    }

    private static void renderLightSensitivity(GuiGraphics graphics, int width, int height, float time, double hangover, double withdrawal) {
        double brightness = cap(hangover * 0.34D + withdrawal * 0.16D);
        double irritation = cap(withdrawal * 0.28D + hangover * 0.22D);
        if (brightness > 0.01D) {
            graphics.fill(0, 0, width, height, color(brightness * 0.20D, 255, 246, 215));
        }
        if (irritation <= 0.01D) {
            return;
        }
        int bands = 4 + (int) Math.round(irritation * 6.0D);
        for (int index = 0; index < bands; index++) {
            double phase = time * (0.012D + irritation * 0.020D) + index * 0.8D;
            int y = (int) Math.round((height * (index + 1.0D) / (bands + 1.0D)) + Math.sin(phase * 5.0D) * height * 0.010D * irritation);
            int bandHeight = Math.max(1, (int) Math.round(1.0D + irritation * 2.0D));
            graphics.fill(0, y, width, y + bandHeight, color(irritation * 0.08D, 255, 255, 245));
        }
    }

    private static double hangoverLoad(RecoveryState recovery, AlcoholEffectState state) {
        if (!recovery.hangoverActive()) {
            return 0.0D;
        }
        double activeDrinks = state.active() ? state.activeStandardDrinks() : 0.0D;
        double mask = 1.0D - smoothstep(0.20D, 1.25D, activeDrinks);
        double remaining = recovery.hangoverDurationTicks() <= 0 ? 0.0D : recovery.hangoverTicks() / (double) recovery.hangoverDurationTicks();
        return cap(recovery.hangoverSeverity() * Math.sqrt(Math.max(0.0D, remaining)) * mask);
    }

    private static double cap(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        double capped = Math.max(0.0D, Math.min(1.0D, value));
        if (TrippyClientConfig.photosensitivityReduction) {
            capped = Math.min(capped, 0.44D);
        }
        return capped;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static int color(double alpha, int red, int green, int blue) {
        int a = (int) Math.round(Math.min(0.42D, cap(alpha)) * 255.0D);
        return ((a & 0xFF) << 24) | (clampColor(red) << 16) | (clampColor(green) << 8) | clampColor(blue);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
