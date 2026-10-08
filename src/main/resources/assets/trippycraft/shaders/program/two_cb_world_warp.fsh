#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 InSize;
uniform vec4 WarpA; // intensity, saturation, edge glow, time
uniform vec4 WarpB; // surface geometry, tracers, bass pulse, fisheye pulse
uniform vec4 WarpC; // symmetry, crystalline, photosensitivity reduction, extreme-dose overload
uniform vec4 WarpD; // cocaine synergy, tavern presence, trip progress, dose tier

out vec4 fragColor;

const float TAU = 6.2831853;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float hash21(vec2 point) {
    vec3 p3 = fract(vec3(point.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

mat2 rotation(float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return mat2(cosine, -sine, sine, cosine);
}

vec3 scene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

vec3 screenBlend(vec3 base, vec3 glow) {
    return 1.0 - (1.0 - clamp(base, 0.0, 1.0)) * (1.0 - clamp(glow, 0.0, 0.94));
}

float triangleGrid(vec2 point) {
    point.x += point.y * 0.57735027;
    vec2 cell = abs(fract(point) - 0.5);
    float bars = min(cell.x, min(cell.y, abs(fract(point.x + point.y) - 0.5)));
    return 1.0 - smoothstep(0.025, 0.090, bars);
}

vec2 kaleido(vec2 uv, float segments) {
    vec2 point = uv - 0.5;
    point.x *= OutSize.x / max(OutSize.y, 1.0);
    float radius = length(point);
    float angle = atan(point.y, point.x);
    float sector = TAU / max(3.0, segments);
    angle = abs(mod(angle + sector * 0.5, sector) - sector * 0.5);
    point = vec2(cos(angle), sin(angle)) * radius;
    point.x /= OutSize.x / max(OutSize.y, 1.0);
    return point + 0.5;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec4 source = texture(DiffuseSampler, uv);
    float intensity = clamp(WarpA.x, 0.0, 3.2);
    float saturation = clamp(WarpA.y, 0.0, 4.0);
    float edgeGlow = clamp(WarpA.z, 0.0, 4.0);
    float time = WarpA.w;
    float geometry = clamp(WarpB.x, 0.0, 4.0);
    float tracers = clamp(WarpB.y, 0.0, 4.0);
    float beat = clamp(WarpB.z, 0.0, 3.0);
    float fisheye = clamp(WarpB.w, 0.0, 1.2);
    float symmetry = clamp(WarpC.x, 0.0, 4.0);
    float crystalline = clamp(WarpC.y, 0.0, 4.0);
    float photosensitive = saturate(WarpC.z);
    float overload = clamp(WarpC.w, 0.0, 3.0);
    float cocaineSynergy = clamp(WarpD.x, 0.0, 1.35);
    float club = saturate(WarpD.y);
    float tripProgress = saturate(WarpD.z);
    float tier = saturate(WarpD.w);

    if (intensity <= 0.0001) {
        fragColor = source;
        return;
    }

    // The first reality failures are already assertive at 1000mg, then add new behavior near 1500mg.
    float highDose = smoothstep(0.48, 0.66, overload);
    float veryHighDose = smoothstep(1.00, 1.35, overload);
    float extremeDose = smoothstep(1.75, 2.35, overload);
    float motionSafety = 1.0 - photosensitive * 0.72;
    float beatEnergy = saturate(beat * 0.78 + club * 0.18 + cocaineSynergy * 0.08);
    float modeClock = time * (0.0042 + cocaineSynergy * 0.0018) + tripProgress * TAU;
    float realityMode = 0.58 + sin(modeClock) * 0.28;
    float sliceMode = 0.54 + sin(modeClock * 0.73 + 2.10) * 0.30;
    float laserMode = 0.52 + sin(modeClock * 0.47 + 4.20) * 0.34;
    float skyMode = 0.56 + sin(modeClock * 0.39 + 1.30) * 0.32;
    float phrase = floor(time * (0.0026 + cocaineSynergy * 0.0008) + tripProgress * 5.0);

    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 centered = uv - 0.5;
    centered.x *= aspect;
    float radius2 = dot(centered, centered);
    centered *= 1.0 + radius2 * fisheye * beat * (0.20 - photosensitive * 0.11);
    centered.x /= aspect;
    vec2 warpedUv = centered + 0.5;

    float symmetryGate = smoothstep(0.26, 0.92, beat + sin(time * 0.031) * 0.22 + 0.16);
    symmetryGate *= symmetry * (1.0 - photosensitive * 0.48);
    vec2 mirrorUv = kaleido(warpedUv, 4.0 + floor(crystalline * 3.0 + overload * 2.0));
    float kaleidoMix = clamp(symmetryGate * 0.34, 0.0, 0.54) * (1.0 - highDose * 0.80);
    kaleidoMix += highDose * beatEnergy * 0.035 * motionSafety;
    vec3 base = mix(scene(warpedUv), scene(mirrorUv), saturate(kaleidoMix));

    // Above 1000mg, the scene itself stops agreeing with its position and scale.
    float slabCount = 5.0 + floor(overload * 2.0 + club);
    float slabId = floor(warpedUv.y * slabCount + sin(modeClock * 0.23) * 0.45);
    float slabHash = hash21(vec2(slabId, phrase));
    float slabPulse = smoothstep(0.24, 0.90, beatEnergy + sin(modeClock + slabHash * TAU) * 0.18);
    vec2 slabShift = vec2(
            (slabHash - 0.5) * texel.x * (22.0 + overload * 28.0),
            sin(slabHash * 17.0 + modeClock * 0.31) * texel.y * (3.0 + overload * 5.0)
    );
    vec3 displacedReality = scene(warpedUv + slabShift * (0.30 + beatEnergy * 0.70));
    float slabMix = highDose * motionSafety * realityMode * slabPulse
            * (0.08 + veryHighDose * 0.10 + club * 0.055 + cocaineSynergy * 0.07);
    base = mix(base, displacedReality, saturate(slabMix));

    vec2 echoPoint = (warpedUv - 0.5) * vec2(aspect, 1.0);
    float echoScale = 0.90 + sin(modeClock * 0.37) * (0.035 + veryHighDose * 0.035);
    echoPoint = rotation(sin(modeClock * 0.19) * 0.055 * veryHighDose) * echoPoint * echoScale;
    echoPoint.x /= aspect;
    vec2 echoUv = echoPoint + 0.5;
    vec3 impossibleEcho = scene(echoUv);
    float echoMix = highDose * motionSafety * sliceMode
            * (0.055 + beatEnergy * 0.075 + veryHighDose * 0.09 + extremeDose * 0.06);
    base = mix(base, impossibleEcho, saturate(echoMix));

    // A wandering horizon locally reflects the wrong piece of the world without becoming a full-screen mirror.
    float falseHorizon = 0.46 + sin(modeClock * 0.21 + phrase * 0.37) * 0.16;
    float horizonBand = 1.0 - smoothstep(0.035, 0.18, abs(warpedUv.y - falseHorizon));
    vec2 horizonUv = vec2(
            warpedUv.x + sin(modeClock * 0.43 + warpedUv.y * 11.0) * 0.018 * veryHighDose,
            falseHorizon - (warpedUv.y - falseHorizon) * (0.72 + sin(modeClock * 0.17) * 0.18)
    );
    float horizonMix = horizonBand * veryHighDose * motionSafety
            * (0.065 + beatEnergy * 0.085 + extremeDose * 0.055);
    base = mix(base, scene(horizonUv), saturate(horizonMix));

    // Sparse beat-swapped strips make left and right disagree only in selected parts of the frame.
    float swapField = sin(warpedUv.y * (31.0 + overload * 9.0) + modeClock * 0.77 + phrase);
    float swapMask = smoothstep(0.72, 0.98, swapField * 0.5 + 0.5) * veryHighDose;
    vec2 swappedUv = vec2(1.0 - warpedUv.x, warpedUv.y + sin(modeClock * 0.29) * 0.012);
    float swapMix = swapMask * motionSafety * (0.035 + beatEnergy * 0.095 + extremeDose * 0.06);
    base = mix(base, scene(swappedUv), saturate(swapMix));

    float centerLuma = luma(base);
    float colorGain = 1.0 + saturation * (1.25 + intensity * 0.48);
    vec3 color = vec3(centerLuma) + (base - vec3(centerLuma)) * colorGain;
    color = (color - 0.5) * (1.0 + intensity * 0.18 + crystalline * 0.12) + 0.5;

    // Dose and cocaine clarity create crisp RGB diffraction rather than liquid warping.
    vec2 aspectCentered = (warpedUv - 0.5) * vec2(aspect, 1.0);
    vec2 prismDirection = normalize(aspectCentered + vec2(0.0001));
    prismDirection = rotation(modeClock * 0.17) * prismDirection;
    prismDirection.x /= aspect;
    vec2 prismOffset = prismDirection * texel * (3.0 + overload * 7.0 + cocaineSynergy * 7.0)
            * (0.45 + beatEnergy * 0.55);
    vec3 prismColor = vec3(scene(warpedUv + prismOffset).r, base.g, scene(warpedUv - prismOffset).b);
    float prismMix = highDose * motionSafety
            * (0.075 + veryHighDose * 0.075 + cocaineSynergy * 0.11 + beatEnergy * 0.045);
    color = mix(color, prismColor, saturate(prismMix));

    vec3 sx1 = scene(warpedUv + vec2(texel.x * 1.7, 0.0));
    vec3 sx2 = scene(warpedUv - vec2(texel.x * 1.7, 0.0));
    vec3 sy1 = scene(warpedUv + vec2(0.0, texel.y * 1.7));
    vec3 sy2 = scene(warpedUv - vec2(0.0, texel.y * 1.7));
    float edgeSignal = length(sx1 - sx2) + length(sy1 - sy2);
    float edge = smoothstep(0.08, 0.46, edgeSignal) * edgeGlow * (1.0 - photosensitive * 0.42);
    vec3 neon = mix(vec3(0.08, 0.96, 1.0), vec3(1.0, 0.10, 0.78), smoothstep(0.20, 0.82, centerLuma));
    color = screenBlend(color, neon * edge * (0.46 + beat * 0.32 + cocaineSynergy * 0.15));

    // Bright, low-detail upper regions (especially clouds) acquire flowing electric texture.
    float skyGate = smoothstep(0.42, 0.86, centerLuma)
            * (1.0 - smoothstep(0.045, 0.30, edgeSignal))
            * smoothstep(0.26, 0.76, warpedUv.y);
    vec2 skyPoint = warpedUv * vec2(aspect, 1.0) * vec2(9.0 + overload * 2.0, 6.0 + overload);
    float skyRibbonA = sin(skyPoint.x * 1.55 + sin(skyPoint.y * 2.10 - modeClock * 0.58) * 1.65 + modeClock * 0.42);
    float skyRibbonB = cos(skyPoint.y * 1.82 + sin(skyPoint.x * 1.34 + modeClock * 0.31) * 1.42 - modeClock * 0.36);
    float skyTexture = skyRibbonA * skyRibbonB * 0.5 + 0.5;
    float skyVeins = pow(abs(sin((skyRibbonA - skyRibbonB) * 3.2 + modeClock * 0.44)), 9.0);
    vec3 skyPalette = mix(vec3(0.05, 0.92, 1.0), vec3(1.0, 0.06, 0.72), skyTexture);
    skyPalette = mix(skyPalette, vec3(0.48, 0.12, 1.0), skyVeins * 0.48);
    vec3 texturedSky = color * (0.88 + skyTexture * 0.18) + skyPalette * (0.055 + skyVeins * 0.16);
    float skyMix = skyGate * highDose * skyMode * motionSafety
            * (0.14 + veryHighDose * 0.14 + club * 0.06 + beatEnergy * 0.06);
    color = mix(color, texturedSky, saturate(skyMix));

    float scale = 7.0 + crystalline * 13.0 + overload * 12.0;
    vec2 patternUv = (warpedUv * vec2(aspect, 1.0) + vec2(time * 0.0028, -time * 0.0019)) * scale;
    float pattern = triangleGrid(patternUv);
    float surfaceGate = smoothstep(0.10, 0.88, centerLuma) * (1.0 - smoothstep(0.92, 1.0, centerLuma));
    float patternStrength = pattern * geometry * surfaceGate
            * (0.10 + crystalline * 0.14 + overload * 0.10) * (1.0 - photosensitive * 0.46)
            * (1.0 - highDose * 0.78);
    color = screenBlend(color, mix(vec3(0.10, 0.88, 1.0), vec3(1.0, 0.12, 0.72), pattern) * patternStrength);

    vec2 snapDirection = normalize(vec2(0.72, 0.38) + vec2(sin(time * 0.017), cos(time * 0.013)) * 0.24);
    vec2 snap = snapDirection * texel * (2.0 + beat * 8.0 + crystalline * 3.0 + cocaineSynergy * 5.0);
    vec3 traceA = scene(warpedUv - snap);
    vec3 traceB = scene(warpedUv - snap * 2.0);
    vec3 traceC = scene(warpedUv - snap * (3.0 + overload));
    float tracerGate = smoothstep(0.055, 0.38, length(base - traceA));
    float tracerStrength = tracers * beat * tracerGate * (0.22 + intensity * 0.16 + cocaineSynergy * 0.10)
            * (1.0 - photosensitive * 0.58);
    vec3 traceColor = mix(traceA * vec3(1.0, 0.18, 0.86), traceB * vec3(0.12, 0.92, 1.0), 0.46);
    traceColor = mix(traceColor, traceC * vec3(0.78, 0.22, 1.0), clamp(overload * 0.22, 0.0, 0.64));
    color = screenBlend(color, traceColor * tracerStrength);

    if (crystalline > 0.02) {
        float facets = triangleGrid(patternUv * (1.36 + crystalline * 0.20) + 0.25);
        float facetLight = (facets - 0.42) * crystalline * (0.055 + beat * 0.030) * (1.0 - highDose * 0.76);
        color += neon * facetLight * (1.0 - photosensitive * 0.35);
    }

    if (overload > 0.02) {
        float microFacets = triangleGrid(patternUv * (2.1 + overload * 0.7) - vec2(time * 0.004, 0.0));
        float electricLattice = smoothstep(0.45, 0.92, microFacets) * overload
                * (1.0 - photosensitive * 0.55) * (1.0 - highDose * 0.92);
        vec3 latticeColor = mix(vec3(0.15, 0.92, 1.0), vec3(1.0, 0.08, 0.82), fract(patternUv.x * 0.13));
        color = screenBlend(color, latticeColor * electricLattice * (0.16 + beat * 0.08));
    }

    // Narrow moving light sheets retain the club identity without covering the view in polygons.
    vec2 beamDirectionA = normalize(vec2(0.72 + sin(modeClock * 0.11) * 0.20, 0.42));
    vec2 beamDirectionB = normalize(vec2(-0.38, 0.82 + cos(modeClock * 0.13) * 0.16));
    float beamA = pow(max(0.0, sin(dot(aspectCentered, beamDirectionA)
            * (34.0 + overload * 8.0) - modeClock * (1.8 + cocaineSynergy * 0.55))), 28.0);
    float beamB = pow(max(0.0, cos(dot(aspectCentered, beamDirectionB)
            * (41.0 + overload * 9.0) + modeClock * (1.35 + club * 0.40))), 32.0);
    float lightSheets = (beamA + beamB * veryHighDose) * highDose * laserMode * motionSafety
            * (0.08 + beatEnergy * 0.16 + club * 0.08 + cocaineSynergy * 0.09);
    vec3 laserColor = mix(vec3(0.04, 1.0, 0.92), vec3(1.0, 0.035, 0.70), beamB);
    color = screenBlend(color, laserColor * lightSheets);

    // At the top tiers, sparse ruptures briefly sample a rotated, differently scaled reality.
    float ruptureWave = sin(aspectCentered.x * (17.0 + overload * 3.0)
            + sin(aspectCentered.y * 13.0 - modeClock * 0.52) * 2.4 + modeClock * 0.74);
    float ruptureMask = smoothstep(0.82, 0.985, ruptureWave * 0.5 + 0.5) * extremeDose * motionSafety;
    vec2 rupturePoint = rotation(sin(modeClock * 0.27) * 0.11) * aspectCentered
            * (1.08 + sin(modeClock * 0.33) * 0.055);
    rupturePoint.x /= aspect;
    vec3 ruptureReality = scene(rupturePoint + 0.5 + slabShift * 0.45);
    float ruptureMix = ruptureMask * (0.08 + beatEnergy * 0.14 + tier * 0.08);
    color = mix(color, ruptureReality.brg * vec3(0.96, 1.04, 1.08), saturate(ruptureMix));

    float discontinuity = pow(abs(sin(warpedUv.y * (43.0 + overload * 8.0)
            + sin(warpedUv.x * 9.0 + modeClock) * 1.8)), 34.0);
    discontinuity *= veryHighDose * motionSafety * (0.08 + beatEnergy * 0.15);
    color = screenBlend(color, mix(vec3(0.10, 0.84, 1.0), vec3(0.96, 0.06, 0.74), warpedUv.x)
            * discontinuity);

    fragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
