package com.trippycraft.substance.dmt;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ActiveDoseState;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;

public final class DmtEncounterDirector {
    private static final int MIN_EVENT_PERIOD_TICKS = 130;

    private final DmtEncounterTheme theme;
    private final long seed;

    private DmtEncounterDirector(DmtEncounterTheme theme, long seed) {
        this.theme = theme;
        this.seed = seed;
    }

    public static DmtEncounterDirector start(long seed, double doseUnits, DmtTripSignature signature, PsilocybinEnvironmentSnapshot environment) {
        long themeSeed = mix(seed ^ Double.doubleToLongBits(doseUnits), 0xC0D1F1EDBEEFL);
        double architecture = 1.0D + signature.symmetry() * 0.46D + environment.artificialBlocks() * 0.18D + environment.end() * 0.18D;
        double entity = 0.92D + signature.entityHint() * 0.58D + environment.entities() * 0.20D + environment.forest() * 0.12D;
        double symbol = 0.92D + signature.glyphDensity() * 0.64D + environment.light() * 0.12D;
        double consciousness = 0.82D + environment.organicBlocks() * 0.24D + environment.forest() * 0.20D + environment.night() * 0.10D;
        double roll = unit(themeSeed);
        DmtEncounterTheme chosen = weightedTheme(roll, architecture, entity, symbol, consciousness);
        return new DmtEncounterDirector(chosen, themeSeed);
    }

    public DmtEncounterState resolve(
            ActiveDoseState dose,
            DmtStage stage,
            DmtLayerProfile layers,
            DmtTripSignature signature,
            PsilocybinEnvironmentSnapshot environment
    ) {
        if (dose == null || !dose.active()) {
            return DmtEncounterState.inactive(theme);
        }
        double postGate = stage == DmtStage.POST_BREAKTHROUGH ? 1.0D
                : stage == DmtStage.BREAKTHROUGH_TRANSITION ? 0.35D
                : stage == DmtStage.RETURN ? 0.22D
                : 0.0D;
        double breakthroughGate = ActiveDoseState.smoothstep(
                TrippyConfig.dmtBreakthroughThresholdUnits * 0.90D,
                TrippyConfig.dmtBreakthroughThresholdUnits * 1.12D,
                dose.activeDose()
        );
        double themeStrength = ActiveDoseState.clamp(
                breakthroughGate
                        * postGate
                        * (0.28D + layers.realityReplacement() * 0.34D + layers.impossibleArchitecture() * 0.16D + layers.symbolLanguage() * 0.12D + layers.entityLayer() * 0.12D)
        );
        if (themeStrength <= 0.01D) {
            return DmtEncounterState.inactive(theme);
        }
        if (TrippyConfig.dmtEncounterFrequency <= 0.0D) {
            return new DmtEncounterState(
                    false,
                    theme,
                    DmtEncounterEventType.NONE,
                    themeStrength,
                    0.0D,
                    0.0D,
                    seed,
                    focusArchitecture(layers),
                    focusEntity(layers),
                    focusSymbol(layers),
                    focusConsciousness(layers, environment)
            );
        }

        int period = eventPeriodTicks();
        int postStart = Math.max(0, (int) Math.round(dose.durationTicks() * TrippyConfig.dmtPostBreakthroughStartProgress));
        int localAge = Math.max(0, dose.ageTicks() - postStart);
        int slot = localAge / period;
        int slotTick = localAge - slot * period;
        long slotSeed = mix(seed + slot * 0x9E3779B97F4A7C15L, signature.seed());
        double frequency = Math.max(0.0D, TrippyConfig.dmtEncounterFrequency);
        double chance = Math.min(0.90D, 0.14D + frequency * 0.22D + themeStrength * 0.18D + breakthroughGate * 0.12D);
        if (unit(slotSeed ^ 0x572D15A5L) > chance) {
            return new DmtEncounterState(
                    false,
                    theme,
                    DmtEncounterEventType.NONE,
                    themeStrength,
                    0.0D,
                    0.0D,
                    slotSeed,
                    focusArchitecture(layers),
                    focusEntity(layers),
                    focusSymbol(layers),
                    focusConsciousness(layers, environment)
            );
        }

        int window = Math.min(period - 24, Math.max(52, (int) Math.round(period * (0.28D + unit(slotSeed ^ 0x1248ABCDL) * 0.16D))));
        int start = Math.max(8, (int) Math.round((period - window - 12) * unit(slotSeed ^ 0xA11CE5EEDL)));
        double eventProgress = ActiveDoseState.clamp((slotTick - start) / (double) Math.max(1, window));
        double eventEnvelope = ActiveDoseState.smoothstep(start, start + window * 0.32D, slotTick)
                * (1.0D - ActiveDoseState.smoothstep(start + window * 0.70D, start + window, slotTick));
        double eventStrength = ActiveDoseState.clamp(eventEnvelope * themeStrength * (0.58D + unit(slotSeed ^ 0x91E10B5L) * 0.42D));
        DmtEncounterEventType eventType = eventStrength <= 0.01D ? DmtEncounterEventType.NONE : chooseEvent(slotSeed, theme, layers, environment);
        return new DmtEncounterState(
                eventStrength > 0.01D,
                theme,
                eventType,
                themeStrength,
                eventStrength,
                eventProgress,
                slotSeed,
                focusArchitecture(layers) * (eventType == DmtEncounterEventType.ASSEMBLY_ROOM || eventType == DmtEncounterEventType.INVITATION_PORTAL ? 1.18D : 1.0D),
                focusEntity(layers) * (eventType == DmtEncounterEventType.INSPECTION || eventType == DmtEncounterEventType.OBSERVER_SKY ? 1.24D : 1.0D),
                focusSymbol(layers) * (eventType == DmtEncounterEventType.REVEAL_HIDDEN_LAYER ? 1.22D : 1.0D),
                focusConsciousness(layers, environment) * (eventType == DmtEncounterEventType.MIRRORED_SELF ? 1.18D : 1.0D)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("theme", theme.name());
        tag.putLong("seed", seed);
        return tag;
    }

    public static DmtEncounterDirector load(CompoundTag tag) {
        DmtEncounterTheme theme = DmtEncounterTheme.HYPERDIMENSIONAL_CHAMBERS;
        try {
            theme = DmtEncounterTheme.valueOf(NbtCompat.getStringOr(tag, "theme", theme.name()));
        } catch (IllegalArgumentException ignored) {
            theme = DmtEncounterTheme.HYPERDIMENSIONAL_CHAMBERS;
        }
        return new DmtEncounterDirector(theme, NbtCompat.getLongOr(tag, "seed", 0xD17EC7EDL));
    }

    public DmtEncounterTheme theme() {
        return theme;
    }

    private static DmtEncounterTheme weightedTheme(double roll, double architecture, double entity, double symbol, double consciousness) {
        DmtEncounterTheme[] values = DmtEncounterTheme.values();
        double total = 0.0D;
        double[] weights = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            DmtEncounterTheme theme = values[i];
            double weight = theme.architectureBias() * architecture
                    + theme.entityBias() * entity
                    + theme.symbolBias() * symbol
                    + theme.consciousnessBias() * consciousness;
            weights[i] = Math.max(0.01D, weight);
            total += weights[i];
        }
        double cursor = roll * total;
        for (int i = 0; i < values.length; i++) {
            cursor -= weights[i];
            if (cursor <= 0.0D) {
                return values[i];
            }
        }
        return values[values.length - 1];
    }

