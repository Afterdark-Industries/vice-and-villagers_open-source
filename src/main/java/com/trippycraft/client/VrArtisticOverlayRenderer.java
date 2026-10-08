package com.trippycraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;

/** Captures the existing artistic GUI once for full-view stereo composition. */
public final class VrArtisticOverlayRenderer {
    private static TextureTarget target;

    private VrArtisticOverlayRenderer() {
    }

    /**
     * Called once before eye replay, with the same reference dimensions as the
     * original shader artwork. RGB is premultiplied: compose as RGB + scene*(1-A).
     * RenderType.gui uses separate alpha factors ONE, ONE_MINUS_SRC_ALPHA, so
     * overlapping transparent strokes retain their original combined opacity.
     */
    @Nullable
    public static RenderTarget capture(float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!VivecraftCompat.isVrActive() || !ClientExperienceStore.active()
                || minecraft.level == null || width < 1 || height < 1) {
            return null;
        }

        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        int[] colorMask = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float[] shaderColor = RenderSystem.getShaderColor().clone();
        float[] clearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            if (target == null) {
                target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                target.setFilterMode(GL11.GL_LINEAR);
            } else if (target.width != width || target.height != height) {
                target.resize(width, height, Minecraft.ON_OSX);
            }
            if (stencil) GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.colorMask(true, true, true, true);
            target.bindWrite(true);
            // Vivecraft's RenderTarget.clear hook expects an eye-sized target
            // during world passes. This independent artwork canvas has its own
            // reference size, so clear the bound target directly.
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            double guiScale = Math.max(1.0D, minecraft.getWindow().getGuiScale());
            int guiWidth = Math.max(1, (int) Math.ceil(width / guiScale));
            int guiHeight = Math.max(1, (int) Math.ceil(height / guiScale));
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(
                    0.0F, guiWidth, guiHeight, 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
            modelView.translation(0.0F, 0.0F, -11000.0F);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            try (ByteBufferBuilder buffer = new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)) {
                MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(buffer);
                GuiGraphics graphics = new GuiGraphics(minecraft, buffers) {
                    @Override
                    public int guiWidth() { return guiWidth; }

                    @Override
                    public int guiHeight() { return guiHeight; }
                };
                PsychedelicOverlayRenderer.render(graphics, partialTick);
                graphics.flush();
            }
            return target;
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            RenderSystem.colorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0);
            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunction);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }

    public static void reset() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
