#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 offset = vec2(1.5, 0.75) / OutSize;
    vec3 separated = vec3(
        texture(DiffuseSampler, texCoord + offset).r,
        texture(DiffuseSampler, texCoord).g,
        texture(DiffuseSampler, texCoord - offset).b);
    fragColor = vec4(separated, 1.0);
}
