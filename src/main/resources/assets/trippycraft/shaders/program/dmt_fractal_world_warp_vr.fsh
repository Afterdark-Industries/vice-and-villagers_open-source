#version 150

// An anchored geometric realm. Minecraft colour is used only for transitions;
// desktop and stereo render the same solid field with actual per-eye rays.
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform mat4 WorldViewProjection;
uniform mat4 WorldInverseViewProjection;
uniform vec4 WorldEyePosition;
uniform vec4 WorldHeadPosition;
uniform vec4 FractalA; // blend, integrated motion seconds, quality 0/1/2, photosensitivity reduction
uniform vec4 FractalB; // signature seeds A/B, dimensional depth, temporal flow
uniform vec4 FractalC; // selected intensity, signature seeds C/D, session style 0..6
uniform vec4 RealmOrigin; // anchored head position in realm units, physical-to-realm scale
uniform vec4 PaletteA;
uniform vec4 PaletteB;
uniform vec4 PaletteC;

out vec4 fragColor;

const float CELL_SIZE = 13.6;
const float FAR_DISTANCE = 66.0;
const int MAX_STEPS = 160;
const float TAU = 6.28318530718;

mat3 fieldRotation;
mat3 foldRotation;
float boxScale;
float minimumRadiusSquared;
float chamberRadius;
float signaturePhase;
float morphClock;
float morphAmount;
float detailEnergy;
float familyIndex;
float styleIndex;
float familyMorph;
int foldIterations;

float nonzero(float v) { return abs(v) < 0.000001 ? (v < 0.0 ? -0.000001 : 0.000001) : v; }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
mat3 rotateX(float a) { float s = sin(a), c = cos(a); return mat3(1, 0, 0, 0, c, s, 0, -s, c); }
mat3 rotateY(float a) { float s = sin(a), c = cos(a); return mat3(c, 0, -s, 0, 1, 0, s, 0, c); }
mat3 rotateZ(float a) { float s = sin(a), c = cos(a); return mat3(c, s, 0, -s, c, 0, 0, 0, 1); }

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 realmRay(vec2 uv) {
    vec2 ndc = uv * 2.0 - 1.0;
    vec4 nearPoint = WorldInverseViewProjection * vec4(ndc, -1.0, 1.0);
    vec4 farPoint = WorldInverseViewProjection * vec4(ndc, 1.0, 1.0);
    return normalize(farPoint.xyz / nonzero(farPoint.w) - nearPoint.xyz / nonzero(nearPoint.w));
}

vec3 realmEyeOrigin() {
    return RealmOrigin.xyz + (WorldEyePosition.xyz - WorldHeadPosition.xyz) * max(RealmOrigin.w, 0.001);
}

vec3 sourceRelativeAt(vec2 uv, float depth) {
    vec4 point = WorldInverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return point.xyz / nonzero(point.w);
}

vec3 morphFlow(vec3 point) {
    return vec3(sin(point.y * 0.71 + point.z * 0.38 + morphClock * 0.57 + signaturePhase),
            sin(point.z * 0.67 - point.x * 0.31 - morphClock * 0.49 + signaturePhase * 0.7),
            sin(point.x * 0.63 + point.y * 0.29 + morphClock * 0.43 + signaturePhase * 1.3));
}

// Inverse texture advection through an actual depth-interpolated 3D point.
// Kept separate so GPU tests can measure source-geometry reprojection directly.
vec2 morphSourceUv(vec2 uv, vec3 sourceRealmPoint, vec3 targetRealmPoint, float blend) {
    float morph = smoothstep(0.0, 1.0, blend);
    vec3 hybrid = mix(sourceRealmPoint, targetRealmPoint, morph);
    vec3 flow = morphFlow(hybrid) * morph * (1.0 - morph) * (1.3 + detailEnergy * 1.4);
    // Limit once in the shared head/world domain. Per-eye UV compression would
    // bend two projections of the same point into different, invalid 3D rays.
    float sharedRadius = length(hybrid - RealmOrigin.xyz);
    flow /= 1.0 + length(flow) / max(sharedRadius * 0.18, 0.004);
    vec3 probe = (hybrid - realmEyeOrigin() - flow) / max(RealmOrigin.w, 0.001);
    vec4 clip = WorldViewProjection * vec4(probe, 1.0);
    vec2 warped = clip.xy / nonzero(clip.w) * 0.5 + 0.5;
    return mix(uv, warped, smoothstep(0.0001, 0.06, clip.w));
}

