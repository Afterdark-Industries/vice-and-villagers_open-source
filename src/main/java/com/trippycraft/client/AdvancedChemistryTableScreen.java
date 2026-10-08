package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.menu.AdvancedChemistryTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class AdvancedChemistryTableScreen extends FittedContainerScreen<AdvancedChemistryTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/gui/container/advanced_chemistry_table.png");

    private static final List<DosageButton> DOSAGE_BUTTONS = List.of(
            new DosageButton(0, 14, 18, 28, 16, "25"),
            new DosageButton(1, 46, 18, 28, 16, "100"),
            new DosageButton(2, 78, 18, 28, 16, "250"),
            new DosageButton(3, 110, 18, 28, 16, "500")
    );

    public AdvancedChemistryTableScreen(AdvancedChemistryTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 202;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 62, topPos + 71, 176, 0, progress, 12, 256, 256);
        }

        renderDosageButtons(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, (imageWidth - titleWidth) / 2, 8, 0xE7DDBB, false);
    }

    @Override
    protected boolean clickContents(double mouseX, double mouseY, int button) {
        for (DosageButton dosageButton : DOSAGE_BUTTONS) {
            if (dosageButton.contains(mouseX - leftPos, mouseY - topPos)) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, dosageButton.id());
                }
                return true;
            }
        }
        return super.clickContents(mouseX, mouseY, button);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
        for (DosageButton dosageButton : DOSAGE_BUTTONS) {
            if (dosageButton.contains(mouseX - leftPos, mouseY - topPos)) {
                guiGraphics.renderComponentTooltip(font, List.of(
                        Component.literal(menu.getPreviewOutputSummary(dosageButton.id())),
                        Component.literal(menu.getPreviewRequirementSummary(dosageButton.id()))), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderDosageButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int selectedId = menu.getSelectedProductId();
        for (DosageButton dosageButton : DOSAGE_BUTTONS) {
            boolean hovered = dosageButton.contains(mouseX - leftPos, mouseY - topPos);
            boolean selected = dosageButton.id() == selectedId;
            int outer = selected ? 0xFFE1C15A : hovered ? 0xFF9A8661 : 0xFF625247;
            int inner = selected ? 0xFF5B2A7A : hovered ? 0xFF403631 : 0xFF2B2624;
            guiGraphics.fill(leftPos + dosageButton.x(), topPos + dosageButton.y(),
                    leftPos + dosageButton.x() + dosageButton.width(),
                    topPos + dosageButton.y() + dosageButton.height(),
                    outer);
            guiGraphics.fill(leftPos + dosageButton.x() + 1, topPos + dosageButton.y() + 1,
                    leftPos + dosageButton.x() + dosageButton.width() - 1,
                    topPos + dosageButton.y() + dosageButton.height() - 1,
                    inner);

            Component label = Component.literal(dosageButton.label());
            int textX = leftPos + dosageButton.x() + (dosageButton.width() - font.width(label)) / 2;
            int textY = topPos + dosageButton.y() + 4;
            guiGraphics.drawString(font, label, textX, textY, selected ? 0xFFF5E8FF : 0xFFD7D0C4, false);
        }
    }

    private record DosageButton(int id, int x, int y, int width, int height, String label) {
        private boolean contains(double localX, double localY) {
            return localX >= x && localX < x + width && localY >= y && localY < y + height;
        }
    }
}
