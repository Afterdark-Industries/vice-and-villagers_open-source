package com.trippycraft.substance.dmt;

import net.minecraft.network.FriendlyByteBuf;

public record DmtEncounterState(
        boolean active,
        DmtEncounterTheme theme,
        DmtEncounterEventType eventType,
        double themeStrength,
        double eventStrength,
        double eventProgress,
        long eventSeed,
        double architectureFocus,
        double entityFocus,
        double symbolFocus,
        double consciousnessFocus
) {
    public static DmtEncounterState inactive(DmtEncounterTheme theme) {
        return new DmtEncounterState(
                false,
                theme == null ? DmtEncounterTheme.HYPERDIMENSIONAL_CHAMBERS : theme,
                DmtEncounterEventType.NONE,
                0.0D,
                0.0D,
                0.0D,
                0L,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeEnum(theme);
        buffer.writeEnum(eventType);
        buffer.writeDouble(themeStrength);
        buffer.writeDouble(eventStrength);
        buffer.writeDouble(eventProgress);
        buffer.writeLong(eventSeed);
        buffer.writeDouble(architectureFocus);
        buffer.writeDouble(entityFocus);
        buffer.writeDouble(symbolFocus);
        buffer.writeDouble(consciousnessFocus);
    }

    public static DmtEncounterState decode(FriendlyByteBuf buffer) {
        return new DmtEncounterState(
                buffer.readBoolean(),
                buffer.readEnum(DmtEncounterTheme.class),
                buffer.readEnum(DmtEncounterEventType.class),
                clamp(buffer.readDouble()),
                clamp(buffer.readDouble()),
                clamp(buffer.readDouble()),
                buffer.readLong(),
                clamp(buffer.readDouble()),
                clamp(buffer.readDouble()),
                clamp(buffer.readDouble()),
                clamp(buffer.readDouble())
        );
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
