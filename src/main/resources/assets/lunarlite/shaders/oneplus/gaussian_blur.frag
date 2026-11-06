#version 120
// Gaussian blur tach doi (chay 2 luot: ngang roi doc). 17 tap, ban kinh doi duoc bang uStep.
uniform sampler2D uTex;
uniform vec2 uStep;      // do lech UV giua hai tap lien tiep (da gom huong + ban kinh)
varying vec2 vUV;

void main() {
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -8; i <= 8; i++) {
        float f = float(i);
        float w = exp(-(f * f) / 32.0);   // sigma = 4 tap
        sum += texture2D(uTex, vUV + uStep * f).rgb * w;
        wsum += w;
    }
    gl_FragColor = vec4(sum / wsum, 1.0);
}
