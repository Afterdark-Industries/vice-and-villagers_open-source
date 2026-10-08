package com.trippycraft.substance.allad;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public record AlLadTripSignature(long seed, double jewelBias, double leadBias, double prismBias,
        double recursionBias, double paneRhythm, double musicBias) {
    public static AlLadTripSignature create(long seed) {
        RandomSource random = RandomSource.create(seed ^ 0xA11AD5A11AD5L);
        return new AlLadTripSignature(seed,
                0.78D + random.nextDouble() * 0.22D,
                0.42D + random.nextDouble() * 0.40D,
                0.55D + random.nextDouble() * 0.40D,
                0.38D + random.nextDouble() * 0.50D,
                0.86D + random.nextDouble() * 0.20D,
                0.28D + random.nextDouble() * 0.48D);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("seed", seed);
        tag.putDouble("jewelBias", jewelBias);
        tag.putDouble("leadBias", leadBias);
        tag.putDouble("prismBias", prismBias);
        tag.putDouble("recursionBias", recursionBias);
        tag.putDouble("paneRhythm", paneRhythm);
        tag.putDouble("musicBias", musicBias);
        return tag;
    }

    public static AlLadTripSignature load(CompoundTag tag) {
        AlLadTripSignature fallback = create(NbtCompat.getLongOr(tag, "seed", 0xA11AD5L));
        return new AlLadTripSignature(fallback.seed(),
                metric(tag, "jewelBias", fallback.jewelBias),
                metric(tag, "leadBias", fallback.leadBias),
                metric(tag, "prismBias", fallback.prismBias),
                metric(tag, "recursionBias", fallback.recursionBias),
                AlLadDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "paneRhythm", fallback.paneRhythm), 0.84D, 1.12D),
                metric(tag, "musicBias", fallback.musicBias));
    }

    private static double metric(CompoundTag tag, String key, double fallback) {
        return AlLadDoseCurve.clamp(NbtCompat.getDoubleOr(tag, key, fallback), 0.0D, 1.0D);
    }
}
