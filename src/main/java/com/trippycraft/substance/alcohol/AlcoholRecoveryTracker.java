package com.trippycraft.substance.alcohol;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class AlcoholRecoveryTracker {
    public static final int MAX_DEPENDENCY_PHASE = 15;

    private static final int MINECRAFT_DAY_TICKS = 24000;
    private static final int BASE_RESET_AFTER_TICKS = 2 * MINECRAFT_DAY_TICKS;
    private static final int LATE_STAGE_RESET_AFTER_TICKS = 3 * MINECRAFT_DAY_TICKS;
    private static final int LATE_STAGE_DEPENDENCY_PHASE = 10;
    private static final double DRUNK_EXPERIENCE_THRESHOLD_DRINKS = 3.0D;
    private static final double HANGOVER_THRESHOLD_DRINKS = 4.0D;
    private static final double HANGOVER_NAUSEA_THRESHOLD_DRINKS = 6.0D;
    private static final double HANGOVER_SUPPRESSION_THRESHOLD_DRINKS = 0.75D;
    private static final int HANGOVER_DAMAGE_INTERVAL_TICKS = 14 * 20;
    private static final int WITHDRAWAL_DAMAGE_INTERVAL_TICKS = 100;
    private static final int WITHDRAWAL_LETHAL_CHECK_INTERVAL_TICKS = 90 * 20;
    private static final int SLEEP_RECOVERY_TICKS = MINECRAFT_DAY_TICKS / 2;
    private static final int MAX_HANGOVER_TICKS = 3 * MINECRAFT_DAY_TICKS;

    private int dependencyPhase;
    private int soberTicks;
    private double sessionPeakDrinks;
    private boolean sessionQualified;
    private boolean sessionActiveLastTick;
    private int poisonChecksUpToDrink;
    private int hangoverTicks;
    private int hangoverDurationTicks;
    private double hangoverSeverity;
    private double hangoverPeakDrinks;
    private int withdrawalDeficitTicks;
    private double withdrawalSeverity;
    private int hangoverDamageCooldownTicks;
    private int withdrawalDamageCooldownTicks;
    private int withdrawalLethalCheckCooldownTicks;

    public void onDoseAdded(double standardDrinks) {
        if (standardDrinks > 0.0D) {
            soberTicks = 0;
        }
    }

    public void tick(ServerPlayer player, AlcoholEffectState alcohol) {
        if (!player.isAlive() || player.isDeadOrDying()) {
            resetAfterDeath();
            return;
        }

        if (hangoverDamageCooldownTicks > 0) {
            hangoverDamageCooldownTicks--;
        }
        if (withdrawalDamageCooldownTicks > 0) {
            withdrawalDamageCooldownTicks--;
        }
        if (withdrawalLethalCheckCooldownTicks > 0) {
            withdrawalLethalCheckCooldownTicks--;
        }

        if (sessionActiveLastTick && !alcohol.active()) {
            finishSession();
        }

        double activeDrinks = alcohol.active() ? Math.max(alcohol.activeStandardDrinks(), alcohol.displayStandardDrinks()) : 0.0D;
        if (alcohol.active()) {
            soberTicks = 0;
            sessionPeakDrinks = Math.max(sessionPeakDrinks, Math.max(activeDrinks, alcohol.targetStandardDrinks()));
            sessionQualified = sessionQualified || sessionPeakDrinks >= DRUNK_EXPERIENCE_THRESHOLD_DRINKS;
            applyPoisonChecks(player, sessionPeakDrinks);
        } else if (dependencyPhase > 0) {
            soberTicks++;
            if (soberTicks >= resetTargetTicks()) {
                clearLongTermState();
            }
        } else {
            soberTicks = 0;
        }

        tickHangover(player, activeDrinks);
        tickWithdrawal(player, activeDrinks);
        sessionActiveLastTick = alcohol.active();
    }

    public void forceEndSession(AlcoholEffectState alcohol) {
        if (alcohol.active()) {
            sessionPeakDrinks = Math.max(sessionPeakDrinks, Math.max(alcohol.activeStandardDrinks(), alcohol.targetStandardDrinks()));
            sessionQualified = sessionQualified || sessionPeakDrinks >= DRUNK_EXPERIENCE_THRESHOLD_DRINKS;
        }
        if (sessionPeakDrinks > 0.0D || sessionQualified || sessionActiveLastTick) {
            finishSession();
        }
        sessionActiveLastTick = false;
    }

    public void onSleep() {
        if (dependencyPhase > 0) {
            soberTicks = Math.min(resetTargetTicks(), soberTicks + SLEEP_RECOVERY_TICKS);
            if (soberTicks >= resetTargetTicks()) {
                clearLongTermState();
            }
        }
        if (hangoverTicks <= 0) {
            return;
        }
        hangoverTicks = Math.max(0, hangoverTicks - SLEEP_RECOVERY_TICKS);
        if (hangoverTicks <= 0) {
            clearHangover();
        }
    }

    public void resetAfterDeath() {
        sessionPeakDrinks = 0.0D;
        sessionQualified = false;
        sessionActiveLastTick = false;
        poisonChecksUpToDrink = 0;
        clearHangover();
        clearLongTermState();
    }

    public RecoveryState snapshot() {
        int resetTargetTicks = dependencyPhase > 0 ? resetTargetTicks() : BASE_RESET_AFTER_TICKS;
        int resetTicksRemaining = dependencyPhase > 0 ? Math.max(0, resetTargetTicks - soberTicks) : 0;
        return new RecoveryState(
                isActive(),
                dependencyPhase,
                MAX_DEPENDENCY_PHASE,
                soberTicks,
            resetTicksRemaining,
            resetTargetTicks,
                hangoverTicks,
                hangoverDurationTicks,
                hangoverSeverity,
            hangoverPeakDrinks,
                withdrawalSeverity,
                suppressionThresholdDrinks()
        );
    }

    public boolean isActive() {
        return dependencyPhase > 0
                || hangoverTicks > 0
                || hangoverSeverity > 0.001D
                || withdrawalSeverity > 0.001D
                || sessionPeakDrinks > 0.001D
                || sessionQualified
                || sessionActiveLastTick;
    }

    public double effectiveHangoverSeverity(double activeDrinks) {
        if (hangoverTicks <= 0 || hangoverSeverity <= 0.0D) {
            return 0.0D;
        }
        double remaining = hangoverDurationTicks <= 0 ? 0.0D : hangoverTicks / (double) hangoverDurationTicks;
        double mask = 1.0D - smoothstep(0.20D, 1.25D, activeDrinks);
        return saturate(hangoverSeverity * Math.sqrt(Math.max(0.0D, remaining)) * mask);
    }

    public double withdrawalSeverity() {
        return saturate(withdrawalSeverity);
    }

    public void applyPsilocybinTherapy(double targetDoseGrams, int therapyStage) {
        // Only DMT therapy should reduce alcoholism.
    }

    public void applyLsdTherapy(double targetDoseMicrograms, int therapyStage) {
        // Only DMT therapy should reduce alcoholism.
    }

    public void applyDmtTherapy(double targetDoseUnits, int therapyStage) {
        applyPsychedelicTherapy(targetDoseUnits, 2.5D, 5.0D, therapyStage);
    }

    public String debugSummary() {
        if (!isActive()) {
            return "alcohol_recovery=steady";
        }
        return String.format(
                Locale.ROOT,
                "alcohol_recovery phase=%d/%d hangover=%.2f withdrawal=%.2f sober_reset=%ds threshold=%.2f",
                dependencyPhase,
                MAX_DEPENDENCY_PHASE,
                hangoverSeverity,
                withdrawalSeverity,
                Math.max(0, resetTargetTicks() - soberTicks) / 20,
                suppressionThresholdDrinks()
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dependencyPhase", dependencyPhase);
        tag.putInt("soberTicks", soberTicks);
        tag.putDouble("sessionPeakDrinks", sessionPeakDrinks);
        tag.putBoolean("sessionQualified", sessionQualified);
        tag.putBoolean("sessionActiveLastTick", sessionActiveLastTick);
        tag.putInt("poisonChecksUpToDrink", poisonChecksUpToDrink);
        tag.putInt("hangoverTicks", hangoverTicks);
        tag.putInt("hangoverDurationTicks", hangoverDurationTicks);
        tag.putDouble("hangoverSeverity", hangoverSeverity);
        tag.putDouble("hangoverPeakDrinks", hangoverPeakDrinks);
        tag.putInt("withdrawalDeficitTicks", withdrawalDeficitTicks);
        tag.putDouble("withdrawalSeverity", withdrawalSeverity);
        tag.putInt("hangoverDamageCooldownTicks", hangoverDamageCooldownTicks);
        tag.putInt("withdrawalDamageCooldownTicks", withdrawalDamageCooldownTicks);
        tag.putInt("withdrawalLethalCheckCooldownTicks", withdrawalLethalCheckCooldownTicks);
        return tag;
    }

    public static AlcoholRecoveryTracker load(CompoundTag tag) {
        AlcoholRecoveryTracker tracker = new AlcoholRecoveryTracker();
        tracker.dependencyPhase = Math.max(0, Math.min(MAX_DEPENDENCY_PHASE, NbtCompat.getIntOr(tag, "dependencyPhase", 0)));
        tracker.soberTicks = Math.max(0, NbtCompat.getIntOr(tag, "soberTicks", 0));
        tracker.sessionPeakDrinks = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "sessionPeakDrinks", 0.0D));
        tracker.sessionQualified = NbtCompat.getBooleanOr(tag, "sessionQualified", false);
        tracker.sessionActiveLastTick = NbtCompat.getBooleanOr(tag, "sessionActiveLastTick", false);
        tracker.poisonChecksUpToDrink = Math.max(0, NbtCompat.getIntOr(tag, "poisonChecksUpToDrink", 0));
        tracker.hangoverTicks = Math.max(0, NbtCompat.getIntOr(tag, "hangoverTicks", 0));
        tracker.hangoverDurationTicks = Math.max(tracker.hangoverTicks, NbtCompat.getIntOr(tag, "hangoverDurationTicks", tracker.hangoverTicks));
        tracker.hangoverSeverity = saturate(NbtCompat.getDoubleOr(tag, "hangoverSeverity", 0.0D));
        tracker.hangoverPeakDrinks = Math.max(0.0D, NbtCompat.getDoubleOr(tag, "hangoverPeakDrinks", 0.0D));
        tracker.withdrawalDeficitTicks = Math.max(0, NbtCompat.getIntOr(tag, "withdrawalDeficitTicks", 0));
        tracker.withdrawalSeverity = saturate(NbtCompat.getDoubleOr(tag, "withdrawalSeverity", 0.0D));
        tracker.hangoverDamageCooldownTicks = Math.max(0, NbtCompat.getIntOr(tag, "hangoverDamageCooldownTicks", 0));
        tracker.withdrawalDamageCooldownTicks = Math.max(0, NbtCompat.getIntOr(tag, "withdrawalDamageCooldownTicks", 0));
        tracker.withdrawalLethalCheckCooldownTicks = Math.max(0, NbtCompat.getIntOr(tag, "withdrawalLethalCheckCooldownTicks", 0));
        return tracker;
    }

    private void finishSession() {
        if (sessionPeakDrinks >= HANGOVER_THRESHOLD_DRINKS) {
            queueHangover(sessionPeakDrinks);
        }
        int dependencyGain = dependencyGainForSession(sessionPeakDrinks);
        if (dependencyGain > 0) {
            dependencyPhase = Math.min(MAX_DEPENDENCY_PHASE, dependencyPhase + dependencyGain);
        }
        sessionPeakDrinks = 0.0D;
        sessionQualified = false;
        poisonChecksUpToDrink = 0;
    }

    private void queueHangover(double peakDrinks) {
        double phaseFactor = dependencyPhase / (double) MAX_DEPENDENCY_PHASE;
        double doseFactor = smoothstep(HANGOVER_THRESHOLD_DRINKS, 16.0D, peakDrinks);
        double bingeFactor = smoothstep(8.0D, 20.0D, peakDrinks);
        double addedSeverity = saturate(doseFactor * (0.42D + phaseFactor * 0.22D) + bingeFactor * (0.16D + phaseFactor * 0.20D));
        hangoverSeverity = saturate(hangoverSeverity + addedSeverity * (hangoverTicks > 0 ? 0.72D : 1.0D));
        hangoverPeakDrinks = Math.max(hangoverPeakDrinks, peakDrinks);

        double baseDays = 0.75D + doseFactor * 0.40D;
        double dependencyDays = bingeFactor * Math.max(0.0D, phaseFactor - 0.40D) * 2.0D;
        int addedDuration = (int) Math.round(MINECRAFT_DAY_TICKS * (baseDays + dependencyDays));
        if (hangoverTicks > 0) {
            addedDuration = (int) Math.round(addedDuration * 0.70D);
        }
        hangoverTicks = Math.min(MAX_HANGOVER_TICKS, hangoverTicks + Math.max(MINECRAFT_DAY_TICKS / 3, addedDuration));
        hangoverDurationTicks = Math.max(hangoverDurationTicks, hangoverTicks);
    }

    private void tickHangover(ServerPlayer player, double activeDrinks) {
        if (hangoverTicks <= 0) {
            clearHangover();
            return;
        }

        if (activeDrinks >= HANGOVER_SUPPRESSION_THRESHOLD_DRINKS) {
            return;
        }

        hangoverTicks--;
        if (hangoverTicks <= 0) {
            clearHangover();
            return;
        }

        double effective = effectiveHangoverSeverity(activeDrinks);
        if (effective < 0.64D || hangoverDamageCooldownTicks > 0) {
            return;
        }
        float damage = effective >= 0.90D ? 1.5F : effective >= 0.78D ? 1.0F : 0.5F;
        player.hurt(player.damageSources().magic(), damage);
        hangoverDamageCooldownTicks = Math.max(HANGOVER_DAMAGE_INTERVAL_TICKS, secondsToTicks(mix(18.0D, 10.0D, effective)));
    }

    private void tickWithdrawal(ServerPlayer player, double activeDrinks) {
        if (dependencyPhase <= 0) {
            withdrawalDeficitTicks = 0;
            withdrawalSeverity = 0.0D;
            return;
        }

        double threshold = suppressionThresholdDrinks();
        if (activeDrinks >= threshold) {
            withdrawalDeficitTicks = Math.max(0, withdrawalDeficitTicks - 120);
            withdrawalSeverity += (0.0D - withdrawalSeverity) * 0.10D;
            if (withdrawalSeverity < 0.002D) {
                withdrawalSeverity = 0.0D;
            }
            return;
        }

        withdrawalDeficitTicks++;
        double phaseFactor = dependencyPhase / (double) MAX_DEPENDENCY_PHASE;
        double deficit = saturate((threshold - activeDrinks) / Math.max(0.30D, threshold));
        double timeFactor = smoothstep(withdrawalOnsetDelayTicks(), withdrawalOnsetDelayTicks() + withdrawalRampTicks(), withdrawalDeficitTicks);
        double target = saturate(phaseFactor * deficit * (0.28D + timeFactor * 0.72D));
        withdrawalSeverity += (target - withdrawalSeverity) * (target > withdrawalSeverity ? 0.018D : 0.060D);

        if (withdrawalSeverity >= 0.34D && withdrawalDamageCooldownTicks <= 0) {
            float damage = withdrawalSeverity >= 0.84D ? 2.0F : withdrawalSeverity >= 0.60D ? 1.0F : 0.5F;
            player.hurt(AlcoholDamageTypes.withdrawal(player), damage);
            withdrawalDamageCooldownTicks = Math.max(55, WITHDRAWAL_DAMAGE_INTERVAL_TICKS - (int) Math.round(withdrawalSeverity * 35.0D));
        }

        if (dependencyPhase >= 10 && withdrawalSeverity >= 0.985D && withdrawalLethalCheckCooldownTicks <= 0) {
            double deathChance = Math.min(1.0D, (dependencyPhase - 9.0D) / 6.0D);
            if (player.getRandom().nextDouble() < deathChance) {
                resetAfterDeath();
                player.die(AlcoholDamageTypes.withdrawal(player));
                return;
            }
            withdrawalLethalCheckCooldownTicks = WITHDRAWAL_LETHAL_CHECK_INTERVAL_TICKS;
        }
    }

    private void applyPoisonChecks(ServerPlayer player, double peakDrinks) {
        int threshold = Math.max(0, (int) Math.floor(peakDrinks));
        while (poisonChecksUpToDrink < threshold) {
            poisonChecksUpToDrink++;
            double deathChance = poisonDeathChance(poisonChecksUpToDrink);
            if (deathChance > 0.0D && player.getRandom().nextDouble() < deathChance) {
                ModNetwork.sendAlcoholDeathPresentation(player, true);
                resetAfterDeath();
                player.die(AlcoholDamageTypes.blackout(player));
                return;
            }
        }
    }

    private void clearHangover() {
        hangoverTicks = 0;
        hangoverDurationTicks = 0;
        hangoverSeverity = 0.0D;
        hangoverPeakDrinks = 0.0D;
        hangoverDamageCooldownTicks = 0;
    }

    private void clearLongTermState() {
        dependencyPhase = 0;
        soberTicks = 0;
        withdrawalDeficitTicks = 0;
        withdrawalSeverity = 0.0D;
        withdrawalDamageCooldownTicks = 0;
        withdrawalLethalCheckCooldownTicks = 0;
    }

    private void applyPsychedelicTherapy(double targetDose, double halfResetDose, double dayOneDose, int therapyStage) {
        if (dependencyPhase <= 0 || targetDose <= 0.0D) {
            return;
        }
        if (therapyStage < 1 && targetDose >= halfResetDose) {
            reduceDependencyTo(Math.max(1, (int) Math.ceil(dependencyPhase / 2.0D)));
        }
        if (therapyStage < 2 && targetDose >= dayOneDose) {
            reduceDependencyTo(1);
        }
    }

    private void reduceDependencyTo(int targetPhase) {
        int clampedTarget = Math.max(0, Math.min(MAX_DEPENDENCY_PHASE, targetPhase));
        if (clampedTarget >= dependencyPhase) {
            return;
        }
        double ratio = dependencyPhase <= 0 ? 0.0D : clampedTarget / (double) dependencyPhase;
        dependencyPhase = clampedTarget;
        withdrawalSeverity = saturate(withdrawalSeverity * Math.max(0.35D, ratio));
        withdrawalDeficitTicks = Math.min(withdrawalDeficitTicks, withdrawalOnsetDelayTicks() + withdrawalRampTicks());
        soberTicks = 0;
        if (dependencyPhase <= 0) {
            clearLongTermState();
        }
    }

    private int dependencyGainForSession(double peakDrinks) {
        if (peakDrinks < DRUNK_EXPERIENCE_THRESHOLD_DRINKS) {
            return 0;
        }
        int extraSteps = (int) Math.floor(Math.max(0.0D, peakDrinks - DRUNK_EXPERIENCE_THRESHOLD_DRINKS) / 3.0D);
        return Math.max(1, Math.min(5, 1 + extraSteps));
    }

    private int resetTargetTicks() {
        return dependencyPhase >= LATE_STAGE_DEPENDENCY_PHASE ? LATE_STAGE_RESET_AFTER_TICKS : BASE_RESET_AFTER_TICKS;
    }

    private double suppressionThresholdDrinks() {
        if (dependencyPhase <= 0) {
            return 0.0D;
        }
        return 0.15D + dependencyPhase / (double) MAX_DEPENDENCY_PHASE * 2.35D;
    }

    private int withdrawalOnsetDelayTicks() {
        double phaseFactor = dependencyPhase / (double) MAX_DEPENDENCY_PHASE;
        return minutesToTicks(mix(8.0D, 1.5D, phaseFactor));
    }

    private int withdrawalRampTicks() {
        double phaseFactor = dependencyPhase / (double) MAX_DEPENDENCY_PHASE;
        return minutesToTicks(mix(12.0D, 3.0D, phaseFactor));
    }

    private static double poisonDeathChance(int drinks) {
        if (drinks < 20) {
            return 0.0D;
        }
        if (drinks >= 25) {
            return Math.min(1.0D, 0.50D + (drinks - 25) * 0.10D);
        }
        return 0.10D + (drinks - 20) * 0.08D;
    }

    private static int minutesToTicks(double minutes) {
        return Math.max(20, (int) Math.round(minutes * 60.0D * 20.0D));
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(20, (int) Math.round(seconds * 20.0D));
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

    public record RecoveryState(
            boolean active,
            int dependencyPhase,
            int maxDependencyPhase,
            int soberTicks,
            int resetTicksRemaining,
            int resetTargetTicks,
            int hangoverTicks,
            int hangoverDurationTicks,
            double hangoverSeverity,
            double hangoverPeakDrinks,
            double withdrawalSeverity,
            double suppressionThresholdDrinks
    ) {
        public boolean hangoverActive() {
            return hangoverTicks > 0 && hangoverSeverity > 0.01D;
        }

        public boolean withdrawalActive() {
            return withdrawalSeverity > 0.01D;
        }

        public double dependencyProgress() {
            if (maxDependencyPhase <= 0) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, dependencyPhase / (double) maxDependencyPhase));
        }

        public double discomfort() {
            return Math.max(hangoverSeverity, withdrawalSeverity);
        }

        public boolean hangoverNauseaActive() {
            return hangoverPeakDrinks > HANGOVER_NAUSEA_THRESHOLD_DRINKS && hangoverActive();
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(active);
            buffer.writeInt(dependencyPhase);
            buffer.writeInt(maxDependencyPhase);
            buffer.writeInt(soberTicks);
            buffer.writeInt(resetTicksRemaining);
            buffer.writeInt(resetTargetTicks);
            buffer.writeInt(hangoverTicks);
            buffer.writeInt(hangoverDurationTicks);
            buffer.writeDouble(hangoverSeverity);
            buffer.writeDouble(hangoverPeakDrinks);
            buffer.writeDouble(withdrawalSeverity);
            buffer.writeDouble(suppressionThresholdDrinks);
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
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
        }

        public static RecoveryState inactive() {
            return new RecoveryState(false, 0, MAX_DEPENDENCY_PHASE, 0, 0, BASE_RESET_AFTER_TICKS, 0, 0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
