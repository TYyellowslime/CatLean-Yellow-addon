#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform SaturationConfig {
    float SaturationAmount;
};

void main() {
    vec4 source = texture(InSampler, texCoord);

    // Perceptual luminance: saturation 0 = grayscale, 1 = original, 3 = vivid colors.
    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 grayscale = vec3(luminance);
    vec3 result = mix(grayscale, source.rgb, clamp(SaturationAmount, 0.0, 3.0));

    fragColor = vec4(result, source.a);
}
