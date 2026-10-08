package com.trippycraft.client;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;
import java.util.Map;

public final class ClientEffectState {
    private final EnumMap<ExperienceChannel, Double> current = new EnumMap<>(ExperienceChannel.class);
    private final EnumMap<ExperienceChannel, Double> previous = new EnumMap<>(ExperienceChannel.class);
    private final EnumMap<SubstanceFamily, Double> currentFamilies = new EnumMap<>(SubstanceFamily.class);
    private final EnumMap<SubstanceFamily, Double> previousFamilies = new EnumMap<>(SubstanceFamily.class);

    public ClientEffectState() {
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            current.put(channel, 0.0D);
            previous.put(channel, 0.0D);
        }
        for (SubstanceFamily family : SubstanceFamily.values()) {
            currentFamilies.put(family, 0.0D);
            previousFamilies.put(family, 0.0D);
        }
    }

    public void acceptServerSnapshot(Map<ExperienceChannel, Double> snapshot, Map<SubstanceFamily, Double> familyBlend) {
        previous.clear();
        previous.putAll(current);
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            current.put(channel, snapshot.getOrDefault(channel, 0.0D));
        }
        previousFamilies.clear();
        previousFamilies.putAll(currentFamilies);
        for (SubstanceFamily family : SubstanceFamily.values()) {
            currentFamilies.put(family, familyBlend.getOrDefault(family, 0.0D));
        }
    }

    public void clear() {
        previous.clear();
        current.clear();
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            previous.put(channel, 0.0D);
            current.put(channel, 0.0D);
        }
        previousFamilies.clear();
        currentFamilies.clear();
        for (SubstanceFamily family : SubstanceFamily.values()) {
            previousFamilies.put(family, 0.0D);
            currentFamilies.put(family, 0.0D);
        }
    }

    public double sample(ExperienceChannel channel, float partialTick) {
        double from = previous.getOrDefault(channel, 0.0D);
        double to = current.getOrDefault(channel, 0.0D);
        double t = Math.max(0.0D, Math.min(1.0D, partialTick));
        return from + (to - from) * t;
    }

    public ClientFamilyBlend sampleFamilyBlend(float partialTick) {
        EnumMap<SubstanceFamily, Double> sampled = new EnumMap<>(SubstanceFamily.class);
        double t = Math.max(0.0D, Math.min(1.0D, partialTick));
        for (SubstanceFamily family : SubstanceFamily.values()) {
            double from = previousFamilies.getOrDefault(family, 0.0D);
            double to = currentFamilies.getOrDefault(family, 0.0D);
            sampled.put(family, from + (to - from) * t);
        }
        return ClientFamilyBlend.from(sampled);
    }
}
