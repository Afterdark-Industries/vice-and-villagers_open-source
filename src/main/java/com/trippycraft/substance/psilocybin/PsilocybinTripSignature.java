package com.trippycraft.substance.psilocybin;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;
import java.util.Random;

public record PsilocybinTripSignature(
        long seed,
        PsilocybinTripVibe vibe,
        int primaryRed,
        int primaryGreen,
        int primaryBlue,
        int secondaryRed,
        int secondaryGreen,
        int secondaryBlue,
        double breathingRhythm,
        double waveRhythm,
        double hallucinationTendency,
        double particleStyle,
        double environmentalSensitivity,
        double emotionalTone,
        double geometrySoftness,
        double tracerPersistence,
        PsilocybinFungalPattern patternType
) {
    public static PsilocybinTripSignature create(long seed) {
        Random random = new Random(seed);
        PsilocybinTripVibe vibe = PsilocybinTripVibe.values()[random.nextInt(PsilocybinTripVibe.values().length)];
        PsilocybinFungalPattern pattern = PsilocybinFungalPattern.values()[random.nextInt(PsilocybinFungalPattern.values().length)];
        int[] primary = vibe.primary();
        int[] secondary = vibe.secondary();
        return new PsilocybinTripSignature(
                seed,
                vibe,
                jitter(primary[0], random), jitter(primary[1], random), jitter(primary[2], random),
                jitter(secondary[0], random), jitter(secondary[1], random), jitter(secondary[2], random),
                0.78D + random.nextDouble() * 0.42D,
                0.72D + random.nextDouble() * 0.36D,
                0.25D + random.nextDouble() * 0.68D,
                random.nextDouble(),
                0.45D + random.nextDouble() * 0.50D,
                -0.35D + random.nextDouble() * 0.90D,
                0.38D + random.nextDouble() * 0.54D,
                0.24D + random.nextDouble() * 0.62D,
                pattern
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seed", seed);
        tag.putString("vibe", vibe.name());
        tag.putInt("primaryRed", primaryRed);
        tag.putInt("primaryGreen", primaryGreen);
        tag.putInt("primaryBlue", primaryBlue);
        tag.putInt("secondaryRed", secondaryRed);
        tag.putInt("secondaryGreen", secondaryGreen);
        tag.putInt("secondaryBlue", secondaryBlue);
        tag.putDouble("breathingRhythm", breathingRhythm);
        tag.putDouble("waveRhythm", waveRhythm);
        tag.putDouble("hallucinationTendency", hallucinationTendency);
        tag.putDouble("particleStyle", particleStyle);
        tag.putDouble("environmentalSensitivity", environmentalSensitivity);
        tag.putDouble("emotionalTone", emotionalTone);
        tag.putDouble("geometrySoftness", geometrySoftness);
        tag.putDouble("tracerPersistence", tracerPersistence);
        tag.putString("patternType", patternType.name());
        return tag;
    }

    public static PsilocybinTripSignature load(CompoundTag tag) {
        long seed = NbtCompat.getLongOr(tag, "seed", 0L);
        if (seed == 0L) {
            return create(0x51F2EEDL);
        }
        return new PsilocybinTripSignature(
                seed,
                parseEnum(PsilocybinTripVibe.class, NbtCompat.getStringOr(tag, "vibe", ""), PsilocybinTripVibe.MOSSY_GOLDEN_FOREST),
                NbtCompat.getIntOr(tag, "primaryRed", 96),
                NbtCompat.getIntOr(tag, "primaryGreen", 150),
                NbtCompat.getIntOr(tag, "primaryBlue", 82),
                NbtCompat.getIntOr(tag, "secondaryRed", 183),
                NbtCompat.getIntOr(tag, "secondaryGreen", 139),
                NbtCompat.getIntOr(tag, "secondaryBlue", 72),
                NbtCompat.getDoubleOr(tag, "breathingRhythm", 0.95D),
                NbtCompat.getDoubleOr(tag, "waveRhythm", 0.86D),
                NbtCompat.getDoubleOr(tag, "hallucinationTendency", 0.45D),
                NbtCompat.getDoubleOr(tag, "particleStyle", 0.50D),
                NbtCompat.getDoubleOr(tag, "environmentalSensitivity", 0.70D),
                NbtCompat.getDoubleOr(tag, "emotionalTone", 0.20D),
                NbtCompat.getDoubleOr(tag, "geometrySoftness", 0.60D),
                NbtCompat.getDoubleOr(tag, "tracerPersistence", 0.42D),
                parseEnum(PsilocybinFungalPattern.class, NbtCompat.getStringOr(tag, "patternType", ""), PsilocybinFungalPattern.MYCELIUM_THREADS)
        );
    }

    public String debugName() {
        return vibe.name().toLowerCase(Locale.ROOT) + "/" + patternType.name().toLowerCase(Locale.ROOT);
    }

    private static int jitter(int value, Random random) {
        return Math.max(0, Math.min(255, value - 14 + random.nextInt(29)));
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public enum PsilocybinTripVibe {
        MOSSY_GOLDEN_FOREST(new int[]{82, 132, 70}, new int[]{212, 165, 78}),
        DEEP_BLUE_CAVE_MYCELIUM(new int[]{42, 86, 132}, new int[]{118, 190, 198}),
        WARM_SUNSET_HONEYCOMB(new int[]{216, 151, 74}, new int[]{196, 80, 126}),
        RAINY_GLASS_SPORE(new int[]{120, 166, 166}, new int[]{172, 190, 224}),
        PURPLE_FUNGAL_NIGHT(new int[]{90, 58, 136}, new int[]{182, 86, 166}),
        PALE_DREAMLIKE_MORNING(new int[]{178, 188, 152}, new int[]{222, 196, 142});

        private final int[] primary;
        private final int[] secondary;

        PsilocybinTripVibe(int[] primary, int[] secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        int[] primary() {
            return primary;
        }

        int[] secondary() {
            return secondary;
        }
    }

    public enum PsilocybinFungalPattern {
        MYCELIUM_THREADS,
        SPORE_RINGS,
        BARK_EYES,
        HONEYCOMB_GILLS,
        VEINED_MANDALA,
        VELVET_CRACKS
    }
}
