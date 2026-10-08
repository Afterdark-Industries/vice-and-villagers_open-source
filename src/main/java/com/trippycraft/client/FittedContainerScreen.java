package com.trippycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keeps vanilla inventory interaction and the rendered slots in the same fitted coordinates. */
abstract class FittedContainerScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected FittedContainerScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    final FittedGui viewport() {
        return FittedGui.fit(width, height, imageWidth + 18, imageHeight + 20);
    }

    @Override
    protected void init() {
        super.init();
        FittedGui view = viewport();
        leftPos = (view.width() - imageWidth) / 2;
        topPos = (view.height() - imageHeight) / 2;
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        FittedGui view = viewport();
        int canvasWidth = width, canvasHeight = height;
        try (var drawing = view.begin(graphics)) {
            width = view.width();
            height = view.height();
            renderContents(drawing.graphics(), (int) view.mouseX(mouseX), (int) view.mouseY(mouseY), partialTick);
        } finally {
            width = canvasWidth;
            height = canvasHeight;
        }
    }

    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        FittedGui view = viewport();
        return clickContents(view.mouseX(x), view.mouseY(y), button);
    }

    protected boolean clickContents(double x, double y, int button) {
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        FittedGui view = viewport();
        return super.mouseReleased(view.mouseX(x), view.mouseY(y), button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        FittedGui view = viewport();
        return super.mouseDragged(view.mouseX(x), view.mouseY(y), button, dx / view.scale(), dy / view.scale());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        FittedGui view = viewport();
        return super.mouseScrolled(view.mouseX(x), view.mouseY(y), dx, dy);
    }

    @Override
    public void mouseMoved(double x, double y) {
        FittedGui view = viewport();
        super.mouseMoved(view.mouseX(x), view.mouseY(y));
    }
}