float sourceFrameConfidence(vec2 uv) {
    float outside = max(max(-uv.x, uv.x - 1.0), max(-uv.y, uv.y - 1.0));
    return 1.0 - smoothstep(0.0, 0.035, outside);
}

vec3 sourceSurfaceNormal(vec2 uv, vec3 relative, vec3 ray) {
    vec2 texel = 1.0 / max(OutSize, vec2(1.0));
    vec2 rightUv = clamp(uv + vec2(texel.x * 2.0, 0.0), vec2(0.001), vec2(0.999));
    vec2 upUv = clamp(uv + vec2(0.0, texel.y * 2.0), vec2(0.001), vec2(0.999));
    vec3 right = sourceRelativeAt(rightUv, texture(DepthSampler, rightUv).r) - relative;
    vec3 up = sourceRelativeAt(upUv, texture(DepthSampler, upUv).r) - relative;
    vec3 gradient = cross(right, up);
    float edgeLimit = max(0.5, length(relative) * 0.15);
    if (length(right) > edgeLimit || length(up) > edgeLimit || dot(gradient, gradient) < 0.0000000001) return -ray;
    vec3 normal = normalize(gradient);
    return dot(normal, -ray) < 0.0 ? -normal : normal;
}

float boxDistance(vec3 p, vec3 size) {
    vec3 d = abs(p) - size;
    return length(max(d, vec3(0.0))) + min(max(d.x, max(d.y, d.z)), 0.0);
}

vec2 mandelbox(vec3 q) {
    vec3 z = q;
    float derivative = 1.0;
    float orbit = 100.0;
    for (int i = 0; i < 10; i++) {
        if (i >= foldIterations) break;
        z = clamp(z, vec3(-1.0), vec3(1.0)) * 2.0 - z;
        float radiusSquared = max(dot(z, z), 0.000001);
        float fold = clamp(1.0 / radiusSquared, 1.0, 1.0 / minimumRadiusSquared);
        z *= fold;
        derivative *= fold;
        z = foldRotation * z;
        z = z * boxScale + q;
        derivative = derivative * abs(boxScale) + 1.0;
        orbit = min(orbit, dot(abs(z), vec3(0.31, 0.43, 0.26)));
    }
    return vec2(length(z) / max(derivative, 0.000001) - 0.0045, clamp(orbit * 0.55, 0.0, 1.0));
}

vec2 mengerTemple(vec3 q) {
    float distance = boxDistance(q, vec3(1.22));
    float scale = 1.0;
    float orbit = 1.0;
    float aperture = 0.92 + sin(morphClock * 0.87 + signaturePhase) * morphAmount * 0.14;
    for (int i = 0; i < 5; i++) {
        if (i >= 3 + (foldIterations - 6) / 2) break;
        vec3 a = mod(q * scale, 2.0) - 1.0;
        scale *= 3.0;
        vec3 r = abs(1.0 - 3.0 * abs(a));
        float cut = (min(max(r.x, r.y), min(max(r.y, r.z), max(r.z, r.x))) - aperture) / scale;
        distance = max(distance, cut);
        orbit = min(orbit, dot(r, vec3(0.24, 0.37, 0.39)));
    }
    return vec2(distance, clamp(orbit, 0.0, 1.0));
}

