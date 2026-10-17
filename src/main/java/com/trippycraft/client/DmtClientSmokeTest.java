package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.dmt.DmtTripSignature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in integration test: a natural trip in a new disposable world, never an existing save. */
public final class DmtClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.dmtSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "dmt-polish-smoke";
    private static final double[] CAPTURE_SECONDS = {3, 25, 42, 65, 90, 105, 125, 145, 168};
    private static final List<String> REPORT = new ArrayList<>();
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static boolean started, setupQueued, finished, dosed;
    private static volatile boolean sceneReady;
    private static volatile String failure;
    private static long startedNanos, sceneTick = -1;
    private static int captureStage, settingStage, activeTicks;
    private static long originalGcCount, originalGcMillis;
    private static final Map<String, List<Double>> FRAME_INTERVALS = new HashMap<>();
    private static long lastFrameNanos;
    private static UUID subjectId;
    private static long lastEntityTick = Long.MIN_VALUE;
    private static double walkStartZ = Double.NaN;
    private static boolean walkingFinished;

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!ENABLED || finished || sceneTick < 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            var state = ClientExperienceStore.dmt();
            String label = state.active() ? state.stage() + " quality=" + TrippyClientConfig.dmtFractalQuality
                    + " intensity=" + TrippyClientConfig.dmtFractalIntensity : "sober";
            FRAME_INTERVALS.computeIfAbsent(label, ignored -> new ArrayList<>()).add((now - lastFrameNanos) / 1e6);
        }
        lastFrameNanos = now;
    }

    private DmtClientSmokeTest() { }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!started) {
                if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                if (mc.getOverlay() != null) return;
                started = true;
                startedNanos = System.nanoTime();
                if (Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
                    throw new IllegalStateException("Dedicated world already exists; refusing to open or replace it");
                }
                mc.options.pauseOnLostFocus = false;
                mc.options.onboardAccessibility = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.renderDistance().set(5);
                mc.options.simulationDistance().set(5);
                mc.options.fov().set(75);
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(120);
                TrippyClientConfig.dmtFractalWorld = true;
                TrippyClientConfig.dmtFractalIntensity = 1;
                TrippyClientConfig.dmtFractalQuality = 2;
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(0x444D54504F4C4953L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                log("Fresh isolated desktop world; natural 6-unit trip, slow camera turns, full entry/return, live quality/intensity changes");
                return;
            }
            if ((System.nanoTime() - startedNanos) / 1e9 > 300) throw new IllegalStateException("Integration test timed out");
            if (failure != null) throw new IllegalStateException(failure);
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            var server = mc.getSingleplayerServer();
            var playerId = mc.player.getUUID();
            if (!setupQueued) {
                setupQueued = true;
                server.execute(() -> {
                    try {
                        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                        if (player == null) throw new IllegalStateException("Integrated player missing");
                        buildScene(player);
                        sceneReady = true;
                    } catch (Exception exception) { failure = exception.toString(); }
                });
                return;
            }
            if (!sceneReady || (sceneTick < 0 && mc.player.distanceToSqr(.5, 65, 2.5) > 16)) return;
            if (sceneTick < 0) {
                sceneTick = mc.level.getGameTime();
                mc.setScreen(null);
                originalGcCount = gcCount();
                originalGcMillis = gcMillis();
            }
            double seconds = (mc.level.getGameTime() - sceneTick) * .05;
            double walkAlignment = DmtRealmDynamics.smootherstep(63, 65, seconds)
                    * (1 - DmtRealmDynamics.smootherstep(77, 79, seconds));
            mc.player.setYRot((float) (Math.sin(seconds * .075) * 22 * (1 - walkAlignment)));
            mc.player.setXRot((float) (3 + Math.sin(seconds * .043) * 4));
            if (seconds >= 65 && seconds < 77) {
                if (Double.isNaN(walkStartZ)) { walkStartZ = mc.player.getZ(); log("Begin scripted forward movement across realm cells"); }
                mc.options.keyUp.setDown(true);
            } else if (!walkingFinished && !Double.isNaN(walkStartZ)) {
                mc.options.keyUp.setDown(false);
                walkingFinished = true;
                log(String.format(Locale.ROOT, "Scripted movement displacement=%.3f blocks", mc.player.getZ() - walkStartZ));
                if (mc.player.getZ() - walkStartZ < 40 || Math.abs(mc.player.getY() - 65) > .2)
                    throw new IllegalStateException("Scripted movement did not cross a supported realm cell");
            }
            if (lastEntityTick == Long.MIN_VALUE || mc.level.getGameTime() - lastEntityTick >= 20) {
                lastEntityTick = mc.level.getGameTime();
                server.execute(() -> {
                    var entity = server.overworld().getEntity(subjectId);
                    if (entity != null) entity.teleportTo(2 + Math.sin(seconds * .17) * 2, 65, 10 + Math.cos(seconds * .17) * 2);
                });
            }
            if (!dosed && seconds >= 5) {
                dosed = true;
                server.execute(() -> {
                    try {
                        var player = server.getPlayerList().getPlayer(playerId);
                        if (player == null) throw new IllegalStateException("Integrated player missing at dosing");
                        SubstanceEngine.addDmtDose(player, 6);
                        var saved = SubstanceEngine.getExperience(playerId).orElseThrow().save();
                        saved.getCompound("dmt").put("signature", DmtTripSignature.create(0x444D54504F4C4953L).save());
                        saved.getCompound("dmt").remove("encounterDirector");
                        SubstanceEngine.setExperience(playerId, PsychoactiveExperience.load(saved));
                        SubstanceEngine.sync(player);
                    } catch (Exception exception) { failure = exception.toString(); }
                });
            }
            if (ClientExperienceStore.dmt().active()) activeTicks++;
            if (settingStage == 0 && seconds >= 80) { TrippyClientConfig.dmtFractalQuality = 0; settingStage++; log("Quality Low"); }
            if (settingStage == 1 && seconds >= 95) { TrippyClientConfig.dmtFractalQuality = 1; settingStage++; log("Quality Balanced"); }
            if (settingStage == 2 && seconds >= 110) { TrippyClientConfig.dmtFractalQuality = 2; settingStage++; log("Quality High"); }
            if (settingStage == 3 && seconds >= 115) { TrippyClientConfig.dmtFractalIntensity = .5; settingStage++; log("Intensity 50%"); }
            if (settingStage == 4 && seconds >= 120) { TrippyClientConfig.dmtFractalIntensity = 1; settingStage++; log("Intensity 100%"); }
            if (captureStage < CAPTURE_SECONDS.length && seconds >= CAPTURE_SECONDS[captureStage]) {
                String name = String.format(Locale.ROOT, "dmt-%02d-%03ds.png", captureStage++, Math.round(seconds));
                var state = ClientExperienceStore.dmt();
                log(String.format(Locale.ROOT, "%s dose=%.3f progress=%.4f active=%s heapMiB=%.1f",
                        name, state.activeDoseUnits(), state.progress(), state.active(),
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0));
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
                    if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(name))) CAPTURES.incrementAndGet();
                    else failure = "Screenshot failed: " + message.getString();
                });
            }
            if (seconds >= 175 && CAPTURES.get() == CAPTURE_SECONDS.length) {
                if (activeTicks < 1000) throw new IllegalStateException("Did not observe the natural active trip");
                if (ClientExperienceStore.dmt().active()) throw new IllegalStateException("Trip failed to expire naturally");
                if (WorldWarpPostProcessor.hasFailedDmtEffects()) throw new IllegalStateException("A DMT renderer pass was disabled");
                if (WorldWarpPostProcessor.dmtFractalDrawCount() < 100) throw new IllegalStateException("Fractal pass did not render a sustained crossing");
                log("Successful visible-fractal pass draws=" + WorldWarpPostProcessor.dmtFractalDrawCount());
                log("GC collections=" + (gcCount() - originalGcCount) + " collectionMillis=" + (gcMillis() - originalGcMillis));
                FRAME_INTERVALS.forEach((label, intervals) -> {
                    double[] sorted = intervals.stream().mapToDouble(Double::doubleValue).sorted().toArray();
                    log(String.format(Locale.ROOT, "AFTER_LEVEL intervals [%s] n=%d mean/p50/p95/p99=%.3f/%.3f/%.3f/%.3fms",
                            label, sorted.length, Arrays.stream(sorted).average().orElse(0), percentile(sorted, .5),
                            percentile(sorted, .95), percentile(sorted, .99)));
                });
                log("PASS: complete natural DMT integration trip; screenshots=" + CAPTURES.get()
                        + "; isolated flat-world desktop coverage only; no headset or walking-comfort claim");
                finish(mc);
            }
        } catch (Exception exception) {
            log("FAIL: " + exception);
            TrippyCraft.LOGGER.error("DMT integration smoke failed", exception);
            finish(mc);
        }
    }

    private static void buildScene(ServerPlayer player) {
        var level = player.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
        level.setDayTime(6000);
        for (int x = -8; x <= 8; x++) for (int z = 0; z <= 18; z++) for (int y = 64; y <= 73; y++) {
            var block = Blocks.AIR;
            if (y == 64) block = (x + z) % 3 == 0 ? Blocks.MOSS_BLOCK : Blocks.STONE_BRICKS;
            else if (x == -8 || x == 8 || (z == 18 && (Math.abs(x) > 2 || y >= 69))) block = y % 3 == 0 ? Blocks.GLOWSTONE : Blocks.OAK_LOG;
            else if (y == 73 && z > 6) block = x % 3 == 0 ? Blocks.OAK_LEAVES : Blocks.GLASS;
            level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
        }
        for (int x = -4; x <= 4; x++) for (int z = 19; z <= 75; z++) {
            level.setBlock(new BlockPos(x, 64, z), (z % 3 == 0 ? Blocks.MOSS_BLOCK : Blocks.STONE_BRICKS).defaultBlockState(), 3);
        }
        var villager = EntityType.VILLAGER.create(level);
        // Keep nearby wall/overhead evidence within the production sampler's
        // four-block horizontal/eight-block vertical reach at the test camera.
        for (int z = 1; z <= 6; z++) for (int y = 65; y <= 69; y++) {
            level.setBlock(new BlockPos(-3, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        for (int x = -2; x <= 2; x++) for (int z = 1; z <= 6; z++) {
            level.setBlock(new BlockPos(x, 71, z), (x % 2 == 0 ? Blocks.OAK_LEAVES : Blocks.GLASS).defaultBlockState(), 3);
        }
        if (villager == null) throw new IllegalStateException("Failed to create scene entity");
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        villager.moveTo(2, 65, 10, 180, 0);
        level.addFreshEntity(villager);
        subjectId = villager.getUUID();
        player.teleportTo(level, .5, 65, 2.5, 0, 3);
    }

    private static long gcCount() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum(); }
    private static long gcMillis() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum(); }
    private static double percentile(double[] sorted, double fraction) { return sorted[(int) Math.ceil(sorted.length * fraction) - 1]; }
    private static void log(String message) { REPORT.add(message); TrippyCraft.LOGGER.info("[DMT smoke] {}", message); }
    private static void finish(Minecraft mc) {
        finished = true;
        mc.options.keyUp.setDown(false);
        try { Files.write(mc.gameDirectory.toPath().resolve("dmt-smoke-result.txt"), REPORT); }
        catch (Exception exception) { TrippyCraft.LOGGER.error("Cannot write DMT smoke report", exception); }
        mc.stop();
    }
}
