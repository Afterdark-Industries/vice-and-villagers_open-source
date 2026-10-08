package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.menu.MortarAndPestleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MortarAndPestleScreen extends FittedContainerScreen<MortarAndPestleMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/gui/container/mortar_and_pestle.png");

    public MortarAndPestleScreen(MortarAndPestleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 61, topPos + 37, 176, 0, progress, 10, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, (imageWidth - titleWidth) / 2, 10, 0x2D261F, false);
    }

}
