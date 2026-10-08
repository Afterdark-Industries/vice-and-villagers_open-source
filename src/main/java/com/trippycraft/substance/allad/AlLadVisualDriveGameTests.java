package com.trippycraft.substance.allad;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlLadVisualDriveGameTests {
    private AlLadVisualDriveGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void clientNativeCurveMatchesThePhysicalCalibration(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            for (double dose : new double[]{0, 75, 150, 300, 450, 900, 2250, 4500, 9000}) {
                close(helper, AlLadVisualDrive.peakIntensity(dose, TrippyConfig.globalIntensity),
                        AlLadDoseCurve.peakIntensity(dose), "Client/server native intensity");
            }
            for (double content : new double[]{25, 100, 250, 500, 1000, 5000, 10000}) {
                double effect = AlLadVisualDrive.effectMicrogramsForContent(content);
                close(helper, effect, effectForContent(content), "Native physical calibration");
                close(helper, AlLadSession.contentMilligramsForEffect(effect), content,
                        "Client calibration must invert the untouched server content mapping");
            }
            AlLadEffectState nativeState = fixture(450.0D, 500.0D, 1.0D).effectState();
            AlLadVisualDrive presentation = drive(nativeState, 1.0D, 1.0D);
            close(helper, presentation.heroicLift(), AlLadDoseCurve.heroicLift(4500.0D), "Promoted presentation lift");
            helper.assertTrue(nativeState.activeDoseMicrograms() == 450.0D && nativeState.targetContentMilligrams() == 500.0D,
                    "Client presentation must preserve the calibrated dose and physical content");
            double previous = 0.0D;
            for (double amount : new double[]{0, 0.1, 0.5, 1, 5, 20, 100, 1000}) {
                double compressed = AlLadVisualDrive.compress(amount);
                helper.assertTrue(compressed >= previous && compressed < 1.0D,
                        "Material compression must stay bounded and continue progressing");
                if (amount > 0.0D) helper.assertTrue(compressed > previous, "Material compression flattened prematurely");
                previous = compressed;
            }
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void promotedMaterialsMatchThePriorFiveThousandMilligramAppearance(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            // Keep age, environment, signature and the received phase identical;
            // only the reference's native visual dose/content is ten times larger.
            for (double content : new double[]{10, 25, 100, 250, 500}) {
                for (double envelope : new double[]{0.04D, 0.35D, 1.0D}) {
                    AlLadSession session = fixture(effectForContent(content), content, envelope);
                    CompoundTag source = session.save();
                    CompoundTag environment = new CompoundTag();
                    environment.putDouble("light", 0.85D);
                    environment.putDouble("village", 0.55D);
                    source.put("environment", environment);
                    AlLadEffectState actual = AlLadSession.load(source).effectState();
                    AlLadEffectState reference = promotedReference(source, content * 10.0D).effectState();
                    for (double[] controls : new double[][]{{1, 1}, {0.6D, 0.25D}}) {
                        double[] expected = priorPresentationValues(reference, controls[0], controls[1]);
                        double[] received = values(drive(actual, controls[0], controls[1]));
                        for (int index = 0; index < expected.length; index++) close(helper, received[index], expected[index],
                                "Promoted baseline content=" + content + " envelope=" + envelope + " channel=" + index);
                        for (double extended : higherExtras(drive(actual, controls[0], controls[1]))) {
                            helper.assertTrue(extended == 0.0D, "The promoted anchor must retain exactly the prior material families");
                        }
                    }
                    helper.assertTrue(actual.signature().equals(reference.signature()) && actual.ageTicks() == reference.ageTicks(),
                            "Promotion parity must retain the same phase clock and authored signature");
                    helper.assertTrue(actual.activeDoseMicrograms() == effectForContent(content)
                                    && actual.targetContentMilligrams() == content && actual.bodyLoad() <= 0.46D,
                            "The visual promotion must leave the physical state unchanged");
                }
            }
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void physicalContentOpensDistinctLayersThroughFiveThousandAndBeyond(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            double[] previous = {0, 0, 0, 0};
            for (double content : new double[]{25, 100, 250, 500, 1000, 2500, 5000, 10000}) {
                AlLadVisualDrive current = drive(fixture(effectForContent(content), content, 1.0D).effectState(), 1.0D, 1.0D);
                double[] values = higherExtras(current);
                for (int index = 0; index < values.length; index++) {
                    helper.assertTrue(values[index] >= previous[index], "Premium material family reversed with higher content");
                    if (previous[index] > 0.0D) helper.assertTrue(values[index] > previous[index],
                            "A developed material family stopped growing before the requested high range");
                    helper.assertTrue(Double.isFinite(values[index]) && values[index] < 1.0D,
                            "Premium response must have a finite compressed tail");
                }
                if (content <= 500.0D) for (double value : values) helper.assertTrue(value == 0.0D,
                        "Content at or below the promoted anchor opened a new family");
                if (content >= 5000.0D) helper.assertTrue(values[3] > 0.0D, "The requested upper range must develop immersive continuity");
                previous = values;
            }
            AlLadEffectState threshold = fixture(450.0D, 500.0D, 1.0D).effectState();
            AlLadVisualDrive barelyAbove = AlLadVisualDrive.resolve(500.001D, 450, 450, 1, 1, 1, input(threshold));
            helper.assertTrue(barelyAbove.phaseMultiplicity() > 0.0D && barelyAbove.phaseMultiplicity() < 1.0E-8D,
                    "New refractive families must arrive continuously at their threshold");
            for (double thresholdContent : new double[]{500, 900, 1800, 3000}) {
                double[] at = higherExtras(drive(fixture(effectForContent(thresholdContent), thresholdContent, 1).effectState(), 1, 1));
                double[] above = higherExtras(drive(fixture(effectForContent(thresholdContent + 0.001D), thresholdContent + 0.001D, 1).effectState(), 1, 1));
                int index = thresholdContent == 500 ? 0 : thresholdContent == 900 ? 1 : thresholdContent == 1800 ? 2 : 3;
                helper.assertTrue(at[index] == 0 && above[index] > 0 && above[index] < 1.0E-8D,
                        "Each new family must have a continuous zero-slope threshold");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void redoseKeepsExistingMaterialsThenFollowsAbsorptionAndTolerance(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            AlLadSession session = fixture(450.0D, 500.0D, 1.0D);
            AlLadVisualDrive before = drive(session.effectState(), 1.0D, 1.0D);
            session.stackDose(450.0D, 500.0D);
            AlLadEffectState stacked = session.effectState();
            helper.assertTrue(stacked.targetContentMilligrams() == 1000.0D && stacked.activeDoseMicrograms() == 450.0D,
                    "The fixture must distinguish the new target from its unabsorbed dose");
            AlLadVisualDrive immediate = drive(stacked, 1.0D, 1.0D);
            double[] original = values(before);
            double[] after = values(immediate);
            for (int index = 0; index < original.length; index++) close(helper, after[index], original[index],
                    "Existing presentation at the redose instant " + index);
            session.tick(null);
            helper.assertTrue(drive(session.effectState(), 1.0D, 1.0D).phaseMultiplicity() < 0.001D,
                    "The first absorption tick must retain a gradual laminar transition");
            for (int tick = 0; tick < 800; tick++) session.tick(null);
            AlLadEffectState absorbed = session.effectState();
            double content = AlLadVisualDrive.effectiveContentMilligrams(absorbed.targetContentMilligrams(),
                    absorbed.activeDoseMicrograms(), absorbed.targetDoseMicrograms());
            helper.assertTrue(content > 750.0D && content < 1000.0D,
                    "Actual absorption and acute tolerance must attenuate the target ledger");
            helper.assertTrue(drive(absorbed, 1.0D, 1.0D).phaseMultiplicity() > immediate.phaseMultiplicity(),
                    "The absorbed redose must eventually introduce more continuous refractive architecture");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void synchronizedBlendAndAfterglowStillDimPremiumMaterials(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            AlLadSession session = fixture(4500.0D, 5000.0D, 1.0D);
            AlLadEffectState peak = session.effectState();
            AlLadVisualDrive full = drive(peak, 1.0D, 1.0D);
            AlLadVisualDrive attenuated = drive(peak.scaled(0.20D), 1.0D, 1.0D);
            close(helper, attenuated.phaseContours(), full.phaseContours() * 0.20D, "Received blend attenuation");
            close(helper, attenuated.interferenceVolumes(), full.interferenceVolumes() * 0.20D, "Received immersive attenuation");
            double[] fullHigher = higherExtras(full);
            double[] dilutedHigher = higherExtras(attenuated);
            for (int index = 0; index < fullHigher.length; index++) close(helper, dilutedHigher[index], fullHigher[index] * 0.20D,
                    "Received attenuation of the expanded range " + index);
            helper.assertTrue(attenuated.visualPresence() <= full.visualPresence() * 0.200001D,
                    "Saturated bounded presence must not retain peak-strength coverage after attenuation");
            helper.assertTrue(attenuated.stainedGlass() < full.stainedGlass(),
                    "The received material-channel attenuation must survive compression");
            AlLadVisualDrive weakerBlend = drive(peak, 1.0D, 0.25D);
            close(helper, weakerBlend.laminarDepth(), full.laminarDepth() * 0.25D, "Shader blend attribution");
            close(helper, weakerBlend.immersiveContinuity(), full.immersiveContinuity() * 0.25D, "Expanded shader blend attribution");
            session.setAgeTicks((int) (peak.durationTicks() * 0.97D));
            AlLadVisualDrive fading = drive(session.effectState(), 1.0D, 1.0D);
            helper.assertTrue(fading.interferenceVolumes() < full.interferenceVolumes() * 0.18D,
                    "High physical content must still fade during afterglow");
            helper.assertTrue(fading.immersiveContinuity() < full.immersiveContinuity() * 0.18D,
                    "The expanded high-dose families must still fade with the physical trip");
            helper.assertTrue(allZero(drive(AlLadSession.start(null, 4500.0D, 5000.0D).effectState(), 1, 1)),
                    "A fresh untouched onset must not inherit peak material imagery");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void inactiveControlsAndInvalidExtremesProduceFiniteZeroOrBoundedInputs(GameTestHelper helper) {
        withDefaultIntensity(() -> {
            AlLadEffectState peak = fixture(4500.0D, 5000.0D, 1.0D).effectState();
            helper.assertTrue(allZero(drive(peak, 0, 1)) && allZero(drive(peak, 1, 0))
                            && allZero(drive(peak.scaled(0), 1, 1)) && allZero(drive(AlLadEffectState.inactive(), 1, 1)),
                    "Visual, blend, intensity and inactive zero controls must disable all dose-only drives");
            helper.assertTrue(allZero(AlLadVisualDrive.resolve(5000, 4500, 4500, 0, 1, 1, input(peak))),
                    "Global intensity zero must disable every material family");
            helper.assertTrue(allZero(AlLadVisualDrive.resolve(0, 4500, 4500, 1, 1, 1, input(peak)))
                            && allZero(AlLadVisualDrive.resolve(5000, 4500, 0, 1, 1, 1, input(peak))),
                    "An absent content/target ledger must not manufacture a promoted trip");
            for (double content : new double[]{Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, -1}) {
                AlLadVisualDrive extreme = AlLadVisualDrive.resolve(content, Double.MAX_VALUE, Double.MAX_VALUE,
                        100, 2, 1, input(peak));
                for (double value : values(extreme)) helper.assertTrue(Double.isFinite(value),
                        "Extreme input produced a non-finite shader channel");
            }
            helper.assertTrue(AlLadVisualDrive.compress(Double.MAX_VALUE) == 1.0D
                            && AlLadVisualDrive.compress(Double.NaN) == 0.0D,
                    "Compression must remain defined at invalid and extreme inputs");
            helper.assertTrue(Double.isFinite(AlLadVisualDrive.promotedContentMilligrams(Double.MAX_VALUE))
                            && AlLadVisualDrive.effectMicrogramsForContent(Double.NaN) == 0.0D,
                    "Client promotion must remain finite without accepting invalid physical content");
        });
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void sharedClockWrapsContinuouslyAndComfortZeroFreezesEveryHarmonic(GameTestHelper helper) {
        double period = AlLadVisualDrive.CLOCK_PERIOD_TICKS;
        double epsilon = 0.0001D;
        double before = AlLadVisualDrive.wrapTicks(period - epsilon);
        double after = AlLadVisualDrive.advanceVisualTicks(before, epsilon * 2.0D, 1.0D);
        for (int harmonic : new int[]{1, 7, 29, 37, 41, 47, 97, 149}) {
            double left = Math.sin(AlLadVisualDrive.colorCycle(before) * Math.PI * 2.0D * harmonic);
            double right = Math.sin(AlLadVisualDrive.colorCycle(after) * Math.PI * 2.0D * harmonic);
            helper.assertTrue(Math.abs(left - right) < 0.00001D,
                    "Material/colour harmonic jumped at the shared clock seam");
        }
        close(helper, AlLadVisualDrive.advanceVisualTicks(1234.5D, 5, 0), 1234.5D, "Motion-zero clock");
        close(helper, AlLadVisualDrive.advanceVisualTicks(1234.5D, 0, 1), 1234.5D, "Paused clock");
        close(helper, AlLadVisualDrive.advanceVisualTicks(1234.5D, 5, 0.22D), 1235.6D, "Reduced-motion clock");
        close(helper, AlLadVisualDrive.colorCycle(1234.5D + period), AlLadVisualDrive.colorCycle(1234.5D),
                "Normalized colour-cycle periodicity");
        helper.assertTrue(AlLadVisualDrive.wrapTicks(-1) == period - 1
                        && AlLadVisualDrive.colorCycle(Double.NaN) == 0.0D,
                "Wrapped shader time must stay normalized for negative or invalid clocks");
        helper.succeed();
    }

    private static AlLadSession fixture(double effect, double content, double envelope) {
        AlLadSession session = AlLadSession.start(null, effect, content);
        session.setAgeTicks((int) (session.effectState().durationTicks() * 0.60D));
        CompoundTag tag = session.save();
        tag.putDouble("visualEnvelope", envelope);
        return AlLadSession.load(tag);
    }

    private static double effectForContent(double content) {
        if (content <= 25) return content * 3;
        if (content <= 100) return 75 + (content - 25);
        if (content <= 250) return 150 + (content - 100);
        if (content <= 500) return 300 + (content - 250) * 0.6D;
        return content * 0.9D;
    }

    private static AlLadSession promotedReference(CompoundTag actual, double content) {
        CompoundTag reference = actual.copy();
        double effect = effectForContent(content);
        reference.putDouble("activeDoseMicrograms", effect);
        reference.putDouble("effectiveTargetDoseMicrograms", effect);
        reference.putDouble("targetDoseMicrograms", effect);
        reference.putDouble("displayedDoseMicrograms", effect);
        reference.putDouble("displayedContentMilligrams", content);
        reference.putDouble("targetContentMilligrams", content);
        return AlLadSession.load(reference);
    }

    /** The frozen prior sixteen shader inputs, evaluated from the untouched server reference. */
    private static double[] priorPresentationValues(AlLadEffectState state, double visual, double blend) {
        double dose = state.activeDoseMicrograms();
        double envelope = Math.min(state.intensity(), AlLadDoseCurve.peakIntensity(dose)) / AlLadDoseCurve.peakIntensity(dose);
        double presence = Math.min(state.visualPresence(), AlLadDoseCurve.visualPresence(AlLadDoseCurve.peakIntensity(dose)) * envelope) * visual * blend;
        double gain = envelope * Math.min(visual, 1.35D) * Math.min(blend, 1.0D);
        double content = state.targetContentMilligrams();
        return new double[]{presence, AlLadVisualDrive.compress(state.stainedGlass()), AlLadVisualDrive.compress(state.prismDepth()),
                AlLadVisualDrive.compress(state.paneMotion()), AlLadVisualDrive.compress(state.chromaticFringe()),
                AlLadVisualDrive.compress(state.cathedralRecursion()), AlLadVisualDrive.compress(state.jewelColor()),
                AlLadVisualDrive.compress(state.musicSensitivity()), Math.clamp((dose - 70) / 80, 0, 1),
                Math.clamp((dose - 140) / 160, 0, 1), Math.clamp((dose - 280) / 170, 0, 1), AlLadDoseCurve.heroicLift(dose),
                priorDetail(content, 100, 200) * gain, priorDetail(content, 250, 350) * gain,
                priorDetail(content, 500, 650) * gain, priorDetail(content, 1500, 1200) * gain};
    }

    private static double priorDetail(double content, double threshold, double span) {
        double expansion = Math.max(0, content - threshold) / span;
        double logarithm = Math.log1p(expansion * expansion);
        return logarithm / (1 + logarithm);
    }

    private static AlLadVisualDrive drive(AlLadEffectState state, double visualIntensity, double blendGain) {
        return AlLadVisualDrive.resolve(state.targetContentMilligrams(), state.activeDoseMicrograms(),
                state.targetDoseMicrograms(), TrippyConfig.globalIntensity, visualIntensity, blendGain,
                new AlLadVisualDrive.Context(state.environment().light(), state.environment().village(), state.signature().recursionBias()),
                input(state));
    }

    private static AlLadVisualDrive.Channels input(AlLadEffectState state) {
        return new AlLadVisualDrive.Channels(state.intensity(), state.visualPresence(), state.stainedGlass(),
                state.prismDepth(), state.paneMotion(), state.chromaticFringe(), state.cathedralRecursion(),
                state.jewelColor(), state.musicSensitivity());
    }

    private static double[] higherExtras(AlLadVisualDrive value) {
        return new double[]{value.phaseMultiplicity(), value.laminarComplexity(), value.recursiveInterference(), value.immersiveContinuity()};
    }

    private static double[] values(AlLadVisualDrive value) {
        return new double[]{value.visualPresence(), value.stainedGlass(), value.prismDepth(), value.paneMotion(),
                value.chromaticFringe(), value.cathedralRecursion(), value.jewelColor(), value.musicSensitivity(),
                value.gate75(), value.gate150(), value.gate300(), value.heroicLift(), value.phaseContours(), value.laminarDepth(),
                value.recursiveSheets(), value.interferenceVolumes(), value.phaseMultiplicity(), value.laminarComplexity(),
                value.recursiveInterference(), value.immersiveContinuity()};
    }

    private static boolean allZero(AlLadVisualDrive value) {
        for (double channel : values(value)) if (channel != 0.0D) return false;
        return true;
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
