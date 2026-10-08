package com.trippycraft.client;

import com.trippycraft.substance.lsd.LsdEffectState;
import net.minecraft.client.gui.GuiGraphics;

public final class LsdVisualRenderer {
    private LsdVisualRenderer() { }

    public static void render(GuiGraphics graphics, LsdEffectState state, float time) {
        // The coherent world surface shader owns LSD colour and geometry.
        // HUD-space filter layers would wash out the scene and duplicate motion.
    }
}
