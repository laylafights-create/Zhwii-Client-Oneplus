#version 120
varying vec2 vUV;

void main() {
    vUV = gl_MultiTexCoord0.xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
