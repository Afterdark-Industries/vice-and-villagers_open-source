package com.trippycraft.substance.mdma;

import net.minecraft.network.FriendlyByteBuf;

public record MdmaEffectState(
        boolean active,
        double displayDoseMilligrams,
        double activeDoseMilligrams,
        double targetDoseMilligrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        boolean tripActive,
        int recoveryTicks,
        int recoveryDurationTicks,
        MdmaPhase phase,
        double progress,
        int tier,
        int repeatedDoses,
        double intensity,
        double warmth,
        double saturation,
        double bloom,
        double bassSensitivity,
        double fovBreathing,
        double movementLift,
        double hungerSuppression,
        double regeneration,
        double socialAura,
        double touchAffinity,
        double audioFullness,
        double euphoria,
        double comedownDesaturation,
        double comedownFatigue,
        double audioMuffle,
        double coordinationNoise,
        double dreaminess,
        double repeatedUsePenalty
) {
    public static MdmaEffectState inactive() {
        return new MdmaEffectState(
                false, 0.0D, 0.0D, 0.0D, 0, 0, 0, false, 0, 0, MdmaPhase.SOBER, 1.0D, 0, 0,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D
        );
    }

    public boolean peakWindow() {
        return tripActive && (phase == MdmaPhase.PEAK || phase == MdmaPhase.ROLL);
    }

    public boolean recoveryActive() {
        return active && !tripActive && recoveryTicks > 0;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeDouble(displayDoseMilligrams);
        buffer.writeDouble(activeDoseMilligrams);
        buffer.writeDouble(targetDoseMilligrams);
        buffer.writeInt(ageTicks);
        buffer.writeInt(durationTicks);
        buffer.writeInt(remainingTicks);
        buffer.writeBoolean(tripActive);
        buffer.writeInt(recoveryTicks);
        buffer.writeInt(recoveryDurationTicks);
        buffer.writeEnum(phase);
        buffer.writeDouble(progress);
        buffer.writeInt(tier);
        buffer.writeInt(repeatedDoses);
        buffer.writeDouble(intensity);
        buffer.writeDouble(warmth);
        buffer.writeDouble(saturation);
        buffer.writeDouble(bloom);
        buffer.writeDouble(bassSensitivity);
        buffer.writeDouble(fovBreathing);
        buffer.writeDouble(movementLift);
        buffer.writeDouble(hungerSuppression);
        buffer.writeDouble(regeneration);
        buffer.writeDouble(socialAura);
        buffer.writeDouble(touchAffinity);
        buffer.writeDouble(audioFullness);
        buffer.writeDouble(euphoria);
        buffer.writeDouble(comedownDesaturation);
        buffer.writeDouble(comedownFatigue);
        buffer.writeDouble(audioMuffle);
        buffer.writeDouble(coordinationNoise);
        buffer.writeDouble(dreaminess);
        buffer.writeDouble(repeatedUsePenalty);
    }

    public static MdmaEffectState decode(FriendlyByteBuf buffer) {
        return new MdmaEffectState(
                buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readInt(), buffer.readInt(), buffer.readEnum(MdmaPhase.class),
                buffer.readDouble(), buffer.readInt(), buffer.readInt(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble()
        );
    }
}
