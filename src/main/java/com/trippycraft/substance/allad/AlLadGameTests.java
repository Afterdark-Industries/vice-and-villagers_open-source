package com.trippycraft.substance.allad;

import com.trippycraft.TrippyCraft;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.homet.HoMetSession;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlLadGameTests {
    private AlLadGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void durationAndPhasesFollowTheAuthoredClock(GameTestHelper helper) {
        helper.assertTrue(AlLadDoseCurve.baseDurationTicks(75.0D) == 3_600, "75ug must last three minutes by default");
        helper.assertTrue(AlLadDoseCurve.baseDurationTicks(150.0D) == 4_800, "150ug must last four minutes by default");
        helper.assertTrue(AlLadDoseCurve.baseDurationTicks(300.0D) == 6_000, "300ug must last five minutes by default");
        helper.assertTrue(AlLadDoseCurve.baseDurationTicks(450.0D) == 7_200, "450ug must last six minutes by default");
        AlLadSession session = AlLadSession.start(null, 150.0D);
        helper.assertTrue(session.phase() == AlLadPhase.ONSET, "A fresh trip must begin in onset");
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.40D));
        helper.assertTrue(session.phase() == AlLadPhase.PEAK, "Mid-clock AL-LAD must be at peak");
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.65D));
        helper.assertTrue(session.phase() == AlLadPhase.PLATEAU, "The long glass plateau must occupy the later clock");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void stackingJoinsOneSessionAndRaisesIntensity(GameTestHelper helper) {
        AlLadSession session = AlLadSession.start(null, 75.0D);
        int beforeRemaining = session.remainingTicks();
        session.stackDose(150.0D);
        for (int i = 0; i < 2_400; i++) {
            session.tick(null);
        }
        AlLadEffectState after = session.effectState();
        helper.assertTrue(after.targetDoseMicrograms() == 225.0D, "Stacked blotters must merge into one target");
        helper.assertTrue(session.remainingTicks() > beforeRemaining - 2_400, "Stacking must extend the remaining clock");
        helper.assertTrue(after.stainedGlass() > peakState(75.0D).stainedGlass() * 0.80D,
                "A stacked 225ug trip must keep substantial stained glass");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void higherDosesOpenGlassThenRecursion(GameTestHelper helper) {
        AlLadEffectState light = peakState(75.0D);
        AlLadEffectState common = peakState(150.0D);
        AlLadEffectState strong = peakState(300.0D);
        AlLadEffectState heavy = peakState(450.0D);
        helper.assertTrue(common.stainedGlass() > light.stainedGlass(), "150ug must show more glass than 75ug");
        helper.assertTrue(strong.prismDepth() > common.prismDepth(), "300ug must add more prismatic depth");
        helper.assertTrue(heavy.cathedralRecursion() > strong.cathedralRecursion(), "450ug must open nested cathedral recursion");
        helper.assertTrue(light.cathedralRecursion() < 0.12D, "75ug must not already be recursive architecture");
        helper.assertTrue(AlLadDoseCurve.heroicLift(400.0D) == 0.0D, "Heroic lift must stay off below 420ug");
        helper.assertTrue(AlLadDoseCurve.heroicLift(600.0D) > AlLadDoseCurve.heroicLift(450.0D),
                "Heroic lift must keep growing past 450ug");
        helper.assertTrue(peakState(700.0D).cathedralRecursion() > heavy.cathedralRecursion(),
                "Stacked heroic amounts must keep deepening the geometry");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void persistenceAndBlendKeepTheSameTrip(GameTestHelper helper) {
        AlLadSession session = AlLadSession.start(null, 150.0D);
        session.stackDose(75.0D);
        session.setAgeTicks(1_200);
        AlLadSession restored = AlLadSession.load(session.save());
        helper.assertTrue(Math.abs(restored.targetDoseMicrograms() - 225.0D) < 0.0001D, "Save/load lost the stacked total");
        helper.assertTrue(restored.effectState().ageTicks() == 1_200, "Save/load lost the clock");
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addAlLadDose(null, 150.0D);
        helper.assertTrue(experience.alLadState().active(), "The experience did not start an AL-LAD session");
        helper.assertTrue(experience.scrubPsilocybin(2_000), "Timeline editor must accept a solo AL-LAD trip");
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(
                PsilocybinEffectState.inactive(), LsdEffectState.inactive(), DmtEffectState.inactive(),
                HoMetEffectState.inactive(), experience.alLadState());
        helper.assertTrue(blend.alLadActive() && blend.alLadShaderGain() > 0.99D, "Solo AL-LAD must keep full shader gain");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mixedBlendKeepsAlLadWithHoMetAndLsd(GameTestHelper helper) {
        AlLadEffectState alLad = peakState(150.0D);
        HoMetSession hoMetSession = HoMetSession.start(null, 20.0D);
        hoMetSession.setAgeTicks((int) (hoMetSession.effectState().durationTicks() * 0.40D));
        HoMetEffectState hoMet = hoMetSession.effectState();
        PsychedelicBlendState withHoMet = CompositeEffectResolver.resolve(
                PsilocybinEffectState.inactive(), LsdEffectState.inactive(), DmtEffectState.inactive(),
                hoMet, alLad);
        helper.assertTrue(withHoMet.blended() && withHoMet.alLadActive() && withHoMet.hoMetActive(),
                "AL-LAD + 4-HO-MET must both stay in the mixed blend");
        helper.assertTrue(withHoMet.alLadWeight() > 0.08D && withHoMet.hoMetWeight() > 0.08D,
                "Mixed AL-LAD + 4-HO-MET must keep both trip-bar weights");
        helper.assertTrue(withHoMet.combinedProgress(PsilocybinEffectState.inactive(), LsdEffectState.inactive(),
                DmtEffectState.inactive(), hoMet, alLad) < 1.0D,
                "Mixed progress must include the AL-LAD clock, not freeze at 100%");

        net.minecraft.nbt.CompoundTag lsdSave = new net.minecraft.nbt.CompoundTag();
        lsdSave.putDouble("activeDoseMicrograms", 200.0D);
        lsdSave.putInt("durationTicks", 12_000);
        lsdSave.putInt("ageTicks", 5_500);
        LsdEffectState lsd = com.trippycraft.substance.lsd.LsdSession.load(lsdSave).effectState();
        PsychedelicBlendState withLsd = CompositeEffectResolver.resolve(
                PsilocybinEffectState.inactive(), lsd, DmtEffectState.inactive(),
                HoMetEffectState.inactive(), alLad);
        helper.assertTrue(withLsd.blended() && withLsd.alLadActive() && withLsd.lsdActive(),
                "AL-LAD + LSD must both stay in the mixed blend");
        helper.assertTrue(withLsd.alLadWeight() > 0.08D && withLsd.lsdWeight() > 0.08D,
                "Mixed AL-LAD + LSD must keep both trip-bar weights");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidDosesDoNotStartATrip(GameTestHelper helper) {
        helper.assertTrue(!AlLadSession.start(null, Double.NaN).isAlive(), "Invalid dose started a trip");
        helper.assertTrue(!AlLadSession.start(null, -20.0D).isAlive(), "Negative dose started a trip");
        helper.assertTrue(!AlLadSession.start(null, 0.0D).isAlive(), "Zero dose started a trip");
        helper.succeed();
    }

    private static AlLadEffectState peakState(double dose) {
        AlLadSession session = AlLadSession.start(null, dose);
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.40D));
        return session.effectState();
    }
}
