import javax.tools.ToolProvider;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Executes the production pass and blend resolver against isolated time/state/render doubles. */
public final class ValidateDmtFractalControls {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath();
        Path work = root.resolve("build/dmt-fractal-control-validation");
        Path sources = work.resolve("sources");
        Path classes = work.resolve("classes");
        Files.createDirectories(classes);
        Map<String, String> doubles = new LinkedHashMap<>();
        doubles.put("com/trippycraft/config/TrippyClientConfig.java", """
                package com.trippycraft.config;
                public final class TrippyClientConfig {
                    public static boolean dmtFractalWorld, rendererEnabled = true, motionReduction, photosensitivityReduction;
                    public static double visualIntensity = 1, motionIntensity = 1;
                    public static int dmtFractalQuality = 1;
                }
                """);
        doubles.put("com/trippycraft/config/TrippyConfig.java", """
                package com.trippycraft.config;
                public final class TrippyConfig {
                    public static boolean enableDmtEffects = true, dmtWorldWarp = true, dmtAccessibilityMode;
                    public static boolean enablePsilocybinEffects = true, enableLsdEffects = true;
                    public static double globalIntensity = 1, dmtBreakthroughThresholdUnits = 2.4;
                    public static double dmtTransitionStartProgress = .18, dmtPostBreakthroughStartProgress = .30;
                    public static double dmtReturnStartProgress = .72, dmtAfterglowStartProgress = .88;
                    public static double psilocybinMaxVisualIntensity = 6, lsdMaxVisualIntensity = 7, dmtMaxVisualIntensity = 8.5;
                    public static double psilocybinBlendWeight = 1, lsdBlendWeight = 1, dmtBlendWeight = 1;
                    public static double dmtBreakthroughDominanceWeight = 1;
                }
                """);
        doubles.put("net/minecraft/world/phys/Vec3.java", """
                package net.minecraft.world.phys;
                public final class Vec3 {
                    public final double x, y, z;
                    public Vec3(double x, double y, double z) { this.x=x; this.y=y; this.z=z; }
                    public Vec3 subtract(Vec3 v) { return new Vec3(x-v.x,y-v.y,z-v.z); }
                    public Vec3 scale(double scale) { return new Vec3(x*scale,y*scale,z*scale); }
                }
                """);
        doubles.put("net/minecraft/client/Minecraft.java", """
                package net.minecraft.client;
                import net.minecraft.world.phys.Vec3;
                public final class Minecraft {
                    private static final Minecraft INSTANCE = new Minecraft();
                    public Level level = new Level();
                    public Object player = new Object();
                    public final GameRenderer gameRenderer = new GameRenderer();
                    public static Minecraft getInstance() { return INSTANCE; }
                    public static final class Level { public long time; public long getGameTime() { return time; } }
                    public static final class GameRenderer {
                        public final Camera camera = new Camera();
                        public Camera getMainCamera() { return camera; }
                    }
                    public static final class Camera {
                        public Vec3 position = new Vec3(10,64,-5);
                        public Vec3 getPosition() { return position; }
                    }
                }
                """);
        doubles.put("com/trippycraft/substance/dmt/DmtTripSignature.java", """
                package com.trippycraft.substance.dmt;
                public record DmtTripSignature(long seed) {
                    public int primaryRed(){return 210;} public int primaryGreen(){return 120;} public int primaryBlue(){return 245;}
                    public int secondaryRed(){return 65;} public int secondaryGreen(){return 220;} public int secondaryBlue(){return 180;}
                    public int accentRed(){return 255;} public int accentGreen(){return 190;} public int accentBlue(){return 80;}
                }
                """);
        doubles.put("com/trippycraft/substance/dmt/DmtFractalScaffold.java", """
                package com.trippycraft.substance.dmt;
                public record DmtFractalScaffold(double dimensionalDepth, double temporalFlow) {}
                """);
        doubles.put("com/trippycraft/substance/dmt/DmtEffectState.java", """
                package com.trippycraft.substance.dmt;
                public record DmtEffectState(boolean active, double activeDoseUnits, double progress,
                        double visualIntensity, double intensity, int tier, DmtTripSignature signature,
                        DmtFractalScaffold fractalScaffold) {
                    public static DmtEffectState inactive() {
                        return new DmtEffectState(false,0,1,0,0,0,new DmtTripSignature(0),new DmtFractalScaffold(0,0));
                    }
                    public int remainingTicks() { return active ? 1000 : 0; }
                }
                """);
        doubles.put("com/trippycraft/substance/psilocybin/PsilocybinEffectState.java", """
                package com.trippycraft.substance.psilocybin;
                public record PsilocybinEffectState(boolean active, double progress, double intensity,
                        double visualIntensity, double activeDoseGrams) {
                    public static PsilocybinEffectState inactive(){return new PsilocybinEffectState(false,1,0,0,0);}
                    public int remainingTicks(){return active ? 1000 : 0;}
                }
                """);
        doubles.put("com/trippycraft/substance/lsd/LsdEffectState.java", """
                package com.trippycraft.substance.lsd;
                public record LsdEffectState(boolean active, double progress, double intensity,
                        double visualIntensity, double activeDoseMicrograms, int tier) {
                    public static LsdEffectState inactive(){return new LsdEffectState(false,1,0,0,0,0);}
                    public int remainingTicks(){return active ? 1000 : 0;}
                }
                """);
        for (String[] extra : new String[][]{{"homet","HoMet","spatialDepth"},
                {"allad","AlLad","stainedGlass"},{"twoce","TwoCe","tessellation"}}) {
            doubles.put("com/trippycraft/substance/"+extra[0]+"/"+extra[1]+"EffectState.java", """
                    package com.trippycraft.substance.%s;
                    public record %sEffectState(boolean active, double progress, double visualPresence, double %s) {
                        public static %sEffectState inactive(){return new %sEffectState(false,1,0,0);}
                        public int remainingTicks(){return active ? 1000 : 0;}
                    }
                    """.formatted(extra[0],extra[1],extra[2],extra[1],extra[1]));
        }
        doubles.put("com/trippycraft/client/ClientExperienceStore.java", """
                package com.trippycraft.client;
                import com.trippycraft.substance.dmt.DmtEffectState;
                import com.trippycraft.substance.lsd.LsdEffectState;
                import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
                import com.trippycraft.substance.homet.HoMetEffectState;
                import com.trippycraft.substance.allad.AlLadEffectState;
                import com.trippycraft.substance.twoce.TwoCeEffectState;
                public final class ClientExperienceStore {
                    public static DmtEffectState state = DmtEffectState.inactive();
                    public static LsdEffectState lsdState = LsdEffectState.inactive();
                    public static DmtEffectState dmt(){return state;}
                    public static PsilocybinEffectState psilocybin(){return PsilocybinEffectState.inactive();}
                    public static LsdEffectState lsd(){return lsdState;}
                    public static HoMetEffectState hoMet(){return HoMetEffectState.inactive();}
                    public static AlLadEffectState alLad(){return AlLadEffectState.inactive();}
                    public static TwoCeEffectState twoCe(){return TwoCeEffectState.inactive();}
                }
                """);
        doubles.put("com/trippycraft/client/VivecraftCompat.java", """
                package com.trippycraft.client;
                import net.minecraft.world.phys.Vec3;
                public final class VivecraftCompat {
                    public static boolean vr;
                    public static Vec3 head;
                    public static boolean isVrWorldPass(){return vr;}
                    public static Vec3 headPosition(){return head;}
                }
                """);
        doubles.put("com/trippycraft/client/WorldWarpPostProcessor.java", """
                package com.trippycraft.client;
                import java.util.ArrayList;
                import java.util.List;
                public final class WorldWarpPostProcessor {
                    public record UniformValue(String name, float[] values) {}
                    public record Draw(String effect, boolean depth, UniformValue[] uniforms) {
                        public float[] get(String name) {
                            for (var value: uniforms) if(value.name().equals(name)) return value.values();
                            throw new AssertionError("Missing uniform: "+name);
                        }
                    }
                    public static final List<Draw> calls = new ArrayList<>();
                    public static UniformValue uniform(String name,float... values){return new UniformValue(name,values);}
                    public static void process(String effect,boolean depth,float partialTick,UniformValue... uniforms){
                        calls.add(new Draw(effect,depth,uniforms));
                    }
                }
                """);
        doubles.put("com/trippycraft/client/DmtFractalControlProbe.java", PROBE);
        ArrayList<String> compile = new ArrayList<>();
        compile.addAll(java.util.List.of("-proc:none","-d",classes.toString()));
        for (var source : doubles.entrySet()) {
            Path file = sources.resolve(source.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue());
            compile.add(file.toString());
        }
        for (String production : new String[]{"client/DmtFractalWorldPass.java",
                "substance/PsychedelicBlendState.java","substance/CompositeEffectResolver.java"}) {
            compile.add(root.resolve("src/main/java/com/trippycraft/"+production).toString());
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A Java 21 JDK is required");
        if (compiler.run(null,System.out,System.err,compile.toArray(String[]::new)) != 0)
            throw new AssertionError("Production pass and isolated fixtures did not compile");
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("com.trippycraft.client.DmtFractalControlProbe").getMethod("run").invoke(null);
        } catch (InvocationTargetException exception) {
            if(exception.getCause() instanceof Error error) throw error;
            throw exception;
        }
        System.out.println("Scope: production controller + production blend resolver executed with time/state/render doubles.");
        System.out.println("Not covered: NeoForge config disk persistence, key events, Minecraft framegraph, live pause or a VR headset.");
    }

    private static final String PROBE = """
            package com.trippycraft.client;
            import com.trippycraft.config.TrippyClientConfig;
            import com.trippycraft.config.TrippyConfig;
            import com.trippycraft.substance.CompositeEffectResolver;
            import com.trippycraft.substance.dmt.*;
            import com.trippycraft.substance.lsd.LsdEffectState;
            import net.minecraft.client.Minecraft;
            import net.minecraft.world.phys.Vec3;
            public final class DmtFractalControlProbe {
                private static int assertions;
                private static final Minecraft MC=Minecraft.getInstance();
                public static void run() {
                    disabled(); peakAndEnvelope(); clockAndMotion(); qualityAndAccessibility();
                    mixtureAndIntensity(); anchorsAndReset(); stereoHead();
                    System.out.println("PASS: "+assertions+" behavioral assertions");
                }
                private static void setup() {
                    DmtFractalWorldPass.reset();
                    MC.level=new Minecraft.Level(); MC.player=new Object();
                    MC.gameRenderer.camera.position=new Vec3(10,64,-5);
                    VivecraftCompat.vr=false; VivecraftCompat.head=null;
                    TrippyClientConfig.dmtFractalWorld=true; TrippyClientConfig.rendererEnabled=true;
                    TrippyClientConfig.visualIntensity=1; TrippyClientConfig.motionIntensity=1;
                    TrippyClientConfig.dmtFractalQuality=1; TrippyClientConfig.motionReduction=false;
                    TrippyClientConfig.photosensitivityReduction=false;
                    TrippyConfig.enableDmtEffects=true; TrippyConfig.dmtWorldWarp=true;
                    TrippyConfig.dmtAccessibilityMode=false; TrippyConfig.globalIntensity=1;
                    ClientExperienceStore.lsdState=LsdEffectState.inactive();
                    ClientExperienceStore.state=state(4,.5,1,1234567);
                    WorldWarpPostProcessor.calls.clear();
                }
                private static DmtEffectState state(double dose,double progress,double visual,long seed) {
                    return new DmtEffectState(true,dose,progress,visual,visual,5,
                        new DmtTripSignature(seed),new DmtFractalScaffold(.7,.6));
                }
                private static WorldWarpPostProcessor.Draw tick(double time) {
                    MC.level.time=(long)Math.floor(time); WorldWarpPostProcessor.calls.clear();
                    DmtFractalWorldPass.render((float)(time-Math.floor(time)));
                    check(WorldWarpPostProcessor.calls.size()<=1,"Controller emitted duplicate draws");
                    if(WorldWarpPostProcessor.calls.isEmpty()) return null;
                    var draw=WorldWarpPostProcessor.calls.get(0);
                    check(draw.effect().equals("dmt_fractal_world_warp") && draw.depth(),"Wrong effect or missing depth request");
                    for(var uniform:draw.uniforms()) for(float value:uniform.values())
                        check(Float.isFinite(value),"Non-finite "+uniform.name());
                    return draw;
                }
                private static WorldWarpPostProcessor.Draw settle(int from,int to) {
                    WorldWarpPostProcessor.Draw draw=null;
                    for(int time=from;time<=to;time++) draw=tick(time);
                    return draw;
                }
                private static float[] a(WorldWarpPostProcessor.Draw draw) {
                    check(draw!=null,"Expected a realm draw"); return draw.get("FractalA");
                }
                private static void disabled() {
                    setup(); TrippyClientConfig.dmtFractalWorld=false;
                    check(settle(0,200)==null,"Disabled option emitted a draw");
                    TrippyClientConfig.dmtFractalWorld=true;
                    check(tick(201)==null,"Enabling resurrected stale opacity");
                    check(a(settle(202,401))[0]==1,"Enabled realm failed to enter");
                    TrippyClientConfig.dmtFractalWorld=false;
                    check(tick(402)==null,"Off switch failed during a visible realm");
                    TrippyClientConfig.dmtFractalWorld=true;
                    check(tick(403)==null,"Off switch did not clear the controller");
                    for(int gate=0;gate<6;gate++) {
                        setup(); settle(0,200);
                        switch(gate) {
                            case 0 -> TrippyClientConfig.rendererEnabled=false;
                            case 1 -> TrippyConfig.enableDmtEffects=false;
                            case 2 -> TrippyConfig.dmtWorldWarp=false;
                            case 3 -> TrippyClientConfig.visualIntensity=0;
                            case 4 -> TrippyConfig.globalIntensity=0;
                            case 5 -> MC.player=null;
                        }
                        check(tick(201)==null,"Disabled renderer/intensity/player gate ignored: "+gate);
                    }
                    setup(); settle(0,200); MC.level=null; WorldWarpPostProcessor.calls.clear();
                    DmtFractalWorldPass.render(0);
                    check(WorldWarpPostProcessor.calls.isEmpty(),"Missing world emitted a draw");
                    System.out.println("PASS: off switch, renderer/common controls, zero intensity and absent world/player emit no new pass");
                }
                private static void peakAndEnvelope() {
                    setup();
                    double gain=CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),ClientExperienceStore.lsd(),
                        ClientExperienceStore.dmt(),ClientExperienceStore.hoMet(),ClientExperienceStore.alLad(),ClientExperienceStore.twoCe()).dmtShaderGain();
                    near(gain,1,0,"Production isolated-DMT blend gain");
                    check(tick(0)==null,"First frame skipped the entry fade");
                    check(a(tick(1))[0]>0 && a(tick(1))[0]<1,"Entry must be gradual");
                    near(a(settle(2,200))[0],1,0,"High dose peak never fully replaces scene");
                    ClientExperienceStore.state=state(4,.80,1,1234567);
                    float returning=a(tick(201))[0]; check(returning>0 && returning<1,"Return did not start fading");
                    ClientExperienceStore.state=state(4,.90,1,1234567);
                    check(settle(202,500)==null,"Afterglow did not restore old rendering");
                    setup(); ClientExperienceStore.state=state(4,.10,1,1234567);
                    check(settle(0,200)==null,"Pre-transition onset rendered the realm");
                    setup(); ClientExperienceStore.state=state(.2,.5,1,1234567);
                    check(settle(0,200)==null,"Sub-threshold dose entered full realm");
                    setup(); ClientExperienceStore.state=state(4,.5,0,1234567);
                    check(settle(0,200)==null,"Zero visual state rendered the realm");
                    setup(); settle(0,200); ClientExperienceStore.state=DmtEffectState.inactive();
                    float fade=a(tick(201))[0]; check(fade>0 && fade<1,"Session end did not retain fading palette");
                    check(settle(202,500)==null,"Session end never released realm draw");
                    check(tick(501)==null,"Sober frame resurrected realm");
                    System.out.println("PASS: actual isolated blend reaches opaque peak; onset/dose/visual gates, return and session-end fade release rendering");
                }
                private static void clockAndMotion() {
                    setup(); float before=a(settle(0,200))[1];
                    near(a(tick(200))[1],before,0,"Frozen game time advanced animation");
                    near(a(tick(199))[1],before,0,"Clock reversal moved animation");
                    near(a(tick(10000))[1]-before,.25,1e-5,"Long clock jump was not capped");
                    setup(); TrippyClientConfig.motionIntensity=0;
                    near(a(settle(0,200))[1],0,0,"Zero motion did not freeze animation");
                    TrippyClientConfig.motionIntensity=1;
                    float resumed=a(tick(201))[1]; near(resumed,.05,1e-6,"Motion change rescaled elapsed animation");
                    TrippyClientConfig.motionIntensity=0;
                    near(a(tick(202))[1],resumed,0,"Disabling motion changed phase");
                    setup(); TrippyClientConfig.motionReduction=true;
                    near(a(settle(0,200))[1],1.5,1e-5,"Motion reduction multiplier");
                    setup(); TrippyConfig.dmtAccessibilityMode=true;
                    near(a(settle(0,200))[1],2.5,1e-5,"DMT accessibility motion multiplier");
                    System.out.println("PASS: frozen/reversed/clamped clocks, motion pause/resume continuity and accessibility rates");
                }
                private static void qualityAndAccessibility() {
                    setup(); settle(0,200);
                    for(int quality:new int[]{-10,0,1,2,10}) {
                        TrippyClientConfig.dmtFractalQuality=quality;
                        near(a(tick(201))[2],Math.max(0,Math.min(2,quality)),0,"Quality bounds");
                    }
                    near(a(tick(201))[3],0,0,"Unexpected soft-color flag");
                    TrippyClientConfig.photosensitivityReduction=true;
                    near(a(tick(201))[3],1,0,"Photosensitivity reduction not forwarded");
                    TrippyClientConfig.photosensitivityReduction=false; TrippyConfig.dmtAccessibilityMode=true;
                    near(a(tick(201))[3],1,0,"DMT accessibility colors not forwarded");
                    System.out.println("PASS: live quality bounds and both color-reduction controls reach shader uniforms");
                }
                private static void mixtureAndIntensity() {
                    setup(); TrippyClientConfig.visualIntensity=.5; TrippyConfig.globalIntensity=.5;
                    near(a(settle(0,200))[0],.25,1e-5,"Client/common intensity scaling");
                    setup(); ClientExperienceStore.lsdState=new LsdEffectState(true,.5,7,7,250,5);
                    double gain=CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),ClientExperienceStore.lsd(),
                        ClientExperienceStore.dmt(),ClientExperienceStore.hoMet(),ClientExperienceStore.alLad(),ClientExperienceStore.twoCe()).dmtShaderGain();
                    check(gain>0 && gain<1,"Fixture did not exercise real mixed-effect weighting");
                    near(a(settle(0,250))[0],gain,1e-5,"Mixed-effect blend gain ignored");
                    System.out.println("PASS: real LSD/DMT blend weighting and client/common intensity scale replacement opacity");
                }
                private static void anchorsAndReset() {
                    setup(); ClientExperienceStore.state=state(4,.1,1,1234567); settle(0,100);
                    MC.gameRenderer.camera.position=new Vec3(1010,64,-5); tick(101);
                    ClientExperienceStore.state=state(4,.5,1,1234567);
                    near(a(tick(102))[0]>0?WorldWarpPostProcessor.calls.get(0).get("RealmOrigin")[0]:99,0,0,"Walking before entry displaced entrance");
                    var peak=settle(103,303);
                    MC.gameRenderer.camera.position=new Vec3(1012,67,-9);
                    var moved=tick(304).get("RealmOrigin");
                    near(moved[0],.7,1e-6,"World movement x parallax"); near(moved[1],1.05,1e-6,"World movement y parallax");
                    near(moved[2],-1.4,1e-6,"World movement z parallax");
                    ClientExperienceStore.state=state(4,.5,1,2222222);
                    check(tick(305)==null,"New signature retained old realm");
                    var fresh=settle(306,506); near(fresh.get("RealmOrigin")[0],0,0,"New signature did not reanchor");
                    MC.level=new Minecraft.Level(); check(tick(507)==null,"New world retained old opacity");
                    near(a(settle(508,708))[1],10.05,1e-4,"New world retained animation phase");
                    DmtFractalWorldPass.reset(); check(tick(709)==null,"Explicit reset retained old opacity");
                    System.out.println("PASS: pre-entry walking, world-space parallax, new signatures/worlds and explicit reset");
                }
                private static void stereoHead() {
                    setup(); VivecraftCompat.vr=true; VivecraftCompat.head=new Vec3(100,65,100);
                    MC.gameRenderer.camera.position=new Vec3(-999,2,-999);
                    var first=settle(0,200); near(first.get("RealmOrigin")[0],0,0,"VR anchor used eye camera");
                    MC.gameRenderer.camera.position=new Vec3(100.032,65,100);
                    var right=tick(200); near(a(right)[1],a(first)[1],0,"Repeated eye frame advanced clock");
                    near(right.get("RealmOrigin")[0],0,0,"Eye offset moved shared realm origin");
                    VivecraftCompat.head=new Vec3(101,65,100);
                    near(tick(201).get("RealmOrigin")[0],.35,1e-6,"Tracked head translation lost parallax");
                    System.out.println("PASS: VR uses shared tracked-head origin; eye-camera offsets and repeated timestamps do not advance it");
                }
                private static void near(double actual,double expected,double tolerance,String message) {
                    check(Double.isFinite(actual) && Math.abs(actual-expected)<=tolerance,
                        message+": actual="+actual+", expected="+expected);
                }
                private static void check(boolean condition,String message) {
                    assertions++; if(!condition) throw new AssertionError(message);
                }
            }
            """;
}
