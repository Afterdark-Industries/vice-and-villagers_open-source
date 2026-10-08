package com.trippycraft.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.block.entity.CannabisPressBlockEntity;
import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.BartenderService;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.menu.AdvancedChemistryTableMenu;
import com.trippycraft.menu.BrewCauldronMenu;
import com.trippycraft.menu.CannabisPressMenu;
import com.trippycraft.menu.ChemistryTableMenu;
import com.trippycraft.menu.MortarAndPestleMenu;
import com.trippycraft.network.CustomerSnapshotPacket;
import com.trippycraft.network.DealerSnapshotPacket;
import com.trippycraft.network.MarketSnapshotPacket;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Opt-in native GUI regression. Creates a fresh dedicated world; never opens an existing save. */
public final class GuiSmokeCheck {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.guiSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "gui-render-input-smoke";
    private static final List<String> REPORT = new ArrayList<>();
    private static final BlockPos CHEMISTRY = new BlockPos(-2, 65, 1);
    private static final BlockPos ADVANCED = new BlockPos(-1, 65, 1);
    private static final BlockPos BREW = new BlockPos(0, 65, 1);
    private static final BlockPos PRESS = new BlockPos(1, 65, 1);
    private static final BlockPos MORTAR = new BlockPos(2, 65, 1);
    private static Phase phase = Phase.START;
    private static long startedNanos;
    private static volatile Throwable failure;
    private static volatile boolean fixtureReady;
    private static volatile boolean serverChecked;
    private static volatile MerchantOffers bartenderOffers;
    private static boolean queued;
    private static int screenshots;

