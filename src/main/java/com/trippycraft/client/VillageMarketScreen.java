package com.trippycraft.client;

import com.trippycraft.economy.BuyableMarketMaterial;
import com.trippycraft.economy.EmeraldWallet;
import com.trippycraft.economy.MarketMath;
import com.trippycraft.economy.MarketRegistries;
import com.trippycraft.economy.SellableMarketProduct;
import com.trippycraft.network.MarketActionPacket;
import com.trippycraft.network.MarketSnapshotPacket;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class VillageMarketScreen extends Screen {
    private static final int BACKDROP = 0xE50B0705;
    private static final int PANEL = 0xFF211712;
    private static final int PANEL_ALT = 0xFF2D2119;
    private static final int PANEL_EDGE = 0xFF806034;
    private static final int PANEL_EDGE_DARK = 0xFF110B08;
    private static final int PARCHMENT = 0xFFD4BD86;
    private static final int PARCHMENT_DARK = 0xFFA88954;
    private static final int WOOD = 0xFF35251B;
    private static final int WOOD_ACTIVE = 0xFF513724;
    private static final int EMERALD = 0xFF49D37D;
    private static final int EMERALD_DARK = 0xFF1E8B53;
    private static final int GOLD = 0xFFE0B35A;
    private static final int TEXT_LIGHT = 0xFFF1E4BF;
    private static final int TEXT_MUTED = 0xFFC5AE82;
    private static final int TEXT_DARK = 0xFF24170F;
    private static final int TEXT_FADED = 0xFF8B7559;
    private static final int DISABLED = 0xFF5B5147;
    private static final String ALL = "All";

    private MarketSnapshotPacket snapshot;
    private MarketTab selectedTab = MarketTab.SELL;
    private String selectedSellCategory = ALL;
    private String selectedMaterialCategory = ALL;
    private String materialSearch = "";
    private boolean materialSearchFocused;
    private int sellScroll;
    private int buyScroll;
    private int contractScroll;
    private int demandScroll;
    private int sellCategoryScroll;
    private int materialCategoryScroll;
    private int maxSellScroll;
    private int maxBuyScroll;
    private int maxContractScroll;
    private int maxDemandScroll;
    private int maxSellCategoryScroll;
    private int maxMaterialCategoryScroll;
    private boolean settlementRequested;
    private ItemStack pendingTooltip = ItemStack.EMPTY;

    public VillageMarketScreen(MarketSnapshotPacket snapshot) {
        super(Component.translatable("container.trippycraft.village_market"));
        this.snapshot = snapshot;
    }

    public void updateSnapshot(MarketSnapshotPacket snapshot) {
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
            renderTabs(graphics, layout, mouseX, mouseY);
            switch (selectedTab) {
                case SELL -> renderSellProducts(graphics, layout, mouseX, mouseY);
                case BUY -> renderBuyMaterials(graphics, layout, mouseX, mouseY);
                case CONTRACTS -> renderContracts(graphics, layout, mouseX, mouseY);
                case REPUTATION -> renderReputation(graphics, layout, mouseX, mouseY);
            }
            if (!pendingTooltip.isEmpty()) {
                graphics.renderTooltip(font, pendingTooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), BACKDROP);
        for (int i = 0; i < 6; i++) {
            int inset = i * 2;
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
        int tabWidth = (layout.panelWidth - 20) / MarketTab.values().length;
        int tabX = layout.panelX + 10;
        for (MarketTab tab : MarketTab.values()) {
            Bounds bounds = new Bounds(tabX, layout.panelY + 32, tabWidth - 3, 19);
            if (bounds.contains(mouseX, mouseY)) {
                selectedTab = tab;
                materialSearchFocused = false;
                return true;
            }
            tabX += tabWidth;
        }

        return switch (selectedTab) {
            case SELL -> clickSellProducts(layout, mouseX, mouseY);
            case BUY -> clickBuyMaterials(layout, mouseX, mouseY);
            case CONTRACTS -> clickContracts(layout, mouseX, mouseY);
            case REPUTATION -> false;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        FittedGui viewport = viewport();
        mouseX = viewport.mouseX(mouseX);
        mouseY = viewport.mouseY(mouseY);
        Layout layout = layout();
        if (!layout.contentBounds().contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int delta = (int) Math.round(scrollY * 18.0D);
        if (railBounds(layout).contains(mouseX, mouseY)) {
            switch (selectedTab) {
                case SELL -> sellCategoryScroll = Mth.clamp(sellCategoryScroll - delta, 0, maxSellCategoryScroll);
                case BUY -> materialCategoryScroll = Mth.clamp(materialCategoryScroll - delta, 0, maxMaterialCategoryScroll);
                default -> {
                }
            }
            return true;
        }
        switch (selectedTab) {
            case SELL -> sellScroll = Mth.clamp(sellScroll - delta, 0, maxSellScroll);
            case BUY -> buyScroll = Mth.clamp(buyScroll - delta, 0, maxBuyScroll);
            case CONTRACTS -> contractScroll = Mth.clamp(contractScroll - delta, 0, maxContractScroll);
            case REPUTATION -> demandScroll = Mth.clamp(demandScroll - delta, 0, maxDemandScroll);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (selectedTab == MarketTab.BUY && materialSearchFocused && !Character.isISOControl(codePoint)) {
            if (materialSearch.length() < 28) {
                materialSearch += codePoint;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedTab == MarketTab.BUY && materialSearchFocused) {
            if (keyCode == 259 && !materialSearch.isEmpty()) {
                materialSearch = materialSearch.substring(0, materialSearch.length() - 1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335 || keyCode == 256) {
                materialSearchFocused = false;
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
        fillBorder(graphics, layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight, PANEL_EDGE_DARK, PANEL);
        graphics.fill(layout.panelX + 2, layout.panelY + 2, layout.panelX + layout.panelWidth - 2, layout.panelY + 4, PANEL_EDGE);
        graphics.fill(layout.panelX + 2, layout.panelY + layout.panelHeight - 4, layout.panelX + layout.panelWidth - 2, layout.panelY + layout.panelHeight - 2, PANEL_EDGE_DARK);
        graphics.fill(layout.panelX + 8, layout.panelY + 58, layout.panelX + layout.panelWidth - 8, layout.panelY + 59, 0xFF5A412B);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout) {
        graphics.drawString(font, title, layout.panelX + 13, layout.panelY + 9, TEXT_LIGHT, false);
        String level = Component.translatable("container.trippycraft.village_market.level", snapshot.level()).getString();
        graphics.drawString(font, level, layout.panelX + 13, layout.panelY + 21, TEXT_MUTED, false);

        String emeralds = walletValue() + " emeralds";
        graphics.renderItem(new ItemStack(Items.EMERALD), layout.panelX + layout.panelWidth - font.width(emeralds) - 33, layout.panelY + 10);
        graphics.drawString(font, Component.literal(emeralds), layout.panelX + layout.panelWidth - font.width(emeralds) - 13, layout.panelY + 15, EMERALD, false);
    }

    private void renderTabs(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        int tabWidth = (layout.panelWidth - 20) / MarketTab.values().length;
        int tabX = layout.panelX + 10;
        for (MarketTab tab : MarketTab.values()) {
            Bounds bounds = new Bounds(tabX, layout.panelY + 32, tabWidth - 3, 19);
            boolean selected = tab == selectedTab;
            boolean hovered = bounds.contains(mouseX, mouseY);
            int fill = selected ? WOOD_ACTIVE : hovered ? 0xFF423025 : WOOD;
            graphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), fill);
            if (selected) {
                graphics.fill(bounds.x, bounds.bottom() - 2, bounds.right(), bounds.bottom(), EMERALD_DARK);
            }
            String label = fit(tab.label, bounds.w - 8);
            graphics.drawString(font, Component.literal(label), bounds.x + (bounds.w - font.width(label)) / 2, bounds.y + 6, selected ? TEXT_LIGHT : TEXT_MUTED, false);
            tabX += tabWidth;
        }
    }

    private void renderSellProducts(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        renderCategoryRail(graphics, layout, sellCategories(), selectedSellCategory, mouseX, mouseY, true);

        int x = layout.contentX;
        int y = layout.contentY;
        fillBorder(graphics, x, y, layout.contentWidth, layout.contentHeight, 0xFF130D09, 0xFF251B15);
        graphics.drawString(font, Component.literal("Sell Products"), x + 9, y + 8, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal("Demand changes prices, never access."), x + layout.contentWidth - 190, y + 8, TEXT_MUTED, false);

        int viewportX = x + 7;
        int viewportY = y + 24;
        int viewportWidth = layout.contentWidth - 14;
        int viewportHeight = layout.contentHeight - 31;
        List<SellableMarketProduct> products = filteredSellProducts();
        int contentHeight = products.size() * 37;
        maxSellScroll = Math.max(0, contentHeight - viewportHeight + 4);
        sellScroll = Mth.clamp(sellScroll, 0, maxSellScroll);

        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        int rowY = viewportY - sellScroll;
        for (SellableMarketProduct product : products) {
            renderSellRow(graphics, product, viewportX, rowY, viewportWidth, mouseX, mouseY);
            rowY += 37;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, viewportX + viewportWidth - 3, viewportY, viewportHeight, sellScroll, maxSellScroll);
    }

    private void renderSellRow(GuiGraphics graphics, SellableMarketProduct product, int x, int y, int width, int mouseX, int mouseY) {
        int count = inventoryCount(product.item().get());
        int fill = count > 0 ? 0xFF31231A : 0xFF211A16;
        graphics.fill(x, y, x + width, y + 33, fill);
        graphics.fill(x, y, x + 2, y + 33, count > 0 ? EMERALD_DARK : DISABLED);

        ItemStack stack = product.stack();
        graphics.renderItem(stack, x + 6, y + 8);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x + 6, y + 8, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = stack;
        }

        String name = fit(stack.getHoverName().getString(), width - 330);
        graphics.drawString(font, Component.literal(name), x + 27, y + 6, TEXT_LIGHT, false);
        String category = product.category();
        graphics.drawString(font, Component.literal(category), x + 27, y + 18, TEXT_MUTED, false);

        int demand = snapshot.demandMultiplier(product.id());
        int price = MarketMath.priceHundredths(product.baseEmeraldValue(), demand, snapshot.level());
        graphics.drawString(font, Component.literal(formatPrice(price) + "e"), x + width - 273, y + 6, EMERALD, false);
        graphics.drawString(font, Component.literal(formatDemand(demand)), x + width - 273, y + 18, demand >= 100 ? GOLD : TEXT_MUTED, false);
        graphics.drawString(font, Component.literal("Inv " + count), x + width - 215, y + 12, count > 0 ? TEXT_LIGHT : TEXT_FADED, false);

        int buttonX = x + width - 149;
        renderButton(graphics, new Bounds(buttonX, y + 7, 33, 19), "1", count >= 1, mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 37, y + 7, 33, 19), "8", count >= 8, mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 74, y + 7, 34, 19), "Stack", count > 0, mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 112, y + 7, 34, 19), "All", count > 0, mouseX, mouseY);
    }

    private void renderBuyMaterials(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        renderCategoryRail(graphics, layout, materialCategories(), selectedMaterialCategory, mouseX, mouseY, false);

        int x = layout.contentX;
        int y = layout.contentY;
        fillBorder(graphics, x, y, layout.contentWidth, layout.contentHeight, 0xFF130D09, 0xFF251B15);
        graphics.drawString(font, Component.literal("Buy Materials"), x + 9, y + 8, TEXT_LIGHT, false);
        Bounds search = searchBounds(layout);
        graphics.fill(search.x, search.y, search.right(), search.bottom(), materialSearchFocused ? 0xFF3B2B20 : 0xFF1B130F);
        graphics.fill(search.x, search.bottom() - 1, search.right(), search.bottom(), materialSearchFocused ? EMERALD_DARK : 0xFF5E432A);
        String query = materialSearch.isEmpty() && !materialSearchFocused ? "Search catalog" : materialSearch;
        graphics.drawString(font, Component.literal(fit(query, search.w - 8)), search.x + 5, search.y + 6, materialSearch.isEmpty() ? TEXT_FADED : TEXT_LIGHT, false);

        int viewportX = x + 7;
        int viewportY = y + 45;
        int viewportWidth = layout.contentWidth - 14;
        int viewportHeight = layout.contentHeight - 52;
        List<BuyableMarketMaterial> materials = filteredMaterials();
        int contentHeight = materials.size() * 35;
        maxBuyScroll = Math.max(0, contentHeight - viewportHeight + 4);
        buyScroll = Mth.clamp(buyScroll, 0, maxBuyScroll);

        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        int rowY = viewportY - buyScroll;
        for (BuyableMarketMaterial material : materials) {
            renderMaterialRow(graphics, material, viewportX, rowY, viewportWidth, mouseX, mouseY);
            rowY += 35;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, viewportX + viewportWidth - 3, viewportY, viewportHeight, buyScroll, maxBuyScroll);
    }

    private void renderMaterialRow(GuiGraphics graphics, BuyableMarketMaterial material, int x, int y, int width, int mouseX, int mouseY) {
        int emeralds = walletValue();
        graphics.fill(x, y, x + width, y + 31, 0xFF31231A);
        graphics.fill(x, y, x + 2, y + 31, PARCHMENT_DARK);

        ItemStack stack = material.stack(1);
        graphics.renderItem(stack, x + 6, y + 7);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x + 6, y + 7, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = stack;
        }

        String name = fit(stack.getHoverName().getString(), width - 300);
        graphics.drawString(font, Component.literal(name), x + 27, y + 5, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal(material.category()), x + 27, y + 17, TEXT_MUTED, false);
        graphics.drawString(font, Component.literal(material.emeraldPricePerUnit() + "e each"), x + width - 221, y + 11, EMERALD, false);

        int buttonX = x + width - 149;
        int maxStack = Math.max(1, stack.getMaxStackSize());
        int midAmount = Math.min(16, maxStack);
        int stackAmount = maxStack;
        renderButton(graphics, new Bounds(buttonX, y + 6, 33, 19), "1", emeralds >= material.emeraldPricePerUnit(), mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 37, y + 6, 33, 19), Integer.toString(midAmount), maxStack > 1 && emeralds >= material.emeraldPricePerUnit() * midAmount, mouseX, mouseY);
        renderButton(graphics, new Bounds(buttonX + 74, y + 6, 34, 19), Integer.toString(stackAmount), maxStack > 1 && emeralds >= material.emeraldPricePerUnit() * stackAmount, mouseX, mouseY);
    }

    private void renderContracts(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        fillBorder(graphics, layout.bodyX, layout.contentY, layout.bodyWidth, layout.contentHeight, 0xFF130D09, 0xFF251B15);
        graphics.drawString(font, Component.literal("Contracts"), layout.bodyX + 9, layout.contentY + 8, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal("Refresh in " + formatRefresh(snapshot.ticksUntilContractRefresh())), layout.bodyX + layout.bodyWidth - 92, layout.contentY + 8, TEXT_MUTED, false);

        int viewportX = layout.bodyX + 8;
        int viewportY = layout.contentY + 27;
        int viewportWidth = layout.bodyWidth - 16;
        int viewportHeight = layout.contentHeight - 35;
        int contentHeight = snapshot.contracts().size() * 61;
        maxContractScroll = Math.max(0, contentHeight - viewportHeight + 4);
        contractScroll = Mth.clamp(contractScroll, 0, maxContractScroll);

        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        int rowY = viewportY - contractScroll;
        if (snapshot.contracts().isEmpty()) {
            graphics.drawString(font, Component.literal("No contracts are posted right now."), viewportX + 5, rowY + 8, TEXT_MUTED, false);
        }
        for (MarketSnapshotPacket.ContractInfo contract : snapshot.contracts()) {
            renderContractCard(graphics, contract, viewportX, rowY, viewportWidth, mouseX, mouseY);
            rowY += 61;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, viewportX + viewportWidth - 3, viewportY, viewportHeight, contractScroll, maxContractScroll);
    }

    private void renderContractCard(GuiGraphics graphics, MarketSnapshotPacket.ContractInfo contract, int x, int y, int width, int mouseX, int mouseY) {
        Optional<SellableMarketProduct> maybeProduct = MarketRegistries.sellableProduct(contract.productId());
        if (maybeProduct.isEmpty()) {
            return;
        }
        SellableMarketProduct product = maybeProduct.get();
        int available = inventoryCount(product.item().get());
        boolean canComplete = available >= contract.quantity();
        fillBorder(graphics, x, y, width, 55, 0xFF4A3320, 0xFF302219);
        graphics.fill(x + 1, y + 1, x + 4, y + 54, canComplete ? EMERALD_DARK : 0xFF7A5435);

        ItemStack stack = product.stack();
        graphics.renderItem(stack, x + 9, y + 12);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x + 9, y + 12, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = stack;
        }

        graphics.drawString(font, Component.literal(contract.type()), x + 32, y + 8, GOLD, false);
        graphics.drawString(font, Component.literal(fit(stack.getHoverName().getString(), width - 260)), x + 32, y + 20, TEXT_LIGHT, false);
        graphics.drawString(font, Component.literal("Need " + contract.quantity() + " / Have " + available), x + 32, y + 32, canComplete ? EMERALD : TEXT_MUTED, false);
        graphics.drawString(font, Component.literal(contract.rewardEmeralds() + "e"), x + width - 155, y + 13, EMERALD, false);
        graphics.drawString(font, Component.literal("+" + contract.reputationReward() + " rep"), x + width - 155, y + 26, TEXT_MUTED, false);

        renderButton(graphics, new Bounds(x + width - 72, y + 17, 58, 20), "Complete", canComplete, mouseX, mouseY);
    }

    private void renderReputation(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        fillBorder(graphics, layout.bodyX, layout.contentY, layout.bodyWidth, layout.contentHeight, 0xFF130D09, PARCHMENT);
        int x = layout.bodyX + 12;
        int y = layout.contentY + 12;
        graphics.drawString(font, Component.literal("Market Reputation"), x, y, TEXT_DARK, false);
        y += 16;
        graphics.drawString(font, Component.literal("Level " + snapshot.level() + "  Reputation " + snapshot.reputation()), x, y, TEXT_DARK, false);
        y += 15;
        int progressWidth = Math.min(230, layout.bodyWidth - 30);
        int progress = snapshot.level() >= 20 ? progressWidth : progressWidth * (snapshot.reputation() % 250) / 250;
        graphics.fill(x, y, x + progressWidth, y + 8, 0xFF6E5839);
        graphics.fill(x, y, x + progress, y + 8, EMERALD_DARK);
        y += 17;
        graphics.drawString(font, Component.literal("Units sold: " + snapshot.totalUnitsSold()), x, y, TEXT_DARK, false);
        graphics.drawString(font, Component.literal("Contracts completed: " + snapshot.completedContracts()), x + 145, y, TEXT_DARK, false);
        y += 20;
        graphics.fill(x, y, layout.bodyX + layout.bodyWidth - 12, y + 1, PARCHMENT_DARK);
        y += 9;
        graphics.drawString(font, Component.literal("Current Demand"), x, y, TEXT_DARK, false);

        int viewportX = x;
        int viewportY = y + 15;
        int viewportWidth = layout.bodyWidth - 24;
        int viewportHeight = layout.contentY + layout.contentHeight - viewportY - 9;
        List<SellableMarketProduct> products = MarketRegistries.sellableProducts().stream()
                .sorted(Comparator.comparingInt((SellableMarketProduct product) -> snapshot.demandMultiplier(product.id())).reversed())
                .toList();
        int contentHeight = products.size() * 23;
        maxDemandScroll = Math.max(0, contentHeight - viewportHeight + 4);
        demandScroll = Mth.clamp(demandScroll, 0, maxDemandScroll);

        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        int rowY = viewportY - demandScroll;
        for (SellableMarketProduct product : products) {
            renderDemandRow(graphics, product, viewportX, rowY, viewportWidth, mouseX, mouseY);
            rowY += 23;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, viewportX + viewportWidth - 3, viewportY, viewportHeight, demandScroll, maxDemandScroll);
    }

    private void renderDemandRow(GuiGraphics graphics, SellableMarketProduct product, int x, int y, int width, int mouseX, int mouseY) {
        int demand = snapshot.demandMultiplier(product.id());
        int price = MarketMath.priceHundredths(product.baseEmeraldValue(), demand, snapshot.level());
        graphics.fill(x, y, x + width, y + 20, 0x20FFFFFF);
        ItemStack stack = product.stack();
        graphics.renderItem(stack, x + 3, y + 2);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x + 3, y + 2, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = stack;
        }
        graphics.drawString(font, Component.literal(fit(stack.getHoverName().getString(), width - 155)), x + 24, y + 6, TEXT_DARK, false);
        graphics.drawString(font, Component.literal(formatDemand(demand)), x + width - 91, y + 6, demand >= 100 ? 0xFF157846 : 0xFF745C34, false);
        graphics.drawString(font, Component.literal(formatPrice(price) + "e"), x + width - 45, y + 6, 0xFF157846, false);
    }

    private boolean clickSellProducts(Layout layout, double mouseX, double mouseY) {
        if (clickCategory(layout, sellCategories(), mouseX, mouseY, true)) {
            return true;
        }

        int viewportX = layout.contentX + 7;
        int viewportY = layout.contentY + 24;
        int viewportWidth = layout.contentWidth - 14;
        if (!new Bounds(viewportX, viewportY, viewportWidth, layout.contentHeight - 31).contains(mouseX, mouseY)) {
            return false;
        }
        int rowY = viewportY - sellScroll;
        for (SellableMarketProduct product : filteredSellProducts()) {
            int count = inventoryCount(product.item().get());
            if (count > 0) {
                int buttonX = viewportX + viewportWidth - 149;
                if (new Bounds(buttonX, rowY + 7, 33, 19).contains(mouseX, mouseY)) {
                    sendSell(product.id(), 1);
                    return true;
                }
                if (new Bounds(buttonX + 37, rowY + 7, 33, 19).contains(mouseX, mouseY)) {
                    sendSell(product.id(), 8);
                    return true;
                }
                if (new Bounds(buttonX + 74, rowY + 7, 34, 19).contains(mouseX, mouseY)) {
                    sendSell(product.id(), product.stack().getMaxStackSize());
                    return true;
                }
                if (new Bounds(buttonX + 112, rowY + 7, 34, 19).contains(mouseX, mouseY)) {
                    sendSell(product.id(), 0);
                    return true;
                }
            }
            rowY += 37;
        }
        return false;
    }

    private boolean clickBuyMaterials(Layout layout, double mouseX, double mouseY) {
        materialSearchFocused = false;
        if (searchBounds(layout).contains(mouseX, mouseY)) {
            materialSearchFocused = true;
            return true;
        }
        if (clickCategory(layout, materialCategories(), mouseX, mouseY, false)) {
            return true;
        }

        int viewportX = layout.contentX + 7;
        int viewportY = layout.contentY + 45;
        int viewportWidth = layout.contentWidth - 14;
        if (!new Bounds(viewportX, viewportY, viewportWidth, layout.contentHeight - 52).contains(mouseX, mouseY)) {
            return false;
        }
        int rowY = viewportY - buyScroll;
        int emeralds = walletValue();
        for (BuyableMarketMaterial material : filteredMaterials()) {
            int buttonX = viewportX + viewportWidth - 149;
            int maxStack = Math.max(1, material.stack(1).getMaxStackSize());
            int midAmount = Math.min(16, maxStack);
            int stackAmount = maxStack;
            if (emeralds >= material.emeraldPricePerUnit() && new Bounds(buttonX, rowY + 6, 33, 19).contains(mouseX, mouseY)) {
                sendBuy(material.id(), 1);
                return true;
            }
            if (maxStack > 1 && emeralds >= material.emeraldPricePerUnit() * midAmount && new Bounds(buttonX + 37, rowY + 6, 33, 19).contains(mouseX, mouseY)) {
                sendBuy(material.id(), midAmount);
                return true;
            }
            if (maxStack > 1 && emeralds >= material.emeraldPricePerUnit() * stackAmount && new Bounds(buttonX + 74, rowY + 6, 34, 19).contains(mouseX, mouseY)) {
                sendBuy(material.id(), stackAmount);
                return true;
            }
            rowY += 35;
        }
        return false;
    }

    private boolean clickContracts(Layout layout, double mouseX, double mouseY) {
        int viewportX = layout.bodyX + 8;
        int viewportY = layout.contentY + 27;
        int viewportWidth = layout.bodyWidth - 16;
        if (!new Bounds(viewportX, viewportY, viewportWidth, layout.contentHeight - 35).contains(mouseX, mouseY)) {
            return false;
        }
        int rowY = viewportY - contractScroll;
        for (MarketSnapshotPacket.ContractInfo contract : snapshot.contracts()) {
            Optional<SellableMarketProduct> product = MarketRegistries.sellableProduct(contract.productId());
            if (product.isPresent()
                    && inventoryCount(product.get().item().get()) >= contract.quantity()
                    && new Bounds(viewportX + viewportWidth - 72, rowY + 17, 58, 20).contains(mouseX, mouseY)) {
                ModNetwork.sendMarketAction(new MarketActionPacket(MarketActionPacket.Action.COMPLETE_CONTRACT, contract.id(), 0));
                return true;
            }
            rowY += 61;
        }
        return false;
    }

    private void renderCategoryRail(GuiGraphics graphics, Layout layout, List<String> categories, String selected, int mouseX, int mouseY, boolean sell) {
        fillBorder(graphics, layout.railX, layout.contentY, layout.railWidth, layout.contentHeight, 0xFF130D09, PANEL_ALT);
        graphics.drawString(font, Component.literal("Categories"), layout.railX + 7, layout.contentY + 8, TEXT_LIGHT, false);
        int listY = layout.contentY + 24;
        int listHeight = layout.contentHeight - 29;
        int maxScroll = Math.max(0, categories.size() * 21 - listHeight + 2);
        int scroll = sell ? Mth.clamp(sellCategoryScroll, 0, maxScroll) : Mth.clamp(materialCategoryScroll, 0, maxScroll);
        if (sell) {
            sellCategoryScroll = scroll;
            maxSellCategoryScroll = maxScroll;
        } else {
            materialCategoryScroll = scroll;
            maxMaterialCategoryScroll = maxScroll;
        }

        graphics.enableScissor(layout.railX + 3, listY, layout.railX + layout.railWidth - 3, listY + listHeight);
        int y = listY - scroll;
        for (String category : categories) {
            Bounds bounds = new Bounds(layout.railX + 5, y, layout.railWidth - 10, 18);
            boolean active = selected.equals(category);
            boolean hovered = bounds.contains(mouseX, mouseY);
            graphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), active ? WOOD_ACTIVE : hovered ? 0xFF3A2A20 : WOOD);
            if (active) {
                graphics.fill(bounds.x, bounds.y, bounds.x + 2, bounds.bottom(), EMERALD_DARK);
            }
            graphics.drawString(font, Component.literal(fit(category, bounds.w - 8)), bounds.x + 5, bounds.y + 5, active ? TEXT_LIGHT : TEXT_MUTED, false);
            y += 21;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, layout.railX + layout.railWidth - 4, listY, listHeight, scroll, maxScroll);
    }

    private boolean clickCategory(Layout layout, List<String> categories, double mouseX, double mouseY, boolean sell) {
        if (!new Bounds(layout.railX + 3, layout.contentY + 24, layout.railWidth - 6, layout.contentHeight - 29).contains(mouseX, mouseY)) {
            return false;
        }

        int scroll = sell ? sellCategoryScroll : materialCategoryScroll;
        int y = layout.contentY + 24 - scroll;
        for (String category : categories) {
            Bounds bounds = new Bounds(layout.railX + 5, y, layout.railWidth - 10, 18);
            if (bounds.contains(mouseX, mouseY)) {
                if (sell) {
                    selectedSellCategory = category;
                    sellScroll = 0;
                } else {
                    selectedMaterialCategory = category;
                    buyScroll = 0;
                }
                return true;
            }
            y += 21;
        }
        return false;
    }

    private List<SellableMarketProduct> filteredSellProducts() {
        return MarketRegistries.sellableProducts().stream()
                .filter(product -> selectedSellCategory.equals(ALL) || product.category().equals(selectedSellCategory))
                .toList();
    }

    private List<BuyableMarketMaterial> filteredMaterials() {
        String query = materialSearch.trim().toLowerCase(Locale.ROOT);
        return MarketRegistries.buyableMaterials().stream()
                .filter(material -> selectedMaterialCategory.equals(ALL) || material.category().equals(selectedMaterialCategory))
                .filter(material -> query.isEmpty()
                        || material.id().contains(query)
                        || material.category().toLowerCase(Locale.ROOT).contains(query)
                        || material.stack(1).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<String> sellCategories() {
        List<String> categories = new ArrayList<>();
        categories.add(ALL);
        categories.addAll(MarketRegistries.sellableCategories());
        return categories;
    }

    private List<String> materialCategories() {
        List<String> categories = new ArrayList<>();
        categories.add(ALL);
        categories.addAll(MarketRegistries.materialCategories());
        return categories;
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

    private void sendSell(String productId, int amount) {
        ModNetwork.sendMarketAction(new MarketActionPacket(MarketActionPacket.Action.SELL_PRODUCT, productId, amount));
    }

    private void sendBuy(String materialId, int amount) {
        ModNetwork.sendMarketAction(new MarketActionPacket(MarketActionPacket.Action.BUY_MATERIAL, materialId, amount));
    }

    private void requestSettlement() {
        if (settlementRequested) {
            return;
        }
        settlementRequested = true;
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendMarketAction(new MarketActionPacket(MarketActionPacket.Action.CLOSE_MARKET, "", 0));
        }
    }

    private void renderButton(GuiGraphics graphics, Bounds bounds, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && bounds.contains(mouseX, mouseY);
        int fill = !enabled ? 0xFF332A23 : hovered ? 0xFF2A7E50 : 0xFF205F3D;
        int text = !enabled ? TEXT_FADED : TEXT_LIGHT;
        graphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), fill);
        graphics.fill(bounds.x, bounds.bottom() - 1, bounds.right(), bounds.bottom(), !enabled ? DISABLED : EMERALD_DARK);
        String fitted = fit(label, bounds.w - 4);
        graphics.drawString(font, Component.literal(fitted), bounds.x + (bounds.w - font.width(fitted)) / 2, bounds.y + 6, text, false);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int scroll, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        graphics.fill(x, y, x + 2, y + height, 0x6033221C);
        int thumbHeight = Math.max(18, height * height / Math.max(height, height + maxScroll));
        int thumbY = y + (height - thumbHeight) * scroll / Math.max(1, maxScroll);
        graphics.fill(x - 1, thumbY, x + 3, thumbY + thumbHeight, EMERALD_DARK);
    }

    private String formatPrice(int hundredths) {
        if (hundredths % 100 == 0) {
            return Integer.toString(hundredths / 100);
        }
        return String.format(Locale.ROOT, "%.1f", hundredths / 100.0D);
    }

    private String formatDemand(int multiplier) {
        return String.format(Locale.ROOT, "%.2fx", multiplier / 100.0D);
    }

    private String formatRefresh(long ticks) {
        if (ticks <= 20) {
            return "soon";
        }
        long seconds = ticks / 20;
        long minutes = Math.max(1, seconds / 60);
        return minutes + "m";
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

    private Bounds searchBounds(Layout layout) {
        return new Bounds(layout.contentX + layout.contentWidth - 158, layout.contentY + 5, 149, 21);
    }

    private Bounds railBounds(Layout layout) {
        return new Bounds(layout.railX, layout.contentY, layout.railWidth, layout.contentHeight);
    }

    private FittedGui viewport() {
        return FittedGui.fit(width, height, 708, 390);
    }

    private Layout layout() {
        FittedGui viewport = viewport();
        int width = viewport.width();
        int height = viewport.height();
        int panelWidth = Math.min(width - 18, 690);
        int panelHeight = Math.min(height - 20, 370);
        panelWidth = Math.max(panelWidth, 520);
        panelHeight = Math.max(panelHeight, 270);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int contentY = panelY + 66;
        int contentHeight = panelHeight - 76;
        int bodyX = panelX + 10;
        int bodyWidth = panelWidth - 20;
        int railWidth = 113;
        int railX = bodyX;
        int contentX = railX + railWidth + 7;
        int contentWidth = bodyX + bodyWidth - contentX;
        return new Layout(panelX, panelY, panelWidth, panelHeight, bodyX, bodyWidth, railX, railWidth, contentX, contentY, contentWidth, contentHeight);
    }

    private enum MarketTab {
        SELL("Sell Products"),
        BUY("Buy Materials"),
        CONTRACTS("Contracts"),
        REPUTATION("Reputation / Demand");

        private final String label;

        MarketTab(String label) {
            this.label = label;
        }
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
            int bodyX,
            int bodyWidth,
            int railX,
            int railWidth,
            int contentX,
            int contentY,
            int contentWidth,
            int contentHeight
    ) {
        Bounds contentBounds() {
            return new Bounds(bodyX, contentY, bodyWidth, contentHeight);
        }
    }
}
