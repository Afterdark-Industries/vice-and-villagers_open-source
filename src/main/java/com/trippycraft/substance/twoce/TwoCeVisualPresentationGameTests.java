package com.trippycraft.substance.twoce;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TwoCeVisualPresentationGameTests {
    private TwoCeVisualPresentationGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void fiveHundredContentMatchesTheApprovedFiveThousandVisualProfile(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            TwoCeSession physical = fixture(500.0D, 1.0D);
            TwoCeEffectState state = physical.effectState();
            TwoCeVisualPresentation promoted = presentation(state, 1.0D);
            CompoundTag historical = physical.save();
            historical.putDouble("activeDoseMilligrams", 800.0D);
            historical.putDouble("effectiveTargetDoseMilligrams", 800.0D);
            historical.putDouble("targetDoseMilligrams", 800.0D);
            historical.putDouble("displayedDoseMilligrams", 800.0D);
            historical.putDouble("targetContentMilligrams", 5000.0D);
            historical.putDouble("displayedContentMilligrams", 5000.0D);
            // Keep the signature, envelope and animation age identical so this
            // compares visual calibration rather than two randomly seeded trips.
            TwoCeEffectState previous = TwoCeSession.load(historical).effectState();
            double[] actual = channels(promoted);
            double[] expected = channels(previous);
            for (int index = 0; index < expected.length; index++) {
                close(helper, actual[index], expected[index], "Promoted legacy channel " + index);
            }
            close(helper, promoted.virtualContentMilligrams(), 5000.0D, "Promoted content");
            close(helper, promoted.virtualEffectMilligrams(), 800.0D, "Promoted calibrated effect");
            close(helper, promoted.machineLift(), TwoCeDoseCurve.machineLift(800.0D), "Machine architecture");
            TwoCeVisualDoseResponse approvedHigh = TwoCeVisualDoseResponse.resolve(5000.0D, 800.0D, 800.0D, 1.0D);
            helper.assertTrue(promoted.highDose().equals(approvedHigh), "The existing high-dose detail must be retained exactly");
            helper.assertTrue(promoted.extraDose().equals(new TwoCeVisualPresentation.ExtraDose(0, 0, 0, 0)),
                    "500 mg must preserve the promoted chambers before additional layers arrive");
            helper.assertTrue(state.activeDoseMilligrams() == 80.0D && state.displayContentMilligrams() == 500.0D,
                    "Presentation must leave physical dose and displayed content unchanged");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void promotedLowDoseGainsNativeRecursiveAndSymmetryFamilies(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            TwoCeSession physical = fixture(20.0D, 1.0D);
            TwoCeEffectState state = physical.effectState();
            helper.assertTrue(state.recursion() == 0.0D && state.symmetryLock() == 0.0D,
                    "This fixture must exercise zero native family coefficients");
            TwoCeVisualPresentation response = presentation(state, 1.0D);
            helper.assertTrue(response.recursion() > 0.0D && response.symmetryLock() > 0.0D,
                    "Virtual dose must introduce the authored families rather than multiplying zero channels");
            helper.assertTrue(response.cavities() > state.cavities() && response.nestedRooms() > state.nestedRooms(),
                    "The low-dose promotion must retain and strengthen its existing surface profile");
            helper.assertTrue(response.highDose().surfaceComplexity() == 0.0D,
                    "Low-dose families must not be replaced by premature premium layers");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void premiumLayersContinueThroughFiveThousandAndBeyond(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            double[] prior = {0, 0, 0, 0};
            for (double content : new double[]{500.0D, 1000.0D, 2500.0D, 5000.0D, 10000.0D}) {
                TwoCeVisualPresentation response = presentation(fixture(content, 1.0D).effectState(), 1.0D);
                double[] current = extras(response);
                for (int index = 0; index < current.length; index++) {
                    helper.assertTrue(current[index] >= prior[index], "Premium channel reversed as physical content increased");
                    if (prior[index] > 0.0D) {
                        helper.assertTrue(current[index] > prior[index], "An active premium channel clipped before the next dose");
                    }
                    helper.assertTrue(Double.isFinite(current[index]) && current[index] < 1.0D,
                            "Premium detail must have a finite, compressed tail");
                }
                if (content >= 5000.0D) {
                    helper.assertTrue(current[3] > 0.0D, "The immersive layer must arrive within the requested high range");
                }
                prior = current;
            }
            TwoCeVisualPresentation atBoundary = presentation(fixture(500.0D, 1.0D).effectState(), 1.0D);
            TwoCeVisualPresentation afterBoundary = presentation(withContent(fixture(500.0D, 1.0D), 500.001D).effectState(), 1.0D);
            helper.assertTrue(afterBoundary.extraDose().recursiveVaults() < 1.0E-8D,
                    "Crossing the new detail threshold must be gradual");
            helper.assertTrue(Math.abs(afterBoundary.cavities() - atBoundary.cavities()) < 0.001D,
                    "The promoted cavity profile must stay continuous at 500 mg");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void synchronizedPhaseAndBlendAttenuationStillControlPresentation(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            TwoCeEffectState peak = fixture(5000.0D, 1.0D).effectState();
            TwoCeVisualPresentation full = presentation(peak, 1.0D);
            TwoCeVisualPresentation diluted = presentation(peak.scaled(0.20D), 1.0D);
            close(helper, diluted.cavities(), full.cavities() * 0.20D, "Mixed cavity attenuation");
            close(helper, diluted.recursion(), full.recursion() * 0.20D, "Mixed recursion attenuation");
            close(helper, diluted.highDose().architecturalDepth(), full.highDose().architecturalDepth() * 0.20D,
                    "Mixed high-dose attenuation");
            close(helper, diluted.extraDose().immersiveLattice(), full.extraDose().immersiveLattice() * 0.20D,
                    "Mixed premium attenuation");
            helper.assertTrue(diluted.visualPresence() < full.visualPresence()
                            && diluted.visualPresence() >= 0.0D && diluted.visualPresence() <= 1.0D,
                    "Bounded presence must retain the received mixture attenuation");
            TwoCeSession descending = fixture(5000.0D, 1.0D);
            descending.setAgeTicks((int) (descending.effectState().durationTicks() * 0.97D));
            TwoCeVisualPresentation fade = presentation(descending.effectState(), 1.0D);
            helper.assertTrue(fade.extraDose().recursiveVaults() < full.extraDose().recursiveVaults() * 0.16D,
                    "High physical content must still fade during afterglow");
            helper.assertTrue(presentation(TwoCeSession.start(null, 800.0D, 5000.0D).effectState(), 1.0D)
                            .visualPresence() == 0.0D,
                    "An unstarted synchronized envelope must not display peak visuals at ingestion");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void stackedPremiumLayersFollowAbsorptionRatherThanTargetContent(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            TwoCeSession session = fixture(500.0D, 1.0D);
            TwoCeVisualPresentation beforeStack = presentation(session.effectState(), 1.0D);
            session.stackDose(720.0D, 4500.0D);
            TwoCeEffectState stacked = session.effectState();
            helper.assertTrue(stacked.targetContentMilligrams() == 5000.0D && stacked.activeDoseMilligrams() == 80.0D,
                    "Stack fixture must distinguish target content from absorbed effect");
            helper.assertTrue(presentation(stacked, 1.0D).extraDose().recursiveVaults() == 0.0D,
                    "Unabsorbed target content must not immediately open the premium surface layers");
            double[] before = channels(beforeStack);
            double[] immediatelyAfter = channels(presentation(stacked, 1.0D));
            for (int index = 0; index < before.length; index++) {
                close(helper, immediatelyAfter[index], before[index], "Existing profile at the redose instant " + index);
            }
            session.tick(null);
            helper.assertTrue(presentation(session.effectState(), 1.0D).extraDose().recursiveVaults() < 0.001D,
                    "The first absorption tick must retain a gradual transition");
            for (int tick = 0; tick < 800; tick++) session.tick(null);
            TwoCeEffectState absorbed = session.effectState();
            double effectiveContent = TwoCeVisualDoseResponse.effectiveContentMilligrams(
                    absorbed.targetContentMilligrams(), absorbed.activeDoseMilligrams(), absorbed.targetDoseMilligrams());
            helper.assertTrue(effectiveContent > 3000.0D && effectiveContent < absorbed.targetContentMilligrams(),
                    "The renderer must retain both absorption and acute-tolerance attenuation");
            helper.assertTrue(presentation(absorbed, 1.0D).extraDose().immersiveLattice() > 0.0D,
                    "The absorbed stack must eventually introduce the immersive family");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void zeroControlsAndExtremeInputsRemainFinite(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            TwoCeEffectState state = fixture(5000.0D, 1.0D).effectState();
            helper.assertTrue(allZero(presentation(state, 0.0D)), "Visual intensity zero must disable every visual drive immediately");
            helper.assertTrue(allZero(presentation(state.scaled(0.0D), 1.0D)), "An attenuated-away effect must not retain dose-only drives");
            TwoCeVisualPresentation.Channels channels = input(state);
            helper.assertTrue(allZero(TwoCeVisualPresentation.resolve(5000, 800, 800, 0, 1, 1, channels)),
                    "Global intensity zero must disable premium and baseline effects");
            for (double content : new double[]{Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, -1.0D}) {
                TwoCeVisualPresentation response = TwoCeVisualPresentation.resolve(content,
                        Double.MAX_VALUE, Double.MAX_VALUE, 100.0D, 1.0D, 1.0D, channels);
                for (double value : channels(response)) {
                    helper.assertTrue(Double.isFinite(value), "Extreme numeric input produced a non-finite baseline channel");
                }
                for (double value : extras(response)) {
                    helper.assertTrue(Double.isFinite(value), "Extreme numeric input produced a non-finite premium channel");
                }
                helper.assertTrue(Double.isFinite(response.machineLift()) && Double.isFinite(response.virtualEffectMilligrams()),
                        "Extreme content produced a non-finite shader control");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void clientCalibrationMatchesTheUnchangedPhysicalCurve(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            for (double content : new double[]{0, 25, 100, 250, 500, 1000, 2500, 5000}) {
                double effect = TwoCeVisualPresentation.effectMilligramsForContent(content);
                close(helper, TwoCeSession.contentMilligramsForEffect(effect), content, "Canonical inverse calibration");
                close(helper, TwoCeVisualPresentation.peakIntensity(effect, TrippyConfig.globalIntensity),
                        TwoCeDoseCurve.peakIntensity(effect), "Client/server native intensity calibration");
            }
            helper.assertTrue(TwoCeVisualPresentation.effectMilligramsForContent(500.0D) == 80.0D
                            && TwoCeVisualPresentation.effectMilligramsForContent(5000.0D) == 800.0D,
                    "Visual promotion must not reinterpret the physical ledger's calibrated units");
        });
        helper.succeed();
    }

    private static TwoCeSession fixture(double content, double envelope) {
        TwoCeSession session = TwoCeSession.start(null, TwoCeVisualPresentation.effectMilligramsForContent(content), content);
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.60D));
        CompoundTag tag = session.save();
        tag.putDouble("visualEnvelope", envelope);
        return TwoCeSession.load(tag);
    }

    private static TwoCeSession withContent(TwoCeSession template, double content) {
        CompoundTag tag = template.save();
        double effect = TwoCeVisualPresentation.effectMilligramsForContent(content);
        tag.putDouble("activeDoseMilligrams", effect);
        tag.putDouble("effectiveTargetDoseMilligrams", effect);
        tag.putDouble("targetDoseMilligrams", effect);
        tag.putDouble("displayedDoseMilligrams", effect);
        tag.putDouble("targetContentMilligrams", content);
        tag.putDouble("displayedContentMilligrams", content);
        return TwoCeSession.load(tag);
    }

    private static TwoCeVisualPresentation presentation(TwoCeEffectState state, double visualIntensity) {
        return TwoCeVisualPresentation.resolve(state.targetContentMilligrams(), state.activeDoseMilligrams(),
                state.targetDoseMilligrams(), TrippyConfig.globalIntensity, visualIntensity,
                state.signature().symmetryBias(), input(state));
    }

    private static TwoCeVisualPresentation.Channels input(TwoCeEffectState state) {
        return new TwoCeVisualPresentation.Channels(state.intensity(), state.visualPresence(), state.cavities(),
                state.nestedRooms(), state.tessellation(), state.edgeLock(), state.perspectiveFold(), state.recursion(),
                state.corridorZoom(), state.symmetryLock(), state.geometricTrails());
    }

    private static double[] channels(TwoCeVisualPresentation value) {
        return new double[]{value.visualPresence(), value.cavities(), value.nestedRooms(), value.tessellation(),
                value.edgeLock(), value.perspectiveFold(), value.recursion(), value.corridorZoom(),
                value.symmetryLock(), value.geometricTrails()};
    }

    private static double[] channels(TwoCeEffectState value) {
        return new double[]{value.visualPresence(), value.cavities(), value.nestedRooms(), value.tessellation(),
                value.edgeLock(), value.perspectiveFold(), value.recursion(), value.corridorZoom(),
                value.symmetryLock(), value.geometricTrails()};
    }

    private static double[] extras(TwoCeVisualPresentation value) {
        return new double[]{value.extraDose().recursiveVaults(), value.extraDose().impossiblePerspective(),
                value.extraDose().materialMorphogenesis(), value.extraDose().immersiveLattice()};
    }

    private static boolean allZero(TwoCeVisualPresentation value) {
        for (double channel : channels(value)) if (channel != 0.0D) return false;
        for (double channel : extras(value)) if (channel != 0.0D) return false;
        return value.machineLift() == 0.0D && value.gate10() == 0.0D
                && value.highDose().surfaceComplexity() == 0.0D && value.highDose().architecturalDepth() == 0.0D
                && value.highDose().perspectiveComplexity() == 0.0D && value.highDose().immersiveGeometry() == 0.0D;
    }

    private static void close(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) <= Math.max(1.0D, Math.abs(expected)) * 1.0E-10D,
                message + ": expected " + expected + ", received " + actual);
    }

    private static void withDefaultIntensity(Runnable checks) {
        double prior = TrippyConfig.globalIntensity;
        try {
            TrippyConfig.globalIntensity = 1.0D;
            checks.run();
        } finally {
            TrippyConfig.globalIntensity = prior;
        }
    }
}
