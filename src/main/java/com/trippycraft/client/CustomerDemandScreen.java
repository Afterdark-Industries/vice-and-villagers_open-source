package com.trippycraft.client;

import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.EmeraldWallet;
import com.trippycraft.network.CustomerActionPacket;
import com.trippycraft.network.CustomerSnapshotPacket;
import com.trippycraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CustomerDemandScreen extends Screen {
    private static final int BACKDROP = 0xE00B0806;
    private static final int PANEL = 0xFF241911;
    private static final int PANEL_EDGE = 0xFF7B5632;
    private static final int CARD = 0xFF322218;
    private static final int PARCHMENT = 0xFFD3BC85;
    private static final int EMERALD = 0xFF4DDA82;
    private static final int EMERALD_DARK = 0xFF1E8A53;
    private static final int GOLD = 0xFFE0B35A;
    private static final int TEXT_LIGHT = 0xFFF2E2BD;
    private static final int TEXT_MUTED = 0xFFC2A879;
    private static final int TEXT_DARK = 0xFF24170F;
    private static final int TEXT_FADED = 0xFF8E7760;
    private static final int DISABLED = 0xFF55493F;

    private CustomerSnapshotPacket snapshot;
    private boolean settlementRequested;
    private ItemStack pendingTooltip = ItemStack.EMPTY;

    public CustomerDemandScreen(CustomerSnapshotPacket snapshot) {
        super(Component.translatable("container.trippycraft.customer_demand"));
        this.snapshot = snapshot;
    }

    public void updateSnapshot(CustomerSnapshotPacket snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void onClose() {
        requestSettlement();
        super.onClose();
    }

    @Override
    public void removed() {
        requestSettlement();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        FittedGui viewport = viewport();
        try (FittedGui.Drawing drawing = viewport.begin(graphics)) {
            graphics = drawing.graphics();
            mouseX = (int) viewport.mouseX(mouseX);
            mouseY = (int) viewport.mouseY(mouseY);
            pendingTooltip = ItemStack.EMPTY;
            renderBackground(graphics, mouseX, mouseY, partialTick);
            Layout layout = layout();
            renderFrame(graphics, layout);
            renderHeader(graphics, layout);
            renderOffers(graphics, layout, mouseX, mouseY);
            if (!pendingTooltip.isEmpty()) {
                graphics.renderTooltip(font, pendingTooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), BACKDROP);
        for (int i = 0; i < 5; i++) {
            int inset = i * 3;
            graphics.fill(inset, inset, graphics.guiWidth() - inset, graphics.guiHeight() - inset, 0x07000000 + i * 0x02000000);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        FittedGui viewport = viewport();
        mouseX = viewport.mouseX(mouseX);
        mouseY = viewport.mouseY(mouseY);
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Layout layout = layout();
        int y = layout.bodyY + 31;
        for (CustomerSnapshotPacket.OfferInfo offer : snapshot.offers()) {
            ItemStack stack = offerStack(offer);
            boolean canSell = !stack.isEmpty() && inventoryCount(stack.getItem()) > 0;
            Bounds sellButton = new Bounds(layout.panelX + layout.panelWidth - 83, y + 17, 58, 20);
            if (canSell && sellButton.contains(mouseX, mouseY)) {
                ModNetwork.sendCustomerAction(new CustomerActionPacket(snapshot.customerId(), offer.id()));
                return true;
            }
            y += 58;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderFrame(GuiGraphics graphics, Layout layout) {
        fillBorder(graphics, layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight, 0xFF110B08, PANEL);
        graphics.fill(layout.panelX + 2, layout.panelY + 2, layout.panelX + layout.panelWidth - 2, layout.panelY + 4, PANEL_EDGE);
        graphics.fill(layout.panelX + 9, layout.panelY + 43, layout.panelX + layout.panelWidth - 9, layout.panelY + 44, 0xFF5B3E28);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout) {
        graphics.drawString(font, title, layout.panelX + 13, layout.panelY + 10, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal("Local buyers refresh their requests each day."), layout.panelX + 13, layout.panelY + 22, TEXT_MUTED, false);
        String emeralds = walletValue() + " emeralds";
        graphics.renderItem(new ItemStack(Items.EMERALD), layout.panelX + layout.panelWidth - font.width(emeralds) - 33, layout.panelY + 12);
        graphics.drawString(font, Component.literal(emeralds), layout.panelX + layout.panelWidth - font.width(emeralds) - 13, layout.panelY + 17, EMERALD, false);
    }

    private void renderOffers(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        int y = layout.bodyY + 31;
        if (snapshot.offers().isEmpty()) {
            fillBorder(graphics, layout.panelX + 12, y, layout.panelWidth - 24, 52, 0xFF4A3320, PARCHMENT);
            graphics.drawString(font, Component.literal("No customer requests are available yet."), layout.panelX + 22, y + 20, TEXT_DARK, false);
            return;
        }

        for (CustomerSnapshotPacket.OfferInfo offer : snapshot.offers()) {
            renderOffer(graphics, offer, layout, y, mouseX, mouseY);
            y += 58;
        }
    }

    private void renderOffer(GuiGraphics graphics, CustomerSnapshotPacket.OfferInfo offer, Layout layout, int y, int mouseX, int mouseY) {
        int x = layout.panelX + 12;
        int w = layout.panelWidth - 24;
        fillBorder(graphics, x, y, w, 52, 0xFF4A3320, CARD);
        ItemStack stack = offerStack(offer);
        boolean known = !stack.isEmpty();
        int available = known ? inventoryCount(stack.getItem()) : 0;
        boolean canSell = known && available > 0;
        boolean canComplete = available >= offer.quantity();
        graphics.fill(x + 1, y + 1, x + 4, y + 51, canComplete ? EMERALD_DARK : canSell ? 0xFFB8893B : 0xFF7A5435);
        if (known) {
            graphics.renderItem(stack, x + 10, y + 17);
            if (new Bounds(x + 10, y + 17, 16, 16).contains(mouseX, mouseY)) {
                pendingTooltip = stack;
            }
        }

        graphics.drawString(font, Component.literal(offer.requestType()), x + 33, y + 8, GOLD, false);
        String name = known ? stack.getHoverName().getString() : offer.productId();
        graphics.drawString(font, Component.literal(fit(name, w - 250)), x + 33, y + 20, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal("Need " + offer.quantity() + " / Have " + available), x + 33, y + 32, canComplete ? EMERALD : canSell ? GOLD : TEXT_MUTED, false);
        graphics.drawString(font, Component.literal(fit(rewardPreview(offer, available), 78)), x + w - 154, y + 13, EMERALD, false);
        graphics.drawString(font, Component.literal(fit(offer.unitPrice() + "e each, +" + offer.reputationReward() + " rep full", 78)), x + w - 154, y + 26, TEXT_MUTED, false);
        if (offer.habitLevel() >= 25) {
            graphics.drawString(font, Component.literal("Habit " + offer.habitLevel() + "/100"), x + w - 154, y + 38, GOLD, false);
        }
        renderButton(graphics, new Bounds(x + w - 71, y + 17, 58, 20), canComplete ? "Fill" : "Sell", canSell, mouseX, mouseY);
    }

    private ItemStack offerStack(CustomerSnapshotPacket.OfferInfo offer) {
        return DealerRegistries.product(offer.productId())
                .map(product -> product.stack(1))
                .orElse(ItemStack.EMPTY);
    }

    private int inventoryCount(Item item) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        Inventory inventory = minecraft.player.getInventory();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int walletValue() {
        return EmeraldWallet.value(minecraft == null ? null : minecraft.player);
    }

    private void requestSettlement() {
        if (settlementRequested) {
            return;
        }
        settlementRequested = true;
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendCustomerAction(CustomerActionPacket.close(snapshot.customerId()));
        }
    }

    private String rewardPreview(CustomerSnapshotPacket.OfferInfo offer, int available) {
        if (available <= 0) {
            return offer.rewardEmeralds() + "e full";
        }
        int sellable = Math.min(available, offer.quantity());
        if (sellable >= offer.quantity()) {
            return offer.rewardEmeralds() + "e +" + completionTip(offer) + " tip";
        }
        return Math.max(1, offer.unitPrice() * sellable) + "e now";
    }

    private int completionTip(CustomerSnapshotPacket.OfferInfo offer) {
        return Math.max(1, offer.rewardEmeralds() / 8);
    }

    private void renderButton(GuiGraphics graphics, Bounds bounds, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && bounds.contains(mouseX, mouseY);
        int fill = !enabled ? 0xFF332A23 : hovered ? 0xFF2B7C50 : 0xFF205F3D;
        graphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), fill);
        graphics.fill(bounds.x, bounds.bottom() - 1, bounds.right(), bounds.bottom(), enabled ? EMERALD_DARK : DISABLED);
        graphics.drawString(font, Component.literal(label), bounds.x + (bounds.w - font.width(label)) / 2, bounds.y + 6, enabled ? TEXT_LIGHT : TEXT_FADED, false);
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(suffix))) + suffix;
    }

    private void fillBorder(GuiGraphics graphics, int x, int y, int width, int height, int border, int fill) {
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    private FittedGui viewport() {
        return FittedGui.fit(width, height, 578, 350);
    }

    private Layout layout() {
        FittedGui viewport = viewport();
        int width = viewport.width();
        int height = viewport.height();
        int panelWidth = Math.min(width - 18, 560);
        int panelHeight = Math.min(height - 20, 330);
        panelWidth = Math.max(panelWidth, 450);
        panelHeight = Math.max(panelHeight, 270);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        return new Layout(panelX, panelY, panelWidth, panelHeight, panelY + 48);
    }

    private record Bounds(int x, int y, int w, int h) {
        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }

        boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }

    private record Layout(int panelX, int panelY, int panelWidth, int panelHeight, int bodyY) {
    }
}
