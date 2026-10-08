package com.trippycraft.economy;

public record CustomerAddictionProfile(
        String id,
        int sampleGain,
        int purchaseGain,
        int quantityGain,
        int maxHabitLevel
) {
    public CustomerAddictionProfile {
        sampleGain = Math.max(0, sampleGain);
        purchaseGain = Math.max(0, purchaseGain);
        quantityGain = Math.max(0, quantityGain);
        maxHabitLevel = Math.max(0, Math.min(100, maxHabitLevel));
    }

    public int sampleGrowth(int currentLevel, int samples) {
        return dampenedGrowth(sampleGain, currentLevel, samples);
    }

    public int purchaseGrowth(int currentLevel, int purchases, int quantity) {
        int quantityBonus = quantity <= 1 ? 0 : log2(quantity) * quantityGain;
        return dampenedGrowth(purchaseGain + quantityBonus, currentLevel, purchases);
    }

    public int clampLevel(int level) {
        return Math.max(0, Math.min(maxHabitLevel, level));
    }

    private int dampenedGrowth(int rawGrowth, int currentLevel, int exposures) {
        if (rawGrowth <= 0 || currentLevel >= maxHabitLevel) {
            return 0;
        }

        int adjusted = rawGrowth;
        if (currentLevel >= 80) {
            adjusted = Math.max(1, (adjusted + 2) / 3);
        } else if (currentLevel >= 55) {
            adjusted = Math.max(1, (adjusted + 1) / 2);
        } else if (currentLevel >= 25) {
            adjusted = Math.max(1, adjusted * 2 / 3);
        }

        if (exposures > 10) {
            adjusted = Math.max(1, adjusted - (exposures - 10) / 8);
        }
        return Math.min(adjusted, maxHabitLevel - currentLevel);
    }

    private static int log2(int value) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(1, value));
    }
}
