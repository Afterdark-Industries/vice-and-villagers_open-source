package com.trippycraft.substance.lsd;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public record LsdTripSignature(
        long seed,
        int primaryRed,
        int primaryGreen,
        int primaryBlue,
        int secondaryRed,
        int secondaryGreen,
        int secondaryBlue,
        int accentRed,
        int accentGreen,
        int accentBlue,
        double electricSharpness,
        double geometricPrecision,
        double skyVortexBias,
        double blockAutonomyBias,
        double liquidBias,
        double meltFrequency,
        double tracerStrength,
        double entityEchoStrength,
        double prismSplit,
        int dominantDistortion,
        int skyStyle
) {
    public static LsdTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0x51D5D51D5D51D5DL);
        int palette = random.nextInt(9);
        int[][] palettes = new int[][] {
                {0x00F0FF, 0xFF33F8, 0xF6FF00},
                {0x39FF14, 0xB000FF, 0xFF6A00},
                {0x00C8FF, 0xFF007A, 0x8DFFEA},
                {0xFF3DF2, 0x39FF88, 0xFFB000},
                {0x7A5CFF, 0xFFEA00, 0x00FFB8},
                {0xFF1744, 0x00E5FF, 0xD6FF00},
                {0xFF7A00, 0x00FF66, 0x7C4DFF},
                {0x00B8FF, 0xFF2D95, 0xB6FF3B},
                {0xE9FF70, 0x2BFFEA, 0xFF4FD8}
        };
        int primary = jitter(palettes[palette][0], random, 34);
        int secondary = jitter(palettes[palette][1], random, 34);
        int accent = jitter(palettes[palette][2], random, 34);
        return new LsdTripSignature(
                seed,
                primary >> 16 & 255,
                primary >> 8 & 255,
                primary & 255,
                secondary >> 16 & 255,
                secondary >> 8 & 255,
                secondary & 255,
                accent >> 16 & 255,
                accent >> 8 & 255,
                accent & 255,
                0.55D + random.nextDouble() * 0.45D,
                0.45D + random.nextDouble() * 0.55D,
                0.20D + random.nextDouble() * 0.80D,
                0.20D + random.nextDouble() * 0.80D,
                0.18D + random.nextDouble() * 0.82D,
                0.20D + random.nextDouble() * 0.80D,
                0.35D + random.nextDouble() * 0.65D,
                0.25D + random.nextDouble() * 0.75D,
                0.30D + random.nextDouble() * 0.70D,
                random.nextInt(6),
                random.nextInt(5)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seed", seed);
        tag.putInt("primaryRed", primaryRed);
        tag.putInt("primaryGreen", primaryGreen);
        tag.putInt("primaryBlue", primaryBlue);
        tag.putInt("secondaryRed", secondaryRed);
        tag.putInt("secondaryGreen", secondaryGreen);
        tag.putInt("secondaryBlue", secondaryBlue);
        tag.putInt("accentRed", accentRed);
        tag.putInt("accentGreen", accentGreen);
        tag.putInt("accentBlue", accentBlue);
        tag.putDouble("electricSharpness", electricSharpness);
        tag.putDouble("geometricPrecision", geometricPrecision);
        tag.putDouble("skyVortexBias", skyVortexBias);
        tag.putDouble("blockAutonomyBias", blockAutonomyBias);
        tag.putDouble("liquidBias", liquidBias);
        tag.putDouble("meltFrequency", meltFrequency);
        tag.putDouble("tracerStrength", tracerStrength);
        tag.putDouble("entityEchoStrength", entityEchoStrength);
        tag.putDouble("prismSplit", prismSplit);
        tag.putInt("dominantDistortion", dominantDistortion);
        tag.putInt("skyStyle", skyStyle);
        return tag;
    }

    public static LsdTripSignature load(CompoundTag tag) {
        return new LsdTripSignature(
                NbtCompat.getLongOr(tag, "seed", 0x15D15D15DL),
                NbtCompat.getIntOr(tag, "primaryRed", 0),
                NbtCompat.getIntOr(tag, "primaryGreen", 240),
                NbtCompat.getIntOr(tag, "primaryBlue", 255),
                NbtCompat.getIntOr(tag, "secondaryRed", 255),
                NbtCompat.getIntOr(tag, "secondaryGreen", 51),
                NbtCompat.getIntOr(tag, "secondaryBlue", 248),
                NbtCompat.getIntOr(tag, "accentRed", 246),
                NbtCompat.getIntOr(tag, "accentGreen", 255),
                NbtCompat.getIntOr(tag, "accentBlue", 0),
                NbtCompat.getDoubleOr(tag, "electricSharpness", 0.7D),
                NbtCompat.getDoubleOr(tag, "geometricPrecision", 0.7D),
                NbtCompat.getDoubleOr(tag, "skyVortexBias", 0.5D),
                NbtCompat.getDoubleOr(tag, "blockAutonomyBias", 0.5D),
                NbtCompat.getDoubleOr(tag, "liquidBias", 0.5D),
                NbtCompat.getDoubleOr(tag, "meltFrequency", 0.5D),
                NbtCompat.getDoubleOr(tag, "tracerStrength", 0.5D),
                NbtCompat.getDoubleOr(tag, "entityEchoStrength", 0.5D),
                NbtCompat.getDoubleOr(tag, "prismSplit", 0.5D),
                NbtCompat.getIntOr(tag, "dominantDistortion", 0),
                NbtCompat.getIntOr(tag, "skyStyle", 0)
        );
    }

    public String debugName() {
        return "lsd-" + Long.toUnsignedString(seed, 16).substring(0, Math.min(6, Long.toUnsignedString(seed, 16).length()));
    }

    private static int jitter(int color, RandomSource random, int amount) {
        int r = clamp(((color >> 16) & 255) + random.nextInt(amount * 2 + 1) - amount);
        int g = clamp(((color >> 8) & 255) + random.nextInt(amount * 2 + 1) - amount);
        int b = clamp((color & 255) + random.nextInt(amount * 2 + 1) - amount);
        return r << 16 | g << 8 | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
