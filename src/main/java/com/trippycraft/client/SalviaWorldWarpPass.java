package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.salvia.SalviaDoseCurve;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

public final class SalviaWorldWarpPass {
    private static float smoothedDoseUnits;
    private static boolean smoothedDoseInitialized;

    private SalviaWorldWarpPass() {
    }

    public static void render(float partialTick) {
        SalviaEffectState state = ClientExperienceStore.salvia();
        float shaderDose = smoothedShaderDose(state);
        float intensity = (float) shaderIntensity(state, shaderDose);
        if (intensity <= 0.0001F) {
            return;
        }

        writeWarpUniform(state, intensity, worldTime(), shaderDose, partialTick);
    }

    private static double shaderIntensity(SalviaEffectState state, float shaderDose) {
        if (!state.active() || !TrippyConfig.enableSalviaEffects) {
            return 0.0D;
        }

        double safe = Math.max(1.0D, Math.max(TrippyConfig.salviaMaxVisualIntensity, SalviaDoseCurve.safeVisualIntensity()));
        double phase = Math.min(1.85D, state.visualIntensity() / safe + state.megaPresence() * 0.32D);
        double tier = Math.min(1.0D, state.tier() / 5.0D);
        double peakDrive = smoothstep(0.20F, 0.36F, (float) state.progress()) * (1.0D - smoothstep(0.90F, 0.98F, (float) state.progress()));
        double highDose = smoothstep(6.0F, 10.0F, shaderDose);
        double realityBreak = smoothstep(10.0F, 15.0F, shaderDose);
        double beyondBreak = smoothstep(15.0F, 20.0F, shaderDose);
        double deepBreak = smoothstep(20.0F, 28.0F, shaderDose);
        double shape = state.anchorSlip() * 0.64D
                + state.sceneReplacement() * 0.58D
                + state.loopSeam() * 0.56D
                + state.observerPressure() * 0.52D
                + state.bodyPull() * 0.40D
                + state.cutoutPresence() * 0.34D
                + state.reentryConfusion() * 0.28D
                + state.wheelConveyor() * 0.84D
                + state.zipperFold() * 0.78D
                + state.flattening() * 0.80D
                + state.objecthood() * 0.66D
                + state.entityContact() * 0.74D
                + state.realityStack() * 0.94D
                + state.kaleidoscope() * 0.78D;
        if (TrippyConfig.salviaAccessibilityMode) {
            shape *= 0.58D;
        }
        double doseDrive = smoothstep(0.75F, 1.50F, shaderDose) * 0.32D
                + smoothstep(1.50F, 2.80F, shaderDose) * 0.68D
                + smoothstep(2.80F, 6.00F, shaderDose) * 0.72D
                + smoothstep(6.00F, 10.00F, shaderDose) * 1.30D
            + smoothstep(10.00F, 15.00F, shaderDose) * 1.90D
            + smoothstep(15.00F, 20.00F, shaderDose) * 2.35D
            + smoothstep(20.00F, 28.00F, shaderDose) * 2.80D;
        double computed = phase * (0.22D + tier * 0.60D + doseDrive * 0.30D + peakDrive * state.megaPresence() * 0.30D + highDose * 0.18D + realityBreak * 0.24D + beyondBreak * 0.28D + deepBreak * 0.34D)
            + shape * (1.0D + peakDrive * state.megaPresence() * 0.34D + highDose * 0.30D + realityBreak * 0.42D + beyondBreak * 0.34D + deepBreak * 0.44D)
            + state.megaPresence() * (0.36D + peakDrive * 0.28D + highDose * 0.26D + realityBreak * 0.40D + beyondBreak * 0.32D + deepBreak * 0.42D);
        double ceiling = 12.0D + shaderDose * 0.95D;
        return Math.min(ceiling, computed);
    }