// Distance and orbit material coordinate. Seven signature styles span crystal
// folds, angular recursive temples and flowing gyroid lace; style 6 morphs
// between families continuously. No noise is attached to screen coordinates.
vec2 realmField(vec3 position) {
    vec3 cell = floor((position + CELL_SIZE * 0.5) / CELL_SIZE);
    vec3 local = position - cell * CELL_SIZE;
    float cellSeed = hash31(cell + vec3(FractalB.x * 23.0, FractalB.y * 31.0, FractalC.y * 47.0));
    vec3 rotated = fieldRotation * local;
    float twist = sin(local.y * 0.42 + morphClock * 0.57 + signaturePhase) * morphAmount * 0.19;
    vec3 q = rotateY(twist) * rotated;
    q += sin(q.yzx * 0.64 + vec3(0.0, 2.1, 4.2) + morphClock * 0.61) * morphAmount * 0.13;
    float worldToFractal = 0.47 + FractalC.y * 0.075 + (cellSeed - 0.5) * 0.045;
    vec2 shape;
    if (familyIndex < 0.5) {
        shape = mandelbox(q * worldToFractal);
        shape.x /= worldToFractal;
    } else if (familyIndex < 1.5) {
        vec3 templeScale = styleIndex > 3.5 ? vec3(0.215, 0.285, 0.25) : vec3(0.245);
        vec3 templeOffset = styleIndex > 3.5 ? vec3(0.19, -0.13, 0.27) : vec3(0.0);
        shape = mengerTemple(q * templeScale + templeOffset);
        shape.x /= max(templeScale.x, max(templeScale.y, templeScale.z));
    } else {
        vec3 g = q * (0.92 + FractalC.z * 0.38);
        g += sin(g.zxy * 0.60 + morphClock * 0.43) * morphAmount * 0.32;
        float gyroid = dot(sin(g), cos(g.yzx));
        float thickness = 0.29 + FractalC.y * 0.21 + sin(morphClock * 0.71 + signaturePhase) * morphAmount * 0.10;
        shape = vec2((abs(gyroid) - thickness) * 0.40, 0.5 + 0.5 * sin(g.x + g.y - g.z));
        if (styleIndex > 4.5) {
            vec2 crystal = mandelbox(q * worldToFractal);
            shape.x = min(shape.x, crystal.x / worldToFractal);
            shape.y = mix(shape.y, crystal.y, 0.45);
        }
    }
    if (styleIndex > 5.5) {
        vec2 temple = mengerTemple(q * 0.245);
        shape = mix(shape, vec2(temple.x / 0.245, temple.y), familyMorph);
    }

    // Finite rooms are sealed between cells. Their carved vaults are faceted,
    // corrugated and embossed, rather than enormous smooth cylindrical holes.
    float outerSolid = 6.10 - length(local);
    float solid = min(shape.x, outerSolid);
    float vault = max(max(abs(local.x) * 0.88, abs(local.y) * 1.04), max(abs(local.z) * 0.91, (abs(local.x) + abs(local.z)) * 0.63));
    float vaultRibs = sin(local.x * 3.4 + sin(local.z * 1.5) + morphClock * 0.72)
            * sin(local.y * 3.1 - local.z * 1.2 - morphClock * 0.51);
    float chamber = vault - chamberRadius + vaultRibs * (0.08 + detailEnergy * 0.13);

    float twistX = 0.34 * sin(position.x * 0.33 + signaturePhase) + sin(morphClock * 0.63 + position.x * 0.19) * morphAmount * 0.28;
    float twistZ = 0.31 * sin(position.z * 0.29 + signaturePhase * 1.3) - sin(morphClock * 0.57 + position.z * 0.23) * morphAmount * 0.30;
    vec2 crossX = mat2(cos(twistX), sin(twistX), -sin(twistX), cos(twistX)) * local.yz;
    vec2 crossZ = mat2(cos(twistZ), sin(twistZ), -sin(twistZ), cos(twistZ)) * local.xy;
    crossX += vec2(sin(position.x * 0.43 + morphClock * 0.47), sin(position.x * 0.31 + signaturePhase)) * 0.18;
    crossZ += vec2(sin(position.z * 0.37 - morphClock * 0.41), sin(position.z * 0.41 + signaturePhase)) * 0.18;
    float widthX = 1.25 + sin(position.x * 0.71 + signaturePhase + morphClock * 0.67) * 0.16;
    float widthZ = 1.23 + sin(position.z * 0.67 + signaturePhase * 0.73 - morphClock * 0.61) * 0.17;
    float tunnelX = max(max(abs(crossX.x), abs(crossX.y) * 0.94), (abs(crossX.x) + abs(crossX.y)) * 0.78) - widthX;
    float tunnelZ = max(max(abs(crossZ.x) * 0.96, abs(crossZ.y)), (abs(crossZ.x) + abs(crossZ.y)) * 0.79) - widthZ;
    // Surface-scale relief remains visible on the CSG cut faces themselves.
    float emboss = sin(position.x * 7.0 + sin(position.z * 3.1) + morphClock * 0.45)
            * sin(position.y * 6.2 - position.z * 2.3 - morphClock * 0.36);
    float emptySpace = min(chamber, min(tunnelX, tunnelZ));
    float distance = max(solid, -emptySpace) + emboss * (0.012 + detailEnergy * 0.024);
    return vec2(distance * 0.64, clamp(shape.y * 0.86 + cellSeed * 0.14, 0.0, 1.0));
}

