package com.trippycraft.substance.mdma;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/** Persistent fictional tolerance and serotonin-depletion ledger for MDMA gameplay. */
public final class MdmaRecoveryTracker {
    public static final int MAX_TOLERANCE_PHASE = 15;
    public static final int MAX_DEPLETION_PHASE = 15;

    private static final int MINECRAFT_DAY_TICKS = 24_000;
    private static final int MIN_RESET_TICKS = MINECRAFT_DAY_TICKS;
    private static final int MAX_CONSECUTIVE_HIGH_DOSE_DAYS = 3;
    private static final int SLEEP_RECOVERY_TICKS = MINECRAFT_DAY_TICKS / 2;
    private static final double EXPERIENCE_THRESHOLD_MILLIGRAMS = 25.0D;
    private static final double DEPLETION_THRESHOLD_MILLIGRAMS = 100.0D;
    private static final double HIGH_DAILY_DOSE_MILLIGRAMS = 250.0D;

    private int tolerancePhase;
    private int depletionPhase;
    private double pendingToleranceGain;
    private double pendingDepletionGain;
    private int soberTicks;
    private double sessionPeakMilligrams;
    private boolean sessionActiveLastTick;
    private int withdrawalDeficitTicks;
    private double withdrawalSeverity;
    private int recoveryTicks;
    private int recoveryDurationTicks;
    private double recoverySeverity;
    private double recoveryPeakMilligrams;
    private long currentDoseDay = Long.MIN_VALUE;
    private double doseConsumedThisDay;
    private long lastHighDoseDay = Long.MIN_VALUE;
    private int consecutiveHighDoseDays;

    public void onDoseAdded(double doseMilligrams, long gameTime) {
        if (doseMilligrams > 0.0D) {
            soberTicks = 0;
            sessionActiveLastTick = true;
            recordDailyDose(doseMilligrams, gameTime);
        }
    }

    public void tick(ServerPlayer player, MdmaEffectState mdma) {
        if (!player.isAlive()) {
            resetAfterDeath();
            return;
        }

        boolean tripActive = mdma != null && mdma.tripActive();
        if (sessionActiveLastTick && !tripActive) {
            finishSession();
        }

        if (tripActive) {
            soberTicks = 0;
            sessionPeakMilligrams = Math.max(sessionPeakMilligrams,
                    Math.max(mdma.activeDoseMilligrams(), mdma.targetDoseMilligrams()));
        } else if (tolerancePhase > 0 || depletionPhase > 0) {
            soberTicks++;
            if (soberTicks >= resetTargetTicks()) {
                clearLongTermState();
            }
        } else {
            soberTicks = 0;
        }

        if (mdma != null && mdma.active()) {
            recoveryTicks = mdma.recoveryTicks();
            recoveryDurationTicks = Math.max(recoveryTicks, mdma.recoveryDurationTicks());
            recoverySeverity = Math.max(mdma.comedownFatigue(),
                    Math.max(mdma.comedownDesaturation(), mdma.audioMuffle()));
            recoveryPeakMilligrams = Math.max(recoveryPeakMilligrams,
                    Math.max(mdma.activeDoseMilligrams(), mdma.targetDoseMilligrams()));
        } else {
            recoveryTicks = 0;
            recoveryDurationTicks = 0;
            recoverySeverity = 0.0D;
        }

        tickWithdrawal(tripActive);
        sessionActiveLastTick = tripActive;
    }

    public void forceEndSession(MdmaEffectState mdma) {
        if (mdma != null && mdma.tripActive()) {
            sessionPeakMilligrams = Math.max(sessionPeakMilligrams,
                    Math.max(mdma.activeDoseMilligrams(), mdma.targetDoseMilligrams()));
        }
        if (sessionPeakMilligrams > 0.0D || sessionActiveLastTick) {
            finishSession();
        }
        sessionActiveLastTick = false;
    }

    public void onSleep() {
        if (tolerancePhase > 0 || depletionPhase > 0) {
            soberTicks = Math.min(resetTargetTicks(), soberTicks + SLEEP_RECOVERY_TICKS);
            if (soberTicks >= resetTargetTicks()) {
                clearLongTermState();
            }
        }
        withdrawalDeficitTicks = Math.max(0, withdrawalDeficitTicks - SLEEP_RECOVERY_TICKS);
        withdrawalSeverity *= 0.42D;
        if (withdrawalSeverity < 0.002D) {
            withdrawalSeverity = 0.0D;
        }
    }

