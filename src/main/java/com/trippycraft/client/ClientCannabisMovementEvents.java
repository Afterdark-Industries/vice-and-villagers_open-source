package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ClientCannabisMovementEvents {
    private static double smoothedHorizontalX;
    private static double smoothedHorizontalZ;
    private static boolean initialized;

    private ClientCannabisMovementEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TrippyClientConfig.rendererEnabled || TrippyClientConfig.motionReduction || !ClientExperienceStore.active()) {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || player.isSpectator() || player.isPassenger() || player.isInWater() || !player.onGround()) {
            reset();
            return;
        }

        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        if (!cannabis.active()) {
            reset();
            return;
        }

        double load = movementLoad(cannabis);
        if (load < 0.04D) {
            resetToCurrent(player.getDeltaMovement());
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed < 0.0025D) {
            smoothedHorizontalX += (velocity.x - smoothedHorizontalX) * 0.28D;
            smoothedHorizontalZ += (velocity.z - smoothedHorizontalZ) * 0.28D;
            return;
        }

        if (!initialized) {
            resetToCurrent(velocity);
            return;
        }

        double response = Math.max(0.09D, 0.24D - load * 0.14D);
        smoothedHorizontalX += (velocity.x - smoothedHorizontalX) * response;
        smoothedHorizontalZ += (velocity.z - smoothedHorizontalZ) * response;

        double heaviness = 0.28D + load * 0.54D;
        double drag = 1.0D - Math.min(0.090D, cannabisVisualLoad(cannabis) * 0.046D + load * 0.030D);
        double adjustedX = mix(velocity.x, smoothedHorizontalX, heaviness) * drag;
        double adjustedZ = mix(velocity.z, smoothedHorizontalZ, heaviness) * drag;
        player.setDeltaMovement(adjustedX, velocity.y, adjustedZ);
    }

    private static double movementLoad(CannabisEffectState cannabis) {
        return clamp(cannabis.temporalDrag() * 0.12D + cannabis.thoughtStickiness() * 0.11D + cannabisVisualLoad(cannabis) * 0.34D);
    }

    private static double cannabisVisualLoad(CannabisEffectState cannabis) {
        double load = Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D);
        load += clamp((cannabis.doseScale() - 1.0D) / 19.0D) * 0.08D;
        return clamp(load);
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
}