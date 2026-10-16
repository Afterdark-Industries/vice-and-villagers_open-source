import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Runs the production shaders on an isolated GPU without opening Minecraft. */
public final class ValidateDmtFractalShaders {
    private static int WIDTH = 128, HEIGHT = 96;
    private static final float REALM_SCALE = 0.35f;
    private static final Vector3f ANCHOR = new Vector3f(0.25f, 1.62f, -0.35f);
    private static final Vector3f BASE_ORIGIN = new Vector3f(0.18f, -0.12f, 0.37f);
    private static final List<Integer> PROGRAMS = new ArrayList<>();
    private static final List<Frame> FRAMES = new ArrayList<>();
    private static int colorTexture, depthTexture, outputTexture, frameCount;
    private static final String VERTEX = """
            #version 150
            in vec3 Position;
            out vec2 texCoord;
            out vec2 oneTexel;
            uniform vec2 InSize;
            void main() {
                gl_Position = vec4(Position, 1.0);
                texCoord = Position.xy * 0.5 + 0.5;
                oneTexel = 1.0 / max(InSize, vec2(1.0));
            }
            """;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        Path root = Path.of(args[0]);
        GLFWErrorCallback callback = GLFWErrorCallback.createPrint(System.err);
        callback.set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        long window = NULL;
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(WIDTH, HEIGHT, "DMT fractal GPU validation (hidden)", NULL, NULL);
            if (window == NULL) throw new IllegalStateException("Hidden OpenGL context unavailable");
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("GPU: " + glGetString(GL_RENDERER) + " / " + glGetString(GL_VERSION));
            glBindVertexArray(glGenVertexArrays());
            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 3,-1,0, -1,3,0}, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
            glEnableVertexAttribArray(0);
            glBindFramebuffer(GL_FRAMEBUFFER, glGenFramebuffers());
            outputTexture = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH*HEIGHT*4]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, outputTexture, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
                throw new AssertionError("Floating-point output framebuffer incomplete");
            glViewport(0, 0, WIDTH, HEIGHT);
            glDisable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            colorTexture = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH*HEIGHT*4]);
            depthTexture = texture(GL_R32F, GL_RED, new float[WIDTH*HEIGHT]);
            Path directory = root.resolve("src/main/resources/assets/trippycraft/shaders/program");
            String desktop = Files.readString(directory.resolve("dmt_fractal_world_warp.fsh"));
            String vr = Files.readString(directory.resolve("dmt_fractal_world_warp_vr.fsh"));
            int desktopProgram = program(desktop, "production desktop fractal world");
            int vrProgram = program(vr, "production VR fractal world");
            System.out.println("PASS: production desktop and native per-eye VR shaders compile and link");
            validatePrograms(desktopProgram, vrProgram);
            cameraResponse(desktopProgram);
            stereoWorld(desktop, desktopProgram, vrProgram);
            previewFrames(desktopProgram, vrProgram);
            contactSheet(root.resolve("build/dmt-fractal-contact-sheet.png"));
            performance(desktopProgram);
            int error = glGetError();
            if (error != GL_NO_ERROR) throw new AssertionError("OpenGL error 0x" + Integer.toHexString(error));
            System.out.printf("PASS: %d finite GPU frames; Minecraft integration, full-resolution performance and headset comfort still require live testing%n", frameCount);
        } finally {
            for (int p : PROGRAMS) glDeleteProgram(p);
            if (window != NULL) glfwDestroyWindow(window);
            glfwTerminate();
            callback.free();
        }
    }

    private static int shader(int kind, String source, String name) {
        int shader = glCreateShader(kind);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new AssertionError(name + " compilation failed:\n" + glGetShaderInfoLog(shader));
        return shader;
    }

    private static int program(String source, String name) {
        int vertex = shader(GL_VERTEX_SHADER, VERTEX, "validation vertex");
        int fragment = shader(GL_FRAGMENT_SHADER, source, name);
        int result = glCreateProgram();
        glAttachShader(result, vertex);
        glAttachShader(result, fragment);
        glBindAttribLocation(result, 0, "Position");
        glBindFragDataLocation(result, 0, "fragColor");
        glLinkProgram(result);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        if (glGetProgrami(result, GL_LINK_STATUS) == GL_FALSE)
            throw new AssertionError(name + " link failed:\n" + glGetProgramInfoLog(result));
        PROGRAMS.add(result);
        return result;
    }

    private static int texture(int internalFormat, int format, float[] data) {
        int result = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, result);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, WIDTH, HEIGHT, 0, format, GL_FLOAT, data);
        return result;
    }

    private static void resize(int width, int height) {
        WIDTH = width;
        HEIGHT = height;
        for (int texture : new int[]{outputTexture, colorTexture}) {
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, WIDTH, HEIGHT, 0, GL_RGBA, GL_FLOAT, (java.nio.FloatBuffer)null);
        }
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, WIDTH, HEIGHT, 0, GL_RED, GL_FLOAT, (java.nio.FloatBuffer)null);
        glViewport(0, 0, WIDTH, HEIGHT);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new AssertionError("Resized output framebuffer incomplete");
    }

    private static void vec(int p, String name, float a, float b, float c, float d) {
        glUniform4f(glGetUniformLocation(p, name), a, b, c, d);
    }

    private static void setup(int p, Eye eye, float blend, int quality, float access, float time, float seed) {
        glUseProgram(p);
        glUniform2f(glGetUniformLocation(p, "InSize"), WIDTH, HEIGHT);
        glUniform2f(glGetUniformLocation(p, "OutSize"), WIDTH, HEIGHT);
        glUniform1i(glGetUniformLocation(p, "DiffuseSampler"), 0);
        glUniform1i(glGetUniformLocation(p, "DepthSampler"), 1);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glUniformMatrix4fv(glGetUniformLocation(p, "WorldInverseViewProjection"), false, eye.inverse.get(new float[16]));
        vec(p, "WorldEyePosition", eye.position.x, eye.position.y, eye.position.z, 0);
        vec(p, "WorldHeadPosition", eye.head.x, eye.head.y, eye.head.z, 0);
        Vector3f realmHead = new Vector3f(eye.head).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
        vec(p, "RealmOrigin", realmHead.x, realmHead.y, realmHead.z, REALM_SCALE);
        vec(p, "FractalA", blend, time, quality, access);
        vec(p, "FractalB", seed, 0.631f, 0.78f, 0.65f);
        vec(p, "PaletteA", 0.93f, 0.31f, 0.08f, 1);
        vec(p, "PaletteB", 0.04f, 0.82f, 0.72f, 1);
        vec(p, "PaletteC", 0.65f, 0.06f, 0.92f, 1);
    }

    private static Eye eye(int side, float yaw, float pitch, float roll, Vector3f translation, boolean canted) {
        Vector3f head = new Vector3f(ANCHOR).add(translation);
        Matrix4f rotation = new Matrix4f().rotationYXZ(yaw, pitch, roll);
        Vector3f position = rotation.transformDirection(new Vector3f(side*0.032f, 0, 0)).add(head);
        Matrix4f eyeRotation = new Matrix4f(rotation).rotateY(canted ? side*0.025f : 0);
        float near = 0.05f, tanY = 0.85f, tanX = tanY*WIDTH/HEIGHT;
        float asymX = side*0.007f, asymY = side == 0 ? 0 : 0.003f;
        Matrix4f projection = new Matrix4f().frustum(-near*tanX+asymX, near*tanX+asymX,
                -near*tanY+asymY, near*tanY+asymY, near, 96);
        Matrix4f vp = projection.mul(new Matrix4f(eyeRotation).transpose());
        return new Eye(position, head, vp, new Matrix4f(vp).invert(), rotation);
    }

    private static float[] fixture(int variant) {
        float[] source = new float[WIDTH*HEIGHT*4], depth = new float[WIDTH*HEIGHT];
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            int i = y*WIDTH+x;
            source[i*4] = variant == 0 ? 0.2f+0.5f*x/WIDTH : 0.9f-0.6f*y/HEIGHT;
            source[i*4+1] = variant == 0 ? 0.15f+0.5f*y/HEIGHT : 0.12f;
            source[i*4+2] = variant == 0 ? 0.3f+0.12f*(float)Math.sin(x*0.23+y*0.16) : 0.85f;
            source[i*4+3] = 1;
            depth[i] = variant == 0 ? 0.01f+0.98f*x/WIDTH : (x < WIDTH/2 ? 1 : 0.24f);
        }
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, source);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RED, GL_FLOAT, depth);
        return source;
    }

    private static float[] draw() {
        glDrawArrays(GL_TRIANGLES, 0, 3);
        float[] output = new float[WIDTH*HEIGHT*4];
        glReadPixels(0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, output);
        frameCount++;
        for (float value : output) if (!Float.isFinite(value))
            throw new AssertionError("Non-finite GPU output in frame " + frameCount);
        return output;
    }

    private static double difference(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.abs(a[i]-b[i]);
        return sum/a.length;
    }

    private static float maximumDifference(float[] a, float[] b) {
        float maximum = 0;
        for (int i = 0; i < a.length; i++) maximum = Math.max(maximum, Math.abs(a[i]-b[i]));
        return maximum;
    }

    private static void contrast(float[] output, String description) {
        double sum = 0, sumSquares = 0;
        float minimum = 1, maximum = 0;
        for (int i = 0; i < output.length; i += 4) {
            double luma = output[i]*0.2126+output[i+1]*0.7152+output[i+2]*0.0722;
            sum += luma;
            sumSquares += luma*luma;
            minimum = Math.min(minimum, (float)luma);
            maximum = Math.max(maximum, (float)luma);
            if (output[i] < 0 || output[i+1] < 0 || output[i+2] < 0 ||
                    output[i] > 1.00001 || output[i+1] > 1.00001 || output[i+2] > 1.00001)
                throw new AssertionError(description + " output outside display range");
        }
        int count = WIDTH*HEIGHT;
        double deviation = Math.sqrt(Math.max(0, sumSquares/count-Math.pow(sum/count, 2)));
        if (deviation < 0.01 || maximum-minimum < 0.06)
            throw new AssertionError(description + " flat image: stddev=" + deviation + ", range=" + (maximum-minimum));
    }

    private static void validatePrograms(int desktop, int vr) {
        float neutralError = 0, sourceError = 0, parityError = 0, blendError = 0;
        for (int p : new int[]{desktop, vr}) for (int quality = 0; quality <= 2; quality++)
                for (float access : new float[]{0, 1}) for (int side : new int[]{-1, 0, 1}) {
            Eye eye = eye(side, 0.27f, -0.15f, 0.32f, new Vector3f(), side != 0);
            float[] source = fixture(0);
            setup(p, eye, 0, quality, access, 12, 0.317f);
            neutralError = Math.max(neutralError, maximumDifference(source, draw()));
            setup(p, eye, 1, quality, access, 12, 0.317f);
            float[] full = draw();
            contrast(full, "quality=" + quality + ", access=" + access + ", side=" + side);
            fixture(1);
            sourceError = Math.max(sourceError, maximumDifference(full, draw()));
            fixture(0);
            setup(p, eye, 0.37f, quality, access, 12, 0.317f);
            float[] partial = draw();
            for (int i = 0; i < partial.length; i++)
                blendError = Math.max(blendError, Math.abs(partial[i]-(source[i]*0.63f+full[i]*0.37f)));
            if (p == desktop && access == 0 && side == 0)
                FRAMES.add(new Frame("Quality " + quality, full));
        }
        Eye center = eye(0, 0.27f, -0.15f, 0.32f, new Vector3f(), false);
        fixture(0);
        for (int quality = 0; quality <= 2; quality++) for (float blend : new float[]{0, 0.37f, 1}) {
            setup(desktop, center, blend, quality, 0, 12, 0.317f);
            float[] a = draw();
            setup(vr, center, blend, quality, 0, 12, 0.317f);
            parityError = Math.max(parityError, maximumDifference(a, draw()));
        }
        if (neutralError > 0.000001f || sourceError > 0.000001f || parityError > 0.000001f || blendError > 0.000002f)
            throw new AssertionError("World replacement mismatch: neutral=" + neutralError + ", source/depth=" + sourceError +
                    ", native parity=" + parityError + ", blend=" + blendError);
        System.out.printf("PASS: all 3 quality levels, accessibility states and desktop/both eyes; neutral max %.8f, source/depth independence %.8f, native parity %.8f, linear transitions %.8f%n",
                neutralError, sourceError, parityError, blendError);
    }

    private static void cameraResponse(int p) {
        Eye center = eye(0, 0.27f, -0.15f, 0, new Vector3f(), false);
        setup(p, center, 1, 2, 0, 12, 0.317f);
        float[] baseline = draw();
        String[] names = {"Head rotation", "Head translation", "Time", "Session seed", "Reduced photosensitivity"};
        for (int variant = 0; variant < names.length; variant++) {
            Eye changed = variant == 0 ? eye(0, 0.62f, -0.03f, 0.24f, new Vector3f(), false) : variant == 1 ?
                    eye(0, 0.27f, -0.15f, 0, new Vector3f(0.8f, -0.25f, -0.5f), false) : center;
            setup(p, changed, 1, 2, variant == 4 ? 1 : 0, variant == 2 ? 26 : 12, variant == 3 ? 0.817f : 0.317f);
            float[] output = draw();
            contrast(output, names[variant]);
            double difference = difference(baseline, output);
            if (difference < 0.0001) throw new AssertionError(names[variant] + " does not affect the fractal world: " + difference);
            if (variant < 4) FRAMES.add(new Frame(names[variant], output));
            System.out.printf("PASS: %s changes generated world; mean RGBA difference %.6f%n", names[variant], difference);
        }
        // Representative extremal legal seed/depth/flow inputs must remain finite.
        for (float seed : new float[]{0, 1}) for (float depth : new float[]{0, 1}) for (float flow : new float[]{0, 1}) {
            setup(p, center, 1, 0, 0, 25000, seed);
            vec(p, "FractalB", seed, 1-seed, depth, flow);
            contrast(draw(), "legal seed/depth/flow extremes");
        }
    }

    private static void stereoWorld(String source, int desktop, int vr) {
        // Replace only main: production inverse projection, eye-origin and fractal
        // field helpers execute verbatim for projected, shared physical points.
        int initStart = source.indexOf("    float reduced =", source.indexOf("void main()"));
        int initEnd = source.indexOf("    vec3 origin = realmEyeOrigin();", initStart);
        if (initStart < 0 || initEnd < 0) throw new AssertionError("Production field initialization not found for stereo probe");
        String initialization = source.substring(initStart, initEnd);
        String probeSource = source.replaceFirst("void\\s+main\\s*\\(\\s*\\)", "void artistEntry()")
                .replaceFirst("#version 150", "#version 150\nuniform vec4 TestUv;\n") + "\nvoid main() {\n" + initialization + """
                    vec3 point = realmEyeOrigin() + realmRay(TestUv.xy) * TestUv.z;
                    if (TestUv.w < 0.5) fragColor = vec4(point, 1.0);
                    else fragColor = vec4(realmField(point), 0.0, 1.0);
                }
                """;
        int probe = program(probeSource, "production fractal stereo ray/origin/field probe");
        float positionError = 0, fieldError = 0;
        int probes = 0;
        for (int variant = 0; variant < 3; variant++) for (float distance : new float[]{0.3f, 1, 4, 12}) {
            Vector3f translation = variant == 2 ? new Vector3f(0.8f, -0.25f, -0.5f) : new Vector3f();
            float roll = variant == 0 ? 0 : variant == 1 ? 0.46f : -0.29f;
            Eye[] eyes = {eye(-1, 0.27f, -0.15f, roll, translation, variant == 2),
                    eye(1, 0.27f, -0.15f, roll, translation, variant == 2)};
            Vector3f world = eyes[0].rotation.transformDirection(new Vector3f(distance*0.13f, -distance*0.07f, -distance)).add(eyes[0].head);
            Vector3f expected = new Vector3f(world).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
            float[] leftField = null;
            for (Eye eye : eyes) {
                Vector4f clip = eye.vp.transform(new Vector4f(new Vector3f(world).sub(eye.position), 1));
                float u = clip.x/clip.w*0.5f+0.5f, v = clip.y/clip.w*0.5f+0.5f;
                float rayDistance = new Vector3f(world).sub(eye.position).length()*REALM_SCALE;
                setup(probe, eye, 1, 2, 0, 12, 0.317f);
                vec(probe, "TestUv", u, v, rayDistance, 0);
                float[] position = draw();
                positionError = Math.max(positionError, new Vector3f(position[0], position[1], position[2]).distance(expected));
                vec(probe, "TestUv", u, v, rayDistance, 1);
                float[] field = draw();
                if (leftField == null) leftField = field;
                else for (int component = 0; component < 2; component++)
                    fieldError = Math.max(fieldError, Math.abs(leftField[component]-field[component]));
                probes++;
            }
        }
        if (positionError > 0.00002f || fieldError > 0.002f)
            throw new AssertionError("Stereo field does not share one generated world: position=" + positionError + ", field=" + fieldError);
        System.out.printf("PASS: %d projected-point GPU probes share the same fractal coordinates/field across asymmetric eyes, canted optics, head roll and movement; max position %.8f, field %.8f%n", probes, positionError, fieldError);
        Eye left = eye(-1, 0.27f, -0.15f, 0.32f, new Vector3f(), true);
        Eye right = eye(1, 0.27f, -0.15f, 0.32f, new Vector3f(), true);
        setup(vr, left, 1, 2, 0, 12, 0.317f);
        float[] a = draw();
        setup(vr, right, 1, 2, 0, 12, 0.317f);
        float[] b = draw();
        double difference = difference(a, b);
        if (difference < 0.0001) throw new AssertionError("Eye views are identical: missing stereo parallax");
        FRAMES.add(new Frame("VR left eye", a));
        FRAMES.add(new Frame("VR right eye", b));
        System.out.printf("PASS: asymmetric, canted 64mm-IPD eyes produce different world views; mean RGBA difference %.6f%n", difference);
    }

    private static void contactSheet(Path target) throws Exception {
        int columns = 3, scale = 1, label = 24;
        int rows = (FRAMES.size()+columns-1)/columns;
        BufferedImage result = new BufferedImage(columns*WIDTH*scale, rows*(HEIGHT*scale+label), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(new Color(12, 14, 20));
        g.fillRect(0, 0, result.getWidth(), result.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        for (int index = 0; index < FRAMES.size(); index++) {
            Frame frame = FRAMES.get(index);
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
                int pixel = ((HEIGHT-1-y)*WIDTH+x)*4;
                int r = Math.round(Math.max(0, Math.min(1, frame.pixels[pixel]))*255);
                int green = Math.round(Math.max(0, Math.min(1, frame.pixels[pixel+1]))*255);
                int b = Math.round(Math.max(0, Math.min(1, frame.pixels[pixel+2]))*255);
                image.setRGB(x, y, r<<16|green<<8|b);
            }
            int x = index%columns*WIDTH*scale, y = index/columns*(HEIGHT*scale+label);
            g.drawImage(image, x, y+label, WIDTH*scale, HEIGHT*scale, null);
            g.setColor(Color.WHITE);
            g.drawString(frame.label, x+8, y+18);
        }
        g.dispose();
        Files.createDirectories(target.getParent());
        ImageIO.write(result, "png", target.toFile());
        System.out.println("Preview: " + target.toAbsolutePath());
    }

    private static void previewFrames(int desktop, int vr) {
        resize(384, 256);
        FRAMES.clear();
        fixture(0);
        Eye center = eye(0, 0.27f, -0.15f, 0, new Vector3f(), false);
        for (int quality = 0; quality <= 2; quality++) {
            setup(desktop, center, 1, quality, 0, 12, 0.317f);
            FRAMES.add(new Frame("Quality " + quality, draw()));
        }
        setup(desktop, eye(0, 0.62f, -0.03f, 0.24f, new Vector3f(), false), 1, 2, 0, 12, 0.317f);
        FRAMES.add(new Frame("Head rotation", draw()));
        setup(desktop, eye(0, 0.27f, -0.15f, 0, new Vector3f(0.8f, -0.25f, -0.5f), false), 1, 2, 0, 12, 0.317f);
        FRAMES.add(new Frame("Head translation", draw()));
        setup(desktop, center, 1, 2, 0, 26, 0.317f);
        FRAMES.add(new Frame("Time", draw()));
        setup(desktop, center, 1, 2, 0, 12, 0.817f);
        FRAMES.add(new Frame("Session seed", draw()));
        for (int side : new int[]{-1, 1}) {
            setup(vr, eye(side, 0.27f, -0.15f, 0.32f, new Vector3f(), true), 1, 2, 0, 12, 0.317f);
            FRAMES.add(new Frame(side < 0 ? "VR left eye" : "VR right eye", draw()));
        }
    }

    private static void performance(int p) {
        // Time only GPU drawing; no pixel reads, allocation, game rendering or
        // shader compilation is included. This is one realm pass, not game FPS.
        resize(1280, 720);
        fixture(0);
        Eye center = eye(0, 0.27f, -0.15f, 0, new Vector3f(), false);
        int query = glGenQueries();
        for (int quality = 0; quality <= 2; quality++) {
            setup(p, center, 1, quality, 0, 12, 0.317f);
            for (int warm = 0; warm < 2; warm++) glDrawArrays(GL_TRIANGLES, 0, 3);
            glFinish();
            double[] milliseconds = new double[5];
            for (int sample = 0; sample < milliseconds.length; sample++) {
                glBeginQuery(GL_TIME_ELAPSED, query);
                glDrawArrays(GL_TRIANGLES, 0, 3);
                glEndQuery(GL_TIME_ELAPSED);
                milliseconds[sample] = glGetQueryObjectui64(query, GL_QUERY_RESULT)/1_000_000.0;
            }
            Arrays.sort(milliseconds);
            System.out.printf("TIMING: quality %d, offscreen 1280x720 single realm pass, 2 warm-up + 5 GPU samples: median %.3f ms (range %.3f-%.3f ms); excludes Minecraft and second VR eye%n",
                    quality, milliseconds[2], milliseconds[0], milliseconds[4]);
        }
        glDeleteQueries(query);
    }

    private record Eye(Vector3f position, Vector3f head, Matrix4f vp, Matrix4f inverse, Matrix4f rotation) {}
    private record Frame(String label, float[] pixels) {}
}
