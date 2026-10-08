package com.trippycraft.substance.psilocybin;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PsilocybinGameTests {
    private PsilocybinGameTests() { }

    @GameTest(template = "village/bars/oak_taproom")
    public static void timelineEnvelopeStartsQuietAndJoinsContinuously(GameTestHelper helper) {
        for (int duration : new int[] {1_800, 8_400}) {
            double previous = 0.0D;
            for (int tick = 0; tick <= duration; tick++) {
                double envelope = PsilocybinDoseCurve.phaseEnvelope(PsilocybinDoseCurve.phase(tick, duration), tick / (double) duration);
                helper.assertTrue(envelope >= 0.0D && envelope <= 1.0D, "Envelope escaped its visibility range");
                helper.assertTrue(Math.abs(envelope - previous) < 0.006D, "Timeline jumped at tick " + tick);
                previous = envelope;
            }
            helper.assertTrue(previous == 0.0D, "Final sober join retained effects");
        }
        helper.assertTrue(PsilocybinDoseCurve.phaseEnvelope(PsilocybinPhase.ONSET, 0.01D) == 0.0D,
                "First instant of the come-up should remain visually quiet");
        helper.assertTrue(PsilocybinDoseCurve.phaseEnvelope(PsilocybinPhase.ONSET, 0.05D) < 0.04D,
                "Come-up changed color too quickly");
        for (double join : new double[] {0.10D, 0.30D, 0.65D, 0.85D}) {
            double before = PsilocybinDoseCurve.phaseEnvelope(PsilocybinPhase.PEAK, join - 0.00001D);
            double after = PsilocybinDoseCurve.phaseEnvelope(PsilocybinPhase.PEAK, join + 0.00001D);
            helper.assertTrue(Math.abs(before - after) < 1.0E-8D, "A phase join introduced velocity into the envelope");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void nativeChannelsEaseFromZeroAndFinishWithoutAJump(GameTestHelper helper) {
        PsilocybinSession session = comparableSession(4.0D);
        for (double amplitude : amplitudes(session.effectState())) {
            helper.assertTrue(amplitude == 0.0D, "Consuming a fresh capsule instantly changed a visual channel");
        }
        helper.assertTrue(session.effectState().cameraBreathing() == 0.0D, "Camera moved at the instant of ingestion");
        PsilocybinEffectState previous = session.effectState();
        double peak = PsilocybinDoseCurve.peakIntensity(4.0D);
        while (session.isAlive()) {
            session.tick(null);
            PsilocybinEffectState current = session.effectState();
            helper.assertTrue(Math.abs(current.intensity() - previous.intensity()) < peak * 0.003D,
                    "Rendered intensity jumped at tick " + session.ageTicks());
            helper.assertTrue(current.cameraBreathing() == 0.0D,
                    "Texture breathing introduced camera movement at tick " + session.ageTicks());
            previous = current;
        }
        helper.assertTrue(!previous.active() && previous.intensity() == 0.0D, "Expired trip kept a visible state");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void redosingPreservesEveryRenderedChannelIncludingLateAfterglow(GameTestHelper helper) {
        for (double progress : new double[] {0.05D, 0.20D, 0.50D, 0.80D, 0.997D}) {
            PsilocybinSession session = comparableSession(4.0D);
            session.setAgeTicks((int) (session.durationTicks() * progress));
            PsilocybinEffectState before = session.effectState();
            session.stackDose(4.0D);
            PsilocybinEffectState after = session.effectState();
            helper.assertTrue(after.targetDoseGrams() == 8.0D && after.activeDoseGrams() == 4.0D,
                    "Redose confused consumed and absorbed dose");
            helper.assertTrue(after.remainingTicks() > before.remainingTicks(), "Redose failed to extend the trip");
            double[] beforeAmplitudes = amplitudes(before);
            double[] afterAmplitudes = amplitudes(after);
            for (int index = 0; index < beforeAmplitudes.length; index++) {
                helper.assertTrue(beforeAmplitudes[index] == afterAmplitudes[index],
                        "Redosing instantly changed amplitude " + index + " at progress " + progress);
            }
            helper.assertTrue(after.cameraBreathing() == 0.0D && before.cameraBreathing() == 0.0D,
                    "Redose introduced camera breathing");
            helper.assertTrue(after.signature().equals(before.signature()), "Redose replaced the trip's visual identity");
            helper.assertTrue(channels(before).equals(channels(after)), "Redose instantly changed shared rendering channels");
            session.tick(null);
            helper.assertTrue(session.effectState().activeDoseGrams() > 4.0D
                    && session.effectState().activeDoseGrams() < 4.04D, "Redose did not ease into the absorbed dose");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void higherDosesKeepIncreasingPastTheOldCeiling(GameTestHelper helper) {
        double savedGain = TrippyConfig.psilocybinMaxVisualIntensity;
        double savedParticles = TrippyConfig.psilocybinParticleMultiplier;
        try {
            TrippyConfig.psilocybinMaxVisualIntensity = 6.0D;
            TrippyConfig.psilocybinParticleMultiplier = 1.75D;
            double[] previous = new double[10];
            for (double dose : new double[] {7.0D, 14.0D, 28.0D, 56.0D, 1_000.0D, 1.0E100D, Double.MAX_VALUE}) {
                PsilocybinSession session = comparableSession(dose);
                session.setAgeTicks(4_200);
                double[] current = amplitudes(session.effectState());
                for (int index = 0; index < current.length; index++) {
                    helper.assertTrue(Double.isFinite(current[index]) && current[index] > previous[index],
                            "Amplitude " + index + " stopped increasing or overflowed at " + dose + " g");
                }
                helper.assertTrue(session.effectState().cameraBreathing() == 0.0D,
                        "Higher dose reintroduced camera breathing");
                previous = current;
            }
            helper.assertTrue(PsilocybinDoseCurve.normalizedDose(56.0D) > PsilocybinDoseCurve.normalizedDose(28.0D),
                    "Normalized dose still contains a hidden ceiling");
        } finally {
            TrippyConfig.psilocybinMaxVisualIntensity = savedGain;
            TrippyConfig.psilocybinParticleMultiplier = savedParticles;
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void familiarMushroomStrengthsArePreservedWhileHigherDosesEscalate(GameTestHelper helper) {
        double[][] calibration = {
                {0.5D, 0.10D}, {1.0D, 0.55D}, {1.5D, 1.25D},
                {3.0D, 3.55D}, {5.0D, 5.55D}, {7.0D, 6.95D},
                {8.0D, 6.95D + 1.75D * (7.0D / 27.0D)}
        };
        for (double[] point : calibration) {
            helper.assertTrue(Math.abs(PsilocybinDoseCurve.visualIntensity(point[0]) - point[1]) < 1.0E-12D,
                    "The familiar visual strength changed at " + point[0] + " g");
        }
        double previous = PsilocybinDoseCurve.visualIntensity(8.0D);
        for (double dose = 8.01D; dose <= 80.0D; dose += 0.01D) {
            double current = PsilocybinDoseCurve.visualIntensity(dose);
            helper.assertTrue(current > previous, "A stacked dose entered an intensity plateau at " + dose + " g");
            previous = current;
        }
        helper.assertTrue(PsilocybinDoseCurve.visualIntensity(28.0D) > PsilocybinDoseCurve.visualIntensity(8.0D) * 4.0D,
                "High doses still look like a capped version of the 8 g trip");
        double epsilon = 0.00001D;
        double join = PsilocybinDoseCurve.visualIntensity(8.0D);
        double before = (join - PsilocybinDoseCurve.visualIntensity(8.0D - epsilon)) / epsilon;
        double after = (PsilocybinDoseCurve.visualIntensity(8.0D + epsilon) - join) / epsilon;
        helper.assertTrue(Math.abs(before - after) < 0.00002D, "High-dose gain starts with a sudden slope change");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void visualPreferenceScalesRatherThanFlatteningDoseResponse(GameTestHelper helper) {
        double savedGain = TrippyConfig.psilocybinVisualGain;
        double savedLegacy = TrippyConfig.psilocybinMaxVisualIntensity;
        try {
            PsilocybinSession session = comparableSession(28.0D);
            session.setAgeTicks(4_200);
            TrippyConfig.psilocybinMaxVisualIntensity = 6.0D;
            TrippyConfig.psilocybinVisualGain = 1.0D;
            double original = session.effectState().visualIntensity();
            TrippyConfig.psilocybinVisualGain = 0.5D;
            helper.assertTrue(session.effectState().visualIntensity() == original * 0.5D, "Reduced intensity preference became a dose ceiling");
            TrippyConfig.psilocybinVisualGain = 2.0D;
            helper.assertTrue(session.effectState().visualIntensity() == original * 2.0D, "Increased intensity preference did not scale strength");
            TrippyConfig.psilocybinVisualGain = 0.0D;
            for (double amplitude : amplitudes(session.effectState())) helper.assertTrue(amplitude == 0.0D, "Disabled visual preference retained an amplitude");
        } finally {
            TrippyConfig.psilocybinVisualGain = savedGain;
            TrippyConfig.psilocybinMaxVisualIntensity = savedLegacy;
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void oldVisualCeilingCannotEraseReworkedTextures(GameTestHelper helper) {
        double savedGain = TrippyConfig.psilocybinVisualGain;
        double savedLegacy = TrippyConfig.psilocybinMaxVisualIntensity;
        try {
            TrippyConfig.psilocybinVisualGain = 1.0D;
            for (double dose : new double[]{1.0D, 5.0D, 8.0D, 28.0D}) {
                PsilocybinSession session = comparableSession(dose);
                session.setAgeTicks(4_200);
                TrippyConfig.psilocybinMaxVisualIntensity = 6.0D;
                PsilocybinEffectState neutral = session.effectState();
                TrippyConfig.psilocybinMaxVisualIntensity = 1.25D;
                helper.assertTrue(session.effectState().equals(neutral), "An existing 1.25 setting erased texture strength at " + dose + "g");
                TrippyConfig.psilocybinMaxVisualIntensity = 0.0D;
                helper.assertTrue(session.effectState().visualIntensity() == 0.0D, "Legacy disable preference was lost");
            }
        } finally {
            TrippyConfig.psilocybinVisualGain = savedGain;
            TrippyConfig.psilocybinMaxVisualIntensity = savedLegacy;
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void sessionSaveAndNetworkPreserveEasedState(GameTestHelper helper) {
        PsilocybinSession session = comparableSession(4.0D);
        session.setAgeTicks(2_000);
        session.stackDose(3.0D);
        for (int tick = 0; tick < 120; tick++) session.tick(null);
        PsilocybinSession restored = PsilocybinSession.load(session.save());
        helper.assertTrue(restored.effectState().equals(session.effectState()), "Save/load changed an eased channel or dose");
        for (int tick = 0; tick < 80; tick++) {
            session.tick(null);
            restored.tick(null);
        }
        helper.assertTrue(restored.effectState().equals(session.effectState()), "Restored smoothing departed from the original timeline");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            session.effectState().encode(buffer);
            helper.assertTrue(PsilocybinEffectState.decode(buffer).equals(session.effectState()), "Network codec changed an eased channel or dose");
            helper.assertTrue(buffer.readableBytes() == 0, "Psilocybin codec changed its packet shape");
        } finally {
            buffer.release();
        }
        CompoundTag legacy = session.save();
        legacy.remove("visualEnvelope");
        legacy.remove("targetEnvironment");
        PsilocybinSession oldSave = PsilocybinSession.load(legacy);
        helper.assertTrue(oldSave.effectState().active() && Double.isFinite(oldSave.effectState().intensity()), "Legacy session could not migrate its envelope");
        helper.assertTrue(!PsilocybinSession.load(new CompoundTag()).isAlive(), "Empty/older save started a trip");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidDosesCannotPoisonTheSmoothState(GameTestHelper helper) {
        for (double invalid : new double[] {-4.0D, 0.0D, Double.NaN, Double.POSITIVE_INFINITY}) {
            helper.assertTrue(!PsilocybinSession.start(null, invalid).isAlive(), "Invalid amount started a trip");
            PsilocybinSession session = comparableSession(4.0D);
            CompoundTag before = session.save();
            session.stackDose(invalid);
            helper.assertTrue(session.save().equals(before), "Invalid redose altered the session");
        }
        PsilocybinSession session = comparableSession(Double.MAX_VALUE);
        session.stackDose(Double.MAX_VALUE);
        session.setAgeTicks(4_200);
        session.tick(null);
        helper.assertTrue(Double.isFinite(session.effectState().activeDoseGrams())
                && Double.isFinite(session.targetDoseGrams()) && Double.isFinite(session.effectState().intensity()),
                "Finite oversized doses overflowed during absorption");
        helper.succeed();
    }

    private static PsilocybinSession comparableSession(double dose) {
        CompoundTag saved = PsilocybinSession.start(null, dose).save();
        saved.put("signature", PsilocybinTripSignature.create(0x5053494C4F34L).save());
        saved.putInt("durationTicks", 8_400);
        return PsilocybinSession.load(saved);
    }

    private static double[] amplitudes(PsilocybinEffectState state) {
        return new double[] {state.intensity(), state.visualIntensity(), state.organicBreathing(),
                state.surfaceMelting(), state.tracers(), state.spores(), state.fungalOverlays(),
                state.colorWarmth(), state.colorDepth(), state.audioDepth()};
    }

    private static EnumMap<ExperienceChannel, Double> channels(PsilocybinEffectState state) {
        EnumMap<ExperienceChannel, Double> channels = new EnumMap<>(ExperienceChannel.class);
        PsilocybinEffectProfile.contribute(state, channels, new EnumMap<>(SubstanceFamily.class));
        return channels;
    }
}
