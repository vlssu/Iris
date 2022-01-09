package net.coderbot.iris.shaderpack.rendergraph.pass;

import net.coderbot.iris.shaderpack.ProgramSource;
import net.coderbot.iris.shaderpack.rendergraph.ColorAttachments;
import net.coderbot.iris.shaderpack.rendergraph.ImageBinding;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;
import net.coderbot.iris.vendored.joml.Vector2f;

import java.util.Map;
import java.util.Set;

public class ScreenRenderPassInfo implements PassInfo {
	private final ProgramSource source;
	// TODO: This defaultSamplerName behavior is ugly, but is needed for packs that depend on default
	//       sampler binding behavior (like Enhanced Default). We might need another lowering pass
	//       that makes use of program introspection.
	private final String defaultSamplerName;
	private final Map<String, TextureHandle[]> samplers;
	private final Map<String, ImageBinding[]> images;
	private final Set<String> uniforms;
	private final ColorAttachments[] attachmentsByParity;
	private final Vector2f viewportScale;

	public ScreenRenderPassInfo(ProgramSource source, Map<String, TextureHandle[]> samplers,
								String defaultSamplerName,
								Map<String, ImageBinding[]> images, Set<String> uniforms,
								ColorAttachments[] attachmentsByParity, Vector2f viewportScale) {
		this.source = source;
		this.defaultSamplerName = defaultSamplerName;
		this.samplers = samplers;
		this.images = images;
		this.uniforms = uniforms;
		this.attachmentsByParity = attachmentsByParity;
		this.viewportScale = viewportScale;
	}

	public static ScreenRenderPassInfoBuilder builder() {
		return new ScreenRenderPassInfoBuilder();
	}

	public ProgramSource getSource() {
		return source;
	}

	public String getDefaultSamplerName() {
		return defaultSamplerName;
	}

	public Map<String, TextureHandle[]> getSamplers() {
		return samplers;
	}

	public Map<String, ImageBinding[]> getImages() {
		return images;
	}

	public ColorAttachments[] getAttachmentsByParity() {
		return attachmentsByParity;
	}

	public Vector2f getViewportScale() {
		return viewportScale;
	}
}
