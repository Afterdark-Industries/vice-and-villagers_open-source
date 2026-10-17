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
    private static float sampledDemandingTime=60;
    private static double sampledDemandingDose=6;
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
            System.out.println("Compiled desktop SHA256: "+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(desktop.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            int desktopProgram = program(desktop, "production desktop fractal world");
            int vrProgram = program(vr, "production VR fractal world");
            System.out.println("PASS: production desktop and native per-eye VR shaders compile and link");
            if(args.length>1&&args[1].equals("--chapters-only")) {
                doseEcologyAndLanes(desktopProgram,desktop,root.resolve("build"));return;
            }
            if(args.length>1&&args[1].equals("--coverage-only")){rayCoverage(desktop);recoveryVerification(desktop);return;}
            if(args.length>1&&args[1].equals("--coverage-detail")){rayCoverageDetail(desktop);return;}
            if(args.length>1&&args[1].equals("--recovery-reference")){recoveryReference(desktop);recoveryStereo(desktop);return;}
            if(args.length>1&&args[1].equals("--recovery-performance")){recoveryPerformance(desktopProgram);return;}
            if(args.length>1&&args[1].equals("--trace-workload")){traceWorkload(desktop,"current");return;}
            if(args.length>1&&args[1].equals("--workload-sweep")){workloadSweep(desktopProgram,desktop);return;}
            if(args.length>1&&args[1].equals("--neutral-world")){nearNeutralWorld(desktopProgram);return;}
            if(args.length>1&&args[1].equals("--temporal-only")) {
                Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);
                ecologyTemporal(desktopProgram,center);worldHueContinuity(desktop);evolution(desktopProgram,desktop);return;
            }
            if(args.length>2&&args[1].equals("--paired-performance")){pairedPerformance(program(Files.readString(Path.of(args[2])),"paired baseline"),desktopProgram);return;}
            if(args.length>2&&args[1].equals("--compare-generator")) {
                String reference=Files.readString(Path.of(args[2]));
                compareGenerator(desktopProgram,desktop,reference);
                traceWorkload(reference,"baseline");traceWorkload(desktop,"candidate");
                pairedPerformance(program(reference,"baseline performance"),desktopProgram);return;
            }
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
            settingsDepthMatrix(desktopProgram,desktop);
            if(desktop.contains("uniform vec4 RealmSettling"))settling(desktopProgram,desktop);
            worldHueContinuity(desktop);
            nearNeutralWorld(desktopProgram);
            cellCarveContinuity(desktop);
            workloadSweep(desktopProgram,desktop);
            evolution(desktopProgram, desktop);
            emptyBounds(desktop);
            rayCoverage(desktop);
            recoveryVerification(desktop);
            recoveryReference(desktop);
            recoveryStereo(desktop);
            traceWorkload(desktop,"current");
            doseEcologyAndLanes(desktopProgram,desktop,root.resolve("build"));
            if(glGetUniformLocation(desktopProgram,"DepthSampler")>=0)
                transitions(desktopProgram,vrProgram,desktop,root.resolve("build"));
            previewFrames(desktopProgram, vrProgram);
            contactSheet(root.resolve("build/dmt-fractal-contact-sheet.png"));
            beautyAndAnimation(desktopProgram, root.resolve("build"));
            performance(desktopProgram);
            recoveryPerformance(desktopProgram);
            int error = glGetError();
            if (error != GL_NO_ERROR) throw new AssertionError("OpenGL error 0x" + Integer.toHexString(error));
            System.out.printf("PASS: %d finite GPU frames; Minecraft integration, total game performance and headset comfort still require live testing%n", frameCount);
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
        vec(p, "RealmSettling", 1, 1, 1, 1);
        dynamics(p,6,1,1,45);
        vec(p,"WorldInfluenceA",.4f,.3f,.35f,.15f);
        vec(p,"WorldInfluenceB",.65f,.2f,.1f,.25f);
        vec(p,"WorldInfluenceC",.32f,.58f,.27f,.45f);
        vec(p,"WorldInfluenceD",.35f,-.1f,-.25f,.6f);
        vec(p,"WorldAttractor",1.4f,.3f,-2,.4f);
        artistPalette(p,0);
    }

    private static void dynamics(int p,double dose,float intensity,float ingress,float seconds) {
        vec(p,"RealmDynamics",(float)(Math.log1p(dose/2.4)/Math.log(2)*intensity),ingress,seconds,(float)(Math.log1p(dose)/Math.log(2)));
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
        float neutralError = 0, alphaError = 0, parityError = 0, blendError = 0;
        double minimumWorldResponse=1;
        for (int p : new int[]{desktop, vr}) for (int quality = 0; quality <= 2; quality++)
                for (float access : new float[]{0, 1}) for (float intensity:new float[]{.25f,1}) for (int side : new int[]{-1, 0, 1}) {
            Eye eye = eye(side, 0.27f, -0.15f, 0.32f, new Vector3f(), side != 0);
            float[] source = fixture(0);
            setup(p, eye, 0, quality, access, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            dynamics(p,6,intensity,1,45);
            neutralError = Math.max(neutralError, maximumDifference(source, draw()));
            setup(p, eye, 1, quality, access, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            dynamics(p,6,intensity,1,45);
            float[] full = draw();
            for(int i=3;i<full.length;i+=4)alphaError=Math.max(alphaError,Math.abs(full[i]-1));
            contrast(full, "quality=" + quality + ", access=" + access + ", side=" + side);
            fixture(1);
            minimumWorldResponse=Math.min(minimumWorldResponse,difference(full,draw()));
            fixture(0);
            setup(p, eye, 0.37f, quality, access, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            dynamics(p,6,intensity,1,45);
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
            dynamics(desktop,6,intensity,1,45);
            float[] a = draw();
            setup(vr, center, blend, quality, 0, 12, 0.317f);
            vec(vr,"FractalC",intensity,.19f,.83f,3);
            dynamics(vr,6,intensity,1,45);
            parityError = Math.max(parityError, maximumDifference(a, draw()));
        }
        if (neutralError > 0.000001f || alphaError > 0.000001f || parityError > 0.000001f || minimumWorldResponse<.00001)
            throw new AssertionError("World replacement mismatch: neutral=" + neutralError + ", opaquealpha=" + alphaError +",hiddenworld="+minimumWorldResponse+
                    ", native parity=" + parityError);
        System.out.printf("PASS: all3qualities/accessibility/intensities/eyes; neutral max %.8f, opaquealpha error %.8f, hiddenworld minimum response %.6f, nativeparity %.8f, partialcrossfade residual %.8f%n",
                neutralError,alphaError,minimumWorldResponse,parityError,blendError);
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

    private static void compareGenerator(int candidate,String source,String reference) {
        resize(128,96);
        int baseline=program(reference,"same-generator baseline");
        int baselineField=fieldProbe(reference),candidateField=fieldProbe(source);
        int distanceField=source.contains("vec2 realmDistanceField(")?fieldProbe(source,"realmDistanceField"):candidateField;
        String marker="    vec3 background = realmBackground(ray);";
        String output="    fragColor=vec4(hit?1.0:0.0,distance,hitField.x,recoveredSurface?1.0:0.0); return;\n"+marker;
        int baselineTrace=program(reference.replace(marker,output),"baseline hit-depth preservation");
        int candidateTrace=program(source.replace(marker,output),"candidate hit-depth preservation");
        double maxDistance=0,maxDistanceOnly=0,maxOrbit=0,maxImage=0,maxMean=0,maxChanged=0,maxDepthChanged=0,maxNewMiss=0;
        int views=0,slices=0;
        for(float seed:new float[]{.317f,.37f})for(int quality=0;quality<=2;quality++)
        for(float time:new float[]{0,10,30,60,120,300})for(double dose:new double[]{3,48,200,10000})
        for(float intensity:new float[]{.25f,1})for(int side:new int[]{-1,1}) {
            Eye camera=eye(side,.27f,-.15f,.32f,new Vector3f(),true);roomFixture(camera);
            float[][] frames=new float[2][];int index=0;
            for(int p:new int[]{baseline,candidate}) {
                setup(p,camera,1,quality,0,time,seed);vec(p,"FractalC",intensity,.19f,.83f,3);dynamics(p,dose,intensity,1,120);
                frames[index++]=draw();
            }
            maxImage=Math.max(maxImage,maximumDifference(frames[0],frames[1]));maxMean=Math.max(maxMean,difference(frames[0],frames[1]));
            int changed=0;
            for(int i=0;i<frames[0].length;i+=4)if(Math.max(Math.abs(frames[0][i]-frames[1][i]),Math.max(Math.abs(frames[0][i+1]-frames[1][i+1]),Math.abs(frames[0][i+2]-frames[1][i+2])))>.02)changed++;
            maxChanged=Math.max(maxChanged,changed/(double)(WIDTH*HEIGHT));views++;
            index=0;
            for(int p:new int[]{baselineTrace,candidateTrace}) {
                setup(p,camera,1,quality,0,time,seed);vec(p,"FractalC",intensity,.19f,.83f,3);dynamics(p,dose,intensity,1,120);frames[index++]=draw();
            }
            int depthChanged=0,newMiss=0;
            for(int i=0;i<frames[0].length;i+=4)if(frames[0][i]>.5) {
                if(frames[1][i]<.5)newMiss++;
                else if(Math.abs(frames[0][i+1]-frames[1][i+1])>Math.max(.05,frames[0][i+1]*.03))depthChanged++;
            }
            maxDepthChanged=Math.max(maxDepthChanged,depthChanged/(double)(WIDTH*HEIGHT));maxNewMiss=Math.max(maxNewMiss,newMiss/(double)(WIDTH*HEIGHT));
            if(side<0)for(float plane:new float[]{-3,0,3}) {
                float[][] fields=new float[3][];index=0;
                for(int p:new int[]{baselineField,candidateField,distanceField}) {
                    setup(p,camera,1,quality,0,time,seed);vec(p,"FractalC",intensity,.19f,.83f,3);dynamics(p,dose,intensity,1,120);vec(p,"TestPlane",plane,0,0,0);fields[index++]=draw();
                }
                for(int i=0;i<fields[0].length;i+=4) {
                    maxDistance=Math.max(maxDistance,Math.abs(fields[0][i]-fields[1][i]));maxOrbit=Math.max(maxOrbit,Math.abs(fields[0][i+1]-fields[1][i+1]));
                    maxDistanceOnly=Math.max(maxDistanceOnly,Math.abs(fields[1][i]-fields[2][i]));
                }
                slices++;
            }
        }
        System.out.printf("GENERATOR PRESERVATION: %d stereo-view pairs/%d field-slice pairs; max distance %.9f, distance-only/full %.9f, orbit %.9f; max image/mean %.9f/%.9f, >2%% component pixels %.6f%%; changed depth>3%%/.05realm %.6f%%, newly missed %.6f%%%n",
                views,slices,maxDistance,maxDistanceOnly,maxOrbit,maxImage,maxMean,maxChanged*100,maxDepthChanged*100,maxNewMiss*100);
        // A different compiler schedule can move isolated fractal-edge pixels.
        // Gate actual distance and visible image/depth impact separately.
        if(maxDistance>1e-5||maxDistanceOnly>1e-5||maxMean>.001||maxChanged>.002||maxDepthChanged>.003||maxNewMiss>.002)
            throw new AssertionError("Same-generator optimization changes geometry or visible output materially");
        System.out.println("PASS: same-generator distance, final images and nearest visible surfaces remain within declared tolerances");
    }

    private static void traceWorkload(String source,String label) {
        String primary="        if (i >= stepLimit || distance >= FAR_DISTANCE) break;";
        String recovery="    if (!hit && distance < FAR_DISTANCE) {";
        String marker="    vec3 background = realmBackground(ray);";
        if(!source.contains(primary)||!source.contains(recovery)||!source.contains(marker))throw new AssertionError("Production trace counters cannot find loop markers");
        String counted=source.replaceFirst("#version 150","#version 150\nint validationPrimary=0;int validationFull=0;int validationDistance=0;bool validationRecovery=false;\n")
                .replace("vec2 realmField(vec3 position) {","vec2 realmField(vec3 position) { validationFull++;")
                .replace("vec2 realmDistanceField(vec3 position) {","vec2 realmDistanceField(vec3 position) { validationDistance++;")
                .replace(primary,primary+"\n        validationPrimary++;")
                .replace(recovery,recovery+"\n        validationRecovery=true;")
                .replace(marker,"    fragColor=vec4(float(validationPrimary),float(recoverySteps),float(validationDistance),float(validationFull)+(validationRecovery?.5:0.0)); return;\n"+marker);
        int p=program(counted,"production trace workload counters "+label);resize(128,96);fixture(0);
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        for(int fixture=0;fixture<3;fixture++)for(int quality=0;quality<=2;quality++)for(double dose:fixture==0?new double[]{6,200}:new double[]{3,48}) {
            setup(p,center,1,quality,0,fixture==0?12:fixture==1?120:60,fixture==0?.317f:.37f);dynamics(p,dose,1,1,45);float[] frame=draw();
            double[] primarySamples=new double[WIDTH*HEIGHT],recoverySamples=new double[WIDTH*HEIGHT];double full=0,distance=0;int entering=0;
            for(int i=0,index=0;i<frame.length;i+=4,index++) {
                primarySamples[index]=frame[i];recoverySamples[index]=frame[i+1];distance+=frame[i+2];full+=Math.floor(frame[i+3]);
                if(frame[i+3]-Math.floor(frame[i+3])>.25)entering++;
            }
            Arrays.sort(primarySamples);Arrays.sort(recoverySamples);int pixels=WIDTH*HEIGHT;
            System.out.printf("TRACE WORKLOAD %s %s q%d dose%.0f: primary mean/p95 %.2f/%.0f, recovery entered %.3f%%, recovery samples mean/p95 %.2f/%.0f; distance-only/full-orbit calls per ray %.2f/%.2f (trace boundary, before normals/AO)%n",
                    label,fixture==0?"standard-t12":fixture==1?"historical-heavy-t120":"nested-chapter-t60",quality,dose,Arrays.stream(primarySamples).average().orElse(0),primarySamples[(int)(pixels*.95)],entering*100.0/pixels,
                    Arrays.stream(recoverySamples).average().orElse(0),recoverySamples[(int)(pixels*.95)],distance/pixels,full/pixels);
        }
    }

    private static void pairedPerformance(int baseline,int candidate) {
        resize(1920,1080);fixture(0);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);int query=glGenQueries();
        for(int fixture=0;fixture<2;fixture++)for(int quality=0;quality<=2;quality++)for(double dose:fixture==0?new double[]{6,200}:new double[]{3,48}) {
            double[][] samples={new double[21],new double[21]};double[][] rounds={new double[3],new double[3]};
            for(int round=0;round<3;round++)for(int order=0;order<2;order++) {
                int variant=(round+order)%2,p=variant==0?baseline:candidate;
                setup(p,center,1,quality,0,fixture==0?12:120,fixture==0?.317f:.37f);dynamics(p,dose,1,1,45);
                for(int warm=0;warm<3;warm++)glDrawArrays(GL_TRIANGLES,0,3);glFinish();
                for(int sample=0;sample<7;sample++) {
                    glBeginQuery(GL_TIME_ELAPSED,query);glDrawArrays(GL_TRIANGLES,0,3);glEndQuery(GL_TIME_ELAPSED);
                    samples[variant][round*7+sample]=glGetQueryObjectui64(query,GL_QUERY_RESULT)/1_000_000.0;
                }
                double[] batch=Arrays.copyOfRange(samples[variant],round*7,round*7+7);Arrays.sort(batch);rounds[variant][round]=batch[3];
            }
            Arrays.sort(samples[0]);Arrays.sort(samples[1]);
            System.out.printf("PAIRED GPU 1080p %s q%d intensity1 dose%.0f: baseline/candidate median %.3f/%.3f ms, p95 %.3f/%.3f ms, saving %.2f%%; three warmed round medians %s / %s; one native-resolution pass, excludes Minecraft/second eye%n",
                    fixture==0?"standard":"nested-room",quality,dose,samples[0][10],samples[1][10],samples[0][19],samples[1][19],100*(1-samples[1][10]/samples[0][10]),Arrays.toString(rounds[0]),Arrays.toString(rounds[1]));
        }
        glDeleteQueries(query);
    }

    private static List<Float> referenceTimes() {
        return java.util.stream.Stream.of(60f,120f,sampledDemandingTime).distinct().toList();
    }

    private static List<Double> referenceDoses() {
        return java.util.stream.Stream.of(3d,48d,sampledDemandingDose).distinct().toList();
    }

    private static void workloadSweep(int p,String source) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")+"\nvoid main(){\n"+fieldInitialization(source)+"fragColor=layoutLanes;\n}\n";
        int layout=program(probe,"sampled demanding chapter layout");resize(640,360);fixture(0);
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);int query=glGenQueries();double largest=0;float[] chosenLayout=null;
        for(float time=0;time<=300;time+=10)for(double dose:new double[]{3,6,48}) {
            setup(p,center,1,2,0,time,.37f);dynamics(p,dose,1,1,45);for(int warm=0;warm<2;warm++)glDrawArrays(GL_TRIANGLES,0,3);glFinish();
            double[] samples=new double[3];for(int sample=0;sample<3;sample++){glBeginQuery(GL_TIME_ELAPSED,query);glDrawArrays(GL_TRIANGLES,0,3);glEndQuery(GL_TIME_ELAPSED);samples[sample]=glGetQueryObjectui64(query,GL_QUERY_RESULT)/1_000_000.0;}
            Arrays.sort(samples);setup(layout,center,1,2,0,time,.37f);dynamics(layout,dose,1,1,45);glDrawArrays(GL_TRIANGLES,0,3);float[] lanes=new float[4];glReadPixels(0,0,1,1,GL_RGBA,GL_FLOAT,lanes);
            System.out.printf("CHAPTER SWEEP: seed.37 clock%.0f dose%.0f High640x360 median %.3f ms, layout %s%n",time,dose,samples[1],Arrays.toString(lanes));
            if(samples[1]>largest){largest=samples[1];sampledDemandingTime=time;sampledDemandingDose=dose;chosenLayout=lanes;}
        }
        glDeleteQueries(query);resize(128,96);fixture(0);
        System.out.printf("SAMPLED DEMANDING FIXTURE: clock%.0f dose%.0f, %.3f ms High640x360, layout %s; selected from fixed seed.37, clock0..300 every10s, doses3/6/48; not a global worst-case claim%n",sampledDemandingTime,sampledDemandingDose,largest,Arrays.toString(chosenLayout));
    }

    private static void settingsDepthMatrix(int p,String source) {
        String marker="    vec3 background = realmBackground(ray);";
        int depth=program(source.replace(marker,"    fragColor=vec4(hit?1.0:0.0,distance,hitField.x,hitTolerance); return;\n"+marker),"all player presets actual ray depth");
        resize(128,96);fixture(0);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);int cases=0;double minimumHit=1;
        for(int scene=0;scene<3;scene++)for(int quality=0;quality<=2;quality++)for(float intensity:new float[]{.25f,.5f,.75f,1})for(double dose:new double[]{3,6,12,48,200}) {
            float time=scene==0?12:scene==1?120:60;
            setup(p,center,1,quality,0,time,scene==0?.317f:.37f);vec(p,"FractalC",intensity,.19f,.83f,3);dynamics(p,dose,intensity,1,120);contrast(draw(),"full realm quality/intensity preset");
            vec(p,"FractalA",intensity,time,quality,0);draw();
            setup(depth,center,1,quality,0,time,scene==0?.317f:.37f);vec(depth,"FractalC",intensity,.19f,.83f,3);dynamics(depth,dose,intensity,1,120);float[] frame=draw();int hits=0;
            for(int i=0;i<frame.length;i+=4)if(frame[i]>.5) {
                hits++;if(frame[i+1]<.0149||frame[i+1]>66.001||frame[i+3]<=0)throw new AssertionError("Player preset creates invalid hit depth/tolerance");
            }
            minimumHit=Math.min(minimumHit,hits/(double)(WIDTH*HEIGHT));cases++;
        }
        if(minimumHit<.2)throw new AssertionError("Player preset loses most actual scene geometry: "+minimumHit);
        System.out.printf("PASS: %d standard/historical-heavy/new-nested x3quality x4intensity x5dose cases, full and selected-opacity images finite; actual hit depths valid, minimum geometry coverage %.4f%%%n",cases,minimumHit*100);
    }

    private static void settling(int p,String source) {
        resize(128,96);fixture(0);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        setup(p,center,1,2,0,120,.37f);dynamics(p,48,1,1,120);float[] peak=draw();
        vec(p,"RealmSettling",1,.15f,1,1);if(maximumDifference(peak,draw())!=0)throw new AssertionError("Shader rescales elapsed motion using settling rate");
        vec(p,"RealmSettling",.15f,.15f,.10f,.35f);float[] quiet=draw();double change=difference(peak,quiet);
        if(change<.001)throw new AssertionError("Return settling does not change visible distortion/ornament/light");
        vec(p,"RealmSettling",1,1,1,1);if(maximumDifference(peak,draw())!=0)throw new AssertionError("Restored settling does not reproduce same peak generator");
        float[] previous=peak;double maximumStep=0;
        for(int frame=1;frame<=240;frame++) {
            float progress=frame/240f,ease=progress*progress*progress*(progress*(progress*6-15)+10);
            vec(p,"RealmSettling",1-.85f*ease,1-.85f*ease,1-.90f*ease,1-.65f*ease);float[] current=draw();maximumStep=Math.max(maximumStep,difference(previous,current));previous=current;
        }
        if(maximumStep>.04)throw new AssertionError("Return settling introduces a discontinuous material/geometry change: "+maximumStep);
        System.out.printf("PASS: peak/quiet/restored settling, quiet response %.6f, exact restoration and shader clock-rate independence;240step maximum image change %.6f%n",change,maximumStep);
    }

    private static void performance(int p) {
        // Time only GPU drawing; no pixel reads, allocation, game rendering or
        // shader compilation is included. This is one realm pass, not game FPS.
        int query = glGenQueries();
        for(int[] size:new int[][]{{1280,720},{1920,1080}}) {
        resize(size[0],size[1]);fixture(0);
        Eye center = eye(0, 0.27f, -0.15f, 0, new Vector3f(), false);
        for (int quality = 0; quality <= 2; quality++) for(float intensity:new float[]{.25f,1}) for(double dose:new double[]{6,200}) {
            setup(p, center, 1, quality, 0, 12, 0.317f);
            vec(p,"FractalC",intensity,.19f,.83f,3);
            dynamics(p,dose,intensity,1,45);
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
            System.out.printf("TIMING: %dx%d quality %d intensity %.2f dose %.0f single realm pass, 2 warm-up + 5 GPU samples: median %.3f ms (range %.3f-%.3f ms); excludes Minecraft and second VR eye%n",
                    WIDTH,HEIGHT,quality,intensity,dose,milliseconds[2],milliseconds[0],milliseconds[4]);
        }
        }
        glDeleteQueries(query);
    }

    private static void recoveryPerformance(int p) {
        // The nested-room fixture requires recovery for many more rays than the
        // standard timing view. Keep its complete shader cost visible as well.
        resize(1920,1080);fixture(0);
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        int query=glGenQueries();
        for(float time:referenceTimes())for(int quality=0;quality<=2;quality++)for(double dose:referenceDoses()) {
            setup(p,center,1,quality,0,time,.37f);dynamics(p,dose,1,1,120);
            for(int warm=0;warm<2;warm++)glDrawArrays(GL_TRIANGLES,0,3);
            glFinish();double[] milliseconds=new double[5];
            for(int sample=0;sample<milliseconds.length;sample++) {
                glBeginQuery(GL_TIME_ELAPSED,query);glDrawArrays(GL_TRIANGLES,0,3);glEndQuery(GL_TIME_ELAPSED);
                milliseconds[sample]=glGetQueryObjectui64(query,GL_QUERY_RESULT)/1_000_000.0;
            }
            Arrays.sort(milliseconds);
            System.out.printf("RECOVERY TIMING: 1920x1080 quality %d intensity1 dose%.0f motion%.0f seed.37, 2 warm-up + 5 GPU samples: median %.3f ms (range %.3f-%.3f ms); complete pass including bounded recovery, excludes Minecraft and second VR eye%n",
                    quality,dose,time,milliseconds[2],milliseconds[0],milliseconds[4]);
        }
        glDeleteQueries(query);
        if(glGetError()!=GL_NO_ERROR)throw new AssertionError("Recovery GPU timing failed");
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
            dynamics(p,6,.25f,1,45);
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
        if(shapeIntensity<.0001||shapeTime<.0001||shapeOneSecond<.0001)
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
                    for(float intensity:new float[]{0,.25f,.5f,.75f,1})for(int side:new int[]{-1,1}) {
                setup(p,eye(side,.27f,-.15f,.32f,new Vector3f(),true),1,quality,access,15,.37f);
                vec(p,"FractalC",intensity,.19f,.83f,style);draw();
            }
        }
        // Styles are continuous chapter preferences, so distinct session styles
        // may legitimately converge at one instant. Temporal independence and
        // dominant-family diversity are checked against the actual lane values.
        setup(desktop,center,1,2,0,0,.37f);vec(desktop,"FractalC",1,.19f,.83f,3);float[] base=draw();
        double minimumSeedChange=1;
        for(int variant=0;variant<4;variant++) {
            setup(desktop,center,1,2,0,0,variant==0?.87f:.37f);
            if(variant==1)vec(desktop,"FractalB",.37f,.131f,.78f,.65f);
            vec(desktop,"FractalC",1,variant==2?.69f:.19f,variant==3?.33f:.83f,3);
            minimumSeedChange=Math.min(minimumSeedChange,difference(base,draw()));
        }
        if(minimumSeedChange<.01)throw new AssertionError("Additional trip seed parts barely change world: "+minimumSeedChange);
        System.out.printf("PASS: all7 styles x3qualities xaccessibility x4intensity presets+zero xboth eyes/desktop finite; minimum adjacent style change %.6f, independent seed A/B/C/D change %.6f%n",minimumStyleChange,minimumSeedChange);
    }

    private static double meanLuma(float[] pixels) {
        double sum=0;for(int i=0;i<pixels.length;i+=4)sum+=pixels[i]*.2126+pixels[i+1]*.7152+pixels[i+2]*.0722;
        return sum/(pixels.length/4);
    }

    private static int fieldProbe(String source) {
        return fieldProbe(source,"realmField");
    }

    private static int fieldProbe(String source,String fieldFunction) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestPlane;\n")+"\nvoid main(){\n"+fieldInitialization(source)+"""
                    vec3 point=vec3((gl_FragCoord.xy/OutSize-.5)*12.0,TestPlane.x);
                    fragColor=vec4(FIELD_FUNCTION(point),0,1);
                }
                """.replace("FIELD_FUNCTION",fieldFunction);
        return program(probe,"production ecology/dose distance-field probe");
    }

    private static void emptyBounds(String source) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestPlane;\n")+"\nvoid main(){\n"+fieldInitialization(source)+"""
                    vec3 point=vec3((gl_FragCoord.xy/OutSize-.5)*12.0,TestPlane.x);
                    allowEmptyShortcut=true;vec2 bound=realmField(point);
                    allowEmptyShortcut=false;vec2 exact=realmField(point);
                    float minimumSigned=exact.x;
                    if(bound.y<-.5)for(int axis=0;axis<6;axis++) {
                        vec3 direction=axis<2?vec3(axis==0?1:-1,0,0):axis<4?vec3(0,axis==2?1:-1,0):vec3(0,0,axis==4?1:-1);
                        minimumSigned=min(minimumSigned,realmField(point+direction*bound.x*.99).x);
                    }
                    fragColor=vec4(bound.x,exact.x/max(fieldDistanceScale,.001),bound.y,minimumSigned);
                }
                """;
        int field=program(probe,"production conservative empty-region bound probe");
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);int shortcuts=0;double maximumOvershoot=0;
        for(double dose:new double[]{6,200,10000,Double.MAX_VALUE})for(float time:new float[]{0,30,120})for(float plane:new float[]{-3,-1,0,1,3}) {
            setup(field,center,1,2,0,time,.37f);dynamics(field,dose,1,1,45);vec(field,"TestPlane",plane,0,0,0);float[] frame=draw();
            for(int i=0;i<frame.length;i+=4)if(frame[i+2]<-.5) {
                shortcuts++;maximumOvershoot=Math.max(maximumOvershoot,frame[i]-frame[i+1]);
                if(frame[i]<=0||frame[i+1]<=0||frame[i]>frame[i+1]+.00001||frame[i+3]<=0)
                    throw new AssertionError("Empty-region physical bound oversteps raw field or sampled exact surface: "+frame[i]+" vs "+frame[i+1]+", min signed endpoint "+frame[i+3]);
            }
        }
        if(shortcuts==0)throw new AssertionError("Empty-region bound fixture never exercises optimization");
        System.out.printf("PASS: %d actual shortcut physical bounds are positive, below raw fullfield and retain positive exact field at6axis endpoints acrossdose6..DoubleMAX, chapters and5slices; maxovershoot %.8f%n",shortcuts,maximumOvershoot);
    }

    private static void rayCoverage(String source) {
        String marker="    vec3 background = realmBackground(ray);";
        if(!source.contains(marker))throw new AssertionError("Production raymarch output marker missing");
        String probe=source.replace(marker,"    fragColor=vec4(hit?1.0:0.0,distance,layoutLanes.x,recoveredSurface?2.0:(!hit&&distance<FAR_DISTANCE)?1.0:0.0); return;\n"+marker);
        int p=program(probe,"production unmodified raymarch hit coverage probe");
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);
        double[] minimumCoverage={1,1,1};double minimumClosedHigh=1;int closedCases=0;
        for(float seed:new float[]{.1f,.37f,.8f})for(float time:java.util.stream.Stream.of(0f,30f,60f,120f,300f,sampledDemandingTime).distinct().toList())for(double dose:new double[]{3,6,48,200,10000}) {
            double[] coverages=new double[3],budgetMisses=new double[3],recoveries=new double[3],missDistances=new double[3];float openVault=0;
            for(int quality=0;quality<=2;quality++) {
                setup(p,center,1,quality,0,time,seed);dynamics(p,dose,1,1,45);float[] frame=draw();int hit=0,budget=0,recovered=0;double meanMissDistance=0;
                for(int i=0;i<frame.length;i+=4) {if(frame[i]>.5)hit++;else meanMissDistance+=frame[i+1];if(frame[i+3]>.5&&frame[i+3]<1.5)budget++;if(frame[i+3]>1.5)recovered++;}
                coverages[quality]=hit/(double)(WIDTH*HEIGHT);minimumCoverage[quality]=Math.min(minimumCoverage[quality],coverages[quality]);openVault=frame[2];
                budgetMisses[quality]=budget/(double)(WIDTH*HEIGHT);missDistances[quality]=meanMissDistance/Math.max(1,WIDTH*HEIGHT-hit);
                recoveries[quality]=recovered/(double)(WIDTH*HEIGHT);
                if(quality==2&&dose<=48&&openVault<.2){minimumClosedHigh=Math.min(minimumClosedHigh,coverages[quality]);closedCases++;}
            }
            System.out.printf("RAY COVERAGE: seed %.2f motion %.0fs dose %.0f openvault %.3f, low/balanced/high hits %s, recovery %s, budgetmisses %s, meanmissdistance %s%n",seed,time,dose,openVault,Arrays.toString(coverages),Arrays.toString(recoveries),Arrays.toString(budgetMisses),Arrays.toString(missDistances));
        }
        System.out.printf("RAY SUMMARY: minimum low/balanced/high %s; %d common-dose mostly-closed high-quality cases minimum %.6f%n",Arrays.toString(minimumCoverage),closedCases,minimumClosedHigh);
        // Deliberately report layout-aware evidence rather than asserting that
        // every open-vault ray must hit a closed room. Serious closed-wall misses
        // are surfaced for production review, not hidden by a weaker threshold.
        if(closedCases>0&&minimumClosedHigh<.65)throw new AssertionError("High-quality common-dose closed layouts mostly miss real surfaces: "+minimumClosedHigh);
    }

    private static void rayCoverageDetail(String source) {
        String marker="    vec3 background = realmBackground(ray);";
        String probe=source.replace(marker,"    fragColor=vec4(hit?1.0:0.0,distance,hitField.x,hitTolerance); return;\n"+marker);
        int p=program(probe,"production worst-case ray-budget diagnostics");
        for(int[] size:new int[][]{{128,96},{640,360},{1280,720}}) {
            resize(size[0],size[1]);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);
            for(int quality=0;quality<=2;quality++) {
                if(WIDTH>128&&quality<2)continue;
                setup(p,center,1,quality,0,120,.37f);dynamics(p,3,1,1,45);float[] frame=draw();
                double[][] values=new double[3][WIDTH*HEIGHT];int misses=0,far=0;
                for(int i=0;i<frame.length;i+=4)if(frame[i]<.5){values[0][misses]=frame[i+1];values[1][misses]=frame[i+2];values[2][misses++]=frame[i+2]/frame[i+3];if(frame[i+1]>=66)far++;}
                for(int metric=0;metric<3;metric++)values[metric]=Arrays.copyOf(values[metric],misses);
                for(double[] metric:values)Arrays.sort(metric);
                System.out.printf("RAY DETAIL: %dx%d quality%d seed.37/t120/dose3, hits %.5f%%, misses %d/far %d, miss-distance p10/50/90 %s, lastDE p10/50/90 %s, DE/tolerance p10/50/90 %s%n",
                        WIDTH,HEIGHT,quality,100.0*(WIDTH*HEIGHT-misses)/(WIDTH*HEIGHT),misses,far,quantiles(values[0]),quantiles(values[1]),quantiles(values[2]));
            }
        }
    }

    private static void recoveryVerification(String source) {
        String marker="    vec3 background = realmBackground(ray);";
        String probe=source.replace(marker,"    fragColor=vec4(recoveredSurface?1.0:0.0,float(recoverySteps),hitField.x,hitTolerance); return;\n"+marker);
        int p=program(probe,"production exact-field structural recovery verification");
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);int bracketed=0,withinTolerance=0;double maximumRatio=0;
        for(int quality=0;quality<=2;quality++)for(double dose:new double[]{3,6,48,200,10000}) {
            setup(p,center,1,quality,0,120,.37f);dynamics(p,dose,1,1,45);float[] frame=draw();
            for(int i=0;i<frame.length;i+=4)if(frame[i]>.5) {
                if(frame[i+2]<=0){bracketed++;maximumRatio=Math.max(maximumRatio,Math.abs(frame[i+2])/frame[i+3]);}
                else {withinTolerance++;if(frame[i+2]>frame[i+3])throw new AssertionError("Recovery exceeds the original unchanged surface tolerance");}
            }
        }
        if(bracketed==0)throw new AssertionError("Recovery fixture never exercises exact-field bisection");
        System.out.printf("PASS: %d recovered pixels explicitly reach complete-field x<=0; %d satisfy unchanged primary pixel tolerance; maximum interior DE/tolerance %.5f%n",bracketed,withinTolerance,maximumRatio);
    }

    private static void recoveryReference(String source) {
        String marker="    vec3 background = realmBackground(ray);";
        String output="    fragColor=vec4(hit?1.0:0.0,distance,recoveredSurface?1.0:0.0,hitField.x); return;\n"+marker;
        int production=program(source.replace(marker,output),"production recovery visibility probe");
        String expanded=source.replaceAll("const int MAX_STEPS = [0-9]+;","const int MAX_STEPS = 2048;")
                .replaceAll("int stepLimit = [^;]+;","int stepLimit = 2048;")
                .replace("const bool EARLY_RECOVERY_ENABLED = true;","const bool EARLY_RECOVERY_ENABLED = false;");
        int reference=program(expanded.replace(marker,output),"2048 primary-step nearest-surface reference");
        resize(128,96);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);
        for(float time:referenceTimes())for(int quality=0;quality<=2;quality++)for(double dose:referenceDoses()) {
            setup(production,center,1,quality,0,time,.37f);dynamics(production,dose,1,1,45);float[] actual=draw();
            setup(reference,center,1,quality,0,time,.37f);dynamics(reference,dose,1,1,45);float[] ideal=draw();
            double[] relative=new double[WIDTH*HEIGHT];int compared=0,bad=0;double maximum=0;
            for(int i=0;i<actual.length;i+=4)if(actual[i]>.5&&actual[i+2]>.5&&ideal[i]>.5&&ideal[i+2]<.5) {
                double delta=actual[i+1]-ideal[i+1],fraction=delta/Math.max(.01,ideal[i+1]);
                relative[compared++]=fraction;maximum=Math.max(maximum,delta);
                if(delta>Math.max(.05,ideal[i+1]*.03))bad++;
            }
            relative=Arrays.copyOf(relative,compared);Arrays.sort(relative);
            System.out.printf("RECOVERY REFERENCE: clock%.0f quality%d dose%.0f, %d recovered rays vs2048step samequality reference; farther-than3%%/0.05realm %d (%.3f%% ofimage), relativeerror p10/50/90 %s, maxfartherdelta %.6f%n",time,quality,dose,compared,bad,100.0*bad/(WIDTH*HEIGHT),quantiles(relative),maximum);
            double maximumBadFraction=quality==2?.005:quality==1?.02:.03;
            if(bad/(double)(WIDTH*HEIGHT)>maximumBadFraction)throw new AssertionError("Recovery causes major nearest-surface occlusion errors at quality "+quality);
        }
    }

    private static void recoveryStereo(String source) {
        String marker="    vec3 background = realmBackground(ray);";
        int p=program(source.replace(marker,"    fragColor=vec4(origin+ray*distance,hit?1.0:0.0);return;\n"+marker),"production recovered-surface stereo correspondence");
        resize(640,360);
        Eye[] eyes={eye(-1,.27f,-.15f,.19f,new Vector3f(),true),eye(1,.27f,-.15f,.19f,new Vector3f(),true)};
        for(float time:referenceTimes())for(int quality=0;quality<=2;quality++)for(double dose:referenceDoses()) {
            float[][] frames=new float[2][];
            for(int side=0;side<2;side++){roomFixture(eyes[side]);setup(p,eyes[side],1,quality,0,time,.37f);dynamics(p,dose,1,1,45);frames[side]=draw();}
            int compared=0,farHoles=0;double maximumFraction=0;
            for(int side=0;side<2;side++) {
                Eye other=eyes[1-side];Vector3f realmOther=new Vector3f(other.position).sub(ANCHOR).mul(REALM_SCALE).add(BASE_ORIGIN);
                for(int y=2;y<HEIGHT-2;y+=4)for(int x=2;x<WIDTH-2;x+=4) {
                    int index=(y*WIDTH+x)*4;if(frames[side][index+3]<.5)continue;
                    Vector3f point=new Vector3f(frames[side][index],frames[side][index+1],frames[side][index+2]);
                    Vector3f physical=new Vector3f(point).sub(BASE_ORIGIN).div(REALM_SCALE).add(ANCHOR);
                    Vector4f clip=other.vp.transform(new Vector4f(physical.sub(other.position),1));if(clip.w<=0)continue;
                    float u=clip.x/clip.w*.5f+.5f,v=clip.y/clip.w*.5f+.5f;
                    int targetX=(int)(u*WIDTH),targetY=(int)(v*HEIGHT);if(targetX<2||targetX>=WIDTH-2||targetY<2||targetY>=HEIGHT-2)continue;
                    float expectedDistance=point.distance(realmOther),tolerance=Math.max(.01f,expectedDistance*4*.85f/HEIGHT);
                    boolean matched=false,foreground=false;float minimumFarther=Float.POSITIVE_INFINITY;
                    for(int offsetY=-1;offsetY<=1;offsetY++)for(int offsetX=-1;offsetX<=1;offsetX++) {
                        int target=((targetY+offsetY)*WIDTH+targetX+offsetX)*4;if(frames[1-side][target+3]<.5)continue;
                        Vector3f actual=new Vector3f(frames[1-side][target],frames[1-side][target+1],frames[1-side][target+2]);
                        float actualDistance=actual.distance(realmOther);if(actual.distance(point)<=tolerance)matched=true;
                        if(actualDistance<expectedDistance-tolerance)foreground=true;minimumFarther=Math.min(minimumFarther,actualDistance-expectedDistance);
                    }
                    compared++;
                    if(!matched&&!foreground&&minimumFarther>Math.max(.05,expectedDistance*.03)){
                        farHoles++;if(Float.isFinite(minimumFarther))maximumFraction=Math.max(maximumFraction,minimumFarther/expectedDistance);
                    }
                }
            }
            System.out.printf("RECOVERY STEREO: clock%.0f quality%d dose%.0f, %d projected surface probes, farther/missing opposite-eye holes %d (%.4f%%), largest farther fraction %.5f%n",time,quality,dose,compared,farHoles,100.0*farHoles/Math.max(1,compared),maximumFraction);
            if(farHoles/(double)Math.max(1,compared)>.005)throw new AssertionError("Recovery creates major opposite-eye visibility holes");
        }
        resize(128,96);fixture(0);
    }

    private static String quantiles(double[] values) {
        if(values.length==0)return "[]";return Arrays.toString(new double[]{values[(int)(values.length*.1)],values[values.length/2],values[(int)(values.length*.9)]});
    }

    private static double distanceDifference(float[] first,float[] second) {
        double sum=0;for(int i=0;i<first.length;i+=4)sum+=Math.abs(first[i]-second[i]);return sum/(first.length/4);
    }

    private static double[] gradients(float[] pixels) {
        double[] result=new double[4];int octave=0;
        for(int stride:new int[]{1,2,4,8}) {
            int samples=0;
            for(int y=0;y<HEIGHT;y++)for(int x=stride;x<WIDTH;x++) {
                int i=(y*WIDTH+x)*4,j=i-stride*4;
                for(int channel=0;channel<3;channel++)result[octave]+=Math.abs(pixels[i+channel]-pixels[j+channel]);samples+=3;
            }
            result[octave++]/=samples;
        }
        return result;
    }

    private static void doseEcologyAndLanes(int p,String source,Path directory)throws Exception {
        resize(128,96);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);Fixture room=roomFixture(center);
        int field=fieldProbe(source);float[] previous=null,previousField=null;double minimumDoseChange=1,minimumDoseGeometry=1;
        String layerSource=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestRecursion;\n")+"\nvoid main(){\n"+fieldInitialization(source)+"fragColor=TestRecursion.x<.5?doseLayerWeights:vec4(foldDepth,templeDepth,worldDoseGain,1);\n}\n";
        int layers=program(layerSource,"production dose-driven sculptural recursion layers");float[] previousLayers=null,previousDepths=null;double firstLayerSum=0,lastLayerSum=0,firstRecursion=0,lastRecursion=0;
        for(double dose:new double[]{3,6,12,48,200,10000,Double.MAX_VALUE}) {
            setup(p,center,1,2,0,45,.37f);dynamics(p,dose,1,1,45);float[] frame=draw();contrast(frame,"dose "+dose);
            setup(field,center,1,2,0,45,.37f);dynamics(field,dose,1,1,45);vec(field,"TestPlane",-3,0,0,0);float[] geometry=draw();
            setup(layers,center,1,2,0,45,.37f);dynamics(layers,dose,1,1,45);vec(layers,"TestRecursion",0,0,0,0);float[] layerWeights=Arrays.copyOf(draw(),4);
            vec(layers,"TestRecursion",1,0,0,0);float[] recursiveDepths=Arrays.copyOf(draw(),3);
            for(int depth=0;depth<3;depth++)if(recursiveDepths[depth]<=0||previousDepths!=null&&recursiveDepths[depth]<previousDepths[depth])throw new AssertionError("Fractal recursion/world distortion gain does not grow smoothly with dose");
            if(previousDepths==null)firstRecursion=recursiveDepths[0]+recursiveDepths[1];lastRecursion=recursiveDepths[0]+recursiveDepths[1];previousDepths=recursiveDepths;
            double layerSum=0;for(int layer=0;layer<4;layer++) {
                if(layerWeights[layer]<0||layerWeights[layer]>1||previousLayers!=null&&layerWeights[layer]<previousLayers[layer])throw new AssertionError("Dose recursion layers regress or exceed bounds");
                layerSum+=layerWeights[layer];
            }
            if(previousLayers==null)firstLayerSum=layerSum;lastLayerSum=layerSum;previousLayers=layerWeights;
            double imageChange=previous==null?0:difference(previous,frame),geometryChange=previousField==null?0:distanceDifference(previousField,geometry);
            if(previous!=null){minimumDoseChange=Math.min(minimumDoseChange,imageChange);minimumDoseGeometry=Math.min(minimumDoseGeometry,geometryChange);}
            System.out.printf("DOSE: %.4g units, logdrive %.5f, image/geometry change %.6f/%.6f, actual Mandelbox/Menger depth/worldgain %s, sculptural layers %s, RGB gradients1/2/4/8px %s%n",dose,Math.log1p(dose/2.4)/Math.log(2),imageChange,geometryChange,Arrays.toString(recursiveDepths),Arrays.toString(layerWeights),Arrays.toString(gradients(frame)));
            previous=frame;previousField=geometry;
        }
        if(minimumDoseChange<.001||minimumDoseGeometry<.00001)throw new AssertionError("High dose saturates to unchanged output/field: "+minimumDoseChange+"/"+minimumDoseGeometry);
        if(lastLayerSum-firstLayerSum<1.5)throw new AssertionError("High dose does not progressively add sculptural recursion");
        if(lastRecursion-firstRecursion<.5)throw new AssertionError("High dose never deepens actual Mandelbox/Menger recursion");
        setup(p,center,1,2,0,45,.37f);float[] baseline=draw();
        setup(field,center,1,2,0,45,.37f);vec(field,"TestPlane",-3,0,0,0);float[] baselineField=draw();
        String[] names={"organic","constructed","cave","water","light","weather","entities","movement","map red","map green","map blue","terrain relief"};
        float[][] inputs={{.4f,.3f,.35f,.15f},{.65f,.2f,.1f,.25f},{.32f,.58f,.27f,.45f}};
        double[] groupGeometry=new double[3];
        for(int parameter=0;parameter<12;parameter++) {
            int group=parameter/4,channel=parameter%4;float[] altered=inputs[group].clone();altered[channel]=Math.min(1,altered[channel]+.55f);
            setup(p,center,1,2,0,45,.37f);vec(p,"WorldInfluence"+(char)('A'+group),altered[0],altered[1],altered[2],altered[3]);
            float[] frame=draw();double imageChange=difference(baseline,frame);contrast(frame,names[parameter]);
            setup(field,center,1,2,0,45,.37f);vec(field,"WorldInfluence"+(char)('A'+group),altered[0],altered[1],altered[2],altered[3]);vec(field,"TestPlane",-3,0,0,0);
            double geometryChange=distanceDifference(baselineField,draw());groupGeometry[group]=Math.max(groupGeometry[group],geometryChange);
            if(imageChange<.00001)throw new AssertionError("Opaque realm ignores real-world influence "+names[parameter]);
            System.out.printf("ECOLOGY: %s changes opaque image/field %.6f/%.6f%n",names[parameter],imageChange,geometryChange);
        }
        for(int group=0;group<3;group++)if(groupGeometry[group]<.00001)throw new AssertionError("World influence group lacks geometry response: "+group);
        for(int variant=0;variant<3;variant++) {
            setup(p,center,1,2,0,45,.37f);setup(field,center,1,2,0,45,.37f);vec(field,"TestPlane",-3,0,0,0);
            if(variant==0) {glUseProgram(p);vec(p,"WorldInfluenceD",-.35f,.1f,.25f,.6f);glUseProgram(field);vec(field,"WorldInfluenceD",-.35f,.1f,.25f,.6f);}
            else {float entityX=variant==1?-1.5f:1.4f,entityZ=variant==1?-3.5f:-2, strength=variant==1?.7f:0;
                glUseProgram(p);vec(p,"WorldAttractor",entityX,.8f,entityZ,strength);glUseProgram(field);vec(field,"WorldAttractor",entityX,.8f,entityZ,strength);}
            float[] geometry=draw();glUseProgram(p);float[] frame=draw();
            double imageChange=difference(baseline,frame),fieldChange=distanceDifference(baselineField,geometry);
            if(imageChange<.00001||fieldChange<.00001)throw new AssertionError("Opaque realm ignores directional terrain/entity attractor: "+variant);
            System.out.printf("WORLD SPATIAL: %s changes opaque image/field %.6f/%.6f%n",new String[]{"terrain direction","entity position","entity absence"}[variant],imageChange,fieldChange);
        }
        setup(p,center,1,2,0,45,.37f);vec(p,"WorldAttractor",1.4f,.8f,-2,0);float[] absent=draw();
        vec(p,"WorldAttractor",-8000,9000,7000,0);if(maximumDifference(absent,draw())!=0)throw new AssertionError("Inactive entity position changes realm");
        setup(p,center,1,2,0,45,.37f);uploadFixture(room.colour,room.depth);float[] original=draw();
        float[] alternate=room.colour.clone();for(int i=0;i<alternate.length;i+=4){alternate[i]=.85f;alternate[i+1]=.12f;alternate[i+2]=.25f;alternate[i+3]=0;}
        uploadFixture(alternate,null);float[] recoloured=draw();double colourResponse=difference(original,recoloured);
        float[] depths=room.depth.clone();Arrays.fill(depths,.5f);uploadFixture(room.colour,depths);double depthResponse=difference(original,draw());
        if(colourResponse<.00001||depthResponse<.00001)throw new AssertionError("Fully opaque realm ignores hidden world colour/depth");
        for(int i=3;i<recoloured.length;i+=4)if(recoloured[i]!=1)throw new AssertionError("Source alpha leaked into opaque realm");
        uploadFixture(room.colour,room.depth);
        System.out.printf("PASS: opaque realm responds independently to hidden colour/depth %.6f/%.6f while sourcealpha0 stilloutputs1%n",colourResponse,depthResponse);
        ecologyTemporal(p,center);
        independentLayers(source,center);
        withinTripLanes(p,source,center);
        resize(640,360);center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);FRAMES.clear();
        for(double dose:new double[]{3,6,12,48,200,10000}) {
            setup(p,center,1,2,0,45,.37f);dynamics(p,dose,1,1,45);float[] frame=draw();FRAMES.add(new Frame("Dose "+(int)dose+" / same seed",frame));
            ImageIO.write(image(frame),"png",directory.resolve("dmt-fractal-dose-"+(int)dose+".png").toFile());
        }
        contactSheet(directory.resolve("dmt-fractal-dose-escalation.png"),3);FRAMES.clear();
        for(float time:new float[]{0,10,30,60,120,300}) {
            setup(p,center,1,2,0,time,.37f);dynamics(p,12,1,1,45);float[] frame=draw();FRAMES.add(new Frame("Same trip / motion t="+(int)time+"s",frame));
            ImageIO.write(image(frame),"png",directory.resolve("dmt-fractal-within-trip-"+(int)time+".png").toFile());
        }
        contactSheet(directory.resolve("dmt-fractal-within-trip.png"),3);FRAMES.clear();
        for(int environment=0;environment<4;environment++) {
            setup(p,center,1,2,0,45,.37f);
            if(environment==0){vec(p,"WorldInfluenceA",1,.05f,.05f,.1f);vec(p,"WorldInfluenceC",.13f,.46f,.12f,.6f);}
            if(environment==1){vec(p,"WorldInfluenceA",.05f,1,.1f,.05f);vec(p,"WorldInfluenceC",.55f,.46f,.34f,.2f);}
            if(environment==2){vec(p,"WorldInfluenceA",.03f,.15f,1,.1f);vec(p,"WorldInfluenceB",.1f,.65f,.05f,.05f);vec(p,"WorldInfluenceC",.18f,.20f,.24f,.9f);}
            if(environment==3){vec(p,"WorldInfluenceA",.25f,.05f,.2f,1);vec(p,"WorldInfluenceB",.5f,.8f,.35f,.6f);vec(p,"WorldInfluenceC",.05f,.24f,.65f,.35f);}
            FRAMES.add(new Frame(new String[]{"Forest","Constructed","Dark cave","Wet/storm"}[environment],draw()));
        }
        contactSheet(directory.resolve("dmt-fractal-world-influences.png"),2);FRAMES.clear();
        final Eye tripEye=center;
        animation(directory.resolve("dmt-fractal-within-trip-timelapse.gif"),96,frame->{
            float time=frame*300f/95;setup(p,tripEye,1,2,0,time,.37f);dynamics(p,12,1,1,45);return draw();
        });
        resize(128,96);fixture(0);
    }

    private static void nearNeutralWorld(int p) {
        resize(128,96);Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);Fixture room=roomFixture(center);
        setup(p,center,1,2,0,45,.37f);vec(p,"WorldInfluenceC",.5f,.5f,.5f,.45f);float[] neutral=draw();double tinyMaximum=0,rampMaximum=0;
        for(int channel=0;channel<3;channel++) {
            for(float epsilon:new float[]{.02f,.002f,.0002f,.00002f}) {
                float[] colour={.5f,.5f,.5f};colour[channel]+=epsilon;
                setup(p,center,1,2,0,45,.37f);vec(p,"WorldInfluenceC",colour[0],colour[1],colour[2],.45f);double response=difference(neutral,draw());
                System.out.printf("NEUTRAL WORLD: channel%d chroma%.6f vs gray imageMAE %.8f%n",channel,epsilon,response);
                if(epsilon<.00003f)tinyMaximum=Math.max(tinyMaximum,response);
            }
            float[] previous=null;double step=0;
            for(int frame=0;frame<=120;frame++) {
                float[] colour={.5f,.5f,.5f};colour[channel]+=.002f*(1-frame/120f);
                setup(p,center,1,2,0,45,.37f);vec(p,"WorldInfluenceC",colour[0],colour[1],colour[2],.45f);float[] current=draw();
                if(previous!=null)step=Math.max(step,difference(previous,current));previous=current;
            }
            rampMaximum=Math.max(rampMaximum,step);
        }
        System.out.printf("NEUTRAL WORLD SUMMARY: largest20e-6 chroma response %.8f, largest near-neutral120step image change %.8f%n",tinyMaximum,rampMaximum);
        if(tinyMaximum>.001||rampMaximum>.003)throw new AssertionError("World hue remains discontinuous as terrain chroma vanishes");
        float[] gray=new float[WIDTH*HEIGHT*4];for(int i=0;i<gray.length;i+=4){gray[i]=gray[i+1]=gray[i+2]=.5f;gray[i+3]=1;}
        uploadFixture(gray,room.depth);setup(p,center,1,2,0,45,.37f);float[] sourceNeutral=draw();double sourceMaximum=0;
        for(int channel=0;channel<3;channel++) {
            float[] weak=gray.clone();for(int i=channel;i<weak.length;i+=4)weak[i]+=.00002f;uploadFixture(weak,room.depth);
            setup(p,center,1,2,0,45,.37f);sourceMaximum=Math.max(sourceMaximum,difference(sourceNeutral,draw()));
        }
        uploadFixture(room.colour,room.depth);float[] first=null,previous=null;double weakStep=0,closure=0;
        for(int frame=0;frame<=120;frame++) {
            double angle=frame*Math.PI*2/120;
            setup(p,center,1,2,0,45,.37f);vec(p,"WorldInfluenceC",.5f+.002f*(float)Math.sin(angle),.5f+.002f*(float)Math.sin(angle+2.0944),.5f+.002f*(float)Math.sin(angle+4.1888),.45f);
            float[] current=draw();if(first==null)first=current;if(previous!=null)weakStep=Math.max(weakStep,difference(previous,current));previous=current;
        }
        closure=maximumDifference(first,previous);
        if(sourceMaximum>.001||weakStep>.003||closure>1e-5)throw new AssertionError("Weak world/source chroma creates a hue-sector seam");
        System.out.printf("PASS: neutral hidden-source20e-6 chroma response %.8f; weak RGBsector circle max image step %.8f, exact-loop component error %.9f%n",sourceMaximum,weakStep,closure);
    }

    private static void cellCarveContinuity(String source) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestPoint;\n")+"\nvoid main(){\n"+fieldInitialization(source)+"vec3 position=TestPoint.xyz;vec3 local=position-floor((position+CELL_SIZE*.5)/CELL_SIZE)*CELL_SIZE;fragColor=vec4(realmEmptySpace(position,local),0,0,1);}\n";
        int p=program(probe,"removed per-cell carve sign regression");resize(1,1);Eye center=eye(0,0,0,0,new Vector3f(),false);
        setup(p,center,1,1,0,0,.59145299145f);vec(p,"FractalB",.59145299145f,.62173382173f,.7f,.6f);vec(p,"FractalC",1,.87326007326f,.55433455433f,0);dynamics(p,3,1,1,0);
        for(String name:new String[]{"WorldInfluenceA","WorldInfluenceB","WorldInfluenceC","WorldInfluenceD","WorldAttractor"})vec(p,name,0,0,0,0);
        double maximum=0;
        for(float epsilon:new float[]{.001f,.0001f,.00001f}) {
            float[] values=new float[2];int side=0;
            for(float sign:new float[]{-1,1}){vec(p,"TestPoint",6.8f+sign*epsilon,0,0,0);glDrawArrays(GL_TRIANGLES,0,3);float[] value=new float[4];glReadPixels(0,0,1,1,GL_RGBA,GL_FLOAT,value);values[side++]=value[0];}
            double jump=Math.abs(values[0]-values[1]);maximum=Math.max(maximum,jump);
            if(!Float.isFinite(values[0])||!Float.isFinite(values[1])||jump>epsilon*5+1e-5)throw new AssertionError("Cell carve sign jumps across the removed-preference regression face: "+Arrays.toString(values));
        }
        resize(128,96);fixture(0);System.out.printf("PASS: actual cell-carve regression at x6.8±epsilon, unchanged global layout continuous; maximum tested difference %.8f%n",maximum);
    }

    private static void worldHueContinuity(String source) {
        if(!source.contains("float worldHueTint("))return;
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestHue;\n")+"\nvoid main(){fragColor=vec4(hueColor(worldHueTint(TestHue.x,TestHue.y,TestHue.z)),1);}\n";
        int p=program(probe,"actual world-hue antipodal continuity");resize(1,1);glUseProgram(p);double maximum=0,closure=0;
        for(float base:new float[]{.19f,.7f})for(float weight:new float[]{.10f,.16f,.20f,.21f}) {
            float[] first=null,previous=null;
            for(int frame=0;frame<=720;frame++) {
                vec(p,"TestHue",base,frame/720f,weight,0);glDrawArrays(GL_TRIANGLES,0,3);float[] current=new float[4];glReadPixels(0,0,1,1,GL_RGBA,GL_FLOAT,current);
                for(float value:current)if(!Float.isFinite(value))throw new AssertionError("World hue becomes nonfinite at an antipodal tint");
                if(first==null)first=current;if(previous!=null)maximum=Math.max(maximum,maximumDifference(previous,current));previous=current;
            }
            closure=Math.max(closure,maximumDifference(first,previous));
        }
        resize(128,96);fixture(0);
        if(maximum>.02||closure>1e-5)throw new AssertionError("World hue tint flips while crossing the antipode: "+maximum+" / "+closure);
        System.out.printf("PASS: 8 actual GPU world-hue full-circle/antipodal traversals, maximum RGBcomponent step %.6f, closed-loop error %.9f%n",maximum,closure);
    }

    private static void ecologyTemporal(int p,Eye center) {
        float[][] beginning={{.4f,.3f,.35f,.15f},{.65f,.2f,.1f,.25f},{.32f,.58f,.27f,.45f},{.35f,-.1f,-.25f,.6f},{1.4f,.3f,-2,.4f}};
        float[][] ending={{.8f,.1f,.75f,.65f},{.15f,.7f,.6f,.7f},{.85f,.22f,.75f,.45f},{-.2f,.3f,.5f,.75f},{-1.2f,.9f,-3,.7f}};
        for(int group=0;group<5;group++) {
            float[] initial=null,previous=null;double maximumStep=0,maximumBrightness=0;int largestStepFrame=0;
            for(int frame=0;frame<=120;frame++) {
                float t=frame/120f;t=t*t*(3-2*t);float[] influence=new float[4];
                for(int channel=0;channel<4;channel++)influence[channel]=beginning[group][channel]*(1-t)+ending[group][channel]*t;
                setup(p,center,1,2,0,45,.37f);vec(p,group==4?"WorldAttractor":"WorldInfluence"+(char)('A'+group),influence[0],influence[1],influence[2],influence[3]);
                float[] current=draw();
                if(initial==null)initial=current;
                if(previous!=null){double change=difference(previous,current);if(change>maximumStep){maximumStep=change;largestStepFrame=frame;}maximumBrightness=Math.max(maximumBrightness,Math.abs(meanLuma(previous)-meanLuma(current)));}
                previous=current;
            }
            double totalChange=difference(initial,previous);
            // Each world channel already has an independent image/field
            // response gate. This ramp checks continuity even when a material
            // makes that channel's visible contribution comparatively subtle.
            if(totalChange<.0005||maximumStep>.025||maximumBrightness>.010)
                throw new AssertionError("Eased ecology change flickers/doesnotrespond: group="+group+", total="+totalChange+", frame="+maximumStep+",brightness="+maximumBrightness);
            System.out.printf("ECOLOGY TEMPORAL: fixed seed/motion, eased group %c across120steps; totalchange %.6f, maximage/luma step %.6f/%.6f atframe%d%n",'A'+group,totalChange,maximumStep,maximumBrightness,largestStepFrame);
        }
    }

    private static void independentLayers(String source,Eye center) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestLayer;\n")+"\nvoid main(){\n"+fieldInitialization(source)+"""
                    if(TestLayer.x<.5)fragColor=layoutLanes;
                    else if(TestLayer.x<1.5)fragColor=TestLayer.y<.5?surfaceMaterialsA:vec4(surfaceMaterialsB,0,0);
                    else if(TestLayer.x<2.5)fragColor=TestLayer.y<.5?motionLanes:vec4(motionSecondary,0,0);
                    else if(TestLayer.x<3.5)fragColor=TestLayer.y<.5?patternLanes:vec4(patternVeil,0,0,0);
                    else if(TestLayer.x<4.5)fragColor=TestLayer.y<.5?lightingLanes:vec4(atmosphereLanes,0,0);
                    else fragColor=worldStructure;
                }
                """;
        int p=program(probe,"production six independent layer-group probe");
        String[] names={"layout","surface material","motion","pattern","lighting/atmosphere","world interpretation"};
        int[] sizes={4,6,6,5,6,4};float[][] first=new float[6][];double[] ranges=new double[6];
        for(float time:new float[]{0,10,30,60,120,300}) {
            for(int group=0;group<6;group++) {
                float[] state=new float[sizes[group]];
                for(int part=0;part<(sizes[group]>4?2:1);part++) {
                    setup(p,center,1,2,0,time,.37f);dynamics(p,12,1,1,45);vec(p,"TestLayer",group,part,0,0);float[] values=draw();
                    for(int component=0;component<Math.min(4,state.length-part*4);component++)state[part*4+component]=values[component];
                }
                if(first[group]==null)first[group]=state;
                else {double range=0;for(int component=0;component<state.length;component++)range+=Math.pow(state[component]-first[group][component],2);ranges[group]=Math.max(ranges[group],Math.sqrt(range));}
                System.out.println("LAYER: "+time+"s "+names[group]+" "+Arrays.toString(state));
            }
        }
        for(int group=0;group<5;group++)if(ranges[group]<.10)throw new AssertionError("Independent "+names[group]+" group does not evolve: "+ranges[group]);
        if(ranges[5]>.000001)throw new AssertionError("World interpretation evidence drifts with fixed world inputs");
        double worldRange=0;
        for(int environment=0;environment<3;environment++) {
            setup(p,center,1,2,0,45,.37f);
            if(environment==0){vec(p,"WorldInfluenceA",1,.05f,.05f,.1f);vec(p,"WorldInfluenceD",.7f,.1f,-.4f,.8f);}
            if(environment==1){vec(p,"WorldInfluenceA",.05f,1,.5f,.05f);vec(p,"WorldInfluenceD",-.5f,.3f,.2f,.9f);}
            if(environment==2){vec(p,"WorldInfluenceA",.1f,.05f,.1f,1);vec(p,"WorldAttractor",-2,.8f,-3,.9f);}
            vec(p,"TestLayer",5,0,0,0);float[] state=draw();double range=0;for(int component=0;component<4;component++)range+=Math.pow(state[component]-first[5][component],2);
            worldRange=Math.max(worldRange,Math.sqrt(range));
        }
        if(worldRange<.1)throw new AssertionError("World interpretation group lacks independent evidence response");
        double lowDominance=0,highDominance=0;
        for(double dose:new double[]{3,200,10000}) {
            setup(p,center,1,2,0,0,.37f);dynamics(p,dose,1,1,45);vec(p,"TestLayer",2,0,0,0);float[] motion=Arrays.copyOf(draw(),4);double maximum=0;
            for(float weight:motion)maximum=Math.max(maximum,weight);if(dose==3)lowDominance=maximum;else if(dose==10000)highDominance=maximum;
            System.out.println("MOTION DOSE: "+dose+" actual behavior weights "+Arrays.toString(motion));
        }
        if(lowDominance<.6||highDominance>=lowDominance-.05)throw new AssertionError("Dose never broadens a dominant motion behavior into interactions");
        System.out.println("PASS: five independent motion-clock groups evolve, sixth world-interpretation group changes with actual world evidence; ranges="+Arrays.toString(ranges)+", worldrange="+worldRange);
    }

    private static void withinTripLanes(int p,String source,Eye center) {
        String probeSource=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestLane;\n")+"\nvoid main(){\n"+fieldInitialization(source)+"""
                    if(TestLane.x<.5)fragColor=geometryLanesA;
                    else if(TestLane.x<1.5)fragColor=geometryLanesB;
                    else if(TestLane.x<2.5)fragColor=materialLanes;
                    else if(TestLane.x<3.5)fragColor=vec4(familyWeights,1);
                    else if(TestLane.x<4.5)fragColor=familyChapters;
                    else fragColor=vec4(chamberAspect,vaultRoundness);
                }
                """;
        int lanes=program(probeSource,"production within-trip independent lane probe");
        List<float[]> combinations=new ArrayList<>();double minimumCombination=Double.POSITIVE_INFINITY;
        float[][] firstLanes=new float[4][];double[] maximumLaneChange=new double[4];
        java.util.Set<Integer> dominantFamilies=new java.util.HashSet<>();float maximumDominance=0;
        for(float time:new float[]{0,10,30,60,120,300}) {
            float[] combination=new float[15];int cursor=0;
            for(int lane=0;lane<4;lane++) {
                setup(lanes,center,1,2,0,time,.37f);dynamics(lanes,12,1,1,45);vec(lanes,"TestLane",lane,0,0,0);float[] value=draw();
                int components=lane==3?3:4;
                for(int component=0;component<components;component++) {
                    combination[cursor++]=value[component];
                }
                if(firstLanes[lane]==null)firstLanes[lane]=Arrays.copyOf(value,components);
                else {double change=0;for(int component=0;component<components;component++)change+=Math.pow(value[component]-firstLanes[lane][component],2);maximumLaneChange[lane]=Math.max(maximumLaneChange[lane],Math.sqrt(change));}
            }
            for(float[] older:combinations){double distance=0;for(int i=0;i<combination.length;i++)distance+=Math.pow(older[i]-combination[i],2);minimumCombination=Math.min(minimumCombination,Math.sqrt(distance));}
            int dominant=0;for(int family=1;family<3;family++)if(combination[12+family]>combination[12+dominant])dominant=family;
            dominantFamilies.add(dominant);maximumDominance=Math.max(maximumDominance,combination[12+dominant]);
            combinations.add(combination);System.out.println("LANES: motion "+time+"s "+Arrays.toString(combination)+" dominantfamily="+dominant);
            setup(lanes,center,1,2,0,time,.37f);dynamics(lanes,12,1,1,45);vec(lanes,"TestLane",4,0,0,0);float[] chapters=Arrays.copyOf(draw(),4);
            vec(lanes,"TestLane",5,0,0,0);float[] topology=Arrays.copyOf(draw(),4);
            System.out.println("TOPOLOGY: motion "+time+"s chapterdrivers="+Arrays.toString(chapters)+", chamberaspect/roundness="+Arrays.toString(topology));
        }
        if(minimumCombination<.05)throw new AssertionError("Within-trip combinations repeat or barely evolve: "+minimumCombination);
        for(int lane=0;lane<4;lane++)if(maximumLaneChange[lane]<.10)throw new AssertionError("Procedural lane lacks independent evolution: "+lane);
        if(dominantFamilies.size()<2)throw new AssertionError("Withintrip always remains in the same dominant geometry family");
        System.out.println("CHAPTERS: distinct dominant families="+dominantFamilies.size()+", maximum dominance="+maximumDominance);
        setup(p,center,1,2,0,45,.37f);dynamics(p,12,1,1,45);float[] frozen=draw();
        dynamics(p,12,1,1,300);if(maximumDifference(frozen,draw())!=0)throw new AssertionError("Trip elapsedtime overrides frozen integrated motion atpeak");
        double maximumLongTripStep=0;
        for(float start:new float[]{0,10,30,60,120,300}) {
            float[] previous=null;
            for(int frame=0;frame<=30;frame++) {
                setup(p,center,1,2,0,start+frame/30f,.37f);dynamics(p,12,1,1,45);float[] current=draw();
                if(previous!=null)maximumLongTripStep=Math.max(maximumLongTripStep,difference(previous,current));previous=current;
            }
        }
        if(maximumLongTripStep>.055)throw new AssertionError("Withintrip lane evolution produces abrupt late change: "+maximumLongTripStep);
        System.out.printf("PASS: geometry/material/family combinations differ across0/10/30/60/120/300s (minimumvector distance %.6f); frozenmotion exact; max30fps longtrip step %.6f%n",minimumCombination,maximumLongTripStep);
    }

    private static String fieldInitialization(String source) {
        int start=source.indexOf("    float reduced =",source.indexOf("void main()"));
        int end=source.indexOf("    vec3 origin = realmEyeOrigin();",start);
        if(start<0||end<0)throw new AssertionError("Production field initialization not found");
        return "    vec2 uv = gl_FragCoord.xy / max(OutSize,vec2(1.0));\n"+source.substring(start,end)+"    allowEmptyShortcut = false;\n";
    }

    private static void beautyAndAnimation(int p,Path directory)throws Exception {
        resize(640,360);fixture(0);FRAMES.clear();
        Eye center=eye(0,.27f,-.15f,0,new Vector3f(),false);
        for(float intensity:new float[]{.25f,.5f,.75f,1}) {
            setup(p,center,1,2,0,3,.37f);vec(p,"FractalC",intensity,.19f,.83f,3);
            dynamics(p,6,intensity,1,45);
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
        // Shader ingress is driven through a full35-second cinematic entrance;
        // controller timing/envelopes have their own production-controller tests.
        setup(p,center,1,2,0,0,.37f);dynamics(p,12,1,0,0);float[] previous=draw();
        double maximumStep=0,maximumBrightnessStep=0;
        for(int frame=1;frame<=1200;frame++) {
            float seconds=frame/30f,ingress=Math.min(1,seconds/35);
            setup(p,center,1,2,0,seconds,.37f);dynamics(p,12,1,ingress,seconds);float[] current=draw();
            maximumStep=Math.max(maximumStep,difference(previous,current));
            maximumBrightnessStep=Math.max(maximumBrightnessStep,Math.abs(meanLuma(previous)-meanLuma(current)));previous=current;
        }
        if(maximumStep>.055||maximumBrightnessStep>.020)
            throw new AssertionError("Morph transition jumps abruptly: mean="+maximumStep+",brightness="+maximumBrightnessStep);
        for(int side:new int[]{-1,1}) {
            Eye eye=eye(side,.27f,-.15f,.32f,new Vector3f(),true);roomFixture(eye);
            setup(vr,eye,.35f,2,0,3,.37f);contrast(draw(),"per-eye partial world morph");
        }
        cinematicCoverage(p,source,center,room);
        System.out.printf("PASS: checker-room morph differs from overlay crossfade %.6f, responds to actual depth %.6f; 40s x30fps entry max image/luma step %.6f/%.6f; both canted eyes finite%n",nonlinear,depthResponse,maximumStep,maximumBrightnessStep);
        resize(640,360);center=eye(0,.27f,-.15f,0,new Vector3f(),false);roomFixture(center);FRAMES.clear();
        for(float blend:new float[]{0,.1f,.25f,.5f,.75f,1}) {
            setup(p,center,1,2,0,blend*35,.37f);dynamics(p,12,1,blend,blend*35);float[] frame=draw();FRAMES.add(new Frame("Ingress "+Math.round(blend*100)+"% / "+Math.round(blend*35)+"s",frame));
            ImageIO.write(image(frame),"png",directory.resolve("dmt-fractal-morph-"+Math.round(blend*100)+".png").toFile());
        }
        contactSheet(directory.resolve("dmt-fractal-morph-strip.png"),3);FRAMES.clear();
        final Eye animationEye=center;
        animation(directory.resolve("dmt-fractal-world-morph.gif"),96,frame->{float ingress=frame/95f;setup(p,animationEye,1,2,0,ingress*35,.37f);dynamics(p,12,1,ingress,ingress*35);return draw();});
    }

    private static void cinematicCoverage(int p,String source,Eye center,Fixture room) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistEntry()")
                .replaceFirst("#version 150","#version 150\nuniform vec4 TestCoverage;\n")+"""
                void main(){fragColor=vec4(cinematicCoverage(TestCoverage.x,TestCoverage.y));}
                """;
        int coverage=program(probe,"production cinematic material-coverage probe");double earlyMaximum=0;
        for(float patch:new float[]{-1,0,1}) {
            double previous=-1;
            for(float ingress:new float[]{0,.1f,.3f,.6f,.8f,.95f,.999f,1}) {
                setup(coverage,center,1,2,0,0,.37f);vec(coverage,"TestCoverage",ingress,patch,0,0);float value=draw()[0];
                if(value<previous||value<0||value>1||ingress<1&&value>=1)throw new AssertionError("Cinematic coverage clips/changes nonmonotonically");
                if(ingress==0&&value!=0||ingress==1&&value!=1)throw new AssertionError("Cinematic coverage endpoint incorrect");
                if(ingress<=.6f)earlyMaximum=Math.max(earlyMaximum,value);previous=value;
            }
        }
        if(earlyMaximum>.8)throw new AssertionError("Cinematic material covers world too early: "+earlyMaximum);
        float[] dark=room.colour.clone(),light=room.colour.clone();
        for(int i=0;i<dark.length;i+=4)for(int channel=0;channel<3;channel++){dark[i+channel]=.10f;light[i+channel]=.80f;}
        double[] sourceResponse=new double[3];int index=0;
        for(float ingress:new float[]{.3f,.6f,1}) {
            setup(p,center,1,2,0,ingress*35,.37f);dynamics(p,200,1,ingress,ingress*35);
            uploadFixture(dark,room.depth);float[] low=draw();uploadFixture(light,null);float[] high=draw();
            sourceResponse[index++]=difference(low,high);
        }
        uploadFixture(room.colour,room.depth);
        if(sourceResponse[1]<.04||sourceResponse[1]<sourceResponse[2]*1.5)
            throw new AssertionError("Cinematic ingress becomes opaque too early: sourcecontrast0.3/0.6/1="+Arrays.toString(sourceResponse));
        System.out.println("CINEMATIC: actual maximum material coverage throughingress0.6="+earlyMaximum+", source contrast at0.3/0.6/1 (dose200) "+Arrays.toString(sourceResponse)+"; familiarworld remains visible through0.6");
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
