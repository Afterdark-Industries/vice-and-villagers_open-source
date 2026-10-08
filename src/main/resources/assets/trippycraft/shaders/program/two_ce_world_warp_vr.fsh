#version 150

// Living architectural perception: relief and self-similar detail inside materials.
// Native world reconstruction is shared by desktop and asymmetric stereo eyes.
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform mat4 WorldViewProjection;
uniform mat4 WorldInverseViewProjection;
uniform vec4 WorldEyePosition;
uniform vec4 WorldHeadPosition;
uniform vec4 WarpA; // presence, cavities, nested rooms, time in ticks
uniform vec4 WarpB; // tessellation, edge definition, perspective fold, recursion
uniform vec4 WarpC; // corridor depth, seed, observation, symmetry
uniform vec4 Intensity; // calibrated low/middle/high gates, continuing machine lift
uniform vec4 HighDose; // phase-aware surface/architecture/perspective/immersion drives
uniform vec4 ExtraDose; // recursive vaults, impossible perspective, morphogenesis, immersive lattice
uniform vec4 MusicA;
uniform vec4 MotionA; // world movement per tick, motion comfort scale
uniform vec4 MotionB; // view direction movement per tick, color comfort scale
uniform vec4 Focus; // shared observed world point, dwell
uniform vec4 MixA;
uniform vec4 Control; // color scale, motion scale, built environment, trails

out vec4 fragColor;
const float TAU = 6.28318530718;
// Artistic amplitude, independent of dose/phase and server-side body load.
const float VISUAL_GAIN = 2.0;
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float nonzero(float value) {
    return abs(value) < 0.000001 ? (value < 0.0 ? -0.000001 : 0.000001) : value;
}

vec3 relativeWorldAt(vec2 uv, float depth) {
    vec4 clip = WorldInverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return clip.xyz / nonzero(clip.w);
}

vec3 worldAt(vec2 uv, float depth) {
    return WorldEyePosition.xyz + relativeWorldAt(uv, depth);
}

float line(float distance, float width, float footprint) {
    return 1.0 - smoothstep(width, width + max(0.001, footprint), distance);
}

