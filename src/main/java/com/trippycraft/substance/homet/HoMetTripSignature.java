package com.trippycraft.substance.homet;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** One stable spatial rhythm survives redosing, save/load, and multiplayer snapshots. */
public record HoMetTripSignature(long seed, double depthBias, double recursionBias,
                                 double trailPersistence, double crystallineBias, double breathingRhythm) {
    public static HoMetTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0x484F4D455434L);
        return new HoMetTripSignature(seed, 0.82D + random.nextDouble() * 0.18D,
                0.80D + random.nextDouble() * 0.20D, 0.84D + random.nextDouble() * 0.16D,
                0.80D + random.nextDouble() * 0.20D, 0.84D + random.nextDouble() * 0.28D);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seed", seed); tag.putDouble("depthBias", depthBias); tag.putDouble("recursionBias", recursionBias);
        tag.putDouble("trailPersistence", trailPersistence); tag.putDouble("crystallineBias", crystallineBias);
        tag.putDouble("breathingRhythm", breathingRhythm);
        return tag;
    }

    public static HoMetTripSignature load(CompoundTag tag) {
        long seed = NbtCompat.getLongOr(tag, "seed", 0x484F4D455434L);
        HoMetTripSignature fallback = create(seed);
        return new HoMetTripSignature(seed, metric(tag, "depthBias", fallback.depthBias),
                metric(tag, "recursionBias", fallback.recursionBias), metric(tag, "trailPersistence", fallback.trailPersistence),
                metric(tag, "crystallineBias", fallback.crystallineBias),
                HoMetDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "breathingRhythm", fallback.breathingRhythm), 0.84D, 1.12D));
    }

    private static double metric(CompoundTag tag, String key, double fallback) {
        return HoMetDoseCurve.clamp(NbtCompat.getDoubleOr(tag, key, fallback), 0.0D, 1.0D);
    }
}
