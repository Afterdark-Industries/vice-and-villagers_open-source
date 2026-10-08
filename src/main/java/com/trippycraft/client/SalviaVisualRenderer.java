package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.salvia.SalviaEffectState;
import net.minecraft.client.gui.GuiGraphics;

public final class SalviaVisualRenderer {
    private SalviaVisualRenderer() {
    }

    public static void render(GuiGraphics graphics, SalviaEffectState state, float time) {
        render(graphics, state, time, 1.0D);
    }

    public static void render(GuiGraphics graphics, SalviaEffectState state, float time, double blendGain) {
        if (!TrippyClientConfig.rendererEnabled || !state.active() || !TrippyConfig.enableSalviaEffects || state.visualIntensity() <= 0.01D) {
            return;
        }

        double gain = ClientEffectGraph.saturate(blendGain);
        if (gain <= 0.01D) {
            return;
        }

        // Keep salvia presentation in the world-space shader path only.
        // Screen-space bands and borders read as broken overlays at high doses.
    }
}