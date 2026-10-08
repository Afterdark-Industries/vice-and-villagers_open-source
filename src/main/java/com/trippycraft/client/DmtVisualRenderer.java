package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtEncounterEventType;
import com.trippycraft.substance.dmt.DmtEncounterState;
import com.trippycraft.substance.dmt.DmtStage;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public final class DmtVisualRenderer {
    private DmtVisualRenderer() {
    }

    public static void render(
            GuiGraphics graphics,
            DmtEffectState state,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            float time,
            PsychedelicBlendState blend
    ) {
        if (!TrippyClientConfig.rendererEnabled || !state.active() || !state.hasAuthoredEffects() || !TrippyConfig.enableDmtEffects || state.intensity() <= 0.01D) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double gain = clamp(blend.dmtVisualGain());
        double organic = psilocybin.active()
                ? clamp((psilocybin.organicBreathing() + psilocybin.colorWarmth()) / Math.max(1.0D, TrippyConfig.psilocybinMaxVisualIntensity)) * blend.psilocybinVisualGain()
                : 0.0D;
        double electric = lsd.active()
                ? clamp((lsd.prismSplit() + lsd.realityFold() + lsd.edgeHum()) / Math.max(1.0D, TrippyConfig.lsdMaxVisualIntensity)) * blend.lsdVisualGain()
                : 0.0D;

        renderThresholdPressure(graphics, state, width, height, time, gain, organic, electric);
        if (state.stage() == DmtStage.LOW_THRESHOLD || state.stage() == DmtStage.PRE_BREAKTHROUGH) {
            renderSymbolLanguage(graphics, state, width, height, time, gain * 0.34D, electric);
        }
        renderEntityPresence(graphics, state, width, height, time, gain * 0.72D, organic, electric);
        renderEncounterEvent(graphics, state, width, height, time, gain, organic, electric);
    }

    private static void renderThresholdPressure(GuiGraphics graphics, DmtEffectState state, int width, int height, float time, double gain, double organic, double electric) {
        double pressure = clamp(state.alienShimmer() * 0.34D + state.spatialInstability() * 0.26D + state.edgeFlicker() * 0.22D) * gain;
        if (pressure <= 0.01D) {
            return;
        }

        int edgeColor = color(pressure * (0.040D + electric * 0.020D), mixChannel(state.signature().primaryRed(), state.signature().accentRed(), 0.35D + electric * 0.35D), state.signature().primaryGreen(), state.signature().accentBlue());
        int band = Math.max(2, (int) Math.round(Math.min(width, height) * (0.010D + pressure * 0.018D)));
        graphics.fill(0, 0, width, band, edgeColor);
        graphics.fill(0, height - band, width, height, edgeColor);
        graphics.fill(0, 0, band, height, edgeColor);
        graphics.fill(width - band, 0, width, height, edgeColor);

        int marks = TrippyConfig.dmtAccessibilityMode ? 18 : 28 + (int) Math.round(pressure * 64.0D);
        for (int i = 0; i < marks; i++) {
            int h = hash(i, (int) state.signature().seed(), 0xD17D);
            double pulse = Math.sin(time * (0.070D + electric * 0.050D) + i * 0.83D + h * 0.003D) * 0.5D + 0.5D;
            if (pulse < 0.42D) {
                continue;
            }
            int x = Math.floorMod(h + (int) (Math.sin(time * 0.023D + i) * width * pressure * 0.050D), Math.max(1, width));
            int y = Math.floorMod(h / 29 + (int) (Math.cos(time * 0.019D + i * 1.3D) * height * pressure * 0.050D), Math.max(1, height));
            if (nearCenter(x, y, width, height)) {
                continue;
            }
            int size = 2 + Math.floorMod(h >>> 11, 5);
            int c = color(pressure * (0.030D + pulse * 0.044D), state.signature().primaryRed(), mixChannel(state.signature().secondaryGreen(), 255, organic * 0.28D), state.signature().accentBlue());
            drawGlyph(graphics, x, y, size, c, h);
        }
    }

    private static void renderEncounterArchitecture(GuiGraphics graphics, DmtEffectState state, int width, int height, float time, double gain, double organic, double electric) {
        double architecture = clamp((state.impossibleArchitecture() * 0.72D + state.encounter().architectureFocus() * 0.42D) * gain);
        if (architecture <= 0.012D || state.stage() != DmtStage.POST_BREAKTHROUGH && state.stage() != DmtStage.RETURN) {
            return;
        }

        int cx = width / 2;
        int cy = height / 2;
        int depth = TrippyConfig.dmtAccessibilityMode ? 5 : 8;
        for (int i = 0; i < depth; i++) {
            double layer = (i + 1) / (double) depth;
            double wobble = Math.sin(time * (0.025D + electric * 0.020D) + i * 0.82D + state.signature().seed() * 0.000002D) * architecture;
            int halfW = (int) Math.round(width * (0.040D + layer * (0.38D + architecture * 0.09D)) * (1.0D + wobble * 0.08D));
            int halfH = (int) Math.round(height * (0.030D + layer * (0.30D + organic * 0.04D)) * (1.0D - wobble * 0.06D));
            int y = cy + (int) Math.round(Math.sin(time * 0.011D + i) * height * 0.020D * architecture);
            int c = color(architecture * (0.028D + layer * 0.034D), mixChannel(state.signature().primaryRed(), state.signature().accentRed(), electric * 0.42D), mixChannel(state.signature().primaryGreen(), 255, organic * 0.20D), state.signature().secondaryBlue());
            graphics.fill(cx - halfW, y - halfH, cx + halfW, y - halfH + 1 + i % 2, c);
            graphics.fill(cx - halfW, y + halfH, cx + halfW, y + halfH + 1 + i % 2, c);
            graphics.fill(cx - halfW, y - halfH, cx - halfW + 1 + i % 2, y + halfH, c);
            graphics.fill(cx + halfW, y - halfH, cx + halfW + 1 + i % 2, y + halfH, c);

            if (i > 0) {
                int prevHalfW = (int) Math.round(width * (0.040D + (i / (double) depth) * (0.38D + architecture * 0.09D)));
                int prevHalfH = (int) Math.round(height * (0.030D + (i / (double) depth) * (0.30D + organic * 0.04D)));
                graphics.fill(cx - prevHalfW, cy - prevHalfH, cx - halfW, y - halfH, c);
                graphics.fill(cx + halfW, y - halfH, cx + prevHalfW, cy - prevHalfH, c);
            }
        }
    }

    private static void renderSymbolLanguage(GuiGraphics graphics, DmtEffectState state, int width, int height, float time, double gain, double electric) {
        double symbols = clamp((state.symbolLanguage() * 0.62D + state.symbolPressure() * 0.32D + state.encounter().symbolFocus() * 0.30D) * gain);
        if (symbols <= 0.012D) {
            return;
        }

        int cell = Math.max(18, Math.min(width, height) / (TrippyConfig.dmtAccessibilityMode ? 11 : 16));
        int cols = width / cell + 3;
        int rows = height / cell + 3;
        int max = TrippyConfig.dmtAccessibilityMode ? 80 : 170;
        int marks = 0;
        for (int y = -1; y < rows && marks < max; y++) {
            for (int x = -1; x < cols && marks < max; x++) {
                int h = hash(x, y, (int) (state.signature().seed() ^ state.encounter().eventSeed()));
                double sequence = Math.sin(time * (0.030D + electric * 0.026D) + h * 0.0017D + x * 0.6D - y * 0.41D) * 0.5D + 0.5D;
                double gate = (h & 255) / 255.0D;
                if (sequence * 0.62D + gate * 0.38D < 0.78D - symbols * 0.32D) {
                    continue;
                }
                int px = x * cell + (int) (Math.sin(time * 0.013D + h) * cell * 0.42D * symbols);
                int py = y * cell + (int) (Math.cos(time * 0.016D + h * 0.5D) * cell * 0.36D * symbols);
                if (nearCenter(px, py, width, height)) {
                    continue;
                }
                int size = Math.max(3, (int) Math.round(cell * (0.15D + symbols * 0.16D)));
                int c = color(symbols * (0.026D + sequence * 0.044D), state.signature().accentRed(), mixChannel(state.signature().accentGreen(), state.signature().primaryGreen(), 0.45D), mixChannel(state.signature().accentBlue(), 255, electric * 0.35D));
                drawGlyph(graphics, px, py, size, c, h);
                marks++;
            }
        }
    }

    private static void renderEntityPresence(GuiGraphics graphics, DmtEffectState state, int width, int height, float time, double gain, double organic, double electric) {
        double presence = clamp((state.entityPresence() / Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity) + state.encounter().entityFocus() * 0.72D) * gain);
        if (presence <= 0.018D) {
            return;
        }

        int observers = state.encounter().active() ? 1 : presence > 0.55D && !TrippyConfig.dmtAccessibilityMode ? 2 : 1;
        for (int i = 0; i < observers; i++) {
            int h = hash(i, (int) state.signature().seed(), 0x51E17);
            double side = observers == 1 ? Math.signum(Math.sin(h)) : (i == 0 ? -1.0D : 1.0D);
            int x = width / 2 + (int) (side * width * (0.22D + ((h >>> 4) & 31) / 255.0D) + Math.sin(time * 0.021D + h) * width * 0.018D * presence);
            int y = height / 2 + (int) (Math.cos(time * 0.018D + h * 0.7D) * height * 0.10D * presence);
            int bodyH = (int) Math.round(height * (0.12D + presence * 0.18D));
            int bodyW = Math.max(4, (int) Math.round(width * (0.012D + presence * 0.016D)));
            int c = color(presence * (0.055D + organic * 0.018D), mixChannel(state.signature().secondaryRed(), state.signature().accentRed(), electric * 0.45D), state.signature().secondaryGreen(), mixChannel(state.signature().primaryBlue(), 255, organic * 0.25D));
            graphics.fill(x - bodyW, y - bodyH / 2, x + bodyW, y + bodyH / 2, c);
            graphics.fill(x - bodyW * 3, y - bodyH / 2, x + bodyW * 3, y - bodyH / 2 + 2, c);
            graphics.fill(x - bodyW * 2, y + bodyH / 2 - 2, x + bodyW * 2, y + bodyH / 2, c);
            drawGlyph(graphics, x - bodyW * 2, y - bodyH / 2 - bodyW * 3, bodyW * 4, color(presence * 0.080D, state.signature().accentRed(), 255, state.signature().accentBlue()), h);
        }
    }

    private static void renderEncounterEvent(GuiGraphics graphics, DmtEffectState state, int width, int height, float time, double gain, double organic, double electric) {
        DmtEncounterState encounter = state.encounter();
        if (!encounter.active() || encounter.eventType() == DmtEncounterEventType.NONE) {
            return;
        }
        double strength = clamp(encounter.eventStrength() * gain);
        if (strength <= 0.01D) {
            return;
        }

        int cx = width / 2;
        int cy = height / 2;
        int cA = color(strength * 0.090D, state.signature().primaryRed(), mixChannel(state.signature().accentGreen(), 255, organic * 0.35D), state.signature().accentBlue());
        int cB = color(strength * 0.060D, mixChannel(state.signature().secondaryRed(), state.signature().accentRed(), electric * 0.45D), state.signature().secondaryGreen(), state.signature().primaryBlue());
        switch (encounter.eventType()) {
            case INSPECTION -> {
                int size = (int) Math.round(Math.min(width, height) * (0.08D + strength * 0.08D));
                drawGlyph(graphics, cx, cy - size, size, cA, (int) encounter.eventSeed());
                graphics.fill(cx - size * 2, cy, cx + size * 2, cy + 2, cB);
            }
            case INVITATION_PORTAL -> renderPortal(graphics, cx, cy, width, height, time, strength, cA, cB);
            case REVEAL_HIDDEN_LAYER -> {
                int bands = TrippyConfig.dmtAccessibilityMode ? 4 : 7;
                for (int i = 0; i < bands; i++) {
                    int y = (int) (height * (0.18D + i * 0.11D) + Math.sin(time * 0.055D + i) * height * 0.018D * strength);
                    graphics.fill(-width / 8, y, width + width / 8, y + 2, i % 2 == 0 ? cA : cB);
                }
            }
            case ASSEMBLY_ROOM -> {
                int pillars = TrippyConfig.dmtAccessibilityMode ? 4 : 7;
                for (int i = 0; i < pillars; i++) {
                    double t = (i + 0.5D) / pillars;
                    int x = (int) (width * t + Math.sin(time * 0.020D + i) * width * 0.012D);
                    int h = (int) (height * (0.18D + strength * 0.18D));
                    graphics.fill(x - 1, cy - h, x + 2, cy + h, i % 2 == 0 ? cA : cB);
                }
            }
            case MIRRORED_SELF -> {
                int w = (int) Math.round(width * (0.050D + strength * 0.030D));
                int h = (int) Math.round(height * (0.14D + strength * 0.060D));
                graphics.fill(cx - w - 20, cy - h, cx - 18, cy + h, cA);
                graphics.fill(cx + 18, cy - h, cx + w + 20, cy + h, cB);
            }
            case OBSERVER_SKY -> {
                int top = Math.max(12, height / 7);
                int span = (int) Math.round(width * (0.18D + strength * 0.18D));
                graphics.fill(cx - span, top, cx + span, top + 3, cA);
                drawGlyph(graphics, cx, top + span / 4, Math.max(12, span / 6), cB, (int) encounter.eventSeed());
            }
            case NONE -> {
            }
        }
    }

    private static void renderPortal(GuiGraphics graphics, int cx, int cy, int width, int height, float time, double strength, int cA, int cB) {
        PoseStack pose = graphics.pose();
        int rings = TrippyConfig.dmtAccessibilityMode ? 4 : 7;
        for (int i = 0; i < rings; i++) {
            int radius = (int) Math.round(Math.min(width, height) * (0.045D + i * 0.030D + strength * 0.020D));
            pose.pushPose();
            pose.translate(cx, cy, 0.0F);
            pose.mulPose(Axis.ZP.rotation((float) (time * 0.012D + i * 0.41D)));
            graphics.fill(-radius, -1, radius, 1, i % 2 == 0 ? cA : cB);
            graphics.fill(-1, -radius, 1, radius, i % 2 == 0 ? cB : cA);
            int half = radius / 2;
            graphics.fill(-half, -half, half, -half + 1, cA);
            graphics.fill(-half, half, half, half + 1, cB);
            pose.popPose();
        }
    }

    private static void drawGlyph(GuiGraphics graphics, int x, int y, int size, int color, int variant) {
        int half = Math.max(2, size / 2);
        int arm = Math.max(2, size);
        int mode = Math.floorMod(variant, 6);
        graphics.fill(x - half, y, x + half, y + 1, color);
        graphics.fill(x, y - half, x + 1, y + half, color);
        if (mode == 0 || mode == 3) {
            graphics.fill(x - arm, y - half, x - arm + 1, y + half, color);
            graphics.fill(x + arm, y - half, x + arm + 1, y + half, color);
        }
        if (mode == 1 || mode == 4) {
            graphics.fill(x - half, y - arm, x + half, y - arm + 1, color);
            graphics.fill(x - half, y + arm, x + half, y + arm + 1, color);
        }
        if (mode == 2 || mode == 5) {
            graphics.fill(x - arm, y - arm, x - half, y - arm + 1, color);
            graphics.fill(x + half, y + arm, x + arm, y + arm + 1, color);
            graphics.fill(x - arm, y + arm, x - arm + 1, y + half, color);
            graphics.fill(x + arm, y - half, x + arm + 1, y - arm, color);
        }
    }

    private static boolean nearCenter(int x, int y, int width, int height) {
        return Math.abs(x - width / 2) < Math.max(28, width / 9) && Math.abs(y - height / 2) < Math.max(22, height / 9);
    }

    private static int color(double alpha, int red, int green, int blue) {
        double capped = clamp(alpha * TrippyClientConfig.visualIntensity);
        if (TrippyClientConfig.photosensitivityReduction) {
            capped = Math.min(capped, 0.22D);
        }
        int a = (int) Math.round(Math.min(0.46D, capped) * 255.0D);
        return ((a & 0xFF) << 24) | (clampColor(red) << 16) | (clampColor(green) << 8) | clampColor(blue);
    }

    private static int mixChannel(int a, int b, double amount) {
        double t = clamp(amount);
        return clampColor((int) Math.round(a + (b - a) * t));
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static int hash(int a, int b, int seed) {
        int value = seed ^ a * 0x45D9F3B ^ b * 0x119DE1F3;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return value;
    }
}
