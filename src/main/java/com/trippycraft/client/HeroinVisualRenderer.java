package com.trippycraft.client;

import com.trippycraft.substance.heroin.HeroinEffectState;
import net.minecraft.client.gui.GuiGraphics;

public final class HeroinVisualRenderer {
    private HeroinVisualRenderer() {
    }

    public static void render(GuiGraphics graphics, HeroinEffectState heroin, float time) {
        if (heroin == null || !heroin.active()) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double load = heroinVisualLoad(heroin);
        if (load <= 0.01D) {
            return;
        }

        double warmth = ClientEffectGraph.saturate(load * 0.68D + heroin.warmth() * 0.074D + heroin.euphoria() * 0.038D);
        double softness = ClientEffectGraph.saturate(load * 0.54D + heroin.softFocus() * 0.060D + heroin.painSuppression() * 0.034D);
        double nod = ClientEffectGraph.saturate(ClientRenderEvents.heroinNodAmount() * 0.92D + heroin.microSleep() * 0.040D + heroin.nodding() * 0.028D);
        double blackout = ClientEffectGraph.saturate(ClientRenderEvents.heroinBlackoutAmount());
        double breath = 0.5D + 0.5D * Math.sin(time * (0.0042D + softness * 0.0015D) + heroin.activeDoseMilligrams() * 0.010D);

        int topWarm = color(0.018D + warmth * 0.058D + breath * 0.010D, 255, 188 + (int) (warmth * 50.0D), 88);
        int bottomWarm = color(0.014D + warmth * 0.038D + softness * 0.016D, 138, 92 + (int) (softness * 44.0D), 46);
        graphics.fillGradient(0, 0, width, height, topWarm, bottomWarm);

        int centerHalfWidth = Math.max(42, width / 3);
        int centerHalfHeight = Math.max(30, height / 4);
        int centerGlowTop = color(0.008D + warmth * 0.026D + breath * 0.010D, 255, 232, 162);
        int centerGlowBottom = color(0.003D + softness * 0.012D, 216, 166, 96);
        graphics.fillGradient(
                width / 2 - centerHalfWidth,
                height / 2 - centerHalfHeight,
                width / 2 + centerHalfWidth,
                height / 2 + centerHalfHeight,
                centerGlowTop,
                centerGlowBottom
        );

        if (nod > 0.01D || blackout > 0.01D) {
            double sleep = smoothstep(0.04D, 0.86D, nod);
            double dusk = ClientEffectGraph.saturate(sleep * 0.64D + blackout * 0.96D);
            int lid = Math.max(4, (int) (height * (0.06D + sleep * 0.24D + blackout * 0.18D)));
            int upper = color(0.030D + dusk * 0.440D, 16, 10, 6);
            int lower = color(0.026D + dusk * 0.380D, 18, 12, 6);
            graphics.fillGradient(0, 0, width, lid, upper, color(0.006D + sleep * 0.060D, 92, 58, 24));
            graphics.fillGradient(0, height - lid, width, height, color(0.006D + sleep * 0.050D, 112, 70, 28), lower);
            int veil = color(dusk * 0.440D, 4, 3, 2);
            graphics.fill(0, 0, width, height, veil);
        }
    }

    private static double heroinVisualLoad(HeroinEffectState heroin) {
        double load = Math.log1p(Math.max(0.0D, heroin.intensity())) / Math.log1p(8.2D);
        load += ClientEffectGraph.saturate((heroin.activeDoseMilligrams() - 25.0D) / 475.0D) * 0.10D;
        load += ClientEffectGraph.saturate(heroin.warmth() / 8.2D) * 0.18D;
        return ClientEffectGraph.saturate(load);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = ClientEffectGraph.saturate((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    private static int color(double alpha, int red, int green, int blue) {
        int a = (int) Math.round(ClientEffectGraph.saturate(alpha) * 255.0D);
        return ((a & 0xFF) << 24) | (clampColor(red) << 16) | (clampColor(green) << 8) | clampColor(blue);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
