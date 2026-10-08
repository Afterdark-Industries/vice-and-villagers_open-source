package com.trippycraft.substance.nitrous;

import com.trippycraft.TrippyCraft;
import com.trippycraft.substance.PsychoactiveExperience;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NitrousGameTests {
    private NitrousGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void durationAndPhasesFollowTheRushClock(GameTestHelper helper) {
        helper.assertTrue(NitrousDoseCurve.baseDurationTicks(1.0D) == 320, "A single hit must last sixteen seconds");
        NitrousSession session = NitrousSession.start(null, 1.0D);
        helper.assertTrue(session.phase() == NitrousPhase.INHALE, "A fresh hit must begin on the inhale");
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.40D));
        helper.assertTrue(session.phase() == NitrousPhase.PEAK, "Mid-clock nitrous must be at peak");
        helper.assertTrue(session.remainingTicks() / 20 >= 9 && session.remainingTicks() / 20 <= 13,
                "Peak remaining time must sit near twelve seconds");
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.75D));
        helper.assertTrue(session.phase() == NitrousPhase.FADE, "The rush must fade after peak");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void stackingJoinsOneRush(GameTestHelper helper) {
        NitrousSession session = NitrousSession.start(null, 1.0D);
        int before = session.remainingTicks();
        session.stackDose(1.0D);
        helper.assertTrue(session.targetHits() == 2.0D, "Stacked canisters must merge into one rush");
        helper.assertTrue(session.remainingTicks() > before, "Redosing must extend the remaining clock");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void peakOpensTunnelBloomAndEcho(GameTestHelper helper) {
        NitrousEffectState peak = peakState(1.0D);
        helper.assertTrue(peak.tunnel() > 0.35D, "Peak must open a tunnel");
        helper.assertTrue(peak.fisheye() > 0.35D, "Peak must stretch with fisheye");
        helper.assertTrue(peak.bloom() > 0.30D, "Peak must bloom");
        helper.assertTrue(peak.echo() > 0.28D, "Peak must leave time-echo trails");
        helper.assertTrue(peak.euphoria() > 0.40D, "Peak must feel euphoric");
        helper.assertTrue(peak.musicAppreciation() > 0.50D, "Peak must heighten music");
        NitrousEffectState stacked = peakState(2.0D);
        helper.assertTrue(stacked.tunnel() > peak.tunnel(), "A second hit must deepen the tunnel");
        NitrousEffectState four = peakState(4.0D);
        NitrousEffectState eight = peakState(8.0D);
        helper.assertTrue(four.tunnel() > stacked.tunnel(), "Four hits must keep growing past two");
        helper.assertTrue(eight.bloom() > four.bloom(), "Eight hits must keep growing past four");
        helper.assertTrue(eight.fisheye() > peak.fisheye() * 2.0D, "Stacked hits must more than double the fisheye");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void persistenceKeepsTheSameRush(GameTestHelper helper) {
        NitrousSession session = NitrousSession.start(null, 1.0D);
        session.stackDose(1.0D);
        session.setAgeTicks(80);
        NitrousSession restored = NitrousSession.load(session.save());
        helper.assertTrue(Math.abs(restored.targetHits() - 2.0D) < 0.0001D, "Save/load lost stacked hits");
        helper.assertTrue(restored.effectState().ageTicks() == 80, "Save/load lost the clock");
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addNitrousDose(null, 1.0D);
        helper.assertTrue(experience.nitrousState().active(), "The experience did not start a nitrous rush");
        helper.assertTrue(experience.scrubPsilocybin(40), "Timeline editor must accept a solo nitrous rush");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidHitsDoNotStartARush(GameTestHelper helper) {
        helper.assertTrue(!NitrousSession.start(null, Double.NaN).isAlive(), "Invalid hit started a rush");
        helper.assertTrue(!NitrousSession.start(null, -1.0D).isAlive(), "Negative hit started a rush");
        helper.assertTrue(!NitrousSession.start(null, 0.0D).isAlive(), "Zero hit started a rush");
        helper.succeed();
    }

    private static NitrousEffectState peakState(double hits) {
        NitrousSession session = NitrousSession.start(null, hits);
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.40D));
        return session.effectState();
    }
}
