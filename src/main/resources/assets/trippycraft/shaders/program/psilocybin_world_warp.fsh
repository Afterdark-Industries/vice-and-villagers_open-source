#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform vec2 InSize;
uniform vec4 WarpA; // eased intensity, breathing, melting, local seconds
uniform vec4 WarpB; // stable trip seeds, eased dose grams, high-dose gate
uniform vec4 PaletteB; // signature color, attention strength
uniform vec4 Focus;
uniform vec4 Dose;
uniform vec4 Control; // motion setting, photosensitivity setting

out vec4 fragColor;

float luma(vec3 color) { return dot(color, vec3(0.299, 0.587, 0.114)); }

float hash21(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float smoothNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    // Quintic interpolation has continuous first and second derivatives at
    // cell boundaries. No time-indexed random cells or switching warp modes.
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(mix(hash21(cell), hash21(cell + vec2(1.0, 0.0)), u.x),
               mix(hash21(cell + vec2(0.0, 1.0)), hash21(cell + vec2(1.0, 1.0)), u.x), u.y);
}

float organicField(vec2 p) {
    float footprint = max(length(dFdx(p)), length(dFdy(p)));
    float value = 0.0;
    float weight = 0.57;
    mat2 turn = mat2(0.7648, -0.6442, 0.6442, 0.7648);
    for (int octave = 0; octave < 4; octave++) {
        // Resolve the texture field before it can alias into shimmer. More
        // dose adds deformation, never a faster animation or a noisy octave.
        float resolved = 1.0 - smoothstep(0.07, 0.35, footprint);
        value += mix(0.5, smoothNoise(p), resolved) * weight;
        p = turn * p * 1.91 + vec2(7.1, -4.3);
        footprint *= 1.91;
        weight *= 0.46;
    }
    return value;
}

vec2 safeUv(vec2 uv) {
    vec2 margin = 0.5 / max(InSize, vec2(1.0));
    return clamp(uv, margin, vec2(1.0) - margin);
}

vec3 scene(vec2 uv) { return texture(DiffuseSampler, safeUv(uv)).rgb; }
float depthAt(vec2 uv) { return texture(DepthSampler, safeUv(uv)).r; }

