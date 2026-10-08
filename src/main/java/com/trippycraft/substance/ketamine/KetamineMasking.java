package com.trippycraft.substance.ketamine;

public final class KetamineMasking {
    private KetamineMasking() {
    }

    public static double otherSubstanceMask(KetamineEffectState state) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        return otherSubstanceMask(doseMilligrams(state));
    }

    public static double otherSubstanceMultiplier(KetamineEffectState state) {
        return 1.0D - otherSubstanceMask(state);
    }

    public static double otherSubstanceMultiplier(double doseMilligrams) {
        return 1.0D - otherSubstanceMask(doseMilligrams);
    }

    public static double visualPresence(KetamineEffectState state) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        return visualPresence(doseMilligrams(state));
    }

    public static double visualPresence(double doseMilligrams) {
        if (!Double.isFinite(doseMilligrams) || doseMilligrams <= 0.0D) {
            return 0.0D;
        }
        if (doseMilligrams < 100.0D) {
            return smoothstep(70.0D, 100.0D, doseMilligrams) * 0.18D;
        }
        if (doseMilligrams < 200.0D) {
            return lerp(0.30D, 0.82D, smoothstep(100.0D, 200.0D, doseMilligrams));
        }
        if (doseMilligrams < 400.0D) {
            return lerp(0.82D, 1.0D, smoothstep(200.0D, 400.0D, doseMilligrams));
        }
        return 1.0D;
    }

    public static double realityBend(KetamineEffectState state) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        return realityBend(doseMilligrams(state));
    }

    public static double realityBend(double doseMilligrams) {
        return smoothstep(280.0D, 400.0D, doseMilligrams);
    }

    private static double otherSubstanceMask(double doseMilligrams) {
        if (!Double.isFinite(doseMilligrams) || doseMilligrams <= 0.0D) {
            return 0.0D;
        }
        if (doseMilligrams < 50.0D) {
            return smoothstep(0.0D, 50.0D, doseMilligrams) * 0.15D;
        }
        if (doseMilligrams < 100.0D) {
            return lerp(0.15D, 0.45D, smoothstep(50.0D, 100.0D, doseMilligrams));
        }
        if (doseMilligrams < 200.0D) {
            return lerp(0.45D, 0.75D, smoothstep(100.0D, 200.0D, doseMilligrams));
        }
        if (doseMilligrams < 400.0D) {
            return lerp(0.75D, 1.0D, smoothstep(200.0D, 400.0D, doseMilligrams));
        }
        return 1.0D;
    }

    private static double doseMilligrams(KetamineEffectState state) {
        return Math.max(state.activeDoseMilligrams(), state.targetDoseMilligrams());
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * clamp(t);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}