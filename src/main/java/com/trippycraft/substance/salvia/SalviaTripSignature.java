package com.trippycraft.substance.salvia;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public record SalviaTripSignature(
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
        double anchorBias,
        double bodyTorque,
        double recursionBias,
        double cutoutBias,
        double observerBias,
        double sceneBias,
        double aftershock
) {
    public static SalviaTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0x5A171A55EEDL);
        int[][] palettes = new int[][] {
                {0x6C7864, 0xB08A66, 0xD1C1A3},
                {0x4B645D, 0x90745E, 0xC6B49A},
                {0x536271, 0xA6845F, 0xD0C6A8},
                {0x5E5B6E, 0x9D7A61, 0xC8C0AD},
                {0x61735B, 0xA58B69, 0xD6CAA1}
        };
        int palette = random.nextInt(palettes.length);
        int primary = jitter(palettes[palette][0], random, 22);
        int secondary = jitter(palettes[palette][1], random, 22);
        int accent = jitter(palettes[palette][2], random, 18);
        return new SalviaTripSignature(
                seed,
                (primary >> 16) & 0xFF,
                (primary >> 8) & 0xFF,
                primary & 0xFF,
                (secondary >> 16) & 0xFF,
                (secondary >> 8) & 0xFF,
                secondary & 0xFF,
                (accent >> 16) & 0xFF,
                (accent >> 8) & 0xFF,
                accent & 0xFF,
                0.24D + random.nextDouble() * 0.76D,
                0.26D + random.nextDouble() * 0.74D,
                0.24D + random.nextDouble() * 0.76D,
                0.20D + random.nextDouble() * 0.80D,
                0.20D + random.nextDouble() * 0.80D,
                0.24D + random.nextDouble() * 0.76D,
                0.22D + random.nextDouble() * 0.78D
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
        tag.putDouble("anchorBias", anchorBias);
        tag.putDouble("bodyTorque", bodyTorque);
        tag.putDouble("recursionBias", recursionBias);
        tag.putDouble("cutoutBias", cutoutBias);
        tag.putDouble("observerBias", observerBias);
        tag.putDouble("sceneBias", sceneBias);
        tag.putDouble("aftershock", aftershock);
        return tag;
    }

    public static SalviaTripSignature load(CompoundTag tag) {
        return new SalviaTripSignature(
                NbtCompat.getLongOr(tag, "seed", 0x5A171A55EEDL),
                NbtCompat.getIntOr(tag, "primaryRed", 108),
                NbtCompat.getIntOr(tag, "primaryGreen", 120),
                NbtCompat.getIntOr(tag, "primaryBlue", 100),
                NbtCompat.getIntOr(tag, "secondaryRed", 176),
                NbtCompat.getIntOr(tag, "secondaryGreen", 138),
                NbtCompat.getIntOr(tag, "secondaryBlue", 102),
                NbtCompat.getIntOr(tag, "accentRed", 209),
                NbtCompat.getIntOr(tag, "accentGreen", 193),
                NbtCompat.getIntOr(tag, "accentBlue", 163),
                NbtCompat.getDoubleOr(tag, "anchorBias", 0.55D),
                NbtCompat.getDoubleOr(tag, "bodyTorque", 0.55D),
                NbtCompat.getDoubleOr(tag, "recursionBias", 0.55D),
                NbtCompat.getDoubleOr(tag, "cutoutBias", 0.50D),
                NbtCompat.getDoubleOr(tag, "observerBias", 0.50D),
                NbtCompat.getDoubleOr(tag, "sceneBias", 0.55D),
                NbtCompat.getDoubleOr(tag, "aftershock", 0.55D)
        );
    }

    public String debugName() {
        String hex = Long.toUnsignedString(seed, 16);
        return "salvia-" + hex.substring(0, Math.min(6, hex.length()));
    }

    private static int jitter(int color, RandomSource random, int amount) {
        int red = clamp(((color >> 16) & 0xFF) + random.nextInt(amount * 2 + 1) - amount);
        int green = clamp(((color >> 8) & 0xFF) + random.nextInt(amount * 2 + 1) - amount);
        int blue = clamp((color & 0xFF) + random.nextInt(amount * 2 + 1) - amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}