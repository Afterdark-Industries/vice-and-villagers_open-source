#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform vec2 InSize;
uniform mat4 WorldViewProjection;
uniform mat4 WorldInverseViewProjection;
uniform vec4 WorldEyePosition;
uniform vec4 WarpA; // uncapped strength, edge, melt, local seconds
uniform vec4 WarpB; // fold, autonomy, liquid, sky
uniform vec4 WarpC; // trails, prism, bloom, continuous dose phenotype
uniform vec4 PaletteA;
uniform vec4 PaletteB;
uniform vec4 PaletteC;
uniform vec4 Profile;
uniform vec4 Env;
uniform vec4 EnvDetail;
uniform vec4 Dose;
uniform vec4 DoseDetail;
uniform vec4 Control; // surface movement, colour/detail

out vec4 fragColor;
const float TAU = 6.28318530718;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
float nonzero(float v) { return abs(v)<0.000001 ? (v<0.0 ? -0.000001 : 0.000001) : v; }
float easedStep(float a,float b,float v) {
    float t=clamp((v-a)/(b-a),0.0,1.0);
    return t*t*t*(t*(t*6.0-15.0)+10.0);
}
float sampleDepth(vec2 uv) { return texture(DepthSampler,clamp(uv,vec2(0.001),vec2(0.999))).r; }
vec3 eyeAt(vec2 uv,float depth) {
    vec4 clip=WorldInverseViewProjection*vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
    return clip.xyz/nonzero(clip.w);
}
vec2 projectEye(vec3 point) {
    vec4 clip=WorldViewProjection*vec4(point,1.0);
    return clip.xy/nonzero(clip.w)*0.5+0.5;
}
float hash21(vec2 p) {
    vec3 q=fract(vec3(p.xyx)*0.1031);
    q+=dot(q,q.yzx+33.33);
    return fract((q.x+q.y)*q.z);
}
float noise(vec2 p,float period) {
    vec2 g=floor(p),f=fract(p);
    vec2 u=f*f*f*(f*(f*6.0-15.0)+10.0);
    // Integral octave periods match the compositor's 4096-block origin.
    // Hashes define a smooth spatial field, never random per-pixel grain.
    vec2 a=mod(g,period),b=mod(g+vec2(1,0),period);
    vec2 c=mod(g+vec2(0,1),period),d=mod(g+vec2(1,1),period);
    return mix(mix(hash21(a),hash21(b),u.x),mix(hash21(c),hash21(d),u.x),u.y);
}
float field(vec2 p,float seed) {
    return noise(p+seed,2048.0)*0.72+noise(p*2.0+seed+17.13,4096.0)*0.28;
}
vec3 palette(float angle) {
    vec3 w=exp(1.7*cos(vec3(angle,angle-2.094395,angle-4.188790)));
    w/=w.x+w.y+w.z;
    vec3 trip=PaletteA.rgb*w.x+PaletteB.rgb*w.y+PaletteC.rgb*w.z;
    vec3 spectrum=0.55+0.45*cos(angle+vec3(0.0,2.094395,4.188790));
    return mix(trip,spectrum,0.34);
}
vec3 hueShift(vec3 c,float a) {
    vec3 k=vec3(0.577350269);
    return c*cos(a)+cross(k,c)*sin(a)+k*dot(k,c)*(1.0-cos(a));
}
float lineField(float phase,float width) {
    float wave=0.5+0.5*cos(phase);
    float footprint=max(fwidth(phase),0.002);
    float aa=max(footprint*0.32,0.028);
    float resolved=1.0-smoothstep(1.3,3.8,footprint);
    return smoothstep(1.0-width-aa,1.0-width+aa,wave)*resolved;
}
vec2 safeSource(vec2 uv,vec3 eye,vec3 source,vec3 normal,vec2 texel) {
    vec2 candidate=projectEye(source);
    vec2 margin=texel*1.5;
    float inside=step(margin.x,candidate.x)*step(candidate.x,1.0-margin.x)
                *step(margin.y,candidate.y)*step(candidate.y,1.0-margin.y);
    candidate=clamp(candidate,margin,vec2(1.0)-margin);
    float depth=sampleDepth(candidate);
    vec3 sampled=eyeAt(candidate,depth);
    float distance=length(eye);
    float planeError=abs(dot(sampled-eye,normal));
    float sameFace=1.0-smoothstep(0.035+distance*0.0015,0.16+distance*0.004,planeError);
    float valid=inside*sameFace*(1.0-step(0.999995,depth));
    return mix(uv,candidate,valid);
}

