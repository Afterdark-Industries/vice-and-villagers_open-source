package com.trippycraft.substance.heroin;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class HeroinRecoveryTracker {
    public static final int MAX_TOLERANCE_PHASE = 15;
    public static final int MAX_DEPENDENCE_PHASE = 15;

    private static final int MINECRAFT_DAY_TICKS = 24000;
    private static final int BASE_RESET_AFTER_TICKS = 3 * MINECRAFT_DAY_TICKS;
    private static final int LATE_STAGE_RESET_AFTER_TICKS = 5 * MINECRAFT_DAY_TICKS;
    private static final int LATE_STAGE_DEPENDENCE_PHASE = 10;
    private static final double EXPERIENCE_THRESHOLD_MILLIGRAMS = 25.0D;
    private static final double CRASH_THRESHOLD_MILLIGRAMS = 100.0D;
    private static final int WITHDRAWAL_DAMAGE_INTERVAL_TICKS = 120;
    private static final int CRASH_DAMAGE_INTERVAL_TICKS = 14 * 20;
    private static final int SLEEP_RECOVERY_TICKS = MINECRAFT_DAY_TICKS / 2;
    private static final int MAX_CRASH_TICKS = 3 * MINECRAFT_DAY_TICKS;

    private int tolerancePhase;
    private int dependencePhase;
    private double pendingToleranceGain;
    private double pendingDependenceGain;
    private int soberTicks;
    private double sessionPeakMilligrams;
    private boolean sessionQualified;
    private boolean sessionActiveLastTick;
    private int overdoseChecksUpToMilligrams;
    private int crashTicks;
    private int crashDurationTicks;
    private double crashSeverity;
    private double crashPeakMilligrams;
    private int withdrawalDeficitTicks;
    private double withdrawalSeverity;
    private int nutritionPenaltyTicks;
    private int nutritionPenaltyDurationTicks;
    private double nutritionPenaltySeverity;
    private int crashDamageCooldownTicks;
    private int withdrawalDamageCooldownTicks;

    public void onDoseAdded(double doseMilligrams) {
        if (doseMilligrams > 0.0D) {
            soberTicks = 0;
        }
    }

    public void tick(ServerPlayer player, HeroinEffectState heroin, boolean cannabisActive) {
        if (!player.isAlive()) {
            resetAfterDeath();
            return;
        }

        if (crashDamageCooldownTicks > 0) {
            crashDamageCooldownTicks--;
        }
        if (withdrawalDamageCooldownTicks > 0) {
            withdrawalDamageCooldownTicks--;
        }

        if (sessionActiveLastTick && !heroin.active()) {
            finishSession();
        }

        double activeMilligrams = heroin.active() ? Math.max(heroin.activeDoseMilligrams(), heroin.displayDoseMilligrams()) : 0.0D;
        if (heroin.active()) {
            soberTicks = 0;
            sessionPeakMilligrams = Math.max(sessionPeakMilligrams, Math.max(activeMilligrams, heroin.targetDoseMilligrams()));
            sessionQualified = sessionQualified || sessionPeakMilligrams >= EXPERIENCE_THRESHOLD_MILLIGRAMS;
        } else if (dependencePhase > 0 || tolerancePhase > 0) {
            soberTicks++;
            if (soberTicks >= resetTargetTicks()) {
                clearLongTermState();
            }
        } else {
            soberTicks = 0;
        }

        tickCrash(player, activeMilligrams);
        tickWithdrawal(player, activeMilligrams);
        tickNutritionPenalty(cannabisActive, activeMilligrams);
        sessionActiveLastTick = heroin.active();
    }

    public void forceEndSession(HeroinEffectState heroin) {
        if (heroin.active()) {
            sessionPeakMilligrams = Math.max(sessionPeakMilligrams, Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams()));
            sessionQualified = sessionQualified || sessionPeakMilligrams >= EXPERIENCE_THRESHOLD_MILLIGRAMS;
        }
        if (sessionPeakMilligrams > 0.0D || sessionQualified || sessionActiveLastTick) {
            finishSession();
        }
        sessionActiveLastTick = false;
    }

    public void onSleep() {
        if (dependencePhase > 0 || tolerancePhase > 0) {
            soberTicks = Math.min(resetTargetTicks(), soberTicks + SLEEP_RECOVERY_TICKS);
            if (soberTicks >= resetTargetTicks()) {
                clearLongTermState();
            }
        }
        if (crashTicks > 0) {
            crashTicks = Math.max(0, crashTicks - SLEEP_RECOVERY_TICKS);
            if (crashTicks <= 0) {
                clearCrash();
            }
        }
        if (nutritionPenaltyTicks > 0) {
            nutritionPenaltyTicks = Math.max(0, nutritionPenaltyTicks - SLEEP_RECOVERY_TICKS / 2);
            if (nutritionPenaltyTicks <= 0) {
                clearNutritionPenalty();
            }
        }
    }

    public void resetAfterDeath() {
        sessionPeakMilligrams = 0.0D;
        sessionQualified = false;
        sessionActiveLastTick = false;
        overdoseChecksUpToMilligrams = 0;
        clearCrash();
        clearNutritionPenalty();
        clearLongTermState();
    }

    public RecoveryState snapshot() {
        int resetTargetTicks = dependencePhase > 0 || tolerancePhase > 0 ? resetTargetTicks() : BASE_RESET_AFTER_TICKS;
        int resetTicksRemaining = dependencePhase > 0 || tolerancePhase > 0 ? Math.max(0, resetTargetTicks - soberTicks) : 0;
        return new RecoveryState(
                isActive(),
                tolerancePhase,
                MAX_TOLERANCE_PHASE,
                dependencePhase,
                MAX_DEPENDENCE_PHASE,
                soberTicks,
                resetTicksRemaining,
                resetTargetTicks,
                crashTicks,
                crashDurationTicks,
                crashSeverity,
                crashPeakMilligrams,
                withdrawalSeverity,
                nutritionPenaltySeverity(),
                suppressionThresholdMilligrams()
        );
    }

    public boolean isActive() {
        return tolerancePhase > 0
                || dependencePhase > 0
                || crashTicks > 0
                || crashSeverity > 0.001D
                || withdrawalSeverity > 0.001D
                || nutritionPenaltyTicks > 0
                || nutritionPenaltySeverity > 0.001D
                || sessionPeakMilligrams > 0.001D
                || sessionQualified
                || sessionActiveLastTick;
    }

    public double toleranceProgress() {
        return saturate(tolerancePhase / (double) MAX_TOLERANCE_PHASE);
    }

    public double dependenceProgress() {
        return saturate(dependencePhase / (double) MAX_DEPENDENCE_PHASE);
    }

    public double activeEffectMultiplier() {
        return Math.max(0.30D, 1.0D - toleranceProgress() * 0.50D - dependenceProgress() * 0.08D);
    }

    public double durationToleranceProgress() {
        return toleranceProgress();
    }

    public double withdrawalSeverity() {
        return saturate(withdrawalSeverity);
    }

    public double crashSeverity(double activeMilligrams) {
        if (crashTicks <= 0 || crashSeverity <= 0.0D) {
            return 0.0D;
        }
        double remaining = crashDurationTicks <= 0 ? 0.0D : crashTicks / (double) crashDurationTicks;
        double mask = 1.0D - smoothstep(8.0D, Math.max(25.0D, suppressionThresholdMilligrams()), activeMilligrams);
        return saturate(crashSeverity * Math.sqrt(Math.max(0.0D, remaining)) * mask);
    }

    public double nutritionPenaltySeverity() {
        if (nutritionPenaltyTicks <= 0 || nutritionPenaltySeverity <= 0.0D) {
            return 0.0D;
        }
        double remaining = nutritionPenaltyDurationTicks <= 0 ? 0.0D : nutritionPenaltyTicks / (double) nutritionPenaltyDurationTicks;
        return saturate(nutritionPenaltySeverity * Math.sqrt(Math.max(0.0D, remaining)));
    }

    public double nutritionPenalty(boolean cannabisActive) {
        double penalty = nutritionPenaltySeverity();
        if (penalty <= 0.0D) {
            return 0.0D;
        }
        return cannabisActive ? penalty * 0.36D : penalty;
    }

    public boolean sleepDisrupted() {
        return withdrawalSeverity >= 0.42D || crashSeverity(0.0D) >= 0.66D;
    }

    public int overdoseChecksUpToMilligrams() {
        return overdoseChecksUpToMilligrams;
    }

    public void setOverdoseChecksUpToMilligrams(int value) {
        overdoseChecksUpToMilligrams = Math.max(0, value);
    }

    public String debugSummary() {
        if (!isActive()) {
            return "opioid_recovery=steady";
        }
        return String.format(
                Locale.ROOT,
                "opioid_recovery tol=%d/%d dep=%d/%d crash=%.2f withdrawal=%.2f food=%.2f reset=%ds",
                tolerancePhase,
                MAX_TOLERANCE_PHASE,
                dependencePhase,
                MAX_DEPENDENCE_PHASE,
                crashSeverity,
                withdrawalSeverity,
                nutritionPenaltySeverity(),
                Math.max(0, resetTargetTicks() - soberTicks) / 20
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("tolerancePhase", tolerancePhase);
        tag.putInt("dependencePhase", dependencePhase);
        tag.putDouble("pendingToleranceGain", pendingToleranceGain);
        tag.putDouble("pendingDependenceGain", pendingDependenceGain);
        tag.putInt("soberTicks", soberTicks);
        tag.putDouble("sessionPeakMilligrams", sessionPeakMilligrams);
        tag.putBoolean("sessionQualified", sessionQualified);
        tag.putBoolean("sessionActiveLastTick", sessionActiveLastTick);
        tag.putInt("overdoseChecksUpToMilligrams", overdoseChecksUpToMilligrams);
        tag.putInt("crashTicks", crashTicks);
        tag.putInt("crashDurationTicks", crashDurationTicks);
        tag.putDouble("crashSeverity", crashSeverity);
        tag.putDouble("crashPeakMilligrams", crashPeakMilligrams);
        tag.putInt("withdrawalDeficitTicks", withdrawalDeficitTicks);
        tag.putDouble("withdrawalSeverity", withdrawalSeverity);
        tag.putInt("nutritionPenaltyTicks", nutritionPenaltyTicks);
        tag.putInt("nutritionPenaltyDurationTicks", nutritionPenaltyDurationTicks);
        tag.putDouble("nutritionPenaltySeverity", nutritionPenaltySeverity);
        tag.putInt("crashDamageCooldownTicks", crashDamageCooldownTicks);
        tag.putInt("withdrawalDamageCooldownTicks", withdrawalDamageCooldownTicks);
        return tag;
    }

    public static HeroinRecoveryTracker load(CompoundTag tag) {
        HeroinRecoveryTracker tracker = new HeroinRecoveryTracker();
        tracker.tolerancePhase = clampInt(NbtCompat.getIntOr(tag, "tolerancePhase", 0), 0, MAX_TOLERANCE_PHASE);
        tracker.dependencePhase = clampInt(NbtCompat.getIntOr(tag, "dependencePhase", 0), 0, MAX_DEPENDENCE_PHASE);
        tracker.pendingToleranceGain = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "pendingToleranceGain", 0.0D));
        tracker.pendingDependenceGain = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "pendingDependenceGain", 0.0D));
        tracker.soberTicks = Math.max(0, NbtCompat.getIntOr(tag, "soberTicks", 0));
        tracker.sessionPeakMilligrams = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "sessionPeakMilligrams", 0.0D));
        tracker.sessionQualified = NbtCompat.getBooleanOr(tag, "sessionQualified", false);
        tracker.sessionActiveLastTick = NbtCompat.getBooleanOr(tag, "sessionActiveLastTick", false);
        tracker.overdoseChecksUpToMilligrams = Math.max(0, NbtCompat.getIntOr(tag, "overdoseChecksUpToMilligrams", 0));
        tracker.crashTicks = Math.max(0, NbtCompat.getIntOr(tag, "crashTicks", 0));
        tracker.crashDurationTicks = Math.max(tracker.crashTicks, NbtCompat.getIntOr(tag, "crashDurationTicks", tracker.crashTicks));
        tracker.crashSeverity = saturate(NbtCompat.getDoubleOr(tag, "crashSeverity", 0.0D));
        tracker.crashPeakMilligrams = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "crashPeakMilligrams", 0.0D));
        tracker.withdrawalDeficitTicks = Math.max(0, NbtCompat.getIntOr(tag, "withdrawalDeficitTicks", 0));
        tracker.withdrawalSeverity = saturate(NbtCompat.getDoubleOr(tag, "withdrawalSeverity", 0.0D));
        tracker.nutritionPenaltyTicks = Math.max(0, NbtCompat.getIntOr(tag, "nutritionPenaltyTicks", 0));
        tracker.nutritionPenaltyDurationTicks = Math.max(tracker.nutritionPenaltyTicks, NbtCompat.getIntOr(tag, "nutritionPenaltyDurationTicks", tracker.nutritionPenaltyTicks));
        tracker.nutritionPenaltySeverity = saturate(NbtCompat.getDoubleOr(tag, "nutritionPenaltySeverity", 0.0D));
        tracker.crashDamageCooldownTicks = Math.max(0, NbtCompat.getIntOr(tag, "crashDamageCooldownTicks", 0));
        tracker.withdrawalDamageCooldownTicks = Math.max(0, NbtCompat.getIntOr(tag, "withdrawalDamageCooldownTicks", 0));
        return tracker;
    }

    private void finishSession() {
        if (sessionPeakMilligrams >= CRASH_THRESHOLD_MILLIGRAMS) {
            queueCrash(sessionPeakMilligrams);
            queueNutritionPenalty(sessionPeakMilligrams);
        }
        addToleranceGain(toleranceGainForSession(sessionPeakMilligrams) * 0.50D);
        addDependenceGain(dependenceGainForSession(sessionPeakMilligrams) * 0.50D);
        sessionPeakMilligrams = 0.0D;
        sessionQualified = false;
    }

    private void queueCrash(double peakMilligrams) {
        double dependenceFactor = dependenceProgress();
        double doseFactor = smoothstep(CRASH_THRESHOLD_MILLIGRAMS, 500.0D, peakMilligrams);
        double heavyFactor = smoothstep(250.0D, 500.0D, peakMilligrams);
        double addedSeverity = saturate(doseFactor * (0.34D + dependenceFactor * 0.32D) + heavyFactor * (0.18D + dependenceFactor * 0.22D));
        crashSeverity = saturate(crashSeverity + addedSeverity * (crashTicks > 0 ? 0.72D : 1.0D));
        crashPeakMilligrams = Math.max(crashPeakMilligrams, peakMilligrams);

        double baseDays = 0.50D + doseFactor * 0.34D;
        double dependenceDays = dependenceFactor * (0.40D + heavyFactor * 0.90D);
        int addedDuration = (int) Math.round(MINECRAFT_DAY_TICKS * (baseDays + dependenceDays));
        if (crashTicks > 0) {
            addedDuration = (int) Math.round(addedDuration * 0.70D);
        }
        crashTicks = Math.min(MAX_CRASH_TICKS, crashTicks + Math.max(MINECRAFT_DAY_TICKS / 4, addedDuration));
        crashDurationTicks = Math.max(crashDurationTicks, crashTicks);
    }

    private void queueNutritionPenalty(double peakMilligrams) {
        double doseFactor = smoothstep(CRASH_THRESHOLD_MILLIGRAMS, 500.0D, peakMilligrams);
        double dependenceFactor = dependenceProgress();
        double addedSeverity = saturate(0.28D + doseFactor * 0.38D + dependenceFactor * 0.22D);
        nutritionPenaltySeverity = saturate(nutritionPenaltySeverity + addedSeverity * (nutritionPenaltyTicks > 0 ? 0.60D : 1.0D));
        int addedDuration = (int) Math.round(MINECRAFT_DAY_TICKS * (0.35D + doseFactor * 0.32D + dependenceFactor * 0.40D));
        nutritionPenaltyTicks = Math.min(MAX_CRASH_TICKS, nutritionPenaltyTicks + Math.max(MINECRAFT_DAY_TICKS / 5, addedDuration));
        nutritionPenaltyDurationTicks = Math.max(nutritionPenaltyDurationTicks, nutritionPenaltyTicks);
    }

    private void tickCrash(ServerPlayer player, double activeMilligrams) {
        if (crashTicks <= 0) {
            clearCrash();
            return;
        }

        if (activeMilligrams >= suppressionThresholdMilligrams() * 0.35D) {
            crashSeverity += (0.0D - crashSeverity) * 0.12D;
            return;
        }

        crashTicks--;
        if (crashTicks <= 0) {
            clearCrash();
            return;
        }

        double effective = crashSeverity(activeMilligrams);
        if (effective < 0.72D || crashDamageCooldownTicks > 0) {
            return;
        }
        float damage = effective >= 0.92D ? 1.0F : 0.5F;
        player.hurt(HeroinDamageTypes.withdrawal(player), damage);
        crashDamageCooldownTicks = Math.max(90, CRASH_DAMAGE_INTERVAL_TICKS - (int) Math.round(effective * 80.0D));
    }

    private void tickWithdrawal(ServerPlayer player, double activeMilligrams) {
        if (dependencePhase <= 0) {
            withdrawalDeficitTicks = 0;
            withdrawalSeverity = 0.0D;
            return;
        }

        double threshold = suppressionThresholdMilligrams();
        if (activeMilligrams >= threshold * 0.35D) {
            withdrawalDeficitTicks = Math.max(0, withdrawalDeficitTicks - 160);
            double relief = activeMilligrams >= threshold ? 0.18D : 0.085D;
            withdrawalSeverity += (0.0D - withdrawalSeverity) * relief;
            if (withdrawalSeverity < 0.002D) {
                withdrawalSeverity = 0.0D;
            }
            return;
        }

        withdrawalDeficitTicks++;
        double phaseFactor = dependenceProgress();
        double deficit = saturate((threshold - activeMilligrams) / Math.max(10.0D, threshold));
        double timeFactor = smoothstep(withdrawalOnsetDelayTicks(), withdrawalOnsetDelayTicks() + withdrawalRampTicks(), withdrawalDeficitTicks);
        double lateStage = smoothstep(0.56D, 1.0D, phaseFactor);
        double target = saturate((phaseFactor * 0.92D + lateStage * 0.24D) * deficit * (0.28D + timeFactor * 0.84D));
        withdrawalSeverity += (target - withdrawalSeverity) * (target > withdrawalSeverity ? 0.016D : 0.060D);

        if (withdrawalSeverity >= 0.54D && withdrawalDamageCooldownTicks <= 0) {
            float damage = withdrawalSeverity >= 0.88D ? 1.5F : withdrawalSeverity >= 0.70D ? 1.0F : 0.5F;
            player.hurt(HeroinDamageTypes.withdrawal(player), damage);
            withdrawalDamageCooldownTicks = Math.max(55, WITHDRAWAL_DAMAGE_INTERVAL_TICKS - (int) Math.round(withdrawalSeverity * 42.0D));
        }
    }

    private void tickNutritionPenalty(boolean cannabisActive, double activeMilligrams) {
        if (nutritionPenaltyTicks <= 0) {
            clearNutritionPenalty();
            return;
        }
        if (activeMilligrams >= suppressionThresholdMilligrams() * 0.35D) {
            nutritionPenaltySeverity += (0.0D - nutritionPenaltySeverity) * 0.075D;
            return;
        }
        nutritionPenaltyTicks--;
        if (cannabisActive) {
            nutritionPenaltyTicks = Math.max(0, nutritionPenaltyTicks - 1);
            nutritionPenaltySeverity += (0.0D - nutritionPenaltySeverity) * 0.025D;
        }
        if (nutritionPenaltyTicks <= 0) {
            clearNutritionPenalty();
        }
    }

    private void clearCrash() {
        crashTicks = 0;
        crashDurationTicks = 0;
        crashSeverity = 0.0D;
        crashPeakMilligrams = 0.0D;
        crashDamageCooldownTicks = 0;
    }

    private void clearNutritionPenalty() {
        nutritionPenaltyTicks = 0;
        nutritionPenaltyDurationTicks = 0;
        nutritionPenaltySeverity = 0.0D;
    }

    private void clearLongTermState() {
        tolerancePhase = 0;
        dependencePhase = 0;
        pendingToleranceGain = 0.0D;
        pendingDependenceGain = 0.0D;
        soberTicks = 0;
        withdrawalDeficitTicks = 0;
        withdrawalSeverity = 0.0D;
        withdrawalDamageCooldownTicks = 0;
    }

    private int toleranceGainForSession(double peakMilligrams) {
        if (peakMilligrams < EXPERIENCE_THRESHOLD_MILLIGRAMS) {
            return 0;
        }
        int extraSteps = (int) Math.floor(Math.max(0.0D, peakMilligrams - EXPERIENCE_THRESHOLD_MILLIGRAMS) / 125.0D);
        return Math.max(1, Math.min(5, 1 + extraSteps));
    }

    private int dependenceGainForSession(double peakMilligrams) {
        if (peakMilligrams < CRASH_THRESHOLD_MILLIGRAMS) {
            return 0;
        }
        int extraSteps = (int) Math.floor(Math.max(0.0D, peakMilligrams - CRASH_THRESHOLD_MILLIGRAMS) / 150.0D);
        return Math.max(1, Math.min(4, 1 + extraSteps));
    }

    private void addToleranceGain(double gain) {
        if (gain <= 0.0D || tolerancePhase >= MAX_TOLERANCE_PHASE) {
            return;
        }
        pendingToleranceGain += gain;
        int applied = (int) Math.floor(pendingToleranceGain);
        if (applied <= 0) {
            return;
        }
        tolerancePhase = Math.min(MAX_TOLERANCE_PHASE, tolerancePhase + applied);
        pendingToleranceGain -= applied;
        if (tolerancePhase >= MAX_TOLERANCE_PHASE) {
            pendingToleranceGain = 0.0D;
        }
    }

    private void addDependenceGain(double gain) {
        if (gain <= 0.0D || dependencePhase >= MAX_DEPENDENCE_PHASE) {
            return;
        }
        pendingDependenceGain += gain;
        int applied = (int) Math.floor(pendingDependenceGain);
        if (applied <= 0) {
            return;
        }
        dependencePhase = Math.min(MAX_DEPENDENCE_PHASE, dependencePhase + applied);
        pendingDependenceGain -= applied;
        if (dependencePhase >= MAX_DEPENDENCE_PHASE) {
            pendingDependenceGain = 0.0D;
        }
    }

    private int resetTargetTicks() {
        return dependencePhase >= LATE_STAGE_DEPENDENCE_PHASE ? LATE_STAGE_RESET_AFTER_TICKS : BASE_RESET_AFTER_TICKS;
    }

    private double suppressionThresholdMilligrams() {
        if (dependencePhase <= 0) {
            return 0.0D;
        }
        return 18.0D + dependenceProgress() * 112.0D + toleranceProgress() * 70.0D;
    }

    private int withdrawalOnsetDelayTicks() {
        double phaseFactor = dependenceProgress();
        return minutesToTicks(mix(6.0D, 1.2D, phaseFactor));
    }

    private int withdrawalRampTicks() {
        double phaseFactor = dependenceProgress();
        return minutesToTicks(mix(12.0D, 3.0D, phaseFactor));
    }

    private static int minutesToTicks(double minutes) {
        return Math.max(20, (int) Math.round(minutes * 60.0D * 20.0D));
    }

    private static double mix(double a, double b, double t) {
        double clamped = saturate(t);
        return a + (b - a) * clamped;
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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record RecoveryState(
            boolean active,
            int tolerancePhase,
            int maxTolerancePhase,
            int dependencePhase,
            int maxDependencePhase,
            int soberTicks,
            int resetTicksRemaining,
            int resetTargetTicks,
            int crashTicks,
            int crashDurationTicks,
            double crashSeverity,
            double crashPeakMilligrams,
            double withdrawalSeverity,
            double nutritionPenaltySeverity,
            double suppressionThresholdMilligrams
    ) {
        public boolean crashActive() {
            return crashTicks > 0 && crashSeverity > 0.01D;
        }

        public boolean withdrawalActive() {
            return withdrawalSeverity > 0.01D;
        }

        public double toleranceProgress() {
            if (maxTolerancePhase <= 0) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, tolerancePhase / (double) maxTolerancePhase));
        }

        public double dependenceProgress() {
            if (maxDependencePhase <= 0) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, dependencePhase / (double) maxDependencePhase));
        }

        public double discomfort() {
            return Math.max(crashSeverity, withdrawalSeverity);
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(active);
            buffer.writeInt(tolerancePhase);
            buffer.writeInt(maxTolerancePhase);
            buffer.writeInt(dependencePhase);
            buffer.writeInt(maxDependencePhase);
            buffer.writeInt(soberTicks);
            buffer.writeInt(resetTicksRemaining);
            buffer.writeInt(resetTargetTicks);
            buffer.writeInt(crashTicks);
            buffer.writeInt(crashDurationTicks);
            buffer.writeDouble(crashSeverity);
            buffer.writeDouble(crashPeakMilligrams);
            buffer.writeDouble(withdrawalSeverity);
            buffer.writeDouble(nutritionPenaltySeverity);
            buffer.writeDouble(suppressionThresholdMilligrams);
        }

        public static RecoveryState decode(FriendlyByteBuf buffer) {
            return new RecoveryState(
                    buffer.readBoolean(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
        }

        public static RecoveryState inactive() {
            return new RecoveryState(false, 0, MAX_TOLERANCE_PHASE, 0, MAX_DEPENDENCE_PHASE, 0, 0, BASE_RESET_AFTER_TICKS, 0, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
