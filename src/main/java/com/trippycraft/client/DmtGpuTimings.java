package com.trippycraft.client;

import com.trippycraft.TrippyCraft;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Opt-in diagnostics. Timestamp queries never wait for unfinished GPU work. */
final class DmtGpuTimings implements AutoCloseable {
    private static final int CAPACITY = 16;
    private static final int READ_DELAY = 4;
    private static final long REPORT_NANOS = 5_000_000_000L;
    private static boolean unsupportedReported;
    private final String effect;
    private final int timestampBits;
    private final Slot[] slots = new Slot[CAPACITY];
    private final Map<String, Window> windows = new HashMap<>();
    private long invocation;
    private long lastReport = System.nanoTime();
    private int skipped;
    private Slot active;

    static DmtGpuTimings create(String effect) {
        if (!Boolean.getBoolean("trippycraft.profileDmtGpu") || !effect.startsWith("dmt_")) return null;
        var capabilities = GL.getCapabilities();
        if (capabilities.OpenGL33 || capabilities.GL_ARB_timer_query) {
            int bits = GL15.glGetQueryi(GL33.GL_TIMESTAMP, GL15.GL_QUERY_COUNTER_BITS);
            if (bits > 0) return new DmtGpuTimings(effect, bits);
        }
        if (!unsupportedReported) {
            unsupportedReported = true;
            TrippyCraft.LOGGER.info("DMT GPU timing unavailable: OpenGL timestamp queries are unsupported");
        }
        return null;
    }

    private DmtGpuTimings(String effect, int timestampBits) {
        this.effect = effect;
        this.timestampBits = timestampBits;
        for (int i = 0; i < slots.length; i++) slots[i] = new Slot();
    }

    void begin(String view, int width, int height) {
        invocation++;
        collectAvailable();
        report(false);
        for (Slot slot : slots) {
            if (!slot.pending) {
                active = slot;
                if (slot.queries[0] == 0) {
                    for (int i = 0; i < slot.queries.length; i++) slot.queries[i] = GL15.glGenQueries();
                }
                slot.invocation = invocation;
                slot.label = view + " " + width + "x" + height;
                slot.pending = true;
                slot.complete = false;
                GL33.glQueryCounter(slot.queries[0], GL33.GL_TIMESTAMP);
                return;
            }
        }
        // A slow/queued GPU should drop measurements rather than block rendering.
        skipped++;
    }

    void endShader() {
        if (active != null) GL33.glQueryCounter(active.queries[1], GL33.GL_TIMESTAMP);
    }

    void beginCopy() {
        if (active != null) GL33.glQueryCounter(active.queries[2], GL33.GL_TIMESTAMP);
    }

    void end() {
        if (active != null) {
            GL33.glQueryCounter(active.queries[3], GL33.GL_TIMESTAMP);
            active.complete = true;
            active = null;
        }
    }

    void cancel() {
        if (active != null) {
            // No elapsed-query scope is left open if a draw throws.
            active.pending = false;
            active = null;
        }
    }

    private void collectAvailable() {
        for (Slot slot : slots) {
            if (!slot.pending || !slot.complete || invocation - slot.invocation < READ_DELAY) continue;
            if (GL15.glGetQueryObjecti(slot.queries[3], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) continue;
            // The final timestamp is available: all earlier timestamps in this
            // command stream are complete, so these reads cannot wait on a draw.
            long start = GL33.glGetQueryObjectui64(slot.queries[0], GL15.GL_QUERY_RESULT);
            long shaderEnd = GL33.glGetQueryObjectui64(slot.queries[1], GL15.GL_QUERY_RESULT);
            long copyStart = GL33.glGetQueryObjectui64(slot.queries[2], GL15.GL_QUERY_RESULT);
            long end = GL33.glGetQueryObjectui64(slot.queries[3], GL15.GL_QUERY_RESULT);
            double shader = millis(start, shaderEnd), copy = millis(copyStart, end);
            if (shader >= 0 && copy >= 0 && shader < 10_000 && copy < 10_000) {
                windows.computeIfAbsent(slot.label, ignored -> new Window()).add(shader, copy);
            }
            slot.pending = false;
        }
    }

    private double millis(long start, long end) {
        long elapsed = end - start;
        if (timestampBits < 64) elapsed &= (1L << timestampBits) - 1;
        return elapsed / 1_000_000.0;
    }

    private void report(boolean closing) {
        long now = System.nanoTime();
        if (!closing && now - lastReport < REPORT_NANOS) return;
        windows.forEach((view, window) -> {
            if (window.count > 0) {
                TrippyCraft.LOGGER.info("DMT GPU {} [{}]: {}; skipped={} (asynchronous GPU timeline, excludes other game passes)",
                        effect, view, window.summary(), skipped);
            }
        });
        windows.clear();
        skipped = 0;
        lastReport = now;
    }

    @Override
    public void close() {
        cancel();
        collectAvailable();
        report(true);
        for (Slot slot : slots) {
            for (int query : slot.queries) if (query != 0) GL15.glDeleteQueries(query);
        }
    }

    private static final class Slot {
        private final int[] queries = new int[4];
        private long invocation;
        private String label;
        private boolean pending, complete;
    }

    private static final class Window {
        private final double[] shader = new double[2048], copy = new double[2048];
        private int count;
        private long total;
        private double shaderSum, copySum;

        private void add(double shaderMs, double copyMs) {
            int index = (int) (total++ % shader.length);
            shader[index] = shaderMs;
            copy[index] = copyMs;
            count = Math.min(count + 1, shader.length);
            shaderSum += shaderMs;
            copySum += copyMs;
        }

        private String summary() {
            double[] sortedShader = Arrays.copyOf(shader, count), sortedCopy = Arrays.copyOf(copy, count);
            Arrays.sort(sortedShader);
            Arrays.sort(sortedCopy);
            return String.format(Locale.ROOT,
                    "n=%d shader mean/p50/p95/p99=%.3f/%.3f/%.3f/%.3f ms, copy mean/p95=%.3f/%.3f ms (quantiles n=%d)",
                    total, shaderSum / total, percentile(sortedShader, .50), percentile(sortedShader, .95),
                    percentile(sortedShader, .99), copySum / total, percentile(sortedCopy, .95), count);
        }

        private static double percentile(double[] sorted, double fraction) {
            return sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * fraction) - 1)];
        }
    }
}
