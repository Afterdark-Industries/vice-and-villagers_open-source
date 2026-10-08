package com.trippycraft.substance.lsd;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record LsdEffectState(
        boolean active,
        double displayDoseMicrograms,
        double activeDoseMicrograms,
        double targetDoseMicrograms,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        LsdPhase phase,
        double progress,
        int tier,
        double intensity,
        double visualIntensity,
        double edgeHum,
        double geometryMelt,
        double realityFold,
        double blockAutonomy,
        double liquidFlow,
        double skyVortex,
        double entityTrails,
        double prismSplit,
        double electricBloom,
        LsdTripSignature signature,
        PsilocybinEnvironmentSnapshot environment
) {
    public static LsdEffectState inactive() {
        return new LsdEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                LsdPhase.SOBER,
                1.0D,
                0,
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
                LsdTripSignature.create(0x15D15D15DL),
                PsilocybinEnvironmentSnapshot.empty()
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayDoseMicrograms);
        buffer.writeDouble(activeDoseMicrograms);
        buffer.writeDouble(targetDoseMicrograms);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeInt(tier);
        buffer.writeDouble(intensity);
        buffer.writeDouble(visualIntensity);
        buffer.writeDouble(edgeHum);
        buffer.writeDouble(geometryMelt);
        buffer.writeDouble(realityFold);
        buffer.writeDouble(blockAutonomy);
        buffer.writeDouble(liquidFlow);
        buffer.writeDouble(skyVortex);
        buffer.writeDouble(entityTrails);
        buffer.writeDouble(prismSplit);
        buffer.writeDouble(electricBloom);
        encodeSignature(buffer, signature);
        encodeEnvironment(buffer, environment);
    }

    public static LsdEffectState decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        double displayDoseMicrograms = buffer.readDouble();
        double activeDoseMicrograms = buffer.readDouble();
        double targetDoseMicrograms = buffer.readDouble();
        int ageTicks = buffer.readInt();
        int durationTicks = buffer.readInt();
        int remainingTicks = buffer.readInt();
        LsdPhase phase = buffer.readEnum(LsdPhase.class);
        double progress = buffer.readDouble();
        int tier = buffer.readInt();
        double intensity = buffer.readDouble();
        double visualIntensity = buffer.readDouble();
        double edgeHum = buffer.readDouble();
        double geometryMelt = buffer.readDouble();
        double realityFold = buffer.readDouble();
        double blockAutonomy = buffer.readDouble();
        double liquidFlow = buffer.readDouble();
        double skyVortex = buffer.readDouble();
        double entityTrails = buffer.readDouble();
        double prismSplit = buffer.readDouble();
        double electricBloom = buffer.readDouble();
        LsdTripSignature signature = decodeSignature(buffer);
        PsilocybinEnvironmentSnapshot environment = decodeEnvironment(buffer);
        return new LsdEffectState(active, displayDoseMicrograms, activeDoseMicrograms, targetDoseMicrograms, ageTicks, durationTicks,
                remainingTicks, phase, progress, tier, intensity, visualIntensity, edgeHum, geometryMelt, realityFold, blockAutonomy,
                liquidFlow, skyVortex, entityTrails, prismSplit, electricBloom, signature, environment);
    }

    private static void encodeSignature(FriendlyByteBuf buffer, LsdTripSignature signature) {
        buffer.writeLong(signature.seed());
        buffer.writeInt(signature.primaryRed());
        buffer.writeInt(signature.primaryGreen());
        buffer.writeInt(signature.primaryBlue());
        buffer.writeInt(signature.secondaryRed());
        buffer.writeInt(signature.secondaryGreen());
        buffer.writeInt(signature.secondaryBlue());
        buffer.writeInt(signature.accentRed());
        buffer.writeInt(signature.accentGreen());
        buffer.writeInt(signature.accentBlue());
        buffer.writeDouble(signature.electricSharpness());
        buffer.writeDouble(signature.geometricPrecision());
        buffer.writeDouble(signature.skyVortexBias());
        buffer.writeDouble(signature.blockAutonomyBias());
        buffer.writeDouble(signature.liquidBias());
        buffer.writeDouble(signature.meltFrequency());
        buffer.writeDouble(signature.tracerStrength());
        buffer.writeDouble(signature.entityEchoStrength());
        buffer.writeDouble(signature.prismSplit());
        buffer.writeInt(signature.dominantDistortion());
        buffer.writeInt(signature.skyStyle());
    }

    private static LsdTripSignature decodeSignature(FriendlyByteBuf buffer) {
        return new LsdTripSignature(
                buffer.readLong(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
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
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt()
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
