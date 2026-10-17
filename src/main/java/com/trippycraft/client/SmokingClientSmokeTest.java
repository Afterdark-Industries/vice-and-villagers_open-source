package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.guide.CursedFindingCategory;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingTab;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Native item/hand/guide assets in a new disposable world, without dosing or enabling realm rendering. */
public final class SmokingClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.smokingSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "smoking-assets-smoke";
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static final List<String> REPORT = new ArrayList<>();
    private static final String[] LOAD_NAMES = {"salvia-1x", "salvia-10x", "salvia-25x", "salvia-50x", "cannabis", "dmt"};
    private static Phase phase = Phase.START;
    private static long started, poseReady, renderedScreens, requiredScreen;
    private static int pose;
    private static volatile boolean sceneReady;
    private static volatile String failure;

    private SmokingClientSmokeTest() { }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (ENABLED && phase != Phase.DONE) renderedScreens++;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (failure != null) throw new IllegalStateException(failure);
            if (started != 0 && (System.nanoTime() - started) / 1e9 > 180) throw new IllegalStateException("Smoking smoke timed out at " + phase);
            switch (phase) {
                case START -> {
                    if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                    if (mc.getOverlay() != null) return;
                    require(!Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))
                            && !Files.exists(mc.gameDirectory.toPath().resolve("smoking-smoke-result.txt")), "Use a fresh smokingSmokeDir");
                    started = System.nanoTime();
                    mc.options.pauseOnLostFocus = false;
                    mc.options.onboardAccessibility = false;
                    mc.options.tutorialStep = TutorialSteps.NONE;
                    mc.options.renderDistance().set(3);
                    mc.options.simulationDistance().set(5);
                    mc.options.guiScale().set(2);
                    mc.options.fov().set(65);
                    phase = Phase.WORLD;
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                                    new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(0x534D4F4B494E47L, false, false),
                            registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                    .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                case WORLD -> {
                    if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
                    validateModels(mc);
                    server(mc, player -> { buildScene(player); sceneReady = true; });
                    phase = Phase.SCENE;
                }
                case SCENE -> {
                    if (!sceneReady || mc.player.distanceToSqr(.5, 65, 2.5) > 4) return;
                    for (String recipe : List.of("ancient_pipe", "ancient_pipe_dmt", "bong_dmt")) {
                        if (!mc.player.getRecipeBook().contains(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, recipe))) return;
                    }
                    log("PASS: actual inventory changes discovered pipe/DMT loading recipes and synchronized the native client recipe book");
                    phase = Phase.POSES;
                }
                case POSES -> {
                    mc.player.setYRot(0);
                    mc.player.setXRot(12);
                    if (!mc.player.getMainHandItem().is(pipes().get(pose)) || !mc.player.getOffhandItem().is(bongs().get(pose))) return;
                    if (poseReady == 0) poseReady = System.nanoTime();
                    if ((System.nanoTime() - poseReady) / 1e9 < .7) return;
                    capture(mc, "smoking-held-" + LOAD_NAMES[pose] + ".png");
                    pose++;
                    poseReady = 0;
                    if (pose < pipes().size()) server(mc, player -> setHands(player, pose));
                    else {
                        log("PASS: native first-person Ancient Pipe and Bong models rendered for DMT, cannabis and all four Salvia strengths");
                        mc.setScreen(new InventoryScreen(mc.player));
                        rendered(Phase.INVENTORY);
                    }
                }
                case INVENTORY -> {
                    if (renderedScreens < requiredScreen) return;
                    capture(mc, "smoking-item-chain.png");
                    mc.setScreen(null);
                    server(mc, CursedRecipeCodex::openJournal);
                    phase = Phase.UTILITY;
                }
                case UTILITY -> {
                    if (!(mc.screen instanceof CursedFindingsScreen screen)) return;
                    require(((Set<?>) field(screen, "unlockedFindings")).contains(CursedFindingIds.SMOKING_DEVICES),
                            "Server did not expose the always-available Smoking Devices chapter");
                    selectGuide(screen, CursedFindingCategory.UTILITIES, CursedFindingIds.SMOKING_DEVICES);
                    rendered(Phase.UTILITY_CAPTURE);
                }
                case UTILITY_CAPTURE -> {
                    if (renderedScreens < requiredScreen) return;
                    capture(mc, "smoking-utilities-recipes.png");
                    mc.setScreen(null);
                    server(mc, player -> { CursedRecipeCodex.unlockFinding(player, CursedFindingIds.DMT); CursedRecipeCodex.openJournal(player); });
                    phase = Phase.DMT;
                }
                case DMT -> {
                    if (!(mc.screen instanceof CursedFindingsScreen screen)
                            || !((Set<?>) field(screen, "unlockedFindings")).contains(CursedFindingIds.DMT)) return;
                    selectGuide(screen, CursedFindingCategory.PSYCHEDELICS, CursedFindingIds.DMT);
                    rendered(Phase.DMT_CAPTURE);
                }
                case DMT_CAPTURE -> {
                    if (renderedScreens < requiredScreen) return;
                    capture(mc, "smoking-dmt-recipes.png");
                    log("PASS: actual server-opened journal permits native Utilities/Smoking Devices and DMT category, entry and recipe-tab navigation");
                    phase = Phase.CAPTURES;
                }
                case CAPTURES -> {
                    if (CAPTURES.get() < 9) return;
                    log("PASS: Smoking tools client smoke; screenshots=" + CAPTURES.get());
                    finish(mc);
                }
                default -> { }
            }
        } catch (Exception exception) {
            log("FAIL: " + exception);
            TrippyCraft.LOGGER.error("[Smoking smoke] Native item/guide validation failed", exception);
            finish(mc);
        }
    }

    private static void validateModels(Minecraft mc) {
        var shaper = mc.getItemRenderer().getItemModelShaper();
        for (Item item : chain()) {
            var model = shaper.getItemModel(new ItemStack(item));
            require(model != shaper.getModelManager().getMissingModel()
                            && !model.getParticleIcon().contents().name().toString().equals("minecraft:missingno")
                            && !model.getQuads(null, null, RandomSource.create(17)).isEmpty(),
                    "Missing or empty smoking item model/texture: " + item);
            String name = new ItemStack(item).getHoverName().getString();
            require(!name.startsWith("item.trippycraft."), "Missing translated item name: " + item);
        }
        log("PASS: all seventeen powder/crystal/device item models have native textured meshes and localized names");
    }

    private static void buildScene(ServerPlayer player) {
        var level = player.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
        level.setDayTime(6000);
        for (int x = -5; x <= 5; x++) for (int z = -2; z <= 10; z++) {
            level.setBlock(new BlockPos(x, 64, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            for (int y = 65; y <= 68; y++) level.setBlock(new BlockPos(x, y, z),
                    z == 10 ? Blocks.ACACIA_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
        player.getInventory().clearContent();
        List<Item> icons = chain();
        for (int slot = 0; slot < icons.size(); slot++) player.getInventory().setItem(slot + 9, new ItemStack(icons.get(slot)));
        player.getInventory().setItem(4, new ItemStack(Items.IRON_INGOT));
        player.getInventory().setItem(5, new ItemStack(Items.OAK_PLANKS));
        player.getInventory().setItem(6, new ItemStack(ModItems.ACACIA_BARK.get()));
        player.inventoryMenu.broadcastChanges();
        for (String recipe : List.of("ancient_pipe", "ancient_pipe_dmt", "bong_dmt")) {
            require(player.getRecipeBook().contains(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, recipe)),
                    "Real inventory changes did not discover recipe " + recipe);
        }
        setHands(player, 0);
        player.teleportTo(level, .5, 65, 2.5, 0, 12);
    }

    private static void setHands(ServerPlayer player, int variant) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(pipes().get(variant)));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(bongs().get(variant)));
        player.inventoryMenu.broadcastChanges();
    }

    private static void selectGuide(CursedFindingsScreen screen, CursedFindingCategory category, String id) throws Exception {
        Object layout = invoke(screen, "layout");
        FittedGui viewport = (FittedGui) invoke(screen, "viewport");
        int categoryIndex = CursedFindingsRegistry.categories().indexOf(category);
        require(screen.mouseClicked(viewport.screenX(number(layout, "categoryX") + 12),
                viewport.screenY(number(layout, "categoryY") + 18 + categoryIndex * 22 + 8), 0), "Displayed guide category missed click");
        var entries = CursedFindingsRegistry.entriesFor(category);
        int index = 0;
        while (index < entries.size() && !entries.get(index).id().equals(id)) index++;
        require(index < entries.size(), "Missing guide entry " + id);
        screen.mouseScrolled(viewport.screenX(number(layout, "entryX") + 20),
                viewport.screenY(number(layout, "entryY") + 50), 0, -index);
        double row = number(layout, "entryY") + 18 + index * 29 - number(screen, "entryScroll") + 12;
        require(screen.mouseClicked(viewport.screenX(number(layout, "entryX") + 12), viewport.screenY(row), 0)
                && id.equals(field(screen, "selectedEntryId")), "Displayed guide entry missed click: " + id);
        double tabWidth = Math.max(46, ((int) number(layout, "contentWidth") - 20) / CursedFindingTab.values().length);
        require(screen.mouseClicked(viewport.screenX(number(layout, "contentX") + 10 + 3 * tabWidth + 8),
                viewport.screenY(number(layout, "contentY") + 55), 0)
                && field(screen, "selectedTab") == CursedFindingTab.RECIPES, "Displayed guide recipe tab missed click");
    }

    private static List<Item> pipes() { return List.of(ModItems.ANCIENT_PIPE_SALVIA_1X.get(), ModItems.ANCIENT_PIPE_SALVIA_10X.get(), ModItems.ANCIENT_PIPE_SALVIA_25X.get(), ModItems.ANCIENT_PIPE_SALVIA_50X.get(), ModItems.ANCIENT_PIPE_CANNABIS.get(), ModItems.ANCIENT_PIPE_DMT.get()); }
    private static List<Item> bongs() { return List.of(ModItems.BONG_SALVIA_1X.get(), ModItems.BONG_SALVIA_10X.get(), ModItems.BONG_SALVIA_25X.get(), ModItems.BONG_SALVIA_50X.get(), ModItems.BONG_CANNABIS.get(), ModItems.BONG_DMT.get()); }
    private static List<Item> chain() { List<Item> result = new ArrayList<>(List.of(ModItems.DMT.get(), ModItems.DMT_5X.get(), ModItems.DMT_POWDER.get(), ModItems.ANCIENT_PIPE.get(), ModItems.BONG.get())); result.addAll(pipes()); result.addAll(bongs()); return result; }
    private static Object invoke(Object target, String name) throws Exception { Method method = target.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(target); }
    private static Object field(Object target, String name) throws Exception { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
    private static double number(Object target, String name) throws Exception { return ((Number) field(target, name)).doubleValue(); }
    private static void rendered(Phase next) { phase = next; requiredScreen = renderedScreens + 1; }
    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> { try { action.accept(server.getPlayerList().getPlayer(id)); } catch (Exception exception) { failure = exception.toString(); } });
    }
    private static void capture(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> { if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(name))) CAPTURES.incrementAndGet(); else failure = "Screenshot failed: " + message.getString(); }); }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static void log(String message) { REPORT.add(message); TrippyCraft.LOGGER.info("[Smoking smoke] {}", message); }
    private static void finish(Minecraft mc) { phase = Phase.DONE; try { Files.write(mc.gameDirectory.toPath().resolve("smoking-smoke-result.txt"), REPORT); } catch (Exception exception) { TrippyCraft.LOGGER.error("Cannot write smoking smoke report", exception); } mc.stop(); }
    private enum Phase { START, WORLD, SCENE, POSES, INVENTORY, UTILITY, UTILITY_CAPTURE, DMT, DMT_CAPTURE, CAPTURES, DONE }
}
