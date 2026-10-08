import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Production GPU shaders on perspective-correct dark world surfaces; no game/headset control. */
public final class ValidateLsdShaders {
    private static final int W=384,H=256;
    private static final float NEAR=.05f,FAR=96,TAN=(float)Math.tan(Math.toRadians(37.5));
    private static final long SEED=0x4C5344464C4FL;
    private static int colorTexture,depthTexture;
    private static Path outputDirectory,nativeDirectory;
    private record Scene(float[] source,float x,float y,float z) { }
    private record Signature(long seed,float[][] palette,double edge,double geometry,double sky,double blocks,
                             double liquid,double melt,double tracer,double echo,double prism,int dominant,int skyStyle) { }
    private record State(double dose,double intensity,double[] channels,Signature signature,String provenance,float[] environment,float[] shaderChannels) {
        private State(double dose,double intensity,double[] channels,Signature signature,String provenance) {
            // PsilocybinEnvironmentSnapshot.empty() is lit and still, rather than
            // having zero light; the native session uses that same default.
            this(dose,intensity,channels,signature,provenance,new float[]{0,.28f,0,0,0},null);
        }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        Path root=Path.of(args[0]);
        outputDirectory=root.resolve("build/lsd-shader-validation");Files.createDirectories(outputDirectory);
        nativeDirectory=args.length>1&&!args[1].isBlank()?Path.of(args[1]):null;
        GLFWErrorCallback callback=GLFWErrorCallback.createPrint(System.err);callback.set();
        if(!glfwInit())throw new IllegalStateException("GLFW initialization failed");
        long window=NULL;
        try {
            glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_FOCUSED,GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,2);
            glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
            window=glfwCreateWindow(W,H,"LSD shader verification (hidden)",NULL,NULL);
            if(window==NULL)throw new IllegalStateException("Hidden GPU context unavailable");
            glfwMakeContextCurrent(window);GL.createCapabilities();
            System.out.println("GPU: "+glGetString(GL_RENDERER)+" / "+glGetString(GL_VERSION));
            System.out.println("Perspective world fixtures:6/24/40-block walls, obliqueD24 depth, dim bark/books/stone, origins0/4090");
            glBindVertexArray(glGenVertexArrays());glBindBuffer(GL_ARRAY_BUFFER,glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,0,3,-1,0,-1,3,0},GL_STATIC_DRAW);
            glVertexAttribPointer(0,3,GL_FLOAT,false,0,0L);glEnableVertexAttribArray(0);
            glBindFramebuffer(GL_FRAMEBUFFER,glGenFramebuffers());
            int output=texture(GL_RGBA32F,GL_RGBA,new float[W*H*4]);
            glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0);
            if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Incomplete output framebuffer");
            glViewport(0,0,W,H);glDisable(GL_BLEND);glDisable(GL_DEPTH_TEST);
            colorTexture=texture(GL_RGBA32F,GL_RGBA,new float[W*H*4]);depthTexture=texture(GL_R32F,GL_RED,new float[W*H]);
            Path directory=root.resolve("src/main/resources/assets/trippycraft/shaders/program");
            boolean candidateOnly=args.length>4&&args[4].equals("--candidate-only");
            if(!candidateOnly)for(String name:new String[]{"lsd_world_warp","lsd_world_warp_vr"}) {
                int program=compile(Files.readString(directory.resolve(name+".fsh")),name);
                verify(program,name);glDeleteProgram(program);
            }
            if(!candidateOnly&&args.length>2&&!args[2].isBlank()) {
                int baseline=compile(Files.readString(Path.of(args[2])),"previous installed LSD negative control");
                boolean rejected=false;
                try {
                    anchorTest(baseline,"previous installed LSD",4090);
                    grainTest(baseline,"previous installed LSD",24,4090);
                } catch(AssertionError expected) {
                    rejected=true;System.out.println("PASS negative control: previous installed field rejected: "+expected.getMessage());
                }
                glDeleteProgram(baseline);
                require(rejected,"Expanded anchoring/grain regression checks failed to distinguish the previous field");
            }
            if(args.length>3&&!args[3].isBlank()) {
                Path candidate=Path.of(args[3]);String name=candidate.getFileName().toString().replace(".fsh","");
                int program=compile(Files.readString(candidate),name);verify(program,name);glDeleteProgram(program);
                System.out.println("PASS TEMP candidate; production files were not changed: "+candidate);
            }
            require(glGetError()==GL_NO_ERROR,"OpenGL reported an error");
            System.out.println(candidateOnly
                    ? "PASS: TEMP candidate GPU execution; production files unchanged, not headset comfort"
                    : "PASS: production desktop/generatedVR execution; synthetic projections, not headset comfort");
        } finally {if(window!=NULL)glfwDestroyWindow(window);glfwTerminate();callback.free();}
    }
    private static int texture(int internal,int format,float[] data) {
        int id=glGenTextures();glBindTexture(GL_TEXTURE_2D,id);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D,0,internal,W,H,0,format,GL_FLOAT,data);return id;
    }
    private static int shader(int type,String source,String name) {
        int id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);
        require(glGetShaderi(id,GL_COMPILE_STATUS)!=GL_FALSE,name+" compilation: "+glGetShaderInfoLog(id));return id;
    }
    private static int compile(String source,String name) {
        int vertex=shader(GL_VERTEX_SHADER,"#version 150\nin vec3 Position;void main(){gl_Position=vec4(Position,1.0);}","verification vertex");
        int fragment=shader(GL_FRAGMENT_SHADER,source,name),program=glCreateProgram();
        glAttachShader(program,vertex);glAttachShader(program,fragment);glBindAttribLocation(program,0,"Position");glBindFragDataLocation(program,0,"fragColor");
        glLinkProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
        require(glGetProgrami(program,GL_LINK_STATUS)!=GL_FALSE,name+" link: "+glGetProgramInfoLog(program));return program;
    }
    private static void vec(int p,String name,float a,float b,float c,float d) {glUniform4f(glGetUniformLocation(p,name),a,b,c,d);}
    private static void matrix(int p,String name,Matrix4f m) {glUniformMatrix4fv(glGetUniformLocation(p,name),false,m.get(new float[16]));}
    private static void setup(int p,Scene scene) {
        glUseProgram(p);
        for(String name:new String[]{"InSize","OutSize","VrReferenceSize"})glUniform2f(glGetUniformLocation(p,name),W,H);
        glUniform1i(glGetUniformLocation(p,"DiffuseSampler"),0);glUniform1i(glGetUniformLocation(p,"DepthSampler"),1);
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,colorTexture);glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,depthTexture);
        Matrix4f projection=new Matrix4f().perspective((float)Math.toRadians(75),(float)W/H,NEAR,FAR),inverse=new Matrix4f(projection).invert();
        for(String name:new String[]{"WorldViewProjection","VrViewProjection","VrHeadViewProjection"})matrix(p,name,projection);
        for(String name:new String[]{"WorldInverseViewProjection","VrInverseViewProjection","VrInverseHeadViewProjection"})matrix(p,name,inverse);
        matrix(p,"VrHeadView",new Matrix4f());
        for(String name:new String[]{"WorldEyePosition","VrEyePosition","VrHeadPosition"})vec(p,name,scene.x,scene.y,scene.z,0);
        vec(p,"CameraA",scene.x,scene.y,scene.z,FAR);vec(p,"CameraB",0,0,-NEAR,NEAR);
        vec(p,"CameraC",NEAR*TAN*W/H,0,0,0);vec(p,"CameraD",0,NEAR*TAN,0,0);
    }
    private static Scene fixture(float cx,float cy,float cz,float distance,float sx,float sy,boolean neutral,float boardX) {
        float[] color=new float[W*H*4],depth=new float[W*H];
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            double rx=((x+.5)/W*2-1)*TAN*W/H,ry=((y+.5)/H*2-1)*TAN;
            double axial=distance/(1-sx*rx-sy*ry),localX=cx-boardX+rx*axial,localY=ry*axial;
            double raw=FAR/(FAR-NEAR)-FAR*NEAR/((FAR-NEAR)*axial);
            depth[y*W+x]=(float)(Math.rint(raw*16777215.0)/16777215.0);
            int at=(y*W+x)*4;
            if(neutral)color[at]=color[at+1]=color[at+2]=.18f;
            else {
                int material=Math.floorMod((int)Math.floor((localX+4.5)/3),3);
                double tx=localX-Math.floor(localX),ty=localY-Math.floor(localY);
                double fine=Math.sin(localX*19+Math.sin(localY*5))*Math.cos(localY*17)*.012;
                if(material==0) {
                    double bark=.058+.022*Math.sin(localX*23+Math.sin(localY*7)*.5)+fine;
                    color[at]=(float)(bark*1.12);color[at+1]=(float)(bark*.80);color[at+2]=(float)(bark*.57);
                } else if(material==1) {
                    int spine=Math.floorMod((int)Math.floor(localX*7),5);
                    double[] r={.29,.13,.17,.33,.19},g={.11,.25,.16,.24,.13},b={.08,.10,.28,.10,.18};
                    double line=(ty<.035||ty>.94||tx<.025)?.42:1;
                    color[at]=(float)((r[spine]+fine)*line);color[at+1]=(float)((g[spine]+fine)*line);color[at+2]=(float)((b[spine]+fine)*line);
                } else {
                    double stone=.16+.035*Math.sin(Math.floor(localX*16)*.51+Math.floor(localY*16)*1.23)+fine,seam=(tx<.045||ty<.045)?.38:1;
                    color[at]=(float)(stone*seam);color[at+1]=(float)(stone*.97*seam);color[at+2]=(float)(stone*.92*seam);
                }
            }
            color[at+3]=1;
        }
        upload(color,depth);return new Scene(color,cx,cy,cz);
    }
    private static void upload(float[] color,float[] depth) {
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,colorTexture);glTexSubImage2D(GL_TEXTURE_2D,0,0,0,W,H,GL_RGBA,GL_FLOAT,color);
        glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,depthTexture);glTexSubImage2D(GL_TEXTURE_2D,0,0,0,W,H,GL_RED,GL_FLOAT,depth);
    }
    private static Signature signature() {
        Random r=new Random(SEED^0x51D5D51D5D51D5DL);
        int[][] palettes={{0x00F0FF,0xFF33F8,0xF6FF00},{0x39FF14,0xB000FF,0xFF6A00},{0x00C8FF,0xFF007A,0x8DFFEA},{0xFF3DF2,0x39FF88,0xFFB000},
                {0x7A5CFF,0xFFEA00,0x00FFB8},{0xFF1744,0x00E5FF,0xD6FF00},{0xFF7A00,0x00FF66,0x7C4DFF},{0x00B8FF,0xFF2D95,0xB6FF3B},{0xE9FF70,0x2BFFEA,0xFF4FD8}};
        int[] choice=palettes[r.nextInt(9)];float[][] colors=new float[3][3];
        for(int p=0;p<3;p++)for(int c=0;c<3;c++)colors[p][c]=Math.max(0,Math.min(255,((choice[p]>>(16-c*8))&255)+r.nextInt(69)-34))/255f;
        return new Signature(SEED,colors,.55+r.nextDouble()*.45,.45+r.nextDouble()*.55,.20+r.nextDouble()*.80,.20+r.nextDouble()*.80,
                .18+r.nextDouble()*.82,.20+r.nextDouble()*.80,.35+r.nextDouble()*.65,.25+r.nextDouble()*.75,.30+r.nextDouble()*.70,r.nextInt(6),r.nextInt(5));
    }
    private static double smooth(double a,double b,double x) {double t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t);}
    private static double smoother(double a,double b,double x) {double t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*t*(t*(t*6-15)+10);}
    private static double peak(double d) {
        return .34*smooth(0,50,d)+.72*smooth(50,100,d)+1.45*smooth(100,200,d)+2.10*smooth(200,250,d)+2.45*smooth(250,800,d)
                +2.20*smooth(800,1296,d)+18*Math.log(Math.hypot(1,Math.max(0,(d-1296)/1296)));
    }
    private static State nativeState(double dose) {
        if(nativeDirectory!=null)try(var paths=Files.walk(nativeDirectory)) {
            Path snapshot=paths.filter(p->p.toString().endsWith(".json")&&p.getFileName().toString().contains("peak"))
                    .filter(p->{try{return Math.abs(number(Files.readString(p),"activeDoseMicrograms")-dose)<1e-8;}catch(Exception e){return false;}}).sorted().findFirst().orElse(null);
            if(snapshot!=null)return fromSnapshot(Files.readString(snapshot),snapshot.toString());
        }catch(Exception e){throw new RuntimeException("Cannot read requested native snapshots",e);}
        Signature s=signature();int duration=dose<=75?4200:dose<=175?7200:dose<=525?10400:13200,age=(int)(duration*.46);
        double envelope=1-smoother(.30,.62,age/(double)duration)*.04,t1=smooth(35,60,dose),t2=smooth(80,120,dose),t3=smooth(135,210,dose),t4=smooth(225,340,dose),t5=smooth(800,1000,dose);
        double ramp=(t1+t2+t3+t4+t5)*.20,amplitude=peak(dose)*envelope*(1+Math.sin(age*.0034+(s.seed&2047L)*.004)*.028+Math.sin(age*.0016+s.geometry*4)*(.012+ramp*.014));
        double[] c={amplitude*t1*(.56+s.edge*.78),amplitude*(t2*.38+t3*.35+t4*.35)*(.40+s.melt*.68+.16*.18),
                amplitude*(t3*.32+t4*.48+t5*.46)*(.44+s.geometry*.52+s.sky*.26),amplitude*(t2*.20+t3*.42+t4*.48+t5*.40)*(.42+s.blocks*.68),
                amplitude*(t2*.26+t3*.42+t4*.38+t5*.28)*(.42+s.liquid*.70),amplitude*(t1*.10+t2*.24+t3*.38+t4*.56+t5*.54)*(.38+s.sky*.72),
                amplitude*(t1*.18+t2*.30+t3*.42+t4*.46+t5*.40)*(.44+s.echo*.70),amplitude*t1*(.40+ramp*.74)*(.50+s.prism*.72),
                amplitude*t1*(.38+ramp*.52)*(.44+s.edge*.54+.16)};
        return new State(dose,amplitude,c,s,"exact default server curve/signature at46% peak; empty environment");
    }
    private static double number(String json,String key) {
        Matcher m=Pattern.compile("\""+Pattern.quote(key)+"\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?(?:[Ee][+-]?[0-9]+)?)").matcher(json);
        if(!m.find())throw new IllegalArgumentException("Missing native snapshot field "+key);return Double.parseDouble(m.group(1));
    }
    private static State fromSnapshot(String json,String path) {
        String sig=json.substring(json.indexOf("\"signature\""));long seed=(long)number(sig,"seed");
        float[][] p=new float[3][3];String[] palettes={"primary","secondary","accent"},colors={"Red","Green","Blue"};
        for(int a=0;a<3;a++)for(int c=0;c<3;c++)p[a][c]=(float)number(sig,palettes[a]+colors[c])/255;
        Signature s=new Signature(seed,p,number(sig,"electricSharpness"),number(sig,"geometricPrecision"),number(sig,"skyVortexBias"),number(sig,"blockAutonomyBias"),
                number(sig,"liquidBias"),number(sig,"meltFrequency"),number(sig,"tracerStrength"),number(sig,"entityEchoStrength"),number(sig,"prismSplit"),(int)number(sig,"dominantDistortion"),(int)number(sig,"skyStyle"));
        String[] keys={"edgeHum","geometryMelt","realityFold","blockAutonomy","liquidFlow","skyVortex","entityTrails","prismSplit","electricBloom"};
        String env=json.substring(json.indexOf("\"environment\""));
        float[] environment={(float)Math.min(1,number(env,"water")+number(env,"rain")*.35),
                (float)Math.min(1,number(env,"nether")+number(env,"light")*.28),
                (float)Math.min(1,number(env,"end")+number(env,"night")*.42+number(env,"sunriseSunset")*.24),
                (float)Math.min(1,number(env,"forest")*.32+number(env,"cave")*.34+number(env,"village")*.30),
                (float)Math.min(1,number(env,"depth")*.72+number(env,"cave")*.28)};
        float[] shaderChannels=null;
        Path snapshot=Path.of(path),smoke=snapshot.getParent().getParent();
        Path actual=smoke.resolve("render-fixtures/"+snapshot.getFileName().toString().replace(".json","")+"-shader-channels-after-draw.json");
        if(Files.exists(actual))try {
            String raw=Files.readString(actual).trim();String[] values=raw.substring(1,raw.length()-1).trim().split(",");
            require(values.length==11,"Unexpected actual eased shader channel count");shaderChannels=new float[11];
            for(int i=0;i<11;i++)shaderChannels[i]=Float.parseFloat(values[i].trim());
        }catch(Exception e){throw new RuntimeException("Unable to read actual eased shader uniforms",e);}
        return new State(number(json,"activeDoseMicrograms"),number(json,"visualIntensity"),Arrays.stream(keys).mapToDouble(k->number(json,k)).toArray(),s,
                "actual native checkpoint"+(shaderChannels==null?"":" + exact eased shader uniforms")+": "+path,environment,shaderChannels);
    }
    private static void state(int p,State s,float time,float motion,float grade) {
        float[] c=new float[9];for(int i=0;i<9;i++)c[i]=(float)Math.log1p(s.channels[i]/6);
        float strength=(float)Math.log1p(s.intensity),dose=(float)Math.min(Float.MAX_VALUE,s.dose);
        if(s.shaderChannels!=null) {
            strength=s.shaderChannels[0];dose=s.shaderChannels[10];
            System.arraycopy(s.shaderChannels,1,c,0,9);
        }
        vec(p,"WarpA",strength,c[0],c[1],time);vec(p,"WarpB",c[2],c[3],c[4],c[5]);
        vec(p,"WarpC",c[6],c[7],c[8],(float)(smooth(35,60,dose)+smooth(80,120,dose)+smooth(135,210,dose)+smooth(210,285,dose)+smooth(800,1000,dose)));
        vec(p,"Dose",(float)smooth(425,650,dose),(float)smooth(725,1000,dose),(float)smooth(1400,1600,dose),dose/1000);vec(p,"DoseDetail",dose/1000,dose/1000,1,1);
        vec(p,"Control",motion,grade,0,0);Signature sig=s.signature;
        vec(p,"Profile",((sig.seed>>>6)&4095L)/4095f,((sig.seed>>>28)&4095L)/4095f,sig.dominant,sig.skyStyle);
        vec(p,"Env",s.environment[0],s.environment[1],s.environment[2],s.environment[3]);vec(p,"EnvDetail",s.environment[4],.75f,1,.46f);
        vec(p,"PaletteA",sig.palette[0][0],sig.palette[0][1],sig.palette[0][2],(float)sig.edge);
        vec(p,"PaletteB",sig.palette[1][0],sig.palette[1][1],sig.palette[1][2],(float)sig.geometry);
        vec(p,"PaletteC",sig.palette[2][0],sig.palette[2][1],sig.palette[2][2],(float)sig.prism);
    }
    private static float[] draw() {
        glDrawArrays(GL_TRIANGLES,0,3);float[] out=new float[W*H*4];glReadPixels(0,0,W,H,GL_RGBA,GL_FLOAT,out);
        for(float v:out)require(Float.isFinite(v)&&v>=-.00001f&&v<=1.00001f,"Non-finite/invalid rendered channel "+v);return out;
    }
    private static double difference(float[] a,float[] b) {
        double total=0;int count=0;
        for(int y=8;y<H-8;y++)for(int x=8;x<W-8;x++)for(int c=0;c<3;c++){int i=(y*W+x)*4+c;total+=Math.abs(a[i]-b[i]);count++;}return total/count;
    }
    private static double chroma(float[] a,float[] b) {
        double total=0;int count=0;
        for(int y=8;y<H-8;y++)for(int x=8;x<W-8;x++){int i=(y*W+x)*4;total+=Math.abs((a[i]-a[i+1])-(b[i]-b[i+1]))+Math.abs((a[i+2]-a[i+1])-(b[i+2]-b[i+1]));count+=2;}return total/count;
    }
    private static double curvature(float[] a) {
        double total=0;int count=0;
        for(int y=8;y<H-8;y++)for(int x=8;x<W-8;x++)for(int c=0;c<3;c++) {
            int i=(y*W+x)*4+c;total+=Math.abs(a[i]-(a[i-4]+a[i+4]+a[i-W*4]+a[i+W*4])*.25);count++;
        }return total/count;
    }
    private static double meanChroma(float[] a) {
        double total=0;int count=0;
        for(int y=8;y<H-8;y++)for(int x=8;x<W-8;x++) {
            int i=(y*W+x)*4;total+=Math.max(a[i],Math.max(a[i+1],a[i+2]))-Math.min(a[i],Math.min(a[i+1],a[i+2]));count++;
        }return total/count;
    }
    private static float[] array(String json,String key) {
        Matcher m=Pattern.compile("\""+Pattern.quote(key)+"\"\\s*:\\s*\\[([^]]*)]",Pattern.DOTALL).matcher(json);
        if(!m.find())throw new IllegalArgumentException("Missing native render fixture array "+key);
        String[] values=m.group(1).trim().split(",");float[] result=new float[values.length];
        for(int i=0;i<result.length;i++)result[i]=Float.parseFloat(values[i].trim());return result;
    }
    private static Path checkpoint(double dose,String phase) {
        return checkpoint(dose,phase,"day");
    }
    private static Path checkpoint(double dose,String phase,String scene) {
        if(nativeDirectory==null)return null;
        try(var paths=Files.walk(nativeDirectory)) {
            return paths.filter(p->p.getFileName().toString().contains(scene+"-"+phase+"-"+(int)dose+"ug")&&p.toString().endsWith(".json")).sorted().findFirst().orElse(null);
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private static Scene nativeFixture(int p,Path snapshot) {
        try {
            Path smoke=nativeDirectory.getParent();String point=snapshot.getFileName().toString().replace(".json","");
            Path render=smoke.resolve("render-fixtures/"+point+"-render.json");String metadata=Files.readString(render);
            java.awt.image.BufferedImage png=javax.imageio.ImageIO.read(smoke.resolve("shader-pairs/"+point+"-original.png").toFile());
            int width=png.getWidth(),height=png.getHeight();Path depth=smoke.resolve("render-fixtures/"+point+"-depth-f32le.bin");
            if(!Files.exists(depth)) {
                String sceneName=point.contains("tavern")?"tavern":"day";
                Path matching=checkpoint(500,"wave",sceneName);
                if(matching==null)matching=checkpoint(500,"peak",sceneName);
                require(matching!=null,"No matching fixed-view depth fixture for native palette replay");
                depth=smoke.resolve("render-fixtures/"+matching.getFileName().toString().replace(".json","")+"-depth-f32le.bin");
                System.out.println("REPLAY NOTE "+point+": same fixed-camera500ug depth reused from "+matching.getFileName()+"; focused overlay mesh may differ slightly");
            }
            ByteBuffer depths=ByteBuffer.wrap(Files.readAllBytes(depth)).order(ByteOrder.LITTLE_ENDIAN);
            require(depths.remaining()==width*height*4,"Native depth dimensions mismatch");
            float[] source=new float[W*H*4],sampleDepth=new float[W*H];
            for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
                int px=Math.min(width-1,(int)((x+.5)*width/W)),py=Math.min(height-1,(int)((y+.5)*height/H));
                int rgb=png.getRGB(px,height-py-1),i=(y*W+x)*4;
                source[i]=((rgb>>16)&255)/255f;source[i+1]=((rgb>>8)&255)/255f;source[i+2]=(rgb&255)/255f;source[i+3]=1;
                sampleDepth[y*W+x]=depths.getFloat((py*width+px)*4);
            }
            upload(source,sampleDepth);float[] eye=array(metadata,"camera_eye");Scene scene=new Scene(source,eye[0],eye[1],eye[2]);setup(p,scene);
            Matrix4f vp=new Matrix4f().set(array(metadata,"event_view_projection")),inverse=new Matrix4f().set(array(metadata,"event_inverse_view_projection"));
            matrix(p,"WorldViewProjection",vp);matrix(p,"WorldInverseViewProjection",inverse);
            // Source PNG is top-left while the saved GL depth is bottom-left.
            // Both have now been sampled in the same OpenGL bottom-left order.
            System.out.println("REPLAY "+point+": actual colored source/native state/matrices, downsampled"+W+"x"+H+" palette-cycle QA");
            return scene;
        }catch(Exception e){throw new RuntimeException("Unable to replay actual native colored fixture",e);}
    }
    private static void paletteCycleTest(int p,String name) {
        for(String sceneName:new String[]{"day","tavern"})for(double dose:new double[]{250,500,1000,5000}) {
            Path snapshot=checkpoint(dose,dose==1000?"wave":"peak",sceneName);
            if(snapshot==null)continue;
            State s;try{s=fromSnapshot(Files.readString(snapshot),snapshot.toString());}catch(Exception e){throw new RuntimeException(e);}
            Scene scene=nativeFixture(p,snapshot);double reference=meanChroma(scene.source),minimum=Double.MAX_VALUE,largest=0,largestColor=0;
            float[] worst=null,lastColor=null;
            for(int f=0;f<=360;f++) {
                state(p,s,f*.5f,1,1);float[] out=draw();double chroma=meanChroma(out);
                if(chroma<minimum){minimum=chroma;worst=out;}
                state(p,s,f*.5f+1/60f,1,1);largest=Math.max(largest,difference(draw(),out));
                state(p,s,f*.5f,0,1);float[] colorOnly=draw();
                if(lastColor!=null)largestColor=Math.max(largestColor,difference(colorOnly,lastColor));lastColor=colorOnly;
            }
            save(worst,name+"-native-"+sceneName+"-"+(int)dose+"ug-cycle-min-chroma");
            System.out.printf("CYCLE %s %s %.0fug180s meanChroma min%.6f source%.6f ratio%.4f max60HzRGB%.6f colorOnlyHalfSecondRGB%.6f%n",name,sceneName,dose,minimum,reference,minimum/reference,largest,largestColor);
            require(minimum>=reference*.90,name+" native"+dose+"ug loses vivid color during palette cycle: "+minimum/reference);
            require(largest<.0045,name+" native"+dose+"ug adjacent60Hz cycle frame flickers: "+largest);
            require(largestColor<.035,name+" native"+dose+"ug isolated color cycle flickers: "+largestColor);
        }
    }
    private static void save(float[] image,String name) {
        try {
            java.awt.image.BufferedImage png=new java.awt.image.BufferedImage(W,H,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<H;y++)for(int x=0;x<W;x++){
                int i=(y*W+x)*4,r=(int)Math.round(image[i]*255),g=(int)Math.round(image[i+1]*255),b=(int)Math.round(image[i+2]*255);
                png.setRGB(x,H-y-1,0xff000000|(r<<16)|(g<<8)|b);
            }javax.imageio.ImageIO.write(png,"PNG",outputDirectory.resolve(name+".png").toFile());
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static void verify(int p,String name) {
        Scene scene=fixture(0,64,0,6,0,0,false,0);setup(p,scene);
        State high=nativeState(5000),zero=new State(5000,0,new double[9],high.signature,"zero onset");state(p,zero,8,1,1);
        require(difference(draw(),scene.source)<.00001,name+" zero-intensity5000ug onset changes the scene");
        double previous=0;float[] previousImage=null;save(scene.source,name+"-bark-books-sober");
        for(double dose:new double[]{250,500,1000,5000,10000}) {
            State s=nativeState(dose);state(p,s,8,1,1);float[] out=draw();double visible=difference(out,scene.source);
            state(p,s,8,1,0);double flow=difference(draw(),scene.source);
            System.out.printf("STATE %s %.0fug native visual%.6f melt%.6f; %s%n",name,dose,s.intensity,s.channels[1],s.provenance);
            require(visible>.004,name+" dim textures lack a visible effect at"+dose+"ug: "+visible);
            require(flow>.0015,name+" movement-only effect invisible at"+dose+"ug: "+flow);
            require(dose==250||visible>previous,name+" dose escalation flattened at"+dose+"ug: "+visible+" previous="+previous);
            if(previousImage!=null)require(difference(out,previousImage)>.008,name+" higher dose looks identical to its predecessor at"+dose+"ug");
            previous=visible;previousImage=out;save(out,name+"-bark-books-"+(int)dose+"ug");
            System.out.printf("PASS %s %.0fug dim-scene meanRGB%.6f movement-only%.6f%n",name,dose,visible,flow);
        }
        for(double dose:new double[]{250,500,5000}) {
            State s=nativeState(dose);state(p,s,8,1,0);float[] start=draw();state(p,s,24,1,0);double motion=difference(draw(),start);
            require(motion>.0012,name+" stationary-camera textures failed to animate at"+dose+"ug: "+motion);
            Scene flat=fixture(0,64,0,6,0,0,true,0);setup(p,flat);state(p,s,8,0,1);start=draw();state(p,s,24,0,1);float[] end=draw();
            double pattern=difference(start,end),colors=chroma(start,end);
            require(pattern>.0012,name+" patterns failed to animate at"+dose+"ug: "+pattern);
            require(colors>.0007,name+" chromatic field failed to morph at"+dose+"ug: "+colors);
            System.out.printf("PASS %s %.0fug autonomous16s flow%.6f pattern/light%.6f chroma%.6f%n",name,dose,motion,pattern,colors);
            scene=fixture(0,64,0,6,0,0,false,0);setup(p,scene);state(p,s,8,1,1);float[] last=draw();double maximum=0;
            for(int frame=1;frame<=180;frame++){state(p,s,8+frame/60f,1,1);float[] out=draw();maximum=Math.max(maximum,difference(out,last));last=out;}
            require(maximum<.003,name+" abrupt60fps texture/color changes at"+dose+"ug: "+maximum);
            System.out.printf("PASS %s %.0fug180frames60Hz maxAdjacentRGB%.6f%n",name,dose,maximum);
        }
        for(float origin:new float[]{0,4090})anchorTest(p,name,origin);
        for(float distance:new float[]{6,24,40})for(float origin:new float[]{0,4090})grainTest(p,name,distance,origin);
        paletteCycleTest(p,name);
        scene=fixture(0,64,0,6,0,0,false,0);setup(p,scene);
        for(double join:new double[]{250,425,500,650,725,800,1000,1296,1400,1600}) {
            state(p,nativeState(join-.0001),8,1,1);float[] before=draw();state(p,nativeState(join+.0001),8,1,1);double seam=difference(before,draw());
            require(seam<.00003,name+" dose gate introduced a step at"+join+"ug: "+seam);
        }
        System.out.println("PASS "+name+": dose curve/client gate joins continuously");
        state(p,high,8,0,0);require(difference(draw(),scene.source)<.00001,name+" disabled controls change frame");
        float[] sky=new float[W*H];Arrays.fill(sky,1);upload(scene.source,sky);state(p,high,8,1,1);require(difference(draw(),scene.source)<.00001,name+" sky treated as a surface");
        scene=fixture(0,64,0,6,0,0,false,0);setup(p,scene);state(p,nativeState(Double.MAX_VALUE),8,1,1);draw();
        System.out.println("PASS "+name+": controls/sky identity and maximum finite server dose transport");
    }
    private static void anchorTest(int p,String name,float origin) {
        Scene scene=fixture(origin,64,origin,6,0,0,true,origin);setup(p,scene);State s=nativeState(500);state(p,s,8,0,1);float[] reference=draw();
        int pixels=7;float delta=pixels*(2*TAN*W/H)*6/W;
        scene=fixture(origin+delta,64,origin,6,0,0,true,origin);setup(p,scene);state(p,s,8,0,1);float[] shifted=draw();
        double total=0;int count=0;
        for(int y=16;y<H-16;y++)for(int x=16;x<W-16-pixels;x++)for(int c=0;c<3;c++){total+=Math.abs(shifted[(y*W+x)*4+c]-reference[(y*W+x+pixels)*4+c]);count++;}
        double error=total/count;require(error<.001,name+" world field swims with camera at origin"+origin+": "+error);
        System.out.printf("PASS %s fixed-world camera-pan origin%.0f reprojectionRGB%.7f%n",name,origin,error);
    }
    private static void grainTest(int p,String name,float distance,float origin) {
        Scene scene=fixture(origin,64,origin,distance,.28f,.13f,true,origin);setup(p,scene);state(p,nativeState(500),8,0,1);float[] out=draw();
        double rough=curvature(out),signal=difference(out,scene.source),ratio=rough/Math.max(.001,signal);
        require(signal>.006&&rough<.008&&ratio<.52,name+" obliqueD24 surface invisible or grain at distance"+distance+" origin"+origin+": curvature="+rough+" signal="+signal+" ratio="+ratio);
        save(out,name+"-obliqueD24-"+(int)distance+"blocks-origin"+(int)origin);
        System.out.printf("PASS %s obliqueD24 %.0fblocks origin%.0f curvature%.6f signal%.6f ratio%.3f%n",name,distance,origin,rough,signal,ratio);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
