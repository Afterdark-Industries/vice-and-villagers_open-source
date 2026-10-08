package com.trippycraft.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.network.TavernAmbiencePacket;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.mescaline.MescalineEffectState;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Interior spotlights whose color, movement, and bloom react to the active experience graph. */
public final class TavernRaveRenderer {
    private static final double TAU = Math.PI * 2.0D;
    private static float lastAnimationTime = Float.NaN;
    private static double choreographyPhase;
    private static double smoothedMusicDrive;
    private static ClientLevel vrFrameLevel;
    private static double vrFrameTime = Double.NaN;
    private static ClientTavernAmbience.MusicStructure vrMusic = ClientTavernAmbience.MusicStructure.silent();
    private static double vrBeat;
    private static final Vec3[] FIXTURES = {
            new Vec3(3.5D, 4.08D, 4.5D),
            new Vec3(6.5D, 4.08D, 4.5D),
            new Vec3(3.5D, 4.08D, 7.5D),
            new Vec3(6.5D, 4.08D, 7.5D)
    };

    private TavernRaveRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !TrippyClientConfig.rendererEnabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        TavernAmbiencePacket signal = ClientTavernAmbience.currentSignal();
        if (level == null || minecraft.player == null || !signal.active()) {
            return;
        }

        Vec3 barCenter = transformed(signal, new Vec3(4.5D, 2.0D, 7.0D));
        if (minecraft.player.position().distanceToSqr(barCenter) > 48.0D * 48.0D) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        ClientEffectSample sample = ClientEffectGraph.sample(partialTick);
        ClientRenderSignature signature = sample.signature();
        MdmaEffectState mdma = ClientExperienceStore.mdma();
        MescalineEffectState mescaline = ClientExperienceStore.mescaline();
        boolean advance = advanceChoreography(level);
        boolean vr = VivecraftCompat.isVrActive();
        if (vr && advance) {
            vrMusic = ClientTavernAmbience.musicStructure(partialTick);
            vrBeat = ClientTavernAmbience.bassPulse(partialTick);
        }
        ClientTavernAmbience.MusicStructure structure = vr ? vrMusic : ClientTavernAmbience.musicStructure(partialTick);
        double beat = vr ? vrBeat : ClientTavernAmbience.bassPulse(partialTick);
        double mdmaBeat = mdma.active() && mdma.peakWindow()
                ? beat * mdma.bassSensitivity()
                : beat * mdma.bassSensitivity() * 0.35D;
        double trip = clamp(
                signature.psychedelic() * 0.70D
                        + signature.empathogen() * 0.45D
                        + signature.stimulant() * 0.28D
                        + sample.colorBloom() * 0.24D
                        + sample.patterning() * 0.18D
                        + sample.euphoria() * 0.16D
                        + signature.depthSeparation() * 0.18D
                        + mdma.saturation() * 0.24D
                        + mdma.bloom() * 0.22D
        );
        double visualScale = clamp(TrippyClientConfig.visualIntensity / 1.35D);
        double motionScale = clamp(TrippyClientConfig.motionIntensity / 1.35D);
        if (TrippyClientConfig.motionReduction) {
            motionScale *= 0.38D;
        }

