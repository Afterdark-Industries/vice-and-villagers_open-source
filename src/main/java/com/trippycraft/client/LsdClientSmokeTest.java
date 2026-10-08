package com.trippycraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.google.gson.GsonBuilder;
import com.trippycraft.TrippyCraft;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.lsd.LsdDoseCurve;
import com.trippycraft.substance.lsd.LsdTripSignature;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import org.joml.Matrix4f;

import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Native, opt-in projection and shader QA in a fresh isolated development world. */
public final class LsdClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.lsdSmokeTest") && !FMLEnvironment.production;
    private static final boolean PALETTE_ONLY = Boolean.getBoolean("trippycraft.lsdSmokePaletteOnly");
    private static final String WORLD = "lsd-continuous-color-smoke";
    private static final long SIGNATURE_SEED = 0x4C5344464C4FL;
    private static final Stage[] FULL_STAGES = {
            new Stage("00-sober", 0.0D, 0.0D, 4.0D),
            new Stage("01-natural-250ug", 250.0D, -1.0D, 35.0D),
            new Stage("02-onset", 250.0D, 0.07D, 7.0D),
            new Stage("03-come-up", 250.0D, 0.20D, 7.0D),
            new Stage("04-day-peak-250ug", 250.0D, 0.46D, 16.0D, Scene.DAY, true),
            new Stage("05-day-wave-250ug", 250.0D, 0.70D, 16.0D, Scene.DAY, true),
            new Stage("06-come-down", 250.0D, 0.85D, 7.0D),
            new Stage("07-afterglow", 250.0D, 0.97D, 7.0D),
            new Stage("08-day-peak-500ug", 500.0D, 0.46D, 16.0D, Scene.DAY, true),
            new Stage("09-day-wave-500ug", 500.0D, 0.70D, 16.0D, Scene.DAY, true),
            new Stage("10-day-peak-1000ug", 1000.0D, 0.46D, 16.0D, Scene.DAY, true),
            new Stage("11-day-wave-1000ug", 1000.0D, 0.70D, 16.0D, Scene.DAY, true),
            new Stage("12-day-peak-5000ug", 5000.0D, 0.46D, 16.0D, Scene.DAY, true),
            new Stage("13-day-wave-5000ug", 5000.0D, 0.70D, 16.0D, Scene.DAY, true),
            new Stage("14-tavern-sober", 0.0D, 0.0D, 28.0D, Scene.TAVERN, false),
            new Stage("15-tavern-peak-250ug", 250.0D, 0.46D, 16.0D, Scene.TAVERN, true),
            new Stage("16-tavern-wave-250ug", 250.0D, 0.70D, 16.0D, Scene.TAVERN, true),
            new Stage("17-tavern-peak-500ug", 500.0D, 0.46D, 16.0D, Scene.TAVERN, true),
            new Stage("18-tavern-wave-500ug", 500.0D, 0.70D, 16.0D, Scene.TAVERN, true),
            new Stage("19-tavern-peak-1000ug", 1000.0D, 0.46D, 16.0D, Scene.TAVERN, true),
            new Stage("20-tavern-wave-1000ug", 1000.0D, 0.70D, 16.0D, Scene.TAVERN, true),
            new Stage("21-tavern-peak-5000ug", 5000.0D, 0.46D, 16.0D, Scene.TAVERN, true),
            new Stage("22-tavern-wave-5000ug", 5000.0D, 0.70D, 16.0D, Scene.TAVERN, true),
            new Stage("23-native-expiry-tail", 0.0D, 1.0D, 7.0D, Scene.TAVERN, false)
    };
    private static final Stage[] STAGES = PALETTE_ONLY ? Arrays.stream(FULL_STAGES)
            .filter(selected -> selected.micrograms() == 0.0D
                    || selected.progress() == 0.46D && selected.micrograms() != 1000.0D
                    || selected.progress() == 0.70D && selected.micrograms() == 1000.0D)
            .toArray(Stage[]::new) : FULL_STAGES;
    private static final double[] NATURAL_CHECKPOINT_SECONDS = PALETTE_ONLY ? new double[0] : new double[]{0.5D, 10.0D, 20.0D, 30.0D};
    private static final int EXPECTED_PAIRS = STAGES.length + NATURAL_CHECKPOINT_SECONDS.length;
    private static final int MOTION_FRAMES_PER_CASE = 11;
    private static final int MOTION_CASES = (int) Arrays.stream(STAGES).filter(Stage::observeMotion).count();
    private static final int EXPECTED_PEAK_COMPARISONS = (int) Arrays.stream(STAGES)
            .filter(selected -> selected.observeMotion() && selected.progress() == 0.46D).count();
    private static final int EXPECTED_SCREENSHOTS = EXPECTED_PAIRS + MOTION_CASES * MOTION_FRAMES_PER_CASE;
    private static final List<String> REPORT = new ArrayList<>();
    private static final List<String> FOV_SAMPLES = new ArrayList<>();
    private static final List<String> CAMERA_SAMPLES = new ArrayList<>();
    private static final List<String> PIXEL_SAMPLES = new ArrayList<>();
    private static final List<String> MOTION_SAMPLES = new ArrayList<>();
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static boolean started;
    private static boolean sceneQueued;
    private static volatile boolean sceneReady;
    private static volatile boolean stageReady;
    private static volatile String failure;
    private static volatile UUID playerId;
    private static volatile int pinnedAge = -1;
    private static volatile double pinnedEnvelope;
    private static volatile double pinnedDose;
    private static boolean finished;
    private static boolean stageCaptured;
    private static boolean observingFade;
    private static boolean stageTransitionPending;
    private static long startNanos;
    private static long stageNanos;
    private static int stage;
    private static int naturalCheckpoint;
    private static int naturalAge;
    private static int motionFrame;
    private static int pairedCaptures;
    private static int measuredFovFrames;
    private static int measuredCameraFrames;
    private static int measuredSettledFovFrames;
    private static int peakComparisons;
    private static final Map<Scene, Double> PREVIOUS_PEAK_INTENSITY = new LinkedHashMap<>();
    private static final Map<Scene, Double> PREVIOUS_SHADER_STRENGTH = new LinkedHashMap<>();
    private static int completedMotionCases;
    private static int stageMotionMeasurements;
    private static double stageMotionDelta;
    private static double stageLargestColorSpeed;
    private static double previousMotionTime;
    private static double nativeFov;
    private static float nativeYaw;
    private static float nativePitch;
    private static float nativeRoll;
    private static double maxFovModification;
    private static double maxSettledFovError;
    private static double maxCameraModification;
    private static String pendingPair;
    private static NativeImage originalWorld;
    private static double[] previousMotionPixels;
    private static double[] previousMotionMeans;
    private static double largestMeanColorStep;
    private static double largestMotionDelta;
    private static double totalMotionDelta;
    private static int motionMeasurements;
    private static Scene configuredScene;

    private LsdClientSmokeTest() { }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!started) {
                if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                if (mc.getOverlay() != null) return;
                if (Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
                    throw new IllegalStateException("Dedicated smoke world exists; choose a fresh lsdSmokeDir");
                }
                started = true;
                startNanos = System.nanoTime();
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
                FOV_SAMPLES.add("seconds,stage,dose_ug,phase,renderer_ticks,native_fov,presented_fov,effect_delta,settled_70deg_error");
                CAMERA_SAMPLES.add("seconds,stage,dose_ug,phase,native_yaw,presented_yaw,native_pitch,presented_pitch,native_roll,presented_roll,effect_delta");
                PIXEL_SAMPLES.add("checkpoint,scene,age_ticks,dose_ug,phase,state_intensity,shader_target,shader_strength,rgb_rms,rgb_mean_delta,strong_changed_fraction,source_luma,presented_luma,source_chroma,presented_chroma,flat_spike_fraction,matched_texture_patches,mean_texture_displacement_pixels");
                MOTION_SAMPLES.add("case,scene,frame,renderer_ticks,dose_ug,shader_strength,r_mean,g_mean,b_mean,max_mean_color_step,rgb_motion_mean_delta,color_speed_per_second");
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(SIGNATURE_SEED, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                log("Fresh isolated development world: " + mc.gameDirectory.getAbsolutePath());
                return;
            }
            if (failure != null) throw new IllegalStateException(failure);
            if (seconds(startNanos) > 600.0D) throw new IllegalStateException("LSD renderer smoke timed out");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            if (!sceneQueued) {
                sceneQueued = true;
                playerId = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        buildScene(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId));
                        sceneReady = true;
                    } catch (Exception exception) { failure = exception.toString(); }
                });
                return;
            }
            if (!sceneReady || mc.player.distanceToSqr(0.5D, 65.0D, 2.5D) > 16.0D) return;
            mc.setScreen(null);
            // This isolated stationary fixture must not inherit physical key presses from another game window.
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyUse.setDown(false);
            mc.player.setSprinting(false);
            mc.player.getAbilities().flying = false;
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.setYRot(0.0F);
            mc.player.setXRot(12.0F);
            if (stageTransitionPending) {
                if (!synchronizedStageReady()) return;
                stageTransitionPending = false;
                stageNanos = System.nanoTime();
                stageReady = true;
                log("Client snapshot synchronized for " + stageName() + "; begin native viewport settling observation");
            } else if (stageNanos == 0L) {
                stageNanos = System.nanoTime();
                stageReady = true;
                log("Fixed view of bark, books, moss, stone, copper, water and textured floor; downward view exposes surface melting.");
            }
            if (!stageReady) return;
            double elapsed = seconds(stageNanos);
            if (observingFade) {
                if (elapsed > 5.0D && CAPTURES.get() == EXPECTED_SCREENSHOTS && pairedCaptures == EXPECTED_PAIRS) finish(mc);
                else if (elapsed > 15.0D) throw new IllegalStateException("Incomplete native render captures: screenshots=" + CAPTURES.get() + ", pairs=" + pairedCaptures);
                return;
            }
            if (stage == 1 && naturalCheckpoint < NATURAL_CHECKPOINT_SECONDS.length && elapsed >= NATURAL_CHECKPOINT_SECONDS[naturalCheckpoint]) {
                String name = naturalCheckpoint == 0 ? "natural-immediate" : "natural-" + (int) NATURAL_CHECKPOINT_SECONDS[naturalCheckpoint] + "s";
                capture(mc, name, true);
                naturalCheckpoint++;
            }
            Stage selected = STAGES[stage];
            if (selected.observeMotion() && motionFrame < MOTION_FRAMES_PER_CASE && elapsed >= 5.0D + motionFrame) {
                capture(mc, String.format(Locale.ROOT, "%s-motion-%02d", selected.name(), motionFrame), false);
                measureMotion(mc, motionFrame++);
            }
            if (!stageCaptured && elapsed >= selected.dwellSeconds() - 0.7D) {
                capture(mc, selected.name(), true);
                stageCaptured = true;
            }
            if (stageCaptured && elapsed >= selected.dwellSeconds()) {
                if (selected.observeMotion()) summarizeMotionCase();
                beginStage(mc, stage == STAGES.length - 1 ? -1 : stage + 1);
            }
        } catch (Exception exception) {
            failure = exception.toString();
            TrippyCraft.LOGGER.error("[LSD smoke] Native renderer validation failed", exception);
            finish(mc);
        }
    }

    /** Native timeline pins isolate authored phases while the renderer and palette clock advance. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED || finished || pinnedAge < 0 || playerId == null) return;
        try {
            var saved = SubstanceEngine.getExperience(playerId).orElseThrow().save();
            var lsd = saved.getCompound("lsd");
            lsd.putInt("ageTicks", pinnedAge);
            lsd.putDouble("visualEnvelope", pinnedEnvelope);
            lsd.putDouble("activeDoseMicrograms", pinnedDose);
            lsd.putDouble("displayedDoseMicrograms", pinnedDose);
            // Phase/dose comparisons isolate the authored curve, so accumulated QA tolerance stays neutral.
            saved.put("tolerance", new net.minecraft.nbt.CompoundTag());
            SubstanceEngine.setExperience(playerId, PsychoactiveExperience.load(saved));
            // Publish the fixed native fixture after production player ticking, avoiding a one-tick tolerated dose.
            var server = Minecraft.getInstance().getSingleplayerServer();
            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(playerId);
            if (player != null) SubstanceEngine.sync(player);
        } catch (Exception exception) { failure = "Native LSD phase fixture failed: " + exception; }
    }

    public static void beforeFov(ViewportEvent.ComputeFov event) {
        if (ENABLED && event.usedConfiguredFov()) nativeFov = event.getFOV();
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!ENABLED || finished || !stageReady || !event.usedConfiguredFov()) return;
        double modification = Math.abs(event.getFOV() - nativeFov);
        maxFovModification = Math.max(maxFovModification, modification);
        measuredFovFrames++;
        if (!Double.isFinite(modification) || modification > 0.00001D) failure = "Pure LSD changed native FOV by " + modification + "deg";
        double settledError = Math.abs(event.getFOV() - 70.0D);
        if (!observingFade && STAGES[stage].progress() >= 0.0D && seconds(stageNanos) > 4.0D) {
            measuredSettledFovFrames++;
            maxSettledFovError = Math.max(maxSettledFovError, settledError);
            if (!Double.isFinite(settledError) || settledError > 0.002D) {
                var player = Minecraft.getInstance().player;
                failure = "LSD hidden movement buffs changed the settled native projection by " + settledError + "deg at " + stageName()
                        + "; sprint=" + player.isSprinting() + ", flying=" + player.getAbilities().flying
                        + ", move=" + player.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        + ", speed=" + player.getEffect(MobEffects.MOVEMENT_SPEED)
                        + ", slow=" + player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
        var state = ClientExperienceStore.lsd();
        FOV_SAMPLES.add(String.format(Locale.ROOT, "%.6f,%s,%.3f,%s,%.6f,%.9f,%.9f,%.9f,%.9f",
                seconds(startNanos), stageName(), state.activeDoseMicrograms(), state.phase(),
                LsdPresentationClock.timeTicks((float) event.getPartialTick()), nativeFov, event.getFOV(), modification, settledError));
    }

    public static void beforeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ENABLED) return;
        nativeYaw = event.getYaw();
        nativePitch = event.getPitch();
        nativeRoll = event.getRoll();
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ENABLED || finished || !stageReady) return;
        double modification = Math.max(Math.abs(event.getYaw() - nativeYaw),
                Math.max(Math.abs(event.getPitch() - nativePitch), Math.abs(event.getRoll() - nativeRoll)));
        maxCameraModification = Math.max(maxCameraModification, modification);
        measuredCameraFrames++;
        if (!Double.isFinite(modification) || modification > 0.00001D) failure = "Pure LSD moved the native camera by " + modification + "deg";
        var state = ClientExperienceStore.lsd();
        CAMERA_SAMPLES.add(String.format(Locale.ROOT, "%.6f,%s,%.3f,%s,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f",
                seconds(startNanos), stageName(), state.activeDoseMicrograms(), state.phase(),
                nativeYaw, event.getYaw(), nativePitch, event.getPitch(), nativeRoll, event.getRoll(), modification));
    }

    public static void beforeWorldEffects(RenderLevelStageEvent event) {
        if (!ENABLED || finished || pendingPair == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        try {
            if (originalWorld != null) originalWorld.close();
            originalWorld = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            dumpRenderFixture(event, pendingPair);
        } catch (Exception exception) { failure = "Original world capture failed: " + exception; }
    }

    public static void afterWorldEffects(RenderLevelStageEvent event) {
        if (!ENABLED || finished || pendingPair == null || originalWorld == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        String name = pendingPair;
        try (NativeImage presented = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int width = Math.min(originalWorld.getWidth(), presented.getWidth());
            int height = Math.min(originalWorld.getHeight(), presented.getHeight());
            int left = (int) (width * 0.18D), right = (int) (width * 0.82D);
            int top = (int) (height * 0.12D), bottom = (int) (height * 0.68D);
            double squared = 0.0D, absolute = 0.0D;
            double sourceLuma = 0.0D, shownLuma = 0.0D, sourceChroma = 0.0D, shownChroma = 0.0D;
            int count = 0, changed = 0;
            int flatCount = 0, flatSpikes = 0;
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    int original = originalWorld.getPixelRGBA(x, y);
                    int shown = presented.getPixelRGBA(x, y);
                    int largest = 0;
                    for (int shift = 0; shift < 24; shift += 8) {
                        int delta = Math.abs(((original >>> shift) & 255) - ((shown >>> shift) & 255));
                        squared += delta * delta;
                        absolute += delta;
                        largest = Math.max(largest, delta);
                    }
                    if (largest > 8) changed++;
                    sourceLuma += luminance(original);
                    shownLuma += luminance(shown);
                    sourceChroma += chroma(original);
                    shownChroma += chroma(shown);
                    if (x > left && x < right - 1 && y > top && y < bottom - 1 && sourceFlat(originalWorld, x, y)) {
                        flatCount++;
                        if (isolatedSpike(presented, x, y)) flatSpikes++;
                    }
                    count++;
                }
            }
            if (count == 0) throw new IllegalStateException("Empty world pixel sample");
            double rms = Math.sqrt(squared / (count * 3.0D));
            double mad = absolute / (count * 3.0D);
            double fraction = changed / (double) count;
            sourceLuma /= count;
            shownLuma /= count;
            sourceChroma /= count;
            shownChroma /= count;
            double flatSpikeFraction = flatSpikes / (double) Math.max(1, flatCount);
            TextureMatch textureMatch = matchTexturePatches(originalWorld, presented, left, right, top, bottom);
            var state = ClientExperienceStore.lsd();
            String measurement = String.format(Locale.ROOT, "%s,%s,%d,%.3f,%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%d,%.6f",
                    name, STAGES[stage].scene(), state.ageTicks(), state.activeDoseMicrograms(), state.phase(), state.intensity(),
                    LsdWorldWarpPass.shaderIntensity(state), LsdWorldWarpPass.currentShaderIntensity(), rms, mad, fraction,
                    sourceLuma, shownLuma, sourceChroma, shownChroma, flatSpikeFraction, textureMatch.patches(), textureMatch.displacement());
            PIXEL_SAMPLES.add(measurement);
            log("Same-frame world pixels: " + measurement);
            var directory = mc.gameDirectory.toPath().resolve("shader-pairs");
            Files.createDirectories(directory);
            originalWorld.writeToFile(directory.resolve(name + "-original.png"));
            presented.writeToFile(directory.resolve(name + "-presented.png"));
            Files.writeString(mc.gameDirectory.toPath().resolve("render-fixtures").resolve(name + "-shader-channels-after-draw.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(LsdWorldWarpPass.currentShaderChannels()));
            pairedCaptures++;
            if ((STAGES[stage].micrograms() == 0.0D && stage != STAGES.length - 1 || name.equals("natural-immediate")) && rms > 0.15D) failure = "LSD raised color before natural onset: RMS=" + rms;
            if (STAGES[stage].observeMotion()) {
                double requiredFraction = STAGES[stage].micrograms() >= 500.0D ? 0.25D : 0.15D;
                if (rms < 8.0D || fraction < requiredFraction) failure = "LSD world field remained weak at " + name + ": RMS=" + rms + ", changed=" + fraction;
                if (shownLuma < sourceLuma * 0.85D || shownLuma < 6.0D) failure = "LSD buried scene readability at " + name + ": source=" + sourceLuma + ", presented=" + shownLuma;
                if (shownChroma < sourceChroma * 0.90D) failure = "LSD muted rather than enriched the scene at " + name;
                if (flatSpikeFraction > 0.06D) failure = "LSD introduced isolated per-pixel grain on flat surfaces at " + name + ": " + flatSpikeFraction;
                if (STAGES[stage].micrograms() <= 500.0D && textureMatch.patches() >= 3 && textureMatch.displacement() < 0.35D) {
                    failure = "Ordinary LSD changed colors without measurable texture displacement at " + name + ": " + textureMatch.displacement() + "px";
                }
            }
        } catch (Exception exception) {
            failure = "Same-frame LSD pixel validation failed: " + exception;
        } finally {
            originalWorld.close();
            originalWorld = null;
            pendingPair = null;
        }
    }

    private static void capture(Minecraft mc, String name, boolean checkpoint) {
        var state = ClientExperienceStore.lsd();
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new IllegalStateException("OpenGL error at " + name);
        if (checkpoint) {
            boolean nativeTail = stage == STAGES.length - 1;
            boolean sober = STAGES[stage].micrograms() == 0.0D && !nativeTail;
            if (stage > 0 && !nativeTail && !sober && (!state.active() || !Double.isFinite(state.visualIntensity()))) throw new IllegalStateException("Invalid synchronized LSD state at " + name);
            if (nativeTail && state.active()) throw new IllegalStateException("Native LSD remained active during its residual-family tail");
            if (nativeTail && ClientEffectGraph.sample(1.0F).familyBlend().strength(com.trippycraft.substance.SubstanceFamily.PSYCHEDELIC) <= 0.0D) {
                throw new IllegalStateException("The native-expiry fixture lost its residual psychedelic family channels");
            }
            // Normal player ticking can rebuild one tick of tolerance before the next snapshot.
            // Permit 0.1% response drift while rejecting an incompletely absorbed high-dose fixture.
            if (STAGES[stage].progress() >= 0.0D && !nativeTail && !sober && Math.abs(state.activeDoseMicrograms() - STAGES[stage].micrograms())
                    > Math.max(0.001D, STAGES[stage].micrograms() * 0.001D)) {
                throw new IllegalStateException("Pinned native dose mismatch at " + name + ": expected " + STAGES[stage].micrograms() + "ug, got " + state.activeDoseMicrograms());
            }
            if (mc.player.hasEffect(MobEffects.CONFUSION)) throw new IllegalStateException("Pure LSD added the vanilla Nausea debuff at " + name);
            if (mc.options.hideGui || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                    || !hit.getBlockPos().equals(new BlockPos(0, 65, 6))) {
                throw new IllegalStateException("The QA camera is not exercising the reachable native bookshelf face at " + name);
            }
            if (name.equals("natural-immediate") && state.visualIntensity() > 0.01D) throw new IllegalStateException("Natural LSD ingestion immediately raised visual strength: " + state.visualIntensity());
            if (STAGES[stage].progress() < 0.0D) {
                if (state.ageTicks() < naturalAge) throw new IllegalStateException("Natural LSD timeline moved backwards");
                naturalAge = state.ageTicks();
            }
            if (STAGES[stage].observeMotion() && STAGES[stage].progress() == 0.46D) {
                double strength = LsdWorldWarpPass.currentShaderIntensity();
                Scene scene = STAGES[stage].scene();
                if (state.intensity() <= PREVIOUS_PEAK_INTENSITY.getOrDefault(scene, 0.0D)
                        || strength <= PREVIOUS_SHADER_STRENGTH.getOrDefault(scene, 0.0D)) {
                    throw new IllegalStateException("Higher native peak LSD dose failed to increase actual visual strength: " + name);
                }
                PREVIOUS_PEAK_INTENSITY.put(scene, state.intensity());
                PREVIOUS_SHADER_STRENGTH.put(scene, strength);
                peakComparisons++;
            }
            log(String.format(Locale.ROOT, "%s: native dose=%.0fug phase=%s intensity=%.6f shader_target=%.6f shader_actual=%.6f",
                    name, state.activeDoseMicrograms(), state.phase(), state.intensity(),
                    LsdWorldWarpPass.shaderIntensity(state), LsdWorldWarpPass.currentShaderIntensity()));
            if (pendingPair != null) throw new IllegalStateException("A same-frame world capture is still pending");
            pendingPair = name;
            try {
                var stateDirectory = mc.gameDirectory.toPath().resolve("native-states");
                Files.createDirectories(stateDirectory);
                Files.writeString(stateDirectory.resolve(name + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(state));
            } catch (Exception exception) { throw new IllegalStateException("Could not export actual native LSD state", exception); }
        }
        String file = "lsd-" + name + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(file))) CAPTURES.incrementAndGet();
            else failure = "Screenshot failed: " + message.getString();
        });
    }

    /** Report actual captured frame changes, including coherent palette motion, without HUD pixels. */
    private static void measureMotion(Minecraft mc, int frame) {
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int left = (int) (image.getWidth() * 0.18D), right = (int) (image.getWidth() * 0.82D);
            int top = (int) (image.getHeight() * 0.12D), bottom = (int) (image.getHeight() * 0.68D);
            List<Double> values = new ArrayList<>();
            double[] means = new double[3];
            int count = 0;
            for (int y = top; y < bottom; y += 5) {
                for (int x = left; x < right; x += 5) {
                    int rgba = image.getPixelRGBA(x, y);
                    for (int c = 0; c < 3; c++) {
                        double value = (rgba >>> (c * 8)) & 255;
                        values.add(value);
                        means[c] += value;
                    }
                    count++;
                }
            }
            double[] pixels = values.stream().mapToDouble(Double::doubleValue).toArray();
            for (int c = 0; c < 3; c++) means[c] /= count;
            double meanStep = 0.0D, movement = 0.0D;
            double time = LsdPresentationClock.timeTicks(1.0F) / 20.0D;
            double colorSpeed = 0.0D;
            if (previousMotionPixels != null) {
                for (int i = 0; i < pixels.length; i++) movement += Math.abs(pixels[i] - previousMotionPixels[i]);
                movement /= pixels.length;
                for (int c = 0; c < 3; c++) meanStep = Math.max(meanStep, Math.abs(means[c] - previousMotionMeans[c]));
                largestMeanColorStep = Math.max(largestMeanColorStep, meanStep);
                largestMotionDelta = Math.max(largestMotionDelta, movement);
                totalMotionDelta += movement;
                motionMeasurements++;
                stageMotionMeasurements++;
                stageMotionDelta += movement;
                colorSpeed = meanStep / Math.max(0.01D, time - previousMotionTime);
                stageLargestColorSpeed = Math.max(stageLargestColorSpeed, colorSpeed);
            }
            previousMotionPixels = pixels;
            previousMotionMeans = means;
            previousMotionTime = time;
            var state = ClientExperienceStore.lsd();
            MOTION_SAMPLES.add(String.format(Locale.ROOT, "%s,%s,%d,%.6f,%.3f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                    STAGES[stage].name(), STAGES[stage].scene(), frame, time * 20.0D, state.activeDoseMicrograms(),
                    LsdWorldWarpPass.currentShaderIntensity(), means[0], means[1], means[2], meanStep, movement, colorSpeed));
        }
    }

    private static void summarizeMotionCase() {
        double meanMovement = stageMotionDelta / Math.max(1, stageMotionMeasurements);
        log(String.format(Locale.ROOT, "%s autonomous motion5..15s: frames=%d mean_pixel_movement=%.6f/255 max_mean_color_speed=%.6f/255/s",
                STAGES[stage].name(), motionFrame, meanMovement, stageLargestColorSpeed));
        if (motionFrame != MOTION_FRAMES_PER_CASE || stageMotionMeasurements != MOTION_FRAMES_PER_CASE - 1) {
            throw new IllegalStateException("Incomplete ten-second stationary motion observation at " + stageName());
        }
        if (meanMovement < 0.50D) throw new IllegalStateException("World field remained nearly static at " + stageName() + ": " + meanMovement + "/255 per second");
        if (stageLargestColorSpeed > 35.0D) throw new IllegalStateException("Palette brightness jumped abruptly at " + stageName() + ": " + stageLargestColorSpeed + "/255/s");
        completedMotionCases++;
    }

    private static void beginStage(Minecraft mc, int next) {
        stageReady = false;
        pinnedAge = -1;
        mc.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Missing integrated player at dose transition");
                if (next >= 0) configureScene(player, STAGES[next].scene());
                boolean nativeTail = next == STAGES.length - 1;
                if (!nativeTail) SubstanceEngine.reset(player);
                if (nativeTail) {
                    // Expire only the actual LSD session, retaining real family/channel decay for fallback QA.
                    var saved = SubstanceEngine.getExperience(playerId).orElseThrow().save();
                    var lsd = saved.getCompound("lsd");
                    lsd.putInt("ageTicks", lsd.getInt("durationTicks"));
                    lsd.putDouble("visualEnvelope", 0.0D);
                    SubstanceEngine.setExperience(playerId, PsychoactiveExperience.load(saved));
                    log("Native LSD expired; observe residual psychedelic family tail without an experience reset");
                } else if (next >= 0 && STAGES[next].micrograms() > 0.0D) {
                    Stage selected = STAGES[next];
                    useNativeDose(player, selected.micrograms());
                    var experience = SubstanceEngine.getExperience(playerId).orElseThrow();
                    if (experience.lsdState().targetDoseMicrograms() != selected.micrograms()) throw new IllegalStateException("Native blotters applied the wrong total dose");
                    var saved = experience.save();
                    var lsd = saved.getCompound("lsd");
                    lsd.put("signature", LsdTripSignature.create(SIGNATURE_SEED).save());
                    if (selected.progress() >= 0.0D) {
                        int age = (int) (experience.lsdState().durationTicks() * selected.progress());
                        pinnedEnvelope = LsdDoseCurve.phaseEnvelope(LsdDoseCurve.phase(age, experience.lsdState().durationTicks()), selected.progress());
                        lsd.putInt("ageTicks", age);
                        lsd.putDouble("visualEnvelope", pinnedEnvelope);
                        lsd.putDouble("activeDoseMicrograms", selected.micrograms());
                        lsd.putDouble("displayedDoseMicrograms", selected.micrograms());
                        saved.put("tolerance", new net.minecraft.nbt.CompoundTag());
                        pinnedDose = selected.micrograms();
                        pinnedAge = age;
                        log("Consumed native blotters for " + selected.micrograms() + "ug; QA holds phase at progress " + selected.progress());
                    } else {
                        if (experience.lsdState().visualIntensity() != 0.0D) throw new IllegalStateException("Fresh native ingestion did not start at zero");
                        log("Consumed native 250ug blotter; observe 35s natural onset without scrubbing");
                    }
                    SubstanceEngine.setExperience(playerId, PsychoactiveExperience.load(saved));
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LSD_BLOTTER_250UG.get()));
                SubstanceEngine.sync(player);
                mc.execute(() -> {
                    observingFade = next < 0;
                    if (next >= 0) stage = next;
                    stageCaptured = false;
                    motionFrame = 0;
                    stageMotionMeasurements = 0;
                    stageMotionDelta = 0.0D;
                    stageLargestColorSpeed = 0.0D;
                    previousMotionPixels = null;
                    previousMotionMeans = null;
                    stageNanos = 0L;
                    stageTransitionPending = true;
                });
            } catch (Exception exception) { failure = exception.toString(); }
        });
    }

    /** Use real registered items; instabuild is temporarily disabled to check their consumption. */
    private static void useNativeDose(ServerPlayer player, double dose) {
        boolean creative = player.getAbilities().instabuild;
        player.getAbilities().instabuild = false;
        try {
            double remaining = dose;
            for (var holder : List.of(ModItems.LSD_BLOTTER_500UG, ModItems.LSD_BLOTTER_250UG)) {
                int content = holder == ModItems.LSD_BLOTTER_500UG ? 500 : 250;
                while (remaining >= content) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(holder.get()));
                    holder.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                    if (!player.getMainHandItem().isEmpty()) throw new IllegalStateException("Survival LSD blotter was not consumed");
                    remaining -= content;
                }
            }
            if (remaining != 0.0D) throw new IllegalStateException("Unsupported native smoke dose: " + dose);
        } finally { player.getAbilities().instabuild = creative; }
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
                    Block block = y == 64 ? Math.floorMod(x + z, 3) == 0 ? Blocks.STONE_BRICKS : Blocks.OAK_PLANKS : Blocks.AIR;
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
        // The crosshair reaches this face, so native highlighted-block texture geometry is exercised.
        for (int y = 65; y <= 68; y++) level.setBlock(new BlockPos(0, y, 6), Blocks.BOOKSHELF.defaultBlockState(), 3);
        player.teleportTo(level, 0.5D, 65.0D, 2.5D, 0.0F, 12.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LSD_BLOTTER_250UG.get()));
        SubstanceEngine.reset(player);
        configureScene(player, Scene.DAY);
    }

    private static void configureScene(ServerPlayer player, Scene scene) {
        if (configuredScene == scene) return;
        var level = player.serverLevel();
        level.setDayTime(scene == Scene.TAVERN ? 18000L : 6000L);
        for (int x = -10; x <= 10; x++) {
            for (int z = 0; z <= 14; z++) {
                level.setBlock(new BlockPos(x, 72, z), (scene == Scene.TAVERN ? Blocks.SPRUCE_PLANKS : Blocks.AIR).defaultBlockState(), 3);
                if (x == -10 || x == 10 || z == 0) {
                    for (int y = 70; y <= 71; y++) level.setBlock(new BlockPos(x, y, z),
                            (scene == Scene.TAVERN ? Blocks.SPRUCE_PLANKS : Blocks.AIR).defaultBlockState(), 3);
                }
            }
            for (int y = 65; y <= 69; y++) level.setBlock(new BlockPos(x, y, 0),
                    (scene == Scene.TAVERN ? Blocks.SPRUCE_PLANKS : Blocks.AIR).defaultBlockState(), 3);
        }
        for (int x : new int[]{-8, 8}) {
            level.setBlock(new BlockPos(x, 65, 10), (scene == Scene.TAVERN ? Blocks.LANTERN : Blocks.AIR).defaultBlockState(), 3);
        }
        configuredScene = scene;
        log("Scene changed to " + scene + ": fixed daylight or roofed nighttime tavern with distant lanterns");
    }

    private static double luminance(int rgba) {
        return (rgba & 255) * 0.2126D + ((rgba >>> 8) & 255) * 0.7152D + ((rgba >>> 16) & 255) * 0.0722D;
    }

    private static double chroma(int rgba) {
        int r = rgba & 255, g = (rgba >>> 8) & 255, b = (rgba >>> 16) & 255;
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
    }

    private static boolean sourceFlat(NativeImage image, int x, int y) {
        int center = image.getPixelRGBA(x, y);
        int[] neighbours = {image.getPixelRGBA(x - 1, y), image.getPixelRGBA(x + 1, y), image.getPixelRGBA(x, y - 1), image.getPixelRGBA(x, y + 1)};
        for (int c = 0; c < 3; c++) {
            int value = (center >>> (c * 8)) & 255;
            for (int pixel : neighbours) if (Math.abs(value - ((pixel >>> (c * 8)) & 255)) > 4) return false;
        }
        return true;
    }

    private static boolean isolatedSpike(NativeImage image, int x, int y) {
        int center = image.getPixelRGBA(x, y);
        int[] neighbours = {image.getPixelRGBA(x - 1, y), image.getPixelRGBA(x + 1, y), image.getPixelRGBA(x, y - 1), image.getPixelRGBA(x, y + 1)};
        for (int c = 0; c < 3; c++) {
            int minimum = 255, maximum = 0;
            for (int pixel : neighbours) {
                int value = (pixel >>> (c * 8)) & 255;
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
            int value = (center >>> (c * 8)) & 255;
            if (value > maximum + 8 || value < minimum - 8) return true;
        }
        return false;
    }

    /** Local normalized-luminance matches separate actual texture displacement from a color filter. */
    private static TextureMatch matchTexturePatches(NativeImage source, NativeImage shown, int left, int right, int top, int bottom) {
        int patches = 0;
        double displacement = 0.0D;
        for (int row = 1; row <= 3; row++) {
            for (int column = 1; column <= 5; column++) {
                int x = left + (right - left) * column / 6;
                int y = top + (bottom - top) * row / 4;
                double[] output = normalizedPatch(shown, x, y);
                if (output == null) continue;
                double best = Double.POSITIVE_INFINITY;
                int bestX = 0, bestY = 0;
                for (int dy = -6; dy <= 6; dy += 2) {
                    for (int dx = -6; dx <= 6; dx += 2) {
                        double[] input = normalizedPatch(source, x + dx, y + dy);
                        if (input == null) continue;
                        double error = 0.0D;
                        for (int i = 0; i < input.length; i++) error += Math.pow(input[i] - output[i], 2.0D);
                        error /= input.length;
                        if (error < best - 0.000001D || Math.abs(error - best) < 0.000001D && dx * dx + dy * dy < bestX * bestX + bestY * bestY) {
                            best = error;
                            bestX = dx;
                            bestY = dy;
                        }
                    }
                }
                if (best < 1.20D) {
                    patches++;
                    displacement += Math.hypot(bestX, bestY);
                }
            }
        }
        return new TextureMatch(patches, displacement / Math.max(1, patches));
    }

    private static double[] normalizedPatch(NativeImage image, int x, int y) {
        if (x < 15 || y < 15 || x >= image.getWidth() - 15 || y >= image.getHeight() - 15) return null;
        double[] values = new double[81];
        int slot = 0;
        double mean = 0.0D;
        for (int dy = -12; dy <= 12; dy += 3) {
            for (int dx = -12; dx <= 12; dx += 3) {
                values[slot] = luminance(image.getPixelRGBA(x + dx, y + dy));
                mean += values[slot++];
            }
        }
        mean /= values.length;
        double variance = 0.0D;
        for (double value : values) variance += (value - mean) * (value - mean);
        double deviation = Math.sqrt(variance / values.length);
        if (deviation < 4.0D) return null;
        for (int i = 0; i < values.length; i++) values[i] = (values[i] - mean) / deviation;
        return values;
    }

    private record TextureMatch(int patches, double displacement) { }

    private static void dumpRenderFixture(RenderLevelStageEvent event, String name) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var directory = mc.gameDirectory.toPath().resolve("render-fixtures");
        Files.createDirectories(directory);
        Matrix4f viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        var camera = event.getCamera();
        var near = camera.getNearPlane();
        Vec3 forward = near.getPointOnPlane(0.0F, 0.0F);
        Vec3 right = near.getPointOnPlane(1.0F, 0.0F).subtract(forward);
        Vec3 up = near.getPointOnPlane(0.0F, 1.0F).subtract(forward);
        Map<String, Object> fixture = new LinkedHashMap<>();
        fixture.put("checkpoint", name);
        fixture.put("scene", STAGES[stage].scene().name());
        fixture.put("width", originalWorld.getWidth());
        fixture.put("height", originalWorld.getHeight());
        fixture.put("color_png_orientation", "top-left first; PNG from Screenshot.takeScreenshot");
        fixture.put("depth_bin_orientation", "OpenGL bottom-left first; float32 little-endian, normalized depth");
        fixture.put("matrix_layout", "column-major float arrays");
        fixture.put("renderer_ticks", LsdPresentationClock.timeTicks(event.getPartialTick().getGameTimeDeltaPartialTick(false)));
        fixture.put("projection", event.getProjectionMatrix().get(new float[16]));
        fixture.put("model_view", event.getModelViewMatrix().get(new float[16]));
        fixture.put("event_view_projection", viewProjection.get(new float[16]));
        fixture.put("event_inverse_view_projection", new Matrix4f(viewProjection).invert().get(new float[16]));
        fixture.put("camera_eye", new double[]{camera.getPosition().x, camera.getPosition().y, camera.getPosition().z});
        fixture.put("camera_forward", new double[]{forward.x, forward.y, forward.z});
        fixture.put("camera_right", new double[]{right.x, right.y, right.z});
        fixture.put("camera_up", new double[]{up.x, up.y, up.z});
        fixture.put("camera_near", net.minecraft.client.renderer.GameRenderer.PROJECTION_Z_NEAR);
        fixture.put("camera_far", mc.gameRenderer.getDepthFar());
        fixture.put("native_state", ClientExperienceStore.lsd());
        fixture.put("shader_channels_before_draw", LsdWorldWarpPass.currentShaderChannels());
        boolean depthCase = STAGES[stage].observeMotion() && (STAGES[stage].micrograms() <= 500.0D
                || STAGES[stage].micrograms() == 1000.0D && STAGES[stage].scene() == Scene.DAY);
        if (depthCase) {
            var target = mc.getMainRenderTarget();
            int texture = target.getDepthTextureId();
            if (texture <= 0) throw new IllegalStateException("Native world has no depth texture at " + name);
            FloatBuffer depth = BufferUtils.createFloatBuffer(target.width * target.height);
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
            } finally { GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous); }
            ByteBuffer bytes = ByteBuffer.allocate(depth.capacity() * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
            double minimum = 1.0D, maximum = 0.0D;
            int geometry = 0;
            for (int i = 0; i < depth.capacity(); i++) {
                float value = depth.get(i);
                if (!Float.isFinite(value)) throw new IllegalStateException("Nonfinite actual depth value");
                bytes.putFloat(value);
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
                if (value < 0.999999F) geometry++;
            }
            Files.write(directory.resolve(name + "-depth-f32le.bin"), bytes.array());
            fixture.put("depth_texture_id", texture);
            fixture.put("depth_min", minimum);
            fixture.put("depth_max", maximum);
            fixture.put("depth_geometry_fraction", geometry / (double) depth.capacity());
            log(String.format(Locale.ROOT, "%s actual depth: min=%.9f max=%.9f textured_geometry_fraction=%.6f", name, minimum, maximum, geometry / (double) depth.capacity()));
        }
        Files.writeString(directory.resolve(name + "-render.json"), new GsonBuilder().setPrettyPrinting().create().toJson(fixture));
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        pinnedAge = -1;
        if (originalWorld != null) {
            originalWorld.close();
            originalWorld = null;
        }
        if (failure == null && (measuredFovFrames < 120 || measuredCameraFrames < 120)) failure = "Insufficient native projection observations";
        if (failure == null && measuredSettledFovFrames < 120) failure = "Insufficient settled raw native projection observations";
        if (failure == null && peakComparisons != EXPECTED_PEAK_COMPARISONS) failure = "Native uncapped day/night peak comparisons were incomplete";
        if (failure == null && (completedMotionCases != MOTION_CASES || motionMeasurements != MOTION_CASES * (MOTION_FRAMES_PER_CASE - 1))) failure = "Autonomous day/night texture/palette motion was incomplete";
        log(String.format(Locale.ROOT, "Native projection unchanged: FOV_frames=%d camera_frames=%d FOV_delta=%.9fdeg camera_delta=%.9fdeg",
                measuredFovFrames, measuredCameraFrames, maxFovModification, maxCameraModification));
        log(String.format(Locale.ROOT, "Stationary configured 70deg projection: settled_frames=%d max_raw_FOV_error=%.9fdeg; hidden LSD Speed/Slowness lens and residual-family tail checked",
                measuredSettledFovFrames, maxSettledFovError));
        log(String.format(Locale.ROOT, "Native250/500/1000/5000ug Peak/Wave day/tavern motion: cases=%d frames=%d largest_mean_RGB_step=%.6f/255 max_pixel_step=%.6f/255 mean_pixel_step=%.6f/255",
                completedMotionCases, completedMotionCases * MOTION_FRAMES_PER_CASE, largestMeanColorStep, largestMotionDelta, totalMotionDelta / Math.max(1, motionMeasurements)));
        log((PALETTE_ONLY ? "Selected day/night palette regression checkpoints" : "Natural 250ug ingestion starts at zero, observed for 35s without timeline scrub")
                + "; same-frame shader pairs=" + pairedCaptures);
        log((failure == null ? "PASS" : "FAIL") + ": LSD native client smoke; screenshots=" + CAPTURES.get());
        if (failure != null) log("FAIL: " + failure);
        try {
            synchronized (REPORT) { Files.writeString(mc.gameDirectory.toPath().resolve("lsd-smoke-result.txt"), String.join(System.lineSeparator(), REPORT)); }
            Files.writeString(mc.gameDirectory.toPath().resolve("lsd-fov.csv"), String.join(System.lineSeparator(), FOV_SAMPLES));
            Files.writeString(mc.gameDirectory.toPath().resolve("lsd-camera.csv"), String.join(System.lineSeparator(), CAMERA_SAMPLES));
            Files.writeString(mc.gameDirectory.toPath().resolve("lsd-pixel-deltas.csv"), String.join(System.lineSeparator(), PIXEL_SAMPLES));
            Files.writeString(mc.gameDirectory.toPath().resolve("lsd-motion.csv"), String.join(System.lineSeparator(), MOTION_SAMPLES));
        } catch (Exception exception) { TrippyCraft.LOGGER.error("[LSD smoke] Could not save QA artifacts", exception); }
        mc.stop();
    }

    private static String stageName() { return observingFade ? "fade" : STAGES[stage].name(); }
    private static boolean synchronizedStageReady() {
        var state = ClientExperienceStore.lsd();
        if (observingFade || stage == STAGES.length - 1 || STAGES[stage].micrograms() == 0.0D) return !state.active();
        Stage selected = STAGES[stage];
        if (!state.active() || Math.abs(state.targetDoseMicrograms() - selected.micrograms()) > selected.micrograms() * 0.001D) return false;
        return selected.progress() < 0.0D ? state.ageTicks() < 20 : Math.abs(state.progress() - selected.progress()) < 0.001D;
    }
    private static double seconds(long since) { return (System.nanoTime() - since) / 1_000_000_000.0D; }
    private static void log(String message) {
        TrippyCraft.LOGGER.info("[LSD smoke] {}", message);
        synchronized (REPORT) { REPORT.add(message); }
    }
    private enum Scene { DAY, TAVERN }
    private record Stage(String name, double micrograms, double progress, double dwellSeconds, Scene scene, boolean observeMotion) {
        private Stage(String name, double micrograms, double progress, double dwellSeconds) {
            this(name, micrograms, progress, dwellSeconds, Scene.DAY, false);
        }
    }
}
