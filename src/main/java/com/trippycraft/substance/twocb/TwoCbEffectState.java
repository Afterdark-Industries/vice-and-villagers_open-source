package com.trippycraft.substance.twocb;

import net.minecraft.network.FriendlyByteBuf;

public record TwoCbEffectState(
        boolean active,
        double displayDoseMilligrams,
        double activeDoseMilligrams,
        double targetDoseMilligrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        TwoCbPhase phase,
        double progress,
        int tier,
        int repeatedDoses,
        double intensity,
        double saturation,
        double edgeGlow,
        double surfaceGeometry,
        double tracerStrength,
        double bassSensitivity,
        double fisheyePulse,
        double symmetry,
        double crystalline,
        double sensuality,
        double stimulation,
        double anxiety,
        double settingMultiplier
) {
    public static TwoCbEffectState inactive() {
        return new TwoCbEffectState(
                false, 0.0D, 0.0D, 0.0D, 0, 0, 0, TwoCbPhase.SOBER, 1.0D, 0, 0,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D
        );
    }

    public TwoCbEffectState scaled(double multiplier) {
        double m = Math.max(0.0D, Math.min(1.0D, Double.isFinite(multiplier) ? multiplier : 0.0D));
        if (!active || m >= 0.9999D) return this;
        return new TwoCbEffectState(active, displayDoseMilligrams, activeDoseMilligrams, targetDoseMilligrams,
                ageTicks, durationTicks, remainingTicks, phase, progress, tier, repeatedDoses, intensity * m,
                saturation * m, edgeGlow * m, surfaceGeometry * m, tracerStrength * m, bassSensitivity * m,
                fisheyePulse * m, symmetry * m, crystalline * m, sensuality * m, stimulation * m,
                anxiety * m, settingMultiplier);
    }

    public boolean peakWindow() {
        return active && (phase == TwoCbPhase.PEAK || phase == TwoCbPhase.CRYSTALLINE);
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
        buffer.writeInt(repeatedDoses);
        buffer.writeDouble(intensity);
        buffer.writeDouble(saturation);
        buffer.writeDouble(edgeGlow);
        buffer.writeDouble(surfaceGeometry);
        buffer.writeDouble(tracerStrength);
        buffer.writeDouble(bassSensitivity);
        buffer.writeDouble(fisheyePulse);
        buffer.writeDouble(symmetry);
        buffer.writeDouble(crystalline);
        buffer.writeDouble(sensuality);
        buffer.writeDouble(stimulation);
        buffer.writeDouble(anxiety);
        buffer.writeDouble(settingMultiplier);
    }

    public static TwoCbEffectState decode(FriendlyByteBuf buffer) {
        return new TwoCbEffectState(
                buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readEnum(TwoCbPhase.class),
                buffer.readDouble(), buffer.readInt(), buffer.readInt(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
