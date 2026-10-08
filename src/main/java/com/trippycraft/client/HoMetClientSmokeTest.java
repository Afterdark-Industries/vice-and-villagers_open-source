package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.HoMetPowderItem;
import com.trippycraft.item.AlLadConcentrateItem;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.item.TwoCeCrystalItem;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.homet.HoMetTripSignature;
import com.trippycraft.substance.lsd.LsdPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/** Opt-in development renderer check. Creates only a fresh dedicated world and never opens an existing save. */
public final class HoMetClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.hometSmokeTest") && !FMLEnvironment.production;
    private static final boolean AL_LAD_CRAFTING = Boolean.getBoolean("trippycraft.alLadCraftingSmokeTest");
    private static final boolean TWO_CB_CRAFTING = Boolean.getBoolean("trippycraft.twoCbCraftingSmokeTest");
    private static final boolean TWO_CE_CRAFTING = Boolean.getBoolean("trippycraft.twoCeCraftingSmokeTest");
    private static final String WORLD = "homet-render-smoke";
    private static final AtomicInteger SAVED_SCREENSHOTS = new AtomicInteger();
    private static final List<String> REPORT = new ArrayList<>();
    private static boolean started;
    private static boolean setupQueued;
    private static volatile boolean sceneReady;
    private static volatile boolean failed;
    private static boolean finished;
    private static long startedNanos;
    private static long sceneNanos;
    private static int stage;
    private static int screenshotStage;
    private static int clientTicks;
    private static UUID subjectId;
    private static UUID redstoneId;
    private static UUID lapisId;
    private static boolean dwellLogged;
    private static HoMetEffectState fortySnapshot;
    private static HoMetEffectState fiftySnapshot;

    private HoMetClientSmokeTest() { }

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
                    fail("Dedicated smoke world already exists; refusing to open or replace it.");
                    finish(mc);
                    return;
                }
                mc.options.pauseOnLostFocus = false;
                mc.options.onboardAccessibility = false;
                mc.options.hideGui = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                mc.options.renderDistance().set(5);
                mc.options.simulationDistance().set(5);
                mc.options.fov().set(75);
                mc.options.gamma().set(0.7D);
                GameRules rules = new GameRules();
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
                log("Creating isolated flat world in " + mc.gameDirectory.getAbsolutePath());
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings,
                        new WorldOptions(0x484F4D4554L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                return;
            }
            if (secondsSince(startedNanos) > 210.0D) {
                fail("Timed out waiting for renderer smoke test completion.");
                finish(mc);
                return;
            }
            if (failed) {
                finish(mc);
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            var server = mc.getSingleplayerServer();
            UUID playerId = mc.player.getUUID();
            if (!setupQueued) {
                setupQueued = true;
                server.execute(() -> {
                    try {
                        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                        if (player == null) throw new IllegalStateException("Integrated player disappeared before scene creation");
                        buildScene(player);
                        sceneReady = true;
                    } catch (Exception exception) {
                        fail("Scene creation failed: " + exception);
                    }
                });
                return;
            }
            if (!sceneReady || mc.player.distanceToSqr(0.5D, 65.0D, 2.5D) > 16.0D) return;
            if (TWO_CB_CRAFTING && sceneNanos == 0L && !TwoCbChemistrySmokeCheck.tick(mc, HoMetClientSmokeTest::log)) return;
            if (TWO_CE_CRAFTING && sceneNanos == 0L && !TwoCeHudSmokeCheck.tick(mc, HoMetClientSmokeTest::log)) return;
            if (sceneNanos == 0L) {
                sceneNanos = System.nanoTime();
                mc.setScreen(null);
                validateItemAssets(mc);
                log("Scene ready: oak/cobblestone panels, moving villager, redstone/lapis ribbon sources, colored particles, visible hand and HUD.");
            }
            double seconds = secondsSince(sceneNanos);
            clientTicks++;
            boolean observingStill = (seconds >= 10.0D && seconds < 15.0D) || (seconds >= 20.0D && seconds < 25.0D);
            double cameraSeconds = observingStill ? 20.0D : seconds;
            mc.player.setYRot((float) (Math.sin(cameraSeconds * 0.55D) * 7.0D));
            mc.player.setXRot(3.0F);
            if (!observingStill && clientTicks % 24 == 0) mc.player.swing(InteractionHand.MAIN_HAND);
            if (observingStill && !dwellLogged) {
                dwellLogged = true;
                log("Holding the same camera/hand view from 10s to 15s and 20s to 25s for comparable 40mg/50mg surface observation.");
            }
            // The paired captures at 13s and 23s use the same subject positions and motion cycle.
            server.execute(() -> animateScene(server.getPlayerList().getPlayer(playerId), seconds % 10.0D));

            if (stage == 0 && seconds >= 6.0D) selectStage(mc, playerId, 1);
            if (stage == 1 && seconds >= 16.0D) selectStage(mc, playerId, 2);
            if (stage == 2 && seconds >= 25.0D) selectStage(mc, playerId, 3);
            if (stage == 3 && seconds >= 34.0D) selectStage(mc, playerId, 4);
            double[] screenshotTimes = {5.0D, 13.0D, 23.0D, 32.0D, 42.0D};
            String[] screenshotNames = {"00-sober", "01-40mg", "02-50mg", "03-high-320mg", "04-mixed-100mg"};
            if (screenshotStage < screenshotTimes.length && seconds >= screenshotTimes[screenshotStage]) {
                capture(mc, screenshotNames[screenshotStage++]);
            }
            if (seconds >= 46.0D && SAVED_SCREENSHOTS.get() >= screenshotTimes.length) finish(mc);
            else if (seconds > 58.0D) {
                fail("Screenshot writer did not complete all five scene captures.");
                finish(mc);
            }
        } catch (Exception exception) {
            fail("Smoke harness exception: " + exception);
            TrippyCraft.LOGGER.error("[HoMet smoke] Client harness failed", exception);
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
        for (int x = -8; x <= 8; x++) {
            for (int z = 0; z <= 16; z++) {
                for (int y = 64; y <= 71; y++) {
                    BlockState block = Blocks.AIR.defaultBlockState();
                    if (y == 64) block = ((x + z) % 4 == 0 ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS).defaultBlockState();
                    else if (y == 71) block = (Math.abs(x) <= 3 && z >= 3 && z <= 13 ? Blocks.GLASS : Blocks.OAK_PLANKS).defaultBlockState();
                    else if (x == -8 || x == 8 || z == 0 || z == 16) {
                        boolean timber = ((z == 0 || z == 16) ? x % 4 == 0 : z % 4 == 0) || y == 70;
                        block = (timber ? Blocks.OAK_LOG : y < 68 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS).defaultBlockState();
                        if ((x == -8 || x == 8) && y >= 67 && y <= 69 && z % 4 != 0) block = Blocks.GLASS.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x, y, z), block, 3);
                }
            }
        }
        for (int x : new int[]{-6, 6}) {
            for (int z : new int[]{3, 8, 13}) level.setBlock(new BlockPos(x, 70, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, 65, 14), Blocks.BOOKSHELF.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, 66, 14), Blocks.BOOKSHELF.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, 65, 11), Blocks.OAK_LEAVES.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, 66, 11), Blocks.OAK_LEAVES.defaultBlockState(), 3);
        }
        Villager subject = EntityType.VILLAGER.create(level);
        if (subject == null) throw new IllegalStateException("Villager entity failed to create");
        subject.setNoAi(true);
        subject.setInvulnerable(true);
        subject.setPersistenceRequired();
        subject.moveTo(0.5D, 65.0D, 10.5D, 180.0F, 0.0F);
        level.addFreshEntity(subject);
        subjectId = subject.getUUID();
        redstoneId = spawnMovingItem(level, Items.REDSTONE, -4.2D, 67.0D, 9.0D);
        lapisId = spawnMovingItem(level, Items.LAPIS_LAZULI, 0.0D, 68.2D, 12.5D);
        player.teleportTo(level, 0.5D, 65.0D, 2.5D, 0.0F, 3.0F);
        validateProductionItemUse(player);
        if (AL_LAD_CRAFTING) validateAlLadItemUse(player);
        if (TWO_CB_CRAFTING) validateTwoCbItemUse(player);
        if (TWO_CE_CRAFTING) validateTwoCeItemUse(player);
        List<Item> hotbarItems = TWO_CE_CRAFTING
                ? List.of(ModItems.TWO_CE_PRECURSOR.get(), ModItems.TWO_CE_FREEBASE.get(),
                ModItems.TWO_CE_CRYSTAL.get(), ModItems.TWO_CE_POWDER.get(), ModItems.TWO_CE_25MG.get(),
                ModItems.TWO_CE_100MG.get(), ModItems.TWO_CE_250MG.get(), ModItems.TWO_CE_500MG.get())
                : TWO_CB_CRAFTING
                ? List.of(ModItems.PHENETHYLAMINE_BASE.get(), ModItems.DIMETHOXY_INTERMEDIATE.get(),
                ModItems.TWO_CB_FREEBASE.get(), ModItems.TWO_CB_CRYSTAL.get(), ModItems.TWO_CB_POWDER.get(),
                ModItems.TWO_CB_25MG.get(), ModItems.TWO_CB_250MG.get(), ModItems.TWO_CB_500MG.get())
                : AL_LAD_CRAFTING
                ? List.of(ModItems.AMINOETHYL_REAGENT.get(), ModItems.LYSERGIC_EXTRACT.get(),
                ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get(), ModItems.AL_LAD_CONCENTRATE.get(),
                ModItems.AL_LAD_25MG.get(), ModItems.AL_LAD_100MG.get(), ModItems.AL_LAD_250MG.get(), ModItems.AL_LAD_500MG.get())
                : List.of(ModItems.AMINOETHYL_REAGENT.get(), ModItems.MET_INTERMEDIATE.get(),
                ModItems.HO_MET_FREEBASE.get(), ModItems.HO_MET_CRYSTAL.get(), ModItems.HO_MET_POWDER.get(),
                ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(), ModItems.HO_MET_250MG.get());
        for (int slot = 0; slot < hotbarItems.size(); slot++) {
            player.getInventory().setItem(slot + 1, new ItemStack(hotbarItems.get(slot)));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TWO_CE_CRAFTING ? ModItems.TWO_CE_250MG.get()
                : TWO_CB_CRAFTING ? ModItems.TWO_CB_100MG.get()
                : AL_LAD_CRAFTING ? ModItems.AL_LAD_100MG.get() : ModItems.HO_MET_250MG.get()));
        SubstanceEngine.reset(player);
    }

    private static void animateScene(ServerPlayer player, double seconds) {
        if (player == null || subjectId == null) return;
        ServerLevel level = player.serverLevel();
        var subject = level.getEntity(subjectId);
        if (subject != null) {
            subject.setPos(Math.sin(seconds * 0.72D) * 4.6D + 0.5D, 65.0D, 10.5D);
            subject.setYRot(Math.cos(seconds * 0.72D) >= 0 ? -90.0F : 90.0F);
        }
        animateItem(level, redstoneId, seconds, 0.0D, 67.0D, 9.0D);
        animateItem(level, lapisId, seconds, 4.2D, 68.2D, 12.5D);
        double particleX = Math.sin(seconds * 1.35D) * 4.0D;
        level.sendParticles(ParticleTypes.END_ROD, particleX, 68.3D, 13.0D, 1, 0.01D, 0.01D, 0.01D, 0.02D);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, -particleX, 69.0D, 11.8D, 1, 0.02D, 0.03D, 0.02D, 0.01D);
    }

    private static UUID spawnMovingItem(ServerLevel level, Item item, double x, double y, double z) {
        ItemEntity source = new ItemEntity(level, x, y, z, new ItemStack(item), 0.0D, 0.0D, 0.0D);
        source.setNoGravity(true);
        source.noPhysics = true;
        source.setNeverPickUp();
        source.setUnlimitedLifetime();
        source.setInvulnerable(true);
        level.addFreshEntity(source);
        return source.getUUID();
    }

    private static void animateItem(ServerLevel level, UUID id, double seconds, double offset, double y, double z) {
        var item = level.getEntity(id);
        if (item == null) return;
        // Constant 4.8 blocks/second supplies ~0.48 blocks between two-tick mesh captures except at turns.
        double cycle = (seconds * 4.8D + offset) % 16.8D;
        double x = cycle <= 8.4D ? cycle - 4.2D : 12.6D - cycle;
        item.noPhysics = true;
        item.setDeltaMovement(0.0D, 0.0D, 0.0D);
        item.setPos(x, y, z);
        // Item entities otherwise use a coarse tracking interval; send each scripted motion sample.
        item.hasImpulse = true;
    }

    private static void selectStage(Minecraft mc, UUID playerId, int nextStage) {
        stage = nextStage;
        mc.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Integrated player missing during stage change");
                SubstanceEngine.reset(player);
                if (nextStage == 3) {
                    // A single native capsule releases 320mg without crossing the stochastic overdose threshold.
                    useCapsule(player, java.util.Collections.nCopies(8, ModItems.HO_MET_500MG.get()));
                } else if (nextStage == 2) {
                    useCapsule(player, List.of(ModItems.HO_MET_500MG.get(), ModItems.HO_MET_100MG.get()));
                } else if (nextStage == 4) {
                    useCapsule(player, List.of(ModItems.HO_MET_500MG.get(), ModItems.HO_MET_500MG.get(), ModItems.HO_MET_250MG.get()));
                } else {
                    useItem(player, ModItems.HO_MET_500MG.get());
                }
                if (nextStage == 4) {
                    useItem(player, ModItems.LSD_BLOTTER_100UG.get());
                    useItem(player, ModItems.CANNABIS_JOINT.get());
                }
                var experience = SubstanceEngine.getExperience(playerId).orElseThrow();
                double expectedDose = switch (nextStage) { case 1 -> 40.0D; case 2 -> 50.0D; case 3 -> 320.0D; default -> 100.0D; };
                if (Math.abs(experience.hoMetState().targetDoseMilligrams() - expectedDose) > 0.0001D) {
                    throw new IllegalStateException("Native bottle/capsule produced the wrong dose at stage " + nextStage);
                }
                if (nextStage == 1) {
                    int duration = experience.hoMetState().durationTicks();
                    if (Math.abs(TrippyConfig.experienceTimeScale - 1.0D) < 0.0001D && duration != 8400) {
                        throw new IllegalStateException("Default 40mg trip should last exactly seven minutes; got " + duration + " ticks");
                    }
                    log("40mg native duration=" + duration + " ticks; experienceTimeScale=" + TrippyConfig.experienceTimeScale);
                }
                // Keep dose consumption native, then normalize only the seeded visual signature for QA comparisons.
                var saved = experience.save();
                saved.getCompound("four_ho_met").put("signature", HoMetTripSignature.create(0x484F4D4554L).save());
                if (nextStage == 4) {
                    // The development harness advances the existing persisted session; no production control API is needed.
                    var lsd = saved.getCompound("lsd");
                    lsd.putInt("ageTicks", (int) (lsd.getInt("durationTicks") * 0.45D));
                }
                experience = PsychoactiveExperience.load(saved);
                SubstanceEngine.setExperience(playerId, experience);
                if (nextStage == 4 && experience.lsdState().phase() != LsdPhase.PEAK) {
                    throw new IllegalStateException("Mixed-scene LSD did not enter its native peak phase");
                }
                SubstanceEngine.scrubPsilocybin(player, (int) (experience.hoMetState().durationTicks() * 0.50D));
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HO_MET_500MG.get()));
                SubstanceEngine.sync(player);
                log("Stage " + nextStage + ": " + SubstanceEngine.describe(player));
            } catch (Exception exception) {
                fail("Native stage transition failed: " + exception);
            }
        });
    }

    private static void useItem(ServerPlayer player, Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        item.use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static void validateProductionItemUse(ServerPlayer player) {
        boolean creative = player.getAbilities().instabuild;
        try {
            player.getAbilities().instabuild = false;
            SubstanceEngine.reset(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HO_MET_CRYSTAL.get()));
            for (int use = 1; use <= 5; use++) {
                ModItems.HO_MET_CRYSTAL.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                int remaining = HoMetCrystalItem.remainingMilligrams(player.getMainHandItem());
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState();
                double effectDose = state.targetDoseMilligrams();
                if (remaining != 5000 - use * 1000 || effectDose != use * 80.0D
                        || state.targetContentMilligrams() != use * 1000.0D) {
                    throw new IllegalStateException("Crystal right-click lost content or changed effect strength at use " + use);
                }
            }
            if (!player.getMainHandItem().isEmpty()) throw new IllegalStateException("Spent crystal did not disappear");
            Item[] bottles = {ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(),
                    ModItems.HO_MET_250MG.get(), ModItems.HO_MET_500MG.get()};
            double[] effects = {5.0D, 10.0D, 20.0D, 40.0D};
            int[] contents = {25, 100, 250, 500};
            for (int index = 0; index < bottles.length; index++) {
                SubstanceEngine.reset(player);
                useItem(player, bottles[index]);
                if (!player.getMainHandItem().is(Items.GLASS_BOTTLE)
                        || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState().targetDoseMilligrams() != effects[index]) {
                    throw new IllegalStateException("Bottle format changed its effect tier or lost its empty bottle");
                }
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState();
                if (state.targetContentMilligrams() != contents[index]
                        || !TripStatusBarOverlay.hoMetDose(state, false).equals("4-HO-MET " + contents[index] + "mg")
                        || !TripStatusBarOverlay.hoMetDose(state, true).equals("4-HO-MET " + contents[index] + "mg")) {
                    throw new IllegalStateException("4-HO-MET progress bar did not show the actual bottle dosage");
                }
            }
            SubstanceEngine.reset(player);
            useCapsule(player, List.of(bottles[0], bottles[1]));
            var capsuleState = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState();
            if (capsuleState.targetDoseMilligrams() != 15.0D || capsuleState.targetContentMilligrams() != 125.0D
                    || !TripStatusBarOverlay.hoMetDose(capsuleState, false).equals("4-HO-MET 125mg")) {
                throw new IllegalStateException("4-HO-MET capsule progress bar confused effect strength with content");
            }
            SubstanceEngine.reset(player);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.HO_MET_POWDER.get(), 2));
            ModItems.HO_MET_POWDER.get().use(player.level(), player, InteractionHand.OFF_HAND);
            var powderState = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState();
            if (!player.getOffhandItem().is(ModItems.HO_MET_POWDER.get()) || player.getOffhandItem().getCount() != 1
                    || powderState.targetDoseMilligrams() != HoMetPowderItem.EFFECT_DOSE
                    || powderState.targetContentMilligrams() != 100.0D
                    || !TripStatusBarOverlay.hoMetDose(powderState, false).equals("4-HO-MET 100mg")) {
                throw new IllegalStateException("4-HO-MET powder did not consume one 100mg unit in one click");
            }
            ModItems.HO_MET_POWDER.get().use(player.level(), player, InteractionHand.OFF_HAND);
            var secondPowderState = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState();
            if (!player.getOffhandItem().isEmpty() || secondPowderState.targetDoseMilligrams() != 20.0D
                    || secondPowderState.targetContentMilligrams() != 200.0D
                    || !new ItemStack(ModItems.HO_MET_POWDER.get()).getCraftingRemainingItem().isEmpty()) {
                throw new IllegalStateException("4-HO-MET powder lost its second serving or produced an empty bottle");
            }
            player.getAbilities().instabuild = true;
            SubstanceEngine.reset(player);
            useItem(player, ModItems.HO_MET_POWDER.get());
            if (!player.getMainHandItem().is(ModItems.HO_MET_POWDER.get()) || player.getMainHandItem().getCount() != 1
                    || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().hoMetState().targetContentMilligrams() != 100.0D) {
                throw new IllegalStateException("Creative 4-HO-MET powder use consumed the stack or applied the wrong amount");
            }
            log("PASS: five crystal right-clicks consume 1000mg each with two 500mg bottles' strength; all four bottle formats preserve effect tiers and return glass.");
            log("PASS: 4-HO-MET progress-bar labels show actual 25/100/250/500mg contents; crystal portions accumulate 1000mg each; capsule shows 125mg at unchanged 15 effect units.");
            log("PASS: each 4-HO-MET powder right-click consumes one 100mg unit, uses the 100mg bottle's calibration with doubled effects, stacks exact physical content, returns no glass, and retains the powder in creative.");
        } finally {
            player.getAbilities().instabuild = creative;
            SubstanceEngine.reset(player);
        }
    }

    private static void useCapsule(ServerPlayer player, List<Item> contents) {
        List<CapsuleBlend.Entry> entries = new ArrayList<>();
        for (Item item : contents) entries.add(CapsuleBlend.entryFromDoseItem(new ItemStack(item)).orElseThrow());
        player.setItemInHand(InteractionHand.MAIN_HAND, CapsuleBlend.createStack(entries));
        player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static void validateAlLadItemUse(ServerPlayer player) {
        boolean creative = player.getAbilities().instabuild;
        try {
            player.getAbilities().instabuild = false;
            SubstanceEngine.reset(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AL_LAD_CONCENTRATE.get()));
            for (int use = 1; use <= 5; use++) {
                ModItems.AL_LAD_CONCENTRATE.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                if (AlLadConcentrateItem.remainingMilligrams(player.getMainHandItem()) != 5000 - use * 1000
                        || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState().targetDoseMicrograms() != use * 900.0D
                        || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState().targetContentMilligrams() != use * 1000.0D) {
                    throw new IllegalStateException("AL-LAD concentrate right-click changed content or strength at use " + use);
                }
            }
            if (!player.getMainHandItem().is(Items.GLASS_BOTTLE)) {
                throw new IllegalStateException("Spent AL-LAD concentrate lost its empty bottle");
            }
            Item[] tabs = {ModItems.AL_LAD_25MG.get(), ModItems.AL_LAD_100MG.get(), ModItems.AL_LAD_250MG.get(), ModItems.AL_LAD_500MG.get()};
            double[] effects = {75.0D, 150.0D, 300.0D, 450.0D};
            int[] contents = {25, 100, 250, 500};
            for (int index = 0; index < tabs.length; index++) {
                SubstanceEngine.reset(player);
                useItem(player, tabs[index]);
                if (!player.getMainHandItem().isEmpty()
                        || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState().targetDoseMicrograms() != effects[index]) {
                    throw new IllegalStateException("AL-LAD tab format changed its established effect tier");
                }
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState();
                if (state.targetContentMilligrams() != contents[index]
                        || !TripStatusBarOverlay.alLadDose(state, false).equals("AL-LAD " + contents[index] + "mg")
                        || !TripStatusBarOverlay.alLadDose(state, true).equals("AL-LAD " + contents[index] + "mg")) {
                    throw new IllegalStateException("AL-LAD progress bar did not show the actual blotter dosage");
                }
            }
            SubstanceEngine.reset(player);
            useCapsule(player, List.of(tabs[0], tabs[1]));
            if (!player.getMainHandItem().isEmpty()
                    || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState().targetDoseMicrograms() != 225.0D
                    || !TripStatusBarOverlay.alLadDose(SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState(), false).equals("AL-LAD 125mg")) {
                throw new IllegalStateException("AL-LAD capsule changed its preserved effect sum or was not consumed");
            }
            SubstanceEngine.reset(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, CapsuleBlend.createStack(List.of(
                    new CapsuleBlend.Entry("al_lad_micrograms", 150.0D, ""),
                    CapsuleBlend.entryFromDoseItem(new ItemStack(tabs[0])).orElseThrow())));
            player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            if (!player.getMainHandItem().isEmpty()
                    || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().alLadState().targetDoseMicrograms() != 225.0D) {
                throw new IllegalStateException("Mixed legacy/current AL-LAD capsule changed its saved effects");
            }
            player.getAbilities().instabuild = true;
            SubstanceEngine.reset(player);
            useItem(player, ModItems.AL_LAD_CONCENTRATE.get());
            if (AlLadConcentrateItem.remainingMilligrams(player.getMainHandItem()) != 5000) {
                throw new IllegalStateException("Creative AL-LAD concentrate consumed content");
            }
            log("PASS: five AL-LAD concentrate right-clicks consume 1000mg each at 900 calibrated effect units and return glass; 25/100/250/500mg tabs preserve 75/150/300/450 effect tiers; new and mixed legacy/current capsules preserve 225 effect units and are consumed; creative retains content.");
            log("PASS: AL-LAD progress-bar labels show actual 25/100/250/500mg contents; concentrate portions accumulate 1000mg each; capsule shows 125mg at unchanged 225 effect units.");
        } finally {
            player.getAbilities().instabuild = creative;
            SubstanceEngine.reset(player);
        }
    }

    private static void validateTwoCbItemUse(ServerPlayer player) {
        boolean creative = player.getAbilities().instabuild;
        try {
            player.getAbilities().instabuild = false;
            SubstanceEngine.reset(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TWO_CB_CRYSTAL.get()));
            for (int use = 1; use <= 5; use++) {
                // Keep this item-content check deterministic while retaining the production overdose rule.
                player.getRandom().setSeed(0L);
                ModItems.TWO_CB_CRYSTAL.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCbState();
                if (TwoCbCrystalItem.remainingMilligrams(player.getMainHandItem()) != 5000 - use * 1000
                        || state.targetDoseMilligrams() != use * 1000.0D) {
                    throw new IllegalStateException("2C-B crystal right-click lost content or applied the wrong dose at use " + use);
                }
            }
            if (!player.getMainHandItem().isEmpty()) throw new IllegalStateException("Spent 2C-B crystal did not disappear");
            Item[] pills = {ModItems.TWO_CB_25MG.get(), ModItems.TWO_CB_100MG.get(),
                    ModItems.TWO_CB_250MG.get(), ModItems.TWO_CB_500MG.get()};
            int[] amounts = {25, 100, 250, 500};
            for (int index = 0; index < pills.length; index++) {
                SubstanceEngine.reset(player);
                useItem(player, pills[index]);
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCbState();
                if (!player.getMainHandItem().isEmpty() || state.targetDoseMilligrams() != amounts[index]
                        || !TripStatusBarOverlay.twoCbDose(state, false).equals("2C-B " + amounts[index] + "mg")) {
                    throw new IllegalStateException("2C-B pill format changed its existing dose or progress-bar label");
                }
            }
            SubstanceEngine.reset(player);
            useCapsule(player, List.of(pills[0], pills[1]));
            if (!player.getMainHandItem().isEmpty()
                    || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCbState().targetDoseMilligrams() != 125.0D) {
                throw new IllegalStateException("2C-B capsule did not preserve the packaged pill amounts");
            }
            player.getAbilities().instabuild = true;
            SubstanceEngine.reset(player);
            player.getRandom().setSeed(0L);
            useItem(player, ModItems.TWO_CB_CRYSTAL.get());
            if (TwoCbCrystalItem.remainingMilligrams(player.getMainHandItem()) != 5000
                    || SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCbState().targetDoseMilligrams() != 1000.0D) {
                throw new IllegalStateException("Creative 2C-B crystal changed its serving or consumed contents");
            }
            log("PASS: five survival 2C-B crystal uses consume 1000mg each, accumulate 5000mg, and exhaust the crystal; overdose RNG seeded for deterministic content verification.");
            log("PASS: existing 2C-B 25/100/250/500mg pills retain their native doses and HUD labels; 125mg capsule is consumed; creative crystal retains 5000mg and applies one 1000mg serving.");
        } finally {
            player.getAbilities().instabuild = creative;
            SubstanceEngine.reset(player);
        }
    }

    private static void validateTwoCeItemUse(ServerPlayer player) {
        boolean creative = player.getAbilities().instabuild;
        try {
            player.getAbilities().instabuild = false;
            SubstanceEngine.reset(player);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TWO_CE_CRYSTAL.get()));
            for (int use = 1; use <= 5; use++) {
                ModItems.TWO_CE_CRYSTAL.get().use(player.level(), player, InteractionHand.OFF_HAND);
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCeState();
                if (TwoCeCrystalItem.remainingMilligrams(player.getOffhandItem()) != 5000 - use * 1000
                        || state.targetDoseMilligrams() != use * 160.0D
                        || state.targetContentMilligrams() != use * 1000.0D
                        || !TripStatusBarOverlay.twoCeDose(state, false).endsWith(use * 1000 + "mg")) {
                    throw new IllegalStateException("2C-E crystal right-click changed content, effect calibration, or trip-bar label at use " + use);
                }
            }
            if (!player.getOffhandItem().isEmpty()) throw new IllegalStateException("Spent 2C-E crystal did not disappear");
            Item[] pills = {ModItems.TWO_CE_25MG.get(), ModItems.TWO_CE_100MG.get(),
                    ModItems.TWO_CE_250MG.get(), ModItems.TWO_CE_500MG.get()};
            Item[] legacy = {ModItems.TWO_CE_10MG.get(), ModItems.TWO_CE_20MG.get(),
                    ModItems.TWO_CE_40MG.get(), ModItems.TWO_CE_80MG.get()};
            int[] contents = {25, 100, 250, 500};
            double[] effects = {10, 20, 40, 80};
            for (Item[] formats : List.of(pills, legacy)) {
                for (int tier = 0; tier < formats.length; tier++) {
                    SubstanceEngine.reset(player);
                    useItem(player, formats[tier]);
                    var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCeState();
                    if (!player.getMainHandItem().isEmpty() || state.targetDoseMilligrams() != effects[tier]
                            || state.targetContentMilligrams() != contents[tier]
                            || !TripStatusBarOverlay.twoCeDose(state, false).equals("2C-E " + contents[tier] + "mg")) {
                        throw new IllegalStateException("New or saved 2C-E pill changed its effect tier or physical dose label");
                    }
                }
            }
            SubstanceEngine.reset(player);
            useCapsule(player, List.of(pills[0], pills[1]));
            var capsule = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCeState();
            if (!player.getMainHandItem().isEmpty() || capsule.targetDoseMilligrams() != 30.0D
                    || capsule.targetContentMilligrams() != 125.0D
                    || !TripStatusBarOverlay.twoCeDose(capsule, false).equals("2C-E 125mg")) {
                throw new IllegalStateException("2C-E capsule lost physical content or preserved effect calibration");
            }
            player.getAbilities().instabuild = true;
            SubstanceEngine.reset(player);
            useItem(player, ModItems.TWO_CE_CRYSTAL.get());
            var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCeState();
            if (TwoCeCrystalItem.remainingMilligrams(player.getMainHandItem()) != 5000
                    || state.targetContentMilligrams() != 1000.0D || state.targetDoseMilligrams() != 160.0D) {
                throw new IllegalStateException("Creative 2C-E crystal consumed contents or changed its serving");
            }
            log("PASS: five survival 2C-E crystal clicks consume 1000mg each at 160 calibrated effect units, accumulate 5000mg, and exhaust the crystal; creative retains content.");
            log("PASS: new and saved 2C-E 25/100/250/500mg pills preserve 10/20/40/80 effect tiers and actual HUD labels; a consumed 125mg capsule retains 30 calibrated effect units.");
        } finally {
            player.getAbilities().instabuild = creative;
            SubstanceEngine.reset(player);
        }
    }

    private static void validateItemAssets(Minecraft mc) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/item/four_ho_met.png");
        if (mc.getResourceManager().getResource(texture).isEmpty()) throw new IllegalStateException("4-HO-MET texture resource is missing");
        var shaper = mc.getItemRenderer().getItemModelShaper();
        for (String id : List.of("aminoethyl_reagent", "met_intermediate", "four_ho_met_freebase", "four_ho_met_crystal", "four_ho_met_powder")) {
            if (mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/item/" + id + ".png")).isEmpty()) {
                throw new IllegalStateException("Production-chain texture is missing: " + id);
            }
        }
        for (Item item : List.of(ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(), ModItems.HO_MET_250MG.get(), ModItems.HO_MET_500MG.get(),
                ModItems.AMINOETHYL_REAGENT.get(), ModItems.MET_INTERMEDIATE.get(), ModItems.HO_MET_FREEBASE.get(),
                ModItems.HO_MET_CRYSTAL.get(), ModItems.HO_MET_POWDER.get())) {
            if (shaper.getItemModel(new ItemStack(item)) == shaper.getModelManager().getMissingModel()) {
                throw new IllegalStateException("4-HO-MET baked item model is missing");
            }
        }
        log("PASS: all nine production-chain item models resolve and their textures are present.");
        if (AL_LAD_CRAFTING) {
            for (String id : List.of("allyl_lysergic_intermediate", "al_lad_concentrate", "al_lad")) {
                if (mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/item/" + id + ".png")).isEmpty()) {
                    throw new IllegalStateException("AL-LAD texture is missing: " + id);
                }
            }
            for (Item item : List.of(ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get(), ModItems.AL_LAD_CONCENTRATE.get(),
                    ModItems.AL_LAD_25MG.get(), ModItems.AL_LAD_100MG.get(), ModItems.AL_LAD_250MG.get(), ModItems.AL_LAD_500MG.get())) {
                if (shaper.getItemModel(new ItemStack(item)) == shaper.getModelManager().getMissingModel()) {
                    throw new IllegalStateException("AL-LAD baked item model is missing: " + item);
                }
            }
            log("PASS: all six AL-LAD crafting item models resolve and all three supplied textures are present.");
        }
        if (TWO_CB_CRAFTING) {
            for (String id : List.of("phenethylamine_base", "dimethoxy_intermediate", "two_cb_freebase", "two_cb_crystal", "two_cb_powder")) {
                if (mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/item/" + id + ".png")).isEmpty()) {
                    throw new IllegalStateException("2C-B production texture resource is missing: " + id);
                }
            }
            for (Item item : List.of(ModItems.PHENETHYLAMINE_BASE.get(), ModItems.DIMETHOXY_INTERMEDIATE.get(),
                    ModItems.TWO_CB_FREEBASE.get(), ModItems.TWO_CB_CRYSTAL.get(), ModItems.TWO_CB_POWDER.get(),
                    ModItems.TWO_CB_25MG.get(), ModItems.TWO_CB_100MG.get(), ModItems.TWO_CB_250MG.get(), ModItems.TWO_CB_500MG.get())) {
                if (shaper.getItemModel(new ItemStack(item)) == shaper.getModelManager().getMissingModel()) {
                    throw new IllegalStateException("2C-B baked item model is missing: " + item);
                }
            }
            log("PASS: all nine 2C-B production/pill item models resolve and the five supplied textures are present.");
        }
        if (TWO_CE_CRAFTING) {
            for (String id : List.of("two_ce_precursor", "two_ce_freebase", "two_ce_crystal", "two_ce_powder")) {
                if (mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "textures/item/" + id + ".png")).isEmpty()) {
                    throw new IllegalStateException("2C-E production texture resource is missing: " + id);
                }
            }
            for (Item item : List.of(ModItems.TWO_CE_PRECURSOR.get(), ModItems.TWO_CE_FREEBASE.get(),
                    ModItems.TWO_CE_CRYSTAL.get(), ModItems.TWO_CE_POWDER.get(), ModItems.TWO_CE_25MG.get(),
                    ModItems.TWO_CE_100MG.get(), ModItems.TWO_CE_250MG.get(), ModItems.TWO_CE_500MG.get())) {
                if (shaper.getItemModel(new ItemStack(item)) == shaper.getModelManager().getMissingModel()) {
                    throw new IllegalStateException("2C-E baked item model is missing: " + item);
                }
            }
            log("PASS: all eight 2C-E production/pill item models resolve and the four supplied textures are present.");
        }
    }

    private static void capture(Minecraft mc, String name) {
        var state = ClientExperienceStore.hoMet();
        if (!name.contains("sober") && (!state.active() || !Double.isFinite(state.intensity()) || !Double.isFinite(state.complexity()))) {
            fail("Invalid synchronized effect state at " + name);
        }
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) fail("OpenGL error at " + name + ": " + error);
        log(String.format(Locale.ROOT, "%s: phase=%s intensity=%.3f depth=%.3f trails=%.3f complexity=%.3f GUI=%s",
                name, state.phase(), state.intensity(), state.spatialDepth(), state.trails(), state.complexity(), !mc.options.hideGui));
        log(name + " trail capture: " + HoMetTrailRenderer.smokeDiagnostics());
        if (name.equals("01-40mg")) fortySnapshot = state;
        if (name.equals("02-50mg")) {
            fiftySnapshot = state;
            if (fortySnapshot == null || state.intensity() <= fortySnapshot.intensity()
                    || state.crystallineColor() <= fortySnapshot.crystallineColor()) {
                fail("50mg should increase intensity and crystalline color above the matched-signature 40mg state");
            } else log("PASS: 50mg intensity/color exceed the corresponding 40mg state.");
        }
        if (name.equals("03-high-320mg")) {
            if (fiftySnapshot == null || state.intensity() <= fiftySnapshot.intensity()
                    || state.complexity() <= fiftySnapshot.complexity()) {
                fail("320mg did not keep increasing intensity and complexity above 50mg");
            } else log("PASS: 320mg continues increasing intensity and complexity beyond 50mg.");
        }
        if (name.equals("01-40mg") || name.equals("02-50mg")) {
            double dwell = HoMetWorldWarpPass.observationDwell();
            if (dwell < 0.10D) fail("Observation failed to build on the wall across the room: " + dwell);
            else log("PASS: across-room observation dwell=" + dwell);
        }
        if (!name.equals("00-sober")) {
            long coloredSources = HoMetTrailRenderer.smokeItemSourceColors().stream().filter(rgb -> {
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) >= 24;
            }).distinct().count();
            if (coloredSources < 2) fail("Expected two distinct nonwhite item ribbon source colors at " + name);
            else log("PASS: distinct colored item ribbon sources=" + coloredSources + " at " + name);
        }
        if (name.equals("04-mixed-100mg")) {
            var lsd = ClientExperienceStore.lsd();
            log("Mixed LSD synchronized phase=" + lsd.phase() + " intensity=" + lsd.intensity());
            if (!lsd.active() || lsd.phase() != LsdPhase.PEAK) fail("Mixed screenshot did not contain peak LSD");
        }
        Screenshot.grab(mc.gameDirectory, "homet-" + name + ".png", mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve("homet-" + name + ".png"))) {
                SAVED_SCREENSHOTS.incrementAndGet();
                log("Saved homet-" + name + ".png");
            } else fail("Screenshot failed: " + message.getString());
        });
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        log("Final trail capture: " + HoMetTrailRenderer.smokeDiagnostics());
        String result = (failed ? "FAIL" : "PASS") + ": HoMet client scene smoke; screenshots=" + SAVED_SCREENSHOTS.get();
        log(result);
        try {
            synchronized (REPORT) {
                Files.writeString(mc.gameDirectory.toPath().resolve("homet-smoke-result.txt"), String.join(System.lineSeparator(), REPORT));
            }
        } catch (Exception exception) {
            TrippyCraft.LOGGER.error("[HoMet smoke] Could not write result artifact", exception);
        }
        mc.stop();
    }

    private static void fail(String message) {
        failed = true;
        TrippyCraft.LOGGER.error("[HoMet smoke] {}", message);
        synchronized (REPORT) { REPORT.add("FAIL: " + message); }
    }

    private static void log(String message) {
        TrippyCraft.LOGGER.info("[HoMet smoke] {}", message);
        synchronized (REPORT) { REPORT.add(message); }
    }

    private static double secondsSince(long nanos) {
        return (System.nanoTime() - nanos) / 1_000_000_000.0D;
    }
}
