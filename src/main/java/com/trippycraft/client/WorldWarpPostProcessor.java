package com.trippycraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.trippycraft.TrippyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility renderer for the pre-frame-graph post-processing API used by
 * Minecraft 1.21.1. Each drug effect still gets its own full-screen pass and
 * scratch target, and depth-aware effects sample the untouched world depth.
 */
public final class WorldWarpPostProcessor {
    private static final int GL_NEAREST = 9728;
    private static final int GL_LINEAR = 9729;
    private static final Map<String, EffectPass> PASSES = new HashMap<>();
    private static final Set<String> FAILED_EFFECTS = new HashSet<>();
    private static final List<DrawCommand> FRAME_COMMANDS = new ArrayList<>();
    private static boolean collectingFrame;
    private static boolean frameReady;
    private static boolean previousVr;
    private static Object previousLevel;
    private static StereoView stereoView;
    private static StereoView worldView;
    private static Vec3 frameHead = Vec3.ZERO;
    private static Vec3 frameOrigin = Vec3.ZERO;
    private static ReferenceView frameReference;
    private static RenderTarget frameOverlay;
    private static Matrix4f frameVisualTransform = new Matrix4f();
    private static final EffectDefinition[] EFFECTS = {
            new EffectDefinition("alcohol_world_warp", true),
            new EffectDefinition("cocaine_world_warp", false),
            new EffectDefinition("ketamine_world_warp", false),
            new EffectDefinition("psilocybin_world_warp", true),
            new EffectDefinition("lsd_world_warp", true),
            new EffectDefinition("two_cb_world_warp", false),
            new EffectDefinition("mescaline_world_warp", true),
            new EffectDefinition("homet_world_warp", true),
            new EffectDefinition("al_lad_world_warp", true),
            new EffectDefinition("two_ce_world_warp", true),
            new EffectDefinition("dmt_world_warp", true),
            new EffectDefinition("salvia_world_warp", true),
            new EffectDefinition("mdma_world_warp", false),
            new EffectDefinition("nitrous_world_warp", false)
    };
    private static boolean developmentValidationPending =
            Boolean.getBoolean("trippycraft.validateWorldWarpShaders");

    private WorldWarpPostProcessor() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        boolean vr = VivecraftCompat.isVrWorldPass();
        if (previousLevel != minecraft.level || previousVr != vr) {
            reset();
            previousLevel = minecraft.level;
            previousVr = vr;
            if (vr) {
                TrippyCraft.LOGGER.info("Vivecraft stereo effects active: original artistic programs in shared head coordinates (VR2)");
            }
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (!vr) {
            Vec3 camera = event.getCamera().getPosition();
            Vec3 origin = new Vec3(origin(camera.x), origin(camera.y), origin(camera.z));
            worldView = new StereoView(event, origin, camera);
            try {
                renderEffects(partialTick);
            } finally {
                worldView = null;
            }
            return;
        }

        if (VivecraftCompat.isFirstWorldPass() || !frameReady) {
            FRAME_COMMANDS.clear();
            ClientRenderEvents.updateVrPresentation(event.getCamera(), partialTick);
            Vec3 head = VivecraftCompat.headPosition();
            frameHead = head != null ? head : event.getCamera().getPosition();
            // Both eyes must use the same origin, including across a 4096-block boundary.
            frameOrigin = new Vec3(origin(frameHead.x), origin(frameHead.y), origin(frameHead.z));
            frameReference = new ReferenceView(event, minecraft.getMainRenderTarget());
            frameVisualTransform = visualTransform(ClientRenderEvents.vrCameraMotion(), frameReference);
            collectingFrame = true;
            try {
                renderEffects(partialTick);
                frameOverlay = VrArtisticOverlayRenderer.capture(partialTick,
                        Math.max(1, Math.round(frameReference.width)), Math.max(1, Math.round(frameReference.height)));
                frameReady = true;
            } finally {
                collectingFrame = false;
            }
        }

        // AFTER_LEVEL carries Vivecraft's actual asymmetric eye projection and
        // view rotation. Camera.getNearPlane() describes the desktop projection.
        stereoView = new StereoView(event, frameOrigin, frameHead);
        worldView = stereoView;
        try {
            for (DrawCommand command : FRAME_COMMANDS) {
                process(command.effectId(), command.usesDepth(), command.partialTick(), command.uniforms());
            }
            if (frameOverlay != null || !frameVisualTransform.equals(new Matrix4f(), 0.000001F)) {
                process("vr_visual_composite", true, partialTick);
            }
        } finally {
            stereoView = null;
            worldView = null;
        }
    }

