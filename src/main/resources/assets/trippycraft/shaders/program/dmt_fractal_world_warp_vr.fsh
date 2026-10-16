#version 150

// An anchored, evolving solid field. Minecraft ecology shapes it everywhere;
// hidden scene evidence becomes procedural material, never a transparent layer.
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
uniform vec4 RealmDynamics; // uncapped log dose drive, cinematic ingress, trip seconds, log2(1+dose)
uniform vec4 WorldInfluenceA; // organic, constructed, cave/depth, water
uniform vec4 WorldInfluenceB; // light, night/rain, entities, world motion
uniform vec4 WorldInfluenceC; // sampled real-world RGB, solid/height relief
uniform vec4 WorldInfluenceD; // signed terrain gradient XYZ, strength
uniform vec4 WorldAttractor; // entity trip-anchor-relative realm XYZ, strength
uniform vec4 RealmOrigin; // anchored head position in realm units, physical-to-realm scale
uniform vec4 PaletteA;
uniform vec4 PaletteB;
uniform vec4 PaletteC;

out vec4 fragColor;

const float CELL_SIZE = 13.6;
const float FAR_DISTANCE = 66.0;
const int MAX_STEPS = 192;
const float TAU = 6.28318530718;

mat3 fieldRotation;
mat3 foldRotation;
float boxScale;
float minimumRadiusSquared;
float chamberRadius;
float signaturePhase;
float ecologicalPhase;
float morphClock;
float morphAmount;
float detailEnergy;
float styleIndex;
float doseDrive;
float doseDetail;
float detailFrequency;
float worldRelief;
vec4 geometryLanesA;
vec4 geometryLanesB;
vec4 materialLanes;
vec3 familyWeights;
vec4 familyChapters;
vec3 chamberAspect;
float vaultRoundness;
float chromaticJourney;
vec4 doseLayerWeights;
int foldIterations;
int templeIterations;
int ornamentLayers;
bool allowEmptyShortcut;
vec4 layoutLanes;
vec4 motionLanes;
vec2 motionSecondary;
vec4 surfaceMaterialsA; // glass, membrane, tissue, liquid metal
vec2 surfaceMaterialsB; // emissive glyph, kaleidoscopic shell
vec4 patternLanes; // glyph, eyes, weave, sacred geometry
float patternVeil;
vec4 lightingLanes; // ambient, neon, deep space, bioluminescence
vec2 atmosphereLanes; // fog, shimmer
vec4 worldStructure; // terrain, entity attraction, water, organic
float foldDepth;
float templeDepth;
float worldDoseGain;
float fieldDistanceScale;
float carvedVoidBound;

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

float smoothNoise1(float position, float salt) {
    float cell = floor(position), f = fract(position);
    float ease = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = hash31(vec3(cell, salt, salt * 0.37 + 11.0));
    float b = hash31(vec3(cell + 1.0, salt, salt * 0.37 + 11.0));
    return mix(a, b, ease);
}

// Independent quintic-noise chapters have no shared repeating sine period.
// Exposed for GPU probes of the actual lanes used by geometry and materials.
vec4 proceduralLanes(float clock, float salt) {
    vec4 a = vec4(smoothNoise1(clock * 0.071 + 3.1, salt + 1.7),
            smoothNoise1(clock * 0.113 + 13.7, salt + 19.3),
            smoothNoise1(clock * 0.047 + 29.1, salt + 37.1),
            smoothNoise1(clock * 0.173 + 41.3, salt + 53.7));
    vec4 b = vec4(smoothNoise1(clock * 0.0271 + 8.3, salt + 71.9),
            smoothNoise1(clock * 0.0433 + 23.9, salt + 89.3),
            smoothNoise1(clock * 0.0197 + 43.7, salt + 107.1),
            smoothNoise1(clock * 0.0661 + 67.1, salt + 127.7));
    return a * 0.70 + b * 0.30;
}

vec4 sparseGroup(vec4 signal, float contrast) {
    vec4 weights = exp((signal - 0.5) * contrast);
    weights /= dot(weights, vec4(1.0));
    vec4 ease = clamp((weights - 0.020) / 0.94, vec4(0.0), vec4(1.0));
    weights = ease * ease * ease * (ease * (ease * 6.0 - 15.0) + 10.0);
    return weights / max(dot(weights, vec4(1.0)), 0.000001);
}

float quinticUnit(float value) {
    float x = clamp(value, 0.0, 1.0);
    return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
}

float cinematicProgress(float presence) {
    return min(clamp(presence, 0.0, 1.0), clamp(RealmDynamics.y, 0.0, 1.0));
}

float cinematicCoverage(float progress, float variation) {
    float materialProgress = pow(clamp(progress, 0.0, 1.0), 1.35);
    // With |variation|<=1 this remains below one for every progress<1. The last
    // quarter retains a visible transition instead of saturating at 0.88.
    return materialProgress + clamp(variation, -1.0, 1.0) * 0.62 * materialProgress * (1.0 - materialProgress);
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
    float morph = blend * (0.72 + 0.28 * blend);
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
    vec2 previous = vec2(length(z), 1.0);
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
        vec2 current = vec2(length(z) / max(derivative, 0.000001) - 0.0045, clamp(orbit * 0.55, 0.0, 1.0));
        if (float(i + 1) > floor(foldDepth)) return mix(previous, current, quinticUnit(fract(foldDepth)));
        previous = current;
    }
    return previous;
}

vec2 mengerTemple(vec3 q) {
    float distance = boxDistance(q, vec3(1.22));
    float scale = 1.0;
    float orbit = 1.0;
    float aperture = 0.82 + geometryLanesB.y * 0.23 + sin(morphClock * 0.87 + signaturePhase) * morphAmount * 0.10;
    aperture += motionSecondary.x * 0.13;
    for (int i = 0; i < 5; i++) {
        if (i >= templeIterations) break;
        vec3 a = mod(q * scale, 2.0) - 1.0;
        scale *= 3.0;
        vec3 r = abs(1.0 - 3.0 * abs(a));
        float cut = (min(max(r.x, r.y), min(max(r.y, r.z), max(r.z, r.x))) - aperture) / scale;
        float depthWeight = quinticUnit(templeDepth - float(i));
        distance = mix(distance, max(distance, cut), depthWeight);
        orbit = mix(orbit, min(orbit, dot(r, vec3(0.24, 0.37, 0.39))), depthWeight);
    }
    return vec2(distance, clamp(orbit, 0.0, 1.0));
}

