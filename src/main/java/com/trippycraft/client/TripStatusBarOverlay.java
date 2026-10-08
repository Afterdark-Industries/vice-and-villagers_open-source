package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.cannabis.CannabisPhase;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.cocaine.CocainePhase;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.heroin.HeroinPhase;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadPhase;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.twoce.TwoCePhase;
import com.trippycraft.substance.nitrous.NitrousEffectState;
import com.trippycraft.substance.nitrous.NitrousPhase;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.homet.HoMetPhase;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetaminePhase;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.lsd.LsdPhase;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.mdma.MdmaPhase;
import com.trippycraft.substance.mescaline.MescalineEffectState;
import com.trippycraft.substance.mescaline.MescalinePhase;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholPhase;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtPhase;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinPhase;
import com.trippycraft.substance.salvia.SalviaDoseCurve;
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.salvia.SalviaPhase;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import com.trippycraft.substance.twocb.TwoCbPhase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class TripStatusBarOverlay {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 9;
    private static final int MARGIN_TOP = 6;
    private static final double[] PSILOCYBIN_PHASE_BOUNDARIES = {0.10D, 0.30D, 0.65D, 0.85D};
    private static final double[] LSD_PHASE_BOUNDARIES = {0.11D, 0.30D, 0.62D, 0.78D, 0.92D};
    private static final double[] DMT_PHASE_BOUNDARIES = {0.08D, 0.22D, 0.62D, 0.84D};
    private static final double[] CANNABIS_PHASE_BOUNDARIES = {0.14D, 0.34D, 0.70D, 0.90D};
    private static final double[] KETAMINE_PHASE_BOUNDARIES = {0.12D, 0.30D, 0.60D, 0.86D};
    private static final double[] MDMA_PHASE_BOUNDARIES = {0.15D, 0.55D};
    private static final double[] TWO_CB_PHASE_BOUNDARIES = {0.12D, 0.28D, 0.60D, 0.82D};
    private static final double[] MESCALINE_PHASE_BOUNDARIES = {0.20D, 0.39D, 0.62D, 0.74D, 0.92D};
    private static final double[] HO_MET_PHASE_BOUNDARIES = {0.18D, 0.32D, 0.43D, 0.72D, 0.93D};
    private static final double[] AL_LAD_PHASE_BOUNDARIES = {0.12D, 0.28D, 0.52D, 0.78D, 0.92D};
    private static final double[] TWO_CE_PHASE_BOUNDARIES = {0.10D, 0.24D, 0.46D, 0.76D, 0.92D};
    private static final double[] NITROUS_PHASE_BOUNDARIES = {0.08D, 0.22D, 0.62D, 0.86D};
    private static final double[] COCAINE_PHASE_BOUNDARIES = {0.12D, 0.24D, 0.55D, 0.78D, 0.94D};
    private static final double[] HEROIN_PHASE_BOUNDARIES = {0.13D, 0.32D, 0.56D, 0.74D, 0.86D};
    private static final double[] SALVIA_PHASE_BOUNDARIES = {0.16D, 0.36D, 0.70D, 0.88D};
    private static final double[] ALCOHOL_PHASE_BOUNDARIES = {0.12D, 0.32D, 0.66D, 0.82D, 0.94D};

    private TripStatusBarOverlay() {
    }

    public static boolean shouldRender() {
        if (!ClientExperienceStore.active()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return false;
        }
        if (mc.screen instanceof TripScrubScreen) {
            return false;
        }
        return ClientExperienceStore.psilocybin().active()
                || ClientExperienceStore.lsd().active()
                || ClientExperienceStore.dmt().active()
                || ClientExperienceStore.cannabis().active()
                || ClientExperienceStore.mdma().tripActive()
                || ClientExperienceStore.twoCb().active()
                || ClientExperienceStore.mescaline().active()
                || ClientExperienceStore.hoMet().active()
                || ClientExperienceStore.alLad().active()
                || ClientExperienceStore.twoCe().active()
                || ClientExperienceStore.nitrous().active()
            || ClientExperienceStore.ketamine().active()
                || ClientExperienceStore.cocaine().active()
                || ClientExperienceStore.heroin().active()
                || ClientExperienceStore.salvia().active()
                || ClientExperienceStore.alcohol().active();
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        DmtEffectState dmt = ClientExperienceStore.dmt();
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        MdmaEffectState mdma = ClientExperienceStore.mdma();
        TwoCbEffectState twoCb = ClientExperienceStore.twoCb();
        MescalineEffectState mescaline = ClientExperienceStore.mescaline();
        HoMetEffectState hoMet = ClientExperienceStore.hoMet();
        AlLadEffectState alLad = ClientExperienceStore.alLad();
        TwoCeEffectState twoCe = ClientExperienceStore.twoCe();
        NitrousEffectState nitrous = ClientExperienceStore.nitrous();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        CocaineEffectState cocaine = ClientExperienceStore.cocaine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        SalviaEffectState salvia = ClientExperienceStore.salvia();
        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        if (!state.active() && !lsd.active() && !dmt.active() && !cannabis.active() && !mdma.tripActive() && !twoCb.active() && !mescaline.active() && !hoMet.active() && !alLad.active() && !twoCe.active() && !nitrous.active() && !ketamine.active() && !cocaine.active() && !heroin.active() && !salvia.active() && !alcohol.active()) {
            return;
        }
        int width = graphics.guiWidth();
        int x = (width - BAR_WIDTH) / 2;
        int y = MARGIN_TOP;
        PsychedelicBlendState blend = PsychedelicBlendState.from(state, lsd, dmt, hoMet, alLad, twoCe);
        int mixCount = activeCount(state, lsd, dmt, cannabis, mdma, twoCb, ketamine, cocaine, heroin, salvia, alcohol)
                + (mescaline.active() ? 1 : 0) + (hoMet.active() ? 1 : 0) + (alLad.active() ? 1 : 0) + (twoCe.active() ? 1 : 0)
                + (nitrous.active() ? 1 : 0);
        if (mixCount > 1 && (alLad.active() || hoMet.active() || twoCe.active() || nitrous.active())) {
            renderMixedBar(graphics, state, lsd, dmt, cannabis, mdma, twoCb, mescaline, hoMet,
                    ketamine, cocaine, heroin, salvia, alcohol, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (alLad.active() && mixCount == 1) {
            renderBar(graphics, alLad, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (twoCe.active() && mixCount == 1) {
            renderBar(graphics, twoCe, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (nitrous.active() && mixCount == 1) {
            renderBar(graphics, nitrous, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (hoMet.active()) {
            renderBar(graphics, hoMet, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (mescaline.active() && activeCount(state, lsd, dmt, cannabis, mdma, twoCb, ketamine, cocaine, heroin, salvia, alcohol) > 0) {
            renderMixedBar(graphics, state, lsd, dmt, cannabis, mdma, twoCb, mescaline, ketamine, cocaine, heroin, salvia, alcohol, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (twoCb.active() && activeCount(state, lsd, dmt, cannabis, mdma, twoCb, ketamine, cocaine, heroin, salvia, alcohol) > 1) {
            renderMixedBar(graphics, state, lsd, dmt, cannabis, mdma, twoCb, mescaline, ketamine, cocaine, heroin, salvia, alcohol, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if ((salvia.active() || alcohol.active() || cannabis.active() || mdma.tripActive() || ketamine.active() || cocaine.active() || heroin.active())
                && (state.active() || lsd.active() || dmt.active() || cannabis.active() || ketamine.active() || cocaine.active() || heroin.active() || salvia.active() || alcohol.active())
                && activeCount(state, lsd, dmt, cannabis, mdma, twoCb, ketamine, cocaine, heroin, salvia, alcohol) > 1) {
            renderMixedBar(graphics, state, lsd, dmt, cannabis, mdma, twoCb, mescaline, ketamine, cocaine, heroin, salvia, alcohol, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if ((cannabis.active() || ketamine.active() || cocaine.active() || heroin.active()) && (state.active() || lsd.active() || dmt.active())) {
            renderMixedBar(graphics, state, lsd, dmt, cannabis, mdma, twoCb, mescaline, ketamine, cocaine, heroin, salvia, alcohol, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (blend.blended()) {
            renderBlendedBar(graphics, state, lsd, dmt, blend, x, y, BAR_WIDTH, BAR_HEIGHT, false);
            return;
        }
        if (mdma.tripActive()) {
            renderBar(graphics, mdma, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (twoCb.active()) {
            renderBar(graphics, twoCb, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (mescaline.active()) {
            renderBar(graphics, mescaline, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (alLad.active()) {
            renderBar(graphics, alLad, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (nitrous.active()) {
            renderBar(graphics, nitrous, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (ketamine.active()) {
            renderBar(graphics, ketamine, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (cocaine.active()) {
            renderBar(graphics, cocaine, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (heroin.active()) {
            renderBar(graphics, heroin, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (salvia.active()) {
            renderBar(graphics, salvia, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (alcohol.active()
                && (!state.active() || alcohol.intensity() >= state.intensity())
                && (!lsd.active() || alcohol.intensity() >= lsd.intensity())
                && (!dmt.active() || alcohol.intensity() >= dmt.intensity())
                && (!cannabis.active() || alcohol.intensity() >= cannabis.intensity())
                && (!ketamine.active() || alcohol.intensity() >= ketamine.intensity())) {
            renderBar(graphics, alcohol, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (cannabis.active()
                && (!state.active() || cannabis.intensity() >= state.intensity())
                && (!lsd.active() || cannabis.intensity() >= lsd.intensity())
                && (!dmt.active() || cannabis.intensity() >= dmt.intensity())
                && (!ketamine.active() || cannabis.intensity() >= ketamine.intensity())) {
            renderBar(graphics, cannabis, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (ketamine.active()
                && (!state.active() || ketamine.intensity() >= state.intensity())
                && (!lsd.active() || ketamine.intensity() >= lsd.intensity())
                && (!dmt.active() || ketamine.intensity() >= dmt.intensity())) {
            renderBar(graphics, ketamine, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (dmt.active() && (!state.active() || dmt.intensity() >= state.intensity()) && (!lsd.active() || dmt.intensity() >= lsd.intensity())) {
            renderBar(graphics, dmt, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else if (lsd.active() && (!state.active() || lsd.intensity() >= state.intensity())) {
            renderBar(graphics, lsd, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        } else {
            renderBar(graphics, state, x, y, BAR_WIDTH, BAR_HEIGHT, false);
        }
    }

    private static void renderDominantBar(
            GuiGraphics graphics,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            CannabisEffectState cannabis,
            KetamineEffectState ketamine,
            CocaineEffectState cocaine,
            SalviaEffectState salvia,
            AlcoholEffectState alcohol,
            int x,
            int y,
            int width,
            int height
    ) {
        double psilocybinIntensity = psilocybin.active() ? psilocybin.intensity() : -1.0D;
        double lsdIntensity = lsd.active() ? lsd.intensity() : -1.0D;
        double dmtIntensity = dmt.active() ? dmt.intensity() : -1.0D;
        double cannabisIntensity = cannabis.active() ? cannabis.intensity() : -1.0D;
        double ketamineIntensity = ketamine.active() ? ketamine.intensity() : -1.0D;
        double cocaineIntensity = cocaine.active() ? cocaine.intensity() : -1.0D;
        double salviaIntensity = salvia.active() ? salvia.intensity() : -1.0D;
        double alcoholIntensity = alcohol.active() ? alcohol.intensity() : -1.0D;

        if (salviaIntensity >= alcoholIntensity
            && salviaIntensity >= cocaineIntensity
                && salviaIntensity >= ketamineIntensity
                && salviaIntensity >= cannabisIntensity
                && salviaIntensity >= dmtIntensity
                && salviaIntensity >= lsdIntensity
                && salviaIntensity >= psilocybinIntensity) {
            renderBar(graphics, salvia, x, y, width, height, false);
        } else if (ketamineIntensity >= alcoholIntensity
            && ketamineIntensity >= cocaineIntensity
            && ketamineIntensity >= cannabisIntensity
            && ketamineIntensity >= dmtIntensity
            && ketamineIntensity >= lsdIntensity
            && ketamineIntensity >= psilocybinIntensity) {
            renderBar(graphics, ketamine, x, y, width, height, false);
        } else if (cocaineIntensity >= alcoholIntensity
            && cocaineIntensity >= ketamineIntensity
            && cocaineIntensity >= cannabisIntensity
            && cocaineIntensity >= dmtIntensity
            && cocaineIntensity >= lsdIntensity
            && cocaineIntensity >= psilocybinIntensity) {
            renderBar(graphics, cocaine, x, y, width, height, false);
        } else if (alcoholIntensity >= ketamineIntensity
                && alcoholIntensity >= cannabisIntensity
                && alcoholIntensity >= dmtIntensity
                && alcoholIntensity >= lsdIntensity
                && alcoholIntensity >= psilocybinIntensity) {
            renderBar(graphics, alcohol, x, y, width, height, false);
        } else if (cannabisIntensity >= ketamineIntensity
                && cannabisIntensity >= dmtIntensity
                && cannabisIntensity >= lsdIntensity
                && cannabisIntensity >= psilocybinIntensity) {
            renderBar(graphics, cannabis, x, y, width, height, false);
        } else if (ketamineIntensity >= dmtIntensity
                && ketamineIntensity >= lsdIntensity
                && ketamineIntensity >= psilocybinIntensity) {
            renderBar(graphics, ketamine, x, y, width, height, false);
        } else if (dmtIntensity >= lsdIntensity && dmtIntensity >= psilocybinIntensity) {
            renderBar(graphics, dmt, x, y, width, height, false);
        } else if (lsdIntensity >= psilocybinIntensity) {
            renderBar(graphics, lsd, x, y, width, height, false);
        } else {
            renderBar(graphics, psilocybin, x, y, width, height, false);
        }
    }

    public static void renderBar(GuiGraphics graphics, PsilocybinEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseGrams() > state.activeDoseGrams() + 0.05D,
                state.targetDoseGrams() > state.activeDoseGrams() + 0.05D
                        ? String.format(Locale.ROOT, "%.1fg -> %.1fg", state.displayDoseGrams(), state.targetDoseGrams())
                        : String.format(Locale.ROOT, "%.1fg", state.displayDoseGrams()),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::psilocybinPhaseGradient,
                PSILOCYBIN_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, CannabisEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetThcMilligrams() > state.activeThcMilligrams() + 5.0D,
                cannabisDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::cannabisPhaseGradient,
                CANNABIS_PHASE_BOUNDARIES
        );
    }

    public static void renderBlendedBar(
            GuiGraphics graphics,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            PsychedelicBlendState blend,
            int x,
            int y,
            int width,
            int height,
            boolean detailed
    ) {
        if (blend.hoMetActive()) {
            renderHoMetPsychedelicBlend(graphics, psilocybin, lsd, ClientExperienceStore.dmt(), x, y, width, height, detailed);
            return;
        }
        Font font = Minecraft.getInstance().font;

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF202020);
        graphics.fill(x, y, x + width, y + height, 0xFF101018);

        for (int i = 0; i < width; i++) {
            double t = i / (double) width;
            int color = blendedPhaseColor(t, blend.lsdWeight());
            graphics.fill(x + i, y, x + i + 1, y + height, 0x52000000 | (color & 0x00FFFFFF));
        }

        double combinedProgress = blend.combinedProgress(psilocybin, lsd);
        int filled = (int) Math.round(combinedProgress * width);
        for (int i = 0; i < filled; i++) {
            double t = i / (double) width;
            int color = blendedPhaseColor(t, blend.lsdWeight());
            graphics.fill(x + i, y, x + i + 1, y + height, 0xE0000000 | (color & 0x00FFFFFF));
        }

        renderBlendBoundaries(graphics, x, y, width, height, PSILOCYBIN_PHASE_BOUNDARIES, 0x99A7FFBA);
        renderBlendBoundaries(graphics, x, y, width, height, LSD_PHASE_BOUNDARIES, 0x99FF85EA);
        renderCompoundHead(graphics, x, y, width, height, psilocybin.progress(), 0xFF8AFFB4);
        renderCompoundHead(graphics, x, y, width, height, lsd.progress(), 0xFFFF5FE8);

        int headX = x + (int) Math.round(combinedProgress * width);
        graphics.fill(headX - 1, y - 2, headX + 2, y + height + 2, 0xFFFFFFFF);

        String dose = psilocybinDose(psilocybin, true) + " + " + lsdDose(lsd, true);
        String phase = "Psi " + prettyPhase(psilocybin.phase()) + " + LSD " + prettyPhase(lsd.phase());
        int remainingSeconds = Math.max(0, blend.combinedRemainingTicks(psilocybin, lsd) / 20);
        String time = String.format(Locale.ROOT, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60);

        int textColor = 0xFFE8E8E8;
        Component timeComp = Component.literal(time);
        int timeWidth = font.width(timeComp);
        String fittedDose = fitLabelToWidth(font, dose, width - timeWidth - 6);
        graphics.drawString(font, Component.literal(fittedDose), x, y + height + 2, textColor, true);
        graphics.drawString(font, timeComp, x + width - timeWidth, y + height + 2, textColor, true);

        Component phaseComp = Component.literal(phase);
        int phaseWidth = font.width(phaseComp);
        if (phaseWidth > width) {
            phase = "Blend";
            phaseComp = Component.literal(phase);
        }
        graphics.drawString(font, phaseComp, x, y + height + 12, blendedTextColor(blend.lsdWeight()), true);

        if (detailed) {
            String hint = String.format(Locale.ROOT, "blend %.0f%% / %.0f%%",
                    blend.psilocybinWeight() * 100.0D,
                    blend.lsdWeight() * 100.0D);
            graphics.drawString(font, Component.literal(hint), x, y - 11, textColor, true);
            String label = Component.translatable("trippycraft.trip.bar.label").getString();
            graphics.drawString(font, Component.literal(label), x + width / 2 - font.width(label) / 2, y - 22, 0xFFFFFFFF, true);
        }
    }

    public static void renderBlendedBar(
            GuiGraphics graphics,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            PsychedelicBlendState blend,
            int x,
            int y,
            int width,
            int height,
            boolean detailed
    ) {
        if (blend.hoMetActive()) {
            renderHoMetPsychedelicBlend(graphics, psilocybin, lsd, dmt, x, y, width, height, detailed);
            return;
        }
        Font font = Minecraft.getInstance().font;

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF202020);
        graphics.fill(x, y, x + width, y + height, 0xFF101018);

        for (int i = 0; i < width; i++) {
            double t = i / (double) width;
            int color = blendedPhaseColor(t, blend);
            graphics.fill(x + i, y, x + i + 1, y + height, 0x52000000 | (color & 0x00FFFFFF));
        }

        double combinedProgress = blend.combinedProgress(psilocybin, lsd, dmt);
        int filled = (int) Math.round(combinedProgress * width);
        for (int i = 0; i < filled; i++) {
            double t = i / (double) width;
            int color = blendedPhaseColor(t, blend);
            graphics.fill(x + i, y, x + i + 1, y + height, 0xE0000000 | (color & 0x00FFFFFF));
        }

        if (psilocybin.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, PSILOCYBIN_PHASE_BOUNDARIES, 0x99A7FFBA);
            renderCompoundHead(graphics, x, y, width, height, psilocybin.progress(), 0xFF8AFFB4);
        }
        if (lsd.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, LSD_PHASE_BOUNDARIES, 0x99FF85EA);
            renderCompoundHead(graphics, x, y, width, height, lsd.progress(), 0xFFFF5FE8);
        }
        if (dmt.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, DMT_PHASE_BOUNDARIES, 0x99FFE66D);
            renderCompoundHead(graphics, x, y, width, height, dmt.progress(), 0xFFFFF170);
        }

        int headX = x + (int) Math.round(combinedProgress * width);
        graphics.fill(headX - 1, y - 2, headX + 2, y + height + 2, 0xFFFFFFFF);

        String dose = blendedDose(psilocybin, lsd, dmt);
        String phase = blendedPhase(psilocybin, lsd, dmt);
        int remainingSeconds = Math.max(0, blend.combinedRemainingTicks(psilocybin, lsd, dmt) / 20);
        String time = String.format(Locale.ROOT, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60);

        int textColor = 0xFFE8E8E8;
        Component timeComp = Component.literal(time);
        int timeWidth = font.width(timeComp);
        String fittedDose = fitLabelToWidth(font, dose, width - timeWidth - 6);
        graphics.drawString(font, Component.literal(fittedDose), x, y + height + 2, textColor, true);
        graphics.drawString(font, timeComp, x + width - timeWidth, y + height + 2, textColor, true);

        Component phaseComp = Component.literal(phase);
        int phaseWidth = font.width(phaseComp);
        if (phaseWidth > width) {
            phase = "Blend";
            phaseComp = Component.literal(phase);
        }
        graphics.drawString(font, phaseComp, x, y + height + 12, blendedTextColor(blend), true);

        if (detailed) {
            String hint = String.format(Locale.ROOT, "blend %.0f%% / %.0f%% / %.0f%%",
                    blend.psilocybinWeight() * 100.0D,
                    blend.lsdWeight() * 100.0D,
                    blend.dmtWeight() * 100.0D);
            graphics.drawString(font, Component.literal(hint), x, y - 11, textColor, true);
            String label = Component.translatable("trippycraft.trip.bar.label").getString();
            graphics.drawString(font, Component.literal(label), x + width / 2 - font.width(label) / 2, y - 22, 0xFFFFFFFF, true);
        }
    }

    private static void renderHoMetPsychedelicBlend(GuiGraphics graphics, PsilocybinEffectState psilocybin,
            LsdEffectState lsd, DmtEffectState dmt, int x, int y, int width, int height, boolean detailed) {
        renderMixedBar(graphics, psilocybin, lsd, dmt, CannabisEffectState.inactive(), MdmaEffectState.inactive(),
                TwoCbEffectState.inactive(), MescalineEffectState.inactive(), ClientExperienceStore.hoMet(),
                KetamineEffectState.inactive(), CocaineEffectState.inactive(), HeroinEffectState.inactive(),
                SalviaEffectState.inactive(), AlcoholEffectState.inactive(), x, y, width, height, detailed);
    }

    public static void renderMixedBar(
            GuiGraphics graphics,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            CannabisEffectState cannabis,
            MdmaEffectState mdma,
            KetamineEffectState ketamine,
            CocaineEffectState cocaine,
            HeroinEffectState heroin,
            AlcoholEffectState alcohol,
            int x,
            int y,
            int width,
            int height,
            boolean detailed
    ) {
        renderMixedBar(graphics, psilocybin, lsd, dmt, cannabis, mdma, TwoCbEffectState.inactive(),
                MescalineEffectState.inactive(), ketamine, cocaine, heroin, SalviaEffectState.inactive(),
                alcohol, x, y, width, height, detailed);
    }

    public static void renderMixedBar(
            GuiGraphics graphics,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            CannabisEffectState cannabis,
            MdmaEffectState mdma,
            TwoCbEffectState twoCb,
            MescalineEffectState mescaline,
            KetamineEffectState ketamine,
            CocaineEffectState cocaine,
            HeroinEffectState heroin,
            SalviaEffectState salvia,
            AlcoholEffectState alcohol,
            int x,
            int y,
            int width,
            int height,
            boolean detailed
    ) {
        renderMixedBar(graphics, psilocybin, lsd, dmt, cannabis, mdma, twoCb, mescaline,
                ClientExperienceStore.hoMet(), ketamine, cocaine, heroin, salvia, alcohol, x, y, width, height, detailed);
    }

    public static void renderMixedBar(
            GuiGraphics graphics,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            CannabisEffectState cannabis,
            MdmaEffectState mdma,
            TwoCbEffectState twoCb,
            MescalineEffectState mescaline,
            HoMetEffectState hoMet,
            KetamineEffectState ketamine,
            CocaineEffectState cocaine,
            HeroinEffectState heroin,
            SalviaEffectState salvia,
            AlcoholEffectState alcohol,
            int x,
            int y,
            int width,
            int height,
            boolean detailed
    ) {
        Font font = Minecraft.getInstance().font;
        AlLadEffectState alLad = ClientExperienceStore.alLad();
        TwoCeEffectState twoCe = ClientExperienceStore.twoCe();
        NitrousEffectState nitrous = ClientExperienceStore.nitrous();
        double psilocybinWeight = barLoad(psilocybin.active(), psilocybin.intensity(), TrippyConfig.psilocybinMaxVisualIntensity);
        double lsdWeight = barLoad(lsd.active(), lsd.intensity(), TrippyConfig.lsdMaxVisualIntensity);
        double dmtWeight = barLoad(dmt.active(), dmt.intensity(), TrippyConfig.dmtMaxVisualIntensity);
        double cannabisWeight = barLoad(cannabis.active(), cannabis.intensity(), 8.0D);
        double mdmaWeight = barLoad(mdma.tripActive(), mdma.intensity(), 2.2D);
        double twoCbWeight = barLoad(twoCb.active(), twoCb.intensity(), TrippyConfig.twoCbMaxVisualIntensity);
        double mescalineWeight = barLoad(mescaline.active(), mescaline.intensity(), 5.0D);
        double hoMetWeight = barLoad(hoMet.active(), hoMet.visualPresence(), 1.0D);
        double alLadWeight = barLoad(alLad.active(), alLad.visualPresence(), 1.0D);
        double twoCeWeight = barLoad(twoCe.active(), twoCe.visualPresence(), 1.0D);
        double nitrousWeight = barLoad(nitrous.active(), nitrous.visualPresence(), 1.0D);
        double ketamineWeight = barLoad(ketamine.active(), ketamine.intensity(), 8.0D);
        double cocaineWeight = barLoad(cocaine.active(), cocaine.intensity(), 8.0D);
        double heroinWeight = barLoad(heroin.active(), heroin.intensity(), 8.0D);
        double salviaWeight = barLoad(salvia.active(), salvia.intensity(), Math.max(TrippyConfig.salviaMaxVisualIntensity, SalviaDoseCurve.safeVisualIntensity()));
        double alcoholWeight = barLoad(alcohol.active(), alcohol.intensity(), TrippyConfig.alcoholMaxVisualIntensity);
        double totalWeight = psilocybinWeight + lsdWeight + dmtWeight + cannabisWeight + mdmaWeight
                + twoCbWeight + mescalineWeight + hoMetWeight + alLadWeight + twoCeWeight + nitrousWeight + ketamineWeight + cocaineWeight + heroinWeight
                + salviaWeight + alcoholWeight;
        if (totalWeight <= 0.0D) {
            return;
        }
        psilocybinWeight /= totalWeight;
        lsdWeight /= totalWeight;
        dmtWeight /= totalWeight;
        cannabisWeight /= totalWeight;
        mdmaWeight /= totalWeight;
        twoCbWeight /= totalWeight;
        mescalineWeight /= totalWeight;
        hoMetWeight /= totalWeight;
        alLadWeight /= totalWeight;
        twoCeWeight /= totalWeight;
        nitrousWeight /= totalWeight;
        ketamineWeight /= totalWeight;
        cocaineWeight /= totalWeight;
        heroinWeight /= totalWeight;
        salviaWeight /= totalWeight;
        alcoholWeight /= totalWeight;

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF202020);
        graphics.fill(x, y, x + width, y + height, 0xFF101018);

        for (int i = 0; i < width; i++) {
            double t = i / (double) width;
            int color = mixedPhaseColor(t, psilocybinWeight, lsdWeight, dmtWeight, cannabisWeight,
                    mdmaWeight, twoCbWeight, mescalineWeight, hoMetWeight, alLadWeight, twoCeWeight, ketamineWeight, cocaineWeight,
                    heroinWeight, salviaWeight, alcoholWeight);
            graphics.fill(x + i, y, x + i + 1, y + height, 0x52000000 | (color & 0x00FFFFFF));
        }

        double combinedProgress = psilocybin.progress() * psilocybinWeight
                + lsd.progress() * lsdWeight
                + dmt.progress() * dmtWeight
                + cannabis.progress() * cannabisWeight
                + mdma.progress() * mdmaWeight
                + twoCb.progress() * twoCbWeight
                + mescaline.progress() * mescalineWeight
                + hoMet.progress() * hoMetWeight
                + alLad.progress() * alLadWeight
                + twoCe.progress() * twoCeWeight
                + nitrous.progress() * nitrousWeight
                + ketamine.progress() * ketamineWeight
                + cocaine.progress() * cocaineWeight
                + heroin.progress() * heroinWeight
                + salvia.progress() * salviaWeight
                + alcohol.progress() * alcoholWeight;
        combinedProgress = clamp01(combinedProgress);
        int filled = (int) Math.round(combinedProgress * width);
        for (int i = 0; i < filled; i++) {
            double t = i / (double) width;
            int color = mixedPhaseColor(t, psilocybinWeight, lsdWeight, dmtWeight, cannabisWeight,
                    mdmaWeight, twoCbWeight, mescalineWeight, hoMetWeight, alLadWeight, twoCeWeight, ketamineWeight, cocaineWeight,
                    heroinWeight, salviaWeight, alcoholWeight);
            graphics.fill(x + i, y, x + i + 1, y + height, 0xE0000000 | (color & 0x00FFFFFF));
        }

        if (psilocybin.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, PSILOCYBIN_PHASE_BOUNDARIES, 0x99A7FFBA);
            renderCompoundHead(graphics, x, y, width, height, psilocybin.progress(), 0xFF8AFFB4);
        }
        if (lsd.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, LSD_PHASE_BOUNDARIES, 0x99FF85EA);
            renderCompoundHead(graphics, x, y, width, height, lsd.progress(), 0xFFFF5FE8);
        }
        if (dmt.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, DMT_PHASE_BOUNDARIES, 0x99FFE66D);
            renderCompoundHead(graphics, x, y, width, height, dmt.progress(), 0xFFFFF170);
        }
        if (cannabis.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, CANNABIS_PHASE_BOUNDARIES, 0x99A9C86A);
            renderCompoundHead(graphics, x, y, width, height, cannabis.progress(), 0xFFC6F182);
        }
        if (mdma.tripActive()) {
            renderBlendBoundaries(graphics, x, y, width, height, MDMA_PHASE_BOUNDARIES, 0x99FF78D9);
            renderCompoundHead(graphics, x, y, width, height, mdma.progress(), 0xFFFF78D9);
        }
        if (twoCb.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, TWO_CB_PHASE_BOUNDARIES, 0x99FF4FE1);
            renderCompoundHead(graphics, x, y, width, height, twoCb.progress(), 0xFF40F5FF);
        }
        if (mescaline.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, MESCALINE_PHASE_BOUNDARIES, 0x99E2A13A);
            renderCompoundHead(graphics, x, y, width, height, mescaline.progress(), 0xFF18A99D);
        }
        if (hoMet.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, HO_MET_PHASE_BOUNDARIES, 0x99825EFF);
            renderCompoundHead(graphics, x, y, width, height, hoMet.progress(), 0xFF45E8FF);
        }
        if (alLad.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, AL_LAD_PHASE_BOUNDARIES, 0x99FFB347);
            renderCompoundHead(graphics, x, y, width, height, alLad.progress(), 0xFF7C5CFF);
        }
        if (twoCe.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, TWO_CE_PHASE_BOUNDARIES, 0x9914D0EA);
            renderCompoundHead(graphics, x, y, width, height, twoCe.progress(), 0xFF6118C8);
        }
        if (nitrous.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, NITROUS_PHASE_BOUNDARIES, 0x9940E8FF);
            renderCompoundHead(graphics, x, y, width, height, nitrous.progress(), 0xFFB46CFF);
        }
        if (ketamine.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, KETAMINE_PHASE_BOUNDARIES, 0x999EC8F7);
            renderCompoundHead(graphics, x, y, width, height, ketamine.progress(), 0xFFB8DDFF);
        }
        if (cocaine.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, COCAINE_PHASE_BOUNDARIES, 0x99F3D47A);
            renderCompoundHead(graphics, x, y, width, height, cocaine.progress(), 0xFFFFE087);
        }
        if (heroin.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, HEROIN_PHASE_BOUNDARIES, 0x99E8B46E);
            renderCompoundHead(graphics, x, y, width, height, heroin.progress(), 0xFFFFC879);
        }
        if (salvia.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, SALVIA_PHASE_BOUNDARIES, 0x99A6C59E);
            renderCompoundHead(graphics, x, y, width, height, salvia.progress(), 0xFFCBB692);
        }
        if (alcohol.active()) {
            renderBlendBoundaries(graphics, x, y, width, height, ALCOHOL_PHASE_BOUNDARIES, 0x99FFB36A);
            renderCompoundHead(graphics, x, y, width, height, alcohol.progress(), 0xFFFFB36A);
        }

        int headX = x + (int) Math.round(combinedProgress * width);
        graphics.fill(headX - 1, y - 2, headX + 2, y + height + 2, 0xFFFFFFFF);

        String dose = mixedDose(psilocybin, lsd, dmt, cannabis, mdma, twoCb, mescaline,
                hoMet, alLad, twoCe, ketamine, cocaine, heroin, salvia, alcohol);
        String phase = mixedPhase(psilocybin, lsd, dmt, cannabis, mdma, twoCb, mescaline,
                hoMet, alLad, twoCe, ketamine, cocaine, heroin, salvia, alcohol);
        int remainingSeconds = Math.max(0, Math.max(nitrous.active() ? nitrous.remainingTicks() : 0,
                mixedRemainingTicks(psilocybin, lsd, dmt, cannabis,
                mdma, twoCb, mescaline, hoMet, alLad, twoCe, ketamine, cocaine, heroin, salvia, alcohol))) / 20;
        String time = String.format(Locale.ROOT, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60);

        int textColor = 0xFFE8E8E8;
        Component timeComp = Component.literal(time);
        int timeWidth = font.width(timeComp);
        String fittedDose = fitLabelToWidth(font, dose, width - timeWidth - 6);
        graphics.drawString(font, Component.literal(fittedDose), x, y + height + 2, textColor, true);
        graphics.drawString(font, timeComp, x + width - timeWidth, y + height + 2, textColor, true);

        Component phaseComp = Component.literal(phase);
        int phaseWidth = font.width(phaseComp);
        if (phaseWidth > width) {
            phase = "Mixed";
            phaseComp = Component.literal(phase);
        }
        graphics.drawString(font, phaseComp, x, y + height + 12,
                mixedTextColor(psilocybinWeight, lsdWeight, dmtWeight, cannabisWeight, mdmaWeight,
                        twoCbWeight, mescalineWeight, hoMetWeight, alLadWeight, twoCeWeight, ketamineWeight, cocaineWeight, heroinWeight,
                        salviaWeight, alcoholWeight), true);

        if (detailed) {
            String hint = mixedWeightSummary(
                    new String[]{"Psi", "LSD", "DMT", "THC", "MDMA", "2C-B", "Mes", "4-HO-MET", "AL-LAD", "2C-E", "Ket", "Coc", "Opi", "Sal", "Alc"},
                    new double[]{psilocybinWeight, lsdWeight, dmtWeight, cannabisWeight, mdmaWeight, twoCbWeight,
                            mescalineWeight, hoMetWeight, alLadWeight, twoCeWeight, ketamineWeight, cocaineWeight, heroinWeight, salviaWeight, alcoholWeight});
            graphics.drawString(font, Component.literal(fitLabelToWidth(font, hint, width)), x, y - 11, textColor, true);
            String label = Component.translatable("trippycraft.trip.bar.label").getString();
            graphics.drawString(font, Component.literal(label), x + width / 2 - font.width(label) / 2, y - 22, 0xFFFFFFFF, true);
        }
    }

    public static void renderBar(GuiGraphics graphics, LsdEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseMicrograms() > state.activeDoseMicrograms() + 5.0D,
                state.targetDoseMicrograms() > state.activeDoseMicrograms() + 5.0D
                        ? String.format(Locale.ROOT, "%.0fug -> %.0fug", state.displayDoseMicrograms(), state.targetDoseMicrograms())
                        : String.format(Locale.ROOT, "%.0fug", state.displayDoseMicrograms()),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::lsdPhaseGradient,
                LSD_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, DmtEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseUnits() > state.activeDoseUnits() + 0.05D,
                state.targetDoseUnits() > state.activeDoseUnits() + 0.05D
                        ? String.format(Locale.ROOT, "%.1fx -> %.1fx", state.displayDoseUnits(), state.targetDoseUnits())
                        : String.format(Locale.ROOT, "%.1fx", state.displayDoseUnits()),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::dmtPhaseGradient,
                DMT_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, SalviaEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseUnits() > state.activeDoseUnits() + 0.05D,
                salviaDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::salviaPhaseGradient,
                SALVIA_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, AlcoholEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetStandardDrinks() > state.activeStandardDrinks() + 0.05D,
                alcoholDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::alcoholPhaseGradient,
                ALCOHOL_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, KetamineEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D,
                ketamineDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::ketaminePhaseGradient,
                KETAMINE_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, MdmaEffectState state, int x, int y, int width, int height, boolean detailed) {
        String dose = state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D
                ? String.format(Locale.ROOT, "%.0fmg -> %.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams())
                : String.format(Locale.ROOT, "%.0fmg", state.displayDoseMilligrams());
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.tripActive(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D,
                dose,
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::mdmaPhaseGradient,
                MDMA_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, TwoCbEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D,
                twoCbDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::twoCbPhaseGradient,
                TWO_CB_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, MescalineEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderMescalineBar(graphics, state, x, y, width, height, detailed, false);
    }

    public static void renderBar(GuiGraphics graphics, HoMetEffectState state, int x, int y, int width, int height, boolean detailed) {
        boolean stacking = state.targetContentMilligrams() > state.displayContentMilligrams() + 0.25D;
        renderSharedBar(graphics, x, y, width, height, detailed, state.active(), state.progress(), state.ageTicks(),
                state.durationTicks(), state.remainingTicks(), state.intensity(), stacking, hoMetDose(state, false),
                prettyPhase(state.phase()), phaseColor(state.phase()), TripStatusBarOverlay::hoMetPhaseGradient,
                HO_MET_PHASE_BOUNDARIES);
    }

    public static void renderBar(GuiGraphics graphics, TwoCeEffectState state, int x, int y, int width, int height, boolean detailed) {
        boolean stacking = state.targetDoseMilligrams() > state.displayDoseMilligrams() + 0.25D;
        renderSharedBar(graphics, x, y, width, height, detailed, state.active(), state.progress(), state.ageTicks(),
                state.durationTicks(), state.remainingTicks(), state.intensity(), stacking, twoCeDose(state, false),
                prettyPhase(state.phase()), phaseColor(state.phase()), TripStatusBarOverlay::twoCePhaseGradient,
                TWO_CE_PHASE_BOUNDARIES);
    }

    public static void renderBar(GuiGraphics graphics, NitrousEffectState state, int x, int y, int width, int height, boolean detailed) {
        boolean stacking = state.targetHits() > state.displayHits() + 0.25D;
        renderSharedBar(graphics, x, y, width, height, detailed, state.active(), state.progress(), state.ageTicks(),
                state.durationTicks(), state.remainingTicks(), state.intensity(), stacking, nitrousDose(state, false),
                prettyPhase(state.phase()), phaseColor(state.phase()), TripStatusBarOverlay::nitrousPhaseGradient,
                NITROUS_PHASE_BOUNDARIES);
    }

    public static void renderBar(GuiGraphics graphics, AlLadEffectState state, int x, int y, int width, int height, boolean detailed) {
        boolean stacking = state.targetContentMilligrams() > state.displayContentMilligrams() + 0.25D;
        renderSharedBar(graphics, x, y, width, height, detailed, state.active(), state.progress(), state.ageTicks(),
                state.durationTicks(), state.remainingTicks(), state.intensity(), stacking, alLadDose(state, false),
                prettyPhase(state.phase()), phaseColor(state.phase()), TripStatusBarOverlay::alLadPhaseGradient,
                AL_LAD_PHASE_BOUNDARIES);
    }

    private static void renderMescalineBar(GuiGraphics graphics, MescalineEffectState state, int x, int y,
                                           int width, int height, boolean detailed, boolean mixed) {
        boolean stacking = state.targetDoseMilligrams() > state.displayDoseMilligrams() + 1.0D;
        String dose = stacking
                ? String.format(Locale.ROOT, "Mes %.0fmg -> %.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams())
                : String.format(Locale.ROOT, "Mes %.0fmg", state.displayDoseMilligrams());
        if (mixed) dose += " + blend";
        renderSharedBar(graphics, x, y, width, height, detailed, state.active(), state.progress(), state.ageTicks(),
                state.durationTicks(), state.remainingTicks(), state.intensity(), stacking, dose,
                prettyPhase(state.phase()), phaseColor(state.phase()), TripStatusBarOverlay::mescalinePhaseGradient,
                MESCALINE_PHASE_BOUNDARIES);
    }

    public static void renderBar(GuiGraphics graphics, CocaineEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseGrams() > state.activeDoseGrams() + 0.05D,
                cocaineDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::cocainePhaseGradient,
                COCAINE_PHASE_BOUNDARIES
        );
    }

    public static void renderBar(GuiGraphics graphics, HeroinEffectState state, int x, int y, int width, int height, boolean detailed) {
        renderSharedBar(
                graphics,
                x,
                y,
                width,
                height,
                detailed,
                state.active(),
                state.progress(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.intensity(),
                state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D,
                heroinDose(state, false),
                prettyPhase(state.phase()),
                phaseColor(state.phase()),
                TripStatusBarOverlay::heroinPhaseGradient,
                HEROIN_PHASE_BOUNDARIES
        );
    }

    private static void renderSharedBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean detailed,
            boolean active,
            double progressValue,
            int ageTicks,
            int durationTicks,
            int remainingTicks,
            double intensity,
            boolean stacking,
            String dose,
            String phaseLabel,
            int phaseTextColor,
            PhaseGradient gradient,
            double[] phaseBoundaries
    ) {
        Font font = Minecraft.getInstance().font;

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF202020);
        graphics.fill(x, y, x + width, y + height, 0xFF101018);

        for (int i = 0; i < width; i++) {
            double t = i / (double) width;
            int color = gradient.colorAt(t);
            int alpha = active ? 0x55 : 0x30;
            graphics.fill(x + i, y, x + i + 1, y + height, (alpha << 24) | (color & 0x00FFFFFF));
        }

        double progress = clamp01(progressValue);
        int filled = (int) Math.round(progress * width);
        for (int i = 0; i < filled; i++) {
            double t = i / (double) width;
            int color = gradient.colorAt(t);
            graphics.fill(x + i, y, x + i + 1, y + height, 0xE0000000 | (color & 0x00FFFFFF));
        }

        for (double boundary : phaseBoundaries) {
            int bx = x + (int) Math.round(boundary * width);
            graphics.fill(bx, y - 1, bx + 1, y + height + 1, 0xFFFFFFFF);
        }

        if (stacking && durationTicks > 0) {
            int stackTickX = x + (int) Math.round((ageTicks / (double) durationTicks) * width);
            for (int dy = 0; dy < height; dy += 2) {
                graphics.fill(stackTickX - 1, y + dy, stackTickX + 1, y + dy + 1, 0xFFFFD040);
            }
        }

        int headX = x + (int) Math.round(progress * width);
        graphics.fill(headX - 1, y - 2, headX + 2, y + height + 2, 0xFFFFFFFF);

        int remainingSeconds = Math.max(0, remainingTicks / 20);
        String time = String.format(Locale.ROOT, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60);

        int textColor = 0xFFE8E8E8;
        Component timeComp = Component.literal(time);
        int timeWidth = font.width(timeComp);
        String fittedDose = fitLabelToWidth(font, dose, width - timeWidth - 6);
        graphics.drawString(font, Component.literal(fittedDose), x, y + height + 2, textColor, true);
        graphics.drawString(font, timeComp, x + width - timeWidth, y + height + 2, textColor, true);
        Component phaseComp = Component.literal(phaseLabel);
        graphics.drawString(font, phaseComp, x, y + height + 12, phaseTextColor, true);

        if (detailed) {
            String hint = String.format(Locale.ROOT, "intensity %.2f", intensity);
            graphics.drawString(font, Component.literal(hint), x, y - 11, textColor, true);
            String label = Component.translatable("trippycraft.trip.bar.label").getString();
            graphics.drawString(font, Component.literal(label), x + width / 2 - font.width(label) / 2, y - 22, 0xFFFFFFFF, true);
        }
    }

    private static void renderBlendBoundaries(GuiGraphics graphics, int x, int y, int width, int height, double[] boundaries, int color) {
        for (double boundary : boundaries) {
            int bx = x + (int) Math.round(boundary * width);
            graphics.fill(bx, y - 1, bx + 1, y + height + 1, color);
        }
    }

    private static void renderCompoundHead(GuiGraphics graphics, int x, int y, int width, int height, double progressValue, int color) {
        int headX = x + (int) Math.round(clamp01(progressValue) * width);
        graphics.fill(headX, y - 1, headX + 1, y + height + 1, color);
    }

    private static String psilocybinDose(PsilocybinEffectState state, boolean compact) {
        if (state.targetDoseGrams() > state.activeDoseGrams() + 0.05D) {
            return compact
                    ? String.format(Locale.ROOT, "%.1fg>%.1fg", state.displayDoseGrams(), state.targetDoseGrams())
                    : String.format(Locale.ROOT, "%.1fg -> %.1fg", state.displayDoseGrams(), state.targetDoseGrams());
        }
        return String.format(Locale.ROOT, "%.1fg", state.displayDoseGrams());
    }

    private static String cannabisDose(CannabisEffectState state, boolean compact) {
        if (state.targetThcMilligrams() > state.activeThcMilligrams() + 5.0D) {
            return compact
                    ? String.format(Locale.ROOT, "THC %.0f>%.0fmg", state.displayThcMilligrams(), state.targetThcMilligrams())
                    : String.format(Locale.ROOT, "THC %.0fmg -> %.0fmg", state.displayThcMilligrams(), state.targetThcMilligrams());
        }
        return compact
                ? String.format(Locale.ROOT, "THC %.0fmg", state.displayThcMilligrams())
                : String.format(Locale.ROOT, "THC %.0fmg", state.displayThcMilligrams());
    }

    private static String lsdDose(LsdEffectState state, boolean compact) {
        if (state.targetDoseMicrograms() > state.activeDoseMicrograms() + 5.0D) {
            return compact
                    ? String.format(Locale.ROOT, "%.0fug>%.0fug", state.displayDoseMicrograms(), state.targetDoseMicrograms())
                    : String.format(Locale.ROOT, "%.0fug -> %.0fug", state.displayDoseMicrograms(), state.targetDoseMicrograms());
        }
        return String.format(Locale.ROOT, "%.0fug", state.displayDoseMicrograms());
    }

    private static String dmtDose(DmtEffectState state, boolean compact) {
        if (state.targetDoseUnits() > state.activeDoseUnits() + 0.05D) {
            return compact
                    ? String.format(Locale.ROOT, "%.1fx>%.1fx", state.displayDoseUnits(), state.targetDoseUnits())
                    : String.format(Locale.ROOT, "%.1fx -> %.1fx", state.displayDoseUnits(), state.targetDoseUnits());
        }
        return String.format(Locale.ROOT, "%.1fx", state.displayDoseUnits());
    }

    private static String salviaDose(SalviaEffectState state, boolean compact) {
        String displayDose = SalviaDoseCurve.formatPotencyLabel(state.displayDoseUnits());
        String targetDose = SalviaDoseCurve.formatPotencyLabel(state.targetDoseUnits());
        if (state.targetDoseUnits() > state.activeDoseUnits() + 0.05D) {
            return compact
                    ? displayDose + ">" + targetDose
                    : displayDose + " -> " + targetDose;
        }
        return displayDose;
    }

    private static String alcoholDose(AlcoholEffectState state, boolean compact) {
        if (state.targetStandardDrinks() > state.activeStandardDrinks() + 0.05D) {
            return compact
                    ? String.format(Locale.ROOT, "%.2f>%.2f std", state.displayStandardDrinks(), state.targetStandardDrinks())
                    : String.format(Locale.ROOT, "%.2f -> %.2f std", state.displayStandardDrinks(), state.targetStandardDrinks());
        }
        return String.format(Locale.ROOT, "%.2f std", state.displayStandardDrinks());
    }

    private static String cocaineDose(CocaineEffectState state, boolean compact) {
        if (state.targetDoseGrams() > state.activeDoseGrams() + 0.05D) {
            return compact
                    ? String.format(Locale.ROOT, "Coc %.1fg>%.1fg", state.displayDoseGrams(), state.targetDoseGrams())
                    : String.format(Locale.ROOT, "Cocaine %.1fg -> %.1fg", state.displayDoseGrams(), state.targetDoseGrams());
        }
        return compact
                ? String.format(Locale.ROOT, "Coc %.1fg", state.displayDoseGrams())
                : String.format(Locale.ROOT, "Cocaine %.1fg", state.displayDoseGrams());
    }

    private static String heroinDose(HeroinEffectState state, boolean compact) {
        if (state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D) {
            return compact
                    ? String.format(Locale.ROOT, "Opi %.0f>%.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams())
                    : String.format(Locale.ROOT, "Heroin %.0fmg -> %.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams());
        }
        return compact
                ? String.format(Locale.ROOT, "Opi %.0fmg", state.displayDoseMilligrams())
                : String.format(Locale.ROOT, "Heroin %.0fmg", state.displayDoseMilligrams());
    }

    private static String ketamineDose(KetamineEffectState state, boolean compact) {
        if (state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D) {
            return compact
                    ? String.format(Locale.ROOT, "Ket %.0f>%.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams())
                    : String.format(Locale.ROOT, "Ketamine %.0fmg -> %.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams());
        }
        return compact
                ? String.format(Locale.ROOT, "Ket %.0fmg", state.displayDoseMilligrams())
                : String.format(Locale.ROOT, "Ketamine %.0fmg", state.displayDoseMilligrams());
    }

    static String twoCbDose(TwoCbEffectState state, boolean compact) {
        if (state.targetDoseMilligrams() > state.activeDoseMilligrams() + 5.0D) {
            return compact
                    ? String.format(Locale.ROOT, "2C-B %.0f>%.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams())
                    : String.format(Locale.ROOT, "2C-B %.0fmg -> %.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams());
        }
        return String.format(Locale.ROOT, "2C-B %.0fmg", state.displayDoseMilligrams());
    }

    private static String mescalineDose(MescalineEffectState state, boolean compact) {
        if (state.targetDoseMilligrams() > state.displayDoseMilligrams() + 1.0D) {
            return compact
                    ? String.format(Locale.ROOT, "Mes %.0f>%.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams())
                    : String.format(Locale.ROOT, "Mescaline %.0fmg -> %.0fmg", state.displayDoseMilligrams(), state.targetDoseMilligrams());
        }
        return compact
                ? String.format(Locale.ROOT, "Mes %.0fmg", state.displayDoseMilligrams())
                : String.format(Locale.ROOT, "Mescaline %.0fmg", state.displayDoseMilligrams());
    }

    private static String nitrousDose(NitrousEffectState state, boolean compact) {
        if (state.targetHits() > state.displayHits() + 0.25D) {
            return compact
                    ? String.format(Locale.ROOT, "N2O %.0f>%.0f", state.displayHits(), state.targetHits())
                    : String.format(Locale.ROOT, "Nitrous %.0f hit -> %.0f", state.displayHits(), state.targetHits());
        }
        double hits = state.displayHits();
        return compact
                ? String.format(Locale.ROOT, hits == 1.0D ? "N2O" : "N2O x%.0f", hits)
                : (hits == 1.0D ? "Nitrous Oxide" : String.format(Locale.ROOT, "Nitrous x%.0f", hits));
    }

    static String twoCeDose(TwoCeEffectState state, boolean compact) {
        if (state.targetContentMilligrams() > state.displayContentMilligrams() + 0.25D) {
            return compact
                    ? String.format(Locale.ROOT, "2C-E %.0f>%.0fmg", state.displayContentMilligrams(), state.targetContentMilligrams())
                    : String.format(Locale.ROOT, "2C-E %.0fmg -> %.0fmg", state.displayContentMilligrams(), state.targetContentMilligrams());
        }
        return String.format(Locale.ROOT, "2C-E %.0fmg", state.displayContentMilligrams());
    }

    static String alLadDose(AlLadEffectState state, boolean compact) {
        if (state.targetContentMilligrams() > state.displayContentMilligrams() + 0.25D) {
            return compact
                    ? String.format(Locale.ROOT, "AL-LAD %.0f>%.0fmg", state.displayContentMilligrams(), state.targetContentMilligrams())
                    : String.format(Locale.ROOT, "AL-LAD %.0fmg -> %.0fmg", state.displayContentMilligrams(), state.targetContentMilligrams());
        }
        return String.format(Locale.ROOT, "AL-LAD %.0fmg", state.displayContentMilligrams());
    }

    static String hoMetDose(HoMetEffectState state, boolean compact) {
        if (state.targetContentMilligrams() > state.displayContentMilligrams() + 0.25D) {
            return compact
                    ? String.format(Locale.ROOT, "4-HO-MET %.0f>%.0fmg", state.displayContentMilligrams(), state.targetContentMilligrams())
                    : String.format(Locale.ROOT, "4-HO-MET %.0fmg -> %.0fmg", state.displayContentMilligrams(), state.targetContentMilligrams());
        }
        return String.format(Locale.ROOT, "4-HO-MET %.0fmg", state.displayContentMilligrams());
    }

    private static String mixedWeightSummary(String[] labels, double[] weights) {
        StringBuilder summary = new StringBuilder("Mix ");
        for (int i = 0; i < labels.length; i++) {
            if (weights[i] <= 0.0D) continue;
            if (summary.length() > 4) summary.append(" / ");
            summary.append(String.format(Locale.ROOT, "%s %.0f%%", labels[i], weights[i] * 100.0D));
        }
        return summary.toString();
    }

    private static String fitLabelToWidth(Font font, String label, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(label) <= maxWidth) {
            return label;
        }

        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (suffixWidth >= maxWidth) {
            return "";
        }

        int end = label.length();
        while (end > 0 && font.width(label.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? "" : label.substring(0, end) + suffix;
    }

    private static int activeCount(
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            CannabisEffectState cannabis,
            MdmaEffectState mdma,
            TwoCbEffectState twoCb,
            KetamineEffectState ketamine,
            CocaineEffectState cocaine,
            HeroinEffectState heroin,
            SalviaEffectState salvia,
            AlcoholEffectState alcohol
    ) {
        int count = 0;
        if (psilocybin.active()) {
            count++;
        }
        if (lsd.active()) {
            count++;
        }
        if (dmt.active()) {
            count++;
        }
        if (cannabis.active()) {
            count++;
        }
        if (mdma.tripActive()) {
            count++;
        }
        if (twoCb.active()) {
            count++;
        }
        if (ketamine.active()) {
            count++;
        }
        if (cocaine.active()) {
            count++;
        }
        if (heroin.active()) {
            count++;
        }
        if (salvia.active()) {
            count++;
        }
        if (alcohol.active()) {
            count++;
        }
        return count;
    }

    static String mixedDose(PsilocybinEffectState psilocybin, LsdEffectState lsd,
                                    DmtEffectState dmt, CannabisEffectState cannabis,
                                    MdmaEffectState mdma, TwoCbEffectState twoCb,
                                    MescalineEffectState mescaline, HoMetEffectState hoMet,
                                    AlLadEffectState alLad, TwoCeEffectState twoCe, KetamineEffectState ketamine,
                                    CocaineEffectState cocaine, HeroinEffectState heroin,
                                    SalviaEffectState salvia, AlcoholEffectState alcohol) {
        StringBuilder builder = new StringBuilder();
        if (psilocybin.active()) {
            builder.append(psilocybinDose(psilocybin, true));
        }
        if (lsd.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(lsdDose(lsd, true));
        }
        if (dmt.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("DMT ").append(dmtDose(dmt, true));
        }
        if (cannabis.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(cannabisDose(cannabis, true));
        }
        if (mdma.tripActive()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(String.format(Locale.ROOT, "MDMA %.0fmg", mdma.displayDoseMilligrams()));
        }
        if (twoCb.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(twoCbDose(twoCb, true));
        }
        if (mescaline.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(mescalineDose(mescaline, true));
        }
        if (hoMet.active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append(hoMetDose(hoMet, true));
        }
        if (alLad != null && alLad.active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append(alLadDose(alLad, true));
        }
        if (twoCe != null && twoCe.active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append(twoCeDose(twoCe, true));
        }
        if (ClientExperienceStore.nitrous().active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append(nitrousDose(ClientExperienceStore.nitrous(), true));
        }
        if (ketamine.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(ketamineDose(ketamine, true));
        }
        if (cocaine.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(cocaineDose(cocaine, true));
        }
        if (heroin.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(heroinDose(heroin, true));
        }
        if (salvia.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Sal ").append(salviaDose(salvia, true));
        }
        if (alcohol.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Alc ").append(alcoholDose(alcohol, true));
        }
        return builder.isEmpty() ? "Mixed" : builder.toString();
    }

    static String mixedPhase(PsilocybinEffectState psilocybin, LsdEffectState lsd,
                                     DmtEffectState dmt, CannabisEffectState cannabis,
                                     MdmaEffectState mdma, TwoCbEffectState twoCb,
                                     MescalineEffectState mescaline, HoMetEffectState hoMet,
                                     AlLadEffectState alLad, TwoCeEffectState twoCe, KetamineEffectState ketamine,
                                     CocaineEffectState cocaine, HeroinEffectState heroin,
                                     SalviaEffectState salvia, AlcoholEffectState alcohol) {
        StringBuilder builder = new StringBuilder();
        if (psilocybin.active()) {
            builder.append("Psi ").append(prettyPhase(psilocybin.phase()));
        }
        if (lsd.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("LSD ").append(prettyPhase(lsd.phase()));
        }
        if (dmt.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("DMT ").append(prettyPhase(dmt.phase()));
        }
        if (cannabis.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("THC ").append(prettyPhase(cannabis.phase()));
        }
        if (mdma.tripActive()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("MDMA ").append(prettyPhase(mdma.phase()));
        }
        if (twoCb.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("2C-B ").append(prettyPhase(twoCb.phase()));
        }
        if (mescaline.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Mes ").append(prettyPhase(mescaline.phase()));
        }
        if (hoMet.active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append("4-HO-MET ").append(prettyPhase(hoMet.phase()));
        }
        if (alLad != null && alLad.active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append("AL-LAD ").append(prettyPhase(alLad.phase()));
        }
        if (twoCe != null && twoCe.active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append("2C-E ").append(prettyPhase(twoCe.phase()));
        }
        if (ClientExperienceStore.nitrous().active()) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append("Nitrous ").append(prettyPhase(ClientExperienceStore.nitrous().phase()));
        }
        if (ketamine.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Ket ").append(prettyPhase(ketamine.phase()));
        }
        if (cocaine.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Coc ").append(prettyPhase(cocaine.phase()));
        }
        if (heroin.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Opi ").append(prettyPhase(heroin.phase()));
        }
        if (salvia.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Sal ").append(prettyPhase(salvia.phase()));
        }
        if (alcohol.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("Alc ").append(prettyPhase(alcohol.phase()));
        }
        return builder.isEmpty() ? "Mixed" : builder.toString();
    }

    private static int mixedRemainingTicks(PsilocybinEffectState psilocybin, LsdEffectState lsd,
                                           DmtEffectState dmt, CannabisEffectState cannabis,
                                           MdmaEffectState mdma, TwoCbEffectState twoCb,
                                           MescalineEffectState mescaline, HoMetEffectState hoMet,
                                           AlLadEffectState alLad, TwoCeEffectState twoCe, KetamineEffectState ketamine,
                                           CocaineEffectState cocaine, HeroinEffectState heroin,
                                           SalviaEffectState salvia, AlcoholEffectState alcohol) {
        int remaining = 0;
        if (psilocybin.active()) {
            remaining = Math.max(remaining, psilocybin.remainingTicks());
        }
        if (lsd.active()) {
            remaining = Math.max(remaining, lsd.remainingTicks());
        }
        if (dmt.active()) {
            remaining = Math.max(remaining, dmt.remainingTicks());
        }
        if (cannabis.active()) {
            remaining = Math.max(remaining, cannabis.remainingTicks());
        }
        if (mdma.tripActive()) {
            remaining = Math.max(remaining, mdma.remainingTicks());
        }
        if (twoCb.active()) {
            remaining = Math.max(remaining, twoCb.remainingTicks());
        }
        if (mescaline.active()) {
            remaining = Math.max(remaining, mescaline.remainingTicks());
        }
        if (hoMet.active()) {
            remaining = Math.max(remaining, hoMet.remainingTicks());
        }
        if (alLad != null && alLad.active()) {
            remaining = Math.max(remaining, alLad.remainingTicks());
        }
        if (twoCe != null && twoCe.active()) {
            remaining = Math.max(remaining, twoCe.remainingTicks());
        }
        if (ketamine.active()) {
            remaining = Math.max(remaining, ketamine.remainingTicks());
        }
        if (cocaine.active()) {
            remaining = Math.max(remaining, cocaine.remainingTicks());
        }
        if (heroin.active()) {
            remaining = Math.max(remaining, heroin.remainingTicks());
        }
        if (salvia.active()) {
            remaining = Math.max(remaining, salvia.remainingTicks());
        }
        if (alcohol.active()) {
            remaining = Math.max(remaining, alcohol.remainingTicks());
        }
        return remaining;
    }

    private static double barLoad(boolean active, double intensity, double safeMax) {
        if (!active) {
            return 0.0D;
        }
        return Math.max(0.035D, clamp01(intensity / Math.max(1.0D, safeMax)));
    }

    private static String blendedDose(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        StringBuilder builder = new StringBuilder();
        if (psilocybin.active()) {
            builder.append(psilocybinDose(psilocybin, true));
        }
        if (lsd.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(lsdDose(lsd, true));
        }
        if (dmt.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("DMT ").append(dmtDose(dmt, true));
        }
        return builder.isEmpty() ? "Blend" : builder.toString();
    }

    private static String blendedPhase(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        StringBuilder builder = new StringBuilder();
        if (psilocybin.active()) {
            builder.append("Psi ").append(prettyPhase(psilocybin.phase()));
        }
        if (lsd.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("LSD ").append(prettyPhase(lsd.phase()));
        }
        if (dmt.active()) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append("DMT ").append(prettyPhase(dmt.phase()));
        }
        return builder.isEmpty() ? "Blend" : builder.toString();
    }

    private static int psilocybinPhaseGradient(double t) {
        if (t < 0.10D) {
            return lerpColor(0x4060A0, 0x60FFFF, t / 0.10D);
        }
        if (t < 0.30D) {
            return lerpColor(0x60FFFF, 0xFFD040, (t - 0.10D) / 0.20D);
        }
        if (t < 0.65D) {
            return lerpColor(0xFFD040, 0xFF40C0, (t - 0.30D) / 0.35D);
        }
        if (t < 0.85D) {
            return lerpColor(0xFF40C0, 0x9040FF, (t - 0.65D) / 0.20D);
        }
        return lerpColor(0x9040FF, 0xFF8040, (t - 0.85D) / 0.15D);
    }

    private static int lsdPhaseGradient(double t) {
        if (t < 0.11D) {
            return lerpColor(0x1A365D, 0x33F6FF, t / 0.11D);
        }
        if (t < 0.30D) {
            return lerpColor(0x33F6FF, 0xFF8A1D, (t - 0.11D) / 0.19D);
        }
        if (t < 0.62D) {
            return lerpColor(0xFF8A1D, 0xFF29E8, (t - 0.30D) / 0.32D);
        }
        if (t < 0.78D) {
            return lerpColor(0xFF29E8, 0x73FF2B, (t - 0.62D) / 0.16D);
        }
        if (t < 0.92D) {
            return lerpColor(0x73FF2B, 0x885CFF, (t - 0.78D) / 0.14D);
        }
        return lerpColor(0x885CFF, 0xFFD13F, (t - 0.92D) / 0.08D);
    }

    private static int dmtPhaseGradient(double t) {
        if (t < 0.08D) {
            return lerpColor(0x050018, 0x22F8FF, t / 0.08D);
        }
        if (t < 0.22D) {
            return lerpColor(0x22F8FF, 0xFF22D6, (t - 0.08D) / 0.14D);
        }
        if (t < 0.62D) {
            return lerpColor(0xFF22D6, 0xFFF06A, (t - 0.22D) / 0.40D);
        }
        if (t < 0.84D) {
            return lerpColor(0xFFF06A, 0x39FF8F, (t - 0.62D) / 0.22D);
        }
        return lerpColor(0x39FF8F, 0xBDA8FF, (t - 0.84D) / 0.16D);
    }

    private static int cannabisPhaseGradient(double t) {
        if (t < 0.14D) {
            return lerpColor(0x30421F, 0xA4D96C, t / 0.14D);
        }
        if (t < 0.34D) {
            return lerpColor(0xA4D96C, 0xF5D97A, (t - 0.14D) / 0.20D);
        }
        if (t < 0.70D) {
            return lerpColor(0xF5D97A, 0xE3A668, (t - 0.34D) / 0.36D);
        }
        if (t < 0.90D) {
            return lerpColor(0xE3A668, 0x93A070, (t - 0.70D) / 0.20D);
        }
        return lerpColor(0x93A070, 0xC8C18E, (t - 0.90D) / 0.10D);
    }

    private static int ketaminePhaseGradient(double t) {
        if (t < 0.12D) {
            return lerpColor(0x2E3653, 0x7AB7FF, t / 0.12D);
        }
        if (t < 0.30D) {
            return lerpColor(0x7AB7FF, 0xB8DFFF, (t - 0.12D) / 0.18D);
        }
        if (t < 0.60D) {
            return lerpColor(0xB8DFFF, 0xD8C6FF, (t - 0.30D) / 0.30D);
        }
        if (t < 0.86D) {
            return lerpColor(0xD8C6FF, 0x8D96C4, (t - 0.60D) / 0.26D);
        }
        return lerpColor(0x8D96C4, 0xC6CBD8, (t - 0.86D) / 0.14D);
    }

    private static int mdmaPhaseGradient(double t) {
        if (t < 0.15D) {
            return lerpColor(0x5B145C, 0xFF78D9, t / 0.15D);
        }
        if (t < 0.55D) {
            return lerpColor(0xFF78D9, 0x50E9FF, (t - 0.15D) / 0.40D);
        }
        return lerpColor(0x50E9FF, 0xB767C9, (t - 0.55D) / 0.45D);
    }

    private static int twoCbPhaseGradient(double t) {
        if (t < 0.12D) {
            return lerpColor(0x30104F, 0xFF4FE1, t / 0.12D);
        }
        if (t < 0.28D) {
            return lerpColor(0xFF4FE1, 0x40F5FF, (t - 0.12D) / 0.16D);
        }
        if (t < 0.60D) {
            return lerpColor(0x40F5FF, 0xFF4CCB, (t - 0.28D) / 0.32D);
        }
        if (t < 0.82D) {
            return lerpColor(0xFF4CCB, 0x8D6BFF, (t - 0.60D) / 0.22D);
        }
        return lerpColor(0x8D6BFF, 0x4B5879, (t - 0.82D) / 0.18D);
    }

    private static int nitrousPhaseGradient(double t) {
        if (t < 0.08D) return lerpColor(0x1A2A44, 0x7AD7FF, t / 0.08D);
        if (t < 0.22D) return lerpColor(0x7AD7FF, 0x40E8FF, (t - 0.08D) / 0.14D);
        if (t < 0.62D) return lerpColor(0x40E8FF, 0xB46CFF, (t - 0.22D) / 0.40D);
        if (t < 0.86D) return lerpColor(0xB46CFF, 0x6A8CFF, (t - 0.62D) / 0.24D);
        return lerpColor(0x6A8CFF, 0xC9D8FF, (t - 0.86D) / 0.14D);
    }

    private static int twoCePhaseGradient(double t) {
        if (t < 0.10D) return lerpColor(0x0A0C12, 0x14D0EA, t / 0.10D);
        if (t < 0.24D) return lerpColor(0x14D0EA, 0x6118C8, (t - 0.10D) / 0.14D);
        if (t < 0.46D) return lerpColor(0x6118C8, 0xB81428, (t - 0.24D) / 0.22D);
        if (t < 0.76D) return lerpColor(0xB81428, 0xD19E1E, (t - 0.46D) / 0.30D);
        if (t < 0.92D) return lerpColor(0xD19E1E, 0x1A1E28, (t - 0.76D) / 0.16D);
        return lerpColor(0x1A1E28, 0x3A3050, (t - 0.92D) / 0.08D);
    }

    private static int alLadPhaseGradient(double t) {
        if (t < 0.12D) return lerpColor(0x3A1E4A, 0xFFB347, t / 0.12D);
        if (t < 0.28D) return lerpColor(0xFFB347, 0x3ECF8E, (t - 0.12D) / 0.16D);
        if (t < 0.52D) return lerpColor(0x3ECF8E, 0x7C5CFF, (t - 0.28D) / 0.24D);
        if (t < 0.78D) return lerpColor(0x7C5CFF, 0xFF4F8A, (t - 0.52D) / 0.26D);
        if (t < 0.92D) return lerpColor(0xFF4F8A, 0x5C6BFF, (t - 0.78D) / 0.14D);
        return lerpColor(0x5C6BFF, 0xD8B56A, (t - 0.92D) / 0.08D);
    }

    private static int hoMetPhaseGradient(double t) {
        if (t < 0.18D) return lerpColor(0x16415F, 0x45E8FF, t / 0.18D);
        if (t < 0.32D) return lerpColor(0x45E8FF, 0x3C81FF, (t - 0.18D) / 0.14D);
        if (t < 0.43D) return lerpColor(0x3C81FF, 0xCE54FF, (t - 0.32D) / 0.11D);
        if (t < 0.72D) return lerpColor(0xCE54FF, 0xFF58C9, (t - 0.43D) / 0.29D);
        if (t < 0.93D) return lerpColor(0xFF58C9, 0x886CDF, (t - 0.72D) / 0.21D);
        return lerpColor(0x886CDF, 0x82D6D0, (t - 0.93D) / 0.07D);
    }

    private static int mescalinePhaseGradient(double t) {
        if (t < 0.20D) return lerpColor(0x284E2D, 0xB87824, t / 0.20D);
        if (t < 0.39D) return lerpColor(0xB87824, 0x18A99D, (t - 0.20D) / 0.19D);
        if (t < 0.62D) return lerpColor(0x18A99D, 0xF2A331, (t - 0.39D) / 0.23D);
        if (t < 0.74D) return lerpColor(0xF2A331, 0xA62975, (t - 0.62D) / 0.12D);
        if (t < 0.92D) return lerpColor(0xA62975, 0x4D2A78, (t - 0.74D) / 0.18D);
        return lerpColor(0x4D2A78, 0xD8A856, (t - 0.92D) / 0.08D);
    }

    private static int cocainePhaseGradient(double t) {
        if (t < 0.12D) {
            return lerpColor(0x39425C, 0xF2F1C6, t / 0.12D);
        }
        if (t < 0.24D) {
            return lerpColor(0xF2F1C6, 0xFFF3A1, (t - 0.12D) / 0.12D);
        }
        if (t < 0.55D) {
            return lerpColor(0xFFF3A1, 0xFFD45F, (t - 0.24D) / 0.31D);
        }
        if (t < 0.78D) {
            return lerpColor(0xFFD45F, 0xE09A4A, (t - 0.55D) / 0.23D);
        }
        if (t < 0.94D) {
            return lerpColor(0xE09A4A, 0x9E7A69, (t - 0.78D) / 0.16D);
        }
        return lerpColor(0x9E7A69, 0x78808D, (t - 0.94D) / 0.06D);
    }

    private static int heroinPhaseGradient(double t) {
        if (t < 0.13D) {
            return lerpColor(0x2E241A, 0xD6A45E, t / 0.13D);
        }
        if (t < 0.32D) {
            return lerpColor(0xD6A45E, 0xFFD58B, (t - 0.13D) / 0.19D);
        }
        if (t < 0.56D) {
            return lerpColor(0xFFD58B, 0xF0B86A, (t - 0.32D) / 0.24D);
        }
        if (t < 0.74D) {
            return lerpColor(0xF0B86A, 0x8C6C62, (t - 0.56D) / 0.18D);
        }
        if (t < 0.86D) {
            return lerpColor(0x8C6C62, 0xC09169, (t - 0.74D) / 0.12D);
        }
        return lerpColor(0xC09169, 0xD8C4A4, (t - 0.86D) / 0.14D);
    }

    private static int salviaPhaseGradient(double t) {
        if (t < 0.16D) {
            return lerpColor(0x2F3B38, 0x8EA493, t / 0.16D);
        }
        if (t < 0.36D) {
            return lerpColor(0x8EA493, 0xC79D68, (t - 0.16D) / 0.20D);
        }
        if (t < 0.70D) {
            return lerpColor(0xC79D68, 0xD3C2A1, (t - 0.36D) / 0.34D);
        }
        if (t < 0.88D) {
            return lerpColor(0xD3C2A1, 0x72808C, (t - 0.70D) / 0.18D);
        }
        return lerpColor(0x72808C, 0xCBB692, (t - 0.88D) / 0.12D);
    }

    private static int alcoholPhaseGradient(double t) {
        if (t < 0.12D) {
            return lerpColor(0x25314A, 0xC7E5FF, t / 0.12D);
        }
        if (t < 0.32D) {
            return lerpColor(0xC7E5FF, 0xFFD27A, (t - 0.12D) / 0.20D);
        }
        if (t < 0.66D) {
            return lerpColor(0xFFD27A, 0xFF9A45, (t - 0.32D) / 0.34D);
        }
        if (t < 0.82D) {
            return lerpColor(0xFF9A45, 0xB44D5E, (t - 0.66D) / 0.16D);
        }
        if (t < 0.94D) {
            return lerpColor(0xB44D5E, 0x6E7484, (t - 0.82D) / 0.12D);
        }
        return lerpColor(0x6E7484, 0xC7C6B8, (t - 0.94D) / 0.06D);
    }

    private static int blendedPhaseColor(double t, double lsdWeight) {
        return lerpColor(psilocybinPhaseGradient(t), lsdPhaseGradient(t), lsdWeight);
    }

    private static int blendedPhaseColor(double t, PsychedelicBlendState blend) {
        double total = 0.0D;
        int r = 0;
        int g = 0;
        int b = 0;
        if (blend.psilocybinActive()) {
            int color = psilocybinPhaseGradient(t);
            double weight = blend.psilocybinWeight();
            r += (int) Math.round(((color >> 16) & 0xFF) * weight);
            g += (int) Math.round(((color >> 8) & 0xFF) * weight);
            b += (int) Math.round((color & 0xFF) * weight);
            total += weight;
        }
        if (blend.lsdActive()) {
            int color = lsdPhaseGradient(t);
            double weight = blend.lsdWeight();
            r += (int) Math.round(((color >> 16) & 0xFF) * weight);
            g += (int) Math.round(((color >> 8) & 0xFF) * weight);
            b += (int) Math.round((color & 0xFF) * weight);
            total += weight;
        }
        if (blend.dmtActive()) {
            int color = dmtPhaseGradient(t);
            double weight = blend.dmtWeight();
            r += (int) Math.round(((color >> 16) & 0xFF) * weight);
            g += (int) Math.round(((color >> 8) & 0xFF) * weight);
            b += (int) Math.round((color & 0xFF) * weight);
            total += weight;
        }
        if (total <= 0.0D) {
            return psilocybinPhaseGradient(t);
        }
        return (clampInt((int) Math.round(r / total), 0, 255) << 16)
                | (clampInt((int) Math.round(g / total), 0, 255) << 8)
                | clampInt((int) Math.round(b / total), 0, 255);
    }

    private static int mixedPhaseColor(double t, double psilocybinWeight, double lsdWeight,
                                       double dmtWeight, double cannabisWeight, double mdmaWeight,
                                       double twoCbWeight, double mescalineWeight, double hoMetWeight, double alLadWeight, double twoCeWeight, double ketamineWeight,
                                       double cocaineWeight, double heroinWeight, double salviaWeight,
                                       double alcoholWeight) {
        int r = 0;
        int g = 0;
        int b = 0;
        double total = 0.0D;
        if (psilocybinWeight > 0.0D) {
            int color = psilocybinPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * psilocybinWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * psilocybinWeight);
            b += (int) Math.round((color & 0xFF) * psilocybinWeight);
            total += psilocybinWeight;
        }
        if (lsdWeight > 0.0D) {
            int color = lsdPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * lsdWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * lsdWeight);
            b += (int) Math.round((color & 0xFF) * lsdWeight);
            total += lsdWeight;
        }
        if (dmtWeight > 0.0D) {
            int color = dmtPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * dmtWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * dmtWeight);
            b += (int) Math.round((color & 0xFF) * dmtWeight);
            total += dmtWeight;
        }
        if (cannabisWeight > 0.0D) {
            int color = cannabisPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * cannabisWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * cannabisWeight);
            b += (int) Math.round((color & 0xFF) * cannabisWeight);
            total += cannabisWeight;
        }
        if (mdmaWeight > 0.0D) {
            int color = mdmaPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * mdmaWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * mdmaWeight);
            b += (int) Math.round((color & 0xFF) * mdmaWeight);
            total += mdmaWeight;
        }
        if (twoCbWeight > 0.0D) {
            int color = twoCbPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * twoCbWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * twoCbWeight);
            b += (int) Math.round((color & 0xFF) * twoCbWeight);
            total += twoCbWeight;
        }
        if (mescalineWeight > 0.0D) {
            int color = mescalinePhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * mescalineWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * mescalineWeight);
            b += (int) Math.round((color & 0xFF) * mescalineWeight);
            total += mescalineWeight;
        }
        if (hoMetWeight > 0.0D) {
            int color = hoMetPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * hoMetWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * hoMetWeight);
            b += (int) Math.round((color & 0xFF) * hoMetWeight);
            total += hoMetWeight;
        }
        if (alLadWeight > 0.0D) {
            int color = alLadPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * alLadWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * alLadWeight);
            b += (int) Math.round((color & 0xFF) * alLadWeight);
            total += alLadWeight;
        }
        if (twoCeWeight > 0.0D) {
            int color = twoCePhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * twoCeWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * twoCeWeight);
            b += (int) Math.round((color & 0xFF) * twoCeWeight);
            total += twoCeWeight;
        }
        if (ClientExperienceStore.nitrous().active()) {
            double nitrousWeight = barLoad(true, ClientExperienceStore.nitrous().visualPresence(), 1.0D);
            int color = nitrousPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * nitrousWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * nitrousWeight);
            b += (int) Math.round((color & 0xFF) * nitrousWeight);
            total += nitrousWeight;
        }
        if (ketamineWeight > 0.0D) {
            int color = ketaminePhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * ketamineWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * ketamineWeight);
            b += (int) Math.round((color & 0xFF) * ketamineWeight);
            total += ketamineWeight;
        }
        if (cocaineWeight > 0.0D) {
            int color = cocainePhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * cocaineWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * cocaineWeight);
            b += (int) Math.round((color & 0xFF) * cocaineWeight);
            total += cocaineWeight;
        }
        if (heroinWeight > 0.0D) {
            int color = heroinPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * heroinWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * heroinWeight);
            b += (int) Math.round((color & 0xFF) * heroinWeight);
            total += heroinWeight;
        }
        if (salviaWeight > 0.0D) {
            int color = salviaPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * salviaWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * salviaWeight);
            b += (int) Math.round((color & 0xFF) * salviaWeight);
            total += salviaWeight;
        }
        if (alcoholWeight > 0.0D) {
            int color = alcoholPhaseGradient(t);
            r += (int) Math.round(((color >> 16) & 0xFF) * alcoholWeight);
            g += (int) Math.round(((color >> 8) & 0xFF) * alcoholWeight);
            b += (int) Math.round((color & 0xFF) * alcoholWeight);
            total += alcoholWeight;
        }
        if (total <= 0.0D) {
            return alcoholPhaseGradient(t);
        }
        return (clampInt((int) Math.round(r / total), 0, 255) << 16)
                | (clampInt((int) Math.round(g / total), 0, 255) << 8)
                | clampInt((int) Math.round(b / total), 0, 255);
    }

    private static int blendedTextColor(double lsdWeight) {
        return 0xFF000000 | blendedPhaseColor(0.58D, lsdWeight);
    }

    private static int blendedTextColor(PsychedelicBlendState blend) {
        return 0xFF000000 | blendedPhaseColor(0.58D, blend);
    }

    private static int mixedTextColor(double psilocybinWeight, double lsdWeight, double dmtWeight,
                                      double cannabisWeight, double mdmaWeight, double twoCbWeight,
                                      double mescalineWeight, double hoMetWeight, double alLadWeight, double twoCeWeight, double ketamineWeight, double cocaineWeight,
                                      double heroinWeight, double salviaWeight, double alcoholWeight) {
        return 0xFF000000 | mixedPhaseColor(0.58D, psilocybinWeight, lsdWeight, dmtWeight,
                cannabisWeight, mdmaWeight, twoCbWeight, mescalineWeight, hoMetWeight, alLadWeight, twoCeWeight, ketamineWeight,
                cocaineWeight, heroinWeight, salviaWeight, alcoholWeight);
    }

    private static int phaseColor(PsilocybinPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFF60FFFF;
            case COME_UP -> 0xFFFFD040;
            case PEAK -> 0xFFFF40C0;
            case WAVE -> 0xFF9040FF;
            case AFTERGLOW -> 0xFFFF8040;
        };
    }

    private static int phaseColor(LsdPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFF33F6FF;
            case COME_UP -> 0xFFFF8A1D;
            case PEAK -> 0xFFFF29E8;
            case WAVE -> 0xFF73FF2B;
            case COMEDOWN -> 0xFF885CFF;
            case AFTERGLOW -> 0xFFFFD13F;
        };
    }

    private static int phaseColor(DmtPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case RUSH -> 0xFF22F8FF;
            case TUNNEL -> 0xFFFF22D6;
            case BREAKTHROUGH -> 0xFFFFF06A;
            case RETURN -> 0xFF39FF8F;
            case AFTERGLOW -> 0xFFBDA8FF;
        };
    }

    private static int phaseColor(CannabisPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFFA4D96C;
            case LIFT -> 0xFFF5D97A;
            case CRUISE -> 0xFFE3A668;
            case SINK -> 0xFF93A070;
            case AFTERGLOW -> 0xFFC8C18E;
        };
    }

    private static int phaseColor(KetaminePhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFF7AB7FF;
            case FLOAT -> 0xFFB8DFFF;
            case DETACHMENT -> 0xFFD8C6FF;
            case K_HOLE -> 0xFF8E93D8;
            case REENTRY -> 0xFF8D96C4;
            case AFTERMATH -> 0xFFC6CBD8;
        };
    }

    private static int phaseColor(MdmaPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case COME_UP -> 0xFFFF78D9;
            case PEAK -> 0xFF50E9FF;
            case ROLL -> 0xFFC75BFF;
            case COMEDOWN -> 0xFF65708B;
            case HANGOVER -> 0xFF8D8897;
        };
    }

    private static int phaseColor(TwoCbPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFFFF4FE1;
            case LIFT -> 0xFF40F5FF;
            case PEAK -> 0xFFFF4CCB;
            case CRYSTALLINE -> 0xFF9A7BFF;
            case DESCENT -> 0xFF6D7899;
        };
    }

    private static int phaseColor(MescalinePhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case COME_UP -> 0xFFB87824;
            case OPENING -> 0xFF18A99D;
            case PLATEAU -> 0xFFF2A331;
            case DEEP_PEAK -> 0xFFA62975;
            case DESCENT -> 0xFF4D2A78;
            case AFTERGLOW -> 0xFFD8A856;
        };
    }

    private static int phaseColor(NitrousPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case INHALE -> 0xFF7AD7FF;
            case RUSH -> 0xFF40E8FF;
            case PEAK -> 0xFFB46CFF;
            case FADE -> 0xFF6A8CFF;
            case AFTERGLOW -> 0xFFC9D8FF;
        };
    }

    private static int phaseColor(TwoCePhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFF14D0EA;
            case LOCK_IN -> 0xFF6118C8;
            case PEAK -> 0xFFB81428;
            case MACHINE -> 0xFFD19E1E;
            case DESCENT -> 0xFF1A1E28;
            case AFTERGLOW -> 0xFF3A3050;
        };
    }

    private static int phaseColor(AlLadPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFFFFB347;
            case COME_UP -> 0xFF3ECF8E;
            case PEAK -> 0xFF7C5CFF;
            case PLATEAU -> 0xFFFF4F8A;
            case DESCENT -> 0xFF5C6BFF;
            case AFTERGLOW -> 0xFFD8B56A;
        };
    }

    private static int phaseColor(HoMetPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case COME_UP -> 0xFF45E8FF;
            case OPENING -> 0xFF3C81FF;
            case PEAK -> 0xFFCE54FF;
            case PLATEAU -> 0xFFFF58C9;
            case DESCENT -> 0xFF886CDF;
            case AFTERGLOW -> 0xFF82D6D0;
        };
    }

    private static int phaseColor(CocainePhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFFF2F1C6;
            case RUSH -> 0xFFFFF3A1;
            case PEAK -> 0xFFFFD45F;
            case EDGE -> 0xFFE09A4A;
            case CRASH -> 0xFF9E7A69;
            case AFTERGLOW -> 0xFF78808D;
        };
    }

    private static int phaseColor(HeroinPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFFD6A45E;
            case WARMTH -> 0xFFFFD58B;
            case BLISS -> 0xFFF0B86A;
            case NOD -> 0xFF8C6C62;
            case COMEDOWN -> 0xFFC09169;
            case AFTERGLOW -> 0xFFD8C4A4;
        };
    }

    private static int phaseColor(SalviaPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case LOAD_IN -> 0xFF8EA493;
            case PULL -> 0xFFC79D68;
            case DISLOCATION -> 0xFFD3C2A1;
            case RECURSION -> 0xFF72808C;
            case SHAKEOUT -> 0xFFCBB692;
        };
    }

    private static int phaseColor(AlcoholPhase phase) {
        return switch (phase) {
            case SOBER -> 0xFFA0A0A0;
            case ONSET -> 0xFFC7E5FF;
            case BUZZ -> 0xFFFFD27A;
            case INTOXICATED -> 0xFFFF9A45;
            case HEAVY -> 0xFFB44D5E;
            case COMEDOWN -> 0xFF8B91A0;
            case AFTERGLOW -> 0xFFC7C6B8;
        };
    }

    private static String prettyPhase(PsilocybinPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case COME_UP -> "Come-up";
            case PEAK -> "Peak";
            case WAVE -> "Wave";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(LsdPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case COME_UP -> "Come-up";
            case PEAK -> "Peak";
            case WAVE -> "Wave";
            case COMEDOWN -> "Comedown";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(DmtPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case RUSH -> "Rush";
            case TUNNEL -> "Tunnel";
            case BREAKTHROUGH -> "Breakthrough";
            case RETURN -> "Return";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(CannabisPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case LIFT -> "Lift";
            case CRUISE -> "Cruise";
            case SINK -> "Sink";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(KetaminePhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case FLOAT -> "Float";
            case DETACHMENT -> "Detach";
            case K_HOLE -> "K-hole";
            case REENTRY -> "Reentry";
            case AFTERMATH -> "Aftermath";
        };
    }

    private static String prettyPhase(MdmaPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case COME_UP -> "Come-up";
            case PEAK -> "Peak";
            case ROLL -> "Roll";
            case COMEDOWN -> "Comedown";
            case HANGOVER -> "Hangover";
        };
    }

    private static String prettyPhase(TwoCbPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case LIFT -> "Lift";
            case PEAK -> "Peak";
            case CRYSTALLINE -> "Crystalline";
            case DESCENT -> "Descent";
        };
    }

    private static String prettyPhase(MescalinePhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case COME_UP -> "Come-up";
            case OPENING -> "Opening";
            case PLATEAU -> "Plateau";
            case DEEP_PEAK -> "Deep Peak";
            case DESCENT -> "Long Descent";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(NitrousPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case INHALE -> "Inhale";
            case RUSH -> "Rush";
            case PEAK -> "Peak";
            case FADE -> "Fade";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(TwoCePhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case LOCK_IN -> "Lock-in";
            case PEAK -> "Peak";
            case MACHINE -> "Machine";
            case DESCENT -> "Descent";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(AlLadPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case COME_UP -> "Come-up";
            case PEAK -> "Peak";
            case PLATEAU -> "Glass Plateau";
            case DESCENT -> "Descent";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(HoMetPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case COME_UP -> "Come-up";
            case OPENING -> "Opening";
            case PEAK -> "Peak";
            case PLATEAU -> "Plateau";
            case DESCENT -> "Descent";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(CocainePhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case RUSH -> "Rush";
            case PEAK -> "Peak";
            case EDGE -> "Edge";
            case CRASH -> "Crash";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(HeroinPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case WARMTH -> "Warmth";
            case BLISS -> "Bliss";
            case NOD -> "Nod";
            case COMEDOWN -> "Comedown";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static String prettyPhase(SalviaPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case LOAD_IN -> "Load-in";
            case PULL -> "Pull";
            case DISLOCATION -> "Dislocation";
            case RECURSION -> "Recursion";
            case SHAKEOUT -> "Shakeout";
        };
    }

    private static String prettyPhase(AlcoholPhase phase) {
        return switch (phase) {
            case SOBER -> "Sober";
            case ONSET -> "Onset";
            case BUZZ -> "Buzz";
            case INTOXICATED -> "Intoxicated";
            case HEAVY -> "Heavy";
            case COMEDOWN -> "Comedown";
            case AFTERGLOW -> "Afterglow";
        };
    }

    private static int lerpColor(int a, int b, double t) {
        t = clamp01(t);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, v));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    private interface PhaseGradient {
        int colorAt(double t);
    }
}
