package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.block.SalviaDivinorumPlantBlock;
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
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in visual check in a fresh isolated development world. */
public final class SalviaClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.salviaSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "salvia-assets-smoke";
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static boolean started;
    private static boolean queued;
    private static volatile boolean ready;
    private static volatile String failure;
    private static boolean finished;
    private static long start;
    private static long sceneStart;
    private static int stage;

    private SalviaClientSmokeTest() { }

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
                    throw new IllegalStateException("Dedicated smoke world already exists; choose a fresh salviaSmokeDir");
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
                        new WorldOptions(0x53414C564941L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                return;
            }
            if (failure != null) throw new IllegalStateException(failure);
            if (elapsed(start) > 180.0D) throw new IllegalStateException("Timed out waiting for Salvia asset check");
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
                capture(mc, "salvia-growth-stages.png");
                stage = 1;
            }
            if (stage == 1 && seconds > 5.0D) {
                mc.setScreen(new InventoryScreen(mc.player));
                stage = 2;
            }
            if (stage == 2 && seconds > 7.0D) {
                capture(mc, "salvia-item-chain.png");
                stage = 3;
            }
            if (stage == 3 && CAPTURES.get() == 2 && seconds > 9.0D) finish(mc);
        } catch (Exception exception) {
            failure = exception.toString();
            TrippyCraft.LOGGER.error("Salvia asset check failed", exception);
            finish(mc);
        }
    }

    private static List<Item> chain() {
        return List.of(ModItems.SALVIA_SEEDS.get(), ModItems.FRESH_SALVIA_LEAF.get(), ModItems.DRIED_SALVIA_LEAF.get(),
                ModItems.SALVIA_1X.get(), ModItems.SALVIA_10X.get(), ModItems.SALVIA_25X.get(), ModItems.SALVIA_50X.get(),
                ModItems.BONG.get(), ModItems.BONG_CANNABIS.get(), ModItems.BONG_SALVIA_1X.get(), ModItems.BONG_SALVIA_10X.get(),
                ModItems.BONG_SALVIA_25X.get(), ModItems.BONG_SALVIA_50X.get());
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
            }
        }
        for (int age = 0; age <= SalviaDivinorumPlantBlock.MAX_AGE; age++) {
            BlockPos pos = new BlockPos(age * 2 - 3, 65, 6);
            level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
            level.setBlock(pos, ModBlocks.SALVIA_DIVINORUM_PLANT.get().defaultBlockState()
                    .setValue(SalviaDivinorumPlantBlock.AGE, age), 3);
        }
        // Keep all pipeline icons together in the visible main inventory.
        List<Item> items = chain();
        for (int slot = 0; slot < items.size(); slot++) player.getInventory().setItem(slot + 9, new ItemStack(items.get(slot)));
        player.getInventory().setItem(0, new ItemStack(ModItems.BONG.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.BONG_SALVIA_50X.get()));
        player.getInventory().setItem(2, new ItemStack(ModItems.BONG_CANNABIS.get()));
        player.teleportTo(level, 0.5D, 65.0D, 2.5D, 0.0F, 17.0F);
    }

    private static void validateModels(Minecraft mc) {
        var shaper = mc.getItemRenderer().getItemModelShaper();
        for (Item item : chain()) {
            var model = shaper.getItemModel(new ItemStack(item));
            if (model == shaper.getModelManager().getMissingModel()
                    || model.getParticleIcon().contents().name().toString().equals("minecraft:missingno")) {
                throw new IllegalStateException("Missing Salvia item model or texture: " + item);
            }
        }
        for (int age = 0; age <= SalviaDivinorumPlantBlock.MAX_AGE; age++) {
            var state = ModBlocks.SALVIA_DIVINORUM_PLANT.get().defaultBlockState().setValue(SalviaDivinorumPlantBlock.AGE, age);
            var model = mc.getBlockRenderer().getBlockModel(state);
            if (model == shaper.getModelManager().getMissingModel()
                    || model.getParticleIcon().contents().name().toString().equals("minecraft:missingno")) {
                throw new IllegalStateException("Missing Salvia plant model or texture at age " + age);
            }
        }
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(name))) CAPTURES.incrementAndGet();
            else failure = "Screenshot failed: " + message.getString();
        });
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        String report = failure == null ? "PASS: Salvia client assets; screenshots=" + CAPTURES.get()
                : "FAIL: " + failure;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("salvia-smoke-result.txt"), report + System.lineSeparator());
        } catch (Exception exception) {
            TrippyCraft.LOGGER.error("Could not save Salvia asset report", exception);
        }
        TrippyCraft.LOGGER.info(report);
        mc.stop();
    }

    private static double elapsed(long time) {
        return (System.nanoTime() - time) / 1_000_000_000.0D;
    }
}
