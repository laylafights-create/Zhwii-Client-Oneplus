#version 120
// Kinh mo (frosted glass): lay mau tu texture da blur o dung vi tri man hinh, tron voi mau sua,
// them vien trang, vet sang canh tren. uBlurMix = 0 -> khong co blur, chi con lop sua trong suot.
uniform vec2 uCenter;
uniform vec2 uHalf;
uniform float uRadius;
uniform sampler2D uBlurTex;
uniform vec2 uScreenSize;     // kich thuoc framebuffer chinh (pixel)
uniform float uBlurMix;
uniform vec3 uTint;
uniform float uGlassAlpha;
uniform float uBorderAlpha;
uniform float uBorderWidth;
uniform float uOpacity;
varying vec2 vLocal;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    vec2 p = vLocal - uCenter;
    float r = min(uRadius, min(uHalf.x, uHalf.y));
    float d = sdRoundBox(p, uHalf, r);
    float aa = max(fwidth(d), 0.0001);
    float shape = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, d);
    if (shape <= 0.0) discard;

    float t = clamp(p.y / max(uHalf.y, 0.001) * 0.5 + 0.5, 0.0, 1.0);   // 0 = mep tren, 1 = mep duoi

    vec3 bg = texture2D(uBlurTex, gl_FragCoord.xy / uScreenSize).rgb;
    vec3 milk = mix(bg, uTint, uGlassAlpha);
    vec3 rgb = mix(uTint, milk, uBlurMix);
    float a = mix(uGlassAlpha, 1.0, uBlurMix);

    // do sang chuyen nhe tu tren xuong
    rgb += vec3(1.0) * (1.0 - smoothstep(0.0, 0.55, t)) * 0.10;

    // vien trang: sang hon o mep tren
    float bw = max(uBorderWidth, 0.0);
    float band = smoothstep(-bw - aa * 0.5, -bw + aa * 0.5, d);
    float ba = clamp(uBorderAlpha * mix(1.7, 0.7, t), 0.0, 1.0) * band;
    rgb = mix(rgb, vec3(1.0), ba);
    a = mix(a, 1.0, ba);

    // vet sang manh sat mep tren
    float hl = (1.0 - smoothstep(0.0, 2.5 * aa + 1.0, p.y + uHalf.y)) * (1.0 - smoothstep(uHalf.x * 0.55, uHalf.x, abs(p.x)));
    rgb += vec3(hl * 0.18);

    gl_FragColor = vec4(min(rgb, vec3(1.0)), a * shape * uOpacity);
}
