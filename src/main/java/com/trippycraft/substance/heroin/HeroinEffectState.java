package com.trippycraft.substance.heroin;

import net.minecraft.network.FriendlyByteBuf;

public record HeroinEffectState(
        boolean active,
        double displayDoseMilligrams,
        double activeDoseMilligrams,
        double targetDoseMilligrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        HeroinPhase phase,
        double progress,
        int tier,
        double intensity,
        double warmth,
        double softFocus,
        double euphoria,
        double sedation,
        double painSuppression,
        double hungerSuppression,
        double regeneration,
        double resistance,
        double absorption,
        double motorDrag,
        double miningDrag,
        double nodding,
        double microSleep,
        double respiratorySuppression,
        double nauseaRelief,
        double appetiteSuppression
) {
    public static HeroinEffectState inactive() {
        return new HeroinEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                HeroinPhase.SOBER,
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
                0.0D
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayDoseMilligrams);
        buffer.writeDouble(activeDoseMilligrams);
        buffer.writeDouble(targetDoseMilligrams);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeInt(tier);
        buffer.writeDouble(intensity);
        buffer.writeDouble(warmth);
        buffer.writeDouble(softFocus);
        buffer.writeDouble(euphoria);
        buffer.writeDouble(sedation);
        buffer.writeDouble(painSuppression);
        buffer.writeDouble(hungerSuppression);
        buffer.writeDouble(regeneration);
        buffer.writeDouble(resistance);
        buffer.writeDouble(absorption);
        buffer.writeDouble(motorDrag);
        buffer.writeDouble(miningDrag);
        buffer.writeDouble(nodding);
        buffer.writeDouble(microSleep);
        buffer.writeDouble(respiratorySuppression);
        buffer.writeDouble(nauseaRelief);
        buffer.writeDouble(appetiteSuppression);
    }

    public static HeroinEffectState decode(FriendlyByteBuf buffer) {
        return new HeroinEffectState(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readEnum(HeroinPhase.class),
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
                buffer.readDouble()
        );
    }
}
