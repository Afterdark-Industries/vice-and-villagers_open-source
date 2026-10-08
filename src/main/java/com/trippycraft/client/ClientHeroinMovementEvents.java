package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.heroin.HeroinEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class ClientHeroinMovementEvents {
    private static final RandomSource RNG = RandomSource.create();

    private static double smoothedHorizontalX;
    private static double smoothedHorizontalZ;
    private static boolean initialized;

    private ClientHeroinMovementEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TrippyClientConfig.rendererEnabled || !ClientExperienceStore.active()) {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || player.isSpectator() || player.isPassenger() || player.isFallFlying()) {
            reset();
            return;
        }

        HeroinEffectState heroin = ClientExperienceStore.heroin();
        if (!heroin.active()) {
            reset();
            return;
        }

        spawnAmbientMotes(minecraft, player, heroin);
        if (TrippyClientConfig.motionReduction) {
            reset();
            return;
        }

        double load = movementLoad(heroin);
        Vec3 velocity = player.getDeltaMovement();
        if (load < 0.035D) {
            resetToCurrent(velocity);
            return;
        }
        if (!initialized) {
            resetToCurrent(velocity);
            return;
        }

        double response = Math.max(0.010D, 0.11D - load * 0.07D - ClientRenderEvents.heroinNodAmount() * 0.04D);
        smoothedHorizontalX += (velocity.x - smoothedHorizontalX) * response;
        smoothedHorizontalZ += (velocity.z - smoothedHorizontalZ) * response;

        double time = minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false) + heroin.ageTicks() * 0.018D;
        double nod = clamp((heroin.nodding() + heroin.microSleep()) / 6.0D + ClientRenderEvents.heroinNodAmount() * 0.34D);
        double nodPulse = Math.pow(Math.max(0.0D, Math.sin(time * (0.045D + nod * 0.020D) + heroin.activeDoseMilligrams() * 0.018D)), 8.0D) * nod;
        double drag = Math.min(0.64D,
            load * 0.22D
                + nodPulse * 0.36D
                + ClientRenderEvents.heroinNodAmount() * 0.24D
                + ClientRenderEvents.heroinBlackoutAmount() * 0.16D);
        double heaviness = 0.42D + load * 0.88D + nodPulse * 0.46D + ClientRenderEvents.heroinBlackoutAmount() * 0.24D;
        double adjustedX = mix(velocity.x, smoothedHorizontalX, heaviness) * (1.0D - drag);
        double adjustedZ = mix(velocity.z, smoothedHorizontalZ, heaviness) * (1.0D - drag);

        if (player.isSprinting() && load > 0.14D) {
            player.setSprinting(false);
        }
        player.setDeltaMovement(adjustedX, velocity.y, adjustedZ);
    }

    private static double movementLoad(HeroinEffectState heroin) {
        double motor = clamp(heroin.motorDrag() / 4.2D);
        double mining = clamp(heroin.miningDrag() / 4.0D);
        double nod = clamp((heroin.nodding() + heroin.microSleep()) / 6.2D);
        double dose = smoothstep(25.0D, 500.0D, Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams()));
        return clamp(motor * 0.48D + mining * 0.22D + nod * 0.46D + dose * 0.20D);
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

    private static void spawnAmbientMotes(Minecraft minecraft, LocalPlayer player, HeroinEffectState heroin) {
        if (minecraft.level == null) {
            return;
        }

        double strength = ambientParticleLoad(heroin);
        if (strength <= 0.02D) {
            return;
        }

        int motes = (int) Math.floor(strength * 2.0D);
        if (RNG.nextDouble() < 0.30D + strength * 0.85D) {
            motes++;
        }
        if (ClientRenderEvents.heroinNodAmount() > 0.18D && RNG.nextDouble() < strength * 0.70D) {
            motes++;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 base = player.position();
        double range = 1.8D + strength * 2.6D;
        double heightBand = 0.8D + strength * 1.4D;
        for (int i = 0; i < motes; i++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(RNG.nextDouble()) * range;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            double dy = (RNG.nextDouble() - 0.28D) * heightBand + 0.25D;
            double orbit = 0.004D + strength * 0.012D;
            double vx = -Math.sin(angle) * orbit + (RNG.nextDouble() - 0.5D) * 0.006D;
            double vy = 0.004D + RNG.nextDouble() * 0.010D + strength * 0.006D;
            double vz = Math.cos(angle) * orbit + (RNG.nextDouble() - 0.5D) * 0.006D;
            minecraft.level.addParticle(pickAmbientParticle(i, strength), eye.x + dx, base.y + 0.50D + dy, eye.z + dz, vx, vy, vz);
        }
    }

    private static ParticleOptions pickAmbientParticle(int index, double strength) {
        if ((index & 3) == 0 && strength > 0.42D) {
            return ParticleTypes.END_ROD;
        }
        return RNG.nextDouble() < 0.72D ? ParticleTypes.GLOW : ParticleTypes.END_ROD;
    }

    private static double ambientParticleLoad(HeroinEffectState heroin) {
        double load = Math.log1p(Math.max(0.0D, heroin.intensity())) / Math.log1p(8.2D);
        load += clamp(heroin.warmth() / 8.2D) * 0.24D;
        load += clamp(heroin.softFocus() / 8.2D) * 0.18D;
        load += ClientRenderEvents.heroinNodAmount() * 0.12D;
        return clamp(load);
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
