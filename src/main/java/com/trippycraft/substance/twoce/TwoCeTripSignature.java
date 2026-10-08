package com.trippycraft.substance.twoce;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public record TwoCeTripSignature(long seed, double cavityBias, double lockBias, double recursionBias,
        double corridorBias, double symmetryBias, double trailBias) {
    public static TwoCeTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0x2CE2CE2CE2CEL);
        return new TwoCeTripSignature(seed,
                0.62D + random.nextDouble() * 0.32D,
                0.50D + random.nextDouble() * 0.40D,
                0.42D + random.nextDouble() * 0.48D,
                0.38D + random.nextDouble() * 0.48D,
                0.30D + random.nextDouble() * 0.50D,
                0.40D + random.nextDouble() * 0.40D);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seed", seed);
        tag.putDouble("cavityBias", cavityBias);
        tag.putDouble("lockBias", lockBias);
        tag.putDouble("recursionBias", recursionBias);
        tag.putDouble("corridorBias", corridorBias);
        tag.putDouble("symmetryBias", symmetryBias);
        tag.putDouble("trailBias", trailBias);
        return tag;
    }

    public static TwoCeTripSignature load(CompoundTag tag) {
        TwoCeTripSignature fallback = create(NbtCompat.getLongOr(tag, "seed", 0x2CEL));
        return new TwoCeTripSignature(fallback.seed(),
                metric(tag, "cavityBias", fallback.cavityBias),
                metric(tag, "lockBias", fallback.lockBias),
                metric(tag, "recursionBias", fallback.recursionBias),
                metric(tag, "corridorBias", fallback.corridorBias),
                metric(tag, "symmetryBias", fallback.symmetryBias),
                metric(tag, "trailBias", fallback.trailBias));
    }

    private static double metric(CompoundTag tag, String key, double fallback) {
        return TwoCeDoseCurve.clamp(NbtCompat.getDoubleOr(tag, key, fallback), 0.0D, 1.0D);
    }
}
