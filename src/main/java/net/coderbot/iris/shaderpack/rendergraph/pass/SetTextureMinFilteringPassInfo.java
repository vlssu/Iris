package net.coderbot.iris.shaderpack.rendergraph.pass;

import net.coderbot.iris.shaderpack.rendergraph.TextureFilteringMode;
import net.coderbot.iris.shaderpack.rendergraph.TextureHandle;

// TODO: We should just use sampler objects in render passes instead, though that would require a workaround of some
//       sort for 1.16.5 on Mac (thanks to Apple for being a special snowflake as always...)
public class SetTextureMinFilteringPassInfo implements PassInfo {
	private final TextureHandle[] target;
	private final TextureFilteringMode filteringMode;

	public SetTextureMinFilteringPassInfo(TextureHandle[] target, TextureFilteringMode filteringMode) {
		this.target = target;
		this.filteringMode = filteringMode;
	}

	public TextureHandle[] getTarget() {
		return target;
	}

	public TextureFilteringMode getFilteringMode() {
		return filteringMode;
	}
}