vec3 realmNormal(vec3 point, float epsilon) {
    vec2 e = vec2(1.0, -1.0) * epsilon;
    vec3 gradient = e.xyy * realmField(point + e.xyy).x + e.yyx * realmField(point + e.yyx).x
            + e.yxy * realmField(point + e.yxy).x + e.xxx * realmField(point + e.xxx).x;
    return dot(gradient, gradient) > 0.0000000001 ? normalize(gradient) : vec3(0.0, 1.0, 0.0);
}

float realmOcclusion(vec3 point, vec3 normal) {
    float occlusion = 0.0, weight = 0.48;
    for (int i = 0; i < 4; i++) {
        float distance = 0.075 + float(i) * 0.17;
        occlusion += max(0.0, distance - realmField(point + normal * distance).x) / distance * weight;
        weight *= 0.53;
    }
    return clamp(1.0 - occlusion * 0.44, 0.36, 1.0);
}

vec3 hueColor(float hue) {
    vec3 rgb = clamp(abs(fract(hue + vec3(0.0, 0.6666667, 0.3333333)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return vec3(0.035) + rgb * 0.965;
}

float colorHue(vec3 c) {
    vec4 k = vec4(0.0, -0.3333333, 0.6666667, -1.0);
    vec4 p = mix(vec4(c.bg, k.wz), vec4(c.gb, k.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    return abs(q.z + (q.w - q.y) / (6.0 * max(q.x - min(q.w, q.y), 0.00001)));
}

float hueTravel(float a, float b, float amount) {
    return a + (fract(b - a + 0.5) - 0.5) * amount;
}

// x is broad tile colour; y/z are bounded line/filigree masks. Triplanar
// mapping keeps the ornament in the 3D world, including carved vault faces.
vec3 tilePattern(vec2 coordinate, float frequency, float aa, float family) {
    vec2 grid = coordinate * frequency;
    vec2 cell = floor(grid);
    vec2 p = fract(grid) - 0.5;
    float tileSeed = hash31(vec3(cell, FractalC.y * 13.0 + FractalC.z * 29.0));
    if (tileSeed > 0.5) p = vec2(-p.y, p.x);
    float edge;
    if (family < 0.5) {
        float diamond = abs(abs(p.x) + abs(p.y) - 0.31);
        float spine = max(abs(p.x) - 0.032, abs(p.y) - 0.28);
        edge = min(diamond, abs(spine));
    } else if (family < 1.5) {
        edge = min(abs(length(p - vec2(0.5, 0.5)) - 0.5), abs(length(p + vec2(0.5, 0.5)) - 0.5));
    } else if (family < 2.5) {
        float angle = atan(p.y, p.x);
        edge = abs(length(p) - 0.29 - sin(angle * 6.0 + tileSeed * TAU + morphClock * 0.41) * 0.055);
    } else {
        vec2 diamondP = vec2(p.x + p.y, p.x - p.y) * 0.7071;
        edge = min(abs(max(abs(diamondP.x), abs(diamondP.y)) - 0.22), abs(length(p) - 0.12));
    }
    float line = 1.0 - smoothstep(0.018, 0.018 + aa, edge);
    float filigree = 1.0 - smoothstep(0.012, 0.012 + aa, abs(edge - 0.065));
    float broad = 0.5 + 0.5 * sin((p.x + p.y) * 4.6 + tileSeed * TAU + morphClock * 0.18);
    return vec3(broad * 0.65 + tileSeed * 0.35, line, filigree);
}

vec3 surfacePattern(vec3 point, vec3 normal, float footprint) {
    vec3 p = fieldRotation * point;
    p += sin(p.yzx * 1.17 + morphClock * 0.31 + signaturePhase) * morphAmount * 0.14;
    vec3 weights = pow(abs(fieldRotation * normal), vec3(5.0));
    weights /= max(weights.x + weights.y + weights.z, 0.0001);
    float family = mod(styleIndex + floor(FractalC.z * 4.0), 4.0);
    float frequency = 1.25 + FractalC.y * 1.35 + detailEnergy * 0.90;
    float aa = max(footprint * frequency * 0.80, 0.018);
    vec3 pattern = tilePattern(p.yz, frequency, aa, family) * weights.x
            + tilePattern(p.xz, frequency, aa, family) * weights.y
            + tilePattern(p.xy, frequency, aa, family) * weights.z;
    float primaryCoverage = 1.0 - smoothstep(0.24, 0.58, footprint * frequency);
    pattern.yz *= primaryCoverage * min(1.0, 0.05 / aa);
    if (detailEnergy > 0.25) {
        float fineFrequency = frequency * (2.7 + FractalC.z * 1.3);
        float fineAA = max(footprint * fineFrequency * 0.95, 0.028);
        vec3 fine = tilePattern(p.yz, fineFrequency, fineAA, mod(family + 1.0, 4.0)) * weights.x
                + tilePattern(p.xz, fineFrequency, fineAA, mod(family + 1.0, 4.0)) * weights.y
                + tilePattern(p.xy, fineFrequency, fineAA, mod(family + 1.0, 4.0)) * weights.z;
        float fineCoverage = (1.0 - smoothstep(0.08, 0.26, footprint * fineFrequency))
                * smoothstep(0.25, 0.55, detailEnergy) * min(1.0, 0.055 / fineAA);
        pattern.z = max(pattern.z, fine.y * fineCoverage * 0.48);
        pattern.x = mix(pattern.x, fine.x, fineCoverage * 0.10);
    }
    return pattern;
}

vec3 realmBackground(vec3 ray) {
    float clouds = sin(ray.x * 6.0 + sin(ray.y * 4.0) + morphClock * 0.22)
            * sin(ray.z * 5.0 - ray.y * 3.0 - morphClock * 0.17);
    float hue = FractalC.y + ray.y * 0.11 + clouds * 0.13;
    return hueColor(hue) * (0.07 + pow(0.5 + clouds * 0.5, 3.0) * 0.17) + vec3(0.012, 0.018, 0.035);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    float blend = clamp(FractalA.x, 0.0, 1.0);
    if (blend <= 0.0) { fragColor = texture(DiffuseSampler, uv); return; }

    float reduced = clamp(FractalA.w, 0.0, 1.0);
    float seconds = FractalA.y;
    float seedA = clamp(FractalB.x, 0.0, 1.0);
    float seedB = clamp(FractalB.y, 0.0, 1.0);
    detailEnergy = clamp(FractalC.x, 0.0, 1.0);
    styleIndex = clamp(floor(FractalC.w + 0.5), 0.0, 6.0);
    familyIndex = mod(styleIndex, 3.0);
    signaturePhase = seedA * TAU + seedB * 3.1 + FractalC.y * 8.7 + FractalC.z * 13.1;
    // Only fixed signature values scale the integrated clock. Intensity and
    // snapshot changes alter amplitudes, never jump an accumulated time phase.
    morphClock = seconds * (0.34 + FractalC.y * 0.18);
    morphAmount = (0.36 + detailEnergy * 0.64) * mix(1.0, 0.38, reduced);
    familyMorph = 0.5 + 0.5 * sin(morphClock * 0.58 + signaturePhase);
    fieldRotation = rotateY(seedA * 3.9 + FractalC.z * 2.1) * rotateZ(seedB * 2.6 + 0.21) * rotateX(seedA * 1.2 + FractalC.y * 1.8);
    foldRotation = rotateZ((FractalC.y - 0.5) * 0.18 + sin(morphClock * 0.71) * morphAmount * 0.038)
            * rotateY((FractalC.z - 0.5) * 0.16);
    boxScale = -1.63 - seedA * 0.49 + sin(morphClock * 0.91 + seedB * 6.0) * morphAmount * 0.13;
    minimumRadiusSquared = 0.31 + seedB * 0.18 + sin(morphClock * 0.73 + seedA * 5.0) * morphAmount * 0.055;
    if (styleIndex > 2.5 && styleIndex < 3.5) {
        minimumRadiusSquared *= 0.77;
        boxScale -= 0.17;
        foldRotation = rotateX(0.17 + FractalC.z * 0.12) * foldRotation;
    }
    chamberRadius = 2.72 + FractalB.z * 0.15 + sin(morphClock * 0.77 + signaturePhase) * morphAmount * 0.17;
    int quality = int(clamp(floor(FractalA.z + 0.5), 0.0, 2.0));
    foldIterations = quality == 0 ? 6 : (quality == 1 ? 8 : 10);

    vec3 origin = realmEyeOrigin();
    vec3 ray = realmRay(uv);
    float morph = smoothstep(0.0, 1.0, blend);
    vec3 sourcePoint = origin + ray * FAR_DISTANCE;
    vec3 sourceNormal = -ray;
    float sourceSurface = 0.0;
    float sourcePhysicalDistance = FAR_DISTANCE / max(RealmOrigin.w, 0.001);
    if (blend < 1.0) {
        float rawDepth = texture(DepthSampler, uv).r;
        sourceSurface = 1.0 - step(0.999995, rawDepth);
        if (sourceSurface > 0.5) {
            vec3 relative = sourceRelativeAt(uv, rawDepth);
            sourcePhysicalDistance = length(relative);
            sourcePoint = origin + relative * max(RealmOrigin.w, 0.001);
            sourceNormal = sourceSurfaceNormal(uv, relative, ray);
        }
        // Tiny entry/exit values only advect the reconstructed scene. The
        // following path converges to this before any fractal material appears.
        if (blend < 0.015) {
            vec2 warped = morphSourceUv(uv, sourcePoint, sourcePoint, blend);
            warped = mix(uv, clamp(warped, vec2(0.001), vec2(0.999)), sourceFrameConfidence(warped));
            fragColor = texture(DiffuseSampler, warped);
            return;
        }
    }
    int stepLimit = quality == 0 ? 72 : (quality == 1 ? 112 : 160);
    float pixelTolerance = quality == 0 ? 0.0011 : (quality == 1 ? 0.00070 : 0.00042);
    float distance = 0.015, hitTolerance = 0.002;
    vec2 hitField = vec2(0.0);
    bool hit = false;
    for (int i = 0; i < MAX_STEPS; i++) {
        if (i >= stepLimit || distance >= FAR_DISTANCE) break;
        hitField = realmField(origin + ray * distance);
        hitTolerance = 0.0018 + distance * pixelTolerance;
        if (hitField.x < hitTolerance) { hit = true; break; }
        distance += clamp(hitField.x * 0.58, 0.0012, 1.65);
    }

    vec3 background = realmBackground(ray);
    vec3 color = background;
    vec3 targetPoint = origin + ray * min(distance, FAR_DISTANCE);
    vec3 targetNormal = hit ? realmNormal(targetPoint, max(0.0022, hitTolerance * 0.66)) : -ray;
    vec3 materialPoint = blend < 1.0 ? mix(sourcePoint, targetPoint, morph) : targetPoint;
    vec3 materialNormal = normalize(mix(sourceNormal, targetNormal, morph) + vec3(0.000001));
    vec3 ornament = vec3(0.0);
    vec3 material = background;
    vec3 lineColor = background;
    if (hit) {
        vec3 point = materialPoint;
        vec3 normal = materialNormal;
        float occlusion = realmOcclusion(targetPoint + targetNormal * hitTolerance * 1.8, targetNormal);
        float viewFacing = clamp(dot(normal, -ray), 0.0, 1.0);
        float footprint = max(hitTolerance, length(point - origin) * 1.4 / max(OutSize.y, 1.0)) / max(viewFacing, 0.22);
        ornament = surfacePattern(point, normal, footprint);
        float iridescence = pow(1.0 - viewFacing, 1.7);
        float hueA = colorHue(PaletteA.rgb);
        float hueB = colorHue(PaletteB.rgb);
        float baseChroma = max(PaletteA.r, max(PaletteA.g, PaletteA.b)) - min(PaletteA.r, min(PaletteA.g, PaletteA.b));
        hueA = hueTravel(FractalC.y + seedA * 0.31, hueA, smoothstep(0.08, 0.45, baseChroma) * 0.75);
        float hue = hueTravel(hueA, hueB, ornament.x * 0.68) + hitField.y * 0.20
                + iridescence * 0.16 + sin(morphClock * 0.31 + dot(point, vec3(0.23, 0.17, 0.19))) * 0.045;
        material = hueColor(hue);
        float accentHue = hueTravel(hue + 0.29 + FractalC.z * 0.22, colorHue(PaletteC.rgb), 0.30);
        lineColor = hueColor(accentHue);
        vec3 keyLight = normalize(vec3(0.64, 0.71, -0.43));
        vec3 fillLight = normalize(vec3(-0.53, 0.28, 0.79));
        float diffuse = max(0.0, dot(normal, keyLight));
        float fill = max(0.0, dot(normal, fillLight));
        float illumination = 0.34 + diffuse * 0.52 + fill * 0.26 + viewFacing * 0.20;
        float engraving = 0.76 + ornament.x * 0.24 - ornament.z * 0.15;
        color = material * illumination * engraving * mix(0.62, 1.0, occlusion);
        // Thin emissive ornament stays chromatic instead of averaging
        // complementary RGB colours into brown/grey plastic surfaces.
        float lineEnergy = ornament.y * (0.55 + detailEnergy * 0.65) + ornament.z * (0.12 + detailEnergy * 0.16);
        color *= 1.0 - ornament.y * 0.40;
        color += lineColor * lineEnergy * mix(1.0, 0.36, reduced);
        color += hueColor(hue + 0.10) * iridescence * (0.14 + detailEnergy * 0.18);
        float specular = pow(max(0.0, dot(reflect(ray, normal), keyLight)), 34.0) * 0.18;
        color += hueColor(hue + 0.08) * specular * mix(1.0, 0.35, reduced);
        float fog = 1.0 - exp(-length(point - origin) * 0.025);
        color = mix(color, background + hueColor(FractalC.z + 0.14) * 0.05, fog);
    }

    // Per-channel exponential mapping preserves luminous saturated lines;
    // bounded highlights and steady lighting avoid any strobe/exposure pulse.
    color = vec3(1.0) - exp(-max(color, vec3(0.0)) * 1.12);
    color = pow(clamp(color, 0.0, 1.0), vec3(0.90));
    color = mix(color, vec3(luma(color)), reduced * 0.40);
    color = mix(color, color * 0.72 + vec3(0.10), reduced * 0.65);
    if (blend >= 1.0) {
        fragColor = vec4(color, 1.0);
    } else {
        vec2 sourceUv = morphSourceUv(uv, sourcePoint, targetPoint, blend);
        float frameConfidence = sourceFrameConfidence(sourceUv);
        vec2 safeSourceUv = clamp(sourceUv, vec2(0.001), vec2(0.999));
        float sampledDepth = texture(DepthSampler, safeSourceUv).r;
        float sampledSurface = 1.0 - step(0.999995, sampledDepth);
        float sampledDistance = sampledSurface > 0.5 ? length(sourceRelativeAt(safeSourceUv, sampledDepth)) : sourcePhysicalDistance;
        // Reject foreground/sky crossings and unrelated occluded source layers.
        float correspondence = 1.0 - smoothstep(max(0.65, sourcePhysicalDistance * 0.22),
                max(1.10, sourcePhysicalDistance * 0.42), abs(sampledDistance - sourcePhysicalDistance));
        correspondence *= (1.0 - abs(sampledSurface - sourceSurface)) * frameConfidence;
        sourceUv = mix(uv, safeSourceUv, correspondence);
        vec4 source = texture(DiffuseSampler, sourceUv);
        float warpedLight = 0.70 + max(0.0, dot(materialNormal, normalize(vec3(0.64, 0.71, -0.43)))) * 0.50;
        vec3 oldMaterial = source.rgb * mix(1.0, warpedLight, morph * sourceSurface);
        float growth = morph + (ornament.x - 0.5) * morph * (1.0 - morph) * 0.90
                + sin(dot(materialPoint, vec3(0.47, 0.31, 0.39)) + signaturePhase) * morph * (1.0 - morph) * 0.35;
        float coverage = smoothstep(0.16, 0.88, growth);
        // Source textures acquire anchored engraving while their reconstructed
        // depth/normals move toward the new solid, then recede into its material.
        vec3 engraved = source.rgb * (vec3(0.45) + material * 0.95) + lineColor * ornament.y * 0.22;
        oldMaterial = mix(oldMaterial, engraved, smoothstep(0.04, 0.72, morph) * (1.0 - coverage) * 0.70);
        fragColor = vec4(clamp(mix(oldMaterial, color, coverage), 0.0, 1.0), mix(source.a, 1.0, coverage));
    }
}
