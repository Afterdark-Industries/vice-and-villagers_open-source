package com.trippycraft.client;

import com.trippycraft.economy.DealerProduct;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.EmeraldWallet;
import com.trippycraft.network.DealerActionPacket;
import com.trippycraft.network.DealerSnapshotPacket;
import com.trippycraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DealerScreen extends Screen {
    private static final int BACKDROP = 0xE50A0707;
    private static final int PANEL = 0xFF201511;
    private static final int PANEL_ALT = 0xFF2B1D17;
    private static final int PANEL_EDGE = 0xFF7B5632;
    private static final int WOOD = 0xFF35231A;
    private static final int WOOD_ACTIVE = 0xFF523623;
    private static final int EMERALD = 0xFF4DDA82;
    private static final int EMERALD_DARK = 0xFF1E8A53;
    private static final int GOLD = 0xFFE0B35A;
    private static final int TEXT_LIGHT = 0xFFF2E2BD;
    private static final int TEXT_MUTED = 0xFFC2A879;
    private static final int TEXT_FADED = 0xFF88725A;
    private static final int DISABLED = 0xFF55493F;
    private static final String ALL = "All";

    private DealerSnapshotPacket snapshot;
    private String selectedCategory = ALL;
    private String search = "";
    private boolean searchFocused;
    private int scroll;
    private int maxScroll;
    private int categoryScroll;
    private ItemStack pendingTooltip = ItemStack.EMPTY;

    public DealerScreen(DealerSnapshotPacket snapshot) {
        super(Component.translatable("container.trippycraft.dealer"));
        this.snapshot = snapshot;
    }

    public void updateSnapshot(DealerSnapshotPacket snapshot) {
        this.snapshot = snapshot;
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
            renderCategories(graphics, layout, mouseX, mouseY);
            renderCatalog(graphics, layout, mouseX, mouseY);
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
        searchFocused = false;
        if (layout.searchBounds().contains(mouseX, mouseY)) {
            searchFocused = true;
            return true;
        }

        Bounds categoryViewport = layout.categoryViewport();
        if (categoryViewport.contains(mouseX, mouseY)) {
            int categoryY = categoryViewport.y - categoryScroll;
            for (String category : categories()) {
                Bounds bounds = new Bounds(categoryViewport.x, categoryY, categoryViewport.w - 4, 18);
                if (bounds.contains(mouseX, mouseY)) {
                    selectedCategory = category;
                    scroll = 0;
                    return true;
                }
                categoryY += 21;
            }
            return true;
        }

        int viewportX = layout.catalogX + 7;
        int viewportY = layout.bodyY + 52;
        int viewportWidth = layout.catalogWidth - 14;
        if (!new Bounds(viewportX, viewportY, viewportWidth, layout.bodyHeight - 60).contains(mouseX, mouseY)) {
            return false;
        }
        int rowY = viewportY - scroll;
        int emeralds = walletValue();
        for (DealerProduct product : filteredProducts()) {
            int unitPrice = price(product);
            int buttonX = viewportX + viewportWidth - 129;
            int maxStack = product.stack(1).getMaxStackSize();
            if (emeralds >= unitPrice && new Bounds(buttonX, rowY + 7, 29, 19).contains(mouseX, mouseY)) {
                buy(product.id(), 1);
                return true;
            }
            if (emeralds >= unitPrice * Math.min(8, maxStack) && new Bounds(buttonX + 34, rowY + 7, 29, 19).contains(mouseX, mouseY)) {
                buy(product.id(), Math.min(8, maxStack));
                return true;
            }
            if (emeralds >= unitPrice * maxStack && new Bounds(buttonX + 68, rowY + 7, 57, 19).contains(mouseX, mouseY)) {
                buy(product.id(), maxStack);
                return true;
            }
            rowY += 36;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        FittedGui viewport = viewport();
        mouseX = viewport.mouseX(mouseX);
        mouseY = viewport.mouseY(mouseY);
        Layout layout = layout();
        if (layout.categoryViewport().contains(mouseX, mouseY)) {
            categoryScroll = Mth.clamp(categoryScroll - (int) Math.round(scrollY * 21.0D), 0, categoryMaxScroll(layout));
            return true;
        }
        if (!new Bounds(layout.catalogX, layout.bodyY, layout.catalogWidth, layout.bodyHeight).contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scroll = Mth.clamp(scroll - (int) Math.round(scrollY * 18.0D), 0, maxScroll);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && !Character.isISOControl(codePoint) && search.length() < 28) {
            search += codePoint;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == 259 && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335 || keyCode == 256) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        graphics.drawString(font, Component.literal("Finished formats only"), layout.panelX + 13, layout.panelY + 22, TEXT_MUTED, false);
        graphics.drawString(font, Component.literal("Open catalog"), layout.panelX + 154, layout.panelY + 16, GOLD, false);
        if (snapshot.socialDiscountPercent() > 0) {
            graphics.drawString(font, Component.literal("MDMA social: -" + snapshot.socialDiscountPercent() + "%"), layout.panelX + 245, layout.panelY + 16, 0xFFFF72CE, false);
        }
        String emeralds = walletValue() + " emeralds";
        graphics.renderItem(new ItemStack(Items.EMERALD), layout.panelX + layout.panelWidth - font.width(emeralds) - 33, layout.panelY + 12);
        graphics.drawString(font, Component.literal(emeralds), layout.panelX + layout.panelWidth - font.width(emeralds) - 13, layout.panelY + 17, EMERALD, false);
    }

    private void renderCategories(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        fillBorder(graphics, layout.categoryX, layout.bodyY, layout.categoryWidth, layout.bodyHeight, 0xFF130D09, PANEL_ALT);
        graphics.drawString(font, Component.literal("Types"), layout.categoryX + 8, layout.bodyY + 9, TEXT_LIGHT, false);
        Bounds viewport = layout.categoryViewport();
        int maximum = categoryMaxScroll(layout);
        categoryScroll = Mth.clamp(categoryScroll, 0, maximum);
        graphics.enableScissor(viewport.x, viewport.y, viewport.right(), viewport.bottom());
        int y = viewport.y - categoryScroll;
        for (String category : categories()) {
            Bounds bounds = new Bounds(viewport.x, y, viewport.w - 4, 18);
            boolean selected = selectedCategory.equals(category);
            boolean hovered = viewport.contains(mouseX, mouseY) && bounds.contains(mouseX, mouseY);
            graphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), selected ? WOOD_ACTIVE : hovered ? 0xFF422D21 : WOOD);
            if (selected) {
                graphics.fill(bounds.x, bounds.y, bounds.x + 2, bounds.bottom(), EMERALD_DARK);
            }
            graphics.drawString(font, Component.literal(fit(category, bounds.w - 8)), bounds.x + 5, bounds.y + 5, selected ? TEXT_LIGHT : TEXT_MUTED, false);
            y += 21;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, viewport.right() - 2, viewport.y, viewport.h, categoryScroll, maximum);
    }

    private int categoryMaxScroll(Layout layout) {
        return Math.max(0, categories().size() * 21 - 3 - layout.categoryViewport().h);
    }

    private void renderCatalog(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        fillBorder(graphics, layout.catalogX, layout.bodyY, layout.catalogWidth, layout.bodyHeight, 0xFF130D09, 0xFF251A14);
        graphics.drawString(font, Component.literal("Dealer Catalog"), layout.catalogX + 9, layout.bodyY + 9, TEXT_LIGHT, false);
        Bounds searchBounds = layout.searchBounds();
        graphics.fill(searchBounds.x, searchBounds.y, searchBounds.right(), searchBounds.bottom(), searchFocused ? 0xFF3B2A20 : 0xFF1B120F);
        graphics.fill(searchBounds.x, searchBounds.bottom() - 1, searchBounds.right(), searchBounds.bottom(), searchFocused ? EMERALD_DARK : 0xFF5E432A);
        String query = search.isEmpty() && !searchFocused ? "Search" : search;
        graphics.drawString(font, Component.literal(fit(query, searchBounds.w - 8)), searchBounds.x + 5, searchBounds.y + 6, search.isEmpty() ? TEXT_FADED : TEXT_LIGHT, false);

        int viewportX = layout.catalogX + 7;
        int viewportY = layout.bodyY + 52;
        int viewportWidth = layout.catalogWidth - 14;
        int viewportHeight = layout.bodyHeight - 60;
        List<DealerProduct> products = filteredProducts();
        int contentHeight = products.size() * 36;
        maxScroll = Math.max(0, contentHeight - viewportHeight + 4);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        int rowY = viewportY - scroll;
        for (DealerProduct product : products) {
            renderProductRow(graphics, product, viewportX, rowY, viewportWidth, mouseX, mouseY);
            rowY += 36;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, viewportX + viewportWidth - 3, viewportY, viewportHeight);
    }

    private void renderProductRow(GuiGraphics graphics, DealerProduct product, int x, int y, int width, int mouseX, int mouseY) {
        int emeralds = walletValue();
        int unitPrice = price(product);
        graphics.fill(x, y, x + width, y + 32, 0xFF312219);
        graphics.fill(x, y, x + 2, y + 32, EMERALD_DARK);

        ItemStack stack = product.stack(1);
        graphics.renderItem(stack, x + 6, y + 8);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x + 6, y + 8, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = stack;
        }
        graphics.drawString(font, Component.literal(fit(stack.getHoverName().getString(), width - 270)), x + 27, y + 6, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal(product.category()), x + 27, y + 18, TEXT_MUTED, false);
        graphics.drawString(font, Component.literal(unitPrice + "e each"), x + width - 200, y + 12, EMERALD, false);

        int buttonX = x + width - 129;
        int maxStack = stack.getMaxStackSize();
        renderButton(graphics, new Bounds(buttonX, y + 7, 29, 19), "1", emeralds >= unitPrice, mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 34, y + 7, 29, 19), "8", emeralds >= unitPrice * Math.min(8, maxStack), mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 68, y + 7, 57, 19), "Stack", emeralds >= unitPrice * maxStack, mouseX, mouseY);
    }

    private List<DealerProduct> filteredProducts() {
        String query = search.trim().toLowerCase(Locale.ROOT);
        return DealerRegistries.products().stream()
                .filter(product -> selectedCategory.equals(ALL) || product.category().equals(selectedCategory))
                .filter(product -> query.isEmpty() || product.stack(1).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<String> categories() {
        List<String> categories = new ArrayList<>();
        categories.add(ALL);
        categories.addAll(DealerRegistries.categories());
        return categories;
    }

    private void buy(String productId, int amount) {
        ModNetwork.sendDealerAction(new DealerActionPacket(productId, amount));
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

    private int price(DealerProduct product) {
        int discount = snapshot == null ? 0 : snapshot.socialDiscountPercent();
        return Math.max(1, (int) Math.ceil(product.emeraldPricePerUnit() * (100 - discount) / 100.0D));
    }

    private void renderButton(GuiGraphics graphics, Bounds bounds, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && bounds.contains(mouseX, mouseY);
        int fill = !enabled ? 0xFF332A23 : hovered ? 0xFF2B7C50 : 0xFF205F3D;
        graphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), fill);
        graphics.fill(bounds.x, bounds.bottom() - 1, bounds.right(), bounds.bottom(), !enabled ? DISABLED : EMERALD_DARK);
        String fitted = fit(label, bounds.w - 4);
        graphics.drawString(font, Component.literal(fitted), bounds.x + (bounds.w - font.width(fitted)) / 2, bounds.y + 6, enabled ? TEXT_LIGHT : TEXT_FADED, false);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
        renderScrollbar(graphics, x, y, height, scroll, maxScroll);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int position, int maximum) {
        if (maximum <= 0) {
            return;
        }
        graphics.fill(x, y, x + 2, y + height, 0x6033221C);
        int thumbHeight = Math.max(18, height * height / Math.max(height, height + maximum));
        int thumbY = y + (height - thumbHeight) * position / Math.max(1, maximum);
        graphics.fill(x - 1, thumbY, x + 3, thumbY + thumbHeight, EMERALD_DARK);
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
        return FittedGui.fit(width, height, 638, 358);
    }

    private Layout layout() {
        FittedGui viewport = viewport();
        int width = viewport.width();
        int height = viewport.height();
        int panelWidth = Math.min(width - 18, 620);
        int panelHeight = Math.min(height - 20, 338);
        panelWidth = Math.max(panelWidth, 470);
        panelHeight = Math.max(panelHeight, 250);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int bodyY = panelY + 52;
        int bodyHeight = panelHeight - 62;
        int categoryX = panelX + 10;
        int categoryWidth = 112;
        int catalogX = categoryX + categoryWidth + 7;
        int catalogWidth = panelX + panelWidth - catalogX - 10;
        return new Layout(panelX, panelY, panelWidth, panelHeight, bodyY, bodyHeight, categoryX, categoryWidth, catalogX, catalogWidth);
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

    private record Layout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int bodyY,
            int bodyHeight,
            int categoryX,
            int categoryWidth,
            int catalogX,
            int catalogWidth
    ) {
        Bounds categoryViewport() {
            return new Bounds(categoryX + 5, bodyY + 27, categoryWidth - 10, bodyHeight - 35);
        }

        Bounds searchBounds() {
            return new Bounds(catalogX + catalogWidth - 116, bodyY + 5, 107, 21);
        }
    }
}
