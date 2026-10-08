package com.trippycraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

/** A deterministic world clock keeps every tavern playlist advancing, loaded or not. */
public final class TavernPlaylist {
    public static final int TICKS_PER_SECOND = 20;

    // Durations match the nineteen normalized OGG streams in ModSounds order.
    private static final int[] TRACK_DURATIONS_TICKS = {
            4_362, 3_865, 3_547, 3_356, 3_030,
            3_474, 4_485, 3_383, 2_647, 3_572,
            3_594, 3_487, 5_394, 5_906, 3_963,
            4_868, 3_823, 5_562, 5_676
    };
    /*
     * Low-frequency onset grids measured from the shipped tavern masters. They
     * intentionally live beside the playlist clock so music playback, lights,
     * and MDMA screen pulses all use the same deterministic timeline.
     */
    private static final double[] TRACK_BASS_BPM = {
            160.0D, 160.0D, 160.0D, 160.0D, 142.349D,
            160.0D, 160.0D, 160.0D, 160.0D, 160.0D,
            160.0D, 160.0D, 160.0D, 160.0D, 160.0D,
            160.0D, 160.0D, 160.0D, 160.0D
    };
    private static final double[] TRACK_BASS_OFFSETS_TICKS = {
            1.0D, 5.5D, 5.0D, 1.0D, 3.76D,
            4.0D, 5.0D, 6.5D, 2.5D, 1.0D,
            0.0D, 6.0D, 3.5D, 2.48D, 6.5D,
            7.0D, 1.0D, 5.0D, 3.0D
    };
    private static final long TOTAL_CYCLE_TICKS = totalDuration();

    private TavernPlaylist() {
    }

    public static long timelineTick(long gameTime, BlockPos origin, Rotation rotation) {
        long identity = origin.asLong() ^ ((long) rotation.ordinal() * 0x9E3779B97F4A7C15L);
        long phase = Math.floorMod(mix64(identity), TOTAL_CYCLE_TICKS);
        return Math.floorMod(gameTime + phase, TOTAL_CYCLE_TICKS);
    }

    public static Selection selection(long timelineTick) {
        long remaining = Math.floorMod(timelineTick, TOTAL_CYCLE_TICKS);
        for (int index = 0; index < TRACK_DURATIONS_TICKS.length; index++) {
            int duration = TRACK_DURATIONS_TICKS[index];
            if (remaining < duration) {
                return new Selection(index, (int) remaining, duration);
            }
            remaining -= duration;
        }
        return new Selection(0, 0, TRACK_DURATIONS_TICKS[0]);
    }

    public static int trackCount() {
        return TRACK_DURATIONS_TICKS.length;
    }

    public static long totalCycleTicks() {
        return TOTAL_CYCLE_TICKS;
    }

    /** A sharp 0..1 pulse on each measured low-frequency hit in the active track. */
    public static double bassPulse(long timelineTick, double partialTick) {
        Selection selection = selection(timelineTick);
        int index = selection.trackIndex();
        double beatPeriodTicks = TICKS_PER_SECOND * 60.0D / TRACK_BASS_BPM[index];
        double trackTick = selection.elapsedTicks() + clamp(partialTick, 0.0D, 1.0D);
        double sinceGridStart = trackTick - TRACK_BASS_OFFSETS_TICKS[index];
        double phase = sinceGridStart - Math.floor(sinceGridStart / beatPeriodTicks) * beatPeriodTicks;
        double attackDecay = Math.exp(-phase * 0.82D);
        return clamp(attackDecay * attackDecay, 0.0D, 1.0D);
    }

    /** Slower musical phrases for visuals that should breathe with structure instead of kick hits. */
    public static Structure structure(long timelineTick, double partialTick) {
        Selection selection = selection(timelineTick);
        int index = selection.trackIndex();
        double beatPeriod = TICKS_PER_SECOND * 60.0D / TRACK_BASS_BPM[index];
        double trackTick = selection.elapsedTicks() + clamp(partialTick, 0.0D, 1.0D) - TRACK_BASS_OFFSETS_TICKS[index];
        double beat = trackTick / beatPeriod;
        double bar = beat / 4.0D;
        double phrase = bar / 8.0D;
        double phrasePhase = phrase - Math.floor(phrase);
        double bassExpansion = 0.5D + 0.5D * Math.sin(bar * Math.PI);
        bassExpansion = bassExpansion * bassExpansion * (3.0D - 2.0D * bassExpansion);
        double harmonicShift = 0.5D + 0.5D * Math.sin(phrase * Math.PI * 2.0D + index * 0.71D);
        double melodicFlow = 0.5D + 0.5D * Math.sin(bar * 0.73D + Math.sin(phrase * 1.9D) * 1.4D + index);
        double sustain = 0.5D + 0.5D * Math.sin(phrase * Math.PI + index * 0.43D);
        return new Structure(bassExpansion, harmonicShift, melodicFlow, sustain, phrasePhase);
    }

    private static long totalDuration() {
        long total = 0L;
        for (int duration : TRACK_DURATIONS_TICKS) {
            total += duration;
        }
        return total;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Selection(int trackIndex, int elapsedTicks, int durationTicks) {
    }

    public record Structure(double bassExpansion, double harmonicShift, double melodicFlow,
                            double sustain, double phrasePhase) {
    }
}
