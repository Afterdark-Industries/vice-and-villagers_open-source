package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.menu.ChemistryTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ChemistryTableScreen extends FittedContainerScreen<ChemistryTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/gui/container/chemistry_table.png");

    public ChemistryTableScreen(ChemistryTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 202;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int burn = menu.getScaledBurn();
        if (burn > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 67 - burn, 176, 26 - burn, 14, burn, 256, 256);
        }

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 58, topPos + 46, 176, 0, progress, 12, 256, 256);
        }
        if (menu.hasAromaticInput()) {
            renderAromaticButton(guiGraphics, mouseX, mouseY, 0, 8,
                    Component.translatable("gui.trippycraft.chemistry_table.aromatic_mdma"));
            renderAromaticButton(guiGraphics, mouseX, mouseY, 1, 92,
                    Component.translatable("gui.trippycraft.chemistry_table.aromatic_two_cb"));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, (imageWidth - titleWidth) / 2, 10, 0xD6C39A, false);
        if (menu.hasAromaticInput()) {
            Component selected = menu.getSelectedAromaticRecipeLabel();
            guiGraphics.drawString(font, selected, (imageWidth - font.width(selected)) / 2, 104, 0xE7DDBB, false);
        }
    }

    @Override
    protected boolean clickContents(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.hasAromaticInput() && mouseY - topPos >= 84 && mouseY - topPos < 100) {
            double x = mouseX - leftPos;
            int recipe = x >= 8 && x < 84 ? 0 : x >= 92 && x < 168 ? 1 : -1;
            if (recipe >= 0 && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, recipe);
                return true;
            }
        }
        return super.clickContents(mouseX, mouseY, button);
    }

    private void renderAromaticButton(GuiGraphics guiGraphics, int mouseX, int mouseY, int recipe, int x, Component label) {
        boolean selected = menu.getSelectedAromaticRecipeId() == recipe;
        boolean hovered = mouseX - leftPos >= x && mouseX - leftPos < x + 76
                && mouseY - topPos >= 84 && mouseY - topPos < 100;
        guiGraphics.fill(leftPos + x, topPos + 84, leftPos + x + 76, topPos + 100,
                selected ? 0xFFE1C15A : hovered ? 0xFF9A8661 : 0xFF625247);
        guiGraphics.fill(leftPos + x + 1, topPos + 85, leftPos + x + 75, topPos + 99,
                selected ? 0xFF5B2A7A : 0xFF2B2624);
        guiGraphics.drawString(font, label, leftPos + x + (76 - font.width(label)) / 2, topPos + 88,
                selected ? 0xFFF5E8FF : 0xFFD7D0C4, false);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
        if (menu.hasAromaticInput() && mouseY - topPos >= 84 && mouseY - topPos < 100) {
            int x = mouseX - leftPos;
            int recipe = x >= 8 && x < 84 ? 0 : x >= 92 && x < 168 ? 1 : -1;
            if (recipe >= 0) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.trippycraft.chemistry_table.aromatic_selection",
                        menu.getAromaticRecipeLabel(recipe)), mouseX, mouseY);
            }
        }
    }
}
