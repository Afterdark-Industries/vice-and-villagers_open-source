package com.trippycraft.substance;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholSession;
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
public final class PsilocybinNauseaGameTests {
    private PsilocybinNauseaGameTests() { }

    @GameTest(template = "village/bars/oak_taproom")
    public static void nativeMushroomVisualsNeverBecomeVanillaNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            for (double dose : new double[] {5.0D, 8.0D, 28.0D}) {
                ServerPlayer player = player(helper);
                PsychoactiveExperience experience = peakPsilocybin(dose);
                settle(player, experience);
                helper.assertTrue(visualLoad(experience) > 0.72D,
                        "Fixture did not reach the old visual-to-nausea trigger at " + dose + " g");
                helper.assertTrue(experience.psilocybinState().visualIntensity() > 0.0D
                                && experience.psilocybinState().organicBreathing() > 0.0D,
                        "Removing nausea erased native surface visuals at " + dose + " g");
                helper.assertTrue(experience.channel(ExperienceChannel.NAUSEA) == 0.0D
                                && experience.vanillaNauseaVisualLoad() == 0.0D,
                        "Psilocybin still contributes a nausea bridge signal at " + dose + " g");
                VanillaEffectBridge.applyTick(player, experience);
                helper.assertTrue(!player.hasEffect(MobEffects.CONFUSION),
                        "Pure psilocybin produced vanilla Nausea at " + dose + " g");
            }
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacyMushroomFallbackKeepsVisualsWithoutNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            TrippyConfig.enablePsilocybinEffects = false;
            // The generic fallback has a lower visual calibration. Raise only this fixture's
            // global gain so the old visual-to-nausea threshold is exercised, rather than
            // allowing an already weak fallback to make the regression pass trivially.
            TrippyConfig.globalIntensity = 8.0D;
            ServerPlayer player = player(helper);
            PsychoactiveExperience experience = new PsychoactiveExperience();
            experience.addPsilocybinDose(player, 8.0D);
            CompoundTag saved = experience.save();
            ListTag exposures = saved.getList("exposures", Tag.TAG_COMPOUND);
            helper.assertTrue(exposures.size() == 1 && !saved.contains("psilocybin"),
                    "Fixture did not enter the actual legacy fallback");
            exposures.getCompound(0).putInt("ageTicks", SubstanceProfiles.PSILOCYBIN.curve().peakTicks() / 2);
            experience = PsychoactiveExperience.load(saved);
            settle(player, experience);
            helper.assertTrue(visualLoad(experience) > 0.72D,
                    "Legacy regression fixture never reached the old nausea trigger");
            helper.assertTrue(experience.channel(ExperienceChannel.NAUSEA) == 0.0D
                            && experience.vanillaNauseaVisualLoad() == 0.0D,
                    "Legacy psilocybin retained a nausea contribution");
            VanillaEffectBridge.applyTick(player, experience);
            helper.assertTrue(!player.hasEffect(MobEffects.CONFUSION),
                    "Legacy psilocybin still produces vanilla Nausea");
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void alcoholNauseaSurvivesMushroomMixtures(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            for (boolean withPsilocybin : new boolean[] {false, true}) {
                ServerPlayer player = player(helper);
                PsychoactiveExperience experience = withPsilocybin
                        ? peakPsilocybin(8.0D) : new PsychoactiveExperience();
                AlcoholSession alcohol = AlcoholSession.start(16.0D);
                CompoundTag alcoholSave = alcohol.save();
                alcoholSave.putInt("ageTicks", alcoholSave.getInt("durationTicks") / 2);
                CompoundTag saved = experience.save();
                saved.put("alcohol", alcoholSave);
                experience = PsychoactiveExperience.load(saved);
                settle(player, experience);
                helper.assertTrue(experience.alcoholState().nausea() > 1.0D
                                && experience.channel(ExperienceChannel.NAUSEA) > 0.0D,
                        "Alcohol lost its independent nausea source when mushrooms were present: " + withPsilocybin);
                VanillaEffectBridge.applyTick(player, experience);
                helper.assertTrue(player.hasEffect(MobEffects.CONFUSION),
                        "Removing mushroom nausea suppressed another substance's nausea: " + withPsilocybin);
            }
        });
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mushroomTripDoesNotClearOrRefreshExternalNausea(GameTestHelper helper) {
        atBridgeTick(helper, () -> {
            ServerPlayer player = player(helper);
            PsychoactiveExperience experience = peakPsilocybin(8.0D);
            settle(player, experience);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 2, false, true, true));
            VanillaEffectBridge.applyTick(player, experience);
            MobEffectInstance external = player.getEffect(MobEffects.CONFUSION);
            helper.assertTrue(external != null && external.getDuration() == 600 && external.getAmplifier() == 2
                            && !external.isAmbient() && external.isVisible() && external.showIcon(),
                    "Mushroom nausea removal changed an independently applied vanilla debuff");
        });
    }

    private static PsychoactiveExperience peakPsilocybin(double dose) {
        PsilocybinSession session = PsilocybinSession.start(null, dose);
        session.setAgeTicks(session.durationTicks() / 2);
        CompoundTag saved = new CompoundTag();
        saved.put("psilocybin", session.save());
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
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "psilo-nausea-test"));
        BlockPos location = helper.absolutePos(new BlockPos(4, 2, 4));
        player.setPos(location.getX() + 0.5D, location.getY(), location.getZ() + 0.5D);
        return player;
    }

    private static void atBridgeTick(GameTestHelper helper, Runnable assertions) {
        // Observe a real scheduled bridge tick without changing the shared world's clock.
        helper.onEachTick(() -> {
            if (helper.getLevel().getGameTime() % 40L != 0L) return;
            boolean savedBridge = TrippyConfig.enableVanillaBridge;
            boolean savedPsilocybin = TrippyConfig.enablePsilocybinEffects;
            boolean savedLsd = TrippyConfig.enableLsdEffects;
            double savedGlobal = TrippyConfig.globalIntensity;
            double savedVisualCeiling = TrippyConfig.psilocybinMaxVisualIntensity;
            double savedVisualGain = TrippyConfig.psilocybinVisualGain;
            double savedLsdCeiling = TrippyConfig.lsdMaxVisualIntensity;
            double savedTime = TrippyConfig.experienceTimeScale;
            try {
                TrippyConfig.enableVanillaBridge = true;
                TrippyConfig.enablePsilocybinEffects = true;
                TrippyConfig.enableLsdEffects = true;
                TrippyConfig.globalIntensity = 1.0D;
                TrippyConfig.psilocybinMaxVisualIntensity = 6.0D;
                TrippyConfig.psilocybinVisualGain = 1.0D;
                TrippyConfig.lsdMaxVisualIntensity = 7.0D;
                TrippyConfig.experienceTimeScale = 1.0D;
                assertions.run();
                helper.succeed();
            } finally {
                TrippyConfig.enableVanillaBridge = savedBridge;
                TrippyConfig.enablePsilocybinEffects = savedPsilocybin;
                TrippyConfig.enableLsdEffects = savedLsd;
                TrippyConfig.globalIntensity = savedGlobal;
                TrippyConfig.psilocybinMaxVisualIntensity = savedVisualCeiling;
                TrippyConfig.psilocybinVisualGain = savedVisualGain;
                TrippyConfig.lsdMaxVisualIntensity = savedLsdCeiling;
                TrippyConfig.experienceTimeScale = savedTime;
            }
        });
    }
}
