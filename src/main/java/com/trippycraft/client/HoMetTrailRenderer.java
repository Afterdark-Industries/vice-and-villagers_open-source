package com.trippycraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.trippycraft.config.TrippyClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.*;

/** Bounded history of posed, textured meshes in world space. Never stores screen frames. */
public final class HoMetTrailRenderer {
    private static final int MAX_ENTITIES = 16;
    private static final int MAX_SNAPSHOTS = 20;
    private static final int MAX_VERTICES = 4096;
    private static final int FRAME_VERTEX_BUDGET = 160_000;
    private static final int MAX_PARTICLES = 96;
    private static final Map<UUID, Deque<MeshEcho>> ENTITIES = new LinkedHashMap<>();
    private static final Map<InteractionHand, Deque<MeshEcho>> HANDS = new EnumMap<>(InteractionHand.class);
    private static final Map<Particle, Deque<PathPoint>> PARTICLES = new IdentityHashMap<>();
    private static final Map<Integer, Deque<BeamEcho>> BEAMS = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> MATERIALS = new HashMap<>();
    private static final Map<ResourceLocation, Integer> SOURCE_COLORS = new HashMap<>();
    private static final Map<ItemColorKey, Integer> ITEM_COLORS = new HashMap<>();
    private static final Map<InteractionHand, org.joml.Quaternionf> VR_ROTATIONS = new EnumMap<>(InteractionHand.class);
    private static final RenderType RIBBON = RenderType.create("four_ho_met_ribbons",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
    private static ClientLevel previousLevel;
    private static Vec3 previousPlayer;
    private static long previousSeed = Long.MIN_VALUE;
    private static long lastTick = Long.MIN_VALUE;
    private static boolean capturing;
    private static boolean previousVr;

    private HoMetTrailRenderer() { }

    public static void clear() {
        ENTITIES.clear();
        HANDS.clear();
        PARTICLES.clear();
        BEAMS.clear();
        MATERIALS.clear();
        SOURCE_COLORS.clear();
        ITEM_COLORS.clear();
        VR_ROTATIONS.clear();
        previousPlayer = null;
        previousLevel = null;
        previousSeed = Long.MIN_VALUE;
        lastTick = Long.MIN_VALUE;
    }

    public static void onRegisterReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, resources, preparations, reload, background, game) ->
                barrier.<Void>wait(null).thenRunAsync(HoMetTrailRenderer::clear, game));
    }

    private static double strength() {
        var state = ClientExperienceStore.hoMet();
        if (!TrippyClientConfig.rendererEnabled || !state.active()) return 0.0D;
        double presence = Math.min(1.0D, state.visualPresence());
        double load = state.trails();
        // Other substances modulate one temporal history rather than allocating competing copies.
        var lsd = ClientExperienceStore.lsd();
        var cannabis = ClientExperienceStore.cannabis();
        var ketamine = ClientExperienceStore.ketamine();
        if (lsd.active()) load += Math.min(0.24D, lsd.entityTrails() * 0.04D);
        if (cannabis.active()) load += Math.min(0.18D, cannabis.temporalDrag() * 0.06D);
        if (ketamine.active()) load += Math.min(0.14D, ketamine.bodyEcho() * 0.04D);
        // The channel keeps growing with dose; only its numerical range is compressed.
        double result = presence * Math.log1p(Math.max(0.0D, load))
                * Math.min(1.5D, Math.max(0.0D, TrippyClientConfig.visualIntensity));
        if (TrippyClientConfig.photosensitivityReduction) result *= 0.55D;
        if (TrippyClientConfig.motionReduction) result *= 0.55D;
        return result;
    }

    public static String smokeDiagnostics() {
        int samples = ENTITIES.values().stream().mapToInt(Deque::size).sum();
        int vertices = ENTITIES.values().stream().flatMap(Collection::stream)
                .flatMap(echo -> echo.parts.stream()).mapToInt(part -> part.vertices.size()).sum();
        return String.format(java.util.Locale.ROOT, "trailStrength=%.3f, trailLifetime=%.2ft, ", strength(), lifetime())
                + "sources=" + ENTITIES.size() + ", meshSnapshots=" + samples + ", capturedVertices=" + vertices
                + ", handSnapshots=" + HANDS.values().stream().mapToInt(Deque::size).sum()
                + ", particlePaths=" + PARTICLES.size() + ", beamPaths=" + BEAMS.size()
                + ", itemSourceColors=" + ITEM_COLORS.values().stream().distinct()
                .map(rgb -> String.format(java.util.Locale.ROOT, "#%06X", rgb)).sorted().toList();
    }

    public static Set<Integer> smokeItemSourceColors() {
        return Set.copyOf(ITEM_COLORS.values());
    }

    private static double lifetime() {
        double complexity = Math.log1p(Math.max(0.0D, ClientExperienceStore.hoMet().complexity()));
        return 7.0D + strength() * 24.0D + complexity * 2.0D;
    }

    private static double sampleInterval(int historyBudget, double minimum) {
        // Spread the same number of meshes over a longer path as dose rises.
        return Math.max(minimum, lifetime() / (historyBudget - 1.0D));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || strength() < 0.004D) {
            clear();
            return;
        }
        long now = mc.level.getGameTime();
        long seed = ClientExperienceStore.hoMet().signature().seed();
        boolean vr = VivecraftCompat.isVrActive();
        if (previousLevel != mc.level || previousSeed != seed || previousVr != vr
                || now < lastTick || (previousPlayer != null && previousPlayer.distanceToSqr(mc.player.position()) > 256.0D)) {
            clear();
        }
        previousLevel = mc.level;
        previousSeed = seed;
        previousVr = vr;
        previousPlayer = mc.player.position();
        if (mc.isPaused() || lastTick == now) return;
        lastTick = now;
        double oldest = now - lifetime();
        ENTITIES.values().forEach(history -> expireMeshes(history, oldest));
        ENTITIES.values().removeIf(Deque::isEmpty);
        HANDS.values().forEach(history -> expireMeshes(history, oldest));
        BEAMS.values().forEach(history -> history.removeIf(echo -> echo.time < oldest));
        BEAMS.values().removeIf(Deque::isEmpty);
        PARTICLES.values().forEach(history -> history.removeIf(point -> point.time < oldest));
        PARTICLES.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        int inspected = 0;
        outer: for (var queue : mc.particleEngine.particles.values()) {
            for (Particle particle : queue) {
                if (++inspected > 768) break outer;
                if (!particle.isAlive()) continue;
                Vec3 position = particle.getBoundingBox().getCenter();
                if (position.distanceToSqr(mc.player.position()) > 24.0D * 24.0D) continue;
                if (!PARTICLES.containsKey(particle) && PARTICLES.size() >= MAX_PARTICLES) continue;
                Deque<PathPoint> path = PARTICLES.computeIfAbsent(particle, ignored -> new ArrayDeque<>());
                if (path.isEmpty() || (now - path.peekLast().time >= sampleInterval(28, 1.0D)
                        && path.peekLast().position.distanceToSqr(position) > 0.00008D)) {
                    int rgb = ((int) (particle.rCol * 255.0F) << 16)
                            | ((int) (particle.gCol * 255.0F) << 8) | (int) (particle.bCol * 255.0F);
                    path.addLast(new PathPoint(position, now, rgb, particle.alpha));
                    while (path.size() > 28) path.removeFirst();
                }
            }
        }
        // Nonliving moving sources do not fire RenderLivingEvent.
        if (now % 2L == 0L) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof Projectile || entity instanceof ItemEntity) || entity.isInvisible()) continue;
                if (entity.position().distanceToSqr(mc.player.position()) > 24.0D * 24.0D) continue;
                captureEntity(entity, 1.0F, now);
            }
        }
    }

    public static void onLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (capturing || strength() < 0.004D) return;
        Minecraft mc = Minecraft.getInstance();
        LivingEntity entity = event.getEntity();
        if (mc.level == null || mc.player == null || entity.isInvisibleTo(mc.player)
                || entity.position().distanceToSqr(mc.player.position()) > 28.0D * 28.0D) return;
        captureEntity(entity, event.getPartialTick(), mc.level.getGameTime() + event.getPartialTick());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void captureEntity(Entity entity, float partialTick, double time) {
        Deque<MeshEcho> history = ENTITIES.get(entity.getUUID());
        if (history == null && ENTITIES.size() >= MAX_ENTITIES) return;
        Vec3 position = entity.getPosition(partialTick);
        MeshEcho previous = history == null ? null : history.peekLast();
        if (previous != null) {
            if (time - previous.time < sampleInterval(MAX_SNAPSHOTS, 2.0D)) return;
            double motionAllowance = Math.max(6.0D, (time - previous.time) * 4.0D);
            if (previous.position.distanceToSqr(position) > motionAllowance * motionAllowance) history.clear();
            else if (previous.position.distanceToSqr(position) < 0.001D
                    && Math.abs(net.minecraft.util.Mth.wrapDegrees(entity.getYRot() - previous.yaw)) < 3.0F) return;
        }
        Minecraft mc = Minecraft.getInstance();
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        CaptureBuffer capture = new CaptureBuffer();
        capturing = true;
        try {
            renderer.render(entity, entity.getYRot(), partialTick, new PoseStack(), capture,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick));
        } finally {
            capturing = false;
        }
        List<MeshPart> parts = capture.finish();
        if (parts.isEmpty()) return;
        if (history == null) {
            history = new ArrayDeque<>();
            ENTITIES.put(entity.getUUID(), history);
        }
        net.minecraft.world.item.ItemStack item = entity instanceof ItemEntity dropped ? dropped.getItem()
                : entity instanceof net.minecraft.world.entity.projectile.ThrowableItemProjectile thrown ? thrown.getItem()
                : net.minecraft.world.item.ItemStack.EMPTY;
        history.addLast(new MeshEcho(position, time, entity.getYRot(), parts, itemColor(item)));
        while (history.size() > MAX_SNAPSHOTS) history.removeFirst();
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || strength() < 0.004D) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || previousLevel != mc.level) return;
        double now = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        try (ByteBufferBuilder storage = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(storage);
            PoseStack pose = new PoseStack();
            int remaining = FRAME_VERTEX_BUDGET;
            List<MeshEcho> echoes = new ArrayList<>();
            // Exclude the newest sample: it is the real object's current position.
            ENTITIES.values().forEach(history -> history.stream()
                    .filter(echo -> echo != history.peekLast()).forEach(echoes::add));
            if (VivecraftCompat.isVrWorldPass()) {
                captureVrHands(now, event.getPartialTick().getGameTimeDeltaPartialTick(false));
                HANDS.values().forEach(history -> history.stream()
                        .filter(echo -> echo != history.peekLast()).forEach(echoes::add));
            }
            // Admit nearby/young samples first under load, then render admitted meshes back-to-front.
            echoes.sort(Comparator.comparingDouble((MeshEcho echo) -> echo.position.distanceToSqr(camera)
                    + (now - echo.time) * 0.35D));
            List<MeshEcho> admitted = new ArrayList<>();
            for (MeshEcho echo : echoes) {
                double alpha = opacity(now - echo.time);
                if (alpha <= 0.003D || now - echo.time < 1.2D) continue;
                int count = echo.parts.stream().mapToInt(part -> part.vertices.size()).sum();
                if (count > remaining) continue;
                remaining -= count;
                admitted.add(echo);
            }
            admitted.sort(Comparator.comparingDouble((MeshEcho echo) -> echo.position.distanceToSqr(camera)).reversed());
            for (MeshEcho echo : admitted) {
                double alpha = opacity(now - echo.time);
                Vec3 offset = bend(echo.position, now - echo.time, now).subtract(camera);
                renderMesh(echo, pose, buffers, offset, alpha);
            }
            VertexConsumer ribbon = buffers.getBuffer(RIBBON);
            for (Deque<PathPoint> path : PARTICLES.values()) {
                PathPoint previous = null;
                for (PathPoint point : path) {
                    if (previous != null) {
                        double alpha = opacity(now - point.time) * 1.5D * point.alpha;
                        double motionAllowance = 3.0D * Math.max(1.0D, point.time - previous.time);
                        if (point.position.distanceToSqr(previous.position) < motionAllowance * motionAllowance) {
                            ribbon(ribbon, pose.last(), bend(previous.position, now - previous.time, now).subtract(camera),
                                    bend(point.position, now - point.time, now).subtract(camera),
                                    0.018D + strength() * 0.024D, point.color, alpha);
                        }
                    }
                    previous = point;
                }
            }
            // Fast sources connect their historical positions with luminous source-colored strips.
            for (Deque<MeshEcho> history : ENTITIES.values()) {
                MeshEcho previous = null;
                for (MeshEcho echo : history) {
                    if (previous != null && echo.position.distanceToSqr(previous.position) > 0.07D) {
                        ribbon(ribbon, pose.last(), previous.position.add(0, 0.25D, 0).subtract(camera),
                                echo.position.add(0, 0.25D, 0).subtract(camera), 0.018D + strength() * 0.025D,
                                sourceColor(echo), opacity(now - echo.time) * 0.48D);
                    }
                    previous = echo;
                }
            }
            for (Deque<BeamEcho> history : BEAMS.values()) {
                BeamEcho previous = null;
                for (BeamEcho beam : history) {
                    if (previous != null) {
                        double alpha = opacity(now - beam.time) * 0.38D;
                        quad(ribbon, pose.last(), previous.source.subtract(camera), previous.target.subtract(camera),
                                beam.target.subtract(camera), beam.source.subtract(camera), beam.color, alpha);
                    }
                    previous = beam;
                }
            }
            buffers.endBatch();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Called by the native tavern light renderer with world coordinates and its current color. */
    public static void captureBeam(int id, Vec3 source, Vec3 target, int rgb, double time) {
        if (strength() < 0.004D) return;
        Deque<BeamEcho> history = BEAMS.computeIfAbsent(id, ignored -> new ArrayDeque<>());
        if (!history.isEmpty() && time - history.peekLast().time < sampleInterval(24, 1.5D)) return;
        history.addLast(new BeamEcho(source, target, rgb, time));
        while (history.size() > 24) history.removeFirst();
    }

    public static void onRenderHand(RenderHandEvent event) {
        // Tracked VR hands use a different renderer. Never apply a desktop projection to an HMD hand.
        if (capturing || VivecraftCompat.isVrActive() || strength() < 0.004D) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isScoping()) return;
        double now = mc.level.getGameTime() + event.getPartialTick();
        Deque<MeshEcho> history = HANDS.computeIfAbsent(event.getHand(), ignored -> new ArrayDeque<>());
        double motion = event.getSwingProgress() + event.getEquipProgress();
        Vec3 signature = new Vec3(mc.player.getYRot() * 0.01D, mc.player.getXRot() * 0.01D, motion);
        if (history.isEmpty() || now - history.peekLast().time >= sampleInterval(12, 1.5D)) {
            MeshEcho previous = history.peekLast();
            if (previous == null || signature.distanceToSqr(previous.position) > 0.0003D) {
                CaptureBuffer capture = new CaptureBuffer();
                capturing = true;
                try {
                    mc.gameRenderer.itemInHandRenderer.renderArmWithItem(mc.player, event.getPartialTick(),
                            event.getInterpolatedPitch(), event.getHand(), event.getSwingProgress(), event.getItemStack(),
                            event.getEquipProgress(), new PoseStack(), capture, event.getPackedLight());
                } finally {
                    capturing = false;
                }
                history.addLast(new MeshEcho(signature, now, mc.player.getYRot(), capture.finish(), -1));
                while (history.size() > 12) history.removeFirst();
            }
        }
        expireMeshes(history, now - lifetime());
        try (ByteBufferBuilder storage = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(storage);
            for (MeshEcho echo : history) {
                double age = now - echo.time;
                if (age < 1.5D) continue;
                PoseStack pose = new PoseStack();
                pose.mulPose(event.getPoseStack().last().pose());
                float yawDelta = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() - echo.yaw);
                float pitchDelta = (float) (mc.player.getXRot() - echo.position.y * 100.0D);
                double amount = TrippyClientConfig.motionReduction ? 0.18D : 0.45D;
                pose.mulPose(Axis.YP.rotationDegrees((float) (Math.max(-16.0D, Math.min(16.0D, yawDelta)) * amount)));
                pose.mulPose(Axis.XP.rotationDegrees((float) (Math.max(-12.0D, Math.min(12.0D, pitchDelta)) * amount)));
                pose.translate(0.0D, 0.0D, -age * 0.002D);
                renderMesh(echo, pose, buffers, Vec3.ZERO, opacity(age) * 0.62D);
            }
            buffers.endBatch();
        }
    }

    private static void captureVrHands(double now, float partialTick) {
        if (!VivecraftCompat.isFirstWorldPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            VivecraftCompat.HandPose grip = VivecraftCompat.handPose(hand);
            if (grip == null) continue;
            Deque<MeshEcho> history = HANDS.computeIfAbsent(hand, ignored -> new ArrayDeque<>());
            float angle = grip.rotation().angle();
            org.joml.Quaternionf previousRotation = VR_ROTATIONS.get(hand);
            MeshEcho previous = history.peekLast();
            if (previous != null && (now - previous.time < sampleInterval(12, 1.5D)
                    || (previous.position.distanceToSqr(grip.position()) < 0.0001D
                    && previousRotation != null && Math.abs(previousRotation.dot(grip.rotation())) > 0.9997F))) continue;
            if (previous != null) {
                double motionAllowance = 3.0D * Math.max(1.0D, (now - previous.time) / 1.5D);
                if (previous.position.distanceToSqr(grip.position()) > motionAllowance * motionAllowance) history.clear();
            }
            CaptureBuffer capture = new CaptureBuffer();
            PoseStack pose = new PoseStack();
            pose.mulPose(grip.rotation());
            boolean left = (hand == InteractionHand.MAIN_HAND) == (mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
            int light = mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partialTick);
            capturing = true;
            try {
                pose.pushPose();
                pose.translate(0.0D, -0.025D, -0.10D);
                mc.getItemRenderer().renderStatic(mc.player, mc.player.getItemInHand(hand),
                        left ? net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                                : net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        left, pose, capture, mc.level, light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, mc.player.getId());
                pose.popPose();
                if (!mc.player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(mc.player)
                        instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                    pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    pose.translate(left ? -0.3125D : 0.3125D, -0.75D, 0.0D);
                    if (left) renderer.renderLeftHand(pose, capture, light, mc.player);
                    else renderer.renderRightHand(pose, capture, light, mc.player);
                }
            } finally {
                capturing = false;
            }
            List<MeshPart> parts = capture.finish();
            if (!parts.isEmpty()) history.addLast(new MeshEcho(grip.position(), now, angle, parts, -1));
            VR_ROTATIONS.put(hand, new org.joml.Quaternionf(grip.rotation()));
            while (history.size() > 12) history.removeFirst();
        }
    }

    private static double opacity(double age) {
        double normalized = Math.max(0.0D, age) / lifetime();
        if (normalized >= 1.0D) return 0.0D;
        // Alpha is a physical blend weight; length, width and curvature continue growing.
        return (1.0D - Math.exp(-strength() * 0.26D)) * Math.pow(1.0D - normalized, 1.7D);
    }

    private static Vec3 bend(Vec3 point, double age, double time) {
        if (TrippyClientConfig.motionReduction) return point;
        double amount = strength() * Math.min(1.0D, age / 20.0D) * 0.045D
                * Math.log1p(Math.max(0.0D, ClientExperienceStore.hoMet().complexity()));
        return point.add(Math.sin(point.z * 0.42D + time * 0.012D) * amount,
                Math.sin(point.x * 0.32D + point.z * 0.26D - time * 0.016D) * amount, 0.0D);
    }

    private static int sourceColor(MeshEcho echo) {
        if (echo.ribbonColor >= 0) return echo.ribbonColor;
        if (echo.parts.isEmpty() || echo.parts.getFirst().vertices.isEmpty()) return 0xFFFFFF;
        if (SOURCE_COLORS.size() >= 128) SOURCE_COLORS.clear();
        return SOURCE_COLORS.computeIfAbsent(echo.parts.getFirst().texture, texture -> {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) return echo.parts.getFirst().vertices.getFirst().color & 0xFFFFFF;
            try (var input = resource.get().open(); var pixels = com.mojang.blaze3d.platform.NativeImage.read(input)) {
                long red = 0, green = 0, blue = 0, count = 0;
                int step = Math.max(1, Math.max(pixels.getWidth(), pixels.getHeight()) / 32);
                for (int y = 0; y < pixels.getHeight(); y += step) {
                    for (int x = 0; x < pixels.getWidth(); x += step) {
                        int abgr = pixels.getPixelRGBA(x, y);
                        if ((abgr >>> 24) < 96) continue;
                        red += abgr & 255; green += abgr >> 8 & 255; blue += abgr >> 16 & 255; count++;
                    }
                }
                if (count == 0) return 0xFFFFFF;
                return (int) (red / count) << 16 | (int) (green / count) << 8 | (int) (blue / count);
            } catch (java.io.IOException ignored) {
                return 0xFFFFFF;
            }
        });
    }

    private static int itemColor(net.minecraft.world.item.ItemStack item) {
        if (item.isEmpty()) return -1;
        Minecraft mc = Minecraft.getInstance();
        var sprite = mc.getItemRenderer().getModel(item, mc.level, null, 0)
                .getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
        int tint = mc.getItemColors().getColor(item, 0) & 0xFFFFFF;
        ItemColorKey key = new ItemColorKey(sprite.contents().name(), tint);
        if (ITEM_COLORS.size() >= 128) ITEM_COLORS.clear();
        return ITEM_COLORS.computeIfAbsent(key, ignored -> {
            // Read the source sprite, never the shared dynamically stitched atlas as a PNG.
            var pixels = sprite.contents().getOriginalImage();
            long red = 0, green = 0, blue = 0, count = 0;
            int step = Math.max(1, Math.max(sprite.contents().width(), sprite.contents().height()) / 24);
            for (int y = 0; y < sprite.contents().height(); y += step) {
                for (int x = 0; x < sprite.contents().width(); x += step) {
                    int abgr = pixels.getPixelRGBA(x, y);
                    if ((abgr >>> 24) < 96) continue;
                    red += abgr & 255; green += abgr >> 8 & 255; blue += abgr >> 16 & 255; count++;
                }
            }
            if (count == 0) return tint;
            return ((int) (red / count) * (tint >> 16 & 255) / 255) << 16
                    | ((int) (green / count) * (tint >> 8 & 255) / 255) << 8
                    | (int) (blue / count) * (tint & 255) / 255;
        });
    }

    private static void renderMesh(MeshEcho echo, PoseStack pose, MultiBufferSource buffers, Vec3 offset, double alpha) {
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        for (MeshPart part : echo.parts) {
            VertexConsumer out = buffers.getBuffer(material(part.texture));
            int completeVertices = part.vertices.size() / 4 * 4;
            for (int i = 0; i < completeVertices; i++) {
                CapturedVertex vertex = part.vertices.get(i);
                int a = (int) ((vertex.color >>> 24) * Math.min(1.0D, alpha));
                out.addVertex(pose.last(), vertex.x, vertex.y, vertex.z)
                        .setColor((vertex.color & 0xFFFFFF) | (a << 24)).setUv(vertex.u, vertex.v)
                        .setOverlay(vertex.overlay).setLight(vertex.light)
                        .setNormal(pose.last(), vertex.nx, vertex.ny, vertex.nz);
            }
        }
        pose.popPose();
    }

    private static RenderType material(ResourceLocation texture) {
        if (MATERIALS.size() >= 128) MATERIALS.clear();
        return MATERIALS.computeIfAbsent(texture, key -> RenderType.create("four_ho_met_echo",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(key, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false)));
    }

    private static void ribbon(VertexConsumer out, PoseStack.Pose pose, Vec3 from, Vec3 to,
                               double width, int rgb, double alpha) {
        if (alpha < 0.002D) return;
        Vec3 tangent = to.subtract(from);
        if (tangent.lengthSqr() < 0.00001D) return;
        Vec3 side = tangent.cross(from.add(to).scale(0.5D)).normalize().scale(width);
        if (side.lengthSqr() < 0.000001D) side = new Vec3(width, 0.0D, 0.0D);
        quad(out, pose, from.subtract(side), from.add(side), to.add(side), to.subtract(side), rgb, alpha);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             int rgb, double alpha) {
        int color = (Math.max(0, Math.min(255, (int) (alpha * 255.0D))) << 24) | (rgb & 0xFFFFFF);
        for (Vec3 point : new Vec3[]{a, b, c, d})
            out.addVertex(pose, (float) point.x, (float) point.y, (float) point.z).setColor(color);
    }

    private static void expireMeshes(Deque<MeshEcho> history, double oldest) {
        while (!history.isEmpty() && history.peekFirst().time < oldest) history.removeFirst();
    }

    private record MeshEcho(Vec3 position, double time, float yaw, List<MeshPart> parts, int ribbonColor) { }
    private record ItemColorKey(ResourceLocation sprite, int tint) { }
    private record MeshPart(ResourceLocation texture, List<CapturedVertex> vertices) { }
    private record PathPoint(Vec3 position, double time, int color, float alpha) { }
    private record BeamEcho(Vec3 source, Vec3 target, int color, double time) { }

    private static final class CapturedVertex {
        float x, y, z, u, v, nx, ny = 1.0F, nz;
        int color = -1, overlay, light;
        CapturedVertex(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    private static final class CaptureBuffer implements MultiBufferSource {
        private final Map<ResourceLocation, List<CapturedVertex>> parts = new LinkedHashMap<>();
        private int vertexCount;

        @Override public VertexConsumer getBuffer(RenderType type) {
            ResourceLocation texture = null;
            if (type.mode() == VertexFormat.Mode.QUADS && type.format() == DefaultVertexFormat.NEW_ENTITY
                    && type instanceof RenderType.CompositeRenderType composite)
                texture = composite.state().textureState.cutoutTexture().orElse(null);
            List<CapturedVertex> vertices = texture == null ? null : parts.computeIfAbsent(texture, ignored -> new ArrayList<>());
            return new VertexConsumer() {
                private CapturedVertex current;
                @Override public VertexConsumer addVertex(float x, float y, float z) {
                    current = null;
                    if (vertices != null && vertexCount < MAX_VERTICES && Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
                        current = new CapturedVertex(x, y, z);
                        vertices.add(current);
                        vertexCount++;
                    }
                    return this;
                }
                @Override public VertexConsumer setColor(int r, int g, int b, int a) {
                    if (current != null) current.color = a << 24 | r << 16 | g << 8 | b;
                    return this;
                }
                @Override public VertexConsumer setUv(float u, float v) { if (current != null) { current.u = u; current.v = v; } return this; }
                @Override public VertexConsumer setUv1(int u, int v) { if (current != null) current.overlay = u | v << 16; return this; }
                @Override public VertexConsumer setUv2(int u, int v) { if (current != null) current.light = u | v << 16; return this; }
                @Override public VertexConsumer setNormal(float x, float y, float z) {
                    if (current != null) { current.nx = x; current.ny = y; current.nz = z; } return this;
                }
            };
        }

        List<MeshPart> finish() {
            return parts.entrySet().stream().filter(entry -> entry.getValue().size() >= 4)
                    .map(entry -> new MeshPart(entry.getKey(), List.copyOf(entry.getValue()))).toList();
        }
    }
}