float realmEmptySpace(vec3 position, vec3 local) {
    local += vec3(local.y * WorldInfluenceD.x, dot(local.xz, WorldInfluenceD.xz) * WorldInfluenceD.y,
            local.y * WorldInfluenceD.z) * worldStructure.x * worldDoseGain * 0.18;
    local.y += WorldInfluenceD.y * worldStructure.x * worldDoseGain * 0.18;
    vec3 room = local * chamberAspect;
    float faceted = max(max(abs(room.x), abs(room.y)), max(abs(room.z), (abs(room.x) + abs(room.z)) * 0.72));
    float rounded = length(room) * 0.83;
    float vault = mix(faceted, rounded, vaultRoundness);
    float vaultRibs = sin(local.x * 3.4 + sin(local.z * 1.5) + morphClock * 0.72 + ecologicalPhase)
            * sin(local.y * 3.1 - local.z * 1.2 - morphClock * 0.51);
    float chamber = vault - chamberRadius + vaultRibs * (0.04 + motionLanes.x * 0.13 + worldRelief * 0.04);
    float twistX = 0.34 * sin(position.x * 0.33 + signaturePhase) + sin(morphClock * 0.63 + position.x * 0.19) * morphAmount * 0.28;
    float twistZ = 0.31 * sin(position.z * 0.29 + signaturePhase * 1.3) - sin(morphClock * 0.57 + position.z * 0.23) * morphAmount * 0.30;
    vec2 crossX = mat2(cos(twistX), sin(twistX), -sin(twistX), cos(twistX)) * local.yz;
    vec2 crossZ = mat2(cos(twistZ), sin(twistZ), -sin(twistZ), cos(twistZ)) * local.xy;
    crossX += vec2(sin(position.x * 0.43 + morphClock * 0.47), sin(position.x * 0.31 + signaturePhase)) * 0.18;
    crossZ += vec2(sin(position.z * 0.37 - morphClock * 0.41), sin(position.z * 0.41 + signaturePhase)) * 0.18;
    float widthX = 0.98 + geometryLanesB.w * 0.44 + sin(position.x * 0.71 + signaturePhase + morphClock * 0.67) * 0.15;
    float widthZ = 1.01 + geometryLanesA.w * 0.39 + sin(position.z * 0.67 + signaturePhase * 0.73 - morphClock * 0.61) * 0.16;
    float angularX = max(max(abs(crossX.x), abs(crossX.y) * 0.94), (abs(crossX.x) + abs(crossX.y)) * 0.78);
    float angularZ = max(max(abs(crossZ.x) * 0.96, abs(crossZ.y)), (abs(crossZ.x) + abs(crossZ.y)) * 0.79);
    float tunnelX = mix(angularX, length(crossX) * 0.94, vaultRoundness * 0.72) - widthX;
    float tunnelZ = mix(angularZ, length(crossZ) * 0.94, vaultRoundness * 0.72) - widthZ;
    float corridor = min(tunnelX, tunnelZ);
    float nestedPeriod = 5.5 + motionLanes.y * 1.2;
    vec3 nestedLocal = mod(local + nestedPeriod * 0.5, nestedPeriod) - nestedPeriod * 0.5;
    float nestedRoom = boxDistance(nestedLocal, nestedPeriod * vec3(0.36, 0.31, 0.36));
    float nestedDoor = min(max(abs(local.y), abs(local.z) * 0.95), max(abs(local.x) * 0.95, abs(local.y))) - 0.67;
    float nested = min(nestedRoom, nestedDoor);
    float gallery = boxDistance(local, vec3(4.8, 2.05 + motionLanes.y * 0.85, 4.7));
    float layoutDistance = chamber * layoutLanes.x + nested * layoutLanes.y + gallery * layoutLanes.z + corridor * layoutLanes.w;
    // The persistent trip-anchor cell has an empty core. Unlike a mandatory
    // X/Z passage, this does not dictate the surrounding layout in every chapter.
    return min(layoutDistance, length(local) - 1.38);
}

float realmIndependentStructure(vec3 position, vec3 local) {
    float structure = 10000.0;
    if (layoutLanes.z > 0.0) {
        vec2 columnCell = mod(local.xz + 1.85, 3.7) - 1.85;
        float columns = length(columnCell) - (0.08 + layoutLanes.z * 0.36);
        columns = max(columns, abs(local.y) - 2.7);
        columns = max(columns, 1.45 - length(local));
        structure = min(structure, columns + (1.0 - layoutLanes.z) * 1.25);
    }
    if (worldStructure.y > 0.0) {
        vec3 entityCrystal = abs(position - WorldAttractor.xyz);
        float crystalBody = (entityCrystal.x + entityCrystal.y * 0.70 + entityCrystal.z - worldStructure.y * 0.42) * 0.55;
        crystalBody = max(crystalBody, 1.35 - length(local));
        structure = min(structure, crystalBody + (1.0 - worldStructure.y) * 0.45);
    }
    return structure;
}