    private static void writeWarpUniform(SalviaEffectState state, float intensity, float time, float shaderDose, float partialTick) {
        CameraData cameraData = cameraData();
        long seed = state.signature().seed();
        float seedA = ((seed >>> 6) & 4095L) / 4095.0F;
        float seedB = ((seed >>> 28) & 4095L) / 4095.0F;
        WorldWarpPostProcessor.process(
                "salvia_world_warp",
                true,
                partialTick,
                WorldWarpPostProcessor.uniform("WarpA", intensity, (float) state.anchorSlip(), (float) state.sceneReplacement(), time),
                WorldWarpPostProcessor.uniform("WarpB", (float) state.bodyPull(), (float) state.loopSeam(), (float) state.observerPressure(), (float) state.cutoutPresence()),
                WorldWarpPostProcessor.uniform("WarpC", (float) state.reentryConfusion(), (float) state.progress(), state.tier() / 5.0F, shaderDose),
                WorldWarpPostProcessor.uniform("WarpD", (float) state.megaPresence(), (float) state.wheelConveyor(), (float) state.zipperFold(), (float) state.flattening()),
                WorldWarpPostProcessor.uniform("WarpE", (float) state.objecthood(), (float) state.entityContact(), (float) state.realityStack(), (float) state.kaleidoscope()),
                WorldWarpPostProcessor.uniform("PaletteA", state.signature().primaryRed() / 255.0F, state.signature().primaryGreen() / 255.0F, state.signature().primaryBlue() / 255.0F, (float) state.signature().anchorBias()),
                WorldWarpPostProcessor.uniform("PaletteB", state.signature().secondaryRed() / 255.0F, state.signature().secondaryGreen() / 255.0F, state.signature().secondaryBlue() / 255.0F, (float) state.signature().recursionBias()),
                WorldWarpPostProcessor.uniform("PaletteC", state.signature().accentRed() / 255.0F, state.signature().accentGreen() / 255.0F, state.signature().accentBlue() / 255.0F, (float) state.signature().observerBias()),
                WorldWarpPostProcessor.uniform("EnvA", (float) state.environment().cave(), (float) state.environment().village(), (float) state.environment().stillness(), (float) state.environment().depth()),
                WorldWarpPostProcessor.uniform("EnvB", (float) state.environment().night(), (float) state.environment().artificialBlocks(), (float) state.environment().entities(), (float) state.environment().light()),
                WorldWarpPostProcessor.uniform("CameraA", cameraData.x(), cameraData.y(), cameraData.z(), cameraData.farPlane()),
                WorldWarpPostProcessor.uniform("CameraB", cameraData.forwardX(), cameraData.forwardY(), cameraData.forwardZ(), cameraData.nearPlane()),
                WorldWarpPostProcessor.uniform("CameraC", cameraData.rightX(), cameraData.rightY(), cameraData.rightZ(), seedA),
                WorldWarpPostProcessor.uniform("CameraD", cameraData.upX(), cameraData.upY(), cameraData.upZ(), seedB)
        );
    }

    private static float smoothedShaderDose(SalviaEffectState state) {
        float target = (float) Math.max(0.0D, state.activeDoseUnits());
        if (!smoothedDoseInitialized || state.ageTicks() <= 2 || target < 0.01F) {
            smoothedDoseUnits = target;
            smoothedDoseInitialized = true;
            return smoothedDoseUnits;
        }
        float delta = target - smoothedDoseUnits;
        float blend = Math.min(0.080F, 0.028F + Math.abs(delta) / 80.0F);
        smoothedDoseUnits += delta * blend;
        if (Math.abs(delta) < 0.01F) {
            smoothedDoseUnits = target;
        }
        return smoothedDoseUnits;
    }

    private static CameraData cameraData() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) {
            return CameraData.identity();
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            return CameraData.identity();
        }
        Vec3 position = camera.getPosition();
        Camera.NearPlane nearPlane = camera.getNearPlane();
        Vec3 forward = nearPlane.getPointOnPlane(0.0F, 0.0F);
        Vec3 right = nearPlane.getPointOnPlane(1.0F, 0.0F).subtract(forward);
        Vec3 up = nearPlane.getPointOnPlane(0.0F, 1.0F).subtract(forward);
        float farPlane = Math.max(16.0F, minecraft.gameRenderer.getDepthFar());
        return new CameraData(
                wrappedCoordinate(position.x),
                wrappedCoordinate(position.y),
                wrappedCoordinate(position.z),
                GameRenderer.PROJECTION_Z_NEAR,
                farPlane,
                (float) forward.x,
                (float) forward.y,
                (float) forward.z,
                (float) right.x,
                (float) right.y,
                (float) right.z,
                (float) up.x,
                (float) up.y,
                (float) up.z
        );
    }

    private static float wrappedCoordinate(double value) {
        double period = 4096.0D;
        double wrapped = value - Math.floor(value / period) * period;
        return (float) wrapped;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float worldTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }

    private record CameraData(
            float x,
            float y,
            float z,
            float nearPlane,
            float farPlane,
            float forwardX,
            float forwardY,
            float forwardZ,
            float rightX,
            float rightY,
            float rightZ,
            float upX,
            float upY,
            float upZ
    ) {
        private static CameraData identity() {
            return new CameraData(0.0F, 0.0F, 0.0F, GameRenderer.PROJECTION_Z_NEAR, 192.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        }
    }
}
