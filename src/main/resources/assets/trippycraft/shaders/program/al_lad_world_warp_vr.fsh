#version 150

// AL-LAD: living source patterns, spectral pigment flow and planar split imagery.
// Source materials carry the signature; no excavated volumes or geometric shading.
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform mat4 WorldViewProjection;
uniform mat4 WorldInverseViewProjection;
uniform vec4 WorldEyePosition;
uniform vec4 WorldHeadPosition;
uniform vec4 WarpA; // presence, stained glass, prism depth, wrapped ticks
uniform vec4 WarpB; // pane motion, chromatic fringe, recursion, jewel
uniform vec4 WarpC; // music, seed, observation, wrapped colour cycle [0,1)
uniform vec4 Intensity; // native effect gates, heroic lift
uniform vec4 ExtraDose; // phase contours, laminar depth, recursive sheets, interference volumes
uniform vec4 HighDose; // phase multiplicity, laminar complexity, recursive interference, immersive continuity
uniform vec4 MusicA;
uniform vec4 MotionA;
uniform vec4 MotionB;
uniform vec4 Focus;
uniform vec4 MixA; // LSD, psilocybin, 4-HO-MET, 2C-B
uniform vec4 Control; // colour, motion, environmental light, architecture

out vec4 fragColor;
const float TAU = 6.28318530718;
const vec3 LUMA = vec3(0.299, 0.587, 0.114);
// Artistic amplitude at every active dose; phase, dose thresholds and clocks stay native.
const float AL_LAD_GAIN = 2.0;

float nonzero(float x) { return abs(x) < 0.000001 ? (x < 0.0 ? -0.000001 : 0.000001) : x; }
float response(float x, float k) { return 1.0 - exp(-max(x, 0.0) * k); }
float stroke(float x, float width, float aa) { return 1.0 - smoothstep(width, width + max(aa, 0.0001), abs(x)); }
mat2 rotate2(float x) { float c = cos(x), s = sin(x); return mat2(c, -s, s, c); }
vec3 relativeAt(vec2 uv, float depth) {
    vec4 p = WorldInverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / nonzero(p.w);
}
vec3 worldAt(vec2 uv, float depth) { return WorldEyePosition.xyz + relativeAt(uv, depth); }
vec2 projectWorld(vec3 p) {
    vec4 clip = WorldViewProjection * vec4(p - WorldEyePosition.xyz, 1.0);
    return clip.xy / nonzero(clip.w) * 0.5 + 0.5;
}
float hash21(vec2 p) {
    // All cell grids divide the 4096-block floating origin.
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}
vec3 filmSpectrum(float p) {
    // Cool spectral highlights keep source pigments rather than tinting whole panes.
    vec3 rgb = 0.50 + 0.50 * cos(TAU * (p + vec3(0.0, -0.34, -0.67)));
    return mix(rgb * vec3(0.82, 0.96, 1.0), vec3(0.34, 0.63, 0.96), 0.24);
}

vec2 safeSource(vec2 fallback, vec3 source, vec3 surface, vec3 normal) {
    vec2 uv = projectWorld(source);
    float inFront = step(0.000001, (WorldViewProjection * vec4(source - WorldEyePosition.xyz, 1.0)).w);
    vec2 margin = 1.5 / max(OutSize, vec2(1.0));
    float inside = step(margin.x, uv.x) * step(uv.x, 1.0 - margin.x)
                 * step(margin.y, uv.y) * step(uv.y, 1.0 - margin.y);
    uv = clamp(uv, margin, vec2(1.0) - margin);
    float depth = texture(DepthSampler, uv).r;
    vec3 samplePoint = worldAt(uv, depth);
    float distance = length(surface - WorldEyePosition.xyz);
    float face = 1.0 - smoothstep(0.035 + distance * 0.0015, 0.16 + distance * 0.0035,
                                  abs(dot(samplePoint - surface, normal)));
    float local = 1.0 - smoothstep(0.035 * AL_LAD_GAIN, 0.09 * AL_LAD_GAIN, length(uv - fallback));
    vec2 mixed = mix(fallback, uv, inFront * inside * face * local * (1.0 - step(0.999995, depth)));
    // Validate the actual sampled point after local interpolation too. An
    // intervening foreground strip must not be pulled into the background.
    float mixedDepth = texture(DepthSampler, mixed).r;
    vec3 mixedPoint = worldAt(mixed, mixedDepth);
    if (mixedDepth >= 0.999995 || abs(dot(mixedPoint - surface, normal)) > 0.035 + distance * 0.0015) {
        return fallback;
    }
    return mixed;
}


