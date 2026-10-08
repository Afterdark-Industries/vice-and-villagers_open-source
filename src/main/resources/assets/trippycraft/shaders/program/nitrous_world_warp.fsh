#version 150

// Nitrous: a short, intense rush. Tunnel, fisheye, white-blue haze, bloom,
// chromatic split, and time-echo trails. Charge grows without a ceiling.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 InSize;
uniform vec4 WarpA; // presence, tunnel, fisheye, time
uniform vec4 WarpB; // haze, bloom, chroma, echo
uniform vec4 WarpC; // stretch, dissociation, music pulse, photosensitive

out vec4 fragColor;
const float TAU = 6.28318530718;

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec3 sampleScene(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 source = texture(DiffuseSampler, uv);
    float presence = max(WarpA.x, 0.0);
    if (presence <= 0.0001) {
        fragColor = source;
        return;
    }

    float tunnel = max(0.0, WarpA.y);
    float fisheye = max(0.0, WarpA.z);
    float seconds = WarpA.w * 0.05;
    float haze = max(0.0, WarpB.x);
    float bloom = max(0.0, WarpB.y);
    float chroma = max(0.0, WarpB.z);
    float echo = max(0.0, WarpB.w);
    float stretch = max(0.0, WarpC.x);
    float dissociation = max(0.0, WarpC.y);
    float music = max(0.0, WarpC.z);
    float photosensitive = clamp(WarpC.w, 0.0, 1.0);
    float safety = 1.0 - photosensitive * 0.55;
    float mixAmt = clamp(presence, 0.0, 1.0);

    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 centered = uv - 0.5;
    centered.x *= aspect;
    float radius = length(centered);
    float radius2 = radius * radius;
    vec2 dir = centered / max(radius, 0.0001);

    float bass = 0.70 + 0.30 * sin(seconds * TAU * 0.55 + music * 1.4);
    float breathe = 0.82 + 0.18 * sin(seconds * TAU * 0.22);
    float warp = (fisheye * 0.42 + tunnel * 0.18) * safety * breathe;
    float pull = stretch * 0.28 * safety * bass;
    centered *= 1.0 + radius2 * warp - radius * pull;
    centered.x /= aspect;
    vec2 warped = centered + 0.5;

    float split = chroma * safety * (0.018 + radius * 0.034);
    vec2 chromaDir = dir;
    chromaDir.x /= aspect;
    vec3 color;
    color.r = sampleScene(warped + chromaDir * split * 1.25).r;
    color.g = sampleScene(warped).g;
    color.b = sampleScene(warped - chromaDir * split * 1.05).b;

    float echoAmt = echo * safety;
    if (echoAmt > 0.01) {
        float spin = seconds * 0.55;
        vec2 echoA = warped + vec2(cos(spin), sin(spin)) * (0.018 * echoAmt);
        vec2 echoB = mix(warped, vec2(0.5), clamp(0.10 * echoAmt, 0.0, 0.72));
        vec2 echoC = warped - chromaDir * (0.034 * echoAmt);
        vec3 ghost = sampleScene(echoA) * vec3(0.55, 0.82, 1.0)
                   + sampleScene(echoB) * vec3(0.90, 0.68, 1.0)
                   + sampleScene(echoC) * vec3(0.70, 0.92, 1.0);
        color = mix(color, ghost / 3.0, clamp(echoAmt * 0.28, 0.0, 0.88));
    }

    if (dissociation > 0.02) {
        vec2 twin = vec2(1.0 - warped.x, warped.y);
        vec3 dream = sampleScene(mix(warped, twin, clamp(0.045 * dissociation, 0.0, 0.28)));
        color = mix(color, dream, clamp(dissociation * 0.16, 0.0, 0.42));
    }

    // Extract highlights from the distorted scene, before haze or tunnel light
    // can feed back into bloom and turn the centre into a solid white disc.
    vec3 sceneColor = color;
    float edge = smoothstep(0.10, 0.72, radius);
    float vignette = pow(edge, 1.15);
    vec3 hazeCol = vec3(0.78, 0.92, 1.0);
    color = mix(color, mix(color, hazeCol, 0.62), clamp(haze * (0.28 + vignette * 0.50) * safety, 0.0, 0.92));
    color *= mix(vec3(1.08, 1.10, 1.14), vec3(0.10, 0.18, 0.42), clamp(tunnel * vignette * 0.55 * safety, 0.0, 0.92));
    vec3 light = hazeCol * (1.0 - vignette) * tunnel * 0.12 * safety;

    float bright = max(0.0, luma(sceneColor) - 0.28);
    vec3 bloomCol = mix(vec3(0.78, 0.94, 1.0), vec3(0.96, 0.58, 1.0), clamp(radius * 1.15, 0.0, 1.0));
    light += bloomCol * bright * bloom * 0.95 * safety;
    vec2 texel = 1.0 / max(OutSize, vec2(1.0));
    vec3 glow = sampleScene(warped + vec2(texel.x * 4.0, 0.0))
              + sampleScene(warped - vec2(texel.x * 4.0, 0.0))
              + sampleScene(warped + vec2(0.0, texel.y * 4.0))
              + sampleScene(warped - vec2(0.0, texel.y * 4.0));
    light += max(glow * 0.25 - sceneColor, vec3(0.0)) * bloom * 0.55 * safety;

    vec3 rays = bloomCol * pow(max(0.0, 1.0 - radius * 1.35), 2.6) * (0.16 + music * 0.14);
    light += rays * bloom * safety;

    // Keep the full dose-driven light energy, but fit it into the remaining
    // display headroom. Reserve 40% of scene contrast even at extreme charge;
    // increasing bloom approaches this limit smoothly instead of clipping.
    float lightPeak = max(light.r, max(light.g, light.b));
    vec3 lightTint = light / max(lightPeak, 0.0001);
    float lightAmount = lightPeak / (1.0 + lightPeak);
    color += max(vec3(1.0) - color, vec3(0.0)) * 0.60 * lightTint * lightAmount;

    color = mix(source.rgb, color, mixAmt);
    fragColor = vec4(clamp(color, 0.0, 8.0), source.a);
}
