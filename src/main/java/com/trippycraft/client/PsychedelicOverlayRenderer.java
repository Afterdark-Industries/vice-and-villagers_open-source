package com.trippycraft.client;

import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetamineMasking;
import com.trippycraft.substance.salvia.SalviaEffectState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public final class PsychedelicOverlayRenderer {
    private PsychedelicOverlayRenderer() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        ClientEffectSample sample = ClientEffectGraph.sample(partialTick);
        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        RecoveryState alcoholRecovery = ClientExperienceStore.alcoholRecovery();
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        CocaineEffectState cocaine = ClientExperienceStore.cocaine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        SalviaEffectState salvia = ClientExperienceStore.salvia();
        var mdma = ClientExperienceStore.mdma();
        if (!sample.visible()) {
            if (alcohol.active() || alcoholRecovery.hangoverActive() || alcoholRecovery.withdrawalActive()) {
                AlcoholVisualRenderer.render(graphics, alcohol, alcoholRecovery, partialTick);
            }
            renderKetamineOverlay(graphics, ketamine, sample.time());
            renderCocaineOverlay(graphics, cocaine, sample.time());
            HeroinVisualRenderer.render(graphics, heroin, sample.time());
            renderCannabisTunnel(graphics, cannabis, sample.time());
            SalviaVisualRenderer.render(graphics, salvia, sample.time(), salviaVisualGain(salvia));
            MdmaVisualRenderer.render(graphics, mdma, partialTick, sample.time());
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(), ClientExperienceStore.lsd(), ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        renderColorAdaptation(graphics, sample, width, height);
        renderRiskVignette(graphics, sample, width, height);
        renderKetamineOverlay(graphics, ketamine, sample.time());
        renderCocaineOverlay(graphics, cocaine, sample.time());
        HeroinVisualRenderer.render(graphics, heroin, sample.time());
        renderCannabisTunnel(graphics, cannabis, sample.time());
        AlcoholVisualRenderer.render(graphics, alcohol, alcoholRecovery, partialTick);
        MdmaVisualRenderer.render(graphics, mdma, partialTick, sample.time());
        PsilocybinVisualRenderer.render(graphics, ClientExperienceStore.psilocybin(), sample.time(), blend.psilocybinVisualGain());
        LsdVisualRenderer.render(graphics, ClientExperienceStore.lsd(), sample.time());
        SalviaVisualRenderer.render(graphics, salvia, sample.time(), salviaVisualGain(salvia));
        DmtVisualRenderer.render(graphics, ClientExperienceStore.dmt(), ClientExperienceStore.psilocybin(), ClientExperienceStore.lsd(), sample.time(), blend);
    }

    private static void renderCocaineOverlay(GuiGraphics graphics, CocaineEffectState cocaine, float time) {
        if (cocaine == null || !cocaine.active()) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double load = cocaineVisualLoad(cocaine);
        if (load <= 0.01D) {
            return;
        }

        double focus = ClientEffectGraph.saturate(load * 0.34D + cocaine.tunnelFocus() * 0.032D + cocaine.clarityLift() * 0.022D);
        double harsh = ClientEffectGraph.saturate(load * 0.28D + cocaine.lightHarshness() * 0.030D + cocaine.edgeShimmer() * 0.020D);
        double flicker = ClientEffectGraph.saturate(cocaine.microFlicker() * 0.030D + cocaine.jitter() * 0.016D);
        double crash = ClientEffectGraph.saturate(cocaine.crashDesaturation() * 0.42D + cocaine.crashDrag() * 0.020D);

        int edgeShade = color(0.010D + focus * 0.040D + crash * 0.032D, 24 + (int) (crash * 34.0D), 20 + (int) (crash * 30.0D), 16 + (int) (crash * 28.0D));
        graphics.fillGradient(0, 0, width, height, color(0.0D, 0, 0, 0), edgeShade);

        int centerHalfWidth = Math.max(26, width / 5);
        int centerHalfHeight = Math.max(20, height / 6);
        double pulse = 0.5D + 0.5D * Math.sin(time * (0.020D + flicker * 0.020D) + cocaine.activeDoseGrams() * 0.30D);
        int centerTop = color(0.004D + harsh * 0.020D + pulse * 0.006D, 255, 246, 212);
        int centerBottom = color(0.001D + harsh * 0.010D, 204, 188 + (int) (focus * 18.0D), 148);
        graphics.fillGradient(
                width / 2 - centerHalfWidth,
                height / 2 - centerHalfHeight,
                width / 2 + centerHalfWidth,
                height / 2 + centerHalfHeight,
                centerTop,
                centerBottom
        );

        if (flicker > 0.02D) {
            int lines = 4 + (int) Math.round(flicker * 8.0D);
            for (int i = 0; i < lines; i++) {
                int y = (int) (height * (0.16D + i / (double) (lines + 1) * 0.68D) + Math.sin(time * 0.045D + i * 1.3D) * height * 0.008D * flicker);
                int lineColor = color(0.003D + flicker * 0.010D, 255, 252, 236);
                graphics.fill(width / 6, y, width - width / 6, y + 1, lineColor);
            }
        }
    }

    private static void renderCannabisTunnel(GuiGraphics graphics, CannabisEffectState cannabis, float time) {
        if (cannabis == null || !cannabis.active()) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double load = cannabisVisualLoad(cannabis);
        if (load <= 0.01D) {
            return;
        }

        double warmth = ClientEffectGraph.saturate(load * 0.46D + cannabis.warmthShift() * 0.060D + cannabis.saturationBoost() * 0.030D);
        double greens = ClientEffectGraph.saturate(load * 0.30D + cannabis.greenRichness() * 0.050D);
        double breath = Math.sin(time * (0.0048D + cannabis.breathingPulse() * 0.00035D) + cannabis.doseScale() * 0.10D);
        double bloom = 0.5D + 0.5D * breath;

        int topWarm = color(0.010D + warmth * 0.020D + bloom * 0.004D, 255, 176 + (int) (warmth * 54.0D), 118);
        int bottomWarm = color(0.006D + warmth * 0.014D + greens * 0.006D, 124, 164 + (int) (greens * 44.0D), 92);
        graphics.fillGradient(0, 0, width, height, topWarm, bottomWarm);

        int ambientLift = color(0.004D + warmth * 0.009D + greens * 0.006D, 86, 126 + (int) (greens * 48.0D), 78);
        graphics.fillGradient(0, 0, width, height, ambientLift, color(0.0D, 0, 0, 0));

        int centerHalfWidth = Math.max(32, width / 3);
        int centerHalfHeight = Math.max(24, height / 4);
        int centerGlowTop = color(0.004D + warmth * 0.008D + bloom * 0.004D, 246, 230 + (int) (warmth * 18.0D), 172);
        int centerGlowBottom = color(0.001D + warmth * 0.003D, 178, 192 + (int) (greens * 18.0D), 136);
        graphics.fillGradient(
                width / 2 - centerHalfWidth,
                height / 2 - centerHalfHeight,
                width / 2 + centerHalfWidth,
                height / 2 + centerHalfHeight,
                centerGlowTop,
                centerGlowBottom
        );
    }

    private static void renderKetamineOverlay(GuiGraphics graphics, KetamineEffectState ketamine, float time) {
        if (ketamine == null || !ketamine.active()) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double load = ketamineVisualLoad(ketamine);
        if (load <= 0.01D) {
            return;
        }

        double fade = ClientEffectGraph.saturate(load * 0.28D + ketamine.uiFade() * 0.12D + ketamine.audioMuffle() * 0.05D);
        double tunnel = ClientEffectGraph.saturate(load * 0.20D + ketamine.depthMismatch() * 0.10D + ketamine.collapseDepth() * 0.10D);
        double echo = ClientEffectGraph.saturate(ketamine.bodyEcho() * 0.12D + ketamine.cameraLag() * 0.08D + load * 0.08D);
        double collapse = ClientEffectGraph.saturate(ketamine.collapseRisk() * 0.14D + ketamine.collapseDepth() * 0.18D);

        int veilTop = color(0.004D + fade * 0.022D, 178, 194 + (int) (echo * 18.0D), 214 + (int) (tunnel * 24.0D));
        int veilBottom = color(0.012D + fade * 0.030D + collapse * 0.016D, 34, 38, 52 + (int) (collapse * 18.0D));
        graphics.fillGradient(0, 0, width, height, veilTop, veilBottom);

        int bandX = Math.max(6, (int) (width * (0.06D + tunnel * 0.08D + collapse * 0.06D)));
        int bandY = Math.max(6, (int) (height * (0.05D + tunnel * 0.07D + collapse * 0.05D)));
        int edge = color(0.008D + tunnel * 0.022D + collapse * 0.016D, 10, 12, 18);
        graphics.fill(0, 0, bandX, height, edge);
        graphics.fill(width - bandX, 0, width, height, edge);
        graphics.fill(0, 0, width, bandY, edge);
        graphics.fill(0, height - bandY, width, height, edge);

        double pulse = 0.5D + 0.5D * Math.sin(time * (0.020D + echo * 0.018D) + ketamine.activeDoseMilligrams() * 0.050D);
        int ghostWidth = Math.max(34, width / 4);
        int ghostHeight = Math.max(24, height / 5);
        int offsetX = (int) (Math.sin(time * 0.016D + 1.2D) * width * (0.008D + echo * 0.028D));
        int offsetY = (int) (Math.cos(time * 0.014D + 0.4D) * height * (0.006D + echo * 0.018D));
        int echoA = color(0.004D + echo * 0.018D + pulse * 0.006D, 198, 214, 236);
        int echoB = color(0.002D + echo * 0.010D, 108, 126, 158);
        graphics.fillGradient(
                width / 2 - ghostWidth + offsetX,
                height / 2 - ghostHeight + offsetY,
                width / 2 + ghostWidth + offsetX,
                height / 2 + ghostHeight + offsetY,
                echoA,
                echoB
        );
        graphics.fillGradient(
                width / 2 - (int) (ghostWidth * 0.92D) - offsetX / 2,
                height / 2 - (int) (ghostHeight * 0.92D) - offsetY / 2,
                width / 2 + (int) (ghostWidth * 0.92D) - offsetX / 2,
                height / 2 + (int) (ghostHeight * 0.92D) - offsetY / 2,
                color(0.003D + echo * 0.012D, 154, 178, 206),
                color(0.001D + echo * 0.006D, 68, 80, 104)
        );
    }

    private static double cocaineVisualLoad(CocaineEffectState cocaine) {
        double load = Math.log1p(Math.max(0.0D, cocaine.intensity())) / Math.log1p(10.0D);
        load += ClientEffectGraph.saturate((cocaine.activeDoseGrams() - 1.0D) / 5.0D) * 0.10D;
        return ClientEffectGraph.saturate(load);
    }

    private static void renderColorAdaptation(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        double overlayAlpha = sample.overlayAlpha() * ClientEffectGraph.genericPresentationGain(sample);
        if (overlayAlpha <= 0.0001D) return;
        ClientRenderSignature signature = sample.signature();
        int warm = color(overlayAlpha * (0.42D + sample.euphoria() * 0.32D + signature.warmth() * 0.34D), 255, 126 + (int) (signature.warmth() * 48), 86);
        int cool = color(overlayAlpha * (0.22D + sample.dissociation() * 0.30D + signature.coolDistance() * 0.36D), 78, 116 + (int) (signature.depthSeparation() * 40), 255);

        graphics.fill(0, 0, width, height, cool);
        graphics.fillGradient(0, 0, width, height, warm, color(overlayAlpha * 0.20D, 42, 255, 186));
    }

    private static void renderTemporalEchoes(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        ClientRenderSignature signature = sample.signature();
        double trail = ClientEffectGraph.saturate(sample.timeDilation() * 0.70D + sample.dissociation() * 0.50D + signature.temporalSlip() * 0.18D);
        if (trail <= 0.01D) {
            return;
        }

        float time = sample.time();
        int bands = 4 + (int) Math.round(signature.depthSeparation() * 3.0D);
        for (int i = 0; i < bands; i++) {
            double decay = (bands - i) / (double) bands;
            int alphaColor = color(trail * decay * (0.034D + signature.temporalSlip() * 0.020D), 170, 212 + (int) (signature.coolDistance() * 28), 255);
            int offset = (int) (Math.sin(time * (0.024D + signature.temporalSlip() * 0.018D) + i * 1.7D) * width * (0.026D + signature.depthSeparation() * 0.026D) * trail);
            int top = (int) (height * (0.18D + i * 0.14D));
            graphics.fill(offset - width / 12, top, width + offset + width / 12, top + 2, alphaColor);
        }
    }

    private static void renderDepthSeparation(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        ClientRenderSignature signature = sample.signature();
        double depth = ClientEffectGraph.saturate(sample.dissociation() * 0.42D + sample.depthBreathing() * 0.24D + signature.depthSeparation() * 0.34D);
        if (depth <= 0.01D) {
            return;
        }

        float time = sample.time();
        for (int i = 0; i < 4; i++) {
            double layer = (i + 1) / 4.0D;
            int y = (int) (height * (0.24D + layer * 0.13D) + Math.sin(time * 0.018D + i * 1.4D) * height * 0.018D * depth);
            int offset = (int) (Math.cos(time * 0.016D + i * 0.8D) * width * 0.030D * depth);
            int c = color(depth * (0.018D + layer * 0.014D), 96, 142 + i * 18, 255);
            graphics.fill(offset, y, width + offset, y + 1 + i % 2, c);
            graphics.fill(-offset, y + 5, width - offset, y + 6 + i % 2, color(depth * 0.018D, 255, 130, 220));
        }
    }

    private static void renderPatternField(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        ClientRenderSignature signature = sample.signature();
        double pattern = ClientEffectGraph.saturate(sample.patterning() * 0.88D + sample.egoDissolution() * 0.26D + signature.patternOrder() * 0.12D);
        if (pattern <= 0.01D) {
            return;
        }

        float time = sample.time();
        int cell = Math.max(14, (int) (Math.min(width, height) / (18.0D + signature.edgeSharpness() * 4.0D - signature.organicFlow() * 2.0D)));
        int cols = width / cell + 3;
        int rows = height / cell + 3;
        int maxMarks = 300;
        int marks = 0;
        int safeBottom = Math.max(28, height / 7);
        int centerX = width / 2;
        int centerY = height / 2;

        for (int y = -1; y < rows && marks < maxMarks; y++) {
            for (int x = -1; x < cols && marks < maxMarks; x++) {
                double field = Math.sin(x * (1.58D + signature.edgeSharpness() * 0.36D) + time * (0.021D + signature.temporalSlip() * 0.012D))
                        + Math.cos(y * (1.25D + signature.organicFlow() * 0.22D) - time * 0.017D)
                        + Math.sin((x + y) * 0.83D + time * 0.012D);
                if (field < 1.05D - pattern * 0.55D) {
                    continue;
                }

                int px = x * cell + (int) (Math.sin(y * 0.91D + time * (0.014D + signature.motorLooseness() * 0.012D)) * cell * pattern);
                int py = y * cell + (int) (Math.cos(x * 0.77D - time * 0.014D) * cell * pattern);
                if (py > height - safeBottom || nearCenter(px, py, centerX, centerY, width, height)) {
                    continue;
                }

                int len = Math.max(3, (int) (cell * (0.35D + pattern * 0.55D)));
                int alphaColor = color(0.022D + pattern * (0.054D + signature.patternOrder() * 0.020D), 118 + (x * 17 & 63), 220 + (int) (signature.warmth() * 30), 196 + (y * 11 & 47));
                graphics.fill(px, py, px + len, py + 1, alphaColor);
                graphics.fill(px, py, px + 1, py + len, color(0.018D + pattern * 0.044D, 238, 125 + (y * 13 & 63), 255));
                marks++;
            }
        }
    }

    private static void renderFocusInstability(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        ClientRenderSignature signature = sample.signature();
        double sharp = ClientEffectGraph.saturate(sample.stimulation() * 0.42D + sample.motorNoise() * 0.34D + signature.edgeSharpness() * 0.40D);
        if (sharp <= 0.015D) {
            return;
        }

        float time = sample.time();
        int centerX = width / 2;
        int centerY = height / 2;
        for (int i = 0; i < 6; i++) {
            double side = i % 2 == 0 ? -1.0D : 1.0D;
            int x = centerX + (int) (side * width * (0.13D + i * 0.018D) + Math.sin(time * 0.18D + i) * width * 0.010D * sharp);
            int y = centerY + (int) (Math.cos(time * 0.22D + i * 1.8D) * height * 0.060D * sharp);
            int len = Math.max(10, (int) (width * (0.018D + sharp * 0.020D)));
            int c = color(sharp * 0.045D, 245, 252, 255);
            int x2 = x + (int) (side * len);
            graphics.fill(Math.min(x, x2), y, Math.max(x, x2), y + 1, c);
        }
    }

    private static void renderDriftVeil(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        ClientRenderSignature signature = sample.signature();
        double drift = ClientEffectGraph.saturate(sample.visualDrift() * 0.76D + sample.geometryMelt() * 0.50D + sample.depthBreathing() * 0.22D + signature.organicFlow() * 0.10D + signature.motorLooseness() * 0.08D);
        if (drift <= 0.01D) {
            return;
        }

        PoseStack pose = graphics.pose();
        float time = sample.time();
        int stripes = 6 + (int) Math.round(signature.organicFlow() * 3.0D);
        for (int i = 0; i < stripes; i++) {
            float phase = time * (0.007F + (float) signature.motorLooseness() * 0.004F + i * 0.001F) + i * 1.19F;
            float x = (float) (Math.sin(phase) * width * (0.058D + signature.motorLooseness() * 0.050D) * drift);
            float y = (float) (height * (i / (double) stripes) + Math.cos(phase * 1.4F) * height * (0.026D + signature.organicFlow() * 0.024D) * drift);
            float angle = (float) (Math.sin(phase * 0.6F) * (0.015D + signature.organicFlow() * 0.018D) * drift);
            int stripeHeight = Math.max(8, (int) (height * (0.030D + drift * 0.019D + signature.narrowing() * 0.018D)));
            int c = color(0.017D + drift * 0.038D, 80 + i * 19 + (int) (signature.warmth() * 24), 170 + i * 7, 255 - i * 11);

            pose.pushPose();
            pose.translate(width / 2.0F + x, y, 0.0F);
            pose.mulPose(Axis.ZP.rotation(angle));
            pose.translate(-width / 2.0F, 0.0F, 0.0F);
            graphics.fill(-width / 5, 0, width + width / 5, stripeHeight, c);
            pose.popPose();
        }
    }

    private static void renderRiskVignette(GuiGraphics graphics, ClientEffectSample sample, int width, int height) {
        if (!ClientExperienceStore.lsd().active() && !ClientExperienceStore.psilocybin().active() && !ClientExperienceStore.dmt().active()) {
            return;
        }
        if ((ClientExperienceStore.psilocybin().active() || ClientExperienceStore.dmt().active()) && !ClientExperienceStore.lsd().active()) {
            return;
        }
        ClientRenderSignature signature = sample.signature();
        double edge = ClientEffectGraph.saturate(sample.anxiety() * 0.65D + sample.risk() * 0.78D + sample.sedation() * 0.24D + signature.narrowing() * 0.34D + signature.instability() * 0.20D);
        if (edge <= 0.01D) {
            return;
        }

        int bandX = Math.max(4, (int) (width * (0.038D + edge * 0.046D + signature.narrowing() * 0.026D)));
        int bandY = Math.max(4, (int) (height * (0.038D + edge * 0.046D + signature.narrowing() * 0.026D)));
        int c = color(0.044D + edge * 0.088D, 28 + (int) (sample.risk() * 70) + (int) (signature.warmth() * 26), 6, 20 + (int) (sample.dissociation() * 60));
        graphics.fill(0, 0, bandX, height, c);
        graphics.fill(width - bandX, 0, width, height, c);
        graphics.fill(0, 0, width, bandY, c);
        graphics.fill(0, height - bandY, width, height, c);
    }

    private static boolean nearCenter(int x, int y, int centerX, int centerY, int width, int height) {
        return Math.abs(x - centerX) < Math.max(18, width / 18) && Math.abs(y - centerY) < Math.max(14, height / 18);
    }

    private static int color(double alpha, int red, int green, int blue) {
        int a = (int) Math.round(ClientEffectGraph.saturate(alpha) * 255.0D);
        return ((a & 0xFF) << 24) | (clampColor(red) << 16) | (clampColor(green) << 8) | clampColor(blue);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double salviaVisualGain(SalviaEffectState salvia) {
        if (!salvia.active()) {
            return 0.0D;
        }

        int otherActives = 0;
        if (ClientExperienceStore.psilocybin().active()) {
            otherActives++;
        }
        if (ClientExperienceStore.lsd().active()) {
            otherActives++;
        }
        if (ClientExperienceStore.dmt().active()) {
            otherActives++;
        }
        if (ClientExperienceStore.hoMet().active()) {
            otherActives++;
        }
        if (ClientExperienceStore.alLad().active()) {
            otherActives++;
        }
        if (ClientExperienceStore.twoCe().active()) {
            otherActives++;
        }

        double gain = 1.0D - otherActives * 0.16D;
        if (ClientExperienceStore.dmt().active()) {
            gain -= 0.06D;
        }
        return Math.max(0.52D, gain);
    }

    private static double ketamineVisualLoad(KetamineEffectState ketamine) {
        if (ketamine == null || !ketamine.active()) {
            return 0.0D;
        }
        double presence = KetamineMasking.visualPresence(ketamine);
        double realityBend = KetamineMasking.realityBend(ketamine);
        double load = Math.log1p(Math.max(0.0D, ketamine.intensity() * (1.08D + presence * 0.78D + realityBend * 0.24D))) / Math.log1p(6.4D);
        load += presence * 0.26D + realityBend * 0.18D;
        load += ClientEffectGraph.saturate((ketamine.depthMismatch() + ketamine.bodyEcho() * 0.60D) / 8.0D) * (0.08D + presence * 0.08D);
        return ClientEffectGraph.saturate(load);
    }

    private static double cannabisVisualLoad(CannabisEffectState cannabis) {
        if (cannabis == null || !cannabis.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D);
        load += ClientEffectGraph.saturate((cannabis.doseScale() - 1.0D) / 19.0D) * 0.08D;
        return ClientEffectGraph.saturate(load);
    }
}
