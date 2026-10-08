package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * Optional bridge to Vivecraft's public API, introduced in Vivecraft 1.3.0.
 * The signatures and render-pass names are verified against 1.21.1-1.3.15.
 * Keeping Vivecraft types out of field and method signatures lets this mod load
 * on ordinary clients without a Vivecraft dependency.
 */
public final class VivecraftCompat {
    private static boolean warned;

    private VivecraftCompat() {
    }

    /** Whether the local player is currently using VR; valid outside rendering. */
    public static boolean isVrActive() {
        return invokeBoolean(ApiHolder.API, true);
    }

    /**
     * Whether the current pass renders a VR world view (eyes, mirror camera,
     * telescope or screenshot camera), excluding the GUI and mirror blit.
     * Call only from render callbacks.
     */
    public static boolean isVrWorldPass() {
        return isVrActive() && isWorldPass(currentPassName());
    }

    /** Whether the current view is one of the two headset eyes. */
    public static boolean isStereoEyePass() {
        if (!isVrActive()) {
            return false;
        }
        String pass = currentPassName();
        return "LEFT".equals(pass) || "RIGHT".equals(pass);
    }

    /** Shared headset origin in world coordinates, sampled from the render pose. */
    @Nullable
    public static Vec3 headPosition() {
        Api api = ApiHolder.API;
        if (api == null || !isVrActive()) {
            return null;
        }
        try {
            Object pose = api.worldRenderPose.invoke(api.client);
            if (pose == null) {
                return null;
            }
            Object head = api.poseHead.invoke(pose);
            return head == null ? null : (Vec3) api.bodyPartPosition.invoke(head);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return null;
        }
    }

    /** Full tracked orientation, including roll omitted by Minecraft's XRCamera. */
    @Nullable
    public static Quaternionf headRotation() {
        Api api = ApiHolder.API;
        if (api == null || !isVrActive()) return null;
        try {
            Object pose = api.worldRenderPose.invoke(api.client);
            Object head = pose == null ? null : api.poseHead.invoke(pose);
            return head == null ? null : new Quaternionf((Quaternionfc) api.bodyPartRotation.invoke(head));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return null;
        }
    }

    @Nullable
    public static Vec3 headDirection() {
        Quaternionf rotation = headRotation();
        if (rotation == null) return null;
        Vector3f direction = rotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        return new Vec3(direction.x, direction.y, direction.z);
    }

    /** A tracked grip in world coordinates, independent of which eye is rendering. */
    @Nullable
    public static HandPose handPose(net.minecraft.world.InteractionHand hand) {
        Api api = ApiHolder.API;
        if (api == null || !isVrActive()) return null;
        try {
            Object pose = api.worldRenderPose.invoke(api.client);
            Object grip = pose == null ? null : api.poseHand.invoke(pose, hand);
            return grip == null ? null : new HandPose((Vec3) api.bodyPartPosition.invoke(grip),
                    new Quaternionf((Quaternionfc) api.bodyPartRotation.invoke(grip)));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return null;
        }
    }

    public record HandPose(Vec3 position, Quaternionf rotation) { }

    /** The authored stare mechanic follows HMD gaze, independently of controller picking. */
    public static HitResult headBlockHit(Minecraft minecraft) {
        Vec3 position = headPosition();
        Vec3 direction = headDirection();
        if (!isVrActive() || position == null || direction == null || minecraft.level == null || minecraft.player == null) {
            return minecraft.hitResult;
        }
        return minecraft.level.clip(new ClipContext(position,
                position.add(direction.scale(minecraft.player.blockInteractionRange())),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player));
    }

    /**
     * True once per VR world-render cycle, and on every ordinary render.
     * GUI/mirror blit callbacks must not advance world animation state.
     */
    public static boolean isFirstWorldPass() {
        Api api = ApiHolder.API;
        if (api == null || !isVrActive()) {
            return true;
        }
        if (!isWorldPass(currentPassName())) {
            return false;
        }
        return invokeBoolean(api, false);
    }

    /** Public API enum name, or VANILLA when Vivecraft is absent/inactive. */
    public static String currentPassName() {
        Api api = ApiHolder.API;
        if (api == null || !isVrActive()) {
            return "VANILLA";
        }
        try {
            if ((Boolean) api.isVanillaRenderPass.invoke(api.rendering)) {
                return "VANILLA";
            }
            Object pass = api.currentRenderPass.invoke(api.rendering);
            return pass instanceof Enum<?> renderPass ? renderPass.name() : "VANILLA";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return "VANILLA";
        }
    }

    private static boolean isWorldPass(String pass) {
        return switch (pass) {
            case "LEFT", "RIGHT", "CENTER", "THIRD", "SCOPER", "SCOPEL", "CAMERA" -> true;
            default -> false;
        };
    }

    private static boolean invokeBoolean(Api api, boolean active) {
        if (api == null) {
            return false;
        }
        try {
            return active ? (Boolean) api.isVrActive.invoke(api.client)
                    : (Boolean) api.isFirstRenderPass.invoke(api.rendering);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return false;
        }
    }

    private static Api findApi() {
        try {
            ClassLoader loader = VivecraftCompat.class.getClassLoader();
            Class<?> clientType = Class.forName("org.vivecraft.api.client.VRClientAPI", true, loader);
            Class<?> renderingType = Class.forName("org.vivecraft.api.client.VRRenderingAPI", true, loader);
            Class<?> poseType = Class.forName("org.vivecraft.api.data.VRPose", false, loader);
            Class<?> bodyPartType = Class.forName("org.vivecraft.api.data.VRBodyPartData", false, loader);
            Api api = new Api(clientType.getMethod("instance").invoke(null),
                    renderingType.getMethod("instance").invoke(null),
                    clientType.getMethod("isVRActive"),
                    renderingType.getMethod("isVanillaRenderPass"),
                    renderingType.getMethod("isFirstRenderPass"),
                    renderingType.getMethod("getCurrentRenderPass"),
                    clientType.getMethod("getWorldRenderPose"),
                    poseType.getMethod("getHead"),
                    bodyPartType.getMethod("getPos"),
                    bodyPartType.getMethod("getRotation"),
                    poseType.getMethod("getHand", net.minecraft.world.InteractionHand.class));
            TrippyCraft.LOGGER.info("Vivecraft public rendering API detected; stereo effect adaptation available.");
            return api;
        } catch (ClassNotFoundException exception) {
            // Vivecraft is optional. No classes from it are otherwise linked.
            return null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return null;
        }
    }

    private static void warn(Throwable exception) {
        if (!warned) {
            warned = true;
            TrippyCraft.LOGGER.warn("Unable to read Vivecraft's public rendering API; VR adaptation is unavailable.",
                    exception);
        }
    }

    private static final class ApiHolder {
        private static final Api API = findApi();
    }

    private record Api(Object client, Object rendering, Method isVrActive,
                       Method isVanillaRenderPass, Method isFirstRenderPass, Method currentRenderPass,
                       Method worldRenderPose, Method poseHead, Method bodyPartPosition, Method bodyPartRotation,
                       Method poseHand) {
    }
}
