package com.trippycraft.client;

import com.trippycraft.substance.mdma.MdmaRecoveryTracker.RecoveryState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Locale;

public final class MdmaDepletionScreenOverlay {
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_MARGIN = 8;
    private static final int SCREEN_PADDING = 6;

    private MdmaDepletionScreenOverlay() {
    }

    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        RecoveryState recovery = ClientExperienceStore.mdmaRecovery();
        if (!recovery.active()) {
            return;
        }
        render(event.getGuiGraphics(), screen, recovery);
    }

    private static void render(GuiGraphics graphics, AbstractContainerScreen<?> screen, RecoveryState recovery) {
        Font font = Minecraft.getInstance().font;
        int barWidth = Math.max(120, screen.getXSize() - 18);
        int priorRows = (ClientExperienceStore.alcoholRecovery().active() ? 1 : 0)
                + (ClientExperienceStore.heroinRecovery().active() ? 1 : 0);
        int desiredY = screen.getGuiTop() + screen.getYSize() + BAR_MARGIN + priorRows * (BAR_HEIGHT + 17);
        int x = screen.getGuiLeft() + (screen.getXSize() - barWidth) / 2;
        int y = Math.min(desiredY, graphics.guiHeight() - BAR_HEIGHT - 4);
        x = Math.max(SCREEN_PADDING, Math.min(x, graphics.guiWidth() - barWidth - SCREEN_PADDING));

        int toleranceFill = (int) Math.round((barWidth - 4) * recovery.toleranceProgress());
        int depletionFill = (int) Math.round((barWidth - 4) * recovery.depletionProgress());
        int depletionColor = recovery.withdrawalActive()
                ? 0xFFE66583
                : recovery.hangoverActive()
                ? 0xFFD68BAE
                : depletionColor(recovery.depletionProgress());
        int countdownDays = recovery.resetTicksRemaining() <= 0
                ? 0
                : (int) Math.ceil(recovery.resetTicksRemaining() / 24000.0D);
        String leftLabel = String.format(Locale.ROOT, "MDMA T%d/%d 5-HT %d/%d",
                recovery.tolerancePhase(), recovery.maxTolerancePhase(),
                recovery.depletionPhase(), recovery.maxDepletionPhase());
        String rightLabel = recovery.suppressed()
                ? "DEF"
                : recovery.withdrawalActive()
                ? countdownDays > 0 ? String.format(Locale.ROOT, "WD %dd", countdownDays) : "WD"
                : recovery.hangoverActive()
                ? countdownDays > 0 ? String.format(Locale.ROOT, "HG %dd", countdownDays) : "HG"
                : countdownDays > 0 ? String.format(Locale.ROOT, "%dd", countdownDays) : "clear";
        if (font.width(leftLabel) + font.width(rightLabel) + 8 > barWidth) {
            leftLabel = String.format(Locale.ROOT, "MDMA T%d 5H%d",
                    recovery.tolerancePhase(), recovery.depletionPhase());
        }

        graphics.fill(x - 2, y - 13, x + barWidth + 2, y + BAR_HEIGHT + 2, 0x90000000);
        graphics.fill(x - 1, y - 12, x + barWidth + 1, y + BAR_HEIGHT + 1, 0xFF231121);
        graphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, 0xFF100B15);
        graphics.fill(x + 2, y + 2, x + 2 + toleranceFill, y + BAR_HEIGHT / 2, 0xFF54DDEC);
        graphics.fill(x + 2, y + BAR_HEIGHT / 2, x + 2 + depletionFill, y + BAR_HEIGHT - 2, depletionColor);

        graphics.drawString(font, Component.literal(leftLabel), x + 2, y - 10, 0xFFFFC9F2, false);
        graphics.drawString(font, Component.literal(rightLabel),
                x + barWidth - font.width(rightLabel) - 2, y - 10, 0xFFF4DFF1, false);
    }

    private static int depletionColor(double progress) {
        int red = (int) Math.round(178 + progress * 67.0D);
        int green = (int) Math.round(114 - progress * 38.0D);
        int blue = (int) Math.round(196 - progress * 54.0D);
        return 0xFF000000 | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
