package com.trippycraft.substance.homet;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.network.ExperienceSnapshotPacket;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SetSettingSnapshot;
import com.trippycraft.substance.SubstanceFamily;
import com.trippycraft.substance.SubstanceProfiles;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.lsd.LsdSession;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoMetGameTests {
    private HoMetGameTests() { }

    @GameTest(template = "village/bars/oak_taproom")
    public static void progressionIsContinuousAndHasLongPlateau(GameTestHelper helper) {
        int duration = 12_000;
        double previous = 0.0D;
        int plateau = 0;
        for (int tick = 0; tick <= duration; tick++) {
            HoMetPhase phase = HoMetDoseCurve.phase(tick, duration);
            double value = HoMetDoseCurve.phaseEnvelope(phase, HoMetDoseCurve.progress(tick, duration));
            helper.assertTrue(Math.abs(value - previous) < 0.002D, "Phase boundary caused a discontinuity at tick " + tick);
            helper.assertTrue(value >= 0.0D && value <= 1.0D, "Phase envelope exceeded visual budget");
            if (phase == HoMetPhase.PLATEAU) plateau++;
            previous = value;
        }
        helper.assertTrue(plateau > duration / 4, "4-HO-MET needs a sustained visual plateau");
        helper.assertTrue(previous == 0.0D, "Afterglow did not finish at sober");
        HoMetSession session = HoMetSession.start(null, 40.0D);
        helper.assertTrue(session.effectState().visualPresence() == 0.0D, "New dose skipped its gradual onset");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void redosingPreservesVisualContinuityAndAmounts(GameTestHelper helper) {
        HoMetSession session = HoMetSession.start(null, 20.0D);
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.55D));
        HoMetEffectState before = session.effectState();
        session.stackDose(10.0D);
        HoMetEffectState after = session.effectState();
        helper.assertTrue(after.targetDoseMilligrams() == 30.0D, "Redose amount was not preserved");
        helper.assertTrue(after.remainingTicks() > before.remainingTicks(), "Redose did not extend the trip");
        helper.assertTrue(after.visualPresence() == before.visualPresence() && after.spatialDepth() == before.spatialDepth()
                && after.trails() == before.trails() && after.complexity() == before.complexity(), "Redose instantly changed the rendered state");
        helper.assertTrue(after.signature().equals(before.signature()), "Redose replaced the trip's spatial identity");
        for (int tick = 0; tick < 200; tick++) session.tick(null);
        helper.assertTrue(session.effectState().activeDoseMilligrams() > before.activeDoseMilligrams(), "Added amount never entered the active state");
        helper.assertTrue(session.effectState().activeDoseMilligrams() < after.targetDoseMilligrams(), "Redose lost its gradual absorption/tolerance");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void higherDosesIncreaseEveryAmplitudeWithoutACeiling(GameTestHelper helper) {
        double savedIntensity = TrippyConfig.globalIntensity;
        try {
            TrippyConfig.globalIntensity = 1.0D;
            double[] previous = new double[8];
            for (double dose : new double[] {5, 10, 20, 40, 50, 80, 1000, 1.0E100, Double.MAX_VALUE}) {
                HoMetEffectState state = comparablePeakState(dose).scaled(2.0D);
                double[] amplitudes = amplitudes(state);
                for (int index = 0; index < amplitudes.length; index++) {
                    helper.assertTrue(Double.isFinite(amplitudes[index]) && amplitudes[index] > previous[index],
                            "Amplitude " + index + " stopped increasing or overflowed at " + dose + " mg");
                }
                previous = amplitudes;
                for (double gate : new double[] {state.visualPresence(), state.musicSensitivity(), state.afterglow()}) {
                    helper.assertTrue(Double.isFinite(gate) && gate >= 0.0D && gate <= 1.0D, "Visibility/envelope gate escaped its stable range");
                }
                helper.assertTrue(Double.isFinite(state.activeDoseMilligrams()), "Dose interpolation overflowed");
            }
            double[] forty = amplitudes(comparablePeakState(40.0D));
            double[] fifty = amplitudes(comparablePeakState(50.0D));
            for (int index = 0; index < forty.length; index++) {
                helper.assertTrue(fifty[index] > forty[index] * 1.75D, "50 mg did not strongly increase amplitude " + index);
            }
            HoMetEffectState high = comparablePeakState(1_000.0D);
            helper.assertTrue(high.spatialDepth() > 1.0D && high.crystallineColor() > 1.0D, "Higher doses are still capped at one");
            helper.assertTrue(high.scaled(2.0D).spatialDepth() == high.spatialDepth() * 2.0D,
                    "Mixed-substance scaling silently clamped dose intensity");
            TrippyConfig.globalIntensity = 100.0D;
            for (double value : amplitudes(comparablePeakState(Double.MAX_VALUE).scaled(8.0D))) {
                helper.assertTrue(Double.isFinite(value), "Extreme dose/intensity/mix combination overflowed");
            }
        } finally {
            TrippyConfig.globalIntensity = savedIntensity;
        }
        helper.assertTrue(!HoMetSession.start(null, Double.NaN).isAlive(), "Invalid dose started a trip");
        helper.assertTrue(!HoMetSession.start(null, -20.0D).isAlive(), "Negative dose started a trip");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void fortyMilligramsLastSevenMinutesAtDefaultSpeed(GameTestHelper helper) {
        double savedTimeScale = TrippyConfig.experienceTimeScale;
        try {
            TrippyConfig.experienceTimeScale = 1.0D;
            helper.assertTrue(HoMetDoseCurve.baseDurationTicks(40.0D) == 8_400, "40 mg must last seven minutes by default");
            helper.assertTrue(HoMetDoseCurve.baseDurationTicks(50.0D) > 8_400
                    && HoMetDoseCurve.baseDurationTicks(50.0D) < 9_000, "50 mg reverted to the previous long trip timing");
            HoMetSession session = HoMetSession.start(null, 40.0D);
            session.setAgeTicks(8_399);
            helper.assertTrue(session.isAlive(), "40 mg ended early");
            session.tick(null);
            helper.assertTrue(!session.isAlive() && !session.effectState().active(), "40 mg persisted beyond seven minutes");
            TrippyConfig.experienceTimeScale = 2.0D;
            helper.assertTrue(HoMetDoseCurve.baseDurationTicks(40.0D) == 16_800, "Trip timing ignored the configured time scale");
        } finally {
            TrippyConfig.experienceTimeScale = savedTimeScale;
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void overdoseRollUsesEveryNewDosePastFourHundredConsumed(GameTestHelper helper) {
        HoMetSession session = HoMetSession.start(null, 375.0D);
        session.stackDose(25.0D);
        helper.assertTrue(session.targetDoseMilligrams() == 400.0D
                && HoMetDoseCurve.overdoseChanceForDose(25.0D, session.targetDoseMilligrams()) == 0.0D,
                "Exactly 400 mg must not trigger an overdose roll");
        session = HoMetSession.load(session.save());
        session.stackDose(5.0D);
        helper.assertTrue(session.effectState().activeDoseMilligrams() == 375.0D
                && session.targetDoseMilligrams() == 405.0D, "Consumed amount was confused with absorbed/tolerance-adjusted amount");
        helper.assertTrue(HoMetDoseCurve.overdoseChanceForDose(5.0D, session.targetDoseMilligrams()) == 0.30D,
                "Crossing 400 mg did not trigger the independent 30 percent roll");
        for (int tick = 0; tick < 200; tick++) session.tick(null);
        session = HoMetSession.load(session.save());
        helper.assertTrue(session.targetDoseMilligrams() == 405.0D, "Save/load or elapsed time lost the consumed total");
        session.stackDose(5.0D);
        helper.assertTrue(HoMetDoseCurve.overdoseChanceForDose(5.0D, session.targetDoseMilligrams()) == 0.30D,
                "A subsequent dose above 400 mg lost or increased its independent roll");
        helper.assertTrue(HoMetDoseCurve.overdoseChanceForDose(405.0D, 405.0D) == 0.30D,
                "An oversized first dose skipped its overdose roll");
        for (double invalid : new double[] {0.0D, -5.0D, Double.NaN, Double.NEGATIVE_INFINITY}) {
            helper.assertTrue(HoMetDoseCurve.overdoseChanceForDose(invalid, 500.0D) == 0.0D,
                    "Invalid/no new dose triggered an overdose roll");
            helper.assertTrue(HoMetDoseCurve.overdoseChanceForDose(5.0D, invalid) == 0.0D,
                    "Invalid consumed total triggered an overdose roll");
        }
        helper.assertTrue(HoMetDoseCurve.overdoseRollIsFatal(5.0D, 405.0D, 0.0D)
                && HoMetDoseCurve.overdoseRollIsFatal(5.0D, 405.0D, Math.nextDown(0.30D))
                && !HoMetDoseCurve.overdoseRollIsFatal(5.0D, 405.0D, 0.30D), "The 30 percent RNG boundary is incorrect");
        for (double invalidRoll : new double[] {-0.01D, 1.0D, Double.NaN, Double.POSITIVE_INFINITY}) {
            helper.assertTrue(!HoMetDoseCurve.overdoseRollIsFatal(5.0D, 405.0D, invalidRoll), "Invalid RNG value caused a death");
        }
        int fatal = 0;
        for (int roll = 0; roll < 1_000; roll++) if (HoMetDoseCurve.overdoseRollIsFatal(5.0D, 405.0D, roll / 1_000.0D)) fatal++;
        helper.assertTrue(fatal == 300, "Independent overdose probability is not 30 percent");
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "homet-damage"), ClientInformation.createDefault());
        var damage = HoMetDamageTypes.overdose(player);
        helper.assertTrue(damage.is(HoMetDamageTypes.OVERDOSE) && damage.is(DamageTypeTags.BYPASSES_ARMOR)
                && damage.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && damage.is(DamageTypeTags.BYPASSES_RESISTANCE)
                && damage.is(DamageTypeTags.BYPASSES_COOLDOWN), "Overdose damage is missing its registry or combat-immunity tags");
        helper.succeed();
    }

    private static HoMetEffectState comparablePeakState(double dose) {
        CompoundTag saved = HoMetSession.start(null, dose).save();
        saved.put("signature", HoMetTripSignature.create(0x484F4D455434L).save());
        saved.putInt("durationTicks", 12_000);
        HoMetSession session = HoMetSession.load(saved);
        session.setAgeTicks(6_000);
        session.tick(null);
        return session.effectState();
    }

    private static double[] amplitudes(HoMetEffectState state) {
        return new double[] {state.intensity(), state.complexity(), state.spatialDepth(), state.blockDeformation(),
                state.trails(), state.surfaceRecursion(), state.crystallineColor(), state.perspectiveInstability()};
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void sessionPersistenceAndNetworkKeepExactState(GameTestHelper helper) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addHoMetDose(null, 20.0D);
        helper.assertTrue(experience.hasClientVisualState() && experience.isAlive(), "Fresh onset would be dropped before its first visible tick");
        helper.assertTrue(experience.scrubPsilocybin(3_000), "Existing timeline cannot select 4-HO-MET");
        experience.addHoMetDose(null, 5.0D);
        PsychoactiveExperience restored = PsychoactiveExperience.load(experience.save());
        helper.assertTrue(restored.hoMetState().equals(experience.hoMetState()), "Session state changed through NBT save/load");
        ExperienceSnapshotPacket snapshot = ExperienceSnapshotPacket.from(restored);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ExperienceSnapshotPacket.encode(snapshot, buffer);
            ExperienceSnapshotPacket decoded = ExperienceSnapshotPacket.decode(buffer);
            helper.assertTrue(decoded.hoMet().equals(snapshot.hoMet()), "Snapshot lost dose, envelope, signature, or environment");
            helper.assertTrue(buffer.readableBytes() == 0, "Snapshot codec fields are out of alignment");
            helper.assertTrue(decoded.mescaline().equals(snapshot.mescaline()) && decoded.lsd().equals(snapshot.lsd()),
                    "4-HO-MET broke neighboring substance packet fields");
        } finally {
            buffer.release();
        }
        helper.assertTrue(restored.endActiveTripFromSleep() && !restored.hoMetState().active(), "Sleep did not clear the native session");
        helper.assertTrue(!ExperienceSnapshotPacket.clear().hoMet().active(), "Clear snapshot retained 4-HO-MET");
        helper.assertTrue(!PsychoactiveExperience.load(new CompoundTag()).hoMetState().active(), "Old save without 4-HO-MET started a session");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void nativeProfileContributesToSharedChannelsAndBlend(GameTestHelper helper) {
        // Exercise server-side environment/session logic without logging an unnegotiated
        // GameTest network connection into production login handlers.
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "homet-test"), ClientInformation.createDefault());
        BlockPos location = helper.absolutePos(new BlockPos(4, 2, 4));
        player.setPos(location.getX() + 0.5D, location.getY(), location.getZ() + 0.5D);
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addExposure(SubstanceProfiles.HO_MET, 1.0D, SetSettingSnapshot.capture(player));
        helper.assertTrue(experience.hoMetState().targetDoseMilligrams() == 20.0D, "Generic exposure bypassed the specialized session or used wrong units");
        experience.scrubPsilocybin(6_000);
        experience.tick(player, SetSettingSnapshot.capture(player));
        helper.assertTrue(experience.channel(ExperienceChannel.DEPTH_BREATHING) > 0.0D, "Native depth channel stayed empty");
        helper.assertTrue(experience.channel(ExperienceChannel.PATTERNING) > 0.0D, "Native pattern channel stayed empty");
        helper.assertTrue(experience.family(SubstanceFamily.PSYCHEDELIC) > 0.0D, "Family compositor did not receive 4-HO-MET");

        CompoundTag lsdSave = new CompoundTag();
        lsdSave.putDouble("activeDoseMicrograms", 200.0D); lsdSave.putInt("durationTicks", 12_000); lsdSave.putInt("ageTicks", 5_500);
        var lsd = LsdSession.load(lsdSave).effectState();
        PsychedelicBlendState onset = CompositeEffectResolver.resolve(PsilocybinEffectState.inactive(), lsd,
                DmtEffectState.inactive(), HoMetSession.start(null, 20.0D).effectState());
        helper.assertTrue(onset.lsdWeight() == 1.0D && onset.lsdShaderGain() == 1.0D,
                "Invisible 4-HO-MET onset changed an existing trip's contribution");
        HoMetEffectState state = experience.hoMetState();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(PsilocybinEffectState.inactive(), lsd, DmtEffectState.inactive(), state);
        helper.assertTrue(blend.blended() && blend.activeCount() == 2, "4-HO-MET was omitted from mixed state");
        helper.assertTrue(Math.abs(blend.lsdWeight() + blend.hoMetWeight() - 1.0D) < 1.0E-9D, "Mixed contributions are not normalized");
        helper.assertTrue(blend.hoMetShaderGain() > 0.0D && blend.hoMetShaderGain() < 1.0D
                && blend.lsdShaderGain() > 0.0D && blend.lsdShaderGain() < 1.0D, "One substance overwrote the other in the visual blend");
        helper.succeed();
    }
}
