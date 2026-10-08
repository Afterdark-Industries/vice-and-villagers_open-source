package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinTripSignature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in renderer QA: only a new world in the dedicated development run directory is opened. */
public final class PsilocybinClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.psilocybinSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "psilocybin-flow-smoke";
    private static final Stage[] STAGES = {
            new Stage("00-sober", 0.0D, 0.0D, 4.0D),
            new Stage("01-natural-3g", 3.0D, -1.0D, 100.0D),
            new Stage("02-come-up", 3.5D, 0.20D, 7.5D),
            new Stage("03-peak-1g", 1.0D, 0.45D, 7.5D),
            new Stage("04-peak-3_5g", 3.5D, 0.45D, 7.5D),
            new Stage("05-peak-7g", 7.0D, 0.45D, 12.0D),
            new Stage("06-peak-14g", 14.0D, 0.45D, 7.5D),
            new Stage("07-peak-28g", 28.0D, 0.45D, 7.5D),
            new Stage("08-peak-5g", 5.0D, 0.45D, 7.5D),
            new Stage("09-peak-8g", 8.0D, 0.45D, 7.5D)
    };
    private static final double[] NATURAL_CHECKPOINT_SECONDS = {0.5D, 10.0D, 20.0D, 30.0D, 45.0D, 60.0D, 90.0D};
    private static final int EXPECTED_SCREENSHOTS = STAGES.length + NATURAL_CHECKPOINT_SECONDS.length + 60;
    private static final int EXPECTED_PAIRS = STAGES.length + NATURAL_CHECKPOINT_SECONDS.length;
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static final List<String> REPORT = new ArrayList<>();
    private static final List<String> FOV_SAMPLES = new ArrayList<>();
    private static boolean started;
    private static boolean sceneQueued;
    private static volatile boolean sceneReady;
    private static volatile boolean stageReady;
    private static volatile String failure;
    private static boolean finished;
    private static boolean stageCaptured;
    private static boolean observingFade;
    private static long startNanos;
    private static long stageNanos;
    private static int stage;
    private static int motionFrame;
    private static int naturalCheckpoint;
    private static int pairedCaptures;
    private static String pendingPair;
    private static NativeImage beforeShaderFrame;
    private static final List<String> PIXEL_SAMPLES = new ArrayList<>();
    private static double previousPeakIntensity;
    private static double previousShaderIntensity;
    private static double previousPeakDose;
    private static int previousNaturalAge;
    private static double previousFov = Double.NaN;
    private static long previousFovNanos;
    private static double maxFrameFovDelta;
    private static double maxFovSpeed;
    private static int measuredFovFrames;
    private static int measuredCameraFrames;
    private static double nativeFov = Double.NaN;
    private static float nativeYaw;
    private static float nativePitch;
    private static float nativeRoll;
    private static double maxFovModification;
    private static double maxCameraModification;

    private PsilocybinClientSmokeTest() { }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!started) {
                if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                if (mc.getOverlay() != null) return;
                started = true;
                startNanos = System.nanoTime();
                if (Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
                    throw new IllegalStateException("Dedicated smoke world exists; choose a new psilocybinSmokeDir");
                }
                mc.options.pauseOnLostFocus = false;
                mc.options.onboardAccessibility = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.hideGui = false;
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                mc.options.guiScale().set(2);
                mc.options.fov().set(70);
                mc.options.bobView().set(false);
                mc.options.gamma().set(0.7D);
                FOV_SAMPLES.add("seconds,stage,dose_grams,phase,native_fov_degrees,presented_fov_degrees,effect_delta_degrees,frame_delta_degrees,interval_seconds");
                PIXEL_SAMPLES.add("checkpoint,age_ticks,dose_grams,phase,shader_strength,rgb_rms,rgb_mean_absolute_delta,strong_changed_fraction");
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(0x5053494C4FL, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                log("Fresh isolated world: " + mc.gameDirectory.getAbsolutePath());
                return;
            }
            if (failure != null) throw new IllegalStateException(failure);
            if (seconds(startNanos) > 320.0D) throw new IllegalStateException("Psilocybin renderer smoke timed out");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            var server = mc.getSingleplayerServer();
            if (!sceneQueued) {
                sceneQueued = true;
                var playerId = mc.player.getUUID();
                server.execute(() -> {
                    try {
                        buildScene(server.getPlayerList().getPlayer(playerId));
                        sceneReady = true;
                    } catch (Exception exception) { failure = exception.toString(); }
                });
                return;
            }
            if (!sceneReady || mc.player.distanceToSqr(0.5D, 65.0D, 2.5D) > 16.0D) return;
            mc.player.setYRot(0.0F);
            mc.player.setXRot(2.0F);
            mc.setScreen(null);
            if (stageNanos == 0L) {
                stageNanos = System.nanoTime();
                stageReady = true;
                log("Fixed view of bark, stone, books, foliage, copper and water; hand and HUD retained.");
                log("Configuration fixture: legacy psilocybinMaxVisualIntensity=" + TrippyConfig.psilocybinMaxVisualIntensity
                        + ", new psilocybinVisualGain=" + TrippyConfig.psilocybinVisualGain + ", particles=" + TrippyConfig.psilocybinParticleMultiplier);
            }
            if (!stageReady) return;
            double elapsed = seconds(stageNanos);
            if (observingFade) {
                if (elapsed > 5.0D && CAPTURES.get() == EXPECTED_SCREENSHOTS && pairedCaptures == EXPECTED_PAIRS) finish(mc);
                else if (elapsed > 15.0D) throw new IllegalStateException("Only " + CAPTURES.get() + " of " + EXPECTED_SCREENSHOTS
                        + " screenshots and " + pairedCaptures + " of " + EXPECTED_PAIRS + " shader pairs completed");
                return;
            }
            Stage selected = STAGES[stage];
            if (stage == 1 && naturalCheckpoint < NATURAL_CHECKPOINT_SECONDS.length
                    && elapsed >= NATURAL_CHECKPOINT_SECONDS[naturalCheckpoint]) {
                String name = naturalCheckpoint == 0 ? "natural-immediate" : "natural-" + (int) NATURAL_CHECKPOINT_SECONDS[naturalCheckpoint] + "s";
                capture(mc, name, true);
                naturalCheckpoint++;
            }
            if (stage == 5 && motionFrame < 60 && elapsed >= 5.0D + motionFrame * 0.10D) {
                capture(mc, String.format(Locale.ROOT, "motion-%03d", motionFrame++), false);
            }
            if (!stageCaptured && elapsed >= selected.dwellSeconds() - 0.7D) {
                capture(mc, selected.name(), true);
                stageCaptured = true;
            }
            if (stageCaptured && elapsed >= selected.dwellSeconds()) {
                if (stage == STAGES.length - 1) beginStage(mc, -1);
                else beginStage(mc, stage + 1);
            }
        } catch (Exception exception) {
            failure = exception.toString();
            TrippyCraft.LOGGER.error("[Psilocybin smoke] Client validation failed", exception);
            finish(mc);
        }
    }

    public static void beforeFov(ViewportEvent.ComputeFov event) {
        if (ENABLED && event.usedConfiguredFov()) nativeFov = event.getFOV();
    }

    /** Vanilla interpolates FOV after teleporting; compare the exact incoming native projection. */
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!ENABLED || finished || !stageReady || stageNanos == 0L || !event.usedConfiguredFov()) return;
        double fov = event.getFOV();
        double modification = Math.abs(fov - nativeFov);
        maxFovModification = Math.max(maxFovModification, modification);
        if (!Double.isFinite(fov) || !Double.isFinite(nativeFov) || modification > 0.00001D) {
            failure = "Psilocybin changed native FOV from " + nativeFov + " to " + fov;
            return;
        }
        long now = System.nanoTime();
        double interval = (now - previousFovNanos) / 1_000_000_000.0D;
        double delta = Double.isFinite(previousFov) ? Math.abs(fov - previousFov) : 0.0D;
        if (seconds(stageNanos) > 2.0D && interval >= 0.001D && interval <= 0.060D && Double.isFinite(previousFov)) {
            maxFrameFovDelta = Math.max(maxFrameFovDelta, delta);
            maxFovSpeed = Math.max(maxFovSpeed, delta / interval);
            measuredFovFrames++;
        }
        var state = ClientExperienceStore.psilocybin();
        FOV_SAMPLES.add(String.format(Locale.ROOT, "%.6f,%s,%.3f,%s,%.6f,%.6f,%.6f,%.6f,%.6f",
                seconds(startNanos), observingFade ? "fade" : STAGES[stage].name(), state.activeDoseGrams(), state.phase(), nativeFov, fov, modification, delta, interval));
        previousFov = fov;
        previousFovNanos = now;
    }

    public static void beforeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ENABLED) return;
        nativeYaw = event.getYaw();
        nativePitch = event.getPitch();
        nativeRoll = event.getRoll();
    }

    /** Surface breathing must never rotate or roll the incoming native view. */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ENABLED || finished || !stageReady || stageNanos == 0L || seconds(stageNanos) < 2.0D) return;
        measuredCameraFrames++;
        double modification = Math.max(Math.abs(event.getYaw() - nativeYaw),
                Math.max(Math.abs(event.getPitch() - nativePitch), Math.abs(event.getRoll() - nativeRoll)));
        maxCameraModification = Math.max(maxCameraModification, modification);
        if (!Double.isFinite(modification) || modification > 0.00001D) {
            failure = "Psilocybin moved the native camera: yaw=" + event.getYaw() + " pitch=" + event.getPitch() + " roll=" + event.getRoll();
        }
    }

    /** Sample the exact rendered world before postprocessing; no HUD or animated-frame comparison. */
    public static void beforeWorldEffects(RenderLevelStageEvent event) {
        if (!ENABLED || finished || pendingPair == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        try {
            if (beforeShaderFrame != null) beforeShaderFrame.close();
            beforeShaderFrame = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
        } catch (Exception exception) { failure = "Could not capture original world pixels: " + exception; }
    }

    /** Prove the world shader changes visible textured pixels in this same frame. */
    public static void afterWorldEffects(RenderLevelStageEvent event) {
        if (!ENABLED || finished || pendingPair == null || beforeShaderFrame == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        String name = pendingPair;
        try (NativeImage after = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int width = Math.min(beforeShaderFrame.getWidth(), after.getWidth());
            int height = Math.min(beforeShaderFrame.getHeight(), after.getHeight());
            int left = (int) (width * 0.25D), right = (int) (width * 0.76D);
            int top = (int) (height * 0.15D), bottom = (int) (height * 0.44D);
            double squared = 0.0D;
            double absolute = 0.0D;
            int changed = 0;
            int count = 0;
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    int before = beforeShaderFrame.getPixelRGBA(x, y);
                    int presented = after.getPixelRGBA(x, y);
                    int largest = 0;
                    for (int shift = 0; shift < 24; shift += 8) {
                        int delta = Math.abs(((before >>> shift) & 255) - ((presented >>> shift) & 255));
                        squared += delta * delta;
                        absolute += delta;
                        largest = Math.max(largest, delta);
                    }
                    if (largest > 8) changed++;
                    count++;
                }
            }
            if (count == 0) throw new IllegalStateException("Empty native pixel visibility sample");
            double rms = Math.sqrt(squared / (count * 3.0D));
            double mad = absolute / (count * 3.0D);
            double fraction = changed / (double) count;
            var state = ClientExperienceStore.psilocybin();
            String measurement = String.format(Locale.ROOT, "%s,%d,%.3f,%s,%.6f,%.6f,%.6f,%.6f",
                    name, state.ageTicks(), state.activeDoseGrams(), state.phase(),
                    PsilocybinWorldWarpPass.currentShaderIntensity(), rms, mad, fraction);
            PIXEL_SAMPLES.add(measurement);
            log("Same-frame world pixels: " + measurement);
            var pairDirectory = mc.gameDirectory.toPath().resolve("shader-pairs");
            Files.createDirectories(pairDirectory);
            beforeShaderFrame.writeToFile(pairDirectory.resolve(name + "-original.png"));
            after.writeToFile(pairDirectory.resolve(name + "-presented.png"));
            pairedCaptures++;
            if ((name.equals("00-sober") || name.equals("natural-immediate")) && rms > 0.15D) {
                failure = "Immediate onset altered rendered world before a smooth come-up: RMS=" + rms;
            }
            if (name.equals("natural-60s") && (rms < 2.0D || fraction < 0.03D)) {
                failure = "Natural 3g come-up remained invisible at 60s: RMS=" + rms + ", changed=" + fraction;
            }
            boolean clearDose = name.equals("01-natural-3g") || name.equals("04-peak-3_5g")
                    || name.equals("08-peak-5g") || name.equals("09-peak-8g");
            if (clearDose && (rms < 4.0D || fraction < 0.10D)) {
                failure = "Normal dose world effects remained invisible at " + name + ": RMS=" + rms + ", changed=" + fraction;
            }
            if (name.equals("03-peak-1g") && rms < 2.0D) {
                failure = "Mild 1g texture effects remained invisible: RMS=" + rms;
            }
        } catch (Exception exception) {
            failure = "Same-frame shader visibility validation failed: " + exception;
        } finally {
            beforeShaderFrame.close();
            beforeShaderFrame = null;
            pendingPair = null;
        }
    }

    private static void buildScene(ServerPlayer player) {
        if (player == null) throw new IllegalStateException("Integrated player missing");
        var level = player.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, player.server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, player.server);
        level.setDayTime(6000L);
        level.setWeatherParameters(6000, 0, false, false);
        for (int x = -10; x <= 10; x++) {
            for (int z = 0; z <= 14; z++) {
                for (int y = 64; y <= 73; y++) {
                    Block block = y == 64 ? (Math.floorMod(x + z, 3) == 0 ? Blocks.STONE_BRICKS : Blocks.OAK_PLANKS) : Blocks.AIR;
                    if (y > 64 && z == 14 && y <= 71) block = x < -5 ? Blocks.OAK_LOG : x < -1 ? Blocks.COBBLESTONE : x < 3 ? Blocks.BOOKSHELF : x < 7 ? Blocks.MOSS_BLOCK : Blocks.COPPER_BLOCK;
                    if (y > 64 && (x == -10 || x == 10) && y <= 69) block = y == 69 ? Blocks.OAK_LEAVES : Blocks.STONE_BRICKS;
                    if (y == 64 && x >= 4 && x <= 8 && z >= 5 && z <= 9) block = Blocks.WATER;
                    level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
                }
            }
        }
        for (int x : new int[]{-6, 6}) {
            for (int y = 65; y <= 69; y++) level.setBlock(new BlockPos(x, y, 10), Blocks.OAK_LOG.defaultBlockState(), 3);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) level.setBlock(new BlockPos(x + dx, 70, 10 + dz), Blocks.OAK_LEAVES.defaultBlockState(), 3);
            }
        }
        player.teleportTo(level, 0.5D, 65.0D, 2.5D, 0.0F, 2.0F);
        Villager subject = EntityType.VILLAGER.create(level);
        if (subject == null) throw new IllegalStateException("Could not create entity appearance subject");
        subject.setPos(3.5D, 65.0D, 10.5D);
        subject.setNoAi(true);
        subject.setInvulnerable(true);
        subject.setYRot(180.0F);
        subject.setYHeadRot(180.0F);
        level.addFreshEntity(subject);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PSILOCYBIN_CAPSULE_1G.get()));
        SubstanceEngine.reset(player);
    }

    private static void beginStage(Minecraft mc, int next) {
        stageReady = false;
        previousFov = Double.NaN;
        var playerId = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Missing integrated player at dose transition");
                SubstanceEngine.reset(player);
                if (next >= 0) {
                    Stage selected = STAGES[next];
                    useNativeDose(player, selected.doseGrams());
                    var experience = SubstanceEngine.getExperience(playerId).orElseThrow();
                    if (Math.abs(experience.psilocybinState().targetDoseGrams() - selected.doseGrams()) > 0.000001D) throw new IllegalStateException("Native capsule applied the wrong dose");
                    var saved = experience.save();
                    saved.getCompound("psilocybin").put("signature", PsilocybinTripSignature.create(0x5053494C4FL).save());
                    experience = PsychoactiveExperience.load(saved);
                    SubstanceEngine.setExperience(playerId, experience);
                    if (selected.progress() >= 0.0D) {
                        SubstanceEngine.scrubPsilocybin(player, (int) (experience.psilocybinState().durationTicks() * selected.progress()));
                        log("Native consumed capsule: " + selected.doseGrams() + "g, then QA timeline at " + selected.progress());
                    } else {
                        log("Native consumed capsule: " + selected.doseGrams() + "g; natural age-zero onset for 100s, no timeline scrub");
                    }
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PSILOCYBIN_CAPSULE_1G.get()));
                SubstanceEngine.sync(player);
                mc.execute(() -> {
                    observingFade = next < 0;
                    if (next >= 0) stage = next;
                    stageCaptured = false;
                    stageNanos = System.nanoTime();
                    stageReady = true;
                });
            } catch (Exception exception) { failure = exception.toString(); }
        });
    }

    private static void useNativeDose(ServerPlayer player, double grams) {
        boolean creative = player.getAbilities().instabuild;
        player.getAbilities().instabuild = false;
        try {
            ItemStack capsule;
            if (grams == 1.0D) capsule = new ItemStack(ModItems.PSILOCYBIN_CAPSULE_1G.get());
            else {
                List<CapsuleBlend.Entry> entries = new ArrayList<>();
                double remaining = grams;
                for (var item : List.of(ModItems.PSILOCYBIN_CAPSULE_5G.get(), ModItems.PSILOCYBIN_CAPSULE_3G.get(), ModItems.PSILOCYBIN_CAPSULE_1G.get(), ModItems.PSILOCYBIN_CAPSULE_05G.get())) {
                    var entry = CapsuleBlend.entryFromDoseItem(new ItemStack(item)).orElseThrow();
                    while (remaining >= entry.amount() - 0.000001D) {
                        entries.add(entry);
                        remaining -= entry.amount();
                    }
                }
                capsule = CapsuleBlend.createStack(entries);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
            capsule.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            if (!player.getMainHandItem().isEmpty()) throw new IllegalStateException("Survival capsule was not consumed");
        } finally { player.getAbilities().instabuild = creative; }
    }

    private static void capture(Minecraft mc, String name, boolean checkpoint) {
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new IllegalStateException("OpenGL renderer error at " + name);
        if (checkpoint) {
            if (stage > 0 && (!state.active() || !Double.isFinite(state.visualIntensity()))) throw new IllegalStateException("Invalid synchronized visual state at " + name);
            boolean largerPeak = stage >= 3 && STAGES[stage].doseGrams() > previousPeakDose;
            if (largerPeak && state.intensity() <= previousPeakIntensity) throw new IllegalStateException("Larger peak dose failed to increase intensity at " + name);
            double shader = PsilocybinWorldWarpPass.currentShaderIntensity();
            if (largerPeak && shader <= previousShaderIntensity) throw new IllegalStateException("Larger peak dose failed to increase actual shader strength at " + name);
            if (stage >= 3) previousPeakIntensity = state.intensity();
            if (stage >= 3) previousShaderIntensity = shader;
            if (stage >= 3) previousPeakDose = STAGES[stage].doseGrams();
            if (name.equals("natural-immediate") && state.visualIntensity() > 0.01D) throw new IllegalStateException("Onset immediately raised visual intensity: " + state.visualIntensity());
            if (stage == 1) {
                if (state.ageTicks() < previousNaturalAge) throw new IllegalStateException("Natural trip timeline moved backwards");
                previousNaturalAge = state.ageTicks();
                if (name.equals("01-natural-3g") && state.progress() < 0.30D) throw new IllegalStateException("Natural 3g capsule failed to reach peak by 100s");
            }
            log(String.format(Locale.ROOT, "%s: dose=%.2fg phase=%s intensity=%.6f visual=%.6f breath=%.6f melt=%.6f camera=%.6f",
                    name, state.activeDoseGrams(), state.phase(), state.intensity(), state.visualIntensity(), state.organicBreathing(), state.surfaceMelting(), state.cameraBreathing()));
            log(String.format(Locale.ROOT, "%s: shader_target=%.6f shader_eased=%.6f shader_dose=%.3fg",
                    name, PsilocybinWorldWarpPass.shaderIntensity(state), shader, PsilocybinWorldWarpPass.currentShaderDose()));
            if (pendingPair != null) throw new IllegalStateException("A world shader pair is still pending at " + name);
            pendingPair = name;
        }
        String file = "psilocybin-" + name + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(file))) CAPTURES.incrementAndGet();
            else failure = "Screenshot failed: " + message.getString();
        });
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        if (beforeShaderFrame != null) {
            beforeShaderFrame.close();
            beforeShaderFrame = null;
        }
        if (failure == null && measuredFovFrames < 120) failure = "Insufficient actual FOV render samples: " + measuredFovFrames;
        if (failure == null && measuredCameraFrames < 120) failure = "Insufficient actual camera render samples: " + measuredCameraFrames;
        log(String.format(Locale.ROOT, "FOV continuity: frames=%d max_frame_delta=%.6fdeg max_speed=%.6fdeg/s", measuredFovFrames, maxFrameFovDelta, maxFovSpeed));
        log(String.format(Locale.ROOT, "Native projection unchanged: FOV_effect_delta=%.9fdeg camera_effect_delta=%.9fdeg camera_frames=%d configured_FOV=70deg",
                maxFovModification, maxCameraModification, measuredCameraFrames));
        log("Same-frame shader visibility pairs=" + pairedCaptures + "; natural 3g capsule observed for 100s without scrubbing; source config values retained.");
        log((failure == null ? "PASS" : "FAIL") + ": Psilocybin client flow smoke; screenshots=" + CAPTURES.get());
        if (failure != null) log("FAIL: " + failure);
        try {
            synchronized (REPORT) { Files.writeString(mc.gameDirectory.toPath().resolve("psilocybin-smoke-result.txt"), String.join(System.lineSeparator(), REPORT)); }
            Files.writeString(mc.gameDirectory.toPath().resolve("psilocybin-fov.csv"), String.join(System.lineSeparator(), FOV_SAMPLES));
            Files.writeString(mc.gameDirectory.toPath().resolve("psilocybin-pixel-deltas.csv"), String.join(System.lineSeparator(), PIXEL_SAMPLES));
        } catch (Exception exception) { TrippyCraft.LOGGER.error("[Psilocybin smoke] Could not write QA artifacts", exception); }
        mc.stop();
    }

    private static void log(String message) {
        TrippyCraft.LOGGER.info("[Psilocybin smoke] {}", message);
        synchronized (REPORT) { REPORT.add(message); }
    }

    private static double seconds(long since) { return (System.nanoTime() - since) / 1_000_000_000.0D; }
    private record Stage(String name, double doseGrams, double progress, double dwellSeconds) { }
}
