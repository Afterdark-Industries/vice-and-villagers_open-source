package com.trippycraft.substance.allad;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record AlLadEffectState(boolean active, double displayDoseMicrograms, double activeDoseMicrograms,
        double targetDoseMicrograms, int ageTicks, int durationTicks, int remainingTicks,
        AlLadPhase phase, double progress, int tier, int repeatedDoses, double acuteTolerance,
        double intensity, double visualPresence, double stainedGlass, double prismDepth,
        double paneMotion, double chromaticFringe, double cathedralRecursion, double jewelColor,
        double musicSensitivity, double afterglow, double stimulation, double bodyLoad,
        AlLadTripSignature signature, PsilocybinEnvironmentSnapshot environment,
        double displayContentMilligrams, double targetContentMilligrams) {

    public static AlLadEffectState inactive() {
        return new AlLadEffectState(false, 0, 0, 0, 0, 0, 0, AlLadPhase.SOBER, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                AlLadTripSignature.create(0xA11AD5L), PsilocybinEnvironmentSnapshot.empty(), 0, 0);
    }

    public AlLadEffectState scaled(double multiplier) {
        double m = AlLadDoseCurve.clamp(multiplier, 0.0D, 8.0D);
        if (!active || Math.abs(m - 1.0D) < 0.0001D) {
            return this;
        }
        return new AlLadEffectState(active, displayDoseMicrograms, activeDoseMicrograms, targetDoseMicrograms,
                ageTicks, durationTicks, remainingTicks, phase, progress, tier, repeatedDoses, acuteTolerance,
                intensity * m, bounded(visualPresence, m), stainedGlass * m, prismDepth * m,
                paneMotion * m, chromaticFringe * m, cathedralRecursion * m, jewelColor * m,
                bounded(musicSensitivity, m), bounded(afterglow, m), stimulation * m, bodyLoad * m,
                signature, environment, displayContentMilligrams, targetContentMilligrams);
    }

    private static double bounded(double value, double multiplier) {
        if (multiplier <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - Math.pow(1.0D - AlLadDoseCurve.clamp(value, 0.0D, 1.0D), multiplier);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(active);
        b.writeDouble(displayDoseMicrograms);
        b.writeDouble(activeDoseMicrograms);
        b.writeDouble(targetDoseMicrograms);
        b.writeInt(ageTicks);
        b.writeInt(durationTicks);
        b.writeInt(remainingTicks);
        b.writeEnum(phase);
        b.writeDouble(progress);
        b.writeInt(tier);
        b.writeInt(repeatedDoses);
        b.writeDouble(acuteTolerance);
        b.writeDouble(intensity);
        b.writeDouble(visualPresence);
        b.writeDouble(stainedGlass);
        b.writeDouble(prismDepth);
        b.writeDouble(paneMotion);
        b.writeDouble(chromaticFringe);
        b.writeDouble(cathedralRecursion);
        b.writeDouble(jewelColor);
        b.writeDouble(musicSensitivity);
        b.writeDouble(afterglow);
        b.writeDouble(stimulation);
        b.writeDouble(bodyLoad);
        b.writeLong(signature.seed());
        b.writeDouble(signature.jewelBias());
        b.writeDouble(signature.leadBias());
        b.writeDouble(signature.prismBias());
        b.writeDouble(signature.recursionBias());
        b.writeDouble(signature.paneRhythm());
        b.writeDouble(signature.musicBias());
        encodeEnvironment(b, environment);
        b.writeDouble(displayContentMilligrams);
        b.writeDouble(targetContentMilligrams);
    }

    public static AlLadEffectState decode(FriendlyByteBuf b) {
        boolean active = b.readBoolean();
        double display = b.readDouble();
        double dose = b.readDouble();
        double target = b.readDouble();
        int age = b.readInt();
        int duration = b.readInt();
        int remaining = b.readInt();
        AlLadPhase phase = b.readEnum(AlLadPhase.class);
        double progress = b.readDouble();
        int tier = b.readInt();
        int repeats = b.readInt();
        double tolerance = b.readDouble();
        double intensity = b.readDouble();
        double presence = b.readDouble();
        double glass = b.readDouble();
        double depth = b.readDouble();
        double motion = b.readDouble();
        double fringe = b.readDouble();
        double recursion = b.readDouble();
        double jewel = b.readDouble();
        double music = b.readDouble();
        double afterglow = b.readDouble();
        double stimulation = b.readDouble();
        double bodyLoad = b.readDouble();
        AlLadTripSignature signature = new AlLadTripSignature(b.readLong(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
        return new AlLadEffectState(active, display, dose, target, age, duration, remaining, phase, progress,
                tier, repeats, tolerance, intensity, presence, glass, depth, motion, fringe, recursion, jewel,
                music, afterglow, stimulation, bodyLoad, signature, decodeEnvironment(b), b.readDouble(), b.readDouble());
    }

    private static void encodeEnvironment(FriendlyByteBuf b, PsilocybinEnvironmentSnapshot e) {
        b.writeDouble(e.forest());
        b.writeDouble(e.cave());
        b.writeDouble(e.rain());
        b.writeDouble(e.night());
        b.writeDouble(e.sunriseSunset());
        b.writeDouble(e.snow());
        b.writeDouble(e.desert());
        b.writeDouble(e.water());
        b.writeDouble(e.village());
        b.writeDouble(e.nether());
        b.writeDouble(e.end());
        b.writeDouble(e.organicBlocks());
        b.writeDouble(e.artificialBlocks());
        b.writeDouble(e.entities());
        b.writeDouble(e.hostileEntities());
        b.writeDouble(e.moving());
        b.writeDouble(e.stillness());
        b.writeDouble(e.lowHealth());
        b.writeDouble(e.hunger());
        b.writeDouble(e.light());
        b.writeDouble(e.depth());
    }

    private static PsilocybinEnvironmentSnapshot decodeEnvironment(FriendlyByteBuf b) {
        return new PsilocybinEnvironmentSnapshot(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
    }
}
