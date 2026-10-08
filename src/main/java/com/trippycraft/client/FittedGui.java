package com.trippycraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/** Fits a menu into the existing GUI surface without changing Vivecraft's eye projection. */
final class FittedGui {
    private final int canvasWidth;
    private final int canvasHeight;
    private final double scale;

    private FittedGui(int width, int height, int minimumWidth, int minimumHeight) {
        canvasWidth = Math.max(1, width);
        canvasHeight = Math.max(1, height);
        scale = Math.min(1.0D, Math.min(canvasWidth / (double) Math.max(1, minimumWidth),
                canvasHeight / (double) Math.max(1, minimumHeight)));
    }

    static FittedGui fit(int width, int height, int minimumWidth, int minimumHeight) {
        return new FittedGui(width, height, minimumWidth, minimumHeight);
    }

    int width() { return (int) Math.ceil(canvasWidth / scale); }
    int height() { return (int) Math.ceil(canvasHeight / scale); }
    double scale() { return scale; }
    double mouseX(double x) { return x / scale; }
    double mouseY(double y) { return y / scale; }
    double screenX(double x) { return x * scale; }
    double screenY(double y) { return y * scale; }

    Drawing begin(GuiGraphics source) {
        return new Drawing(source);
    }

    final class Drawing implements AutoCloseable {
        private final boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final int[] previousScissor = new int[4];
        private final int[] viewport = new int[4];
        private final FittedGraphics graphics;

        private Drawing(GuiGraphics source) {
            source.flush();
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, previousScissor);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            graphics = new FittedGraphics(source);
        }

        GuiGraphics graphics() { return graphics; }

        @Override
        public void close() {
            try {
                graphics.flush();
            } finally {
                if (scissorEnabled) {
                    RenderSystem.enableScissor(previousScissor[0], previousScissor[1], previousScissor[2], previousScissor[3]);
                } else {
                    RenderSystem.disableScissor();
                }
                if (depthEnabled) RenderSystem.enableDepthTest();
                else RenderSystem.disableDepthTest();
            }
        }

        private final class FittedGraphics extends GuiGraphics {
            private final Deque<Clip> clips = new ArrayDeque<>();

            private FittedGraphics(GuiGraphics source) {
                super(Minecraft.getInstance(), source.bufferSource());
                pose().last().pose().set(source.pose().last().pose());
                pose().last().normal().set(source.pose().last().normal());
                pose().scale((float) scale, (float) scale, 1.0F);
            }

            @Override public int guiWidth() { return width(); }
            @Override public int guiHeight() { return height(); }

            @Override
            public void enableScissor(int left, int top, int right, int bottom) {
                // Minecraft 1.21.1 ignores the pose when applying a GUI scissor.
                // Use this draw's framebuffer viewport, including Vivecraft's GUI FBO.
                Vector3f a = pose().last().pose().transformPosition(new Vector3f(left, top, 0));
                Vector3f b = pose().last().pose().transformPosition(new Vector3f(right, bottom, 0));
                Clip clip = new Clip(pixelX(Math.min(a.x, b.x), false), pixelY(Math.max(a.y, b.y), false),
                        pixelX(Math.max(a.x, b.x), true), pixelY(Math.min(a.y, b.y), true));
                Clip parent = clips.peekLast();
                if (parent != null) clip = clip.intersect(parent);
                else if (scissorEnabled) clip = clip.intersect(new Clip(previousScissor[0], previousScissor[1],
                        previousScissor[0] + previousScissor[2], previousScissor[1] + previousScissor[3]));
                clips.addLast(clip);
                applyClip(clip);
            }

            @Override
            public void disableScissor() {
                if (clips.isEmpty()) throw new IllegalStateException("GUI scissor stack underflow");
                clips.removeLast();
                applyClip(clips.peekLast());
            }

            @Override
            public boolean containsPointInScissor(int x, int y) {
                Clip clip = clips.peekLast();
                if (clip == null) return true;
                Vector3f p = pose().last().pose().transformPosition(new Vector3f(x, y, 0));
                double px = viewport[0] + p.x * viewport[2] / canvasWidth;
                double py = viewport[1] + (canvasHeight - p.y) * viewport[3] / canvasHeight;
                return px >= clip.left && px < clip.right && py > clip.bottom && py <= clip.top;
            }

            private int pixelX(float x, boolean ceil) {
                double value = viewport[0] + x * viewport[2] / canvasWidth;
                return (int) (ceil ? Math.ceil(value) : Math.floor(value));
            }

            private int pixelY(float y, boolean ceil) {
                double value = viewport[1] + (canvasHeight - y) * viewport[3] / canvasHeight;
                return (int) (ceil ? Math.ceil(value) : Math.floor(value));
            }

            private void applyClip(Clip clip) {
                flush();
                if (clip != null) RenderSystem.enableScissor(clip.left, clip.bottom,
                        Math.max(0, clip.right - clip.left), Math.max(0, clip.top - clip.bottom));
                else if (scissorEnabled) RenderSystem.enableScissor(previousScissor[0], previousScissor[1],
                        previousScissor[2], previousScissor[3]);
                else RenderSystem.disableScissor();
            }
        }
    }

    private record Clip(int left, int bottom, int right, int top) {
        Clip intersect(Clip other) {
            int x = Math.max(left, other.left), y = Math.max(bottom, other.bottom);
            return new Clip(x, y, Math.max(x, Math.min(right, other.right)), Math.max(y, Math.min(top, other.top)));
        }
    }
}
