#version 120
// Hinh chu nhat bo goc bang SDF: nen chuyen mau doc + vien trong, khu rang cua bang fwidth.
uniform vec2 uCenter;
uniform vec2 uHalf;
uniform float uRadius;
uniform vec4 uColorTop;
uniform vec4 uColorBot;
uniform vec4 uBorder;
uniform float uBorderWidth;
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

    float t = clamp(p.y / max(uHalf.y, 0.001) * 0.5 + 0.5, 0.0, 1.0);
    vec4 fill = mix(uColorTop, uColorBot, t);

    float bw = max(uBorderWidth, 0.0);
    float band = 0.0;
    if (bw > 0.0) band = smoothstep(-bw - aa * 0.5, -bw + aa * 0.5, d);

    float a = fill.a;
    vec3 rgb = fill.rgb;
    float ba = uBorder.a * band;
    float outA = ba + a * (1.0 - ba);
    vec3 outRgb = outA > 0.0001 ? (uBorder.rgb * ba + rgb * a * (1.0 - ba)) / outA : rgb;
    gl_FragColor = vec4(outRgb, outA * shape);
}
