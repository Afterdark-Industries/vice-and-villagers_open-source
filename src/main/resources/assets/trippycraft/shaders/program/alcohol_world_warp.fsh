#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec2 OutSize;
uniform vec2 InSize;

uniform vec4 WarpA; // intensity, diplopia, blur, time
uniform vec4 WarpB; // sway, focus lag, nausea, sedation
uniform vec4 WarpC; // motor noise, peripheral narrowing, progress, standard drinks
uniform vec4 WarpD; // risk, tier, accessibility, photosensitivity reduction

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float valueNoise(vec2 uv, float scale, float seed) {
    vec2 g = floor(uv * scale + seed);
    vec2 f = fract(uv * scale + seed);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(g);
    float b = hash21(g + vec2(1.0, 0.0));
    float c = hash21(g + vec2(0.0, 1.0));
    float d = hash21(g + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

vec3 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

float sampleDepth(vec2 uv) {
    return texture(DepthSampler, clamp(uv, vec2(0.001), vec2(0.999))).r;
}

vec3 softFocus(vec2 uv, vec2 axis, vec2 texel, float blur) {
    float radius = 1.0 + blur * 8.0;
    vec2 a = axis * texel * radius;
    vec2 b = vec2(-axis.y, axis.x) * texel * radius * 0.70;
    vec3 color = sampleScene(uv) * 0.34;
    color += sampleScene(uv + a) * 0.16;
    color += sampleScene(uv - a) * 0.16;
    color += sampleScene(uv + b) * 0.12;
    color += sampleScene(uv - b) * 0.12;
    color += sampleScene(uv + a * 1.8) * 0.05;
    color += sampleScene(uv - a * 1.8) * 0.05;
    return color;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    float aspect = OutSize.x / max(OutSize.y, 1.0);

    float intensity = saturate(WarpA.x);
    float diplopia = saturate(WarpA.y);
    float blur = saturate(WarpA.z);
    float time = WarpA.w * 0.050;
    float sway = saturate(WarpB.x);
    float focusLag = saturate(WarpB.y);
    float nausea = saturate(WarpB.z);
    float sedation = saturate(WarpB.w);
    float motor = saturate(WarpC.x);
    float peripheral = saturate(WarpC.y);
    float progress = saturate(WarpC.z);
    float dose = max(0.0, WarpC.w);
    float risk = saturate(WarpD.x);
    float tier = max(0.0, WarpD.y);
    float accessibility = saturate(WarpD.z);

    vec4 source = texture(DiffuseSampler, uv);
    if (intensity <= 0.0001) {
        fragColor = source;
        return;
    }

    vec2 centered = uv - 0.5;
    centered.x *= aspect;
    float radius = length(centered);
    float peripheralGain = smoothstep(0.08, 0.80, radius);
    float centerMercy = mix(0.48, 1.0, smoothstep(0.025, 0.16, radius));

    float depth = sampleDepth(uv);
    float farDepth = smoothstep(0.52, 0.995, depth);
    float nearDepth = 1.0 - smoothstep(0.18, 0.76, depth);
    float depthL = sampleDepth(uv - vec2(texel.x * 2.0, 0.0));
    float depthR = sampleDepth(uv + vec2(texel.x * 2.0, 0.0));
    float depthD = sampleDepth(uv - vec2(0.0, texel.y * 2.0));
    float depthU = sampleDepth(uv + vec2(0.0, texel.y * 2.0));
    float depthEdge = smoothstep(0.0008, 0.024, abs(depthL - depthR) + abs(depthD - depthU));

    float imageEdge = smoothstep(0.025, 0.160,
            abs(luma(sampleScene(uv + vec2(texel.x * 1.4, 0.0))) - luma(sampleScene(uv - vec2(texel.x * 1.4, 0.0))))
            + abs(luma(sampleScene(uv + vec2(0.0, texel.y * 1.4))) - luma(sampleScene(uv - vec2(0.0, texel.y * 1.4)))));
    float edge = saturate(imageEdge * 0.54 + depthEdge * 0.58);

    float slowNoise = valueNoise(vec2(time * 0.035, dose * 0.23), 2.4, dose * 2.1 + 5.0);
    float eyeRoll = sin(time * 0.25 + dose * 0.73) * 0.38 + sin(time * 0.12 + 2.1) * 0.18;
    vec2 eyeAxis = normalize(vec2(1.0, eyeRoll * (0.12 + sway * 0.10)) + vec2(0.001, 0.0));

    float blinkDrift = pow(max(0.0, sin(time * (0.82 + motor * 0.32) + slowNoise * 4.0 + dose)), 12.0) * motor;
    vec2 eyeDrift = eyeAxis * (sin(time * 0.18 + dose) * sway + blinkDrift * 0.80)
            * intensity
            * (0.00045 + diplopia * 0.0011)
            * (1.0 - accessibility * 0.55);
    vec2 baseUv = uv + eyeDrift;

    float parallax = mix(0.80, 1.26, farDepth) + nearDepth * 0.08 + edge * 0.16;
    float doseGate = smoothstep(0.65, 4.8, dose);
    float peakGate = smoothstep(0.06, 0.30, progress) * (1.0 - smoothstep(0.76, 1.0, progress) * 0.42);
    float separation = (0.0007 + diplopia * 0.0135 + motor * 0.0018 + focusLag * 0.0018)
            * parallax
            * centerMercy
            * (0.50 + peripheralGain * 0.50)
            * (0.74 + doseGate * 0.26)
            * (0.66 + peakGate * 0.34)
            * (1.0 - accessibility * 0.56);
    vec2 offset = eyeAxis * separation;

    vec3 base = sampleScene(baseUv);
    vec3 soft = softFocus(baseUv, eyeAxis, texel, blur * (1.0 - accessibility * 0.36));
    vec3 left = sampleScene(baseUv + offset * (1.0 + blinkDrift * 0.20));
    vec3 right = sampleScene(baseUv - offset * (0.86 + slowNoise * 0.06));
    vec3 ghost = mix(left, right, 0.52 + eyeRoll * 0.035);

    float blurMix = saturate(blur * (0.32 + doseGate * 0.28) + sedation * 0.16 + nausea * 0.06) * (1.0 - accessibility * 0.46);
    float ghostMix = saturate(diplopia * (0.18 + peripheralGain * 0.30 + edge * 0.08)
            + motor * blinkDrift * 0.10
            + focusLag * 0.08) * (1.0 - accessibility * 0.52);
    ghostMix = min(0.56, ghostMix);

    vec3 color = mix(base, soft, blurMix);
    color = mix(color, ghost, ghostMix);

    float tripleGate = smoothstep(4.6, 7.2, dose) * smoothstep(0.52, 0.92, diplopia) * (1.0 - accessibility * 0.60);
    vec3 third = sampleScene(baseUv + offset * vec2(-1.55, 0.18 + eyeRoll * 0.10));
    color = mix(color, third, min(0.28, tripleGate * (0.08 + peripheralGain * 0.14 + edge * 0.06)));

    float gray = luma(color);
    float desaturate = saturate(sedation * 0.22 + blur * 0.14 + risk * 0.10);
    color = mix(color, vec3(gray), desaturate);

    float warmFlush = smoothstep(0.40, 2.2, dose) * (1.0 - smoothstep(4.8, 7.5, dose)) * (0.010 + intensity * 0.018);
    color += vec3(warmFlush, warmFlush * 0.30, -warmFlush * 0.08);

    float vignette = smoothstep(0.42, 0.94, radius);
    float tunnel = saturate(peripheral * 0.45 + sedation * 0.22 + risk * 0.18 + nausea * 0.08);
    color *= 1.0 - vignette * tunnel * (0.18 + intensity * 0.28) * (1.0 - accessibility * 0.34);

    fragColor = vec4(max(color, vec3(0.0)), source.a);
}