// Distance and orbit material coordinate. Every frame combines independently
// weighted folds, mirrors, inversion, architecture and organic lace. A trip
// signature biases that evolving mixture; it never selects a fixed catalogue.
vec2 realmField(vec3 position) {
    vec3 cell = floor((position + CELL_SIZE * 0.5) / CELL_SIZE);
    vec3 local = position - cell * CELL_SIZE;
    float cellSeed = hash31(cell + vec3(FractalB.x * 23.0, FractalB.y * 31.0, FractalC.y * 47.0));
    float emptySpace = realmEmptySpace(position, local);
    float independentStructure = realmIndependentStructure(position, local);
    float cellBoundary = min(CELL_SIZE * 0.5 - abs(local.x), min(CELL_SIZE * 0.5 - abs(local.y), CELL_SIZE * 0.5 - abs(local.z)));
    carvedVoidBound = max(0.0, min(min(-emptySpace - 0.16, independentStructure) * 0.45, cellBoundary));
    // No solid can appear deep in a deliberately carved chamber. A lower
    // distance bound skips expensive folds here; the 0.16 margin exceeds all
    // possible emboss/ornament displacement, so actual surfaces use the full DE.
    if (allowEmptyShortcut && -emptySpace > 0.24 && independentStructure > 0.24)
        return vec2(carvedVoidBound, -1.0);
    vec3 rotated = fieldRotation * local;
    float twist = sin(local.y * 0.42 + morphClock * 0.57 + signaturePhase) * morphAmount * (0.025 + motionLanes.z * 0.35);
    vec3 q = rotateY(twist) * rotated;
    q.x += q.x / sqrt(q.x * q.x + 0.20) * sin(morphClock * 0.71 + abs(q.y) * 0.38)
            * motionLanes.y * morphAmount * 0.22;
    q.y += sin(q.x * 0.91 + q.z * 0.63 - morphClock * 0.53) * motionLanes.w * morphAmount * 0.32;
    q += sin(q.yzx * 0.64 + vec3(0.0, 2.1, 4.2) + morphClock * 0.61 + ecologicalPhase) * morphAmount * (0.05 + geometryLanesB.z * 0.10);
    vec3 mirrorOffset = vec3(0.34, 0.21, 0.42) + geometryLanesA.x * 0.12;
    q = mix(q, abs(q + mirrorOffset) - mirrorOffset, geometryLanesA.y * (0.10 + motionLanes.y * 0.60));
    vec3 nestedFold = abs(foldRotation * q + vec3(0.43, 0.31, 0.27)) - vec3(0.43, 0.31, 0.27);
    float recursiveEngagement = doseDrive / (3.5 + doseDrive);
    q = mix(q, nestedFold, recursiveEngagement * (0.10 + geometryLanesA.x * 0.26));
    float inverseRadius = 2.8 / (dot(q, q) + 2.2);
    q *= mix(1.0, inverseRadius, geometryLanesA.w * 0.35);
    vec3 toAttractor = WorldAttractor.xyz - position;
    float attractorDistance = length(toAttractor);
    float attractorForce = worldStructure.y / (1.0 + attractorDistance * attractorDistance * 0.12);
    q += toAttractor / max(attractorDistance, 0.15) * attractorForce * (0.12 + doseDetail * 0.025);

    float worldToFractal = (0.47 + FractalC.y * 0.075 + (cellSeed - 0.5) * 0.045) * (1.0 + doseDetail * 0.025);
    vec2 shape = vec2(0.0);
    if (familyWeights.x > 0.0) {
        vec2 crystal = mandelbox(q * worldToFractal);
        crystal.x /= worldToFractal;
        shape += crystal * familyWeights.x;
    }
    float architectureFrequency = 1.0 + doseDrive * 0.11;
    vec3 templeScale = mix(vec3(0.245), vec3(0.215, 0.285, 0.25), geometryLanesB.y) * architectureFrequency;
    vec3 templeOffset = vec3(0.19, -0.13, 0.27) * geometryLanesA.y;
    if (familyWeights.y > 0.0) {
        vec2 temple = mengerTemple(q * templeScale + templeOffset);
        temple.x /= max(templeScale.x, max(templeScale.y, templeScale.z));
        shape += temple * familyWeights.y;
    }
    float organicFrequency = 0.93 + FractalC.z * 0.32 + doseDetail * 0.12;
    if (familyWeights.z > 0.0) {
        vec3 g = q * organicFrequency;
        g += sin(g.zxy * 0.60 + morphClock * 0.43) * morphAmount * geometryLanesB.z * 0.25;
        float gyroid = dot(sin(g), cos(g.yzx));
        float thickness = 0.20 + geometryLanesB.x * 0.34 + WorldInfluenceA.w * 0.10;
        vec2 organic = vec2((abs(gyroid) - thickness) * 0.39 / organicFrequency,
                0.5 + 0.5 * sin(g.x + g.y - g.z));
        shape += organic * familyWeights.z;
    }

    // Finite rooms are sealed between cells. Their carved vaults are faceted,
    // corrugated and embossed, rather than enormous smooth cylindrical holes.
    float outerSolid = 6.10 - length(local);
    float solid = min(shape.x, outerSolid);
    // Surface-scale relief remains visible on the CSG cut faces themselves.
    float reliefFrequency = 5.2 + detailFrequency * 1.15;
    float emboss = sin(position.x * reliefFrequency + sin(position.z * 3.1) + morphClock * 0.45 + worldRelief * 2.7)
            * sin(position.y * reliefFrequency * 0.88 - position.z * 2.3 - morphClock * 0.36);
    float reliefAmplitude = (0.010 + detailEnergy * 0.021 + worldRelief * 0.009) / (1.0 + doseDetail * 0.13);
    reliefAmplitude *= 0.70 + motionSecondary.x * 0.60;
    // Actual nested sculptural relief, not just increasingly fine colour.
    // Counts stay within quality budgets; dose engages each layer smoothly,
    // while spacing and the coordinate fold continue beyond those thresholds.
    vec3 ornamentPoint = fieldRotation * position;
    float ornamentFrequency = 0.95 + doseDetail * 0.10;
    float ornamentAmplitude = detailEnergy * 0.060;
    float nestedRelief = 0.0;
    for (int level = 0; level < 4; level++) {
        if (level >= ornamentLayers || doseLayerWeights[level] <= 0.0) break;
        vec3 facets = abs(mod(ornamentPoint * ornamentFrequency + vec3(0.17, 0.31, 0.43), 2.0) - 1.0);
        float ridge = clamp(1.0 - abs(facets.x + facets.y + facets.z - 0.90) * 3.0, 0.0, 1.0);
        nestedRelief += (ridge - 0.28) * ornamentAmplitude * doseLayerWeights[level];
        ornamentPoint = abs(foldRotation * ornamentPoint + vec3(0.37, 0.61, 0.43)) - vec3(0.37, 0.61, 0.43);
        ornamentFrequency *= 1.87;
        ornamentAmplitude *= 0.45;
    }
    float distance = max(solid, -emptySpace) + emboss * reliefAmplitude - nestedRelief;
    distance = min(distance, independentStructure);
    return vec2(distance * fieldDistanceScale, clamp(shape.y * 0.86 + cellSeed * 0.14, 0.0, 1.0));
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
        float field = realmField(point + normal * distance).x * 0.52 / max(fieldDistanceScale, 0.001);
        occlusion += max(0.0, distance - field) / distance * weight;
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
    float diamond = min(abs(abs(p.x) + abs(p.y) - 0.31), abs(max(abs(p.x) - 0.032, abs(p.y) - 0.28)));
    float circuit = min(abs(length(p - vec2(0.5, 0.5)) - 0.5), abs(length(p + vec2(0.5, 0.5)) - 0.5));
    float angle = atan(p.y, p.x);
    float petal = abs(length(p) - 0.29 - sin(angle * (5.0 + geometryLanesA.y * 3.0) + tileSeed * TAU + morphClock * 0.41) * 0.055);
    vec2 diamondP = vec2(p.x + p.y, p.x - p.y) * 0.7071;
    float crystal = min(abs(max(abs(diamondP.x), abs(diamondP.y)) - 0.22), abs(length(p) - 0.12));
    float eye = min(abs(length(vec2(p.x, p.y * 1.9)) - 0.30), abs(length(p) - 0.075));
    float weaveX = abs(sin(p.x * 12.566 + sin(p.y * 6.283) * 0.28));
    float weaveY = abs(sin(p.y * 12.566 - sin(p.x * 6.283) * 0.28));
    float weave = min(weaveX, weaveY) * 0.10;
    vec4 edges = vec4(min(diamond, circuit), eye, weave, min(petal, crystal));
    vec4 weights = max(patternLanes, vec4(0.001));
    weights *= 0.72 + 0.28 * cos(vec4(0.0, 1.7, 3.3, 4.9) + family);
    weights /= max(dot(weights, vec4(1.0)), 0.0001);
    float line = dot(vec4(1.0) - smoothstep(vec4(0.018), vec4(0.018 + aa), edges), weights);
    float filigree = dot(vec4(1.0) - smoothstep(vec4(0.012), vec4(0.012 + aa), abs(edges - 0.065)), weights);
    float broad = 0.5 + 0.5 * sin((p.x + p.y) * 4.6 + tileSeed * TAU + morphClock * 0.18);
    return vec3(broad * 0.65 + tileSeed * 0.35, line, filigree);
}

