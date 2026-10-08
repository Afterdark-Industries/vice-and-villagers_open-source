package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.HoMetDoseItem;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.homet.HoMetPhase;
import com.trippycraft.substance.homet.HoMetSession;
import com.trippycraft.substance.homet.HoMetTripSignature;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoMetStrengthGameTests {
    private static final int MAXIMUM_DURATION = 45 * 60 * 20;

    private HoMetStrengthGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allFourBottleFormatsDoubleThePriorEffectsWithoutChangingDoseOrTiming(GameTestHelper helper) {
        withConfig(1.0D, () -> {
            List<Item> bottles = List.of(ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(),
                    ModItems.HO_MET_250MG.get(), ModItems.HO_MET_500MG.get());
            int[] content = {25, 100, 250, 500};
            double[] calibrated = {5, 10, 20, 40};
            for (int tier = 0; tier < bottles.size(); tier++) {
                HoMetDoseItem bottle = (HoMetDoseItem) bottles.get(tier);
                helper.assertTrue(bottle.amountMilligrams() == content[tier] && bottle.doseMilligrams() == calibrated[tier],
                        "The strength change altered a bottle's physical or calibrated amount");
                for (double fraction : new double[]{0.15D, 0.37D, 0.55D, 0.945D}) {
                    assertFreshDoubled(helper, bottle.doseMilligrams(), bottle.amountMilligrams(), fraction, 1.0D);
                }
            }
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void crystalAndBlendedCapsulesUseTheSameGainAndPersistItOnce(GameTestHelper helper) {
        withConfig(1.0D, () -> {
            helper.assertTrue(HoMetCrystalItem.EFFECT_DOSE_PER_USE == 80.0D && HoMetCrystalItem.MILLIGRAMS_PER_USE == 1000,
                    "Doubling strength changed crystal content or its calibrated dose");
            assertFreshDoubled(helper, HoMetCrystalItem.EFFECT_DOSE_PER_USE, HoMetCrystalItem.MILLIGRAMS_PER_USE, 0.55D, 1.0D);
            assertFreshDoubled(helper, HoMetCrystalItem.EFFECT_DOSE_PER_USE, HoMetCrystalItem.MILLIGRAMS_PER_USE, 0.945D, 1.0D);

            for (int strongBottles : new int[]{2, 8}) {
                List<CapsuleBlend.Entry> ingredients = new ArrayList<>();
                for (int bottle = 0; bottle < strongBottles; bottle++) {
                    ingredients.add(CapsuleBlend.entryFromDoseItem(new ItemStack(ModItems.HO_MET_500MG.get())).orElseThrow());
                }
                ingredients.add(CapsuleBlend.entryFromDoseItem(new ItemStack(ModItems.HO_MET_100MG.get())).orElseThrow());
                List<CapsuleBlend.Entry> blend = CapsuleBlend.read(CapsuleBlend.createStack(ingredients));
                helper.assertTrue(blend.size() == 1, "The 4-HO-MET capsule did not merge its bottle entries");
                CapsuleBlend.Entry entry = blend.getFirst();
                helper.assertTrue(entry.amount() == strongBottles * 500 + 100
                                && entry.effectAmount() == strongBottles * 40 + 10,
                        "The strength change altered capsule physical or calibrated amounts");
                assertFreshDoubled(helper, entry.effectAmount(), entry.amount(), 0.55D, 1.0D);
                assertFreshDoubled(helper, entry.effectAmount(), entry.amount(), 0.945D, 1.0D);

                HoMetSession session = HoMetSession.start(null, entry.effectAmount(), entry.amount());
                session.setAgeTicks(priorDuration(entry.effectAmount()) / 2);
                HoMetEffectState before = session.effectState();
                HoMetEffectState restored = HoMetSession.load(session.save()).effectState();
                helper.assertTrue(restored.equals(before), "Saving and reloading applied the doubled gain again or lost session fields");
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    before.encode(buffer);
                    helper.assertTrue(HoMetEffectState.decode(buffer).equals(before),
                            "The network snapshot changed the doubled effects or physical amount");
                    helper.assertTrue(buffer.readableBytes() == 0, "The strength snapshot codec left unread fields");
                } finally {
                    buffer.release();
                }
            }
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void doublingStrengthPreservesRedoseToleranceInterpolationAndDuration(GameTestHelper helper) {
        withConfig(1.0D, () -> {
            int originalDuration = priorDuration(40.0D);
            int age = originalDuration / 2;
            double oldEnvelope = priorEnvelope(age / (double) originalDuration);
            HoMetSession session = HoMetSession.start(null, 40.0D, 500.0D);
            session.setAgeTicks(age);
            HoMetTripSignature signature = session.effectState().signature();
            session.stackDose(40.0D, 500.0D);
            int expectedDuration = Math.min(MAXIMUM_DURATION, originalDuration + (int) (priorDuration(40.0D) * 0.50D));
            HoMetEffectState immediate = session.effectState();
            helper.assertTrue(immediate.activeDoseMilligrams() == 40.0D && immediate.displayDoseMilligrams() == 40.0D
                            && immediate.targetDoseMilligrams() == 80.0D, "Strength gain changed redose calibration or introduced an immediate absorbed-dose jump");
            helper.assertTrue(immediate.displayContentMilligrams() == 500.0D && immediate.targetContentMilligrams() == 1000.0D,
                    "Strength gain changed physical redose content");
            helper.assertTrue(immediate.durationTicks() == expectedDuration && immediate.ageTicks() == age && immediate.repeatedDoses() == 1,
                    "Strength gain changed redose duration, age, or dose count");
            assertNear(helper, immediate.acuteTolerance(), 1.0D - 1.0D / 1.16D, "Redose tolerance changed");
            assertDoubledChannels(helper, immediate, priorChannels(40.0D, oldEnvelope, age,
                    age / (double) expectedDuration, signature, 1.0D));

            session.tick(null);
            HoMetEffectState next = session.effectState();
            double expectedAbsorbed = 40.0D * 0.9982D + (40.0D + 40.0D / 1.16D) * 0.0018D;
            double expectedProgress = (age + 1.0D) / expectedDuration;
            double expectedEnvelope = oldEnvelope + (priorEnvelope(expectedProgress) - oldEnvelope) * 0.018D;
            assertNear(helper, next.activeDoseMilligrams(), expectedAbsorbed, "Strength gain changed absorbed-dose interpolation");
            assertNear(helper, next.displayDoseMilligrams(), 40.48D, "Strength gain changed calibrated display interpolation");
            assertNear(helper, next.displayContentMilligrams(), 506.0D, "Strength gain changed physical display interpolation");
            helper.assertTrue(next.targetDoseMilligrams() == 80.0D && next.targetContentMilligrams() == 1000.0D
                            && next.durationTicks() == expectedDuration && next.ageTicks() == age + 1,
                    "Elapsed time altered the dose totals or redose timing");
            assertDoubledChannels(helper, next, priorChannels(expectedAbsorbed, expectedEnvelope, age + 1,
                    expectedProgress, signature, 1.0D));
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void extremeDosesKeepDoubledAmplitudesFiniteAndVisibilityGatesBounded(GameTestHelper helper) {
        for (double intensity : new double[]{0.5D, 1.0D, 100.0D}) {
            withConfig(intensity, () -> {
                for (double dose : new double[]{80.0D, 330.0D, 1000.0D, 1.0E100D, Double.MAX_VALUE}) {
                    assertFreshDoubled(helper, dose, 1000.0D, 0.55D, intensity);
                    assertFreshDoubled(helper, dose, 1000.0D, 0.945D, intensity);
                }
            });
        }
        helper.succeed();
    }

    private static void assertFreshDoubled(GameTestHelper helper, double calibrated, double content, double fraction, double intensity) {
        int duration = priorDuration(calibrated);
        int age = (int) (duration * fraction);
        HoMetSession session = HoMetSession.start(null, calibrated, content);
        session.setAgeTicks(age);
        HoMetEffectState state = session.effectState();
        double progress = age / (double) duration;
        helper.assertTrue(state.active() && state.ageTicks() == age && state.durationTicks() == duration
                        && state.remainingTicks() == duration - age && state.phase() == priorPhase(progress),
                "The doubled gain changed phase or duration for calibrated dose " + calibrated);
        helper.assertTrue(state.activeDoseMilligrams() == calibrated && state.displayDoseMilligrams() == calibrated
                        && state.targetDoseMilligrams() == calibrated && state.tier() == priorTier(calibrated),
                "The doubled gain changed calibrated dose or tier for " + calibrated);
        helper.assertTrue(state.displayContentMilligrams() == content && state.targetContentMilligrams() == content,
                "The doubled gain changed physical milligrams for calibrated dose " + calibrated);
        HoMetTripSignature signature = HoMetTripSignature.create(Double.doubleToLongBits(calibrated) ^ 0x484F4D455434L);
        helper.assertTrue(state.signature().equals(signature), "The doubled gain changed the original deterministic trip signature");
        double envelope = priorEnvelope(progress) * smooth(0.0D, 40.0D, duration - age);
        assertDoubledChannels(helper, state, priorChannels(calibrated, envelope, age, progress, signature, intensity));
    }

    // Freeze the previous formulas independently of the production dose curve and state-scaling implementation.
    private static PriorChannels priorChannels(double dose, double envelope, int age, double progress,
                                               HoMetTripSignature signature, double intensityScale) {
        double amplification = 1.0D + 1.4D * smooth(40.0D, 60.0D, dose);
        double logarithm = Math.log1p(dose / 12.0D);
        double peak = logarithm * 1.65D * amplification * intensityScale;
        double intensity = peak * envelope;
        double presence = (1.0D - Math.exp(-peak)) * envelope;
        double amplitude = presence * logarithm / Math.log1p(40.0D / 12.0D) * amplification * intensityScale;
        double complexity = logarithm / Math.log(2.0D) * 1.85D * amplification * intensityScale * envelope;
        double detail = 1.0D - Math.exp(-complexity * 0.45D);
        double breathing = 0.96D + Math.sin(age * 0.0045D * signature.breathingRhythm()
                + (signature.seed() & 1023L) * 0.007D) * 0.04D;
        return new PriorChannels(new double[]{intensity,
                amplitude * (0.42D + detail * 0.56D) * signature.depthBias(),
                amplitude * (0.18D + detail * 0.77D) * breathing,
                amplitude * (0.38D + detail * 0.61D) * signature.trailPersistence(),
                amplitude * detail * signature.recursionBias(),
                amplitude * (0.58D + detail * 0.42D) * signature.crystallineBias(),
                amplitude * (0.15D + detail * 0.70D) * signature.depthBias(), complexity},
                new double[]{presence, presence * (0.25D + detail * 0.42D), smooth(0.90D, 0.96D, progress) * presence});
    }

    private static void assertDoubledChannels(GameTestHelper helper, HoMetEffectState state, PriorChannels baseline) {
        double[] actual = {state.intensity(), state.spatialDepth(), state.blockDeformation(), state.trails(),
                state.surfaceRecursion(), state.crystallineColor(), state.perspectiveInstability(), state.complexity()};
        for (int channel = 0; channel < actual.length; channel++) {
            assertNear(helper, actual[channel], baseline.amplitudes()[channel] * 2.0D,
                    "Effect channel " + channel + " is not twice the previous baseline");
        }
        double[] gates = {state.visualPresence(), state.musicSensitivity(), state.afterglow()};
        for (int gate = 0; gate < gates.length; gate++) {
            double previous = Math.clamp(baseline.gates()[gate], 0.0D, 1.0D);
            assertNear(helper, gates[gate], 1.0D - (1.0D - previous) * (1.0D - previous),
                    "Visibility gate " + gate + " did not use the doubled bounded response");
            helper.assertTrue(gates[gate] >= 0.0D && gates[gate] <= 1.0D, "A doubled visibility gate escaped its stable range");
        }
    }

    private static int priorDuration(double dose) {
        double minutes = 3.0D + 4.0D * Math.log1p(dose / 5.0D) / Math.log(9.0D);
        return (int) Math.round(Math.clamp(minutes * 60.0D * 20.0D, 20.0D, MAXIMUM_DURATION));
    }

    private static int priorTier(double dose) {
        if (dose < 10.0D) return 1;
        if (dose < 20.0D) return 2;
        if (dose < 40.0D) return 3;
        return 4 + (int) Math.floor(Math.log(dose / 40.0D) / Math.log(2.0D));
    }

    private static HoMetPhase priorPhase(double progress) {
        if (progress < 0.18D) return HoMetPhase.COME_UP;
        if (progress < 0.32D) return HoMetPhase.OPENING;
        if (progress < 0.43D) return HoMetPhase.PEAK;
        if (progress < 0.72D) return HoMetPhase.PLATEAU;
        if (progress < 0.93D) return HoMetPhase.DESCENT;
        return HoMetPhase.AFTERGLOW;
    }

    private static double priorEnvelope(double progress) {
        return switch (priorPhase(progress)) {
            case COME_UP -> smooth(0.01D, 0.18D, progress) * 0.16D;
            case OPENING -> 0.16D + smooth(0.18D, 0.32D, progress) * 0.64D;
            case PEAK -> 0.80D + smooth(0.32D, 0.43D, progress) * 0.20D;
            case PLATEAU -> 1.0D - smooth(0.43D, 0.72D, progress) * 0.08D;
            case DESCENT -> 0.92D - smooth(0.72D, 0.93D, progress) * 0.82D;
            case AFTERGLOW -> 0.10D * (1.0D - smooth(0.93D, 1.0D, progress));
            case SOBER -> 0.0D;
        };
    }

    private static double smooth(double start, double end, double value) {
        double fraction = Math.clamp((value - start) / (end - start), 0.0D, 1.0D);
        return fraction * fraction * (3.0D - 2.0D * fraction);
    }

    private static void assertNear(GameTestHelper helper, double actual, double expected, String message) {
        double tolerance = Math.max(1.0E-10D, Math.abs(expected) * 1.0E-10D);
        helper.assertTrue(Double.isFinite(actual) && Double.isFinite(expected) && Math.abs(actual - expected) <= tolerance,
                message + ": " + actual + " instead of " + expected);
    }

    private static void withConfig(double intensity, Runnable assertions) {
        double savedIntensity = TrippyConfig.globalIntensity;
        double savedTime = TrippyConfig.experienceTimeScale;
        try {
            TrippyConfig.globalIntensity = intensity;
            TrippyConfig.experienceTimeScale = 1.0D;
            assertions.run();
        } finally {
            TrippyConfig.globalIntensity = savedIntensity;
            TrippyConfig.experienceTimeScale = savedTime;
        }
    }

    private record PriorChannels(double[] amplitudes, double[] gates) {
    }
}