float hash31(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec2 rotate2(vec2 p, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

float structureWave(vec3 p, float seconds, float phase) {
    // Periods divide the renderer's floating origin, so crossing it is seamless.
    p = mod(p, vec3(4096.0));
    return sin(TAU * (dot(p, vec3(1, 0, 1)) / 32.0 - seconds * 178.0 / 4800.0) + phase) * 0.48
         + sin(TAU * (dot(p, vec3(-1, 1, 1)) / 16.0 + seconds * 123.0 / 4800.0) + phase * 0.7) * 0.32
         + sin(TAU * (dot(p, vec3(1, 2, -1)) / 64.0 - seconds * 84.0 / 4800.0) + phase * 1.3) * 0.20;
}

vec3 spectrum(float phase) {
    vec3 hue = 0.54 + 0.46 * cos(TAU * (phase + vec3(0.02, 0.35, 0.67)));
    return mix(hue, vec3(0.84, 0.94, 1.0), 0.14);
}

vec2 safeSource(vec2 fallback, vec3 candidateWorld, vec3 surface, vec3 normal) {
    vec4 clip = WorldViewProjection * vec4(candidateWorld - WorldEyePosition.xyz, 1.0);
    vec2 candidate = clip.xy / nonzero(clip.w) * 0.5 + 0.5;
    vec2 margin = 1.5 / max(OutSize, vec2(1.0));
    float inside = step(margin.x, candidate.x) * step(candidate.x, 1.0 - margin.x)
                 * step(margin.y, candidate.y) * step(candidate.y, 1.0 - margin.y)
                 * step(0.00001, clip.w);
    candidate = clamp(candidate, margin, vec2(1.0) - margin);
    float raw = texture(DepthSampler, candidate).r;
    vec3 sampledWorld = worldAt(candidate, raw);
    float distance = length(surface - WorldEyePosition.xyz);
    // A wall must never drag a nearer tree, hand, other wall or sky into itself.
    float sameFace = 1.0 - smoothstep(0.035 + distance * 0.0015, 0.16 + distance * 0.0035,
                                     abs(dot(sampledWorld - surface, normal)));
    float local = 1.0 - smoothstep(0.035, 0.10, length(candidate - fallback));
    return mix(fallback, candidate, inside * sameFace * local * (1.0 - step(0.999995, raw)));
}

vec3 expandedMaterial(vec3 material, vec2 p, float footprint, float span,
                      vec2 viewSlope, vec4 exposure, float strength, float motion,
                      float colorScale, float seconds, float seed) {
    // Higher-dose structures live on the actual ray-hit wall, inside the
    // approved outer chamber. Their own intersections give them side walls.
    if (dot(exposure, vec4(1.0)) <= 0.0001) return material;
    float resolved = 1.0 - smoothstep(0.10, 0.36, footprint);
    float support = 0.30 + clamp(dot(material, LUMA), 0.0, 1.0) * 0.70;
    float clock = seconds * TAU * 57.0 / 4800.0;
    float u = p.x * TAU / 2.0 + clock + seed * TAU;
    float v = p.y * TAU / 4.0 - seconds * TAU * 83.0 / 4800.0;
    float waveU = sin(u), waveV = sin(v);
    float bevelU = sqrt(waveU * waveU + 0.025), bevelV = sqrt(waveV * waveV + 0.025);
    vec2 slopes = vec2(waveU * cos(u) / bevelU * bevelV,
                       waveV * cos(v) / bevelV * bevelU * 0.5);
    float sculpture = (exposure.x * 0.65 + exposure.z * 1.5) * strength * resolved;
    vec3 sculptedNormal = normalize(vec3(-slopes * sculpture * 2.0, 1.0));
    vec3 light = normalize(vec3(-0.52, 0.58, 1.0));
    float illumination = dot(sculptedNormal, light) - light.z;
    material *= 1.0 + clamp(illumination * sculpture * 1.65, -0.42, 0.55);
    vec3 hue = spectrum(seed + p.x / 16.0 - p.y / 32.0 + seconds * 33.0 / 4800.0);
    float glint = pow(max(0.0, dot(sculptedNormal, normalize(vec3(0.32, 0.48, 1.0)))), 22.0);
    material += hue * glint * sculpture * support * colorScale * 0.16;

    // Squares become octagons, then grow articulated branch joints. Each
    // scale has its own conservative analytic footprint, bevel and shadow.
    float scale = 0.5;
    for (int tier = 0; tier < 3; tier++) {
        float level = float(tier);
        float drive = tier == 0 ? exposure.x : tier == 1 ? exposure.z : exposure.w;
        vec2 sculpted = p + slopes * exposure.z * motion * 0.17;
        vec2 cell = fract(sculpted * scale) - 0.5, ap = abs(cell);
        float square = max(ap.x, ap.y);
        float octagon = max(square, (ap.x + ap.y) * 0.70710678);
        float morphology = exposure.z * (0.70 + sin(clock + seed * TAU) * motion * 0.25);
        float form = mix(square, octagon, clamp(morphology, 0.0, 1.0));
        float aa = footprint * scale * (1.0 + sculpture * 0.25);
        float rim = line(abs(form - 0.34), 0.009, aa);
        float inside = line(abs(ap.x + ap.y - 0.27), 0.006, aa);
        float joints = line(abs(ap.x - ap.y), 0.007, aa) * smoothstep(0.16, 0.33, form);
        float recess = line(abs(form - 0.365), 0.010, aa);
        float weight = drive * strength * resolved * (1.0 - smoothstep(0.08, 0.28, aa));
        material *= 1.0 - recess * weight * 0.28;
        material += spectrum(seed + level * 0.22 + seconds * 33.0 / 4800.0)
                  * (rim * 0.70 + inside * 0.35 + joints * exposure.w * 0.60)
                  * weight * support * colorScale * 0.62;
        scale *= 2.0;
    }

    // A smaller chamber opens inside the hit wall. Its slope is transformed
    // for side walls too, keeping the virtual depth coherent between eyes.
    float cellSpan = clamp(span * 0.25, 1.0, 4.0);
    vec2 q = (fract(p / cellSpan) - 0.5) * cellSpan;
    float opening = cellSpan * (0.22 + exposure.x * 0.13);
    float mouth = 1.0 - smoothstep(opening - max(0.016, footprint * 1.5), opening,
                                  max(abs(q.x), abs(q.y)));
    float reveal = exposure.x * strength * resolved * mouth;
    if (reveal > 0.001) {
        vec2 ray = clamp(viewSlope, vec2(-8.0), vec2(8.0));
        float depth = cellSpan * (0.32 + exposure.x * 1.40 + exposure.y * 2.40 + exposure.w * 1.60);
        vec2 boundary = mix(vec2(-opening), vec2(opening), step(vec2(0.0), ray));
        vec2 sideDepth = (boundary - q) / vec2(nonzero(ray.x), nonzero(ray.y));
        sideDepth = mix(max(sideDepth, vec2(0.0)), vec2(1000000.0),
                        vec2(1.0) - step(vec2(0.00001), abs(ray)));
        float hitDepth = min(depth, min(sideDepth.x, sideDepth.y));
        vec2 hit = q + ray * hitDepth;
        float back = step(depth - 0.001, hitDepth);
        float sideLight = mix(0.46, 0.72, step(sideDepth.y, sideDepth.x));
        vec3 vault = material * mix(sideLight, 0.78, back);
        float wallOcclusion = smoothstep(opening * 0.50, opening, max(abs(hit.x), abs(hit.y)));
        vault *= 1.0 - wallOcclusion * (0.10 + exposure.y * 0.18);
        float frames = 0.0;
        for (int arch = 0; arch < 4; arch++) {
            float level = float(arch) + 1.0, recess = depth * level / 5.0;
            // Paired axes articulate an impossible perspective within a
            // recess; its enclosing jamb and physical wall never move.
            float turn = exposure.y * motion * sin(clock + level * 0.7 + seed * TAU) * 0.85;
            vec2 inset = rotate2(q + ray * recess, turn);
            float aperture = opening * (1.0 - level * 0.085);
            float frame = line(abs(max(abs(inset.x), abs(inset.y)) - aperture),
                               0.014 + level * 0.003, footprint * (1.0 + recess / cellSpan));
            frames += frame * step(recess, hitDepth + 0.012) * exp(-level * 0.18);
        }
        float darkBranch = (1.0 - smoothstep(0.14, 0.21, abs(hit.x / cellSpan)))
                         * (1.0 - smoothstep(0.25, 0.38, abs(hit.y / cellSpan))) * back;
        vault *= 1.0 - darkBranch * (0.20 + exposure.y * 0.45);
        vault += hue * frames * colorScale * (0.24 + exposure.y * 0.35) * support;
        material = mix(material, vault, clamp(reveal * 1.35, 0.0, 0.88));
    }
    return material;
}

vec3 chamberMaterial(vec3 material, vec2 wallPoint, float footprint, float depthFraction,
                     float span, float wallSide, vec4 drives, float strength, float motion,
                     float colorScale, float seconds, float seed, float attention,
                     vec4 extra, vec2 wallSlope) {
    // This is the material of the ray-hit wall, including the side walls. It
    // survives the opaque room composite and turns with the room's perspective.
    // All slopes/footprints are analytic: no derivatives in divergent branches.
    if (drives.x + drives.z + drives.w <= 0.0001) return material;
    float resolved = 1.0 - smoothstep(0.10, 0.38, footprint);
    float support = 0.38 + clamp(dot(material, LUMA), 0.0, 1.0) * 0.62;
    float phaseU = wallPoint.x * TAU / 2.0 + seed * TAU + seconds * TAU * 96.0 / 4800.0;
    float phaseV = wallPoint.y * TAU / 4.0 - seed * TAU * 0.7 - seconds * TAU * 72.0 / 4800.0;
    float su = sin(phaseU), sv = sin(phaseV);
    float ru = sqrt(su * su + 0.06), rv = sqrt(sv * sv + 0.06);
    vec2 slopes = vec2(su * cos(phaseU) / ru * rv, sv * cos(phaseV) / rv * ru * 0.5);
    float emboss = drives.x * strength * resolved;
    vec3 facetNormal = normalize(vec3(-slopes * emboss * motion * VISUAL_GAIN * 0.85, 1.0));
    vec3 lightDirection = normalize(vec3(-0.50 + wallSide * 0.18, 0.65, 1.0));
    float facetLight = dot(facetNormal, lightDirection) - lightDirection.z;
    material *= 1.0 + clamp(facetLight * emboss * VISUAL_GAIN, -0.30, 0.42);
    vec3 facetHue = spectrum(seed + wallPoint.x / 16.0 - wallPoint.y / 32.0
                             + seconds * 28.0 / 4800.0 + depthFraction * 0.16);
    float glint = pow(max(0.0, dot(facetNormal, normalize(vec3(0.35, 0.45, 1.0)))), 18.0);
    material += facetHue * glint * emboss * support * colorScale * VISUAL_GAIN * 0.065;

    float scale = 0.5;
    for (int ornament = 0; ornament < 3; ornament++) {
        float level = float(ornament);
        float drive = ornament == 0 ? drives.x : ornament == 1 ? drives.z : drives.w;
        float wallAA = footprint * scale;
        float visible = 1.0 - smoothstep(0.08, 0.30, wallAA);
        if (drive > 0.0001 && visible > 0.0) {
            // Slow, motion-controlled flexion is shared by the bevel and its
            // engraving, so details stay attached to the sculpted inner wall.
            vec2 sculpted = wallPoint + slopes * emboss * motion * VISUAL_GAIN * 0.075;
            vec2 p = fract(sculpted * scale + vec2(seed * 0.31, seed * 0.17)) - 0.5;
            vec2 ap = abs(p);
            float octagon = max(max(ap.x, ap.y), (ap.x + ap.y) * 0.70710678);
            float rim = line(abs(octagon - 0.36), 0.009, wallAA);
            float inset = line(abs(max(ap.x, ap.y) - 0.22), 0.007, wallAA);
            float branch = line(abs(ap.x - ap.y), 0.006, wallAA)
                         * smoothstep(0.17, 0.34, octagon);
            float ribbon = max(rim, max(inset * 0.80, branch * drive));
            float recess = line(abs(octagon - 0.385), 0.008, wallAA);
            float weight = drive * strength * visible * resolved * VISUAL_GAIN
                         * (0.26 + attention * 0.12) * (0.82 + depthFraction * 0.18);
            material *= 1.0 - clamp(recess * weight * 0.25, 0.0, 0.22);
            vec3 inlayHue = spectrum(seed + level * 0.19 + wallPoint.y / 32.0
                                     + seconds * 28.0 / 4800.0);
            material += inlayHue * ribbon * weight * support * colorScale;
        }
        scale *= 2.0;
    }
    // Faceted bands run down the virtual depth axis on side walls and converge
    // into the back-wall carving. They belong to the chamber, not the viewport.
    float depthBands = line(abs(fract((wallPoint.x + wallPoint.y) * 0.5
                                   + depthFraction * 2.0 + seed) - 0.5), 0.009, footprint);
    float deepDetail = (drives.z * 0.10 + drives.w * 0.16) * strength * resolved * VISUAL_GAIN;
    material += facetHue * depthBands * deepDetail * support * colorScale;
    return expandedMaterial(material, wallPoint, footprint, span, wallSlope, extra,
                            strength, motion, colorScale, seconds, seed);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 original = texture(DiffuseSampler, uv);
    float rawDepth = texture(DepthSampler, uv).r;
    float presence = clamp(WarpA.x, 0.0, 1.4);
    if (presence <= 0.0001) {
        fragColor = original;
        return;
    }

    float motion = clamp(Control.y, 0.0, 1.5);
    float colorScale = clamp(Control.x, 0.0, 1.0);
    float seconds = mod(WarpA.w, 96000.0) * 0.05;
    // Java advances this shared clock at the configured motion rate. Multiplying
    // a wrapped clock by a noninteger rate here would introduce a loop seam.
    float travel = seconds;
    float seedPhase = WarpC.y * TAU;
    float gateLow = clamp(Intensity.x, 0.0, 1.0);
    float gateMid = clamp(Intensity.y, 0.0, 1.0);
    float gateHigh = clamp(Intensity.z, 0.0, 1.0);
    float expansion = log(1.0 + max(0.0, Intensity.w));
    float cavities = clamp(WarpA.y, 0.0, 1.0);
    float nested = clamp(WarpA.z, 0.0, 1.0);
    float tess = clamp(WarpB.x, 0.0, 1.0);
    float edgeDefinition = clamp(WarpB.y, 0.0, 1.0);
    float fold = clamp(WarpB.z, 0.0, 1.0);
    float recursion = clamp(WarpB.w, 0.0, 1.0);
    float corridor = clamp(WarpC.x, 0.0, 1.0);
    vec4 highDose = clamp(HighDose, vec4(0.0), vec4(1.0));
    vec4 extraDose = clamp(ExtraDose, vec4(0.0), vec4(1.0));
    // Derivatives must execute before any nonuniform sky/silhouette branch.
    vec3 relativeWorld = relativeWorldAt(uv, rawDepth);
    vec3 world = WorldEyePosition.xyz + relativeWorld;
    // Derive normals before adding a large floating-origin eye coordinate:
    // nearby subpixel differences then retain precision at origin boundaries.
    vec3 dx = dFdx(relativeWorld);
    vec3 dy = dFdy(relativeWorld);
    vec3 skyRayForAA = normalize(relativeWorldAt(uv, 0.99999));
    float skyFootprint = max(length(dFdx(skyRayForAA)), length(dFdy(skyRayForAA)));

    vec2 pixel = 1.0 / max(OutSize, vec2(1.0));
    vec3 leftColor = texture(DiffuseSampler, clamp(uv - vec2(pixel.x, 0), pixel, 1.0 - pixel)).rgb;
    vec3 rightColor = texture(DiffuseSampler, clamp(uv + vec2(pixel.x, 0), pixel, 1.0 - pixel)).rgb;
    vec3 upperColor = texture(DiffuseSampler, clamp(uv + vec2(0, pixel.y), pixel, 1.0 - pixel)).rgb;
    vec3 lowerColor = texture(DiffuseSampler, clamp(uv - vec2(0, pixel.y), pixel, 1.0 - pixel)).rgb;
    float materialLuma = dot(original.rgb, LUMA);
    float textureRidge = length(rightColor - leftColor) + length(upperColor - lowerColor);
    float ridge = smoothstep(0.055, 0.34, textureRidge);

    if (rawDepth >= 0.999995) {
        // Slow atmosphere follows world direction, preserving daylight/clouds.
        // No screen-centred tunnel or radial stencil over the sky.
        vec3 ray = normalize(worldAt(uv, 0.99999) - WorldEyePosition.xyz);
        float cloud = sin(dot(ray, vec3(9, 5, -7)) + seedPhase + travel * TAU * 20.0 / 4800.0)
                    + sin(dot(ray, vec3(-4, 11, 6)) - travel * TAU * 15.0 / 4800.0) * 0.45;
        vec3 hue = spectrum(cloud * 0.12 + WarpC.y + seconds * 30.0 / 4800.0);
        float airy = presence * colorScale * VISUAL_GAIN * (0.012 + gateHigh * 0.026 + expansion * 0.010);
        vec3 color = original.rgb * (1.0 + hue * airy * (0.35 + ridge * 0.65));
        color += hue * ridge * airy * 0.22;
        if (highDose.z + highDose.w > 0.0001) {
            // At extreme game exposure, slow geometric aurora grows through
            // the sky's world directions. Clouds remain visible underneath.
            float skyField = ray.y * 4.0 + sin(ray.x * 7.0 + seedPhase) * 0.42
                           + sin(ray.z * 9.0 - travel * TAU * 24.0 / 4800.0) * 0.28;
            float veil = pow(0.5 + 0.5 * sin(skyField * TAU + travel * TAU * 36.0 / 4800.0), 5.0);
            float seam = pow(0.5 + 0.5 * sin((abs(ray.x) + abs(ray.z)) * 18.0
                         - travel * TAU * 48.0 / 4800.0 + seedPhase), 14.0);
            vec3 vaultHue = spectrum(skyField * 0.13 + WarpC.y + seconds * 22.0 / 4800.0);
            float vault = presence * colorScale * VISUAL_GAIN * (highDose.z * 0.16 + highDose.w * 0.36);
            color += vaultHue * (veil * 0.35 + seam * highDose.w * 0.25) * vault;
            color *= 1.0 + (vaultHue - 0.5) * veil * vault * 0.30;
        }
        if (extraDose.w > 0.0001) {
            // An angular vault resolves on world directions at the deepest
            // exposure. The layered ribs have broad spacing and no pixel grain.
            float vaultForm = abs(ray.x) + abs(ray.z) + ray.y * 0.75;
            float ribs = line(abs(fract(vaultForm * 3.0 + seconds * 18.0 / 4800.0) - 0.5),
                              0.010, skyFootprint * 4.0);
            float crossRibs = line(abs(fract((ray.x - ray.z) * 2.0) - 0.5),
                                   0.009, skyFootprint * 4.0);
            vec3 skyHue = spectrum(vaultForm * 0.15 + WarpC.y + seconds * 22.0 / 4800.0);
            color += skyHue * (ribs + crossRibs * 0.45) * extraDose.w * presence * colorScale * 0.32;
        }
        fragColor = vec4(clamp(color, 0.0, 3.5), original.a);
        return;
    }

    vec3 crossNormal = cross(dx, dy);
    float normalLength = length(crossNormal);
    vec3 normal = crossNormal / max(normalLength, 0.000000000001);
    vec3 view = normalize(-relativeWorld);
    if (dot(normal, view) < 0.0) normal = -normal;
    float distance = length(relativeWorld + (WorldEyePosition.xyz - WorldHeadPosition.xyz));
    float continuous = 1.0 - smoothstep(0.08 + distance * 0.018, 0.20 + distance * 0.06,
                                       max(length(dx), length(dy)));
    presence *= smoothstep(0.35, 1.10, distance) * continuous * step(0.000000000001, normalLength);
    if (presence <= 0.0001) {
        fragColor = original;
        return;
    }

    vec3 axis = abs(normal);
    vec3 tangentU;
    vec3 tangentV;
    if (axis.y >= axis.x && axis.y >= axis.z) {
        tangentU = vec3(1, 0, 0); tangentV = vec3(0, 0, 1);
    } else if (axis.x >= axis.z) {
        tangentU = vec3(0, 0, 1); tangentV = vec3(0, 1, 0);
    } else {
        tangentU = vec3(1, 0, 0); tangentV = vec3(0, 1, 0);
    }
    vec2 face = mod(vec2(dot(world, tangentU), dot(world, tangentV)), vec2(4096.0));
    vec2 footprint = max(vec2(length(vec2(dot(dx, tangentU), dot(dy, tangentU))),
                              length(vec2(dot(dx, tangentV), dot(dy, tangentV)))), vec2(0.0001));
    float aa = max(footprint.x, footprint.y);
    float resolvable = 1.0 - smoothstep(0.08, 0.36, aa);
    vec2 cell = fract(face) - 0.5;
    float blockEdge = max(abs(cell.x), abs(cell.y));
    float interior = 1.0 - smoothstep(0.24, 0.49, blockEdge);
    float wave = structureWave(world, travel, seedPhase);
    float breath = sin(TAU * (travel * 253.0 / 4800.0 + dot(world, vec3(1, 0, -1)) / 32.0)
                       + seedPhase + wave * 0.65);
    float attention = clamp(Focus.w, 0.0, 1.0) * exp(-length(world - Focus.xyz) * 0.42);
    float facing = max(0.15, dot(view, normal));
    vec2 parallax = vec2(dot(view, tangentU), dot(view, tangentV)) / max(0.38, facing);
    float strength = 1.0 - exp(-presence * 1.15);

    // Neighbouring blocks breathe together. A local lens follows texture detail.
    float relief = (wave * 0.040 + breath * interior * (0.033 + fold * 0.080))
                 * presence * motion * VISUAL_GAIN * (1.0 + gateHigh * 0.42 + expansion * 0.14);
    vec3 displaced = world + normal * relief;
    displaced += tangentU * sin(face.y * TAU / 16.0 + travel * TAU * 85.0 / 4800.0 + seedPhase)
               * (0.010 + fold * 0.020) * strength * motion * VISUAL_GAIN;
    displaced += tangentV * wave * (0.008 + fold * 0.016) * strength * motion * VISUAL_GAIN;
    vec2 sourceUv = safeSource(uv, displaced, world, normal);
    vec3 color = texture(DiffuseSampler, sourceUv).rgb;
    color *= 1.0 + relief * 0.34;

    if (highDose.x > 0.0001) {
        // The first added stage turns gentle breathing into angular embossed
        // folds. Analytic slopes give the folds lighting without derivatives
        // inside a divergent high-dose branch.
        float phaseU = face.x * TAU / 4.0 + seedPhase + travel * TAU * 96.0 / 4800.0;
        float phaseV = face.y * TAU / 8.0 - seedPhase * 0.7 - travel * TAU * 72.0 / 4800.0;
        float ridgeU = sqrt(sin(phaseU) * sin(phaseU) + 0.04);
        float ridgeV = sqrt(sin(phaseV) * sin(phaseV) + 0.04);
        float angularRelief = (ridgeU * ridgeV - 0.42) * 0.52 + wave * 0.16;
        float drive = highDose.x * strength * resolvable;
        vec2 slopes = vec2(sin(phaseU) * cos(phaseU) / ridgeU * ridgeV * TAU / 4.0,
                           sin(phaseV) * cos(phaseV) / ridgeV * ridgeU * TAU / 8.0) * 0.52;
        vec3 foldedWorld = displaced + normal * angularRelief * drive * motion * VISUAL_GAIN;
        foldedWorld += (tangentU * slopes.x + tangentV * slopes.y) * 0.085 * drive * motion * VISUAL_GAIN;
        vec2 foldUv = safeSource(sourceUv, foldedWorld, world, normal);
        vec3 foldColor = texture(DiffuseSampler, foldUv).rgb;
        float bevelLight = dot(slopes, vec2(-0.65, 0.45)) * 0.42 * drive * motion * VISUAL_GAIN;
        foldColor *= 1.0 + clamp(bevelLight, -0.36, 0.36);
        color = mix(color, foldColor, clamp(drive * 0.88, 0.0, 0.85));
        displaced = mix(displaced, foldedWorld, drive);
        sourceUv = mix(sourceUv, foldUv, drive);
    }

    if (extraDose.y + extraDose.z > 0.0001) {
        // Higher exposure articulates a broad surface into incompatible local
        // perspectives. Texture samples remain on that same face, with physical
        // silhouettes and the approved chamber mouths held in place.
        vec2 broad = (fract(face * 0.25) - 0.5) * 4.0;
        float shape = max(abs(broad.x), abs(broad.y));
        float inside = 1.0 - smoothstep(0.65, 1.8, shape);
        float angle = sin(travel * TAU * 67.0 / 4800.0 + seedPhase + wave)
                    * extraDose.y * 0.65 * motion;
        vec2 localFold = (rotate2(broad, angle) - broad) * inside;
        localFold += vec2(breath, wave) * extraDose.z * inside * 0.24 * motion;
        vec3 candidate = displaced + (tangentU * localFold.x + tangentV * localFold.y)
                                   * strength * resolvable;
        vec2 candidateUv = safeSource(sourceUv, candidate, world, normal);
        vec3 foldedMaterial = texture(DiffuseSampler, candidateUv).rgb;
        float foldMix = (extraDose.y * 0.70 + extraDose.z * 0.18) * inside * strength * resolvable;
        color = mix(color, foldedMaterial, clamp(foldMix, 0.0, 0.72));
        displaced = mix(displaced, candidate, clamp(foldMix, 0.0, 0.72));
        sourceUv = mix(sourceUv, candidateUv, clamp(foldMix, 0.0, 0.72));
    }
    color = expandedMaterial(color, face, aa, 4.0, -parallax, extraDose,
                             strength, motion, colorScale, seconds, WarpC.y);

    // Contours emerge from material, with continuous fields across boundaries.
    float grain = materialLuma * 0.9 + dot(original.rgb - vec3(materialLuma), vec3(0.6, -0.2, 0.4));
    vec2 flowing = face + vec2(wave, breath) * (0.045 + fold * 0.06) * motion * VISUAL_GAIN;
    float field = flowing.x + sin(flowing.y * TAU / 4.0 + wave * 1.8) * 0.42
                + sin((flowing.x - flowing.y) * TAU / 8.0 + seedPhase) * 0.28;
    float contourCoordinate = field * 1.5 + grain * (0.65 + tess * 0.6);
    float contourLow = line(abs(fract(contourCoordinate) - 0.5), 0.014, aa * 2.5 + textureRidge * 0.20);
    float contourHigh = line(abs(fract(field * 2.0 + grain * 1.2) - 0.5), 0.012, aa * 3.0 + textureRidge * 0.20);
    float contour = mix(contourLow, max(contourLow * 0.65, contourHigh), gateMid);
    float materialMask = (0.16 + ridge * 0.58 + interior * 0.16) * resolvable;
    float detailGain = tess * strength * colorScale * VISUAL_GAIN * (0.18 + gateLow * 0.22 + attention * 0.15);
    vec3 jewel = spectrum(WarpC.y + face.x / 32.0 - face.y / 64.0 + wave * 0.16 + seconds * 37.0 / 4800.0);
    color += jewel * contour * materialMask * detailGain * (0.3 + materialLuma * 0.7);

    // Selected two-block regions open into rooms with actual side/back walls.
    // Ray/box intersections produce depth and per-eye parallax instead of a stamp.
    vec2 roomCell = floor(face * 0.5);
    vec2 roomP = (fract(face * 0.5) - 0.5) * 2.0;
    vec3 faceId = floor(world - normal * 0.02);
    vec3 planeAxis = abs(cross(tangentU, tangentV));
    float facePlane = dot(faceId, planeAxis);
    float roomHash = hash31(mod(vec3(roomCell, facePlane), vec3(2048, 2048, 4096)) + vec3(17, 31, 47) * WarpC.y);
    float selection = smoothstep(0.35, 0.75, roomHash);
    float architecture = 0.65 + clamp(Control.z, 0.0, 1.0) * 0.35;
    float reveal = cavities * min(presence, 1.25) * selection * architecture
                 * (0.46 + gateMid * 0.44 + gateHigh * 0.15 + expansion * 0.28 + attention * 0.42);
    float opening = (0.40 + gateMid * 0.12 + roomHash * 0.08 + min(expansion, 2.0) * 0.025)
                  * (1.0 + breath * motion * 0.025);
    float square = max(abs(roomP.x), abs(roomP.y));
    float mouth = (1.0 - smoothstep(opening - max(0.024, aa * 1.5), opening, square)) * resolvable;
    vec3 roomHue = spectrum(WarpC.y + roomHash * 0.65 + wave * 0.10 + seconds * 28.0 / 4800.0);

    if (reveal * mouth > 0.002) {
        float depth = (0.24 + nested * 0.70 + corridor * gateMid * 1.20 + expansion * 0.80)
                    * (0.75 + roomHash * 0.40 + attention * 0.32);
        float entranceDepth = depth;
        // The promoted 500mg chamber is unchanged. Later stages excavate a
        // much deeper volume behind that same aperture, retaining every wall.
        depth *= 1.0 + extraDose.y * 1.70 + extraDose.w * 2.50;
        vec2 direction = -vec2(dot(view, tangentU), dot(view, tangentV));
        vec2 exitSide = mix(vec2(-opening), vec2(opening), step(vec2(0.0), direction));
        vec2 sideTravel = (exitSide - roomP) / vec2(nonzero(direction.x), nonzero(direction.y));
        sideTravel = mix(max(sideTravel, vec2(0.0)), vec2(1000000.0),
                         vec2(1.0) - step(vec2(0.00001), abs(direction)));
        float backTravel = depth / facing;
        float hitTravel = min(backTravel, min(sideTravel.x, sideTravel.y));
        vec2 hit = roomP + direction * hitTravel;
        float hitDepth = hitTravel * facing;
        float backWall = step(backTravel - 0.001, hitTravel);
        vec3 roomCenter = world - tangentU * roomP.x - tangentV * roomP.y;
        // Map the existing material onto virtual walls; projecting the ray hit
        // itself would return the original pixel and erase the depth illusion.
        vec2 wallTexture = hit * 0.72;
        if (backWall < 0.5) {
            float depthTexture = (hitDepth / max(depth, 0.001) - 0.5) * opening * 1.6;
            if (sideTravel.x < sideTravel.y) wallTexture.x = depthTexture;
            else wallTexture.y = depthTexture;
        }
        vec3 materialPoint = roomCenter + tangentU * wallTexture.x + tangentV * wallTexture.y;
        vec2 roomUv = safeSource(sourceUv, materialPoint, world, normal);
        vec3 roomColor = texture(DiffuseSampler, roomUv).rgb;
        float occlusion = smoothstep(opening * 0.40, opening, max(abs(hit.x), abs(hit.y)));
        float sideLighting = mix(0.43, 0.63, step(sideTravel.y, sideTravel.x));
        roomColor *= mix(sideLighting, 0.84, backWall) - occlusion * 0.12;
        roomColor += roomHue * (0.018 + materialLuma * 0.025) * colorScale;

        float frames = 0.0;
        float budget = min(5.0, 1.0 + nested * 1.5 + recursion * 1.5 + gateHigh + attention);
        budget += extraDose.y * 2.0 + extraDose.w * 3.0;
        for (int layer = 0; layer < 8; layer++) {
            float level = float(layer);
            float enabled = clamp(budget - level, 0.0, 1.0);
            if (enabled <= 0.0) break;
            // Keep the five approved shallow thresholds, then append deeper
            // arches. Oblique views retain the original entrance framing.
            float recess = layer < 5 ? entranceDepth * (level + 1.0) / 6.0
                                    : entranceDepth + (depth - entranceDepth) * (level - 4.0) / 4.0;
            vec2 inset = roomP - parallax * recess;
            float aperture = opening * (1.0 - (level + 1.0) * 0.065);
            float frame = line(abs(max(abs(inset.x), abs(inset.y)) - aperture),
                               0.006 + level * 0.001, aa * (1.0 + recess));
            float visible = step(recess, hitDepth + 0.02);
            frames += frame * enabled * visible * exp(-level * 0.24);
        }
        roomColor += roomHue * min(frames, 1.6) * colorScale * VISUAL_GAIN * (0.12 + edgeDefinition * 0.23);
        float backEtching = line(abs(fract((hit.x + hit.y) * 3.0 + roomHash) - 0.5), 0.008, aa * 4.0);
        roomColor += roomHue * backEtching * backWall * recursion * colorScale * VISUAL_GAIN * 0.06;
        vec2 wallPoint = hit;
        float wallSide = 0.0;
        if (backWall < 0.5) {
            wallSide = sideTravel.x < sideTravel.y ? 1.0 : 2.0;
            wallPoint = wallSide < 1.5 ? vec2(hitDepth, hit.y) : vec2(hit.x, hitDepth);
        }
        float wallFootprint = aa * (1.0 + hitDepth / max(2.0 * facing, 0.38));
        vec2 wallSlope = direction / facing;
        if (wallSide > 0.5) {
            wallSlope = wallSide < 1.5 ? vec2(facing, direction.y) / max(abs(direction.x), 0.15)
                                      : vec2(direction.x, facing) / max(abs(direction.y), 0.15);
        }
        roomColor = chamberMaterial(roomColor, wallPoint, wallFootprint, hitDepth / max(depth, 0.001),
                                    2.0, wallSide, highDose, strength, motion, colorScale,
                                    seconds, roomHash + WarpC.y, attention, extraDose, wallSlope);
        color = mix(color, roomColor, clamp(reveal * mouth, 0.0, 0.90));
    }

    float lip = line(abs(square - opening), 0.009, aa) * resolvable * reveal;
    color += roomHue * lip * (0.16 + edgeDefinition * 0.24) * colorScale * VISUAL_GAIN;

    // Later stages reveal a new spatial hierarchy: four-, eight- and
    // sixteen-block galleries behind the existing small rooms. Each has real
    // side/back intersections, branching doorways and receding thresholds.
    // Fixed three-level work budget; low doses take no part in this layer.
    if (highDose.y + highDose.z + highDose.w > 0.0001) {
        float span = 4.0;
        for (int hierarchy = 0; hierarchy < 3; hierarchy++) {
            float level = float(hierarchy);
            float drive = hierarchy == 0 ? highDose.y : hierarchy == 1 ? highDose.z : highDose.w;
            if (drive > 0.0001) {
                // Align the hierarchy around a common two-metre elevation so
                // large rooms can emerge on ordinary-height walls as well as
                // cliffs. Power-of-two offsets keep floating-origin parity.
                vec2 galleryCoordinates = face + vec2(span * 0.5 - 2.0);
                vec2 galleryId = floor(galleryCoordinates / span);
                float galleryHash = hash31(vec3(galleryId, mod(facePlane, 4096.0))
                                          + vec3(11, 29, 53) * WarpC.y + level * vec3(13, 31, 7));
                float chosen = smoothstep(0.15, 0.55, galleryHash);
                vec2 galleryP = (fract(galleryCoordinates / span) - 0.5) * span;
                float aperture = span * (0.235 + drive * 0.025);
                float borderDistance = max(abs(galleryP.x), abs(galleryP.y));
                float galleryMouth = (1.0 - smoothstep(aperture - max(0.03, aa * 1.5), aperture,
                                                       borderDistance)) * resolvable;
                float revealGallery = drive * strength * chosen * architecture * galleryMouth;
                vec3 galleryHue = spectrum(WarpC.y + galleryHash * 0.50 + level * 0.21
                                            + wave * 0.09 + seconds * 26.0 / 4800.0);
                float galleryLip = line(abs(borderDistance - aperture), 0.013, aa) * resolvable;
                color += galleryHue * galleryLip * drive * chosen * strength * architecture * colorScale * VISUAL_GAIN * 0.25;
                if (revealGallery > 0.002) {
                    float depth = span * (0.20 + drive * 0.55 + highDose.w * 0.28)
                                * (0.8 + galleryHash * 0.4 + attention * 0.20);
                    float entranceDepth = depth;
                    depth *= 1.0 + extraDose.y * 1.50 + extraDose.w * 2.50;
                    vec2 direction = -vec2(dot(view, tangentU), dot(view, tangentV));
                    vec2 exitSide = mix(vec2(-aperture), vec2(aperture), step(vec2(0.0), direction));
                    vec2 sideTravel = (exitSide - galleryP) / vec2(nonzero(direction.x), nonzero(direction.y));
                    sideTravel = mix(max(sideTravel, vec2(0.0)), vec2(1000000.0),
                                     vec2(1.0) - step(vec2(0.00001), abs(direction)));
                    float backTravel = depth / facing;
                    float hitTravel = min(backTravel, min(sideTravel.x, sideTravel.y));
                    vec2 hit = galleryP + direction * hitTravel;
                    float hitDepth = hitTravel * facing;
                    float backWall = step(backTravel - 0.001, hitTravel);
                    vec3 galleryCenter = world - tangentU * galleryP.x - tangentV * galleryP.y;
                    vec2 material = hit * 0.78;
                    if (backWall < 0.5) {
                        float depthTexture = (hitDepth / max(depth, 0.001) - 0.5) * aperture * 1.5;
                        if (sideTravel.x < sideTravel.y) material.x = depthTexture;
                        else material.y = depthTexture;
                    }
                    vec3 materialPoint = galleryCenter + tangentU * material.x + tangentV * material.y;
                    vec3 galleryColor = texture(DiffuseSampler, safeSource(sourceUv, materialPoint, world, normal)).rgb;
                    float wallLight = mix(0.48, 0.70, step(sideTravel.y, sideTravel.x));
                    float cornerShade = smoothstep(aperture * 0.45, aperture, max(abs(hit.x), abs(hit.y)));
                    galleryColor *= mix(wallLight, 0.87, backWall) - cornerShade * 0.12;
                    float thresholds = 0.0;
                    for (int doorway = 0; doorway < 6; doorway++) {
                        float stepLevel = float(doorway) + 1.0;
                        float enabled = doorway < 4 ? 1.0
                                : clamp((extraDose.y + extraDose.w) * 2.0 - float(doorway - 4), 0.0, 1.0);
                        if (enabled <= 0.0) break;
                        float recess = doorway < 4 ? entranceDepth * stepLevel / 5.0
                                : entranceDepth + (depth - entranceDepth) * (stepLevel - 4.0) / 3.0;
                        vec2 inset = galleryP - parallax * recess;
                        float frameSize = aperture * (1.0 - stepLevel * (0.10 + highDose.z * 0.02));
                        float threshold = line(abs(max(abs(inset.x), abs(inset.y)) - frameSize),
                                               0.010 + stepLevel * 0.002, aa * (1.0 + recess / span));
                        thresholds += threshold * enabled * step(recess, hitDepth + 0.025) * exp(-stepLevel * 0.18);
                    }
                    // Several dark paths branch from the back wall, with small
                    // illuminated jambs rather than a copied viewport tunnel.
                    vec2 backCoordinate = hit / max(aperture, 0.001);
                    vec2 door = vec2(fract((backCoordinate.x + 1.0) * 1.5) - 0.5,
                                     backCoordinate.y + 0.08);
                    float doorBox = max(abs(door.x) / 0.24, abs(door.y) / 0.65);
                    float passage = (1.0 - smoothstep(0.78, 0.98, doorBox)) * backWall;
                    float jamb = line(abs(doorBox - 0.98), 0.035, aa * 4.0 / span) * backWall;
                    galleryColor *= 1.0 - passage * (0.25 + highDose.z * 0.25);
                    galleryColor += galleryHue * colorScale * VISUAL_GAIN * (thresholds * 0.22 + jamb * 0.12)
                                  * (0.55 + materialLuma * 0.45);
                    galleryColor += galleryHue * passage * colorScale * VISUAL_GAIN * (0.008 + highDose.w * 0.015);
                    vec2 wallPoint = hit;
                    float wallSide = 0.0;
                    if (backWall < 0.5) {
                        wallSide = sideTravel.x < sideTravel.y ? 1.0 : 2.0;
                        wallPoint = wallSide < 1.5 ? vec2(hitDepth, hit.y) : vec2(hit.x, hitDepth);
                    }
                    float wallFootprint = aa * (1.0 + hitDepth / max(span * facing, 0.38));
                    vec2 wallSlope = direction / facing;
                    if (wallSide > 0.5) {
                        wallSlope = wallSide < 1.5 ? vec2(facing, direction.y) / max(abs(direction.x), 0.15)
                                                  : vec2(direction.x, facing) / max(abs(direction.y), 0.15);
                    }
                    galleryColor = chamberMaterial(galleryColor, wallPoint, wallFootprint,
                                                   hitDepth / max(depth, 0.001), span, wallSide,
                                                   highDose, strength, motion, colorScale,
                                                   seconds, galleryHash + WarpC.y, attention, extraDose, wallSlope);
                    color = mix(color, galleryColor,
                                clamp(revealGallery * (1.05 + drive * 0.50 + attention * 0.22), 0.0, 0.88));
                }
            }
            span *= 2.0;
        }
    }

    // Quiet recursive microstructure grows inside texture ridges/fixated areas.
    // A fixed six-layer budget and pixel footprints bound cost and aliasing.
    vec3 etched = vec3(0.0);
    float scale = 1.0;
    float layerBudget = 1.0 + gateMid * 1.5 + gateHigh * 1.5 + attention * 1.5
                     + (1.0 - exp(-expansion)) * 1.5;
    for (int layer = 0; layer < 6; layer++) {
        float level = float(layer);
        float enabled = clamp(layerBudget - level, 0.0, 1.0);
        float footprintAtLevel = aa * scale;
        float visible = 1.0 - smoothstep(0.075, 0.26, footprintAtLevel);
        if (enabled <= 0.0 || visible <= 0.0) break;
        vec2 p = rotate2(fract(flowing * scale + vec2(grain * 0.20, wave * 0.09)) - 0.5,
                         seedPhase * 0.12 + level * 0.7853981634);
        float box = max(abs(p.x), abs(p.y));
        float diamond = abs(p.x) + abs(p.y);
        float frame = line(abs(box - 0.36), 0.005, footprintAtLevel);
        float branch = line(abs(diamond - 0.42), 0.004, footprintAtLevel) * 0.55;
        float pattern = max(frame, branch);
        float weight = enabled * visible * exp(-level * 0.43) * recursion * strength * VISUAL_GAIN
                     * materialMask * (0.12 + attention * 0.12 + gateHigh * 0.09);
        etched += spectrum(WarpC.y + level * 0.17 + wave * 0.12 + seconds * 32.0 / 4800.0) * pattern * weight;
        scale *= 2.0;
    }
    color += etched * colorScale * (0.40 + materialLuma * 0.60);

    if (highDose.x + highDose.z + highDose.w > 0.0001) {
        // Engraved ribbons at a larger scale than the original micro-etching.
        // Interlocked square/octagonal tracery emerges from material ridges;
        // increasing exposure adds scale hierarchy, relief and branch topology.
        vec3 reliefInk = vec3(0.0);
        float inkScale = 0.5;
        for (int ornament = 0; ornament < 3; ornament++) {
            float level = float(ornament);
            float drive = ornament == 0 ? highDose.x : ornament == 1 ? highDose.z : highDose.w;
            float footprintAtLevel = aa * inkScale;
            float visible = 1.0 - smoothstep(0.08, 0.30, footprintAtLevel);
            if (drive > 0.0001 && visible > 0.0) {
                vec2 p = fract(flowing * inkScale + vec2(grain * 0.12, wave * 0.04)) - 0.5;
                vec2 ap = abs(p);
                float octagon = max(max(ap.x, ap.y), (ap.x + ap.y) * 0.70710678);
                float outer = line(abs(octagon - 0.36), 0.007, footprintAtLevel);
                float inner = line(abs(max(ap.x, ap.y) - 0.22), 0.006, footprintAtLevel);
                float branches = line(abs(ap.x - ap.y), 0.005, footprintAtLevel)
                               * smoothstep(0.16, 0.34, octagon);
                float ribbon = max(outer, max(inner * 0.75, branches * drive));
                float grainReveal = (0.24 + ridge * 0.55 + interior * 0.20) * resolvable;
                float weight = drive * strength * visible * grainReveal * VISUAL_GAIN * (0.40 + attention * 0.16);
                vec3 inkHue = spectrum(WarpC.y + level * 0.19 + field / 32.0 + seconds * 28.0 / 4800.0);
                reliefInk += inkHue * ribbon * weight * (0.45 + materialLuma * 0.55);
                // Recessed shadow beside the line gives the inlay volume.
                float shadow = line(abs(octagon - 0.375), 0.007, footprintAtLevel);
                color *= 1.0 - shadow * weight * 0.15;
            }
            inkScale *= 2.0;
        }
        color += reliefInk * colorScale;
    }

    // Iridescence concentrates in lit grain/edges, preserving original shadows.
    float hueFlow = sin(TAU * (face.x / 16.0 + face.y / 32.0) + wave * 2.0 + seedPhase + seconds * TAU * 31.0 / 4800.0);
    float sheen = ridge * (0.10 + gateMid * 0.20 + gateHigh * 0.18)
                + contour * materialMask * (0.05 + gateHigh * 0.16);
    sheen *= strength * edgeDefinition * colorScale * VISUAL_GAIN;
    color = mix(color, color * (0.68 + jewel * 0.90), clamp(sheen, 0.0, 0.60));
    color += jewel * sheen * (0.035 + materialLuma * 0.10);
    float broadIridescence = gateHigh * tess * strength * colorScale * VISUAL_GAIN * (0.10 + expansion * 0.035);
    color *= 1.0 + (spectrum(hueFlow * 0.18 + WarpC.y) - 0.5) * broadIridescence;

    // Restrained spectral echoes track camera movement and settle when still.
    vec3 inertia = MotionA.xyz * 0.8 + MotionB.xyz * min(distance, 18.0) * 0.70;
    float moving = smoothstep(0.002, 0.08, length(inertia));
    float trail = clamp(Control.w, 0.0, 1.5) * strength * moving * motion * VISUAL_GAIN
                * (0.055 + gateHigh * 0.095) * ridge * colorScale;
    if (trail > 0.002) {
        inertia *= min(1.0, 0.20 / max(length(inertia), 0.00001));
        vec3 ghostA = texture(DiffuseSampler, safeSource(sourceUv, displaced - inertia, world, normal)).rgb;
        vec3 ghostB = texture(DiffuseSampler, safeSource(sourceUv, displaced - inertia * 1.8, world, normal)).rgb;
        vec3 spectral = vec3(ghostA.r, color.g, ghostB.b);
        color = mix(color, spectral, clamp(trail, 0.0, 0.40));
        color += abs(ghostA - ghostB) * jewel * trail * 0.20;
    }

    if (highDose.w > 0.0001 && moving > 0.001 && motion > 0.0) {
        // The deepest stage adds a pair of longer, material-local geometric
        // echoes. Stillness restores clarity; depth guards retain silhouettes.
        vec3 echoDirection = inertia / max(length(inertia), 0.00001);
        float echoLength = (0.12 + highDose.w * 0.28) * moving * motion * VISUAL_GAIN;
        vec3 echoA = texture(DiffuseSampler, safeSource(sourceUv,
                     displaced - echoDirection * echoLength, world, normal)).rgb;
        vec3 echoB = texture(DiffuseSampler, safeSource(sourceUv,
                     displaced - echoDirection * echoLength * 1.7, world, normal)).rgb;
        float echoWeight = highDose.w * strength * ridge * colorScale * motion * moving * VISUAL_GAIN * 0.22;
        color = mix(color, vec3(echoA.r, color.g, echoB.b), clamp(echoWeight, 0.0, 0.42));
        color += abs(echoA - echoB) * jewel * echoWeight * 0.25;
    }

    // Symmetry resolves locally at long fixation, never across the viewport.
    float symmetry = clamp(WarpC.w, 0.0, 1.0) * attention * recursion * strength;
    float alignment = 0.5 + 0.5 * sin(travel * TAU * 112.0 / 4800.0 + seedPhase);
    float localSeal = line(abs(abs(cell.x) + abs(cell.y) - 0.36), 0.007, aa * 1.5);
    color += jewel * localSeal * symmetry * alignment * colorScale * resolvable * VISUAL_GAIN * 0.13;
    color += etched.brg * clamp(dot(MixA, vec4(0.20, 0.15, 0.08, 0.12)), 0.0, 0.35) * colorScale;
    fragColor = vec4(clamp(color, 0.0, 3.5), original.a);
}
