package com.trippycraft.substance.cocaine;

import net.minecraft.network.FriendlyByteBuf;

public record CocaineEffectState(
        boolean active,
        double displayDoseGrams,
        double activeDoseGrams,
        double targetDoseGrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        CocainePhase phase,
        double progress,
        int tier,
        double intensity,
        double stimulation,
        double locomotorDrive,
        double inputSharpness,
        double jitter,
        double overcorrection,
        double compulsiveMomentum,
        double tunnelFocus,
        double clarityLift,
        double edgeShimmer,
        double lightHarshness,
        double microFlicker,
        double anxiety,
        double euphoria,
        double craving,
        double risk,
        double crashDrag,
        double crashDesaturation,
        double alcoholMask
) {
    public static CocaineEffectState inactive() {
        return new CocaineEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                CocainePhase.SOBER,
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
                0.0D
        );
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
        buffer.writeInt(tier);
        buffer.writeDouble(intensity);
        buffer.writeDouble(stimulation);
        buffer.writeDouble(locomotorDrive);
        buffer.writeDouble(inputSharpness);
        buffer.writeDouble(jitter);
        buffer.writeDouble(overcorrection);
        buffer.writeDouble(compulsiveMomentum);
        buffer.writeDouble(tunnelFocus);
        buffer.writeDouble(clarityLift);
        buffer.writeDouble(edgeShimmer);
        buffer.writeDouble(lightHarshness);
        buffer.writeDouble(microFlicker);
        buffer.writeDouble(anxiety);
        buffer.writeDouble(euphoria);
        buffer.writeDouble(craving);
        buffer.writeDouble(risk);
        buffer.writeDouble(crashDrag);
        buffer.writeDouble(crashDesaturation);
        buffer.writeDouble(alcoholMask);
    }

    public static CocaineEffectState decode(FriendlyByteBuf buffer) {
        return new CocaineEffectState(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readEnum(CocainePhase.class),
                buffer.readDouble(),
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