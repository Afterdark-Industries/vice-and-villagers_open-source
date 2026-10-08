package com.trippycraft.substance.salvia;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record SalviaEffectState(
        boolean active,
        double displayDoseUnits,
        double activeDoseUnits,
        double targetDoseUnits,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        SalviaPhase phase,
        double progress,
        int tier,
        double intensity,
        double visualIntensity,
        double anchorSlip,
        double bodyPull,
        double sceneReplacement,
        double loopSeam,
        double cutoutPresence,
        double observerPressure,
        double reentryConfusion,
        double megaPresence,
        double wheelConveyor,
        double zipperFold,
        double flattening,
        double objecthood,
        double entityContact,
        double realityStack,
        double kaleidoscope,
        SalviaTripSignature signature,
        PsilocybinEnvironmentSnapshot environment
) {
    public static SalviaEffectState inactive() {
        return new SalviaEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                SalviaPhase.SOBER,
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
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                SalviaTripSignature.create(0x5A171A55EEDL),
                PsilocybinEnvironmentSnapshot.empty()
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayDoseUnits);
        buffer.writeDouble(activeDoseUnits);
        buffer.writeDouble(targetDoseUnits);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeInt(tier);
        buffer.writeDouble(intensity);
        buffer.writeDouble(visualIntensity);
        buffer.writeDouble(anchorSlip);
        buffer.writeDouble(bodyPull);
        buffer.writeDouble(sceneReplacement);
        buffer.writeDouble(loopSeam);
        buffer.writeDouble(cutoutPresence);
        buffer.writeDouble(observerPressure);
        buffer.writeDouble(reentryConfusion);
        buffer.writeDouble(megaPresence);
        buffer.writeDouble(wheelConveyor);
        buffer.writeDouble(zipperFold);
        buffer.writeDouble(flattening);
        buffer.writeDouble(objecthood);
        buffer.writeDouble(entityContact);
        buffer.writeDouble(realityStack);
        buffer.writeDouble(kaleidoscope);
        encodeSignature(buffer, signature);
        encodeEnvironment(buffer, environment);
    }

    public static SalviaEffectState decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        double displayDoseUnits = buffer.readDouble();
        double activeDoseUnits = buffer.readDouble();
        double targetDoseUnits = buffer.readDouble();
        int ageTicks = buffer.readInt();
        int durationTicks = buffer.readInt();
        int remainingTicks = buffer.readInt();
        SalviaPhase phase = buffer.readEnum(SalviaPhase.class);
        double progress = buffer.readDouble();
        int tier = buffer.readInt();
        double intensity = buffer.readDouble();
        double visualIntensity = buffer.readDouble();
        double anchorSlip = buffer.readDouble();
        double bodyPull = buffer.readDouble();
        double sceneReplacement = buffer.readDouble();
        double loopSeam = buffer.readDouble();
        double cutoutPresence = buffer.readDouble();
        double observerPressure = buffer.readDouble();
        double reentryConfusion = buffer.readDouble();
        double megaPresence = buffer.readDouble();
        double wheelConveyor = buffer.readDouble();
        double zipperFold = buffer.readDouble();
        double flattening = buffer.readDouble();
        double objecthood = buffer.readDouble();
        double entityContact = buffer.readDouble();
        double realityStack = buffer.readDouble();
        double kaleidoscope = buffer.readDouble();
        SalviaTripSignature signature = decodeSignature(buffer);
        PsilocybinEnvironmentSnapshot environment = decodeEnvironment(buffer);
        return new SalviaEffectState(
                active,
                displayDoseUnits,
                activeDoseUnits,
                targetDoseUnits,
                ageTicks,
                durationTicks,
                remainingTicks,
                phase,
                progress,
                tier,
                intensity,
                visualIntensity,
                anchorSlip,
                bodyPull,
                sceneReplacement,
                loopSeam,
                cutoutPresence,
                observerPressure,
                reentryConfusion,
                megaPresence,
                wheelConveyor,
                zipperFold,
                flattening,
                objecthood,
                entityContact,
                realityStack,
                kaleidoscope,
                signature,
                environment
        );
    }

    private static void encodeSignature(FriendlyByteBuf buffer, SalviaTripSignature signature) {
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
        buffer.writeDouble(signature.anchorBias());
        buffer.writeDouble(signature.bodyTorque());
        buffer.writeDouble(signature.recursionBias());
        buffer.writeDouble(signature.cutoutBias());
        buffer.writeDouble(signature.observerBias());
        buffer.writeDouble(signature.sceneBias());
        buffer.writeDouble(signature.aftershock());
    }

    private static SalviaTripSignature decodeSignature(FriendlyByteBuf buffer) {
        return new SalviaTripSignature(
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
                buffer.readDouble()
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