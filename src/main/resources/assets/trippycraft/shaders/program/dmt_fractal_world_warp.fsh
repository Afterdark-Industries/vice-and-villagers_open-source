#version 150

// A separate geometric realm: source colour is used only for the entrance/exit
// blend. Desktop and VR run this same field with their actual per-eye rays.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform mat4 WorldInverseViewProjection;
uniform vec4 WorldEyePosition;
uniform vec4 WorldHeadPosition;
uniform vec4 FractalA; // blend, integrated motion seconds, quality 0/1/2, photosensitivity reduction
uniform vec4 FractalB; // signature seed A/B, dimensional depth, temporal flow
uniform vec4 RealmOrigin; // head position relative to trip anchor in realm units, physical-to-realm scale
uniform vec4 PaletteA;
uniform vec4 PaletteB;
uniform vec4 PaletteC;

out vec4 fragColor;

const float CELL_SIZE = 12.8;
const float FAR_DISTANCE = 70.0;
const int MAX_STEPS = 104;

// Set once per fragment, outside the distance-estimator loops. Neither the
// field nor its palette depends on the camera orientation or Minecraft depth.
mat3 fieldRotation;
float boxScale;
float minimumRadiusSquared;
float chamberRadius;
float signaturePhase;

float nonzero(float value) {
    return abs(value) < 0.000001 ? (value < 0.0 ? -0.000001 : 0.000001) : value;
}

