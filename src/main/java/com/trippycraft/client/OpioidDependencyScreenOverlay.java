package com.trippycraft.client;

import com.trippycraft.substance.heroin.HeroinRecoveryTracker.RecoveryState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Locale;

public final class OpioidDependencyScreenOverlay {
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_MARGIN = 8;
    private static final int SCREEN_PADDING = 6;

    private OpioidDependencyScreenOverlay() {
    }

    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        RecoveryState recovery = ClientExperienceStore.heroinRecovery();
        if (!recovery.active()) {
            return;
        }
        render(event.getGuiGraphics(), screen, recovery);
    }

    private static void render(GuiGraphics graphics, AbstractContainerScreen<?> screen, RecoveryState recovery) {
        Font font = Minecraft.getInstance().font;
        int barWidth = Math.max(120, screen.getXSize() - 18);
        int offset = ClientExperienceStore.alcoholRecovery().active() ? BAR_HEIGHT + 17 : 0;
        int desiredY = screen.getGuiTop() + screen.getYSize() + BAR_MARGIN + offset;
        int x = screen.getGuiLeft() + (screen.getXSize() - barWidth) / 2;
        int y = Math.min(desiredY, graphics.guiHeight() - BAR_HEIGHT - 4);
        x = Math.max(SCREEN_PADDING, Math.min(x, graphics.guiWidth() - barWidth - SCREEN_PADDING));

        double tolerance = recovery.toleranceProgress();
        double dependence = recovery.dependenceProgress();
        int toleranceFill = (int) Math.round((barWidth - 4) * tolerance);
        int dependenceFill = (int) Math.round((barWidth - 4) * dependence);
        int color = recovery.withdrawalActive()
                ? 0xFFDA6767
                : recovery.crashActive()
                ? 0xFFE0B56E
                : dependenceColor(Math.max(tolerance, dependence));
        int toleranceColor = 0xFFB98D5B;
        int countdownDays = recovery.resetTicksRemaining() <= 0 ? 0 : (int) Math.ceil(recovery.resetTicksRemaining() / 24000.0D);
        String leftLabel = String.format(Locale.ROOT, "OPI T%d/%d D%d/%d",
                recovery.tolerancePhase(),
                recovery.maxTolerancePhase(),
                recovery.dependencePhase(),
                recovery.maxDependencePhase());
        String rightLabel = recovery.withdrawalActive()
                ? countdownDays > 0 ? String.format(Locale.ROOT, "WD %dd", countdownDays) : "WD"
                : recovery.crashActive()
                ? countdownDays > 0 ? String.format(Locale.ROOT, "CR %dd", countdownDays) : "CR"
                : countdownDays > 0 ? String.format(Locale.ROOT, "%dd", countdownDays) : "clear";

        graphics.fill(x - 2, y - 13, x + barWidth + 2, y + BAR_HEIGHT + 2, 0x90000000);
        graphics.fill(x - 1, y - 12, x + barWidth + 1, y + BAR_HEIGHT + 1, 0xFF211711);
        graphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, 0xFF120D08);
        graphics.fill(x + 2, y + 2, x + 2 + toleranceFill, y + BAR_HEIGHT / 2, toleranceColor);
        graphics.fill(x + 2, y + BAR_HEIGHT / 2, x + 2 + dependenceFill, y + BAR_HEIGHT - 2, color);

        graphics.drawString(font, Component.literal(leftLabel), x + 2, y - 10, 0xFFE9D8BC, false);
        graphics.drawString(font, Component.literal(rightLabel), x + barWidth - font.width(rightLabel) - 2, y - 10, 0xFFF3E7D3, false);
    }

    private static int dependenceColor(double progress) {
        int red = (int) Math.round(132 + progress * 86.0D);
        int green = (int) Math.round(88 + (1.0D - progress) * 78.0D);
        int blue = (int) Math.round(44 + (1.0D - progress) * 54.0D);
        return 0xFF000000 | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
