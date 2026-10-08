package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.network.ExperienceSnapshotPacket;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadSession;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.homet.HoMetSession;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoseContentDisplayGameTests {
    private DoseContentDisplayGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void everyFinishedFormatDisplaysItsContentWithoutChangingEffects(GameTestHelper helper) {
        int[] contents = {25, 100, 250, 500};
        double[] hoMetEffects = {5, 10, 20, 40};
        double[] alLadEffects = {75, 150, 300, 450};
        for (int tier = 0; tier < contents.length; tier++) {
            PsychoactiveExperience hoMet = new PsychoactiveExperience();
            PsychoactiveExperience legacyHoMet = new PsychoactiveExperience();
            hoMet.addHoMetDose(null, hoMetEffects[tier], contents[tier]);
            legacyHoMet.addHoMetDose(null, hoMetEffects[tier]);
            assertHoMetContent(helper, hoMet.hoMetState(), contents[tier], contents[tier],
                    "Fresh 4-HO-MET format displayed calibrated strength instead of its content");
            helper.assertTrue(hoMet.hoMetState().targetDoseMilligrams() == hoMetEffects[tier],
                    "Physical content changed the preserved 4-HO-MET effect tier");
            int hoMetAge = hoMet.hoMetState().durationTicks() / 2;
            helper.assertTrue(hoMet.scrubPsilocybin(hoMetAge) && legacyHoMet.scrubPsilocybin(hoMetAge),
                    "Could not compare 4-HO-MET sessions at the same progression");
            assertSameHoMetEffects(helper, hoMet.hoMetState(), legacyHoMet.hoMetState());

            PsychoactiveExperience alLad = new PsychoactiveExperience();
            PsychoactiveExperience legacyAlLad = new PsychoactiveExperience();
            alLad.addAlLadDose(null, alLadEffects[tier], contents[tier]);
            legacyAlLad.addAlLadDose(null, alLadEffects[tier]);
            assertAlLadContent(helper, alLad.alLadState(), contents[tier], contents[tier],
                    "Fresh AL-LAD format displayed calibrated micrograms instead of milligram content");
            helper.assertTrue(alLad.alLadState().targetDoseMicrograms() == alLadEffects[tier],
                    "Physical content changed the preserved AL-LAD effect tier");
            int alLadAge = alLad.alLadState().durationTicks() / 2;
            helper.assertTrue(alLad.scrubPsilocybin(alLadAge) && legacyAlLad.scrubPsilocybin(alLadAge),
                    "Could not compare AL-LAD sessions at the same progression");
            assertSameAlLadEffects(helper, alLad.alLadState(), legacyAlLad.alLadState());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void equalCalibratedTotalsKeepDistinctPhysicalTotals(GameTestHelper helper) {
        PsychoactiveExperience mixedHoMet = new PsychoactiveExperience();
        mixedHoMet.addHoMetDose(null, 5, 25);
        mixedHoMet.addHoMetDose(null, 10, 100);
        PsychoactiveExperience smallHoMet = new PsychoactiveExperience();
        for (int dose = 0; dose < 3; dose++) {
            smallHoMet.addHoMetDose(null, 5, 25);
        }
        helper.assertTrue(mixedHoMet.hoMetState().targetDoseMilligrams() == 15 && smallHoMet.hoMetState().targetDoseMilligrams() == 15,
                "The comparison did not retain equal 4-HO-MET calibrated totals");
        helper.assertTrue(mixedHoMet.hoMetState().targetContentMilligrams() == 125 && smallHoMet.hoMetState().targetContentMilligrams() == 75,
                "4-HO-MET physical content was inferred from its effect total instead of summed from consumed items");

        PsychoactiveExperience mixedAlLad = new PsychoactiveExperience();
        mixedAlLad.addAlLadDose(null, 75, 25);
        mixedAlLad.addAlLadDose(null, 150, 100);
        PsychoactiveExperience smallAlLad = new PsychoactiveExperience();
        for (int dose = 0; dose < 3; dose++) {
            smallAlLad.addAlLadDose(null, 75, 25);
        }
        helper.assertTrue(mixedAlLad.alLadState().targetDoseMicrograms() == 225 && smallAlLad.alLadState().targetDoseMicrograms() == 225,
                "The comparison did not retain equal AL-LAD calibrated totals");
        helper.assertTrue(mixedAlLad.alLadState().targetContentMilligrams() == 125 && smallAlLad.alLadState().targetContentMilligrams() == 75,
                "AL-LAD physical content was inferred from microgram strength instead of consumed milligram amounts");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void reservoirPortionsDisplayOneThousandMilligramsAndAccumulateActualContent(GameTestHelper helper) {
        PsychoactiveExperience hoMet = new PsychoactiveExperience();
        PsychoactiveExperience alLad = new PsychoactiveExperience();
        for (int portion = 1; portion <= 5; portion++) {
            hoMet.addHoMetDose(null, 80, 1000);
            alLad.addAlLadDose(null, 900, 1000);
            helper.assertTrue(hoMet.hoMetState().targetContentMilligrams() == portion * 1000
                            && hoMet.hoMetState().targetDoseMilligrams() == portion * 80,
                    "Crystal portions confused physical content with their calibrated effects");
            helper.assertTrue(alLad.alLadState().targetContentMilligrams() == portion * 1000
                            && alLad.alLadState().targetDoseMicrograms() == portion * 900,
                    "Concentrate portions confused physical milligrams with calibrated micrograms");
            if (portion == 1) {
                helper.assertTrue(hoMet.hoMetState().displayContentMilligrams() == 1000
                                && alLad.alLadState().displayContentMilligrams() == 1000,
                        "The first reservoir portion did not display its actual 1000mg content");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void saveLoadAndSnapshotCodecPreserveContentBesideCalibratedEffects(GameTestHelper helper) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addHoMetDose(null, 5, 25);
        experience.addHoMetDose(null, 10, 100);
        experience.addAlLadDose(null, 75, 25);
        experience.addAlLadDose(null, 150, 100);
        helper.assertTrue(experience.scrubPsilocybin(1500), "Could not advance the content-bearing session before persistence");
        PsychoactiveExperience restored = PsychoactiveExperience.load(experience.save());
        helper.assertTrue(restored.hoMetState().equals(experience.hoMetState()), "NBT changed 4-HO-MET content or its preserved effect state");
        helper.assertTrue(restored.alLadState().equals(experience.alLadState()), "NBT changed AL-LAD content or its preserved effect state");
        ExperienceSnapshotPacket snapshot = ExperienceSnapshotPacket.from(restored);
        ExperienceSnapshotPacket decoded = roundTrip(snapshot, helper);
        helper.assertTrue(decoded.hoMet().equals(snapshot.hoMet()) && decoded.alLad().equals(snapshot.alLad()),
                "Snapshot synchronization lost physical content, effect calibration, or another session field");
        helper.assertTrue(decoded.hoMet().targetContentMilligrams() == 125 && decoded.hoMet().targetDoseMilligrams() == 15,
                "The client snapshot contains the wrong 4-HO-MET content/effect pair");
        helper.assertTrue(decoded.alLad().targetContentMilligrams() == 125 && decoded.alLad().targetDoseMicrograms() == 225,
                "The client snapshot contains the wrong AL-LAD content/effect pair");
        helper.assertTrue(decoded.mescaline().equals(snapshot.mescaline()) && decoded.twoCe().equals(snapshot.twoCe())
                        && decoded.lsd().equals(snapshot.lsd()),
                "Adding physical dose fields misaligned neighboring snapshot states");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mixtureScalingChangesPerceptionWithoutChangingDisplayedContent(GameTestHelper helper) {
        PsychoactiveExperience hoMet = new PsychoactiveExperience();
        hoMet.addHoMetDose(null, 10, 100);
        hoMet.scrubPsilocybin(hoMet.hoMetState().durationTicks() / 2);
        HoMetEffectState hoMetState = hoMet.hoMetState();
        HoMetEffectState hoMetScaled = hoMetState.scaled(2);
        assertHoMetContent(helper, hoMetScaled, hoMetState.displayContentMilligrams(), hoMetState.targetContentMilligrams(),
                "Mixture scaling changed displayed or recorded 4-HO-MET milligrams");
        helper.assertTrue(hoMetState.intensity() > 0 && hoMetScaled.intensity() == hoMetState.intensity() * 2,
                "The test did not exercise real 4-HO-MET visual scaling");
        helper.assertTrue(hoMetScaled.targetDoseMilligrams() == hoMetState.targetDoseMilligrams(),
                "Visual scaling changed the accumulated effect contribution");

        PsychoactiveExperience alLad = new PsychoactiveExperience();
        alLad.addAlLadDose(null, 150, 100);
        alLad.scrubPsilocybin(alLad.alLadState().durationTicks() / 2);
        AlLadEffectState alLadState = alLad.alLadState();
        AlLadEffectState alLadScaled = alLadState.scaled(2);
        assertAlLadContent(helper, alLadScaled, alLadState.displayContentMilligrams(), alLadState.targetContentMilligrams(),
                "Mixture scaling changed displayed or recorded AL-LAD milligrams");
        helper.assertTrue(alLadState.intensity() > 0 && alLadScaled.intensity() == alLadState.intensity() * 2,
                "The test did not exercise real AL-LAD visual scaling");
        helper.assertTrue(alLadScaled.targetDoseMicrograms() == alLadState.targetDoseMicrograms(),
                "Visual scaling changed the accumulated calibrated microgram contribution");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void inactiveAndRestartedSessionsDoNotRetainOldContent(GameTestHelper helper) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        assertHoMetContent(helper, experience.hoMetState(), 0, 0, "Inactive 4-HO-MET state reported consumed content");
        assertAlLadContent(helper, experience.alLadState(), 0, 0, "Inactive AL-LAD state reported consumed content");
        experience.addHoMetDose(null, 80, 1000);
        experience.addAlLadDose(null, 900, 1000);
        helper.assertTrue(experience.endActiveTripFromSleep(), "Sleep did not clear active content-bearing trips");
        assertHoMetContent(helper, experience.hoMetState(), 0, 0, "Sleep retained old 4-HO-MET content");
        assertAlLadContent(helper, experience.alLadState(), 0, 0, "Sleep retained old AL-LAD content");
        ExperienceSnapshotPacket cleared = roundTrip(ExperienceSnapshotPacket.clear(), helper);
        assertHoMetContent(helper, cleared.hoMet(), 0, 0, "Clear packet retained old 4-HO-MET content");
        assertAlLadContent(helper, cleared.alLad(), 0, 0, "Clear packet retained old AL-LAD content");
        experience.addHoMetDose(null, 5, 25);
        experience.addAlLadDose(null, 75, 25);
        assertHoMetContent(helper, experience.hoMetState(), 25, 25, "New session carried content from the previous trip");
        assertAlLadContent(helper, experience.alLadState(), 25, 25, "New session carried content from the previous trip");

        PsychoactiveExperience expiredHoMet = new PsychoactiveExperience();
        expiredHoMet.addHoMetDose(null, 80, 1000);
        expiredHoMet.scrubPsilocybin(expiredHoMet.hoMetState().durationTicks());
        assertHoMetContent(helper, expiredHoMet.hoMetState(), 0, 0, "Expired 4-HO-MET state retained content");
        expiredHoMet.addHoMetDose(null, 10, 100);
        assertHoMetContent(helper, expiredHoMet.hoMetState(), 100, 100, "Restarting expired 4-HO-MET retained old content");
        PsychoactiveExperience expiredAlLad = new PsychoactiveExperience();
        expiredAlLad.addAlLadDose(null, 900, 1000);
        expiredAlLad.scrubPsilocybin(expiredAlLad.alLadState().durationTicks());
        assertAlLadContent(helper, expiredAlLad.alLadState(), 0, 0, "Expired AL-LAD state retained content");
        expiredAlLad.addAlLadDose(null, 150, 100);
        assertAlLadContent(helper, expiredAlLad.alLadState(), 100, 100, "Restarting expired AL-LAD retained old content");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void oldSessionSavesInferKnownFormatsWithoutReplacingRecordedContent(GameTestHelper helper) {
        int[] contents = {25, 100, 250, 500};
        double[] hoMetEffects = {5, 10, 20, 40};
        double[] alLadEffects = {75, 150, 300, 450};
        for (int tier = 0; tier < contents.length; tier++) {
            PsychoactiveExperience old = new PsychoactiveExperience();
            old.addHoMetDose(null, hoMetEffects[tier]);
            old.addAlLadDose(null, alLadEffects[tier]);
            CompoundTag saved = old.save();
            removeContentFields(saved, "four_ho_met");
            removeContentFields(saved, "al_lad");
            PsychoactiveExperience restored = PsychoactiveExperience.load(saved);
            assertHoMetContent(helper, restored.hoMetState(), contents[tier], contents[tier],
                    "An old single-format 4-HO-MET save did not migrate to its matching physical content");
            assertAlLadContent(helper, restored.alLadState(), contents[tier], contents[tier],
                    "An old single-format AL-LAD save did not migrate to its matching milligram content");
            helper.assertTrue(restored.hoMetState().targetDoseMilligrams() == hoMetEffects[tier]
                            && restored.alLadState().targetDoseMicrograms() == alLadEffects[tier],
                    "Legacy content migration changed the established effect dose");
        }

        PsychoactiveExperience exact = new PsychoactiveExperience();
        exact.addHoMetDose(null, 5, 25);
        exact.addHoMetDose(null, 10, 100);
        exact.addAlLadDose(null, 75, 25);
        exact.addAlLadDose(null, 150, 100);
        PsychoactiveExperience currentSave = PsychoactiveExperience.load(exact.save());
        helper.assertTrue(currentSave.hoMetState().targetContentMilligrams() == 125
                        && currentSave.alLadState().targetContentMilligrams() == 125,
                "Recorded physical totals were replaced with an inferred calibration total");
        CompoundTag oldMixedSave = exact.save();
        removeContentFields(oldMixedSave, "four_ho_met");
        removeContentFields(oldMixedSave, "al_lad");
        PsychoactiveExperience inferred = PsychoactiveExperience.load(oldMixedSave);
        // An old accumulated strength has no ingredient history. Migration can only approximate it.
        helper.assertTrue(inferred.hoMetState().targetContentMilligrams() == 175
                        && inferred.alLadState().targetContentMilligrams() == 175,
                "Missing legacy physical fields did not use the documented piecewise fallback");
        helper.assertTrue(inferred.hoMetState().targetDoseMilligrams() == 15
                        && inferred.alLadState().targetDoseMicrograms() == 225,
                "Approximating missing legacy content changed the calibrated effect totals");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidContentAddsNoMassAndFiniteOverflowRemainsStable(GameTestHelper helper) {
        for (double invalid : new double[]{0, -10, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            PsychoactiveExperience experience = new PsychoactiveExperience();
            experience.addHoMetDose(null, 5, invalid);
            experience.addAlLadDose(null, 75, invalid);
            assertHoMetContent(helper, experience.hoMetState(), 0, 0, "Invalid 4-HO-MET content entered the physical ledger");
            assertAlLadContent(helper, experience.alLadState(), 0, 0, "Invalid AL-LAD content entered the physical ledger");
            helper.assertTrue(experience.hoMetState().targetDoseMilligrams() == 5
                            && experience.alLadState().targetDoseMicrograms() == 75,
                    "Sanitizing invalid physical content changed valid calibrated effects");
            experience.addHoMetDose(null, 10, 100);
            experience.addAlLadDose(null, 150, 100);
            helper.assertTrue(experience.hoMetState().targetContentMilligrams() == 100
                            && experience.alLadState().targetContentMilligrams() == 100,
                    "Invalid content poisoned a later valid milligram contribution");
        }

        HoMetSession hoMet = HoMetSession.start(null, 5, Double.MAX_VALUE);
        AlLadSession alLad = AlLadSession.start(null, 75, Double.MAX_VALUE);
        hoMet.stackDose(10, Double.MAX_VALUE);
        alLad.stackDose(150, Double.MAX_VALUE);
        helper.assertTrue(hoMet.effectState().targetContentMilligrams() == Double.MAX_VALUE
                        && alLad.effectState().targetContentMilligrams() == Double.MAX_VALUE,
                "Adding very large finite physical amounts overflowed instead of saturating");
        for (int tick = 0; tick < 200; tick++) {
            hoMet.tick(null);
            alLad.tick(null);
            helper.assertTrue(Double.isFinite(hoMet.effectState().displayContentMilligrams())
                            && Double.isFinite(alLad.effectState().displayContentMilligrams()),
                    "Physical display interpolation overflowed at the largest finite content");
        }
        helper.assertTrue(hoMet.effectState().targetDoseMilligrams() == 15
                        && alLad.effectState().targetDoseMicrograms() == 225,
                "Huge stored content increased calibrated effects");
        helper.assertTrue(HoMetSession.load(hoMet.save()).effectState().targetContentMilligrams() == Double.MAX_VALUE
                        && AlLadSession.load(alLad.save()).effectState().targetContentMilligrams() == Double.MAX_VALUE,
                "Huge finite physical content did not survive persistence");
        helper.assertTrue(HoMetSession.contentMilligramsForEffect(Double.MAX_VALUE) == Double.MAX_VALUE
                        && AlLadSession.contentMilligramsForEffect(Double.MAX_VALUE) == Double.MAX_VALUE,
                "Inferring a huge legacy effect amount overflowed or lost all physical content");
        helper.succeed();
    }

    private static void assertHoMetContent(GameTestHelper helper, HoMetEffectState state,
                                           double display, double target, String message) {
        helper.assertTrue(state.displayContentMilligrams() == display && state.targetContentMilligrams() == target, message);
    }

    private static void assertAlLadContent(GameTestHelper helper, AlLadEffectState state,
                                           double display, double target, String message) {
        helper.assertTrue(state.displayContentMilligrams() == display && state.targetContentMilligrams() == target, message);
    }

    private static void assertSameHoMetEffects(GameTestHelper helper, HoMetEffectState current, HoMetEffectState legacy) {
        helper.assertTrue(current.targetDoseMilligrams() == legacy.targetDoseMilligrams()
                        && current.activeDoseMilligrams() == legacy.activeDoseMilligrams()
                        && current.durationTicks() == legacy.durationTicks() && current.remainingTicks() == legacy.remainingTicks()
                        && current.tier() == legacy.tier() && current.phase() == legacy.phase()
                        && current.intensity() == legacy.intensity() && current.spatialDepth() == legacy.spatialDepth()
                        && current.blockDeformation() == legacy.blockDeformation() && current.trails() == legacy.trails()
                        && current.surfaceRecursion() == legacy.surfaceRecursion() && current.crystallineColor() == legacy.crystallineColor()
                        && current.perspectiveInstability() == legacy.perspectiveInstability() && current.complexity() == legacy.complexity()
                        && current.signature().equals(legacy.signature()),
                "Recording actual content changed 4-HO-MET timing, calibrated effects, or visuals");
    }

    private static void assertSameAlLadEffects(GameTestHelper helper, AlLadEffectState current, AlLadEffectState legacy) {
        helper.assertTrue(current.targetDoseMicrograms() == legacy.targetDoseMicrograms()
                        && current.activeDoseMicrograms() == legacy.activeDoseMicrograms()
                        && current.durationTicks() == legacy.durationTicks() && current.remainingTicks() == legacy.remainingTicks()
                        && current.tier() == legacy.tier() && current.phase() == legacy.phase()
                        && current.intensity() == legacy.intensity() && current.stainedGlass() == legacy.stainedGlass()
                        && current.prismDepth() == legacy.prismDepth() && current.paneMotion() == legacy.paneMotion()
                        && current.chromaticFringe() == legacy.chromaticFringe() && current.cathedralRecursion() == legacy.cathedralRecursion()
                        && current.jewelColor() == legacy.jewelColor() && current.bodyLoad() == legacy.bodyLoad()
                        && current.signature().equals(legacy.signature()),
                "Recording actual content changed AL-LAD timing, calibrated effects, or visuals");
    }

    private static ExperienceSnapshotPacket roundTrip(ExperienceSnapshotPacket packet, GameTestHelper helper) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ExperienceSnapshotPacket.encode(packet, buffer);
            ExperienceSnapshotPacket decoded = ExperienceSnapshotPacket.decode(buffer);
            helper.assertTrue(buffer.readableBytes() == 0, "Physical-content snapshot codec left unread or misaligned fields");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static void removeContentFields(CompoundTag savedExperience, String sessionKey) {
        CompoundTag session = savedExperience.getCompound(sessionKey);
        session.remove("targetContentMilligrams");
        session.remove("displayedContentMilligrams");
        savedExperience.put(sessionKey, session);
    }
}
