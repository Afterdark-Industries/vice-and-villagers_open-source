package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtStage;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public final class ClientWorldHallucinations {
    private static final ParticleOptions[] PSILOCYBIN_MOTES = new ParticleOptions[] {
            ParticleTypes.SPORE_BLOSSOM_AIR,
            ParticleTypes.MYCELIUM
    };

    private static final Random RNG = new Random();
    private static Vec3 spawnEye;
    private static Vec3 spawnLook;
    private static boolean dmtBillboardParticlesDisabled = true;
    private static final ParticleOptions[] LSD_PALETTE = new ParticleOptions[] {
            ParticleTypes.END_ROD,
            ParticleTypes.FIREWORK,
            ParticleTypes.GLOW,
            ParticleTypes.PORTAL,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.WITCH,
            ParticleTypes.DRAGON_BREATH
    };

    private static final ParticleOptions[] LSD_LIQUID_PALETTE = new ParticleOptions[] {
            ParticleTypes.NAUTILUS,
            ParticleTypes.GLOW_SQUID_INK,
            ParticleTypes.PORTAL,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.SOUL_FIRE_FLAME,
            ParticleTypes.DRIPPING_WATER,
            ParticleTypes.DRIPPING_LAVA
    };

    private static final ParticleOptions[] DMT_PALETTE = new ParticleOptions[] {
            ParticleTypes.PORTAL,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.DRAGON_BREATH,
            ParticleTypes.WITCH,
            ParticleTypes.SCULK_SOUL,
            ParticleTypes.GLOW_SQUID_INK,
            ParticleTypes.SOUL
    };

    private ClientWorldHallucinations() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null || mc.player == null) {
            return;
        }
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        // Sample one shared headset pose per simulation tick. Particles are
        // spawned once in world space and subsequently projected by each eye.
        Vec3 head = VivecraftCompat.headPosition();
        Vec3 direction = VivecraftCompat.headDirection();
        spawnEye = head != null ? head : player.getEyePosition();
        spawnLook = direction != null ? direction : player.getLookAngle();
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        DmtEffectState dmt = ClientExperienceStore.dmt();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(state, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        if (TrippyConfig.enablePsilocybinEffects && state.active() && state.intensity() > 0.0D) {
            spawnPsilocybinParticles(level, player, state, blend.psilocybinParticleGain());
        }
        if (TrippyConfig.enableLsdEffects && lsd.active() && lsd.intensity() >= 0.04D) {
            spawnLsdParticles(level, player, lsd, blend.lsdParticleGain());
        }
        if (TrippyConfig.enableDmtEffects && TrippyConfig.dmtParticles && dmt.active() && dmt.hasAuthoredEffects() && dmt.intensity() >= 0.04D) {
            spawnDmtParticles(level, player, dmt, blend.dmtParticleGain());
        }
    }

    private static void spawnPsilocybinParticles(ClientLevel level, LocalPlayer player, PsilocybinEffectState state, double blendGain) {
        double gain = Math.max(0.0D, Math.min(1.0D, blendGain));
        double intensity = Math.max(0.0D, state.intensity() * gain);
        double strength = Math.log1p(intensity);
        double mult = TrippyConfig.psilocybinParticleMultiplier * (TrippyConfig.psilocybinAccessibilityMode ? 0.45D : 1.0D);
        double surroundings = 0.55D + state.environment().forest() * 0.35D + state.environment().cave() * 0.18D;
        double expectedMotes = Math.min(0.25D, strength * 0.030D * mult * surroundings);
        // Fractional emissions avoid the old visible bursts and keep texture detail
        // legible. The particle budget is separate from open-ended shader intensity.
        if (RNG.nextDouble() < expectedMotes) {
            spawnPsilocybinMote(level, player, state, strength);
        }
    }

    private static void spawnLsdParticles(ClientLevel level, LocalPlayer player, LsdEffectState state, double blendGain) {
        double strength = Math.log1p(Math.max(0.0D, state.visualIntensity() * blendGain));
        double mult = TrippyConfig.lsdParticleMultiplier * (TrippyConfig.lsdPerformanceMode ? 0.45D : 1.0D)
                * (TrippyConfig.lsdAccessibilityMode ? 0.45D : 1.0D);
        // A small fractional budget leaves surfaces readable. The old hundreds
        // of random fireworks/portal tiles per tick looked like filter flicker.
        if (RNG.nextDouble() < Math.min(0.22D, strength * mult * 0.025D)) {
            spawnLsdAmbient(level, player, state);
        }
    }

    private static void spawnDmtParticles(ClientLevel level, LocalPlayer player, DmtEffectState state, double blendGain) {
        // DMT billboard particles read as flat screen icons at high intensity. Keep this disabled until
        // a replacement can be built from spatial, world-attached geometry.
        if (dmtBillboardParticlesDisabled) {
            return;
        }
        double gain = Math.max(0.0D, Math.min(1.0D, blendGain));
        double megaDose = smoothstep(7.0D, 10.0D, state.activeDoseUnits());
        double particleBudget = 0.08D + megaDose * 0.92D;
        double mult = TrippyConfig.dmtParticleMultiplier * (TrippyConfig.dmtAccessibilityMode ? 0.42D : 1.0D) * particleBudget;
        double intensity = Math.min(state.intensity(), TrippyConfig.dmtMaxVisualIntensity);
        double launch = smoothstep(0.0D, 0.18D, state.progress()) * (1.0D - smoothstep(0.90D, 1.0D, state.progress()));
        int motes = (int) Math.round(Math.min(18 + megaDose * 86.0D,
                intensity * (3.0D + state.tier() * 1.1D + megaDose * 4.0D) * mult * launch) * gain);
        for (int i = 0; i < motes; i++) {
            spawnDmtMote(level, player, state);
        }

        double tunnel = state.tunnelDepth() + state.chrysanthemum() * 0.35D;
        int tunnelMotes = (int) Math.round(Math.min(14 + megaDose * 58.0D,
                tunnel * (1.4D + state.tier() * 0.35D + megaDose * 2.2D) * mult * launch) * gain);
        for (int i = 0; i < tunnelMotes; i++) {
            spawnDmtTunnelMote(level, player, state);
        }

        long time = level.getGameTime();
        double layerSeparation = state.spatialInstability() + state.veilThinness() + state.symbolPressure() * 0.35D;
        long latticeInterval = megaDose > 0.45D ? 3L : 7L;
        if (time % latticeInterval == 0L && layerSeparation > 0.035D) {
            int lattice = (int) Math.round(Math.min(5 + megaDose * 26.0D,
                    layerSeparation * (1.0D + state.tier() * 0.25D + megaDose * 2.4D) * mult) * gain);
            for (int i = 0; i < lattice; i++) {
                spawnDmtLatticeOrbit(level, player, state, i, time);
            }
        }
        if (time % 14L == 0L && megaDose > 0.18D && state.alienShimmer() + state.spatialInstability() > 0.030D) {
            int flashes = (int) Math.round(Math.min(2 + megaDose * 8.0D,
                    (state.alienShimmer() + state.spatialInstability()) * 0.9D * mult) * gain);
            for (int i = 0; i < flashes; i++) {
                spawnDmtSurfaceGlyphFlash(level, player, state);
            }
        }
        long gateInterval = megaDose > 0.55D ? 2L : 6L;
        if (time % gateInterval == 0L && state.stage() == DmtStage.BREAKTHROUGH_TRANSITION && state.breakthroughTransition() > 0.04D) {
            int gate = (int) Math.round(Math.min(8 + megaDose * 34.0D,
                    state.breakthroughTransition() * (2.2D + state.tier() * 0.45D + megaDose * 4.0D) * mult) * gain);
            for (int i = 0; i < gate; i++) {
                spawnDmtBoundaryGate(level, player, state, i, gate, time);
            }
        }
        if (time % 9L == 0L && (state.stage() == DmtStage.POST_BREAKTHROUGH || state.stage() == DmtStage.RETURN) && state.realityReplacement() > 0.05D) {
            int scaffolds = (int) Math.round(Math.min(4 + megaDose * 18.0D,
                    (state.impossibleArchitecture() + state.realityReplacement()) * (0.9D + state.tier() * 0.14D + megaDose * 1.8D) * mult) * gain);
            for (int i = 0; i < scaffolds; i++) {
                spawnDmtCathedralScaffold(level, player, state, i, time);
            }
        }
        if (time % 8L == 0L && state.impossibleArchitecture() > 0.04D) {
            int structures = (int) Math.round(Math.min(4 + megaDose * 16.0D,
                    state.impossibleArchitecture() * (0.8D + state.tier() * 0.22D + megaDose * 1.8D) * mult) * gain);
            for (int i = 0; i < structures; i++) {
                spawnDmtArchitectureNode(level, player, state);
            }
        }
        if (time % 18L == 0L && megaDose > 0.18D && state.symbolLanguage() + state.symbolPressure() > 0.05D) {
            int glyphs = (int) Math.round(Math.min(2 + megaDose * 10.0D,
                    (state.symbolLanguage() + state.symbolPressure()) * (0.55D + state.tier() * 0.12D + megaDose * 1.2D) * mult) * gain);
            for (int i = 0; i < glyphs; i++) {
                spawnDmtSymbol(level, player, state);
            }
        }
        if (time % 12L == 0L && state.entityPresence() + state.encounter().entityFocus() > 0.12D) {
            int presences = (int) Math.round(Math.min(1 + megaDose * 5.0D,
                    (state.entityPresence() / Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity) + state.encounter().entityFocus()) * (0.8D + megaDose * 1.6D) * mult) * gain);
            for (int i = 0; i < presences; i++) {
                spawnDmtPresenceNode(level, player, state);
            }
        }
        if (time % 15L == 0L && state.environmentalConsciousness() > 0.04D) {
            int breaths = (int) Math.round(Math.min(2 + megaDose * 8.0D,
                    state.environmentalConsciousness() * (0.9D + megaDose * 2.2D) * mult) * gain);
            for (int i = 0; i < breaths; i++) {
                spawnDmtEnvironmentalPulse(level, player, state);
            }
        }
    }

    private static void spawnPsilocybinMote(ClientLevel level, LocalPlayer player, PsilocybinEffectState state, double strength) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        double range = 4.5D + strength * 0.70D;
        double dx = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double dy = (RNG.nextDouble() - 0.60D) * 3.2D;
        double dz = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        int index = (int) Math.floorMod((state.signature().seed() ^ RNG.nextLong()) >>> 1, PSILOCYBIN_MOTES.length);
        ParticleOptions p = PSILOCYBIN_MOTES[index];
        double time = PsilocybinPresentationClock.timeTicks(0.0F);
        double spin = Math.atan2(dz, dx) + Math.PI * 0.5D + Math.sin(time * 0.003D) * 0.20D;
        double speed = 0.0012D + strength / (1.0D + strength) * 0.0005D;
        double vx = Math.cos(spin) * speed + (RNG.nextDouble() - 0.5D) * 0.002D;
        double vy = (RNG.nextDouble() - 0.40D) * 0.002D;
        double vz = Math.sin(spin) * speed + (RNG.nextDouble() - 0.5D) * 0.002D;
        level.addParticle(p, eye.x + dx, eye.y + dy, eye.z + dz, vx, vy, vz);
    }

    private static void spawnLsdAmbient(ClientLevel level, LocalPlayer player, LsdEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        double strength = Math.log1p(Math.max(0.0D, state.visualIntensity()));
        double range = 5.0D + Math.log1p(strength) * 1.4D;
        double dx = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double dy = (RNG.nextDouble() - 0.45D) * range;
        double dz = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double spin = Math.atan2(dz, dx) + Math.PI * 0.5D;
        double speed = 0.006D + Math.min(0.012D, strength * 0.002D);
        level.addParticle(ParticleTypes.GLOW, eye.x + dx, eye.y + dy, eye.z + dz,
                Math.cos(spin) * speed, 0.001D, Math.sin(spin) * speed);
    }

    private static void spawnDmtMote(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        double intensity = Math.min(state.intensity(), TrippyConfig.dmtMaxVisualIntensity);
        double range = 2.5D + state.tier() * 1.4D + state.worldDissolve() * 0.50D;
        double angle = RNG.nextDouble() * Math.PI * 2.0D;
        double radius = Math.sqrt(RNG.nextDouble()) * range;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        double dy = (RNG.nextDouble() - 0.42D) * range * (0.72D + state.tunnelDepth() * 0.04D);
        double spin = angle + Math.PI * 0.5D;
        double pull = 0.010D + intensity * 0.002D + state.tunnelDepth() * 0.003D;
        ParticleOptions particle = DMT_PALETTE[(int) Math.floorMod((state.signature().seed() ^ RNG.nextLong()) >>> 1, DMT_PALETTE.length)];
        level.addParticle(particle,
                eye.x + dx,
                eye.y + dy,
                eye.z + dz,
                Math.cos(spin) * pull + (RNG.nextDouble() - 0.5D) * 0.018D,
                (RNG.nextDouble() - 0.35D) * 0.024D,
                Math.sin(spin) * pull + (RNG.nextDouble() - 0.5D) * 0.018D);
    }

    private static void spawnDmtTunnelMote(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        double ring = 0.35D + RNG.nextDouble() * (2.5D + state.tier() * 0.55D);
        double angle = RNG.nextDouble() * Math.PI * 2.0D;
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        if (side.lengthSqr() < 0.01D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 up = side.cross(look).normalize();
        Vec3 offset = side.scale(Math.cos(angle) * ring).add(up.scale(Math.sin(angle) * ring));
        double forward = 2.0D + RNG.nextDouble() * (4.0D + state.tier() * 0.8D);
        Vec3 pos = eye.add(look.scale(forward)).add(offset);
        ParticleOptions particle = DMT_PALETTE[RNG.nextInt(DMT_PALETTE.length)];
        double speed = 0.035D + state.tunnelDepth() * 0.006D;
        level.addParticle(particle,
                pos.x,
                pos.y,
                pos.z,
                -look.x * speed + (RNG.nextDouble() - 0.5D) * 0.016D,
                -look.y * speed + (RNG.nextDouble() - 0.5D) * 0.016D,
                -look.z * speed + (RNG.nextDouble() - 0.5D) * 0.016D);
    }

    private static void spawnDmtLatticeOrbit(ClientLevel level, LocalPlayer player, DmtEffectState state, int index, long time) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        Vec3 side = perpendicularSide(look);
        Vec3 up = side.cross(look).normalize();
        double phase = state.signature().seed() * 0.0000007D + index * 2.399963D + time * (0.045D + state.signature().tunnelSpeed() * 0.018D);
        double ring = 0.55D + (index % 5) * (0.28D + state.spatialInstability() * 0.015D);
        double forward = 1.6D + (index % 7) * 0.52D + state.tier() * 0.22D;
        Vec3 radial = side.scale(Math.cos(phase)).add(up.scale(Math.sin(phase)));
        Vec3 tangent = side.scale(-Math.sin(phase)).add(up.scale(Math.cos(phase)));
        Vec3 pos = eye.add(look.scale(forward)).add(radial.scale(ring));
        double speed = 0.010D + state.spatialInstability() * 0.0025D + state.symbolPressure() * 0.0015D;
        ParticleOptions particle = (index & 3) == 0 ? ParticleTypes.PORTAL : (index & 3) == 1 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SCULK_SOUL;
        level.addParticle(particle,
                pos.x,
                pos.y,
                pos.z,
                tangent.x * speed - radial.x * speed * 0.35D,
                tangent.y * speed - radial.y * speed * 0.35D,
                tangent.z * speed - radial.z * speed * 0.35D);
    }

    private static void spawnDmtSurfaceGlyphFlash(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        BlockPos origin = player.blockPosition();
        int radius = 2 + Math.min(6, state.tier());
        BlockPos pos = origin.offset(RNG.nextInt(radius * 2 + 1) - radius, RNG.nextInt(radius + 4) - 1, RNG.nextInt(radius * 2 + 1) - radius);
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir() || !blockState.isSolidRender(level, pos)) {
            return;
        }
        Direction face = Direction.values()[RNG.nextInt(Direction.values().length)];
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        double x = pos.getX() + (face.getStepX() > 0 ? 1.012D : face.getStepX() < 0 ? -0.012D : RNG.nextDouble());
        double y = pos.getY() + (face.getStepY() > 0 ? 1.012D : face.getStepY() < 0 ? -0.012D : RNG.nextDouble());
        double z = pos.getZ() + (face.getStepZ() > 0 ? 1.012D : face.getStepZ() < 0 ? -0.012D : RNG.nextDouble());
        double pulse = 0.006D + state.alienShimmer() * 0.0025D + state.symbolPressure() * 0.0020D;
        ParticleOptions particle = RNG.nextBoolean()
                ? new BlockParticleOption(ParticleTypes.BLOCK, blockState)
                : DMT_PALETTE[(int) Math.floorMod((state.signature().seed() ^ RNG.nextLong()) >>> 1, DMT_PALETTE.length)];
        level.addParticle(particle,
                x,
                y,
                z,
                normal.x * pulse + (RNG.nextDouble() - 0.5D) * 0.004D,
                normal.y * pulse + (RNG.nextDouble() - 0.5D) * 0.004D,
                normal.z * pulse + (RNG.nextDouble() - 0.5D) * 0.004D);
    }

    private static void spawnDmtBoundaryGate(ClientLevel level, LocalPlayer player, DmtEffectState state, int index, int count, long time) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        Vec3 side = perpendicularSide(look);
        Vec3 up = side.cross(look).normalize();
        double countSafe = Math.max(1, count);
        double angle = index / countSafe * Math.PI * 2.0D + time * (0.065D + state.signature().tunnelSpeed() * 0.026D);
        double radius = 0.55D + (index % 6) * 0.20D + state.breakthroughTransition() * 0.18D;
        double forward = 2.2D + Math.sin(time * 0.031D + index) * 0.25D + state.cameraPull() * 1.2D;
        Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        Vec3 pos = eye.add(look.scale(forward)).add(radial.scale(radius));
        double pull = 0.026D + state.breakthroughTransition() * 0.008D;
        ParticleOptions particle = index % 3 == 0 ? ParticleTypes.REVERSE_PORTAL : index % 3 == 1 ? ParticleTypes.DRAGON_BREATH : ParticleTypes.SCULK_SOUL;
        level.addParticle(particle,
                pos.x,
                pos.y,
                pos.z,
                -radial.x * pull - look.x * pull * 0.42D,
                -radial.y * pull - look.y * pull * 0.42D,
                -radial.z * pull - look.z * pull * 0.42D);
    }

    private static void spawnDmtCathedralScaffold(ClientLevel level, LocalPlayer player, DmtEffectState state, int index, long time) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        Vec3 side = perpendicularSide(look);
        Vec3 up = side.cross(look).normalize();
        int lane = Math.floorMod(index + (int) (state.signature().seed() >>> 5), 9) - 4;
        int rung = Math.floorMod(index * 3 + (int) (state.signature().seed() >>> 13), 7) - 3;
        double cell = 0.48D + state.impossibleArchitecture() * 0.06D + state.realityReplacement() * 0.12D;
        double forward = 3.0D + Math.floorMod(index * 5, 11) * 0.44D + Math.sin(time * 0.017D + index) * 0.22D;
        Vec3 pos = eye.add(look.scale(forward)).add(side.scale(lane * cell)).add(up.scale(rung * cell));
        Vec3 toCenter = eye.add(look.scale(forward + 1.0D)).subtract(pos);
        if (toCenter.lengthSqr() > 0.001D) {
            toCenter = toCenter.normalize();
        }
        double speed = 0.008D + state.realityReplacement() * 0.003D;
        ParticleOptions particle = index % 4 == 0 ? ParticleTypes.PORTAL : index % 4 == 1 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SCULK_SOUL;
        level.addParticle(particle, pos.x, pos.y, pos.z, toCenter.x * speed, toCenter.y * speed, toCenter.z * speed);
    }

    private static void spawnDmtArchitectureNode(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        Vec3 side = perpendicularSide(look);
        Vec3 up = side.cross(look).normalize();
        long seed = state.signature().seed() ^ (long) state.ageTicks() * 0x9E3779B97F4A7C15L;
        int lane = Math.floorMod((int) (seed + RNG.nextInt(17)), 7) - 3;
        int rung = Math.floorMod((int) (seed >>> 8) + RNG.nextInt(19), 9) - 4;
        double forward = 3.5D + RNG.nextDouble() * (7.0D + state.tier() * 1.2D);
        double grid = 0.55D + state.impossibleArchitecture() * 0.18D;
        Vec3 pos = eye.add(look.scale(forward))
                .add(side.scale(lane * grid + (RNG.nextDouble() - 0.5D) * 0.16D))
                .add(up.scale(rung * grid + (RNG.nextDouble() - 0.5D) * 0.16D));
        ParticleOptions particle = RNG.nextBoolean() ? ParticleTypes.PORTAL : ParticleTypes.REVERSE_PORTAL;
        double pull = 0.006D + state.impossibleArchitecture() * 0.003D;
        level.addParticle(particle, pos.x, pos.y, pos.z, -look.x * pull, -look.y * pull, -look.z * pull);
    }

    private static void spawnDmtSymbol(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        Vec3 side = perpendicularSide(look);
        Vec3 up = side.cross(look).normalize();
        double forward = 1.8D + RNG.nextDouble() * (5.0D + state.tier());
        double spread = 1.0D + state.symbolLanguage() * 0.35D;
        Vec3 pos = eye.add(look.scale(forward))
                .add(side.scale((RNG.nextDouble() - 0.5D) * spread * 4.5D))
                .add(up.scale((RNG.nextDouble() - 0.5D) * spread * 2.8D));
        ParticleOptions particle = DMT_PALETTE[(int) Math.floorMod((state.signature().seed() ^ RNG.nextLong()) >>> 1, DMT_PALETTE.length)];
        double drift = 0.004D + state.symbolLanguage() * 0.002D;
        level.addParticle(particle,
                pos.x,
                pos.y,
                pos.z,
                (RNG.nextDouble() - 0.5D) * drift,
                (RNG.nextDouble() - 0.5D) * drift,
                (RNG.nextDouble() - 0.5D) * drift);
    }

    private static void spawnDmtPresenceNode(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        Vec3 look = spawnLook != null ? spawnLook : player.getLookAngle();
        Vec3 side = perpendicularSide(look);
        Vec3 up = side.cross(look).normalize();
        double sideBias = RNG.nextBoolean() ? -1.0D : 1.0D;
        double forward = 2.8D + RNG.nextDouble() * 4.0D;
        double height = 0.35D + RNG.nextDouble() * 1.2D;
        Vec3 anchor = eye.add(look.scale(forward)).add(side.scale(sideBias * (1.1D + RNG.nextDouble() * 1.8D))).add(up.scale((RNG.nextDouble() - 0.3D) * 0.9D));
        ParticleOptions particle = RNG.nextBoolean() ? ParticleTypes.SCULK_SOUL : ParticleTypes.REVERSE_PORTAL;
        Vec3 pos = anchor.add(up.scale((RNG.nextDouble() - 0.5D) * height));
        double attention = 0.010D + state.encounter().entityFocus() * 0.006D;
        level.addParticle(particle, pos.x, pos.y, pos.z, -look.x * attention, -look.y * attention + 0.004D, -look.z * attention);
    }

    private static void spawnDmtEnvironmentalPulse(ClientLevel level, LocalPlayer player, DmtEffectState state) {
        BlockPos origin = player.blockPosition();
        int radius = 2 + Math.min(5, state.tier());
        BlockPos pos = origin.offset(RNG.nextInt(radius * 2 + 1) - radius, RNG.nextInt(radius + 3) - 1, RNG.nextInt(radius * 2 + 1) - radius);
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir() || !blockState.isSolidRender(level, pos)) {
            return;
        }
        double pulse = 0.006D + state.environmentalConsciousness() * 0.006D;
        double x = pos.getX() + RNG.nextDouble();
        double y = pos.getY() + 0.15D + RNG.nextDouble() * 0.85D;
        double z = pos.getZ() + RNG.nextDouble();
        Vec3 away = new Vec3(x - player.getX(), y - player.getEyeY(), z - player.getZ());
        if (away.lengthSqr() > 0.01D) {
            away = away.normalize();
        }
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                x,
                y,
                z,
                away.x * pulse + (RNG.nextDouble() - 0.5D) * 0.010D,
                Math.abs(away.y) * pulse + 0.006D,
                away.z * pulse + (RNG.nextDouble() - 0.5D) * 0.010D);
    }

    private static void spawnLsdLiquidCurrent(ClientLevel level, LocalPlayer player, LsdEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        double range = 3.5D + state.tier() * 1.3D;
        double dx = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double dz = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double yBias = state.environment().water() > 0.18D ? -0.75D : -0.25D;
        double dy = yBias + (RNG.nextDouble() - 0.35D) * range * 0.70D;
        double upward = state.environment().nether() * 0.080D + state.liquidFlow() * 0.028D;
        ParticleOptions particle = LSD_LIQUID_PALETTE[RNG.nextInt(LSD_LIQUID_PALETTE.length)];
        level.addParticle(particle, eye.x + dx, eye.y + dy, eye.z + dz,
                (RNG.nextDouble() - 0.5D) * 0.045D,
                upward + RNG.nextDouble() * 0.035D,
                (RNG.nextDouble() - 0.5D) * 0.045D);
    }

    private static void spawnFloatingBlockFragment(ClientLevel level, LocalPlayer player, LsdEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        int radius = 3 + Math.min(7, state.tier() * 2);
        BlockPos origin = player.blockPosition();
        BlockPos pos = origin.offset(RNG.nextInt(radius * 2 + 1) - radius, RNG.nextInt(radius + 3) - 1, RNG.nextInt(radius * 2 + 1) - radius);
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) {
            return;
        }
        double lift = 0.05D + state.blockAutonomy() * 0.020D + RNG.nextDouble() * 0.025D;
        ParticleOptions particle = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
        level.addParticle(particle,
                pos.getX() + RNG.nextDouble(),
                pos.getY() + 0.15D + RNG.nextDouble() * 0.85D,
                pos.getZ() + RNG.nextDouble(),
                (RNG.nextDouble() - 0.5D) * 0.026D + (eye.x - pos.getX()) * state.realityFold() * 0.0008D,
                lift,
                (RNG.nextDouble() - 0.5D) * 0.026D + (eye.z - pos.getZ()) * state.realityFold() * 0.0008D);
    }

    private static void spawnLsdMeltDrip(ClientLevel level, LocalPlayer player, LsdEffectState state) {
        BlockPos origin = player.blockPosition();
        int radius = 2 + Math.min(6, state.tier());
        BlockPos pos = origin.offset(RNG.nextInt(radius * 2 + 1) - radius, RNG.nextInt(radius + 2) - 1, RNG.nextInt(radius * 2 + 1) - radius);
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir() || !blockState.isSolidRender(level, pos)) {
            return;
        }
        double x = pos.getX() + RNG.nextDouble();
        double y = pos.getY() + 0.78D + RNG.nextDouble() * 0.26D;
        double z = pos.getZ() + RNG.nextDouble();
        double sway = 0.010D + state.liquidFlow() * 0.010D;
        double vy = -0.020D - state.geometryMelt() * 0.006D + state.environment().nether() * 0.010D;
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                x,
                y,
                z,
                (RNG.nextDouble() - 0.5D) * sway,
                vy,
                (RNG.nextDouble() - 0.5D) * sway);
    }

    private static void spawnLsdDepthSpark(ClientLevel level, LocalPlayer player, LsdEffectState state) {
        Vec3 eye = spawnEye != null ? spawnEye : player.getEyePosition();
        double range = 2.5D + state.tier() * 1.2D;
        double dx = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double dy = (RNG.nextDouble() - 0.4D) * range * 0.9D;
        double dz = (RNG.nextDouble() - 0.5D) * range * 2.0D;
        double spin = Math.atan2(dz, dx);
        double speed = 0.008D + state.skyVortex() * 0.010D;
        ParticleOptions particle = RNG.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.SCULK_SOUL;
        level.addParticle(particle,
                eye.x + dx,
                eye.y + dy,
                eye.z + dz,
                Math.cos(spin + Math.PI * 0.5D) * speed,
                (RNG.nextDouble() - 0.5D) * 0.010D,
                Math.sin(spin + Math.PI * 0.5D) * speed);
    }

    private static Vec3 perpendicularSide(Vec3 look) {
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 0.01D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return side.normalize();
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
