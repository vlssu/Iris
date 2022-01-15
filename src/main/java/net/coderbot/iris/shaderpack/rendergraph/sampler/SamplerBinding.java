package net.coderbot.iris.shaderpack.rendergraph.sampler;

import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;

public class SamplerBinding {
	private final TextureHandle texture;
	private final EdgeBehavior edgeBehavior;
	private final SamplerFiltering filtering;
	private final TextureCompareMode compareMode;

	public SamplerBinding(TextureHandle texture, EdgeBehavior edgeBehavior, SamplerFiltering filtering) {
		this(texture, edgeBehavior, filtering, TextureCompareMode.NONE);
	}

	public SamplerBinding(TextureHandle texture, EdgeBehavior edgeBehavior, SamplerFiltering filtering,
						  TextureCompareMode compareMode) {
		this.texture = texture;
		this.edgeBehavior = edgeBehavior;
		this.filtering = filtering;
		this.compareMode = compareMode;
	}

	public TextureHandle getTexture() {
		return texture;
	}

	public EdgeBehavior getEdgeBehavior() {
		return edgeBehavior;
	}

	public SamplerFiltering getFiltering() {
		return filtering;
	}

	public TextureCompareMode getCompareMode() {
		return compareMode;
	}
}
