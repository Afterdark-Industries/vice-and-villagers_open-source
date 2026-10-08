import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Isolated, hidden OpenGL checks. Does not launch or attach to Minecraft/VR. */
public final class ValidateVrShaders {
    private static final int WIDTH = 96, HEIGHT = 64;
    private static final Vector3f HEAD = new Vector3f(0.25f, 1.62f, -0.35f);
    private static int colorTexture, depthTexture;
    private static final List<Integer> PROGRAMS = new ArrayList<>();
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
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        long window = NULL;
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(WIDTH, HEIGHT, "Vice and Villagers shader validation (hidden)", NULL, NULL);
            if (window == NULL) throw new IllegalStateException("Hidden OpenGL context unavailable");
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("GPU: " + glGetString(GL_RENDERER) + " / " + glGetString(GL_VERSION));

            int vao = glGenVertexArrays();
            glBindVertexArray(vao);
            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 3,-1,0, -1,3,0}, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
            glEnableVertexAttribArray(0);
            int fbo = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            int output = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH * HEIGHT * 4]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, output, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
                throw new AssertionError("Floating-point output framebuffer incomplete");
            glViewport(0, 0, WIDTH, HEIGHT);
            glDisable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            colorTexture = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH * HEIGHT * 4]);
            depthTexture = texture(GL_R32F, GL_RED, new float[WIDTH * HEIGHT]);

            Path directory = Path.of(args[0], "src/main/resources/assets/trippycraft/shaders/program");
            Map<String, Integer> programs = new LinkedHashMap<>();
            Map<String, String> sources = new LinkedHashMap<>();
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".fsh")).sorted().toList()) {
                    String source = Files.readString(file);
                    int program = program(source, file.getFileName().toString());
                    programs.put(file.getFileName().toString().replace(".fsh", ""), program);
                    sources.put(file.getFileName().toString().replace(".fsh", ""), source);
                }
            }
            if (args.length > 1 && args[1].equals("--nitrous-only")) {
                nitrousVisibility(programs);
                return;
            }
            if (programs.size() != 29) throw new AssertionError("Expected 14 original + 14 VR + 1 composite programs, found " + programs.size());
            System.out.println("PASS: compiled and linked all " + programs.size() + " production program fragment shaders");
            validateAll(programs);
            nativeSpatial(sources.get("homet_world_warp"), programs.get("homet_world_warp"));
            hoMetEscalation(programs.get("homet_world_warp"));
            vanillaTrailAlpha(Path.of(args[0]));
            stereo(sources.get("mescaline_world_warp_vr"));
            sampling(sources.get("mescaline_world_warp_vr"));
            composite(programs.get("vr_visual_composite"), sources.get("vr_visual_composite"));
            int error = glGetError();
            if (error != GL_NO_ERROR) throw new AssertionError("OpenGL error 0x" + Integer.toHexString(error));
            System.out.println("PASS: all isolated GPU checks; headset comfort and live Vivecraft integration require headset retest");
        } finally {
            for (int program : PROGRAMS) glDeleteProgram(program);
            if (window != NULL) glfwDestroyWindow(window);
            glfwTerminate();
            errorCallback.free();
        }
    }

    private static int shader(int type, String source, String name) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new AssertionError(name + " compile failed:\n" + glGetShaderInfoLog(shader));
        return shader;
    }

    private static int program(String fragmentSource, String name) {
        return program(VERTEX, fragmentSource, name);
    }

    private static int program(String vertexSource, String fragmentSource, String name) {
        int vertex = shader(GL_VERTEX_SHADER, vertexSource, "validation vertex");
        int fragment = shader(GL_FRAGMENT_SHADER, fragmentSource, name);
        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glBindAttribLocation(program, 0, "Position");
        glBindFragDataLocation(program, 0, "fragColor");
        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
            throw new AssertionError(name + " link failed:\n" + glGetProgramInfoLog(program));
        PROGRAMS.add(program);
        return program;
    }

    private static int texture(int internalFormat, int format, float[] data) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, WIDTH, HEIGHT, 0, format, GL_FLOAT, data);
        return texture;
    }

    private static void vec(int p, String name, float x, float y, float z, float w) {
        glUniform4f(glGetUniformLocation(p, name), x, y, z, w);
    }

    private static void setup(int p, Eye eye) {
        glUseProgram(p);
        glUniform2f(glGetUniformLocation(p, "InSize"), WIDTH, HEIGHT);
        glUniform2f(glGetUniformLocation(p, "OutSize"), WIDTH, HEIGHT);
        glUniform2f(glGetUniformLocation(p, "VrReferenceSize"), WIDTH, HEIGHT);
        glUniform1i(glGetUniformLocation(p, "DiffuseSampler"), 0);
        glUniform1i(glGetUniformLocation(p, "DepthSampler"), 1);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glUniformMatrix4fv(glGetUniformLocation(p, "VrViewProjection"), false, eye.vp.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p, "VrInverseViewProjection"), false, eye.inverse.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p, "WorldViewProjection"), false, eye.vp.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p, "WorldInverseViewProjection"), false, eye.inverse.get(new float[16]));
        vec(p, "WorldEyePosition", eye.position.x, eye.position.y, eye.position.z, 0);
        vec(p, "WorldHeadPosition", HEAD.x, HEAD.y, HEAD.z, 0);
        glUniformMatrix4fv(glGetUniformLocation(p, "VrHeadViewProjection"), false, eye.headVp.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p, "VrInverseHeadViewProjection"), false, new Matrix4f(eye.headVp).invert().get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p, "VrHeadView"), false, new Matrix4f(eye.rotation).transpose().get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p, "VrInverseHeadView"), false, eye.rotation.get(new float[16]));
        glUniform1f(glGetUniformLocation(p, "VrEnabled"), 0);
        vec(p, "VrEyePosition", eye.position.x, eye.position.y, eye.position.z, 0);
        vec(p, "VrHeadPosition", HEAD.x, HEAD.y, HEAD.z, 0);
        vec(p, "CameraA", HEAD.x, HEAD.y, HEAD.z, 96);
        Vector3f forward = eye.rotation.transformDirection(new Vector3f(0, 0, -0.05f));
        Vector3f right = eye.rotation.transformDirection(new Vector3f(0.05f * eye.tanX, 0, 0));
        Vector3f up = eye.rotation.transformDirection(new Vector3f(0, 0.05f * eye.tanY, 0));
        vec(p, "CameraB", forward.x, forward.y, forward.z, 0.05f);
        vec(p, "CameraC", right.x, right.y, right.z, 0.317f);
        vec(p, "CameraD", up.x, up.y, up.z, 0.631f);
        vec(p, "Control", 0, 0, 1, 1);
    }

    private static float[] draw() {
        glDrawArrays(GL_TRIANGLES, 0, 3);
        float[] output = new float[WIDTH * HEIGHT * 4];
        glReadPixels(0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, output);
        for (float value : output) {
            if (!Float.isFinite(value)) throw new AssertionError("Non-finite GPU output");
        }
        return output;
    }

    private static final String[] ART = {"WarpA", "WarpB", "WarpC", "WarpD", "WarpE", "PaletteA", "PaletteB", "PaletteC", "Profile", "Env", "EnvA", "EnvB", "EnvDetail", "Dose", "DoseA", "DoseDetail", "LayersC", "MusicA", "MusicB", "MixA", "MixB", "MotionA", "MotionB", "Focus", "Intensity", "ExtraDose", "HighDose"};

    private static void art(int p, String effect, boolean active, boolean strong, boolean accessibility, float time) {
        for (String uniform : ART) vec(p, uniform, 0, 0, 0, 0);
        if (!active) return;
        float strength = strong ? 2.2f : 0.8f;
        float access = accessibility ? 1 : 0;
        vec(p, "WarpA", strength, strength * 0.8f, strength * 0.7f, time);
        vec(p, "WarpB", 0.7f, 0.8f, 0.9f, 0.65f);
        vec(p, "WarpC", 0.6f, 0.7f, 0.8f, 0.55f);
        vec(p, "WarpD", 0.7f, 0.317f, 0.7f, 0.5f);
        vec(p, "WarpE", 0.6f, 0.7f, 0.8f, 0.6f);
        vec(p, "PaletteA", 0.9f, 0.35f, 0.12f, 0.7f);
        vec(p, "PaletteB", 0.05f, 0.7f, 0.65f, 0.7f);
        vec(p, "PaletteC", 0.7f, 0.08f, 0.52f, 0.7f);
        vec(p, "Profile", 0.317f, 0.631f, 0.7f, 0.6f);
        vec(p, "Env", 0.3f, 0.7f, 0.7f, 0.8f);
        vec(p, "EnvA", 0.3f, 0.7f, 0.7f, 0.8f);
        vec(p, "EnvB", 0.6f, 0.4f, 0.5f, 0.8f);
        vec(p, "EnvDetail", 0.6f, 0.7f, 0.8f, 0.5f);
        vec(p, "Dose", 0.7f, 0.8f, 0.7f, strong ? 2.5f : 0.8f);
        vec(p, "DoseDetail", strong ? 2.5f : 0.8f, strong ? 2.5f : 0.8f, 1, 1);
        vec(p, "DoseA", strong ? 6 : 1.3f, 1, strong ? 1.8f : 0.25f, 0.55f);
        vec(p, "MusicA", 0.4f, 0.6f, 0.3f, 0.4f);
        vec(p, "MusicB", 0.5f, 0.3f, 0.61f, 0.317f);
        vec(p, "MixA", 0.2f, 0.3f, 0.4f, 0.2f);
        vec(p, "MixB", 0.3f, 0.2f, 0.1f, 0.2f);
        vec(p, "Focus", 0.5f, 0.5f, 0.30f, 0.371f);
        vec(p, "Control", access, access, 1, 1);
        switch (effect) {
            case "al_lad", "two_ce" -> {
                vec(p, "Control", accessibility ? 0.35f : 1, accessibility ? 0.22f : 1, 0.6f, 0.5f);
                vec(p, "Intensity", 0.8f, 0.6f, strong ? 0.9f : 0.3f, strong ? 1 : 0.25f);
                vec(p, "ExtraDose", strong ? 0.8f : 0.2f, strong ? 0.8f : 0.2f, strong ? 0.8f : 0.2f, strong ? 0.8f : 0.2f);
                vec(p, "HighDose", strong ? 0.8f : 0, strong ? 0.8f : 0, strong ? 0.8f : 0, strong ? 0.8f : 0);
            }
            case "lsd" -> vec(p, "Control", accessibility ? 0.22f : 1, accessibility ? 0.35f : 1, 0, 0);
            case "homet" -> {
                hoMetDose(p, strong ? 100 : 20, time, 0.31f);
                vec(p, "WarpC", 0.70f, 0.317f, 0.55f, 0.90f);
                vec(p, "MotionA", 0.07f, 0, -0.02f, accessibility ? 0.22f : 1);
                vec(p, "MotionB", 0.006f, 0.002f, 0, accessibility ? 0.35f : 1);
                vec(p, "Control", accessibility ? 0.35f : 1, accessibility ? 0.22f : 1, 0.6f, 0.5f);
            }
            case "alcohol" -> {
                vec(p, "WarpC", 0.7f, 0.8f, 0.5f, strong ? 10 : 4);
                vec(p, "WarpD", 0.5f, strong ? 5 : 2, access, access);
            }
            case "cocaine" -> vec(p, "WarpC", 0.55f, access, 0, 0);
            case "ketamine" -> vec(p, "WarpC", 0.5f, strong ? 1 : 0.4f, access, 0);
            case "two_cb" -> vec(p, "WarpC", strength, strength, access, strong ? 2.7f : 0.3f);
            case "mdma" -> vec(p, "WarpC", 0.2f, access, 0.7f, 0.8f);
            case "dmt" -> {
                vec(p, "Dose", strong ? 2.5f : 1.3f, 2.5f, 0.9f, access);
                vec(p, "LayersC", 0.21f, 0.61f, strong ? 2.5f : 1.3f, 45);
            }
            case "psilocybin" -> {
                vec(p, "WarpB", 0.317f, 0.631f, strong ? 12 : 3, strong ? 1 : 0.4f);
                vec(p, "PaletteA", 0.9f, 0.35f, 0.12f, strong ? 2.5f : 0.7f);
                vec(p, "Control", accessibility ? 0.22f : 1, accessibility ? 0.35f : 1, 0, 0);
            }
        }
    }

    private static float[] fixture(Eye eye, int scene) {
        float[] source = new float[WIDTH * HEIGHT * 4];
        float[] depth = new float[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            int i = y * WIDTH + x;
            source[i*4] = 0.2f + 0.45f * x / WIDTH;
            source[i*4+1] = 0.15f + 0.5f * y / HEIGHT;
            source[i*4+2] = 0.3f + 0.12f * (float)Math.sin(x * 0.23 + y * 0.16);
            source[i*4+3] = 1;
            if (scene == 1) {
                depth[i] = 1;
            } else {
                Vector4f near = eye.inverse.transform(new Vector4f((x + 0.5f) / WIDTH * 2 - 1, (y + 0.5f) / HEIGHT * 2 - 1, 0, 1));
                Vector3f ray = new Vector3f(near.x, near.y, near.z).div(near.w).normalize();
                Vector3f forward = eye.rotation.transformDirection(new Vector3f(0, 0, -1));
                float distance = scene == 2 && x < WIDTH / 2 ? 0.5f : 3;
                float t = (distance - new Vector3f(eye.position).sub(HEAD).dot(forward)) / ray.dot(forward);
                Vector4f clip = eye.vp.transform(new Vector4f(new Vector3f(ray).mul(t), 1));
                depth[i] = clip.z / clip.w * 0.5f + 0.5f;
            }
        }
        upload(source, depth);
        return source;
    }

    private static void upload(float[] source, float[] depth) {
        if (source != null) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, colorTexture);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, source);
        }
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RED, GL_FLOAT, depth);
    }

    private static double difference(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.abs(a[i] - b[i]);
        return sum / a.length;
    }

    private static void nitrousVisibility(Map<String, Integer> programs) {
        int frames = 0;
        double minimumContrast = 1;
        for (String name : List.of("nitrous_world_warp", "nitrous_world_warp_vr")) {
            int p = programs.get(name);
            for (int side : new int[]{-1, 1}) {
                Eye eye = eye(side, 0, 0);
                float[] scene = fixture(eye, 0);
                setup(p, eye);
                vec(p, "WarpA", 0, 0, 0, 0);
                if (difference(draw(), scene) > 0.00001) throw new AssertionError(name + " alters sober view");
                for (float hits : new float[]{1, 2, 8, 32}) for (float scale : new float[]{1, 2}) {
                    float amplitude = (float)Math.log1p(hits) * 1.85f * scale;
                    for (float time : new float[]{0, 100, 500}) {
                        upload(scene, null);
                        setup(p, eye);
                        vec(p, "WarpA", 1, amplitude * 1.15f, amplitude * 1.22f, time);
                        vec(p, "WarpB", amplitude * 1.05f, amplitude * 1.18f, amplitude * 1.10f, amplitude * 1.20f);
                        vec(p, "WarpC", amplitude * 0.95f, amplitude * 0.55f, 1, 0);
                        float[] result = draw();
                        for (int i = 0; i < result.length; i += 4) {
                            if (Math.min(result[i], Math.min(result[i+1], result[i+2])) > 0.97f)
                                throw new AssertionError(name + " whiteout at " + hits + " hits, scale " + scale);
                        }
                        // Two nearby scene tones must remain distinguishable at
                        // the crosshair, even at much higher charge than the report.
                        float[][] tones = new float[2][];
                        for (int tone = 0; tone < 2; tone++) {
                            float[] flat = new float[scene.length];
                            for (int i = 0; i < flat.length; i += 4) {
                                flat[i] = flat[i+1] = flat[i+2] = tone == 0 ? 0.2f : 0.4f;
                                flat[i+3] = 1;
                            }
                            upload(flat, null);
                            tones[tone] = draw();
                        }
                        int center = (HEIGHT / 2 * WIDTH + WIDTH / 2) * 4;
                        double contrast = 0;
                        for (int c = 0; c < 3; c++) contrast += tones[1][center+c] - tones[0][center+c];
                        contrast /= 3;
                        minimumContrast = Math.min(minimumContrast, contrast);
                        if (contrast < 0.035) throw new AssertionError(name + " loses centre detail: " + contrast);
                        frames += 3;
                    }
                }
            }
        }
        if (glGetError() != GL_NO_ERROR) throw new AssertionError("OpenGL error during nitrous checks");
        System.out.printf("PASS: nitrous desktop/both eyes, sober identity, 1/2/8/32 hits, visual scales 1/2; %d frames, no whiteout, minimum centre contrast %.4f%n", frames, minimumContrast);
    }

    private static void validateAll(Map<String, Integer> programs) {
        int frames = 0;
        List<String> failures = new ArrayList<>();
        for (var entry : programs.entrySet()) {
            if (!entry.getKey().endsWith("_world_warp_vr")) continue;
            String name = entry.getKey().replace("_world_warp_vr", "");
            int vr = entry.getValue();
            int original = programs.get(name + "_world_warp");
            float neutralError = 0;
            double minimumChange = Double.POSITIVE_INFINITY;
            double maximumParityError = 0;
            for (int camera = 0; camera < 3; camera++) {
                for (int side : new int[]{-1, 1}) {
                    Eye eye = eye(side, camera == 0 ? 0 : camera == 1 ? 0.38f : -0.6f, camera);
                    for (int scene = 0; scene < 3; scene++) {
                        float[] source = fixture(eye, scene);
                        setup(vr, eye);
                        art(vr, name, false, false, false, 0);
                        float[] neutral = draw();
                        frames++;
                        for (int i = 0; i < source.length; i++) neutralError = Math.max(neutralError, Math.abs(neutral[i] - source[i]));
                        for (boolean strong : new boolean[]{false, true}) {
                            setup(vr, eye);
                            art(vr, name, true, strong, false, 1234);
                            float[] active = draw();
                            frames++;
                            // These native programs intentionally leave empty sky exactly untouched.
                            boolean preservesSky = name.equals("homet") || name.equals("lsd");
                            if (!preservesSky || scene != 1)
                                minimumChange = Math.min(minimumChange, difference(active, neutral));
                            if (preservesSky && scene == 1 && difference(active, source) > 0.000002)
                                failures.add(name + " changes empty sky");
                            art(vr, name, true, strong, true, 1361);
                            draw();
                            frames++;
                        }
                    }
                }
            }
            // A virtual centre eye with identical optics must reproduce the desktop art.
            Eye center = eye(0, 0, 0);
            for (int scene = 0; scene < 3; scene++) {
                fixture(center, scene);
                for (int state = 0; state < 3; state++) {
                    boolean active = state != 0;
                    boolean strong = state == 2;
                    setup(original, center);
                    art(original, name, active, strong, false, 1234);
                    float[] desktop = draw();
                    setup(vr, center);
                    art(vr, name, active, strong, false, 1234);
                    float[] transposed = draw();
                    maximumParityError = Math.max(maximumParityError, difference(desktop, transposed));
                    frames += 2;
                }
            }
            boolean hasAuthoredNeutralGate = List.of("alcohol", "cocaine", "ketamine", "two_cb", "mescaline", "mdma", "homet").contains(name);
            if (hasAuthoredNeutralGate && neutralError > 0.00002f) failures.add(name + " authored neutral gate changes the eye image: " + neutralError);
            if (minimumChange < 0.00001) failures.add(name + " active effect does not change the image");
            if (maximumParityError > 0.003) failures.add(name + " centre-eye artwork parity mean error: " + maximumParityError);
            System.out.printf("CHECK: %-10s all scenes/cameras/doses; %s max %.8f, active minimum mean %.6f, centre artwork maximum mean error %.6f%n", name, hasAuthoredNeutralGate ? "neutral" : "authored inactive residual", neutralError, minimumChange, maximumParityError);
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        System.out.println("PASS: " + frames + " finite GPU frames; 14 effects, asymmetric/canted eye optics, head roll, surface/sky/depth edge, regular/high doses and accessibility states");
    }

    private static void nativeSpatial(String source, int authored) {
        String probe = source.replaceFirst("void\\s+main\\s*\\(\\s*\\)", "void artistEntry()")
                .replaceFirst("#version 150", "#version 150\nuniform vec4 TestPoint;\n") + """
                void main() { fragColor = vec4(worldAt(TestPoint.xy, TestPoint.z), 1.0); }
                """;
        int program = program(probe, "4-HO-MET production world-coordinate reconstruction");
        float maximumError = 0;
        for (int variant = 0; variant < 3; variant++) {
            for (int side : new int[]{-1, 1}) {
                Eye eye = eye(side, variant == 1 ? 0.38f : -0.30f, variant);
                setup(program, eye);
                for (float distance : new float[]{0.5f, 2, 8, 32}) {
                    Vector3f world = eye.rotation.transformDirection(new Vector3f(distance * 0.1f, distance * -0.05f, -distance)).add(HEAD);
                    Vector4f clip = eye.vp.transform(new Vector4f(new Vector3f(world).sub(eye.position), 1));
                    vec(program, "TestPoint", clip.x / clip.w * 0.5f + 0.5f,
                            clip.y / clip.w * 0.5f + 0.5f, clip.z / clip.w * 0.5f + 0.5f, 0);
                    float[] result = draw();
                    maximumError = Math.max(maximumError, new Vector3f(result[0], result[1], result[2]).distance(world));
                }
            }
        }
        if (maximumError > 0.008f) throw new AssertionError("Native world coordinate reconstruction error: " + maximumError);
        Eye eye = eye(0, 0, 0);
        fixture(eye, 0);
        setup(authored, eye);
        art(authored, "homet", true, true, false, 1234);
        vec(authored, "WarpB", 0.86f, 0.95f, 0.80f, 2.8f);
        float[] strong = draw();
        // Equal amplitude, changing complexity only: extreme dose must keep
        // evolving geometry rather than maxing blur, brightness or FOV.
        vec(authored, "WarpB", 0.86f, 0.95f, 0.80f, 4.2f);
        float[] extreme = draw();
        double complexityChange = difference(strong, extreme);
        if (complexityChange < 0.00001) throw new AssertionError("4-HO-MET complexity stops evolving at high dose");
        vec(authored, "WarpB", 0.86f, 0.95f, 0.80f, 1024.0f);
        draw();
        System.out.printf("PASS: 4-HO-MET native stereo world reconstruction max %.6f blocks; extreme complexity-only mean change %.6f; finite adversarial complexity%n", maximumError, complexityChange);
    }

    private static float logAmplitude(double amplitude) {
        return (float) (Math.log1p(Math.max(0, amplitude)) / Math.log(2));
    }

    private static void hoMetDose(int p, double dose, float time, float travel) {
        // Fixed representative signature at the peak. This is the documented
        // backend/pass interface; renderer checks remain independent of a game JVM.
        double threshold = Math.max(0, Math.min(1, (dose - 40) / 20));
        double boost = 1 + 1.4 * threshold * threshold * (3 - 2 * threshold);
        double intensity = Math.log1p(dose / 12) * 1.65 * boost;
        double presence = 1 - Math.exp(-intensity);
        double amplitude = presence * Math.log1p(dose / 12) / Math.log1p(40.0 / 12) * boost;
        double complexity = Math.log1p(dose / 12) / Math.log(2) * 1.85 * boost;
        double colorGate = Math.max(0, Math.min(1, (dose - 40) / 10));
        colorGate *= colorGate * (3 - 2 * colorGate);
        vec(p, "WarpA", (float) presence, logAmplitude(amplitude * 0.90), logAmplitude(amplitude * 0.85), time);
        vec(p, "WarpB", logAmplitude(amplitude * 0.86), logAmplitude(amplitude * 0.95),
                logAmplitude(amplitude * 0.80), (float) Math.log1p(complexity));
        vec(p, "Intensity", (float) colorGate, (float) Math.log1p(Math.max(0, dose - 40) / 10),
                (float) Math.log1p(intensity), travel);
    }

    private static void hoMetEscalation(int p) {
        Eye eye = eye(0, 0, 0);
        float[] neutral = fixture(eye, 0);
        setup(p, eye);
        art(p, "homet", true, false, false, 1234);
        double[] doses = {20, 40, 50, 100, 320, 1000, 1.0e12, 1.0e100, Double.MAX_VALUE};
        float[][] renders = new float[doses.length][];
        double[] changes = new double[doses.length];
        for (int i = 0; i < doses.length; i++) {
            hoMetDose(p, doses[i], 1234, 0.31f);
            renders[i] = draw();
            changes[i] = difference(neutral, renders[i]);
        }
        if (changes[2] < changes[1] * 1.5)
            throw new AssertionError("50mg does not substantially exceed40mg: " + changes[1] + " -> " + changes[2]);
        if (changes[3] < changes[2] * 1.08)
            throw new AssertionError("100mg does not exceed50mg: " + changes[2] + " -> " + changes[3]);
        for (int i = 4; i < doses.length; i++)
            if (difference(renders[i], renders[i - 1]) < 0.001)
                throw new AssertionError("Dose presentation plateaus at " + doses[i]);
        double maximumStep = 0;
        int temporalFrames = 0;
        for (double dose : new double[]{40, 50, 100, 1000}) {
            float[] previous = null;
            for (int frame = 0; frame < 121; frame++) {
                float seconds = frame / 60.0f;
                float travel = 0.31f + seconds * (float) (0.035 + Math.log1p(Math.log1p(Math.max(0, dose - 40) / 10)) * 0.012);
                hoMetDose(p, dose, 1234 + seconds * 20, travel);
                float[] current = draw();
                temporalFrames++;
                if (previous != null) maximumStep = Math.max(maximumStep, difference(previous, current));
                previous = current;
            }
        }
        if (maximumStep > 0.025) throw new AssertionError("Abrupt4-HO-MET temporal color step: " + maximumStep);
        hoMetDose(p, 49.999, 1234, 0.31f);
        float[] below = draw();
        hoMetDose(p, 50.001, 1234, 0.31f);
        double thresholdStep = difference(below, draw());
        if (thresholdStep > 0.002) throw new AssertionError("50mg color gate discontinuity: " + thresholdStep);
        System.out.printf("PASS: 4-HO-MET dose escalation mean changes40mg %.6f /50mg %.6f /100mg %.6f /320mg %.6f /1000mg %.6f; finite continuing output through Double.MAX_VALUE%n",
                changes[1], changes[2], changes[3], changes[4], changes[5]);
        System.out.printf("PASS: %d temporal frames at40/50/100/1000mg; maximum60fps mean step %.6f; smooth50mg threshold %.6f%n",
                temporalFrames, maximumStep, thresholdStep);
    }

    private static void vanillaTrailAlpha(Path root) throws Exception {
        // Execute the exact 1.21.1 shader used by HoMetTrailRenderer. It cuts out
        // transparent source texels before applying fading vertex alpha.
        String fragment;
        Path resourceJar;
        try (var files = Files.list(root.resolve("build/moddev/artifacts"))) {
            resourceJar = files.filter(p -> p.getFileName().toString().endsWith("client-extra-aka-minecraft-resources.jar"))
                    .findFirst().orElseThrow();
        }
        try (var zip = new java.util.zip.ZipFile(resourceJar.toFile())) {
            String fog = new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/include/fog.glsl")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("#version 150", "");
            fragment = new String(zip.getInputStream(zip.getEntry("assets/minecraft/shaders/core/rendertype_entity_translucent.fsh")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("#moj_import <fog.glsl>", fog);
        }
        String vertex = """
                #version 150
                in vec3 Position;
                uniform float Fade;
                out float vertexDistance;
                out vec4 vertexColor;
                out vec4 lightMapColor;
                out vec4 overlayColor;
                out vec2 texCoord0;
                void main() {
                    gl_Position = vec4(Position, 1.0);
                    vertexDistance = 0.0;
                    vertexColor = vec4(1.0, 1.0, 1.0, Fade);
                    lightMapColor = vec4(1.0);
                    overlayColor = vec4(0.0, 0.0, 0.0, 1.0);
                    texCoord0 = Position.xy * 0.5 + 0.5;
                }
                """;
        int p = program(vertex, fragment, "vanilla 1.21.1 translucent trail-alpha probe");
        Eye eye = eye(0, 0, 0);
        float[] opaque = fixture(eye, 0);
        setup(p, eye);
        glUniform1i(glGetUniformLocation(p, "Sampler0"), 0);
        vec(p, "ColorModulator", 1, 1, 1, 1);
        glUniform1f(glGetUniformLocation(p, "FogStart"), 100);
        glUniform1f(glGetUniformLocation(p, "FogEnd"), 200);
        for (float alpha : new float[]{0.003f, 0.02f, 0.08f, 0.20f}) {
            glUniform1f(glGetUniformLocation(p, "Fade"), alpha);
            float[] rendered = draw();
            for (int i = 3; i < rendered.length; i += 4)
                if (Math.abs(rendered[i] - alpha) > 0.000001f)
                    throw new AssertionError("Trail vertex alpha was discarded or modified: " + alpha);
        }
        for (int i = 3; i < opaque.length; i += 4) opaque[i] = 0.05f;
        upload(opaque, new float[WIDTH * HEIGHT]);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        for (float value : draw())
            if (value != 0.0f) throw new AssertionError("Transparent source cutout leaked into trail mesh");
        System.out.println("PASS: exact vanilla entity-translucent fragment preserves trail alpha 0.003/0.02/0.08/0.20; source alpha<0.1 remains cut out");
    }

    private static Eye eye(int side, float roll, int variant) {
        Matrix4f rotation = new Matrix4f().rotationYXZ(0.27f, -0.15f, roll);
        Vector3f position = rotation.transformDirection(new Vector3f(side * 0.032f, 0, 0)).add(HEAD);
        float tanY = (float)Math.tan(Math.toRadians(45));
        float tanX = tanY * WIDTH / HEIGHT;
        float asymX = side == 0 ? 0 : side * (variant == 2 ? 0.013f : 0.007f);
        float asymY = side == 0 ? 0 : 0.003f;
        Matrix4f eyeRotation = new Matrix4f(rotation).rotateY(variant == 2 ? side * 0.025f : 0);
        Matrix4f projection = new Matrix4f().frustum(-0.05f*tanX+asymX, 0.05f*tanX+asymX, -0.05f*tanY+asymY, 0.05f*tanY+asymY, 0.05f, 96);
        Matrix4f headProjection = new Matrix4f().frustum(-0.05f*tanX, 0.05f*tanX, -0.05f*tanY, 0.05f*tanY, 0.05f, 96);
        Matrix4f vp = projection.mul(new Matrix4f(eyeRotation).transpose());
        Matrix4f headVp = headProjection.mul(new Matrix4f(rotation).transpose());
        return new Eye(position, vp, new Matrix4f(vp).invert(), rotation, headVp, tanX, tanY);
    }

    private static void stereo(String source) {
        // Only replace the entry point and fragment coordinate input. All production
        // unprojection/canonical-mapping helpers execute verbatim on the actual GPU.
        String probe = source.replaceFirst("void\\s+main\\s*\\(\\s*\\)", "void artistEntry()")
                .replace("gl_FragCoord", "TestFragmentCoord")
                .replaceFirst("#version 150", "#version 150\nuniform vec4 TestFragmentCoord;\n") + """
                void main() { vrInit(); fragColor = vec4(vrCanonicalUv, vrReferenceDepth, 1.0); }
                """;
        int p = program(probe, "production canonical coordinate stereo probe");
        float maximumUvError = 0, maximumDepthError = 0;
        int probes = 0;
        for (int variant = 0; variant < 3; variant++) {
            float roll = variant == 0 ? 0 : variant == 1 ? 0.38f : -0.6f;
            Eye left = eye(-1, roll, variant), right = eye(1, roll, variant);
            for (float distance : new float[]{0.25f, 1, 3, 10}) {
                Vector3f world = left.rotation.transformDirection(new Vector3f(distance*0.13f, distance*-0.07f, -distance)).add(HEAD);
                Vector4f reference = left.headVp.transform(new Vector4f(new Vector3f(world).sub(HEAD), 1));
                float expectedU = reference.x/reference.w*0.5f+0.5f;
                float expectedV = reference.y/reference.w*0.5f+0.5f;
                for (Eye eye : new Eye[]{left, right}) {
                    Vector4f clip = eye.vp.transform(new Vector4f(new Vector3f(world).sub(eye.position), 1));
                    float depth = clip.z/clip.w*0.5f+0.5f;
                    float[] depths = new float[WIDTH*HEIGHT];
                    java.util.Arrays.fill(depths, depth);
                    upload(null, depths);
                    setup(p, eye);
                    vec(p, "TestFragmentCoord", (clip.x/clip.w*0.5f+0.5f)*WIDTH, (clip.y/clip.w*0.5f+0.5f)*HEIGHT, 0, 1);
                    float[] output = draw();
                    maximumUvError = Math.max(maximumUvError, Math.max(Math.abs(output[0]-expectedU), Math.abs(output[1]-expectedV)));
                    maximumDepthError = Math.max(maximumDepthError, Math.abs(output[2]-distance));
                    probes++;
                }
            }
        }
        if (maximumUvError > 0.00003f || maximumDepthError > 0.003f)
            throw new AssertionError("Canonical stereo coordinates diverged: uv=" + maximumUvError + ", depth=" + maximumDepthError);
        System.out.printf("PASS: %d production canonical-coordinate GPU probes; asymmetry, 64mm IPD, yaw/pitch/roll and canted lenses; maximum UV error %.8f, head-depth error %.7f blocks%n", probes, maximumUvError, maximumDepthError);
    }

    private static void sampling(String source) {
        String probe = source.replaceFirst("void\\s+main\\s*\\(\\s*\\)", "void artistEntry()")
                .replace("gl_FragCoord", "TestFragmentCoord")
                .replaceFirst("#version 150", "#version 150\nuniform vec4 TestFragmentCoord;\nuniform vec4 TestSample;\n") + """
                void main() { vrInit(); fragColor = vec4(vrCanonicalToEyeUv(TestSample.xy), 0.0, 1.0); }
                """;
        int p = program(probe, "production full-strength sample mapping probe");
        float maximumError = 0;
        int probes = 0;
        for (int variant = 0; variant < 3; variant++) for (int side : new int[]{-1, 1}) {
            Eye eye = eye(side, variant == 0 ? 0 : variant == 1 ? 0.38f : -0.6f, variant);
            for (int scene : new int[]{0, 1}) {
                fixture(eye, scene);
                setup(p, eye);
                Vector3f original = eye.rotation.transformDirection(new Vector3f(0.39f, -0.21f, -3)).add(HEAD);
                Vector4f actualClip = eye.vp.transform(new Vector4f(new Vector3f(original).sub(eye.position), 1));
                Vector4f canonicalClip = eye.headVp.transform(new Vector4f(new Vector3f(original).sub(HEAD), 1));
                float u = actualClip.x/actualClip.w*0.5f+0.5f;
                float v = actualClip.y/actualClip.w*0.5f+0.5f;
                vec(p, "TestFragmentCoord", u*WIDTH, v*HEIGHT, 0, 1);
                float canonicalU = canonicalClip.x/canonicalClip.w*0.5f+0.5f;
                float canonicalV = canonicalClip.y/canonicalClip.w*0.5f+0.5f;
                if (scene == 1) {
                    // Sky canonical coordinates follow the actual eye direction at infinity.
                    Vector3f direction = new Vector3f(original).sub(eye.position);
                    Vector4f sky = eye.headVp.transform(new Vector4f(direction, 0));
                    canonicalU = sky.x/sky.w*0.5f+0.5f;
                    canonicalV = sky.y/sky.w*0.5f+0.5f;
                }
                for (float offset : new float[]{0, 0.02f, 0.20f}) {
                    float sampleU = canonicalU + offset;
                    float sampleV = canonicalV - offset * 0.60f;
                    vec(p, "TestSample", sampleU, sampleV, 0, 0);
                    float[] output = draw();
                    Vector4f direction = new Matrix4f(eye.headVp).invert().transform(new Vector4f(sampleU*2-1, sampleV*2-1, 0, 1));
                    Vector3f ray = new Vector3f(direction.x, direction.y, direction.z).div(direction.w);
                    Vector3f headRay = new Matrix4f(eye.rotation).transpose().transformDirection(new Vector3f(ray));
                    Vector3f target = new Vector3f(ray).mul(3 / -headRay.z).add(HEAD);
                    Vector4f expected = scene == 1 ? eye.vp.transform(new Vector4f(ray, 0))
                            : eye.vp.transform(new Vector4f(target.sub(eye.position), 1));
                    maximumError = Math.max(maximumError, Math.max(Math.abs(output[0]-(expected.x/expected.w*0.5f+0.5f)), Math.abs(output[1]-(expected.y/expected.w*0.5f+0.5f))));
                    probes++;
                }
            }
        }
        if (maximumError > 0.00005f) throw new AssertionError("Full-strength sample reprojection diverged: " + maximumError);
        System.out.printf("PASS: %d production source-sample probes, exact neutral and 2%%/20%% canonical shifts without metre cap, surface/sky; maximum eye UV error %.8f%n", probes, maximumError);
    }

    private static void composite(int p, String source) {
        int overlay = texture(GL_RGBA32F, GL_RGBA, new float[WIDTH*HEIGHT*4]);
        float neutralError = 0, alphaError = 0, mappingError = 0;
        for (int side : new int[]{-1, 1}) {
            Eye eye = eye(side, 0.38f, 2);
            float[] input = fixture(eye, 0);
            setup(p, eye);
            glUniform1f(glGetUniformLocation(p, "VrVisualEnabled"), 0);
            glUniform1f(glGetUniformLocation(p, "VrOverlayEnabled"), 0);
            float[] neutral = draw();
            for (int i = 0; i < input.length; i++) neutralError = Math.max(neutralError, Math.abs(neutral[i]-input[i]));
            glUniform1f(glGetUniformLocation(p, "VrVisualEnabled"), 1);
            glUniformMatrix4fv(glGetUniformLocation(p, "VrVisualTransform"), false, new Matrix4f().get(new float[16]));
            float[] identity = draw();
            for (int i = 0; i < input.length; i++) neutralError = Math.max(neutralError, Math.abs(identity[i]-input[i]));
            glUniform1i(glGetUniformLocation(p, "OverlaySampler"), 2);
            glUniform1f(glGetUniformLocation(p, "VrOverlayEnabled"), 1);
            for (float alpha : new float[]{0, 0.4f, 1}) {
                float[] pixels = new float[WIDTH*HEIGHT*4];
                for (int i = 0; i < pixels.length; i += 4) {
                    pixels[i] = 0.5f*alpha;
                    pixels[i+1] = 0.25f*alpha;
                    pixels[i+2] = 0.75f*alpha;
                    pixels[i+3] = alpha;
                }
                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, overlay);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, pixels);
                float[] composed = draw();
                int center = ((HEIGHT/2)*WIDTH+WIDTH/2)*4;
                for (int component = 0; component < 4; component++) {
                    float expected = pixels[center+component] + input[center+component]*(1-alpha);
                    alphaError = Math.max(alphaError, Math.abs(composed[center+component]-expected));
                }
            }
        }
        String probe = source.replaceFirst("void\\s+main\\s*\\(\\s*\\)", "void compositorEntry()") + """
                uniform vec4 TestUv;
                void main() { fragColor = vec4(visualUv(TestUv.xy, worldRay(TestUv.xy)), 0.0, 1.0); }
                """;
        int mapping = program(probe, "production shared camera transform probe");
        int probes = 0;
        for (int variant = 0; variant < 3; variant++) for (int side : new int[]{-1, 1}) {
            Eye eye = eye(side, variant == 0 ? 0 : variant == 1 ? 0.38f : -0.6f, variant);
            for (int scene : new int[]{0, 1}) {
                fixture(eye, scene);
                setup(mapping, eye);
                glUniform1f(glGetUniformLocation(mapping, "VrVisualEnabled"), 1);
                Matrix4f transform = new Matrix4f().translation(0.05f, -0.025f, 0.02f).rotateYXZ(0.1f, -0.06f, 0.22f).scale(0.85f, 0.85f, 1);
                glUniformMatrix4fv(glGetUniformLocation(mapping, "VrVisualTransform"), false, transform.get(new float[16]));
                for (float u : new float[]{0.3f, 0.5f, 0.7f}) {
                    float v = 0.45f;
                    vec(mapping, "TestUv", u, v, 0, 0);
                    float[] output = draw();
                    Vector4f near = eye.inverse.transform(new Vector4f(u*2-1, v*2-1, 0, 1));
                    Vector3f ray = new Vector3f(near.x, near.y, near.z).div(near.w).normalize();
                    Matrix4f headView = new Matrix4f(eye.rotation).transpose();
                    Vector3f origin = headView.transformDirection(new Vector3f(eye.position).sub(HEAD));
                    Vector3f direction = headView.transformDirection(ray);
                    transform.transformPosition(origin);
                    transform.transformDirection(direction);
                    Vector4f expected;
                    if (scene == 1) {
                        expected = eye.vp.transform(new Vector4f(eye.rotation.transformDirection(direction), 0));
                    } else {
                        float distance = (-3-origin.z)/direction.z;
                        Vector3f world = eye.rotation.transformDirection(direction.mul(distance).add(origin)).add(HEAD);
                        expected = eye.vp.transform(new Vector4f(world.sub(eye.position), 1));
                    }
                    mappingError = Math.max(mappingError, Math.max(Math.abs(output[0]-(expected.x/expected.w*0.5f+0.5f)), Math.abs(output[1]-(expected.y/expected.w*0.5f+0.5f))));
                    probes++;
                }
            }
        }
        if (neutralError > 0.00002f || alphaError > 0.00001f || mappingError > 0.00008f)
            throw new AssertionError("Composite mismatch: neutral=" + neutralError + ", premultiplied alpha=" + alphaError + ", eye mapping=" + mappingError);
        System.out.printf("PASS: composite disabled/identity exact, premultiplied overlays alpha0/0.4/1, %d camera roll/yaw/pitch/zoom/translation surface/sky probes; max neutral %.8f, alpha %.8f, eye UV %.8f%n", probes, neutralError, alphaError, mappingError);
        glDeleteTextures(overlay);
    }

    private record Eye(Vector3f position, Matrix4f vp, Matrix4f inverse, Matrix4f rotation, Matrix4f headVp, float tanX, float tanY) {}
}
