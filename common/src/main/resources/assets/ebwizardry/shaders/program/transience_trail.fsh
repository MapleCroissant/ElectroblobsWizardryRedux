#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;
uniform vec3 Phosphor;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 current = texture(DiffuseSampler, texCoord).rgb;
    vec3 afterimage = texture(PrevSampler, texCoord).rgb * Phosphor;
    fragColor = vec4(max(current, afterimage), 1.0);
}
