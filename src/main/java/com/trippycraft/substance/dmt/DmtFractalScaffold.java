package com.trippycraft.substance.dmt;

import net.minecraft.network.FriendlyByteBuf;

public record DmtFractalScaffold(
        double fractal3dPresence,
        double animatedFractalPresence,
        double staticAnimatedCoupling,
        double alternateWorldReadiness,
        double dimensionalDepth,
        double temporalFlow,
        double morphRate,
        double phase,
        long structureSeed,
        long animationSeed
) {
    public static DmtFractalScaffold empty() {
        return new DmtFractalScaffold(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0L, 0L);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(fractal3dPresence);
        buffer.writeDouble(animatedFractalPresence);
        buffer.writeDouble(staticAnimatedCoupling);
        buffer.writeDouble(alternateWorldReadiness);
        buffer.writeDouble(dimensionalDepth);
        buffer.writeDouble(temporalFlow);
        buffer.writeDouble(morphRate);
        buffer.writeDouble(phase);
        buffer.writeLong(structureSeed);
        buffer.writeLong(animationSeed);
    }

    public static DmtFractalScaffold decode(FriendlyByteBuf buffer) {
        return new DmtFractalScaffold(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readLong(),
                buffer.readLong()
        );
    }
}
