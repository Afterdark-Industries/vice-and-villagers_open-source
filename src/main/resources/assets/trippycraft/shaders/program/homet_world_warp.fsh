#version 150

// Native world reconstruction is identical for desktop and asymmetric stereo eyes.
// The compositor supplies each view matrix, but freezes the artistic field per frame.
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform mat4 WorldViewProjection;
uniform mat4 WorldInverseViewProjection;
uniform vec4 WorldEyePosition;
uniform vec4 WorldHeadPosition;
uniform vec4 WarpA; // presence, log2(1+depth), log2(1+deformation), time (ticks)
uniform vec4 WarpB; // log2(1+recursion/color/perspective), log(1+complexity)
uniform vec4 WarpC; // music sensitivity, seed, observation, trails
uniform vec4 Intensity; // eased 40->50mg gate, log dose above40, log active intensity, integrated color travel
uniform vec4 MusicA; // bass, melody, shimmer, phrase
uniform vec4 MotionA; // world velocity, motion scale
uniform vec4 MotionB; // world view-direction velocity, color scale
uniform vec4 Focus; // observed world position and dwell
uniform vec4 MixA; // LSD, psilocybin, mescaline, 2C-B
uniform vec4 Control; // photosensitivity scale, motion scale, vegetation, enclosed setting

out vec4 fragColor;
const float TAU = 6.28318530718;

float nonzero(float value) {
    return abs(value) < 0.000001 ? (value < 0.0 ? -0.000001 : 0.000001) : value;
}

vec3 worldAt(vec2 uv, float depth) {
    vec4 clip = WorldInverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return WorldEyePosition.xyz + clip.xyz / nonzero(clip.w);
}

vec2 projectWorld(vec3 position) {
    vec4 clip = WorldViewProjection * vec4(position - WorldEyePosition.xyz, 1.0);
    return clip.xy / nonzero(clip.w) * 0.5 + 0.5;
}

float line(float distance, float width, float footprint) {
    return 1.0 - smoothstep(width, width + max(0.001, footprint), distance);
}

vec3 crystal(float phase) {
    // Cyan/blue/violet/magenta dominate; emerald and ember/gold appear inside facets.
    vec3 color = 0.52 + 0.48 * cos(TAU * (phase + vec3(0.0, 0.34, 0.64)));
    float cool = 0.5 + 0.5 * sin(phase * TAU * 0.5);
    return mix(color, color.brg, cool * 0.38);
}

float structureWave(vec3 p, float seconds) {
    // Integral frequencies divide the floating origin's 4096-block period.
    // All faces and neighboring structures participate in the same slow wave.
    float broad = sin(TAU * (dot(p, vec3(1.0, 0.0, 1.0)) / 64.0 - seconds / 30.0));
    float crossWave = sin(TAU * (dot(p, vec3(1.0, 1.0, -1.0)) / 32.0 + seconds / 40.0));
    float longWave = sin(TAU * (dot(p, vec3(-1.0, 1.0, 2.0)) / 128.0 - seconds / 60.0));
    return broad * 0.48 + crossWave * 0.30 + longWave * 0.22;
}

