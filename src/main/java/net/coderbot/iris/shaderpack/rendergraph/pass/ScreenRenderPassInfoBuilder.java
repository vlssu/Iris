package net.coderbot.iris.shaderpack.rendergraph.pass;

import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.rendergraph.ColorAttachments;
import net.coderbot.iris.shaderpack.rendergraph.ImageBinding;
import net.coderbot.iris.shaderpack.rendergraph.sampler.SamplerBinding;
import net.coderbot.iris.vendored.joml.Vector2f;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ScreenRenderPassInfoBuilder {
	private ProgramSource source;
	private String defaultSamplerName;
    private Map<String, SamplerBinding[]> samplers;
    private Map<String, ImageBinding[]> images;
    private Set<String> uniforms;
    private ColorAttachments[] attachmentsByParity;
    private Vector2f viewportScale;

	ScreenRenderPassInfoBuilder() {
		// no-op
	}

	public ScreenRenderPassInfoBuilder setSource(ProgramSource source) {
		this.source = source;
		return this;
	}

	public ScreenRenderPassInfoBuilder setDefaultSamplerName(String defaultSamplerName) {
		this.defaultSamplerName = defaultSamplerName;
		return this;
	}

    public ScreenRenderPassInfoBuilder setSamplers(Map<String, SamplerBinding[]> samplers) {
        this.samplers = samplers;
        return this;
    }

    public ScreenRenderPassInfoBuilder setImages(Map<String, ImageBinding[]> images) {
        this.images = images;
        return this;
    }

    public ScreenRenderPassInfoBuilder setUniforms(Set<String> uniforms) {
        this.uniforms = uniforms;
        return this;
    }

    public ScreenRenderPassInfoBuilder setAttachmentsByParity(ColorAttachments[] attachmentsByParity) {
        this.attachmentsByParity = attachmentsByParity;
        return this;
    }

    public ScreenRenderPassInfoBuilder setViewportScale(Vector2f viewportScale) {
        this.viewportScale = viewportScale;
        return this;
    }

    public ScreenRenderPassInfo build() {
        return new ScreenRenderPassInfo(Objects.requireNonNull(source), Objects.requireNonNull(samplers),
				Objects.requireNonNull(defaultSamplerName), Objects.requireNonNull(images),
				Objects.requireNonNull(uniforms), Objects.requireNonNull(attachmentsByParity),
				Objects.requireNonNull(viewportScale));
    }
}
