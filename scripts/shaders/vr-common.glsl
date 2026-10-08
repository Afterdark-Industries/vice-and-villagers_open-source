// Stereo adapter only: the authored shader below retains its complete equations.
// All eye/head positions have one shared floating origin. Matrices contain view
// rotation, not absolute camera translation. Head view looks along negative Z.
uniform mat4 VrViewProjection;
uniform mat4 VrInverseViewProjection;
uniform mat4 VrHeadViewProjection;
uniform mat4 VrInverseHeadViewProjection;
uniform mat4 VrHeadView;
uniform vec4 VrEyePosition;
uniform vec4 VrHeadPosition;
uniform vec2 VrReferenceSize;

vec2 vrEyeUv;
vec2 vrCanonicalUv;
float vrReferenceDepth;
bool vrSky;

float vrNonzero(float value) {
    return abs(value) < 0.0000001 ? (value < 0.0 ? -0.0000001 : 0.0000001) : value;
}

vec3 vrWorldAt(vec2 eyeUv, float rawDepth) {
    vec4 point = VrInverseViewProjection * vec4(eyeUv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    return VrEyePosition.xyz + point.xyz / vrNonzero(point.w);
}

vec3 vrWorldRay(vec2 eyeUv) {
    vec4 ray = VrInverseViewProjection * vec4(eyeUv * 2.0 - 1.0, 0.0, 1.0);
    return normalize(ray.xyz / vrNonzero(ray.w));
}

vec3 vrCanonicalRay(vec2 canonicalUv) {
    vec4 ray = VrInverseHeadViewProjection * vec4(canonicalUv * 2.0 - 1.0, 0.0, 1.0);
    return normalize(ray.xyz / vrNonzero(ray.w));
}

float vrAxialDepth(vec3 worldPoint) {
    return -(VrHeadView * vec4(worldPoint - VrHeadPosition.xyz, 0.0)).z;
}

vec2 vrProjectEye(vec3 worldPoint) {
    vec4 clip = VrViewProjection * vec4(worldPoint - VrEyePosition.xyz, 1.0);
    return clip.xy / vrNonzero(clip.w) * 0.5 + 0.5;
}

vec2 vrProjectDirection(vec3 direction) {
    vec4 clip = VrViewProjection * vec4(direction, 0.0);
    return clip.xy / vrNonzero(clip.w) * 0.5 + 0.5;
}

void vrInit() {
    vrEyeUv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    float rawDepth = texture(DepthSampler, vrEyeUv).r;
    vrSky = rawDepth >= 0.999995;
    vec4 canonicalClip;
    if (vrSky) {
        canonicalClip = VrHeadViewProjection * vec4(vrWorldRay(vrEyeUv), 0.0);
        vrReferenceDepth = 1.0;
    } else {
        vec3 point = vrWorldAt(vrEyeUv, rawDepth);
        canonicalClip = VrHeadViewProjection * vec4(point - VrHeadPosition.xyz, 1.0);
        vrReferenceDepth = max(0.0001, vrAxialDepth(point));
    }
    vrCanonicalUv = canonicalClip.xy / vrNonzero(canonicalClip.w) * 0.5 + 0.5;
}

vec2 vrCanonicalToEyeUv(vec2 canonicalUv) {
    // Preserve neutral/source reads exactly, including eye projection asymmetry.
    if (all(lessThanEqual(abs(canonicalUv - vrCanonicalUv), vec2(0.0000001)))) return vrEyeUv;
    vec3 ray = vrCanonicalRay(canonicalUv);
    if (vrSky) return vrProjectDirection(ray);
    float axialRay = max(0.0001, -(VrHeadView * vec4(ray, 0.0)).z);
    float depth = vrReferenceDepth;
    vec2 candidate = vrEyeUv;
    // Reconstruct the source surface along the common head ray. A fixed metre
    // limit would erase distant deformation: these are full authored UV shifts.
    // Two depth refinements handle varying source depth without forcing either
    // eye's pixel displacement on the other. Occluded/offscreen source content
    // remains limited by what exists in this eye's framebuffer.
    for (int iteration = 0; iteration < 2; iteration++) {
        vec3 target = VrHeadPosition.xyz + ray * (depth / axialRay);
        candidate = vrProjectEye(target);
        float sampledDepth = texture(DepthSampler, clamp(candidate, vec2(0.0), vec2(1.0))).r;
        if (sampledDepth >= 0.999995) return vrProjectDirection(ray);
        vec3 sampled = vrWorldAt(clamp(candidate, vec2(0.0), vec2(1.0)), sampledDepth);
        depth = max(0.0001, vrAxialDepth(sampled));
    }
    return vrProjectEye(VrHeadPosition.xyz + ray * (depth / axialRay));
}

vec4 vrSampleColor(vec2 canonicalUv) {
    return texture(DiffuseSampler, vrCanonicalToEyeUv(canonicalUv));
}

vec4 vrSampleDepth(vec2 canonicalUv) {
    vec2 eyeUv = vrCanonicalToEyeUv(canonicalUv);
    // Texture sampling clamps at the eye framebuffer boundary, so reconstruction
    // must use that same actual sample coordinate, not an offscreen ray.
    vec2 sampleUv = clamp(eyeUv, vec2(0.0), vec2(1.0));
    float rawDepth = texture(DepthSampler, sampleUv).r;
    if (rawDepth >= 0.999995) return vec4(1.0);
    vec3 point = vrWorldAt(sampleUv, rawDepth);
    vec4 canonical = VrHeadViewProjection * vec4(point - VrHeadPosition.xyz, 1.0);
    float depth = clamp(canonical.z / vrNonzero(canonical.w) * 0.5 + 0.5, 0.0, 1.0);
    return vec4(depth);
}
