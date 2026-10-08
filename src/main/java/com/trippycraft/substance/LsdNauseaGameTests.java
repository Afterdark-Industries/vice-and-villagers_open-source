package com.trippycraft.substance;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholSession;
import com.trippycraft.substance.lsd.LsdSession;
import com.trippycraft.substance.psilocybin.PsilocybinSession;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LsdNauseaGameTests {
    private LsdNauseaGameTests() { }

    @GameTest(template = "village/bars/oak_taproom")
    public static void nativeLsdTexturesDoNotCreateVanillaNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            for (double dose : new double[]{250.0D, 5_000.0D}) {
                ServerPlayer player = player(helper);
                PsychoactiveExperience experience = peakLsd(dose, false);
                settle(player, experience);
                helper.assertTrue(visualLoad(experience) > 0.72D,
                        "LSD fixture failed to reach the previous nausea trigger at " + dose + "ug");
                helper.assertTrue(experience.lsdState().visualIntensity() > 0.0D
                                && experience.lsdState().geometryMelt() > 0.0D,
                        "Removing vanilla nausea erased LSD's native textures");
                helper.assertTrue(experience.channel(ExperienceChannel.NAUSEA) == 0.0D
                                && experience.vanillaNauseaVisualLoad() == 0.0D,
                        "LSD visual channels retained a vanilla nausea source at " + dose + "ug");
                VanillaEffectBridge.applyTick(player, experience);
                helper.assertTrue(!player.hasEffect(MobEffects.CONFUSION), "Native LSD produced vanilla Nausea");
            }
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void lsdAndPsilocybinMixtureKeepsVisualsWithoutNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            ServerPlayer player = player(helper);
            PsychoactiveExperience experience = peakLsd(5_000.0D, true);
            settle(player, experience);
            helper.assertTrue(experience.lsdState().geometryMelt() > 0.0D
                            && experience.psilocybinState().surfaceMelting() > 0.0D,
                    "Psychedelic mixture lost its native surface effects");
            helper.assertTrue(visualLoad(experience) > 0.72D && experience.vanillaNauseaVisualLoad() == 0.0D,
                    "LSD/mushroom mixture retained a visual nausea bridge");
            VanillaEffectBridge.applyTick(player, experience);
            helper.assertTrue(!player.hasEffect(MobEffects.CONFUSION), "LSD/mushroom mixture produced vanilla Nausea");
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacyLsdFallbackRetainsVisualsWithoutVanillaNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            TrippyConfig.enableLsdEffects = false;
            // Exercise the old trigger with a strong, real generic fallback instead
            // of passing merely because its ordinary calibration was already faint.
            TrippyConfig.globalIntensity = 8.0D;
            ServerPlayer player = player(helper);
            PsychoactiveExperience experience = new PsychoactiveExperience();
            experience.addLsdDose(player, 250.0D);
            CompoundTag saved = experience.save();
            ListTag exposures = saved.getList("exposures", Tag.TAG_COMPOUND);
            helper.assertTrue(exposures.size() == 1 && !saved.contains("lsd"), "Fixture missed the actual LSD legacy fallback");
            exposures.getCompound(0).putInt("ageTicks", SubstanceProfiles.LSD.curve().peakTicks() / 2);
            experience = PsychoactiveExperience.load(saved);
            settle(player, experience);
            helper.assertTrue(visualLoad(experience) > 0.72D, "Legacy LSD fixture failed to reach the previous nausea trigger");
            helper.assertTrue(experience.channel(ExperienceChannel.NAUSEA) == 0.0D
                            && experience.vanillaNauseaVisualLoad() == 0.0D,
                    "Legacy LSD retained its visual nausea contribution");
            VanillaEffectBridge.applyTick(player, experience);
            helper.assertTrue(!player.hasEffect(MobEffects.CONFUSION), "Legacy LSD produced vanilla Nausea");
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void otherNauseaSourcesSurviveLsdMixtures(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            for (boolean withPsilocybin : new boolean[]{false, true}) {
                ServerPlayer player = player(helper);
                PsychoactiveExperience experience = peakLsd(5_000.0D, withPsilocybin);
                AlcoholSession alcohol = AlcoholSession.start(16.0D);
                CompoundTag alcoholSave = alcohol.save();
                alcoholSave.putInt("ageTicks", alcoholSave.getInt("durationTicks") / 2);
                CompoundTag saved = experience.save();
                saved.put("alcohol", alcoholSave);
                experience = PsychoactiveExperience.load(saved);
                settle(player, experience);
                helper.assertTrue(experience.channel(ExperienceChannel.NAUSEA) > 0.0D
                                && experience.alcoholState().nausea() > 1.0D,
                        "LSD exclusion erased alcohol's body nausea");
                VanillaEffectBridge.applyTick(player, experience);
                helper.assertTrue(player.hasEffect(MobEffects.CONFUSION),
                        "LSD exclusion suppressed independent alcohol Nausea");
            }
            ServerPlayer player = player(helper);
            PsychoactiveExperience experience = peakLsd(5_000.0D, false);
            settle(player, experience);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 2, false, true, true));
            VanillaEffectBridge.applyTick(player, experience);
            MobEffectInstance external = player.getEffect(MobEffects.CONFUSION);
            helper.assertTrue(external != null && external.getDuration() == 600 && external.getAmplifier() == 2
                            && !external.isAmbient() && external.isVisible() && external.showIcon(),
                    "LSD exclusion cleared or refreshed an externally applied Nausea effect");
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void olderLsdSaveDoesNotReplayItsStaleVisualNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            ServerPlayer player = player(helper);
            PsychoactiveExperience original = peakLsd(5_000.0D, true);
            settle(player, original);
            CompoundTag saved = original.save();
            saved.remove("vanillaNauseaExcludesLsd");
            saved.putDouble("vanillaNauseaVisualDrift", 0.99D);
            saved.putDouble("vanillaNauseaGeometryMelt", 0.99D);
            PsychoactiveExperience migrated = PsychoactiveExperience.load(saved);
            helper.assertTrue(migrated.vanillaNauseaVisualLoad() == 0.0D,
                    "Old pure LSD save replayed the previous vanilla nausea bridge");
            helper.assertTrue(migrated.channel(ExperienceChannel.GEOMETRY_MELT)
                            == original.channel(ExperienceChannel.GEOMETRY_MELT),
                    "LSD bridge migration erased a stored rendering channel");
            VanillaEffectBridge.applyTick(player, migrated);
            helper.assertTrue(!player.hasEffect(MobEffects.CONFUSION), "Migrating old LSD state produced Nausea");

            // A mixed save retains its independent bridge source until its actual
            // contributions can be recomputed on the following server tick.
            AlcoholSession alcohol = AlcoholSession.start(16.0D);
            saved.put("alcohol", alcohol.save());
            helper.assertTrue(PsychoactiveExperience.load(saved).vanillaNauseaVisualLoad() == 0.99D,
                    "Migrating mixed LSD save discarded another substance's stored nausea bridge");
        });
    }

    private static PsychoactiveExperience peakLsd(double dose, boolean withPsilocybin) {
        LsdSession session = LsdSession.start(null, dose);
        session.setAgeTicks(session.durationTicks() / 2);
        CompoundTag saved = new CompoundTag();
        saved.put("lsd", session.save());
        if (withPsilocybin) {
            PsilocybinSession psilocybin = PsilocybinSession.start(null, 8.0D);
            psilocybin.setAgeTicks(psilocybin.durationTicks() / 2);
            saved.put("psilocybin", psilocybin.save());
        }
        return PsychoactiveExperience.load(saved);
    }

    private static void settle(ServerPlayer player, PsychoactiveExperience experience) {
        SetSettingSnapshot setting = SetSettingSnapshot.capture(player);
        for (int tick = 0; tick < 120; tick++) experience.tick(player, setting);
    }

    private static double visualLoad(PsychoactiveExperience experience) {
        return Math.max(experience.channel(ExperienceChannel.VISUAL_DRIFT),
                experience.channel(ExperienceChannel.GEOMETRY_MELT));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "lsd-nausea-test"));
        BlockPos location = helper.absolutePos(new BlockPos(4, 2, 4));
        player.setPos(location.getX() + 0.5D, location.getY(), location.getZ() + 0.5D);
        return player;
    }

    private static void atBridgeTick(GameTestHelper helper, Runnable assertions) {
        helper.onEachTick(() -> {
            if (helper.getLevel().getGameTime() % 40L != 0L) return;
            boolean savedBridge = TrippyConfig.enableVanillaBridge;
            boolean savedLsd = TrippyConfig.enableLsdEffects;
            boolean savedPsilocybin = TrippyConfig.enablePsilocybinEffects;
            double savedGlobal = TrippyConfig.globalIntensity;
            double savedLsdCeiling = TrippyConfig.lsdMaxVisualIntensity;
            double savedLsdGain = TrippyConfig.lsdVisualGain;
            double savedPsilocybinCeiling = TrippyConfig.psilocybinMaxVisualIntensity;
            double savedPsilocybinGain = TrippyConfig.psilocybinVisualGain;
            double savedTime = TrippyConfig.experienceTimeScale;
            try {
                TrippyConfig.enableVanillaBridge = true;
                TrippyConfig.enableLsdEffects = true;
                TrippyConfig.enablePsilocybinEffects = true;
                TrippyConfig.globalIntensity = 1.0D;
                TrippyConfig.lsdMaxVisualIntensity = 7.0D;
                TrippyConfig.lsdVisualGain = 1.0D;
                TrippyConfig.psilocybinMaxVisualIntensity = 6.0D;
                TrippyConfig.psilocybinVisualGain = 1.0D;
                TrippyConfig.experienceTimeScale = 1.0D;
                assertions.run();
                helper.succeed();
            } finally {
                TrippyConfig.enableVanillaBridge = savedBridge;
                TrippyConfig.enableLsdEffects = savedLsd;
                TrippyConfig.enablePsilocybinEffects = savedPsilocybin;
                TrippyConfig.globalIntensity = savedGlobal;
                TrippyConfig.lsdMaxVisualIntensity = savedLsdCeiling;
                TrippyConfig.lsdVisualGain = savedLsdGain;
                TrippyConfig.psilocybinMaxVisualIntensity = savedPsilocybinCeiling;
                TrippyConfig.psilocybinVisualGain = savedPsilocybinGain;
                TrippyConfig.experienceTimeScale = savedTime;
            }
        });
    }
}
