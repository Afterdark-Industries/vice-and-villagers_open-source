package com.trippycraft.client;

import com.trippycraft.network.ModNetwork;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class TripScrubScreen extends Screen {
    private static final int BAR_WIDTH = 280;
    private static final int BAR_HEIGHT = 14;

    private TripSlider slider;
    private final Screen parent;

    public TripScrubScreen() {
        this(null);
    }

    public TripScrubScreen(Screen parent) {
        super(Component.translatable("trippycraft.trip.scrub.title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    protected void init() {
        int duration = durationTicks();
        double initial = ageTicks() / (double) duration;

        int centerX = this.width / 2;
        int barX = centerX - BAR_WIDTH / 2;
        int sliderY = this.height / 2;

        this.slider = new TripSlider(barX, sliderY, BAR_WIDTH, 20, initial);
        this.addRenderableWidget(this.slider);

        int btnY = sliderY + 30;
        int btnW = 70;
        this.addRenderableWidget(Button.builder(Component.literal("\u00ab 30s"), b -> nudge(-30 * 20))
                .bounds(centerX - btnW * 2 - 6, btnY, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("\u00ab 5s"), b -> nudge(-5 * 20))
                .bounds(centerX - btnW - 2, btnY, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("5s \u00bb"), b -> nudge(5 * 20))
                .bounds(centerX + 2, btnY, btnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("30s \u00bb"), b -> nudge(30 * 20))
                .bounds(centerX + btnW + 6, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(centerX - 50, btnY + 26, 100, 20).build());
    }

    private void nudge(int deltaTicks) {
        int duration = durationTicks();
        int target = Math.max(0, Math.min(duration - 1, ageTicks() + deltaTicks));
        ModNetwork.sendScrub(target);
        if (slider != null) {
            slider.setProgressFromTicks(target, duration);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        var hoMet = ClientExperienceStore.hoMet();
        var alLad = ClientExperienceStore.alLad();
        var twoCe = ClientExperienceStore.twoCe();
        var nitrous = ClientExperienceStore.nitrous();
        if (!state.active() && !hoMet.active() && !alLad.active() && !twoCe.active() && !nitrous.active()) {
            graphics.drawCenteredString(this.font, Component.translatable("trippycraft.trip.scrub.inactive"),
                    this.width / 2, this.height / 2 - 40, 0xFFFFAAAA);
            return;
        }

        int barX = this.width / 2 - BAR_WIDTH / 2;
        int barY = this.height / 2 - 40;
        LsdEffectState lsd = ClientExperienceStore.lsd();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(state, lsd, ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), alLad, twoCe);
        int mixPartners = (state.active() ? 1 : 0) + (lsd.active() ? 1 : 0) + (ClientExperienceStore.dmt().active() ? 1 : 0)
                + (hoMet.active() ? 1 : 0) + (alLad.active() ? 1 : 0) + (twoCe.active() ? 1 : 0) + (nitrous.active() ? 1 : 0)
                + (ClientExperienceStore.twoCb().active() ? 1 : 0) + (ClientExperienceStore.mescaline().active() ? 1 : 0)
                + (ClientExperienceStore.cannabis().active() ? 1 : 0) + (ClientExperienceStore.mdma().tripActive() ? 1 : 0)
                + (ClientExperienceStore.ketamine().active() ? 1 : 0) + (ClientExperienceStore.cocaine().active() ? 1 : 0)
                + (ClientExperienceStore.heroin().active() ? 1 : 0) + (ClientExperienceStore.salvia().active() ? 1 : 0)
                + (ClientExperienceStore.alcohol().active() ? 1 : 0);
        if (mixPartners > 1 && (alLad.active() || hoMet.active() || twoCe.active() || nitrous.active())) {
            TripStatusBarOverlay.renderMixedBar(graphics, state, lsd, ClientExperienceStore.dmt(),
                    ClientExperienceStore.cannabis(), ClientExperienceStore.mdma(), ClientExperienceStore.twoCb(),
                    ClientExperienceStore.mescaline(), hoMet, ClientExperienceStore.ketamine(), ClientExperienceStore.cocaine(),
                    ClientExperienceStore.heroin(), ClientExperienceStore.salvia(), ClientExperienceStore.alcohol(),
                    barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        } else if (alLad.active()) {
            TripStatusBarOverlay.renderBar(graphics, alLad, barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        } else if (twoCe.active()) {
            TripStatusBarOverlay.renderBar(graphics, twoCe, barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        } else if (nitrous.active()) {
            TripStatusBarOverlay.renderBar(graphics, nitrous, barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        } else if (hoMet.active()) {
            TripStatusBarOverlay.renderBar(graphics, hoMet, barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        } else if (blend.blended()) {
            TripStatusBarOverlay.renderBlendedBar(graphics, state, lsd, blend, barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        } else {
            TripStatusBarOverlay.renderBar(graphics, state, barX, barY, BAR_WIDTH, BAR_HEIGHT, true);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class TripSlider extends AbstractSliderButton {
        TripSlider(int x, int y, int width, int height, double initial) {
            super(x, y, width, height, Component.empty(), clamp01(initial));
            this.updateMessage();
        }

        void setProgressFromTicks(int ageTicks, int durationTicks) {
            this.value = clamp01(ageTicks / (double) Math.max(1, durationTicks));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int duration = durationTicks();
            int targetTicks = (int) Math.round(this.value * duration);
            int targetSeconds = targetTicks / 20;
            int totalSeconds = duration / 20;
            this.setMessage(Component.literal(String.format(Locale.ROOT, "%d:%02d / %d:%02d (%.0f%%)",
                    targetSeconds / 60, targetSeconds % 60,
                    totalSeconds / 60, totalSeconds % 60,
                    this.value * 100.0D)));
        }

        @Override
        protected void applyValue() {
            int duration = durationTicks();
            int targetTicks = (int) Math.round(this.value * duration);
            ModNetwork.sendScrub(targetTicks);
        }
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, v));
    }

    // Same priority as PsychoactiveExperience.scrubPsilocybin, preserving the
    // existing packet and timeline editor while allowing a solo 4-HO-MET trip.
    private static int durationTicks() {
        if (ClientExperienceStore.psilocybin().active()) {
            return Math.max(1, ClientExperienceStore.psilocybin().durationTicks());
        }
        if (ClientExperienceStore.hoMet().active()) {
            return Math.max(1, ClientExperienceStore.hoMet().durationTicks());
        }
        if (ClientExperienceStore.alLad().active()) {
            return Math.max(1, ClientExperienceStore.alLad().durationTicks());
        }
        if (ClientExperienceStore.twoCe().active()) {
            return Math.max(1, ClientExperienceStore.twoCe().durationTicks());
        }
        return Math.max(1, ClientExperienceStore.nitrous().durationTicks());
    }

    private static int ageTicks() {
        if (ClientExperienceStore.psilocybin().active()) {
            return ClientExperienceStore.psilocybin().ageTicks();
        }
        if (ClientExperienceStore.hoMet().active()) {
            return ClientExperienceStore.hoMet().ageTicks();
        }
        if (ClientExperienceStore.alLad().active()) {
            return ClientExperienceStore.alLad().ageTicks();
        }
        if (ClientExperienceStore.twoCe().active()) {
            return ClientExperienceStore.twoCe().ageTicks();
        }
        return ClientExperienceStore.nitrous().ageTicks();
    }
}
