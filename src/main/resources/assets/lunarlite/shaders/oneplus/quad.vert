#version 120
// Vertex shader dung chung: giu toa do cuc bo (truoc modelview) de SDF tinh dung khi GUI bi scale.
varying vec2 vLocal;
varying vec2 vUV;

void main() {
    vLocal = gl_Vertex.xy;
    vUV = gl_MultiTexCoord0.xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