// All fields are continuous at 32 blocks and at the shared 96000-tick cycle.
// Transport lives on the physical face: it never opens a surface or adds a back wall.
vec2 surfaceTransport(vec2 p, float clock, float seed, float motion,
                      vec4 extra, vec4 high) {
    p = mod(p, vec2(32.0));
    float a = p.x * TAU / 8.0 + p.y * TAU / 16.0;
    float b = p.y * TAU / 8.0 - p.x * TAU / 32.0;
    float tA = sin(clock * 31.0 + seed * TAU) * motion * 0.85;
    float tB = sin(clock * 43.0 - seed * TAU) * motion * 0.70;
    float u = a + sin(b + tA) * (0.55 + high.x * 0.35) + tB;
    float v = b + sin(a - tB) * (0.35 + high.w * 0.30) - tA;
    vec2 current = vec2(sin(u), cos(v) * 0.68);
    vec2 secondary = vec2(sin(u * 2.0 - v + sin(v * 3.0) * 0.35),
                          cos(v * 2.0 + u + sin(u * 2.0) * 0.30));
    current += secondary * (extra.z * 0.16 + high.x * 0.28);
    vec2 recursive = vec2(sin(u * 3.0 + sin(v * 2.0 + u) * 0.50),
                          cos(v * 3.0 - sin(u * 2.0 - v) * 0.50));
    current += recursive * high.z * 0.16;
    current += vec2(sin(a * 0.5 + sin(b) * 0.65 + tB),
                    cos(b + sin(a * 0.5) * 0.55 - tA)) * high.w * 0.25;
    return current;
}

vec2 transportedSource(vec2 fallback, vec3 world, vec3 normal,
                       vec3 tangentU, vec3 tangentV, vec2 offset) {
    vec3 candidate = world + tangentU * offset.x + tangentV * offset.y;
    vec2 delta = projectWorld(candidate) - fallback;
    // Smooth local magnification limit keeps the grain active near a wall instead
    // of allowing a large displacement to collapse back to the unmodified image.
    float radius = 0.057 * AL_LAD_GAIN;
    float limit = inversesqrt(1.0 + dot(delta, delta) / (radius * radius));
    vec3 fallbackRay = relativeAt(fallback, 0.99999);
    float along = dot(world - WorldEyePosition.xyz, normal) / nonzero(dot(fallbackRay, normal));
    vec3 anchor = WorldEyePosition.xyz + fallbackRay * along;
    candidate = anchor + (candidate - anchor) * limit;
    return safeSource(fallback, candidate, world, normal);
}

vec3 retainLuminance(vec3 pigment, float luminance) {
    return max(pigment, vec3(0.0)) * max(luminance, 0.0)
         / max(dot(max(pigment, vec3(0.0)), LUMA), 0.0001);
}

vec3 pigmentFlow(vec3 material, vec2 p, float clock, float seed, float strength,
                 vec4 extra, vec4 high, float colour) {
    float luminance = dot(material, LUMA);
    float chroma = clamp(length(material - vec3(luminance)) * 2.5, 0.0, 1.0);
    p = mod(p, vec2(32.0));
    float u = p.x * TAU / 8.0 + p.y * TAU / 16.0;
    float v = p.y * TAU / 8.0 - p.x * TAU / 32.0;
    float migration = 0.5 + 0.5 * sin(u + sin(v) * 0.70 + sin(clock * 29.0) * 0.80);
    // Existing pigments separate and exchange along their own material patterns.
    // Preserve brightness: no dark center, lip, carved groove or depression.
    vec3 exchanged = mix(material, material.brg, migration * (0.28 + chroma * 0.42));
    exchanged = retainLuminance(exchanged, luminance);
    vec3 spectrum = filmSpectrum(seed + sin(u + v) * 0.12 + sin(v * 2.0) * 0.08
                                + clock / TAU * 23.0 + chroma * 0.22);
    vec3 selected = retainLuminance(spectrum * (0.22 + material), luminance);
    float autonomy = (0.20 + extra.x * 0.24 + high.z * 0.24 + high.w * 0.12)
                   * (0.50 + chroma * 0.50);
    vec3 evolving = mix(exchanged, selected, clamp(autonomy, 0.0, 0.82));
    float amount = AL_LAD_GAIN * strength * colour * (0.38 + extra.x * 0.30 + high.x * 0.18 + high.z * 0.12);
    return mix(material, evolving, clamp(amount, 0.0, 1.0));
}

