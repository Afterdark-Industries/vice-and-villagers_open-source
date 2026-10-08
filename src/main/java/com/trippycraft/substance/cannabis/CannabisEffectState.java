package com.trippycraft.substance.cannabis;

import net.minecraft.network.FriendlyByteBuf;

public record CannabisEffectState(
        boolean active,
        double displayThcMilligrams,
        double activeThcMilligrams,
        double targetThcMilligrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        CannabisPhase phase,
        double progress,
        double doseScale,
        double intensity,
        double immersion,
        double temporalDrag,
        double breathingPulse,
        double focusTunnel,
        double audioGlue,
        double thoughtStickiness,
        double warmthShift,
        double saturationBoost,
        double greenRichness
) {
    public static CannabisEffectState inactive() {
        return new CannabisEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                CannabisPhase.SOBER,
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
                0.0D
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayThcMilligrams);
        buffer.writeDouble(activeThcMilligrams);
        buffer.writeDouble(targetThcMilligrams);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeDouble(doseScale);
        buffer.writeDouble(intensity);
        buffer.writeDouble(immersion);
        buffer.writeDouble(temporalDrag);
        buffer.writeDouble(breathingPulse);
        buffer.writeDouble(focusTunnel);
        buffer.writeDouble(audioGlue);
        buffer.writeDouble(thoughtStickiness);
        buffer.writeDouble(warmthShift);
        buffer.writeDouble(saturationBoost);
        buffer.writeDouble(greenRichness);
    }

    public static CannabisEffectState decode(FriendlyByteBuf buffer) {
        return new CannabisEffectState(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readEnum(CannabisPhase.class),
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