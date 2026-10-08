#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform vec2 InSize;
uniform vec4 WarpA; // presence, warmth, color richness, time
uniform vec4 WarpB; // breathing, organic flow, tessellation, log recursive complexity
uniform vec4 WarpC; // symmetry, soft trails, spatial depth, afterglow
uniform vec4 MusicA; // bass expansion, harmonic shift, melodic flow, sustained note
uniform vec4 MusicB; // sax-like sustain, tavern, phrase phase, trip seed
uniform vec4 WarpD; // music sensitivity, trip seed, forest/organic setting, sunset/village setting
uniform vec4 MixA; // MDMA, 2C-B, LSD, mushrooms
uniform vec4 MixB; // cannabis, stimulant, alcohol, ketamine
uniform vec4 DoseA; // dose / 500mg, strong-dose gate, beyond-500 logarithm, trip progress
uniform vec4 Control; // photosensitivity reduction, motion reduction, visual, motion
uniform vec4 CameraA; // wrapped camera xyz, far plane
uniform vec4 CameraB; // near-plane forward xyz, near plane
uniform vec4 CameraC; // near-plane right xyz
uniform vec4 CameraD; // near-plane up xyz

out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.2831853;
const vec3 GOLD = vec3(0.96, 0.56, 0.12);
const vec3 TURQUOISE = vec3(0.05, 0.70, 0.67);
const vec3 DEEP_GREEN = vec3(0.035, 0.30, 0.12);
const vec3 MAGENTA = vec3(0.70, 0.07, 0.42);
const vec3 ORANGE = vec3(0.96, 0.31, 0.055);
const vec3 VIOLET = vec3(0.37, 0.10, 0.58);
const vec3 INDIGO = vec3(0.055, 0.07, 0.27);

float sat(float v) { return clamp(v, 0.0, 1.0); }
float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
float hash21(vec2 p) { vec3 q = fract(vec3(p.xyx) * 0.1031); q += dot(q, q.yzx + 33.33); return fract((q.x + q.y) * q.z); }
mat2 rot(float a) { float s = sin(a), c = cos(a); return mat2(c, -s, s, c); }
vec3 scene(vec2 uv) { return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb; }
float depthAt(vec2 uv) { return texture(DepthSampler, clamp(uv, vec2(0.001), vec2(0.999))).r; }
vec3 screenBlend(vec3 a, vec3 b) { return 1.0 - (1.0 - clamp(a, 0.0, 1.0)) * (1.0 - clamp(b, 0.0, 0.92)); }

// A center-heavy reconstruction tap catches sub-pixel crawl without turning every
// displaced scene sample into a soft-focus copy of the world.
vec3 cleanScene(vec2 uv, float radius) {
    vec2 px = radius / max(InSize, vec2(1.0));
    return scene(uv) * 0.76
            + (scene(uv + vec2(px.x, 0.0)) + scene(uv - vec2(px.x, 0.0))
            + scene(uv + vec2(0.0, px.y)) + scene(uv - vec2(0.0, px.y))) * 0.06;
}

float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float result = 0.0;
    float amplitude = 0.54;
    float footprint = max(length(dFdx(p)), length(dFdy(p)));
    mat2 octaveTurn = rot(0.71);
    for (int i = 0; i < 5; i++) {
        // Fade an octave before floor/fract once its cells approach pixel size.
        // Replacing it with its mean preserves the field's energy without crawl.
        float octaveClarity = 1.0 - smoothstep(0.055, 0.34, footprint);
        result += mix(0.50, valueNoise(p), octaveClarity) * amplitude;
        p = octaveTurn * p * 1.93 + vec2(7.13, -4.71);
        footprint *= 1.93;
        amplitude *= 0.49;
    }
    return result;
}

vec3 mescalinePalette(float t) {
    float band = fract(t) * 6.0;
    if (band < 1.0) return mix(GOLD, TURQUOISE, band);
    if (band < 2.0) return mix(TURQUOISE, DEEP_GREEN, band - 1.0);
    if (band < 3.0) return mix(DEEP_GREEN, MAGENTA, band - 2.0);
    if (band < 4.0) return mix(MAGENTA, VIOLET, band - 3.0);
    if (band < 5.0) return mix(VIOLET, INDIGO, band - 4.0);
    return mix(INDIGO, ORANGE, band - 5.0);
}

vec3 preserveLuma(vec3 color, vec3 reference, float amount) {
    float target = luma(reference);
    float current = max(0.001, luma(color));
    return mix(color, color * (target / current), sat(amount));
}

