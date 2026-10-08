package com.trippycraft.substance.psilocybin;

import net.minecraft.network.FriendlyByteBuf;

public record PsilocybinEffectState(
        boolean active,
        double displayDoseGrams,
        double activeDoseGrams,
        double targetDoseGrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        PsilocybinPhase phase,
        double progress,
        double intensity,
        double visualIntensity,
        double organicBreathing,
        double surfaceMelting,
        double tracers,
        double spores,
        double fungalOverlays,
        double colorWarmth,
        double colorDepth,
        double audioDepth,
        double cameraBreathing,
        PsilocybinTripSignature signature,
        PsilocybinEnvironmentSnapshot environment
) {
    public static PsilocybinEffectState inactive() {
        return new PsilocybinEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                PsilocybinPhase.SOBER,
                1.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                PsilocybinTripSignature.create(0x51F2EEDL),
                PsilocybinEnvironmentSnapshot.empty()
        );
    }

    public PsilocybinEffectState scaled(double multiplier) {
        double m = Math.max(0.0D, Math.min(1.0D, Double.isFinite(multiplier) ? multiplier : 0.0D));
        if (!active || m >= 0.9999D) return this;
        return new PsilocybinEffectState(active, displayDoseGrams, activeDoseGrams, targetDoseGrams,
                ageTicks, durationTicks, remainingTicks, phase, progress, intensity * m, visualIntensity * m,
                organicBreathing * m, surfaceMelting * m, tracers * m, spores * m, fungalOverlays * m,
                colorWarmth * m, colorDepth * m, audioDepth * m, cameraBreathing * m, signature, environment);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayDoseGrams);
        buffer.writeDouble(activeDoseGrams);
        buffer.writeDouble(targetDoseGrams);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeDouble(intensity);
        buffer.writeDouble(visualIntensity);
        buffer.writeDouble(organicBreathing);
        buffer.writeDouble(surfaceMelting);
        buffer.writeDouble(tracers);
        buffer.writeDouble(spores);
        buffer.writeDouble(fungalOverlays);
        buffer.writeDouble(colorWarmth);
        buffer.writeDouble(colorDepth);
        buffer.writeDouble(audioDepth);
        buffer.writeDouble(cameraBreathing);
        buffer.writeLong(signature.seed());
        buffer.writeEnum(signature.vibe());
        buffer.writeInt(signature.primaryRed());
        buffer.writeInt(signature.primaryGreen());
        buffer.writeInt(signature.primaryBlue());
        buffer.writeInt(signature.secondaryRed());
        buffer.writeInt(signature.secondaryGreen());
        buffer.writeInt(signature.secondaryBlue());
        buffer.writeDouble(signature.breathingRhythm());
        buffer.writeDouble(signature.waveRhythm());
        buffer.writeDouble(signature.hallucinationTendency());
        buffer.writeDouble(signature.particleStyle());
        buffer.writeDouble(signature.environmentalSensitivity());
        buffer.writeDouble(signature.emotionalTone());
        buffer.writeDouble(signature.geometrySoftness());
        buffer.writeDouble(signature.tracerPersistence());
        buffer.writeEnum(signature.patternType());
        encodeEnvironment(buffer, environment);
    }

    public static PsilocybinEffectState decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        double displayDoseGrams = buffer.readDouble();
        double activeDoseGrams = buffer.readDouble();
        double targetDoseGrams = buffer.readDouble();
        int ageTicks = buffer.readInt();
        int durationTicks = buffer.readInt();
        int remainingTicks = buffer.readInt();
        PsilocybinPhase phase = buffer.readEnum(PsilocybinPhase.class);
        double progress = buffer.readDouble();
        double intensity = buffer.readDouble();
        double visualIntensity = buffer.readDouble();
        double organicBreathing = buffer.readDouble();
        double surfaceMelting = buffer.readDouble();
        double tracers = buffer.readDouble();
        double spores = buffer.readDouble();
        double fungalOverlays = buffer.readDouble();
        double colorWarmth = buffer.readDouble();
        double colorDepth = buffer.readDouble();
        double audioDepth = buffer.readDouble();
        double cameraBreathing = buffer.readDouble();
        PsilocybinTripSignature signature = new PsilocybinTripSignature(
                buffer.readLong(),
                buffer.readEnum(PsilocybinTripSignature.PsilocybinTripVibe.class),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readEnum(PsilocybinTripSignature.PsilocybinFungalPattern.class)
        );
        return new PsilocybinEffectState(
                active,
                displayDoseGrams,
                activeDoseGrams,
                targetDoseGrams,
                ageTicks,
                durationTicks,
                remainingTicks,
                phase,
                progress,
                intensity,
                visualIntensity,
                organicBreathing,
                surfaceMelting,
                tracers,
                spores,
                fungalOverlays,
                colorWarmth,
                colorDepth,
                audioDepth,
                cameraBreathing,
                signature,
                decodeEnvironment(buffer)
        );
    }

    private static void encodeEnvironment(FriendlyByteBuf buffer, PsilocybinEnvironmentSnapshot environment) {
        buffer.writeDouble(environment.forest());
        buffer.writeDouble(environment.cave());
        buffer.writeDouble(environment.rain());
        buffer.writeDouble(environment.night());
        buffer.writeDouble(environment.sunriseSunset());
        buffer.writeDouble(environment.snow());
        buffer.writeDouble(environment.desert());
        buffer.writeDouble(environment.water());
        buffer.writeDouble(environment.village());
        buffer.writeDouble(environment.nether());
        buffer.writeDouble(environment.end());
        buffer.writeDouble(environment.organicBlocks());
        buffer.writeDouble(environment.artificialBlocks());
        buffer.writeDouble(environment.entities());
        buffer.writeDouble(environment.hostileEntities());
        buffer.writeDouble(environment.moving());
        buffer.writeDouble(environment.stillness());
        buffer.writeDouble(environment.lowHealth());
        buffer.writeDouble(environment.hunger());
        buffer.writeDouble(environment.light());
        buffer.writeDouble(environment.depth());
    }

    private static PsilocybinEnvironmentSnapshot decodeEnvironment(FriendlyByteBuf buffer) {
        return new PsilocybinEnvironmentSnapshot(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
