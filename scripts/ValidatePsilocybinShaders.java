import org.joml.Matrix4f;
import com.trippycraft.substance.psilocybin.PsilocybinDoseCurve;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Production shader execution on a hidden GPU context; no game or headset control. */
public final class ValidatePsilocybinShaders {
    private static final int WIDTH = 192, HEIGHT = 128;
    private static int colorTexture, depthTexture;
    private static final String VERTEX = """
            #version 150
            in vec3 Position;
            void main() { gl_Position = vec4(Position, 1.0); }
            """;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        GLFWErrorCallback callback = GLFWErrorCallback.createPrint(System.err);
        callback.set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        long window = NULL;
        try {
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(WIDTH, HEIGHT, "Psilocybin shader verification (hidden)", NULL, NULL);
            if (window == NULL) throw new IllegalStateException("Hidden GPU context unavailable");
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("GPU: " + glGetString(GL_RENDERER) + " / " + glGetString(GL_VERSION));
            glBindVertexArray(glGenVertexArrays());
            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 3,-1,0, -1,3,0}, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
            glEnableVertexAttribArray(0);
            glBindFramebuffer(GL_FRAMEBUFFER, glGenFramebuffers());
            int output = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH * HEIGHT * 4]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, output, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
                throw new AssertionError("Output framebuffer incomplete");
            glViewport(0, 0, WIDTH, HEIGHT);
            glDisable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            colorTexture = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH * HEIGHT * 4]);
            depthTexture = texture(GL_R32F, GL_RED, new float[WIDTH * HEIGHT]);
            Path directory = Path.of(args[0], "src/main/resources/assets/trippycraft/shaders/program");
            for (String name : new String[]{"psilocybin_world_warp", "psilocybin_world_warp_vr"}) {
                int program = compile(Files.readString(directory.resolve(name + ".fsh")), name);
                verify(program, name);
                glDeleteProgram(program);
            }
            int error = glGetError();
            if (error != GL_NO_ERROR) throw new AssertionError("OpenGL error 0x" + Integer.toHexString(error));
            System.out.println("PASS: production desktop and generated VR shaders; headset comfort was not tested");
        } finally {
            if (window != NULL) glfwDestroyWindow(window);
            glfwTerminate();
            callback.free();
        }
    }

    private static int texture(int internalFormat, int format, float[] data) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, WIDTH, HEIGHT, 0, format, GL_FLOAT, data);
        return id;
    }

    private static int shader(int type, String source, String name) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new AssertionError(name + " compile failed: " + glGetShaderInfoLog(shader));
        return shader;
    }

    private static int compile(String source, String name) {
        int vertex = shader(GL_VERTEX_SHADER, VERTEX, "verification vertex");
        int fragment = shader(GL_FRAGMENT_SHADER, source, name);
        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glBindAttribLocation(program, 0, "Position");
        glBindFragDataLocation(program, 0, "fragColor");
        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
            throw new AssertionError(name + " link failed: " + glGetProgramInfoLog(program));
        return program;
    }

    private static void vec(int program, String name, float a, float b, float c, float d) {
        glUniform4f(glGetUniformLocation(program, name), a, b, c, d);
    }

    private static void setup(int program) {
        glUseProgram(program);
        glUniform2f(glGetUniformLocation(program, "InSize"), WIDTH, HEIGHT);
        glUniform2f(glGetUniformLocation(program, "OutSize"), WIDTH, HEIGHT);
        glUniform2f(glGetUniformLocation(program, "VrReferenceSize"), WIDTH, HEIGHT);
        glUniform1i(glGetUniformLocation(program, "DiffuseSampler"), 0);
        glUniform1i(glGetUniformLocation(program, "DepthSampler"), 1);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(75), (float)WIDTH / HEIGHT, 0.05f, 96);
        Matrix4f inverse = new Matrix4f(projection).invert();
        for (String name : new String[]{"VrViewProjection", "VrHeadViewProjection"})
            glUniformMatrix4fv(glGetUniformLocation(program, name), false, projection.get(new float[16]));
        for (String name : new String[]{"VrInverseViewProjection", "VrInverseHeadViewProjection"})
            glUniformMatrix4fv(glGetUniformLocation(program, name), false, inverse.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(program, "VrHeadView"), false, new Matrix4f().get(new float[16]));
        vec(program, "VrEyePosition", 0, 0, 0, 0);
        vec(program, "VrHeadPosition", 0, 0, 0, 0);
        vec(program, "PaletteA", 0.7f, 0.2f, 0.8f, 1);
        vec(program, "PaletteB", 0.1f, 0.9f, 0.4f, 0.5f);
        vec(program, "Focus", 0.5f, 0.5f, 0.12f, 0.317f);
        vec(program, "Control", 1, 1, 0, 0);
    }

    private static float[] fixture(boolean neutral, boolean sky) {
        float[] color = new float[WIDTH * HEIGHT * 4];
        float[] depth = new float[WIDTH * HEIGHT];
        Arrays.fill(depth, sky ? 1 : 0.95f);
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            int i = (y * WIDTH + x) * 4;
            float texture = (float)(0.12 * Math.sin(x * 0.32) * Math.cos(y * 0.25));
            color[i] = 0.33f + texture + 0.12f * x / WIDTH;
            color[i + 1] = neutral ? color[i] : 0.46f + texture + 0.18f * y / HEIGHT;
            color[i + 2] = neutral ? color[i] : 0.26f + texture;
            color[i + 3] = 1;
        }
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, color);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RED, GL_FLOAT, depth);
        return color;
    }

    private static void dose(int program, float grams, float intensity, float time) {
        vec(program, "WarpA", intensity, intensity * 0.65f, intensity * 0.5f, time);
        vec(program, "WarpB", 0.317f, 0.631f, grams, 1);
        vec(program, "Dose", 1, 1, 1, 1);
    }

    private static float[] draw() {
        glDrawArrays(GL_TRIANGLES, 0, 3);
        float[] result = new float[WIDTH * HEIGHT * 4];
        glReadPixels(0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, result);
        for (float value : result)
            if (!Float.isFinite(value) || value < -0.00001f || value > 1.00001f)
                throw new AssertionError("Non-finite or invalid channel value " + value);
        return result;
    }

    private static double difference(float[] a, float[] b) {
        double total = 0;
        for (int i = 0; i < a.length; i++) total += Math.abs(a[i] - b[i]);
        return total / a.length;
    }

    private static void calibratedDose(int program, float grams, float visual, float breath, float melt, float pattern, float time) {
        // Explicit ordinary-dose state fixtures below use the production pass's
        // uniform transport. Unlike the broad stress test, strength is not an
        // unrelated synthetic multiple of dose grams.
        vec(program, "WarpA", (float)Math.log1p(visual), (float)Math.log1p(breath / 6.0), (float)Math.log1p(melt / 6.0), time);
        vec(program, "WarpB", 0.317f, 0.631f, grams, 1);
        vec(program, "Dose", 1, pattern, 1, 1);
    }

    private static void surfaceVisibility(int program, String name, float[] source) {
        // Peak states with a neutral setting/signature. Minimum output and
        // temporal differences assert surface visibility independently of
        // saturation, sharpening, and uniform monotonicity.
        float[][] fixtures = {
                {0.5f, 0.10f, 0.0825f, 0, 0},
                {1, 0.55f, 0.45375f, 0, 0},
                {3, 3.55f, 4.54f, 3.77f, 1},
                {5, 5.55f, 11.45f, 11.1f, 1},
                {8, 7.4037f, 20.42f, 19.8f, 1}
        };
        double[] minimumWarp = {0.00020, 0.0010, 0.0050, 0.0080, 0.0120};
        double[] minimumTime = {0.00015, 0.00070, 0.0035, 0.0060, 0.0080};
        vec(program, "Control", 1, 0, 0, 0);
        for (int i = 0; i < fixtures.length; i++) {
            float[] f = fixtures[i];
            calibratedDose(program, f[0], f[1], f[2], f[3], f[4], 8);
            float[] peak = draw();
            calibratedDose(program, f[0], f[1], f[2], f[3], f[4], 11);
            float[] moved = draw();
            double warped = difference(peak, source);
            double motion = difference(peak, moved);
            if (warped < minimumWarp[i] || motion < minimumTime[i])
                throw new AssertionError(name + " lacks visible surface flow at " + f[0] + " g: image=" + warped + ", time=" + motion);
            System.out.printf("PASS %s: %.1f g production-strength texture-only change %.6f; 3 s motion %.6f%n", name, f[0], warped, motion);
        }
        // A return to the previously near-zero 1 g transport must fail the
        // new visibility requirement, even if color processing still passes.
        vec(program, "WarpA", 0.0271f, 0.0729f, 0, 8);
        vec(program, "WarpB", 0.317f, 0.631f, 1, 0);
        vec(program, "Dose", 1, 0, 0, 0);
        double oldStrength = difference(draw(), source);
        if (oldStrength >= minimumWarp[1])
            throw new AssertionError("Visibility regression threshold no longer rejects old near-zero calibration: " + oldStrength);
        System.out.printf("PASS %s: minimum visibility rejects old 1 g strength (%.6f)%n", name, oldStrength);
        vec(program, "Control", 1, 1, 0, 0);
    }

    private static double smooth(double start, double end, double dose) {
        double t = Math.max(0, Math.min(1, (dose - start) / (end - start)));
        return t * t * (3 - 2 * t);
    }

    private static void productionHighDose(int program, float grams, float time) {
        // Same neutral peak signature/environment as the ordinary fixtures:
        // geometrySoftness=.5, envelope=1, living drift=1, no forest memory.
        // Compile the actual production curve rather than inventing GPU strength.
        double strength = PsilocybinDoseCurve.visualIntensity(grams);
        double amplifier = 1 + .55 * smooth(1, 3, grams) + .95 * smooth(3, 5, grams)
                + 1.30 * smooth(5, 10, grams) + 1.10 * smooth(10, 14, grams);
        calibratedDose(program, grams, (float)strength, (float)(strength * amplifier * .825),
                (float)(strength * amplifier * smooth(1.1, 3.6, grams) * .80), 1, time);
    }

    private static void verify(int program, String name) {
        float[] source = fixture(false, false);
        setup(program);
        dose(program, 0, 0, 8);
        if (difference(draw(), source) > 0.00001)
            throw new AssertionError(name + " changes sober scene");
        dose(program, 28, 0, 8);
        if (difference(draw(), source) > 0.00001)
            throw new AssertionError(name + " changes onset solely because dose exists");
        System.out.println("PASS " + name + ": sober and zero-intensity high-dose onset are identity");
        surfaceVisibility(program, name, source);

        float[] lastHighDose = null;
        double lastStrength = 0;
        for (float grams : new float[]{8, 10, 14, 28, 56, 1000, 10000}) {
            double strength = PsilocybinDoseCurve.visualIntensity(grams);
            if (strength <= lastStrength) throw new AssertionError("Production dose strength stopped increasing at " + grams);
            productionHighDose(program, grams, 8);
            float[] image = draw();
            double delta = lastHighDose == null ? difference(image, source) : difference(image, lastHighDose);
            if (delta < .0005) throw new AssertionError(name + " lost production high-dose escalation at " + grams + ": " + delta);
            System.out.printf("PASS %s: %.0f g production strength %.4f; high-dose image delta %.6f%n", name, grams, strength, delta);
            lastHighDose = image;
            lastStrength = strength;
        }
        dose(program, Float.MAX_VALUE, 110000, 8);
        draw();
        System.out.println("PASS " + name + ": largest finite GLSL dose transport remains finite");

        productionHighDose(program, 28, 8);
        float[] previous = draw();
        double largestFrameDelta = 0;
        for (int frame = 1; frame <= 120; frame++) {
            productionHighDose(program, 28, 8 + frame / 60.0f);
            float[] image = draw();
            largestFrameDelta = Math.max(largestFrameDelta, difference(previous, image));
            previous = image;
        }
        if (largestFrameDelta > 0.002)
            throw new AssertionError(name + " has abrupt adjacent-frame changes: " + largestFrameDelta);
        System.out.printf("PASS %s: 120 frames at 60 Hz; largest mean adjacent-frame delta %.6f%n", name, largestFrameDelta);

        fixture(true, false);
        dose(program, 28, 2.02f, 8);
        float[] neutral = draw();
        double largestNeutralFringe = 0;
        for (int i = 0; i < neutral.length; i += 4)
            largestNeutralFringe = Math.max(largestNeutralFringe, Math.abs(neutral[i] - neutral[i + 2]));
        if (largestNeutralFringe > 0.0015)
            throw new AssertionError(name + " creates RGB fringes on neutral textures: " + largestNeutralFringe);
        System.out.printf("PASS %s: neutral-texture RGB difference %.6f (subtle warmth only)%n", name, largestNeutralFringe);

        source = fixture(true, true);
        dose(program, 28, 2.02f, 8);
        float[] skyA = draw();
        dose(program, 28, 2.02f, 20);
        if (difference(skyA, draw()) > 0.00001)
            throw new AssertionError(name + " distorts sky as a surface");
        System.out.println("PASS " + name + ": sky excluded from animated surface breathing");
    }
}
