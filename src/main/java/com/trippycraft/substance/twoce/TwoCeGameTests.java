package com.trippycraft.substance.twoce;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadSession;
import com.trippycraft.substance.homet.HoMetSession;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TwoCeGameTests {
    private TwoCeGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void durationAndPhasesFollowTheAuthoredClock(GameTestHelper helper) {
        double previousScale = TrippyConfig.experienceTimeScale;
        try {
            TrippyConfig.experienceTimeScale = 1.0D;
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(10.0D) == 4_800, "25mg content must last four minutes by default");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(20.0D) == 6_000, "100mg content must last five minutes by default");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(40.0D) == 6_900, "250mg content must last five minutes forty-five seconds");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(80.0D) == 7_800, "500mg content must last six minutes thirty seconds");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(160.0D) == 10_800, "1000mg content must last nine minutes");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(480.0D) == 17_400, "3000mg content must approach the fifteen-minute ceiling");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(800.0D) == 18_000, "5000mg content must reach fifteen minutes");
            int previousDuration = 0;
            for (int effectDose = 0; effectDose <= 800; effectDose++) {
                int duration = TwoCeDoseCurve.baseDurationTicks(effectDose);
                helper.assertTrue(duration >= previousDuration && duration <= 18_000,
                        "Duration must grow monotonically through the full content range");
                previousDuration = duration;
            }
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(400.0D) > TwoCeDoseCurve.baseDurationTicks(160.0D)
                            && TwoCeDoseCurve.baseDurationTicks(400.0D) < TwoCeDoseCurve.baseDurationTicks(480.0D),
                    "High-dose timing flattened before the last content tiers");
            TwoCeSession session = TwoCeSession.start(null, 40.0D);
            int duration = session.effectState().durationTicks();
            helper.assertTrue(session.phase() == TwoCePhase.ONSET, "A fresh trip must begin in onset");
            session.setAgeTicks((int) (duration * 0.35D));
            helper.assertTrue(session.phase() == TwoCePhase.PEAK, "Mid-clock 2C-E must be at peak");
            session.setAgeTicks((int) (duration * 0.60D));
            helper.assertTrue(session.phase() == TwoCePhase.MACHINE, "The machine plateau must occupy the later clock");
            session.setAgeTicks((int) (duration * 0.85D));
            helper.assertTrue(session.phase() == TwoCePhase.DESCENT, "The shortened clock must retain a descent");
            session.setAgeTicks((int) (duration * 0.96D));
            helper.assertTrue(session.phase() == TwoCePhase.AFTERGLOW, "The shortened clock must retain an afterglow");
            session.setAgeTicks(duration);
            helper.assertTrue(session.phase() == TwoCePhase.SOBER && !session.effectState().active(),
                    "Effects must end exactly at the authored clock");
            helper.succeed();
        } finally {
            TrippyConfig.experienceTimeScale = previousScale;
        }
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void configuredTimeScalingKeepsTheHardSessionCeiling(GameTestHelper helper) {
        double previousScale = TrippyConfig.experienceTimeScale;
        try {
            TrippyConfig.experienceTimeScale = 2.0D;
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(80.0D) == 15_600,
                    "Configured time scaling must still apply below the ceiling");
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(800.0D) == 18_000
                            && TwoCeDoseCurve.baseDurationTicks(Double.MAX_VALUE) == 18_000,
                    "Neither configured timing nor extreme amounts may exceed fifteen minutes");
            TwoCeSession session = TwoCeSession.start(null, 80.0D);
            session.setAgeTicks(15_500);
            session.stackDose(800.0D);
            helper.assertTrue(session.effectState().durationTicks() == 18_000
                            && session.effectState().ageTicks() == 15_500,
                    "Scaled late redosing reset or extended the original session clock beyond fifteen minutes");
            TrippyConfig.experienceTimeScale = 0.5D;
            helper.assertTrue(TwoCeDoseCurve.baseDurationTicks(80.0D) == 3_900,
                    "Shorter configured timing must remain available");
            helper.succeed();
        } finally {
            TrippyConfig.experienceTimeScale = previousScale;
        }
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void repeatedAndLateRedosesKeepOneFifteenMinuteClock(GameTestHelper helper) {
        double previousScale = TrippyConfig.experienceTimeScale;
        try {
            TrippyConfig.experienceTimeScale = 1.0D;
            TwoCeSession session = TwoCeSession.start(null, 80.0D, 500.0D);
            long signatureSeed = session.effectState().signature().seed();
            session.setAgeTicks(7_799);
            for (int dose = 0; dose < 5; dose++) {
                session.stackDose(80.0D, 500.0D);
                helper.assertTrue(session.effectState().ageTicks() == 7_799
                                && session.effectState().durationTicks() <= 18_000,
                        "Stacking reset elapsed age or escaped the fifteen-minute cap");
            }
            helper.assertTrue(session.effectState().durationTicks() == 18_000,
                    "Multiple high-strength redoses never reached the authored ceiling");
            session.setAgeTicks(17_999);
            session.stackDose(800.0D, 5000.0D);
            helper.assertTrue(session.remainingTicks() == 1 && session.effectState().durationTicks() == 18_000,
                    "A last-tick redose granted a fresh duration beyond the original session age");
            helper.assertTrue(session.effectState().signature().seed() == signatureSeed,
                    "Late redosing replaced the original trip signature");
            session.tick(null);
            helper.assertTrue(!session.isAlive() && !session.effectState().active() && session.remainingTicks() == 0,
                    "The capped session did not expire on its final tick");
            double expiredTarget = session.targetDoseMilligrams();
            session.stackDose(800.0D, 5000.0D);
            helper.assertTrue(!session.isAlive() && session.targetDoseMilligrams() == expiredTarget,
                    "Direct stacking revived an expired session");

            PsychoactiveExperience experience = new PsychoactiveExperience();
            experience.addTwoCeDose(null, 80.0D, 500.0D);
            int duration = experience.twoCeState().durationTicks();
            helper.assertTrue(experience.scrubPsilocybin(duration) && !experience.twoCeState().active(),
                    "The experience did not accept complete expiry of the first session");
            experience.addTwoCeDose(null, 80.0D, 500.0D);
            helper.assertTrue(experience.twoCeState().active() && experience.twoCeState().ageTicks() == 0
                            && experience.twoCeState().durationTicks() == 7_800,
                    "A new dose after expiry did not create its own six-and-a-half-minute session");
            helper.succeed();
        } finally {
            TrippyConfig.experienceTimeScale = previousScale;
        }
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacySavedClocksCannotRenewTheFifteenMinuteLimit(GameTestHelper helper) {
        CompoundTag saved = TwoCeSession.start(null, 80.0D, 500.0D).save();
        saved.putInt("durationTicks", 54_000);
        saved.putInt("ageTicks", 17_000);
        TwoCeSession ongoing = TwoCeSession.load(saved);
        helper.assertTrue(ongoing.isAlive() && ongoing.effectState().durationTicks() == 18_000
                        && ongoing.effectState().ageTicks() == 17_000 && ongoing.remainingTicks() == 1_000,
                "Loading an older long trip reset age or retained its old maximum duration");
        ongoing.stackDose(800.0D, 5000.0D);
        helper.assertTrue(ongoing.remainingTicks() == 1_000,
                "Redosing a migrated session renewed the fifteen-minute allowance");
        TwoCeSession roundTrip = TwoCeSession.load(ongoing.save());
        helper.assertTrue(roundTrip.effectState().equals(ongoing.effectState()),
                "Saving a migrated capped session changed its clock, dose, or appearance");
        saved.putInt("ageTicks", 20_000);
        TwoCeSession expired = TwoCeSession.load(saved);
        helper.assertTrue(!expired.isAlive() && expired.phase() == TwoCePhase.SOBER && expired.remainingTicks() == 0,
                "An old trip already past fifteen minutes was revived when loaded");
        expired.stackDose(80.0D, 500.0D);
        helper.assertTrue(!expired.isAlive(), "Stacking revived an expired migrated save");
        helper.assertTrue(expired.save().getInt("ageTicks") == 18_000,
                "Loaded elapsed age was not safely bounded to the capped clock");
        saved.putInt("durationTicks", Integer.MAX_VALUE);
        saved.putInt("ageTicks", Integer.MAX_VALUE);
        helper.assertTrue(!TwoCeSession.load(saved).isAlive(), "An extreme saved clock bypassed expiry");
        saved.putInt("durationTicks", -100);
        saved.putInt("ageTicks", -100);
        helper.assertTrue(!TwoCeSession.load(saved).isAlive(), "A negative saved clock started a trip");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void stackingJoinsOneSession(GameTestHelper helper) {
        TwoCeSession session = TwoCeSession.start(null, 10.0D);
        session.stackDose(20.0D);
        helper.assertTrue(session.targetDoseMilligrams() == 30.0D, "Stacked tablets must merge into one target");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void higherDosesOpenCavitiesThenRecursion(GameTestHelper helper) {
        TwoCeEffectState light = peakState(10.0D);
        TwoCeEffectState common = peakState(20.0D);
        TwoCeEffectState strong = peakState(40.0D);
        TwoCeEffectState heavy = peakState(80.0D);
        helper.assertTrue(light.edgeLock() > 0.22D, "10mg must already lock block edges");
        helper.assertTrue(light.cavities() > 0.18D, "10mg must already carve machine cavities");
        helper.assertTrue(common.cavities() > light.cavities(), "20mg must show more cavities than 10mg");
        helper.assertTrue(common.nestedRooms() > light.nestedRooms(), "20mg must open nested rooms");
        helper.assertTrue(strong.nestedRooms() > common.nestedRooms(), "40mg must add nested rooms");
        helper.assertTrue(strong.corridorZoom() > common.corridorZoom(), "40mg must deepen the corridor");
        helper.assertTrue(heavy.recursion() > strong.recursion(), "80mg must open recursive architecture");
        helper.assertTrue(TwoCeDoseCurve.machineLift(60.0D) == 0.0D, "Machine lift must stay off below 70mg");
        helper.assertTrue(TwoCeDoseCurve.machineLift(100.0D) > TwoCeDoseCurve.machineLift(80.0D),
                "Machine lift must keep growing past 80mg");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void persistenceAndBlendKeepTheSameTrip(GameTestHelper helper) {
        TwoCeSession session = TwoCeSession.start(null, 40.0D);
        session.stackDose(10.0D);
        session.setAgeTicks(1_200);
        TwoCeSession restored = TwoCeSession.load(session.save());
        helper.assertTrue(Math.abs(restored.targetDoseMilligrams() - 50.0D) < 0.0001D, "Save/load lost the stacked total");
        helper.assertTrue(restored.effectState().ageTicks() == 1_200, "Save/load lost the clock");
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addTwoCeDose(null, 40.0D);
        helper.assertTrue(experience.twoCeState().active(), "The experience did not start a 2C-E session");
        helper.assertTrue(experience.scrubPsilocybin(2_000), "Timeline editor must accept a solo 2C-E trip");
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(
                PsilocybinEffectState.inactive(), LsdEffectState.inactive(), DmtEffectState.inactive(),
                HoMetEffectState.inactive(), AlLadEffectState.inactive(), experience.twoCeState());
        helper.assertTrue(blend.twoCeActive() && blend.twoCeShaderGain() > 0.99D, "Solo 2C-E must keep full shader gain");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mixedBlendKeepsTwoCeWithAlLadAndHoMet(GameTestHelper helper) {
        TwoCeEffectState twoCe = peakState(40.0D);
        AlLadSession alLadSession = AlLadSession.start(null, 150.0D);
        alLadSession.setAgeTicks((int) (alLadSession.effectState().durationTicks() * 0.40D));
        AlLadEffectState alLad = alLadSession.effectState();
        HoMetSession hoMetSession = HoMetSession.start(null, 20.0D);
        hoMetSession.setAgeTicks((int) (hoMetSession.effectState().durationTicks() * 0.40D));
        HoMetEffectState hoMet = hoMetSession.effectState();
        PsychedelicBlendState withAlLad = CompositeEffectResolver.resolve(
                PsilocybinEffectState.inactive(), LsdEffectState.inactive(), DmtEffectState.inactive(),
                HoMetEffectState.inactive(), alLad, twoCe);
        helper.assertTrue(withAlLad.blended() && withAlLad.twoCeActive() && withAlLad.alLadActive(),
                "2C-E + AL-LAD must both stay in the mixed blend");
        helper.assertTrue(withAlLad.twoCeWeight() > 0.08D && withAlLad.alLadWeight() > 0.08D,
                "Mixed 2C-E + AL-LAD must keep both trip-bar weights");
        PsychedelicBlendState withHoMet = CompositeEffectResolver.resolve(
                PsilocybinEffectState.inactive(), LsdEffectState.inactive(), DmtEffectState.inactive(),
                hoMet, AlLadEffectState.inactive(), twoCe);
        helper.assertTrue(withHoMet.blended() && withHoMet.twoCeActive() && withHoMet.hoMetActive(),
                "2C-E + 4-HO-MET must both stay in the mixed blend");
        helper.assertTrue(withHoMet.combinedProgress(PsilocybinEffectState.inactive(), LsdEffectState.inactive(),
                DmtEffectState.inactive(), hoMet, AlLadEffectState.inactive(), twoCe) < 1.0D,
                "Mixed 2C-E progress must keep tracking the 2C-E clock");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidDosesDoNotStartATrip(GameTestHelper helper) {
        helper.assertTrue(!TwoCeSession.start(null, Double.NaN).isAlive(), "Invalid dose started a trip");
        helper.assertTrue(!TwoCeSession.start(null, -20.0D).isAlive(), "Negative dose started a trip");
        helper.assertTrue(!TwoCeSession.start(null, 0.0D).isAlive(), "Zero dose started a trip");
        helper.succeed();
    }

    private static TwoCeEffectState peakState(double dose) {
        TwoCeSession session = TwoCeSession.start(null, dose);
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.35D));
        return session.effectState();
    }
}
