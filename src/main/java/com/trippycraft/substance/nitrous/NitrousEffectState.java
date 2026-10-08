package com.trippycraft.substance.nitrous;

import net.minecraft.network.FriendlyByteBuf;

public record NitrousEffectState(boolean active, double displayHits, double activeHits, double targetHits,
        int ageTicks, int durationTicks, int remainingTicks, NitrousPhase phase, double progress, int tier,
        int repeatedHits, double intensity, double visualPresence, double tunnel, double fisheye, double haze,
        double bloom, double chroma, double echo, double stretch, double dissociation, double euphoria,
        double musicAppreciation, double motorNoise, double audioMuffle) {

    public static NitrousEffectState inactive() {
        return new NitrousEffectState(false, 0, 0, 0, 0, 0, 0, NitrousPhase.SOBER, 1, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public NitrousEffectState scaled(double multiplier) {
        double m = NitrousDoseCurve.clamp(multiplier, 0.0D, 100.0D);
        if (!active || Math.abs(m - 1.0D) < 0.0001D) {
            return this;
        }
        return new NitrousEffectState(active, displayHits, activeHits, targetHits, ageTicks, durationTicks,
                remainingTicks, phase, progress, tier, repeatedHits, intensity * m, visualPresence * m,
                tunnel * m, fisheye * m, haze * m, bloom * m, chroma * m, echo * m, stretch * m,
                dissociation * m, euphoria * m, musicAppreciation * m, motorNoise * m, audioMuffle * m);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBoolean(active);
        b.writeDouble(displayHits);
        b.writeDouble(activeHits);
        b.writeDouble(targetHits);
        b.writeInt(ageTicks);
        b.writeInt(durationTicks);
        b.writeInt(remainingTicks);
        b.writeEnum(phase);
        b.writeDouble(progress);
        b.writeInt(tier);
        b.writeInt(repeatedHits);
        b.writeDouble(intensity);
        b.writeDouble(visualPresence);
        b.writeDouble(tunnel);
        b.writeDouble(fisheye);
        b.writeDouble(haze);
        b.writeDouble(bloom);
        b.writeDouble(chroma);
        b.writeDouble(echo);
        b.writeDouble(stretch);
        b.writeDouble(dissociation);
        b.writeDouble(euphoria);
        b.writeDouble(musicAppreciation);
        b.writeDouble(motorNoise);
        b.writeDouble(audioMuffle);
    }

    public static NitrousEffectState decode(FriendlyByteBuf b) {
        return new NitrousEffectState(b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readInt(), b.readInt(), b.readInt(), b.readEnum(NitrousPhase.class), b.readDouble(), b.readInt(),
                b.readInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble());
    }
}
