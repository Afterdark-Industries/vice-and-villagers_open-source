package com.trippycraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

public final class ClientBlockIllusions {
    private static final double TAU = Math.PI * 2.0D;
    private static final float FACE_EPSILON = 0.0012F;
    private static final double LSD_OUTLINE_FADE_START_MICROGRAMS = 500.0D;
    private static final double LSD_OUTLINE_FADE_END_MICROGRAMS = 750.0D;
    private static BlockPos fadeBlock;
    private static Direction fadeFace;
    private static long lastFadeGameTime = Long.MIN_VALUE;
    private static double targetFade;
    private static final int SURFACE_FADE_TICKS = 100;
    private static ActiveSurface activeSurface;
    private static FadingSurface fadingSurface;
    private static BlockPos gazeBlock;
    private static Direction gazeFace;
    private static long lastGazeGameTime = Long.MIN_VALUE;
    private static int gazeTicks;
    private static Object vrTargetLevel;
    private static double vrTargetFrame = Double.NaN;
    private static HitResult vrTarget;

    private ClientBlockIllusions() {
    }

    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        if (VivecraftCompat.isVrActive()) {
            return; // VR uses a world stage, even when Vivecraft hides block outlines.
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RenderContext context = new RenderContext(
                event.getPoseStack(),
                event.getCamera().getPosition(),
                event.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        renderTargetSurface(context, minecraft, event.getTarget());
    }

    public static void onVrRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VivecraftCompat.isVrWorldPass()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        double frame = (double) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        if (vrTargetLevel != minecraft.level || !Double.isFinite(vrTargetFrame)
                || VivecraftCompat.isFirstWorldPass() && frame != vrTargetFrame) {
            vrTargetLevel = minecraft.level;
            vrTargetFrame = frame;
            vrTarget = VivecraftCompat.headBlockHit(minecraft);
        }
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        try {
            RenderContext context = new RenderContext(new PoseStack(), event.getCamera().getPosition(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
            if (vrTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                renderTargetSurface(context, minecraft, hit);
            } else {
                startFadeActiveSurface(minecraft);
                resetSurfaceGaze();
                renderFadingSurface(context, minecraft, ClientExperienceStore.lsd(),
                        ClientExperienceStore.psilocybin(), null, null);
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void renderTargetSurface(RenderContext context, Minecraft minecraft, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        FaceFocus hitFocus = faceFocus(pos, face, hit.getLocation());

        LsdEffectState lsd = ClientExperienceStore.lsd();
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(state, lsd, ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        startFadeIfLeavingSurface(minecraft, pos, face);
        renderFadingSurface(context, minecraft, lsd, state, pos, face);

        SurfaceGaze gaze = updateSurfaceGaze(minecraft, pos, face, state, lsd);
        int psilocybinStareTicks = gaze.psilocybinTicks();
        int lsdStareTicks = gaze.lsdTicks();

        double lsdActivation = lsdSurfaceActivation(lsd, lsdStareTicks, pos, face) * blend.lsdVisualGain();
        double psilocybinActivation = psilocybinSurfaceActivation(state, psilocybinStareTicks, pos, face) * blend.psilocybinVisualGain();
        FaceFocus renderFocus = hitFocus;

        boolean lsdRendered = false;
        if (lsd.active() && TrippyConfig.enableLsdEffects && TrippyConfig.lsdBlockEffects && lsdActivation > 0.0D) {
            lsdRendered = renderLsdSurface(context, minecraft, lsd, pos, face, renderFocus, lsdActivation, 0);
        }

        boolean psilocybinRendered = false;
        if (state.active() && TrippyConfig.enablePsilocybinEffects && psilocybinActivation > 0.0D) {
            psilocybinRendered = renderPsilocybinSurface(context, minecraft, state, pos, face, renderFocus, psilocybinStareTicks, psilocybinActivation, true);
        }

        if (lsdRendered || psilocybinRendered) {
            activeSurface = new ActiveSurface(
                    pos.immutable(),
                    face,
                    renderFocus,
                    psilocybinRendered ? psilocybinStareTicks : 0,
                    lsdRendered ? lsdStareTicks : 0,
                    Math.floorMod(surfacePlaneHash(pos, face) >>> 8, 80)
            );
            fadingSurface = null;
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (VivecraftCompat.isVrActive() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return;
        }
        startFadeActiveSurface(minecraft);
        resetSurfaceGaze();
    }

    private static SurfaceGaze updateSurfaceGaze(Minecraft minecraft, BlockPos pos, Direction face, PsilocybinEffectState psilocybin, LsdEffectState lsd) {
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (!pos.equals(gazeBlock) || face != gazeFace) {
            gazeBlock = pos.immutable();
            gazeFace = face;
            gazeTicks = 0;
            lastGazeGameTime = gameTime;
            return new SurfaceGaze(0, 0);
        }
        if (gameTime != lastGazeGameTime) {
            int delta = (int) Math.max(1L, Math.min(8L, gameTime - lastGazeGameTime));
            gazeTicks = Math.min(1200, gazeTicks + delta);
            lastGazeGameTime = gameTime;
        }
        return new SurfaceGaze(gazeTicks, gazeTicks);
    }

    private static void resetSurfaceGaze() {
        gazeBlock = null;
        gazeFace = null;
        lastGazeGameTime = Long.MIN_VALUE;
        gazeTicks = 0;
    }

    private static void startFadeIfLeavingSurface(Minecraft minecraft, BlockPos pos, Direction face) {
        if (fadingSurface != null && fadingSurface.matches(pos, face)) {
            fadingSurface = null;
        }
        if (activeSurface != null && !activeSurface.matches(pos, face)) {
            startFadeActiveSurface(minecraft);
        }
    }

    private static void startFadeActiveSurface(Minecraft minecraft) {
        if (activeSurface == null) {
            return;
        }
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        fadingSurface = new FadingSurface(activeSurface, gameTime);
        activeSurface = null;
    }

    private static void renderFadingSurface(
            RenderContext context,
            Minecraft minecraft,
            LsdEffectState lsd,
            PsilocybinEffectState psilocybin,
            BlockPos currentPos,
            Direction currentFace
    ) {
        if (fadingSurface == null || fadingSurface.matches(currentPos, currentFace)) {
            return;
        }
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        double presence = fadingSurface.presence(gameTime);
        if (presence <= 0.0D) {
            fadingSurface = null;
            return;
        }

        ActiveSurface surface = fadingSurface.surface();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(psilocybin, lsd, ClientExperienceStore.dmt(), ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        boolean rendered = false;
        if (lsd.active() && TrippyConfig.enableLsdEffects && TrippyConfig.lsdBlockEffects && surface.lsdStareTicks() > 0) {
            rendered |= renderLsdSurface(context, minecraft, lsd, surface.pos(), surface.face(), surface.focus(), presence * lsdSurfaceActivation(lsd, surface.lsdStareTicks(), surface.pos(), surface.face())
                    * blend.lsdVisualGain(), surface.timeOffset());
        }
        if (psilocybin.active() && TrippyConfig.enablePsilocybinEffects && surface.psilocybinStareTicks() > 0) {
            rendered |= renderPsilocybinSurface(context, minecraft, psilocybin, surface.pos(), surface.face(), surface.focus(), surface.psilocybinStareTicks(), presence * blend.psilocybinVisualGain(), false);
        }
        if (!rendered && presence <= 0.01D) {
            fadingSurface = null;
        }
    }

    private static boolean renderPsilocybinSurface(
            RenderContext context,
            Minecraft minecraft,
            PsilocybinEffectState state,
            BlockPos pos,
            Direction face,
            FaceFocus hitFocus,
            int stareTicks,
            double presence,
            boolean currentTarget
    ) {
        double dose = state.activeDoseGrams();
        double tripWake = psilocybinTripWake(state);
        double textureGate = smoothstep(1.0D, 1.50D, dose) * tripWake;
        if (textureGate <= 0.0D) {
            return false;
        }

        int stareStartTicks = psilocybinActivationStartTicks(dose, pos, face);
        int stareRampTicks = psilocybinActivationRampTicks(dose);
        double stareEnd = stareStartTicks + stareRampTicks;
        double doseWake = psilocybinDoseWake(dose);
        double stareStart = stareStartTicks;
        double earlyStare = smoothstep(stareStart, stareEnd, stareTicks);
        if (earlyStare <= 0.0D) {
            return false;
        }

        BlockState blockState = minecraft.level.getBlockState(pos);
        if (!isCompleteCubeTarget(minecraft, blockState, pos)) {
            return false;
        }
        FaceSurface surface = resolveFaceSurface(minecraft, blockState, pos, face, hitFocus);
        if (surface == null) {
            return false;
        }

        MaterialProfile profile = MaterialProfile.forBlock(blockState);
        double longStart = stareStartTicks + stareRampTicks * mix(0.82D, 0.55D, doseWake);
        double longStare = smoothstep(longStart, longStart + mix(100.0D, 54.0D, doseWake), stareTicks);
        double lockedStart = psilocybinStabilizeTicks(dose, pos, face);
        double lockedStare = smoothstep(lockedStart, lockedStart + mix(82.0D, 44.0D, doseWake), stareTicks);
        double doseStep = perGramDoseStep(dose);
        double morphGate = smoothstep(1.80D, 3.00D, dose);
        double heroic = smoothstep(4.00D, 5.00D, dose);
        double overload = smoothstep(8.0D, 10.0D, dose);
        double close = 1.0D - smoothstep(1.20D, 6.20D, focusDistance(minecraft, pos, face, hitFocus));
        double focusEnergy = clampUnit(textureGate * (0.08D + doseStep * 0.52D + morphGate * 0.55D + heroic * 0.28D + overload * 0.34D)
                * (0.07D + earlyStare * 0.36D + longStare * 0.44D + lockedStare * 0.38D)
                * (0.62D + close * 0.38D)
                * presence);
        double fadeIn = currentTarget ? updateTargetFade(minecraft, pos, face) * presence : presence;
        double stareBuild = smoothstep(0.0D, 1.0D, earlyStare);
        double appearance = fadeIn * stareBuild * tripWake;
        if (focusEnergy <= 0.010D || appearance <= 0.01D) {
            return false;
        }

        float time = (float) PsilocybinPresentationClock.timeTicks(context.partialTick());
        int light = faceLight(minecraft, pos, face);
        Vec3 camera = context.camera();
        PoseStack poseStack = context.poseStack();
        try (ByteBufferBuilder buffer = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource customBuffers = MultiBufferSource.immediate(buffer);
            VertexConsumer quads = customBuffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            FaceFocus localFocus = surface.normalize(hitFocus);

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            drawWholeFaceTextureWarp(
                    quads,
                    poseStack.last(),
                    pos,
                    face,
                    localFocus,
                    surface,
                    profile,
                    state,
                    time,
                    light,
                    focusEnergy,
                    appearance,
                    dose,
                    doseStep,
                    heroic,
                    overload,
                    stareBuild,
                    longStare,
                    lockedStare
            );
            poseStack.popPose();
            customBuffers.endBatch();
        }
        return true;
    }

    private static boolean renderLsdSurface(
            RenderContext context,
            Minecraft minecraft,
            LsdEffectState state,
            BlockPos pos,
            Direction face,
            FaceFocus hitFocus,
            double presence,
            int timeOffset
    ) {
        BlockState blockState = minecraft.level.getBlockState(pos);
        if (!isCompleteCubeTarget(minecraft, blockState, pos)) {
            return false;
        }

        FaceSurface surface = resolveFaceSurface(minecraft, blockState, pos, face, hitFocus);
        if (surface == null) {
            return false;
        }

        MaterialProfile profile = MaterialProfile.forBlock(blockState);
        double close = 1.0D - smoothstep(1.20D, 7.25D, focusDistance(minecraft, pos, face, hitFocus));
        double visualTier = lsdVisualTier(state.activeDoseMicrograms());
        double tier = visualTier / 5.0D;
        double liquidMaterial = profile.style() == 6 ? 0.34D : profile.style() == 3 ? 0.16D : 0.0D;
        double focusStrength = Math.log1p(Math.max(0.0D, state.edgeHum() * 0.18D
                + state.geometryMelt() * 0.20D + state.blockAutonomy() * 0.24D
                + state.liquidFlow() * (0.16D + liquidMaterial) + state.realityFold() * 0.18D));
        double focusEnergy = (1.0D - Math.exp(-focusStrength))
                * (0.40D + close * 0.60D) * presence;
        if (focusEnergy <= 0.010D) {
            return false;
        }

        float time = (float) LsdPresentationClock.timeTicks(context.partialTick());
        int light = faceLight(minecraft, pos, face);
        Vec3 camera = context.camera();
        PoseStack poseStack = context.poseStack();
        try (ByteBufferBuilder buffer = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource customBuffers = MultiBufferSource.immediate(buffer);
            VertexConsumer crawlQuads = customBuffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            FaceFocus localFocus = surface.normalize(hitFocus);

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

            // The vanilla block stays as the lighting-perfect base; this only adds roaming sampled texels.
            drawLsdFaceWarp(
                    crawlQuads,
                    poseStack.last(),
                    pos,
                    face,
                    localFocus,
                    surface,
                    profile,
                    state,
                    time,
                    light,
                    focusEnergy,
                    0,
                    Math.min(210, (int) Math.round(focusEnergy * (160.0D + tier * 40.0D)))
            );

            // Colour and crystalline echoes are generated by the one coherent
            // world shader. This mesh contributes only the block's own flowing texels.

            poseStack.popPose();
            customBuffers.endBatch();
        }
        return true;
    }

    private static void drawLsdFaceWarp(
            VertexConsumer quads,
            PoseStack.Pose pose,
            BlockPos pos,
            Direction face,
            FaceFocus focus,
            FaceSurface surface,
            MaterialProfile profile,
            LsdEffectState state,
            float time,
            int light,
            double focusEnergy,
            int layer,
            int alpha
    ) {
        // Keep a fixed tessellation so a dose boundary never swaps the mesh.
        int divisions = TrippyConfig.lsdPerformanceMode ? 24 : 40;

        int seed = (int) state.signature().seed() ^ surfacePlaneHash(pos, face) ^ profile.style() * 0x51D15D ^ layer * 0x45D9F3B;
        double phase = ((seed & 4095) / 4095.0D) * TAU + layer * 0.71D;
        boolean vertical = face != Direction.UP && face != Direction.DOWN;

        for (int y = 0; y < divisions; y++) {
            double v0 = y / (double) divisions;
            double v1 = (y + 1) / (double) divisions;
            for (int x = 0; x < divisions; x++) {
                double u0 = x / (double) divisions;
                double u1 = (x + 1) / (double) divisions;
                WarpPoint p00 = lsdWarpPoint(u0, v0, pos, face, focus, surface, profile, state, time, phase, focusEnergy, layer, vertical);
                WarpPoint p10 = lsdWarpPoint(u1, v0, pos, face, focus, surface, profile, state, time, phase, focusEnergy, layer, vertical);
                WarpPoint p11 = lsdWarpPoint(u1, v1, pos, face, focus, surface, profile, state, time, phase, focusEnergy, layer, vertical);
                WarpPoint p01 = lsdWarpPoint(u0, v1, pos, face, focus, surface, profile, state, time, phase, focusEnergy, layer, vertical);
                texturedFaceQuad(quads, pose, face, surface, p00, p10, p11, p01, alpha, light);
            }
        }
    }

    private static WarpPoint lsdWarpPoint(
            double u,
            double v,
            BlockPos pos,
            Direction face,
            FaceFocus focus,
            FaceSurface surface,
            MaterialProfile profile,
            LsdEffectState state,
            float time,
            double phase,
            double focusEnergy,
            int layer,
            boolean vertical
    ) {
        double strength = Math.log1p(Math.max(0.0D, state.visualIntensity()));
        double tail = Math.log1p(Math.max(0.0D, state.activeDoseMicrograms() / 1000.0D - 1.0D));
        double doseTravel = Math.log1p(Math.max(0.0D, state.activeDoseMicrograms()) / 125.0D);
        double doseContrast = smoothstep(175.0D, 550.0D, state.activeDoseMicrograms());
        double energy = Math.log1p(strength) * 2.2D * (0.90D + doseTravel * 0.55D + tail * 0.20D)
                * (1.0D + doseContrast * 0.95D) * focusEnergy;
        double movement = (TrippyConfig.lsdAccessibilityMode ? 0.35D : 1.0D)
                * (com.trippycraft.config.TrippyClientConfig.motionReduction ? 0.24D : 1.0D);
        double melt = Math.log1p(Math.max(0.0D, state.geometryMelt()) / 6.0D);
        double liquid = Math.log1p(Math.max(0.0D, state.liquidFlow()) / 6.0D);
        SurfacePoint point = surfacePoint(pos, face, surface.faceU(u), surface.faceV(v));
        double fieldU = point.u(), fieldV = point.v();
        double clock = time * 0.0180D;
        double nA = smoothSurfaceNoise(fieldU * 2.8D + clock * 0.31D,
                fieldV * 2.3D - clock * 0.24D, phase);
        double nB = smoothSurfaceNoise(fieldU * 2.1D - clock * 0.22D,
                fieldV * 2.7D + clock * 0.28D, phase * 0.73D);
        double waveU = Math.sin(fieldU * 5.4D + fieldV * 3.8D + clock + nA * 2.6D + phase);
        double waveV = Math.cos(fieldV * 6.1D - fieldU * 3.5D - clock * 0.82D + nB * 2.3D + phase);
        double edgeU = smoothstep(0.0D, 0.14D, u) * (1.0D - smoothstep(0.86D, 1.0D, u));
        double edgeV = smoothstep(0.0D, 0.12D, v) * (1.0D - smoothstep(0.86D, 1.0D, v));
        double flowU = ((nA - 0.5D) * 0.110D + waveU * 0.030D) * energy * movement;
        double flowV = ((nB - 0.5D) * 0.090D + waveV * 0.030D) * energy * movement;
        flowU /= 1.0D + Math.abs(flowU) / 0.28D;
        flowV /= 1.0D + Math.abs(flowV) / 0.28D;
        double column = 0.5D + 0.5D * Math.sin(fieldU * 7.1D + nA * 3.4D - clock * 0.51D + phase);
        column = column * column * column;
        double drip = vertical ? (0.060D + melt * 0.050D + liquid * 0.030D)
                * column * energy * movement * edgeU : 0.0D;
        // Molten surface paint hangs below the bottom edge, attached at the top.
        // The original collision/world geometry never moves. Adjacent face texels
        // use one smooth field rather than separate random tiles or colour layers.
        double hanging = drip * Math.pow(1.0D - v, 2.0D);
        hanging = hanging / (1.0D + hanging / 0.65D);
        double destU = u + flowU * edgeU * edgeV * 0.24D;
        double destV = v + flowV * edgeU * edgeV * 0.20D - hanging;
        double slideV = vertical ? drip * (0.30D + v * 0.70D) : 0.0D;
        double srcU = clampUnit(u + flowU * edgeU * edgeV);
        double srcV = clampUnit(v + (flowV - slideV) * edgeU);
        double surfacePresence = smoothstep(0.0D, 0.06D, u) * (1.0D - smoothstep(0.94D, 1.0D, u));
        double alphaMask = surfacePresence * (0.60D + nA * 0.18D + focusEnergy * 0.22D);
        return new WarpPoint(destU, destV, srcU, srcV, alphaMask);
    }

    private static FaceSurface recolor(FaceSurface surface, int red, int green, int blue) {
        return new FaceSurface(
                surface.minU(),
                surface.minV(),
                surface.maxU(),
                surface.maxV(),
                surface.c00(),
                surface.c10(),
                surface.c11(),
                surface.c01(),
                red,
                green,
                blue,
                surface.pixelWidth(),
                surface.pixelHeight()
        );
    }

    private static FaceSurface surfaceTint(FaceSurface surface, int red, int green, int blue, double amount) {
        int surfaceLum = luminance(surface.red(), surface.green(), surface.blue());
        int paletteLum = Math.max(12, luminance(red, green, blue));
        double scale = surfaceLum / (double) paletteLum;
        int adjustedRed = clamp((int) Math.round(red * scale));
        int adjustedGreen = clamp((int) Math.round(green * scale));
        int adjustedBlue = clamp((int) Math.round(blue * scale));
        return recolor(
                surface,
                mixColor(surface.red(), adjustedRed, amount),
                mixColor(surface.green(), adjustedGreen, amount),
                mixColor(surface.blue(), adjustedBlue, amount)
        );
    }

    private static int mixColor(int base, int target, double amount) {
        return clamp((int) Math.round(base + (target - base) * clampUnit(amount)));
    }

    private static int luminance(int red, int green, int blue) {
        return clamp((int) Math.round(red * 0.299D + green * 0.587D + blue * 0.114D));
    }

    private static void translateAlongFace(PoseStack poseStack, Direction face, double amount) {
        switch (face) {
            case UP -> poseStack.translate(0.0D, amount, 0.0D);
            case DOWN -> poseStack.translate(0.0D, -amount, 0.0D);
            case NORTH -> poseStack.translate(0.0D, 0.0D, -amount);
            case SOUTH -> poseStack.translate(0.0D, 0.0D, amount);
            case WEST -> poseStack.translate(-amount, 0.0D, 0.0D);
            case EAST -> poseStack.translate(amount, 0.0D, 0.0D);
        }
    }

    private static void drawWholeFaceTextureWarp(
            VertexConsumer quads,
            PoseStack.Pose pose,
            BlockPos pos,
            Direction face,
            FaceFocus focus,
            FaceSurface surface,
            MaterialProfile profile,
            PsilocybinEffectState state,
            float time,
            int light,
            double focusEnergy,
            double appearance,
            double dose,
            double doseStep,
            double heroic,
            double overload,
            double stareBuild,
            double longStare,
            double lockedStare
    ) {
        int divisions = surface.gridDivisions()
                + profile.baseDivisions()
                + (int) Math.round(doseStep * 8.0D + heroic * 6.0D + overload * 8.0D + longStare * 4.0D + lockedStare * 3.0D);
        divisions = Math.max(20, Math.min(68, divisions));

        int seed = (int) state.signature().seed() ^ surfacePlaneHash(pos, face) ^ profile.style() * 0x45D9F3B;
        double phase = ((seed & 4095) / 4095.0D) * Math.PI * 2.0D;
        double rhythm = state.signature().waveRhythm() * profile.speed() * 0.42D;
        double subtleGate = smoothstep(1.00D, 1.50D, dose);
        double morphGate = smoothstep(1.80D, 3.00D, dose);
        double deepGate = smoothstep(3.00D, 5.00D, dose);
        double chaosGate = smoothstep(8.00D, 10.0D, dose);
        FocusBlend blend = focusBlend(seed, time, dose, stareBuild, longStare, lockedStare, heroic, overload);
        double movement = 0.0018D
                + subtleGate * stareBuild * 0.008D
                + morphGate * stareBuild * 0.032D
                + deepGate * stareBuild * 0.034D
                + chaosGate * stareBuild * 0.038D
                + longStare * morphGate * 0.018D
                + lockedStare * (morphGate + deepGate) * 0.018D;
        double textureAmp = appearance * profile.textureScale() * movement
                * (0.70D + focusEnergy * 1.44D)
                * (0.72D + stareBuild * 0.28D + longStare * 0.22D + lockedStare * 0.18D)
                * (1.0D + Math.log1p(Math.max(0.0D, dose - 10.0D) / 10.0D) * 0.24D);
        textureAmp /= 1.0D + textureAmp * 3.0D;
        double geometryAmp = appearance * profile.geometryScale()
                * morphGate * (0.22D + stareBuild * 0.78D)
                * (0.0011D + doseStep * 0.0066D + heroic * 0.0078D + overload * 0.0110D)
                * (0.40D + focusEnergy * 0.84D)
                * (1.0D + lockedStare * 0.18D);
        double focusRadius = profile.focusRadius() * (0.62D + morphGate * 0.38D) + doseStep * 0.11D + longStare * 0.08D + overload * 0.08D;
        double globalWarp = profile.globalWarp() * (0.18D + morphGate * 0.72D) + doseStep * 0.10D + heroic * 0.13D + overload * 0.20D;
        int alpha = Math.min(224, clamp((int) Math.round(72.0D
                + appearance * 50.0D
                + focusEnergy * 112.0D
                + longStare * 34.0D
                + lockedStare * 30.0D
                + overload * 18.0D)));

        for (int y = 0; y < divisions; y++) {
            double v0 = y / (double) divisions;
            double v1 = (y + 1) / (double) divisions;
            for (int x = 0; x < divisions; x++) {
                double u0 = x / (double) divisions;
                double u1 = (x + 1) / (double) divisions;
                WarpPoint p00 = warpPoint(u0, v0, pos, face, focus, surface, profile, blend, time, phase, rhythm, textureAmp, geometryAmp, focusRadius, globalWarp, longStare, lockedStare, overload);
                WarpPoint p10 = warpPoint(u1, v0, pos, face, focus, surface, profile, blend, time, phase, rhythm, textureAmp, geometryAmp, focusRadius, globalWarp, longStare, lockedStare, overload);
                WarpPoint p11 = warpPoint(u1, v1, pos, face, focus, surface, profile, blend, time, phase, rhythm, textureAmp, geometryAmp, focusRadius, globalWarp, longStare, lockedStare, overload);
                WarpPoint p01 = warpPoint(u0, v1, pos, face, focus, surface, profile, blend, time, phase, rhythm, textureAmp, geometryAmp, focusRadius, globalWarp, longStare, lockedStare, overload);
                texturedFaceQuad(quads, pose, face, surface, p00, p10, p11, p01, alpha, light);
            }
        }
    }

    private static WarpPoint warpPoint(
            double u,
            double v,
            BlockPos pos,
            Direction face,
            FaceFocus focus,
            FaceSurface surface,
            MaterialProfile profile,
            FocusBlend blend,
            float time,
            double phase,
            double rhythm,
            double textureAmp,
            double geometryAmp,
            double focusRadius,
            double globalWarp,
            double longStare,
            double lockedStare,
            double overload
    ) {
        SurfacePoint point = surfacePoint(pos, face, surface.faceU(u), surface.faceV(v));
        SurfacePoint focusPoint = surfacePoint(pos, face, surface.faceU(focus.u()), surface.faceV(focus.v()));
        double fieldU = point.u();
        double fieldV = point.v();
        double du = fieldU - focusPoint.u();
        double dv = fieldV - focusPoint.v();
        double dist = Math.sqrt(du * du + dv * dv);
        double focusMask = 1.0D - smoothstep(focusRadius * 0.12D, focusRadius, dist);
        double edgeDistance = Math.min(Math.min(u, 1.0D - u), Math.min(v, 1.0D - v));
        double edgeMask = smoothstep(0.0D, profile.edgeFade(), edgeDistance);
        double edgeFeather = smoothstep(0.0D, profile.edgeFade() * 1.65D, edgeDistance);
        double surfaceMask = clampUnit(globalWarp * 0.30D + focusMask * (0.70D + longStare * 0.26D + overload * 0.22D));
        double localTime = time * (0.0060D + overload * 0.0018D) * rhythm;
        double waveA = Math.sin((fieldU * (8.0D + overload * 8.0D) + fieldV * (3.0D + longStare * 5.0D)) * Math.PI + localTime + phase);
        double waveB = Math.cos((fieldV * (10.0D + overload * 10.0D) - fieldU * (4.0D + lockedStare * 6.0D)) * Math.PI - localTime * 1.11D + phase * 0.73D);
        double swirl = Math.sin((dist * (28.0D + overload * 34.0D)) - localTime * (2.4D + overload * 1.6D) + phase);
        double tangentU = -dv / Math.max(0.025D, dist);
        double tangentV = du / Math.max(0.025D, dist);
        double radialU = du / Math.max(0.025D, dist);
        double radialV = dv / Math.max(0.025D, dist);
        double blendTime = localTime * (0.12D + blend.speed() * 0.18D);
        double microU = Math.sin((fieldU * 14.0D + fieldV * 8.0D) * Math.PI + blendTime + phase * 0.41D)
                + Math.sin((fieldU * 4.0D - fieldV * 10.0D) * Math.PI - blendTime * 0.58D + phase) * 0.45D;
        double microV = Math.cos((fieldV * 13.0D - fieldU * 6.0D) * Math.PI + blendTime * 0.70D + phase * 0.62D)
                + Math.sin((fieldU + fieldV) * 8.0D * Math.PI - blendTime * 0.44D) * 0.35D;
        double ripple = Math.sin(dist * (14.0D + blend.ripple() * 24.0D + overload * 32.0D) - blendTime * (0.82D + overload * 0.45D) + phase);
        double foldAxis = fieldU * blend.axisU() + fieldV * blend.axisV();
        double foldWave = Math.sin(foldAxis * (18.0D + overload * 30.0D) + blendTime * 0.96D + phase);
        double foldSoft = foldWave * foldWave * foldWave;
        double mandala = Math.sin(dist * (20.0D + blend.center() * 34.0D + overload * 24.0D)
                + foldAxis * (8.0D + lockedStare * 10.0D)
                - blendTime * (0.52D + overload * 0.14D)
                + phase * 0.5D);
        double mandalaSoft = mandala * (0.42D + 0.58D * Math.cos(foldAxis * 11.0D + blendTime * 0.24D + phase));
        double boilU = Math.sin((fieldU * 24.0D + fieldV * 10.0D) * Math.PI + blendTime * 0.72D + phase)
                * Math.cos((fieldV * 18.0D - fieldU * 13.0D) * Math.PI - blendTime * 0.58D);
        double boilV = Math.cos((fieldV * 23.0D + fieldU * 8.0D) * Math.PI - blendTime * 0.70D + phase * 1.3D)
                * Math.sin((fieldU * 15.0D - fieldV * 18.0D) * Math.PI + blendTime * 0.52D);
        double cellCount = 8.0D + longStare * 8.0D + lockedStare * 5.0D + overload * 12.0D;
        double pixelSeedA = smoothSurfaceNoise(fieldU * cellCount, fieldV * cellCount, phase);
        double pixelSeedB = smoothSurfaceNoise(fieldU * cellCount + 17.0D, fieldV * cellCount - 11.0D, phase * 0.67D);
        double pixelPulse = Math.sin(pixelSeedA * TAU + localTime * (0.62D + overload * 0.42D) + phase);
        double pixelCrawl = Math.cos((fieldU * cellCount * 0.72D + fieldV * cellCount * 0.51D) * TAU - localTime * (0.44D + longStare * 0.24D) + pixelSeedB * TAU);
        double pixelWeight = (0.12D + longStare * 0.25D + lockedStare * 0.20D + overload * 0.32D) * (0.45D + focusMask * 0.90D);

        double styleU = waveA * 0.56D + tangentU * swirl * 0.44D;
        double styleV = waveB * 0.56D + tangentV * swirl * 0.44D;
        if (profile.style() == 1) {
            double grain = Math.sin((fieldV * 20.0D + fieldU * 2.0D) * Math.PI + localTime * 0.54D + phase);
            double rings = Math.sin((fieldU * 6.0D + fieldV * 3.0D) * Math.PI - localTime * 0.34D + phase * 0.4D);
            styleU = grain * 0.18D + rings * 0.10D + tangentU * swirl * 0.18D;
            styleV = grain * 0.74D + waveB * 0.18D + tangentV * swirl * 0.16D;
        } else if (profile.style() == 2) {
            double cells = 6.0D + overload * 8.0D + lockedStare * 4.0D;
            double block = smoothSurfaceNoise(fieldU * cells, fieldV * cells, phase);
            double nudge = Math.sin(block * 6.28318D + localTime * 0.58D);
            styleU = nudge * 0.58D + waveA * 0.18D;
            styleV = Math.cos(block * 7.1D - localTime * 0.50D) * 0.50D + waveB * 0.16D;
        } else if (profile.style() == 3) {
            double root = Math.sin((fieldU * 10.0D - fieldV * 8.0D) * Math.PI + localTime * 0.46D + phase);
            styleU = waveA * 0.36D + root * 0.42D + tangentU * swirl * 0.30D;
            styleV = waveB * 0.36D - root * 0.30D + tangentV * swirl * 0.34D;
        } else if (profile.style() == 4) {
            double flutter = Math.sin((fieldU + fieldV) * 20.0D + localTime * 0.66D + phase);
            styleU = flutter * 0.34D + waveA * 0.28D + tangentU * swirl * 0.16D;
            styleV = Math.cos((fieldU - fieldV) * 17.0D - localTime * 0.62D + phase) * 0.34D + waveB * 0.24D;
        } else if (profile.style() == 5) {
            double weave = Math.sin(fieldV * 15.0D * Math.PI + localTime * 0.42D + phase);
            styleU = weave * 0.44D + Math.sin(fieldU * 8.0D * Math.PI - localTime * 0.34D) * 0.18D;
            styleV = waveB * 0.22D + Math.cos(fieldU * 11.0D * Math.PI + localTime * 0.31D) * 0.26D;
        } else if (profile.style() == 6) {
            double prism = Math.sin(dist * 34.0D - localTime * 1.34D + phase);
            styleU = tangentU * prism * 0.46D + waveA * 0.18D;
            styleV = tangentV * prism * 0.46D + waveB * 0.18D;
        }

        double materialWeight = 0.38D + blend.micro() * 0.34D + blend.crawl() * 0.22D + blend.ripple() * 0.16D;
        styleU *= materialWeight;
        styleV *= materialWeight;
        styleU += microU * blend.micro() * 0.22D
                + radialU * ripple * blend.ripple() * 0.58D
                + tangentU * swirl * blend.swirl() * 0.70D
                + blend.axisV() * foldSoft * blend.fold() * 0.58D
                + boilU * blend.boil() * 0.42D
                + radialU * Math.sin(blendTime * 0.72D + phase) * focusMask * blend.center() * 0.26D
                + (tangentU * mandalaSoft * 0.42D + radialU * mandala * 0.18D) * focusMask * (blend.center() + lockedStare * 0.45D);
        styleV += microV * blend.micro() * 0.22D
                + radialV * ripple * blend.ripple() * 0.58D
                + tangentV * swirl * blend.swirl() * 0.70D
                - blend.axisU() * foldSoft * blend.fold() * 0.58D
                + boilV * blend.boil() * 0.42D
                + radialV * Math.cos(blendTime * 0.66D + phase) * focusMask * blend.center() * 0.26D
                + (tangentV * mandalaSoft * 0.42D + radialV * mandala * 0.18D) * focusMask * (blend.center() + lockedStare * 0.45D);

        pixelWeight *= 0.22D + blend.crawl() * 0.78D + blend.boil() * 0.58D;
        styleU += (pixelSeedA - 0.5D) * pixelPulse * pixelWeight + pixelCrawl * 0.10D * pixelWeight;
        styleV += (pixelSeedB - 0.5D) * pixelCrawl * pixelWeight - pixelPulse * 0.08D * pixelWeight;

        double swimU = styleU * textureAmp * surfaceMask * edgeMask;
        double swimV = styleV * textureAmp * surfaceMask * edgeMask;
        double breathe = Math.sin(localTime * 0.74D + phase + dist * 18.0D) * geometryAmp * (focusMask * 0.82D + overload * 0.18D);

        double destU = clampRange(u + (tangentU * breathe + waveB * geometryAmp * 0.16D * focusMask) * edgeMask, -0.08D, 1.08D);
        double destV = clampRange(v + (tangentV * breathe + waveA * geometryAmp * 0.16D * focusMask) * edgeMask, -0.08D, 1.08D);
        double srcU = wrapUnit(u + swimU);
        double srcV = wrapUnit(v + swimV);
        double alphaMask = clampUnit((0.30D + surfaceMask * 0.74D + blend.micro() * 0.10D + blend.ripple() * 0.08D + blend.center() * 0.06D)
                * (0.28D + edgeFeather * 0.72D));
        return new WarpPoint(destU, destV, srcU, srcV, alphaMask);
    }

    private static FocusBlend focusBlend(int seed, float time, double dose, double stareBuild, double longStare, double lockedStare, double heroic, double overload) {
        double seedPhase = ((seed >>> 7) & 4095) / 4095.0D * TAU;
        double slowA = time * (0.00135D + overload * 0.00042D) + seedPhase;
        double slowB = time * (0.00105D + heroic * 0.00034D) + seedPhase * 1.73D + 0.8D;
        double slowC = time * (0.00155D + overload * 0.00040D) - seedPhase * 0.91D + 1.9D;
        double subtle = smoothstep(1.00D, 1.50D, dose);
        double morph = smoothstep(1.80D, 3.00D, dose);
        double crawlGate = smoothstep(1.50D, 2.50D, dose);
        double deep = smoothstep(3.00D, 5.00D, dose);
        double chaos = smoothstep(8.00D, 10.0D, dose);
        double stare = 0.18D + stareBuild * 0.50D + longStare * 0.22D + lockedStare * 0.10D;
        double micro = subtle * (0.70D + smoothWave(slowA) * 0.30D);
        double ripple = morph * stare * (0.18D + smoothWave(slowB) * 0.82D);
        double crawl = crawlGate * stare * (0.28D + smoothWave(slowC) * 0.72D);
        double swirl = deep * (0.42D + longStare * 0.58D) * (0.18D + smoothWave(slowA + slowB * 0.42D) * 0.82D);
        double fold = heroic * (0.38D + lockedStare * 0.62D) * (0.16D + smoothWave(slowB - slowC * 0.36D) * 0.84D);
        double boil = chaos * (0.50D + lockedStare * 0.50D) * (0.22D + smoothWave(slowC + 2.4D) * 0.78D);
        double center = smoothstep(2.20D, 3.40D, dose) * (0.32D + longStare * 0.68D) + heroic * lockedStare * 0.30D;
        double angle = seedPhase + smoothWave(slowB * 0.62D) * TAU * 0.25D;
        double speed = 0.26D + smoothWave(slowA * 0.48D + 1.1D) * 0.18D + overload * 0.06D;
        return new FocusBlend(
                micro,
                ripple,
                crawl,
                swirl,
                fold,
                boil,
                clampUnit(center),
                speed,
                Math.cos(angle),
                Math.sin(angle)
        );
    }

    private static boolean isCompleteCubeTarget(Minecraft minecraft, BlockState state, BlockPos pos) {
        if (minecraft.level == null || state.getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        if (!state.isCollisionShapeFullBlock(minecraft.level, pos)) {
            return false;
        }
        VoxelShape shape = state.getShape(minecraft.level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        AABB bounds = shape.bounds();
        return nearly(bounds.minX, 0.0D)
                && nearly(bounds.minY, 0.0D)
                && nearly(bounds.minZ, 0.0D)
                && nearly(bounds.maxX, 1.0D)
                && nearly(bounds.maxY, 1.0D)
                && nearly(bounds.maxZ, 1.0D);
    }

    private static int faceLight(Minecraft minecraft, BlockPos pos, Direction face) {
        if (minecraft.level == null) {
            return 0x00F000F0;
        }
        int blockLight = LevelRenderer.getLightColor(minecraft.level, pos);
        int faceLight = LevelRenderer.getLightColor(minecraft.level, pos.relative(face));
        return maxPackedLight(blockLight, faceLight);
    }

    private static int maxPackedLight(int a, int b) {
        int block = Math.max(a & 0xFFFF, b & 0xFFFF);
        int sky = Math.max(a >>> 16 & 0xFFFF, b >>> 16 & 0xFFFF);
        return block | sky << 16;
    }

    private static FaceSurface resolveFaceSurface(Minecraft minecraft, BlockState blockState, BlockPos pos, Direction face, FaceFocus hitFocus) {
        if (blockState.getRenderShape() != RenderShape.MODEL) {
            return null;
        }

        var model = minecraft.getBlockRenderer().getBlockModel(blockState);
        FaceSurface firstFaceSurface = null;
        List<BakedQuad> quads = model.getQuads(blockState, face, RandomSource.create(blockState.getSeed(pos)));
        for (BakedQuad quad : quads) {
            FaceSurface surface = FaceSurface.fromQuad(quad, face, quadTint(minecraft, blockState, pos, face, quad));
            if (surface == null) {
                continue;
            }
            if (firstFaceSurface == null) {
                firstFaceSurface = surface;
            }
            if (surface.contains(hitFocus)) {
                return surface;
            }
        }
        if (firstFaceSurface != null) {
            return firstFaceSurface;
        }
        TextureAtlasSprite fallback = model.getParticleIcon();
        return fallback == null ? null : FaceSurface.full(fallback, fallbackTint(minecraft, blockState, pos, face));
    }

    private static int quadTint(Minecraft minecraft, BlockState state, BlockPos pos, Direction face, BakedQuad quad) {
        float shade = minecraft.level == null ? 1.0F : minecraft.level.getShade(face, quad.isShade());
        if (!quad.isTinted() || minecraft.level == null) {
            return shadeColor(0xFFFFFF, shade);
        }
        int color = minecraft.getBlockColors().getColor(state, minecraft.level, pos, quad.getTintIndex());
        return shadeColor(color == -1 ? 0xFFFFFF : color & 0xFFFFFF, shade);
    }

    private static int fallbackTint(Minecraft minecraft, BlockState state, BlockPos pos, Direction face) {
        float shade = minecraft.level == null ? 1.0F : minecraft.level.getShade(face, true);
        if (minecraft.level == null) {
            return shadeColor(0xFFFFFF, shade);
        }
        int color = minecraft.getBlockColors().getColor(state, minecraft.level, pos);
        return shadeColor(color == -1 ? 0xFFFFFF : color & 0xFFFFFF, shade);
    }

    private static int shadeColor(int color, float shade) {
        int red = clamp((int) Math.round(((color >> 16) & 255) * shade));
        int green = clamp((int) Math.round(((color >> 8) & 255) * shade));
        int blue = clamp((int) Math.round((color & 255) * shade));
        return red << 16 | green << 8 | blue;
    }

    private static FaceFocus faceFocus(BlockPos pos, Direction face, Vec3 hitLocation) {
        double localX = hitLocation.x - pos.getX();
        double localY = hitLocation.y - pos.getY();
        double localZ = hitLocation.z - pos.getZ();
        double u;
        double v;
        switch (face) {
            case UP, DOWN -> {
                u = localX;
                v = localZ;
            }
            case NORTH, SOUTH -> {
                u = localX;
                v = localY;
            }
            case WEST, EAST -> {
                u = localZ;
                v = localY;
            }
            default -> {
                u = 0.5D;
                v = 0.5D;
            }
        }
        return new FaceFocus(clampUnit(u), clampUnit(v));
    }

    private static void texturedFaceQuad(
            VertexConsumer quads,
            PoseStack.Pose pose,
            Direction face,
            FaceSurface surface,
            WarpPoint p00,
            WarpPoint p10,
            WarpPoint p11,
            WarpPoint p01,
            int alpha,
            int light
    ) {
        switch (face) {
            case UP -> quad(quads, pose,
                    surface.faceU(p00), 1.0D + FACE_EPSILON, surface.faceV(p00), p00,
                    surface.faceU(p10), 1.0D + FACE_EPSILON, surface.faceV(p10), p10,
                    surface.faceU(p11), 1.0D + FACE_EPSILON, surface.faceV(p11), p11,
                    surface.faceU(p01), 1.0D + FACE_EPSILON, surface.faceV(p01), p01,
                    surface, alpha, light, 0.0F, 1.0F, 0.0F);
            case DOWN -> quad(quads, pose,
                    surface.faceU(p00), -FACE_EPSILON, surface.faceV(p00), p00,
                    surface.faceU(p01), -FACE_EPSILON, surface.faceV(p01), p01,
                    surface.faceU(p11), -FACE_EPSILON, surface.faceV(p11), p11,
                    surface.faceU(p10), -FACE_EPSILON, surface.faceV(p10), p10,
                    surface, alpha, light, 0.0F, -1.0F, 0.0F);
            case NORTH -> quad(quads, pose,
                    surface.faceU(p00), surface.faceV(p00), -FACE_EPSILON, p00,
                    surface.faceU(p01), surface.faceV(p01), -FACE_EPSILON, p01,
                    surface.faceU(p11), surface.faceV(p11), -FACE_EPSILON, p11,
                    surface.faceU(p10), surface.faceV(p10), -FACE_EPSILON, p10,
                    surface, alpha, light, 0.0F, 0.0F, -1.0F);
            case SOUTH -> quad(quads, pose,
                    surface.faceU(p00), surface.faceV(p00), 1.0D + FACE_EPSILON, p00,
                    surface.faceU(p10), surface.faceV(p10), 1.0D + FACE_EPSILON, p10,
                    surface.faceU(p11), surface.faceV(p11), 1.0D + FACE_EPSILON, p11,
                    surface.faceU(p01), surface.faceV(p01), 1.0D + FACE_EPSILON, p01,
                    surface, alpha, light, 0.0F, 0.0F, 1.0F);
            case WEST -> quad(quads, pose,
                    -FACE_EPSILON, surface.faceV(p00), surface.faceU(p00), p00,
                    -FACE_EPSILON, surface.faceV(p10), surface.faceU(p10), p10,
                    -FACE_EPSILON, surface.faceV(p11), surface.faceU(p11), p11,
                    -FACE_EPSILON, surface.faceV(p01), surface.faceU(p01), p01,
                    surface, alpha, light, -1.0F, 0.0F, 0.0F);
            case EAST -> quad(quads, pose,
                    1.0D + FACE_EPSILON, surface.faceV(p00), surface.faceU(p00), p00,
                    1.0D + FACE_EPSILON, surface.faceV(p01), surface.faceU(p01), p01,
                    1.0D + FACE_EPSILON, surface.faceV(p11), surface.faceU(p11), p11,
                    1.0D + FACE_EPSILON, surface.faceV(p10), surface.faceU(p10), p10,
                    surface, alpha, light, 1.0F, 0.0F, 0.0F);
        }
    }

    private static void quad(
            VertexConsumer quads,
            PoseStack.Pose pose,
            double x1, double y1, double z1, WarpPoint p1,
            double x2, double y2, double z2, WarpPoint p2,
            double x3, double y3, double z3, WarpPoint p3,
            double x4, double y4, double z4, WarpPoint p4,
            FaceSurface surface,
            int alpha,
            int light,
            float nx,
            float ny,
            float nz
    ) {
        vertex(quads, pose, x1, y1, z1, surface, p1, alpha, light, nx, ny, nz);
        vertex(quads, pose, x2, y2, z2, surface, p2, alpha, light, nx, ny, nz);
        vertex(quads, pose, x3, y3, z3, surface, p3, alpha, light, nx, ny, nz);
        vertex(quads, pose, x4, y4, z4, surface, p4, alpha, light, nx, ny, nz);
    }

    private static void vertex(
            VertexConsumer quads,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            FaceSurface surface,
            WarpPoint point,
            int alpha,
            int light,
            float nx,
            float ny,
            float nz
    ) {
        int localAlpha = clamp((int) Math.round(alpha * point.alphaMask()));
        quads.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(surface.red(), surface.green(), surface.blue(), localAlpha)
                .setUv(surface.atlasU(point.srcU(), point.srcV()), surface.atlasV(point.srcU(), point.srcV()))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static double updateTargetFade(Minecraft minecraft, BlockPos pos, Direction face) {
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (!pos.equals(fadeBlock) || face != fadeFace) {
            fadeBlock = pos;
            fadeFace = face;
            targetFade = 0.0D;
            lastFadeGameTime = gameTime;
            return 0.0D;
        }
        if (gameTime == lastFadeGameTime) {
            return smoothstep(0.0D, 1.0D, targetFade);
        }
        int delta = (int) Math.max(1L, Math.min(8L, gameTime - lastFadeGameTime));
        targetFade = clampUnit(targetFade + delta * 0.045D);
        lastFadeGameTime = gameTime;
        return smoothstep(0.0D, 1.0D, targetFade);
    }

    private static double focusDistance(Minecraft minecraft, BlockHitResult hit) {
        if (minecraft.player == null) {
            return 6.0D;
        }
        Vec3 head = VivecraftCompat.headPosition();
        return (head != null ? head : minecraft.player.getEyePosition()).distanceTo(hit.getLocation());
    }

    private static double focusDistance(Minecraft minecraft, BlockPos pos, Direction face, FaceFocus focus) {
        if (minecraft.player == null) {
            return 6.0D;
        }
        Vec3 head = VivecraftCompat.headPosition();
        return (head != null ? head : minecraft.player.getEyePosition()).distanceTo(facePoint(pos, face, focus));
    }

    private static Vec3 facePoint(BlockPos pos, Direction face, FaceFocus focus) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        return switch (face) {
            case UP -> new Vec3(x + focus.u(), y + 1.0D, z + focus.v());
            case DOWN -> new Vec3(x + focus.u(), y, z + focus.v());
            case NORTH -> new Vec3(x + focus.u(), y + focus.v(), z);
            case SOUTH -> new Vec3(x + focus.u(), y + focus.v(), z + 1.0D);
            case WEST -> new Vec3(x, y + focus.v(), z + focus.u());
            case EAST -> new Vec3(x + 1.0D, y + focus.v(), z + focus.u());
        };
    }

    private static SurfacePoint surfacePoint(BlockPos pos, Direction face, double u, double v) {
        return switch (face) {
            case UP, DOWN -> new SurfacePoint(pos.getX() + u, pos.getZ() + v);
            case NORTH, SOUTH -> new SurfacePoint(pos.getX() + u, pos.getY() + v);
            case WEST, EAST -> new SurfacePoint(pos.getZ() + u, pos.getY() + v);
        };
    }

    private static double psilocybinTripWake(PsilocybinEffectState state) {
        if (!state.active()) {
            return 0.0D;
        }
        double progressWake = smoothstep(0.025D, 0.22D, state.progress());
        double visualWake = smoothstep(0.06D, 0.85D, state.visualIntensity());
        return clampUnit(progressWake * visualWake);
    }

    private static double psilocybinSurfaceActivation(PsilocybinEffectState state, int stareTicks, BlockPos pos, Direction face) {
        if (!state.active() || state.intensity() <= 0.04D) {
            return 0.0D;
        }
        double wake = psilocybinTripWake(state);
        if (wake <= 0.0D) {
            return 0.0D;
        }
        double ramp = smoothstep(
                psilocybinActivationStartTicks(state.activeDoseGrams(), pos, face),
                psilocybinActivationStartTicks(state.activeDoseGrams(), pos, face) + psilocybinActivationRampTicks(state.activeDoseGrams()),
                stareTicks
        );
        double visual = Math.max(0.0D, state.visualIntensity());
        double intensity = visual / (6.0D + visual);
        return clampUnit(ramp * wake * (0.34D + intensity * 0.66D));
    }

    private static int psilocybinActivationStartTicks(double dose, BlockPos pos, Direction face) {
        double seconds = mix(10.0D, 8.0D, smoothstep(1.0D, 1.50D, dose));
        seconds = mix(seconds, 6.0D, smoothstep(1.50D, 3.0D, dose));
        seconds = mix(seconds, 4.2D, smoothstep(3.0D, 5.0D, dose));
        seconds = mix(seconds, 2.6D, smoothstep(5.0D, 10.0D, dose));
        double jitterWindow = mix(2.0D, 0.55D, smoothstep(1.50D, 5.0D, dose));
        seconds += surfaceRandom(pos, face, 0x6A09E667) * jitterWindow;
        return secondsToTicks(seconds);
    }

    private static int psilocybinActivationRampTicks(double dose) {
        double seconds = mix(3.0D, 2.0D, smoothstep(1.0D, 3.0D, dose));
        seconds = mix(seconds, 1.3D, smoothstep(3.0D, 5.0D, dose));
        seconds = mix(seconds, 0.9D, smoothstep(5.0D, 10.0D, dose));
        return secondsToTicks(seconds);
    }

    private static int psilocybinStabilizeTicks(double dose, BlockPos pos, Direction face) {
        return psilocybinActivationStartTicks(dose, pos, face) + psilocybinActivationRampTicks(dose);
    }

    private static double psilocybinDoseWake(double dose) {
        return smoothstep(1.0D, 3.0D, dose);
    }

    private static double lsdSurfaceActivation(LsdEffectState state, int stareTicks, BlockPos pos, Direction face) {
        if (!state.active() || state.intensity() <= 0.04D) {
            return 0.0D;
        }
        double dose = state.activeDoseMicrograms();
        double start = lsdActivationStartTicks(dose);
        double ramp = smoothstep(start, start + lsdActivationRampTicks(dose), stareTicks);
        double wake = smoothstep(0.04D, 0.70D, state.visualIntensity());
        double tierStrength = clampUnit(lsdVisualTier(dose) / 5.0D);
        return clampUnit(ramp * wake * (0.30D + tierStrength * 0.70D));
    }

    private static int lsdActivationStartTicks(double micrograms) {
        double seconds = mix(12.0D, 5.0D, smoothstep(45.0D, 130.0D, micrograms));
        seconds = mix(seconds, 2.8D, smoothstep(170.0D, 320.0D, micrograms));
        seconds = mix(seconds, 1.4D, smoothstep(400.0D, 700.0D, micrograms));
        return secondsToTicks(seconds);
    }

    private static int lsdActivationRampTicks(double micrograms) {
        double seconds = mix(4.0D, 2.4D, smoothstep(45.0D, 130.0D, micrograms));
        seconds = mix(seconds, 1.6D, smoothstep(170.0D, 320.0D, micrograms));
        seconds = mix(seconds, 1.1D, smoothstep(400.0D, 700.0D, micrograms));
        return secondsToTicks(seconds);
    }

    private static int lsdStabilizeTicks(double micrograms) {
        return lsdActivationStartTicks(micrograms) + lsdActivationRampTicks(micrograms);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }

    private static double perGramDoseStep(double dose) {
        if (dose < 1.0D) {
            return 0.0D;
        }
        if (dose >= 10.0D) {
            return 1.0D + Math.log(Math.hypot(1.0D, (dose - 10.0D) / 10.0D));
        }
        double clamped = Math.max(1.0D, Math.min(10.0D, dose));
        double lower = Math.floor(clamped * 2.0D) / 2.0D;
        double upper = Math.min(10.0D, lower + 0.5D);
        double lowerValue = Math.max(0.0D, (lower - 1.0D) / 9.0D);
        double upperValue = Math.max(0.0D, (upper - 1.0D) / 9.0D);
        return lowerValue + (upperValue - lowerValue) * smoothstep(lower, upper, clamped);
    }

    private static double surfaceRandom(BlockPos pos, Direction face, int salt) {
        if (pos == null || face == null) {
            return 0.5D;
        }
        int value = surfaceHash(pos, face) ^ salt;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return (value & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private static double lsdVisualTier(double micrograms) {
        double dose = Math.max(0.0D, micrograms);
        return smoothstep(35.0D, 60.0D, dose)
                + smoothstep(80.0D, 120.0D, dose)
                + smoothstep(135.0D, 210.0D, dose)
                + smoothstep(210.0D, 285.0D, dose)
                + smoothstep(800.0D, 1000.0D, dose);
    }

    private static double lsdOutlineVisibility(double micrograms) {
        return 1.0D - smoothstep(LSD_OUTLINE_FADE_START_MICROGRAMS, LSD_OUTLINE_FADE_END_MICROGRAMS, micrograms);
    }

    private static double localFaceU(Direction face, float x, float y, float z) {
        return switch (face) {
            case UP, DOWN, NORTH, SOUTH -> x;
            case WEST, EAST -> z;
        };
    }

    private static double localFaceV(Direction face, float x, float y, float z) {
        return switch (face) {
            case UP, DOWN -> z;
            case NORTH, SOUTH, WEST, EAST -> y;
        };
    }

    private static double hashNoise(double x, double y, double seed) {
        double value = Math.sin(x * 12.9898D + y * 78.233D + seed * 37.719D) * 43758.5453D;
        return value - Math.floor(value);
    }

    private static double smoothSurfaceNoise(double x, double y, double seed) {
        double cellX = Math.floor(x);
        double cellY = Math.floor(y);
        double u = x - cellX;
        double v = y - cellY;
        u = u * u * u * (u * (u * 6.0D - 15.0D) + 10.0D);
        v = v * v * v * (v * (v * 6.0D - 15.0D) + 10.0D);
        return mix(mix(hashNoise(cellX, cellY, seed), hashNoise(cellX + 1.0D, cellY, seed), u),
                mix(hashNoise(cellX, cellY + 1.0D, seed), hashNoise(cellX + 1.0D, cellY + 1.0D, seed), u), v);
    }

    private static int surfaceHash(BlockPos pos, Direction face) {
        int value = pos.getX() * 0x1F1F1F1F ^ pos.getY() * 0x45D9F3B ^ pos.getZ() * 0x119DE1F3 ^ face.ordinal() * 0x3449B13;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return value;
    }

    private static int surfacePlaneHash(BlockPos pos, Direction face) {
        int plane = switch (face) {
            case UP, DOWN -> pos.getY();
            case NORTH, SOUTH -> pos.getZ();
            case WEST, EAST -> pos.getX();
        };
        int value = plane * 0x45D9F3B ^ face.ordinal() * 0x3449B13;
        value ^= value >>> 16;
        value *= 0x45D9F3B;
        value ^= value >>> 16;
        return value;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = clampUnit((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double smoothWave(double radians) {
        double value = Math.sin(radians) * 0.5D + 0.5D;
        return value * value * (3.0D - 2.0D * value);
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * clampUnit(t);
    }

    private static boolean nearly(double value, double target) {
        return Math.abs(value - target) < 0.0001D;
    }

    private static double clampUnit(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double clampRange(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double wrapUnit(double value) {
        double wrapped = value - Math.floor(value);
        return wrapped >= 1.0D ? 0.0D : wrapped;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record RenderContext(PoseStack poseStack, Vec3 camera, float partialTick) {
    }

    private record ActiveSurface(
            BlockPos pos,
            Direction face,
            FaceFocus focus,
            int psilocybinStareTicks,
            int lsdStareTicks,
            int timeOffset
    ) {
        boolean matches(BlockPos otherPos, Direction otherFace) {
            return otherPos != null && pos.equals(otherPos) && face == otherFace;
        }
    }

    private record FadingSurface(ActiveSurface surface, long startedGameTime) {
        boolean matches(BlockPos pos, Direction face) {
            return surface.matches(pos, face);
        }

        double presence(long gameTime) {
            long age = Math.max(0L, gameTime - startedGameTime);
            return Math.max(0.0D, 1.0D - smoothstep(0.0D, SURFACE_FADE_TICKS, age));
        }
    }

    private record SurfaceGaze(int psilocybinTicks, int lsdTicks) {
    }

    private record FocusBlend(
            double micro,
            double ripple,
            double crawl,
            double swirl,
            double fold,
            double boil,
            double center,
            double speed,
            double axisU,
            double axisV
    ) {
    }

    private record FaceFocus(double u, double v) {
    }

    private record SurfacePoint(double u, double v) {
    }

    private record WarpPoint(double u, double v, double srcU, double srcV, double alphaMask) {
    }

    private record SurfaceCorner(double faceU, double faceV, float atlasU, float atlasV) {
    }

    private record FaceSurface(
            double minU,
            double minV,
            double maxU,
            double maxV,
            SurfaceCorner c00,
            SurfaceCorner c10,
            SurfaceCorner c11,
            SurfaceCorner c01,
            int red,
            int green,
            int blue,
            int pixelWidth,
            int pixelHeight
    ) {
        static FaceSurface fromQuad(BakedQuad quad, Direction face, int tintColor) {
            int[] vertices = quad.getVertices();
            if (vertices.length < 24 || quad.getSprite() == null) {
                return null;
            }
            int stride = vertices.length / 4;
            SurfaceCorner[] corners = new SurfaceCorner[4];
            double minU = 1.0D;
            double minV = 1.0D;
            double maxU = 0.0D;
            double maxV = 0.0D;
            for (int i = 0; i < 4; i++) {
                int base = i * stride;
                float x = Float.intBitsToFloat(vertices[base]);
                float y = Float.intBitsToFloat(vertices[base + 1]);
                float z = Float.intBitsToFloat(vertices[base + 2]);
                double faceU = clampUnit(localFaceU(face, x, y, z));
                double faceV = clampUnit(localFaceV(face, x, y, z));
                float atlasU = Float.intBitsToFloat(vertices[base + 4]);
                float atlasV = Float.intBitsToFloat(vertices[base + 5]);
                corners[i] = new SurfaceCorner(faceU, faceV, atlasU, atlasV);
                minU = Math.min(minU, faceU);
                minV = Math.min(minV, faceV);
                maxU = Math.max(maxU, faceU);
                maxV = Math.max(maxV, faceV);
            }
            if (maxU - minU < 0.001D || maxV - minV < 0.001D) {
                return null;
            }
            return new FaceSurface(
                    minU,
                    minV,
                    maxU,
                    maxV,
                    nearest(corners, minU, minV),
                    nearest(corners, maxU, minV),
                    nearest(corners, maxU, maxV),
                    nearest(corners, minU, maxV),
                    red(tintColor),
                    green(tintColor),
                    blue(tintColor),
                    quad.getSprite().contents().width(),
                    quad.getSprite().contents().height()
            );
        }

        static FaceSurface full(TextureAtlasSprite sprite, int tintColor) {
            return new FaceSurface(
                    0.0D,
                    0.0D,
                    1.0D,
                    1.0D,
                    new SurfaceCorner(0.0D, 0.0D, sprite.getU0(), sprite.getV0()),
                    new SurfaceCorner(1.0D, 0.0D, sprite.getU1(), sprite.getV0()),
                    new SurfaceCorner(1.0D, 1.0D, sprite.getU1(), sprite.getV1()),
                    new SurfaceCorner(0.0D, 1.0D, sprite.getU0(), sprite.getV1()),
                    red(tintColor),
                    green(tintColor),
                    blue(tintColor),
                    sprite.contents().width(),
                    sprite.contents().height()
            );
        }

        boolean contains(FaceFocus focus) {
            return focus.u() >= minU - 0.0005D
                    && focus.u() <= maxU + 0.0005D
                    && focus.v() >= minV - 0.0005D
                    && focus.v() <= maxV + 0.0005D;
        }

        FaceFocus normalize(FaceFocus focus) {
            return new FaceFocus(
                    clampUnit((focus.u() - minU) / Math.max(0.001D, maxU - minU)),
                    clampUnit((focus.v() - minV) / Math.max(0.001D, maxV - minV))
            );
        }

        double faceU(WarpPoint point) {
            return faceU(point.u());
        }

        double faceV(WarpPoint point) {
            return faceV(point.v());
        }

        double faceU(double u) {
            return minU + (maxU - minU) * u;
        }

        double faceV(double v) {
            return minV + (maxV - minV) * v;
        }

        float atlasU(double u, double v) {
            double top = mix(c00.atlasU(), c10.atlasU(), u);
            double bottom = mix(c01.atlasU(), c11.atlasU(), u);
            return (float) mix(top, bottom, v);
        }

        float atlasV(double u, double v) {
            double top = mix(c00.atlasV(), c10.atlasV(), u);
            double bottom = mix(c01.atlasV(), c11.atlasV(), u);
            return (float) mix(top, bottom, v);
        }

        int gridDivisions() {
            return Math.max(14, Math.min(34, Math.max(pixelWidth, pixelHeight)));
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

        private static SurfaceCorner nearest(SurfaceCorner[] corners, double u, double v) {
            SurfaceCorner best = corners[0];
            double bestDistance = Double.MAX_VALUE;
            for (SurfaceCorner corner : corners) {
                double du = corner.faceU() - u;
                double dv = corner.faceV() - v;
                double distance = du * du + dv * dv;
                if (distance < bestDistance) {
                    best = corner;
                    bestDistance = distance;
                }
            }
            return best;
        }

        private static double mix(double a, double b, double t) {
            return a + (b - a) * clampUnit(t);
        }
    }

    private record MaterialProfile(
            int style,
            int baseDivisions,
            double alphaBase,
            double alphaRange,
            double textureScale,
            double geometryScale,
            double speed,
            double focusRadius,
            double globalWarp,
            double edgeFade
    ) {
        static MaterialProfile forBlock(BlockState state) {
            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (containsAny(path, "log", "wood", "stem", "hyphae", "planks", "bamboo")) {
                return new MaterialProfile(1, 16, 78.0D, 62.0D, 0.78D, 0.46D, 0.92D, 0.24D, 0.16D, 0.105D);
            }
            if (containsAny(path, "stone", "deepslate", "cobble", "granite", "diorite", "andesite", "tuff", "basalt", "blackstone", "brick", "ore", "slate")) {
                return new MaterialProfile(2, 12, 70.0D, 54.0D, 0.64D, 0.38D, 0.74D, 0.21D, 0.13D, 0.095D);
            }
            if (containsAny(path, "dirt", "grass", "mud", "moss", "nylium", "podzol", "sand", "gravel", "clay", "farmland")) {
                return new MaterialProfile(3, 14, 72.0D, 58.0D, 0.78D, 0.52D, 0.88D, 0.25D, 0.18D, 0.115D);
            }
            if (containsAny(path, "leaves", "flower", "vine", "fern", "sapling", "kelp", "crop", "mushroom", "roots")) {
                return new MaterialProfile(4, 18, 62.0D, 50.0D, 0.58D, 0.34D, 1.18D, 0.26D, 0.15D, 0.120D);
            }
            if (containsAny(path, "bed", "wool", "carpet", "banner", "cloth")) {
                return new MaterialProfile(5, 16, 64.0D, 48.0D, 0.52D, 0.28D, 0.78D, 0.23D, 0.12D, 0.130D);
            }
            if (containsAny(path, "glass", "ice", "crystal", "amethyst", "lantern", "glowstone", "sea_lantern")) {
                return new MaterialProfile(6, 18, 54.0D, 42.0D, 0.46D, 0.22D, 1.05D, 0.24D, 0.10D, 0.135D);
            }
            return new MaterialProfile(0, 14, 68.0D, 52.0D, 0.62D, 0.36D, 0.86D, 0.22D, 0.14D, 0.110D);
        }
    }
}
