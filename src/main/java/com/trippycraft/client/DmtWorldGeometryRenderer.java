package com.trippycraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class DmtWorldGeometryRenderer {
    private static final double TAU = Math.PI * 2.0D;
    private static final double EPSILON = 0.0035D;
    private static double persistentGeometryPresence;
    private static long lastPersistentSeed = Long.MIN_VALUE;
    private static long lastPersistentGameTime = Long.MIN_VALUE;
    private static Vec3 smoothedForward;
    private static ClientLevel vrGeometryLevel;
    private static double vrGeometryFrame = Double.NaN;

    private DmtWorldGeometryRenderer() {
    }

    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        if (VivecraftCompat.isVrActive()) {
            return; // World geometry remains visible when Vivecraft hides outlines.
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        renderWorldMorphs(minecraft, level, player, event.getPoseStack(), event.getCamera().getPosition(),
                event.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    private static void renderWorldMorphs(Minecraft minecraft, ClientLevel level, LocalPlayer player,
            PoseStack poseStack, Vec3 camera, float partialTick) {

        DmtEffectState state = ClientExperienceStore.dmt();
        if (!state.active() || !state.hasAuthoredEffects() || !TrippyConfig.enableDmtEffects || state.intensity() <= 0.03D) {
            return;
        }

        PsychedelicBlendState blend = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(), ClientExperienceStore.lsd(), state, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        double gain = clamp(blend.dmtVisualGain());
        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double blockWarp = clamp((state.alienShimmer() * 0.32D
                + state.spatialInstability() * 0.58D
                + state.veilThinness() * 0.46D
                + state.breakthroughTransition() * 0.55D
                + state.realityReplacement() * 0.62D) * gain / safe);
        double architecture = clamp((state.impossibleArchitecture() * 0.66D
                + state.symbolLanguage() * 0.22D
                + state.realityReplacement() * 0.74D
                + state.encounter().architectureFocus() * 0.38D) * gain);
        double replacement = clamp((state.realityReplacement() * 0.74D
                + state.breakthroughTransition() * 0.24D
                + stageReplacementBias(state) * 0.42D) * gain);
        if (blockWarp <= 0.015D && architecture <= 0.015D && replacement <= 0.015D) {
            return;
        }

        float time = level.getGameTime() + partialTick;
        try (ByteBufferBuilder buffer = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(buffer);
            VertexConsumer quads = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            renderNearbyBlockMorphs(quads, poseStack, minecraft, level, player, camera, state, time, blockWarp, replacement);
            buffers.endBatch();
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (VivecraftCompat.isVrActive() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        renderPersistentGeometry(minecraft, level, player, event.getPoseStack(), event.getMultiBufferSource(),
                player.getEyePosition(), event.getPartialTick());
    }

    public static void onVrRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VivecraftCompat.isVrWorldPass()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        DmtEffectState state = ClientExperienceStore.dmt();
        if (!TrippyConfig.enableDmtEffects) {
            resetPersistentGeometry();
            return;
        }
        if ((!state.active() || !state.hasAuthoredEffects() || state.intensity() <= 0.03D)
                && persistentGeometryPresence <= 0.006D) {
            return;
        }
        // Realm objects belong to world space, not either tracked hand. Install
        // this pass's view explicitly and flush before restoring the caller's
        // state. Multiplying the event pose by this matrix would apply the view
        // twice: LevelRenderer already installs it in RenderSystem at this stage.
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        try (ByteBufferBuilder buffer = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(buffer);
            renderWorldMorphs(minecraft, minecraft.level, minecraft.player, new PoseStack(),
                    event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            renderPersistentGeometry(minecraft, minecraft.level, minecraft.player, new PoseStack(), buffers,
                    event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            buffers.endBatch();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void renderPersistentGeometry(Minecraft minecraft, ClientLevel level, LocalPlayer player,
            PoseStack poseStack, MultiBufferSource buffers, Vec3 camera, float partialTick) {

        DmtEffectState state = ClientExperienceStore.dmt();
        if (!TrippyConfig.enableDmtEffects) {
            resetPersistentGeometry();
            return;
        }

        PsychedelicBlendState blend = CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(), ClientExperienceStore.lsd(), state, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        double gain = clamp(blend.dmtVisualGain());
        boolean active = state.active() && state.hasAuthoredEffects() && state.intensity() > 0.03D;
        double architecture = active ? clamp((state.impossibleArchitecture() * 0.66D
                + state.symbolLanguage() * 0.22D
                + state.realityReplacement() * 0.74D
                + state.encounter().architectureFocus() * 0.38D) * gain) : 0.0D;
        double replacement = active ? clamp((state.realityReplacement() * 0.74D
                + state.breakthroughTransition() * 0.24D
                + stageReplacementBias(state) * 0.42D) * gain) : 0.0D;
        double targetPresence = active ? clamp(architecture * 0.62D + replacement * 0.82D + state.breakthroughTransition() * gain * 0.16D) : 0.0D;
        boolean advance = advancePersistentGeometry(level);
        double presence = advance ? updatePersistentGeometryPresence(level, state, targetPresence)
                : smoothstep(0.0D, 1.0D, persistentGeometryPresence);
        if (presence <= 0.012D) {
            return;
        }

        float time = level.getGameTime() + partialTick;
        CameraBasis basis = persistentBasis(level, player, state, presence, advance);
        VertexConsumer quads = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        double renderArchitecture = Math.max(architecture, presence * 0.46D) * (0.72D + presence * 0.28D);
        double renderReplacement = Math.max(replacement, presence * 0.34D) * (0.74D + presence * 0.26D);

        renderFractalRealm(quads, poseStack, minecraft, level, player, camera, state, time,
                renderArchitecture * 0.58D, renderReplacement * 0.56D, basis, presence);
        renderPersistentCollapseMachines(quads, poseStack, minecraft, level, player, camera, state, time,
                renderArchitecture, renderReplacement, basis, presence);
    }

    private static void renderNearbyBlockMorphs(
            VertexConsumer quads,
            PoseStack poseStack,
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            Vec3 camera,
            DmtEffectState state,
            float time,
            double blockWarp,
            double replacement
    ) {
        int radius = TrippyConfig.dmtAccessibilityMode ? 3 : 4 + Math.min(3, state.tier());
        int verticalDown = Math.min(3, radius);
        int verticalUp = Math.min(4, radius + 1);
        int budget = TrippyConfig.dmtAccessibilityMode
                ? (int) Math.round(12.0D + blockWarp * 18.0D)
                : (int) Math.round(24.0D + blockWarp * 48.0D + replacement * 34.0D);
        BlockPos origin = player.blockPosition();
        Vec3 eye = effectEye(player);
        Vec3 look = effectLook(player);
        int rendered = 0;
        for (int y = -verticalDown; y <= verticalUp && rendered < budget; y++) {
            for (int x = -radius; x <= radius && rendered < budget; x++) {
                for (int z = -radius; z <= radius && rendered < budget; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState blockState = level.getBlockState(pos);
                    if (!isMorphableBlock(level, blockState, pos)) {
                        continue;
                    }
                    Vec3 center = Vec3.atCenterOf(pos);
                    Vec3 toBlock = center.subtract(eye);
                    double distance = toBlock.length();
                    if (distance < 0.75D || distance > radius + 3.5D) {
                        continue;
                    }
                    double view = toBlock.normalize().dot(look);
                    if (view < -0.24D && replacement < 0.48D) {
                        continue;
                    }
                    int hash = hash(pos.getX(), pos.getY(), pos.getZ(), (int) state.signature().seed());
                    double select = unsignedUnit(hash);
                    double distanceFade = 1.0D - smoothstep(radius * 0.58D, radius + 3.2D, distance);
                    double selectionGate = 0.20D + blockWarp * 0.52D + replacement * 0.28D;
                    if (select > selectionGate * distanceFade + replacement * 0.12D) {
                        continue;
                    }
                    renderMorphingBlock(quads, poseStack, minecraft, level, camera, state, pos, blockState, time, blockWarp * distanceFade, replacement, hash);
                    rendered++;
                }
            }
        }
    }

    private static void renderMorphingBlock(
            VertexConsumer quads,
            PoseStack poseStack,
            Minecraft minecraft,
            ClientLevel level,
            Vec3 camera,
            DmtEffectState state,
            BlockPos pos,
            BlockState blockState,
            float time,
            double blockWarp,
            double replacement,
            int blockHash
    ) {
        TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModel(blockState).getParticleIcon();
        if (sprite == null) {
            return;
        }
        int baseColor = fallbackTint(minecraft, level, blockState, pos);
        int layers = 1 + (replacement > 0.28D ? 1 : 0) + (replacement > 0.62D && !TrippyConfig.dmtAccessibilityMode ? 1 : 0);
        int divisions = Math.max(1, Math.min(TrippyConfig.dmtAccessibilityMode ? 3 : 6,
                1 + (int) Math.round(blockWarp * 4.0D + replacement * 3.0D)));
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        PoseStack.Pose pose = poseStack.last();
        for (int layer = 0; layer < layers; layer++) {
            double layerDepth = (layer + 1) / (double) layers;
            for (Direction face : Direction.values()) {
                boolean exposed = level.getBlockState(pos.relative(face)).isAir();
                if (!exposed && replacement < 0.36D && blockWarp < 0.42D) {
                    continue;
                }
                int faceHash = hash(blockHash, face.ordinal(), layer, 0xD171E);
                double faceGate = unsignedUnit(faceHash);
                if (!exposed && faceGate > replacement * 0.58D) {
                    continue;
                }
                int light = maxLight(LevelRenderer.getLightColor(level, pos), LevelRenderer.getLightColor(level, pos.relative(face)));
                int red = mixColor(red(baseColor), state.signature().primaryRed(), 0.12D + replacement * 0.18D);
                int green = mixColor(green(baseColor), state.signature().accentGreen(), 0.10D + state.symbolPressure() * 0.040D);
                int blue = mixColor(blue(baseColor), state.signature().accentBlue(), 0.13D + replacement * 0.20D);
                int alpha = clampColor((int) Math.round((28.0D + blockWarp * 54.0D + replacement * 42.0D) * (1.0D - layer * 0.22D)));
                renderWarpedFace(quads, pose, state, pos, face, sprite, time, divisions, blockWarp, replacement, layerDepth, faceHash, red, green, blue, alpha, light);
            }
        }
        if (blockWarp + replacement > 0.18D) {
            renderBlockFragments(quads, pose, sprite, state, time, blockWarp, replacement, blockHash, baseColor, LevelRenderer.getLightColor(level, pos));
        }
        poseStack.popPose();
    }

    private static void renderBlockFragments(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            DmtEffectState state,
            float time,
            double blockWarp,
            double replacement,
            int blockHash,
            int baseColor,
            int light
    ) {
        int fragments = Math.min(TrippyConfig.dmtAccessibilityMode ? 5 : 14, Math.max(3, (int) Math.round(3.0D + blockWarp * 8.0D + replacement * 10.0D)));
        for (int i = 0; i < fragments; i++) {
            int h = hash(blockHash, i * 37, 0xF12A, 0xD71);
            double u = unsignedUnit(h);
            double v = unsignedUnit(hash(h, i * 19, 0xBEEF));
            Direction face = Direction.values()[Math.floorMod(h >>> 5, Direction.values().length)];
            Vec3 normal = normal(face);
            Vec3 tu = tangentU(face);
            Vec3 tv = tangentV(face);
            double phase = (h & 2047) * 0.0061D + time * (0.042D + state.signature().tunnelSpeed() * 0.016D);
            double emergence = (0.08D + blockWarp * 0.32D + replacement * 0.55D) * (0.45D + Math.sin(phase) * 0.20D + unsignedUnit(h >>> 3) * 0.35D);
            Vec3 surface = facePoint(face, u, v).add(normal.scale(EPSILON + emergence));
            Vec3 swirl = tu.scale(Math.sin(phase * 1.7D) * emergence * 0.40D)
                    .add(tv.scale(Math.cos(phase * 1.3D) * emergence * 0.36D));
            Vec3 center = surface.add(swirl);
            double size = 0.035D + unsignedUnit(h >>> 7) * 0.060D + replacement * 0.050D;
            double stretch = 0.50D + unsignedUnit(h >>> 13) * 1.4D + replacement * 1.10D;
            int red = mixColor(red(baseColor), state.signature().primaryRed(), 0.10D + replacement * 0.18D);
            int green = mixColor(green(baseColor), state.signature().primaryGreen(), 0.08D + blockWarp * 0.10D);
            int blue = mixColor(blue(baseColor), state.signature().accentBlue(), 0.12D + replacement * 0.16D);
            int alpha = clampColor((int) Math.round(34.0D + blockWarp * 54.0D + replacement * 38.0D));
            Vec3 ru = tu.scale(Math.cos(phase)).add(tv.scale(Math.sin(phase))).normalize();
            Vec3 rv = normal.cross(ru).normalize();
            drawOrientedBox(quads, pose, sprite, center, ru, rv, normal, size * stretch, size, size * (0.65D + replacement * 0.80D), red, green, blue, alpha, light);
        }
    }

    private static void renderWarpedFace(
            VertexConsumer quads,
            PoseStack.Pose pose,
            DmtEffectState state,
            BlockPos pos,
            Direction face,
            TextureAtlasSprite sprite,
            float time,
            int divisions,
            double blockWarp,
            double replacement,
            double layerDepth,
            int seed,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        for (int y = 0; y < divisions; y++) {
            double v0 = y / (double) divisions;
            double v1 = (y + 1) / (double) divisions;
            for (int x = 0; x < divisions; x++) {
                double u0 = x / (double) divisions;
                double u1 = (x + 1) / (double) divisions;
                double cellNoise = hashNoise(pos.getX() + x * 13.0D, pos.getY() + pos.getZ() + y * 17.0D, seed * 0.0003D);
                if (replacement > 0.54D && cellNoise < replacement * 0.16D) {
                    continue;
                }
                WarpVertex p00 = warpedFacePoint(face, u0, v0, state, time, blockWarp, replacement, layerDepth, seed);
                WarpVertex p10 = warpedFacePoint(face, u1, v0, state, time, blockWarp, replacement, layerDepth, seed);
                WarpVertex p11 = warpedFacePoint(face, u1, v1, state, time, blockWarp, replacement, layerDepth, seed);
                WarpVertex p01 = warpedFacePoint(face, u0, v1, state, time, blockWarp, replacement, layerDepth, seed);
                int localAlpha = clampColor((int) Math.round(alpha * (0.58D + cellNoise * 0.42D) * p00.alpha()));
                texturedQuad(quads, pose, sprite, face, p00, p10, p11, p01, red, green, blue, localAlpha, light);
            }
        }
    }

    private static WarpVertex warpedFacePoint(
            Direction face,
            double u,
            double v,
            DmtEffectState state,
            float time,
            double blockWarp,
            double replacement,
            double layerDepth,
            int seed
    ) {
        Vec3 normal = normal(face);
        Vec3 tangentU = tangentU(face);
        Vec3 tangentV = tangentV(face);
        Vec3 center = facePoint(face, 0.5D, 0.5D);
        Vec3 base = facePoint(face, u, v);
        double cu = u - 0.5D;
        double cv = v - 0.5D;
        double dist = Math.sqrt(cu * cu + cv * cv);
        double edgeDistance = Math.min(Math.min(u, 1.0D - u), Math.min(v, 1.0D - v));
        double edge = 1.0D - smoothstep(0.0D, 0.24D, edgeDistance);
        double seedPhase = (seed & 4095) / 4095.0D * TAU;
        double machineCells = 3.0D + state.tier() * 1.4D + replacement * 5.0D;
        double gearA = Math.sin((u * machineCells + v * (machineCells * 0.63D)) * TAU + time * (0.072D + state.signature().tunnelSpeed() * 0.020D) + seedPhase);
        double gearB = Math.cos((v * (machineCells + 1.0D) - u * (machineCells * 0.42D)) * TAU - time * (0.064D + state.signature().symmetry() * 0.022D) + seedPhase * 0.7D);
        double radial = Math.sin(dist * (18.0D + state.tier() * 8.0D + replacement * 18.0D) - time * (0.060D + state.signature().mandalaDensity() * 0.030D) + seedPhase);
        double hinge = switch (face) {
            case UP, NORTH, EAST -> smoothstep(0.08D, 0.78D, u);
            case DOWN, SOUTH, WEST -> smoothstep(0.08D, 0.78D, v);
        };
        double peel = (0.010D + blockWarp * 0.060D + replacement * 0.210D)
                * layerDepth
                * (0.34D + edge * 0.40D + Math.abs(radial) * 0.26D + hinge * replacement * 0.62D);
        double liftPulse = Math.sin(time * (0.085D + state.signature().portalBias() * 0.030D) + seedPhase + layerDepth * 2.1D) * 0.5D + 0.5D;
        double outward = peel * (0.58D + liftPulse * 0.42D);
        double splitScale = 1.0D + replacement * layerDepth * (0.08D + hinge * 0.24D) + state.breakthroughTransition() * 0.035D;
        Vec3 scaled = center.add(base.subtract(center).scale(splitScale));
        double tangentAmp = (0.006D + blockWarp * 0.032D + replacement * 0.050D) * (0.36D + layerDepth * 0.64D);
        Vec3 moved = scaled
                .add(normal.scale(EPSILON + outward))
                .add(tangentU.scale((gearA * 0.56D + radial * 0.28D) * tangentAmp))
                .add(tangentV.scale((gearB * 0.56D - radial * 0.22D) * tangentAmp));
        double srcU = wrap(u + gearB * blockWarp * 0.020D + replacement * layerDepth * 0.030D);
        double srcV = wrap(v + gearA * blockWarp * 0.020D - replacement * layerDepth * 0.020D);
        double alpha = clamp(0.52D + edge * 0.20D + layerDepth * 0.18D + replacement * 0.16D);
        return new WarpVertex(moved, srcU, srcV, alpha);
    }

    private static void renderPersistentCollapseMachines(
            VertexConsumer quads,
            PoseStack poseStack,
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            Vec3 camera,
            DmtEffectState state,
            float time,
            double architecture,
            double replacement,
            CameraBasis basis,
            double presence
    ) {
        if (presence <= 0.012D || architecture + replacement <= 0.08D) {
            return;
        }

        Vec3 eye = effectEye(player);
        Vec3 forward = basis.forward();
        Vec3 right = basis.right();
        Vec3 up = basis.up();
        int light = 0x00F000F0;
        int maxMachines = TrippyConfig.dmtAccessibilityMode ? 4 : 7;
        double drive = clamp(architecture * 0.58D + replacement * 0.74D + presence * 0.32D);
        PoseStack.Pose pose = poseStack.last();
        for (int machine = 0; machine < maxMachines; machine++) {
            double slot = machine / (double) Math.max(1, maxMachines - 1);
            double machinePresence = presence * smoothstep(slot * 0.64D, slot * 0.64D + 0.26D, drive);
            if (machinePresence <= 0.014D) {
                continue;
            }
            int h = hash(machine, (int) state.signature().seed(), 0x5EED, 0x9B1D);
            double seed = unsignedUnit(h);
            TextureAtlasSprite sprite = realmSprite(minecraft, level, player.blockPosition(), h);
            if (sprite == null) {
                continue;
            }
            double depth = 2.8D + machine * (0.72D + replacement * 0.16D) + seed * (1.25D + architecture * 0.42D);
            double orbit = time * (0.014D + state.signature().tunnelSpeed() * 0.008D) + seed * TAU + machine * 0.73D;
            double side = Math.sin(orbit * 1.07D) * (0.52D + replacement * 1.12D) + (seed - 0.5D) * (1.35D + replacement * 0.45D);
            double lift = Math.cos(orbit * 0.83D + seed) * (0.35D + architecture * 0.78D);
            Vec3 anchorWorld = eye.add(forward.scale(depth)).add(right.scale(side)).add(up.scale(lift));
            double spin = orbit * (0.45D + architecture * 0.22D);
            Vec3 machineRight = safeNormalize(right.scale(Math.cos(spin)).add(up.scale(Math.sin(spin))), right);
            Vec3 machineUp = safeNormalize(forward.cross(machineRight), up);
            Vec3 machineForward = safeNormalize(machineRight.cross(machineUp), forward);
            double scale = (0.46D + architecture * 0.54D + replacement * 0.66D + seed * 0.26D) * (0.58D + machinePresence * 0.42D);
            int nodes = TrippyConfig.dmtAccessibilityMode ? 24 : 46;
            renderCollapsingBlockMachine(quads, pose, sprite, state, anchorWorld.subtract(camera), machineForward, machineRight, machineUp,
                    scale, machine, nodes, architecture, replacement, machinePresence, time, light);
            renderMachinePortalShell(quads, pose, sprite, state, anchorWorld.subtract(camera), machineForward, machineRight, machineUp,
                    scale, machine, architecture, replacement, machinePresence, time, light);
        }
    }

    private static void renderCollapsingBlockMachine(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            DmtEffectState state,
            Vec3 anchor,
            Vec3 forward,
            Vec3 right,
            Vec3 up,
            double scale,
            int machine,
            int nodeBudget,
            double architecture,
            double replacement,
            double presence,
            float time,
            int light
    ) {
        int seed = hash(machine, (int) state.signature().seed(), 0xC0A17);
        double cell = scale * (0.22D + replacement * 0.08D);
        double mergeWave = Math.sin(time * (0.026D + state.signature().portalBias() * 0.010D) + machine * 1.31D + seed * 0.00007D) * 0.5D + 0.5D;
        double latticeBlend = clamp(0.22D + architecture * 0.26D + replacement * 0.20D);
        double nodeDrive = clamp(architecture * 0.46D + replacement * 0.60D + presence * 0.36D);
        for (int i = 0; i < nodeBudget; i++) {
            int h = hash(seed, i * 31, machine * 17, 0xB10C);
            double t = nodeBudget <= 1 ? 0.0D : i / (double) (nodeBudget - 1);
            double nodePresence = presence * smoothstep(t * 0.70D, t * 0.70D + 0.24D, nodeDrive);
            if (nodePresence <= 0.014D) {
                continue;
            }
            double angle = i * 2.399963229728653D + time * (0.034D + state.signature().symmetry() * 0.012D) + unsignedUnit(h) * TAU;
            double layer = t - 0.5D;
            double radius = scale * (0.34D + Math.sin(t * Math.PI) * (0.72D + replacement * 0.42D));
            double branch = (h & 3);
            Vec3 fluid = right.scale(Math.cos(angle) * radius)
                    .add(up.scale(Math.sin(angle * (branch == 2.0D ? 1.7D : 1.0D)) * radius * (0.74D + architecture * 0.18D)))
                    .add(forward.scale(layer * scale * (2.4D + replacement * 0.9D)));
            Vec3 scaffold = right.scale(Math.rint(Math.cos(angle) * 2.0D) * cell * (1.7D + replacement))
                    .add(up.scale(Math.rint(Math.sin(angle) * 2.0D) * cell * (1.4D + architecture)))
                    .add(forward.scale(Math.rint(layer * 4.0D) * cell * (1.8D + replacement)));
            double collapse = replacement * (0.18D + mergeWave * 0.44D + unsignedUnit(h >>> 6) * 0.18D);
            double pulse = Math.sin(time * (0.046D + state.signature().tunnelSpeed() * 0.016D) + i * 0.71D + machine) * 0.5D + 0.5D;
            Vec3 offset = fluid.scale(1.0D - latticeBlend).add(scaffold.scale(latticeBlend));
            if ((h & 7) == 0) {
                offset = offset.scale(0.44D + pulse * 0.38D + replacement * 0.16D);
            } else if ((h & 7) == 3) {
                offset = offset.add(right.scale(Math.sin(time * 0.031D + i) * cell * 2.2D));
            }
            Vec3 center = anchor.add(offset.scale((0.34D + nodePresence * 0.66D) * (1.0D - collapse * 0.44D)));
            double node = cell * (0.22D + unsignedUnit(h >>> 9) * 0.26D + replacement * 0.16D)
                    * (0.50D + nodePresence * 0.50D)
                    * (0.72D + pulse * 0.34D);
            double stretchA = 1.0D;
            double stretchB = 1.0D;
            double stretchC = 1.0D;
            switch (h & 7) {
                case 0 -> stretchC = 1.8D + replacement * 1.1D;
                case 1 -> {
                    stretchA = 1.9D + architecture * 0.9D;
                    stretchB = 0.52D;
                }
                case 2 -> {
                    stretchB = 1.8D + replacement * 0.8D;
                    stretchC = 0.66D;
                }
                case 3 -> {
                    stretchA = 0.70D;
                    stretchB = 0.70D;
                    stretchC = 2.2D + pulse * 0.8D;
                }
                case 4 -> {
                    stretchA = 2.7D + replacement * 1.2D;
                    stretchB = 0.18D;
                    stretchC = 0.72D;
                }
                case 5 -> {
                    stretchA = 0.22D;
                    stretchB = 2.3D + architecture * 1.0D;
                    stretchC = 0.62D;
                }
                default -> {
                    stretchA = 1.25D + pulse * 0.65D;
                    stretchB = 1.25D + (1.0D - pulse) * 0.65D;
                    stretchC = 0.36D + replacement * 0.38D;
                }
            }
            double spin = angle * 0.46D + pulse * TAU + replacement * 0.7D;
            Vec3 axisRight = safeNormalize(right.scale(Math.cos(spin)).add(up.scale(Math.sin(spin))), right);
            Vec3 axisUp = safeNormalize(forward.cross(axisRight), up);
            Vec3 axisForward = safeNormalize(axisRight.cross(axisUp), forward);
            int red = mixColor(120, state.signature().primaryRed(), 0.16D + replacement * 0.16D + unsignedUnit(h >>> 4) * 0.10D);
            int green = mixColor(136, state.signature().accentGreen(), 0.18D + architecture * 0.10D);
            int blue = mixColor(150, state.signature().accentBlue(), 0.20D + replacement * 0.14D);
            int alpha = clampColor((int) Math.round((18.0D + architecture * 34.0D + replacement * 28.0D + pulse * 8.0D) * nodePresence));
            drawOrientedBox(quads, pose, sprite, center, axisRight, axisUp, axisForward,
                    node * stretchA, node * stretchB, node * stretchC, red, green, blue, alpha, light);

            if ((i % 5 == 0 || (h & 15) == 2) && replacement > 0.18D && nodePresence > 0.18D) {
                Vec3 toAnchor = anchor.subtract(center);
                double distance = toAnchor.length();
                if (distance > node * 2.0D) {
                    Vec3 linkForward = safeNormalize(toAnchor, forward);
                    Vec3 linkRight = safeNormalize(linkForward.cross(axisUp), axisRight);
                    Vec3 linkUp = safeNormalize(linkRight.cross(linkForward), axisUp);
                    Vec3 linkCenter = center.add(toAnchor.scale(0.48D));
                    int linkAlpha = clampColor((int) Math.round(alpha * (0.20D + replacement * 0.16D)));
                    drawOrientedBox(quads, pose, sprite, linkCenter, linkRight, linkUp, linkForward,
                            node * 0.22D, node * 0.22D, distance * (0.18D + replacement * 0.10D), red, green, blue, linkAlpha, light);
                }
            }
        }
    }

    private static void renderMachinePortalShell(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            DmtEffectState state,
            Vec3 anchor,
            Vec3 forward,
            Vec3 right,
            Vec3 up,
            double scale,
            int machine,
            double architecture,
            double replacement,
            double presence,
            float time,
            int light
    ) {
        int ribs = TrippyConfig.dmtAccessibilityMode ? 8 : 14;
        int seed = hash(machine, (int) state.signature().seed(), 0xD00F, 0xA71);
        double ringRadius = scale * (0.62D + architecture * 0.34D + replacement * 0.26D);
        double spin = time * (0.022D + state.signature().symmetry() * 0.010D) + seed * 0.00019D;
        for (int i = 0; i < ribs; i++) {
            int h = hash(seed, i * 29, machine * 11, 0xB0D1);
            double t = i / (double) ribs;
            double appear = presence * smoothstep(t * 0.62D, t * 0.62D + 0.30D, architecture * 0.48D + replacement * 0.62D + presence * 0.42D);
            if (appear <= 0.018D) {
                continue;
            }
            double angle = t * TAU + spin + unsignedUnit(h) * 0.18D;
            Vec3 radial = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 tangent = right.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle)));
            double wave = Math.sin(time * 0.041D + i * 0.83D + seed * 0.00031D) * 0.5D + 0.5D;
            Vec3 center = anchor
                    .add(radial.scale(ringRadius * (0.78D + wave * 0.18D)))
                    .add(forward.scale(Math.sin(angle * 2.0D + time * 0.019D) * scale * (0.18D + replacement * 0.18D)));
            Vec3 ribRight = safeNormalize(tangent, right);
            Vec3 ribUp = safeNormalize(radial.scale(0.55D).add(forward.scale(0.45D)), up);
            Vec3 ribForward = safeNormalize(ribRight.cross(ribUp), forward);
            int red = mixColor(128, state.signature().accentRed(), 0.14D + replacement * 0.12D);
            int green = mixColor(146, state.signature().primaryGreen(), 0.16D + architecture * 0.10D);
            int blue = mixColor(158, state.signature().accentBlue(), 0.18D + wave * 0.08D);
            int alpha = clampColor((int) Math.round((14.0D + architecture * 24.0D + replacement * 20.0D) * appear));
            drawOrientedBox(quads, pose, sprite, center, ribRight, ribUp, ribForward,
                    scale * (0.10D + replacement * 0.030D), scale * 0.018D, scale * (0.030D + wave * 0.018D), red, green, blue, alpha, light);
            if ((i + machine) % 4 == 0) {
                Vec3 panelCenter = center.scale(0.72D).add(anchor.scale(0.28D));
                drawOrientedBox(quads, pose, sprite, panelCenter, ribRight, ribUp, ribForward,
                        scale * (0.18D + wave * 0.10D), scale * 0.012D, scale * (0.11D + replacement * 0.040D),
                        red, green, blue, clampColor((int) Math.round(alpha * 0.42D)), light);
            }
        }
    }

    private static void renderFractalRealm(
            VertexConsumer quads,
            PoseStack poseStack,
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            Vec3 camera,
            DmtEffectState state,
            float time,
            double architecture,
            double replacement,
            CameraBasis basis,
            double presence
    ) {
        if (presence <= 0.012D || architecture <= 0.025D && replacement <= 0.08D) {
            return;
        }
        Vec3 eye = effectEye(player);
        Vec3 forward = basis.forward();
        Vec3 right = basis.right();
        Vec3 up = basis.up();
        int light = 0x00F000F0;
        int maxChambers = TrippyConfig.dmtAccessibilityMode ? 3 : 5;
        int maxNodes = TrippyConfig.dmtAccessibilityMode ? 30 : 64;
        double drive = clamp(architecture * 0.58D + replacement * 0.72D + presence * 0.26D);
        PoseStack.Pose pose = poseStack.last();
        for (int c = 0; c < maxChambers; c++) {
            int chamberHash = hash(c, (int) state.signature().seed(), 0xA11CE);
            double chamberPresence = presence * smoothstep(c * 0.14D, c * 0.14D + 0.26D, drive);
            if (chamberPresence <= 0.015D) {
                continue;
            }
            TextureAtlasSprite sprite = realmSprite(minecraft, level, player.blockPosition(), chamberHash);
            if (sprite == null) {
                continue;
            }
            double chamberSeed = unsignedUnit(chamberHash);
            double depth = 3.0D + c * (0.92D + replacement * 0.22D) + chamberSeed * 1.0D;
            double sideOffset = (chamberSeed - 0.5D) * (2.6D + replacement * 2.0D);
            double upOffset = (unsignedUnit(hash(c, 17, (int) state.signature().seed())) - 0.45D) * (1.7D + architecture * 1.6D);
            Vec3 anchorWorld = eye.add(forward.scale(depth)).add(right.scale(sideOffset)).add(up.scale(upOffset));
            double spin = time * (0.014D + state.signature().symmetry() * 0.010D) + c * 0.74D + state.signature().seed() * 0.000001D;
            Vec3 chamberRight = right.scale(Math.cos(spin)).add(up.scale(Math.sin(spin))).normalize();
            Vec3 chamberUp = chamberRight.cross(forward).normalize();
            double scale = (0.54D + c * 0.14D + architecture * 0.54D + replacement * 0.42D) * (0.55D + chamberPresence * 0.45D);
            int nodes = Math.min(maxNodes, TrippyConfig.dmtAccessibilityMode ? 26 : 52);
            renderRecursiveVoxelChamber(quads, pose, sprite, state, anchorWorld.subtract(camera), forward, chamberRight, chamberUp,
                    scale, c, nodes, architecture, replacement, chamberPresence, time, light);
        }
    }

    private static void renderRecursiveVoxelChamber(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            DmtEffectState state,
            Vec3 anchor,
            Vec3 forward,
            Vec3 right,
            Vec3 up,
            double scale,
            int chamber,
            int nodeBudget,
            double architecture,
            double replacement,
            double presence,
            float time,
            int light
    ) {
        int grid = 3 + Math.min(3, state.tier());
        double cell = scale / grid;
        int emitted = 0;
        int seed = hash(chamber, (int) state.signature().seed(), 0xC0DE);
        double nodeDrive = clamp(architecture * 0.50D + replacement * 0.60D + presence * 0.30D);
        for (int z = -grid; z <= grid && emitted < nodeBudget; z++) {
            for (int y = -grid; y <= grid && emitted < nodeBudget; y++) {
                for (int x = -grid; x <= grid && emitted < nodeBudget; x++) {
                    int manhattan = Math.abs(x) + Math.abs(y) + Math.abs(z);
                    int shell = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
                    boolean outerFrame = shell == grid && manhattan % 2 == chamber % 2;
                    boolean corridor = Math.abs(x) <= 1 && Math.abs(y) == grid || Math.abs(y) <= 1 && Math.abs(x) == grid || Math.abs(z) <= 1 && (Math.abs(x) == grid || Math.abs(y) == grid);
                    boolean recursiveVoid = Math.abs(x) <= 1 && Math.abs(y) <= 1 || Math.abs(x) <= 1 && Math.abs(z) <= 1 || Math.abs(y) <= 1 && Math.abs(z) <= 1;
                    if ((!outerFrame && !corridor) || recursiveVoid) {
                        continue;
                    }
                    int h = hash(seed, x * 31 + y * 17, z * 13 + chamber);
                    double keep = unsignedUnit(h);
                    double nodePresence = presence * smoothstep(keep - 0.12D, keep + 0.18D, 0.52D + nodeDrive * 0.34D);
                    if (nodePresence <= 0.014D) {
                        continue;
                    }
                    double pulse = Math.sin((h & 1023) * 0.006D + chamber * 0.8D
                            + time * (0.030D + state.signature().tunnelSpeed() * 0.012D)) * 0.5D + 0.5D;
                    double assembly = Math.sin(time * (0.020D + state.signature().portalBias() * 0.010D)
                            + h * 0.00031D + chamber * 1.4D) * 0.5D + 0.5D;
                    Vec3 gridOffset = right.scale(x * cell)
                            .add(up.scale(y * cell))
                            .add(forward.scale(z * cell * (0.82D + replacement * 0.22D)));
                    Vec3 motion = right.scale(Math.sin(time * 0.037D + h * 0.0017D) * cell * (0.10D + replacement * 0.22D))
                            .add(up.scale(Math.cos(time * 0.031D + h * 0.0011D) * cell * (0.08D + architecture * 0.18D)))
                            .add(forward.scale(Math.sin(time * 0.026D + chamber + x - z) * cell * replacement * 0.20D));
                    Vec3 center = anchor.add(gridOffset.scale(1.0D - assembly * replacement * 0.20D)).add(motion);
                    double node = cell * (0.22D + pulse * 0.18D + replacement * 0.12D)
                            * (0.46D + nodePresence * 0.54D)
                            * (0.84D + assembly * 0.28D);
                    double roll = time * (0.018D + replacement * 0.014D) + h * 0.0009D;
                    Vec3 nodeRight = safeNormalize(right.scale(Math.cos(roll)).add(up.scale(Math.sin(roll))), right);
                    Vec3 nodeUp = safeNormalize(forward.cross(nodeRight), up);
                    Vec3 nodeForward = safeNormalize(nodeRight.cross(nodeUp), forward);
                    int red = mixColor(126, state.signature().accentRed(), 0.22D + keep * 0.16D);
                    int green = mixColor(142, state.signature().primaryGreen(), 0.24D + state.symbolLanguage() * 0.035D);
                    int blue = mixColor(154, state.signature().accentBlue(), 0.28D + replacement * 0.12D);
                    int alpha = clampColor((int) Math.round((22.0D + architecture * 40.0D + replacement * 24.0D - shell * 1.6D) * nodePresence));
                    drawOrientedBox(quads, pose, sprite, center, nodeRight, nodeUp, nodeForward, node, node, node * (0.72D + replacement * 0.52D), red, green, blue, alpha, light);
                    emitted++;
                }
            }
        }
    }

    private static void drawOrientedBox(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            Vec3 forward,
            double halfRight,
            double halfUp,
            double halfForward,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        emitOrientedFace(quads, pose, sprite, center.add(forward.scale(halfForward)), right, up, forward, halfRight, halfUp, red, green, blue, alpha, light);
        emitOrientedFace(quads, pose, sprite, center.add(forward.scale(-halfForward)), right.scale(-1.0D), up, forward.scale(-1.0D), halfRight, halfUp, red, green, blue, alpha, light);
        emitOrientedFace(quads, pose, sprite, center.add(right.scale(halfRight)), forward.scale(-1.0D), up, right, halfForward, halfUp, red, green, blue, alpha, light);
        emitOrientedFace(quads, pose, sprite, center.add(right.scale(-halfRight)), forward, up, right.scale(-1.0D), halfForward, halfUp, red, green, blue, alpha, light);
        emitOrientedFace(quads, pose, sprite, center.add(up.scale(halfUp)), right, forward.scale(-1.0D), up, halfRight, halfForward, red, green, blue, alpha, light);
        emitOrientedFace(quads, pose, sprite, center.add(up.scale(-halfUp)), right, forward, up.scale(-1.0D), halfRight, halfForward, red, green, blue, alpha, light);
    }

    private static void emitOrientedFace(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            Vec3 center,
            Vec3 axisU,
            Vec3 axisV,
            Vec3 normal,
            double halfU,
            double halfV,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        Vec3 p00 = center.add(axisU.scale(-halfU)).add(axisV.scale(-halfV));
        Vec3 p10 = center.add(axisU.scale(halfU)).add(axisV.scale(-halfV));
        Vec3 p11 = center.add(axisU.scale(halfU)).add(axisV.scale(halfV));
        Vec3 p01 = center.add(axisU.scale(-halfU)).add(axisV.scale(halfV));
        vertex(quads, pose, sprite, p00, 0.0D, 0.0D, red, green, blue, alpha, light, normal);
        vertex(quads, pose, sprite, p10, 1.0D, 0.0D, red, green, blue, alpha, light, normal);
        vertex(quads, pose, sprite, p11, 1.0D, 1.0D, red, green, blue, alpha, light, normal);
        vertex(quads, pose, sprite, p01, 0.0D, 1.0D, red, green, blue, alpha, light, normal);
    }

    private static void texturedQuad(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            Direction face,
            WarpVertex p00,
            WarpVertex p10,
            WarpVertex p11,
            WarpVertex p01,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        Vec3 normal = normal(face);
        if (face == Direction.DOWN || face == Direction.NORTH || face == Direction.EAST) {
            vertex(quads, pose, sprite, p00.pos(), p00.srcU(), p00.srcV(), red, green, blue, alpha, light, normal);
            vertex(quads, pose, sprite, p01.pos(), p01.srcU(), p01.srcV(), red, green, blue, alpha, light, normal);
            vertex(quads, pose, sprite, p11.pos(), p11.srcU(), p11.srcV(), red, green, blue, alpha, light, normal);
            vertex(quads, pose, sprite, p10.pos(), p10.srcU(), p10.srcV(), red, green, blue, alpha, light, normal);
        } else {
            vertex(quads, pose, sprite, p00.pos(), p00.srcU(), p00.srcV(), red, green, blue, alpha, light, normal);
            vertex(quads, pose, sprite, p10.pos(), p10.srcU(), p10.srcV(), red, green, blue, alpha, light, normal);
            vertex(quads, pose, sprite, p11.pos(), p11.srcU(), p11.srcV(), red, green, blue, alpha, light, normal);
            vertex(quads, pose, sprite, p01.pos(), p01.srcU(), p01.srcV(), red, green, blue, alpha, light, normal);
        }
    }

    private static void vertex(
            VertexConsumer quads,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            Vec3 pos,
            double u,
            double v,
            int red,
            int green,
            int blue,
            int alpha,
            int light,
            Vec3 normal
    ) {
        quads.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(red, green, blue, alpha)
                .setUv(atlasU(sprite, u), atlasV(sprite, v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static boolean isMorphableBlock(ClientLevel level, BlockState state, BlockPos pos) {
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        VoxelShape shape = state.getShape(level, pos);
        return !shape.isEmpty();
    }

    private static TextureAtlasSprite realmSprite(Minecraft minecraft, ClientLevel level, BlockPos origin) {
        return realmSprite(minecraft, level, origin, 0);
    }

    private static TextureAtlasSprite realmSprite(Minecraft minecraft, ClientLevel level, BlockPos origin, int salt) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int h = hash(origin.getX() + attempt * 13, origin.getY() + salt, origin.getZ() - attempt * 17, salt ^ attempt * 0x45D9F3B);
            int radius = 2 + attempt / 2;
            BlockPos sample = origin.offset(
                    Math.floorMod(h, radius * 2 + 1) - radius,
                    Math.floorMod(h >>> 8, 5) - 2,
                    Math.floorMod(h >>> 16, radius * 2 + 1) - radius
            );
            BlockState sampledState = level.getBlockState(sample);
            if (!sampledState.isAir() && sampledState.getRenderShape() == RenderShape.MODEL) {
                TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModel(sampledState).getParticleIcon();
                if (sprite != null) {
                    return sprite;
                }
            }
        }
        BlockState state = level.getBlockState(origin.below());
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            state = level.getBlockState(origin);
        }
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            return null;
        }
        return minecraft.getBlockRenderer().getBlockModel(state).getParticleIcon();
    }

    private static double updatePersistentGeometryPresence(ClientLevel level, DmtEffectState state, double target) {
        long time = level.getGameTime();
        long seed = state.signature().seed();
        if (state.active() && (lastPersistentSeed != seed || state.ageTicks() <= 2)) {
            persistentGeometryPresence = 0.0D;
            smoothedForward = null;
            lastPersistentSeed = seed;
            lastPersistentGameTime = time;
        }
        if (lastPersistentGameTime == Long.MIN_VALUE) {
            lastPersistentGameTime = time;
        }
        int delta = (int) Math.max(1L, Math.min(8L, time - lastPersistentGameTime));
        lastPersistentGameTime = time;
        double rate = target > persistentGeometryPresence ? 0.155D : 0.038D;
        double blend = 1.0D - Math.pow(1.0D - rate, delta);
        persistentGeometryPresence += (target - persistentGeometryPresence) * blend;
        if (persistentGeometryPresence < 0.006D && target <= 0.0D) {
            persistentGeometryPresence = 0.0D;
        }
        return smoothstep(0.0D, 1.0D, persistentGeometryPresence);
    }

    private static boolean advancePersistentGeometry(ClientLevel level) {
        if (!VivecraftCompat.isVrActive()) {
            vrGeometryLevel = null;
            vrGeometryFrame = Double.NaN;
            return true;
        }
        double frame = (double) level.getGameTime()
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        if (vrGeometryLevel == level && Double.isFinite(vrGeometryFrame)
                && (!VivecraftCompat.isFirstWorldPass() || frame == vrGeometryFrame)) {
            return false;
        }
        vrGeometryLevel = level;
        vrGeometryFrame = frame;
        return true;
    }

    private static CameraBasis persistentBasis(ClientLevel level, LocalPlayer player, DmtEffectState state, double presence, boolean advance) {
        Vec3 targetForward = effectLook(player).normalize();
        if (smoothedForward == null || advance && (state.ageTicks() <= 2 || presence < 0.02D)) {
            smoothedForward = targetForward;
        } else if (advance) {
            double follow = 0.18D + presence * 0.12D;
            smoothedForward = safeNormalize(smoothedForward.scale(1.0D - follow).add(targetForward.scale(follow)), targetForward);
        }
        Vec3 right = perpendicularSide(smoothedForward);
        Vec3 up = right.cross(smoothedForward).normalize();
        return new CameraBasis(smoothedForward, right, up);
    }

    private static void resetPersistentGeometry() {
        persistentGeometryPresence = 0.0D;
        lastPersistentGameTime = Long.MIN_VALUE;
        lastPersistentSeed = Long.MIN_VALUE;
        smoothedForward = null;
        vrGeometryLevel = null;
        vrGeometryFrame = Double.NaN;
    }

    private static Vec3 effectEye(LocalPlayer player) {
        Vec3 head = VivecraftCompat.headPosition();
        return head != null ? head : player.getEyePosition();
    }

    private static Vec3 effectLook(LocalPlayer player) {
        Vec3 direction = VivecraftCompat.headDirection();
        return direction != null ? direction : player.getLookAngle();
    }

    private static int fallbackTint(Minecraft minecraft, ClientLevel level, BlockState state, BlockPos pos) {
        int color = minecraft.getBlockColors().getColor(state, level, pos);
        return color == -1 ? 0xFFFFFF : color & 0xFFFFFF;
    }

    private static int maxLight(int a, int b) {
        int block = Math.max(a & 0xFFFF, b & 0xFFFF);
        int sky = Math.max(a >>> 16 & 0xFFFF, b >>> 16 & 0xFFFF);
        return block | sky << 16;
    }

    private static Vec3 facePoint(Direction face, double u, double v) {
        return switch (face) {
            case UP -> new Vec3(u, 1.0D, v);
            case DOWN -> new Vec3(u, 0.0D, v);
            case NORTH -> new Vec3(u, v, 0.0D);
            case SOUTH -> new Vec3(u, v, 1.0D);
            case WEST -> new Vec3(0.0D, v, u);
            case EAST -> new Vec3(1.0D, v, u);
        };
    }

    private static Vec3 normal(Direction face) {
        return new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    private static Vec3 tangentU(Direction face) {
        return switch (face) {
            case UP, DOWN, NORTH, SOUTH -> new Vec3(1.0D, 0.0D, 0.0D);
            case WEST, EAST -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private static Vec3 tangentV(Direction face) {
        return switch (face) {
            case UP, DOWN -> new Vec3(0.0D, 0.0D, 1.0D);
            case NORTH, SOUTH, WEST, EAST -> new Vec3(0.0D, 1.0D, 0.0D);
        };
    }

    private static Vec3 perpendicularSide(Vec3 look) {
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 0.01D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return side.normalize();
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        return vector.lengthSqr() < 1.0E-6D ? fallback : vector.normalize();
    }

    private static double stageReplacementBias(DmtEffectState state) {
        return switch (state.stage()) {
            case BREAKTHROUGH_TRANSITION -> 0.70D;
            case POST_BREAKTHROUGH -> 1.0D;
            case RETURN -> 0.36D;
            case LOW_THRESHOLD, PRE_BREAKTHROUGH -> 0.0D;
            case AFTERGLOW, SOBER -> 0.0D;
        };
    }

    private static float atlasU(TextureAtlasSprite sprite, double u) {
        return (float) mix(sprite.getU0(), sprite.getU1(), clamp(u));
    }

    private static float atlasV(TextureAtlasSprite sprite, double v) {
        return (float) mix(sprite.getV0(), sprite.getV1(), clamp(v));
    }

    private static int red(int color) {
        return color >> 16 & 255;
    }

    private static int green(int color) {
        return color >> 8 & 255;
    }

    private static int blue(int color) {
        return color & 255;
    }

    private static int mixColor(int base, int target, double amount) {
        return clampColor((int) Math.round(base + (target - base) * clamp(amount)));
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double hashNoise(double x, double y, double seed) {
        double value = Math.sin(x * 12.9898D + y * 78.233D + seed * 37.719D) * 43758.5453D;
        return value - Math.floor(value);
    }

    private static int hash(int a, int b, int c) {
        int value = a * 0x45D9F3B ^ b * 0x119DE1F3 ^ c * 0x3449B13;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return value;
    }

    private static int hash(int a, int b, int c, int d) {
        int value = hash(a, b, c) ^ d * 0x27D4EB2D;
        value ^= value >>> 15;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return value;
    }

    private static double unsignedUnit(int value) {
        return (value & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private static double wrap(double value) {
        double wrapped = value - Math.floor(value);
        return wrapped >= 1.0D ? 0.0D : wrapped;
    }

    private static double mix(double a, double b, double amount) {
        return a + (b - a) * clamp(amount);
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record WarpVertex(Vec3 pos, double srcU, double srcV, double alpha) {
    }

    private record CameraBasis(Vec3 forward, Vec3 right, Vec3 up) {
    }
}
