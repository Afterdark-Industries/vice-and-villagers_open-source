package com.trippycraft.substance.dmt;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public record DmtEffectState(
        boolean active,
        double displayDoseUnits,
        double activeDoseUnits,
        double targetDoseUnits,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        DmtPhase phase,
        DmtStage stage,
        double progress,
        int tier,
        double intensity,
        double visualIntensity,
        double chrysanthemum,
        double tunnelDepth,
        double hyperGeometry,
        double fractalOverload,
        double worldDissolve,
        double chromaticBurst,
        double entityPresence,
        double egoLoss,
        double cameraPull,
        double alienShimmer,
        double spatialInstability,
        double edgeFlicker,
        double symbolPressure,
        double veilThinness,
        double breakthroughTransition,
        double impossibleArchitecture,
        double symbolLanguage,
        double environmentalConsciousness,
        double realityReplacement,
        double dominance,
        DmtFractalScaffold fractalScaffold,
        DmtEncounterState encounter,
        DmtTripSignature signature,
        PsilocybinEnvironmentSnapshot environment
) {
    public static DmtEffectState inactive() {
        return new DmtEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                DmtPhase.SOBER,
                DmtStage.SOBER,
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
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                DmtFractalScaffold.empty(),
                DmtEncounterState.inactive(DmtEncounterTheme.HYPERDIMENSIONAL_CHAMBERS),
                DmtTripSignature.create(0xD17D17D17L),
                PsilocybinEnvironmentSnapshot.empty()
        );
    }

    public boolean hasAuthoredEffects() {
        return chrysanthemum > 0.0001D
                || tunnelDepth > 0.0001D
                || hyperGeometry > 0.0001D
                || fractalOverload > 0.0001D
                || worldDissolve > 0.0001D
                || chromaticBurst > 0.0001D
                || entityPresence > 0.0001D
                || egoLoss > 0.0001D
                || cameraPull > 0.0001D
                || alienShimmer > 0.0001D
                || spatialInstability > 0.0001D
                || edgeFlicker > 0.0001D
                || symbolPressure > 0.0001D
                || veilThinness > 0.0001D
                || breakthroughTransition > 0.0001D
                || impossibleArchitecture > 0.0001D
                || symbolLanguage > 0.0001D
                || environmentalConsciousness > 0.0001D
                || realityReplacement > 0.0001D
                || dominance > 0.0001D;
    }

    public void encode(FriendlyByteBuf buffer) {
        DmtFractalScaffold scaffold = fractalScaffold == null ? DmtFractalScaffold.empty() : fractalScaffold;
        buffer.writeBoolean(active);
        buffer.writeDouble(displayDoseUnits);
        buffer.writeDouble(activeDoseUnits);
        buffer.writeDouble(targetDoseUnits);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeEnum(stage);
        buffer.writeDouble(progress);
        buffer.writeInt(tier);
        buffer.writeDouble(intensity);
        buffer.writeDouble(visualIntensity);
        buffer.writeDouble(chrysanthemum);
        buffer.writeDouble(tunnelDepth);
        buffer.writeDouble(hyperGeometry);
        buffer.writeDouble(fractalOverload);
        buffer.writeDouble(worldDissolve);
        buffer.writeDouble(chromaticBurst);
        buffer.writeDouble(entityPresence);
        buffer.writeDouble(egoLoss);
        buffer.writeDouble(cameraPull);
        buffer.writeDouble(alienShimmer);
        buffer.writeDouble(spatialInstability);
        buffer.writeDouble(edgeFlicker);
        buffer.writeDouble(symbolPressure);
        buffer.writeDouble(veilThinness);
        buffer.writeDouble(breakthroughTransition);
        buffer.writeDouble(impossibleArchitecture);
        buffer.writeDouble(symbolLanguage);
        buffer.writeDouble(environmentalConsciousness);
        buffer.writeDouble(realityReplacement);
        buffer.writeDouble(dominance);
        scaffold.encode(buffer);
        encounter.encode(buffer);
        encodeSignature(buffer, signature);
        encodeEnvironment(buffer, environment);
    }

    public static DmtEffectState decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        double displayDoseUnits = buffer.readDouble();
        double activeDoseUnits = buffer.readDouble();
        double targetDoseUnits = buffer.readDouble();
        int ageTicks = buffer.readInt();
        int durationTicks = buffer.readInt();
        int remainingTicks = buffer.readInt();
        DmtPhase phase = buffer.readEnum(DmtPhase.class);
        DmtStage stage = buffer.readEnum(DmtStage.class);
        double progress = buffer.readDouble();
        int tier = buffer.readInt();
        double intensity = buffer.readDouble();
        double visualIntensity = buffer.readDouble();
        double chrysanthemum = buffer.readDouble();
        double tunnelDepth = buffer.readDouble();
        double hyperGeometry = buffer.readDouble();
        double fractalOverload = buffer.readDouble();
        double worldDissolve = buffer.readDouble();
        double chromaticBurst = buffer.readDouble();
        double entityPresence = buffer.readDouble();
        double egoLoss = buffer.readDouble();
        double cameraPull = buffer.readDouble();
        double alienShimmer = buffer.readDouble();
        double spatialInstability = buffer.readDouble();
        double edgeFlicker = buffer.readDouble();
        double symbolPressure = buffer.readDouble();
        double veilThinness = buffer.readDouble();
        double breakthroughTransition = buffer.readDouble();
        double impossibleArchitecture = buffer.readDouble();
        double symbolLanguage = buffer.readDouble();
        double environmentalConsciousness = buffer.readDouble();
        double realityReplacement = buffer.readDouble();
        double dominance = buffer.readDouble();
        DmtFractalScaffold fractalScaffold = DmtFractalScaffold.decode(buffer);
        DmtEncounterState encounter = DmtEncounterState.decode(buffer);
        DmtTripSignature signature = decodeSignature(buffer);
        PsilocybinEnvironmentSnapshot environment = decodeEnvironment(buffer);
        return new DmtEffectState(active, displayDoseUnits, activeDoseUnits, targetDoseUnits, ageTicks, durationTicks, remainingTicks,
                phase, stage, progress, tier, intensity, visualIntensity, chrysanthemum, tunnelDepth, hyperGeometry, fractalOverload,
                worldDissolve, chromaticBurst, entityPresence, egoLoss, cameraPull, alienShimmer, spatialInstability, edgeFlicker,
                symbolPressure, veilThinness, breakthroughTransition, impossibleArchitecture, symbolLanguage,
                environmentalConsciousness, realityReplacement, dominance, fractalScaffold, encounter, signature, environment);
    }

    private static void encodeSignature(FriendlyByteBuf buffer, DmtTripSignature signature) {
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
        buffer.writeDouble(signature.symmetry());
        buffer.writeDouble(signature.tunnelSpeed());
        buffer.writeDouble(signature.mandalaDensity());
        buffer.writeDouble(signature.glyphDensity());
        buffer.writeDouble(signature.meltBias());
        buffer.writeDouble(signature.portalBias());
        buffer.writeDouble(signature.colorStrobe());
        buffer.writeDouble(signature.entityHint());
        buffer.writeInt(signature.hyperspaceStyle());
        buffer.writeInt(signature.paletteStyle());
    }

    private static DmtTripSignature decodeSignature(FriendlyByteBuf buffer) {
        return new DmtTripSignature(
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
