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
                    public static double visualIntensity = 1, motionIntensity = 1, dmtFractalIntensity = 1;
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
                    public int hyperspaceStyle(){return Math.floorMod(seed,7);}
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
                import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
                public record DmtEffectState(boolean active, double activeDoseUnits, double progress,
                        double visualIntensity, double intensity, int tier, DmtTripSignature signature,
                        DmtFractalScaffold fractalScaffold, PsilocybinEnvironmentSnapshot environment) {
                    public DmtEffectState(boolean active, double activeDoseUnits, double progress,
                            double visualIntensity, double intensity, int tier, DmtTripSignature signature,
                            DmtFractalScaffold fractalScaffold) {
                        this(active,activeDoseUnits,progress,visualIntensity,intensity,tier,signature,fractalScaffold,
                            PsilocybinEnvironmentSnapshot.empty());
                    }
                    public static DmtEffectState inactive() {
                        return new DmtEffectState(false,0,1,0,0,0,new DmtTripSignature(0),new DmtFractalScaffold(0,0));
                    }
                    public int remainingTicks() { return active ? 1000 : 0; }
                    public int ageTicks() { return active ? 100 : 0; }
                }
                """);
        doubles.put("com/trippycraft/substance/psilocybin/PsilocybinEnvironmentSnapshot.java", """
                package com.trippycraft.substance.psilocybin;
                public record PsilocybinEnvironmentSnapshot(double organicBlocks, double artificialBlocks, double cave,
                        double depth, double water, double forest, double village, double light, double night,
                        double rain, double entities, double hostileEntities, double moving) {
                    public static PsilocybinEnvironmentSnapshot empty() {
                        return new PsilocybinEnvironmentSnapshot(0,0,0,0,0,0,0,.5,0,0,0,0,0);
                    }
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
        doubles.put("com/trippycraft/client/DmtWorldInfluenceSampler.java", """
                package com.trippycraft.client;
                import net.minecraft.client.Minecraft;
                public final class DmtWorldInfluenceSampler {
                    public record Sample(double red, double green, double blue, double relief,
                            double directionX, double directionY, double directionZ, double directionStrength,
                            double attractorX, double attractorY, double attractorZ, double attractorStrength) {
                        public Sample(double red,double green,double blue,double relief) {
                            this(red,green,blue,relief,0,0,0,0,0,0,0,0);
                        }
                    }
                    public static Sample value = new Sample(.3,.6,.2,.1);
                    public static int calls;
                    public static Sample sample(Minecraft minecraft){calls++;return value;}
                    public static void reset(){calls=0;}
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
        runFixtures(root, sources, classes, doubles, new String[]{"client/DmtFractalWorldPass.java","client/DmtRealmDynamics.java",
                "substance/PsychedelicBlendState.java","substance/CompositeEffectResolver.java"},
                "com.trippycraft.client.DmtFractalControlProbe");
        runSamplerFixtures(root, work.resolve("sampler"));
        System.out.println("Scope: production controller, dose/cinematic helper, blend resolver and loaded-world sampler executed with isolated dependency doubles.");
        System.out.println("Not covered: NeoForge config disk persistence, key events, Minecraft framegraph, live pause or a VR headset.");
    }

    private static void runFixtures(Path root, Path sources, Path classes, Map<String,String> doubles,
                                    String[] productionFiles, String probe) throws Exception {
        Files.createDirectories(classes);
        ArrayList<String> compile = new ArrayList<>();
        compile.addAll(java.util.List.of("-proc:none","-d",classes.toString()));
        for (var source : doubles.entrySet()) {
            Path file = sources.resolve(source.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue());
            compile.add(file.toString());
        }
        for (String production : productionFiles) {
            compile.add(root.resolve("src/main/java/com/trippycraft/"+production).toString());
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A Java 21 JDK is required");
        if (compiler.run(null,System.out,System.err,compile.toArray(String[]::new)) != 0)
            throw new AssertionError("Production sources and isolated fixtures did not compile: "+probe);
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            loader.loadClass(probe).getMethod("run").invoke(null);
        } catch (InvocationTargetException exception) {
            if(exception.getCause() instanceof Error error) throw error;
            throw exception;
        }
    }

    private static void runSamplerFixtures(Path root, Path work) throws Exception {
        Map<String,String> doubles = new LinkedHashMap<>();
        doubles.put("net/minecraft/core/BlockPos.java", """
                package net.minecraft.core;
                public record BlockPos(int x,int y,int z) {
                    public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
                    public static BlockPos containing(double x,double y,double z) {
                        return new BlockPos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));
                    }
                    public BlockPos offset(int dx,int dy,int dz){return new BlockPos(x+dx,y+dy,z+dz);}
                    public BlockPos immutable(){return this;}
                    public double distSqr(BlockPos p){double dx=x-p.x,dy=y-p.y,dz=z-p.z;return dx*dx+dy*dy+dz*dz;}
                }
                """);
        doubles.put("net/minecraft/world/phys/AABB.java", """
                package net.minecraft.world.phys;
                public record AABB(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
                    public AABB inflate(double r){return new AABB(minX-r,minY-r,minZ-r,maxX+r,maxY+r,maxZ+r);}
                    public boolean contains(double x,double y,double z){return x>=minX&&x<=maxX&&y>=minY&&y<=maxY&&z>=minZ&&z<=maxZ;}
                }
                """);
        doubles.put("net/minecraft/world/entity/Entity.java", """
                package net.minecraft.world.entity;
                import java.util.UUID;
                import net.minecraft.core.BlockPos;
                import net.minecraft.world.phys.AABB;
                public class Entity {
                    public double x,y,z; public boolean alive=true; public final UUID id=UUID.randomUUID();
                    public Entity(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
                    public double getX(){return x;} public double getY(){return y;} public double getZ(){return z;}
                    public float getBbHeight(){return 2;} public boolean isAlive(){return alive;}
                    public UUID getUUID(){return id;} public double getEyeY(){return y+1.62;}
                    public BlockPos blockPosition(){return BlockPos.containing(x,y,z);}
                    public AABB getBoundingBox(){return new AABB(x-.3,y,z-.3,x+.3,y+2,z+.3);}
                    public double distanceToSqr(Entity other){double dx=x-other.x,dy=y-other.y,dz=z-other.z;return dx*dx+dy*dy+dz*dz;}
                }
                """);
        doubles.put("net/minecraft/world/entity/LivingEntity.java", """
                package net.minecraft.world.entity;
                public class LivingEntity extends Entity {public LivingEntity(double x,double y,double z){super(x,y,z);}}
                """);
        doubles.put("net/minecraft/world/level/material/MapColor.java", """
                package net.minecraft.world.level.material;
                public final class MapColor {
                    public static final MapColor NONE=new MapColor(0);
                    public final int col; public MapColor(int rgb){col=rgb;}
                }
                """);
        doubles.put("net/minecraft/world/level/block/state/BlockState.java", """
                package net.minecraft.world.level.block.state;
                import net.minecraft.client.multiplayer.ClientLevel;
                import net.minecraft.core.BlockPos;
                import net.minecraft.world.level.material.MapColor;
                public record BlockState(boolean air,boolean solid,MapColor color) {
                    public static final BlockState AIR=new BlockState(true,false,MapColor.NONE);
                    public boolean isAir(){return air;}
                    public MapColor getMapColor(ClientLevel level,BlockPos pos){return color;}
                    public boolean isSolidRender(ClientLevel level,BlockPos pos){return solid;}
                }
                """);
        doubles.put("net/minecraft/world/level/levelgen/Heightmap.java", """
                package net.minecraft.world.level.levelgen;
                public final class Heightmap {public enum Types {MOTION_BLOCKING_NO_LEAVES}}
                """);
        doubles.put("net/minecraft/client/multiplayer/ClientLevel.java", """
                package net.minecraft.client.multiplayer;
                import java.util.*;
                import java.util.function.Predicate;
                import net.minecraft.core.BlockPos;
                import net.minecraft.world.entity.Entity;
                import net.minecraft.world.phys.AABB;
                import net.minecraft.world.level.block.state.BlockState;
                import net.minecraft.world.level.levelgen.Heightmap;
                public final class ClientLevel {
                    public long time; public int reads,heightReads,entityQueries;
                    public Predicate<BlockPos> loaded=p->true;
                    public final Map<BlockPos,BlockState> states=new HashMap<>();
                    public final List<Entity> entities=new ArrayList<>();
                    public long getGameTime(){return time;}
                    public boolean hasChunkAt(BlockPos pos){return loaded.test(pos);}
                    public int getMinBuildHeight(){return -64;} public int getMaxBuildHeight(){return 320;}
                    public int getHeight(Heightmap.Types type,int x,int z) {
                        if(!loaded.test(new BlockPos(x,64,z))) throw new AssertionError("Height requested in unloaded column");
                        heightReads++; return 64;
                    }
                    public BlockState getBlockState(BlockPos pos) {
                        if(!loaded.test(pos)) throw new AssertionError("Block requested in unloaded chunk: "+pos);
                        reads++; return states.getOrDefault(pos,BlockState.AIR);
                    }
                    public List<Entity> getEntities(Entity exclude,AABB bounds,Predicate<Entity> predicate) {
                        entityQueries++; var result=new ArrayList<Entity>();
                        for(var entity:entities) if(entity!=exclude&&bounds.contains(entity.x,entity.y,entity.z)&&predicate.test(entity)) result.add(entity);
                        return result;
                    }
                }
                """);
        doubles.put("net/minecraft/client/Minecraft.java", """
                package net.minecraft.client;
                import net.minecraft.client.multiplayer.ClientLevel;
                import net.minecraft.world.entity.LivingEntity;
                public final class Minecraft {
                    public ClientLevel level=new ClientLevel();
                    public LivingEntity player=new LivingEntity(0,64,0);
                }
                """);
        doubles.put("com/trippycraft/client/DmtSamplerProbe.java", SAMPLER_PROBE);
        runFixtures(root, work.resolve("sources"), work.resolve("classes"), doubles,
                new String[]{"client/DmtWorldInfluenceSampler.java"}, "com.trippycraft.client.DmtSamplerProbe");
    }

    private static final String SAMPLER_PROBE = """
            package com.trippycraft.client;
            import net.minecraft.client.Minecraft;
            import net.minecraft.client.multiplayer.ClientLevel;
            import net.minecraft.core.BlockPos;
            import net.minecraft.world.entity.*;
            import net.minecraft.world.level.block.state.BlockState;
            import net.minecraft.world.level.material.MapColor;
            public final class DmtSamplerProbe {
                private static final Minecraft MC=new Minecraft();
                private static int checks;
                private static final int[][] SUPPORTS={{-3,-3},{0,-4},{3,-3},{4,0},{3,3},{0,4},{-3,3},{-4,0}};
                private static final int[][] ROOF={{0,0},{-2,0},{2,0},{0,-2},{0,2}};
                public static void run() {
                    boundsAndCache(); directionalEvidence(); entityHysteresis();
                    System.out.println("PASS: "+checks+" production-sampler fixture assertions");
                }
                private static void setup(){DmtWorldInfluenceSampler.reset();MC.level=new ClientLevel();MC.player=new LivingEntity(0,64,0);}
                private static DmtWorldInfluenceSampler.Sample at(long tick){MC.level.time=tick;return DmtWorldInfluenceSampler.sample(MC);}
                private static void put(int x,int y,int z,int rgb,boolean solid) {
                    MC.level.states.put(new BlockPos(x,y,z),new BlockState(false,solid,new MapColor(rgb)));
                }
                private static void boundsAndCache() {
                    setup(); var empty=at(0);
                    check(MC.level.reads==168,"Empty loaded sample did not obey its168 block-read budget");
                    check(MC.level.heightReads==8 && MC.level.entityQueries==1,"Supporting/query budget changed");
                    for(int tick=1;tick<20;tick++) check(at(tick)==empty,"Sub-second sample bypassed cache");
                    check(MC.level.reads==168 && MC.level.entityQueries==1,"Cached sample performed world work");
                    at(20); check(MC.level.reads==336 && MC.level.entityQueries==2,"20-tick cache did not refresh");
                    MC.player.x=8;at(21);check(MC.level.reads==504,"8-block travel did not invalidate cache");
                    at(0);check(MC.level.reads==672,"Backwards time retained a stale cache");
                    setup();MC.level.loaded=p->false;at(0);
                    check(MC.level.reads==0 && MC.level.heightReads==0,"Unloaded chunks were read or queried by heightmap");
                    setup();MC.level.loaded=p->p.getX()>=0;at(0);
                    check(MC.level.reads>0 && MC.level.reads<168,"Loaded boundary was not sampled within budget");
                    MC.player.y=318;at(20);check(MC.level.reads<=336,"Ceiling-bound sample exceeded budget");
                    var old=MC.level;MC.level=new ClientLevel();at(21);
                    check(MC.level.entityQueries==1,"New level retained old cached sample");
                    MC.level=null;near(DmtWorldInfluenceSampler.sample(MC).attractorStrength(),0,0,"Missing world retained entity");
                    MC.level=old;MC.player=null;near(DmtWorldInfluenceSampler.sample(MC).relief(),0,0,"Missing player did not use empty sample");
                    System.out.println("PASS: sampler read/query budgets,20-tick and movement cache, world/time invalidation, loaded-boundary guards and height limit");
                }
                private static void directionalEvidence() {
                    setup();for(var p:SUPPORTS)put(p[0],63,p[1],0xFF0000,true);
                    var floor=at(0);near(floor.red(),1,0,"Supporting color was lost");
                    check(floor.directionY()<0 && floor.directionStrength()>0,"Supporting terrain did not point down");
                    setup();for(var p:ROOF)put(p[0],67,p[1],0x0000FF,true);
                    var roof=at(0);near(roof.blue(),1,0,"Ceiling color was lost");
                    check(roof.directionY()>0 && roof.directionStrength()>0,"Ceiling did not point up");
                    setup();for(var p:ROOF)put(p[0],67,p[1],0x00FF00,false);
                    var canopy=at(0);near(canopy.green(),1,0,"Nonsolid canopy was ignored");
                    check(canopy.directionY()>0 && canopy.directionStrength()>0,"Canopy lacks directional presence");
                    setup();put(2,65,0,0x00FF00,true);
                    var wall=at(0);near(wall.green(),1,0,"Eye-height wall was ignored");
                    check(wall.directionX()>0 && wall.directionY()==0,"Wall direction was not horizontal");
                    setup();for(var p:SUPPORTS)put(p[0],63,p[1],0xFF0000,true);
                    for(var p:ROOF)put(p[0],67,p[1],0x0000FF,true);
                    var enclosed=at(0);
                    check(enclosed.red()>0 && enclosed.blue()>0,"Ceiling replaced rather than combined supporting color");
                    check(enclosed.relief()>floor.relief(),"Floor/ceiling separation did not affect spatial relief");
                    check(MC.level.reads<=168,"Occupied sample exceeded read budget");
                    System.out.println("PASS: actual supporting/wall/ceiling/canopy samples have distinct directional/color evidence and combine enclosure relief");
                }
                private static void entityHysteresis() {
                    setup();var a=new LivingEntity(10,64,0);var b=new LivingEntity(-10.2,64,0);
                    MC.level.entities.add(a);MC.level.entities.add(b);MC.level.entities.add(new Entity(1,64,0));
                    near(at(0).attractorX(),10,0,"Initial nearest living entity not selected");
                    b.x=-9.5;near(at(20).attractorX(),10,0,"Small nearest-order change replaced tracked identity");
                    b.x=-7.9;var replaced=at(40);near(replaced.attractorX(),-7.9,0,"Materially nearer entity failed to replace target");
                    near(replaced.attractorY(),65,0,"Entity attractor not placed at body center");
                    near(replaced.attractorStrength(),1-7.9/24,1e-12,"Entity strength not distance based");
                    a.x=7.8;near(at(60).attractorX(),-7.9,0,"New identity immediately flickered back");
                    b.alive=false;near(at(80).attractorX(),7.8,0,"Dead tracked target blocked replacement");
                    a.x=25;near(at(100).attractorStrength(),0,0,"Out-of-range entity retained attraction");
                    a.x=6;near(at(120).attractorX(),6,0,"Returning living target was not acquired");
                    DmtWorldInfluenceSampler.reset();b.alive=true;b.x=-5.8;
                    near(at(121).attractorX(),-5.8,0,"Explicit reset retained identity hysteresis");
                    MC.level=new ClientLevel();MC.level.entities.add(a);
                    near(at(122).attractorX(),6,0,"New world retained prior entity identity");
                    System.out.println("PASS: actual living-entity query filters, UUID hysteresis, stronger replacement, death/range loss and reset/world reacquisition");
                }
                private static void near(double actual,double expected,double tolerance,String message) {
                    check(Double.isFinite(actual)&&Math.abs(actual-expected)<=tolerance,message+": actual="+actual+", expected="+expected);
                }
                private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
            }
            """;

    private static final String PROBE = """
            package com.trippycraft.client;
            import com.trippycraft.config.TrippyClientConfig;
            import com.trippycraft.config.TrippyConfig;
            import com.trippycraft.substance.CompositeEffectResolver;
            import com.trippycraft.substance.dmt.*;
            import com.trippycraft.substance.lsd.LsdEffectState;
            import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
            import net.minecraft.client.Minecraft;
            import net.minecraft.world.phys.Vec3;
            public final class DmtFractalControlProbe {
                private static int assertions;
                private static double now;
                private static boolean started;
                private static final Minecraft MC=Minecraft.getInstance();
                public static void run() {
                    gates(); cinematicEntry(); cinematicReturn(); doseGrowth(); clockAndMotion(); stationaryAnimation();
                    qualityAndAccessibility(); mixtureAndIntensity(); liveIntensity(); scaffoldContinuity();
                    environmentMemory(); spatialWorldInfluence(); anchorsAndReset(); stereoHead();
                    System.out.println("PASS: "+assertions+" behavioral assertions");
                }
                private static void setup() {
                    DmtFractalWorldPass.reset(); now=0; started=false;
                    MC.level=new Minecraft.Level(); MC.player=new Object();
                    MC.gameRenderer.camera.position=new Vec3(10,64,-5);
                    VivecraftCompat.vr=false; VivecraftCompat.head=null;
                    TrippyClientConfig.dmtFractalWorld=true; TrippyClientConfig.rendererEnabled=true;
                    TrippyClientConfig.visualIntensity=1; TrippyClientConfig.motionIntensity=1;
                    TrippyClientConfig.dmtFractalIntensity=1; TrippyClientConfig.dmtFractalQuality=1;
                    TrippyClientConfig.motionReduction=false; TrippyClientConfig.photosensitivityReduction=false;
                    TrippyConfig.enableDmtEffects=true; TrippyConfig.dmtWorldWarp=true;
                    TrippyConfig.dmtAccessibilityMode=false; TrippyConfig.globalIntensity=1;
                    ClientExperienceStore.lsdState=LsdEffectState.inactive();
                    ClientExperienceStore.state=state(4,.5,1,1234567);
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.3,.6,.2,.1);
                    WorldWarpPostProcessor.calls.clear();
                }
                private static DmtEffectState state(double dose,double progress,double visual,long seed) {
                    return new DmtEffectState(true,dose,progress,visual,visual,5,
                        new DmtTripSignature(seed),new DmtFractalScaffold(.7,.6));
                }
                private static void environment(PsilocybinEnvironmentSnapshot environment) {
                    var s=ClientExperienceStore.state;
                    ClientExperienceStore.state=new DmtEffectState(s.active(),s.activeDoseUnits(),s.progress(),
                        s.visualIntensity(),s.intensity(),s.tier(),s.signature(),s.fractalScaffold(),environment);
                }
                private static WorldWarpPostProcessor.Draw at(double time) {
                    now=time; started=true; MC.level.time=(long)Math.floor(time);
                    WorldWarpPostProcessor.calls.clear();
                    DmtFractalWorldPass.render((float)(time-Math.floor(time)));
                    check(WorldWarpPostProcessor.calls.size()<=1,"Controller emitted duplicate draws");
                    if(WorldWarpPostProcessor.calls.isEmpty()) return null;
                    var draw=WorldWarpPostProcessor.calls.get(0);
                    check(draw.effect().equals("dmt_fractal_world_warp") && draw.depth(),"Wrong effect or missing depth request");
                    for(var uniform:draw.uniforms()) for(float value:uniform.values())
                        check(Float.isFinite(value),"Non-finite "+uniform.name());
                    check(a(draw)[0]>=0 && a(draw)[0]<=1,"Physical opacity exceeded its bounds");
                    return draw;
                }
                private static WorldWarpPostProcessor.Draw advance(double ticks) {
                    if(!started) at(0);
                    var draw=(WorldWarpPostProcessor.Draw)null;
                    while(ticks>0) {
                        double step=Math.min(1,ticks); draw=at(now+step); ticks-=step;
                    }
                    return draw==null ? at(now) : draw;
                }
                private static WorldWarpPostProcessor.Draw peak() {
                    setup(); var draw=advance(1000);
                    near(a(draw)[0],1,0,"High-dose isolated realm did not reach an opaque peak after cinematic entry");
                    return draw;
                }
                private static float[] a(WorldWarpPostProcessor.Draw draw) {
                    check(draw!=null,"Expected a realm draw"); return draw.get("FractalA");
                }
                private static double baseRate() {
                    return DmtRealmDynamics.transformationRate(DmtRealmDynamics.doseLog(4,2.4));
                }
                private static void gates() {
                    setup(); TrippyClientConfig.dmtFractalWorld=false;
                    for(int i=0;i<100;i++) check(advance(1)==null,"Disabled option emitted a draw");
                    TrippyClientConfig.dmtFractalWorld=true;
                    check(advance(1)==null,"Enabling resurrected stale opacity");
                    near(a(advance(1000))[0],1,0,"Enabled realm could not complete entry");
                    TrippyClientConfig.dmtFractalWorld=false;
                    check(advance(1)==null,"Off switch failed during a visible realm");
                    TrippyClientConfig.dmtFractalWorld=true;
                    check(advance(1)==null,"Off switch did not clear the controller");
                    for(int gate=0;gate<7;gate++) {
                        peak();
                        switch(gate) {
                            case 0 -> TrippyClientConfig.rendererEnabled=false;
                            case 1 -> TrippyConfig.enableDmtEffects=false;
                            case 2 -> TrippyConfig.dmtWorldWarp=false;
                            case 3 -> TrippyClientConfig.visualIntensity=0;
                            case 4 -> TrippyConfig.globalIntensity=0;
                            case 5 -> MC.player=null;
                            case 6 -> TrippyClientConfig.dmtFractalIntensity=0;
                        }
                        check(advance(1)==null,"Disabled renderer/intensity/player gate ignored: "+gate);
                    }
                    peak(); MC.level=null; WorldWarpPostProcessor.calls.clear(); DmtFractalWorldPass.render(0);
                    check(WorldWarpPostProcessor.calls.isEmpty(),"Missing world emitted a draw");
                    setup(); ClientExperienceStore.state=state(4,.10,1,1234567);
                    check(advance(1000)==null,"Pre-transition onset rendered the realm");
                    setup(); ClientExperienceStore.state=state(.2,.5,1,1234567);
                    check(advance(1000)==null,"Sub-threshold dose entered the realm");
                    setup(); ClientExperienceStore.state=state(4,.5,0,1234567);
                    check(advance(1000)==null,"Zero visual state rendered the realm");
                    System.out.println("PASS: disabled mode/renderer/intensity, absent world/player, onset, low-dose and zero-visual gates");
                }
                private static void cinematicEntry() {
                    for(double dose:new double[]{2.4,4,64,1.0e8}) {
                        double log=DmtRealmDynamics.doseLog(dose,2.4), last=0;
                        near(DmtRealmDynamics.cinematicEntry(0,log),0,0,"Cinematic entry starts above zero");
                        for(double seconds=.25;seconds<=35;seconds+=.25) {
                            double next=DmtRealmDynamics.cinematicEntry(seconds,log);
                            check(next>=last && next<=1,"Cinematic phase regressed or exceeded one"); last=next;
                        }
                        check(DmtRealmDynamics.cinematicEntry(20,log)<1,"Cinematic entry completed before deliberate ingress");
                        near(DmtRealmDynamics.cinematicEntry(35,log),1,0,"Cinematic entry did not complete within35 seconds");
                        check(DmtRealmDynamics.cinematicEntry(.1,log)<.001,"Cinematic entrance lacks eased start");
                    }
                    setup(); check(at(0)==null,"First frame skipped cinematic entry");
                    var five=advance(100); var ten=advance(100); var twenty=advance(200);
                    check(a(five)[0]>0 && a(five)[0]<a(ten)[0] && a(ten)[0]<a(twenty)[0],"Entry is not gradual over5/10/20 seconds");
                    check(twenty.get("RealmDynamics")[1]<1 && a(twenty)[0]<.95,"Scene became opaque before cinematic crossing completed");
                    var thirtyFive=advance(300);
                    near(thirtyFive.get("RealmDynamics")[1],1,0,"Controller did not finish cinematic ingress");
                    near(thirtyFive.get("RealmDynamics")[2],35,1e-5,"Trip clock is not real elapsed game seconds");
                    near(a(advance(300))[0],1,0,"Settled cinematic peak is not opaque");
                    ClientExperienceStore.state=state(4,.80,1,1234567);
                    float returning=a(advance(1))[0]; check(returning>0 && returning<1,"Return did not fade the realm");
                    ClientExperienceStore.state=state(4,.90,1,1234567);
                    check(advance(1100)==null,"Afterglow failed to restore old rendering");
                    peak(); ClientExperienceStore.state=DmtEffectState.inactive();
                    float fade=a(advance(1))[0]; check(fade>0 && fade<1,"Session end lost its retained fade");
                    check(advance(1100)==null,"Session end failed to release realm rendering");
                    check(advance(1)==null,"Sober frame resurrected realm");
                    System.out.println("PASS: eased 20-35-second cinematic ingress, real trip clock, gradual return and completed-session release");
                }
                private static void cinematicReturn() {
                    var full=peak();
                    same(full.get("RealmSettling"),new float[]{1,1,1,1},"Peak realm activity was reduced");
                    ClientExperienceStore.state=state(4,.8,1,1234567);
                    same(at(now).get("RealmSettling"),full.get("RealmSettling"),"Snapshot changed return gains without elapsed time");
                    var first=advance(1); float[] early=first.get("RealmSettling");
                    for(float gain:early) check(gain<1 && gain>.95,"Return activity snapped instead of easing");
                    near(a(first)[1]-a(full)[1],.05*baseRate()*early[1],1e-4,
                        "Return motion did not integrate only the next phase increment");
                    same(at(now).get("RealmSettling"),early,"Repeated eye timestamp advanced settling");
                    var later=advance(40);
                    for(int i=0;i<4;i++) check(later.get("RealmSettling")[i]<early[i],"Return gain failed to settle");
                    float[] anchor=later.get("RealmOrigin").clone();
                    ClientExperienceStore.state=state(136,.5,1,1234567);
                    same(at(now).get("RealmSettling"),later.get("RealmSettling"),"Redose reset settling state");
                    var resumed=advance(1);
                    check(resumed.get("RealmSettling")[1]>later.get("RealmSettling")[1],"Redose did not ease out of return");
                    check(a(resumed)[1]>a(later)[1],"Redose reset the integrated motion phase");
                    near(resumed.get("RealmDynamics")[2]-later.get("RealmDynamics")[2],.05,1e-4,"Return redose reset real trip time");
                    same(resumed.get("RealmOrigin"),anchor,"Return redose moved the realm anchor");
                    TrippyClientConfig.dmtFractalIntensity=.5;
                    same(at(now).get("RealmSettling"),resumed.get("RealmSettling"),"Nonzero intensity reset settling");
                    ClientExperienceStore.state=state(136,.9,1,1234567);
                    var afterglow=advance(250); float[] quiet=afterglow.get("RealmSettling");
                    float[] residual={.15f,.15f,.10f,.35f};
                    for(int i=0;i<4;i++) near(quiet[i],residual[i],.001,"Afterglow residual gain "+i);
                    check(a(afterglow)[0]>0,"Afterglow discarded opacity instead of retaining its fade");
                    TrippyClientConfig.motionIntensity=0;
                    float fixed=a(afterglow)[1]; var frozen=advance(20);
                    near(a(frozen)[1],fixed,0,"Return motion ignored motion zero");
                    check(frozen.get("RealmDynamics")[2]>afterglow.get("RealmDynamics")[2],"Return motion zero froze real trip time");
                    peak(); ClientExperienceStore.state=DmtEffectState.inactive();
                    var ended=advance(200);
                    for(int i=0;i<4;i++) near(ended.get("RealmSettling")[i],residual[i],.004,"Abrupt session end did not settle activity "+i);
                    check(a(ended)[0]>0 && a(ended)[0]<1,"Abrupt end bypassed the retained fade");
                    check(advance(1000)==null,"Settled session never released rendering");
                    same(peak().get("RealmSettling"),new float[]{1,1,1,1},"New trip retained old return gains");
                    ClientExperienceStore.state=state(4,.9,1,1234567);
                    check(advance(1100)==null,"Late active afterglow did not finish its fade");
                    MC.gameRenderer.camera.position=new Vec3(20,64,-5);
                    check(advance(1)==null,"Walking during invisible afterglow resurrected rendering");
                    ClientExperienceStore.state=state(136,.5,1,1234567);
                    var lateRedose=advance(1000);
                    near(lateRedose.get("RealmOrigin")[0],3.5,1e-6,"Invisible afterglow moved the same-trip redose anchor");
                    check(lateRedose.get("RealmDynamics")[2]>150,"Late redose reset the real trip clock");
                    System.out.println("PASS: return and abrupt end ease distortion/motion/ornament/light, preserve redose and setting continuity, and integrate future motion without phase resets");
                }
                private static void doseGrowth() {
                    double last=0;
                    for(double dose:new double[]{4,8.5,34,136,1000000,Double.MAX_VALUE}) {
                        double log=DmtRealmDynamics.doseLog(dose,2.4);
                        check(Double.isFinite(log) && log>last,"Logarithmic dose drive capped or became non-finite"); last=log;
                    }
                    check(Double.isFinite(DmtRealmDynamics.doseLog(Double.MAX_VALUE,Double.MIN_NORMAL)),"Dose/threshold division overflowed");
                    peak(); var before=at(now); float previous=before.get("RealmDynamics")[0];
                    double previousRate=DmtRealmDynamics.transformationRate(previous);
                    float[] origin=before.get("RealmOrigin").clone(), identity=before.get("FractalC").clone();
                    for(double dose:new double[]{8.5,34,136,1000000}) {
                        var old=at(now); ClientExperienceStore.state=state(dose,.5,1,1234567);
                        var first=advance(1);
                        near(first.get("RealmDynamics")[2]-old.get("RealmDynamics")[2],.05,1e-4,"Redose restarted or jumped trip time");
                        double nextRate=DmtRealmDynamics.transformationRate(DmtRealmDynamics.doseLog(dose,2.4));
                        double firstPhaseStep=a(first)[1]-a(old)[1];
                        check(firstPhaseStep>0 && firstPhaseStep<=.05*nextRate+1e-4,
                            "Redose rescaled elapsed phase instead of integrating its new speed");
                        near(first.get("RealmDynamics")[1],old.get("RealmDynamics")[1],0,"Redose changed completed ingress phase");
                        var settled=advance(1000); var dynamics=settled.get("RealmDynamics");
                        check(dynamics[0]>previous,"Beyond-maximum dose stopped increasing geometry drive");
                        near(dynamics[0],DmtRealmDynamics.doseLog(dose,2.4),1e-4,"Uncapped dose drive not forwarded");
                        near(dynamics[3],DmtRealmDynamics.doseLog(dose),1e-4,"Actual raw dose logarithm not forwarded");
                        near(a(settled)[0],1,0,"Uncapped complexity exceeded or reduced physical opacity");
                        same(settled.get("RealmOrigin"),origin,"Redose moved realm anchor");
                        for(int i=1;i<4;i++) near(settled.get("FractalC")[i],identity[i],0,"Redose rerolled realm identity");
                        previous=dynamics[0];
                        check(nextRate>previousRate,"Higher dose did not increase transformation rate");
                        float phase=a(settled)[1]; var second=advance(20);
                        near(a(second)[1]-phase,nextRate,2e-4,"Actual phase speed ignored settled dose rate");
                        previousRate=nextRate;
                    }
                    // Redosing during entry changes dose strength, not the elapsed
                    // cinematic phase or its independent real-time clock.
                    setup(); var entry=advance(200); ClientExperienceStore.state=state(136,.5,1,1234567);
                    var redosed=advance(1);
                    near(redosed.get("RealmDynamics")[2]-entry.get("RealmDynamics")[2],.05,1e-5,"Entry redose changed trip-clock origin");
                    check(redosed.get("RealmDynamics")[1]<.5,"Entry redose jumped straight to full crossing");
                    advance(1000); float highDosePhase=a(at(now))[1];
                    TrippyClientConfig.motionIntensity=0;
                    near(a(advance(20))[1],highDosePhase,0,"High dose overrode the zero-motion control");
                    System.out.println("PASS: uncapped logarithmic detail/speed growth, high-dose motion freeze and continuous redose identity/anchor/cinematic time");
                }
                private static void clockAndMotion() {
                    var base=peak(); float phase=a(base)[1];
                    near(a(at(now))[1],phase,0,"Paused timestamp advanced animation");
                    near(a(at(now-1))[1],phase,0,"Reversed clock moved animation");
                    near(a(at(now+10000))[1]-phase,.25*baseRate(),1e-4,"Large game-clock discontinuity was not capped");
                    peak(); TrippyClientConfig.motionIntensity=0; var paused=advance(1);
                    float fixed=a(paused)[1], trip=paused.get("RealmDynamics")[2];
                    var later=advance(20); near(a(later)[1],fixed,0,"Motion0 failed to freeze field animation");
                    near(later.get("RealmDynamics")[2]-trip,1,1e-5,"Motion0 incorrectly froze cinematic/game time");
                    TrippyClientConfig.motionIntensity=1;
                    near(a(advance(1))[1]-fixed,.05*baseRate(),1e-4,"Motion resume rescaled elapsed phase");
                    var clipped=at(now); TrippyClientConfig.motionIntensity=-100;
                    near(a(advance(1))[1],a(clipped)[1],0,"Negative motion did not clip to zero");
                    TrippyClientConfig.motionIntensity=100;
                    near(a(advance(1))[1]-a(clipped)[1],.10*baseRate(),1e-4,"High motion did not clip to double speed");
                    setup(); TrippyClientConfig.motionReduction=true;
                    near(a(advance(1000))[1],7.5*baseRate(),1e-4,"Motion reduction multiplier");
                    setup(); TrippyConfig.dmtAccessibilityMode=true;
                    near(a(advance(1000))[1],12.5*baseRate(),1e-4,"DMT accessibility motion multiplier");
                    System.out.println("PASS: pause/reverse/gap clocks, motion clipping, independent cinematic time and reduction rates");
                }
                private static void stationaryAnimation() {
                    var first=peak(); var snapshot=ClientExperienceStore.state;
                    float phase=a(first)[1], start=phase; double begin=now;
                    float[] origin=first.get("RealmOrigin").clone();
                    for(int frame=1;frame<=1200;frame++) {
                        var draw=at(begin+frame/60.0); float next=a(draw)[1];
                        check(next>phase,"Stationary fractional frame froze animation: "+frame);
                        check(ClientExperienceStore.state==snapshot && snapshot.ageTicks()==100,"Snapshot fixture age unexpectedly advanced");
                        same(draw.get("RealmOrigin"),origin,"Stationary animation moved the camera anchor"); phase=next;
                    }
                    near(phase-start,baseRate(),1e-4,"20 game ticks did not advance field phase at its dose-dependent rate");
                    near(a(at(now))[1],phase,0,"Paused stationary frame changed phase");
                    ClientExperienceStore.state=state(4,.5,1,1234567);
                    near(a(advance(.25))[1]-phase,.0125*baseRate(),1e-4,"Equivalent synchronized snapshot restarted animation");
                    System.out.println("PASS: 1,200 stationary fractional frames animate with identical snapshot age and stable anchor");
                }
                private static void qualityAndAccessibility() {
                    peak();
                    for(int quality:new int[]{-10,0,1,2,10}) {
                        TrippyClientConfig.dmtFractalQuality=quality;
                        near(a(at(now))[2],Math.max(0,Math.min(2,quality)),0,"Quality bounds");
                    }
                    near(a(at(now))[3],0,0,"Unexpected soft-color flag");
                    TrippyClientConfig.photosensitivityReduction=true;
                    near(a(at(now))[3],1,0,"Photosensitivity flag not forwarded");
                    TrippyClientConfig.photosensitivityReduction=false; TrippyConfig.dmtAccessibilityMode=true;
                    near(a(at(now))[3],1,0,"DMT accessibility flag not forwarded");
                    System.out.println("PASS: live quality bounds and both soft-color controls");
                }
                private static void mixtureAndIntensity() {
                    setup(); TrippyClientConfig.visualIntensity=.5; TrippyConfig.globalIntensity=.5;
                    near(a(advance(1000))[0],.25,1e-5,"Client/common visual gain multiplication");
                    setup(); ClientExperienceStore.lsdState=new LsdEffectState(true,.5,7,7,250,5);
                    double gain=CompositeEffectResolver.resolve(ClientExperienceStore.psilocybin(),ClientExperienceStore.lsd(),
                        ClientExperienceStore.dmt(),ClientExperienceStore.hoMet(),ClientExperienceStore.alLad(),ClientExperienceStore.twoCe()).dmtShaderGain();
                    check(gain>0 && gain<1,"Fixture missed real mixture weighting");
                    near(a(advance(1000))[0],gain,1e-5,"Production mixture gain ignored");
                    for(double setting:new double[]{.25,.5,.75,1}) {
                        setup(); TrippyClientConfig.dmtFractalIntensity=setting;
                        var draw=advance(1000); near(a(draw)[0],setting,1e-5,"Selected peak intensity");
                        near(draw.get("RealmDynamics")[0],DmtRealmDynamics.doseLog(4,2.4)*setting,1e-4,"Selected intensity did not scale geometric dose drive");
                    }
                    System.out.println("PASS: production mixture weighting and independent selected/global intensity scale opacity and dose drive");
                }
                private static void liveIntensity() {
                    peak(); MC.gameRenderer.camera.position=new Vec3(12,65,-7); var before=advance(1);
                    float[] origin=before.get("RealmOrigin").clone(), identity=before.get("FractalC").clone();
                    for(double selected:new double[]{.25,.75,.5,1,.33}) {
                        var old=at(now); TrippyClientConfig.dmtFractalIntensity=selected;
                        var changed=advance(1);
                        check(a(changed)[0]<=selected+1e-6,"Live intensity failed to cap existing opacity");
                        double selectedRate=DmtRealmDynamics.transformationRate(DmtRealmDynamics.doseLog(4,2.4)*selected);
                        near(a(changed)[1]-a(old)[1],.05*selectedRate,1e-4,"Nonzero intensity reset/rescaled animation");
                        near(changed.get("FractalC")[0],(float)selected,0,"Selected intensity missing from FractalC");
                        same(changed.get("RealmOrigin"),origin,"Nonzero intensity moved anchor");
                        for(int i=1;i<4;i++) near(changed.get("FractalC")[i],identity[i],0,"Nonzero intensity rerolled identity");
                        near(changed.get("RealmDynamics")[2]-old.get("RealmDynamics")[2],.05,1e-4,"Nonzero intensity reset cinematic clock");
                        near(a(advance(1000))[0],selected,1e-5,"Live intensity failed to settle");
                    }
                    TrippyClientConfig.dmtFractalIntensity=0; check(advance(1)==null,"Intensity0 retained visible realm");
                    TrippyClientConfig.dmtFractalIntensity=1; check(advance(1)==null,"Re-enabled intensity resurrected stale opacity");
                    var fresh=advance(100); check(fresh.get("RealmDynamics")[1]<.1,"Re-enabled intensity inherited completed crossing");
                    long bits=(0x12AL<<5)|(0xABCL<<17)|(0x67DL<<31)|(0xD39L<<43);
                    for(long seed:new long[]{bits,-1L,Long.MIN_VALUE,1234567L}) {
                        setup(); ClientExperienceStore.state=state(4,.5,1,seed); var draw=advance(1000);
                        float[] c=draw.get("FractalC");
                        near(c[1],((seed>>>17)&4095L)/4095.0F,0,"Seed C bit slice lost");
                        near(c[2],((seed>>>43)&4095L)/4095.0F,0,"Seed D bit slice lost");
                        near(c[3],ClientExperienceStore.state.signature().hyperspaceStyle(),0,"Session style lost");
                    }
                    System.out.println("PASS: live intensity continuity, immediate lower caps, zero/reset and disparate seed/style identity forwarding");
                }
                private static void scaffoldContinuity() {
                    var before=peak(); var s=ClientExperienceStore.state;
                    ClientExperienceStore.state=new DmtEffectState(s.active(),s.activeDoseUnits(),s.progress(),s.visualIntensity(),
                        s.intensity(),s.tier(),s.signature(),new DmtFractalScaffold(.1,.2),s.environment());
                    var changed=advance(1); float[] b=changed.get("FractalB");
                    check(b[2]>.1 && b[2]<before.get("FractalB")[2],"Snapshot depth snapped instead of easing");
                    check(b[3]>.2 && b[3]<before.get("FractalB")[3],"Snapshot flow snapped instead of easing");
                    same(at(now).get("FractalB"),b,"Duplicate stereo timestamp advanced scaffold easing");
                    var settled=advance(500);
                    near(settled.get("FractalB")[2],.1,1e-5,"Depth easing failed to converge");
                    near(settled.get("FractalB")[3],.2,1e-5,"Flow easing failed to converge");
                    ClientExperienceStore.state=state(4,.5,1,7654321); check(at(now)==null,"New trip retained old opacity");
                    var fresh=advance(100);
                    near(fresh.get("FractalB")[2],.7,1e-6,"New trip retained previous depth memory");
                    near(fresh.get("FractalB")[3],.6,1e-6,"New trip retained previous flow memory");
                    System.out.println("PASS: snapshot structural easing, duplicate-frame invariance and new-trip reset");
                }
                private static void environmentMemory() {
                    setup(); environment(new PsilocybinEnvironmentSnapshot(1,0,0,0,0,1,0,1,0,0,0,0,0));
                    var forest=advance(1000); float[] identity=forest.get("FractalC").clone(), origin=forest.get("RealmOrigin").clone();
                    check(forest.get("WorldInfluenceA")[0]>0,"Forest/organic snapshot did not influence opaque realm");
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.8,.2,.1,.9);
                    environment(new PsilocybinEnvironmentSnapshot(0,1,1,1,1,0,1,.1,1,1,1,.7,1));
                    var early=advance(1);
                    check(early.get("WorldInfluenceA")[0]>0,"Environment memory discarded previous organic world in one frame");
                    same(at(now).get("WorldInfluenceA"),early.get("WorldInfluenceA"),"Repeated eye advanced world memory");
                    var changed=advance(1500); float[] wa=changed.get("WorldInfluenceA"), wb=changed.get("WorldInfluenceB"), wc=changed.get("WorldInfluenceC");
                    near(a(changed)[0],1,0,"Opaque environment influence depended on visible source image");
                    check(wa[1]>forest.get("WorldInfluenceA")[1] && wa[2]>forest.get("WorldInfluenceA")[2]
                        && wa[3]>forest.get("WorldInfluenceA")[3],"Constructed/cave/water environment changes were not forwarded");
                    check(wb[1]>forest.get("WorldInfluenceB")[1] && wb[2]>forest.get("WorldInfluenceB")[2]
                        && wb[3]>forest.get("WorldInfluenceB")[3],"Night/rain/entities/movement influence was not forwarded");
                    check(wb[0]<forest.get("WorldInfluenceB")[0],"Changing ambient light was not forwarded");
                    check(wc[0]>forest.get("WorldInfluenceC")[0] && wc[3]>forest.get("WorldInfluenceC")[3],"Actual terrain color/relief was not forwarded");
                    near(wc[0],.8,1e-6,"Terrain red channel"); near(wc[1],.2,1e-6,"Terrain green channel");
                    near(wc[2],.1,1e-6,"Terrain blue channel"); near(wc[3],.9,1e-6,"Terrain relief channel");
                    same(changed.get("FractalC"),identity,"Environment change rerolled trip seed/style");
                    same(changed.get("RealmOrigin"),origin,"Environment change moved realm anchor");
                    MC.gameRenderer.camera.position=new Vec3(12,64,-5); var moved=at(now);
                    same(moved.get("WorldInfluenceA"),wa,"Camera movement changed environment memory at same timestamp");
                    same(moved.get("WorldInfluenceB"),wb,"Camera movement changed dynamic world influence");
                    same(moved.get("WorldInfluenceC"),wc,"Camera movement changed cached terrain influence");
                    System.out.println("PASS: opaque realm receives organic/constructed/cave/water/light/weather/entities/movement and real terrain inputs with smooth world memory");
                }
                private static void anchorsAndReset() {
                    setup(); ClientExperienceStore.state=state(4,.1,1,1234567); advance(100);
                    MC.gameRenderer.camera.position=new Vec3(1010,64,-5); advance(1);
                    ClientExperienceStore.state=state(4,.5,1,1234567); var first=advance(100);
                    near(first.get("RealmOrigin")[0],0,0,"Walking before entry displaced entrance");
                    advance(1000); MC.gameRenderer.camera.position=new Vec3(1012,67,-9);
                    var moved=advance(1).get("RealmOrigin");
                    near(moved[0],.7,1e-6,"World parallax x"); near(moved[1],1.05,1e-6,"World parallax y"); near(moved[2],-1.4,1e-6,"World parallax z");
                    ClientExperienceStore.state=state(4,.5,1,2222222); check(at(now)==null,"New signature retained old realm");
                    var fresh=advance(1000); near(fresh.get("RealmOrigin")[0],0,0,"New trip failed to reanchor");
                    MC.level=new Minecraft.Level(); check(at(now)==null,"New world retained old opacity");
                    var world=advance(1000); near(world.get("RealmDynamics")[2],50,1e-5,"New world retained old trip clock");
                    DmtFractalWorldPass.reset(); check(at(now)==null,"Explicit reset retained old opacity");
                    System.out.println("PASS: pre-entry walking, world parallax, new-signature/world and explicit lifecycle resets");
                }
                private static void spatialWorldInfluence() {
                    setup();
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.3,.6,.2,.1,
                        -.6,.3,-.8,.75,30,70,-25,.8);
                    var initial=advance(1000);
                    float[] direction=initial.get("WorldInfluenceD").clone();
                    near(direction[0],-.6,1e-6,"Negative terrain gradient X was clipped");
                    near(direction[1],.3,1e-6,"Terrain gradient Y was lost");
                    near(direction[2],-.8,1e-6,"Negative terrain gradient Z was clipped");
                    near(direction[3],.75,1e-6,"Terrain directional strength was lost");
                    float[] entity=initial.get("WorldAttractor").clone();
                    near(entity[0],7,1e-5,"World entity X did not map relative to trip anchor");
                    near(entity[1],2.1,1e-5,"World entity Y did not map relative to trip anchor");
                    near(entity[2],-7,1e-5,"Negative entity Z was clipped or used head coordinates");
                    near(entity[3],.8,1e-6,"Entity strength was not forwarded");
                    // A stationary cached WORLD entity must not follow the head.
                    MC.gameRenderer.camera.position=new Vec3(13,66,-9);
                    var movedHead=at(now);
                    same(movedHead.get("WorldAttractor"),entity,"Stationary entity followed head movement");
                    near(movedHead.get("RealmOrigin")[0],1.05,1e-6,"Head movement did not preserve world-space separation");
                    float[] identity=movedHead.get("FractalC").clone(), anchor=movedHead.get("RealmOrigin").clone();
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.3,.6,.2,.1,
                        .8,-.2,.6,.5,-20,50,35,.9);
                    var changing=advance(1); float[] first=changing.get("WorldAttractor");
                    check(first[0]<entity[0] && first[0]>-10.5,"Entity switch snapped instead of easing");
                    check(changing.get("WorldInfluenceD")[0]>direction[0]
                        && changing.get("WorldInfluenceD")[0]<.8,"Terrain direction change snapped instead of easing");
                    near(a(changing)[1]-a(movedHead)[1],.05*baseRate(),1e-4,"Entity update restarted animation");
                    near(changing.get("RealmDynamics")[2]-movedHead.get("RealmDynamics")[2],.05,1e-4,"Entity update restarted cinematic time");
                    same(changing.get("FractalC"),identity,"Spatial world update rerolled realm identity");
                    same(changing.get("RealmOrigin"),anchor,"Spatial world update moved realm anchor");
                    same(at(now).get("WorldAttractor"),first,"Repeated stereo frame advanced entity smoothing");
                    var settled=advance(1500); float[] target=settled.get("WorldAttractor").clone();
                    near(target[0],-10.5,1e-5,"Entity attractor X easing did not converge");
                    near(target[1],-4.9,1e-5,"Entity attractor Y easing did not converge");
                    near(target[2],14,1e-5,"Entity attractor Z easing did not converge");
                    near(target[3],.9,1e-6,"Entity attractor strength easing did not converge");
                    float[] terrain=settled.get("WorldInfluenceD");
                    near(terrain[0],.8,1e-6,"Positive terrain gradient X");
                    near(terrain[1],-.2,1e-6,"Negative terrain gradient Y");
                    near(terrain[2],.6,1e-6,"Positive terrain gradient Z");
                    near(terrain[3],.5,1e-6,"Changed terrain directional strength");
                    // Disappearing entities must fade without an attraction point
                    // suddenly sweeping toward world/realm zero.
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.3,.6,.2,.1,
                        .8,-.2,.6,.5,0,0,0,0);
                    var absent=advance(1); float[] fading=absent.get("WorldAttractor");
                    for(int i=0;i<3;i++) near(fading[i],target[i],0,"Absent entity pulled retained position toward zero");
                    check(fading[3]>0 && fading[3]<target[3],"Missing entity did not fade strength gradually");
                    var disappeared=advance(1000); float[] gone=disappeared.get("WorldAttractor");
                    for(int i=0;i<3;i++) near(gone[i],target[i],0,"Faded entity lost retained position");
                    near(gone[3],0,1e-5,"Missing entity attraction never faded out");
                    same(disappeared.get("FractalC"),identity,"Entity disappearance reset realm identity");
                    near(a(disappeared)[0],1,0,"Entity disappearance reset opaque coverage");
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.3,.6,.2,.1,
                        .8,-.2,.6,.5,25,68,-15,.7);
                    var returned=advance(1); float[] reappearing=returned.get("WorldAttractor");
                    check(reappearing[0]>target[0] && reappearing[0]<5.25,
                        "Reappearing entity snapped to a new position instead of easing from retained memory");
                    check(reappearing[3]>gone[3] && reappearing[3]<.7,"Reappearing entity skipped strength easing");
                    same(returned.get("FractalC"),identity,"Entity reappearance reset realm identity");
                    same(returned.get("RealmOrigin"),anchor,"Entity reappearance reset realm anchor");
                    near(returned.get("RealmDynamics")[2]-disappeared.get("RealmDynamics")[2],.05,1e-4,
                        "Entity reappearance restarted cinematic time");
                    System.out.println("PASS: signed terrain directions and trip-anchored entity positions ease across updates; head motion, disappearance and stereo replay preserve identity/clocks");
                }
                private static void stereoHead() {
                    setup(); VivecraftCompat.vr=true; VivecraftCompat.head=new Vec3(100,65,100);
                    DmtWorldInfluenceSampler.value=new DmtWorldInfluenceSampler.Sample(.3,.6,.2,.1,
                        -.4,.2,.7,.6,101,67,98,.8);
                    MC.gameRenderer.camera.position=new Vec3(-999,2,-999); var left=advance(1000);
                    near(left.get("RealmOrigin")[0],0,0,"VR anchor used eye camera instead of shared head");
                    near(left.get("WorldAttractor")[0],.35,1e-6,"VR entity anchor used eye instead of shared head X");
                    near(left.get("WorldAttractor")[1],.7,1e-6,"VR entity anchor used eye instead of shared head Y");
                    near(left.get("WorldAttractor")[2],-.7,1e-6,"VR entity anchor used eye instead of shared head Z");
                    MC.gameRenderer.camera.position=new Vec3(100.032,65,100); var right=at(now);
                    for(String uniform:new String[]{"FractalA","FractalB","FractalC","RealmDynamics","RealmSettling","WorldInfluenceA","WorldInfluenceB","WorldInfluenceC","WorldInfluenceD","WorldAttractor","RealmOrigin"})
                        same(right.get(uniform),left.get(uniform),"Stereo replay changed "+uniform);
                    VivecraftCompat.head=new Vec3(101,65,100);
                    var moved=advance(1);
                    near(moved.get("RealmOrigin")[0],.35,1e-6,"Tracked head translation lost realm parallax");
                    same(moved.get("WorldAttractor"),left.get("WorldAttractor"),"Stationary VR entity followed tracked head translation");
                    System.out.println("PASS: shared tracked-head anchor and identical dose/cinematic/environment uniforms across repeated eye timestamps");
                }
                private static void same(float[] actual,float[] expected,String message) {
                    check(actual.length==expected.length,message+" length");
                    for(int i=0;i<actual.length;i++) near(actual[i],expected[i],0,message+"["+i+"]");
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