    public void resetAfterDeath() {
        sessionPeakMilligrams = 0.0D;
        sessionActiveLastTick = false;
        recoveryTicks = 0;
        recoveryDurationTicks = 0;
        recoverySeverity = 0.0D;
        recoveryPeakMilligrams = 0.0D;
        clearLongTermState();
        currentDoseDay = Long.MIN_VALUE;
        doseConsumedThisDay = 0.0D;
        lastHighDoseDay = Long.MIN_VALUE;
        consecutiveHighDoseDays = 0;
    }

    public boolean isActive() {
        return tolerancePhase > 0
                || depletionPhase > 0
                || withdrawalSeverity > 0.001D
                || recoveryTicks > 0
                || recoverySeverity > 0.001D
                || sessionPeakMilligrams > 0.001D
                || sessionActiveLastTick;
    }

    public double toleranceProgress() {
        return saturate(tolerancePhase / (double) MAX_TOLERANCE_PHASE);
    }

    public double depletionProgress() {
        return saturate(depletionPhase / (double) MAX_DEPLETION_PHASE);
    }

    public double activeEffectMultiplier() {
        return Math.max(0.30D, 1.0D - toleranceProgress() * 0.58D - depletionProgress() * 0.12D);
    }

    public double overdoseToleranceProgress() {
        return toleranceProgress();
    }

    public double withdrawalSeverity() {
        return saturate(withdrawalSeverity);
    }

    public RecoveryState snapshot() {
        int target = tolerancePhase > 0 || depletionPhase > 0 ? resetTargetTicks() : MIN_RESET_TICKS;
        int remaining = tolerancePhase > 0 || depletionPhase > 0 ? Math.max(0, target - soberTicks) : 0;
        return new RecoveryState(
                isActive(),
                tolerancePhase,
                MAX_TOLERANCE_PHASE,
                depletionPhase,
                MAX_DEPLETION_PHASE,
                soberTicks,
                remaining,
                target,
                recoveryTicks,
                recoveryDurationTicks,
                saturate(recoverySeverity),
                recoveryPeakMilligrams,
                saturate(withdrawalSeverity),
                sessionActiveLastTick
        );
    }

