#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D OverlaySampler;
uniform vec2 OutSize;
uniform mat4 VrViewProjection;
uniform mat4 VrInverseViewProjection;
uniform mat4 VrHeadViewProjection;
uniform mat4 VrHeadView;
uniform mat4 VrInverseHeadView;
uniform mat4 VrVisualTransform;
uniform vec4 VrEyePosition;
uniform vec4 VrHeadPosition;
uniform float VrVisualEnabled;
uniform float VrOverlayEnabled;

out vec4 fragColor;

float nonzero(float value) {
    return abs(value) < 0.0000001 ? (value < 0.0 ? -0.0000001 : 0.0000001) : value;
}

vec3 worldAt(vec2 uv, float depth) {
    vec4 point = VrInverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return VrEyePosition.xyz + point.xyz / nonzero(point.w);
}

vec3 worldRay(vec2 uv) {
    vec4 point = VrInverseViewProjection * vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    return normalize(point.xyz / nonzero(point.w));
}

vec2 directionUv(vec3 direction) {
    vec4 clip = VrViewProjection * vec4(direction, 0.0);
    return clip.xy / nonzero(clip.w) * 0.5 + 0.5;
}

vec2 pointUv(vec3 point) {
    vec4 clip = VrViewProjection * vec4(point - VrEyePosition.xyz, 1.0);
    return clip.xy / nonzero(clip.w) * 0.5 + 0.5;
}

vec2 visualUv(vec2 uv, vec3 ray) {
    // Exact neutral avoids round-trip projection noise and preserves source pixels.
    if (VrVisualEnabled <= 0.5 || VrVisualTransform == mat4(1.0)) return uv;
    vec3 headRay = (VrHeadView * vec4(ray, 0.0)).xyz;
    vec3 sourceDirection = (VrVisualTransform * vec4(headRay, 0.0)).xyz;
    vec3 worldDirection = (VrInverseHeadView * vec4(sourceDirection, 0.0)).xyz;
    float depth = texture(DepthSampler, uv).r;
    if (depth >= 0.999995) return directionUv(worldDirection);

    // Transform the current eye ray in one shared head space. Eye separation is
    // preserved by transforming the ray origin as well as its direction; using
    // identical pixel yaw/roll/zoom in both eye images would break correspondence.
    vec3 eyeHead = (VrHeadView * vec4(VrEyePosition.xyz - VrHeadPosition.xyz, 0.0)).xyz;
    vec3 sourceOrigin = (VrVisualTransform * vec4(eyeHead, 1.0)).xyz;
    float distance = length(worldAt(uv, depth) - VrEyePosition.xyz);
    vec2 candidate = uv;
    for (int iteration = 0; iteration < 2; iteration++) {
        vec3 sourceHead = sourceOrigin + sourceDirection * distance;
        vec3 sourceWorld = VrHeadPosition.xyz + (VrInverseHeadView * vec4(sourceHead, 0.0)).xyz;
        candidate = pointUv(sourceWorld);
        vec2 sampleUv = clamp(candidate, vec2(0.0), vec2(1.0));
        float sampleDepth = texture(DepthSampler, sampleUv).r;
        if (sampleDepth >= 0.999995) return directionUv(worldDirection);
        vec3 sampledHead = (VrHeadView * vec4(worldAt(sampleUv, sampleDepth) - VrHeadPosition.xyz, 0.0)).xyz;
        distance = max(0.0001, (sampledHead.z - sourceOrigin.z) / nonzero(sourceDirection.z));
    }
    vec3 sourceHead = sourceOrigin + sourceDirection * distance;
    return pointUv(VrHeadPosition.xyz + (VrInverseHeadView * vec4(sourceHead, 0.0)).xyz);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    if (VrVisualEnabled <= 0.5 && VrOverlayEnabled <= 0.5) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }
    vec3 ray = worldRay(uv);
    vec4 scene = texture(DiffuseSampler, visualUv(uv, ray));
    if (VrOverlayEnabled > 0.5) {
        // Overlays are drawn after world motion in the original shared head
        // direction, at infinity. Their opacity and authored timing are intact.
        vec4 headClip = VrHeadViewProjection * vec4(ray, 0.0);
        vec2 overlayUv = headClip.xy / nonzero(headClip.w) * 0.5 + 0.5;
        if (all(greaterThanEqual(overlayUv, vec2(0.0))) && all(lessThanEqual(overlayUv, vec2(1.0)))) {
            vec4 overlay = texture(OverlaySampler, overlayUv);
            float alpha = clamp(overlay.a, 0.0, 1.0);
            // Overlay capture is premultiplied; applying alpha again darkens art.
            scene = vec4(overlay.rgb + scene.rgb * (1.0 - alpha), alpha + scene.a * (1.0 - alpha));
        }
    }
    fragColor = scene;
}