float triangleLattice(vec2 p) {
    p.x += p.y * 0.57735027;
    vec2 cell = abs(fract(p) - 0.5);
    float d = min(cell.x, min(cell.y, abs(fract(p.x + p.y) - 0.5)));
    float aa = clamp(fwidth(d) * 1.35, 0.0035, 0.052);
    return 1.0 - smoothstep(0.018 - aa, 0.075 + aa, d);
}

float mandala(vec2 p, float petals, float phase) {
    float r = length(p);
    float a = atan(p.y, p.x);
    float radial = sin(r * (28.0 + petals) - phase + sin(a * petals) * 2.1);
    float petalsField = cos(a * petals + sin(r * 13.0 - phase * 0.42));
    float signal = abs(radial * petalsField);
    float aa = clamp(fwidth(signal) * 0.72, 0.004, 0.18);
    return smoothstep(0.54 - aa, 0.94 + aa, signal);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec4 source = texture(DiffuseSampler, uv);
    float presence = sat(WarpA.x);
    if (presence <= 0.0001) { fragColor = source; return; }

    float warmth = max(0.0, WarpA.y);
    float richness = max(0.0, WarpA.z);
    float time = WarpA.w;
    float breathing = max(0.0, WarpB.x);
    float organic = max(0.0, WarpB.y);
    float tessellation = max(0.0, WarpB.z);
    float complexity = max(0.0, WarpB.w);
    float symmetry = max(0.0, WarpC.x);
    float trails = max(0.0, WarpC.y);
    float spatialDepth = max(0.0, WarpC.z);
    float afterglow = max(0.0, WarpC.w);
    float mdma = sat(MixA.x), twoCb = sat(MixA.y), lsd = sat(MixA.z), mushrooms = sat(MixA.w);
    float cannabis = sat(MixB.x), stimulant = sat(MixB.y), alcohol = sat(MixB.z), ketamine = sat(MixB.w);
    float photosensitive = sat(Control.x), motionReduction = sat(Control.y);
    float tripProgress = sat(DoseA.w);
    // Dose determines the eventual scale, but progress controls its arrival. This
    // prevents a 500 mg trip from looking fully developed on its first rendered frame.
    float onset = smoothstep(0.025, 0.40, tripProgress);
    float shapeArrival = smoothstep(0.16, 0.52, tripProgress);
    float doseLevel = max(0.0, DoseA.x) * onset;
    float strongDose = sat(DoseA.y) * onset;
    float beyondDose = max(0.0, DoseA.z) * shapeArrival;
    float musicSensitivity = sat(WarpD.x);
    float forestSetting = sat(WarpD.z);
    float sunsetVillage = sat(WarpD.w);
    float speed = (1.0 + stimulant * 0.85) * (1.0 - cannabis * 0.38);
    float motionSafety = (1.0 - motionReduction * 0.72) * max(0.0, Control.w);
    float slowTime = time * 0.0042 * speed;
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 centered = (uv - 0.5) * vec2(aspect, 1.0);

    float depth = depthAt(uv);
    float depthLeft = depthAt(uv - vec2(texel.x * 1.35, 0.0));
    float depthRight = depthAt(uv + vec2(texel.x * 1.35, 0.0));
    float depthDown = depthAt(uv - vec2(0.0, texel.y * 1.35));
    float depthUp = depthAt(uv + vec2(0.0, texel.y * 1.35));
    float neighborDepth = min(min(depthLeft, depthRight), min(depthDown, depthUp));
    // Alpha-cutout leaves often alternate between geometry depth and the clear value
    // on adjacent pixels. Fill only those tiny holes for pattern anchoring so foliage
    // reads as one living surface instead of a field of stippled screen pixels.
    float clearDepth = step(0.999995, depth);
    float neighboringSurface = 1.0 - step(0.999995, neighborDepth);
    float cutoutHole = clearDepth * neighboringSurface;
    float stableDepth = mix(depth, neighborDepth, cutoutHole * 0.94);
    // Depth is nonlinear: treating values near one as a gradual validity mask made
    // real geometry beyond a few dozen blocks drift toward an invented distance.
    // A binary clear-buffer test keeps every valid surface at its reconstructed depth.
    float worldMask = 1.0 - step(0.999995, stableDepth);
    float skyMask = (1.0 - worldMask) * smoothstep(0.18, 0.76, uv.y);
    float depthEdge = abs(depthRight - depthLeft) + abs(depthUp - depthDown);
    // Some renderer/driver combinations leave the sampled depth buffer at its clear
    // value for this stage. Keep a conservative screen-space surface fallback so the
    // trip cannot silently degrade to "no effect" on those clients.
    float surfaceFallback = (1.0 - skyMask) * 0.24;
    float surface = sat(max(worldMask, surfaceFallback));

    // Reconstruct a stable world-space anchor. Patterns now cling to blocks and terrain
    // while the camera moves, instead of reading as a flat filter stuck to the monitor.
    vec2 screenNdc = uv * 2.0 - 1.0;
    vec3 worldRay = normalize(CameraB.xyz + CameraC.xyz * screenNdc.x + CameraD.xyz * screenNdc.y);
    vec3 cameraForward = normalize(CameraB.xyz);
    float nearPlane = max(0.001, CameraB.w);
    float farPlane = max(nearPlane + 1.0, CameraA.w);
    float linearDepth = (nearPlane * farPlane) / max(nearPlane, farPlane - stableDepth * (farPlane - nearPlane));
    float rayForward = max(0.16, dot(worldRay, cameraForward));
    float depthDistance = clamp(linearDepth / rayForward, 1.0, farPlane);
    float anchorDistance = mix(96.0 + spatialDepth * 48.0, depthDistance, worldMask);
    vec3 worldAnchor = CameraA.xyz + worldRay * anchorDistance;
    // This fixed oblique projection is continuous in all three world dimensions.
    // Averaging three unrelated triplanar coordinates caused cancellation whenever
    // the estimated normal changed across foliage, corners, or distant silhouettes.
    vec2 stableProjection = vec2(
            dot(worldAnchor, vec3(0.064, 0.019, 0.031)),
            dot(worldAnchor, vec3(-0.023, 0.047, 0.058))
    );
    float projectionStability = 1.0 - smoothstep(0.0015, 0.045, depthEdge);
    vec2 worldUv = stableProjection;
    worldUv += vec2(WarpD.y * 3.7, -WarpD.y * 2.9);
    // World-space derivatives tell us when a procedural feature is smaller than a
    // screen pixel. Only those fine layers fade with distance; the captured world
    // and its broad breathing displacement stay sharp and fully animated.
    float worldFootprint = max(length(dFdx(worldUv)), length(dFdy(worldUv)));
    float detailStability = 1.0 - smoothstep(0.010, 0.070, worldFootprint);
    detailStability *= mix(0.72, 1.0, projectionStability);

    float livingA = fbm(worldUv * (1.10 + complexity * 0.10) + vec2(slowTime * 0.10, -slowTime * 0.075));
    float livingB = fbm(rot(1.37) * worldUv * (1.36 + complexity * 0.08) + vec2(-slowTime * 0.065, slowTime * 0.085));
    float livingC = fbm(worldUv * 0.48 + vec2(livingA, livingB) * 0.92 - slowTime * 0.025);
    vec2 livingFlowRaw = vec2(livingA - livingB, livingC - 0.48);
    vec2 livingFlow = livingFlowRaw / sqrt(dot(livingFlowRaw, livingFlowRaw) + 0.045);
    float livingPulse = sin((livingA * 0.58 + livingB * 0.42) * TAU * (2.0 + complexity * 0.32)
            + slowTime * 0.72) * 0.5 + 0.5;
    float deformation = sat(presence * (0.28 + strongDose * 0.88) + beyondDose * 0.16);
    float doseWarpScale = min(2.85, 0.70 + doseLevel * 0.68 + beyondDose * 0.30);
    float materialWarpScale = min(2.35, 0.78 + doseLevel * 0.44 + beyondDose * 0.22);

    float breathWave = sin(slowTime * 1.17 + length(centered) * 7.0 + sin(centered.y * 5.0) * 0.45);
    float bassExpansion = MusicA.x * MusicB.y * musicSensitivity;
    float expansion = breathing * breathWave * (0.007 + strongDose * 0.015)
            + bassExpansion * WarpC.z * (0.008 + strongDose * 0.010);
    expansion *= surface * motionSafety * (0.62 + doseWarpScale * 0.47);
    vec2 warpedUv = (centered * (1.0 - expansion * (0.65 + length(centered)))) / vec2(aspect, 1.0) + 0.5;
    float organicWave = sin(warpedUv.y * 16.0 + slowTime * 1.21 + sin(warpedUv.x * 11.0 - slowTime) * 1.8);
    vec2 depthSlope = vec2(
            depthAt(uv - vec2(texel.x * 2.0, 0.0)) - depthAt(uv + vec2(texel.x * 2.0, 0.0)),
            depthAt(uv - vec2(0.0, texel.y * 2.0)) - depthAt(uv + vec2(0.0, texel.y * 2.0))
    );
    float edgeStability = 1.0 - smoothstep(0.012, 0.22, depthEdge);
    depthSlope = clamp(depthSlope * 7.0, vec2(-1.0), vec2(1.0));
    vec2 depthFlowRaw = depthSlope * (0.52 + edgeStability * 0.48) + livingFlow * 0.62;
    vec2 depthFlow = depthFlowRaw / sqrt(dot(depthFlowRaw, depthFlowRaw) + 0.040);
    vec2 worldWarp = (livingFlow * (livingPulse - 0.42) + depthFlow * (livingC - 0.48) * 0.72)
            * deformation * surface * (0.005 + strongDose * 0.030 + min(0.048, beyondDose * 0.015))
            * doseWarpScale * mix(0.38, 1.0, edgeStability) * motionSafety;
    // A broad coherent current makes the geometry of the actual scene inhale and
    // travel as one surface. It replaces the old screen-centered kaleidoscope as
    // the main high-dose shape change, so stronger motion does not cost legibility.
    float coherentBreath = sin(worldAnchor.y * 0.090 + livingC * 2.7
            + worldAnchor.z * 0.026 - slowTime * 0.41);
    vec2 coherentDirectionRaw = livingFlow * 0.74
            + vec2(sin(worldAnchor.z * 0.032 - slowTime * 0.29),
                    cos(worldAnchor.x * 0.029 + slowTime * 0.25)) * 0.36;
    vec2 coherentDirection = coherentDirectionRaw
            / sqrt(dot(coherentDirectionRaw, coherentDirectionRaw) + 0.055);
    worldWarp += coherentDirection * coherentBreath * deformation * surface
            * (0.004 + strongDose * 0.021 + min(0.026, beyondDose * 0.009))
            * (0.72 + doseWarpScale * 0.22) * mix(0.54, 1.0, edgeStability) * motionSafety;
    worldWarp += vec2(organicWave, sin(warpedUv.x * 13.0 - slowTime * 0.83))
            * organic * surface * (0.0035 + strongDose * 0.0065) * materialWarpScale * motionSafety;
    warpedUv += worldWarp;
    if (ketamine > 0.01) {
        vec2 detached = warpedUv + normalize(centered + vec2(0.0001)) * (depth - 0.62) * ketamine * spatialDepth * 0.013 * motionSafety;
        warpedUv = mix(warpedUv, detached, 0.72);
    }

    vec3 probe = scene(warpedUv);
    vec3 classifier = (probe * 2.0 + scene(warpedUv + vec2(texel.x * 1.5, 0.0))
            + scene(warpedUv - vec2(texel.x * 1.5, 0.0))
            + scene(warpedUv + vec2(0.0, texel.y * 1.5))
            + scene(warpedUv - vec2(0.0, texel.y * 1.5))) / 6.0;
    float probeLum = luma(classifier);
    float maximum = max(classifier.r, max(classifier.g, classifier.b));
    float minimum = min(classifier.r, min(classifier.g, classifier.b));
    float saturation = maximum - minimum;
    float greenMask = sat((classifier.g - max(classifier.r, classifier.b) * 0.78) * 4.4 + 0.20) * surface;
    float woodMask = sat((classifier.r - classifier.b) * 2.2 + (classifier.g - classifier.b) * 0.8)
            * surface * (1.0 - greenMask * 0.6);
    float stoneMask = (1.0 - smoothstep(0.10, 0.34, saturation)) * smoothstep(0.12, 0.78, probeLum) * surface;
    float lightMask = smoothstep(0.58, 0.96, probeLum);

    // Material-specific coordinate motion actually reshapes the captured world.
    vec2 vegetationWarp = vec2(
            sin(worldAnchor.z * 0.22 + livingA * 5.0 + slowTime * 0.55),
            cos(worldAnchor.x * 0.18 + livingB * 4.2 - slowTime * 0.42)
    ) * greenMask * (0.003 + strongDose * 0.015);
    vec2 grainDirection = normalize(vec2(0.82, 0.28) + livingFlow * 0.34);
    vec2 woodWarp = grainDirection * sin((worldUv.x + worldUv.y * 0.31) * 31.0 + livingC * 5.0 - slowTime * 0.46)
            * woodMask * (0.002 + strongDose * 0.010);
    float stoneScale = 5.0 + complexity * 0.75;
    float surfacePatternStability = 1.0 - smoothstep(0.035, 0.22, worldFootprint * stoneScale);
    float grainStability = 1.0 - smoothstep(0.030, 0.20, worldFootprint * 31.0);
    vegetationWarp *= surfacePatternStability;
    woodWarp *= grainStability;
    float stoneCell = triangleLattice(worldUv * stoneScale);
    float stableStoneCell = mix(0.40, stoneCell, surfacePatternStability);
    vec2 stoneWarp = depthFlow * (stableStoneCell - 0.36) * stoneMask * (0.002 + strongDose * 0.013);
    vec2 materialUv = warpedUv + (vegetationWarp + woodWarp + stoneWarp)
            * deformation * materialWarpScale * motionSafety * (1.0 + mushrooms * 0.22 + stimulant * 0.16);
    vec3 materialScene = cleanScene(materialUv, 0.14 + strongDose * 0.10);
    vec3 base = mix(probe, materialScene, sat(0.34 + strongDose * 0.54 + beyondDose * 0.08));
    float lum = luma(base);

    // Pigment lives inside the moving material fields. It preserves scene luminance,
    // avoiding the cheap appearance of a uniform warm/saturation overlay.
    float palettePhase = livingA * 0.24 + livingB * 0.17 + livingC * 0.13
            + slowTime * (0.012 + strongDose * 0.010) + MusicA.y * musicSensitivity * 0.08;
    vec3 livingPigment = mescalinePalette(palettePhase);
    livingPigment = mix(livingPigment, mix(DEEP_GREEN, TURQUOISE, livingPulse), greenMask * 0.64);
    livingPigment = mix(livingPigment, mix(ORANGE, GOLD, livingA), woodMask * 0.58);
    livingPigment = mix(livingPigment, mix(INDIGO, VIOLET, stableStoneCell), stoneMask * 0.48);
    vec3 pigmented = preserveLuma(mix(base, livingPigment * (0.48 + lum * 0.96),
            0.34 + strongDose * 0.42 + min(0.20, beyondDose * 0.08)), base, 0.70);
    float pigmentMask = surface * presence * (0.12 + richness * 0.22 + strongDose * 0.44)
            * sat(0.30 + livingPulse * 0.50 + greenMask * 0.12 + woodMask * 0.10 + stoneMask * stableStoneCell * 0.18);
    vec3 graded = mix(base, pigmented, sat(pigmentMask));
    graded = mix(graded, preserveLuma(graded * vec3(1.13, 1.035, 0.91), base, 0.82),
            lightMask * warmth * (0.10 + strongDose * 0.12));

    vec2 p = centered;
    float phrase = MusicB.z * TAU;
    float nested = 0.0;
    float weight = 0.0;
    // Geometry is built in repeating world-space cells. The former screen-centred
    // triangle/kaleidoscope construction produced giant colour-changing X shapes
    // and duplicated distant scenery; these cells stay attached to real surfaces.
    float patternScale = 1.45 + complexity * 0.12;
    float primaryPatternStability = 1.0 - smoothstep(0.006, 0.030, worldFootprint * patternScale);
    vec2 patternUv = worldUv * patternScale;
    vec2 primaryCell = fract(patternUv) - 0.5;
    primaryCell = rot((WarpD.y - 0.5) * 0.72) * primaryCell;
    vec2 q = patternUv;
    float octaveScale = 1.0;
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        float octaveGate = smoothstep(fi * 0.43 - 0.18, fi * 0.43 + 0.32, complexity + 0.22);
        // q is folded below, so measure the projected footprint here, before
        // fract/mandala operations hide how compressed the cell really is.
        float octaveFootprint = worldFootprint * patternScale * octaveScale;
        float octaveStability = 1.0 - smoothstep(0.006, 0.030, octaveFootprint);
        octaveGate *= octaveStability;
        vec2 localCell = fract(q) - 0.5;
        localCell = rot((WarpD.y - 0.5) * 0.86 + fi * 0.11) * localCell;
        float petals = 5.0 + mod(floor(complexity * 1.8 + fi * 3.0 + WarpD.y * 7.0), 13.0);
        float radial = mandala(localCell * (1.55 + fi * 0.17), petals,
                slowTime * (0.30 + fi * 0.030) + phrase + fi * 1.71);
        float rings = sin(length(localCell) * (22.0 + fi * 2.6)
                - slowTime * (0.24 + fi * 0.018) + livingC * 2.4) * 0.5 + 0.5;
        float layer = mix(rings, radial, sat(0.38 + symmetry * 0.42 + fi * 0.035));
        nested += layer * octaveGate / (1.0 + fi * 0.46);
        weight += octaveGate / (1.0 + fi * 0.46);
        q = rot(0.29 + fi * 0.055) * q * 1.57 + vec2(7.3 + fi * 1.9, -4.1 + fi * 1.3);
        octaveScale *= 1.57;
    }
    nested /= max(0.001, weight);

    // Broad chromatic strata arrive with the opening and remain tied to living
    // world coordinates. Every hue is drawn from the mescaline palette; this is
    // material coloration, not a screen-wide RGB cycle.
    float colorStrata = sin((nested * 0.52 + livingA * 0.34 + livingC * 0.28) * TAU
            + slowTime * 0.19) * 0.5 + 0.5;
    vec3 strataPalette = mescalinePalette(palettePhase + colorStrata * 0.46
            + livingB * 0.20 + tripProgress * 0.10);
    vec3 strataColor = preserveLuma(mix(graded, strataPalette * (0.52 + lum * 0.98),
            0.28 + strongDose * 0.38 + min(0.18, beyondDose * 0.07)), graded, 0.68);
    float strataMask = surface * presence * shapeArrival
            * sat(0.12 + strongDose * 0.34 + beyondDose * 0.08)
            * smoothstep(0.16, 0.84, colorStrata * 0.62 + livingPulse * 0.38);
    graded = mix(graded, strataColor, sat(strataMask));

    // Strong doses refract one nearby copy of the real surface. Avoiding opposing
    // displaced copies prevents distant blocks and foliage from becoming fuzzy.
    float realityStrength = sat(presence * (strongDose * 0.82 + min(0.50, beyondDose * 0.16))
            * (0.58 + spatialDepth * 0.34));
    float sheetBand = sin(worldAnchor.y * (0.54 + complexity * 0.035) + livingA * 5.2
            - slowTime * 0.48) * 0.5 + 0.5;
    float sheetMask = surface * smoothstep(0.22, 0.84, sheetBand * 0.58 + nested * 0.42);
    vec2 sheetOffset = (depthFlow * (sheetBand - 0.46) + livingFlow * (livingB - 0.48))
            * (0.006 + strongDose * 0.020 + min(0.026, beyondDose * 0.009))
            * (0.78 + doseWarpScale * 0.20) * motionSafety;
    vec3 sheetScene = scene(materialUv + sheetOffset);

    // Slow petal refraction now grows inside local surface cells instead of radiating
    // from the middle of the monitor. The visible blocks bend with the rosette.
    float localAngle = atan(primaryCell.y, primaryCell.x);
    float localRadius = length(primaryCell);
    vec2 radialDirection = primaryCell / sqrt(dot(primaryCell, primaryCell) + 0.014);
    vec2 tangentDirection = vec2(-radialDirection.y, radialDirection.x);
    float rosettePetals = 5.0 + mod(floor(complexity * 1.6 + WarpD.y * 11.0), 8.0);
    float rosetteWave = sin(localAngle * rosettePetals + localRadius * (18.0 + complexity * 1.4)
            + livingC * 3.1 - slowTime * 0.44);
    vec2 rosetteOffset = (radialDirection * rosetteWave
            + tangentDirection * cos(localAngle * (rosettePetals - 1.0) - slowTime * 0.31) * 0.52)
            * surface * shapeArrival * (0.003 + strongDose * 0.020 + min(0.030, beyondDose * 0.011))
            * motionSafety * primaryPatternStability;
    vec3 rosetteScene = scene(materialUv + rosetteOffset);
    vec3 rosettePigment = mescalinePalette(palettePhase + rosetteWave * 0.16 + localRadius * 0.18);
    vec3 coloredRosette = preserveLuma(mix(rosetteScene, rosettePigment * (0.48 + luma(rosetteScene)),
            0.20 + strongDose * 0.34), base, 0.58);
    float rosetteMask = surface * presence * shapeArrival
            * smoothstep(0.20, 0.82, nested * 0.42 + livingPulse * 0.32 + sheetBand * 0.26)
            * sat(0.16 + strongDose * 0.46 + beyondDose * 0.10)
            * primaryPatternStability;
    graded = mix(graded, coloredRosette, sat(rosetteMask * (0.24 + strongDose * 0.24)));

    // Recursive complexity remains open-ended, but recursion alters nearby surface
    // coordinates instead of zooming and mirroring the full framebuffer.
    float recursionSignal = sin((nested * 0.72 + livingA * 0.38 + livingB * 0.24) * TAU
            + slowTime * 0.23) * 0.5 + 0.5;
    vec2 recursiveOffset = (rot(0.36 + livingB * 0.22) * livingFlow * (recursionSignal - 0.48)
            + radialDirection * (nested - 0.50) * 0.62)
            * surface * (0.004 + strongDose * 0.017 + min(0.025, beyondDose * 0.009))
            * motionSafety * primaryPatternStability;
    vec3 recursiveScene = scene(materialUv + recursiveOffset);
    float recursiveMask = surface * smoothstep(0.36, 0.86, nested * 0.62 + stableStoneCell * 0.18 + livingA * 0.20)
            * sat(strongDose * 0.56 + beyondDose * 0.16) * (1.0 - photosensitive * 0.28)
            * primaryPatternStability;

    float sheetMix = sheetMask * realityStrength * (0.14 + strongDose * 0.18);
    float recursiveMix = recursiveMask * presence * (0.10 + strongDose * 0.18 + min(0.12, beyondDose * 0.04));
    graded = mix(graded, preserveLuma(sheetScene, base, 0.36), sat(sheetMix));
    graded = mix(graded, preserveLuma(recursiveScene, base, 0.22), sat(recursiveMix));

    float crispness = 1.0 + twoCb * 0.72 + lsd * 0.48 + stimulant * 0.38 - alcohol * 0.34;
    float geometryAa = clamp(fwidth(nested) * 0.68, 0.008, 0.16);
    float geometryLines = smoothstep(0.50 + 0.10 / crispness - geometryAa,
            0.91 + geometryAa, nested) * surface;
    float patternStrength = sat((tessellation * 0.52 + complexity * 0.20 + symmetry * 0.30 + strongDose * 0.16)
            * (1.0 - photosensitive * 0.54)) * geometryLines * mix(0.18, 1.0, detailStability);
    vec3 mineralPalette = mescalinePalette(palettePhase + nested * (0.16 + strongDose * 0.32)
            + sheetBand * 0.12 + tripProgress * 0.08);
    mineralPalette = mix(mineralPalette, mix(MAGENTA, VIOLET, nested), sat(complexity * 0.08 + lsd * 0.25));
    mineralPalette = mix(mineralPalette, ORANGE, woodMask * 0.34);
    vec3 geometryPigment = preserveLuma(mineralPalette * (0.62 + lum * 0.92), graded, 0.50 - twoCb * 0.16);
    graded = mix(graded, geometryPigment,
            sat(patternStrength * (0.42 + strongDose * 0.46 + min(0.20, beyondDose * 0.07))));
    graded = screenBlend(graded, mineralPalette * patternStrength * (twoCb * 0.12 + lsd * 0.08));

    // Flowing grain, coherent vegetation waves and stone glyphs change local material,
    // preserving texture relief instead of painting translucent color over everything.
    float leafRhythm = sin(worldAnchor.x * 0.31 + worldAnchor.z * 0.24 + livingA * 4.8 + slowTime * 0.72) * 0.5 + 0.5;
    float woodGrain = sin((worldUv.x + worldUv.y * 0.31) * 44.0 + livingB * 3.2 - slowTime * 0.51) * 0.5 + 0.5;
    float glyphScale = 5.8 + complexity * 0.82;
    float glyphStability = 1.0 - smoothstep(0.035, 0.22, worldFootprint * glyphScale);
    float stoneGlyph = mix(0.40, triangleLattice(worldUv * glyphScale), glyphStability);
    vec3 leafColor = preserveLuma(mix(graded, mix(DEEP_GREEN, TURQUOISE, leafRhythm), 0.48), graded, 0.86);
    vec3 woodColor = preserveLuma(mix(graded, mix(ORANGE, GOLD, woodGrain), 0.46), graded, 0.82);
    vec3 stoneColor = preserveLuma(mix(graded, mix(INDIGO, TURQUOISE, stoneGlyph), 0.52), graded, 0.76);
    graded = mix(graded, leafColor, sat(greenMask * leafRhythm * organic
            * (0.24 + strongDose * 0.36 + forestSetting * 0.08) * surfacePatternStability));
    graded = mix(graded, woodColor, sat(woodMask * woodGrain * organic
            * (0.20 + strongDose * 0.32) * grainStability));
    graded = mix(graded, stoneColor, sat(stoneMask * stoneGlyph * tessellation
            * (0.22 + strongDose * 0.38) * glyphStability));

    // Clouds and sky gain slow layered depth; sustained phrases draw ribbons rather than flashes.
    float skyLayer = sin(p.x * 10.0 + sin(p.y * 8.0 - slowTime * 0.45) * 1.9 + slowTime * 0.31) * 0.5 + 0.5;
    float skyLayer2 = cos(p.y * 13.0 + sin(p.x * 6.0 + slowTime * 0.24) * 2.3 - slowTime * 0.28) * 0.5 + 0.5;
    vec3 skyColor = mix(INDIGO, TURQUOISE, skyLayer);
    skyColor = mix(skyColor, mix(GOLD, VIOLET, MusicA.y), skyLayer2 * 0.46);
    vec2 skyWarpUvA = materialUv + vec2(sin(p.y * 7.0 + slowTime * 0.31), cos(p.x * 5.0 - slowTime * 0.24))
            * skyMask * presence * (0.004 + strongDose * 0.022) * motionSafety;
    vec2 skyWarpUvB = materialUv + rot(1.20) * (skyWarpUvA - materialUv) * 1.70;
    vec3 breathingSky = mix(cleanScene(skyWarpUvA, 0.16 + strongDose * 0.10),
            cleanScene(skyWarpUvB, 0.16 + strongDose * 0.10), skyLayer2 * (0.18 + strongDose * 0.18));
    breathingSky = preserveLuma(mix(breathingSky, skyColor * (0.48 + luma(breathingSky)),
            0.12 + strongDose * 0.30), base, 0.58);
    graded = mix(graded, breathingSky,
            skyMask * sat(spatialDepth * 0.34 + complexity * 0.055 + strongDose * 0.30));

    float sustain = MusicA.w * MusicB.y * musicSensitivity;
    float sax = MusicB.x * MusicB.y * musicSensitivity;
    float ribbonA = exp(-abs(sin(p.y * (5.0 + complexity * 0.35) + sin(p.x * 6.0 + phrase) * 1.6
            - slowTime * (0.35 + MusicA.z * 0.28))) * 10.0);
    float ribbonB = exp(-abs(cos(dot(p, normalize(vec2(0.72, 0.42))) * (12.0 + complexity)
            + slowTime * 0.27 - phrase)) * 13.0);
    float ribbonStrength = (ribbonA * sustain + ribbonB * sax) * WarpC.x * (0.08 + sax * 0.15)
            * (1.0 - photosensitive * 0.42);
    graded = screenBlend(graded, mix(GOLD, MAGENTA, ribbonB) * ribbonStrength);
    graded = screenBlend(graded, mix(GOLD, ORANGE, sat(MusicA.y)) * sunsetVillage
            * sat(warmth) * (0.018 + MusicB.y * musicSensitivity * 0.025));

    // Soft light halos and directional echoes stay bounded regardless of accumulated dose.
    vec3 halo = (scene(warpedUv + texel * vec2(3.0, 0.0)) + scene(warpedUv - texel * vec2(3.0, 0.0))
            + scene(warpedUv + texel * vec2(0.0, 3.0)) + scene(warpedUv - texel * vec2(0.0, 3.0))) * 0.25;
    float haloGate = smoothstep(0.56, 0.93, luma(halo));
    graded = screenBlend(graded, mix(GOLD, TURQUOISE, MusicA.y) * haloGate * sat(warmth * 0.16 + mdma * 0.09));
    vec2 trailDirection = normalize(vec2(0.7 + sin(slowTime) * 0.2, 0.45 + cos(slowTime * 0.73) * 0.16));
    trailDirection *= texel * (2.0 + 7.0 * sat(trails)) * (1.0 + cannabis * 0.45);
    vec3 echo = scene(warpedUv - trailDirection) * 0.58 + scene(warpedUv - trailDirection * 2.0) * 0.28
            + scene(warpedUv - trailDirection * 3.0) * 0.14;
    float trailGate = smoothstep(0.035, 0.30, length(base - echo));
    graded = mix(graded, mix(echo, echo * mix(GOLD, TURQUOISE, 0.55), 0.16),
            trailGate * sat(trails * 0.16 + sax * MusicB.y * 0.075) * (1.0 - motionReduction * 0.58));

    // Restore high-resolution texture and silhouette information from the same
    // displaced world coordinate. This is distance-aware reconstruction, not a
    // sharpen filter, so it cannot add ringing or pixel noise.
    vec3 sharpWorld = scene(materialUv);
    float sourceEdge = sat((length(dFdx(base)) + length(dFdy(base))) * 1.65);
    float distanceRisk = smoothstep(0.018, 0.075, worldFootprint);
    float patternRisk = smoothstep(0.085, 0.24, fwidth(nested));
    float crispRestore = surface * sat(max(distanceRisk, patternRisk * 0.68)
            * (0.52 + strongDose * 0.22) + sourceEdge * (0.08 + strongDose * 0.08));
    vec3 crispColoredWorld = preserveLuma(mix(sharpWorld, graded, 0.30), sharpWorld, 0.82);
    graded = mix(graded, crispColoredWorld, crispRestore);

    // Alcohol softens clarity, ketamine loosens depth anchoring; neither erases mescaline's palette.
    graded = mix(graded, mix(graded, halo, 0.18), alcohol * 0.10);
    graded = mix(graded, graded.brg * vec3(0.91, 1.04, 1.02), ketamine * sat(spatialDepth) * 0.045);
    graded = mix(graded, graded * vec3(1.035, 1.01, 0.985), sat(afterglow) * 0.22);
    fragColor = vec4(clamp(graded, 0.0, 1.0), source.a);
}
