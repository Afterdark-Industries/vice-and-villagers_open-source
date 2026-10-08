package com.trippycraft.substance.homet;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record HoMetEffectState(boolean active, double displayDoseMilligrams, double activeDoseMilligrams,
        double targetDoseMilligrams, int ageTicks, int durationTicks, int remainingTicks,
        HoMetPhase phase, double progress, int tier, int repeatedDoses, double acuteTolerance,
        double intensity, double visualPresence, double spatialDepth, double blockDeformation,
        double trails, double surfaceRecursion, double crystallineColor, double perspectiveInstability,
        double complexity, double musicSensitivity, double afterglow,
        HoMetTripSignature signature, PsilocybinEnvironmentSnapshot environment,
        double displayContentMilligrams, double targetContentMilligrams) {

    public static HoMetEffectState inactive() {
        return new HoMetEffectState(false, 0, 0, 0, 0, 0, 0, HoMetPhase.SOBER, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                HoMetTripSignature.create(0x484F4D455434L), PsilocybinEnvironmentSnapshot.empty(), 0, 0);
    }

    /** Strength, mix, and tolerance changes affect perception without changing dose records or timing. */
    public HoMetEffectState scaled(double multiplier) {
        double m = HoMetDoseCurve.clamp(multiplier, 0.0D, 8.0D);
        if (!active || Math.abs(m - 1.0D) < 0.0001D) return this;
        return new HoMetEffectState(active, displayDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks, phase, progress, tier, repeatedDoses, acuteTolerance,
                intensity * m, bounded(visualPresence, m), spatialDepth * m, blockDeformation * m,
                trails * m, surfaceRecursion * m, crystallineColor * m,
                perspectiveInstability * m, complexity * m, bounded(musicSensitivity, m), bounded(afterglow, m),
                signature, environment, displayContentMilligrams, targetContentMilligrams);
    }

    private static double bounded(double value, double multiplier) {
        // Only visibility/envelope gates are bounded; dose-dependent amplitudes are multiplied above.
        if (multiplier <= 0.0D) return 0.0D;
        return 1.0D - Math.pow(1.0D - HoMetDoseCurve.clamp(value, 0.0D, 1.0D), multiplier);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(active); b.writeDouble(displayDoseMilligrams); b.writeDouble(activeDoseMilligrams);
        b.writeDouble(targetDoseMilligrams); b.writeInt(ageTicks); b.writeInt(durationTicks); b.writeInt(remainingTicks);
        b.writeEnum(phase); b.writeDouble(progress); b.writeInt(tier); b.writeInt(repeatedDoses); b.writeDouble(acuteTolerance);
        b.writeDouble(intensity); b.writeDouble(visualPresence); b.writeDouble(spatialDepth); b.writeDouble(blockDeformation);
        b.writeDouble(trails); b.writeDouble(surfaceRecursion); b.writeDouble(crystallineColor); b.writeDouble(perspectiveInstability);
        b.writeDouble(complexity); b.writeDouble(musicSensitivity); b.writeDouble(afterglow);
        b.writeLong(signature.seed()); b.writeDouble(signature.depthBias()); b.writeDouble(signature.recursionBias());
        b.writeDouble(signature.trailPersistence()); b.writeDouble(signature.crystallineBias()); b.writeDouble(signature.breathingRhythm());
        encodeEnvironment(b, environment);
        b.writeDouble(displayContentMilligrams); b.writeDouble(targetContentMilligrams);
    }

    public static HoMetEffectState decode(FriendlyByteBuf b) {
        boolean active = b.readBoolean(); double display = b.readDouble(); double dose = b.readDouble(); double target = b.readDouble();
        int age = b.readInt(); int duration = b.readInt(); int remaining = b.readInt(); HoMetPhase phase = b.readEnum(HoMetPhase.class);
        double progress = b.readDouble(); int tier = b.readInt(); int repeats = b.readInt(); double tolerance = b.readDouble();
        double intensity = b.readDouble(); double presence = b.readDouble(); double depth = b.readDouble(); double deformation = b.readDouble();
        double trails = b.readDouble(); double recursion = b.readDouble(); double color = b.readDouble(); double perspective = b.readDouble();
        double complexity = b.readDouble(); double music = b.readDouble(); double afterglow = b.readDouble();
        HoMetTripSignature signature = new HoMetTripSignature(b.readLong(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble());
        PsilocybinEnvironmentSnapshot environment = decodeEnvironment(b);
        double displayContent = b.readDouble(); double targetContent = b.readDouble();
        return new HoMetEffectState(active, display, dose, target, age, duration, remaining, phase, progress,
                tier, repeats, tolerance, intensity, presence, depth, deformation, trails, recursion, color,
                perspective, complexity, music, afterglow, signature, environment, displayContent, targetContent);
    }

    private static void encodeEnvironment(FriendlyByteBuf b, PsilocybinEnvironmentSnapshot e) {
        b.writeDouble(e.forest()); b.writeDouble(e.cave()); b.writeDouble(e.rain()); b.writeDouble(e.night());
        b.writeDouble(e.sunriseSunset()); b.writeDouble(e.snow()); b.writeDouble(e.desert()); b.writeDouble(e.water());
        b.writeDouble(e.village()); b.writeDouble(e.nether()); b.writeDouble(e.end()); b.writeDouble(e.organicBlocks());
        b.writeDouble(e.artificialBlocks()); b.writeDouble(e.entities()); b.writeDouble(e.hostileEntities()); b.writeDouble(e.moving());
        b.writeDouble(e.stillness()); b.writeDouble(e.lowHealth()); b.writeDouble(e.hunger()); b.writeDouble(e.light()); b.writeDouble(e.depth());
    }

    private static PsilocybinEnvironmentSnapshot decodeEnvironment(FriendlyByteBuf b) {
        return new PsilocybinEnvironmentSnapshot(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble());
    }
}
