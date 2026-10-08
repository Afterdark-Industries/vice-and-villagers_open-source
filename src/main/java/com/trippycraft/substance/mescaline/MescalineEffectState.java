package com.trippycraft.substance.mescaline;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record MescalineEffectState(
        boolean active,
        double displayDoseMilligrams,
        double activeDoseMilligrams,
        double targetDoseMilligrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        MescalinePhase phase,
        double progress,
        int tier,
        int repeatedDoses,
        double acuteTolerance,
        double intensity,
        double visualPresence,
        double warmth,
        double colorRichness,
        double surfaceBreathing,
        double organicFlow,
        double tessellation,
        double recursiveComplexity,
        double symmetry,
        double softTrails,
        double spatialDepth,
        double bodyWarmth,
        double nauseaWave,
        double musicSensitivity,
        double afterglow,
        MescalineTripSignature signature,
        PsilocybinEnvironmentSnapshot environment
) {
    public static MescalineEffectState inactive() {
        return new MescalineEffectState(false, 0.0D, 0.0D, 0.0D, 0, 0, 0, MescalinePhase.SOBER,
                1.0D, 0, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                MescalineTripSignature.create(0x6D657363616C696EL), PsilocybinEnvironmentSnapshot.empty());
    }

    public MescalineEffectState scaled(double multiplier) {
        double m = MescalineDoseCurve.clamp(multiplier, 0.0D, 1.0D);
        if (!active || m >= 0.9999D) return this;
        return new MescalineEffectState(active, displayDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks, phase, progress, tier, repeatedDoses, acuteTolerance,
                intensity * m, visualPresence * m, warmth * m, colorRichness * m, surfaceBreathing * m,
                organicFlow * m, tessellation * m, recursiveComplexity * m, symmetry * m, softTrails * m,
                spatialDepth * m, bodyWarmth * m, nauseaWave * m, musicSensitivity * m, afterglow * m,
                signature, environment);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(active); b.writeDouble(displayDoseMilligrams); b.writeDouble(activeDoseMilligrams);
        b.writeDouble(targetDoseMilligrams); b.writeInt(ageTicks); b.writeInt(durationTicks); b.writeInt(remainingTicks);
        b.writeEnum(phase); b.writeDouble(progress); b.writeInt(tier); b.writeInt(repeatedDoses);
        b.writeDouble(acuteTolerance); b.writeDouble(intensity); b.writeDouble(visualPresence); b.writeDouble(warmth);
        b.writeDouble(colorRichness); b.writeDouble(surfaceBreathing); b.writeDouble(organicFlow); b.writeDouble(tessellation);
        b.writeDouble(recursiveComplexity); b.writeDouble(symmetry); b.writeDouble(softTrails); b.writeDouble(spatialDepth);
        b.writeDouble(bodyWarmth); b.writeDouble(nauseaWave); b.writeDouble(musicSensitivity); b.writeDouble(afterglow);
        b.writeLong(signature.seed()); b.writeDouble(signature.organicBias()); b.writeDouble(signature.tessellationBias());
        b.writeDouble(signature.mandalaBias()); b.writeDouble(signature.trailPersistence());
        b.writeDouble(signature.musicSensitivity()); b.writeDouble(signature.breathingRhythm());
        encodeEnvironment(b, environment);
    }

    public static MescalineEffectState decode(FriendlyByteBuf b) {
        boolean active = b.readBoolean(); double display = b.readDouble(); double dose = b.readDouble(); double target = b.readDouble();
        int age = b.readInt(); int duration = b.readInt(); int remaining = b.readInt(); MescalinePhase phase = b.readEnum(MescalinePhase.class);
        double progress = b.readDouble(); int tier = b.readInt(); int repeats = b.readInt(); double tolerance = b.readDouble();
        double intensity = b.readDouble(); double presence = b.readDouble(); double warmth = b.readDouble(); double richness = b.readDouble();
        double breathing = b.readDouble(); double organic = b.readDouble(); double tessellation = b.readDouble(); double recursion = b.readDouble();
        double symmetry = b.readDouble(); double trails = b.readDouble(); double depth = b.readDouble(); double body = b.readDouble();
        double nausea = b.readDouble(); double music = b.readDouble(); double afterglow = b.readDouble();
        MescalineTripSignature signature = new MescalineTripSignature(b.readLong(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble());
        return new MescalineEffectState(active, display, dose, target, age, duration, remaining, phase, progress, tier, repeats,
                tolerance, intensity, presence, warmth, richness, breathing, organic, tessellation, recursion, symmetry, trails,
                depth, body, nausea, music, afterglow, signature, decodeEnvironment(b));
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