vec3 sourceEchoes(vec3 material, vec2 sourceUv, vec3 world, vec3 normal,
                  vec3 tangentU, vec3 tangentV, vec2 face, float aa,
                  float clock, float seed, float strength, vec4 extra, vec4 high,
                  float motion, float colour, float attention, float music) {
    float resolved = 1.0 - smoothstep(0.07, 0.25, aa);
    float drive = AL_LAD_GAIN * strength * resolved * (0.035 + extra.y * 0.07 + extra.z * 0.08
                + high.y * 0.18 + high.w * 0.07);
    vec3 source = material;
    float luminance = dot(material, LUMA);
    for (int layer = 0; layer < 3; layer++) {
        float level = float(layer);
        vec2 field = surfaceTransport(face + vec2(level * 4.0 + 2.0, -level * 8.0 - 4.0),
                                      clock, seed + level * 0.11 + 0.23, motion, extra, high);
        float separation = AL_LAD_GAIN * (0.045 + extra.y * 0.11 + high.y * 0.19 + high.w * 0.10)
                         * (0.65 + level * 0.30);
        vec2 echoUv = transportedSource(sourceUv, world, normal, tangentU, tangentV,
                                        field * separation * motion);
        vec3 echo = texture(DiffuseSampler, echoUv).rgb;
        float salience = clamp(length(echo - source) * 2.4, 0.0, 1.0);
        // Separate real source images in colour as their local phase diverges.
        vec3 split = mix(echo, vec3(echo.r, source.g, echo.b), colour * (0.35 + high.y * 0.20));
        split = retainLuminance(split, luminance);
        float weight = drive * (0.15 + salience * 0.85) * (0.85 + attention * 0.10 + music * 0.05);
        material = mix(material, split, clamp(weight, 0.0, 0.27 * AL_LAD_GAIN));
    }
    return material;
}