        double mescalinePresence = mescaline.active() ? clamp(mescaline.visualPresence()) : 0.0D;
        double longMusicDrive = structure.bassExpansion() * 0.25D + structure.harmonicShift() * 0.20D
                + structure.melodicFlow() * 0.22D + structure.sustainedRibbon() * 0.20D
                + structure.saxRibbon() * 0.28D;
        double rawMusicDrive = beat * (0.10D + trip * 0.14D) + mdmaBeat * 0.62D
                + longMusicDrive * mescalinePresence * 0.46D;
        if (TrippyClientConfig.photosensitivityReduction) {
            rawMusicDrive *= 0.10D;
        }
        double musicDrive = advance
                ? updateChoreography(time, rawMusicDrive, trip, sample.stimulation(), sample.sedation(), mescalinePresence)
                : smoothedMusicDrive;
        double sweep = (TrippyClientConfig.motionReduction ? 0.34D : 1.04D + trip * 0.46D + musicDrive * 0.52D) * motionScale;
        int baseAlpha = TrippyClientConfig.photosensitivityReduction
                ? 30
                : (int) Math.round((44.0D + trip * 48.0D + musicDrive * 46.0D) * (0.72D + visualScale * 0.28D));

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        try (ByteBufferBuilder buffer = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(buffer);
            VertexConsumer beams = buffers.getBuffer(RenderType.lightning());
            PoseStack.Pose pose = poseStack.last();

            for (int index = 0; index < FIXTURES.length; index++) {
                double phase = index * TAU / FIXTURES.length;
                double temporal = signature.temporalSlip() * Math.sin(choreographyPhase * 0.42D + phase) * 0.34D;
                double radiusX = sweep * (0.82D + signature.depthSeparation() * 0.38D);
                double radiusZ = sweep * (0.62D + sample.visualDrift() * 0.12D);
                double horizontalArc = choreographyPhase + phase + temporal;
                double depthArc = choreographyPhase * 0.72D + phase * 1.37D - temporal;
                Vec3 localTarget = new Vec3(
                        4.5D + Math.sin(horizontalArc + structure.melodicFlow() * mescalinePresence * 0.38D) * radiusX,
                        1.035D,
                        6.5D + Math.cos(depthArc - structure.sustainedRibbon() * mescalinePresence * 0.44D) * radiusZ
                );
                Vec3 source = transformed(signal, FIXTURES[index]).subtract(camera);
                Vec3 target = transformed(signal, localTarget).subtract(camera);

                float hue = (float) ((time * 0.00045D + choreographyPhase * 0.045D
                        + index * 0.245D
                        + signature.patternOrder() * 0.14D
                        + sample.colorBloom() * 0.08D) % 1.0D);
                int rgb = Color.HSBtoRGB(hue, (float) (0.82D + trip * 0.18D), 1.0F);
                int red = rgb >> 16 & 0xFF;
                int green = rgb >> 8 & 0xFF;
                int blue = rgb & 0xFF;
                if (mescalinePresence > 0.0D) {
                    int[] cathedralPalette = {0xF2A331, 0x18A99D, 0x1A6335, 0xA62975, 0xE75B20, 0x4D2A78};
                    int ancient = cathedralPalette[Math.floorMod(index + (int) Math.floor(structure.phrasePhase() * 6.0D), cathedralPalette.length)];
                    double paletteBlend = mescalinePresence * (0.52D + structure.harmonicShift() * 0.28D);
                    red = (int) Math.round(red + (((ancient >> 16) & 0xFF) - red) * paletteBlend);
                    green = (int) Math.round(green + (((ancient >> 8) & 0xFF) - green) * paletteBlend);
                    blue = (int) Math.round(blue + ((ancient & 0xFF) - blue) * paletteBlend);
                }
                double pulse = TrippyClientConfig.photosensitivityReduction
                        ? 0.84D
                        : 0.84D + Math.sin(choreographyPhase * 0.82D + phase) * 0.08D + musicDrive * 0.34D;
                int alpha = (int) Math.round(clamp(baseAlpha * pulse, 18.0D, 132.0D));
                renderBeam(beams, pose, source, target, 0.065D, 0.42D + trip * 0.24D + musicDrive * 0.16D, red, green, blue, alpha);
                HoMetTrailRenderer.captureBeam(index, source.add(camera), target.add(camera),
                        red << 16 | green << 8 | blue, time);
                if (mescalinePresence > 0.08D) {
                    int ribbonAlpha = (int) Math.round(alpha * mescalinePresence
                            * (0.10D + structure.sustainedRibbon() * 0.12D + structure.saxRibbon() * 0.18D));
                    renderBeam(beams, pose, source, target, 0.12D, 0.76D + structure.bassExpansion() * 0.24D,
                            red, green, blue, Math.max(3, ribbonAlpha));
                }
                renderFloorGlow(beams, pose, target, 0.46D + trip * 0.24D + musicDrive * 0.20D, red, green, blue, Math.max(10, alpha / 3));
            }
            buffers.endBatch();
        }
    }

    private static Vec3 transformed(TavernAmbiencePacket signal, Vec3 local) {
        return StructureTemplate.transform(local, Mirror.NONE, signal.rotation(), BlockPos.ZERO)
                .add(signal.origin().getX(), signal.origin().getY(), signal.origin().getZ());
    }

    private static boolean advanceChoreography(ClientLevel level) {
        if (!VivecraftCompat.isVrActive()) {
            vrFrameLevel = null;
            vrFrameTime = Double.NaN;
            return true;
        }
        double frame = (double) level.getGameTime()
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        if (vrFrameLevel == level && Double.isFinite(vrFrameTime)
                && (!VivecraftCompat.isFirstWorldPass() || frame == vrFrameTime)) {
            return false;
        }
        vrFrameLevel = level;
        vrFrameTime = frame;
        return true;
    }

    private static double updateChoreography(float time, double rawMusicDrive, double trip, double stimulation, double sedation,
                                             double mescalinePresence) {
        float delta = Float.isFinite(lastAnimationTime) ? time - lastAnimationTime : 0.0F;
        if (!Float.isFinite(delta) || delta < 0.0F || delta > 20.0F) {
            delta = 0.0F;
            choreographyPhase = time * 0.0120D;
            smoothedMusicDrive = 0.0D;
        }
        double targetDrive = clamp(rawMusicDrive);
        double timeConstant = targetDrive > smoothedMusicDrive ? 1.8D : 7.5D;
        double smoothing = delta <= 0.0F ? 1.0D : 1.0D - Math.exp(-delta / timeConstant);
        smoothedMusicDrive += (targetDrive - smoothedMusicDrive) * smoothing;

        // A broad cinematic sweep at rest, with measured acceleration on strong low-frequency hits.
        // This sits between the original near-strobe motion and the overly languid first retune.
        double speed = 0.0155D + trip * 0.0070D + clamp(stimulation) * 0.0040D + smoothedMusicDrive * 0.032D;
        speed *= 1.0D - clamp(sedation) * 0.30D;
        speed *= 1.0D - mescalinePresence * 0.34D;
        if (TrippyClientConfig.motionReduction) {
            speed *= 0.32D;
        }
        choreographyPhase = Math.floorMod((long) Math.floor((choreographyPhase + delta * speed) * 1_000_000.0D),
                (long) Math.floor(TAU * 1_000_000.0D)) / 1_000_000.0D;
        lastAnimationTime = time;
        return smoothedMusicDrive;
    }

    private static void renderBeam(VertexConsumer consumer,
                                   PoseStack.Pose pose,
                                   Vec3 source,
                                   Vec3 target,
                                   double sourceRadius,
                                   double targetRadius,
                                   int red,
                                   int green,
                                   int blue,
                                   int alpha) {
        Vec3 direction = target.subtract(source);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        Vec3 reference = Math.abs(direction.y) < 0.92D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = direction.cross(reference).normalize();
        Vec3 around = direction.cross(side).normalize();
        Vec3[] sourceRing = new Vec3[4];
        Vec3[] targetRing = new Vec3[4];
        for (int index = 0; index < 4; index++) {
            double angle = index * TAU / 4.0D + Math.PI / 4.0D;
            Vec3 offset = side.scale(Math.cos(angle)).add(around.scale(Math.sin(angle)));
            sourceRing[index] = source.add(offset.scale(sourceRadius));
            targetRing[index] = target.add(offset.scale(targetRadius));
        }
        for (int index = 0; index < 4; index++) {
            int next = (index + 1) % 4;
            vertex(consumer, pose, sourceRing[index], red, green, blue, alpha);
            vertex(consumer, pose, sourceRing[next], red, green, blue, alpha);
            vertex(consumer, pose, targetRing[next], red, green, blue, 2);
            vertex(consumer, pose, targetRing[index], red, green, blue, 2);
        }
    }

    private static void renderFloorGlow(VertexConsumer consumer,
                                        PoseStack.Pose pose,
                                        Vec3 center,
                                        double radius,
                                        int red,
                                        int green,
                                        int blue,
                                        int alpha) {
        vertex(consumer, pose, center.add(-radius, 0.002D, -radius), red, green, blue, alpha);
        vertex(consumer, pose, center.add(-radius, 0.002D, radius), red, green, blue, alpha);
        vertex(consumer, pose, center.add(radius, 0.002D, radius), red, green, blue, alpha);
        vertex(consumer, pose, center.add(radius, 0.002D, -radius), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer,
                               PoseStack.Pose pose,
                               Vec3 point,
                               int red,
                               int green,
                               int blue,
                               int alpha) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha);
    }

    private static double clamp(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
