package net.coderbot.iris.shaderpack.rendergraph.pass;

import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;

public class GenerateMipmapPassInfo implements PassInfo {
	private final TextureHandle[] target;

	public GenerateMipmapPassInfo(TextureHandle[] target) {
		this.target = target;
	}

	public TextureHandle[] getTarget() {
		return target;
	}
}
