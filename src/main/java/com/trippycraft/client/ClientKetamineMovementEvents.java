package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ClientKetamineMovementEvents {
    private static double smoothedHorizontalX;
    private static double smoothedHorizontalZ;
    private static boolean initialized;

    private ClientKetamineMovementEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TrippyClientConfig.rendererEnabled || TrippyClientConfig.motionReduction || !ClientExperienceStore.active()) {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || player.isSpectator() || player.isPassenger() || player.isInWater() || player.isFallFlying()) {
            reset();
            return;
        }

        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        if (!ketamine.active()) {
            reset();
            return;
        }

        double load = movementLoad(ketamine);
        Vec3 velocity = player.getDeltaMovement();
        if (load < 0.04D) {
            resetToCurrent(velocity);
            return;
        }
        if (!initialized) {
            resetToCurrent(velocity);
            return;
        }

        double response = Math.max(0.08D, 0.24D - load * 0.15D);
        smoothedHorizontalX += (velocity.x - smoothedHorizontalX) * response;
        smoothedHorizontalZ += (velocity.z - smoothedHorizontalZ) * response;

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double disconnect = clamp(ketamine.motorDisconnect() / 2.4D);
        double drift = clamp(ketamine.gravityDrift() / 2.5D);
        double floatiness = clamp(ketamine.bodyFloat() / 2.4D);
        double collapse = clamp((ketamine.collapseRisk() + ketamine.collapseDepth()) / 2.6D);
        double time = minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false) + ketamine.ageTicks() * 0.013D;
        double lagWindow = Math.pow(Math.max(0.0D, Math.sin(time * (0.10D + drift * 0.05D) + ketamine.activeDoseMilligrams() * 0.035D)), 6.0D);
        double sideDrift = Math.sin(time * (0.16D + drift * 0.08D) + 1.4D) * (0.002D + drift * 0.010D + disconnect * 0.004D);
        double stumble = Math.cos(time * (0.060D + disconnect * 0.035D) + 0.6D) * lagWindow * (0.004D + disconnect * 0.016D + collapse * 0.020D);
        double drag = Math.min(0.20D, load * 0.08D + collapse * 0.10D);
        double heaviness = 0.24D + load * 0.56D;
        double adjustedX = mix(velocity.x, smoothedHorizontalX, heaviness) * (1.0D - drag);
        double adjustedZ = mix(velocity.z, smoothedHorizontalZ, heaviness) * (1.0D - drag);
        if (horizontalSpeed > 0.008D) {
            Vec3 side = new Vec3(-velocity.z / horizontalSpeed, 0.0D, velocity.x / horizontalSpeed);
            double sidewaysAmount = (sideDrift + stumble) * (0.65D + horizontalSpeed * 2.6D);
            adjustedX += side.x * sidewaysAmount;
            adjustedZ += side.z * sidewaysAmount;
        }

        double adjustedY = velocity.y;
        if (!player.onGround()) {
            adjustedY += Math.sin(time * (0.11D + floatiness * 0.06D) + 0.3D) * (0.0008D + floatiness * 0.004D) - collapse * 0.002D;
        }
        player.setDeltaMovement(adjustedX, adjustedY, adjustedZ);
    }

    private static double movementLoad(KetamineEffectState ketamine) {
        double motor = clamp(ketamine.motorDisconnect() / 2.4D);
        double latency = clamp(ketamine.inputLatency() / 2.3D);
        double floatiness = clamp(ketamine.bodyFloat() / 2.4D);
        double depth = clamp(ketamine.depthMismatch() / 2.5D);
        double collapse = clamp((ketamine.collapseRisk() + ketamine.collapseDepth()) / 2.6D);
        double dose = smoothstep(50.0D, 500.0D, Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams()));
        return clamp(motor * 0.34D + latency * 0.24D + floatiness * 0.18D + depth * 0.10D + collapse * 0.20D + dose * 0.12D);
    }

    private static void reset() {
        initialized = false;
        smoothedHorizontalX = 0.0D;
        smoothedHorizontalZ = 0.0D;
    }

    private static void resetToCurrent(Vec3 velocity) {
        initialized = true;
        smoothedHorizontalX = velocity.x;
        smoothedHorizontalZ = velocity.z;
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * clamp(t);
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