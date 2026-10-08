#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec2 OutSize;
uniform vec2 InSize;

uniform vec4 WarpA;    // intensity, anchor slip, scene replacement, time
uniform vec4 WarpB;    // body pull, loop seam, observer pressure, cutout presence
uniform vec4 WarpC;    // reentry confusion, progress, tier, dose
uniform vec4 WarpD;    // mega presence, wheel conveyor, zipper fold, flattening
uniform vec4 WarpE;    // objecthood, entity contact, reality stack, kaleidoscope
uniform vec4 PaletteA; // primary rgb, anchor bias
uniform vec4 PaletteB; // secondary rgb, recursion bias
uniform vec4 PaletteC; // accent rgb, observer bias
uniform vec4 EnvA;     // cave, village, stillness, depth
uniform vec4 EnvB;     // night, artificial, entities, light
uniform vec4 CameraA;  // wrapped camera position xyz, far plane
uniform vec4 CameraB;  // near-plane forward xyz, near plane
uniform vec4 CameraC;  // near-plane right xyz, seedA
uniform vec4 CameraD;  // near-plane up xyz, seedB
uniform float VrEnabled;
uniform mat4 VrInverseViewProjection;
uniform vec4 VrEyePosition;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 74.7);
    return fract(p.x * p.y);
}

