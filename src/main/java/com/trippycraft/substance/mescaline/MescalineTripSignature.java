package com.trippycraft.substance.mescaline;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** Stable per-trip variation inside mescaline's fixed warm, mineral and botanical palette. */
public record MescalineTripSignature(
        long seed,
        double organicBias,
        double tessellationBias,
        double mandalaBias,
        double trailPersistence,
        double musicSensitivity,
        double breathingRhythm
) {
    public static MescalineTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0x6D657363616C696EL);
        return new MescalineTripSignature(
                seed,
                0.72D + random.nextDouble() * 0.28D,
                0.55D + random.nextDouble() * 0.45D,
                0.58D + random.nextDouble() * 0.42D,
                0.48D + random.nextDouble() * 0.52D,
                0.68D + random.nextDouble() * 0.32D,
                0.78D + random.nextDouble() * 0.36D
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seed", seed);
        tag.putDouble("organicBias", organicBias);
        tag.putDouble("tessellationBias", tessellationBias);
        tag.putDouble("mandalaBias", mandalaBias);
        tag.putDouble("trailPersistence", trailPersistence);
        tag.putDouble("musicSensitivity", musicSensitivity);
        tag.putDouble("breathingRhythm", breathingRhythm);
        return tag;
    }

    public static MescalineTripSignature load(CompoundTag tag) {
        return new MescalineTripSignature(
                NbtCompat.getLongOr(tag, "seed", 0x6D657363616C696EL),
                NbtCompat.getDoubleOr(tag, "organicBias", 0.86D),
                NbtCompat.getDoubleOr(tag, "tessellationBias", 0.72D),
                NbtCompat.getDoubleOr(tag, "mandalaBias", 0.78D),
                NbtCompat.getDoubleOr(tag, "trailPersistence", 0.70D),
                NbtCompat.getDoubleOr(tag, "musicSensitivity", 0.84D),
                NbtCompat.getDoubleOr(tag, "breathingRhythm", 0.96D)
        );
    }
}
