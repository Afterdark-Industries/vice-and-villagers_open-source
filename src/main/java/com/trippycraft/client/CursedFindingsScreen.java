package com.trippycraft.client;

import com.trippycraft.guide.CursedFindingCategory;
import com.trippycraft.guide.CursedFindingEntry;
import com.trippycraft.guide.CursedFindingItemRef;
import com.trippycraft.guide.CursedFindingProductionStep;
import com.trippycraft.guide.CursedFindingRecipe;
import com.trippycraft.guide.CursedFindingTab;
import com.trippycraft.guide.CursedFindingsRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CursedFindingsScreen extends Screen {
    private static final int BACKDROP = 0xE00B090A;
    private static final int PANEL = 0xFF2B211A;
    private static final int PANEL_EDGE = 0xFF7C5A39;
    private static final int PANEL_EDGE_DARK = 0xFF17100D;
    private static final int SIDEBAR = 0xFF211916;
    private static final int SIDEBAR_ACTIVE = 0xFF3A2922;
    private static final int CONTENT = 0xFFC1AC82;
    private static final int CONTENT_DARK = 0xFF9C855C;
    private static final int RED_INK = 0xFF9F3636;
    private static final int PURPLE_INK = 0xFF7B5C9E;
    private static final int LIGHT_TEXT = 0xFFE8D8B8;
    private static final int MUTED_TEXT = 0xFFB8A88A;
    private static final int DARK_TEXT = 0xFF241913;
    private static final int DARK_MUTED_TEXT = 0xFF5A4636;
    private static final int UNKNOWN_TEXT = 0xFF8F8170;
    private static final int ENTRY_ROW_HEIGHT = 25;
    private static final int ENTRY_ROW_STEP = 29;

    private Set<String> unlockedFindings;
    private CursedFindingCategory selectedCategory = CursedFindingCategory.PSYCHEDELICS;
    private String selectedEntryId;
    private CursedFindingTab selectedTab = CursedFindingTab.OVERVIEW;
    private int entryScroll;
    private int maxEntryScroll;
    private int contentScroll;
    private int maxContentScroll;
    private int contentViewportHeight;
    private Component pendingTooltip;

    public CursedFindingsScreen(Set<String> unlockedFindings) {
        super(Component.literal("Cursed Findings"));
        this.unlockedFindings = Set.copyOf(unlockedFindings);
    }

    public void updateUnlockedFindings(Set<String> unlockedFindings) {
        this.unlockedFindings = Set.copyOf(unlockedFindings);
        if (selectedEntryId != null && !CursedFindingsRegistry.contains(selectedEntryId)) {
            selectedEntryId = null;
        }
        contentScroll = Mth.clamp(contentScroll, 0, maxContentScroll);
        clampEntryScroll(layout());
    }

    @Override
    protected void init() {
        super.init();
        clampEntryScroll(layout());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        FittedGui viewport = viewport();
        try (FittedGui.Drawing drawing = viewport.begin(graphics)) {
            graphics = drawing.graphics();
            mouseX = (int) viewport.mouseX(mouseX);
            mouseY = (int) viewport.mouseY(mouseY);
            pendingTooltip = null;
            renderBackground(graphics, mouseX, mouseY, partialTick);
            Layout layout = layout();
            renderFrame(graphics, layout);
            renderHeader(graphics, layout);
            renderCategories(graphics, layout, mouseX, mouseY);
            renderEntries(graphics, layout, mouseX, mouseY);
            renderContent(graphics, layout, mouseX, mouseY);
            if (pendingTooltip != null) {
                graphics.renderTooltip(font, pendingTooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), BACKDROP);
        for (int i = 0; i < 5; i++) {
            int inset = i * 3;
            graphics.fill(inset, inset, graphics.guiWidth() - inset, graphics.guiHeight() - inset, 0x08000000 + i * 0x02000000);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        FittedGui viewport = viewport();
        mouseX = viewport.mouseX(mouseX);
        mouseY = viewport.mouseY(mouseY);
        Layout layout = layout();
        clampEntryScroll(layout);
        if (button == 0) {
            int y = layout.categoryY + 18;
            for (CursedFindingCategory category : CursedFindingsRegistry.categories()) {
                Bounds row = new Bounds(layout.categoryX + 5, y, layout.categoryWidth - 10, 19);
                if (row.contains(mouseX, mouseY)) {
                    selectedCategory = category;
                    selectedEntryId = null;
                    selectedTab = CursedFindingTab.OVERVIEW;
                    entryScroll = 0;
                    contentScroll = 0;
                    return true;
                }
                y += 22;
            }

            Bounds entryViewport = entryViewport(layout);
            if (entryViewport.contains(mouseX, mouseY)) {
                y = entryViewport.y - entryScroll;
                for (CursedFindingEntry entry : CursedFindingsRegistry.entriesFor(selectedCategory)) {
                    Bounds row = new Bounds(entryViewport.x, y, entryViewport.w, ENTRY_ROW_HEIGHT);
                    if (row.contains(mouseX, mouseY)) {
                        selectedEntryId = entry.id();
                        selectedTab = CursedFindingTab.OVERVIEW;
                        contentScroll = 0;
                        return true;
                    }
                    y += ENTRY_ROW_STEP;
                }
            }

            Optional<CursedFindingEntry> selected = selectedEntry();
            if (selected.isPresent() && isUnlocked(selected.get())) {
                int tabX = layout.contentX + 10;
                int tabY = layout.contentY + 47;
                int tabWidth = Math.max(46, (layout.contentWidth - 20) / CursedFindingTab.values().length);
                for (CursedFindingTab tab : CursedFindingTab.values()) {
                    Bounds tabBounds = new Bounds(tabX, tabY, tabWidth - 2, 18);
                    if (tabBounds.contains(mouseX, mouseY)) {
                        selectedTab = tab;
                        contentScroll = 0;
                        return true;
                    }
                    tabX += tabWidth;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        FittedGui viewport = viewport();
        mouseX = viewport.mouseX(mouseX);
        mouseY = viewport.mouseY(mouseY);
        Layout layout = layout();
        clampEntryScroll(layout);
        Bounds entryBounds = new Bounds(layout.entryX, layout.entryY, layout.entryWidth, layout.entryHeight);
        if (entryBounds.contains(mouseX, mouseY) && maxEntryScroll > 0) {
            entryScroll = Mth.clamp(entryScroll - (int) Math.round(scrollY * ENTRY_ROW_STEP), 0, maxEntryScroll);
            return true;
        }
        Bounds contentBounds = new Bounds(layout.contentX, layout.contentY, layout.contentWidth, layout.contentHeight);
        if (contentBounds.contains(mouseX, mouseY) && maxContentScroll > 0) {
            contentScroll = Mth.clamp(contentScroll - (int) Math.round(scrollY * 18.0D), 0, maxContentScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderFrame(GuiGraphics graphics, Layout layout) {
        fillBorder(graphics, layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight, PANEL_EDGE_DARK, PANEL);
        graphics.fill(layout.panelX + 2, layout.panelY + 2, layout.panelX + layout.panelWidth - 2, layout.panelY + 4, PANEL_EDGE);
        graphics.fill(layout.panelX + 2, layout.panelY + layout.panelHeight - 4, layout.panelX + layout.panelWidth - 2, layout.panelY + layout.panelHeight - 2, PANEL_EDGE_DARK);
        graphics.fill(layout.panelX + 9, layout.panelY + 31, layout.panelX + layout.panelWidth - 9, layout.panelY + 32, 0xFF5D4230);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout) {
        graphics.drawString(font, title, layout.panelX + 14, layout.panelY + 8, LIGHT_TEXT, false);
        String count = unlockedFindings.size() + "/" + CursedFindingsRegistry.entries().size() + " findings opened";
        graphics.drawString(font, Component.literal(count), layout.panelX + layout.panelWidth - font.width(count) - 14, layout.panelY + 8, MUTED_TEXT, false);
        graphics.drawString(font, Component.literal("The archive writes only after the first taste."), layout.panelX + 14, layout.panelY + 20, 0xFF937D68, false);
    }

    private void renderCategories(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        fillBorder(graphics, layout.categoryX, layout.categoryY, layout.categoryWidth, layout.categoryHeight, 0xFF140F0D, SIDEBAR);
        graphics.drawString(font, Component.literal("Categories"), layout.categoryX + 8, layout.categoryY + 7, LIGHT_TEXT, false);

        int y = layout.categoryY + 18;
        for (CursedFindingCategory category : CursedFindingsRegistry.categories()) {
            Bounds row = new Bounds(layout.categoryX + 5, y, layout.categoryWidth - 10, 19);
            boolean selected = category == selectedCategory;
            boolean hovered = row.contains(mouseX, mouseY);
            int fill = selected ? SIDEBAR_ACTIVE : hovered ? 0xFF2C211C : 0xFF1C1512;
            graphics.fill(row.x, row.y, row.right(), row.bottom(), fill);
            if (selected) {
                graphics.fill(row.x, row.y, row.x + 2, row.bottom(), RED_INK);
            }
            String label = fit(category.displayName(), row.w - 34);
            graphics.drawString(font, Component.literal(label), row.x + 5, row.y + 6, selected ? LIGHT_TEXT : MUTED_TEXT, false);
            String count = categoryUnlockedCount(category) + "/" + CursedFindingsRegistry.entriesFor(category).size();
            graphics.drawString(font, Component.literal(count), row.right() - font.width(count) - 5, row.y + 6, selected ? 0xFFE1B0A9 : 0xFF806D5C, false);
            y += 22;
        }
    }

    private void renderEntries(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        clampEntryScroll(layout);
        fillBorder(graphics, layout.entryX, layout.entryY, layout.entryWidth, layout.entryHeight, 0xFF140F0D, 0xFF281F1A);
        graphics.drawString(font, Component.literal("Findings"), layout.entryX + 8, layout.entryY + 7, LIGHT_TEXT, false);

        List<CursedFindingEntry> entries = CursedFindingsRegistry.entriesFor(selectedCategory);
        if (entries.isEmpty()) {
            drawWrapped(graphics, "No entries have been pinned here yet.", layout.entryX + 8, layout.entryY + 28, layout.entryWidth - 16, 0xFF8A7766);
            return;
        }

        Bounds viewport = entryViewport(layout);
        graphics.enableScissor(viewport.x, viewport.y, viewport.right(), viewport.bottom());
        int y = viewport.y - entryScroll;
        for (CursedFindingEntry entry : entries) {
            Bounds row = new Bounds(viewport.x, y, viewport.w, ENTRY_ROW_HEIGHT);
            y += ENTRY_ROW_STEP;
            if (row.bottom() <= viewport.y || row.y >= viewport.bottom()) {
                continue;
            }
            boolean unlocked = isUnlocked(entry);
            boolean selected = entry.id().equals(selectedEntryId);
            boolean hovered = viewport.contains(mouseX, mouseY) && row.contains(mouseX, mouseY);
            int fill = selected ? 0xFF46312A : hovered ? 0xFF342720 : 0xFF201814;
            graphics.fill(row.x, row.y, row.right(), row.bottom(), fill);
            if (selected) {
                graphics.fill(row.x, row.y, row.x + 2, row.bottom(), PURPLE_INK);
            }
            if (unlocked) {
                graphics.renderItem(entry.iconStack(), row.x + 5, row.y + 4);
                String label = fit(entry.displayName(), row.w - 29);
                graphics.drawString(font, Component.literal(label), row.x + 26, row.y + 9, LIGHT_TEXT, false);
            } else {
                drawUnknownIcon(graphics, row.x + 7, row.y + 5);
                graphics.drawString(font, Component.literal("Unknown Finding"), row.x + 26, row.y + 9, UNKNOWN_TEXT, false);
            }
        }
        graphics.disableScissor();
        if (maxEntryScroll > 0) {
            renderScrollbar(graphics, layout.entryX + layout.entryWidth - 4, viewport.y, viewport.h,
                    entryScroll, maxEntryScroll);
        }
    }

    private void renderContent(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        fillBorder(graphics, layout.contentX, layout.contentY, layout.contentWidth, layout.contentHeight, 0xFF1A120E, CONTENT);
        Optional<CursedFindingEntry> maybeEntry = selectedEntry();
        if (maybeEntry.isEmpty()) {
            renderArchiveIntro(graphics, layout);
            return;
        }

        CursedFindingEntry entry = maybeEntry.get();
        if (!isUnlocked(entry)) {
            renderLockedEntry(graphics, layout, entry);
            return;
        }

        renderUnlockedEntry(graphics, layout, entry, mouseX, mouseY);
    }

    private void renderArchiveIntro(GuiGraphics graphics, Layout layout) {
        int x = layout.contentX + 13;
        int y = layout.contentY + 12;
        graphics.drawString(font, Component.literal("Field Index"), x, y, DARK_TEXT, false);
        y += 17;
        y = drawWrapped(graphics, "A ledger of forbidden work. Names surface in the margins only after the body has made its first confession.", x, y, layout.contentWidth - 26, DARK_MUTED_TEXT);
        y += 7;
        graphics.fill(x, y, layout.contentX + layout.contentWidth - 13, y + 1, CONTENT_DARK);
        y += 12;
        for (CursedFindingCategory category : CursedFindingsRegistry.categories()) {
            int total = CursedFindingsRegistry.entriesFor(category).size();
            if (total == 0) {
                continue;
            }
            String line = category.displayName() + ": " + categoryUnlockedCount(category) + "/" + total + " revealed";
            graphics.drawString(font, Component.literal(line), x, y, DARK_TEXT, false);
            y += 12;
        }
    }

    private void renderLockedEntry(GuiGraphics graphics, Layout layout, CursedFindingEntry entry) {
        int x = layout.contentX + 13;
        int y = layout.contentY + 12;
        drawUnknownIcon(graphics, x, y);
        graphics.drawString(font, Component.literal("Unknown Finding"), x + 24, y + 4, DARK_TEXT, false);
        y += 28;
        graphics.fill(x, y, layout.contentX + layout.contentWidth - 13, y + 1, RED_INK);
        y += 13;
        drawWrapped(graphics, entry.lockedMessage(), x, y, layout.contentWidth - 26, DARK_MUTED_TEXT);
    }

    private void renderUnlockedEntry(GuiGraphics graphics, Layout layout, CursedFindingEntry entry, int mouseX, int mouseY) {
        int x = layout.contentX + 10;
        int y = layout.contentY + 9;
        graphics.renderItem(entry.iconStack(), x, y);
        graphics.drawString(font, Component.literal(entry.displayName()), x + 23, y + 2, DARK_TEXT, false);
        graphics.drawString(font, Component.literal(entry.category().displayName()), x + 23, y + 13, DARK_MUTED_TEXT, false);

        int tabX = layout.contentX + 10;
        int tabY = layout.contentY + 47;
        int tabWidth = Math.max(46, (layout.contentWidth - 20) / CursedFindingTab.values().length);
        for (CursedFindingTab tab : CursedFindingTab.values()) {
            boolean selected = tab == selectedTab;
            Bounds tabBounds = new Bounds(tabX, tabY, tabWidth - 2, 18);
            int fill = selected ? 0xFF66453A : tabBounds.contains(mouseX, mouseY) ? 0xFFE0C693 : 0xFFB4996D;
            graphics.fill(tabBounds.x, tabBounds.y, tabBounds.right(), tabBounds.bottom(), fill);
            String label = fit(tab.displayName(), tabBounds.w - 6);
            int color = selected ? LIGHT_TEXT : DARK_TEXT;
            graphics.drawString(font, Component.literal(label), tabBounds.x + (tabBounds.w - font.width(label)) / 2, tabBounds.y + 5, color, false);
            tabX += tabWidth;
        }

        int viewportX = layout.contentX + 10;
        int viewportY = layout.contentY + 71;
        int viewportWidth = layout.contentWidth - 20;
        int viewportHeight = layout.contentHeight - 80;
        contentViewportHeight = viewportHeight;
        contentScroll = Mth.clamp(contentScroll, 0, maxContentScroll);
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        int contentHeight = renderSelectedTab(graphics, entry, viewportX, viewportY - contentScroll, viewportWidth, mouseX, mouseY);
        graphics.disableScissor();
        maxContentScroll = Math.max(0, contentHeight - contentViewportHeight + 8);
        contentScroll = Mth.clamp(contentScroll, 0, maxContentScroll);
        if (maxContentScroll > 0) {
            renderScrollbar(graphics, viewportX + viewportWidth - 4, viewportY, viewportHeight,
                    contentScroll, maxContentScroll);
        }
    }

    private int renderSelectedTab(GuiGraphics graphics, CursedFindingEntry entry, int x, int y, int width, int mouseX, int mouseY) {
        int startY = y;
        switch (selectedTab) {
            case OVERVIEW -> y = renderParagraphs(graphics, entry.overview(), x, y, width, DARK_TEXT);
            case EFFECTS -> y = renderParagraphs(graphics, entry.effects(), x, y, width, DARK_TEXT);
            case PRODUCTION -> y = renderProduction(graphics, entry.productionChain(), x, y, width, mouseX, mouseY);
            case RECIPES -> y = renderRecipes(graphics, entry.recipes(), x, y, width, mouseX, mouseY);
            case NOTES -> y = renderParagraphs(graphics, entry.notes(), x, y, width, DARK_TEXT);
        }
        return y - startY;
    }

    private int renderParagraphs(GuiGraphics graphics, List<String> paragraphs, int x, int y, int width, int color) {
        if (paragraphs.isEmpty()) {
            return drawWrapped(graphics, "No notes have been written here yet.", x, y, width, DARK_MUTED_TEXT);
        }
        for (String paragraph : paragraphs) {
            graphics.fill(x, y + 3, x + 3, y + 6, RED_INK);
            y = drawWrapped(graphics, paragraph, x + 9, y, width - 9, color);
            y += 5;
        }
        return y;
    }

    private int renderProduction(GuiGraphics graphics, List<CursedFindingProductionStep> steps, int x, int y, int width, int mouseX, int mouseY) {
        if (steps.isEmpty()) {
            return drawWrapped(graphics, "No production steps have been pinned yet.", x, y, width, DARK_MUTED_TEXT);
        }
        for (CursedFindingProductionStep step : steps) {
            int cardY = y;
            int noteHeight = font.wordWrapHeight(Component.literal(step.note()), width - 18);
            int cardHeight = 68 + noteHeight;
            fillBorder(graphics, x, cardY, width, cardHeight, CONTENT_DARK, 0xFFD1BD91);
            int nodeWidth = Math.max(48, (width - 48) / 3);
            int nodeY = cardY + 10;
            renderItemNode(graphics, step.input(), x + 9, nodeY, nodeWidth, mouseX, mouseY);
            drawFlowArrow(graphics, x + 13 + nodeWidth, nodeY + 8, 18);
            renderItemNode(graphics, step.station(), x + 25 + nodeWidth, nodeY, nodeWidth, mouseX, mouseY);
            drawFlowArrow(graphics, x + 29 + nodeWidth * 2, nodeY + 8, 18);
            renderItemNode(graphics, step.output(), x + 41 + nodeWidth * 2, nodeY, nodeWidth, mouseX, mouseY);
            drawWrapped(graphics, step.note(), x + 9, cardY + 48, width - 18, DARK_MUTED_TEXT);
            y += cardHeight + 7;
        }
        return y;
    }

    private int renderRecipes(GuiGraphics graphics, List<CursedFindingRecipe> recipes, int x, int y, int width, int mouseX, int mouseY) {
        if (recipes.isEmpty()) {
            return drawWrapped(graphics, "No recipe cards have been pinned yet.", x, y, width, DARK_MUTED_TEXT);
        }
        for (CursedFindingRecipe recipe : recipes) {
            int cardY = y;
            int notesHeight = recipe.notes().stream()
                    .mapToInt(note -> font.wordWrapHeight(Component.literal(note), width - 18) + 3)
                    .sum();
            int cardHeight = 60 + notesHeight;
            fillBorder(graphics, x, cardY, width, cardHeight, CONTENT_DARK, 0xFFD1BD91);
            String titleText = fit(recipe.title(), width - 95);
            graphics.drawString(font, Component.literal(titleText), x + 8, cardY + 7, DARK_TEXT, false);
            String type = fit(recipe.type().displayName(), 82);
            graphics.drawString(font, Component.literal(type), x + width - font.width(type) - 8, cardY + 7, RED_INK, false);

            int iconX = x + 10;
            int iconY = cardY + 23;
            for (CursedFindingItemRef input : recipe.inputs()) {
                renderRecipeIcon(graphics, input, iconX, iconY, mouseX, mouseY);
                iconX += 22;
            }
            drawFlowArrow(graphics, Math.max(iconX + 2, x + width / 2 - 14), iconY + 5, 24);
            renderRecipeIcon(graphics, recipe.output(), x + width - 29, iconY, mouseX, mouseY);

            int noteY = cardY + 47;
            for (String note : recipe.notes()) {
                noteY = drawWrapped(graphics, note, x + 8, noteY, width - 16, DARK_MUTED_TEXT);
                noteY += 3;
            }
            y += cardHeight + 7;
        }
        return y;
    }

    private void renderItemNode(GuiGraphics graphics, CursedFindingItemRef ref, int x, int y, int width, int mouseX, int mouseY) {
        graphics.renderItem(ref.stack(), x, y);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x, y, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = Component.literal(ref.label());
        }
        String label = fit(ref.label(), width - 20);
        graphics.drawString(font, Component.literal(label), x + 20, y + 5, DARK_TEXT, false);
    }

    private void renderRecipeIcon(GuiGraphics graphics, CursedFindingItemRef ref, int x, int y, int mouseX, int mouseY) {
        graphics.renderItem(ref.stack(), x, y);
        if (graphics.containsPointInScissor(mouseX, mouseY)
                && new Bounds(x, y, 16, 16).contains(mouseX, mouseY)) {
            pendingTooltip = Component.literal(ref.label());
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int scroll, int maxScroll) {
        graphics.fill(x, y, x + 2, y + height, 0x5533221C);
        int thumbHeight = Math.max(18, height * height / Math.max(height, height + maxScroll));
        int thumbY = y + (height - thumbHeight) * scroll / Math.max(1, maxScroll);
        graphics.fill(x - 1, thumbY, x + 3, thumbY + thumbHeight, RED_INK);
    }

    private Bounds entryViewport(Layout layout) {
        return new Bounds(layout.entryX + 5, layout.entryY + 18, layout.entryWidth - 10,
                Math.max(0, layout.entryHeight - 23));
    }

    private void clampEntryScroll(Layout layout) {
        int entriesHeight = CursedFindingsRegistry.entriesFor(selectedCategory).size() * ENTRY_ROW_STEP
                - (ENTRY_ROW_STEP - ENTRY_ROW_HEIGHT);
        maxEntryScroll = Math.max(0, entriesHeight - entryViewport(layout).h);
        entryScroll = Mth.clamp(entryScroll, 0, maxEntryScroll);
    }

    private void drawUnknownIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 14, y + 14, 0xFF342720);
        graphics.fill(x + 1, y + 1, x + 13, y + 13, 0xFF17100D);
        graphics.drawString(font, Component.literal("?"), x + 5, y + 3, UNKNOWN_TEXT, false);
    }

    private void drawFlowArrow(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y + 4, x + width - 4, y + 6, RED_INK);
        graphics.fill(x + width - 5, y + 2, x + width - 2, y + 8, RED_INK);
        graphics.fill(x + width - 2, y + 4, x + width, y + 6, RED_INK);
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int categoryUnlockedCount(CursedFindingCategory category) {
        int count = 0;
        for (CursedFindingEntry entry : CursedFindingsRegistry.entriesFor(category)) {
            if (isUnlocked(entry)) {
                count++;
            }
        }
        return count;
    }

    private Optional<CursedFindingEntry> selectedEntry() {
        return selectedEntryId == null ? Optional.empty() : CursedFindingsRegistry.byId(selectedEntryId);
    }

    private boolean isUnlocked(CursedFindingEntry entry) {
        return unlockedFindings.contains(entry.id());
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
        return FittedGui.fit(width, height, 660, 368);
    }

    private Layout layout() {
        FittedGui viewport = viewport();
        int width = viewport.width();
        int height = viewport.height();
        int panelWidth = Math.min(width - 18, 642);
        int panelHeight = Math.min(height - 20, 348);
        panelWidth = Math.max(panelWidth, 430);
        panelHeight = Math.max(panelHeight, 254);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int bodyY = panelY + 38;
        int bodyHeight = panelHeight - 48;
        int categoryWidth = Math.min(136, Math.max(108, panelWidth / 5));
        int entryWidth = Math.min(162, Math.max(124, panelWidth / 4));
        int gap = 7;
        int categoryX = panelX + 10;
        int entryX = categoryX + categoryWidth + gap;
        int contentX = entryX + entryWidth + gap;
        int contentWidth = panelX + panelWidth - contentX - 10;
        return new Layout(panelX, panelY, panelWidth, panelHeight,
                categoryX, bodyY, categoryWidth, bodyHeight,
                entryX, bodyY, entryWidth, bodyHeight,
                contentX, bodyY, contentWidth, bodyHeight);
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
            int categoryX,
            int categoryY,
            int categoryWidth,
            int categoryHeight,
            int entryX,
            int entryY,
            int entryWidth,
            int entryHeight,
            int contentX,
            int contentY,
            int contentWidth,
            int contentHeight
    ) {
    }
}
