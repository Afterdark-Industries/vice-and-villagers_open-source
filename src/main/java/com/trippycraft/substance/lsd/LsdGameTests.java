package com.trippycraft.substance.lsd;

import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
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
public final class LsdGameTests {
    private LsdGameTests() { }

    @GameTest(template = "village/bars/oak_taproom")
    public static void envelopeStartsQuietAndAllPhaseJoinsAreSmooth(GameTestHelper helper) {
        for (int duration : new int[]{4_200, 13_200}) {
            double previous = 0.0D;
            for (int tick = 0; tick <= duration; tick++) {
                double envelope = LsdDoseCurve.phaseEnvelope(LsdDoseCurve.phase(tick, duration), tick / (double) duration);
                helper.assertTrue(envelope >= 0.0D && envelope <= 1.0D, "LSD envelope escaped its range");
                helper.assertTrue(Math.abs(envelope - previous) < 0.003D, "Envelope jumped at tick " + tick);
                previous = envelope;
            }
            helper.assertTrue(previous == 0.0D, "LSD envelope failed to finish quietly");
        }
        helper.assertTrue(LsdDoseCurve.phaseEnvelope(LsdPhase.ONSET, 0.01D) == 0.0D,
                "Fresh blotter instantly changed colors");
        for (double join : new double[]{0.11D, 0.30D, 0.62D, 0.78D, 0.92D}) {
            double before = LsdDoseCurve.phaseEnvelope(LsdPhase.PEAK, join - 0.00001D);
            double after = LsdDoseCurve.phaseEnvelope(LsdPhase.PEAK, join + 0.00001D);
            helper.assertTrue(Math.abs(before - after) < 1.0E-8D, "LSD phase join kicked its envelope");
            for (LsdPhase label : LsdPhase.values()) {
                if (label == LsdPhase.SOBER) continue;
                helper.assertTrue(LsdDoseCurve.phaseEnvelope(label, join) == LsdDoseCurve.phaseEnvelope(LsdPhase.PEAK, join),
                        "Phase label changed a continuous LSD envelope");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allNativeChannelsEaseFromZeroAndFinishWithoutAFlash(GameTestHelper helper) {
        LsdSession session = comparableSession(250.0D);
        for (double amplitude : amplitudes(session.effectState())) {
            helper.assertTrue(amplitude == 0.0D, "Fresh blotter instantly changed a visual channel");
        }
        for (double amplitude : channels(session.effectState()).values()) {
            helper.assertTrue(amplitude == 0.0D, "Fresh blotter instantly changed a shared channel");
        }
        double[] previous = amplitudes(session.effectState());
        double peak = LsdDoseCurve.peakIntensity(250.0D);
        while (session.isAlive()) {
            session.tick(null);
            double[] current = amplitudes(session.effectState());
            for (int index = 0; index < current.length; index++) {
                helper.assertTrue(Math.abs(current[index] - previous[index]) < peak * 0.004D,
                        "Native channel " + index + " flashed at tick " + session.ageTicks());
            }
            previous = current;
        }
        helper.assertTrue(!session.effectState().active() && session.effectState().intensity() == 0.0D,
                "Expired LSD session retained visible effects");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void redosingPreservesRenderedChannelsAndVisualIdentity(GameTestHelper helper) {
        for (double progress : new double[]{0.05D, 0.20D, 0.50D, 0.80D, 0.997D}) {
            LsdSession session = comparableSession(250.0D);
            session.setAgeTicks((int) (session.durationTicks() * progress));
            LsdEffectState before = session.effectState();
            session.stackDose(250.0D);
            LsdEffectState after = session.effectState();
            helper.assertTrue(after.activeDoseMicrograms() == 250.0D && after.targetDoseMicrograms() == 500.0D,
                    "Redose bypassed gradual absorption");
            helper.assertTrue(after.remainingTicks() > before.remainingTicks(), "LSD redose failed to extend its trip");
            double[] beforeAmplitudes = amplitudes(before);
            double[] afterAmplitudes = amplitudes(after);
            for (int index = 0; index < beforeAmplitudes.length; index++) {
                helper.assertTrue(beforeAmplitudes[index] == afterAmplitudes[index],
                        "Redosing changed native channel " + index + " at progress " + progress);
            }
            helper.assertTrue(before.signature().equals(after.signature()), "Redose replaced its palette identity");
            helper.assertTrue(channels(before).equals(channels(after)), "Redose kicked a shared rendering channel");
            session.tick(null);
            helper.assertTrue(session.effectState().activeDoseMicrograms() > 250.0D
                    && session.effectState().activeDoseMicrograms() < 252.0D, "Redose did not begin gradual absorption");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void fiveThousandMicrogramsAndBeyondKeepGrowing(GameTestHelper helper) {
        double savedLegacy = TrippyConfig.lsdMaxVisualIntensity;
        double savedGain = TrippyConfig.lsdVisualGain;
        try {
            TrippyConfig.lsdMaxVisualIntensity = 7.0D;
            TrippyConfig.lsdVisualGain = 1.0D;
            double[] previous = new double[11];
            for (double dose : new double[]{800.0D, 1_296.0D, 2_500.0D, 5_000.0D, 10_000.0D, 1.0E100D, Double.MAX_VALUE}) {
                LsdSession session = comparableSession(dose);
                session.setAgeTicks(6_600);
                double[] current = amplitudes(session.effectState());
                for (int index = 0; index < current.length; index++) {
                    helper.assertTrue(Double.isFinite(current[index]) && current[index] > previous[index],
                            "Native channel " + index + " capped or overflowed at " + dose + " ug");
                }
                previous = current;
            }
            helper.assertTrue(LsdDoseCurve.peakIntensity(5_000.0D) > LsdDoseCurve.peakIntensity(250.0D) * 6.0D,
                    "5000ug was flattened into the ordinary-dose range");
        } finally {
            TrippyConfig.lsdMaxVisualIntensity = savedLegacy;
            TrippyConfig.lsdVisualGain = savedGain;
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacyCeilingsDoNotReduceOrCapContinuousDoseResponse(GameTestHelper helper) {
        double savedLegacy = TrippyConfig.lsdMaxVisualIntensity;
        double savedGain = TrippyConfig.lsdVisualGain;
        try {
            TrippyConfig.lsdVisualGain = 1.0D;
            for (double dose : new double[]{50.0D, 250.0D, 5_000.0D}) {
                LsdSession session = comparableSession(dose);
                session.setAgeTicks(6_600);
                TrippyConfig.lsdMaxVisualIntensity = 7.0D;
                LsdEffectState normal = session.effectState();
                TrippyConfig.lsdMaxVisualIntensity = 1.25D;
                helper.assertTrue(session.effectState().equals(normal), "Saved legacy ceiling reduced " + dose + "ug visuals");
                TrippyConfig.lsdVisualGain = 0.5D;
                helper.assertTrue(session.effectState().visualIntensity() == normal.visualIntensity() * 0.5D,
                        "New gain became an intensity ceiling");
                TrippyConfig.lsdVisualGain = 1.0D;
                TrippyConfig.lsdMaxVisualIntensity = 0.0D;
                for (double amplitude : amplitudes(session.effectState())) {
                    helper.assertTrue(amplitude == 0.0D, "Legacy visual disable preference was lost");
                }
            }
        } finally {
            TrippyConfig.lsdMaxVisualIntensity = savedLegacy;
            TrippyConfig.lsdVisualGain = savedGain;
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void environmentInfluenceInterpolatesInsteadOfChangingInSteps(GameTestHelper helper) {
        LsdSession original = comparableSession(500.0D);
        original.setAgeTicks(6_600);
        CompoundTag saved = original.save();
        CompoundTag observed = PsilocybinEnvironmentSnapshot.empty().save();
        observed.putDouble("water", 1.0D);
        observed.putDouble("rain", 1.0D);
        observed.putDouble("night", 1.0D);
        observed.putDouble("light", 1.0D);
        saved.put("targetEnvironment", observed);
        LsdSession session = LsdSession.load(saved);
        helper.assertTrue(session.effectState().equals(original.effectState()), "New observation instantly changed environment visuals");
        double previousWater = 0.0D;
        for (int tick = 0; tick < 120; tick++) {
            session.tick(null);
            double currentWater = session.effectState().environment().water();
            helper.assertTrue(currentWater > previousWater && currentWater - previousWater < 0.015D,
                    "Environment transitioned as a discrete jump");
            previousWater = currentWater;
        }
        helper.assertTrue(previousWater > 0.65D && previousWater < 0.90D, "Environment failed to ease toward its observation");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void saveAndNetworkPreserveSmoothStateAndMigrateOlderTrips(GameTestHelper helper) {
        LsdSession session = comparableSession(250.0D);
        session.setAgeTicks(4_000);
        session.stackDose(500.0D);
        for (int tick = 0; tick < 120; tick++) session.tick(null);
        LsdSession restored = LsdSession.load(session.save());
        helper.assertTrue(restored.effectState().equals(session.effectState()), "Save/load changed an eased LSD channel");
        for (int tick = 0; tick < 80; tick++) {
            session.tick(null);
            restored.tick(null);
        }
        helper.assertTrue(restored.effectState().equals(session.effectState()), "Restored LSD smoothing changed its timeline");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            session.effectState().encode(buffer);
            helper.assertTrue(LsdEffectState.decode(buffer).equals(session.effectState()), "LSD wire codec changed its state");
            helper.assertTrue(buffer.readableBytes() == 0, "LSD packet shape changed");
        } finally {
            buffer.release();
        }
        CompoundTag legacy = session.save();
        legacy.remove("visualEnvelope");
        legacy.remove("targetEnvironment");
        helper.assertTrue(Double.isFinite(LsdSession.load(legacy).effectState().intensity()), "Older LSD save failed migration");
        helper.assertTrue(!LsdSession.load(new CompoundTag()).isAlive(), "Empty save started an LSD trip");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void doseTierLabelsCannotSwitchColorsOrMotion(GameTestHelper helper) {
        for (double boundary : new double[]{100.0D, 150.0D, 250.0D, 800.0D}) {
            LsdSession lower = comparableSession(boundary - 0.0001D);
            LsdSession upper = comparableSession(boundary + 0.0001D);
            lower.setAgeTicks(6_600);
            upper.setAgeTicks(6_600);
            double[] before = amplitudes(lower.effectState());
            double[] after = amplitudes(upper.effectState());
            for (int index = 0; index < before.length; index++) {
                helper.assertTrue(Math.abs(before[index] - after[index]) < 0.0001D,
                        "Tier label changed channel " + index + " at " + boundary + "ug");
            }
            EnumMap<ExperienceChannel, Double> lowerChannels = channels(lower.effectState());
            EnumMap<ExperienceChannel, Double> upperChannels = channels(upper.effectState());
            for (ExperienceChannel channel : lowerChannels.keySet()) {
                helper.assertTrue(Math.abs(lowerChannels.get(channel) - upperChannels.get(channel)) < 0.0001D,
                        "Tier label changed shared channel " + channel);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidOrOversizedDosesCannotPoisonTheState(GameTestHelper helper) {
        for (double invalid : new double[]{-50.0D, 0.0D, Double.NaN, Double.POSITIVE_INFINITY}) {
            helper.assertTrue(!LsdSession.start(null, invalid).isAlive(), "Invalid LSD amount started a trip");
            LsdSession session = comparableSession(250.0D);
            CompoundTag before = session.save();
            session.stackDose(invalid);
            helper.assertTrue(session.save().equals(before), "Invalid LSD redose altered its trip");
        }
        LsdSession oversized = comparableSession(Double.MAX_VALUE);
        oversized.stackDose(Double.MAX_VALUE);
        oversized.setAgeTicks(6_600);
        oversized.tick(null);
        helper.assertTrue(Double.isFinite(oversized.targetDoseMicrograms())
                && Double.isFinite(oversized.effectState().activeDoseMicrograms()), "Oversized LSD dose overflowed during absorption");
        for (double amplitude : amplitudes(oversized.effectState())) {
            helper.assertTrue(Double.isFinite(amplitude), "Oversized LSD amount poisoned a native visual channel");
        }
        helper.succeed();
    }

    private static LsdSession comparableSession(double dose) {
        CompoundTag saved = LsdSession.start(null, dose).save();
        saved.put("signature", LsdTripSignature.create(0x4C5344534D4F4F54L).save());
        saved.putInt("durationTicks", 13_200);
        return LsdSession.load(saved);
    }

    private static double[] amplitudes(LsdEffectState state) {
        return new double[]{state.intensity(), state.visualIntensity(), state.edgeHum(), state.geometryMelt(),
                state.realityFold(), state.blockAutonomy(), state.liquidFlow(), state.skyVortex(),
                state.entityTrails(), state.prismSplit(), state.electricBloom()};
    }

    private static EnumMap<ExperienceChannel, Double> channels(LsdEffectState state) {
        EnumMap<ExperienceChannel, Double> channels = new EnumMap<>(ExperienceChannel.class);
        LsdEffectProfile.contribute(state, channels, new EnumMap<>(SubstanceFamily.class));
        return channels;
    }
}
