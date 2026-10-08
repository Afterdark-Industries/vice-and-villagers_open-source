package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.mdma.MdmaEffectState;
import net.minecraft.client.gui.GuiGraphics;

/** Pink-cloud warmth layered over the world-grade saturation and bloom pass. */
public final class MdmaVisualRenderer {
    private MdmaVisualRenderer() {
    }

    public static void render(GuiGraphics graphics, MdmaEffectState state, float partialTick, float time) {
        if (state == null || !state.active()) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double visualScale = clamp(TrippyClientConfig.visualIntensity / 1.10D);
        double positive = clamp(state.saturation() * 0.38D + state.bloom() * 0.40D + state.warmth() * 0.22D) * visualScale;
        double bass = ClientTavernAmbience.bassPulse(partialTick) * clamp(state.bassSensitivity() / 1.15D) * visualScale;
        double doseDrive = smoothstep(100.0D, 500.0D, state.activeDoseMilligrams());
        double peakDrive = state.peakWindow() ? 1.0D : 0.30D;
        if (TrippyClientConfig.photosensitivityReduction) {
            bass *= 0.20D;
            positive *= 0.54D;
        }

        double slowBreath = 0.5D + Math.sin(time * 0.025D + state.activeDoseMilligrams() * 0.008D) * 0.5D;
        double cloud = positive * (0.030D + doseDrive * peakDrive * 0.042D);
        int magenta = color(cloud + bass * (0.038D + doseDrive * 0.030D), 255, 58, 204);
        int cyan = color(positive * (0.018D + doseDrive * peakDrive * 0.018D) + bass * 0.040D, 58, 226, 255);
        graphics.fillGradient(0, 0, width, height, magenta, cyan);

        int centerHalfWidth = Math.max(44, width / 3);
        int centerHalfHeight = Math.max(30, height / 3);
        int glowTop = color(positive * (0.024D + slowBreath * 0.018D + doseDrive * peakDrive * 0.024D) + bass * 0.090D, 255, 174, 237);
        int glowBottom = color(positive * (0.012D + doseDrive * peakDrive * 0.014D) + bass * 0.046D, 94, 220, 255);
        graphics.fillGradient(
                width / 2 - centerHalfWidth,
                height / 2 - centerHalfHeight,
                width / 2 + centerHalfWidth,
                height / 2 + centerHalfHeight,
                glowTop,
                glowBottom
        );

        double vignette = clamp(positive * (0.026D + doseDrive * 0.012D) + bass * 0.038D + state.coordinationNoise() * 0.028D);
        int bandX = Math.max(3, (int) Math.round(width * (0.012D + vignette * 0.34D)));
        int bandY = Math.max(3, (int) Math.round(height * (0.012D + vignette * 0.30D)));
        int edge = color(vignette, 58, 4, 52);
        graphics.fill(0, 0, bandX, height, edge);
        graphics.fill(width - bandX, 0, width, height, edge);
        graphics.fill(0, 0, width, bandY, edge);
        graphics.fill(0, height - bandY, width, height, edge);

        double comedown = clamp(state.comedownDesaturation() * 0.22D + state.audioMuffle() * 0.060D);
        if (comedown > 0.002D) {
            graphics.fill(0, 0, width, height, color(comedown, 116, 120, 126));
            int downEdge = color(comedown * 0.55D, 24, 25, 30);
            graphics.fill(0, 0, Math.max(5, width / 24), height, downEdge);
            graphics.fill(width - Math.max(5, width / 24), 0, width, height, downEdge);
        }
    }

    private static int color(double alpha, int red, int green, int blue) {
        int a = (int) Math.round(clamp(alpha) * 255.0D);
        return a << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / Math.max(0.0001D, edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }
}