vec3 retainNaturalColor(vec3 color, float saturation) {
    float gray = luma(color);
    vec3 chroma = (color - vec3(gray)) * saturation;
    // Match available highlight/shadow headroom rather than clipping vivid
    // foliage into a flat neon color. Source hues and luminance are retained.
    float positive = max(max(chroma.r, chroma.g), max(chroma.b, 0.0001));
    float negative = max(max(-chroma.r, -chroma.g), max(-chroma.b, 0.0001));
    float headroom = min(1.0, min((1.0 - gray) / positive, gray / negative));
    return vec3(gray) + chroma * max(0.0, headroom);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 source = texture(DiffuseSampler, safeUv(uv));
    float intensity = max(0.0, WarpA.x);
    if (intensity <= 0.0001) { fragColor = source; return; }

    // Every appearance channel below is multiplied by intensity: a freshly
    // swallowed capsule cannot switch saturation on just because dose exists.
    float presence = 1.0 - exp(-intensity);
    float strength = log(1.0 + intensity);
    float dose = log(1.0 + max(0.0, WarpB.z) / 3.0);
    float breathing = max(0.0, WarpA.y);
    float melting = max(0.0, WarpA.z);
    float time = WarpA.w;
    float seedA = WarpB.x * 31.7;
    float seedB = WarpB.y * 43.1;
    float motion = max(0.0, Control.x);
    float colorSetting = clamp(Control.y, 0.0, 1.0);
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);

    float depth = depthAt(uv);
    float surface = 1.0 - step(0.999995, depth);
    float lum = luma(source.rgb);
    float leftLum = luma(scene(uv - vec2(texel.x * 1.8, 0.0)));
    float rightLum = luma(scene(uv + vec2(texel.x * 1.8, 0.0)));
    float downLum = luma(scene(uv - vec2(0.0, texel.y * 1.8)));
    float upLum = luma(scene(uv + vec2(0.0, texel.y * 1.8)));
    float detail = smoothstep(0.010, 0.17, abs(rightLum - leftLum) + abs(upLum - downLum));
    float depthJump = max(abs(depthAt(uv + vec2(texel.x * 2.0, 0.0)) - depth),
                         abs(depthAt(uv + vec2(0.0, texel.y * 2.0)) - depth));
    float silhouette = 1.0 - smoothstep(0.0003, 0.006, depthJump);
    float surfaceWeight = surface * (0.50 + detail * 0.50) * (0.38 + silhouette * 0.62);
    float border = smoothstep(0.0, 0.055, min(min(uv.x, uv.y), min(1.0 - uv.x, 1.0 - uv.y)));

    // One slowly advected field carries broad breathing and fine wrinkles.
    // Dose increases its extent/depth; all temporal frequencies stay fixed.
    vec2 drift = vec2(time * 0.013, -time * 0.010);
    float fieldA = organicField(p * (3.6 + dose * 0.6) + drift + vec2(seedA, seedB));
    float fieldB = organicField(p * (4.3 + dose * 0.7) - drift * 0.73 + vec2(seedB, -seedA));
    float phaseA = p.y * 13.0 + fieldA * 4.6 + time * 0.24 + seedA;
    float phaseB = p.x * 11.0 + fieldB * 4.2 - time * 0.20 + seedB;
    vec2 flow = vec2(sin(phaseA), cos(phaseB)) * 0.64
              + vec2(fieldA - 0.5, fieldB - 0.5) * 0.76;
    float broadBreath = sin(time * 0.34 + seedA * 0.21 + fieldA * 1.2);
    float breathPhaseX = p.x * (8.0 + dose * 0.35) + fieldA * 1.1 + seedA;
    float breathPhaseY = p.y * (7.0 + dose * 0.30) + fieldB * 1.2 + seedB;
    // Neighboring patches expand/relax in opposite directions. There is no
    // radial displacement about screen center and therefore no lens zoom.
    vec2 breathingFlow = vec2(sin(breathPhaseX) * cos(breathPhaseY),
                              cos(breathPhaseX) * sin(breathPhaseY)) * broadBreath * 0.30
                       + flow * 0.15;

    float ripplePhase = p.x * (25.0 + dose * 5.0) + p.y * (18.0 + dose * 3.0)
                      + fieldA * 7.4 + fieldB * 3.6 - time * 0.46;
    float wrinklePhase = p.y * (42.0 + dose * 7.0) - p.x * 17.0
                       + fieldB * 9.5 + sin(phaseA) * 1.8 + time * 0.31;
    float rippleResolved = 1.0 - smoothstep(0.30, 1.30, fwidth(ripplePhase));
    float wrinkleResolved = 1.0 - smoothstep(0.30, 1.30, fwidth(wrinklePhase));
    vec2 ripples = vec2(sin(ripplePhase), cos(ripplePhase * 0.79 + fieldB * 2.4)) * rippleResolved;
    vec2 wrinkles = vec2(cos(wrinklePhase), sin(wrinklePhase * 0.87 + fieldA * 2.1)) * wrinkleResolved;

    vec2 focusDelta = (uv - Focus.xy) * vec2(aspect, 1.0);
    float attention = (1.0 - smoothstep(Focus.z * 0.30, max(0.06, Focus.z), length(focusDelta)))
                    * log(1.0 + max(0.0, PaletteB.w));
    float flowAmount = strength * (0.0032 + breathing * 0.0026 + melting * 0.0030 + dose * 0.0009);
    float rippleAmount = strength * (0.00065 + Dose.y * 0.0012 + dose * 0.00065);
    vec2 warp = flow * flowAmount + breathingFlow * strength * breathing * 0.0040;
    warp += (ripples * 0.65 + wrinkles * 0.35) * rippleAmount * (0.62 + attention * 0.65);
    warp *= surfaceWeight * border * motion * 2.2;
    // Smooth domain compression keeps enormous stacked doses within the
    // source image while retaining strictly increasing strength, not a cap.
    warp /= 1.0 + length(warp) * 3.0;
    warp.x /= max(aspect, 0.1);
    vec2 displaced = safeUv(uv + warp);
    // Resample the complete texel together. There are deliberately no separate
    // red/green/blue coordinates, rainbow fringes, or palette replacements.
    vec3 color = scene(displaced);

    vec3 neighborA = scene(displaced + vec2(texel.x * 1.25, 0.0));
    vec3 neighborB = scene(displaced - vec2(texel.x * 1.25, 0.0));
    vec3 neighborC = scene(displaced + vec2(0.0, texel.y * 1.25));
    vec3 neighborD = scene(displaced - vec2(0.0, texel.y * 1.25));
    vec3 localMean = (neighborA + neighborB + neighborC + neighborD) * 0.25;
    vec3 localLow = min(color, min(min(neighborA, neighborB), min(neighborC, neighborD)));
    vec3 localHigh = max(color, max(max(neighborA, neighborB), max(neighborC, neighborD)));
    float clarity = (0.18 * presence + strength * 0.12) * surface * colorSetting;
    // Local contrast makes the existing texture feel clear, and neighborhood
    // bounds prevent halos. This does not invent extra source resolution.
    color = clamp(color + (color - localMean) * clarity, localLow, localHigh);
    float ridge = (sin(ripplePhase) * rippleResolved * 0.60 + cos(wrinklePhase) * wrinkleResolved * 0.40);
    float relief = ridge * strength * (0.008 + dose * 0.003) * surfaceWeight * colorSetting;
    color += color * (vec3(1.0) - color) * relief;
    // Calibrate surface visibility separately from color strength: clearer
    // breathing does not require a matching jump in saturation.
    float saturation = 1.0 + (presence * 0.040 + strength * 0.035) * colorSetting;
    color = retainNaturalColor(color, saturation);
    float warmPresence = presence * 0.0025 * colorSetting;
    color += vec3(1.0, 0.30, -0.45) * warmPresence * color * (vec3(1.0) - color);

    fragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
