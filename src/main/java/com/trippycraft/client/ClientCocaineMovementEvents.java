package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.config.TrippyClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ClientCocaineMovementEvents {
    private ClientCocaineMovementEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TrippyClientConfig.rendererEnabled || TrippyClientConfig.motionReduction || !ClientExperienceStore.active()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || player.isSpectator() || player.isPassenger() || player.isInWater() || !player.onGround()) {
            return;
        }

        var cocaine = ClientExperienceStore.cocaine();
        if (!cocaine.active()) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed < 0.014D) {
            return;
        }

        double load = cocaineLoad(cocaine.intensity(), cocaine.activeDoseGrams());
        double drive = clamp(cocaine.locomotorDrive() * 0.080D + cocaine.compulsiveMomentum() * 0.055D + cocaine.inputSharpness() * 0.020D + load * 0.24D - cocaine.crashDrag() * 0.050D);
        double jitter = clamp(cocaine.jitter() * 0.050D + cocaine.overcorrection() * 0.060D + cocaine.microFlicker() * 0.018D);
        double crash = clamp(cocaine.crashDrag() * 0.080D + cocaine.crashDesaturation() * 0.32D);
        if (drive < 0.03D && jitter < 0.03D && crash < 0.06D) {
            return;
        }

        double time = minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false) + cocaine.ageTicks() * 0.031D;
        double window = Math.pow(Math.max(0.0D, Math.sin(time * (0.22D + jitter * 0.18D) + cocaine.activeDoseGrams() * 0.70D)), 10.0D);
        double microJab = Math.sin(time * (1.15D + jitter * 0.55D) + 0.4D) * (0.0025D + jitter * 0.010D);
        double overstep = Math.sin(time * (0.42D + jitter * 0.22D) + 1.2D) * window * (0.006D + cocaine.overcorrection() * 0.012D);
        double forwardScale = 1.0D + Math.min(0.14D, drive * 0.095D + cocaine.inputSharpness() * 0.030D) - Math.min(0.045D, crash * 0.032D);
        double sidewaysAmount = (microJab + overstep) * (0.82D + horizontalSpeed * 2.5D);

        Vec3 side = new Vec3(-velocity.z / horizontalSpeed, 0.0D, velocity.x / horizontalSpeed);
        Vec3 adjusted = new Vec3(
                velocity.x * forwardScale + side.x * sidewaysAmount,
                velocity.y,
                velocity.z * forwardScale + side.z * sidewaysAmount
        );
        player.setDeltaMovement(adjusted);
    }

    private static double cocaineLoad(double intensity, double activeDoseGrams) {
        double load = Math.log1p(Math.max(0.0D, intensity)) / Math.log1p(10.0D);
        load += smoothstep(1.0D, 6.0D, activeDoseGrams) * 0.10D;
        return clamp(load);
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