package net.coderbot.iris.shaderpack.rendergraph;

public class ColorAttachments {
	TextureHandle[] textures;
	TextureSize size;

	public ColorAttachments(TextureHandle[] textures, TextureSize size) {
		this.textures = textures;
		this.size = size;
	}

	public TextureHandle[] getTextures() {
		return textures;
	}
}