vec3 surfacePattern(vec3 point, vec3 normal, float footprint) {
    vec3 p = fieldRotation * point;
    p += sin(p.yzx * 1.17 + morphClock * 0.31 + signaturePhase) * morphAmount * (0.025 + motionSecondary.y * 0.18);
    vec3 weights = pow(abs(fieldRotation * normal), vec3(5.0));
    weights /= max(weights.x + weights.y + weights.z, 0.0001);
    float family = signaturePhase * 0.31 + materialLanes.w * TAU;
    float frequency = (1.25 + FractalC.y * 1.35 + detailEnergy * 0.70) * (1.0 + doseDrive * 0.18);
    float aa = max(footprint * frequency * 0.80, 0.018);
    vec3 pattern = tilePattern(p.yz, frequency, aa, family) * weights.x
            + tilePattern(p.xz, frequency, aa, family) * weights.y
            + tilePattern(p.xy, frequency, aa, family) * weights.z;
    float primaryCoverage = 1.0 - smoothstep(0.24, 0.58, footprint * frequency);
    pattern.yz *= primaryCoverage * min(1.0, 0.05 / aa);
    if (detailEnergy > 0.0) {
        float fineFrequency = frequency * (2.35 + FractalC.z * 1.1 + doseDetail * 0.11);
        float fineAA = max(footprint * fineFrequency * 0.95, 0.028);
        vec3 fine = tilePattern(p.yz, fineFrequency, fineAA, family + 1.7) * weights.x
                + tilePattern(p.xz, fineFrequency, fineAA, family + 1.7) * weights.y
                + tilePattern(p.xy, fineFrequency, fineAA, family + 1.7) * weights.z;
        float fineCoverage = (1.0 - smoothstep(0.08, 0.26, footprint * fineFrequency))
                * detailEnergy * (0.35 + materialLanes.x * 0.65) * min(1.0, 0.055 / fineAA);
        pattern.z = max(pattern.z, fine.y * fineCoverage * 0.48);
        pattern.x = mix(pattern.x, fine.x, fineCoverage * 0.10);
    }
    float veinFrequency = 1.8 + doseDetail * 0.28;
    float veinField = sin(p.x * veinFrequency + sin(p.y * 1.7 + morphClock * 0.19)
            + sin(p.z * 1.9 - morphClock * 0.23) * (0.5 + geometryLanesB.z));
    float veinAA = max(0.025, footprint * veinFrequency);
    float veins = (1.0 - smoothstep(0.035, 0.035 + veinAA, abs(veinField)))
            * min(1.0, 0.07 / veinAA) * materialLanes.y;
    pattern.y = max(pattern.y, veins * (0.45 + WorldInfluenceA.x * 0.35));
    pattern.x = mix(pattern.x, 0.5 + veinField * 0.5, materialLanes.z * 0.24);
    float veil = pow(0.5 + 0.5 * sin(p.x * 1.9 + sin(p.y * 2.3 - morphClock * 0.29) + p.z * 1.1), 4.0);
    pattern.z = max(pattern.z, veil * patternVeil * 0.42);
    return pattern;
}