    private GuiSmokeCheck() { }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (failure != null) throw new IllegalStateException("Server GUI check failed", failure);
            if (phase != Phase.START && seconds() > 180.0D) throw new IllegalStateException("GUI smoke timed out at " + phase);
            if (phase == Phase.START) {
                if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                if (mc.getOverlay() != null) return;
                require(!Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD)),
                        "Dedicated smoke world already exists; refusing to open or replace it");
                startedNanos = System.nanoTime();
                mc.options.pauseOnLostFocus = false;
                mc.options.onboardAccessibility = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.renderDistance().set(3);
                mc.options.simulationDistance().set(4);
                mc.options.hideGui = false;
                phase = Phase.WORLD;
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0x475549L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                                .value().createWorldDimensions(), mc.screen);
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            switch (phase) {
                case WORLD -> {
                    if (!queued) {
                        queued = true;
                        server(mc, player -> {
                            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                                player.serverLevel().setBlock(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState(), 3);
                            }
                            player.teleportTo(0.5D, 65.0D, 2.5D);
                            place(player, CHEMISTRY, ModBlocks.CHEMISTRY_TABLE.get());
                            place(player, ADVANCED, ModBlocks.ADVANCED_CHEMISTRY_TABLE.get());
                            place(player, BREW, ModBlocks.BREW_CAULDRON.get());
                            place(player, PRESS, ModBlocks.CANNABIS_PRESS.get());
                            place(player, MORTAR, ModBlocks.MORTAR_AND_PESTLE.get());
                            ((ChemistryTableBlockEntity) player.serverLevel().getBlockEntity(CHEMISTRY))
                                    .setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.AROMATIC_EXTRACT.get()));
                            player.getInventory().setItem(8, new ItemStack(Items.EMERALD_BLOCK, 64));
                            player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 6));
                            player.getInventory().setItem(10, ItemStack.EMPTY);
                            player.getInventory().setItem(11, ItemStack.EMPTY);
                            // Exercise the production bartender stock in Minecraft's unchanged merchant screen.
                            Villager bartender = new Villager(EntityType.VILLAGER, player.serverLevel());
                            BartenderService.configure(bartender, new BlockPos(3, 65, 2), new BlockPos(3, 65, 1));
                            MerchantOffers offers = new MerchantOffers();
                            bartender.getOffers().forEach(offer -> offers.add(offer.copy()));
                            require(offers.size() == 6, "Production Bartender stock did not contain six drink offers");
                            bartenderOffers = offers;
                            player.inventoryMenu.broadcastChanges();
                            fixtureReady = true;
                        });
                    }
                    if (!fixtureReady || !(mc.level.getBlockEntity(CHEMISTRY) instanceof ChemistryTableBlockEntity)
                            || !(mc.level.getBlockEntity(MORTAR) instanceof MortarAndPestleBlockEntity)
                            || mc.player.getInventory().getItem(9).getCount() != 6) return;
                    mc.setScreen(null);
                    clippingProbe(mc);
                    captureScreens(mc);
                    queued = false;
                    serverChecked = false;
                    phase = Phase.CHEMISTRY_MENU;
                    server(mc, player -> player.openMenu((ChemistryTableBlockEntity) player.serverLevel().getBlockEntity(CHEMISTRY),
                            buffer -> buffer.writeBlockPos(CHEMISTRY)));
                }
                case CHEMISTRY_MENU -> {
                    if (!(mc.screen instanceof ChemistryTableScreen screen) || !(mc.player.containerMenu instanceof ChemistryTableMenu menu)
                            || !menu.hasAromaticInput()) return;
                    // Smaller than this inventory forces fitting and catches input mapped differently from its slots.
                    screen.init(mc, 140, 120);
                    capture(mc, screen, 140, 120, "chemistry-scaled-native");
                    FittedGui view = viewport(screen);
                    double left = number(screen, "leftPos"), top = number(screen, "topPos");
                    require(screen.mouseClicked(view.screenX(left + 120), view.screenY(top + 91), 0), "Displayed 2C-B selector missed click");
                    phase = Phase.CHEMISTRY_SELECTION;
                }
                case CHEMISTRY_SELECTION -> {
                    require(mc.screen instanceof ChemistryTableScreen, "Native Chemistry menu closed before selection synchronized");
                    ChemistryTableScreen screen = (ChemistryTableScreen) mc.screen;
                    if (screen.getMenu().getSelectedAromaticRecipeId() != ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE) return;
                    if (!queuedCheck(mc, player -> require(((ChemistryTableMenu) player.containerMenu).getSelectedAromaticRecipeId()
                            == ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE, "Server did not receive scaled selector click"))) return;
                    FittedGui view = viewport(screen);
                    double left = number(screen, "leftPos"), top = number(screen, "topPos");
                    double sourceX = view.screenX(left + 16), targetX = view.screenX(left + 34), target2X = view.screenX(left + 52);
                    double y = view.screenY(top + 128);
                    screen.mouseClicked(sourceX, y, 0);
                    screen.mouseReleased(sourceX, y, 0);
                    require(screen.getMenu().getCarried().getCount() == 6, "Scaled slot pickup missed displayed emeralds");
                    screen.mouseClicked(targetX, y, 0);
                    screen.mouseDragged(targetX, y, 0, 0.0D, 0.0D);
                    screen.mouseDragged(target2X, y, 0, target2X - targetX, 0.0D);
                    screen.mouseReleased(target2X, y, 0);
                    queued = false;
                    serverChecked = false;
                    phase = Phase.SLOT_DRAG;
                }
                case SLOT_DRAG -> {
                    if (!mc.player.containerMenu.getCarried().isEmpty() || mc.player.getInventory().getItem(10).getCount() != 3
                            || mc.player.getInventory().getItem(11).getCount() != 3) return;
                    if (!queuedCheck(mc, player -> {
                        require(player.containerMenu.getCarried().isEmpty() && player.getInventory().getItem(9).isEmpty()
                                && player.getInventory().getItem(10).is(Items.EMERALD) && player.getInventory().getItem(10).getCount() == 3
                                && player.getInventory().getItem(11).is(Items.EMERALD) && player.getInventory().getItem(11).getCount() == 3,
                                "Scaled slot drag/release did not synchronize six emeralds as two stacks of three");
                    })) return;
                    log("PASS: Native scaled Chemistry click, slot pickup, two-slot drag and release synchronized with the integrated server");
                    queued = false;
                    serverChecked = false;
                    phase = Phase.ADVANCED_MENU;
                    server(mc, player -> player.openMenu((AdvancedChemistryTableBlockEntity) player.serverLevel().getBlockEntity(ADVANCED),
                            buffer -> buffer.writeBlockPos(ADVANCED)));
                }
                case ADVANCED_MENU -> {
                    if (!(mc.screen instanceof AdvancedChemistryTableScreen screen)) return;
                    screen.init(mc, 140, 120);
                    FittedGui view = viewport(screen);
                    require(screen.mouseClicked(view.screenX(number(screen, "leftPos") + 124),
                            view.screenY(number(screen, "topPos") + 26), 0), "Displayed 500 selector missed click");
                    phase = Phase.ADVANCED_SELECTION;
                }
                case ADVANCED_SELECTION -> {
                    if (!(mc.player.containerMenu instanceof AdvancedChemistryTableMenu menu) || menu.getSelectedProductId() != 3) return;
                    if (!queuedCheck(mc, player -> require(((AdvancedChemistryTableMenu) player.containerMenu).getSelectedProductId() == 3,
                            "Server did not receive scaled Advanced Chemistry dosage click"))) return;
                    log("PASS: Native scaled Advanced Chemistry dosage click synchronized with the integrated server");
                    finish(mc, true);
                }
                default -> { }
            }
        } catch (Throwable exception) {
            TrippyCraft.LOGGER.error("[GUI smoke] Native validation failed", exception);
            log("FAIL: " + exception);
            finish(mc, false);
        }
    }

    private static void captureScreens(Minecraft mc) throws Exception {
        var inventory = mc.player.getInventory();
        Set<String> unlocked = CursedFindingsRegistry.entries().stream().map(entry -> entry.id()).collect(Collectors.toSet());
        var offers = DealerRegistries.products().stream().limit(4).map(product -> new CustomerSnapshotPacket.OfferInfo(
                "gui-" + product.id(), product.id(), "daily", 8, 16, 1, 2, 0)).toList();
        for (int[] size : new int[][]{{640, 360}, {320, 240}, {427, 240}}) {
            MerchantMenu bartenderMenu = new MerchantMenu(0, inventory);
            bartenderMenu.setOffers(bartenderOffers);
            bartenderMenu.setMerchantLevel(1);
            List<Screen> screens = List.of(new VillageMarketScreen(new MarketSnapshotPacket(false, 0, 0, 0, 0, 24000L, List.of(), List.of())),
                    new DealerScreen(new DealerSnapshotPacket(false, 0, 0, 0)),
                    new CustomerDemandScreen(new CustomerSnapshotPacket(false, new UUID(0L, 1L), 0, 0, 0L, offers)),
                    new CursedFindingsScreen(unlocked),
                    new BrewCauldronScreen(new BrewCauldronMenu(0, inventory, (BrewCauldronBlockEntity) mc.level.getBlockEntity(BREW), new SimpleContainerData(4)), inventory, Component.literal("Brew Cauldron")),
                    new ChemistryTableScreen(new ChemistryTableMenu(0, inventory, (ChemistryTableBlockEntity) mc.level.getBlockEntity(CHEMISTRY), new SimpleContainerData(5)), inventory, Component.literal("Chemistry Table")),
                    new AdvancedChemistryTableScreen(new AdvancedChemistryTableMenu(0, inventory, (AdvancedChemistryTableBlockEntity) mc.level.getBlockEntity(ADVANCED), new SimpleContainerData(3)), inventory, Component.literal("Advanced Chemistry")),
                    new CannabisPressScreen(new CannabisPressMenu(0, inventory, (CannabisPressBlockEntity) mc.level.getBlockEntity(PRESS), new SimpleContainerData(4)), inventory, Component.literal("Cannabis Press")),
                    new MortarAndPestleScreen(new MortarAndPestleMenu(0, inventory, (MortarAndPestleBlockEntity) mc.level.getBlockEntity(MORTAR), new SimpleContainerData(2)), inventory, Component.literal("Mortar and Pestle")),
                    new TripScrubScreen(),
                    new MerchantScreen(bartenderMenu, inventory, Component.translatable("entity.trippycraft.bartender")));
            for (Screen screen : screens) {
                screen.init(mc, size[0], size[1]);
                checkPanel(screen, size[0], size[1]);
                if (screen instanceof DealerScreen dealer) checkDealer(dealer);
                if (screen instanceof VillageMarketScreen market) checkMarket(market);
                if (screen instanceof CursedFindingsScreen findings) checkFindings(findings);
                String screenName = screen instanceof MerchantScreen ? "BartenderMerchantScreen" : screen.getClass().getSimpleName();
                capture(mc, screen, size[0], size[1], screenName + "-" + size[0] + "x" + size[1]);
                if (screen instanceof DealerScreen dealer && size[0] == 427) captureTooltip(mc, dealer);
                checkScroll(screen);
            }
        }
        log("PASS: All ten mod screens and the vanilla Bartender merchant screen with six production offers captured at 640x360, 320x240 and 427x240; displayed category/tab/search/scroll targets matched input");
        require(FittedGui.fit(1280, 720, 708, 390).scale() == 1.0D, "Full-size desktop canvas unexpectedly scaled");
    }

    private static void checkPanel(Screen screen, int width, int height) throws Exception {
        FittedGui view = viewport(screen);
        if (screen instanceof AbstractContainerScreen<?> container) {
            double left = number(screen, "leftPos"), top = number(screen, "topPos");
            require(view.screenX(left) >= 0 && view.screenY(top) >= 0
                    && view.screenX(left + number(screen, "imageWidth")) <= width + 1
                    && view.screenY(top + number(screen, "imageHeight")) <= height + 1, "Inventory extends outside the GUI canvas");
            require(container.getMenu().slots.stream().allMatch(slot -> slot.x >= 0 && slot.y >= 0), "Invalid native slot fixture");
        } else if (!(screen instanceof TripScrubScreen)) {
            Object layout = invoke(screen, "layout");
            double left = number(layout, "panelX"), top = number(layout, "panelY");
            require(view.screenX(left) >= 0 && view.screenY(top) >= 0
                    && view.screenX(left + number(layout, "panelWidth")) <= width + 1
                    && view.screenY(top + number(layout, "panelHeight")) <= height + 1, "Panel extends outside the GUI canvas");
            if (screen instanceof CustomerDemandScreen) {
                require(view.screenY(number(layout, "bodyY") + 31 + 3 * 58 + 54) <= height,
                        "Fourth customer offer extends outside the GUI canvas");
            }
        } else {
            for (var child : screen.children()) if (child instanceof AbstractWidget widget) {
                require(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= width
                        && widget.getY() + widget.getHeight() <= height, "Trip timeline widget extends outside the GUI canvas");
            }
        }
    }

    private static void checkDealer(DealerScreen screen) throws Exception {
        Object layout = invoke(screen, "layout");
        FittedGui view = viewport(screen);
        Object bounds = invoke(layout, "categoryViewport");
        require(screen.mouseClicked(view.screenX(number(bounds, "x") + 10), view.screenY(number(bounds, "y") + 28), 0), "Displayed Dealer category missed click");
        require(!"All".equals(field(screen, "selectedCategory")), "Dealer category input selected the wrong row");
        Object search = invoke(layout, "searchBounds");
        require(screen.mouseClicked(view.screenX(number(search, "x") + 8), view.screenY(number(search, "y") + 8), 0), "Displayed Dealer search missed click");
        require(screen.charTyped('a', 0) && "a".equals(field(screen, "search")), "Dealer search did not receive keyboard input");
        set(screen, "search", "");
        set(screen, "selectedCategory", "All");
    }

    private static void checkMarket(VillageMarketScreen screen) throws Exception {
        Object layout = invoke(screen, "layout");
        FittedGui view = viewport(screen);
        double tabWidth = (number(layout, "panelWidth") - 20) / 4;
        require(screen.mouseClicked(view.screenX(number(layout, "panelX") + 10 + tabWidth + 8),
                view.screenY(number(layout, "panelY") + 40), 0), "Displayed market Buy tab missed click");
        require("BUY".equals(field(screen, "selectedTab").toString()), "Market input selected the wrong tab");
        double x = number(layout, "contentX") + number(layout, "contentWidth") - 158 + 8;
        double y = number(layout, "contentY") + 13;
        require(screen.mouseClicked(view.screenX(x), view.screenY(y), 0) && screen.charTyped('o', 0)
                && "o".equals(field(screen, "materialSearch")), "Market search input missed its displayed target");
        set(screen, "materialSearch", "");
        set(screen, "buyScroll", 18);
        double viewportX = number(layout, "contentX") + 7;
        double viewportWidth = number(layout, "contentWidth") - 14;
        double clippedX = viewportX + viewportWidth - 149 + 10;
        double clippedY = number(layout, "contentY") + 45 - 4;
        require(!screen.mouseClicked(view.screenX(clippedX), view.screenY(clippedY), 0), "Hidden market button accepted a click above its clipped viewport");
        set(screen, "buyScroll", 0);
    }

    private static void checkFindings(CursedFindingsScreen screen) throws Exception {
        Object layout = invoke(screen, "layout");
        FittedGui view = viewport(screen);
        require(screen.mouseClicked(view.screenX(number(layout, "categoryX") + 12),
                view.screenY(number(layout, "categoryY") + 18 + 22 + 8), 0), "Displayed findings category missed click");
        require(!"PSYCHEDELICS".equals(field(screen, "selectedCategory").toString()), "Findings input selected the wrong category");
        screen.mouseClicked(view.screenX(number(layout, "categoryX") + 12), view.screenY(number(layout, "categoryY") + 26), 0);
        require(screen.mouseClicked(view.screenX(number(layout, "entryX") + 12),
                view.screenY(number(layout, "entryY") + 27), 0), "Displayed findings entry missed click");
        require(field(screen, "selectedEntryId") != null, "Findings entry was not selected");
        double tabWidth = Math.max(46, ((int) number(layout, "contentWidth") - 20) / 5);
        require(screen.mouseClicked(view.screenX(number(layout, "contentX") + 10 + tabWidth + 8),
                view.screenY(number(layout, "contentY") + 55), 0)
                && "EFFECTS".equals(field(screen, "selectedTab").toString()), "Displayed findings Effects tab missed click");
    }

    private static void checkScroll(Screen screen) throws Exception {
        if (!(screen instanceof DealerScreen) && !(screen instanceof VillageMarketScreen) && !(screen instanceof CursedFindingsScreen)) return;
        Object layout = invoke(screen, "layout");
        FittedGui view = viewport(screen);
        double x, y;
        String scroll;
        if (screen instanceof DealerScreen) {
            x = number(layout, "catalogX") + 20;
            y = number(layout, "bodyY") + 80;
            scroll = "scroll";
        } else {
            x = number(layout, "contentX") + 20;
            y = number(layout, "contentY") + 100;
            scroll = screen instanceof VillageMarketScreen ? "buyScroll" : "contentScroll";
        }
        require(screen.mouseScrolled(view.screenX(x), view.screenY(y), 0.0D, -1.0D) && number(screen, scroll) > 0,
                "Displayed " + screen.getClass().getSimpleName() + " scroll region missed input");
        require(!screen.mouseScrolled(-10.0D, -10.0D, 0.0D, -1.0D), "Off-canvas scrolling was accepted");
    }

    private static void captureTooltip(Minecraft mc, DealerScreen screen) throws Exception {
        Object layout = invoke(screen, "layout");
        FittedGui view = viewport(screen);
        double x = number(layout, "catalogX") + 7 + 6 + 8;
        double clipTop = number(layout, "bodyY") + 52;
        int mouseX = (int) Math.ceil(view.screenX(x)), mouseY = (int) Math.ceil(view.screenY(clipTop + 9));
        canvas(mc, 427, 240, new int[]{0, 0, 1280, 720}, graphics -> screen.render(graphics, mouseX, mouseY, 0.0F), "dealer-tooltip-edge-427x240");
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        try (NativeImage before = NativeImage.read(Files.readAllBytes(directory.resolve("DealerScreen-427x240.png")));
             NativeImage after = NativeImage.read(Files.readAllBytes(directory.resolve("dealer-tooltip-edge-427x240.png")))) {
            int left = (int) Math.ceil(view.screenX(x + 12) * 1280 / 427);
            int right = Math.min(1279, (int) Math.floor(view.screenX(x + 110) * 1280 / 427));
            int top = (int) Math.ceil(view.screenY(clipTop - 6) * 720 / 240);
            int bottom = (int) Math.floor(view.screenY(clipTop - 1) * 720 / 240);
            int changed = 0;
            for (int py = top; py < bottom; py++) for (int px = left; px < right; px++) {
                if (before.getPixelRGBA(px, py) != after.getPixelRGBA(px, py)) changed++;
            }
            require(changed > 10, "Item tooltip disappeared above the catalog scissor boundary");
        }
        log("PASS: Hovered item tooltip pixels remain visible outside the catalog scissor boundary");
    }

    private static void capture(Minecraft mc, Screen screen, int width, int height, String name) throws Exception {
        canvas(mc, width, height, new int[]{0, 0, 1280, 720}, graphics -> screen.render(graphics, -100, -100, 0.0F), name);
    }

    private static void clippingProbe(Minecraft mc) throws Exception {
        int[] viewport = {73, 41, 1011, 613};
        FittedGui view = FittedGui.fit(320, 240, 640, 360);
        canvas(mc, 320, 240, viewport, source -> {
            // Positive reproduction of the original defect, using actual Minecraft GuiGraphics.
            source.pose().pushPose();
            source.pose().scale(0.5F, 0.5F, 1.0F);
            source.enableScissor(100, 100, 180, 160);
            source.fill(0, 0, 640, 480, 0xFF00FF00);
            source.flush();
            pixel(view, viewport, 120, 120, 0, 0, 0, 0);
            source.disableScissor();
            source.pose().popPose();
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableScissor(10, 20, 1200, 660);
            int[] previous = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, previous);
            RenderSystem.enableDepthTest();
            try (var drawing = view.begin(source)) {
                GuiGraphics graphics = drawing.graphics();
                graphics.enableScissor(60, 80, 260, 200);
                graphics.fill(0, 0, view.width(), view.height(), 0xFFFF0000);
                graphics.enableScissor(100, 100, 180, 160);
                graphics.fill(0, 0, view.width(), view.height(), 0xFF00FF00);
                require(graphics.containsPointInScissor(120, 120) && !graphics.containsPointInScissor(80, 120), "Nested GUI hit clipping disagrees with visible clip");
                graphics.disableScissor();
                graphics.fill(190, 100, 220, 130, 0xFF0000FF);
                graphics.disableScissor();
            }
            int[] restored = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, restored);
            require(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) && Arrays.equals(previous, restored)
                    && GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "GUI drawing leaked scissor or depth state");
            pixel(view, viewport, 120, 120, 0, 255, 0, 255);
            pixel(view, viewport, 80, 120, 255, 0, 0, 255);
            pixel(view, viewport, 205, 115, 0, 0, 255, 255);
            pixel(view, viewport, 40, 120, 0, 0, 0, 0);
            pixel(view, viewport, 120, 220, 0, 0, 0, 0);
            RenderSystem.disableScissor();
        }, "nested-scissor-offset-viewport");
        log("PASS: Actual vanilla GuiGraphics reproduces the original window-based clipping defect; fitted real GL pixels prove nested scissor mapping with shifted viewport, unequal framebuffer scales and window GUI scale 3; clipping/depth restored");
    }

    private static void pixel(FittedGui view, int[] viewport, double x, double y, int r, int g, int b, int a) {
        int px = viewport[0] + (int) Math.floor(view.screenX(x) * viewport[2] / 320);
        int py = viewport[1] + (int) Math.floor((240 - view.screenY(y)) * viewport[3] / 240);
        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        try {
            GL11.glReadPixels(px, py, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
            int[] actual = {Byte.toUnsignedInt(pixel.get(0)), Byte.toUnsignedInt(pixel.get(1)), Byte.toUnsignedInt(pixel.get(2)), Byte.toUnsignedInt(pixel.get(3))};
            require(Arrays.equals(actual, new int[]{r, g, b, a}), "Scissor pixel mismatch at " + x + "," + y + ": " + Arrays.toString(actual));
        } finally { MemoryUtil.memFree(pixel); }
    }

    private static void canvas(Minecraft mc, int width, int height, int[] viewport, Consumer<GuiGraphics> draw, String name) throws Exception {
        TextureTarget target = null;
        try (RenderState state = new RenderState(mc)) {
            target = new TextureTarget(1280, 720, true, Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            mc.getWindow().setGuiScale(3.0D);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().translation(0.0F, 0.0F, -11000.0F);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
                @Override public int guiWidth() { return width; }
                @Override public int guiHeight() { return height; }
            };
            draw.accept(graphics);
            graphics.flush();
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error while drawing " + name);
            var output = mc.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png");
            Files.createDirectories(output.getParent());
            require(!Files.exists(output), "Capture exists in fresh smoke directory: " + name);
            try (NativeImage image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(output);
            }
            screenshots++;
        } finally { if (target != null) target.destroyBuffers(); }
    }

    private static FittedGui viewport(Screen screen) throws Exception {
        if (screen instanceof FittedContainerScreen<?> container) return container.viewport();
        if (screen instanceof TripScrubScreen || screen instanceof MerchantScreen) {
            return FittedGui.fit(screen.width, screen.height, screen.width, screen.height);
        }
        return (FittedGui) invoke(screen, "viewport");
    }

    private static Object invoke(Object object, String name) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(object);
    }

    private static Field reflectedField(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object field(Object object, String name) throws Exception { return reflectedField(object, name).get(object); }
    private static void set(Object object, String name, Object value) throws Exception { reflectedField(object, name).set(object, value); }
    private static double number(Object object, String name) throws Exception { return ((Number) field(object, name)).doubleValue(); }
    private static double seconds() { return (System.nanoTime() - startedNanos) / 1_000_000_000.0D; }

    private static boolean queuedCheck(Minecraft mc, Consumer<ServerPlayer> check) {
        if (!queued) { queued = true; server(mc, player -> { check.accept(player); serverChecked = true; }); }
        return serverChecked;
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        UUID id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                require(player != null, "Integrated player disappeared");
                action.accept(player);
            } catch (Throwable exception) { failure = exception; }
        });
    }

    private static void place(ServerPlayer player, BlockPos pos, Block block) {
        require(player.serverLevel().getBlockState(pos).isAir(), "Fixture position is occupied");
        require(player.serverLevel().setBlock(pos, block.defaultBlockState(), 3), "Could not place fixture");
    }

    private static void finish(Minecraft mc, boolean pass) {
        if (phase == Phase.DONE) return;
        phase = Phase.DONE;
        log((pass ? "PASS" : "FAIL") + ": Native GUI smoke; screenshots=" + screenshots
                + "; synthetic framebuffer/GUI input coverage; physical headset comfort and controller aiming untested");
        try { Files.writeString(mc.gameDirectory.toPath().resolve("gui-smoke-result.txt"), String.join(System.lineSeparator(), REPORT)); }
        catch (Exception exception) { TrippyCraft.LOGGER.error("[GUI smoke] Could not save result", exception); }
        mc.stop();
    }

    private static void log(String message) { REPORT.add(message); TrippyCraft.LOGGER.info("[GUI smoke] {}", message); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private enum Phase { START, WORLD, CHEMISTRY_MENU, CHEMISTRY_SELECTION, SLOT_DRAG, ADVANCED_MENU, ADVANCED_SELECTION, DONE }

    private static final class RenderState implements AutoCloseable {
        private final Minecraft mc;
        private final double guiScale;
        private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        private final VertexSorting sorting = RenderSystem.getVertexSorting();
        private final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int[] viewport = new int[4], scissor = new int[4];
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), clip = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final float[] color = RenderSystem.getShaderColor().clone(), clear = new float[4];

        private RenderState(Minecraft mc) {
            this.mc = mc;
            guiScale = mc.getWindow().getGuiScale();
            mc.renderBuffers().bufferSource().endBatch();
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clear);
            RenderSystem.getModelViewStack().pushMatrix();
        }

        @Override public void close() {
            mc.renderBuffers().bufferSource().endBatch();
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.clearColor(clear[0], clear[1], clear[2], clear[3]);
            mc.getWindow().setGuiScale(guiScale);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (clip) RenderSystem.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]); else RenderSystem.disableScissor();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        }
    }
}
