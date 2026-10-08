package com.trippycraft.substance.ketamine;

import net.minecraft.network.FriendlyByteBuf;

public record KetamineEffectState(
        boolean active,
        double displayDoseMilligrams,
        double activeDoseMilligrams,
        double targetDoseMilligrams,
        int ageTicks,
        int durationTicks,
        int remainingTicks,
        KetaminePhase phase,
        double progress,
        int tier,
        double intensity,
        double dissociation,
        double timeSlip,
        double motorDisconnect,
        double bodyFloat,
        double cameraLag,
        double depthMismatch,
        double audioMuffle,
        double gravityDrift,
        double inputLatency,
        double painSuppression,
        double uiFade,
        double bodyEcho,
        double collapseRisk,
        double collapseDepth
) {
    public static KetamineEffectState inactive() {
        return new KetamineEffectState(
                false,
                0.0D,
                0.0D,
                0.0D,
                0,
                0,
                0,
                KetaminePhase.SOBER,
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
        buffer.writeDouble(dissociation);
        buffer.writeDouble(timeSlip);
        buffer.writeDouble(motorDisconnect);
        buffer.writeDouble(bodyFloat);
        buffer.writeDouble(cameraLag);
        buffer.writeDouble(depthMismatch);
        buffer.writeDouble(audioMuffle);
        buffer.writeDouble(gravityDrift);
        buffer.writeDouble(inputLatency);
        buffer.writeDouble(painSuppression);
        buffer.writeDouble(uiFade);
        buffer.writeDouble(bodyEcho);
        buffer.writeDouble(collapseRisk);
        buffer.writeDouble(collapseDepth);
    }

    public static KetamineEffectState decode(FriendlyByteBuf buffer) {
        return new KetamineEffectState(
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readEnum(KetaminePhase.class),
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
                buffer.readDouble()
        );
    }
}