float valueNoise(vec2 uv, float scale, float seed) {
    vec2 g = floor(uv * scale + seed);
    vec2 f = fract(uv * scale + seed);
    float a = hash21(g);
    float b = hash21(g + vec2(1.0, 0.0));
    float c = hash21(g + vec2(0.0, 1.0));
    float d = hash21(g + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

vec2 rotate2(vec2 p, float a) {
    float s = sin(a);
    float c = cos(a);
    return vec2(p.x * c - p.y * s, p.x * s + p.y * c);
}

float fbm(vec2 uv, float seed) {
    float value = 0.0;
    float amplitude = 0.52;
    float scale = 1.0;
    vec2 p = uv;
    for (int i = 0; i < 4; i++) {
        value += valueNoise(p, scale, seed + float(i) * 17.13) * amplitude;
        p = rotate2(p + vec2(1.73, -2.41), 0.54 + seed * 0.003);
        scale *= 2.03;
        amplitude *= 0.50;
    }
    return clamp(value, 0.0, 1.0);
}

float sampleDepth(vec2 uv) {
    return texture(DepthSampler, clamp(uv, 0.001, 0.999)).r;
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));

    float intensity = WarpA.x;
    float anchorSlip = WarpA.y;
    float sceneReplacement = WarpA.z;
    float time = WarpA.w * 0.016;
    float bodyPull = WarpB.x;
    float loopSeam = WarpB.y;
    float observerPressure = WarpB.z;
    float cutoutPresence = WarpB.w;
    float reentryConfusion = WarpC.x;
    float progress = WarpC.y;
    float tier = WarpC.z;
    float dose = WarpC.w;
    float megaPresence = WarpD.x;
    float wheelConveyor = WarpD.y;
    float zipperFold = WarpD.z;
    float flattening = WarpD.w;
    float objecthood = WarpE.x;
    float entityContact = WarpE.y;
    float realityStack = WarpE.z;
    float kaleidoscope = WarpE.w;
    float anchorBias = PaletteA.w;
    float recursionBias = PaletteB.w;
    float observerBias = PaletteC.w;
    float cave = EnvA.x;
    float village = EnvA.y;
    float stillness = EnvA.z;
    float depthEnv = EnvA.w;
    float night = EnvB.x;
    float artificial = EnvB.y;
    float entities = EnvB.z;
    float lightEnv = EnvB.w;
    float seedA = CameraC.w * 37.0 + 1.0;
    float seedB = CameraD.w * 53.0 + 3.0;
    float highDose = smoothstep(6.0, 10.0, dose);
    float realityBreak = smoothstep(10.0, 15.0, dose);

    vec3 base = texture(DiffuseSampler, uv).rgb;
    float rawDepth = sampleDepth(uv);
    float depthLeft = sampleDepth(uv - vec2(texel.x * 2.0, 0.0));
    float depthRight = sampleDepth(uv + vec2(texel.x * 2.0, 0.0));
    float depthDown = sampleDepth(uv - vec2(0.0, texel.y * 2.0));
    float depthUp = sampleDepth(uv + vec2(0.0, texel.y * 2.0));
    vec2 depthSlope = vec2(depthLeft - depthRight, depthDown - depthUp);
    float depthEdge = smoothstep(0.0006, 0.024, abs(depthLeft - depthRight) + abs(depthDown - depthUp));
    float sceneGeometry = VrEnabled > 0.5 ? 1.0 - step(0.999995, rawDepth)
            : 1.0 - smoothstep(0.995, 0.99995, rawDepth);
    float farDepth = smoothstep(0.60, 0.999, rawDepth);
    float nearDepth = 1.0 - smoothstep(0.15, 0.80, rawDepth);
    vec2 depthNormal = normalize(depthSlope * (28.0 + sceneReplacement * 36.0 + anchorSlip * 18.0) + vec2(0.001));

    vec2 centered = uv - 0.5;
    centered.x *= OutSize.x / max(OutSize.y, 1.0);
    float radius = length(centered);
    vec2 radial = centered / max(radius, 0.025);
    vec2 tangent = vec2(-radial.y, radial.x);

    vec2 screenNdc = uv * 2.0 - 1.0;
    vec3 cameraForward = normalize(CameraB.xyz);
    vec3 worldRay = normalize(CameraB.xyz + CameraC.xyz * screenNdc.x + CameraD.xyz * screenNdc.y);
    float depthNear = max(0.001, CameraB.w);
    float depthFar = max(depthNear + 1.0, CameraA.w);
    float linearViewDepth = (depthNear * depthFar) / max(depthNear, depthFar - rawDepth * (depthFar - depthNear));
    float rayForward = max(0.18, dot(worldRay, cameraForward));
    float depthDistance = clamp(linearViewDepth / rayForward, 1.0, depthFar);
    float skyDistance = 120.0 + cave * 40.0 + stillness * 30.0 + sceneReplacement * 60.0;
    float anchorDistance = mix(skyDistance, depthDistance, sceneGeometry);
    vec3 worldAnchor = CameraA.xyz + worldRay * anchorDistance;
    if (VrEnabled > 0.5) {
        vec4 eyeRay = VrInverseViewProjection * vec4(screenNdc, 0.0, 1.0);
        worldRay = normalize(eyeRay.xyz / max(eyeRay.w, 0.000001));
        vec4 eyePoint = VrInverseViewProjection * vec4(screenNdc, rawDepth * 2.0 - 1.0, 1.0);
        vec3 relativePoint = eyePoint.xyz / max(eyePoint.w, 0.000001);
        depthDistance = length(relativePoint);
        anchorDistance = mix(skyDistance, depthDistance, sceneGeometry);
        worldAnchor = sceneGeometry > 0.5 ? VrEyePosition.xyz + relativePoint : worldRay * skyDistance;
    }
    float megaBlend = clamp(megaPresence, 0.0, 1.0);

    float tripEnvelope = smoothstep(0.04, 0.30, progress) * (1.0 - smoothstep(0.76, 1.0, progress));
    float seamField = abs(fract(worldAnchor.y * (0.072 + bodyPull * 0.030)
            + fbm(vec2(worldAnchor.x * 0.024 + worldAnchor.z * 0.014, worldAnchor.y * 0.032) + vec2(time * 0.07, -time * 0.05), 43.0 + recursionBias * 23.0) * (1.5 + loopSeam * 2.6)
            + time * (0.18 + loopSeam * 0.30)) - 0.5);
    float seamMask = (1.0 - smoothstep(0.10, 0.42, seamField)) * sceneGeometry;
    float slotMask = seamMask
            * (0.18 + sceneReplacement * 0.52 + depthEdge * 0.30)
            * (0.30 + farDepth * 0.45 + nearDepth * 0.25)
            * (0.34 + tripEnvelope * 0.66);
    float slabNoise = fbm(vec2(worldAnchor.x * 0.036 + worldAnchor.y * 0.010, worldAnchor.z * 0.036 - worldAnchor.y * 0.012), 11.0 + anchorBias * 31.0 + seedA * 0.11);
    float pullWave = sin(worldAnchor.x * (0.055 + anchorBias * 0.028)
            + worldAnchor.z * 0.046
            + time * (0.82 + bodyPull * 0.85)
            + slabNoise * 6.28318);
    vec2 slotShift = depthNormal * slotMask * (0.008 + sceneReplacement * 0.030 + anchorSlip * 0.022) * (0.4 + abs(pullWave));
    vec2 anchorShift = tangent * pullWave * (0.002 + anchorSlip * 0.012) * (0.35 + farDepth * 0.65);
    vec2 loopShift = depthNormal.yx * sin(worldAnchor.y * 0.090 + time * (1.4 + loopSeam * 1.3) + slabNoise * 4.0) * loopSeam * 0.006;

        vec2 wheelCenter = vec2(0.0, -0.04 + (seedA - 0.5) * 0.12);
        vec2 wheelLocal = centered - wheelCenter;
        float wheelRadius = length(wheelLocal * vec2(1.0, 0.82));
        float wheelNoise = fbm(centered * (5.2 + wheelConveyor * 2.8) + vec2(time * 0.05, -time * 0.04), 17.0 + seedA * 0.21);
        float wheelBand = 1.0 - smoothstep(0.02 + wheelConveyor * 0.02, 0.18 + wheelConveyor * 0.10, abs(wheelRadius - (0.18 + wheelConveyor * 0.10 + megaBlend * 0.05)));
        float beltStripes = sin(wheelRadius * (32.0 + wheelConveyor * 40.0) - time * (1.8 + wheelConveyor * 4.5) + wheelNoise * 6.28318) * 0.5 + 0.5;
        float wheelMask = wheelBand * smoothstep(0.16, 0.88, beltStripes) * (0.26 + farDepth * 0.74);
        vec2 wheelShift = (tangent * (0.003 + wheelConveyor * 0.022) - radial * (0.001 + wheelConveyor * 0.014)) * wheelMask;

        float zipperAngle = 0.55 + anchorBias * 1.8 + recursionBias * 0.7;
        vec2 foldNormal = vec2(cos(zipperAngle), sin(zipperAngle));
        vec2 pageNormal = vec2(-foldNormal.y, foldNormal.x);
        float zipperAxis = dot(centered, foldNormal)
            + (fbm(vec2(worldAnchor.y * 0.020, worldAnchor.x * 0.015 + worldAnchor.z * 0.011) + vec2(time * 0.03, -time * 0.02), 119.0 + seedB * 0.15) - 0.5)
            * (0.05 + zipperFold * 0.12);
        float zipperField = abs(zipperAxis);
        float zipperMask = (1.0 - smoothstep(0.010 + flattening * 0.010, 0.095 + zipperFold * 0.18, zipperField)) * (0.30 + sceneGeometry * 0.70) * zipperFold;
        vec2 zipperShift = foldNormal * sign(zipperAxis) * zipperMask * (0.003 + zipperFold * 0.020 + flattening * 0.012);
        vec2 stackShift = depthNormal.yx * sin(time * (0.52 + realityStack * 0.45) + worldAnchor.y * 0.08 + worldAnchor.x * 0.03) * (0.002 + realityStack * 0.018)
            + tangent * (0.001 + realityStack * 0.010) * (0.5 - farDepth);
        vec2 megaShift = (wheelShift + zipperShift + stackShift) * (1.0 + highDose * 0.80 + realityBreak * 1.20);

        vec2 sampleUvA = clamp(uv + slotShift + anchorShift + loopShift + megaShift, 0.001, 0.999);
        vec2 sampleUvB = clamp(uv - slotShift * 0.45 + anchorShift * 0.25 - loopShift * 0.65 - megaShift * 0.55, 0.001, 0.999);
    vec3 sampleA = texture(DiffuseSampler, sampleUvA).rgb;
    vec3 sampleB = texture(DiffuseSampler, sampleUvB).rgb;
    float ghostMix = clamp(slotMask * 0.55 + loopSeam * 0.20 + anchorSlip * 0.14 + highDose * 0.12 + realityBreak * 0.16, 0.0, 0.90);
    vec3 color = mix(sampleA, sampleB, ghostMix);

        vec2 foldUv = clamp(uv - foldNormal * zipperAxis * zipperMask * (0.28 + flattening * 0.45), 0.001, 0.999);
        vec3 foldSample = texture(DiffuseSampler, foldUv).rgb;
        color = mix(color, foldSample, clamp(zipperMask * (0.22 + zipperFold * 0.28 + highDose * 0.10 + realityBreak * 0.10), 0.0, 0.58));

        float flatPhase = dot(worldAnchor.xz, pageNormal * (0.018 + flattening * 0.008))
            + worldAnchor.y * (0.014 + flattening * 0.006)
            + time * 0.08;
        float flatField = 1.0 - smoothstep(0.10, 0.34, abs(fract(flatPhase) - 0.5));
        vec2 flatShift = pageNormal * sign(dot(centered, pageNormal) + 0.001) * flattening * flatField * (0.002 + sceneGeometry * 0.010 + farDepth * 0.004);
        vec3 flatSample = texture(DiffuseSampler, clamp(uv + flatShift + depthNormal * flattening * 0.004, 0.001, 0.999)).rgb;
        float flatMask = flattening * flatField * (0.16 + sceneGeometry * 0.26 + farDepth * 0.12 + highDose * 0.10 + realityBreak * 0.12);
        color = mix(color, flatSample, clamp(flatMask, 0.0, 0.52));

        vec3 stackA = texture(DiffuseSampler, clamp(uv + stackShift * 1.35 + slotShift * 0.18, 0.001, 0.999)).rgb;
        vec3 stackB = texture(DiffuseSampler, clamp(uv - stackShift * 1.20 - anchorShift * 0.20, 0.001, 0.999)).rgb;
        float stackPhase = sin(time * (0.64 + realityStack * 0.42) + worldAnchor.y * 0.08 + worldAnchor.x * 0.025) * 0.5 + 0.5;
        vec3 stackedReality = mix(stackA, stackB, stackPhase);
        color = mix(color, stackedReality, clamp(realityStack * (0.18 + farDepth * 0.18 + depthEdge * 0.12 + highDose * 0.10 + realityBreak * 0.18), 0.0, 0.72));

        float kaleiAngle = atan(centered.y, centered.x);
        float segmentCount = floor(mix(3.0, 8.0, clamp(kaleidoscope * 0.75 + megaBlend * 0.25, 0.0, 1.0)) + 0.5);
        float wedgeAngle = 6.2831853 / max(3.0, segmentCount);
        float foldedAngle = abs(mod(kaleiAngle + wedgeAngle * 0.5, wedgeAngle) - wedgeAngle * 0.5);
        vec2 kaleiCentered = vec2(cos(foldedAngle), sin(foldedAngle)) * radius;
        kaleiCentered.x /= OutSize.x / max(OutSize.y, 1.0);
        vec2 kaleiUv = clamp(kaleiCentered + 0.5 + depthNormal * kaleidoscope * 0.010, 0.001, 0.999);
        vec3 kaleiSample = texture(DiffuseSampler, kaleiUv).rgb;
        vec3 prismSample = vec3(
            texture(DiffuseSampler, clamp(kaleiUv + texel * vec2(2.0, 0.0), 0.001, 0.999)).r,
            texture(DiffuseSampler, clamp(kaleiUv + texel * vec2(-1.5, 1.5), 0.001, 0.999)).g,
            texture(DiffuseSampler, clamp(kaleiUv + texel * vec2(0.0, -2.0), 0.001, 0.999)).b
        );
        color = mix(color, kaleiSample, clamp(kaleidoscope * (0.12 + farDepth * 0.16 + depthEdge * 0.12 + highDose * 0.12 + realityBreak * 0.20), 0.0, 0.76));
        color = mix(color, prismSample, clamp(kaleidoscope * (0.08 + megaBlend * 0.08 + highDose * 0.12 + realityBreak * 0.18), 0.0, 0.62));

    float volume = 0.0;
    vec3 volumeTint = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        float stepT = (float(i) + 1.0) / 4.0;
        vec3 slicePos = (VrEnabled > 0.5 ? VrEyePosition.xyz : CameraA.xyz)
                + worldRay * mix(depthDistance * 0.24, depthDistance + 24.0 + sceneReplacement * 28.0, stepT);
        vec2 driftUv = vec2(
            slicePos.x * 0.018 + slicePos.z * 0.011,
            slicePos.y * 0.015 - slicePos.x * 0.007
        ) + vec2(time * (0.018 + stepT * 0.012), -time * (0.014 + stepT * 0.010));
        float ribbon = fbm(driftUv, 71.0 + float(i) * 19.0 + seedB * 0.09);
        float knot = fbm(driftUv.yx * 1.6 + vec2(2.3, -1.7), 121.0 + float(i) * 13.0 + seedA * 0.11);
        float plume = smoothstep(0.52, 0.88, ribbon * 0.72 + knot * 0.48);
        float wisps = smoothstep(0.46, 0.92, abs(ribbon - knot) + fbm(driftUv * 2.1 + vec2(-3.1, 1.4), 151.0 + float(i) * 9.0) * 0.28);
        float density = clamp(plume * 0.70 + wisps * 0.30, 0.0, 1.0);
        float weight = (0.06 + stepT * 0.11) * (0.16 + observerPressure * 0.34 + sceneReplacement * 0.14 + realityStack * 0.08);
        volume += density * weight;
        vec3 tint = mix(PaletteA.rgb, PaletteB.rgb, stepT * 0.40 + knot * 0.22);
        volumeTint += tint * density * weight;
    }
    volume = clamp(volume, 0.0, 0.9);
    volumeTint = volume > 0.0001 ? volumeTint / volume : PaletteB.rgb;
    float observerHaze = volume * (0.03 + observerPressure * 0.08 + cutoutPresence * 0.03) * (0.28 + farDepth * 0.30);
    color += volumeTint * observerHaze;

        vec2 entityField = fract(vec2(
            dot(worldAnchor.xz, vec2(0.013, -0.017)),
            worldAnchor.y * 0.027 + dot(worldAnchor.xz, vec2(0.008, 0.012))
        ) + vec2(seedA * 2.0 + time * 0.01, seedB * 2.0 - time * 0.008)) - 0.5;
        float eyeLeft = 1.0 - smoothstep(0.04, 0.14, length((entityField - vec2(0.16, 0.0)) * vec2(1.8, 1.0)));
        float eyeRight = 1.0 - smoothstep(0.04, 0.14, length((entityField + vec2(0.16, 0.0)) * vec2(1.8, 1.0)));
        float brow = 1.0 - smoothstep(0.02, 0.08, abs(entityField.y + 0.18));
        float faceNoise = smoothstep(0.58, 0.92, fbm(vec2(worldAnchor.x * 0.010 + time * 0.014, worldAnchor.y * 0.018 - worldAnchor.z * 0.012), 151.0 + seedA * 0.19));
        float entityMask = entityContact * faceNoise * (eyeLeft + eyeRight * 0.95) * (0.25 + volume * 0.75) * (0.22 + farDepth * 0.52 + depthEdge * 0.26 + realityBreak * 0.18);
        vec3 entityTint = mix(PaletteC.rgb, vec3(1.0, 0.96, 0.88), 0.26 + observerPressure * 0.22);
        color += entityTint * entityMask * (0.10 + brow * 0.04);

        vec2 objectCell = abs(fract(worldAnchor.xz * (0.050 + objecthood * 0.024) + vec2(seedA, seedB)) - 0.5);
        float objectGrid = 1.0 - smoothstep(0.03, 0.16 + objecthood * 0.08, min(objectCell.x, objectCell.y));
        float rivet = 1.0 - smoothstep(0.02, 0.07, length(fract(worldAnchor.xz * (0.10 + objecthood * 0.04)) - 0.5));
        float objectMask = objecthood * (objectGrid * 0.70 + rivet * 0.30) * (0.24 + artificial * 0.48 + village * 0.20);
        vec3 rigidColor = floor(color * (6.0 + objecthood * 9.0)) / max(1.0, 6.0 + objecthood * 9.0);
        rigidColor = mix(rigidColor, PaletteB.rgb, objectMask * 0.22);
        color = mix(color, rigidColor, clamp(objectMask * (0.26 + highDose * 0.08 + realityBreak * 0.12), 0.0, 0.52));

    float cutout = smoothstep(0.38, 0.96, fbm(vec2(worldAnchor.x * 0.015, worldAnchor.z * 0.015) + vec2(time * 0.012, -time * 0.010), 97.0 + observerBias * 29.0 + seedB * 0.13));
    float cutoutMask = cutoutPresence * cutout * depthEdge * 0.20;
    color = mix(color, mix(color, PaletteC.rgb, 0.35), cutoutMask);

    float caveTint = cave * 0.08 + night * 0.06 + observerPressure * 0.04;
    float familiarTint = village * 0.05 + artificial * 0.06 + sceneReplacement * 0.04;
    color *= vec3(1.0 + familiarTint * 0.12, 1.0 + familiarTint * 0.04 - caveTint * 0.04, 1.0 - caveTint * 0.10);
    color += mix(PaletteA.rgb, PaletteC.rgb, beltStripes) * (wheelConveyor * 0.04 + kaleidoscope * 0.08 + megaBlend * 0.025 + highDose * 0.08 + realityBreak * 0.14) * (0.24 + lightEnv * 0.16 + farDepth * 0.18);

    float reentryPulse = sin(time * (1.1 + reentryConfusion * 2.4) + radius * 12.0 + slabNoise * 2.0) * 0.5 + 0.5;
    color = mix(color, mix(color, PaletteC.rgb, 0.25), reentryConfusion * (1.0 - sceneGeometry * 0.3) * reentryPulse * 0.18);

    float baseLuma = max(0.02, luma(color));
    vec3 saturated = clamp((color - vec3(baseLuma)) * (1.0 + highDose * 1.40 + realityBreak * 2.30) + vec3(baseLuma), 0.0, 1.6);
    vec3 realityPalette = mix(
        mix(PaletteA.rgb, PaletteB.rgb, 0.40 + wheelConveyor * 0.10),
        mix(PaletteC.rgb, vec3(1.0, 0.28, 0.08), 0.22 + kaleidoscope * 0.18),
        0.32 + realityBreak * 0.34
    );
    color = mix(color, saturated, clamp(highDose * 0.26 + realityBreak * 0.20, 0.0, 0.52));
    color = mix(color, mix(color, realityPalette, 0.68), clamp(highDose * 0.12 + realityBreak * 0.34, 0.0, 0.58));

    float baseLum = max(0.02, luma(base));
    float colorLum = max(0.02, luma(color));
    color *= clamp(baseLum / colorLum, 0.72, 1.28);
    float megaMix = megaBlend * 0.12 + wheelConveyor * 0.18 + zipperFold * 0.16 + flattening * 0.18 + objecthood * 0.12 + entityContact * 0.14 + realityStack * 0.22 + kaleidoscope * 0.18 + highDose * 0.18 + realityBreak * 0.28;
    float finalMix = clamp(0.16 + intensity * 0.34 + slotMask * 0.28 + observerHaze * 0.20 + tier * 0.08 + dose * 0.022 + megaMix, 0.0, 0.985);
    finalMix *= 0.80 + lightEnv * 0.12 + entities * 0.04 + depthEnv * 0.04;
    color = mix(base, color, finalMix);

    float vignette = smoothstep(0.22, 0.92, radius) * (observerPressure * 0.10 + reentryConfusion * 0.05);
    color *= 1.0 - vignette;
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

