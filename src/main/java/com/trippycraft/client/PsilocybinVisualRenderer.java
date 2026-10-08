package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

/** A few quiet peripheral motes; colour, clarity and ripples belong on world textures. */
public final class PsilocybinVisualRenderer {
    private PsilocybinVisualRenderer() {
    }

    public static void render(GuiGraphics graphics, PsilocybinEffectState state, float time) {
        render(graphics, state, time, 1.0D);
    }

    public static void render(GuiGraphics graphics, PsilocybinEffectState state, float time, double blendGain) {
        if (!state.active() || !TrippyConfig.enablePsilocybinEffects) return;
        double presence = (1.0D - Math.exp(-Math.max(0.0D, state.spores()) * 0.022D))
                * Math.max(0.0D, Math.min(1.0D, blendGain));
        if (presence <= 0.002D) return;
        double clock = PsilocybinPresentationClock.timeTicks(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int i = 0; i < 128; i++) {
            int hash = hash(i, state.signature().seed());
            double phase = (hash & 4095) / 4096.0D * Math.PI * 2.0D;
            double x = ((hash >>> 8) & 65535) / 65536.0D;
            x += Math.sin(clock * 0.003D + phase) * 0.009D * presence;
            double y = (((hash >>> 2) & 65535) / 65536.0D + clock * 0.00010D) % 1.0D;
            double borderFade = smoothstep(0.0D, 0.07D, y) * (1.0D - smoothstep(0.93D, 1.0D, y));
            double center = Math.hypot((x - 0.5D) * 1.5D, y - 0.5D);
            double peripheralFade = smoothstep(0.10D, 0.32D, center);
            double drift = 0.70D + Math.sin(clock * 0.005D + phase) * 0.30D;
            int alpha = (int) Math.round(255.0D * presence * 0.085D * drift * borderFade * peripheralFade);
            if (alpha <= 0) continue;
            int px = (int) Math.round(x * width);
            int py = (int) Math.round(y * height);
            graphics.fill(px, py, px + 1, py + 1, (alpha << 24) | 0xC9D5BB);
        }
    }

    private static int hash(int index, long seed) {
        long value = seed + index * 0x9E3779B97F4A7C15L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return (int) value;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
