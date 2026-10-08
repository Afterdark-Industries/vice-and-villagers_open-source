package com.trippycraft.substance.dmt;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public record DmtTripSignature(
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
        double symmetry,
        double tunnelSpeed,
        double mandalaDensity,
        double glyphDensity,
        double meltBias,
        double portalBias,
        double colorStrobe,
        double entityHint,
        int hyperspaceStyle,
        int paletteStyle
) {
    public static DmtTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0xD17D17D17D17D17L);
        int palette = random.nextInt(8);
        int[][] palettes = new int[][] {
                {0x00F6FF, 0xFF1FD8, 0xFFF02D},
                {0xFFB347, 0xFF2D55, 0x2B65FF},
                {0x39FF14, 0xB000FF, 0xFFE85C},
                {0x00FFE0, 0x285CFF, 0xFF38F4},
                {0xFF7A00, 0x00F0FF, 0x8DFF70},
                {0xF8E7FF, 0x9C6CFF, 0xFFF6A0},
                {0xFF4D9D, 0xFFD166, 0x1DD3B0},
                {0x64FFDA, 0xFF00B8, 0xF6FF00}
        };
        int primary = jitter(palettes[palette][0], random, 40);
        int secondary = jitter(palettes[palette][1], random, 42);
        int accent = jitter(palettes[palette][2], random, 36);
        return new DmtTripSignature(
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
                0.40D + random.nextDouble() * 0.60D,
                0.45D + random.nextDouble() * 0.90D,
                0.38D + random.nextDouble() * 0.62D,
                0.26D + random.nextDouble() * 0.74D,
                0.18D + random.nextDouble() * 0.82D,
                0.35D + random.nextDouble() * 0.65D,
                0.30D + random.nextDouble() * 0.70D,
                0.16D + random.nextDouble() * 0.84D,
                random.nextInt(7),
                palette
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
        tag.putDouble("symmetry", symmetry);
        tag.putDouble("tunnelSpeed", tunnelSpeed);
        tag.putDouble("mandalaDensity", mandalaDensity);
        tag.putDouble("glyphDensity", glyphDensity);
        tag.putDouble("meltBias", meltBias);
        tag.putDouble("portalBias", portalBias);
        tag.putDouble("colorStrobe", colorStrobe);
        tag.putDouble("entityHint", entityHint);
        tag.putInt("hyperspaceStyle", hyperspaceStyle);
        tag.putInt("paletteStyle", paletteStyle);
        return tag;
    }

    public static DmtTripSignature load(CompoundTag tag) {
        return new DmtTripSignature(
                NbtCompat.getLongOr(tag, "seed", 0xD17D17D17L),
                NbtCompat.getIntOr(tag, "primaryRed", 0),
                NbtCompat.getIntOr(tag, "primaryGreen", 246),
                NbtCompat.getIntOr(tag, "primaryBlue", 255),
                NbtCompat.getIntOr(tag, "secondaryRed", 255),
                NbtCompat.getIntOr(tag, "secondaryGreen", 31),
                NbtCompat.getIntOr(tag, "secondaryBlue", 216),
                NbtCompat.getIntOr(tag, "accentRed", 255),
                NbtCompat.getIntOr(tag, "accentGreen", 240),
                NbtCompat.getIntOr(tag, "accentBlue", 45),
                NbtCompat.getDoubleOr(tag, "symmetry", 0.7D),
                NbtCompat.getDoubleOr(tag, "tunnelSpeed", 0.8D),
                NbtCompat.getDoubleOr(tag, "mandalaDensity", 0.6D),
                NbtCompat.getDoubleOr(tag, "glyphDensity", 0.5D),
                NbtCompat.getDoubleOr(tag, "meltBias", 0.5D),
                NbtCompat.getDoubleOr(tag, "portalBias", 0.6D),
                NbtCompat.getDoubleOr(tag, "colorStrobe", 0.6D),
                NbtCompat.getDoubleOr(tag, "entityHint", 0.4D),
                NbtCompat.getIntOr(tag, "hyperspaceStyle", 0),
                NbtCompat.getIntOr(tag, "paletteStyle", 0)
        );
    }

    public String debugName() {
        String id = Long.toUnsignedString(seed, 16);
        return "dmt-" + id.substring(0, Math.min(6, id.length()));
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
