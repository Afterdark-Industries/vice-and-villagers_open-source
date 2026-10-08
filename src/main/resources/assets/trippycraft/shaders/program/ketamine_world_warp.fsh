#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform vec2 InSize;

uniform vec4 WarpA; // intensity, lag, depth, time
uniform vec4 WarpB; // drift, tunnel, echo, dose
uniform vec4 WarpC; // progress, collapse, photosensitivity reduction, reserved

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

vec3 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
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

    float lag = saturate(WarpA.y);
    float depth = saturate(WarpA.z);
    float time = WarpA.w * 0.014;
    float drift = saturate(WarpB.x);
    float tunnel = saturate(WarpB.y);
    float echo = saturate(WarpB.z);
    float dose = saturate(WarpB.w);
    float progress = saturate(WarpC.x);
    float collapse = saturate(WarpC.y);
    float photosensitive = saturate(WarpC.z);

    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 centered = uv - 0.5;
    centered.x *= aspect;
    float radius = length(centered);
    vec2 direction = normalize(centered + vec2(0.0001, 0.0));
    float edge = smoothstep(0.12, 0.92, radius);
    float centerFocus = 1.0 - smoothstep(0.08, 0.66, radius);

    float breathe = sin(time * (0.09 + drift * 0.14) + dose * 4.8 + radius * 6.5) * 0.5 + 0.5;
    vec2 parallax = direction * (0.004 + depth * 0.024 + tunnel * 0.014 + collapse * 0.010) * (0.34 + edge * 0.78);
    vec2 slowDrift = vec2(
        sin(time * (0.030 + drift * 0.050) + centered.y * 4.0),
        cos(time * (0.024 + drift * 0.040) + centered.x * 3.5)
    ) * (0.0024 + drift * 0.010 + echo * 0.0036);
    parallax += slowDrift;
    parallax *= 1.0 - photosensitive * 0.35;

    vec3 base = source.rgb;
    vec3 echoA = sampleScene(uv + parallax);
    vec3 echoB = sampleScene(uv - parallax * (0.55 + echo * 0.45));
    vec3 blur = (
        sampleScene(uv + vec2(texel.x * 2.0, 0.0))
        + sampleScene(uv - vec2(texel.x * 2.0, 0.0))
        + sampleScene(uv + vec2(0.0, texel.y * 2.0))
        + sampleScene(uv - vec2(0.0, texel.y * 2.0))
    ) * 0.25;

    float disconnect = saturate(intensity * (0.16 + depth * 0.38 + lag * 0.20) + echo * 0.16);
    vec3 color = mix(base, (echoA + echoB) * 0.5, disconnect * 0.54 + echo * 0.24);
    color = mix(color, blur, tunnel * (0.16 + depth * 0.28 + edge * 0.22));

    float desaturate = intensity * (0.18 + tunnel * 0.24 + collapse * 0.22);
    color = mix(color, vec3(luminance(color)), desaturate * 0.46);
    color += vec3(-0.003, 0.006, 0.016) * (echo * 0.18 + depth * 0.16 + tunnel * 0.10 + centerFocus * 0.04);
    color *= 1.0 - edge * (tunnel * 0.16 + collapse * 0.22);
    color *= 1.0 - collapse * 0.08;
    color += vec3(breathe * 0.010) * (drift * 0.50 + centerFocus * 0.16) * (1.0 - photosensitive * 0.45);

    fragColor = vec4(max(color, vec3(0.0)), source.a);
}
