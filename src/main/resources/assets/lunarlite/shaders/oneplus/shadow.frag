#version 120
// Bong do mem: SDF cua hop bi dich xuong, mo theo uBlur; an phan nam duoi chinh panel de kinh khong bi xam.
uniform vec2 uCenter;
uniform vec2 uHalf;
uniform float uRadius;
uniform float uBlur;
uniform vec2 uOffset;
uniform vec4 uColor;
varying vec2 vLocal;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    float r = min(uRadius, min(uHalf.x, uHalf.y));
    float dShadow = sdRoundBox(vLocal - uCenter - uOffset, uHalf, r);
    float dPanel = sdRoundBox(vLocal - uCenter, uHalf, r);
    float aa = max(fwidth(dPanel), 0.0001);
    float a = 1.0 - smoothstep(-uBlur * 0.5, uBlur, dShadow);
    a *= a;
    a *= smoothstep(-aa * 0.5, aa * 0.5, dPanel);   // 0 ben trong panel
    gl_FragColor = vec4(uColor.rgb, uColor.a * a);
}
