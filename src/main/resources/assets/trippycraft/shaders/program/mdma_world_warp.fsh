#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform vec2 InSize;
uniform vec4 WarpA; // intensity, saturation, bloom, time
uniform vec4 WarpB; // warmth, bass pulse, dose, peak window
uniform vec4 WarpC; // comedown, photosensitivity reduction, dreaminess, club proximity

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

vec3 screenBlend(vec3 base, vec3 glow) {
    return 1.0 - (1.0 - clamp(base, 0.0, 1.0)) * (1.0 - clamp(glow, 0.0, 0.92));
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec4 source = texture(DiffuseSampler, uv);

    float intensity = clamp(WarpA.x, 0.0, 1.35);
    float saturation = clamp(WarpA.y, 0.0, 1.55);
    float bloom = clamp(WarpA.z, 0.0, 1.55);
    float warmth = clamp(WarpB.x, 0.0, 1.45);
    float bass = clamp(WarpB.y, 0.0, 1.45);
    float dose = clamp(WarpB.z, 0.0, 1.5);
    float peak = saturate(WarpB.w);
    float comedown = clamp(WarpC.x, 0.0, 1.25);
    float photosensitive = saturate(WarpC.y);
    float dreaminess = clamp(WarpC.z, 0.0, 1.2);
    float club = saturate(WarpC.w);

    if (intensity <= 0.0001 && comedown <= 0.0001) {
        fragColor = source;
        return;
    }

    vec2 nearOffset = texel * (1.5 + bloom * 1.7 + dreaminess * 0.8);
    vec2 farOffset = texel * (4.0 + bloom * 3.2 + dreaminess * 1.4);
    vec3 nearBlur = (sampleScene(uv + vec2(nearOffset.x, 0.0))
            + sampleScene(uv - vec2(nearOffset.x, 0.0))
            + sampleScene(uv + vec2(0.0, nearOffset.y))
            + sampleScene(uv - vec2(0.0, nearOffset.y))) * 0.25;
    vec3 farBlur = (sampleScene(uv + farOffset)
            + sampleScene(uv - farOffset)
            + sampleScene(uv + vec2(farOffset.x, -farOffset.y))
            + sampleScene(uv + vec2(-farOffset.x, farOffset.y))) * 0.25;
    vec3 softLight = mix(nearBlur, farBlur, 0.46 + dreaminess * 0.20);

    vec3 color = source.rgb;
    float originalLuma = luma(color);
    float colorGain = 1.0 + saturation * (1.18 + dose * 0.72 + peak * 0.24);
    color = vec3(originalLuma) + (color - vec3(originalLuma)) * colorGain;

    float highlightGate = smoothstep(max(0.16, 0.54 - bloom * 0.16 - dose * 0.10), 0.96, luma(softLight));
    vec3 bloomLayer = max(softLight - vec3(0.16 - dose * 0.035), vec3(0.0));
    float bloomStrength = intensity * bloom * (0.46 + dose * 0.28 + peak * 0.20);
    bloomStrength *= 1.0 - photosensitive * 0.52;
    color = screenBlend(color, bloomLayer * bloomStrength * (0.70 + highlightGate * 0.68));

    vec3 pink = vec3(1.0, 0.23, 0.72);
    vec3 softPink = vec3(1.0, 0.58, 0.86);
    vec3 cyan = vec3(0.20, 0.90, 1.0);
    float midtone = smoothstep(0.10, 0.66, originalLuma) * (1.0 - smoothstep(0.86, 1.0, originalLuma));
    float cloud = intensity * (0.060 + warmth * 0.075 + bloom * 0.055 + dose * peak * 0.055);
    cloud *= 1.0 - photosensitive * 0.34;
    color = mix(color, color * vec3(1.12, 0.94, 1.08) + softPink * 0.075,
            clamp(cloud * (0.70 + midtone * 0.55), 0.0, 0.34));

    float beatLift = bass * peak * (0.070 + dose * 0.050 + club * 0.012) * (1.0 - photosensitive * 0.72);
    color = screenBlend(color, mix(pink, cyan, 0.24 + highlightGate * 0.22) * beatLift);
    color += mix(softPink, pink, 0.45) * cloud * (0.060 + highlightGate * 0.055);

    float edgeCloud = 1.0 - smoothstep(0.10, 0.72, length((uv - 0.5) * vec2(1.0, OutSize.y / max(OutSize.x, 1.0))));
    color = screenBlend(color, softPink * edgeCloud * cloud * 0.045);

    if (comedown > 0.0) {
        float gray = luma(color);
        color = mix(color, vec3(gray), clamp(comedown * 0.62, 0.0, 0.78));
        color *= 1.0 - comedown * 0.075;
    }

    fragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
