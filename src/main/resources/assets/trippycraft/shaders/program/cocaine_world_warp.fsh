#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform vec2 InSize;

uniform vec4 WarpA; // intensity, harshness, focus, time
uniform vec4 WarpB; // flicker, crash, clarity, dose
uniform vec4 WarpC; // progress, photosensitivity reduction, reserved, reserved

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec3 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));

    vec4 source = texture(DiffuseSampler, uv);
    float intensity = saturate(WarpA.x);
    if (intensity <= 0.0001) {
        fragColor = source;
        return;
    }

    float harshness = saturate(WarpA.y);
    float focus = saturate(WarpA.z);
    float time = WarpA.w * 0.050;
    float flicker = saturate(WarpB.x);
    float crash = saturate(WarpB.y);
    float clarity = saturate(WarpB.z);
    float dose = saturate(WarpB.w);
    float progress = saturate(WarpC.x);
    float photosensitive = saturate(WarpC.y);

    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 centered = uv - 0.5;
    centered.x *= aspect;
    float radius = length(centered);
    float centerFocus = 1.0 - smoothstep(0.10, 0.78, radius);
    float tunnel = smoothstep(0.18, 0.92, radius) * focus;

    vec3 color = source.rgb;
    vec3 blur = (sampleScene(uv + vec2(texel.x * 2.0, 0.0))
            + sampleScene(uv - vec2(texel.x * 2.0, 0.0))
            + sampleScene(uv + vec2(0.0, texel.y * 2.0))
            + sampleScene(uv - vec2(0.0, texel.y * 2.0))) * 0.25;
    float sceneLuma = luma(color);
    float highlightGate = smoothstep(max(0.18, 0.62 - dose * 0.22 - clarity * 0.12), 1.0, sceneLuma);
    float pulse = sin(time * (0.68 + flicker * 1.25) + dose * 6.2831 + progress * 1.4) * 0.5 + 0.5;
    float flickerAmp = (0.010 + flicker * 0.050) * (1.0 - photosensitive * 0.55);
    float bloomMix = saturate((0.18 + harshness * 0.42 + clarity * 0.24) * highlightGate + centerFocus * clarity * 0.24);
    color = mix(color, max(color, blur), bloomMix * 0.42);

    float exposure = intensity * (0.026 + highlightGate * (0.10 + harshness * 0.18) + centerFocus * (0.032 + clarity * 0.090));
    exposure += pulse * flickerAmp * highlightGate;
    exposure *= 1.0 - crash * 0.44;
    color += vec3(exposure);

    float contrast = 1.0 + clarity * 0.18 - crash * 0.10;
    color = (color - 0.5) * contrast + 0.5;
    color *= 1.0 - tunnel * (0.045 + crash * 0.10);

    fragColor = vec4(max(color, vec3(0.0)), source.a);
}
