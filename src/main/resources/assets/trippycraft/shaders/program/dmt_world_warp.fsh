#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec2 OutSize;
uniform vec2 InSize;

uniform vec4 WarpA;    // intensity, 3d fold presence, animated fold presence, time
uniform vec4 WarpB;    // coupling, alternate-world readiness, dimensional depth, temporal flow
uniform vec4 WarpC;    // morph rate, scaffold phase, visual tier, trip progress
uniform vec4 PaletteA; // primary rgb, symmetry
uniform vec4 PaletteB; // secondary rgb, tunnel speed
uniform vec4 PaletteC; // accent rgb, glyph density
uniform vec4 Profile;  // seedA, seedB, style, mandala density
uniform vec4 Dose;     // active units, target units, breakthrough gate, accessibility
uniform vec4 LayersC;  // scaffold seeds, displayed dose, remaining seconds
uniform vec4 CameraA;  // wrapped camera position xyz, far plane
uniform vec4 CameraB;  // near-plane forward vector xyz, near plane
uniform vec4 CameraC;  // near-plane right vector xyz
uniform vec4 CameraD;  // near-plane up vector xyz
uniform float VrEnabled;
uniform mat4 VrInverseViewProjection;
uniform vec4 VrEyePosition;
uniform vec4 VrHeadPosition;

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec2 rotate2(vec2 p, float a) {
    float s = sin(a);
    float c = cos(a);
    return vec2(p.x * c - p.y * s, p.x * s + p.y * c);
}

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float valueNoise(vec2 uv, float seed) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i + seed);
    float b = hash21(i + vec2(1.0, 0.0) + seed);
    float c = hash21(i + vec2(0.0, 1.0) + seed);
    float d = hash21(i + vec2(1.0, 1.0) + seed);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 uv, float seed) {
    float value = 0.0;
    float amp = 0.52;
    vec2 q = uv;
    for (int i = 0; i < 4; i++) {
        value += valueNoise(q, seed + float(i) * 13.37) * amp;
        q = rotate2(q * 2.02 + vec2(1.7, -2.1), 0.37 + seed * 0.011);
        amp *= 0.50;
    }
    return saturate(value);
}

vec3 hueShift(vec3 color, float amount) {
    float a = amount * 6.28318;
    vec3 k = vec3(0.57735);
    return color * cos(a) + cross(k, color) * sin(a) + k * dot(k, color) * (1.0 - cos(a));
}

vec2 fromAspectUv(vec2 uv, float aspect) {
    vec2 p = uv - 0.5;
    p.x *= aspect;
    return p;
}

vec2 toAspectUv(vec2 p, float aspect) {
    return vec2(p.x / max(aspect, 0.001), p.y) + 0.5;
}

vec3 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

float sampleDepth(vec2 uv) {
    return texture(DepthSampler, clamp(uv, vec2(0.001), vec2(0.999))).r;
}