void main() {
    vec2 uv=gl_FragCoord.xy/max(OutSize,vec2(1.0));
    vec2 texel=1.0/max(InSize,vec2(1.0));
    vec4 original=texture(DiffuseSampler,uv);
    float strength=max(0.0,WarpA.x);
    float rawDepth=sampleDepth(uv);
    if(strength<=0.000001 || rawDepth>=0.999995) { fragColor=original; return; }

    vec3 eye=eyeAt(uv,rawDepth);
    vec3 world=WorldEyePosition.xyz+eye;
    // Wide eye-relative finite differences avoid float cancellation. The
    // shorter side stays on the same face at silhouette/depth boundaries.
    // Cardinal selection never blends large coordinates with noisy normals.
    vec2 sx=vec2(texel.x*3.0,0.0),sy=vec2(0.0,texel.y*3.0);
    vec3 left=eyeAt(uv-sx,sampleDepth(uv-sx));
    vec3 right=eyeAt(uv+sx,sampleDepth(uv+sx));
    vec3 down=eyeAt(uv-sy,sampleDepth(uv-sy));
    vec3 up=eyeAt(uv+sy,sampleDepth(uv+sy));
    vec3 dx=length(right-eye)<length(eye-left) ? right-eye : eye-left;
    vec3 dy=length(up-eye)<length(eye-down) ? up-eye : eye-down;
    vec3 axis=abs(cross(dx,dy));
    vec3 normal,tangentU,tangentV;
    // A fixed tie band gives oblique/45-degree surfaces one stable projection
    // despite the last few depth-buffer bits changing between neighbouring pixels.
    if(axis.y>=axis.x*0.94 && axis.y>=axis.z*0.94) {
        normal=vec3(0,1,0); tangentU=vec3(1,0,0); tangentV=vec3(0,0,1);
    } else if(axis.z>=axis.x*0.94) {
        normal=vec3(0,0,1); tangentU=vec3(1,0,0); tangentV=vec3(0,1,0);
    } else {
        normal=vec3(1,0,0); tangentU=vec3(0,0,1); tangentV=vec3(0,1,0);
    }
    float surfaceMask=smoothstep(0.20,0.65,length(eye));
    float vertical=1.0-abs(normal.y);
    vec2 face=vec2(dot(world,tangentU),dot(world,tangentV));
    vec2 p=face*0.5;
    float time=WarpA.w;
    float presence=1.0-exp(-strength*0.90);
    float doseTravel=log(0.125+max(0.0,DoseDetail.y))+2.079441542;
    float highTail=log(1.0+max(0.0,DoseDetail.y-1.0));
    float doseContrast=easedStep(0.175,0.55,DoseDetail.y);
    // Intensity and dose both keep increasing. Logarithmic presentation keeps
    // extreme doses finite without flattening their artistic response.
    float drive=log(1.0+strength)*2.2;
    float energy=drive*(0.90+doseTravel*0.55+highTail*0.20)*(1.0+doseContrast*0.95);
    float density=1.0+drive*0.32+doseTravel*0.30+highTail*0.45+doseContrast*0.48;
    float seedA=Profile.x*37.0+1.0,seedB=Profile.y*53.0+3.0;
    float fold=max(0.0,WarpB.x),melt=max(0.0,WarpA.z),liquid=max(0.0,WarpB.z);
    vec2 clock=vec2(time*0.095,-time*0.074);
    float nA=field(p+clock,seedA);
    float nB=field(p-clock.yx*0.83,seedB);
    vec2 flow=vec2(nA-0.5,nB-0.5);
    vec2 domain=p+flow*(0.92+fold*0.10+highTail*0.13);
    float living=field(domain*2.0+vec2(time*0.082,time*0.066),seedA+seedB);
    float crystalA=sin(TAU*(domain.x+domain.y*0.5)+time*0.36+seedA);
    float crystalB=sin(TAU*(domain.y-domain.x*0.5)-time*0.29+seedB);
    float molten=0.5+0.5*sin(TAU*domain.x+nA*3.0-time*0.40+seedB);
    float column=pow(0.5+0.5*sin(TAU*(domain.x*2.0+domain.y*0.25)+nB*2.5-time*0.32),2.0);

    // Paint flows in block-space; projection gives perspective-correct local
    // texture movement rather than a lens wobble. Sky/HUD remain untouched.
    vec2 localFlow=flow*0.34+vec2(crystalA,crystalB)*0.040;
    localFlow.y+=sin(TAU*domain.x+living*3.0-time*0.27)*fold*0.014;
    float drip=(column*0.70+molten*0.30)*vertical*(0.060+melt*0.032+liquid*0.026);
    localFlow.y+=drip;
    vec2 drift=localFlow*energy*presence*Control.x*surfaceMask;
    drift/=1.0+length(drift)/0.90;
    vec3 source=eye+tangentU*drift.x+tangentV*drift.y;
    vec2 sourceUv=safeSource(uv,eye,source,normal,texel);
    vec3 color=texture(DiffuseSampler,sourceUv).rgb;
    float sourceChroma=max(color.r,max(color.g,color.b))-min(color.r,min(color.g,color.b));
    float detailMask=presence*surfaceMask*Control.y;

    // Broad marbling and crystalline bands emerge from one flowing domain.
    // Fine detail fades before becoming sub-pixel noise at extreme doses.
    float contours=lineField(living*TAU*(2.0+density*0.7)+time*0.48,0.22);
    float lattice=lineField((crystalA+crystalB)*density*2.2+time*0.24,0.20);
    float recursive=0.0;
    for(int i=0;i<3;i++) {
        float level=float(i)+1.0;
        float phase=TAU*(domain.x+domain.y*0.5)*level
                  +sin(TAU*(domain.y-domain.x*0.5)*level-time*0.28)*(0.75+density*0.32)+time*0.33;
        recursive+=lineField(phase,0.18)/(level+1.0);
    }
    float geometric=contours*0.43+lattice*0.29+recursive*0.28;
    float pattern=geometric*detailMask;
    float paletteClock=time*0.31+Profile.w*0.35+PaletteC.w*0.40;
    vec3 prism=palette(paletteClock+living*3.8+crystalA*0.30);
    float sourceLuma=luma(color);
    float hueAngle=sin(paletteClock*0.91+living*3.0+crystalB*0.22)
                  *(0.24+drive*0.22+highTail*0.08+doseContrast*0.22)*detailMask;
    color=max(vec3(0.0),hueShift(color,hueAngle));
    color=mix(vec3(luma(color)),color,1.0+(0.18+drive*0.06)*detailMask);
    // Preserve source luminance while pigments migrate. Dark blocks reveal
    // gentle luminous geometry instead of a murky full-frame filter.
    float shadow=1.0-smoothstep(0.05,0.38,sourceLuma);
    float lift=(0.025+geometric*0.065)*shadow*detailMask;
    vec3 ink=prism/max(0.20,luma(prism))*(sourceLuma+lift);
    float pigment=1.0-exp(-pattern*(0.38+energy*0.20));
    // Opposing RGB pigments used to cancel the original colour midway
    // through a palette cycle. Blend unit chroma directions with the source
    // dominant, then retain its colour range and independently blend light.
    float baseLuma=luma(color),inkLuma=luma(ink);
    float baseChroma=max(color.r,max(color.g,color.b))-min(color.r,min(color.g,color.b));
    float inkChroma=max(ink.r,max(ink.g,ink.b))-min(ink.r,min(ink.g,ink.b));
    vec3 direction=mix((color-vec3(baseLuma))/max(baseChroma,0.0001),
                       (ink-vec3(inkLuma))/max(inkChroma,0.0001),pigment*0.38);
    float directionRange=max(direction.r,max(direction.g,direction.b))-min(direction.r,min(direction.g,direction.b));
    float retainedChroma=max(max(baseChroma,sourceChroma*(1.0+detailMask*0.055)),inkChroma*pigment*0.50);
    retainedChroma=retainedChroma/(1.0+max(0.0,retainedChroma-1.0));
    direction*=retainedChroma/max(directionRange,0.0001);
    float darkRoom=-min(direction.r,min(direction.g,direction.b));
    float lightRoom=1.0-max(direction.r,max(direction.g,direction.b));
    float light=clamp(mix(baseLuma,inkLuma,pigment*0.40),darkRoom,lightRoom);
    color=vec3(light)+direction;
    color+=prism*(lift*0.75+pattern*(0.024+drive*0.009));
    float relief=sin(living*TAU*2.0-time*0.36)*0.065*detailMask;
    color*=1.0+relief;
    fragColor=vec4(clamp(color,0.0,1.0),original.a);
}