    private static double origin(double value) {
        return Math.floor(value / 4096.0D) * 4096.0D;
    }

    private static Matrix4f visualTransform(ClientRenderEvents.VrCameraMotion motion, ReferenceView reference) {
        float radians = (float) (Math.PI / 180.0D);
        float halfFov = Minecraft.getInstance().options.fov().get() * radians * 0.5F;
        // Match the original desktop lens magnification. Applying its angle
        // factor to wider HMD optics would amplify the zoom and can cross 180°.
        float scale = (float) (Math.tan(halfFov * motion.fovFactor()) / Math.tan(halfFov));
        Vec3 translation = motion.translationWorld();
        Vector3f localTranslation = reference.view.transformDirection(new Vector3f(
                (float) translation.x, (float) translation.y, (float) translation.z));
        Vector3f forward = new Vector3f(reference.forward).normalize();
        float baseYaw = (float) Math.atan2(-forward.x, forward.z);
        float basePitch = (float) Math.asin(-forward.y);
        // Match Camera.setRotation's YXZ convention (all three artistic angles
        // have negative quaternion signs). Apply the difference in world space,
        // then carry it back into the fully tracked head frame, including roll.
        Quaternionf baseline = new Quaternionf().rotationYXZ((float) Math.PI - baseYaw, -basePitch, 0.0F);
        Quaternionf authored = new Quaternionf().rotationYXZ((float) Math.PI - baseYaw - motion.yawDegrees() * radians,
                -basePitch - motion.pitchDegrees() * radians, -motion.rollDegrees() * radians);
        Quaternionf worldDelta = authored.mul(baseline.conjugate());
        Matrix4f localRotation = new Matrix4f(reference.view).mul(new Matrix4f().rotation(worldDelta))
                .mul(new Matrix4f(reference.view).invert());
        return new Matrix4f().translation(localTranslation)
                .mul(localRotation)
                .scale(scale, scale, 1.0F);
    }