vec3 patternInterference(vec3 material, vec3 first, vec3 second, vec2 p,
                         float clock, float seed, float grainEdge, float aa,
                         float strength, vec4 extra, vec4 high, float colour) {
    float resolved = 1.0 - smoothstep(0.07, 0.24, aa);
    float luminance = dot(material, LUMA);
    float disagreement = clamp(length(first - second) * 2.2, 0.0, 1.0);
    float structure = clamp(grainEdge * 0.65 + disagreement * 0.75, 0.0, 1.0);
    p = mod(p, vec2(32.0));
    float a = p.x * TAU / 4.0 + p.y * TAU / 8.0;
    float b = p.y * TAU / 4.0 - p.x * TAU / 16.0;
    float weave = 0.5 + 0.5 * sin(a + sin(b * 2.0 + sin(clock * 53.0) * 0.45) * 0.70);
    vec3 recombined = mix(first, second.gbr, weave * (0.28 + high.z * 0.35));
    recombined = retainLuminance(recombined, luminance);
    float independence = AL_LAD_GAIN * (0.08 + extra.z * 0.10 + high.z * 0.24 + high.w * 0.10)
                       * structure * strength * resolved * colour;
    material = mix(material, recombined, clamp(independence, 0.0, 1.0));
    vec3 spectrum = filmSpectrum(seed + weave * 0.30 + clock / TAU * 31.0);
    // Fine spectral highlights are discovered in source edges, not drawn as
    // independent world-space rings. Blank material has no generated contour.
    float shine = AL_LAD_GAIN * structure * strength * resolved * colour
                * (0.035 + extra.x * 0.06 + high.z * 0.12 + high.w * 0.07);
    material += spectrum * shine * smoothstep(0.025, 0.30, luminance);
    return material;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 original = texture(DiffuseSampler, uv);
    float depth = texture(DepthSampler, uv).r;
    float presence = clamp(WarpA.x, 0.0, 1.35);
    if (presence <= 0.0001) { fragColor = original; return; }
    float clock = fract(WarpA.w / 96000.0) * TAU;
    float colourClock = fract(WarpC.w) * TAU;
    float seed = WarpC.y;
    float motion = clamp(Control.y, 0.0, 2.0);
    float colour = clamp(Control.x, 0.0, 1.0);
    float glass = clamp(WarpA.y, 0.0, 1.0);
    float prism = clamp(WarpA.z, 0.0, 1.0);
    float flow = clamp(WarpB.x, 0.0, 1.0);
    float chroma = clamp(WarpB.y, 0.0, 1.0);
    float recursion = clamp(WarpB.z, 0.0, 1.0);
    float jewels = clamp(WarpB.w, 0.0, 1.0);
    vec4 extra = clamp(ExtraDose, 0.0, 1.35);
    vec4 high = clamp(HighDose, 0.0, 1.35);
    float music = clamp(WarpC.x, 0.0, 1.0) * clamp(MusicA.x * 0.45 + MusicA.y * 0.30 + MusicA.z * 0.25, 0.0, 1.0);
    // Surface and source derivatives precede divergent material branches.
    vec3 relative = relativeAt(uv, min(depth, 0.99999));
    vec3 dx = dFdx(relative), dy = dFdy(relative);
    float grainEdge = clamp((length(dFdx(original.rgb)) + length(dFdy(original.rgb))) * 3.0, 0.0, 1.0);
    vec3 crossN = cross(dx, dy);
    float normalLength = length(crossN);
    vec3 world = WorldEyePosition.xyz + relative;
    vec3 view = -normalize(relative);
    vec3 ray = normalize(relativeAt(uv, 0.99999));
    float footprint = max(length(dx), length(dy));
    vec3 normal = crossN / max(normalLength, 0.000000000001);
    if (dot(normal, view) < 0.0) normal = -normal;
    if (depth >= 0.999995) {
        // Atmospheric pigment evolution stays in world directions; no drawn sky geometry.
        float cloud = sin(dot(ray, vec3(7.0, 4.0, -6.0)) + sin(clock * 47.0) * motion * 0.70 + seed * TAU)
                    * cos(dot(ray, vec3(-4.0, 8.0, 3.0)) - sin(clock * 37.0) * motion * 0.60);
        vec3 hue = retainLuminance(filmSpectrum(seed + ray.y * 0.35 + colourClock / TAU * 31.0), dot(original.rgb, LUMA));
        float amount = AL_LAD_GAIN * presence * colour * (0.035 + jewels * 0.06 + extra.w * 0.05 + high.w * 0.10) * (0.6 + cloud * 0.4);
        fragColor = vec4(clamp(mix(original.rgb, hue, clamp(amount, 0.0, 0.30 * AL_LAD_GAIN)), 0.0, 3.5), original.a);
        return;
    }
    float distance = length(world - WorldHeadPosition.xyz);
    float nearGuard = smoothstep(0.35, 1.10, distance);
    float surfaceGuard = (1.0 - smoothstep(0.18, 0.65, footprint)) * step(0.000000000001, normalLength);
    float strength = min(presence, 1.0) * nearGuard * surfaceGuard;
    if (strength <= 0.0001) { fragColor = original; return; }
    vec3 axis = abs(normal);
    vec3 tangentU, tangentV;
    if (axis.y >= axis.x && axis.y >= axis.z) {
        tangentU = vec3(1, 0, 0); tangentV = vec3(0, 0, 1);
    } else if (axis.x >= axis.z) {
        tangentU = vec3(0, 0, 1); tangentV = vec3(0, 1, 0);
    } else {
        tangentU = vec3(1, 0, 0); tangentV = vec3(0, 1, 0);
    }
    vec2 face = vec2(dot(world, tangentU), dot(world, tangentV));
    float aa = max(footprint, 0.0001);
    float attention = clamp(Focus.w, 0.0, 1.0) * exp(-length(world - Focus.xyz) * 0.30);
    float flowAmount = 0.03 + flow * 0.22 + prism * 0.05 + recursion * 0.06
                     + extra.z * 0.25 + extra.w * 0.22
                     + high.x * 0.20 + high.w * 0.23;
    vec2 shift = surfaceTransport(face, clock, seed, motion, extra, high) * flowAmount * strength * motion * AL_LAD_GAIN;
    vec2 movement = vec2(dot(MotionA.xyz, tangentU), dot(MotionA.xyz, tangentV));
    shift -= movement * motion * flow * (0.035 + high.w * 0.05) * AL_LAD_GAIN;
    vec2 sourceUv = transportedSource(uv, world, normal, tangentU, tangentV, shift);
    vec3 material = texture(DiffuseSampler, sourceUv).rgb;
    vec2 splitField = surfaceTransport(face + vec2(4.0, -8.0), clock, seed + 0.37, motion, extra, high);
    vec2 separation = splitField * chroma * strength * colour * motion * AL_LAD_GAIN
                    * (0.020 + prism * 0.025 + extra.x * 0.040 + high.x * 0.060 + high.z * 0.030);
    vec2 firstUv = transportedSource(sourceUv, world, normal, tangentU, tangentV, shift + separation);
    vec2 secondUv = transportedSource(sourceUv, world, normal, tangentU, tangentV, shift - separation);
    vec3 first = texture(DiffuseSampler, firstUv).rgb;
    vec3 second = texture(DiffuseSampler, secondUv).rgb;
    material = mix(material, vec3(first.r, material.g, second.b), 0.65 * colour);
    material = sourceEchoes(material, sourceUv, world, normal, tangentU, tangentV, face, aa,
                            clock, seed, strength, extra, high, motion, colour, attention, music);
    material = pigmentFlow(material, face, colourClock, seed, strength * (0.78 + glass * 0.22), extra, high, colour);
    material = patternInterference(material, first, second, face, clock, seed, grainEdge, aa,
                                   strength * (0.80 + recursion * 0.20), extra, high, colour);
    fragColor = vec4(clamp(mix(original.rgb, material, strength), 0.0, 3.5), original.a);
}
