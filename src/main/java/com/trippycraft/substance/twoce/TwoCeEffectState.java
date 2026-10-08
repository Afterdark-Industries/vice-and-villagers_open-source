package com.trippycraft.substance.twoce;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record TwoCeEffectState(boolean active, double displayDoseMilligrams, double activeDoseMilligrams,
        double targetDoseMilligrams, int ageTicks, int durationTicks, int remainingTicks,
        TwoCePhase phase, double progress, int tier, int repeatedDoses, double acuteTolerance,
        double intensity, double visualPresence, double cavities, double nestedRooms, double tessellation,
        double edgeLock, double perspectiveFold, double recursion, double corridorZoom, double symmetryLock,
        double geometricTrails, double afterglow, double bodyLoad,
        TwoCeTripSignature signature, PsilocybinEnvironmentSnapshot environment,
        double displayContentMilligrams, double targetContentMilligrams) {

    public static TwoCeEffectState inactive() {
        return new TwoCeEffectState(false, 0, 0, 0, 0, 0, 0, TwoCePhase.SOBER, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                TwoCeTripSignature.create(0x2CEL), PsilocybinEnvironmentSnapshot.empty(), 0, 0);
    }

    public TwoCeEffectState scaled(double multiplier) {
        double m = TwoCeDoseCurve.clamp(multiplier, 0.0D, 8.0D);
        if (!active || Math.abs(m - 1.0D) < 0.0001D) {
            return this;
        }
        return new TwoCeEffectState(active, displayDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks, phase, progress, tier, repeatedDoses, acuteTolerance,
                intensity * m, bounded(visualPresence, m), cavities * m, nestedRooms * m, tessellation * m,
                edgeLock * m, perspectiveFold * m, recursion * m, corridorZoom * m, bounded(symmetryLock, m),
                geometricTrails * m, bounded(afterglow, m), bodyLoad * m, signature, environment,
                displayContentMilligrams, targetContentMilligrams);
    }

    private static double bounded(double value, double multiplier) {
        if (multiplier <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - Math.pow(1.0D - TwoCeDoseCurve.clamp(value, 0.0D, 1.0D), multiplier);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(active);
        b.writeDouble(displayDoseMilligrams);
        b.writeDouble(activeDoseMilligrams);
        b.writeDouble(targetDoseMilligrams);
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
        b.writeDouble(cavities);
        b.writeDouble(nestedRooms);
        b.writeDouble(tessellation);
        b.writeDouble(edgeLock);
        b.writeDouble(perspectiveFold);
        b.writeDouble(recursion);
        b.writeDouble(corridorZoom);
        b.writeDouble(symmetryLock);
        b.writeDouble(geometricTrails);
        b.writeDouble(afterglow);
        b.writeDouble(bodyLoad);
        b.writeLong(signature.seed());
        b.writeDouble(signature.cavityBias());
        b.writeDouble(signature.lockBias());
        b.writeDouble(signature.recursionBias());
        b.writeDouble(signature.corridorBias());
        b.writeDouble(signature.symmetryBias());
        b.writeDouble(signature.trailBias());
        encodeEnvironment(b, environment);
        b.writeDouble(displayContentMilligrams);
        b.writeDouble(targetContentMilligrams);
    }

    public static TwoCeEffectState decode(FriendlyByteBuf b) {
        boolean active = b.readBoolean();
        double display = b.readDouble();
        double dose = b.readDouble();
        double target = b.readDouble();
        int age = b.readInt();
        int duration = b.readInt();
        int remaining = b.readInt();
        TwoCePhase phase = b.readEnum(TwoCePhase.class);
        double progress = b.readDouble();
        int tier = b.readInt();
        int repeats = b.readInt();
        double tolerance = b.readDouble();
        double intensity = b.readDouble();
        double presence = b.readDouble();
        double cavities = b.readDouble();
        double nested = b.readDouble();
        double tess = b.readDouble();
        double lock = b.readDouble();
        double fold = b.readDouble();
        double recursion = b.readDouble();
        double corridor = b.readDouble();
        double symmetry = b.readDouble();
        double trails = b.readDouble();
        double afterglow = b.readDouble();
        double bodyLoad = b.readDouble();
        TwoCeTripSignature signature = new TwoCeTripSignature(b.readLong(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
        PsilocybinEnvironmentSnapshot environment = decodeEnvironment(b);
        double displayContent = b.readDouble();
        double targetContent = b.readDouble();
        return new TwoCeEffectState(active, display, dose, target, age, duration, remaining, phase, progress,
                tier, repeats, tolerance, intensity, presence, cavities, nested, tess, lock, fold, recursion,
                corridor, symmetry, trails, afterglow, bodyLoad, signature, environment, displayContent, targetContent);
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
