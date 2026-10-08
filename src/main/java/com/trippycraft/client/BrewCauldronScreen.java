package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.menu.BrewCauldronMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class BrewCauldronScreen extends FittedContainerScreen<BrewCauldronMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/gui/container/brew_cauldron.png");

    public BrewCauldronScreen(BrewCauldronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 336;
        imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 336, 256);

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            guiGraphics.fill(leftPos + 227, topPos + 76, leftPos + 227 + progress, topPos + 80, 0xFF9B8B76);
            guiGraphics.fill(leftPos + 227, topPos + 75, leftPos + 227 + progress, topPos + 76, 0xFFF6E6C7);
        }

        int burn = menu.getScaledBurn();
        if (burn > 0) {
            int flameBottom = topPos + 147;
            guiGraphics.fill(leftPos + 170, flameBottom - burn, leftPos + 174, flameBottom, 0xE0E97818);
            guiGraphics.fill(leftPos + 176, flameBottom - Math.max(1, burn - 2), leftPos + 180, flameBottom, 0xE0F19B1F);
            guiGraphics.fill(leftPos + 182, flameBottom - Math.max(1, burn - 5), leftPos + 186, flameBottom, 0xE0FFD764);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, (imageWidth - titleWidth) / 2, 14, 0x24201C, false);
    }

}
