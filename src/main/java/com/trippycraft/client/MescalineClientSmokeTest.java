package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.guide.CursedFindingCategory;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingTab;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.registry.ModBlocks;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in visual check in a fresh isolated development world. */
public final class MescalineClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.mescalineSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "mescaline-assets-smoke";
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static boolean started;
    private static boolean queued;
    private static volatile boolean ready;
    private static volatile String failure;
    private static boolean finished;
    private static long start;
    private static long sceneStart;
    private static int stage;

    private MescalineClientSmokeTest() { }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!started) {
                if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                if (mc.getOverlay() != null) return;
                started = true;
                start = System.nanoTime();
                if (Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
                    throw new IllegalStateException("Dedicated smoke world already exists; choose a fresh mescalineSmokeDir");
                }
                mc.options.pauseOnLostFocus = false;
                mc.options.onboardAccessibility = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.hideGui = false;
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                mc.options.guiScale().set(2);
                mc.options.fov().set(60);
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(0x4D455343414CL, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                return;
            }
            if (failure != null) throw new IllegalStateException(failure);
            if (elapsed(start) > 180.0D) throw new IllegalStateException("Timed out waiting for Mescaline asset check");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            var server = mc.getSingleplayerServer();
            if (!queued) {
                queued = true;
                var playerId = mc.player.getUUID();
                server.execute(() -> {
                    try {
                        buildScene(server.getPlayerList().getPlayer(playerId));
                        ready = true;
                    } catch (Exception exception) {
                        failure = exception.toString();
                    }
                });
                return;
            }
            if (!ready || mc.player.distanceToSqr(0.5D, 65.0D, 2.5D) > 16.0D) return;
            if (sceneStart == 0L) {
                validateModels(mc);
                sceneStart = System.nanoTime();
                mc.setScreen(null);
            }
            mc.player.setYRot(0.0F);
            mc.player.setXRot(17.0F);
            double seconds = elapsed(sceneStart);
            if (stage == 0 && seconds > 4.0D) {
                capture(mc, "mescaline-cactus.png");
                stage = 1;
            }
            if (stage == 1 && seconds > 5.0D) {
                mc.setScreen(new InventoryScreen(mc.player));
                stage = 2;
            }
            if (stage == 2 && seconds > 7.0D) {
                capture(mc, "mescaline-item-chain.png");
                stage = 3;
            }
            if (stage == 3 && seconds > 8.0D) {
                openGuide(mc, true);
                stage = 4;
            }
            if (stage == 4 && seconds > 10.0D) {
                capture(mc, "mescaline-findings-scroll.png");
                stage = 5;
            }
            if (stage == 5 && seconds > 11.0D) {
                openGuide(mc, false);
                stage = 6;
            }
            if (stage == 6 && seconds > 13.0D) {
                capture(mc, "mescaline-cursed-findings.png");
                stage = 7;
            }
            if (stage == 7 && CAPTURES.get() == 4 && seconds > 15.0D) finish(mc);
        } catch (Exception exception) {
            failure = exception.toString();
            TrippyCraft.LOGGER.error("Mescaline asset check failed", exception);
            finish(mc);
        }
    }

    private static List<Item> chain() {
        return List.of(ModItems.MESCALINE_CACTUS.get(), ModItems.MESCALINE_25MG.get(), ModItems.MESCALINE_100MG.get(),
                ModItems.MESCALINE_250MG.get(), ModItems.MESCALINE_500MG.get());
    }
    private static void buildScene(ServerPlayer player) {
        if (player == null) throw new IllegalStateException("Missing smoke player");
        var level = player.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, player.server);
        level.setDayTime(6000L);
        for (int x = -5; x <= 5; x++) {
            for (int z = 0; z <= 9; z++) {
                for (int y = 64; y <= 68; y++) {
                    level.setBlock(new BlockPos(x, y, z),
                            (y == 64 ? Blocks.GRASS_BLOCK : z == 9 ? Blocks.SPRUCE_PLANKS : Blocks.AIR).defaultBlockState(), 3);
                }
                level.setBlock(new BlockPos(x, 63, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        for (int x : new int[]{-2, 0, 2}) {
            BlockPos pos = new BlockPos(x, 65, 6);
            level.setBlock(pos.below(), (x < 0 ? Blocks.SAND : x == 0 ? Blocks.RED_SAND : Blocks.TERRACOTTA).defaultBlockState(), 3);
            level.setBlock(pos, ModBlocks.MESCALINE_CACTUS.get().defaultBlockState(), 3);
        }
        // Keep all pipeline icons together in the visible main inventory.
        List<Item> items = chain();
        for (int slot = 0; slot < items.size(); slot++) player.getInventory().setItem(slot + 9, new ItemStack(items.get(slot)));
        player.getInventory().setItem(0, new ItemStack(ModItems.MESCALINE_CACTUS.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.MESCALINE_100MG.get()));
        player.teleportTo(level, 0.5D, 65.0D, 2.5D, 0.0F, 17.0F);
    }

    private static void validateModels(Minecraft mc) {
        var shaper = mc.getItemRenderer().getItemModelShaper();
        for (Item item : chain()) {
            var model = shaper.getItemModel(new ItemStack(item));
            if (model == shaper.getModelManager().getMissingModel()
                    || model.getParticleIcon().contents().name().toString().equals("minecraft:missingno")) {
                throw new IllegalStateException("Missing Mescaline item model or texture: " + item);
            }
        }
        var state = ModBlocks.MESCALINE_CACTUS.get().defaultBlockState();
        var blockModel = mc.getBlockRenderer().getBlockModel(state);
        if (blockModel == shaper.getModelManager().getMissingModel()
                || blockModel.getParticleIcon().contents().name().toString().equals("minecraft:missingno")) {
            throw new IllegalStateException("Missing Mescaline cactus model or texture");
        }
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(name))) CAPTURES.incrementAndGet();
            else failure = "Screenshot failed: " + message.getString();
        });
    }

    private static void openGuide(Minecraft mc, boolean scrollToLast) {
        var guide = new CursedFindingsScreen(Set.of(CursedFindingIds.MESCALINE, CursedFindingIds.SALVIA));
        mc.setScreen(guide);
        // Use the guide's normal mouse handlers to select the chapter and recipe tab.
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int panelWidth = Math.max(Math.min(width - 18, 642), 430);
        int panelHeight = Math.max(Math.min(height - 20, 348), 254);
        int panelX = (width - panelWidth) / 2;
        int bodyY = (height - panelHeight) / 2 + 38;
        int categoryWidth = Math.min(136, Math.max(108, panelWidth / 5));
        int entryWidth = Math.min(162, Math.max(124, panelWidth / 4));
        int entryX = panelX + 10 + categoryWidth + 7;
        var entries = CursedFindingsRegistry.entriesFor(CursedFindingCategory.PSYCHEDELICS);
        String target = scrollToLast ? CursedFindingIds.SALVIA : CursedFindingIds.MESCALINE;
        int findingRow = -1;
        for (int row = 0; row < entries.size(); row++) {
            if (entries.get(row).id().equals(target)) findingRow = row;
        }
        if (findingRow < 0) throw new IllegalStateException("Missing chapter from the guide: " + target);
        int scroll = 0;
        if (scrollToLast) {
            scroll = Math.max(0, entries.size() * 29 - 4 - (panelHeight - 48 - 23));
            if (scroll > 0 && !guide.mouseScrolled(entryX + 15, bodyY + 40, 0.0D, -20.0D)) {
                throw new IllegalStateException("The overflowing findings list cannot scroll");
            }
        }
        if (!guide.mouseClicked(entryX + 15, bodyY + 18 + findingRow * 29 - scroll + 10, 0)) {
            throw new IllegalStateException("The findings list cannot select " + target);
        }
        int contentX = entryX + entryWidth + 7;
        int contentWidth = panelX + panelWidth - contentX - 10;
        int tabWidth = Math.max(46, (contentWidth - 20) / CursedFindingTab.values().length);
        if (!guide.mouseClicked(contentX + 10 + CursedFindingTab.RECIPES.ordinal() * tabWidth + 10, bodyY + 55, 0)) {
            throw new IllegalStateException("The guide cannot open the recipe tab");
        }
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        String report = failure == null ? "PASS: Mescaline client assets; screenshots=" + CAPTURES.get()
                : "FAIL: " + failure;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("mescaline-smoke-result.txt"), report + System.lineSeparator());
        } catch (Exception exception) {
            TrippyCraft.LOGGER.error("Could not save Mescaline asset report", exception);
        }
        TrippyCraft.LOGGER.info(report);
        mc.stop();
    }

    private static double elapsed(long time) {
        return (System.nanoTime() - time) / 1_000_000_000.0D;
    }
}
