package com.trippycraft.substance.alcohol;

import net.minecraft.network.FriendlyByteBuf;

public record AlcoholEffectState(
        boolean active,
        double displayStandardDrinks,
        double activeStandardDrinks,
        double targetStandardDrinks,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        AlcoholPhase phase,
        double progress,
        int tier,
        double intensity,
        double sedation,
        double motorNoise,
        double diplopia,
        double focusBlur,
        double vestibularSway,
        double peripheralNarrowing,
        double euphoria,
        double nausea,
        double disinhibition,
        double risk
) {
    public static AlcoholEffectState inactive() {
        return new AlcoholEffectState(false, 0.0D, 0.0D, 0.0D, 0, 0, 0, AlcoholPhase.SOBER, 1.0D, 0,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayStandardDrinks);
        buffer.writeDouble(activeStandardDrinks);
        buffer.writeDouble(targetStandardDrinks);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeInt(tier);
        buffer.writeDouble(intensity);
        buffer.writeDouble(sedation);
        buffer.writeDouble(motorNoise);
        buffer.writeDouble(diplopia);
        buffer.writeDouble(focusBlur);
        buffer.writeDouble(vestibularSway);
        buffer.writeDouble(peripheralNarrowing);
        buffer.writeDouble(euphoria);
        buffer.writeDouble(nausea);
        buffer.writeDouble(disinhibition);
        buffer.writeDouble(risk);
    }

    public static AlcoholEffectState decode(FriendlyByteBuf buffer) {
        return new AlcoholEffectState(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readEnum(AlcoholPhase.class),
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
                buffer.readDouble()
        );
    }
}
