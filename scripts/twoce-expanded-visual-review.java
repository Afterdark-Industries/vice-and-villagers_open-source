import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import com.trippycraft.substance.twoce.TwoCeVisualDoseResponse;
import com.trippycraft.substance.twoce.TwoCeVisualPresentation;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Production GLSL on a hidden real GPU. Ray-box fixture has exact projected depth
 * and Minecraft albedo, but is an isolated rendering review, not an in-game capture. */
final class TwoCeExpandedVisualReview {
    static int width = 640, height = 360, colorTexture, depthTexture, baselineProgram;
    static final float[] DISPLAY_DOSES = {25,100,250,500,1000,2000,3000,4000,5000};
    static final long FIXTURE_SEED = 20774;
    static final float SEED = FIXTURE_SEED / 65535.0f;
    static Path root, output;
    static final Map<String, BufferedImage> albedo = new HashMap<>();
    static final String VERTEX = """
        #version 150
        in vec3 Position;
        void main() { gl_Position = vec4(Position, 1.0); }
        """;
    record Eye(Vector3f position, Matrix4f vp, Matrix4f inverse, Matrix4f rotation) {}
    record Box(Vector3f minimum, Vector3f maximum, String texture) {}
    record Scene(float[] color, float[] depth, int[] material) {}
    record Hit(float distance, Vector3f position, Vector3f normal, String texture) {}

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        root = Path.of(args[0]);
        output = root.resolve("build/twoce-expanded-strength-review");
        Files.createDirectories(output);
        if (args.length > 1) { width = Integer.parseInt(args[1]); height = width * 9 / 16; }
        if (args.length < 3) throw new IllegalArgumentException("Usage: <repository> <width> <Minecraft resources JAR>");
        loadTextures(Path.of(args[2]));
        GLFWErrorCallback error = GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        long window = NULL;
        try {
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(width, height, "2C-E isolated visual review", NULL, NULL);
            if (window == NULL) throw new IllegalStateException("Hidden GPU context unavailable");
            glfwMakeContextCurrent(window); GL.createCapabilities();
            System.out.println("GPU: " + glGetString(GL_RENDERER) + " / " + glGetString(GL_VERSION));
            int vao = glGenVertexArrays(); glBindVertexArray(vao);
            int vbo = glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0,3,-1,0,-1,3,0}, GL_STATIC_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,0,0L); glEnableVertexAttribArray(0);
            int fbo = glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER,fbo);
            int target = texture(GL_RGBA32F,GL_RGBA,new float[width*height*4]);
            glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,target,0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) throw new AssertionError("Framebuffer incomplete");
            glViewport(0,0,width,height); glDisable(GL_BLEND); glDisable(GL_DEPTH_TEST);
            colorTexture = texture(GL_RGBA32F,GL_RGBA,new float[width*height*4]);
            depthTexture = texture(GL_R32F,GL_RED,new float[width*height]);
            Path shaders = root.resolve("src/main/resources/assets/trippycraft/shaders/program");
            String finalSource=Files.readString(shaders.resolve("two_ce_world_warp.fsh"));
            String vrSource=Files.readString(shaders.resolve("two_ce_world_warp_vr.fsh"));
            if(!finalSource.replace("\r\n","\n").equals(vrSource.replace("\r\n","\n")))throw new AssertionError("Native desktop/VR sources differ; finish shader regeneration before validation");
            Path baselinePath=root.resolve("tests/fixtures/twoce-before/two_ce_world_warp.fsh");
            boolean baselineAvailable=Files.exists(baselinePath);
            String baselineSource=baselineAvailable?Files.readString(baselinePath):finalSource;
            int before = program(baselineSource, baselineAvailable?"previous approved desktop":"current desktop comparison fallback");
            baselineProgram=before;
            int after = program(finalSource, "after desktop");
            int vr = program(vrSource, "after VR");
            java.util.regex.Matcher gainMatch=java.util.regex.Pattern.compile("const\\s+float\\s+VISUAL_GAIN\\s*=\\s*([0-9.]+)\\s*;").matcher(finalSource);
            if(!gainMatch.find() || Double.parseDouble(gainMatch.group(1))!=2.0)throw new AssertionError("Requested authored 2x VISUAL_GAIN missing");
            int singleGain=program(finalSource.substring(0,gainMatch.start(1))+"1.0"+finalSource.substring(gainMatch.end(1)),"one-times strength ablation");
            int roomsWithoutInterior=program(withoutInteriorCall(finalSource,"roomColor"),"small-room material ablation");
            int galleriesWithoutInterior=program(withoutInteriorCall(finalSource,"galleryColor"),"gallery material ablation");
            int noInterior=program(withoutInteriorCall(withoutInteriorCall(finalSource,"roomColor"),"galleryColor"),"all chamber materials disabled");
            int noExpandedInside=program(withoutFunctionCall(finalSource,"expandedMaterial","material"),"expanded inside-wall materials disabled");
            System.out.println("PASS: production desktop/VR GLSL compile and link"+(baselineAvailable?"; archived baseline compiled":"; archived baseline unavailable, no before images generated"));
            int frames = safeSampling(finalSource);
            frames += chamberProbes(finalSource);
            doseResponseChecks();
            presentationChecks();
            for (boolean natural : new boolean[]{false,true}) {
                Eye eye = eye(0,0);
                Scene scene = fixture(eye,natural);
                upload(scene);
                String label = natural ? "outdoor" : "architecture";
                save(scene.color, label+"-sober.png");
                setup(after,eye); uniforms(after,0,1200,1,1,0.5f);
                float[] neutral = draw(); frames++;
                if (difference(neutral,scene.color) > 0.000001) throw new AssertionError("Sober identity changed");
                float[] previous = null;
                for (float dose : DISPLAY_DOSES) {
                    setup(before,eye); uniforms(before,dose,1200,0.86f,0.92f,0.5f);
                    float[] baseline = draw(); frames++;
                    if(baselineAvailable)save(baseline,label+"-previous-"+(int)dose+".png");
                    setup(after,eye); uniforms(after,dose,1200,0.86f,0.92f,0.5f);
                    float[] result = draw(); frames++;
                    save(result,label+"-after-"+(int)dose+".png");
                    vec(after,"HighDose",0,0,0,0);
                    float[] baseOnly = draw(); frames++;
                    double strengthChange = difference(result,baseline);
                    double previousActive = difference(scene.color,baseline);
                    if(baselineAvailable && strengthChange<0.00001)throw new AssertionError("Requested stronger shader changes no pixels at "+dose+"mg");
                    setup(singleGain,eye); uniforms(singleGain,dose,1200,0.86f,0.92f,0.5f);
                    float[] single=draw(); frames++;
                    double gainChange=difference(single,result);
                    if(gainChange<0.000001)throw new AssertionError("VISUAL_GAIN=2 contributes no visible pixels at "+dose+"mg");
                    double layerChange = difference(baseOnly,result);
                    if(dose > 250 && layerChange < 0.000001) throw new AssertionError("High-dose layers change no pixels at " + dose + " displayed mg");
                    double adjacent = previous == null ? 0 : difference(previous,result);
                    if(previous != null && adjacent < 0.00001) throw new AssertionError("Adjacent displayed stages collapsed at " + dose + " mg: " + adjacent);
                    previous = result;
                    setup(vr,eye); uniforms(vr,dose,1200,0.86f,0.92f,0.5f);
                    float[] stereo = draw(); frames++;
                    if (difference(result,stereo)>0.000001) throw new AssertionError("Native center-eye parity changed");
                    double active = difference(scene.color,result);
                    if (active < 0.0001) throw new AssertionError("Active shader changes no pixels");
                    double near = regionDifference(scene.color,result,scene.material,-1);
                    if (near > 0.00005) throw new AssertionError("Near-hand fixture changed: "+near);
                    System.out.printf("CHECK: %s displayed %.0fmg -> calibrated %.1f units; previous-to-stronger delta %.6f, sober deltas %.6f to %.6f, gain1-to-gain2 delta %.6f, high-dose layer delta %.6f, adjacent delta %.6f, VR parity %.8f, protected near %.8f%n",label,dose,effectForContent(dose),strengthChange,previousActive,active,gainChange,layerChange,adjacent,difference(result,stereo),near);
                }
                setup(after,eye); uniforms(after,5000,1200,0,0.38f,0.5f);
                save(draw(),label+"-reduced-motion-color.png"); frames++;
                setup(after,eye); uniforms(after,5000,1200,0.86f,0.92f,0.5f,0,true);
                float[] tail = draw(); frames++;
                if(maximumDifference(tail,scene.color) > 0.000001) throw new AssertionError("Retained 5000mg content leaves a tail at zero envelope");
                frames += comfort(after,scene,eye,natural?"outdoor":"architecture");
                frames += separateLayers(after,scene,eye,label);
                frames += separateExtraLayers(after,scene,eye,label);
                if(!natural)frames += chamberScene(after,roomsWithoutInterior,galleriesWithoutInterior,noInterior,scene,eye,label);
                if(!natural)frames += expandedInsideScene(after,noExpandedInside,scene,eye,label);
                if(baselineAvailable) {
                    setup(before,eye);uniforms(before,5000,1200,0.86f,0.92f,0.5f);float[] oldAnchor=draw();frames++;
                    setup(after,eye);uniforms(after,500,1200,0.86f,0.92f,0.5f);float[] newAnchor=draw();frames++;
                    double anchorDelta=difference(oldAnchor,newAnchor);
                    System.out.printf("CHECK: %s previous5000mg/new500mg anchor mean %.9f, max %.6f; sober changes %.6f/%.6f%n",label,anchorDelta,maximumDifference(oldAnchor,newAnchor),difference(scene.color,oldAnchor),difference(scene.color,newAnchor));
                    if(anchorDelta>0.00002)throw new AssertionError("New500mg does not reproduce previous5000mg authored anchor: "+anchorDelta);
                }
            }
            Eye eye = eye(0,0); Scene scene = fixture(eye,false); upload(scene);
            for (int index=0;index<12;index++) {
                if(baselineAvailable) {
                    setup(before,eye); uniforms(before,5000,1200+index*28,0.86f,0.92f,0.72f);
                    save(draw(),String.format("animation-previous-%02d.png",index)); frames++;
                }
                setup(after,eye); uniforms(after,5000,1200+index*28,0.86f,0.92f,0.72f);
                save(draw(),String.format("animation-%02d.png",index)); frames++;
            }
            for (float dose : new float[]{1,25,250,500,1250,3750,5000,Float.MAX_VALUE}) {
                for (float time : new float[]{0,1234,95000}) for (float motion : new float[]{0,0.22f,1}) {
                    setup(after,eye); uniforms(after,dose,time,motion,0.38f,0.8f); draw(); frames++;
                }
            }
            for (float roll : new float[]{0,0.38f,-0.6f}) for (int side : new int[]{-1,1}) {
                Eye stereoEye=eye(side,roll); upload(fixture(stereoEye,false));
                setup(vr,stereoEye); uniforms(vr,5000,1234,0.22f,0.38f,0.8f); draw(); frames++;
            }
            frames+=continuity(after,scene,eye,250);
            frames+=continuity(after,scene,eye,5000);
            frames+=highResolution(after,eye);
            upload(scene); setup(after,eye); uniforms(after,5000,1234,0.86f,0.92f,0.5f);
            for (int i=0;i<4;i++) draw();
            long start=System.nanoTime();
            for (int i=0;i<8;i++) { glDrawArrays(GL_TRIANGLES,0,3); glFinish(); }
            double elapsed=(System.nanoTime()-start)/1e6/8;
            System.out.printf("MEASURE: isolated %dx%d production pass GPU+finish %.3f ms/frame (not game/VR performance)%n",width,height,elapsed);
            if(GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query) {
                int query=glGenQueries();long gpuNanoseconds=0;
                for(int i=0;i<8;i++) {
                    glBeginQuery(GL_TIME_ELAPSED,query);glDrawArrays(GL_TRIANGLES,0,3);glEndQuery(GL_TIME_ELAPSED);
                    gpuNanoseconds+=glGetQueryObjectui64(query,GL_QUERY_RESULT);
                }
                glDeleteQueries(query);
                System.out.printf("MEASURE: isolated GL_TIME_ELAPSED %dx%d %.3f GPU ms/frame%n",width,height,gpuNanoseconds/8.0/1e6);
            }
            if (glGetError()!=GL_NO_ERROR) throw new AssertionError("OpenGL error");
            Files.writeString(output.resolve("two_ce_rendered_current.fsh"),finalSource);
            Files.writeString(output.resolve("two_ce_rendered_current_vr.fsh"),vrSource);
            System.out.printf("PASS: %d finite rendered frames; sober identity, alpha, near protection, real depth silhouettes, dose/overrange, time wrap, motion/color controls, both VR eyes and roll; images in %s%n",frames,output);
        } finally {
            if(window!=NULL) glfwDestroyWindow(window); glfwTerminate(); error.free();
        }
    }

    static Eye eye(int side,float roll) {
        Matrix4f rotation=new Matrix4f().rotationYXZ(-0.15f,-0.08f,roll);
        Vector3f position=rotation.transformDirection(new Vector3f(side*0.032f,0,0)).add(0,1.72f,4.7f);
        Matrix4f projection=new Matrix4f().perspective((float)Math.toRadians(75),(float)width/height,0.05f,96);
        Matrix4f vp=projection.mul(new Matrix4f(rotation).transpose());
        return new Eye(position,vp,new Matrix4f(vp).invert(),rotation);
    }
    static String withoutInteriorCall(String source,String color) {
        return withoutFunctionCall(source,"chamberMaterial",color);
    }
    static String withoutFunctionCall(String source,String function,String color) {
        java.util.regex.Matcher match=java.util.regex.Pattern.compile(function+"\\s*\\(\\s*"+color+"\\s*,").matcher(source);
        if(!match.find())throw new AssertionError("Production "+color+" has no "+function+" call");
        int start=match.start(),end=source.indexOf('(',start)+1,balance=1;
        while(end<source.length()&&balance>0){char c=source.charAt(end++);if(c=='(')balance++;else if(c==')')balance--;}
        if(balance!=0)throw new AssertionError("Unbalanced chamberMaterial invocation");
        return source.substring(0,start)+color+source.substring(end);
    }
    static int chamberProbes(String source) throws Exception {
        String call="chamberMaterial(material,wall,0.006,0.15+uv.y*0.80,4.0,step(0.5,uv.x),HighDose,0.8,Control.y,Control.x,mod(WarpA.w,96000.0)*0.05,WarpC.y,0.75,ExtraDose,vec2(0.6,0.4))";
        String prefix=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistMain()");
        String wrapper="\nvoid main(){vec2 uv=gl_FragCoord.xy/max(OutSize,vec2(1));vec2 wall=(uv-0.5)*8.0;vec3 material=vec3(0.26,0.33,0.39);fragColor=vec4(mix(material,"+call+",0.88),1.0);}\n";
        int p=program(prefix+wrapper,"production chamberMaterial behind 88% hole blend");
        Eye eye=eye(0,0);setup(p,eye);
        float[] reference=new float[width*height*4];for(int i=0;i<reference.length;i+=4){reference[i]=0.26f;reference[i+1]=0.33f;reference[i+2]=0.39f;reference[i+3]=1;}
        int frames=0;
        for(float dose:new float[]{250,500,1000,5000})for(float motion:new float[]{0,0.86f}) {
            uniforms(p,dose,1200,motion,0.92f,0.75f);float[] result=draw();frames++;
            double contribution=difference(result,reference);
            if(dose>=500 && contribution<0.00001)throw new AssertionError("Inside-wall detail vanished behind88% blend at "+dose+"mg, motion="+motion);
            save(result,"chamber-probe-"+(int)dose+"-motion-"+(motion==0?"off":"on")+".png");
            System.out.printf("CHECK: chamberMaterial %.0fmg, 88%% blend, motion %.2f; analytic interior contribution %.6f%n",dose,motion,contribution);
        }
        uniforms(p,5000,0,0.86f,0.92f,0.75f);float[] begin=draw();setTime(p,96000);float[] end=draw();frames+=2;
        if(maximumDifference(begin,end)>0.002 || difference(begin,end)>0.00002)throw new AssertionError("Inside-wall clock seam failed");
        System.out.println("PASS: actual production chamberMaterial, side/back wall coordinates, 88% blend, retained motion-off relief, inside-wall clock seam");
        return frames;
    }
    static int chamberScene(int after,int noRooms,int noGalleries,int none,Scene scene,Eye eye,String label) throws Exception {
        upload(scene);int frames=0;
        for(float dose:new float[]{250,1000,5000}) {
            setup(after,eye);uniforms(after,dose,1200,0.86f,0.92f,0.75f);float[] full=draw();frames++;
            setup(noRooms,eye);uniforms(noRooms,dose,1200,0.86f,0.92f,0.75f);float[] rooms=draw();frames++;
            setup(noGalleries,eye);uniforms(noGalleries,dose,1200,0.86f,0.92f,0.75f);float[] galleries=draw();frames++;
            setup(none,eye);uniforms(none,dose,1200,0.86f,0.92f,0.75f);float[] disabled=draw();frames++;
            double roomDelta=difference(full,rooms),galleryDelta=difference(full,galleries),interiorDelta=difference(full,disabled);
            if(dose>=1000&&(roomDelta<0.000001||galleryDelta<0.000001))throw new AssertionError("Inside-wall detail missing from actual small rooms or galleries at "+dose);
            save(full,label+"-inside-full-"+(int)dose+".png");save(disabled,label+"-inside-disabled-"+(int)dose+".png");
            float[] delta=new float[full.length];for(int i=0;i<delta.length;i++)delta[i]=i%4==3?1:Math.abs(full[i]-disabled[i])*5;save(delta,label+"-inside-difference5x-"+(int)dose+".png");
            System.out.printf("CHECK: actual ray-hit chamber materials %.0fmg; small-room delta %.6f, gallery delta %.6f, total inside delta %.6f (mask/ray-hit/light path retained)%n",dose,roomDelta,galleryDelta,interiorDelta);
        }
        return frames;
    }
    static int expandedInsideScene(int after,int noExpanded,Scene scene,Eye eye,String label) throws Exception {
        upload(scene);int frames=0;
        for(float dose:new float[]{500,1000,3000,5000}) {
            setup(after,eye);uniforms(after,dose,1200,0.86f,0.92f,0.75f);float[] full=draw();frames++;
            setup(noExpanded,eye);uniforms(noExpanded,dose,1200,0.86f,0.92f,0.75f);float[] disabled=draw();frames++;
            double insideContribution=difference(full,disabled);
            if(dose==500&&insideContribution>0.000002)throw new AssertionError("New500mg opened premium inside-wall material before its threshold");
            if(dose>500&&insideContribution<0.000001)throw new AssertionError("Premium geometry is lost inside actual ray-hit room/gallery composite at "+dose);
            save(full,label+"-premium-inside-full-"+(int)dose+".png");save(disabled,label+"-premium-inside-disabled-"+(int)dose+".png");
            float[] delta=new float[full.length];for(int i=0;i<delta.length;i++)delta[i]=i%4==3?1:Math.abs(full[i]-disabled[i])*5;save(delta,label+"-premium-inside-difference5x-"+(int)dose+".png");
            System.out.printf("CHECK: premium expandedMaterial on actual ray-hit inner walls %.0fmg; retained inside contribution %.6f%n",dose,insideContribution);
        }
        return frames;
    }
    static int safeSampling(String source) {
        String probe=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)","void artistMain()")
            .replaceFirst("#version 150", "#version 150\nuniform vec4 TestFallback;\nuniform vec4 TestCandidate;\nuniform vec4 TestSurface;\nuniform vec4 TestNormal;")
            + "\nvoid main() { fragColor=vec4(safeSource(TestFallback.xy,TestCandidate.xyz,TestSurface.xyz,TestNormal.xyz),0.0,1.0); }\n";
        int p=program(probe,"production depth-safe source probes");Eye eye=eye(0,0);
        float[] color=new float[width*height*4],depth=new float[width*height];
        for(int i=3;i<color.length;i+=4)color[i]=1;
        Vector3f surface=planePoint(eye,0.49f,0.5f,-3),candidate=planePoint(eye,0.51f,0.5f,-3);
        double maximum=0;
        for(int variant=0;variant<5;variant++) {
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                float z=x>=width/2 && variant==1?-1:x>=width/2&&variant==3?-6:-3;
                Vector3f point=planePoint(eye,(x+0.5f)/width,(y+0.5f)/height,z);
                Vector4f clip=eye.vp.transform(new Vector4f(new Vector3f(point).sub(eye.position),1));
                depth[y*width+x]=x>=width/2&&variant==2?1:clip.z/clip.w*0.5f+0.5f;
            }
            upload(new Scene(color,depth,new int[width*height]));setup(p,eye);
            Vector3f target=variant==4?new Vector3f(eye.position).add(0,0,1):candidate;
            vec(p,"TestFallback",0.49f,0.5f,0,0);vec(p,"TestSurface",surface.x,surface.y,surface.z,0);
            vec(p,"TestNormal",0,0,1,0);vec(p,"TestCandidate",target.x,target.y,target.z,0);
            float[] pixels=draw();float expected=variant==0?0.51f:0.49f;
            maximum=Math.max(maximum,Math.max(Math.abs(pixels[0]-expected),Math.abs(pixels[1]-0.5f)));
        }
        if(maximum>0.000003)throw new AssertionError("Depth-safe source guard failed: "+maximum);
        System.out.printf("PASS: production safeSource accepts coplanar shifts and rejects foreground, sky, other-depth wall, behind-camera; max UV error %.8f%n",maximum);
        return 5;
    }
    static Vector3f planePoint(Eye eye,float u,float v,float z) {
        Vector4f clip=eye.inverse.transform(new Vector4f(u*2-1,v*2-1,0,1));
        Vector3f ray=new Vector3f(clip.x,clip.y,clip.z).div(clip.w);
        return ray.mul((z-eye.position.z)/ray.z).add(eye.position);
    }
    /** Canonical fixture formats: established tablet calibration, then 500mg units.
     * Physical content and calibrated effects are intentionally separate inputs. */
    static double effectForContent(double content) {
        if (!Double.isFinite(content) || content <= 0) return 0;
        if (content <= 25) return content / 2.5;
        if (content <= 100) return 10 + (content - 25) / 7.5;
        if (content <= 250) return 20 + (content - 100) / 7.5;
        if (content <= 500) return 40 + (content - 250) / 6.25;
        return content / 6.25;
    }
    static double[] channels(TwoCeVisualDoseResponse value) {
        return new double[]{value.surfaceComplexity(),value.architecturalDepth(),value.perspectiveComplexity(),value.immersiveGeometry()};
    }
    static void doseResponseChecks() {
        double[] previous = new double[4];
        for (float content : DISPLAY_DOSES) {
            double dose=effectForContent(content);
            double effective=TwoCeVisualDoseResponse.effectiveContentMilligrams(content,dose,dose);
            if(Math.abs(effective-content)>0.000001)throw new AssertionError("Displayed peak content mapping drifted");
            double[] response=channels(TwoCeVisualDoseResponse.resolve(content,dose,dose,1));
            for(int index=0;index<4;index++) {
                if(!Double.isFinite(response[index]) || response[index]<0 || response[index]>1)throw new AssertionError("Nonfinite/out-of-range production response");
                if(response[index]<previous[index])throw new AssertionError("Dose stage regressed at "+content);
                if(content<=250 && response[index]!=0)throw new AssertionError("New layers enabled at established dose");
                if(content>=3750 && response[index]<=previous[index])throw new AssertionError("High-end progression plateaued at "+content);
            }
            System.out.printf("CHECK: production response %.0fmg / %.1f calibrated -> (%.6f,%.6f,%.6f,%.6f)%n",content,dose,response[0],response[1],response[2],response[3]);
            previous=response;
        }
        for(double[] state:new double[][]{{5000,0,800,1},{5000,800,800,0},{250,800,800,1},{5000,20,800,1},{Double.NaN,800,800,1},{Double.POSITIVE_INFINITY,800,800,1},{5000,Double.NaN,800,1},{5000,800,Double.POSITIVE_INFINITY,1},{5000,800,800,Double.NaN}}) {
            for(double value:channels(TwoCeVisualDoseResponse.resolve(state[0],state[1],state[2],state[3])))if(value!=0)throw new AssertionError("Inactive, weak, low-dose or invalid state retained high-dose layer");
        }
        for(double envelope:new double[]{0.001,0.05,0.16,0.5,1}) {
            double[] value=channels(TwoCeVisualDoseResponse.resolve(5000,800,800,envelope));
            for(int i=0;i<4;i++)if(Math.abs(value[i]-previous[i]*envelope)>0.000000001)throw new AssertionError("Phase envelope stopped scaling high layers");
        }
        for(double value:channels(TwoCeVisualDoseResponse.resolve(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE,1)))if(!Double.isFinite(value)||value<0||value>1)throw new AssertionError("Finite extreme response failed");
        double toleranceContent=TwoCeVisualDoseResponse.effectiveContentMilligrams(5000,400,800);
        if(toleranceContent!=2500)throw new AssertionError("Effective content ignored tolerance/absorption");
        System.out.println("PASS: exact production numeric response; displayed/calibrated mapping, high-dose layers off through250mg, monotonic stages through5000mg, active/target scaling, envelope tail, invalid/extreme values");
    }
    static double[] raw(TwoCeVisualPresentation.Channels value) {
        return new double[]{value.visualPresence(),value.cavities(),value.nestedRooms(),value.tessellation(),value.edgeLock(),value.perspectiveFold(),value.recursion(),value.corridorZoom(),value.symmetryLock(),value.geometricTrails()};
    }
    static double[] raw(TwoCeVisualPresentation value) {
        return new double[]{value.visualPresence(),value.cavities(),value.nestedRooms(),value.tessellation(),value.edgeLock(),value.perspectiveFold(),value.recursion(),value.corridorZoom(),value.symmetryLock(),value.geometricTrails()};
    }
    static void presentationChecks() {
        double[] previous=new double[4];
        for(float content:DISPLAY_DOSES) {
            var mapped=presentation(content,1200,1);double[] extra=extras(mapped.extraDose());
            if(mapped.virtualContentMilligrams()!=content*10 || Math.abs(mapped.virtualEffectMilligrams()-effectForContent(content*10))>0.000001)throw new AssertionError("Physical-to-visual content calibration drifted");
            for(int index=0;index<4;index++) {
                if(!Double.isFinite(extra[index])||extra[index]<0||extra[index]>1)throw new AssertionError("Invalid premium channel");
                if(extra[index]<previous[index])throw new AssertionError("Premium dose stage regressed");
                if(content<=500&&extra[index]!=0)throw new AssertionError("Premium layer opened at/below500mg anchor");
                if(content>=4000&&extra[index]<=previous[index])throw new AssertionError("Premium stage plateaued at "+content);
            }
            System.out.printf("CHECK: presentation %.0fmg -> %.0f virtual content/%.1f visual effect; premium (%.6f,%.6f,%.6f,%.6f)%n",content,mapped.virtualContentMilligrams(),mapped.virtualEffectMilligrams(),extra[0],extra[1],extra[2],extra[3]);
            previous=extra;
        }
        double[] oldAnchor=raw(fixtureChannels(5000,1200,1)),newAnchor=raw(presentation(500,1200,1));
        for(int index=0;index<oldAnchor.length;index++)if(Math.abs(oldAnchor[index]-newAnchor[index])>0.000000001)throw new AssertionError("New500mg does not reproduce previous5000mg raw visual channel "+index);
        for(double envelope:new double[]{0,0.001,0.05,0.16,0.5,1}) {
            var mapped=presentation(5000,1200,envelope);double[] extra=extras(mapped.extraDose());
            for(int index=0;index<4;index++)if(Math.abs(extra[index]-previous[index]*envelope)>0.000000001)throw new AssertionError("Premium phase-envelope fade changed");
            if(envelope==0)for(double value:raw(mapped))if(value!=0)throw new AssertionError("Zero-envelope promoted field retained a tail");
        }
        var initial=TwoCeVisualPresentation.resolve(500,80,80,1,1,1,fixtureChannels(500,1200,1));
        var justStacked=TwoCeVisualPresentation.resolve(1000,80,160,1,1,1,fixtureChannels(500,1200,1));
        if(initial.virtualContentMilligrams()!=justStacked.virtualContentMilligrams())throw new AssertionError("Same-format redose immediately changed absorbed presentation");
        double[] before=raw(initial),stacked=raw(justStacked);
        for(int index=0;index<before.length;index++)if(Math.abs(before[index]-stacked[index])>0.000000001)throw new AssertionError("Same-format redose immediately changed visual channel "+index);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,0}) {
            var mapped=TwoCeVisualPresentation.resolve(invalid,80,80,1,1,1,fixtureChannels(500,1200,1));
            for(double value:raw(mapped))if(value!=0)throw new AssertionError("Invalid content reached promoted field");
            for(double value:extras(mapped.extraDose()))if(value!=0)throw new AssertionError("Invalid content reached premium layer");
        }
        var extreme=presentation(Double.MAX_VALUE,1200,1);
        for(double value:raw(extreme))if(!Double.isFinite(value)||value<0)throw new AssertionError("Finite extreme visual presentation failed");
        for(double value:extras(extreme.extraDose()))if(!Double.isFinite(value)||value<0||value>1)throw new AssertionError("Finite extreme premium presentation failed");
        System.out.println("PASS: exact production presentation; new500mg=previous5000mg channels, premium progression, phase fade, same-format redose continuity and invalid/extreme inputs");
    }
    static int comfort(int p,Scene scene,Eye eye,String label) throws Exception {
        upload(scene);setup(p,eye);uniforms(p,5000,1200,0.86f,0.92f,0.7f);
        float[] full=draw();
        uniforms(p,5000,1200,0.22f,0.38f,0.7f);float[] reduced=draw();
        save(reduced,label+"-comfort-5000.png");
        if(regionDifference(scene.color,reduced,scene.material,-1)>0.00005)throw new AssertionError("Comfort profile changed protected near geometry");
        uniforms(p,5000,1200,0,0.38f,0.7f);float[] frozen=draw();float[] repeat=draw();
        if(maximumDifference(frozen,repeat)>0.000001)throw new AssertionError("Zero-motion fixed clock nondeterministic");
        vec(p,"HighDose",0,0,0,0);float[] frozenBase=draw();
        if(difference(frozen,frozenBase)<0.000001)throw new AssertionError("Reduced-motion comfort removed static high-dose detail");
        System.out.printf("CHECK: %s 5000mg comfort profile mean delta %.6f; zero-motion static additive detail %.6f%n",label,difference(full,reduced),difference(frozen,frozenBase));
        return 5;
    }
    static int separateLayers(int p,Scene scene,Eye eye,String label) throws Exception {
        upload(scene);setup(p,eye);uniforms(p,5000,1200,0.86f,0.92f,0.7f);
        vec(p,"HighDose",0,0,0,0);vec(p,"ExtraDose",0,0,0,0);float[] base=draw();
        double[] layerValues=channels(presentation(5000,1200,1).highDose());
        String[] names={"surface","architecture","perspective","immersion"};
        for(int layer=0;layer<4;layer++) {
            float[] values=new float[4];values[layer]=(float)layerValues[layer];
            vec(p,"HighDose",values[0],values[1],values[2],values[3]);
            float[] result=draw();save(result,label+"-isolated-"+names[layer]+".png");
            double contribution=difference(base,result);
            if(contribution<0.000001)throw new AssertionError("Independent "+names[layer]+" channel contributes no visible pixels in "+label);
            if(regionDifference(scene.color,result,scene.material,-1)>0.00005)throw new AssertionError("Independent layer changed near protection");
            System.out.printf("CHECK: %s independent %s channel contribution %.6f%n",label,names[layer],contribution);
        }
        return 5;
    }
    static int separateExtraLayers(int p,Scene scene,Eye eye,String label) throws Exception {
        upload(scene);setup(p,eye);uniforms(p,5000,1200,0.86f,0.92f,0.7f);
        vec(p,"ExtraDose",0,0,0,0);float[] base=draw();
        double[] layerValues=extras(presentation(5000,1200,1).extraDose());
        String[] names={"recursive-vaults","impossible-perspective","material-morphogenesis","immersive-lattice"};
        for(int layer=0;layer<4;layer++) {
            float[] values=new float[4];values[layer]=(float)layerValues[layer];
            vec(p,"ExtraDose",values[0],values[1],values[2],values[3]);float[] result=draw();
            save(result,label+"-isolated-premium-"+names[layer]+".png");
            double contribution=difference(base,result);
            if(contribution<0.000001)throw new AssertionError("Independent premium "+names[layer]+" channel contributes no visible pixels in "+label);
            if(regionDifference(scene.color,result,scene.material,-1)>0.00005)throw new AssertionError("Premium layer changed near protection");
            System.out.printf("CHECK: %s independent premium %s channel contribution %.6f%n",label,names[layer],contribution);
        }
        return 5;
    }
    static int continuity(int p,Scene scene,Eye eye,float dose) throws Exception {
        upload(scene);setup(p,eye);uniforms(p,dose,0,0.86f,0.92f,0.5f);
        float[] reference=draw();setTime(p,96000);float[] wrap=draw();
        double wrapMean=difference(reference,wrap),wrapMaximum=maximumDifference(reference,wrap);
        if(wrapMean>0.00002 || wrapMaximum>0.002)throw new AssertionError("4800s clock seam: mean "+wrapMean+", max "+wrapMaximum);
        System.out.printf("PASS: %.0f displayed mg, 4800-second shader clock seam; mean %.8f, max %.8f%n",dose,wrapMean,wrapMaximum);
        double originMean=0,originMaximum=0,originLargeFraction=0;float[] worst=null;
        for(Vector3f offset:List.of(new Vector3f(4096,0,0),new Vector3f(-4096,0,0),new Vector3f(0,4096,0),new Vector3f(0,0,-4096),new Vector3f(4096,4096,4096))) {
            Eye translated=new Eye(new Vector3f(eye.position).add(offset),eye.vp,eye.inverse,eye.rotation);
            setup(p,translated);uniforms(p,dose,0,0.86f,0.92f,0.5f);
            vec(p,"WorldHeadPosition",offset.x,1.72f+offset.y,4.7f+offset.z,0);
            vec(p,"Focus",0.1f+offset.x,2+offset.y,-10+offset.z,0.5f);
            float[] translatedResult=draw();double mean=difference(reference,translatedResult),maximum=maximumDifference(reference,translatedResult);
            int changed=0,large=0;for(int i=0;i<reference.length;i+=4){double pixel=Math.max(Math.abs(reference[i]-translatedResult[i]),Math.max(Math.abs(reference[i+1]-translatedResult[i+1]),Math.abs(reference[i+2]-translatedResult[i+2])));if(pixel>0.002)changed++;if(pixel>0.05)large++;}
            System.out.printf("CHECK: origin (%g,%g,%g), mean %.8f, max %.6f; RGB pixel differences >.002 %.4f%%, >.05 %.4f%%%n",offset.x,offset.y,offset.z,mean,maximum,100.0*changed/(width*height),100.0*large/(width*height));
            if(maximum>originMaximum)worst=translatedResult;
            originMean=Math.max(originMean,mean);originMaximum=Math.max(originMaximum,maximum);
            originLargeFraction=Math.max(originLargeFraction,(double)large/(width*height));
        }
        save(reference,"origin-reference-"+(int)dose+".png");save(worst,"origin-translated-"+(int)dose+".png");float[] delta=new float[reference.length];for(int i=0;i<delta.length;i++)delta[i]=i%4==3?1:Math.abs(reference[i]-worst[i])*10;save(delta,"origin-difference-10x-"+(int)dose+".png");
        if(originMean>0.0015 || originLargeFraction>0.0025)throw new AssertionError("4096-block origin seam: mean "+originMean+", >.05 fraction "+originLargeFraction+", max "+originMaximum);
        System.out.printf("PASS: positive/negative 4096-block origin translations; maximum mean %.8f, maximum pixel %.6f (float reconstruction/derivatives included)%n",originMean,originMaximum);
        return 7;
    }
    static int highResolution(int p,Eye eye) {
        float[] color=new float[width*height*4],depth=new float[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int i=y*width+x;Vector3f point=planePoint(eye,(x+0.5f)/width,(y+0.5f)/height,eye.position.z-1.3f);
            Vector4f clip=eye.vp.transform(new Vector4f(new Vector3f(point).sub(eye.position),1));depth[i]=clip.z/clip.w*0.5f+0.5f;
            color[i*4]=0.25f+0.35f*x/width;color[i*4+1]=0.20f+0.45f*y/height;color[i*4+2]=0.30f;color[i*4+3]=1;
        }
        upload(new Scene(color,depth,new int[width*height]));setup(p,eye);uniforms(p,0,1234,1,1,0.5f);
        glUniform2f(glGetUniformLocation(p,"OutSize"),3840,2160);glViewport(0,0,3840,2160);
        float[] neutral=draw();uniforms(p,5000,1234,1,1,0.5f);float[] active=draw();
        glViewport(0,0,width,height);
        double change=difference(neutral,active);if(change<0.001)throw new AssertionError("4K near-wall effect vanished due normal footprint: "+change);
        System.out.printf("PASS: 3840x2160 pixel footprint, cropped near wall at1.3 blocks; active mean %.6f (finite, resolvable relief retained)%n",change);
        return 2;
    }
    static void setTime(int p,float time) {int location=glGetUniformLocation(p,"WarpA");float[] value=new float[4];glGetUniformfv(p,location,value);value[3]=time;glUniform4fv(location,value);}
    static void setup(int p,Eye eye) {
        glUseProgram(p); glUniform2f(glGetUniformLocation(p,"OutSize"),width,height);
        glUniform1i(glGetUniformLocation(p,"DiffuseSampler"),0); glUniform1i(glGetUniformLocation(p,"DepthSampler"),1);
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,colorTexture);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,depthTexture);
        glUniformMatrix4fv(glGetUniformLocation(p,"WorldViewProjection"),false,eye.vp.get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(p,"WorldInverseViewProjection"),false,eye.inverse.get(new float[16]));
        vec(p,"WorldEyePosition",eye.position.x,eye.position.y,eye.position.z,0);
        vec(p,"WorldHeadPosition",0,1.72f,4.7f,0);
    }
    static void uniforms(int p,float content,float time,float motion,float color,float focus) {
        uniforms(p,content,time,motion,color,focus,1,true);
    }
    static void uniforms(int p,float content,float time,float motion,float color,float focus,double envelope,boolean highDose) {
        double dose=effectForContent(content);
        double presence=(dose>0?1-Math.exp(-Math.log1p(dose/8)*1.55*amplification(dose)*1.85):0)*envelope;
        double amp=presence*amplitude(dose), t2=smooth(8,20,dose),t3=smooth(18,40,dose),t4=smooth(35,80,dose);
        double machine=Math.log1p(Math.max(0,(double)dose-70)/18);
        float low=clamp((float)((dose-4)/8)),mid=clamp((float)((dose-12)/12)),high=clamp((float)((dose-24)/20));
        double seedPhase=SEED*Math.PI*2;
        double modulation=(motion<=0.22f?motion:Math.min(1,motion/0.86f))*(color<=0.38f?0.25:1);
        double cavities=amp*(0.72+t2*0.40+t3*0.28)*(0.98+Math.sin(time*0.0024+(FIXTURE_SEED&511)*0.01)*0.02);
        double nested=amp*(0.32+t2*0.50+t3*0.45+t4*0.28),tess=amp*(0.58+t2*0.40+t3*0.28),edge=amp*(0.74+t2*0.35+t3*0.22);
        double fold=amp*(0.22+t2*0.40+t3*0.50+t4*0.40),recursive=amp*(t2*0.28+t3*0.55+t4*0.80+machine*0.60),corridor=amp*(0.22+t2*0.42+t3*0.55+t4*0.50+machine*0.45);
        double symmetry=presence*(t2*0.16+t3*0.42+t4*0.55+machine*0.35),trails=amp*(0.38+t2*0.40+t3*0.30);
        TwoCeVisualDoseResponse layers=TwoCeVisualDoseResponse.resolve(content,dose,dose,envelope);
        TwoCeVisualPresentation.ExtraDose extra=new TwoCeVisualPresentation.ExtraDose(0,0,0,0);
        if(p!=baselineProgram) {
            var mapped=presentation(content,time,envelope);
            presence=mapped.visualPresence();cavities=mapped.cavities();nested=mapped.nestedRooms();
            tess=mapped.tessellation();edge=mapped.edgeLock();fold=mapped.perspectiveFold();
            recursive=mapped.recursion();corridor=mapped.corridorZoom();symmetry=mapped.symmetryLock();trails=mapped.geometricTrails();
            low=(float)mapped.gate10();mid=(float)mapped.gate20();high=(float)mapped.gate40();machine=mapped.machineLift();
            layers=mapped.highDose();extra=mapped.extraDose();
        }
        vec(p,"WarpA",(float)(presence*1.12),compressed(cavities*(0.46+mid*0.38+high*0.16))*living(time,seedPhase,0,modulation),compressed(nested*(0.42+mid*0.40+high*0.18))*living(time,seedPhase,1.7,modulation),time);
        vec(p,"WarpB",compressed(tess*1.10)*living(time,seedPhase,3.2,modulation),compressed(edge*0.90),compressed(fold*(0.45+mid*0.35+high*0.20))*living(time,seedPhase,4.9,modulation),compressed(recursive*(0.60+high*0.40))*living(time,seedPhase,2.4,modulation));
        vec(p,"WarpC",compressed(corridor*(0.65+mid*0.25+high*0.10))*living(time,seedPhase,5.8,modulation),SEED,focus,compressed(symmetry));
        vec(p,"Intensity",low,mid,high,(float)machine);
        vec(p,"Control",color,motion,0.85f,compressed(trails));
        vec(p,"MotionA",0.03f,0,-0.01f,motion); vec(p,"MotionB",0.001f,0.002f,0,color);
        vec(p,"Focus",0.1f,2,-10,focus); vec(p,"MixA",0.15f,0.08f,0.10f,0.12f); vec(p,"MusicA",0,0,0,0);
        vec(p,"HighDose",highDose?(float)layers.surfaceComplexity():0,highDose?(float)layers.architecturalDepth():0,highDose?(float)layers.perspectiveComplexity():0,highDose?(float)layers.immersiveGeometry():0);
        vec(p,"ExtraDose",highDose?(float)extra.recursiveVaults():0,highDose?(float)extra.impossiblePerspective():0,highDose?(float)extra.materialMorphogenesis():0,highDose?(float)extra.immersiveLattice():0);
    }
    static TwoCeVisualPresentation.Channels fixtureChannels(double content,double time,double envelope) {
        double dose=effectForContent(content),peak=TwoCeVisualPresentation.peakIntensity(dose,1);
        double presence=(dose>0?-Math.expm1(-peak):0)*envelope;
        double amp=presence*amplitude(dose),t2=smooth(8,20,dose),t3=smooth(18,40,dose),t4=smooth(35,80,dose);
        double machine=Math.log1p(Math.max(0,dose-70)/18);
        double cavities=amp*(0.72+t2*0.40+t3*0.28)*(0.98+Math.sin(time*0.0024+(FIXTURE_SEED&511)*0.01)*0.02);
        return new TwoCeVisualPresentation.Channels(peak*envelope,presence,cavities,
                amp*(0.32+t2*0.50+t3*0.45+t4*0.28),amp*(0.58+t2*0.40+t3*0.28),amp*(0.74+t2*0.35+t3*0.22),
                amp*(0.22+t2*0.40+t3*0.50+t4*0.40),amp*(t2*0.28+t3*0.55+t4*0.80+machine*0.60),
                amp*(0.22+t2*0.42+t3*0.55+t4*0.50+machine*0.45),
                presence*(t2*0.16+t3*0.42+t4*0.55+machine*0.35),amp*(0.38+t2*0.40+t3*0.30));
    }
    static TwoCeVisualPresentation presentation(double content,double time,double envelope) {
        double dose=effectForContent(content);
        return TwoCeVisualPresentation.resolve(content,dose,dose,1,1,1,fixtureChannels(content,time,envelope));
    }
    static double[] extras(TwoCeVisualPresentation.ExtraDose value) {
        return new double[]{value.recursiveVaults(),value.impossiblePerspective(),value.materialMorphogenesis(),value.immersiveLattice()};
    }
    static double amplification(double d) { return 1+0.50*smooth(8,16,d)+0.85*smooth(16,28,d)+1.15*smooth(32,50,d)+1.80*smooth(65,100,d); }
    static double amplitude(double d) { return Math.log1p(d/8)/Math.log(6)*amplification(d)*1.85; }
    static float compressed(double value) { return (float)(1-1/(1+Math.max(0,value)*1.45)); }
    static float living(double time,double phase,double offset,double modulation) {return (float)(0.94+modulation*(Math.sin(time*0.05*Math.PI*2*145/4800+phase+offset)*0.045+Math.sin(time*0.05*Math.PI*2*63/4800+phase*1.7-offset*0.6)*0.015));}
    static double smooth(double a,double b,double v) { double t=Math.max(0,Math.min(1,(v-a)/(b-a)));return t*t*(3-2*t); }
    static float clamp(float x) { return Math.max(0,Math.min(1,x)); }
    static void vec(int p,String n,float a,float b,float c,float d) { glUniform4f(glGetUniformLocation(p,n),a,b,c,d); }
    static int program(String source,String label) {
        int vertex=shader(GL_VERTEX_SHADER,VERTEX,label+" vertex"), fragment=shader(GL_FRAGMENT_SHADER,source,label);
        int p=glCreateProgram();glAttachShader(p,vertex);glAttachShader(p,fragment);glBindAttribLocation(p,0,"Position");glBindFragDataLocation(p,0,"fragColor");glLinkProgram(p);
        if(glGetProgrami(p,GL_LINK_STATUS)==GL_FALSE)throw new AssertionError(label+" link: "+glGetProgramInfoLog(p));
        glDeleteShader(vertex);glDeleteShader(fragment);return p;
    }
    static int shader(int type,String source,String label) {int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==GL_FALSE)throw new AssertionError(label+" compile: "+glGetShaderInfoLog(s));return s;}
    static int texture(int internal,int format,float[] data) {
        int t=glGenTextures();glBindTexture(GL_TEXTURE_2D,t);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);glTexImage2D(GL_TEXTURE_2D,0,internal,width,height,0,format,GL_FLOAT,data);return t;
    }
    static void upload(Scene scene) {glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,colorTexture);glTexSubImage2D(GL_TEXTURE_2D,0,0,0,width,height,GL_RGBA,GL_FLOAT,scene.color);glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,depthTexture);glTexSubImage2D(GL_TEXTURE_2D,0,0,0,width,height,GL_RED,GL_FLOAT,scene.depth);}
    static float[] draw() {
        glDrawArrays(GL_TRIANGLES,0,3);float[] data=new float[width*height*4];glReadPixels(0,0,width,height,GL_RGBA,GL_FLOAT,data);
        for(int i=0;i<data.length;i++) { if(!Float.isFinite(data[i]))throw new AssertionError("Non-finite output at "+i);if(i%4==3&&Math.abs(data[i]-1)>0.000001)throw new AssertionError("Alpha changed"); }
        return data;
    }
    static void save(float[] data,String name) throws Exception {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {int i=(y*width+x)*4;image.setRGB(x,height-y-1,Math.round(clamp(data[i])*255)<<16|Math.round(clamp(data[i+1])*255)<<8|Math.round(clamp(data[i+2])*255));}
        ImageIO.write(image,"PNG",output.resolve(name).toFile());
    }
    static double difference(float[] a,float[] b) {double sum=0;for(int i=0;i<a.length;i++)sum+=Math.abs(a[i]-b[i]);return sum/a.length;}
    static double maximumDifference(float[] a,float[] b) {double maximum=0;for(int i=0;i<a.length;i++)maximum=Math.max(maximum,Math.abs(a[i]-b[i]));return maximum;}
    static double regionDifference(float[] a,float[] b,int[] mask,int region) {double sum=0;int count=0;for(int i=0;i<mask.length;i++)if(mask[i]==region){for(int c=0;c<3;c++)sum+=Math.abs(a[i*4+c]-b[i*4+c]);count+=3;}return count==0?0:sum/count;}
    static void loadTextures(Path jar) throws Exception {
        try(ZipFile zip=new ZipFile(jar.toFile())) {for(String name:List.of("oak_planks","cobblestone","stone_bricks","oak_log","grass_block_top","oak_leaves","orange_terracotta","bricks")){var entry=zip.getEntry("assets/minecraft/textures/block/"+name+".png");if(entry==null)throw new AssertionError("Missing texture "+name);albedo.put(name,ImageIO.read(zip.getInputStream(entry)));}}
    }
    static Scene fixture(Eye eye,boolean natural) {
        float[] color=new float[width*height*4],depth=new float[width*height];int[] material=new int[width*height];
        List<Box> boxes=new ArrayList<>();
        if(natural) {
            boxes.add(box(-40,-1,-40,40,0,40,"grass_block_top"));
            for(int k=0;k<7;k++){float x=-7+k*2.4f,z=-4-(k%3)*3;boxes.add(box(x,0,z,x+1,5,z+1,"oak_log"));boxes.add(box(x-1,3,z-1,x+2,6,z+2,"oak_leaves"));}
            boxes.add(box(-2,0,-2,0,1,0,"cobblestone"));
        } else {
            boxes.add(box(-40,-1,-40,40,0,40,"oak_planks"));
            boxes.add(box(-7,0,-12,7,2,-11,"cobblestone"));
            boxes.add(box(-7,2,-12,7,6,-11,"stone_bricks"));
            boxes.add(box(-7,0,-12,-6,5,12,"cobblestone"));boxes.add(box(6,0,-12,7,5,12,"cobblestone"));
            for(float x:new float[]{-5,-2,2,5}){boxes.add(box(x,0,-9,x+1,6,-8,"oak_log"));boxes.add(box(x-0.2f,5,-9,x+1.2f,6,-3,"oak_planks"));}
            boxes.add(box(-5,0,-3,-3,2,-1,"bricks"));boxes.add(box(3,0,-4,5,1,-2,"oak_planks"));
        }
        // This cube is in the hand/near-protection range, ahead of the eye.
        Vector3f near=eye.rotation.transformDirection(new Vector3f(0.105f,-0.07f,-0.18f)).add(eye.position);
        boxes.add(new Box(new Vector3f(near).sub(0.028f,0.028f,0.028f),new Vector3f(near).add(0.028f,0.028f,0.028f),"orange_terracotta"));
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int i=y*width+x;Vector4f nearClip=eye.inverse.transform(new Vector4f((x+0.5f)/width*2-1,(y+0.5f)/height*2-1,0,1));
            Vector3f ray=new Vector3f(nearClip.x,nearClip.y,nearClip.z).div(nearClip.w).normalize();
            Hit hit=null;for(Box b:boxes){Hit current=intersect(eye.position,ray,b);if(current!=null&&(hit==null||current.distance<hit.distance))hit=current;}
            Vector3f rgb;
            if(hit==null){depth[i]=1;material[i]=0;float t=clamp(ray.y*0.8f+0.4f);rgb=new Vector3f(0.61f,0.78f,0.91f).lerp(new Vector3f(0.25f,0.49f,0.83f),t);}
            else {
                Vector4f clip=eye.vp.transform(new Vector4f(new Vector3f(hit.position).sub(eye.position),1));depth[i]=clip.z/clip.w*0.5f+0.5f;
                material[i]=hit.texture.equals("orange_terracotta")?-1:1;
                Vector3f normal=hit.normal;float u,v;
                if(Math.abs(normal.y)>0.5){u=hit.position.x;v=hit.position.z;}else if(Math.abs(normal.x)>0.5){u=hit.position.z;v=-hit.position.y;}else{u=hit.position.x;v=-hit.position.y;}
                BufferedImage tex=albedo.get(hit.texture);int tx=(int)((u-Math.floor(u))*tex.getWidth()),ty=(int)((v-Math.floor(v))*tex.getHeight());int sample=tex.getRGB(tx,ty);
                rgb=new Vector3f((sample>>16&255)/255f,(sample>>8&255)/255f,(sample&255)/255f);
                if(hit.texture.equals("grass_block_top"))rgb.mul(0.52f,0.82f,0.28f);if(hit.texture.equals("oak_leaves"))rgb.mul(0.34f,0.70f,0.22f);
                float light=0.64f+0.30f*Math.max(0,new Vector3f(0.35f,0.8f,0.48f).normalize().dot(normal));rgb.mul(light);
                rgb.lerp(new Vector3f(0.58f,0.75f,0.88f),(float)smooth(20,80,hit.distance)*0.4f);
            }
            color[i*4]=rgb.x;color[i*4+1]=rgb.y;color[i*4+2]=rgb.z;color[i*4+3]=1;
        }
        return new Scene(color,depth,material);
    }
    static Box box(float a,float b,float c,float d,float e,float f,String texture){return new Box(new Vector3f(a,b,c),new Vector3f(d,e,f),texture);}
    static Hit intersect(Vector3f origin,Vector3f direction,Box box) {
        float minimum=-Float.MAX_VALUE,maximum=Float.MAX_VALUE;int normalAxis=0;float normalSign=0;
        for(int axis=0;axis<3;axis++){float o=origin.get(axis),d=direction.get(axis);if(Math.abs(d)<1e-8){if(o<box.minimum.get(axis)||o>box.maximum.get(axis))return null;continue;}float a=(box.minimum.get(axis)-o)/d,b=(box.maximum.get(axis)-o)/d;float sign=-1;if(a>b){float t=a;a=b;b=t;sign=1;}if(a>minimum){minimum=a;normalAxis=axis;normalSign=sign;}maximum=Math.min(maximum,b);if(maximum<minimum)return null;}
        if(minimum<0.05f||minimum>95)return null;Vector3f point=new Vector3f(direction).mul(minimum).add(origin),normal=new Vector3f().setComponent(normalAxis,normalSign);return new Hit(minimum,point,normal,box.texture);
    }
}