    private static void renderEffects(float partialTick) {
        AlcoholWorldWarpPass.render(partialTick);
        CocaineWorldWarpPass.render(partialTick);
        KetamineWorldWarpPass.render(partialTick);
        PsilocybinWorldWarpPass.render(partialTick);
        LsdWorldWarpPass.render(partialTick);
        TwoCbWorldWarpPass.render(partialTick);
        MescalineWorldWarpPass.render(partialTick);
        HoMetWorldWarpPass.render(partialTick);
        AlLadWorldWarpPass.render(partialTick);
        TwoCeWorldWarpPass.render(partialTick);
        DmtWorldWarpPass.render(partialTick);
        SalviaWorldWarpPass.render(partialTick);
        MdmaWorldWarpPass.render(partialTick);
        NitrousWorldWarpPass.render(partialTick);
    }

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                barrier.<Void>wait(null).thenRunAsync(WorldWarpPostProcessor::reset, gameExecutor));
    }

    /**
     * Compiles every migrated 1.21.1 post-process program once in development
     * runs. Production clients never take this path because the enabling system
     * property is only supplied by the Gradle client run configuration.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null && previousLevel != null) {
            reset();
            previousLevel = null;
            previousVr = false;
        }
        if (!developmentValidationPending) {
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null || mainTarget.width <= 0 || mainTarget.height <= 0) {
            return;
        }

        developmentValidationPending = false;
        List<EffectDefinition> validationEffects = new ArrayList<>();
        for (EffectDefinition effect : EFFECTS) {
            validationEffects.add(effect);
            validationEffects.add(new EffectDefinition(effect.id() + "_vr", true));
        }
        validationEffects.add(new EffectDefinition("vr_visual_composite", true));
        for (EffectDefinition effect : validationEffects) {
            try (EffectPass validationPass = new EffectPass(effect.id(), effect.usesDepth(), minecraft, mainTarget)) {
                // Construction compiles and links the program; one neutral draw
                // also exercises its samplers, uniforms, scratch FBO, and blit.
                validationPass.process(0.0F, new UniformValue[0]);
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException(
                        "Unable to validate Vice and Villagers world-warp shader " + effect.id(),
                        exception
                );
            }
        }
        TrippyCraft.LOGGER.info("Validated all {} Vice and Villagers 1.21.1 world-warp shaders", validationEffects.size());
    }

    static void process(String effectId, boolean usesDepth, float partialTick, UniformValue... uniforms) {
        if (collectingFrame) {
            UniformValue[] snapshot = new UniformValue[uniforms.length];
            for (int i = 0; i < uniforms.length; i++) {
                snapshot[i] = new UniformValue(uniforms[i].name(), uniforms[i].values().clone());
            }
            FRAME_COMMANDS.add(new DrawCommand(effectId, usesDepth, partialTick, snapshot));
            return;
        }

        String programId = stereoView != null && !effectId.startsWith("vr_") ? effectId + "_vr" : effectId;
        usesDepth = usesDepth || stereoView != null;
        // LEFT and RIGHT share Vivecraft's stereo target. Mirror, camera and
        // telescope views can have different sizes and must retain their passes.
        String viewKey = stereoView == null ? "VANILLA"
                : VivecraftCompat.isStereoEyePass() ? "STEREO" : VivecraftCompat.currentPassName();
        String passKey = viewKey + ":" + programId;
        if (FAILED_EFFECTS.contains(programId)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (mainTarget == null || mainTarget.width <= 0 || mainTarget.height <= 0) {
            return;
        }

        EffectPass effectPass = PASSES.get(passKey);
        if (effectPass == null || effectPass.mainTarget != mainTarget) {
            PASSES.remove(passKey);
            close(effectPass);
            try {
                effectPass = new EffectPass(programId, usesDepth, minecraft, mainTarget);
                PASSES.put(passKey, effectPass);
            } catch (IOException | RuntimeException exception) {
                FAILED_EFFECTS.add(programId);
                TrippyCraft.LOGGER.error("Unable to load Vice and Villagers world-warp effect {}", programId, exception);
                return;
            }
        }

        try {
            effectPass.process(partialTick, uniforms);
        } catch (RuntimeException exception) {
            PASSES.remove(passKey);
            close(effectPass);
            FAILED_EFFECTS.add(programId);
            TrippyCraft.LOGGER.error("Disabling broken Vice and Villagers world-warp effect {} until resources reload", programId, exception);
        }
    }

    static UniformValue uniform(String name, float... values) {
        return new UniformValue(name, values);
    }

    private static void reset() {
        PASSES.values().forEach(WorldWarpPostProcessor::close);
        PASSES.clear();
        FAILED_EFFECTS.clear();
        FRAME_COMMANDS.clear();
        frameReady = false;
        stereoView = null;
        worldView = null;
        frameReference = null;
        frameOverlay = null;
        VrArtisticOverlayRenderer.reset();
        HoMetWorldWarpPass.reset();
        AlLadWorldWarpPass.reset();
        TwoCeWorldWarpPass.reset();
        NitrousWorldWarpPass.reset();
        developmentValidationPending = Boolean.getBoolean("trippycraft.validateWorldWarpShaders");
    }

    private static void close(EffectPass effectPass) {
        if (effectPass != null) {
            effectPass.close();
        }
    }

    record UniformValue(String name, float[] values) {
    }

    private record EffectDefinition(String id, boolean usesDepth) {
    }

    private record DrawCommand(String effectId, boolean usesDepth, float partialTick, UniformValue[] uniforms) {
    }

    /** A single, symmetric virtual camera on which the original artistic formulas operate. */
    private static final class ReferenceView {
        private final Matrix4f view;
        private final Matrix4f projection;
        private final Matrix4f viewProjection;
        private final Matrix4f inverseViewProjection;
        private final Vector3f forward;
        private final Vector3f right;
        private final Vector3f up;
        private final float near;
        private final float far;
        private final float width;
        private final float height;

        private ReferenceView(RenderLevelStageEvent event, RenderTarget target) {
            Quaternionf rotation = VivecraftCompat.headRotation();
            view = rotation == null ? new Matrix4f(event.getModelViewMatrix())
                    : new Matrix4f().rotation(rotation.conjugate());
            projection = new Matrix4f(event.getProjectionMatrix());
            // Enclose the runtime eye frustum rather than using the narrower desktop
            // FOV. The same symmetric domain and size are frozen for both eyes.
            float tanX = (1.0F + Math.abs(projection.m20())) / Math.abs(projection.m00());
            float tanY = (1.0F + Math.abs(projection.m21())) / Math.abs(projection.m11());
            projection.m00(1.0F / tanX).m11(1.0F / tanY).m20(0.0F).m21(0.0F);
            viewProjection = new Matrix4f(projection).mul(view);
            inverseViewProjection = new Matrix4f(viewProjection).invert();
            Matrix4f inverseProjection = new Matrix4f(projection).invert();
            near = Math.abs(inverseProjection.transformProject(new Vector3f(0, 0, -1)).z);
            far = Math.abs(inverseProjection.transformProject(new Vector3f(0, 0, 1)).z);
            forward = inverseViewProjection.transformProject(new Vector3f(0, 0, -1));
            right = inverseViewProjection.transformProject(new Vector3f(1, 0, -1)).sub(forward);
            up = inverseViewProjection.transformProject(new Vector3f(0, 1, -1)).sub(forward);
            height = target.height;
            width = height * tanX / tanY;
        }
    }

    private static final class StereoView {
        private final Matrix4f viewProjection;
        private final Matrix4f inverseViewProjection;
        private final Vec3 eye;
        private final Vec3 head;

        private StereoView(RenderLevelStageEvent event, Vec3 origin, Vec3 head) {
            viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            inverseViewProjection = new Matrix4f(viewProjection).invert();
            eye = event.getCamera().getPosition().subtract(origin);
            this.head = head.subtract(origin);
        }
    }

    private static final class EffectPass implements AutoCloseable {
        private final RenderTarget mainTarget;
        private final TextureTarget scratchTarget;
        private final PostPass postPass;

        private EffectPass(String effectId, boolean usesDepth, Minecraft minecraft, RenderTarget mainTarget) throws IOException {
            this.mainTarget = mainTarget;
            this.scratchTarget = new TextureTarget(
                    Math.max(1, mainTarget.width),
                    Math.max(1, mainTarget.height),
                    false,
                    Minecraft.ON_OSX
            );
            this.scratchTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            PostPass createdPass;
            try {
                createdPass = new PostPass(
                        minecraft.getResourceManager(),
                        TrippyCraft.MOD_ID + ":" + effectId,
                        mainTarget,
                        scratchTarget,
                        true
                );
            } catch (IOException | RuntimeException exception) {
                scratchTarget.destroyBuffers();
                throw exception;
            }
            this.postPass = createdPass;
            if (usesDepth) {
                this.postPass.addAuxAsset(
                        "DepthSampler",
                        mainTarget::getDepthTextureId,
                        mainTarget.width,
                        mainTarget.height
                );
            }
            if (effectId.equals("vr_visual_composite")) {
                this.postPass.addAuxAsset("OverlaySampler",
                        () -> frameOverlay == null ? mainTarget.getColorTextureId() : frameOverlay.getColorTextureId(),
                        mainTarget.width, mainTarget.height);
            }
            updateProjection();
        }

        private void process(float partialTick, UniformValue[] uniforms) {
            resizeIfNeeded();
            for (UniformValue value : uniforms) {
                applyUniform(value);
            }
            // Generated VR programs retain the desktop body. The wrapper supplies
            // canonical depth/camera data, so VR1's eye-only branches remain off.
            postPass.getEffect().safeGetUniform("VrEnabled").set(0.0F);
            // Native spatial programs reconstruct from the actual current view,
            // including asymmetric/canted Vivecraft eyes, rather than a 2D lens.
            if (worldView != null) {
                postPass.getEffect().safeGetUniform("WorldViewProjection").set(worldView.viewProjection);
                postPass.getEffect().safeGetUniform("WorldInverseViewProjection").set(worldView.inverseViewProjection);
                Vec3 eye = worldView.eye;
                Vec3 head = worldView.head;
                postPass.getEffect().safeGetUniform("WorldEyePosition").set((float) eye.x, (float) eye.y, (float) eye.z, 0.0F);
                postPass.getEffect().safeGetUniform("WorldHeadPosition").set((float) head.x, (float) head.y, (float) head.z, 0.0F);
            }
            if (stereoView != null) {
                postPass.getEffect().safeGetUniform("VrViewProjection").set(stereoView.viewProjection);
                postPass.getEffect().safeGetUniform("VrInverseViewProjection").set(stereoView.inverseViewProjection);
                Vec3 eye = stereoView.eye;
                Vec3 head = stereoView.head;
                postPass.getEffect().safeGetUniform("VrEyePosition").set((float) eye.x, (float) eye.y, (float) eye.z, 0.0F);
                postPass.getEffect().safeGetUniform("VrHeadPosition").set((float) head.x, (float) head.y, (float) head.z, 0.0F);
                ReferenceView reference = frameReference;
                postPass.getEffect().safeGetUniform("VrHeadView").set(reference.view);
                postPass.getEffect().safeGetUniform("VrHeadViewProjection").set(reference.viewProjection);
                postPass.getEffect().safeGetUniform("VrInverseHeadViewProjection").set(reference.inverseViewProjection);
                postPass.getEffect().safeGetUniform("VrReferenceSize").set(reference.width, reference.height);
                postPass.getEffect().safeGetUniform("VrVisualTransform").set(frameVisualTransform);
                postPass.getEffect().safeGetUniform("VrInverseHeadView").set(new Matrix4f(reference.view).invert());
                postPass.getEffect().safeGetUniform("VrVisualEnabled").set(frameVisualTransform.equals(new Matrix4f(), 0.000001F) ? 0.0F : 1.0F);
                postPass.getEffect().safeGetUniform("VrOverlayEnabled").set(frameOverlay == null ? 0.0F : 1.0F);
                // Restore original symmetric CameraA-D semantics in the virtual
                // head view. Salvia stores signature seeds in CameraC/D.w.
                for (UniformValue value : uniforms) {
                    float[] values = value.values();
                    if (values.length != 4) continue;
                    switch (value.name()) {
                        case "CameraA" -> applyUniform(uniform("CameraA", (float) head.x, (float) head.y, (float) head.z, reference.far));
                        case "CameraB" -> applyUniform(uniform("CameraB", reference.forward.x, reference.forward.y, reference.forward.z, reference.near));
                        case "CameraC" -> applyUniform(uniform("CameraC", reference.right.x, reference.right.y, reference.right.z, values[3]));
                        case "CameraD" -> applyUniform(uniform("CameraD", reference.up.x, reference.up.y, reference.up.z, values[3]));
                        default -> { }
                    }
                }
            }

            int mainFilter = mainTarget.filterMode;
            int scratchFilter = scratchTarget.filterMode;
            // Vivecraft's hidden-area stencil is still active at AFTER_LEVEL.
            // Scratch targets have no stencil; retain the caller's stencil state.
            boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
            try {
                if (stencil) GL11.glDisable(GL11.GL_STENCIL_TEST);
                mainTarget.setFilterMode(GL_LINEAR);
                postPass.process(partialTick);
                mainTarget.setFilterMode(mainFilter);

                scratchTarget.setFilterMode(GL_LINEAR);
                mainTarget.bindWrite(false);
                scratchTarget.blitToScreen(mainTarget.viewWidth, mainTarget.viewHeight, true);
            } finally {
                if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST);
                mainTarget.setFilterMode(mainFilter < 0 ? GL_NEAREST : mainFilter);
                scratchTarget.setFilterMode(scratchFilter < 0 ? GL_NEAREST : scratchFilter);
                // blitToScreen is normally the final render operation and leaves
                // depth testing disabled. This compatibility path blits into the
                // main framebuffer before first-person rendering, so restore the
                // world-render state expected by the remaining client pipeline.
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
                // GameRenderer renders the first-person hand immediately after AFTER_LEVEL.
                mainTarget.bindWrite(false);
            }
        }

        private void applyUniform(UniformValue value) {
            AbstractUniform uniform = postPass.getEffect().safeGetUniform(value.name());
            float[] values = value.values();
            switch (values.length) {
                case 1 -> uniform.set(values[0]);
                case 2 -> uniform.set(values[0], values[1]);
                case 3 -> uniform.set(values[0], values[1], values[2]);
                case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
                default -> throw new IllegalArgumentException("Uniform " + value.name() + " must have between one and four values");
            }
        }

        private void resizeIfNeeded() {
            if (scratchTarget.width != mainTarget.width || scratchTarget.height != mainTarget.height) {
                scratchTarget.resize(Math.max(1, mainTarget.width), Math.max(1, mainTarget.height), Minecraft.ON_OSX);
                updateProjection();
            }
        }

        private void updateProjection() {
            postPass.setOrthoMatrix(new Matrix4f().setOrtho(
                    0.0F,
                    (float) mainTarget.width,
                    0.0F,
                    (float) mainTarget.height,
                    0.1F,
                    1000.0F
            ));
        }

        @Override
        public void close() {
            postPass.close();
            scratchTarget.destroyBuffers();
        }
    }
}
