package com.trippycraft.economy;

public final class MarketMath {
    public static final int MIN_DEMAND = 80;
    public static final int MAX_DEMAND = 150;

    private MarketMath() {
    }

    public static int reputationLevel(int reputation) {
        return Math.min(20, Math.max(0, reputation) / 250);
    }

    public static int priceHundredths(int baseEmeraldValue, int demandMultiplier, int reputationLevel) {
        int safeDemand = Math.clamp(demandMultiplier, MIN_DEMAND, MAX_DEMAND);
        int reputationMultiplier = 100 + Math.min(15, Math.max(0, reputationLevel)) * 2;
        return Math.max(25, baseEmeraldValue * safeDemand * reputationMultiplier / 100);
    }

    public static int emeraldsFor(int priceHundredths, int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        return Math.max(1, (priceHundredths * quantity + 99) / 100);
    }
}
