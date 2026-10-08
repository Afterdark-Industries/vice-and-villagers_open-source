package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ClientAlcoholMovementEvents {
    private ClientAlcoholMovementEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TrippyClientConfig.rendererEnabled
                || !TrippyConfig.enableAlcoholEffects
                || !TrippyConfig.alcoholMovementDifficulty
                || TrippyConfig.alcoholAccessibilityMode
                || TrippyClientConfig.motionReduction
                || !ClientExperienceStore.active()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || player.isSpectator() || player.isPassenger() || player.isInWater()) {
            return;
        }

        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        RecoveryState recovery = ClientExperienceStore.alcoholRecovery();
        double hangover = hangoverLoad(recovery, alcohol);
        double withdrawal = clamp(recovery.withdrawalSeverity());
        if (!alcohol.active() && hangover < 0.04D && withdrawal < 0.04D) {
            return;
        }

        double motor = Math.max(alcoholLoad(alcohol, alcohol.motorNoise()), hangover * 0.28D + withdrawal * 0.44D);
        double sway = Math.max(alcoholLoad(alcohol, alcohol.vestibularSway()), hangover * 0.36D + withdrawal * 0.54D);
        double sedation = Math.max(alcoholLoad(alcohol, alcohol.sedation()), hangover * 0.42D + withdrawal * 0.30D);
        double heavyStumble = Math.max(smoothstep(12.0D, 18.0D, alcohol.activeStandardDrinks()), hangover * 0.16D + withdrawal * 0.28D);
        double difficulty = Math.max(Math.max(motor, sway * 0.82D), heavyStumble * 0.72D);
        if (difficulty < 0.055D) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed < 0.018D || !player.onGround()) {
            return;
        }

        double time = minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        double dosePhase = alcohol.active()
            ? alcohol.activeStandardDrinks() * 0.73D + alcohol.ageTicks() * 0.011D
            : recovery.dependencyPhase() * 0.33D + recovery.soberTicks() * 0.006D;
        double slowWander = Math.sin(time * 0.115D + dosePhase) * 0.62D
                + Math.sin(time * 0.049D + dosePhase * 1.7D + 2.0D) * 0.38D;
        double stumbleWindow = Math.pow(Math.max(0.0D, Math.sin(time * (0.061D + difficulty * 0.045D) + dosePhase * 0.9D)), 18.0D);
        double hardStumbleWindow = Math.pow(Math.max(0.0D, Math.sin(time * (0.048D + difficulty * 0.038D) + dosePhase * 1.4D + 1.2D)), 9.0D) * heavyStumble;
        double correction = Math.sin(time * (0.41D + motor * 0.24D) + dosePhase * 2.3D) * stumbleWindow;
        double hardCorrection = Math.sin(time * (0.23D + motor * 0.30D) + dosePhase * 1.1D + 0.4D) * hardStumbleWindow;
        double sidewaysAmount = (slowWander * (0.006D + sway * 0.018D) + correction * (0.010D + motor * 0.030D))
                * (0.55D + horizontalSpeed * 2.0D)
                * (1.0D + Math.max(0.0D, alcohol.tier() - 3) * 0.16D);
        sidewaysAmount += hardCorrection * (0.018D + heavyStumble * 0.052D) * (0.80D + horizontalSpeed * 2.7D);
        double drag = Math.min(0.115D, sedation * 0.014D + motor * 0.010D + heavyStumble * (0.020D + hardStumbleWindow * 0.035D));
        double forwardFalter = Math.min(0.16D, heavyStumble * (0.020D + hardStumbleWindow * 0.075D) + stumbleWindow * motor * 0.016D);

        Vec3 side = new Vec3(-velocity.z / horizontalSpeed, 0.0D, velocity.x / horizontalSpeed);
        Vec3 adjusted = new Vec3(
                velocity.x * (1.0D - drag - forwardFalter) + side.x * sidewaysAmount,
                velocity.y,
                velocity.z * (1.0D - drag - forwardFalter) + side.z * sidewaysAmount
        );
        player.setDeltaMovement(adjusted);
    }

    private static double hangoverLoad(RecoveryState recovery, AlcoholEffectState alcohol) {
        if (!recovery.hangoverActive()) {
            return 0.0D;
        }
        double activeDrinks = alcohol.active() ? alcohol.activeStandardDrinks() : 0.0D;
        double mask = 1.0D - smoothstep(0.20D, 1.25D, activeDrinks);
        double remaining = recovery.hangoverDurationTicks() <= 0 ? 0.0D : recovery.hangoverTicks() / (double) recovery.hangoverDurationTicks();
        return clamp(recovery.hangoverSeverity() * Math.sqrt(Math.max(0.0D, remaining)) * mask);
    }

    private static double alcoholLoad(AlcoholEffectState state, double value) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        double safe = Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value / safe));
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