mat3 rotateX(float angle) {
    float s = sin(angle), c = cos(angle);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotateY(float angle) {
    float s = sin(angle), c = cos(angle);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

mat3 rotateZ(float angle) {
    float s = sin(angle), c = cos(angle);
    return mat3(c, s, 0.0, -s, c, 0.0, 0.0, 0.0, 1.0);
}

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

// x: conservative Mandelbox/CSG distance; y: orbit-trap material coordinate.
// The 7 bounded box/sphere folds produce real surfaces, silhouettes and depth.
// Repeated chambers and connecting X/Z tunnels are empty space carved through
// the solid field, so the trip starts in a room instead of inside the fractal.
vec2 realmField(vec3 position) {
    vec3 cell = floor((position + CELL_SIZE * 0.5) / CELL_SIZE);
    vec3 local = position - cell * CELL_SIZE;
    float cellSeed = hash31(cell + vec3(FractalB.x * 23.0, FractalB.y * 31.0, 7.0));
    vec3 q = fieldRotation * local;
    // A reflection changes each room without expensive per-step rotations.
    q.xz *= mix(vec2(1.0), vec2(-1.0, -1.0), step(0.5, cellSeed));
    float worldToFractal = 0.53 + clamp(FractalB.z, 0.0, 1.0) * 0.035;
    q *= worldToFractal;
    vec3 z = q;
    float derivative = 1.0;
    float orbit = 100.0;
    for (int iteration = 0; iteration < 7; iteration++) {
        z = clamp(z, vec3(-1.0), vec3(1.0)) * 2.0 - z;
        float radiusSquared = max(dot(z, z), 0.000001);
        float sphereFold = clamp(1.0 / radiusSquared, 1.0, 1.0 / minimumRadiusSquared);
        z *= sphereFold;
        derivative *= sphereFold;
        z = z * boxScale + q;
        derivative = derivative * abs(boxScale) + 1.0;
        orbit = min(orbit, dot(abs(z), vec3(0.28, 0.43, 0.29)));
    }
    float fractal = (length(z) / max(abs(derivative), 0.000001) - 0.0030) / worldToFractal;
    // An outer solid shell closes gaps between repeated fractal cells. The
    // tunnels remain continuous across cell boundaries in both directions.
    float enclosingSolid = 5.65 - length(local);
    float solid = min(fractal, enclosingSolid);
    float chamber = length(local) - chamberRadius;
    float tunnelX = length(local.yz) - 1.24;
    float tunnelZ = length(local.xy) - 1.24;
    float emptySpace = min(chamber, min(tunnelX, tunnelZ));
    return vec2(max(solid, -emptySpace), clamp(orbit * 0.52 + cellSeed * 0.13, 0.0, 1.0));
}

vec3 realmNormal(vec3 point, float epsilon) {
    // Tetrahedral normal: four field evaluations, independent of the eye.
    vec2 e = vec2(1.0, -1.0) * epsilon;
    vec3 gradient = e.xyy * realmField(point + e.xyy).x
            + e.yyx * realmField(point + e.yyx).x
            + e.yxy * realmField(point + e.yxy).x
            + e.xxx * realmField(point + e.xxx).x;
    return dot(gradient, gradient) > 0.0000000001 ? normalize(gradient) : vec3(0.0, 1.0, 0.0);
}

float realmOcclusion(vec3 point, vec3 normal) {
    float occlusion = 0.0;
    float weight = 0.56;
    for (int sampleIndex = 0; sampleIndex < 4; sampleIndex++) {
        float distance = 0.075 + float(sampleIndex) * 0.18;
        float field = realmField(point + normal * distance).x;
        occlusion += max(0.0, distance - field) / distance * weight;
        weight *= 0.55;
    }
    return clamp(1.0 - occlusion * 0.68, 0.16, 1.0);
}

vec3 realmBackground(vec3 ray, float seconds) {
    float horizon = 0.5 + ray.y * 0.5;
    vec3 color = mix(PaletteA.rgb * 0.035, PaletteB.rgb * 0.13, horizon);
    float ribbon = 0.5 + 0.5 * sin(ray.y * 7.0 + ray.x * 3.0 - ray.z * 4.0 + signaturePhase + seconds * 0.025);
    color += PaletteC.rgb * pow(ribbon, 5.0) * 0.065;
    return color + vec3(0.012, 0.016, 0.028);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    float blend = clamp(FractalA.x, 0.0, 1.0);
    if (blend <= 0.0) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    float reduced = clamp(FractalA.w, 0.0, 1.0);
    float seconds = FractalA.y * mix(1.0, 0.55, reduced);
    float seedA = clamp(FractalB.x, 0.0, 1.0);
    float seedB = clamp(FractalB.y, 0.0, 1.0);
    signaturePhase = seedA * 6.2831853 + seedB * 3.1;
    fieldRotation = rotateY(seedA * 2.7 + 0.32) * rotateZ(seedB * 1.7 + 0.21) * rotateX(seedA * 0.8 + seedB * 0.6);
    float motion = seconds * (0.055 + clamp(FractalB.w, 0.0, 1.0) * 0.025);
    boxScale = -1.88 - seedA * 0.20 + sin(motion + seedB * 6.0) * 0.018;
    minimumRadiusSquared = 0.24 + seedB * 0.045 + sin(motion * 0.71 + seedA * 5.0) * 0.008;
    chamberRadius = 2.88 + clamp(FractalB.z, 0.0, 1.0) * 0.18 + sin(motion * 0.67 + signaturePhase) * 0.07;

    vec3 origin = realmEyeOrigin();
    vec3 ray = realmRay(uv);
    int quality = int(clamp(floor(FractalA.z + 0.5), 0.0, 2.0));
    int stepLimit = quality == 0 ? 48 : (quality == 1 ? 72 : 104);
    float pixelTolerance = quality == 0 ? 0.0015 : (quality == 1 ? 0.0010 : 0.00065);
    float distance = 0.015;
    float hitTolerance = 0.003;
    vec2 hitField = vec2(0.0);
    bool hit = false;
    for (int stepIndex = 0; stepIndex < MAX_STEPS; stepIndex++) {
        if (stepIndex >= stepLimit || distance >= FAR_DISTANCE) break;
        vec3 point = origin + ray * distance;
        hitField = realmField(point);
        hitTolerance = 0.0025 + distance * pixelTolerance;
        if (hitField.x < hitTolerance) {
            hit = true;
            break;
        }
        distance += clamp(hitField.x * 0.64, 0.0015, 2.2);
    }

    vec3 background = realmBackground(ray, seconds);
    vec3 color = background;
    if (hit) {
        vec3 point = origin + ray * distance;
        vec3 normal = realmNormal(point, max(0.003, hitTolerance * 0.70));
        float occlusion = realmOcclusion(point + normal * hitTolerance, normal);
        vec3 keyLight = normalize(vec3(0.62, 0.76, -0.41));
        float diffuse = max(0.0, dot(normal, keyLight));
        float eyeLight = max(0.0, dot(normal, -ray));
        float orbitBand = 0.5 + 0.5 * cos(hitField.y * 11.0 + signaturePhase);
        float mineralBand = 0.5 + 0.5 * cos(hitField.y * 26.0 + dot(point, vec3(0.17, 0.11, 0.13)));
        vec3 material = mix(PaletteA.rgb, PaletteB.rgb, orbitBand);
        material = mix(material, PaletteC.rgb, mineralBand * 0.34);
        material = mix(material, vec3(dot(material, vec3(0.2126, 0.7152, 0.0722))), reduced * 0.38);
        color = material * (0.18 + diffuse * 0.58 + eyeLight * 0.30) * occlusion;
        color += mix(PaletteB.rgb, PaletteC.rgb, orbitBand) * (0.028 + mineralBand * 0.045) * (0.38 + occlusion * 0.62);
        vec3 reflected = reflect(ray, normal);
        float specular = pow(max(0.0, dot(reflected, keyLight)), 20.0) * 0.14 * occlusion;
        color += mix(vec3(0.60), PaletteC.rgb, 0.50) * specular * (1.0 - reduced * 0.60);
        float fog = 1.0 - exp(-distance * 0.033);
        color = mix(color, background + PaletteB.rgb * 0.045, fog);
    }

    // Bounded display mapping, with no strobe, exposure pumping or grain.
    color = color / (vec3(0.72) + color);
    color = pow(clamp(color, 0.0, 1.0), vec3(0.86));
    color = mix(color, vec3(dot(color, vec3(0.2126, 0.7152, 0.0722))), reduced * 0.18);
    color = mix(color, color * 0.68 + vec3(0.11), reduced * 0.65);
    if (blend >= 1.0) {
        // Realm opacity includes alpha: no source sampler dependence at all.
        fragColor = vec4(color, 1.0);
    } else {
        vec4 source = texture(DiffuseSampler, uv);
        fragColor = mix(source, vec4(color, 1.0), blend);
    }
}