    private static DmtEncounterEventType chooseEvent(long seed, DmtEncounterTheme theme, DmtLayerProfile layers, PsilocybinEnvironmentSnapshot environment) {
        double architecture = theme.architectureBias() + layers.impossibleArchitecture() + environment.artificialBlocks() * 0.24D;
        double entity = theme.entityBias() + layers.entityLayer() + environment.entities() * 0.22D;
        double symbol = theme.symbolBias() + layers.symbolLanguage() + environment.light() * 0.10D;
        double consciousness = theme.consciousnessBias() + layers.environmentalConsciousness() + environment.organicBlocks() * 0.18D;
        DmtEncounterEventType[] events = {
                DmtEncounterEventType.INSPECTION,
                DmtEncounterEventType.INVITATION_PORTAL,
                DmtEncounterEventType.REVEAL_HIDDEN_LAYER,
                DmtEncounterEventType.ASSEMBLY_ROOM,
                DmtEncounterEventType.MIRRORED_SELF,
                DmtEncounterEventType.OBSERVER_SKY
        };
        double[] weights = {
                entity * 1.20D,
                architecture * 0.82D + symbol * 0.28D,
                symbol * 1.16D + consciousness * 0.22D,
                architecture * 1.12D + symbol * 0.32D,
                consciousness * 1.00D + entity * 0.28D,
                entity * 0.64D + architecture * 0.38D + symbol * 0.20D
        };
        double total = 0.0D;
        for (double weight : weights) {
            total += Math.max(0.01D, weight);
        }
        double cursor = unit(seed ^ 0xE7717E5L) * total;
        for (int i = 0; i < events.length; i++) {
            cursor -= Math.max(0.01D, weights[i]);
            if (cursor <= 0.0D) {
                return events[i];
            }
        }
        return DmtEncounterEventType.INSPECTION;
    }

    private static int eventPeriodTicks() {
        double frequency = Math.max(0.05D, TrippyConfig.dmtEncounterFrequency);
        return Math.max(MIN_EVENT_PERIOD_TICKS, (int) Math.round(360.0D / frequency));
    }

    private static double focusArchitecture(DmtLayerProfile layers) {
        return ActiveDoseState.clamp(layers.impossibleArchitecture() * 0.58D + layers.realityReplacement() * 0.26D);
    }

    private static double focusEntity(DmtLayerProfile layers) {
        return ActiveDoseState.clamp(layers.entityLayer() * 0.66D + layers.breakthroughTransition() * 0.16D);
    }

    private static double focusSymbol(DmtLayerProfile layers) {
        return ActiveDoseState.clamp(layers.symbolLanguage() * 0.66D + layers.symbolPressure() * 0.24D);
    }

    private static double focusConsciousness(DmtLayerProfile layers, PsilocybinEnvironmentSnapshot environment) {
        return ActiveDoseState.clamp(layers.environmentalConsciousness() * 0.64D + environment.organicBlocks() * 0.18D + environment.entities() * 0.12D);
    }

    private static long mix(long value, long salt) {
        long z = value + salt + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double unit(long value) {
        long mixed = mix(value, 0xD1CEB00CL);
        return ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }
}