vec2 safeSource(vec2 original, vec3 source, vec3 surface, vec3 normal) {
    vec2 candidate = projectWorld(source);
    vec2 margin = 1.5 / max(OutSize, vec2(1.0));
    float inside = step(margin.x, candidate.x) * step(candidate.x, 1.0 - margin.x)
                 * step(margin.y, candidate.y) * step(candidate.y, 1.0 - margin.y);
    candidate = clamp(candidate, margin, vec2(1.0) - margin);
    float raw = texture(DepthSampler, candidate).r;
    vec3 sampled = worldAt(candidate, raw);
    float distance = length(surface - WorldEyePosition.xyz);
    // Never pull a foreground silhouette or the sky across a receding surface.
    float sameFace = 1.0 - smoothstep(0.055 + distance * 0.002, 0.30 + distance * 0.006,
                                    abs(dot(sampled - surface, normal)));
    float valid = inside * sameFace * (1.0 - step(0.999995, raw));
    return mix(original, candidate, valid);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 original = texture(DiffuseSampler, uv);
    float rawDepth = texture(DepthSampler, uv).r;
    if (WarpA.x <= 0.0001 || rawDepth >= 0.999995) {
        fragColor = original;
        return;
    }
    vec3 world = worldAt(uv, rawDepth);
    vec3 dx = dFdx(world);
    vec3 dy = dFdy(world);
    vec3 crossNormal = cross(dx, dy);
    float normalLength = length(crossNormal);
    vec3 normal = crossNormal / max(normalLength, 0.000001);
    // Internal parallax follows the eye ray, yielding actual stereo recession.
    // Field phase/observation remain shared and do not depend on eye identity.
    vec3 view = normalize(WorldEyePosition.xyz - world);
    if (dot(normal, view) < 0.0) normal = -normal;
    float distance = length(world - WorldHeadPosition.xyz);
    float nearGuard = smoothstep(0.32, 1.1, distance);
    float presence = max(WarpA.x, 0.0) * nearGuard * step(0.000001, normalLength);
    float seconds = WarpA.w * 0.05;
    float seedPhase = WarpC.y * TAU;
    float motion = max(0.0, Control.y);
    float colorScale = clamp(Control.x, 0.0, 1.0);
    float complexity = max(0.0, WarpB.w);
    float highGate = clamp(Intensity.x, 0.0, 1.0);
    float doseExpansion = max(0.0, Intensity.y);
    float activeExpansion = log(1.0 + max(0.0, Intensity.z));
    float highDrive = highGate * doseExpansion * activeExpansion;
    float spatialDrive = log(1.0 + highDrive);
    float colorTravel = Intensity.w;
    float colorDrive = max(0.0, WarpB.y);

    // Dominant face axes preserve Minecraft's block grid. The positions are
    // reconstructed from depth, so these coordinates foreshorten with the wall.
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
    vec2 face = vec2(dot(world, tangentU), dot(world, tangentV));
    vec2 cell = fract(face) - 0.5;
    vec2 footprint = max(fwidth(face), vec2(0.0001));
    float resolvable = 1.0 - smoothstep(0.14, 0.60, max(footprint.x, footprint.y));
    float edge = max(abs(cell.x), abs(cell.y));
    float blockInterior = 1.0 - smoothstep(0.25, 0.49, edge);
    float wave = structureWave(world, seconds + seedPhase);
    float neighborPhase = TAU * dot(floor(world + normal * -0.012), vec3(1, 3, 2)) / 16.0;
    float breathing = sin(seconds * TAU / 20.0 + neighborPhase + wave * (0.7 + MixA.y * 0.2) + seedPhase);
    float attention = Focus.w * exp(-length(world - Focus.xyz) * 0.35);
    float bass = MusicA.x * WarpC.x * 0.24;
    float deform = (WarpA.z + spatialDrive * 0.40) * presence * motion;
    float bowl = blockInterior * breathing * (0.035 + 0.12 * WarpA.y + spatialDrive * 0.09);
    float structure = wave * (0.022 + WarpA.y * 0.10 + bass * 0.09 + spatialDrive * 0.09);
    vec3 displaced = world + normal * (bowl * resolvable + structure) * deform;
    // Shear/ripples travel across whole structures; turning adds a decaying
    // displacement in world direction rather than a random camera perturbation.
    displaced += tangentU * sin(seconds * TAU / 24.0 + face.y * TAU / 16.0 + seedPhase) * 0.035 * deform;
    displaced += tangentV * wave * 0.040 * deform * (0.5 + Control.z);
    displaced += (MotionA.xyz * 0.28 + MotionB.xyz * min(distance, 12.0) * 0.20)
                    * WarpB.z * presence * motion * (0.4 + blockInterior * 0.6);
    vec2 sourceUv = safeSource(uv, displaced, world, normal);
    vec3 color = texture(DiffuseSampler, sourceUv).rgb;

    // Shading changes are tied to this face's local relief, never a screen tint.
    float relief = (breathing * blockInterior * 0.065 + wave * 0.025) * deform * resolvable;
    color *= 1.0 + relief / (1.0 + abs(relief));
    vec3 embedded = vec3(0.0);
    float embeddedAlpha = 0.0;
    float recursion = (WarpB.x + spatialDrive * 0.22) * presence * resolvable;
    float observedDetail = attention * (0.24 + complexity * 0.07);
    float layerBudget = 1.0 + complexity * 2.0 + observedDetail * 2.0;
    float layerScale = 1.0;
    vec2 parallax = vec2(dot(view, tangentU), dot(view, tangentV)) / max(0.32, abs(dot(view, normal)));
    float virtualDepth = (0.020 + WarpA.y * 0.11 + spatialDrive * 0.10) * (1.0 + observedDetail + Control.w * 0.12);
    float textureLuma = dot(original.rgb, vec3(0.2126, 0.7152, 0.0722));
    float textureContrast = length(fwidth(original.rgb));

    // A fixed shader work budget bounds GPU cost. Increasing dose continues
    // changing scale, apparent recession and interleaved motif topology after
    // all layers are present; derivatives remove details smaller than a pixel.
    for (int layer = 0; layer < 9; layer++) {
        float level = float(layer);
        float enabled = clamp(layerBudget - level, 0.0, 1.0);
        float recess = (level + 1.0) * virtualDepth * (1.0 + complexity * 0.055);
        vec2 inset = cell * layerScale - parallax * recess * layerScale;
        inset += vec2(wave, breathing) * 0.012 * motion * (1.0 + level * 0.14);
        vec2 q = fract(inset) - 0.5;
        vec2 fw = footprint * layerScale;
        float antialias = max(fw.x, fw.y);
        float visible = 1.0 - smoothstep(0.13, 0.50, antialias);
        float square = max(abs(q.x), abs(q.y));
        float diamond = abs(q.x) + abs(q.y);
        float frame = line(abs(square - 0.405), 0.007, antialias);
        float nested = line(abs(diamond - 0.35 - 0.018 * sin(seconds * TAU / 30.0 + level)), 0.005, antialias);
        float architecture = mix(frame, max(frame * 0.65, nested), 0.32 + MixA.x * 0.20);
        float openness = 0.5 + 0.5 * sin(seedPhase + level * 1.9 + wave * 1.3);
        float weight = enabled * visible * (1.0 - exp(-recursion * 0.36)) * exp(-level * 0.31)
                     * (0.15 + 0.045 * log(1.0 + WarpB.x) + observedDetail * 0.024);
        float phase = WarpC.y + level * 0.15 + face.x / 32.0 + face.y / 64.0
                    + wave * 0.13 + MusicA.y * WarpC.x * 0.11 + colorTravel * 0.5;
        vec3 hue = crystal(phase);
        embedded += hue * architecture * weight * (0.55 + openness * 0.45);
        embeddedAlpha += architecture * weight;
        // Successive inset facets reveal the source texture at apparent depth.
        // Keep original world lighting/material identity dominant.
        if (layer < 3) {
            vec3 behind = world - normal * recess;
            vec2 interiorUv = safeSource(sourceUv, behind, world, normal);
            vec3 interior = texture(DiffuseSampler, interiorUv).rgb;
            color = mix(color, interior * (0.93 - level * 0.025) + hue * 0.025,
                        1.0 - exp(-blockInterior * weight * (0.75 + attention * 0.5)));
        }
        layerScale *= 1.55 + complexity * 0.08 + MixA.w * 0.05;
    }

    // Thin iridescent block rims, texture ridges and dark internal facets.
    // Refraction is local to these features; all other pixels keep their material.
    float rim = line(abs(edge - 0.475), 0.004, max(footprint.x, footprint.y)) * resolvable;
    float internalRidge = smoothstep(0.035, 0.18, textureContrast) * blockInterior;
    float refractMask = (1.0 - exp(-(rim * 0.34 + internalRidge * 0.13) * colorDrive * presence)) * colorScale;
    vec3 hue = crystal(face.x / 16.0 + face.y / 32.0 + wave * 0.23 + WarpC.y + colorTravel * 0.5);
    vec2 fringeUv = safeSource(sourceUv, displaced + tangentU * (0.004 + WarpB.y * 0.010), world, normal);
    vec3 refraction = texture(DiffuseSampler, fringeUv).rgb;
    color += (hue * (0.13 + textureLuma * 0.12) + abs(refraction - color) * hue * 0.45) * refractMask;
    float darkFacet = (1.0 - smoothstep(0.04, 0.42, textureLuma)) * blockInterior;
    color += hue * darkFacet * embeddedAlpha * 0.25 * (1.0 - exp(-colorDrive)) * colorScale;
    embedded /= 1.0 + embeddedAlpha * 0.45;
    color = color * (1.0 - min(0.12, embeddedAlpha * colorScale * 0.3)) + embedded * colorScale;

    // High-dose iridescence flows through large faces like colored mineral
    // layers/aurora curtains. Its coordinates are metres on the surface, never
    // viewport UV. 40->50mg opens it smoothly, with continuing dose expansion.
    float auroraResponse = highDrive / (0.65 + highDrive);
    float surfaceFlow = sin(TAU * (face.x / 8.0 + face.y / 16.0 + colorTravel))
                      + sin(TAU * (face.y / 8.0 - colorTravel * 0.5) + wave * 1.4) * 0.45;
    float foldFlow = sin(surfaceFlow * (1.8 + spatialDrive * 0.25)
                       + cell.y * (1.4 + spatialDrive * 0.20) + seedPhase);
    float ribbonCoordinate = face.x / 4.0 + face.y / 8.0 + surfaceFlow * 0.27
                           + foldFlow * 0.13 + colorTravel;
    float ribbonWidth = max(0.004, fwidth(ribbonCoordinate));
    float ribbon = 0.5 + 0.5 * cos(TAU * ribbonCoordinate);
    float flowingBand = smoothstep(0.14 - ribbonWidth, 0.86 + ribbonWidth, ribbon);
    float facetPhase = ribbonCoordinate + surfaceFlow * 0.16 + spatialDrive * foldFlow * 0.065;
    vec3 auroraHue = crystal(facetPhase);
    vec3 innerHue = crystal(facetPhase + 0.20 + wave * 0.06);
    float albedo = max(0.035, dot(color, vec3(0.2126, 0.7152, 0.0722)));
    vec3 luminousMaterial = mix(auroraHue, innerHue, blockInterior * 0.36)
                         * (0.24 + albedo * 0.95) + color * 0.14;
    float surfaceCoverage = (0.30 + flowingBand * 0.48 + internalRidge * 0.10 + attention * 0.10)
                          * (1.0 + log(1.0 + highDrive) * 0.16);
    float auroraAlpha = (1.0 - exp(-presence * auroraResponse * surfaceCoverage * 1.65)) * colorScale;
    color = mix(color, luminousMaterial, auroraAlpha);
    // Source-colored refraction grows in apparent depth without introducing
    // blur, extra frame-history surfaces, flash pulses or a brightness ceiling.
    vec3 refractedPoint = displaced + (tangentU * foldFlow + tangentV * surfaceFlow * 0.5)
                                   * spatialDrive * 0.028 * motion;
    vec3 chromaticSource = texture(DiffuseSampler, safeSource(sourceUv, refractedPoint, world, normal)).rgb;
    color += abs(chromaticSource - original.rgb) * auroraHue * auroraAlpha * 0.35;
    // Richness follows relief and texture, rather than washing over the sky/HUD.
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, 1.0 + (1.0 - exp(-colorDrive * presence)) * colorScale * 0.28 * (0.35 + blockInterior));
    fragColor = vec4(clamp(color, vec3(0.0), vec3(1.0)), original.a);
}