vec3 realmBackground(vec3 ray) {
    float clouds = sin(ray.x * 6.0 + sin(ray.y * 4.0) + morphClock * 0.22)
            * sin(ray.z * 5.0 - ray.y * 3.0 - morphClock * 0.17);
    float hue = hueTravel(FractalC.y, colorHue(WorldInfluenceC.rgb + vec3(0.001)), 0.20)
            + ray.y * 0.11 + clouds * 0.13 + materialLanes.z * 0.16 + chromaticJourney;
    float light = 0.06 + WorldInfluenceB.x * 0.04 + pow(0.5 + clouds * 0.5, 3.0) * 0.17;
    return hueColor(hue) * light + vec3(0.012, 0.018, 0.035);
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
    doseDrive = max(RealmDynamics.x, 0.0);
    doseDetail = log2(1.0 + doseDrive);
    detailFrequency = detailEnergy * (1.0 + doseDetail * 0.60);
    doseLayerWeights = smoothstep(vec4(0.2, 1.1, 2.4, 4.0), vec4(2.0, 3.7, 6.4, 9.4), vec4(doseDrive));
    worldRelief = clamp(WorldInfluenceC.w, 0.0, 1.0);
    styleIndex = clamp(floor(FractalC.w + 0.5), 0.0, 6.0);
    signaturePhase = seedA * TAU + seedB * 3.1 + FractalC.y * 8.7 + FractalC.z * 13.1;
    // Changing real-world colours shift continuous coordinates, never the
    // immutable hash salts that determine a trip's noise chapters or motifs.
    ecologicalPhase = dot(WorldInfluenceC.rgb, vec3(0.41, 0.73, 0.29)) * 0.55;
    // Only fixed signature values scale the integrated clock. Intensity and
    // snapshot changes alter amplitudes, never jump an accumulated time phase.
    morphClock = seconds * (0.34 + FractalC.y * 0.18);
    morphAmount = (0.32 + detailEnergy * 0.68) * (1.0 + doseDetail * 0.16) / (1.0 + doseDetail * 0.07) * mix(1.0, 0.38, reduced);
    geometryLanesA = proceduralLanes(morphClock, seedA * 31.7 + FractalC.y * 71.3);
    geometryLanesB = proceduralLanes(morphClock * 0.619, seedB * 47.3 + FractalC.z * 89.1);
    materialLanes = proceduralLanes(morphClock * 0.937, signaturePhase * 13.7 + 113.3);
    geometryLanesA = clamp(vec4(0.10) + geometryLanesA * 0.78
            + vec4(WorldInfluenceA.z * 0.13 + WorldInfluenceB.y * 0.07,
                    WorldInfluenceA.y * 0.15 + WorldInfluenceB.z * 0.09,
                    WorldInfluenceA.w * 0.15 + WorldInfluenceB.w * 0.13,
                    WorldInfluenceA.w * 0.17 + WorldInfluenceB.y * 0.08), vec4(0.03), vec4(0.97));
    geometryLanesB = clamp(vec4(0.10) + geometryLanesB * 0.75
            + vec4(WorldInfluenceA.x * 0.20 + WorldInfluenceA.w * 0.12,
                    WorldInfluenceA.y * 0.23 + WorldInfluenceB.z * 0.08,
                    WorldInfluenceA.x * 0.15 + WorldInfluenceB.x * 0.08,
                    worldRelief * 0.18 + WorldInfluenceB.w * 0.08), vec4(0.03), vec4(0.97));
    materialLanes = clamp(materialLanes * 0.80 + vec4(0.10)
            + vec4(WorldInfluenceB.z * 0.13, WorldInfluenceA.x * 0.13,
                    WorldInfluenceA.w * 0.13, WorldInfluenceB.y * 0.13), vec4(0.03), vec4(0.97));
    familyChapters = proceduralLanes(morphClock * 2.3, seedA * 43.1 + seedB * 67.3 + FractalC.z * 97.7);
    vec3 scores = (familyChapters.xyz - 0.5) * 2.0
            + vec3(geometryLanesA.x, geometryLanesB.y, geometryLanesB.x) * 0.20
            + vec3(WorldInfluenceA.z, WorldInfluenceA.y, WorldInfluenceA.x + WorldInfluenceA.w * 0.5) * 0.14;
    scores += sin(morphClock * vec3(0.31, 0.27, 0.23) + vec3(0.0, 2.1, 4.2) + signaturePhase) * 0.28;
    scores += cos(vec3(0.0, 2.1, 4.2) + styleIndex * 1.91 + FractalC.y * TAU) * 0.06;
    familyWeights = exp(scores * 7.5);
    familyWeights /= familyWeights.x + familyWeights.y + familyWeights.z;
    // C2 zero-weight transitions permit genuine family dominance and avoid
    // paying for an invisible field. No catalogue switch or minimum mix locks
    // the realm into the same spiny hybrid for the entire trip.
    vec3 familyEase = clamp((familyWeights - 0.020) / 0.93, vec3(0.0), vec3(1.0));
    familyWeights = familyEase * familyEase * familyEase * (familyEase * (familyEase * 6.0 - 15.0) + 10.0);
    familyWeights /= familyWeights.x + familyWeights.y + familyWeights.z;
    chamberAspect = vec3(0.70 + geometryLanesA.y * 0.47,
            0.77 + geometryLanesB.y * 0.54, 0.71 + geometryLanesA.w * 0.49);
    vaultRoundness = clamp(familyWeights.z * 0.78 + geometryLanesB.z * 0.14, 0.0, 0.92);
    vec4 colorChapters = proceduralLanes(morphClock * 0.71, signaturePhase * 17.3 + 223.7);
    chromaticJourney = (colorChapters.w - 0.5) * 0.80 + (materialLanes.z - 0.5) * 0.50;
    fieldRotation = rotateY(seedA * 3.9 + FractalC.z * 2.1) * rotateZ(seedB * 2.6 + 0.21) * rotateX(seedA * 1.2 + FractalC.y * 1.8);
    foldRotation = rotateZ((FractalC.y - 0.5) * 0.18 + (geometryLanesA.y - 0.5) * 0.15 + sin(morphClock * 0.71) * morphAmount * 0.025)
            * rotateY((FractalC.z - 0.5) * 0.16 + (geometryLanesA.w - 0.5) * 0.14);
    boxScale = -1.67 - seedA * 0.36 + (geometryLanesA.x - 0.5) * 0.24 + sin(morphClock * 0.91 + seedB * 6.0) * morphAmount * 0.09;
    minimumRadiusSquared = 0.25 + geometryLanesA.x * 0.26 + sin(morphClock * 0.73 + seedA * 5.0) * morphAmount * 0.035;
    chamberRadius = 2.72 + FractalB.z * 0.12 - WorldInfluenceA.z * 0.18 + WorldInfluenceA.w * 0.08 + worldRelief * 0.10
            + sin(morphClock * 0.77 + signaturePhase) * morphAmount * 0.13;
    int quality = int(clamp(floor(FractalA.z + 0.5), 0.0, 2.0));
    float recursionEngagement = doseDrive / (1.7 + doseDrive);
    float maxFoldDepth = quality == 0 ? 6.0 : (quality == 1 ? 8.0 : 10.0);
    float minFoldDepth = quality == 0 ? 3.5 : (quality == 1 ? 4.0 : 5.0);
    foldDepth = mix(minFoldDepth, maxFoldDepth, recursionEngagement);
    foldIterations = int(ceil(foldDepth));
    float maxTempleDepth = quality == 0 ? 2.0 : (quality == 1 ? 3.0 : 4.0);
    templeDepth = mix(1.25, maxTempleDepth, recursionEngagement);
    templeIterations = int(ceil(templeDepth));
    ornamentLayers = quality == 0 ? 2 : (quality == 1 ? 3 : 4);
    // Layout, motion, material, pattern and atmosphere have independent salts
    // and clocks. Their mixtures form combinations, not named trip presets.
    vec4 layoutSignal = proceduralLanes(morphClock * 1.83, signaturePhase * 19.1 + 271.3);
    layoutSignal += vec4(WorldInfluenceA.x * 0.08, WorldInfluenceA.z * 0.10,
            WorldInfluenceA.y * 0.14, WorldInfluenceB.w * 0.08);
    layoutLanes = sparseGroup(layoutSignal, 7.5);
    vec4 motionSignal = proceduralLanes(morphClock * 1.37, signaturePhase * 23.7 + 293.9);
    vec4 broadMotion = exp(motionSignal * 1.6);
    broadMotion /= dot(broadMotion, vec4(1.0));
    float motionInteraction = doseDrive / (5.0 + doseDrive) * 0.75;
    motionLanes = mix(sparseGroup(motionSignal, 9.0), broadMotion, motionInteraction);
    motionLanes *= (0.80 + doseDrive / (1.0 + doseDrive) * 0.20) * mix(1.0, 0.38, reduced);
    motionSecondary = proceduralLanes(morphClock * 0.817, signaturePhase * 29.3 + 317.1).xy
            * vec2(doseLayerWeights.y, 0.15 + doseLayerWeights.z * 0.85);
    vec4 materialSignalA = proceduralLanes(morphClock * 1.117, signaturePhase * 31.9 + 337.3);
    vec2 materialSignalB = proceduralLanes(morphClock * 0.733, signaturePhase * 37.1 + 359.9).zw;
    surfaceMaterialsA = exp((materialSignalA - 0.5) * 8.5);
    surfaceMaterialsB = exp((materialSignalB - 0.5) * 8.5);
    float materialSum = dot(surfaceMaterialsA, vec4(1.0)) + dot(surfaceMaterialsB, vec2(1.0));
    surfaceMaterialsA /= materialSum;
    surfaceMaterialsB /= materialSum;
    vec4 materialEaseA = clamp((surfaceMaterialsA - 0.015) / 0.94, vec4(0.0), vec4(1.0));
    vec2 materialEaseB = clamp((surfaceMaterialsB - 0.015) / 0.94, vec2(0.0), vec2(1.0));
    surfaceMaterialsA = materialEaseA * materialEaseA * materialEaseA * (materialEaseA * (materialEaseA * 6.0 - 15.0) + 10.0);
    surfaceMaterialsB = materialEaseB * materialEaseB * materialEaseB * (materialEaseB * (materialEaseB * 6.0 - 15.0) + 10.0);
    materialSum = dot(surfaceMaterialsA, vec4(1.0)) + dot(surfaceMaterialsB, vec2(1.0));
    surfaceMaterialsA /= max(materialSum, 0.000001);
    surfaceMaterialsB /= max(materialSum, 0.000001);
    vec4 patternSignal = proceduralLanes(morphClock * 0.889, signaturePhase * 41.3 + 379.7);
    patternSignal += vec4(WorldInfluenceA.y * 0.10, WorldInfluenceB.z * 0.12, WorldInfluenceA.x * 0.08, WorldInfluenceA.z * 0.08);
    vec4 broadPattern = exp(patternSignal * 2.0);
    broadPattern /= dot(broadPattern, vec4(1.0));
    patternLanes = mix(sparseGroup(patternSignal, 7.0), broadPattern, doseDrive / (4.0 + doseDrive) * 0.70);
    patternVeil = proceduralLanes(morphClock * 0.497, signaturePhase * 43.7 + 401.9).z
            * doseLayerWeights.y * (0.25 + WorldInfluenceA.w * 0.75);
    lightingLanes = sparseGroup(proceduralLanes(morphClock * 0.577, signaturePhase * 47.3 + 419.3), 7.0);
    atmosphereLanes = proceduralLanes(morphClock * 0.377, signaturePhase * 53.1 + 443.9).xy;
    worldStructure = clamp(vec4(WorldInfluenceD.w, WorldAttractor.w, WorldInfluenceA.w, WorldInfluenceA.x), vec4(0.0), vec4(1.0));
    worldDoseGain = 1.0 + doseDrive / (2.0 + doseDrive) * 0.38 + doseDetail * 0.09;
    fieldDistanceScale = 0.52 / (1.0 + motionLanes.z * 0.40 + motionLanes.w * 0.20 + motionLanes.y * 0.15
            + worldStructure.x * worldDoseGain * 0.12 + worldStructure.y * (0.40 + doseDetail * 0.025));
    chamberRadius += sin(morphClock * 1.30 + signaturePhase) * motionLanes.x * 0.31;
    allowEmptyShortcut = true;

    vec3 origin = realmEyeOrigin();
    vec3 ray = realmRay(uv);
    blend = cinematicProgress(blend);
    if (blend <= 0.0) { fragColor = texture(DiffuseSampler, uv); return; }
    float morph = blend * (0.72 + 0.28 * blend);
    vec3 sourcePoint = origin + ray * FAR_DISTANCE;
    vec3 sourceNormal = -ray;
    float sourceSurface = 0.0;
    float sourcePhysicalDistance = FAR_DISTANCE / max(RealmOrigin.w, 0.001);
    vec3 hiddenSceneColor = texture(DiffuseSampler, uv).rgb;
    float rawDepth = texture(DepthSampler, uv).r;
    {
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
    int stepLimit = quality == 0 ? 72 : (quality == 1 ? 112 : 192);
    float pixelTolerance = quality == 0 ? 0.0011 : (quality == 1 ? 0.00070 : 0.00042);
    float distance = 0.015, hitTolerance = 0.002;
    vec2 hitField = vec2(0.0);
    bool hit = false;
    bool recoveredSurface = false;
    int recoverySteps = 0;
    for (int i = 0; i < MAX_STEPS; i++) {
        if (i >= stepLimit || distance >= FAR_DISTANCE) break;
        hitField = realmField(origin + ray * distance);
        hitTolerance = 0.0018 + distance * pixelTolerance;
        // At large distances a conservative empty-space stepping bound can
        // fall below the pixel footprint. Verify the real solid before calling
        // it a hit, then use the complete field for normals and lighting.
        if (hitField.x < hitTolerance && hitField.y < 0.0) {
            allowEmptyShortcut = false;
            hitField = realmField(origin + ray * distance);
            allowEmptyShortcut = true;
        }
        if (hitField.x < hitTolerance) { hit = true; break; }
        distance += clamp(hitField.x * 0.92, 0.0012, 1.65);
    }
    allowEmptyShortcut = false;
    if (!hit && distance < FAR_DISTANCE) {
        // Near recursive surfaces a conservative DE can exhaust the fine pass.
        // Walk FORWARD through the complete field with a small bounded spacing.
        // Each sign bracket is local to adjacent samples: bisection over a whole
        // room could silently choose its back wall instead of a foreground solid.
        // Only a proven carved-void bound permits a larger empty-space advance.
        vec2 startField = realmField(origin + ray * distance);
        hitTolerance = 0.0018 + distance * pixelTolerance;
        if (startField.x < hitTolerance) {
            hitField = startField;
            hit = true;
            recoveredSurface = true;
        } else {
            float lowerDistance = distance;
            float searchDistance = distance;
            float upperDistance = distance;
            bool bracketed = false;
            int recoveryLimit = quality == 0 ? 160 : (quality == 1 ? 192 : 224);
            float recoverySpacing = quality == 0 ? 0.025 : (quality == 1 ? 0.018 : 0.012);
            vec2 searchField = startField;
            for (int recoveryStep = 0; recoveryStep < 224; recoveryStep++) {
                if (recoveryStep >= recoveryLimit) break;
                float advance = max(recoverySpacing, max(searchField.x * 0.92, carvedVoidBound * 0.92));
                lowerDistance = searchDistance;
                searchDistance += min(advance, 1.65);
                if (searchDistance >= FAR_DISTANCE) break;
                searchField = realmField(origin + ray * searchDistance);
                recoverySteps++;
                hitTolerance = 0.0018 + searchDistance * pixelTolerance;
                if (searchField.x <= 0.0) {
                    upperDistance = searchDistance;
                    bracketed = true;
                    break;
                }
                if (searchField.x < hitTolerance) {
                    distance = searchDistance;
                    hitField = searchField;
                    hit = true;
                    recoveredSurface = true;
                    break;
                }
            }
            if (bracketed) {
                int refinements = quality == 0 ? 8 : (quality == 1 ? 10 : 12);
                for (int refinement = 0; refinement < 12; refinement++) {
                    if (refinement >= refinements) break;
                    float middle = (lowerDistance + upperDistance) * 0.5;
                    if (realmField(origin + ray * middle).x > 0.0) lowerDistance = middle;
                    else upperDistance = middle;
                }
                distance = upperDistance;
                hitField = realmField(origin + ray * distance);
                if (hitField.x <= 0.0) {
                    hit = true;
                    recoveredSurface = true;
                    hitTolerance = 0.0018 + distance * pixelTolerance;
                }
            }
        }
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
                + iridescence * (0.07 + materialLanes.z * 0.20)
                + sin(morphClock * 0.31 + dot(point, vec3(0.23, 0.17, 0.19))) * 0.045 + chromaticJourney;
        float ecologicalHue = hueTravel(colorHue(WorldInfluenceC.rgb + vec3(0.001)),
                colorHue(hiddenSceneColor + vec3(0.001)), 0.28 + sourceSurface * 0.12);
        float hiddenRelief = sourceSurface * (0.25 + 0.75 * abs(dot(sourceNormal, targetNormal)))
                / (1.0 + sourcePhysicalDistance * 0.02);
        hue = hueTravel(hue, ecologicalHue, 0.10 + WorldInfluenceA.x * 0.07 + WorldInfluenceA.w * 0.04);
        hue += sin(log2(1.0 + sourcePhysicalDistance) * 1.1 + signaturePhase) * sourceSurface * 0.025;
        material = hueColor(hue);
        float accentHue = hueTravel(hue + 0.29 + FractalC.z * 0.22, colorHue(PaletteC.rgb) + chromaticJourney * 0.80, 0.30);
        lineColor = hueColor(accentHue);
        vec3 keyLight = normalize(vec3(0.64, 0.71, -0.43));
        vec3 fillLight = normalize(vec3(-0.53, 0.28, 0.79));
        float diffuse = max(0.0, dot(normal, keyLight));
        float fill = max(0.0, dot(normal, fillLight));
        float ambient = 0.10 + lightingLanes.x * 0.27 + lightingLanes.y * 0.12 + lightingLanes.w * 0.10;
        float keyStrength = 0.10 + lightingLanes.x * 0.55 + lightingLanes.y * 0.40 + lightingLanes.w * 0.21;
        float illumination = ambient + diffuse * keyStrength + fill * (0.12 + lightingLanes.x * 0.20)
                + viewFacing * 0.14 + WorldInfluenceB.x * 0.10;
        illumination *= 0.94 + luma(hiddenSceneColor) * 0.08 + hiddenRelief * 0.05;
        float engraving = 0.76 + ornament.x * 0.24 - ornament.z * 0.15;
        color = material * illumination * engraving * mix(0.62, 1.0, occlusion);
        // Thin emissive ornament stays chromatic instead of averaging
        // complementary RGB colours into brown/grey plastic surfaces.
        float lineEnergy = (ornament.y * (0.55 + detailEnergy * 0.65) + ornament.z * (0.12 + detailEnergy * 0.16))
                * (0.76 + materialLanes.w * 0.56 + WorldInfluenceB.y * 0.15)
                * (1.0 + doseDetail * 0.08) * (0.94 + hiddenRelief * 0.12);
        lineEnergy *= 0.60 + lightingLanes.y * 0.80 + lightingLanes.w * 1.20 + lightingLanes.z * 0.45;
        color *= 1.0 - ornament.y * 0.40;
        color += lineColor * lineEnergy * mix(1.0, 0.36, reduced);
        color += hueColor(hue + 0.10) * iridescence * (0.14 + detailEnergy * 0.18);
        float specular = pow(max(0.0, dot(reflect(ray, normal), keyLight)), 34.0) * 0.18;
        color += hueColor(hue + 0.08) * specular * mix(1.0, 0.35, reduced);
        vec3 engravedShell = color;
        float fresnel = pow(1.0 - viewFacing, 4.0);
        vec3 reflection = realmBackground(reflect(ray, normal));
        vec3 refraction = realmBackground(refract(ray, normal, 0.69));
        vec3 glass = reflection * (0.65 + fresnel * 3.1) + refraction * (1.0 - fresnel) * 0.85
                + material * 0.07 + lineColor * ornament.y * 0.22;
        float thinFilm = sin(viewFacing * 10.0 + ornament.x * 4.0 + morphClock * 0.21);
        vec3 membrane = hueColor(hue + thinFilm * 0.18) * (0.18 + viewFacing * 0.43 + fill * 0.17)
                + reflection * 0.35 + lineColor * (fresnel * 0.42 + ornament.z * 0.20);
        float subsurface = pow(max(0.0, dot(-normal, keyLight)), 2.0);
        vec3 tissue = material * (0.30 + diffuse * 0.40 + fill * 0.24) * mix(0.55, 1.0, occlusion)
                + hueColor(hue - 0.08) * subsurface * 0.26 + lineColor * ornament.y * 0.18;
        float metalRipple = sin(dot(point, vec3(1.7, 2.1, 1.3)) + morphClock * 0.37);
        vec3 metal = reflection * (2.2 + fresnel * 2.0) * (0.82 + metalRipple * 0.18)
                + material * illumination * 0.12 + hueColor(hue + 0.06) * specular * 3.4;
        vec3 glyph = material * 0.045 + lineColor * (lineEnergy * 1.35 + ornament.z * 0.22);
        float facetHue = dot(normal, normalize(vec3(1.0, 2.0, 3.0))) * 0.25;
        vec3 kaleidoShell = engravedShell * 0.42 + hueColor(hue + facetHue) * illumination * 0.60
                + lineColor * ornament.y * 0.30;
        color = glass * surfaceMaterialsA.x + membrane * surfaceMaterialsA.y
                + tissue * surfaceMaterialsA.z + metal * surfaceMaterialsA.w
                + glyph * surfaceMaterialsB.x + kaleidoShell * surfaceMaterialsB.y;
        float shimmer = pow(0.5 + 0.5 * sin(dot(point, vec3(0.71, 0.93, 0.57)) + morphClock * 0.31 + viewFacing * 4.0), 6.0);
        color += hueColor(hue + 0.14) * shimmer * atmosphereLanes.y * 0.12 * mix(1.0, 0.25, reduced);
        float entityGlow = worldStructure.y / (1.0 + dot(point - WorldAttractor.xyz, point - WorldAttractor.xyz));
        color += hueColor(ecologicalHue + 0.18) * entityGlow * 0.19;
        float fogDensity = 0.007 + atmosphereLanes.x * 0.035 + WorldInfluenceA.z * 0.008;
        float fog = 1.0 - exp(-length(point - origin) * fogDensity);
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
        float growthPatch = (ornament.x - 0.5) * 1.20
                + sin(dot(materialPoint, vec3(0.47, 0.31, 0.39)) + signaturePhase) * 0.35;
        float coverage = cinematicCoverage(blend, growthPatch);
        // Source textures acquire anchored engraving while their reconstructed
        // depth/normals move toward the new solid, then recede into its material.
        vec3 engraved = source.rgb * (vec3(0.45) + material * 0.95) + lineColor * ornament.y * 0.22;
        oldMaterial = mix(oldMaterial, engraved, smoothstep(0.04, 0.72, morph) * (1.0 - coverage) * 0.70);
        fragColor = vec4(clamp(mix(oldMaterial, color, coverage), 0.0, 1.0), mix(source.a, 1.0, coverage));
    }
}
