package com.trippycraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.allad.AlLadDoseCurve;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadTripSignature;
import com.trippycraft.substance.allad.AlLadPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in development QA: actual items, integrated-server snapshots, HUD and world captures. */
public final class AlLadClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.alLadSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "allad-material-flow-smoke";
    private static final long SIGNATURE = 0xA11AD54321L;
    private static final Stage[] STAGES = {
            new Stage("00-sober", 0, Mode.NORMAL),
            new Stage("01-25mg", 25, Mode.NORMAL),
            new Stage("02-100mg", 100, Mode.NORMAL),
            new Stage("03-250mg", 250, Mode.NORMAL),
            new Stage("04-500mg", 500, Mode.NORMAL),
            new Stage("05-1000mg", 1000, Mode.NORMAL),
            new Stage("06-3000mg", 3000, Mode.NORMAL),
            new Stage("07-5000mg", 5000, Mode.NORMAL),
            new Stage("08-natural-100mg-onset", 100, Mode.NATURAL_ONSET),
            new Stage("09-5000mg-motion-off", 5000, Mode.MOTION_OFF),
            new Stage("10-5000mg-visual-off", 5000, Mode.VISUAL_OFF),
            new Stage("11-native-expiry", 5000, Mode.EXPIRED),
            new Stage("12-mixed-100mg-psilocybin-lsd", 100, Mode.MIXED),
            new Stage("13-mixed-500mg-twoce", 500, Mode.MIXED_TWO_CE)
    };
    private static final List<String> REPORT = new ArrayList<>();
    private static final AtomicInteger SAVED = new AtomicInteger();
    private static boolean started, setupQueued, finished;
    private static volatile boolean sceneReady, stageReady, failed;
    private static long startNanos, queuedNanos, settledNanos;
    private static int stageIndex = -1;
    private static double originalMotion, originalVisual;
    private static boolean controlsSaved;

    private AlLadClientSmokeTest() { }

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
                    throw new IllegalStateException("Dedicated smoke world already exists; refusing to open or replace it");
                }
                mc.options.pauseOnLostFocus = false;
                mc.options.onboardAccessibility = false;
                mc.options.hideGui = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.renderDistance().set(5);
                mc.options.simulationDistance().set(5);
                mc.options.fov().set(75);
                mc.options.gamma().set(0.7D);
                log("Creating fresh isolated world in " + mc.gameDirectory.getAbsolutePath());
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings,
                        new WorldOptions(SIGNATURE, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                return;
            }
            if (seconds(startNanos) > 240.0D) throw new IllegalStateException("Native smoke timed out");
            if (failed) { finish(mc); return; }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            UUID playerId = mc.player.getUUID();
            if (!setupQueued) {
                setupQueued = true;
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                        if (player == null) throw new IllegalStateException("Integrated player disappeared before scene setup");
                        buildScene(player);
                        sceneReady = true;
                    } catch (Exception exception) { fail("Fixture creation failed: " + exception); }
                });
                return;
            }
            if (!sceneReady || mc.player.distanceToSqr(0.5D, 65.0D, 2.5D) > 16.0D) return;
            if (!controlsSaved) {
                controlsSaved = true;
                originalMotion = TrippyClientConfig.motionIntensity;
                originalVisual = TrippyClientConfig.visualIntensity;
                mc.setScreen(null);
                log("Fixed first-person scene: bookshelves, stone, timber, copper, foliage, roof skylight, visible hand and HUD");
            }
            mc.player.setYRot(0.0F);
            mc.player.setXRot(5.0F);
            if (stageIndex < 0) { selectStage(mc, playerId, 0); return; }
            if (stageIndex >= STAGES.length) {
                if (SAVED.get() == STAGES.length) finish(mc);
                else if (seconds(queuedNanos) > 15.0D) throw new IllegalStateException("Not all native screenshots were saved");
                return;
            }
            Stage selected = STAGES[stageIndex];
            if (!stageReady || !synchronizedStage(selected)) {
                if (seconds(queuedNanos) > 15.0D) throw new IllegalStateException("Native snapshot did not synchronize for " + selected.name());
                return;
            }
            if (settledNanos == 0L) {
                settledNanos = System.nanoTime();
                log("Native packet synchronized: " + selected.name());
            }
            if (seconds(settledNanos) < 5.0D) return;
            capture(mc, selected);
            selectStage(mc, playerId, stageIndex + 1);
        } catch (Exception exception) {
            fail("Client smoke failed: " + exception);
            TrippyCraft.LOGGER.error("[AL-LAD smoke] Native client validation failed", exception);
            finish(mc);
        }
    }

    private static void buildScene(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, player.server);
        level.setDayTime(6000L);
        level.setWeatherParameters(6000, 0, false, false);
        for (int x = -10; x <= 10; x++) {
            for (int z = 0; z <= 16; z++) {
                for (int y = 64; y <= 74; y++) {
                    BlockState block = Blocks.AIR.defaultBlockState();
                    if (y == 64) block = ((x + z) % 4 == 0 ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS).defaultBlockState();
                    else if (y == 74) block = (Math.abs(x) <= 4 ? Blocks.GLASS : Blocks.OAK_PLANKS).defaultBlockState();
                    else if (x == -10 || x == 10 || z == 0 || z == 16) {
                        block = (x % 4 == 0 ? Blocks.OAK_LOG : x < 0 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS).defaultBlockState();
                        if (z == 16 && x >= -2 && x <= 2 && y >= 65 && y <= 70) block = Blocks.BOOKSHELF.defaultBlockState();
                        if (z == 16 && x >= 5 && x <= 8 && y >= 66 && y <= 69) block = Blocks.COPPER_BLOCK.defaultBlockState();
                        if ((x == -10 || x == 10) && z >= 7 && z <= 12 && y >= 68 && y <= 71) block = Blocks.GLASS.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x, y, z), block, 3);
                }
            }
        }
        for (int x : new int[]{-8, 8}) {
            level.setBlock(new BlockPos(x, 70, 12), Blocks.GLOWSTONE.defaultBlockState(), 3);
            for (int y = 65; y <= 68; y++) level.setBlock(new BlockPos(x, y, 14), Blocks.OAK_LEAVES.defaultBlockState(), 3);
        }
        player.teleportTo(level, 0.5D, 65.0D, 2.5D, 0.0F, 5.0F);
        validateNativeStackClock(player);
        SubstanceEngine.reset(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AL_LAD_500MG.get()));
    }

    private static void validateNativeStackClock(ServerPlayer player) {
        SubstanceEngine.reset(player);
        Stage portion = new Stage("native-stack-clock-check", 500, Mode.NORMAL);
        for (int serving = 0; serving < 10; serving++) consumeNative(player, portion);
        AlLadEffectState stacked = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState();
        if (stacked.targetContentMilligrams() != 5000.0D || stacked.targetDoseMicrograms() != 4500.0D
                || stacked.repeatedDoses() != 9 || stacked.durationTicks() > AlLadDoseCurve.maximumDurationTicks()) {
            throw new IllegalStateException("Ten native 500mg doses changed the content ledger or stacked clock cap");
        }
        if (Math.abs(TrippyConfig.experienceTimeScale - 1.0D) < 0.0001D && stacked.durationTicks() != 40896) {
            throw new IllegalStateException("Default ten-dose native stack changed the existing AL-LAD clock");
        }
        log("PASS: ten consumed 500mg native items retain 5000mg content/4500 calibrated effect units; existing stacked clock "
                + stacked.durationTicks() / 20 + " seconds");
        SubstanceEngine.reset(player);
    }

    private static void selectStage(Minecraft mc, UUID playerId, int next) {
        stageIndex = next;
        stageReady = false;
        settledNanos = 0L;
        queuedNanos = System.nanoTime();
        if (next >= STAGES.length) return;
        Stage selected = STAGES[next];
        TrippyClientConfig.motionIntensity = selected.mode() == Mode.MOTION_OFF ? 0.0D : originalMotion;
        TrippyClientConfig.visualIntensity = selected.mode() == Mode.VISUAL_OFF ? 0.0D : originalVisual;
        mc.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Integrated player disappeared during stage selection");
                SubstanceEngine.reset(player);
                if (selected.content() > 0) {
                    consumeNative(player, selected);
                    PsychoactiveExperience experience = SubstanceEngine.getExperience(playerId).orElseThrow();
                    AlLadEffectState raw = experience.alLadState();
                    double expectedEffect = expectedEffect(selected.content());
                    if (Math.abs(raw.targetContentMilligrams() - selected.content()) > 0.001D
                            || Math.abs(raw.targetDoseMicrograms() - expectedEffect) > 0.001D) {
                        throw new IllegalStateException("Native consumption changed physical content or calibrated effect units");
                    }
                    if (raw.durationTicks() > AlLadDoseCurve.maximumDurationTicks()) throw new IllegalStateException("Native trip exceeded the unchanged AL-LAD clock cap");
                    if (raw.durationTicks() != AlLadDoseCurve.baseDurationTicks(expectedEffect)) {
                        throw new IllegalStateException("Native single-consumption duration changed from the existing dose curve");
                    }
                    if (selected.mode() != Mode.NATURAL_ONSET) {
                        CompoundTag saved = experience.save();
                        CompoundTag session = saved.getCompound("al_lad");
                        session.put("signature", AlLadTripSignature.create(SIGNATURE).save());
                        session.putInt("ageTicks", selected.mode() == Mode.EXPIRED ? raw.durationTicks() : (int) (raw.durationTicks() * 0.42D));
                        session.putDouble("visualEnvelope", selected.mode() == Mode.EXPIRED ? 0.0D : AlLadDoseCurve.phaseEnvelope(AlLadPhase.PEAK, 0.42D));
                        if (selected.mode() == Mode.MIXED) {
                            for (String key : List.of("psilocybin", "lsd")) {
                                CompoundTag other = saved.getCompound(key);
                                other.putInt("ageTicks", (int) (other.getInt("durationTicks") * 0.50D));
                                other.putDouble("visualEnvelope", 1.0D);
                            }
                        } else if (selected.mode() == Mode.MIXED_TWO_CE) {
                            CompoundTag other = saved.getCompound("two_ce");
                            other.putInt("ageTicks", (int) (other.getInt("durationTicks") * 0.60D));
                            other.putDouble("visualEnvelope", 1.0D);
                        }
                        // QA normalizes only persisted phase and signature. Native ledgers and durations remain untouched.
                        experience = PsychoactiveExperience.load(saved);
                        SubstanceEngine.setExperience(playerId, experience);
                    }
                    log(String.format(Locale.ROOT, "%s native content=%dmg calibratedEffect=%.3f duration=%ds; %s",
                            selected.name(), selected.content(), expectedEffect, raw.durationTicks() / 20,
                            selected.mode() == Mode.NATURAL_ONSET ? "natural untouched onset" : "QA phase/signature normalized"));
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AL_LAD_500MG.get()));
                SubstanceEngine.sync(player);
                stageReady = true;
            } catch (Exception exception) { fail("Native stage transition failed: " + exception); }
        });
    }

    private static void consumeNative(ServerPlayer player, Stage selected) {
        List<CapsuleBlend.Entry> entries = new ArrayList<>();
        Item pill = selected.content() == 25 ? ModItems.AL_LAD_25MG.get()
                : selected.content() == 100 ? ModItems.AL_LAD_100MG.get()
                : selected.content() == 250 ? ModItems.AL_LAD_250MG.get() : ModItems.AL_LAD_500MG.get();
        int servings = selected.content() <= 500 ? 1 : selected.content() / 500;
        for (int portion = 0; portion < servings; portion++) entries.add(CapsuleBlend.entryFromDoseItem(new ItemStack(pill)).orElseThrow());
        if (selected.mode() == Mode.MIXED) {
            entries.add(CapsuleBlend.entryFromDoseItem(new ItemStack(ModItems.PSILOCYBIN_CAPSULE_3G.get())).orElseThrow());
            entries.add(CapsuleBlend.entryFromDoseItem(new ItemStack(ModItems.LSD_BLOTTER_250UG.get())).orElseThrow());
        } else if (selected.mode() == Mode.MIXED_TWO_CE) {
            entries.add(CapsuleBlend.entryFromDoseItem(new ItemStack(ModItems.TWO_CE_500MG.get())).orElseThrow());
        }
        ItemStack dose = servings == 1 && selected.mode() != Mode.MIXED && selected.mode() != Mode.MIXED_TWO_CE
                ? new ItemStack(pill) : CapsuleBlend.createStack(entries);
        boolean creative = player.getAbilities().instabuild;
        try {
            player.getAbilities().instabuild = false;
            player.setItemInHand(InteractionHand.MAIN_HAND, dose);
            dose.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            if (!player.getMainHandItem().isEmpty()) throw new IllegalStateException("Native dose item was not consumed in survival");
        } finally { player.getAbilities().instabuild = creative; }
    }

    private static boolean synchronizedStage(Stage selected) {
        AlLadEffectState state = ClientExperienceStore.alLad();
        if (selected.content() == 0 || selected.mode() == Mode.EXPIRED) return !state.active();
        if (!state.active() || Math.abs(state.targetContentMilligrams() - selected.content()) > 0.001D) return false;
        if (selected.mode() == Mode.NATURAL_ONSET) return state.phase() == AlLadPhase.ONSET && state.progress() < 0.10D;
        if (state.progress() < 0.41D) return false;
        if (selected.mode() == Mode.MIXED) return ClientExperienceStore.psilocybin().active() && ClientExperienceStore.lsd().active();
        return selected.mode() != Mode.MIXED_TWO_CE || ClientExperienceStore.twoCe().active();
    }

    private static void capture(Minecraft mc, Stage selected) {
        // A fixture change is a failure, even if another application interrupted rendering.
        if (!mc.level.getBlockState(new BlockPos(0, 68, 16)).is(Blocks.BOOKSHELF)
                || !mc.level.getBlockState(new BlockPos(6, 68, 16)).is(Blocks.COPPER_BLOCK)) {
            throw new IllegalStateException("Authored bookshelf/copper fixture changed or failed to synchronize");
        }
        AlLadEffectState state = ClientExperienceStore.alLad();
        if (selected.content() > 0 && selected.mode() != Mode.EXPIRED) {
            String label = "AL-LAD " + selected.content() + "mg";
            if (!TripStatusBarOverlay.alLadDose(state, false).equals(label)
                    || Math.abs(state.displayContentMilligrams() - selected.content()) > 0.001D
                    || !Double.isFinite(state.intensity()) || state.intensity() < 0.0D
                    || selected.mode() != Mode.NATURAL_ONSET && state.intensity() <= 0.0D) {
                throw new IllegalStateException("Synchronized native HUD/state lost the physical dose at " + selected.name());
            }
        }
        if (selected.mode() == Mode.MOTION_OFF && TrippyClientConfig.motionIntensity != 0.0D) throw new IllegalStateException("Motion-off control changed during capture");
        if (selected.mode() == Mode.VISUAL_OFF && TrippyClientConfig.visualIntensity != 0.0D) throw new IllegalStateException("Visual-off control changed during capture");
        if (selected.mode() == Mode.NATURAL_ONSET && (state.phase() != AlLadPhase.ONSET || state.progress() >= 0.10D)) {
            throw new IllegalStateException("Natural onset stage was overwritten or advanced unexpectedly");
        }
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) throw new IllegalStateException("OpenGL error at " + selected.name() + ": " + error);
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int samples = 0;
            double mean = 0.0D, square = 0.0D;
            for (int y = image.getHeight() / 5; y < image.getHeight() * 3 / 5; y += 4) {
                for (int x = image.getWidth() / 5; x < image.getWidth() * 4 / 5; x += 4) {
                    int rgb = image.getPixelRGBA(x, y);
                    double luma = ((rgb & 255) + ((rgb >>> 8) & 255) + ((rgb >>> 16) & 255)) / 3.0D;
                    mean += luma;
                    square += luma * luma;
                    samples++;
                }
            }
            mean /= Math.max(1, samples);
            double variance = square / Math.max(1, samples) - mean * mean;
            if (samples <= 0 || mean <= 1.0D || variance <= 1.0D) throw new IllegalStateException("Native world capture was blank or flat at " + selected.name());
            log(String.format(Locale.ROOT, "%s: physicalContent=%.0fmg effective=%.3f phase=%s intensity=%.5f duration=%ds motion=%.2f visual=%.2f worldLuma=%.3f variance=%.3f GUI=%s",
                    selected.name(), state.displayContentMilligrams(), state.activeDoseMicrograms(), state.phase(), state.intensity(),
                    state.durationTicks() / 20, TrippyClientConfig.motionIntensity, TrippyClientConfig.visualIntensity, mean, variance, !mc.options.hideGui));
        }
        String filename = "allad-" + selected.name() + ".png";
        Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(filename))) {
                SAVED.incrementAndGet();
                log("Saved " + filename);
            } else fail("Native screenshot writer failed: " + message.getString());
        });
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        if (controlsSaved) {
            TrippyClientConfig.motionIntensity = originalMotion;
            TrippyClientConfig.visualIntensity = originalVisual;
        }
        log((failed ? "FAIL" : "PASS") + ": AL-LAD native client smoke; screenshots=" + SAVED.get());
        try {
            synchronized (REPORT) {
                Files.writeString(mc.gameDirectory.toPath().resolve("allad-smoke-result.txt"), String.join(System.lineSeparator(), REPORT));
            }
        } catch (Exception exception) { TrippyCraft.LOGGER.error("[AL-LAD smoke] Could not save result", exception); }
        mc.stop();
    }

    private static void fail(String message) {
        failed = true;
        TrippyCraft.LOGGER.error("[AL-LAD smoke] {}", message);
        synchronized (REPORT) { REPORT.add("FAIL: " + message); }
    }
    private static void log(String message) {
        TrippyCraft.LOGGER.info("[AL-LAD smoke] {}", message);
        synchronized (REPORT) { REPORT.add(message); }
    }
    private static double seconds(long nanos) { return (System.nanoTime() - nanos) / 1_000_000_000.0D; }
    private static double expectedEffect(int content) {
        return content <= 500 ? content == 25 ? 75.0D : content == 100 ? 150.0D : content == 250 ? 300.0D : 450.0D
                : content * 0.90D;
    }
    private enum Mode { NORMAL, MOTION_OFF, VISUAL_OFF, EXPIRED, MIXED, MIXED_TWO_CE, NATURAL_ONSET }
    private record Stage(String name, int content, Mode mode) { }
}