vec2 foldSpace(vec2 p, float angle, float fold, float depth, float time, float seed) {
    vec2 axis = vec2(cos(angle), sin(angle));
    vec2 side = vec2(-axis.y, axis.x);
    float x = dot(p, axis);
    float y = dot(p, side);
    float hinge = 0.18 + depth * 0.26 + 0.05 * sin(time * 0.21 + seed);
    float folded = sign(x) * (abs(x) - hinge * fold);
    float crease = smoothstep(0.0, hinge * 1.8 + 0.08, abs(x));
    x = mix(x, folded, fold * crease);
    y += sin((abs(x) + depth * 0.7) * (2.2 + fold * 1.8) + time * 0.34 + seed) * fold * (0.018 + depth * 0.018);
    return axis * x + side * y;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    float aspect = OutSize.x / max(OutSize.y, 1.0);

    float intensity = WarpA.x;
    float fold3d = saturate(WarpA.y);
    float animatedFold = saturate(WarpA.z);
    float time = WarpA.w * 0.050;
    float coupling = saturate(WarpB.x);
    float alternateWorld = saturate(WarpB.y);
    float dimensionalDepth = saturate(WarpB.z);
    float temporalFlow = saturate(WarpB.w);
    float morphRate = WarpC.x;
    float scaffoldPhase = WarpC.y;
    float tier = WarpC.z;
    float progress = saturate(WarpC.w);
    float seedA = Profile.x * 83.0 + LayersC.x * 41.0 + 1.0;
    float seedB = Profile.y * 109.0 + LayersC.y * 67.0 + 3.0;
    float doseUnits = Dose.x;
    float targetDoseUnits = Dose.y;
    float displayedDoseUnits = LayersC.z;
    float breakthroughGate = saturate(Dose.z);
    float accessibility = saturate(Dose.w);

    vec3 base = texture(DiffuseSampler, uv).rgb;
    float alpha = texture(DiffuseSampler, uv).a;

    float depth = sampleDepth(uv);
    float depthL = sampleDepth(uv - vec2(texel.x * 2.0, 0.0));
    float depthR = sampleDepth(uv + vec2(texel.x * 2.0, 0.0));
    float depthD = sampleDepth(uv - vec2(0.0, texel.y * 2.0));
    float depthU = sampleDepth(uv + vec2(0.0, texel.y * 2.0));
    vec2 depthSlope = vec2(depthL - depthR, depthD - depthU);
    float depthScene = VrEnabled > 0.5 ? 1.0 - step(0.999995, depth)
            : 1.0 - smoothstep(0.992, 1.0, depth);
    float depthEdge = smoothstep(0.0010, 0.030, abs(depthL - depthR) + abs(depthD - depthU));

    float edgeX = abs(luma(sampleScene(uv + vec2(texel.x * 1.5, 0.0))) - luma(sampleScene(uv - vec2(texel.x * 1.5, 0.0))));
    float edgeY = abs(luma(sampleScene(uv + vec2(0.0, texel.y * 1.5))) - luma(sampleScene(uv - vec2(0.0, texel.y * 1.5))));
    float imageEdge = smoothstep(0.030, 0.180, edgeX + edgeY);
    float worldEdge = saturate(depthEdge * 0.70 + imageEdge * 0.42);

    vec2 p = fromAspectUv(uv, aspect);
    float radius = length(p);
    vec2 radial = p / max(radius, 0.001);
    vec2 tangent = vec2(-radial.y, radial.x);

    vec2 ndc = uv * 2.0 - 1.0;
    vec3 ray = normalize(CameraB.xyz + CameraC.xyz * ndc.x + CameraD.xyz * ndc.y);
    vec3 forward = normalize(CameraB.xyz);
    float nearPlane = max(0.001, CameraB.w);
    float farPlane = max(nearPlane + 1.0, CameraA.w);
    float linearDepth = (nearPlane * farPlane) / max(nearPlane, farPlane - depth * (farPlane - nearPlane));
    float surfaceDistance = clamp(linearDepth / max(0.18, dot(ray, forward)), 1.0, farPlane);
    if (VrEnabled > 0.5) {
        vec4 eyeRay = VrInverseViewProjection * vec4(ndc, 0.0, 1.0);
        ray = normalize(eyeRay.xyz / max(eyeRay.w, 0.000001));
        vec4 eyePoint = VrInverseViewProjection * vec4(ndc, depth * 2.0 - 1.0, 1.0);
        vec3 worldPoint = VrEyePosition.xyz + eyePoint.xyz / max(eyePoint.w, 0.000001);
        // The same real point gets the same volumetric phase in both eyes.
        surfaceDistance = depthScene > 0.5 ? length(worldPoint - VrHeadPosition.xyz) : farPlane;
    }
    float distanceCue = saturate(log(surfaceDistance + 1.0) / log(farPlane + 1.0));

    float rise = smoothstep(0.0, 0.10, progress);
    float tunnel = smoothstep(0.045, 0.25, progress) * (1.0 - smoothstep(0.72, 0.94, progress));
    float breakthrough = smoothstep(0.16, 0.38, progress) * (1.0 - smoothstep(0.80, 0.97, progress));
    float returning = smoothstep(0.66, 0.88, progress) * (1.0 - smoothstep(0.97, 1.0, progress));
    float tail = 1.0 - smoothstep(0.94, 1.0, progress);
    float doseGate = smoothstep(0.12, 1.35, doseUnits);
    float deepDose = smoothstep(0.85, 2.65, doseUnits);
    float highDose = smoothstep(1.80, 6.20, doseUnits);
    float doseOverdrive = saturate((doseUnits - 5.40) / 9.60);
    float impossibleDose = smoothstep(5.40, 7.20, doseUnits);
    float doseForce = 1.0 + doseOverdrive * 0.58 + impossibleDose * 0.48;
    float remainingSeconds = max(0.0, LayersC.w);
    float sessionClockAvailable = step(0.001, remainingSeconds) * (1.0 - step(0.995, progress));
    float tripDurationSeconds = remainingSeconds / max(1.0 - progress, 0.020);
    float tripClock = mix(time, tripDurationSeconds * progress, sessionClockAvailable);
    float landingPhase = smoothstep(0.44, 0.56, progress) * (1.0 - smoothstep(0.82, 0.96, progress));
    float landingBreath = sin(tripClock * (0.034 + temporalFlow * 0.014) + seedB * 1.7) * 0.5 + 0.5;
    float landingSince = max(0.0, tripClock - tripDurationSeconds * 0.44);
    float motionTime = mix(time, tripClock - landingSince * landingPhase * (0.86 - landingBreath * 0.050), sessionClockAvailable);
    float driftTime = mix(time, tripClock - landingSince * landingPhase * (0.74 - landingBreath * 0.070), sessionClockAvailable);
    float slowTime = mix(time, tripClock - landingSince * landingPhase * (0.90 - landingBreath * 0.035), sessionClockAvailable);
    float landingPulse = landingPhase * (0.50 + landingBreath * 0.50);
    float landingTraceTempo = mix(1.0, 0.28 + landingBreath * 0.08, landingPhase);
    float settledScaffoldPhase = scaffoldPhase + (landingBreath - 0.5) * landingPhase * 0.10;

    float scaffold = saturate(fold3d * 0.48 + animatedFold * 0.42 + coupling * 0.50 + alternateWorld * 0.60 + dimensionalDepth * 0.34);
    float foldPresence = saturate(scaffold * rise * tail * (0.56 + doseGate * 0.34 + deepDose * 0.22 + impossibleDose * 0.26 + intensity * 0.18));
    float foldPower = foldPresence * doseForce;
    float realmPresence = saturate(foldPresence * (0.24 + breakthrough * 0.62 + alternateWorld * 0.36 + highDose * 0.16 + impossibleDose * 0.28));
    float returnPull = returning * foldPresence * (0.44 + alternateWorld * 0.30 + dimensionalDepth * 0.20);

    float travelSign = mix(1.0, -0.86, returning);
    float travel = (motionTime * (0.46 + temporalFlow * 1.35 + targetDoseUnits * 0.030 + impossibleDose * 0.62) + settledScaffoldPhase * 5.0) * travelSign;
    float flowNoise = fbm(rotate2(p, driftTime * (0.055 + temporalFlow * 0.040) + seedA * 0.003) * (0.92 + dimensionalDepth * 0.38)
            + vec2(driftTime * 0.070, -driftTime * 0.052) + seedA,
            seedB);
    float breathRate = mix(0.16 + temporalFlow * 0.20, 0.032 + temporalFlow * 0.030, landingPhase);
    float breath = sin(slowTime * breathRate + seedB) * 0.5 + 0.5;
    float rotorDrive = saturate((tunnel * 0.42 + breakthrough * 0.88 + returning * 0.28)
            * foldPresence
            * (0.70 + deepDose * 0.42 + impossibleDose * 0.92));
    float splitDrive = saturate((tunnel * 0.34 + breakthrough * 1.04 + returning * 0.84)
            * foldPresence
            * (0.76 + deepDose * 0.46 + highDose * 0.18 + impossibleDose * 1.08));
    float rotorSpeed = (1.30 + temporalFlow * 2.10 + rotorDrive * 3.75 + splitDrive * 2.30 + impossibleDose * 2.40 + doseOverdrive * 1.15) * travelSign;
    float rotorAngle = settledScaffoldPhase * 6.28318 + seedA * 0.037 + motionTime * rotorSpeed
            + sin(slowTime * (0.92 + temporalFlow * 0.34) + seedB) * splitDrive * mix(0.46, 0.12 + landingBreath * 0.04, landingPhase);
    float swirlDrive = saturate(foldPresence * (0.24 + tunnel * 0.58 + breakthrough * 0.84 + returning * 0.62 + highDose * 0.20 + impossibleDose * 0.60));
    float swirlOuter = 1.48 + dimensionalDepth * 0.52 + highDose * 0.22 + impossibleDose * 0.40;
    float swirlFalloff = 1.0 - smoothstep(0.08, swirlOuter, radius);
    float trailStrength = saturate(splitDrive * (0.58 + impossibleDose * 0.74 + highDose * 0.22) * mix(1.0, 0.36, accessibility));
    vec2 fanP = rotate2(p, -rotorAngle * (0.22 + splitDrive * 0.34));
    float polarAngle = atan(p.y, p.x);
    float spiralPhase = polarAngle * (2.4 + dimensionalDepth * 1.1)
            + radius * (12.0 + dimensionalDepth * 8.0 + impossibleDose * 5.0)
            - rotorAngle * (1.35 + temporalFlow * 0.54);
    float spiralBand = pow(0.5 + 0.5 * sin(spiralPhase), 3.2);
    float spiralNoise = fbm(rotate2(p, rotorAngle * 0.16) * (4.8 + dimensionalDepth * 2.2)
            + vec2(driftTime * 0.18, -driftTime * 0.14) + seedB,
            seedA);
    float fanCore = saturate((spiralBand * 0.72 + spiralNoise * 0.34) * splitDrive * swirlFalloff);
    float fanTrails = 0.0;
    vec3 fanTrailColor = vec3(0.0);
    float fanTrailWeight = 0.0;
    for (int fanTrail = 1; fanTrail <= 8; fanTrail++) {
        float lag = float(fanTrail);
        float lagAngle = rotorSpeed * lag * (0.045 + splitDrive * 0.018 + impossibleDose * 0.010) * landingTraceTempo;
        vec2 lagP = rotate2(p, -lagAngle);
        lagP += vec2(-lagP.y, lagP.x) * sin(radius * (7.0 + lag * 0.55) - driftTime * (2.4 + temporalFlow) - lag) * splitDrive * (0.006 + lag * 0.0018);
        lagP *= 1.0 - splitDrive * lag * (0.006 + impossibleDose * 0.002);
        vec2 lagUv = clamp(toAspectUv(lagP, aspect), vec2(0.001), vec2(0.999));
        float lagPhase = atan(lagP.y, lagP.x) * (2.1 + dimensionalDepth)
                + length(lagP) * (10.0 + lag * 0.62 + impossibleDose * 2.2)
                - rotorAngle * (0.82 + lag * 0.030);
        float lagRibbon = pow(0.5 + 0.5 * sin(lagPhase), 2.2);
        float lagWeight = trailStrength * swirlFalloff * (0.44 / lag) * (0.38 + lagRibbon * 0.62);
        fanTrails += lagWeight;
        fanTrailColor += sampleScene(lagUv) * lagWeight;
        fanTrailWeight += lagWeight;
    }
    fanTrailColor = fanTrailWeight > 0.001 ? fanTrailColor / fanTrailWeight : base;
    float fanMask = saturate((fanCore * 0.42 + fanTrails * 0.82) * (0.78 + impossibleDose * 0.42));
    vec2 effectP = rotate2(p, -rotorAngle * splitDrive * (0.050 + breakthrough * 0.030 + returning * 0.035));

    vec2 inwardWarp = -radial * tunnel * foldPower * (0.012 + deepDose * 0.018 + breakthroughGate * 0.013 + impossibleDose * 0.014);
    vec2 surfaceWarp = normalize(depthSlope * 32.0 + vec2(0.001)) * worldEdge * foldPower * (0.007 + deepDose * 0.010 + impossibleDose * 0.006);
    vec2 rotorWarp = tangent * rotorDrive * (0.018 + deepDose * 0.014 + impossibleDose * 0.020) * (0.55 + breath * 0.45)
            + normalize(vec2(fanP.y, -fanP.x) + vec2(0.001)) * fanMask * (0.018 + impossibleDose * 0.020);
    float swirlWave = sin(radius * (10.0 + dimensionalDepth * 8.0 + impossibleDose * 4.0)
            - motionTime * (5.4 + temporalFlow * 4.2 + impossibleDose * 2.5)
            + seedA * 2.1) * 0.5 + 0.5;
    vec2 swirlWarp = (tangent * (0.058 + deepDose * 0.044 + highDose * 0.024 + impossibleDose * 0.064) * (0.72 + swirlWave * 0.74)
            + radial * sin(slowTime * (3.2 + temporalFlow * 2.2) + radius * 14.0 + seedB) * (0.014 + impossibleDose * 0.024))
            * swirlDrive * swirlFalloff;
    vec2 landingWarp = radial * sin(slowTime * (0.30 + temporalFlow * 0.08) + radius * (4.4 + dimensionalDepth * 2.0) + seedA) * landingPulse * foldPower * (0.005 + deepDose * 0.005);
    vec2 currentWarp = (inwardWarp + surfaceWarp + rotorWarp + swirlWarp + landingWarp + tangent * (flowNoise - 0.5) * foldPower * 0.020) * mix(1.0, 0.42, accessibility);
    vec2 warpedUv = clamp(uv + currentWarp, vec2(0.001), vec2(0.999));
    vec3 warpedBase = sampleScene(warpedUv);

    vec3 foldColor = vec3(0.0);
    float foldWeight = 0.0;
    vec2 foldFlow = vec2(0.0);
    float crease = 0.0;

    for (int i = 0; i < 5; i++) {
        float fi = float(i);
        float layer = (fi + 1.0) / 5.0;
        float layerDepth = mix(distanceCue, layer, 0.68);
        float layerPresence = 0.0;
        float quadrant = mod(fi, 2.0) * 1.5707963;
        float angle = rotorAngle + quadrant + layer * 0.108 + sin(slowTime * 0.045 + layer * 4.3 + seedB) * rotorDrive * mix(0.18, 0.09, landingPhase);
        float foldAmount = saturate(layerPresence * (0.32 + dimensionalDepth * 0.38 + breakthrough * 0.34 + highDose * 0.18 + impossibleDose * 0.34));
        vec2 folded = foldSpace(effectP * (1.0 + layer * 0.18 + dimensionalDepth * 0.28 + impossibleDose * 0.12), angle, foldAmount, layerDepth, motionTime, seedB + layer);
        folded = rotate2(folded, sin(slowTime * (0.16 + temporalFlow * 0.075) + layer * 2.0 + seedA) * rotorDrive * mix(0.18, 0.08, landingPhase));
        folded += radial * (travel * (0.024 + layer * 0.018) - tunnel * layer * (0.050 + dimensionalDepth * 0.034 + impossibleDose * 0.026));
        folded += tangent * sin(travel + layer * 5.7 + flowNoise * 2.0 + driftTime * (0.44 + temporalFlow * 0.22)) * foldAmount * (0.026 + layer * 0.018 + impossibleDose * 0.014);
        vec2 sampleUv = toAspectUv(folded, aspect);
        float inward = tunnel * foldPower * layer * (0.22 + deepDose * 0.20 + impossibleDose * 0.18);
        float reverse = returnPull * layer * (0.34 + dimensionalDepth * 0.18 + impossibleDose * 0.10);
        sampleUv = 0.5 + (sampleUv - 0.5) * (1.0 - inward + reverse);
        sampleUv += depthSlope * (0.034 + layer * 0.016 + impossibleDose * 0.016) * worldEdge * foldAmount;
        sampleUv = clamp(sampleUv, vec2(0.001), vec2(0.999));

        vec3 scene = sampleScene(sampleUv);
        float sampleD = sampleDepth(sampleUv);
        float sampleSceneMask = 1.0 - smoothstep(0.992, 1.0, sampleD);
        float layerEdge = smoothstep(0.05, 0.55, abs(luma(scene) - luma(base)) + worldEdge * 0.38);
        float weight = layerPresence * (0.24 + layer * 0.20 + impossibleDose * 0.10) * (0.54 + sampleSceneMask * 0.30 + layerEdge * 0.22);
        foldColor += scene * weight;
        foldWeight += weight;
        foldFlow += normalize(sampleUv - uv + vec2(0.001)) * weight;
        crease += layerEdge * weight * (0.20 + layer * 0.18);

        for (int j = 1; j <= 4; j++) {
            float lag = float(j);
            float trailAngle = angle - rotorSpeed * lag * (0.30 + layer * 0.10 + impossibleDose * 0.20);
            vec2 trailFold = foldSpace(effectP * (1.0 + layer * 0.18 + dimensionalDepth * 0.28 + impossibleDose * 0.12),
                    trailAngle,
                    foldAmount * (1.0 - lag * 0.075),
                    layerDepth,
                    motionTime - lag * 2.0,
                    seedB + layer + lag);
            trailFold += radial * (travel * (0.018 + layer * 0.012) - tunnel * layer * (0.034 + dimensionalDepth * 0.020));
            trailFold += tangent * sin(travel - lag * 0.75 + layer * 5.7 + flowNoise * 2.0 + driftTime * (0.32 + temporalFlow * 0.18)) * foldAmount * (0.017 + layer * 0.010);
            vec2 trailUv = toAspectUv(trailFold, aspect);
            trailUv = 0.5 + (trailUv - 0.5) * (1.0 - inward * 0.72 + reverse * 0.92);
            trailUv = clamp(trailUv, vec2(0.001), vec2(0.999));
            float trailWeight = weight * trailStrength * (0.34 / lag) * (0.72 + layer * 0.28);
            foldColor += sampleScene(trailUv) * trailWeight;
            foldWeight += trailWeight;
            foldFlow += normalize(trailUv - uv + vec2(0.001)) * trailWeight;
            crease += trailWeight * (0.06 + rotorDrive * 0.08);
        }
    }

    foldColor = foldWeight > 0.001 ? foldColor / foldWeight : warpedBase;
    crease = saturate(crease * 0.72);
    vec2 flow = normalize(foldFlow + radial * tunnel * 0.24 + tangent * (breath - 0.5) * 0.18 + vec2(0.001));

    float lens = saturate(foldPresence * (0.18 + tunnel * 0.40 + breakthrough * 0.28 + coupling * 0.14));
    vec2 lensUv = clamp(warpedUv + flow * lens * (0.010 + dimensionalDepth * 0.010 + highDose * 0.006) * mix(1.0, 0.38, accessibility), vec2(0.001), vec2(0.999));
    vec3 lensColor = sampleScene(lensUv);

    vec3 accent = mix(PaletteA.rgb, PaletteB.rgb, 0.45 + 0.25 * sin(travel + seedA));
    accent = mix(accent, PaletteC.rgb, saturate(crease * 0.32 + alternateWorld * 0.16));
    vec3 foldedWorld = mix(foldColor, lensColor, saturate(0.28 + crease * 0.26 + tunnel * 0.10));
    foldedWorld = hueShift(foldedWorld, (flowNoise - 0.5) * 0.030 * foldPresence + alternateWorld * 0.010);
    foldedWorld = mix(foldedWorld, foldedWorld * accent, realmPresence * 0.10 + fanMask * 0.10);

    float worldFoldMix = saturate((foldWeight * 0.56 + crease * 0.30 + worldEdge * 0.12 + fanMask * 0.26)
            * (0.32 + foldPresence * 0.74 + realmPresence * 0.34)
            * mix(1.0, 0.50, accessibility));
    worldFoldMix = min(worldFoldMix, mix(0.78, 0.42, accessibility));
    float recedeMix = saturate(returnPull * (0.18 + crease * 0.22 + foldWeight * 0.22));

    vec3 color = mix(base, warpedBase, saturate(foldPresence * 0.22 + tunnel * 0.14 + swirlDrive * 0.20));
    color = mix(color, foldedWorld, worldFoldMix);
    color = mix(color, sampleScene(clamp(uv - radial * recedeMix * (0.030 + dimensionalDepth * 0.026), vec2(0.001), vec2(0.999))), recedeMix * 0.34);
    color += accent * crease * (0.018 + realmPresence * 0.040) * mix(1.0, 0.42, accessibility);
    color = mix(color, mix(mix(fanTrailColor, foldedWorld, 0.55), accent, 0.24), fanMask * (0.22 + trailStrength * 0.34) * mix(1.0, 0.34, accessibility));
    color = mix(color, mix(color, accent, 0.26), fanTrails * trailStrength * splitDrive * 0.12 * mix(1.0, 0.34, accessibility));
    color += accent * fanMask * (0.026 + splitDrive * 0.040 + impossibleDose * 0.034) * mix(1.0, 0.36, accessibility);

    float visualDose = max(max(doseUnits, targetDoseUnits), displayedDoseUnits);
    float mandalaBaseOpacity = smoothstep(1.0, 5.0, visualDose);
    float mandalaReplacement = smoothstep(5.0, 7.0, visualDose);
    float mandalaOpacity = mix(mandalaBaseOpacity * 0.72, 1.0, mandalaReplacement);
    float mandalaDose = saturate((visualDose - 1.0) / 6.0);
    float mandalaSlices = mix(6.0, 13.0, saturate(mandalaDose * 0.76 + dimensionalDepth * 0.18 + Profile.w * 0.12));
    float wedge = 6.28318 / mandalaSlices;
    vec2 mandalaP = rotate2(p, -rotorAngle * (0.18 + mandalaDose * 0.22 + temporalFlow * 0.06));
    mandalaP += depthSlope * worldEdge * (0.030 + mandalaDose * 0.035 + dimensionalDepth * 0.020);
    mandalaP += tangent * sin(radius * (8.0 + dimensionalDepth * 7.0) - driftTime * (3.2 + temporalFlow * 2.0) + seedB) * mandalaDose * 0.018;
    float mandalaRadius = length(mandalaP);
    float mandalaAngle = atan(mandalaP.y, mandalaP.x)
            + rotorAngle * (0.42 + mandalaDose * 0.32)
            + sin(mandalaRadius * (7.0 + dimensionalDepth * 5.0) - slowTime * (2.2 + temporalFlow)) * mandalaDose * 0.44;
    float foldedA = abs(mod(mandalaAngle + wedge * 0.5, wedge) - wedge * 0.5);
    float foldedB = abs(mod(mandalaAngle + wedge, wedge) - wedge * 0.5);
    float petal = sin(mandalaRadius * (13.0 + highDose * 8.0) - motionTime * (3.4 + temporalFlow * 2.6) + foldedA * mandalaSlices);
    float pulse = sin(mandalaRadius * (21.0 + dimensionalDepth * 8.0) + foldedA * 5.0 - rotorAngle * 1.4 + seedA);
    vec2 kaleidoP = vec2(cos(foldedA), sin(foldedA)) * mandalaRadius;
    vec2 kaleidoP2 = vec2(cos(foldedB), sin(foldedB)) * mandalaRadius;
    kaleidoP = rotate2(kaleidoP, rotorAngle * 0.10 + petal * mandalaDose * 0.20);
    kaleidoP2 = rotate2(kaleidoP2, -rotorAngle * 0.08 + pulse * mandalaDose * 0.16);
    kaleidoP += radial * petal * mandalaDose * (0.026 + highDose * 0.018)
            + tangent * pulse * mandalaDose * (0.020 + dimensionalDepth * 0.016);
    kaleidoP2 += radial * pulse * mandalaDose * (0.018 + highDose * 0.012)
            - tangent * petal * mandalaDose * (0.016 + dimensionalDepth * 0.014);
    vec2 mandalaUv = clamp(toAspectUv(kaleidoP, aspect), vec2(0.001), vec2(0.999));
    vec2 mandalaUv2 = clamp(toAspectUv(kaleidoP2, aspect), vec2(0.001), vec2(0.999));
    float mandalaDepthA = sampleDepth(mandalaUv);
    float mandalaDepthB = sampleDepth(mandalaUv2);
    float mandalaSurface = 1.0 - smoothstep(0.930, 1.0, min(mandalaDepthA, mandalaDepthB));
    float depthDelta = clamp((mandalaDepthB - mandalaDepthA) * 7.0, -1.0, 1.0);
    vec2 mandalaParallax = (depthSlope * (0.030 + dimensionalDepth * 0.026)
            + radial * depthDelta * (0.022 + highDose * 0.014)
            + tangent * sin(driftTime * (2.6 + temporalFlow) + mandalaRadius * 12.0 + seedA) * 0.010)
            * mandalaDose * mandalaSurface * mix(1.0, 0.42, accessibility);
    mandalaUv = clamp(mandalaUv + mandalaParallax, vec2(0.001), vec2(0.999));
    mandalaUv2 = clamp(mandalaUv2 - mandalaParallax * 0.72, vec2(0.001), vec2(0.999));
    vec2 glowStep = normalize(vec2(-mandalaP.y, mandalaP.x) + vec2(0.001)) * texel * (7.0 + mandalaDose * 10.0);
    vec3 mandalaTex = sampleScene(mandalaUv);
    vec3 mandalaTex2 = sampleScene(mandalaUv2);
    vec3 mandalaGlow = (sampleScene(clamp(mandalaUv + glowStep, vec2(0.001), vec2(0.999)))
            + sampleScene(clamp(mandalaUv - glowStep, vec2(0.001), vec2(0.999)))
            + sampleScene(clamp(mandalaUv2 + glowStep * 1.7, vec2(0.001), vec2(0.999)))
            + sampleScene(clamp(mandalaUv2 - glowStep * 1.7, vec2(0.001), vec2(0.999)))) * 0.25;
    float mandalaMix = 0.38 + 0.24 * sin(slowTime * 0.65 + mandalaRadius * 9.0 + seedB);
    vec3 mandalaWorld = mix(mandalaTex, mandalaTex2, saturate(mandalaMix));
    float mandalaLum = luma(mandalaWorld);
    float glowLum = luma(mandalaGlow);
    float textureDetail = saturate(abs(luma(mandalaTex) - luma(mandalaTex2)) * 2.35
            + abs(glowLum - mandalaLum) * 1.80
            + mandalaSurface * 0.18);
    vec3 neonTint = mix(PaletteA.rgb, PaletteB.rgb, saturate(0.50 + 0.50 * sin(foldedA * mandalaSlices + driftTime * 0.38)));
    neonTint = mix(neonTint, PaletteC.rgb, saturate(0.34 + 0.34 * petal + mandalaDose * 0.16));
    mandalaWorld = mix(vec3(mandalaLum), mandalaWorld, 1.28 + mandalaDose * 0.42);
    mandalaWorld = mix(mandalaWorld, mandalaWorld * (0.84 + neonTint * 0.92), 0.24 + mandalaDose * 0.24);
    mandalaWorld += mix(vec3(glowLum), mandalaGlow, 1.42) * (0.14 + mandalaDose * 0.30) * (0.38 + textureDetail * 0.62);
    mandalaWorld += neonTint * pow(0.5 + 0.5 * pulse, 4.0) * (0.020 + mandalaDose * 0.058)
            * saturate(mandalaLum * 1.20 + textureDetail * 1.35);
    mandalaWorld = hueShift(mandalaWorld, (flowNoise - 0.5) * 0.075 + mandalaDose * 0.035);
    mandalaWorld = mandalaWorld / (vec3(0.72) + mandalaWorld);
    mandalaWorld = pow(clamp(mandalaWorld, 0.0, 1.0), vec3(0.84));
    color = mix(color, clamp(mandalaWorld, 0.0, 0.96), mandalaOpacity);

    float volumeDose = smoothstep(1.15, 5.0, visualDose);
    float volumeDepth = smoothstep(0.04, 0.92, distanceCue) * (1.0 - smoothstep(0.982, 1.0, depth));
    float volumeFlow = fbm(ray.xy * (3.2 + dimensionalDepth * 1.6)
            + (VrEnabled > 0.5 ? VrHeadPosition.xz : CameraA.xz) * 0.0007
            + vec2(driftTime * (0.13 + temporalFlow * 0.09), -driftTime * (0.10 + temporalFlow * 0.07)),
            seedB + 31.0);
    float volumeRibbon = 0.5 + 0.5 * sin(surfaceDistance * (0.030 + dimensionalDepth * 0.028)
            + dot(ray, normalize(vec3(0.62, -0.34, 0.71))) * (7.0 + mandalaDose * 5.0)
            - slowTime * (0.72 + temporalFlow * 0.40)
            + seedA * 3.7);
    float volumeVeil = saturate((volumeDepth * 0.78 + worldEdge * 0.18 + mandalaSurface * 0.16)
            * (0.36 + volumeFlow * 0.42 + pow(volumeRibbon, 2.2) * 0.44)
            * volumeDose
            * mix(1.0, 0.42, accessibility));
    vec3 volumeA = mix(PaletteA.rgb, PaletteB.rgb, saturate(0.50 + 0.50 * sin(ray.y * 4.0 + slowTime * 0.18 + seedA)));
    vec3 volumeB = mix(PaletteC.rgb, vec3(0.38, 0.95, 1.0), saturate(0.36 + 0.34 * sin(ray.x * 5.0 - slowTime * 0.13 + seedB)));
    vec3 volumeColor = mix(volumeA, volumeB, saturate(volumeFlow * 0.58 + volumeRibbon * 0.42));
    volumeColor = hueShift(volumeColor, (volumeFlow - 0.5) * 0.10 + mandalaDose * 0.055);
    float volumeBloom = pow(saturate(volumeRibbon * 0.68 + textureDetail * 0.32), 2.45)
            * (0.030 + volumeDose * 0.080 + mandalaReplacement * 0.050);
    vec3 volumeTinted = color * (0.78 + volumeColor * (0.80 + mandalaReplacement * 0.28))
            + volumeColor * volumeBloom * (0.70 + mandalaOpacity * 0.45);
    color = mix(color, volumeTinted, volumeVeil * mix(0.34, 0.58, mandalaReplacement));
    color += volumeColor * volumeBloom * volumeVeil * 0.55;
    color = mix(color, vec3(luma(color)), accessibility * 0.18);

    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}

