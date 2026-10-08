package com.trippycraft.client;

import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ClientFamilyBlend {
    private static final double EPSILON = 0.0001D;

    private final EnumMap<SubstanceFamily, Double> strengths;
    private final double totalStrength;
    private final SubstanceFamily dominantFamily;
    private final double dominantStrength;

    private ClientFamilyBlend(Map<SubstanceFamily, Double> strengths) {
        this.strengths = new EnumMap<>(SubstanceFamily.class);
        double total = 0.0D;
        SubstanceFamily strongestFamily = null;
        double strongest = 0.0D;

        for (SubstanceFamily family : SubstanceFamily.values()) {
            double value = sanitize(strengths.getOrDefault(family, 0.0D));
            this.strengths.put(family, value);
            total += value;
            if (value > strongest) {
                strongest = value;
                strongestFamily = family;
            }
        }

        totalStrength = total;
        dominantFamily = strongestFamily;
        dominantStrength = strongest;
    }

    public static ClientFamilyBlend empty() {
        return from(Map.of());
    }

    public static ClientFamilyBlend from(Map<SubstanceFamily, Double> strengths) {
        return new ClientFamilyBlend(strengths);
    }

    public double strength(SubstanceFamily family) {
        return strengths.getOrDefault(family, 0.0D);
    }

    public double weight(SubstanceFamily family) {
        if (totalStrength <= EPSILON) {
            return 0.0D;
        }
        return strength(family) / totalStrength;
    }

    public double intensity() {
        return Math.max(0.0D, Math.min(1.0D, totalStrength));
    }

    public SubstanceFamily dominantFamily() {
        return dominantFamily;
    }

    public double dominantStrength() {
        return dominantStrength;
    }

    public String dominantLabel() {
        if (dominantFamily == null || dominantStrength <= EPSILON) {
            return "none";
        }
        return dominantFamily.name().toLowerCase(Locale.ROOT);
    }

    public Map<SubstanceFamily, Double> strengths() {
        return Map.copyOf(strengths);
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
