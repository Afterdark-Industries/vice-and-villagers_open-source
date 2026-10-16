import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataNode;
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
    private static int[][] artistPalettes;
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
        loadArtistPalettes(root);
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
            if(args.length>1 && args[1].equals("--beauty-only")) {
                previewFrames(desktopProgram,vrProgram);
                contactSheet(root.resolve("build/dmt-fractal-contact-sheet.png"));
                beautyAndAnimation(desktopProgram,root.resolve("build"));
                return;
            }
            validatePrograms(desktopProgram, vrProgram);
            cameraResponse(desktopProgram);
            stereoWorld(desktop, desktopProgram, vrProgram);
            finiteStyles(desktopProgram,vrProgram);
            evolution(desktopProgram, desktop);
            if(glGetUniformLocation(desktopProgram,"DepthSampler")>=0)
                transitions(desktopProgram,vrProgram,desktop,root.resolve("build"));
            previewFrames(desktopProgram, vrProgram);
            contactSheet(root.resolve("build/dmt-fractal-contact-sheet.png"));
            beautyAndAnimation(desktopProgram, root.resolve("build"));
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
        glUniformMatrix4fv(glGetUniformLocation(p, "WorldViewProjection"), false, eye.vp.get(new float[16]));
        vec(p, "WorldEyePosition", eye.position.x, eye.position.y, eye.position.z, 0);
        vec(p, "WorldHeadPosition", eye.head.x, eye.head.y, eye.head.z, 0);
        Vector3f realmHead = new Vector3f(eye.head).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
        vec(p, "RealmOrigin", realmHead.x, realmHead.y, realmHead.z, REALM_SCALE);
        vec(p, "FractalA", blend, time, quality, access);
        vec(p, "FractalB", seed, 0.631f, 0.78f, 0.65f);
        vec(p, "FractalC", 1, 0.19f, 0.83f, 3);
        artistPalette(p,0);
    }

    private static void loadArtistPalettes(Path root)throws Exception {
        // Fixture colours come from the shipped trip signature palette table.
        String source=Files.readString(root.resolve("src/main/java/com/trippycraft/substance/dmt/DmtTripSignature.java"));
        int start=source.indexOf("int[][] palettes ="),end=source.indexOf("};",start);
        if(start<0||end<0)throw new AssertionError("Trip artist palette fixture unavailable");
        var matcher=java.util.regex.Pattern.compile("0x[0-9A-Fa-f]{6}").matcher(source.substring(start,end));
        List<Integer> colours=new ArrayList<>();while(matcher.find())colours.add(Integer.parseInt(matcher.group().substring(2),16));
        if(colours.size()<3||colours.size()%3!=0)throw new AssertionError("Trip palette fixture malformed");
        artistPalettes=new int[colours.size()/3][3];
        for(int i=0;i<colours.size();i++)artistPalettes[i/3][i%3]=colours.get(i);
    }

    private static void artistPalette(int p,int index) {
        int[] colours=artistPalettes[Math.floorMod(index,artistPalettes.length)];
        for(int i=0;i<3;i++)vec(p,"Palette"+(char)('A'+i),(colours[i]>>16&255)/255f,(colours[i]>>8&255)/255f,(colours[i]&255)/255f,1);
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
        uploadFixture(source,depth);
        return source;
    }

    private static void uploadFixture(float[] source,float[] depth) {
        if(source!=null) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RGBA, GL_FLOAT, source);
        }
        if(depth!=null) {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL_RED, GL_FLOAT, depth);
        }
    }

    private static Fixture roomFixture(Eye eye) {
        float[] source=new float[WIDTH*HEIGHT*4],depth=new float[WIDTH*HEIGHT];
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
            int i=y*WIDTH+x;
            Vector4f clip=eye.inverse.transform(new Vector4f((x+.5f)/WIDTH*2-1,(y+.5f)/HEIGHT*2-1,0,1));
            Vector3f ray=new Vector3f(clip.x,clip.y,clip.z).div(clip.w).normalize();
            Vector3f origin=new Vector3f(eye.position).sub(ANCHOR);
            float nearest=96;int surface=0;
            float[] rates={ray.y,ray.y,ray.x,ray.x,ray.z};
            float[] offsets={-1.5f-origin.y,3-origin.y,-4-origin.x,4-origin.x,-7-origin.z};
            for(int plane=0;plane<rates.length;plane++) {
                float distance=offsets[plane]/rates[plane];
                if(distance>0&&distance<nearest){nearest=distance;surface=plane;}
            }
            Vector3f hit=new Vector3f(ray).mul(nearest).add(origin);
            float a=surface<2?hit.x:surface<4?hit.z:hit.x,b=surface<2?hit.z:hit.y;
            float checker=((int)Math.floor(a)+(int)Math.floor(b))%2==0?.72f:.24f;
            float grid=Math.min(a-(float)Math.floor(a),b-(float)Math.floor(b));
            if(grid<.045f)checker=.08f;
            source[i*4]=surface<2?checker:checker*.48f;
            source[i*4+1]=surface<2?checker*.82f:checker*.93f;
            source[i*4+2]=surface<2?checker*.52f:checker;
            source[i*4+3]=1;
            Vector4f projected=eye.vp.transform(new Vector4f(new Vector3f(ray).mul(nearest),1));
            depth[i]=Math.max(0,Math.min(1,projected.z/projected.w*.5f+.5f));
        }
        uploadFixture(source,depth);return new Fixture(source,depth);
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
                for (float access : new float[]{0, 1}) for (float intensity:new float[]{.25f,1}) for (int side : new int[]{-1, 0, 1}) {
            Eye eye = eye(side, 0.27f, -0.15f, 0.32f, new Vector3f(), side != 0);
            float[] source = fixture(0);
            setup(p, eye, 0, quality, access, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            neutralError = Math.max(neutralError, maximumDifference(source, draw()));
            setup(p, eye, 1, quality, access, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            float[] full = draw();
            contrast(full, "quality=" + quality + ", access=" + access + ", side=" + side);
            fixture(1);
            sourceError = Math.max(sourceError, maximumDifference(full, draw()));
            fixture(0);
            setup(p, eye, 0.37f, quality, access, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            float[] partial = draw();
            for (int i = 0; i < partial.length; i++)
                blendError = Math.max(blendError, Math.abs(partial[i]-(source[i]*0.63f+full[i]*0.37f)));
            if (p == desktop && access == 0 && side == 0)
                FRAMES.add(new Frame("Quality " + quality, full));
        }
        Eye center = eye(0, 0.27f, -0.15f, 0.32f, new Vector3f(), false);
        fixture(0);
        for (int quality = 0; quality <= 2; quality++) for(float intensity:new float[]{.25f,1}) for (float blend : new float[]{0, 0.37f, 1}) {
            setup(desktop, center, blend, quality, 0, 12, 0.317f);
            vec(desktop,"FractalC",intensity,.19f,.83f,3);
            float[] a = draw();
            setup(vr, center, blend, quality, 0, 12, 0.317f);
            vec(vr,"FractalC",intensity,.19f,.83f,3);
            parityError = Math.max(parityError, maximumDifference(a, draw()));
        }
        if (neutralError > 0.000001f || sourceError > 0.000001f || parityError > 0.000001f)
            throw new AssertionError("World replacement mismatch: neutral=" + neutralError + ", source/depth=" + sourceError +
                    ", native parity=" + parityError);
        System.out.printf("PASS: all 3 quality levels, accessibility states and desktop/both eyes; neutral max %.8f, source/depth independence %.8f, native parity %.8f, transition residual vs endpoint crossfade %.8f%n",
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
        String initialization = fieldInitialization(source);
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
        contactSheet(target,3);
    }

    private static void contactSheet(Path target,int columns) throws Exception {
        int scale = 1, label = 24;
        int rows = (FRAMES.size()+columns-1)/columns;
        BufferedImage result = new BufferedImage(columns*WIDTH*scale, rows*(HEIGHT*scale+label), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(new Color(12, 14, 20));
        g.fillRect(0, 0, result.getWidth(), result.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        for (int index = 0; index < FRAMES.size(); index++) {
            Frame frame = FRAMES.get(index);
            BufferedImage image = image(frame.pixels);
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
        resize(640, 360);
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
        int query = glGenQueries();
        for(int[] size:new int[][]{{1280,720},{1920,1080}}) {
        resize(size[0],size[1]);fixture(0);
        Eye center = eye(0, 0.27f, -0.15f, 0, new Vector3f(), false);
        for (int quality = 0; quality <= 2; quality++) for(float intensity:new float[]{.25f,1}) {
            setup(p, center, 1, quality, 0, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
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
            System.out.printf("TIMING: %dx%d quality %d intensity %.2f single realm pass, 2 warm-up + 5 GPU samples: median %.3f ms (range %.3f-%.3f ms); excludes Minecraft and second VR eye%n",
                    WIDTH,HEIGHT,quality,intensity,milliseconds[2],milliseconds[0],milliseconds[4]);
        }
        }
        glDeleteQueries(query);
    }

    private static BufferedImage image(float[] pixels) {
        BufferedImage result=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
            int index=((HEIGHT-1-y)*WIDTH+x)*4;
            int red=Math.round(Math.max(0,Math.min(1,pixels[index]))*255);
            int green=Math.round(Math.max(0,Math.min(1,pixels[index+1]))*255);
            int blue=Math.round(Math.max(0,Math.min(1,pixels[index+2]))*255);
            result.setRGB(x,y,red<<16|green<<8|blue);
        }
        return result;
    }

    private static void evolution(int p,String source) {
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        for(int style=0;style<=6;style++) {
            setup(p,center,1,2,0,0,.37f);vec(p,"FractalC",1,.19f,.83f,style);
            float[] initial=draw();contrast(initial,"style "+style);
            double maximumStep=0,maximumLumaStep=0,maximumPoppingFraction=0,oneSecondChange=0;
            float[] previous=initial;
            for(int frame=1;frame<=120;frame++) {
                setup(p,center,1,2,0,frame/60f,.37f);vec(p,"FractalC",1,.19f,.83f,style);
                float[] current=draw();
                maximumStep=Math.max(maximumStep,difference(previous,current));
                maximumLumaStep=Math.max(maximumLumaStep,Math.abs(meanLuma(current)-meanLuma(previous)));
                int popping=0;
                for(int i=0;i<current.length;i+=4) {
                    double before=previous[i]*.2126+previous[i+1]*.7152+previous[i+2]*.0722;
                    double after=current[i]*.2126+current[i+1]*.7152+current[i+2]*.0722;
                    if(Math.abs(after-before)>.25)popping++;
                }
                maximumPoppingFraction=Math.max(maximumPoppingFraction,popping/(double)(WIDTH*HEIGHT));
                if(frame==60)oneSecondChange=difference(initial,current);
                previous=current;
            }
            double change=difference(initial,previous);
            if(change<.007||oneSecondChange<.005)throw new AssertionError("Style "+style+" barely evolves over1/2s: "+oneSecondChange+"/"+change);
            if(maximumStep>.055 || maximumLumaStep>.020 || maximumPoppingFraction>.35)
                throw new AssertionError("Style "+style+" abrupt60fps changes: mean="+maximumStep+", brightness="+maximumLumaStep);
            float[] frozen=draw();
            if(maximumDifference(previous,frozen)!=0)throw new AssertionError("Same supplied motion time changes world");
            setup(p,center,1,2,0,2,.37f);vec(p,"FractalC",.25f,.19f,.83f,style);
            double intensityChange=difference(previous,draw());
            if(intensityChange<.01)throw new AssertionError("Style "+style+" intensity only affects external opacity");
            System.out.printf("EVOLUTION: style %d, 1s/2s mean change %.6f/%.6f, max 60fps change %.6f, brightness step %.6f, >25%% pixel-luma jumps %.3f%%, full-blend 25/100%% intensity change %.6f; identical-time exact%n",
                    style,oneSecondChange,change,maximumStep,maximumLumaStep,maximumPoppingFraction*100,intensityChange);
        }
        String initialization=fieldInitialization(source);
        String probeSource=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestPlane;\n")+"\nvoid main(){\n"+initialization+"""
                    vec3 point=vec3((gl_FragCoord.xy/OutSize-.5)*12.0,TestPlane.x);
                    fragColor=vec4(realmField(point),0,1);
                }
                """;
        int field=program(probeSource,"production structural intensity and time field probe");
        double shapeIntensity=0,shapeTime=0,shapeOneSecond=0;
        double[] styleGeometry=new double[6];
        for(float plane:new float[]{-5,-3,1.5f}) {
            setup(field,center,1,2,0,0,.37f);vec(field,"FractalC",1,.19f,.83f,3);vec(field,"TestPlane",plane,0,0,0);
            float[] high=draw();
            vec(field,"FractalC",.25f,.19f,.83f,3);float[] low=draw();
            setup(field,center,1,2,0,3,.37f);vec(field,"FractalC",1,.19f,.83f,3);vec(field,"TestPlane",plane,0,0,0);
            float[] evolved=draw();
            setup(field,center,1,2,0,1,.37f);vec(field,"FractalC",1,.19f,.83f,3);vec(field,"TestPlane",plane,0,0,0);
            float[] oneSecond=draw();
            for(int i=0;i<high.length;i+=4){shapeIntensity+=Math.abs(high[i]-low[i]);shapeTime+=Math.abs(high[i]-evolved[i]);shapeOneSecond+=Math.abs(high[i]-oneSecond[i]);}
            float[] previous=null;
            for(int style=0;style<=6;style++) {
                setup(field,center,1,2,0,0,.37f);vec(field,"FractalC",1,.19f,.83f,style);vec(field,"TestPlane",plane,0,0,0);
                float[] current=draw();
                if(previous!=null)for(int i=0;i<current.length;i+=4)styleGeometry[style-1]+=Math.abs(previous[i]-current[i]);
                previous=current;
            }
        }
        shapeIntensity/=WIDTH*HEIGHT*3;shapeTime/=WIDTH*HEIGHT*3;shapeOneSecond/=WIDTH*HEIGHT*3;
        double minimumStyleGeometry=Arrays.stream(styleGeometry).min().orElseThrow()/(WIDTH*HEIGHT*3);
        if(shapeIntensity<.0001||shapeTime<.0001||shapeOneSecond<.0001||minimumStyleGeometry<.0001)
            throw new AssertionError("Fractal distance field lacks structural evolution: intensity="+shapeIntensity+",time="+shapeTime+",1s="+shapeOneSecond+",style="+minimumStyleGeometry);
        System.out.printf("PASS: actual distance field changes with intensity, time1s/3s, and style independently of opacity/palette; mean changes %.6f / %.6f / %.6f / minimumstyle %.6f across3 fixed world slices%n",shapeIntensity,shapeOneSecond,shapeTime,minimumStyleGeometry);
    }

    private static void finiteStyles(int desktop,int vr) {
        double minimumStyleChange=1;
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        float[] previous=null;
        for(int style=0;style<=6;style++) {
            setup(desktop,center,1,2,0,0,.37f);vec(desktop,"FractalC",1,.19f,.83f,style);
            float[] image=draw();contrast(image,"sessionstyle"+style);
            if(previous!=null)minimumStyleChange=Math.min(minimumStyleChange,difference(previous,image));
            previous=image;
            for(int p:new int[]{desktop,vr})for(int quality=0;quality<=2;quality++)for(float access:new float[]{0,1})
                    for(float intensity:new float[]{0,.25f,1})for(int side:new int[]{-1,1}) {
                setup(p,eye(side,.27f,-.15f,.32f,new Vector3f(),true),1,quality,access,15,.37f);
                vec(p,"FractalC",intensity,.19f,.83f,style);draw();
            }
        }
        if(minimumStyleChange<.005)throw new AssertionError("Adjacent session styles too similar: "+minimumStyleChange);
        setup(desktop,center,1,2,0,0,.37f);vec(desktop,"FractalC",1,.19f,.83f,3);float[] base=draw();
        double minimumSeedChange=1;
        for(int variant=0;variant<4;variant++) {
            setup(desktop,center,1,2,0,0,variant==0?.87f:.37f);
            if(variant==1)vec(desktop,"FractalB",.37f,.131f,.78f,.65f);
            vec(desktop,"FractalC",1,variant==2?.69f:.19f,variant==3?.33f:.83f,3);
            minimumSeedChange=Math.min(minimumSeedChange,difference(base,draw()));
        }
        if(minimumSeedChange<.01)throw new AssertionError("Additional trip seed parts barely change world: "+minimumSeedChange);
        System.out.printf("PASS: all7 styles x3qualities xaccessibility x3intensities xboth eyes/desktop finite; minimum adjacent style change %.6f, independent seed A/B/C/D change %.6f%n",minimumStyleChange,minimumSeedChange);
    }

    private static double meanLuma(float[] pixels) {
        double sum=0;for(int i=0;i<pixels.length;i+=4)sum+=pixels[i]*.2126+pixels[i+1]*.7152+pixels[i+2]*.0722;
        return sum/(pixels.length/4);
    }

    private static String fieldInitialization(String source) {
        int start=source.indexOf("    float reduced =",source.indexOf("void main()"));
        int end=source.indexOf("    vec3 origin = realmEyeOrigin();",start);
        if(start<0||end<0)throw new AssertionError("Production field initialization not found");
        return source.substring(start,end);
    }

    private static void beautyAndAnimation(int p,Path directory)throws Exception {
        resize(640,360);fixture(0);FRAMES.clear();
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        for(float intensity:new float[]{.25f,.5f,.75f,1}) {
            setup(p,center,1,2,0,3,.37f);vec(p,"FractalC",intensity,.19f,.83f,3);
            FRAMES.add(new Frame("Detail "+Math.round(intensity*100)+"% / blend 100%",draw()));
        }
        contactSheet(directory.resolve("dmt-fractal-intensity-comparison.png"),2);FRAMES.clear();
        for(int style=0;style<=6;style++) {
            // Different session seeds and artist palettes, same tracked camera.
            float seed=(.37f+style*.173f)%1f,seedC=(.19f+style*.29f)%1f,seedD=(.83f+style*.137f)%1f;
            for(float time:new float[]{0,1,3,8,20}) {
                setup(p,center,1,2,0,time,seed);vec(p,"FractalC",1,seedC,seedD,style);
                artistPalette(p,style);
                float[] pixels=draw();contrast(pixels,"beauty style"+style+" time"+time);
                FRAMES.add(new Frame("Style "+style+" / t="+time+"s / 100%",pixels));
                ImageIO.write(image(pixels),"png",directory.resolve("dmt-fractal-style"+style+"-t"+(int)time+".png").toFile());
            }
            contactSheet(directory.resolve("dmt-fractal-evolution-style"+style+".png"),5);FRAMES.clear();
        }
        for(int style:new int[]{0,3}) {
            Path target=directory.resolve("dmt-fractal-evolution-style"+style+".gif");
            final int chosenStyle=style;
            animation(target,96,frame->{
                setup(p,center,1,2,0,frame/12f,.37f);vec(p,"FractalC",1,.19f,.83f,chosenStyle);return draw();
            });
        }
        resize(1280,720);fixture(0);Eye large=eye(0,.27f,-.15f,0,new Vector3f(),false);
        for(int style:new int[]{0,2}) {
            setup(p,large,1,2,0,3,.37f);vec(p,"FractalC",1,.19f,.83f,style);artistPalette(p,style);
            float[] frame=draw();beautyStatistics(frame,style);
            ImageIO.write(image(frame),"png",directory.resolve("dmt-fractal-style"+style+"-720p.png").toFile());
        }
    }

    private static void beautyStatistics(float[] pixels,int style) {
        double[] luminance=new double[WIDTH*HEIGHT];int clipped=0,bright=0,dark=0;double edge=0;int edges=0;
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
            int i=(y*WIDTH+x)*4;double value=pixels[i]*.2126+pixels[i+1]*.7152+pixels[i+2]*.0722;
            luminance[y*WIDTH+x]=value;if(value>.9)bright++;if(value<.08)dark++;
            if(pixels[i]>.97&&pixels[i+1]>.97&&pixels[i+2]>.97)clipped++;
            if(x>WIDTH*.45&&x<WIDTH*.65&&y>HEIGHT*.35&&y<HEIGHT*.65) {
                int previous=i-4;double neighbor=pixels[previous]*.2126+pixels[previous+1]*.7152+pixels[previous+2]*.0722;
                edge+=Math.abs(value-neighbor);edges++;
            }
        }
        Arrays.sort(luminance);double count=WIDTH*HEIGHT;
        System.out.printf("BEAUTY: style %d 720p, luma p05/median/p95 %.3f/%.3f/%.3f, near-whiteclip %.3f%%, luma>.9 %.3f%%, shadows<.08 %.3f%%, central far-region neighbor delta %.5f%n",
                style,luminance[(int)(count*.05)],luminance[(int)(count*.5)],luminance[(int)(count*.95)],clipped/count*100,bright/count*100,dark/count*100,edge/Math.max(1,edges));
    }

    private static void animation(Path target,int frames,java.util.function.IntFunction<float[]> render)throws Exception {
            ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
            try(var output=ImageIO.createImageOutputStream(target.toFile())) {
                writer.setOutput(output);writer.prepareWriteSequence(null);
                for(int frame=0;frame<frames;frame++) {
                    BufferedImage picture=image(render.apply(frame));
                    var metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(picture),null);
                    String format=metadata.getNativeMetadataFormatName();
                    var tree=(IIOMetadataNode)metadata.getAsTree(format);
                    var control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                    control.setAttribute("delayTime","8");control.setAttribute("disposalMethod","none");
                    if(frame==0) {
                        var extensions=new IIOMetadataNode("ApplicationExtensions");var loop=new IIOMetadataNode("ApplicationExtension");
                        loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});
                        extensions.appendChild(loop);tree.appendChild(extensions);
                    }
                    metadata.setFromTree(format,tree);writer.writeToSequence(new IIOImage(picture,null,metadata),null);
                }
                writer.endWriteSequence();
            } finally {writer.dispose();}
            System.out.println("Animation: "+target.toAbsolutePath());
    }

    private static void transitions(int p,int vr,String source,Path directory)throws Exception {
        morphCoordinates(source);
        resize(128,96);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);Fixture room=roomFixture(center);
        setup(p,center,0,2,0,3,.37f);float[] start=draw();
        if(maximumDifference(start,room.colour)>1e-6)throw new AssertionError("Morph neutral endpoint changes synthetic room");
        setup(p,center,1,2,0,3,.37f);float[] complete=draw();
        setup(p,center,.35f,2,0,3,.37f);float[] middle=draw();
        float[] crossfade=new float[middle.length];for(int i=0;i<middle.length;i++)crossfade[i]=start[i]*.65f+complete[i]*.35f;
        double nonlinear=difference(middle,crossfade);
        float[] alternateDepth=new float[WIDTH*HEIGHT];Arrays.fill(alternateDepth,.5f);uploadFixture(null,alternateDepth);
        double depthResponse=difference(middle,draw());uploadFixture(room.colour,room.depth);
        if(nonlinear<.002||depthResponse<.0001)throw new AssertionError("Partial realm lacks depth morph: crossfade residual="+nonlinear+",depth response="+depthResponse);
        double maximumStep=0,maximumBrightnessStep=0;float[] previous=start;
        for(int frame=1;frame<=240;frame++) {
            setup(p,center,frame/240f,2,0,3+frame/60f,.37f);float[] current=draw();
            maximumStep=Math.max(maximumStep,difference(previous,current));
            maximumBrightnessStep=Math.max(maximumBrightnessStep,Math.abs(meanLuma(previous)-meanLuma(current)));previous=current;
        }
        if(maximumStep>.085||maximumBrightnessStep>.030)
            throw new AssertionError("Morph transition jumps abruptly: mean="+maximumStep+",brightness="+maximumBrightnessStep);
        for(int side:new int[]{-1,1}) {
            Eye eye=eye(side,.27f,-.15f,.32f,new Vector3f(),true);roomFixture(eye);
            setup(vr,eye,.35f,2,0,3,.37f);contrast(draw(),"per-eye partial world morph");
        }
        System.out.printf("PASS: checker-room morph differs from overlay crossfade %.6f, responds to actual depth %.6f; 240-frame60fps entry max image/luma step %.6f/%.6f; both canted eyes finite%n",nonlinear,depthResponse,maximumStep,maximumBrightnessStep);
        resize(640,360);center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);FRAMES.clear();
        for(float blend:new float[]{0,.1f,.25f,.5f,.75f,1}) {
            setup(p,center,blend,2,0,3,.37f);float[] frame=draw();FRAMES.add(new Frame("World morph "+Math.round(blend*100)+"%",frame));
            ImageIO.write(image(frame),"png",directory.resolve("dmt-fractal-morph-"+Math.round(blend*100)+".png").toFile());
        }
        contactSheet(directory.resolve("dmt-fractal-morph-strip.png"),3);FRAMES.clear();
        final Eye animationEye=center;
        animation(directory.resolve("dmt-fractal-world-morph.gif"),60,frame->{setup(p,animationEye,frame/59f,2,0,3+frame/12f,.37f);return draw();});
    }

    private static void morphCoordinates(String source) {
        String probeSource=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestUv;\nuniform vec4 TestSource;\nuniform vec4 TestTarget;\n")+
                "\nvoid main(){\n"+fieldInitialization(source)+"""
                    vec2 warped=morphSourceUv(TestUv.xy,TestSource.xyz,TestTarget.xyz,TestUv.z);
                    if(TestUv.w<.5)fragColor=vec4(warped,0,1);
                    else fragColor=vec4(realmRay(warped),1);
                }
                """;
        int p=program(probeSource,"production depth-interpolated source geometry reprojection probe");
        double neutralError=0,maximumWarp=0,totalWarp=0,maximumStereoResidual=0;
        int cases=0,warpedViews=0;
        for(int variant=0;variant<3;variant++)for(float distance:new float[]{1,3,6}) {
            Vector3f translation=variant==2?new Vector3f(.8f,-.25f,-.5f):new Vector3f();
            Eye[] eyes={eye(-1,.27f,-.15f,variant*.19f,translation,variant==2),eye(1,.27f,-.15f,variant*.19f,translation,variant==2)};
            Vector3f physical=eyes[0].rotation.transformDirection(new Vector3f(distance*.13f,-distance*.07f,-distance)).add(eyes[0].head);
            Vector3f targetPhysical=eyes[0].rotation.transformDirection(new Vector3f(distance*.17f,distance*.09f,-distance*1.5f)).add(eyes[0].head);
            Vector3f realm=new Vector3f(physical).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
            Vector3f target=new Vector3f(targetPhysical).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
            Vector3f[] rays=new Vector3f[2],origins=new Vector3f[2];
            for(int side=0;side<2;side++) {
                Eye eye=eyes[side];Vector4f clip=eye.vp.transform(new Vector4f(new Vector3f(physical).sub(eye.position),1));
                float u=clip.x/clip.w*.5f+.5f,v=clip.y/clip.w*.5f+.5f;
                setup(p,eye,1,2,0,3,.37f);vec(p,"TestSource",realm.x,realm.y,realm.z,1);vec(p,"TestTarget",target.x,target.y,target.z,1);
                vec(p,"TestUv",u,v,0,0);float[] neutral=draw();neutralError=Math.max(neutralError,Math.max(Math.abs(neutral[0]-u),Math.abs(neutral[1]-v)));
                vec(p,"TestUv",u,v,.35f,0);float[] warped=draw();double displacement=Math.hypot(warped[0]-u,warped[1]-v);
                totalWarp+=displacement;maximumWarp=Math.max(maximumWarp,displacement);if(displacement>.001)warpedViews++;
                vec(p,"TestUv",u,v,.35f,1);float[] ray=draw();rays[side]=new Vector3f(ray[0],ray[1],ray[2]);
                origins[side]=new Vector3f(eye.position).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
            }
            // Triangulate the two production warped rays. Consistent world-space
            // source deformation produces intersecting rays despite eye asymmetry.
            Vector3f w=new Vector3f(origins[0]).sub(origins[1]);
            double a=rays[0].dot(rays[0]),b=rays[0].dot(rays[1]),c=rays[1].dot(rays[1]);
            double d=rays[0].dot(w),e=rays[1].dot(w),denominator=a*c-b*b;
            if(denominator<1e-8)throw new AssertionError("Morph stereo rays unexpectedly parallel");
            float alongLeft=(float)((b*e-c*d)/denominator),alongRight=(float)((a*e-b*d)/denominator);
            Vector3f left=new Vector3f(rays[0]).mul(alongLeft).add(origins[0]),right=new Vector3f(rays[1]).mul(alongRight).add(origins[1]);
            maximumStereoResidual=Math.max(maximumStereoResidual,left.distance(right));
            if(alongLeft<=0||alongRight<=0)throw new AssertionError("Warped stereo source lies behind camera: variant="+variant+", physical distance="+distance+", alongleft/right="+alongLeft+"/"+alongRight+", residual="+left.distance(right));cases++;
        }
        if(neutralError>1e-6||maximumWarp<.01||warpedViews<cases||maximumStereoResidual>.001)
            throw new AssertionError("Morph source coordinate failure: neutral="+neutralError+",maxwarp="+maximumWarp+",warpedviews="+warpedViews+",stereo="+maximumStereoResidual);
        System.out.printf("PASS: %d actual source-deformation reprojection cases; neutral UV %.8f, %d/%d warped views, average/max UV displacement %.6f/%.6f, maximum shared-world stereo ray residual %.7f%n",cases,neutralError,warpedViews,cases*2,totalWarp/(cases*2),maximumWarp,maximumStereoResidual);
    }

    private record Eye(Vector3f position, Vector3f head, Matrix4f vp, Matrix4f inverse, Matrix4f rotation) {}
    private record Frame(String label, float[] pixels) {}
    private record Fixture(float[] colour,float[] depth) {}
}