    public String debugSummary() {
        if (!isActive()) {
            return "mdma_recovery=steady";
        }
        return String.format(Locale.ROOT,
                "mdma_recovery tol=%d/%d depletion=%d/%d high_days=%d hangover=%.2f withdrawal=%.2f reset=%ds",
                tolerancePhase, MAX_TOLERANCE_PHASE, depletionPhase, MAX_DEPLETION_PHASE,
                consecutiveHighDoseDays, recoverySeverity, withdrawalSeverity,
                Math.max(0, resetTargetTicks() - soberTicks) / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("tolerancePhase", tolerancePhase);
        tag.putInt("depletionPhase", depletionPhase);
        tag.putDouble("pendingToleranceGain", pendingToleranceGain);
        tag.putDouble("pendingDepletionGain", pendingDepletionGain);
        tag.putInt("soberTicks", soberTicks);
        tag.putDouble("sessionPeakMilligrams", sessionPeakMilligrams);
        tag.putBoolean("sessionActiveLastTick", sessionActiveLastTick);
        tag.putInt("withdrawalDeficitTicks", withdrawalDeficitTicks);
        tag.putDouble("withdrawalSeverity", withdrawalSeverity);
        tag.putInt("recoveryTicks", recoveryTicks);
        tag.putInt("recoveryDurationTicks", recoveryDurationTicks);
        tag.putDouble("recoverySeverity", recoverySeverity);
        tag.putDouble("recoveryPeakMilligrams", recoveryPeakMilligrams);
        tag.putLong("currentDoseDay", currentDoseDay);
        tag.putDouble("doseConsumedThisDay", doseConsumedThisDay);
        tag.putLong("lastHighDoseDay", lastHighDoseDay);
        tag.putInt("consecutiveHighDoseDays", consecutiveHighDoseDays);
        return tag;
    }

    public static MdmaRecoveryTracker load(CompoundTag tag) {
        MdmaRecoveryTracker tracker = new MdmaRecoveryTracker();
        tracker.tolerancePhase = clampInt(NbtCompat.getIntOr(tag, "tolerancePhase", 0), 0, MAX_TOLERANCE_PHASE);
        tracker.depletionPhase = clampInt(NbtCompat.getIntOr(tag, "depletionPhase", 0), 0, MAX_DEPLETION_PHASE);
        tracker.pendingToleranceGain = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "pendingToleranceGain", 0.0D));
        tracker.pendingDepletionGain = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "pendingDepletionGain", 0.0D));
        tracker.soberTicks = Math.max(0, NbtCompat.getIntOr(tag, "soberTicks", 0));
        tracker.sessionPeakMilligrams = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "sessionPeakMilligrams", 0.0D));
        tracker.sessionActiveLastTick = NbtCompat.getBooleanOr(tag, "sessionActiveLastTick", false);
        tracker.withdrawalDeficitTicks = Math.max(0, NbtCompat.getIntOr(tag, "withdrawalDeficitTicks", 0));
        tracker.withdrawalSeverity = saturate(NbtCompat.getDoubleOr(tag, "withdrawalSeverity", 0.0D));
        tracker.recoveryTicks = Math.max(0, NbtCompat.getIntOr(tag, "recoveryTicks", 0));
        tracker.recoveryDurationTicks = Math.max(tracker.recoveryTicks, NbtCompat.getIntOr(tag, "recoveryDurationTicks", tracker.recoveryTicks));
        tracker.recoverySeverity = saturate(NbtCompat.getDoubleOr(tag, "recoverySeverity", 0.0D));
        tracker.recoveryPeakMilligrams = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "recoveryPeakMilligrams", 0.0D));
        tracker.currentDoseDay = NbtCompat.getLongOr(tag, "currentDoseDay", Long.MIN_VALUE);
        tracker.doseConsumedThisDay = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "doseConsumedThisDay", 0.0D));
        tracker.lastHighDoseDay = NbtCompat.getLongOr(tag, "lastHighDoseDay", Long.MIN_VALUE);
        tracker.consecutiveHighDoseDays = clampInt(
                NbtCompat.getIntOr(tag, "consecutiveHighDoseDays", 0), 0, MAX_CONSECUTIVE_HIGH_DOSE_DAYS);
        return tracker;
    }

    private void finishSession() {
        addToleranceGain(toleranceGain(sessionPeakMilligrams));
        addDepletionGain(depletionGain(sessionPeakMilligrams));
        recoveryPeakMilligrams = Math.max(recoveryPeakMilligrams, sessionPeakMilligrams);
        sessionPeakMilligrams = 0.0D;
    }

    private void tickWithdrawal(boolean tripActive) {
        if (tripActive) {
            withdrawalDeficitTicks = Math.max(0, withdrawalDeficitTicks - 180);
            withdrawalSeverity += (0.0D - withdrawalSeverity) * 0.16D;
            if (withdrawalSeverity < 0.002D) {
                withdrawalSeverity = 0.0D;
            }
            return;
        }
        if (depletionPhase <= 0) {
            withdrawalDeficitTicks = 0;
            withdrawalSeverity = 0.0D;
            return;
        }

        withdrawalDeficitTicks++;
        double depletion = depletionProgress();
        double time = smoothstep(withdrawalDelayTicks(), withdrawalDelayTicks() + withdrawalRampTicks(), withdrawalDeficitTicks);
        double hangoverBridge = recoveryTicks > 0 ? 0.24D + recoverySeverity * 0.34D : 0.0D;
        double target = saturate(depletion * (0.28D + time * 0.72D) + hangoverBridge);
        withdrawalSeverity += (target - withdrawalSeverity) * (target > withdrawalSeverity ? 0.014D : 0.050D);
    }

    private int toleranceGain(double peakMilligrams) {
        if (peakMilligrams < EXPERIENCE_THRESHOLD_MILLIGRAMS) {
            return 0;
        }
        return clampInt(1 + (int) Math.floor((peakMilligrams - EXPERIENCE_THRESHOLD_MILLIGRAMS) / 125.0D), 1, 5);
    }

    private int depletionGain(double peakMilligrams) {
        if (peakMilligrams < DEPLETION_THRESHOLD_MILLIGRAMS) {
            return 0;
        }
        return clampInt(1 + (int) Math.floor((peakMilligrams - DEPLETION_THRESHOLD_MILLIGRAMS) / 150.0D), 1, 5);
    }

    private void addToleranceGain(double gain) {
        if (gain <= 0.0D || tolerancePhase >= MAX_TOLERANCE_PHASE) {
            return;
        }
        pendingToleranceGain += gain;
        int applied = (int) Math.floor(pendingToleranceGain);
        tolerancePhase = Math.min(MAX_TOLERANCE_PHASE, tolerancePhase + applied);
        pendingToleranceGain -= applied;
    }

    private void addDepletionGain(double gain) {
        if (gain <= 0.0D || depletionPhase >= MAX_DEPLETION_PHASE) {
            return;
        }
        pendingDepletionGain += gain;
        int applied = (int) Math.floor(pendingDepletionGain);
        depletionPhase = Math.min(MAX_DEPLETION_PHASE, depletionPhase + applied);
        pendingDepletionGain -= applied;
    }

    private int resetTargetTicks() {
        int recoveryDays = clampInt(consecutiveHighDoseDays, 1, MAX_CONSECUTIVE_HIGH_DOSE_DAYS);
        return recoveryDays * MINECRAFT_DAY_TICKS;
    }

    private void recordDailyDose(double doseMilligrams, long gameTime) {
        if (gameTime == Long.MIN_VALUE) {
            return;
        }
        long day = Math.floorDiv(gameTime, MINECRAFT_DAY_TICKS);
        if (day != currentDoseDay) {
            if (lastHighDoseDay != Long.MIN_VALUE && day > lastHighDoseDay + 1L) {
                consecutiveHighDoseDays = 0;
            }
            currentDoseDay = day;
            doseConsumedThisDay = 0.0D;
        }
        doseConsumedThisDay += Math.max(0.0D, doseMilligrams);
        if (doseConsumedThisDay <= HIGH_DAILY_DOSE_MILLIGRAMS || lastHighDoseDay == day) {
            return;
        }
        consecutiveHighDoseDays = lastHighDoseDay == day - 1L
                ? Math.min(MAX_CONSECUTIVE_HIGH_DOSE_DAYS, consecutiveHighDoseDays + 1)
                : 1;
        lastHighDoseDay = day;
    }

    private int withdrawalDelayTicks() {
        return (int) Math.round(mix(2.0D * 60.0D * 20.0D, 30.0D * 20.0D, depletionProgress()));
    }

    private int withdrawalRampTicks() {
        return (int) Math.round(mix(5.0D * 60.0D * 20.0D, 90.0D * 20.0D, depletionProgress()));
    }

    private void clearLongTermState() {
        tolerancePhase = 0;
        depletionPhase = 0;
        pendingToleranceGain = 0.0D;
        pendingDepletionGain = 0.0D;
        soberTicks = 0;
        withdrawalDeficitTicks = 0;
        withdrawalSeverity = 0.0D;
        recoveryPeakMilligrams = 0.0D;
    }

    private static double mix(double from, double to, double amount) {
        return from + (to - from) * saturate(amount);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = saturate((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record RecoveryState(
            boolean active,
            int tolerancePhase,
            int maxTolerancePhase,
            int depletionPhase,
            int maxDepletionPhase,
            int soberTicks,
            int resetTicksRemaining,
            int resetTargetTicks,
            int recoveryTicks,
            int recoveryDurationTicks,
            double recoverySeverity,
            double recoveryPeakMilligrams,
            double withdrawalSeverity,
            boolean suppressed
    ) {
        public boolean hangoverActive() {
            return !suppressed && recoveryTicks > 0 && recoverySeverity > 0.01D;
        }

        public boolean withdrawalActive() {
            return !suppressed && withdrawalSeverity > 0.01D;
        }

        public double toleranceProgress() {
            return maxTolerancePhase <= 0 ? 0.0D : saturate(tolerancePhase / (double) maxTolerancePhase);
        }

        public double depletionProgress() {
            return maxDepletionPhase <= 0 ? 0.0D : saturate(depletionPhase / (double) maxDepletionPhase);
        }

        public double discomfort() {
            return Math.max(recoverySeverity, withdrawalSeverity);
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(active);
            buffer.writeInt(tolerancePhase);
            buffer.writeInt(maxTolerancePhase);
            buffer.writeInt(depletionPhase);
            buffer.writeInt(maxDepletionPhase);
            buffer.writeInt(soberTicks);
            buffer.writeInt(resetTicksRemaining);
            buffer.writeInt(resetTargetTicks);
            buffer.writeInt(recoveryTicks);
            buffer.writeInt(recoveryDurationTicks);
            buffer.writeDouble(recoverySeverity);
            buffer.writeDouble(recoveryPeakMilligrams);
            buffer.writeDouble(withdrawalSeverity);
            buffer.writeBoolean(suppressed);
        }

        public static RecoveryState decode(FriendlyByteBuf buffer) {
            return new RecoveryState(
                    buffer.readBoolean(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean()
            );
        }

        public static RecoveryState inactive() {
            return new RecoveryState(false, 0, MAX_TOLERANCE_PHASE, 0, MAX_DEPLETION_PHASE,
                    0, 0, MIN_RESET_TICKS, 0, 0, 0.0D, 0.0D, 0.0D, false);
        }
    }
}
